package ru.vertical.climbing.data.remote

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import ru.vertical.climbing.data.remote.dto.ErrorResponseDto
import ru.vertical.climbing.domain.util.AppError
import ru.vertical.climbing.domain.util.AppResult
import ru.vertical.climbing.domain.util.ErrorCode

/**
 * Оборачивает сетевой вызов: перехватывает исключения транспорта/сериализации в [AppError]
 * (NFR-003). Отмена корутины пробрасывается.
 */
suspend fun <T> apiCall(block: suspend () -> AppResult<T>): AppResult<T> = try {
    block()
} catch (e: CancellationException) {
    throw e
} catch (e: Throwable) {
    AppResult.Failure(AppError(code = ErrorCode.NETWORK, message = e.message, cause = e))
}

/** Разбирает успешный ответ в [T] либо `ErrorResponse` в [AppError]. */
suspend inline fun <reified T> HttpResponse.readResult(json: Json): AppResult<T> {
    val statusCode = status.value
    if (statusCode in 200..299) {
        return AppResult.Success(body<T>())
    }
    return AppResult.Failure(toAppError(json, statusCode))
}

/** Разбор пустого (204) ответа. */
suspend fun HttpResponse.readUnit(json: Json): AppResult<Unit> {
    val statusCode = status.value
    if (statusCode in 200..299) return AppResult.Success(Unit)
    return AppResult.Failure(toAppError(json, statusCode))
}

suspend fun HttpResponse.toAppError(json: Json, statusCode: Int): AppError {
    val raw = runCatching { bodyAsText() }.getOrNull()
    val parsed = raw?.let {
        runCatching { json.decodeFromString(ErrorResponseDto.serializer(), it) }.getOrNull()
    }
    val code = when {
        parsed != null -> ErrorCode.fromApiCode(parsed.code).takeIf { it != ErrorCode.UNKNOWN } ?: statusCode.toDefaultCode()
        else -> statusCode.toDefaultCode()
    }
    return AppError(code = code, message = parsed?.message, httpStatus = statusCode)
}

private fun Int.toDefaultCode(): ErrorCode = when (this) {
    400 -> ErrorCode.BAD_REQUEST
    401 -> ErrorCode.UNAUTHORIZED
    403 -> ErrorCode.FORBIDDEN
    404 -> ErrorCode.NOT_FOUND
    409 -> ErrorCode.CONFLICT
    in 500..599 -> ErrorCode.SERVER_ERROR
    else -> ErrorCode.UNKNOWN
}
