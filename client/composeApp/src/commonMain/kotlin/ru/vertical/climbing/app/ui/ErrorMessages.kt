package ru.vertical.climbing.app.ui

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import ru.vertical.climbing.domain.util.AppError
import ru.vertical.climbing.domain.util.ErrorCode
import ru.vertical.climbing.resources.Res
import ru.vertical.climbing.resources.error_bad_request
import ru.vertical.climbing.resources.error_booking_conflict
import ru.vertical.climbing.resources.error_booking_cutoff
import ru.vertical.climbing.resources.error_cancellation_forbidden
import ru.vertical.climbing.resources.error_client_already_exists
import ru.vertical.climbing.resources.error_instructor_clearance_required
import ru.vertical.climbing.resources.error_network
import ru.vertical.climbing.resources.error_no_free_spots
import ru.vertical.climbing.resources.error_not_found
import ru.vertical.climbing.resources.error_rental_modify_forbidden
import ru.vertical.climbing.resources.error_rating_already_submitted
import ru.vertical.climbing.resources.error_rating_gym_cancelled
import ru.vertical.climbing.resources.error_rating_window_expired
import ru.vertical.climbing.resources.error_rental_unavailable
import ru.vertical.climbing.resources.error_risk_consent_required
import ru.vertical.climbing.resources.error_serialization
import ru.vertical.climbing.resources.error_server
import ru.vertical.climbing.resources.error_unauthorized
import ru.vertical.climbing.resources.error_unknown

/**
 * Единый каталог user-facing сообщений по кодам ошибок (NFR-003).
 * Приоритет — локализованный текст по коду; сообщение сервера используется как fallback.
 */
@Composable
fun errorMessageFor(error: AppError): String = when (error.code) {
    ErrorCode.NETWORK -> stringResource(Res.string.error_network)
    ErrorCode.SERIALIZATION -> stringResource(Res.string.error_serialization)
    ErrorCode.UNAUTHORIZED -> stringResource(Res.string.error_unauthorized)
    ErrorCode.NOT_FOUND -> stringResource(Res.string.error_not_found)
    ErrorCode.SERVER_ERROR -> stringResource(Res.string.error_server)
    ErrorCode.CLIENT_ALREADY_EXISTS -> stringResource(Res.string.error_client_already_exists)
    ErrorCode.INSTRUCTOR_CLEARANCE_REQUIRED -> stringResource(Res.string.error_instructor_clearance_required)
    ErrorCode.RISK_CONSENT_REQUIRED -> stringResource(Res.string.error_risk_consent_required)
    ErrorCode.NO_FREE_SPOTS -> stringResource(Res.string.error_no_free_spots)
    ErrorCode.BOOKING_CUTOFF_EXCEEDED -> stringResource(Res.string.error_booking_cutoff)
    ErrorCode.BOOKING_CONFLICT, ErrorCode.CONFLICT -> stringResource(Res.string.error_booking_conflict)
    ErrorCode.BAD_REQUEST -> stringResource(Res.string.error_bad_request)
    ErrorCode.CANCELLATION_FORBIDDEN -> stringResource(Res.string.error_cancellation_forbidden)
    ErrorCode.RENTAL_UNAVAILABLE -> stringResource(Res.string.error_rental_unavailable)
    ErrorCode.FORBIDDEN -> stringResource(Res.string.error_rental_modify_forbidden)
    ErrorCode.RATING_NOT_ALLOWED_GYM_CANCELLED -> stringResource(Res.string.error_rating_gym_cancelled)
    ErrorCode.RATING_WINDOW_EXPIRED -> stringResource(Res.string.error_rating_window_expired)
    ErrorCode.RATING_ALREADY_SUBMITTED -> stringResource(Res.string.error_rating_already_submitted)
    else -> error.message ?: stringResource(Res.string.error_unknown)
}
