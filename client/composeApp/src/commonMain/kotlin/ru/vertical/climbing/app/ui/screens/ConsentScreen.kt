@file:OptIn(ExperimentalMaterial3Api::class)

package ru.vertical.climbing.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.jetbrains.compose.resources.stringResource
import ru.vertical.climbing.app.navigation.ConsentComponent
import ru.vertical.climbing.app.ui.UiTestTags
import ru.vertical.climbing.app.ui.errorMessageFor
import ru.vertical.climbing.resources.Res
import ru.vertical.climbing.resources.action_cancel
import ru.vertical.climbing.resources.action_confirm
import ru.vertical.climbing.resources.consent_checkbox
import ru.vertical.climbing.resources.consent_text
import ru.vertical.climbing.resources.consent_title

/** SCR-015 Consent Screen — согласие на риск (LOGIC-006, FR-012). */
@Composable
fun ConsentScreen(component: ConsentComponent) {
    val model by component.model.subscribeAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    model.error?.let { error ->
        val errorMessage = errorMessageFor(error)
        LaunchedEffect(error) {
            snackbarHostState.showSnackbar(errorMessage)
            component.onErrorConsumed()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.consent_title)) },
                navigationIcon = {
                    IconButton(onClick = component::onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = component::onConfirm,
                    enabled = model.canConfirm,
                    modifier = Modifier.fillMaxWidth().testTag(UiTestTags.CONSENT_CONFIRM),
                ) {
                    if (model.isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                    }
                    Text(stringResource(Res.string.action_confirm))
                }
                TextButton(onClick = component::onCancel, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text(stringResource(Res.string.action_cancel))
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(
                text = stringResource(Res.string.consent_text),
                style = MaterialTheme.typography.bodyLarge,
            )
            RowWithCheckbox(
                checked = model.consentChecked,
                onCheckedChange = component::onConsentChecked,
                label = stringResource(Res.string.consent_checkbox),
            )
        }
    }
}

@Composable
private fun RowWithCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(UiTestTags.CONSENT_CHECKBOX),
        )
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}
