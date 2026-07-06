package ru.vertical.climbing.app.navigation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import ru.vertical.climbing.domain.model.BookingDraft
import ru.vertical.climbing.domain.model.CreateBookingCommand
import ru.vertical.climbing.domain.model.RentalEquipmentType
import ru.vertical.climbing.domain.model.RentalLineInput
import ru.vertical.climbing.domain.model.SlotRentalAvailability
import ru.vertical.climbing.domain.model.TrainingSlot
import ru.vertical.climbing.domain.model.UpdateRentalCommand
import ru.vertical.climbing.domain.model.hasRentalSelectionChanged
import ru.vertical.climbing.domain.repository.SlotRepository
import ru.vertical.climbing.domain.usecase.CalculateBookingPriceUseCase
import ru.vertical.climbing.domain.usecase.CreateBookingUseCase
import ru.vertical.climbing.domain.usecase.GetBookingDetailUseCase
import ru.vertical.climbing.domain.usecase.GetProfileUseCase
import ru.vertical.climbing.domain.usecase.GetSlotDetailUseCase
import ru.vertical.climbing.domain.usecase.GetSlotRentalAvailabilityUseCase
import ru.vertical.climbing.domain.usecase.ListRentalEquipmentTypesUseCase
import ru.vertical.climbing.domain.usecase.LoadBookingDraftUseCase
import ru.vertical.climbing.domain.usecase.SaveBookingDraftUseCase
import ru.vertical.climbing.domain.usecase.UpdateBookingRentalUseCase
import ru.vertical.climbing.domain.util.AppError
import ru.vertical.climbing.domain.util.AppResult
import ru.vertical.climbing.domain.util.ErrorCode
import ru.vertical.climbing.presentation.Async

/**
 * SCR-005 Booking Screen — оформление записи (LOGIC-005, LOGIC-007)
 * или изменение проката (LOGIC-010, mode = edit_rental).
 */
interface BookingComponent {
    val slotId: String
    val bookingId: String?
    val isEditMode: Boolean
    val model: Value<Model>

    fun onRetry()
    fun onBack()
    fun onOwnEquipmentToggled(value: Boolean)
    fun onRentalToggled(equipmentTypeId: String, selected: Boolean)
    fun onConfirm()
    fun onDismissSnackbar()
    fun onDismissConflictDialog()

    data class Model(
        val isEditMode: Boolean = false,
        val content: Async<Content> = Async.Loading,
        val usesOwnEquipment: Boolean = false,
        val selectedRentalIds: Set<String> = emptySet(),
        val heldRentalIds: Set<String> = emptySet(),
        val originalUsesOwnEquipment: Boolean = false,
        val originalRentalIds: Set<String> = emptySet(),
        val priceBreakdown: CalculateBookingPriceUseCase.PriceBreakdown? = null,
        val isSubmitting: Boolean = false,
        val snackbarError: AppError? = null,
        val conflictMessage: String? = null,
    ) {
        val canConfirm: Boolean
            get() {
                if (isSubmitting || content !is Async.Content) return false
                val hasSelection = usesOwnEquipment || selectedRentalIds.isNotEmpty()
                if (!hasSelection) return false
                if (isEditMode) {
                    val changed = hasRentalSelectionChanged(
                        originalUsesOwn = originalUsesOwnEquipment,
                        originalLines = originalRentalIds.map { RentalLineInput(it, 1) },
                        newUsesOwn = usesOwnEquipment,
                        newLines = selectedRentalIds.map { RentalLineInput(it, 1) },
                    )
                    return changed
                }
                return true
            }
    }

    data class Content(
        val slot: TrainingSlot,
        val equipmentTypes: List<RentalEquipmentType>,
        val rentalAvailability: List<SlotRentalAvailability>,
        val client: ru.vertical.climbing.domain.model.Client,
    )
}

class DefaultBookingComponent(
    componentContext: ComponentContext,
    override val slotId: String,
    override val bookingId: String? = null,
    private val autoSubmit: Boolean = false,
    private val getSlotDetail: GetSlotDetailUseCase,
    private val getBookingDetail: GetBookingDetailUseCase,
    private val listEquipmentTypes: ListRentalEquipmentTypesUseCase,
    private val getRentalAvailability: GetSlotRentalAvailabilityUseCase,
    private val getProfile: GetProfileUseCase,
    private val loadDraft: LoadBookingDraftUseCase,
    private val saveDraft: SaveBookingDraftUseCase,
    private val calculatePrice: CalculateBookingPriceUseCase,
    private val createBooking: CreateBookingUseCase,
    private val updateBookingRental: UpdateBookingRentalUseCase,
    private val slotRepository: SlotRepository,
    private val onBackRequested: () -> Unit,
    private val onConsentRequired: (String) -> Unit = {},
    private val onBookingCreated: () -> Unit = {},
    private val onConflictNavigateToDetail: () -> Unit = {},
    private val onRentalUpdated: () -> Unit = {},
    private val onRentalUpdateForbidden: (AppError) -> Unit = {},
    private val onBookingNotFound: () -> Unit = {},
) : BookingComponent, ComponentContext by componentContext {

    override val isEditMode: Boolean = bookingId != null

    private val scope = componentScope()

    private val _model = MutableValue(BookingComponent.Model(isEditMode = isEditMode))
    override val model: Value<BookingComponent.Model> = _model

    init {
        load()
    }

    override fun onRetry() = load()

    override fun onBack() = onBackRequested()

    override fun onDismissSnackbar() {
        _model.value = _model.value.copy(snackbarError = null)
    }

    override fun onDismissConflictDialog() {
        _model.value = _model.value.copy(conflictMessage = null)
        onConflictNavigateToDetail()
    }

    override fun onOwnEquipmentToggled(value: Boolean) {
        updateSelection(usesOwnEquipment = value)
    }

    override fun onRentalToggled(equipmentTypeId: String, selected: Boolean) {
        val ids = _model.value.selectedRentalIds.toMutableSet()
        if (selected) ids.add(equipmentTypeId) else ids.remove(equipmentTypeId)
        updateSelection(selectedRentalIds = ids)
    }

    override fun onConfirm() {
        val current = _model.value
        if (!current.canConfirm || current.content !is Async.Content) return

        if (isEditMode) {
            submitRentalUpdate()
            return
        }

        val client = current.content.data.client
        if (!client.riskConsentAccepted) {
            persistDraft(current)
            onConsentRequired(slotId)
            return
        }
        submitCreate()
    }

    private fun load() {
        _model.value = _model.value.copy(content = Async.Loading, snackbarError = null)
        scope.launch {
            val result = coroutineScope {
                val slotDeferred = async { getSlotDetail(slotId) }
                val equipmentDeferred = async { listEquipmentTypes() }
                val rentalDeferred = async { getRentalAvailability(slotId) }
                val profileDeferred = async { getProfile() }
                val bookingDeferred = bookingId?.let { async { getBookingDetail(it) } }
                val draft = if (!isEditMode) loadDraft() else null

                val slotResult = slotDeferred.await()
                val equipmentResult = equipmentDeferred.await()
                val rentalResult = rentalDeferred.await()
                val profileResult = profileDeferred.await()
                val bookingResult = bookingDeferred?.await()

                when {
                    slotResult is AppResult.Failure -> slotResult
                    equipmentResult is AppResult.Failure -> equipmentResult
                    rentalResult is AppResult.Failure -> rentalResult
                    profileResult is AppResult.Failure -> profileResult
                    bookingResult is AppResult.Failure -> bookingResult
                    else -> {
                        val booking = (bookingResult as? AppResult.Success)?.value
                        val content = BookingComponent.Content(
                            slot = (slotResult as AppResult.Success).value,
                            equipmentTypes = (equipmentResult as AppResult.Success).value,
                            rentalAvailability = (rentalResult as AppResult.Success).value,
                            client = (profileResult as AppResult.Success).value,
                        )
                        val (usesOwn, rentalIds, heldIds) = when {
                            booking != null -> {
                                val ids = booking.rentalLines.map { it.equipmentTypeId }.toSet()
                                Triple(booking.usesOwnEquipment, ids, ids)
                            }
                            draft?.slotId == slotId -> {
                                val ids = draft.rentalLines.map { it.equipmentTypeId }.toSet()
                                Triple(draft.usesOwnEquipment, ids, emptySet<String>())
                            }
                            else -> Triple(false, emptySet<String>(), emptySet<String>())
                        }
                        AppResult.Success(
                            LoadResult(
                                content = content,
                                usesOwnEquipment = usesOwn,
                                selectedRentalIds = rentalIds,
                                heldRentalIds = heldIds,
                                originalUsesOwnEquipment = usesOwn,
                                originalRentalIds = rentalIds,
                            ),
                        )
                    }
                }
            }

            when (result) {
                is AppResult.Success -> {
                    val data = result.value
                    _model.value = _model.value.copy(
                        content = Async.Content(data.content),
                        usesOwnEquipment = data.usesOwnEquipment,
                        selectedRentalIds = data.selectedRentalIds,
                        heldRentalIds = data.heldRentalIds,
                        originalUsesOwnEquipment = data.originalUsesOwnEquipment,
                        originalRentalIds = data.originalRentalIds,
                    )
                    recalculatePrice()
                    if (!isEditMode && autoSubmit && data.content.client.riskConsentAccepted) {
                        submitCreate()
                    }
                }
                is AppResult.Failure -> _model.value = _model.value.copy(content = Async.Error(result.error))
            }
        }
    }

    private fun updateSelection(
        usesOwnEquipment: Boolean = _model.value.usesOwnEquipment,
        selectedRentalIds: Set<String> = _model.value.selectedRentalIds,
    ) {
        _model.value = _model.value.copy(
            usesOwnEquipment = usesOwnEquipment,
            selectedRentalIds = selectedRentalIds,
        )
        recalculatePrice()
        if (!isEditMode) {
            persistDraft(_model.value)
        }
    }

    private fun recalculatePrice() {
        val current = _model.value
        val content = current.content as? Async.Content ?: return
        val breakdown = calculatePrice(
            trainingPrice = content.data.slot.trainingPrice,
            rentalLines = current.selectedRentalIds.map { RentalLineInput(it, 1) },
            equipmentTypes = content.data.equipmentTypes,
            loyaltyDiscount = content.data.client.loyaltyDiscount,
        )
        _model.value = current.copy(priceBreakdown = breakdown)
    }

    private fun persistDraft(model: BookingComponent.Model) {
        scope.launch {
            saveDraft(
                BookingDraft(
                    slotId = slotId,
                    usesOwnEquipment = model.usesOwnEquipment,
                    rentalLines = model.selectedRentalIds.map { RentalLineInput(it, 1) },
                ),
            )
        }
    }

    private fun submitCreate() {
        val current = _model.value
        if (current.isSubmitting || current.content !is Async.Content) return

        val command = CreateBookingCommand(
            slotId = slotId,
            usesOwnEquipment = current.usesOwnEquipment,
            rentalLines = current.selectedRentalIds.map { RentalLineInput(it, 1) },
        )

        _model.value = current.copy(isSubmitting = true, snackbarError = null)
        scope.launch {
            when (val result = createBooking(command)) {
                is AppResult.Success -> {
                    _model.value = _model.value.copy(isSubmitting = false)
                    onBookingCreated()
                }
                is AppResult.Failure -> handleCreateFailure(result.error)
            }
        }
    }

    private fun submitRentalUpdate() {
        val current = _model.value
        val id = bookingId ?: return
        if (current.isSubmitting || current.content !is Async.Content) return

        val command = UpdateRentalCommand(
            usesOwnEquipment = current.usesOwnEquipment,
            rentalLines = current.selectedRentalIds.map { RentalLineInput(it, 1) },
        )

        _model.value = current.copy(isSubmitting = true, snackbarError = null)
        scope.launch {
            when (val result = updateBookingRental(id, command)) {
                is AppResult.Success -> {
                    _model.value = _model.value.copy(isSubmitting = false)
                    onRentalUpdated()
                }
                is AppResult.Failure -> handleRentalUpdateFailure(result.error)
            }
        }
    }

    private suspend fun handleCreateFailure(error: AppError) {
        _model.value = _model.value.copy(isSubmitting = false)
        when (error.code) {
            ErrorCode.RISK_CONSENT_REQUIRED -> {
                persistDraft(_model.value)
                onConsentRequired(slotId)
            }
            ErrorCode.BOOKING_CONFLICT -> {
                error.conflictSlot?.let { slotRepository.updateCachedSlot(it) }
                _model.value = _model.value.copy(conflictMessage = error.message)
            }
            ErrorCode.INSTRUCTOR_CLEARANCE_REQUIRED -> {
                _model.value = _model.value.copy(snackbarError = error)
                onConflictNavigateToDetail()
            }
            ErrorCode.NO_FREE_SPOTS, ErrorCode.BOOKING_CUTOFF_EXCEEDED -> {
                _model.value = _model.value.copy(snackbarError = error)
            }
            else -> _model.value = _model.value.copy(snackbarError = error)
        }
    }

    private suspend fun handleRentalUpdateFailure(error: AppError) {
        _model.value = _model.value.copy(isSubmitting = false)
        when (error.code) {
            ErrorCode.RENTAL_UNAVAILABLE -> {
                _model.value = _model.value.copy(snackbarError = error)
                refreshRentalAvailability()
            }
            ErrorCode.FORBIDDEN -> {
                onRentalUpdateForbidden(error)
            }
            ErrorCode.NOT_FOUND -> {
                onBookingNotFound()
            }
            else -> _model.value = _model.value.copy(snackbarError = error)
        }
    }

    private suspend fun refreshRentalAvailability() {
        when (val result = getRentalAvailability(slotId)) {
            is AppResult.Success -> {
                val content = _model.value.content as? Async.Content ?: return
                _model.value = _model.value.copy(
                    content = Async.Content(content.data.copy(rentalAvailability = result.value)),
                )
            }
            is AppResult.Failure -> Unit
        }
    }

    private data class LoadResult(
        val content: BookingComponent.Content,
        val usesOwnEquipment: Boolean,
        val selectedRentalIds: Set<String>,
        val heldRentalIds: Set<String>,
        val originalUsesOwnEquipment: Boolean,
        val originalRentalIds: Set<String>,
    )
}
