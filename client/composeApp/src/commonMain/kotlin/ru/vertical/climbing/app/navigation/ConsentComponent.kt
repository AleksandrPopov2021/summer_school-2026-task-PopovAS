package ru.vertical.climbing.app.navigation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.launch
import ru.vertical.climbing.domain.usecase.AcceptRiskConsentUseCase
import ru.vertical.climbing.domain.util.AppError
import ru.vertical.climbing.domain.util.AppResult

/**
 * SCR-015 Consent Screen — подтверждение согласия на риск (LOGIC-006, FR-012).
 */
interface ConsentComponent {
    val slotId: String
    val model: Value<Model>

    fun onConsentChecked(checked: Boolean)
    fun onConfirm()
    fun onCancel()
    fun onErrorConsumed()

    data class Model(
        val consentChecked: Boolean = false,
        val isSubmitting: Boolean = false,
        val error: AppError? = null,
    ) {
        val canConfirm: Boolean get() = consentChecked && !isSubmitting
    }
}

class DefaultConsentComponent(
    componentContext: ComponentContext,
    override val slotId: String,
    private val acceptRiskConsent: AcceptRiskConsentUseCase,
    private val onConsentAccepted: (String) -> Unit,
    private val onCancelRequested: () -> Unit,
) : ConsentComponent, ComponentContext by componentContext {

    private val scope = componentScope()

    private val _model = MutableValue(ConsentComponent.Model())
    override val model: Value<ConsentComponent.Model> = _model

    override fun onConsentChecked(checked: Boolean) {
        _model.value = _model.value.copy(consentChecked = checked)
    }

    override fun onErrorConsumed() {
        _model.value = _model.value.copy(error = null)
    }

    override fun onCancel() = onCancelRequested()

    override fun onConfirm() {
        val current = _model.value
        if (!current.canConfirm) return

        _model.value = current.copy(isSubmitting = true, error = null)
        scope.launch {
            when (val result = acceptRiskConsent()) {
                is AppResult.Success -> onConsentAccepted(slotId)
                is AppResult.Failure -> _model.value =
                    _model.value.copy(isSubmitting = false, error = result.error)
            }
        }
    }
}
