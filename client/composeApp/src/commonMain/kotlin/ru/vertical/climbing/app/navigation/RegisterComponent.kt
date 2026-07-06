package ru.vertical.climbing.app.navigation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import ru.vertical.climbing.domain.model.ClientRegistration
import ru.vertical.climbing.domain.usecase.RegisterClientUseCase
import ru.vertical.climbing.domain.util.AppError
import ru.vertical.climbing.domain.util.AppResult
import ru.vertical.climbing.domain.validation.RegistrationError
import ru.vertical.climbing.domain.validation.RegistrationValidator

/** SCR-002 Registration: форма и отправка регистрации (LOGIC-002). */
interface RegisterComponent {
    val model: Value<Model>

    fun onPhoneChanged(value: String)
    fun onFullNameChanged(value: String)
    fun onBirthDateChanged(value: LocalDate)
    fun onSubmit()
    fun onSubmitErrorConsumed()

    data class Model(
        val phoneDigits: String = "",
        val fullName: String = "",
        val birthDate: LocalDate? = null,
        val phoneError: RegistrationError? = null,
        val fullNameError: RegistrationError? = null,
        val birthDateError: RegistrationError? = null,
        val isSubmitting: Boolean = false,
        val submitError: AppError? = null,
    ) {
        val canSubmit: Boolean
            get() = !isSubmitting && phoneDigits.isNotBlank() && fullName.isNotBlank() && birthDate != null
    }
}

class DefaultRegisterComponent(
    componentContext: ComponentContext,
    private val registerClient: RegisterClientUseCase,
    private val onRegistered: () -> Unit,
) : RegisterComponent, ComponentContext by componentContext {

    private val scope = componentScope()

    private val _model = MutableValue(RegisterComponent.Model())
    override val model: Value<RegisterComponent.Model> = _model

    override fun onPhoneChanged(value: String) {
        val digits = value.filter { it.isDigit() }.take(RegistrationValidator.PHONE_DIGITS)
        _model.value = _model.value.copy(phoneDigits = digits, phoneError = null)
    }

    override fun onFullNameChanged(value: String) {
        _model.value = _model.value.copy(fullName = value, fullNameError = null)
    }

    override fun onBirthDateChanged(value: LocalDate) {
        _model.value = _model.value.copy(birthDate = value, birthDateError = null)
    }

    override fun onSubmitErrorConsumed() {
        _model.value = _model.value.copy(submitError = null)
    }

    override fun onSubmit() {
        val current = _model.value
        if (current.isSubmitting) return // защита от двойного тапа (AC-E03)

        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val validation = RegistrationValidator.validate(
            phoneDigits = current.phoneDigits,
            fullName = current.fullName,
            birthDate = current.birthDate,
            today = today,
        )
        if (!validation.isValid) {
            _model.value = current.copy(
                phoneError = validation.phone,
                fullNameError = validation.fullName,
                birthDateError = validation.birthDate,
            )
            return
        }

        val registration = ClientRegistration(
            phone = RegistrationValidator.toE164(current.phoneDigits),
            fullName = RegistrationValidator.normalizeName(current.fullName),
            birthDate = current.birthDate!!,
        )

        _model.value = current.copy(isSubmitting = true, submitError = null)
        scope.launch {
            when (val result = registerClient(registration)) {
                is AppResult.Success -> onRegistered()
                is AppResult.Failure -> _model.value =
                    _model.value.copy(isSubmitting = false, submitError = result.error)
            }
        }
    }
}
