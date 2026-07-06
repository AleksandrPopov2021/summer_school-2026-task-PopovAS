package ru.vertical.climbing.app.navigation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackCallback
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import ru.vertical.climbing.domain.model.EditableNotificationToggles
import ru.vertical.climbing.domain.model.NotificationPreferences
import ru.vertical.climbing.domain.model.hasUnsavedNotificationChanges
import ru.vertical.climbing.domain.model.toEditableToggles
import ru.vertical.climbing.domain.usecase.GetNotificationPreferencesUseCase
import ru.vertical.climbing.domain.usecase.SignOutUseCase
import ru.vertical.climbing.domain.usecase.UpdateNotificationPreferencesUseCase
import ru.vertical.climbing.domain.util.AppError
import ru.vertical.climbing.domain.util.AppResult
import ru.vertical.climbing.domain.util.ErrorCode
import ru.vertical.climbing.presentation.Async

/** SCR-011 — настройки push-уведомлений (LOGIC-011). */
interface NotificationSettingsComponent {
    val model: Value<Model>

    fun onRetry()
    fun onBookingConfirmationToggled(enabled: Boolean)
    fun onRatingInvitationToggled(enabled: Boolean)
    fun onSaveClicked()
    fun onBackClicked()
    fun onBackDiscardConfirmed()
    fun onBackSaveConfirmed()
    fun onBackDialogDismissed()
    fun onDismissSnackbar()
    fun onDismissSuccessSnackbar()

    data class Model(
        val content: Async<NotificationPreferences> = Async.Loading,
        val localToggles: EditableNotificationToggles? = null,
        val isSaving: Boolean = false,
        val snackbarError: AppError? = null,
        val showSuccessSnackbar: Boolean = false,
        val showUnsavedDialog: Boolean = false,
        val isOffline: Boolean = false,
    ) {
        val hasUnsavedChanges: Boolean
            get() {
                val saved = (content as? Async.Content)?.data ?: return false
                val local = localToggles ?: return false
                return hasUnsavedNotificationChanges(saved, local)
            }

        val canSave: Boolean get() = hasUnsavedChanges && !isSaving && content is Async.Content
    }
}

class DefaultNotificationSettingsComponent(
    componentContext: ComponentContext,
    private val getPreferences: GetNotificationPreferencesUseCase,
    private val updatePreferences: UpdateNotificationPreferencesUseCase,
    private val signOut: SignOutUseCase,
    private val onBackRequested: () -> Unit,
    private val onSessionExpired: () -> Unit,
) : NotificationSettingsComponent, ComponentContext by componentContext {

    private val scope = componentScope()
    private val _model = MutableValue(NotificationSettingsComponent.Model())
    override val model: Value<NotificationSettingsComponent.Model> = _model

    private var loadJob: Job? = null
    private var saveJob: Job? = null
    private var cachedPreferences: NotificationPreferences? = null

    init {
        load()
        backHandler.register(BackCallback { onBackClicked() })
    }

    override fun onRetry() = load()

    override fun onBookingConfirmationToggled(enabled: Boolean) {
        val local = _model.value.localToggles ?: return
        _model.value = _model.value.copy(
            localToggles = local.copy(bookingConfirmationEnabled = enabled),
        )
    }

    override fun onRatingInvitationToggled(enabled: Boolean) {
        val local = _model.value.localToggles ?: return
        _model.value = _model.value.copy(
            localToggles = local.copy(ratingInvitationEnabled = enabled),
        )
    }

    override fun onSaveClicked() {
        if (!_model.value.canSave) return
        val local = _model.value.localToggles ?: return
        saveJob?.cancel()
        _model.value = _model.value.copy(isSaving = true, snackbarError = null)
        saveJob = scope.launch {
            when (val result = updatePreferences(local)) {
                is AppResult.Success -> _model.value = _model.value.copy(
                    content = Async.Content(result.value),
                    localToggles = result.value.toEditableToggles(),
                    isSaving = false,
                    showSuccessSnackbar = true,
                )
                is AppResult.Failure -> {
                    _model.value = _model.value.copy(isSaving = false, snackbarError = result.error)
                    if (result.error.code == ErrorCode.UNAUTHORIZED) {
                        signOut()
                        onSessionExpired()
                    }
                }
            }
        }
    }

    override fun onBackClicked() {
        if (_model.value.hasUnsavedChanges) {
            _model.value = _model.value.copy(showUnsavedDialog = true)
        } else {
            onBackRequested()
        }
    }

    override fun onBackDiscardConfirmed() {
        _model.value = _model.value.copy(showUnsavedDialog = false)
        onBackRequested()
    }

    override fun onBackSaveConfirmed() {
        _model.value = _model.value.copy(showUnsavedDialog = false)
        onSaveClicked()
    }

    override fun onBackDialogDismissed() {
        _model.value = _model.value.copy(showUnsavedDialog = false)
    }

    override fun onDismissSnackbar() {
        _model.value = _model.value.copy(snackbarError = null)
    }

    override fun onDismissSuccessSnackbar() {
        _model.value = _model.value.copy(showSuccessSnackbar = false)
    }

    private fun load() {
        loadJob?.cancel()
        _model.value = _model.value.copy(content = Async.Loading, snackbarError = null)
        loadJob = scope.launch {
            when (val result = getPreferences()) {
                is AppResult.Success -> {
                    cachedPreferences = result.value
                    _model.value = _model.value.copy(
                        content = Async.Content(result.value),
                        localToggles = result.value.toEditableToggles(),
                        isOffline = false,
                    )
                }
                is AppResult.Failure -> {
                    val cached = cachedPreferences
                    if (result.error.code == ErrorCode.NETWORK && cached != null) {
                        _model.value = _model.value.copy(
                            content = Async.Content(cached),
                            localToggles = cached.toEditableToggles(),
                            isOffline = true,
                        )
                    } else {
                        _model.value = _model.value.copy(content = Async.Error(result.error), isOffline = false)
                    }
                    if (result.error.code == ErrorCode.UNAUTHORIZED) {
                        signOut()
                        onSessionExpired()
                    }
                }
            }
        }
    }
}
