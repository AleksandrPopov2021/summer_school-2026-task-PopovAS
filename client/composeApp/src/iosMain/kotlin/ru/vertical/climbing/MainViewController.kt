package ru.vertical.climbing

import androidx.compose.ui.window.ComposeUIViewController
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext
import platform.UIKit.UIViewController
import ru.vertical.climbing.app.App
import ru.vertical.climbing.app.di.initKoin
import ru.vertical.climbing.app.navigation.DefaultRootComponent
import ru.vertical.climbing.domain.usecase.PushRegistrationCoordinator
import ru.vertical.climbing.push.IosPushBridge
import ru.vertical.climbing.push.IosPushTokenProvider
import ru.vertical.climbing.push.PushTokenProvider

/** Точка входа для Swift: настройка DI. Вызывать один раз при старте приложения. */
fun initKoinIos() {
    initKoin()
    wireIosPushBridge()
}

private fun wireIosPushBridge() {
    val koin = GlobalContext.get()
    val provider = koin.get<PushTokenProvider>() as? IosPushTokenProvider ?: return
    val coordinator = koin.get<PushRegistrationCoordinator>()
    IosPushBridge.tokenUpdateHandler = { token ->
        provider.updateApnsToken(token)
        MainScope().launch { coordinator.onTokenRefreshed(token) }
    }
    MainScope().launch { coordinator.registerIfNeeded() }
}

/** Точка входа для Swift: корневой UIViewController с Compose-контентом. */
fun MainViewController(): UIViewController {
    val lifecycle = LifecycleRegistry()
    val root = DefaultRootComponent(componentContext = DefaultComponentContext(lifecycle = lifecycle))
    lifecycle.resume()
    return ComposeUIViewController { App(root) }
}
