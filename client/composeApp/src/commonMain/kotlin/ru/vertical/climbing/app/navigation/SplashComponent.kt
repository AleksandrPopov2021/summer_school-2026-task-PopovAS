package ru.vertical.climbing.app.navigation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.vertical.climbing.domain.usecase.CheckSessionUseCase
import ru.vertical.climbing.domain.usecase.SessionState
import ru.vertical.climbing.domain.util.AppError
import ru.vertical.climbing.domain.util.AppResult

/** Результат маршрутизации после проверки сессии (LOGIC-001). */
enum class SessionRoute { REGISTRATION, SCHEDULE, SCHEDULE_OFFLINE }

/** SCR-001 Splash: проверка сессии и выбор стартового экрана. */
interface SplashComponent {
    val model: Value<Model>
    fun onRetry()

    data class Model(
        val isLoading: Boolean = true,
        val error: AppError? = null,
    )
}

class DefaultSplashComponent(
    componentContext: ComponentContext,
    private val checkSession: CheckSessionUseCase,
    private val onSessionResolved: (SessionRoute) -> Unit,
) : SplashComponent, ComponentContext by componentContext {

    private val scope = componentScope()

    private val _model = MutableValue(SplashComponent.Model())
    override val model: Value<SplashComponent.Model> = _model

    init {
        runCheck()
    }

    override fun onRetry() {
        _model.value = SplashComponent.Model(isLoading = true, error = null)
        runCheck()
    }

    private fun runCheck() {
        scope.launch {
            // Проверка сессии идёт параллельно с минимальным показом splash (NFR-UI-002).
            val sessionDeferred = async { checkSession() }
            delay(MIN_SPLASH_MS)
            when (val result = sessionDeferred.await()) {
                is AppResult.Success -> onSessionResolved(result.value.toRoute())
                is AppResult.Failure -> _model.value =
                    SplashComponent.Model(isLoading = false, error = result.error)
            }
        }
    }

    private fun SessionState.toRoute(): SessionRoute = when (this) {
        is SessionState.Unauthenticated -> SessionRoute.REGISTRATION
        is SessionState.Authenticated -> SessionRoute.SCHEDULE
        is SessionState.Offline -> SessionRoute.SCHEDULE_OFFLINE
    }

    private companion object {
        /** Минимальный брендинг splash; не блокирует быстрый переход (NFR-007). */
        const val MIN_SPLASH_MS = 400L
    }
}
