package ru.vertical.climbing.app.navigation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import ru.vertical.climbing.domain.model.PushNavigationTarget
import ru.vertical.climbing.domain.model.PushNotificationPayload
import ru.vertical.climbing.domain.model.directPushNavigationTarget
import ru.vertical.climbing.domain.model.parseDeepLink
import ru.vertical.climbing.domain.model.primaryPushNavigationTarget
import ru.vertical.climbing.domain.navigation.PendingNavigationStore
import ru.vertical.climbing.domain.usecase.PushRegistrationCoordinator
import ru.vertical.climbing.push.PushNavigationEvent
import ru.vertical.climbing.push.PushNotificationCenter

/** Вкладки нижней навигации (Shell). */
enum class MainTab {
    SCHEDULE,
    BOOKINGS,
    PROFILE,
}

/**
 * Main flow с нижней навигацией: Расписание | Мои записи | Профиль.
 * Push overlay (SCR-014) и маршрутизация LOGIC-013.
 */
/** Обёртка для опционального push-overlay: Decompose [Value] требует non-null тип. */
data class PushPreviewState(val component: PushNotificationComponent?)

interface MainComponent {
    val selectedTab: Value<MainTab>
    val schedule: ScheduleComponent
    val bookings: BookingsComponent
    val profile: ProfileComponent
    val pushPreview: Value<PushPreviewState>

    fun onTabSelected(tab: MainTab)
    fun onSignOut()
    fun handlePushEvent(event: PushNavigationEvent)
    fun navigateToTarget(target: PushNavigationTarget)
    fun showPushPreview(payload: PushNotificationPayload)
    fun dismissPushPreview()
}

class DefaultMainComponent(
    componentContext: ComponentContext,
    private val onSignOutRequested: () -> Unit,
    private val isAuthenticated: () -> Boolean = { true },
) : MainComponent, ComponentContext by componentContext, KoinComponent {

    private val scope = componentScope()
    private val pushRegistration: PushRegistrationCoordinator = get()

    private val _selectedTab = MutableValue(MainTab.SCHEDULE)
    override val selectedTab: Value<MainTab> = _selectedTab

    private val _pushPreview = MutableValue(PushPreviewState(null))
    override val pushPreview: Value<PushPreviewState> = _pushPreview

    override val bookings: BookingsComponent = DefaultBookingsComponent(
        componentContext = childContext(key = "bookings"),
        onGoToSchedule = { _selectedTab.value = MainTab.SCHEDULE },
        onBookingCreated = {
            _selectedTab.value = MainTab.BOOKINGS
            bookings.refreshList()
        },
    )

    override val schedule: ScheduleComponent = DefaultScheduleComponent(
        componentContext = childContext(key = "schedule"),
        onBookingCreated = {
            _selectedTab.value = MainTab.BOOKINGS
            bookings.refreshList()
        },
    )

    override val profile: ProfileComponent = DefaultProfileComponent(
        componentContext = childContext(key = "profile"),
        onSignOutRequested = onSignOutRequested,
    )

    private var pushEventsJob: Job? = null

    init {
        scope.launch { pushRegistration.registerIfNeeded() }
        pushRegistration.bindTokenRefresh { token ->
            scope.launch { pushRegistration.onTokenRefreshed(token) }
        }
        subscribePushEvents()
        consumeColdStartAndPending()
    }

    override fun onTabSelected(tab: MainTab) {
        _selectedTab.value = tab
    }

    override fun onSignOut() = onSignOutRequested()

    override fun handlePushEvent(event: PushNavigationEvent) {
        if (!isAuthenticated()) {
            event.toNavigationTarget()?.let { PendingNavigationStore.save(it) }
            return
        }
        val payload = event.payload
        val deepLink = event.deepLink
        when {
            event.showPreview && payload != null -> showPushPreview(payload)
            payload != null -> directPushNavigationTarget(payload)?.let { navigateToTarget(it) }
            deepLink != null -> parseDeepLink(deepLink)?.let { navigateToTarget(it) }
        }
    }

    override fun showPushPreview(payload: PushNotificationPayload) {
        _pushPreview.value = PushPreviewState(
            DefaultPushNotificationComponent(
                componentContext = childContext(key = "push_${payload.bookingId ?: payload.type}"),
                payload = payload,
                onNavigate = { resolved ->
                    dismissPushPreview()
                    primaryPushNavigationTarget(resolved)?.let { navigateToTarget(it) }
                },
                onDismissRequested = { dismissPushPreview() },
            ),
        )
    }

    override fun dismissPushPreview() {
        _pushPreview.value = PushPreviewState(null)
    }

    override fun navigateToTarget(target: PushNavigationTarget) {
        dismissPushPreview()
        when (target) {
            PushNavigationTarget.Schedule -> _selectedTab.value = MainTab.SCHEDULE
            is PushNavigationTarget.ScheduleDate -> _selectedTab.value = MainTab.SCHEDULE
            PushNavigationTarget.BookingsList -> _selectedTab.value = MainTab.BOOKINGS
            is PushNavigationTarget.BookingDetail -> {
                _selectedTab.value = MainTab.BOOKINGS
                bookings.openBookingDetail(target.bookingId)
            }
            is PushNavigationTarget.AlternativeSlot -> {
                _selectedTab.value = MainTab.BOOKINGS
                bookings.openAlternative(target.bookingId, target.cancelledSlotId)
            }
            is PushNavigationTarget.RatingStub -> {
                _selectedTab.value = MainTab.BOOKINGS
                bookings.openRating(target.bookingId)
            }
        }
    }

    private fun subscribePushEvents() {
        pushEventsJob?.cancel()
        pushEventsJob = scope.launch {
            PushNotificationCenter.events.collectLatest { event ->
                handlePushEvent(event)
            }
        }
    }

    private fun consumeColdStartAndPending() {
        PushNotificationCenter.coldStartEvent?.let { event ->
            PushNotificationCenter.setColdStart(null)
            handlePushEvent(event)
        }
        PendingNavigationStore.consume()?.let { navigateToTarget(it) }
    }
}

private fun PushNavigationEvent.toNavigationTarget(): PushNavigationTarget? =
    payload?.let { directPushNavigationTarget(it) }
        ?: deepLink?.let { parseDeepLink(it) }
