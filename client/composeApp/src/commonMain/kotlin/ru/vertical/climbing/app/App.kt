package ru.vertical.climbing.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import ru.vertical.climbing.app.navigation.RootComponent
import ru.vertical.climbing.app.theme.VerticalTheme
import ru.vertical.climbing.app.ui.screens.MainScaffold
import ru.vertical.climbing.app.ui.screens.RegisterScreen
import ru.vertical.climbing.app.ui.screens.SplashScreen

/** Корневой Compose-контент приложения. */
@Composable
fun App(root: RootComponent) {
    VerticalTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Children(
                stack = root.stack,
                animation = stackAnimation(fade()),
            ) { child ->
                when (val instance = child.instance) {
                    is RootComponent.Child.Splash -> SplashScreen(instance.component)
                    is RootComponent.Child.Registration -> RegisterScreen(instance.component)
                    is RootComponent.Child.Main -> MainScaffold(instance.component)
                }
            }
        }
    }
}
