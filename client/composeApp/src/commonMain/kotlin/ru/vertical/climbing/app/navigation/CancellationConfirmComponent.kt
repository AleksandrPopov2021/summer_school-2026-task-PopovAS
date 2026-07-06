package ru.vertical.climbing.app.navigation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.launch
import ru.vertical.climbing.domain.model.Booking
import ru.vertical.climbing.domain.model.isCancellationConfirmEnabled
import ru.vertical.climbing.domain.usecase.CancelBookingUseCase
import ru.vertical.climbing.domain.usecase.GetBookingDetailUseCase
import ru.vertical.climbing.domain.util.AppError
import ru.vertical.climbing.domain.util.AppResult
import ru.vertical.climbing.domain.util.ErrorCode
import ru.vertical.climbing.presentation.Async

/** SCR-008 — подтверждение отмены записи (LOGIC-008). */
interface CancellationConfirmComponent {
    val bookingId: String
    val model: Value<Model>

    fun onConfirm()
    fun onDismiss()
    fun onDismissSnackbar()

    data class Model(
        val content: Async<Booking> = Async.Loading,
        val isSubmitting: Boolean = false,
        val snackbarError: AppError? = null,
    ) {
        val canConfirm: Boolean
            get() {
                if (isSubmitting || content !is Async.Content) return false
                return content.data.cancellationPolicy.isCancellationConfirmEnabled()
            }
    }
}

class DefaultCancellationConfirmComponent(
    componentContext: ComponentContext,
    override val bookingId: String,
    private val getBookingDetail: GetBookingDetailUseCase,
    private val cancelBooking: CancelBookingUseCase,
    private val onCancelled: () -> Unit,
    private val onDismissRequested: () -> Unit,
) : CancellationConfirmComponent, ComponentContext by componentContext {

    private val scope = componentScope()

    private val _model = MutableValue(CancellationConfirmComponent.Model())
    override val model: Value<CancellationConfirmComponent.Model> = _model

    init {
        load()
    }

    override fun onDismiss() = onDismissRequested()

    override fun onDismissSnackbar() {
        _model.value = _model.value.copy(snackbarError = null)
    }

    override fun onConfirm() {
        val current = _model.value
        if (!current.canConfirm) return

        _model.value = current.copy(isSubmitting = true, snackbarError = null)
        scope.launch {
            when (val result = cancelBooking(bookingId)) {
                is AppResult.Success -> onCancelled()
                is AppResult.Failure -> handleFailure(result.error)
            }
        }
    }

    private fun load() {
        scope.launch {
            _model.value = when (val result = getBookingDetail(bookingId)) {
                is AppResult.Success -> _model.value.copy(content = Async.Content(result.value))
                is AppResult.Failure -> _model.value.copy(content = Async.Error(result.error))
            }
        }
    }

    private fun handleFailure(error: AppError) {
        _model.value = _model.value.copy(isSubmitting = false, snackbarError = error)
        when (error.code) {
            ErrorCode.CANCELLATION_FORBIDDEN, ErrorCode.CONFLICT -> onCancelled()
            else -> Unit
        }
    }
}
