package ru.vertical.climbing.app.navigation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import ru.vertical.climbing.domain.model.Client
import ru.vertical.climbing.data.local.ApiEnvStore
import ru.vertical.climbing.data.remote.ApiEnvironment
import ru.vertical.climbing.platform.isDebugBuild
import ru.vertical.climbing.domain.usecase.LoadProfileUseCase
import ru.vertical.climbing.domain.usecase.ProfileData
import ru.vertical.climbing.domain.usecase.SignOutUseCase
import ru.vertical.climbing.domain.util.AppResult
import ru.vertical.climbing.domain.util.ErrorCode
import ru.vertical.climbing.presentation.Async

/** SCR-010 — профиль и лояльность. */
interface ProfileScreenComponent {
    val model: Value<Model>

    fun onRefresh()
    fun onRetry()
    fun onNotificationSettingsClicked()
    fun onLogoutClicked()
    fun onLogoutConfirmed()
    fun onLogoutDismissed()
    fun onDebugEnvironmentSelected(environment: ApiEnvironment)
    fun onDebugMockToggled(useMock: Boolean)

    data class DebugEnvState(
        val environment: ApiEnvironment,
        val useMock: Boolean,
    )

    data class Model(
        val content: Async<ProfileData> = Async.Loading,
        val isRefreshing: Boolean = false,
        val showLogoutDialog: Boolean = false,
        val isSigningOut: Boolean = false,
        val debugEnv: DebugEnvState? = null,
    )
}

class DefaultProfileScreenComponent(
    componentContext: ComponentContext,
    private val loadProfile: LoadProfileUseCase,
    private val signOut: SignOutUseCase,
    private val apiEnvStore: ApiEnvStore,
    private val onNotificationSettingsRequested: () -> Unit,
    private val onSignedOut: () -> Unit,
    private val onSessionExpired: () -> Unit,
) : ProfileScreenComponent, ComponentContext by componentContext {

    private val scope = componentScope()
    private val _model = MutableValue(
        ProfileScreenComponent.Model(
            debugEnv = if (isDebugBuild()) {
                ProfileScreenComponent.DebugEnvState(
                    environment = apiEnvStore.currentEnvironment(),
                    useMock = apiEnvStore.isMockEnabled(),
                )
            } else {
                null
            },
        ),
    )
    override val model: Value<ProfileScreenComponent.Model> = _model

    private var loadJob: Job? = null
    private var cachedClient: Client? = null

    init {
        load(refreshing = false)
    }

    override fun onRefresh() {
        _model.value = _model.value.copy(isRefreshing = true)
        load(refreshing = true)
    }

    override fun onRetry() = load(refreshing = false)

    override fun onNotificationSettingsClicked() = onNotificationSettingsRequested()

    override fun onLogoutClicked() {
        _model.value = _model.value.copy(showLogoutDialog = true)
    }

    override fun onLogoutDismissed() {
        _model.value = _model.value.copy(showLogoutDialog = false)
    }

    override fun onDebugEnvironmentSelected(environment: ApiEnvironment) {
        val current = _model.value.debugEnv ?: return
        apiEnvStore.save(environment, current.useMock)
        _model.value = _model.value.copy(debugEnv = current.copy(environment = environment))
    }

    override fun onDebugMockToggled(useMock: Boolean) {
        val current = _model.value.debugEnv ?: return
        apiEnvStore.save(current.environment, useMock)
        _model.value = _model.value.copy(debugEnv = current.copy(useMock = useMock))
    }

    override fun onLogoutConfirmed() {
        if (_model.value.isSigningOut) return
        _model.value = _model.value.copy(isSigningOut = true, showLogoutDialog = false)
        scope.launch {
            signOut()
            _model.value = _model.value.copy(isSigningOut = false)
            onSignedOut()
        }
    }

    private fun load(refreshing: Boolean) {
        loadJob?.cancel()
        if (!refreshing) {
            _model.value = _model.value.copy(content = Async.Loading)
        }
        loadJob = scope.launch {
            when (val result = loadProfile(cachedClient)) {
                is AppResult.Success -> {
                    cachedClient = result.value.client
                    _model.value = _model.value.copy(
                        content = Async.Content(result.value),
                        isRefreshing = false,
                    )
                }
                is AppResult.Failure -> {
                    _model.value = _model.value.copy(
                        content = Async.Error(result.error),
                        isRefreshing = false,
                    )
                    if (result.error.code == ErrorCode.UNAUTHORIZED) {
                        signOut()
                        onSessionExpired()
                    }
                }
            }
        }
    }
}
