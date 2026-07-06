package ru.vertical.climbing.app.navigation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.popWhile
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.value.Value
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import ru.vertical.climbing.domain.repository.SlotRepository
import ru.vertical.climbing.domain.usecase.GetSlotDetailUseCase
import ru.vertical.climbing.domain.usecase.LoadScheduleUseCase

/**
 * Навигационный стек вкладки «Расписание»:
 * SCR-003 (список) → SCR-004 (детали) → SCR-005 (запись) → SCR-015 (согласие).
 */
interface ScheduleComponent {
    val stack: Value<ChildStack<*, Child>>

    sealed interface Child {
        class List(val component: ScheduleListComponent) : Child
        class Detail(val component: SlotDetailComponent) : Child
        class Booking(val component: BookingComponent) : Child
        class Consent(val component: ConsentComponent) : Child
    }
}

class DefaultScheduleComponent(
    componentContext: ComponentContext,
    private val onBookingCreated: () -> Unit = {},
) : ScheduleComponent, ComponentContext by componentContext, KoinComponent {

    private val navigation = StackNavigation<Config>()

    override val stack: Value<ChildStack<*, ScheduleComponent.Child>> =
        childStack(
            source = navigation,
            serializer = null,
            initialConfiguration = Config.List,
            handleBackButton = true,
            childFactory = ::createChild,
        )

    private fun createChild(config: Config, context: ComponentContext): ScheduleComponent.Child = when (config) {
        Config.List -> ScheduleComponent.Child.List(
            DefaultScheduleListComponent(
                componentContext = context,
                loadSchedule = get<LoadScheduleUseCase>(),
                getRebookingForbiddenSlotIds = get(),
                cachedSlots = get<SlotRepository>()::cachedSlots,
                onSlotSelected = { navigation.push(Config.Detail(it)) },
            ),
        )
        is Config.Detail -> ScheduleComponent.Child.Detail(
            DefaultSlotDetailComponent(
                componentContext = context,
                slotId = config.slotId,
                getSlotDetail = get<GetSlotDetailUseCase>(),
                getRebookingForbiddenSlotIds = get(),
                cachedSlots = get<SlotRepository>()::cachedSlots,
                onBackRequested = { navigation.pop() },
                onBookRequested = { navigation.push(Config.Booking(it)) },
            ),
        )
        is Config.Booking -> ScheduleComponent.Child.Booking(
            DefaultBookingComponent(
                componentContext = context,
                slotId = config.slotId,
                autoSubmit = config.autoSubmit,
                getSlotDetail = get(),
                getBookingDetail = get(),
                listEquipmentTypes = get(),
                getRentalAvailability = get(),
                getProfile = get(),
                loadDraft = get(),
                saveDraft = get(),
                calculatePrice = get(),
                createBooking = get(),
                updateBookingRental = get(),
                slotRepository = get(),
                onBackRequested = { navigation.pop() },
                onConsentRequired = { navigation.push(Config.Consent(it)) },
                onBookingCreated = {
                    navigation.replaceAll(Config.List)
                    onBookingCreated()
                },
                onConflictNavigateToDetail = {
                    navigation.popWhile { it !is Config.Detail }
                },
            ),
        )
        is Config.Consent -> ScheduleComponent.Child.Consent(
            DefaultConsentComponent(
                componentContext = context,
                slotId = config.slotId,
                acceptRiskConsent = get(),
                onConsentAccepted = { slotId ->
                    navigation.pop()
                    navigation.pop()
                    navigation.push(Config.Booking(slotId, autoSubmit = true))
                },
                onCancelRequested = {
                    navigation.pop()
                    navigation.pop()
                },
            ),
        )
    }

    private sealed interface Config {
        data object List : Config
        data class Detail(val slotId: String) : Config
        data class Booking(val slotId: String, val autoSubmit: Boolean = false) : Config
        data class Consent(val slotId: String) : Config
    }
}
