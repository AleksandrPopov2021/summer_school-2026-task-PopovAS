package ru.vertical.climbing.app.navigation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.launch
import ru.vertical.climbing.domain.model.TrainingSlot
import ru.vertical.climbing.domain.usecase.GetRebookingForbiddenSlotIdsUseCase
import ru.vertical.climbing.domain.usecase.GetSlotDetailUseCase
import ru.vertical.climbing.domain.util.AppResult
import ru.vertical.climbing.presentation.Async
import ru.vertical.climbing.presentation.offlineFallbackOrNull

/**
 * SCR-004 Slot Detail Screen: полная информация о слоте + доступность записи (LOGIC-004).
 * Кнопка «Записаться» ведёт на flow записи (SCR-005, Итерация 3).
 */
interface SlotDetailComponent {
    val slotId: String
    val model: Value<Model>

    fun onRetry()
    fun onBack()
    fun onBook()

    data class Model(
        val content: Async<TrainingSlot> = Async.Loading,
        val rebookingForbiddenSlotIds: Set<String> = emptySet(),
        val isOffline: Boolean = false,
    )
}

class DefaultSlotDetailComponent(
    componentContext: ComponentContext,
    override val slotId: String,
    private val getSlotDetail: GetSlotDetailUseCase,
    private val getRebookingForbiddenSlotIds: GetRebookingForbiddenSlotIdsUseCase,
    private val cachedSlots: suspend () -> List<TrainingSlot>,
    private val onBackRequested: () -> Unit,
    private val onBookRequested: (String) -> Unit,
) : SlotDetailComponent, ComponentContext by componentContext {

    private val scope = componentScope()

    private val _model = MutableValue(SlotDetailComponent.Model())
    override val model: Value<SlotDetailComponent.Model> = _model

    init {
        load()
    }

    override fun onRetry() = load()

    override fun onBack() = onBackRequested()

    override fun onBook() = onBookRequested(slotId)

    private fun load() {
        scope.launch {
            val cached = cachedSlots().firstOrNull { it.id == slotId }
            if (cached != null) {
                _model.value = _model.value.copy(content = Async.Content(cached), isOffline = true)
            } else {
                _model.value = _model.value.copy(content = Async.Loading, isOffline = false)
            }
            val forbiddenIds = when (val result = getRebookingForbiddenSlotIds()) {
                is AppResult.Success -> result.value
                is AppResult.Failure -> emptySet()
            }
            _model.value = when (val result = getSlotDetail(slotId)) {
                is AppResult.Success -> _model.value.copy(
                    content = Async.Content(result.value),
                    rebookingForbiddenSlotIds = forbiddenIds,
                    isOffline = false,
                )
                is AppResult.Failure -> {
                    val fallback = offlineFallbackOrNull(result.error.code, cached)
                    if (fallback != null) {
                        _model.value.copy(
                            content = fallback.content,
                            rebookingForbiddenSlotIds = forbiddenIds,
                            isOffline = true,
                        )
                    } else {
                        _model.value.copy(
                            content = Async.Error(result.error),
                            rebookingForbiddenSlotIds = forbiddenIds,
                            isOffline = false,
                        )
                    }
                }
            }
        }
    }
}
