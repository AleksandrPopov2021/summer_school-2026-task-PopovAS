package ru.vertical.climbing.app.navigation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import ru.vertical.climbing.domain.model.TrainingSlot
import ru.vertical.climbing.domain.usecase.GetRebookingForbiddenSlotIdsUseCase
import ru.vertical.climbing.domain.usecase.LoadScheduleUseCase
import ru.vertical.climbing.domain.util.AppResult
import ru.vertical.climbing.domain.util.ErrorCode
import ru.vertical.climbing.presentation.Async
import ru.vertical.climbing.presentation.offlineListFallbackOrNull
import ru.vertical.climbing.presentation.toListAsync

/**
 * SCR-003 Schedule Screen: расписание слотов на 7 дней с переключателем дат,
 * pull-to-refresh, offline-кэшем и inline empty state (SCR-013). Реализует LOGIC-003.
 */
interface ScheduleListComponent {
    val model: Value<Model>

    fun onDateSelected(date: LocalDate)
    fun onRefresh()
    fun onRetry()
    fun onSlotClicked(slotId: String)

    data class Model(
        val dates: List<LocalDate>,
        val selectedDate: LocalDate,
        val content: Async<List<TrainingSlot>> = Async.Loading,
        val isRefreshing: Boolean = false,
        val isOffline: Boolean = false,
        val rebookingForbiddenSlotIds: Set<String> = emptySet(),
    )
}

class DefaultScheduleListComponent(
    componentContext: ComponentContext,
    private val loadSchedule: LoadScheduleUseCase,
    private val getRebookingForbiddenSlotIds: GetRebookingForbiddenSlotIdsUseCase,
    private val cachedSlots: suspend () -> List<TrainingSlot>,
    private val onSlotSelected: (String) -> Unit,
) : ScheduleListComponent, ComponentContext by componentContext {

    private val scope = componentScope()

    private val _model = MutableValue(
        ScheduleListComponent.Model(dates = weekFromToday(), selectedDate = today()),
    )
    override val model: Value<ScheduleListComponent.Model> = _model

    private var loadJob: Job? = null

    init {
        scope.launch {
            val cached = cachedSlots().filter {
                it.startsAt.toLocalDateTime(TimeZone.currentSystemDefault()).date == _model.value.selectedDate
            }
            if (cached.isNotEmpty()) {
                _model.value = _model.value.copy(content = Async.Content(cached), isOffline = true)
            }
        }
        loadRebookingRestrictions()
        load(_model.value.selectedDate, refreshing = false)
    }

    override fun onDateSelected(date: LocalDate) {
        if (date == _model.value.selectedDate) return
        _model.value = _model.value.copy(selectedDate = date)
        load(date, refreshing = false)
    }

    override fun onRefresh() {
        _model.value = _model.value.copy(isRefreshing = true)
        load(_model.value.selectedDate, refreshing = true)
    }

    override fun onRetry() = load(_model.value.selectedDate, refreshing = false)

    override fun onSlotClicked(slotId: String) = onSlotSelected(slotId)

    private fun loadRebookingRestrictions() {
        scope.launch {
            when (val result = getRebookingForbiddenSlotIds()) {
                is AppResult.Success -> _model.value = _model.value.copy(rebookingForbiddenSlotIds = result.value)
                is AppResult.Failure -> Unit
            }
        }
    }

    private fun load(date: LocalDate, refreshing: Boolean) {
        loadJob?.cancel()
        if (!refreshing) {
            _model.value = _model.value.copy(content = Async.Loading, isOffline = false)
        }
        loadJob = scope.launch {
            when (val result = loadSchedule(from = date, to = date)) {
                is AppResult.Success -> if (date == _model.value.selectedDate) {
                    // AC-E03: отображаем только результат последней выбранной даты.
                    _model.value = _model.value.copy(
                        content = result.toListAsync(),
                        isRefreshing = false,
                        isOffline = false,
                    )
                }
                is AppResult.Failure -> if (date == _model.value.selectedDate) {
                    applyFailure(result.error.code)
                }
            }
        }
    }

    private suspend fun applyFailure(code: ErrorCode) {
        val cache = if (code == ErrorCode.NETWORK) {
            cachedSlots().filter {
                it.startsAt.toLocalDateTime(TimeZone.currentSystemDefault()).date == _model.value.selectedDate
            }
        } else {
            emptyList()
        }
        val fallback = offlineListFallbackOrNull(code, cache)
        _model.value = if (fallback != null) {
            _model.value.copy(content = fallback.content, isRefreshing = false, isOffline = true)
        } else {
            _model.value.copy(content = Async.Error(errorFor(code)), isRefreshing = false, isOffline = false)
        }
    }

    private fun errorFor(code: ErrorCode) = ru.vertical.climbing.domain.util.AppError(code)

    private companion object {
        const val SCHEDULE_HORIZON_DAYS = 7

        fun today(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())

        fun weekFromToday(): List<LocalDate> {
            val start = today()
            return (0 until SCHEDULE_HORIZON_DAYS).map { start.plus(it, DateTimeUnit.DAY) }
        }
    }
}
