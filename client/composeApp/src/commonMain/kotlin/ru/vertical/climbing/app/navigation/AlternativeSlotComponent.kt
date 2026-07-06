package ru.vertical.climbing.app.navigation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import ru.vertical.climbing.domain.model.AlternativeSlotResult
import ru.vertical.climbing.domain.model.Booking
import ru.vertical.climbing.domain.model.TrainingSlot
import ru.vertical.climbing.domain.model.canBookAlternativeSlot
import ru.vertical.climbing.domain.usecase.FindAlternativeSlotUseCase
import ru.vertical.climbing.domain.usecase.GetBookingDetailUseCase
import ru.vertical.climbing.domain.usecase.PrefillBookingDraftFromBookingUseCase
import ru.vertical.climbing.domain.util.AppError
import ru.vertical.climbing.domain.util.AppResult
import ru.vertical.climbing.domain.util.ErrorCode
import ru.vertical.climbing.presentation.Async

/** SCR-009 — предложение альтернативного слота (LOGIC-009). */
interface AlternativeSlotComponent {
    val bookingId: String
    val cancelledSlotId: String
    val model: Value<Model>

    fun onRetry()
    fun onBack()
    fun onBookAlternative()
    fun onChooseOtherSlot()
    fun onLater()
    fun onDismissSnackbar()

    sealed interface AlternativeState {
        data class Found(val slot: TrainingSlot, val canBook: Boolean) : AlternativeState
        data object NotFound : AlternativeState
    }

    data class Model(
        val booking: Async<Booking> = Async.Loading,
        val alternative: Async<AlternativeState> = Async.Loading,
        val snackbarError: AppError? = null,
    )
}

class DefaultAlternativeSlotComponent(
    componentContext: ComponentContext,
    override val bookingId: String,
    override val cancelledSlotId: String,
    private val getBookingDetail: GetBookingDetailUseCase,
    private val findAlternative: FindAlternativeSlotUseCase,
    private val prefillDraft: PrefillBookingDraftFromBookingUseCase,
    private val onBackRequested: () -> Unit,
    private val onBookAlternativeRequested: (String) -> Unit,
    private val onChooseScheduleRequested: () -> Unit,
    private val onLaterRequested: () -> Unit,
    private val onNotFoundNavigateToList: () -> Unit,
) : AlternativeSlotComponent, ComponentContext by componentContext {

    private val scope = componentScope()

    private val _model = MutableValue(AlternativeSlotComponent.Model())
    override val model: Value<AlternativeSlotComponent.Model> = _model

    init {
        load()
    }

    override fun onRetry() = load()

    override fun onBack() = onBackRequested()

    override fun onDismissSnackbar() {
        _model.value = _model.value.copy(snackbarError = null)
    }

    override fun onBookAlternative() {
        val booking = (_model.value.booking as? Async.Content)?.data ?: return
        val alternative = (_model.value.alternative as? Async.Content)?.data as? AlternativeSlotComponent.AlternativeState.Found
            ?: return
        if (!alternative.canBook) return

        scope.launch {
            prefillDraft(alternative.slot.id, booking)
            onBookAlternativeRequested(alternative.slot.id)
        }
    }

    override fun onChooseOtherSlot() = onChooseScheduleRequested()

    override fun onLater() = onLaterRequested()

    private fun load() {
        _model.value = _model.value.copy(
            booking = Async.Loading,
            alternative = Async.Loading,
            snackbarError = null,
        )
        scope.launch {
            val result = coroutineScope {
                val bookingDeferred = async { getBookingDetail(bookingId) }
                val alternativeDeferred = async { findAlternative(cancelledSlotId, bookingId) }

                val bookingResult = bookingDeferred.await()
                val alternativeResult = alternativeDeferred.await()

                when {
                    bookingResult is AppResult.Failure && bookingResult.error.code == ErrorCode.NOT_FOUND ->
                        LoadResult(bookingResult, null)
                    bookingResult is AppResult.Failure -> LoadResult(bookingResult, null)
                    alternativeResult is AppResult.Failure -> LoadResult(bookingResult, alternativeResult)
                    else -> {
                        val booking = (bookingResult as AppResult.Success).value
                        val alt = (alternativeResult as AppResult.Success).value
                        LoadResult(
                            AppResult.Success(booking),
                            AppResult.Success(resolveAlternativeState(alt, booking)),
                        )
                    }
                }
            }

            when {
                result.booking is AppResult.Failure && result.booking.error.code == ErrorCode.NOT_FOUND -> {
                    _model.value = _model.value.copy(
                        snackbarError = result.booking.error,
                    )
                    onNotFoundNavigateToList()
                }
                result.booking is AppResult.Failure -> {
                    _model.value = _model.value.copy(booking = Async.Error(result.booking.error))
                }
                result.alternative is AppResult.Failure -> {
                    _model.value = _model.value.copy(
                        booking = (result.booking as AppResult.Success).value.let { Async.Content(it) },
                        alternative = Async.Error(result.alternative.error),
                    )
                }
                else -> {
                    val booking = (result.booking as AppResult.Success).value
                    val alternative = (result.alternative as AppResult.Success).value
                    _model.value = _model.value.copy(
                        booking = Async.Content(booking),
                        alternative = Async.Content(alternative),
                    )
                }
            }
        }
    }

    private fun resolveAlternativeState(
        result: AlternativeSlotResult,
        booking: Booking,
    ): AlternativeSlotComponent.AlternativeState {
        val slot = result.alternativeSlot
        if (!result.found || slot == null) {
            return AlternativeSlotComponent.AlternativeState.NotFound
        }
        if (booking.rebookingForbidden && slot.id == cancelledSlotId) {
            return AlternativeSlotComponent.AlternativeState.NotFound
        }
        val canBook = canBookAlternativeSlot(
            alternative = slot,
            cancelledSlotId = cancelledSlotId,
            rebookingForbidden = booking.rebookingForbidden,
        )
        return AlternativeSlotComponent.AlternativeState.Found(slot, canBook)
    }

    private data class LoadResult(
        val booking: AppResult<Booking>,
        val alternative: AppResult<AlternativeSlotComponent.AlternativeState>?,
    )
}
