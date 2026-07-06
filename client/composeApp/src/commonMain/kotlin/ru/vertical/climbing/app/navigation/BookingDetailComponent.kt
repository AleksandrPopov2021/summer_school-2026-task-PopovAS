package ru.vertical.climbing.app.navigation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.launch
import ru.vertical.climbing.domain.model.Booking
import ru.vertical.climbing.domain.model.canFindAlternative
import ru.vertical.climbing.domain.model.canModifyRental
import ru.vertical.climbing.domain.model.canOpenCancellationScreen
import ru.vertical.climbing.domain.model.isCancellationForbidden
import ru.vertical.climbing.domain.usecase.GetBookingDetailUseCase
import ru.vertical.climbing.domain.util.AppError
import ru.vertical.climbing.domain.util.AppResult
import ru.vertical.climbing.domain.util.ErrorCode
import ru.vertical.climbing.presentation.Async
import ru.vertical.climbing.presentation.offlineFallbackOrNull

/** SCR-007 — детали записи. */
interface BookingDetailComponent {
    val bookingId: String
    val model: Value<Model>

    fun onRetry()
    fun onBack()
    fun onCancelBooking()
    fun onModifyRental()
    fun onFindAlternative()
    fun onDismissSnackbar()
    fun onDismissSuccess()

    data class Model(
        val content: Async<Booking> = Async.Loading,
        val snackbarError: AppError? = null,
        val rentalUpdated: Boolean = false,
        val isOffline: Boolean = false,
    )
}

class DefaultBookingDetailComponent(
    componentContext: ComponentContext,
    override val bookingId: String,
    private val openAlternativeOnLoad: Boolean = false,
    private val getBookingDetail: GetBookingDetailUseCase,
    private val cachedBookings: suspend () -> List<Booking>,
    private val onBackRequested: () -> Unit,
    private val onCancelRequested: (String) -> Unit,
    private val onModifyRentalRequested: (bookingId: String, slotId: String) -> Unit,
    private val onFindAlternativeRequested: (bookingId: String, cancelledSlotId: String) -> Unit,
) : BookingDetailComponent, ComponentContext by componentContext {

    private val scope = componentScope()

    private val _model = MutableValue(BookingDetailComponent.Model())
    override val model: Value<BookingDetailComponent.Model> = _model

    init {
        load()
    }

    override fun onRetry() = load()

    override fun onBack() = onBackRequested()

    override fun onDismissSnackbar() {
        _model.value = _model.value.copy(snackbarError = null)
    }

    override fun onDismissSuccess() {
        _model.value = _model.value.copy(rentalUpdated = false)
    }

    fun onRentalUpdated() {
        _model.value = _model.value.copy(rentalUpdated = true)
        load()
    }

    fun showSnackbarError(error: AppError) {
        _model.value = _model.value.copy(snackbarError = error)
    }

    override fun onModifyRental() {
        val booking = (_model.value.content as? Async.Content)?.data ?: return
        if (!booking.canModifyRental()) return
        onModifyRentalRequested(bookingId, booking.slotId)
    }

    override fun onFindAlternative() {
        val booking = (_model.value.content as? Async.Content)?.data ?: return
        if (!booking.canFindAlternative()) return
        onFindAlternativeRequested(bookingId, booking.slotId)
    }

    override fun onCancelBooking() {
        val booking = (_model.value.content as? Async.Content)?.data ?: return
        when {
            booking.isCancellationForbidden() -> {
                _model.value = _model.value.copy(
                    snackbarError = AppError(code = ErrorCode.CANCELLATION_FORBIDDEN),
                )
            }
            booking.canOpenCancellationScreen() -> onCancelRequested(bookingId)
        }
    }

    private fun load() {
        _model.value = _model.value.copy(content = Async.Loading, isOffline = false)
        scope.launch {
            _model.value = when (val result = getBookingDetail(bookingId)) {
                is AppResult.Success -> {
                    val booking = result.value
                    if (openAlternativeOnLoad && booking.canFindAlternative()) {
                        onFindAlternativeRequested(bookingId, booking.slotId)
                    }
                    _model.value.copy(content = Async.Content(booking), isOffline = false)
                }
                is AppResult.Failure -> {
                    val cached = cachedBookings().firstOrNull { it.id == bookingId }
                    val fallback = offlineFallbackOrNull(result.error.code, cached)
                    if (fallback != null) {
                        _model.value.copy(content = fallback.content, isOffline = true)
                    } else {
                        _model.value.copy(content = Async.Error(result.error), isOffline = false)
                    }
                }
            }
        }
    }
}
