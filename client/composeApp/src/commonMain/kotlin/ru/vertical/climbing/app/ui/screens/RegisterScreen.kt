package ru.vertical.climbing.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import ru.vertical.climbing.app.navigation.RegisterComponent
import ru.vertical.climbing.app.ui.UiTestTags
import ru.vertical.climbing.app.ui.errorMessageFor
import ru.vertical.climbing.domain.validation.RegistrationError
import ru.vertical.climbing.resources.Res
import ru.vertical.climbing.resources.a11y_open_calendar
import ru.vertical.climbing.resources.action_register
import ru.vertical.climbing.resources.error_birth_invalid
import ru.vertical.climbing.resources.error_name_invalid
import ru.vertical.climbing.resources.error_phone_invalid
import ru.vertical.climbing.resources.field_birth_date
import ru.vertical.climbing.resources.field_full_name
import ru.vertical.climbing.resources.field_full_name_placeholder
import ru.vertical.climbing.resources.field_phone
import ru.vertical.climbing.resources.picker_cancel
import ru.vertical.climbing.resources.picker_confirm
import ru.vertical.climbing.resources.title_registration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(component: RegisterComponent) {
    val model by component.model.subscribeAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val submitError = model.submitError
    val submitErrorMessage = submitError?.let { errorMessageFor(it) }
    LaunchedEffect(submitError) {
        if (submitError != null && submitErrorMessage != null) {
            snackbarHostState.showSnackbar(submitErrorMessage)
            component.onSubmitErrorConsumed()
        }
    }

    var showDatePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(Res.string.title_registration)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Button(
                    onClick = component::onSubmit,
                    enabled = model.canSubmit,
                    modifier = Modifier.fillMaxWidth().testTag(UiTestTags.REGISTER_SUBMIT),
                ) {
                    if (model.isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    } else {
                        Text(stringResource(Res.string.action_register))
                    }
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = model.phoneDigits,
                onValueChange = component::onPhoneChanged,
                label = { Text(stringResource(Res.string.field_phone)) },
                prefix = { Text("+7 ") },
                singleLine = true,
                isError = model.phoneError != null,
                supportingText = { model.phoneError?.let { Text(it.messageText()) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                enabled = !model.isSubmitting,
                modifier = Modifier.fillMaxWidth().testTag(UiTestTags.REGISTER_PHONE),
            )

            OutlinedTextField(
                value = model.fullName,
                onValueChange = component::onFullNameChanged,
                label = { Text(stringResource(Res.string.field_full_name)) },
                placeholder = { Text(stringResource(Res.string.field_full_name_placeholder)) },
                singleLine = true,
                isError = model.fullNameError != null,
                supportingText = { model.fullNameError?.let { Text(it.messageText()) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                enabled = !model.isSubmitting,
                modifier = Modifier.fillMaxWidth().testTag(UiTestTags.REGISTER_NAME),
            )

            OutlinedTextField(
                value = model.birthDate?.formatRu().orEmpty(),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(Res.string.field_birth_date)) },
                isError = model.birthDateError != null,
                supportingText = { model.birthDateError?.let { Text(it.messageText()) } },
                trailingIcon = {
                    IconButton(onClick = { if (!model.isSubmitting) showDatePicker = true }) {
                        Icon(Icons.Filled.CalendarMonth, contentDescription = stringResource(Res.string.a11y_open_calendar))
                    }
                },
                enabled = !model.isSubmitting,
                modifier = Modifier.fillMaxWidth().testTag(UiTestTags.REGISTER_BIRTH_DATE),
            )
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        component.onBirthDateChanged(millis.toLocalDate())
                    }
                    showDatePicker = false
                }) { Text(stringResource(Res.string.picker_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(Res.string.picker_cancel))
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun RegistrationError.messageText(): String = when (this) {
    RegistrationError.PHONE_INVALID -> stringResource(Res.string.error_phone_invalid)
    RegistrationError.NAME_INVALID -> stringResource(Res.string.error_name_invalid)
    RegistrationError.BIRTH_DATE_INVALID -> stringResource(Res.string.error_birth_invalid)
}

private fun LocalDate.formatRu(): String =
    "${dayOfMonth.pad()}.${monthNumber.pad()}.$year"

private fun Int.pad(): String = toString().padStart(2, '0')

private fun Long.toLocalDate(): LocalDate =
    Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.UTC).date
