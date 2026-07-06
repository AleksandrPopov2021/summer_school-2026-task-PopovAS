package ru.vertical.climbing

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.arkivanov.decompose.defaultComponentContext
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import ru.vertical.climbing.app.App
import ru.vertical.climbing.app.navigation.DefaultRootComponent
import ru.vertical.climbing.domain.usecase.PushRegistrationCoordinator
import ru.vertical.climbing.push.PushDeliverySource
import ru.vertical.climbing.push.PushNotificationCenter
import ru.vertical.climbing.push.toPushNavigationEvent

class MainActivity : ComponentActivity() {

    private val pushRegistration: PushRegistrationCoordinator by inject()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) registerPushInBackground()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermission()

        val root = DefaultRootComponent(
            componentContext = defaultComponentContext(),
            initialPushEvent = intent?.toPushNavigationEvent(),
        )

        setContent { App(root) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deliverIntentEvent(intent)
    }

    private fun deliverIntentEvent(intent: Intent?) {
        intent?.toPushNavigationEvent()?.let { event ->
            PushNotificationCenter.publish(
                event.copy(source = PushDeliverySource.NOTIFICATION_TAP, showPreview = true),
            )
        }
    }

    private fun requestNotificationPermission() {
        if (isRunningInstrumentedTest()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED -> registerPushInBackground()
                else -> notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            registerPushInBackground()
        }
    }

    private fun registerPushInBackground() {
        lifecycleScope.launch {
            pushRegistration.registerIfNeeded()
        }
    }

    private fun isRunningInstrumentedTest(): Boolean = runCatching {
        Class.forName("androidx.test.platform.app.InstrumentationRegistry")
            .getMethod("getInstrumentation")
            .invoke(null) != null
    }.getOrDefault(false)
}
