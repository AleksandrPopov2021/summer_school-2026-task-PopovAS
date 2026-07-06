package ru.vertical.climbing.app.navigation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import ru.vertical.climbing.domain.model.Booking
import ru.vertical.climbing.domain.usecase.ListMyBookingsUseCase
import ru.vertical.climbing.domain.usecase.ListRateableBookingsUseCase
import ru.vertical.climbing.domain.util.AppResult
import ru.vertical.climbing.domain.util.ErrorCode
import ru.vertical.climbing.presentation.Async
import ru.vertical.climbing.presentation.offlineListFallbackOrNull
import ru.vertical.climbing.presentation.toListAsync

/** SCR-006 — список активных записей (FR-016) + CTA оценки (Post-MVP). */
interface MyBookingsListComponent {
    val model: Value<Model>

    fun onRefresh()
    fun onRetry()
    fun onBookingClicked(bookingId: String)
    fun onRateBooking(bookingId: String)
    fun onGoToSchedule()

    data class Model(
        val content: Async<List<Booking>> = Async.Loading,
        val rateableBookings: List<Booking> = emptyList(),
        val isRefreshing: Boolean = false,
        val isOffline: Boolean = false,
    )
}

class DefaultMyBookingsListComponent(
    componentContext: ComponentContext,
    private val listBookings: ListMyBookingsUseCase,
    private val listRateableBookings: ListRateableBookingsUseCase,
    private val cachedBookings: suspend () -> List<Booking>,
    private val onBookingSelected: (String) -> Unit,
    private val onRateBookingRequested: (String) -> Unit,
    private val onGoToScheduleRequested: () -> Unit,
) : MyBookingsListComponent, ComponentContext by componentContext {

    private val scope = componentScope()
    private val _model = MutableValue(MyBookingsListComponent.Model())
    override val model: Value<MyBookingsListComponent.Model> = _model

    private var loadJob: Job? = null
    private var lastBookings: List<Booking>? = null

    init {
        scope.launch {
            val cached = cachedBookings()
            if (cached.isNotEmpty()) {
                lastBookings = cached
                _model.value = _model.value.copy(content = Async.Content(cached), isOffline = true)
            }
        }
        load(refreshing = false)
    }

    fun reload() = load(refreshing = false)

    override fun onRefresh() {
        _model.value = _model.value.copy(isRefreshing = true)
        load(refreshing = true)
    }

    override fun onRetry() = load(refreshing = false)

    override fun onBookingClicked(bookingId: String) = onBookingSelected(bookingId)

    override fun onRateBooking(bookingId: String) = onRateBookingRequested(bookingId)

    override fun onGoToSchedule() = onGoToScheduleRequested()

    private fun load(refreshing: Boolean) {
        loadJob?.cancel()
        if (!refreshing && lastBookings == null) {
            _model.value = _model.value.copy(content = Async.Loading, isOffline = false)
        }
        loadJob = scope.launch {
            val result = coroutineScope {
                val bookedDeferred = async { listBookings() }
                val rateableDeferred = async { listRateableBookings() }
                LoadResult(bookedDeferred.await(), rateableDeferred.await())
            }
            when (val bookedResult = result.booked) {
                is AppResult.Success -> {
                    lastBookings = bookedResult.value
                    val rateable = (result.rateable as? AppResult.Success)?.value.orEmpty()
                    _model.value = _model.value.copy(
                        content = bookedResult.toListAsync(),
                        rateableBookings = rateable,
                        isRefreshing = false,
                        isOffline = false,
                    )
                }
                is AppResult.Failure -> applyFailure(bookedResult.error.code)
            }
        }
    }

    private suspend fun applyFailure(code: ErrorCode) {
        val cache = lastBookings ?: cachedBookings().takeIf { it.isNotEmpty() }
        val fallback = offlineListFallbackOrNull(code, cache.orEmpty())
        _model.value = if (fallback != null) {
            lastBookings = fallback.content.data
            _model.value.copy(
                content = Async.Content(fallback.content.data),
                isRefreshing = false,
                isOffline = true,
            )
        } else {
            _model.value.copy(
                content = Async.Error(ru.vertical.climbing.domain.util.AppError(code)),
                isRefreshing = false,
                isOffline = false,
            )
        }
    }

    private data class LoadResult(
        val booked: AppResult<List<Booking>>,
        val rateable: AppResult<List<Booking>>,
    )
}
