package ru.vertical.climbing.app.navigation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.launch
import ru.vertical.climbing.domain.model.Booking
import ru.vertical.climbing.domain.model.RatingAvailability
import ru.vertical.climbing.domain.usecase.LoadRatingContextUseCase
import ru.vertical.climbing.domain.usecase.SubmitRatingUseCase
import ru.vertical.climbing.domain.util.AppError
import ru.vertical.climbing.domain.util.AppResult
import ru.vertical.climbing.domain.util.ErrorCode
import ru.vertical.climbing.presentation.Async

/** SCR-012 — оценка инструктора (LOGIC-014, Post-MVP). */
interface RatingComponent {
    val bookingId: String
    val model: Value<Model>

    fun onRetry()
    fun onBack()
    fun onLater()
    fun onStarsSelected(stars: Int)
    fun onSubmit()
    fun onDismissSnackbar()
    fun onSuccessConsumed()

    data class Model(
        val booking: Async<Booking> = Async.Loading,
        val availability: RatingAvailability? = null,
        val selectedStars: Int? = null,
        val isSubmitting: Boolean = false,
        val snackbarError: AppError? = null,
        val successMessage: String? = null,
        val pendingFinish: Boolean = false,
    )
}

class DefaultRatingComponent(
    componentContext: ComponentContext,
    override val bookingId: String,
    private val loadRatingContext: LoadRatingContextUseCase,
    private val submitRating: SubmitRatingUseCase,
    private val onFinished: () -> Unit,
    private val onBackRequested: () -> Unit,
) : RatingComponent, ComponentContext by componentContext {

    private val scope = componentScope()

    private val _model = MutableValue(RatingComponent.Model())
    override val model: Value<RatingComponent.Model> = _model

    init {
        load()
    }

    override fun onRetry() = load()

    override fun onBack() = onBackRequested()

    override fun onLater() = onFinished()

    override fun onStarsSelected(stars: Int) {
        if (_model.value.isSubmitting) return
        if (_model.value.availability !is RatingAvailability.Available) return
        _model.value = _model.value.copy(selectedStars = stars.coerceIn(1, 5))
    }

    override fun onSubmit() {
        val stars = _model.value.selectedStars ?: return
        if (_model.value.availability !is RatingAvailability.Available) return
        if (_model.value.isSubmitting) return

        _model.value = _model.value.copy(isSubmitting = true, snackbarError = null)
        scope.launch {
            when (val result = submitRating(bookingId, stars)) {
                is AppResult.Success -> {
                    _model.value = _model.value.copy(
                        isSubmitting = false,
                        availability = RatingAvailability.AlreadyRated(stars),
                        successMessage = "thanks",
                    )
                }
                is AppResult.Failure -> applySubmitFailure(result.error, stars)
            }
        }
    }

    override fun onDismissSnackbar() {
        if (_model.value.pendingFinish) {
            _model.value = _model.value.copy(pendingFinish = false, snackbarError = null)
            onFinished()
        } else {
            _model.value = _model.value.copy(snackbarError = null)
        }
    }

    override fun onSuccessConsumed() {
        _model.value = _model.value.copy(successMessage = null)
        onFinished()
    }

    private fun load() {
        _model.value = RatingComponent.Model(
            booking = Async.Loading,
            availability = null,
            selectedStars = null,
            isSubmitting = false,
            snackbarError = null,
        )
        scope.launch {
            when (val result = loadRatingContext(bookingId)) {
                is AppResult.Failure -> {
                    if (result.error.code == ErrorCode.NOT_FOUND) {
                        onFinished()
                    } else {
                        _model.value = _model.value.copy(booking = Async.Error(result.error))
                    }
                }
                is AppResult.Success -> {
                    val ctx = result.value
                    val availability = ctx.availability
                    _model.value = _model.value.copy(
                        booking = Async.Content(ctx.booking),
                        availability = availability,
                        selectedStars = (availability as? RatingAvailability.AlreadyRated)?.stars,
                    )
                }
            }
        }
    }

    private fun applySubmitFailure(error: AppError, stars: Int) {
        when (error.code) {
            ErrorCode.RATING_ALREADY_SUBMITTED, ErrorCode.CONFLICT -> {
                _model.value = _model.value.copy(
                    isSubmitting = false,
                    availability = RatingAvailability.AlreadyRated(stars),
                    snackbarError = AppError(code = ErrorCode.RATING_ALREADY_SUBMITTED),
                    pendingFinish = true,
                )
            }
            ErrorCode.RATING_NOT_ALLOWED_GYM_CANCELLED -> {
                _model.value = _model.value.copy(
                    isSubmitting = false,
                    availability = RatingAvailability.GymCancelled,
                )
            }
            ErrorCode.RATING_WINDOW_EXPIRED -> {
                _model.value = _model.value.copy(
                    isSubmitting = false,
                    availability = RatingAvailability.Expired,
                )
            }
            else -> {
                _model.value = _model.value.copy(isSubmitting = false, snackbarError = error)
            }
        }
    }
}
