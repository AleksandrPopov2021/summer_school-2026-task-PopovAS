package ru.vertical.climbing.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.jetbrains.compose.resources.stringResource
import ru.vertical.climbing.app.navigation.SplashComponent
import ru.vertical.climbing.app.ui.ErrorView
import ru.vertical.climbing.app.ui.UiTestTags
import ru.vertical.climbing.resources.Res
import ru.vertical.climbing.resources.a11y_app_logo
import ru.vertical.climbing.resources.app_name
import ru.vertical.climbing.resources.app_tagline
import ru.vertical.climbing.resources.app_version

/** SCR-001 Splash Screen — брендинг + индикатор проверки сессии (LOGIC-001). */
@Composable
fun SplashScreen(component: SplashComponent) {
    val model by component.model.subscribeAsState()

    Box(modifier = Modifier.fillMaxSize().testTag(UiTestTags.SPLASH), contentAlignment = Alignment.Center) {
        if (model.error != null) {
            ErrorView(error = model.error!!, onRetry = component::onRetry)
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Terrain,
                    contentDescription = stringResource(Res.string.a11y_app_logo),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(120.dp),
                )
                Text(
                    text = stringResource(Res.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(Res.string.app_tagline),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (model.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.padding(top = 8.dp))
                }
            }
        }

        Text(
            text = stringResource(Res.string.app_version),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
        )
    }
}
