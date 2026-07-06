package ru.vertical.climbing.app.navigation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.value.Value
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

/**
 * Навигационный стек вкладки «Мои записи»:
 * SCR-006 → SCR-007 → SCR-008 / SCR-005 (прокат) / SCR-009 (альтернатива) / SCR-012 (оценка).
 */
interface BookingsComponent {
    val stack: Value<ChildStack<*, Child>>

    fun refreshList()
    fun openAlternative(bookingId: String, cancelledSlotId: String)
    fun openBookingDetail(bookingId: String, openAlternative: Boolean = false)
    fun openRating(bookingId: String)

    sealed interface Child {
        class List(val component: MyBookingsListComponent) : Child
        class Detail(val component: BookingDetailComponent) : Child
        class Cancel(val component: CancellationConfirmComponent) : Child
        class EditRental(val component: BookingComponent) : Child
        class Alternative(val component: AlternativeSlotComponent) : Child
        class Book(val component: BookingComponent) : Child
        class Rating(val component: RatingComponent) : Child
    }
}

class DefaultBookingsComponent(
    componentContext: ComponentContext,
    private val onGoToSchedule: () -> Unit = {},
    private val onBookingCreated: () -> Unit = {},
) : BookingsComponent, ComponentContext by componentContext, KoinComponent {

    private val navigation = StackNavigation<Config>()
    private var listComponent: DefaultMyBookingsListComponent? = null
    private var detailComponent: DefaultBookingDetailComponent? = null

    override val stack: Value<ChildStack<*, BookingsComponent.Child>> =
        childStack(
            source = navigation,
            serializer = null,
            initialConfiguration = Config.List,
            handleBackButton = true,
            childFactory = ::createChild,
        )

    override fun refreshList() {
        listComponent?.reload()
    }

    override fun openAlternative(bookingId: String, cancelledSlotId: String) {
        navigation.push(Config.Alternative(bookingId, cancelledSlotId))
    }

    override fun openBookingDetail(bookingId: String, openAlternative: Boolean) {
        navigation.push(Config.Detail(bookingId, openAlternative))
    }

    override fun openRating(bookingId: String) {
        navigation.push(Config.Rating(bookingId))
    }

    private fun createChild(config: Config, context: ComponentContext): BookingsComponent.Child = when (config) {
        Config.List -> {
            val component = DefaultMyBookingsListComponent(
                componentContext = context,
                listBookings = get(),
                listRateableBookings = get(),
                cachedBookings = get<ru.vertical.climbing.domain.repository.BookingRepository>()::cachedBookings,
                onBookingSelected = { navigation.push(Config.Detail(it)) },
                onRateBookingRequested = { navigation.push(Config.Rating(it)) },
                onGoToScheduleRequested = onGoToSchedule,
            )
            listComponent = component
            BookingsComponent.Child.List(component)
        }
        is Config.Detail -> {
            val component = DefaultBookingDetailComponent(
                componentContext = context,
                bookingId = config.bookingId,
                openAlternativeOnLoad = config.openAlternative,
                getBookingDetail = get(),
                cachedBookings = get<ru.vertical.climbing.domain.repository.BookingRepository>()::cachedBookings,
                onBackRequested = { navigation.pop() },
                onCancelRequested = { navigation.push(Config.Cancel(it)) },
                onModifyRentalRequested = { bookingId, slotId ->
                    navigation.push(Config.EditRental(bookingId, slotId))
                },
                onFindAlternativeRequested = { bookingId, cancelledSlotId ->
                    navigation.push(Config.Alternative(bookingId, cancelledSlotId))
                },
            )
            detailComponent = component
            BookingsComponent.Child.Detail(component)
        }
        is Config.Cancel -> BookingsComponent.Child.Cancel(
            DefaultCancellationConfirmComponent(
                componentContext = context,
                bookingId = config.bookingId,
                getBookingDetail = get(),
                cancelBooking = get(),
                onCancelled = {
                    navigation.replaceAll(Config.List)
                    listComponent?.reload()
                },
                onDismissRequested = { navigation.pop() },
            ),
        )
        is Config.EditRental -> BookingsComponent.Child.EditRental(
            createBookingComponent(
                context = context,
                slotId = config.slotId,
                bookingId = config.bookingId,
                onBack = { navigation.pop() },
                onRentalUpdated = {
                    navigation.pop()
                    detailComponent?.onRentalUpdated()
                },
            ),
        )
        is Config.Alternative -> BookingsComponent.Child.Alternative(
            DefaultAlternativeSlotComponent(
                componentContext = context,
                bookingId = config.bookingId,
                cancelledSlotId = config.cancelledSlotId,
                getBookingDetail = get(),
                findAlternative = get(),
                prefillDraft = get(),
                onBackRequested = { navigation.pop() },
                onBookAlternativeRequested = { slotId ->
                    navigation.push(Config.Book(slotId))
                },
                onChooseScheduleRequested = onGoToSchedule,
                onLaterRequested = {
                    navigation.replaceAll(Config.List)
                },
                onNotFoundNavigateToList = {
                    navigation.replaceAll(Config.List)
                    listComponent?.reload()
                },
            ),
        )
        is Config.Book -> BookingsComponent.Child.Book(
            createBookingComponent(
                context = context,
                slotId = config.slotId,
                onBack = { navigation.pop() },
                onBookingCreated = {
                    navigation.replaceAll(Config.List)
                    listComponent?.reload()
                    onBookingCreated()
                },
            ),
        )
        is Config.Rating -> BookingsComponent.Child.Rating(
            DefaultRatingComponent(
                componentContext = context,
                bookingId = config.bookingId,
                loadRatingContext = get(),
                submitRating = get(),
                onFinished = {
                    navigation.replaceAll(Config.List)
                    listComponent?.reload()
                },
                onBackRequested = { navigation.pop() },
            ),
        )
    }

    private fun createBookingComponent(
        context: ComponentContext,
        slotId: String,
        bookingId: String? = null,
        onBack: () -> Unit,
        onRentalUpdated: () -> Unit = {},
        onBookingCreated: () -> Unit = {},
    ) = DefaultBookingComponent(
        componentContext = context,
        slotId = slotId,
        bookingId = bookingId,
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
        onBackRequested = onBack,
        onRentalUpdated = onRentalUpdated,
        onRentalUpdateForbidden = { error ->
            navigation.pop()
            detailComponent?.showSnackbarError(error)
        },
        onBookingNotFound = {
            navigation.replaceAll(Config.List)
            listComponent?.reload()
        },
        onBookingCreated = onBookingCreated,
    )

    private sealed interface Config {
        data object List : Config
        data class Detail(val bookingId: String, val openAlternative: Boolean = false) : Config
        data class Cancel(val bookingId: String) : Config
        data class EditRental(val bookingId: String, val slotId: String) : Config
        data class Alternative(val bookingId: String, val cancelledSlotId: String) : Config
        data class Book(val slotId: String) : Config
        data class Rating(val bookingId: String) : Config
    }
}
