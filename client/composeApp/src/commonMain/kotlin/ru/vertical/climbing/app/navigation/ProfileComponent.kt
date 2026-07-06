package ru.vertical.climbing.app.navigation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.value.Value
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

/**
 * Навигационный стек вкладки «Профиль»:
 * SCR-010 → SCR-011.
 */
interface ProfileComponent {
    val stack: Value<ChildStack<*, Child>>

    sealed interface Child {
        class Profile(val component: ProfileScreenComponent) : Child
        class NotificationSettings(val component: NotificationSettingsComponent) : Child
    }
}

class DefaultProfileComponent(
    componentContext: ComponentContext,
    private val onSignOutRequested: () -> Unit,
) : ProfileComponent, ComponentContext by componentContext, KoinComponent {

    private val navigation = StackNavigation<Config>()

    override val stack: Value<ChildStack<*, ProfileComponent.Child>> =
        childStack(
            source = navigation,
            serializer = null,
            initialConfiguration = Config.Profile,
            handleBackButton = true,
            childFactory = ::createChild,
        )

    private fun createChild(config: Config, context: ComponentContext): ProfileComponent.Child = when (config) {
        Config.Profile -> ProfileComponent.Child.Profile(
            DefaultProfileScreenComponent(
                componentContext = context,
                loadProfile = get(),
                signOut = get(),
                apiEnvStore = get(),
                onNotificationSettingsRequested = { navigation.push(Config.NotificationSettings) },
                onSignedOut = onSignOutRequested,
                onSessionExpired = onSignOutRequested,
            ),
        )
        Config.NotificationSettings -> ProfileComponent.Child.NotificationSettings(
            DefaultNotificationSettingsComponent(
                componentContext = context,
                getPreferences = get(),
                updatePreferences = get(),
                signOut = get(),
                onBackRequested = { navigation.pop() },
                onSessionExpired = onSignOutRequested,
            ),
        )
    }

    private sealed interface Config {
        data object Profile : Config
        data object NotificationSettings : Config
    }
}
