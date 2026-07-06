package ru.vertical.climbing.app.navigation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.value.Value
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import ru.vertical.climbing.domain.model.parseDeepLink
import ru.vertical.climbing.domain.navigation.PendingNavigationStore
import ru.vertical.climbing.domain.usecase.CheckSessionUseCase
import ru.vertical.climbing.domain.usecase.RegisterClientUseCase
import ru.vertical.climbing.push.PushNavigationEvent

/**
 * Корневой компонент навигации (Decompose): SCR-001 Splash → Auth flow (SCR-002) / Main flow (tabs).
 * Cold start push / deep link — LOGIC-013.
 */
interface RootComponent {
    val stack: Value<ChildStack<*, Child>>

    sealed interface Child {
        class Splash(val component: SplashComponent) : Child
        class Registration(val component: RegisterComponent) : Child
        class Main(val component: MainComponent) : Child
    }
}

class DefaultRootComponent(
    componentContext: ComponentContext,
    initialPushEvent: PushNavigationEvent? = null,
) : RootComponent, ComponentContext by componentContext, KoinComponent {

    private val navigation = StackNavigation<Config>()

    init {
        initialPushEvent?.toNavigationTarget()?.let { PendingNavigationStore.save(it) }
        initialPushEvent?.let { ru.vertical.climbing.push.PushNotificationCenter.setColdStart(it) }
    }

    override val stack: Value<ChildStack<*, RootComponent.Child>> =
        childStack(
            source = navigation,
            serializer = null,
            initialConfiguration = Config.Splash,
            handleBackButton = true,
            childFactory = ::createChild,
        )

    private fun createChild(config: Config, context: ComponentContext): RootComponent.Child = when (config) {
        Config.Splash -> RootComponent.Child.Splash(
            DefaultSplashComponent(
                componentContext = context,
                checkSession = get<CheckSessionUseCase>(),
                onSessionResolved = ::onSessionResolved,
            ),
        )
        Config.Registration -> RootComponent.Child.Registration(
            DefaultRegisterComponent(
                componentContext = context,
                registerClient = get<RegisterClientUseCase>(),
                onRegistered = { navigation.replaceAll(Config.Main) },
            ),
        )
        Config.Main -> RootComponent.Child.Main(
            DefaultMainComponent(
                componentContext = context,
                onSignOutRequested = {
                    PendingNavigationStore.clear()
                    navigation.replaceAll(Config.Registration)
                },
            ),
        )
    }

    private fun onSessionResolved(route: SessionRoute) {
        when (route) {
            SessionRoute.REGISTRATION -> {
                ru.vertical.climbing.push.PushNotificationCenter.coldStartEvent
                    ?.toNavigationTarget()
                    ?.let { target ->
                        PendingNavigationStore.save(target)
                        ru.vertical.climbing.push.PushNotificationCenter.setColdStart(null)
                    }
                navigation.replaceAll(Config.Registration)
            }
            SessionRoute.SCHEDULE, SessionRoute.SCHEDULE_OFFLINE -> navigation.replaceAll(Config.Main)
        }
    }

    private sealed interface Config {
        data object Splash : Config
        data object Registration : Config
        data object Main : Config
    }
}

private fun PushNavigationEvent.toNavigationTarget() =
    payload?.let { ru.vertical.climbing.domain.model.directPushNavigationTarget(it) }
        ?: deepLink?.let { parseDeepLink(it) }
