package ru.vertical.climbing.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.vector.ImageVector
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import ru.vertical.climbing.app.navigation.BookingsComponent
import ru.vertical.climbing.app.navigation.MainComponent
import ru.vertical.climbing.app.navigation.MainTab
import ru.vertical.climbing.app.navigation.ProfileComponent
import ru.vertical.climbing.app.navigation.ScheduleComponent
import ru.vertical.climbing.app.ui.PushForegroundBannerHost
import ru.vertical.climbing.app.ui.UiTestTags
import ru.vertical.climbing.resources.Res
import ru.vertical.climbing.resources.tab_bookings
import ru.vertical.climbing.resources.tab_profile
import ru.vertical.climbing.resources.tab_schedule

private data class TabItem(
    val tab: MainTab,
    val icon: ImageVector,
    val label: StringResource,
)

private val tabs = listOf(
    TabItem(MainTab.SCHEDULE, Icons.Filled.CalendarMonth, Res.string.tab_schedule),
    TabItem(MainTab.BOOKINGS, Icons.Filled.ConfirmationNumber, Res.string.tab_bookings),
    TabItem(MainTab.PROFILE, Icons.Filled.Person, Res.string.tab_profile),
)

/** Main flow: Scaffold + нижняя навигация с тремя вкладками (Shell UI). */
@Composable
fun MainScaffold(component: MainComponent) {
    val selected by component.selectedTab.subscribeAsState()
    val pushPreview by component.pushPreview.subscribeAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    tabs.forEach { item ->
                        NavigationBarItem(
                            selected = selected == item.tab,
                            onClick = { component.onTabSelected(item.tab) },
                            icon = { Icon(item.icon, contentDescription = null) },
                            label = { Text(stringResource(item.label)) },
                            modifier = Modifier.testTag(
                                when (item.tab) {
                                    MainTab.SCHEDULE -> UiTestTags.TAB_SCHEDULE
                                    MainTab.BOOKINGS -> UiTestTags.TAB_BOOKINGS
                                    MainTab.PROFILE -> UiTestTags.TAB_PROFILE
                                },
                            ),
                        )
                    }
                }
            },
        ) { innerPadding ->
            Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                PushForegroundBannerHost(onBannerTapped = component::showPushPreview)
                Box(modifier = Modifier.fillMaxSize()) {
                    when (selected) {
                        MainTab.SCHEDULE -> ScheduleFlow(component.schedule)
                        MainTab.BOOKINGS -> BookingsFlow(component.bookings)
                        MainTab.PROFILE -> ProfileFlow(component.profile)
                    }
                }
            }
        }

        pushPreview.component?.let { preview ->
            PushNotificationScreen(preview)
        }
    }
}

/** Навигационный стек вкладки «Мои записи»: SCR-006 → SCR-007 → SCR-008. */
@Composable
private fun BookingsFlow(component: BookingsComponent) {
    Children(stack = component.stack, animation = stackAnimation(fade())) { child ->
        when (val instance = child.instance) {
            is BookingsComponent.Child.List -> MyBookingsScreen(instance.component)
            is BookingsComponent.Child.Detail -> BookingDetailScreen(instance.component)
            is BookingsComponent.Child.Cancel -> CancellationConfirmScreen(instance.component)
            is BookingsComponent.Child.EditRental -> BookingScreen(instance.component)
            is BookingsComponent.Child.Alternative -> AlternativeSlotScreen(instance.component)
            is BookingsComponent.Child.Book -> BookingScreen(instance.component)
            is BookingsComponent.Child.Rating -> RatingScreen(instance.component)
        }
    }
}

/** Навигационный стек вкладки «Профиль»: SCR-010 → SCR-011. */
@Composable
private fun ProfileFlow(component: ProfileComponent) {
    Children(stack = component.stack, animation = stackAnimation(fade())) { child ->
        when (val instance = child.instance) {
            is ProfileComponent.Child.Profile -> ProfileScreen(instance.component)
            is ProfileComponent.Child.NotificationSettings -> NotificationSettingsScreen(instance.component)
        }
    }
}

/** Навигационный стек вкладки «Расписание»: SCR-003 → SCR-004 → SCR-005 → SCR-015. */
@Composable
private fun ScheduleFlow(component: ScheduleComponent) {
    Children(stack = component.stack, animation = stackAnimation(fade())) { child ->
        when (val instance = child.instance) {
            is ScheduleComponent.Child.List -> ScheduleScreen(instance.component)
            is ScheduleComponent.Child.Detail -> SlotDetailScreen(instance.component)
            is ScheduleComponent.Child.Booking -> BookingScreen(instance.component)
            is ScheduleComponent.Child.Consent -> ConsentScreen(instance.component)
        }
    }
}
