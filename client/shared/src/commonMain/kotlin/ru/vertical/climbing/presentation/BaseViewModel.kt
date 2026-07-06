package ru.vertical.climbing.presentation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Базовый MVI view model для KMP. Хранит [state], принимает [UiEvent] и шлёт
 * одноразовые [UiEffect]. Владеет собственным [CoroutineScope]; вызовите [dispose]
 * при уничтожении владельца (Decompose-компонента).
 */
abstract class BaseViewModel<S : UiState, E : UiEvent, F : UiEffect>(initialState: S) {

    protected val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow()

    private val _effects = Channel<F>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    val currentState: S get() = _state.value

    protected fun setState(reducer: S.() -> S) {
        _state.update(reducer)
    }

    protected fun sendEffect(effect: F) {
        scope.launch { _effects.send(effect) }
    }

    /** Обработка намерения пользователя. */
    abstract fun onEvent(event: E)

    open fun dispose() {
        scope.cancel()
    }
}
