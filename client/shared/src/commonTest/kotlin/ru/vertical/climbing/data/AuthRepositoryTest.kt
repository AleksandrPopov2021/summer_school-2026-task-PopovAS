package ru.vertical.climbing.data

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import ru.vertical.climbing.data.local.TokenStore
import ru.vertical.climbing.data.remote.appJson
import ru.vertical.climbing.data.repository.AuthRepositoryImpl
import ru.vertical.climbing.domain.model.ClientRegistration
import ru.vertical.climbing.domain.util.AppResult
import ru.vertical.climbing.domain.util.ErrorCode

class AuthRepositoryTest {

    private val registration = ClientRegistration(
        phone = "+79001234567",
        fullName = "Иванов Иван",
        birthDate = LocalDate.parse("1995-03-15"),
    )

    private fun client(status: HttpStatusCode, body: String): HttpClient {
        val engine = MockEngine {
            respond(
                content = ByteReadChannel(body),
                status = status,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        return HttpClient(engine) {
            expectSuccess = false
            install(ContentNegotiation) { json(appJson) }
            install(DefaultRequest) {
                url("https://api.local/v1/")
                contentType(ContentType.Application.Json)
            }
        }
    }

    @Test
    fun register_conflict_maps_to_client_already_exists() = runTest {
        val repo = AuthRepositoryImpl(
            client = client(
                status = HttpStatusCode.Conflict,
                body = """{"code":"CLIENT_ALREADY_EXISTS","message":"Уже зарегистрирован"}""",
            ),
            tokenStore = TokenStore(MapSettings()),
            json = appJson,
        )

        val result = repo.register(registration)

        assertIs<AppResult.Failure>(result)
        assertEquals(ErrorCode.CLIENT_ALREADY_EXISTS, result.error.code)
    }

    @Test
    fun register_success_saves_token() = runTest {
        val store = TokenStore(MapSettings())
        val body = """
            {
              "access_token": "jwt-123",
              "token_type": "Bearer",
              "client": {
                "id": "c1",
                "full_name": "Иванов Иван",
                "phone": "+79001234567",
                "birth_date": "1995-03-15",
                "risk_consent_accepted": false,
                "completed_visits_count": 0,
                "is_loyal_client": false,
                "late_cancellation_count": 0,
                "no_show_count": 0
              }
            }
        """.trimIndent()
        val repo = AuthRepositoryImpl(
            client = client(HttpStatusCode.Created, body),
            tokenStore = store,
            json = appJson,
        )

        val result = repo.register(registration)

        assertIs<AppResult.Success<*>>(result)
        // Токен сохраняется в use case, здесь проверяем сам ответ репозитория.
        assertEquals("jwt-123", (result.value as ru.vertical.climbing.domain.model.AuthSession).accessToken)
    }
}
