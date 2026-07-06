package httpx

import (
	"net/http"
	"time"

	"github.com/go-chi/chi/v5"
	"github.com/vertical-climbing/backend/internal/adapters/http/api"
	"github.com/vertical-climbing/backend/internal/domain"
	portbooking "github.com/vertical-climbing/backend/internal/ports/booking"
)

func (h *Handler) ListBookings(w http.ResponseWriter, r *http.Request) {
	if _, err := clientIDFromRequest(r); err != nil {
		WriteError(w, err)
		return
	}

	status := r.URL.Query().Get("status")
	bookings, err := h.bookings.List(r.Context(), status)
	if err != nil {
		WriteError(w, err)
		return
	}

	config, err := h.reference.GetSystemConfig(r.Context())
	if err != nil {
		WriteError(w, err)
		return
	}

	response := api.BookingList{
		Items: make([]api.BookingSummary, 0, len(bookings)),
	}
	for _, booking := range bookings {
		availability := h.bookingSlotAvailability(booking, config.BookingCutoffMinutes, true)
		policy := domain.BuildCancellationPolicy(domain.CancellationPolicyInput{
			BookingStatus:                booking.BookingStatus,
			SlotStartsAt:                 booking.Slot.StartsAt,
			CancellationForbiddenMinutes: config.CancellationForbiddenMinutes,
			Now:                          time.Now(),
		})
		response.Items = append(response.Items, ToAPIBookingSummary(booking, availability, policy))
	}

	WriteJSON(w, http.StatusOK, response)
}

func (h *Handler) GetBooking(w http.ResponseWriter, r *http.Request) {
	if _, err := clientIDFromRequest(r); err != nil {
		WriteError(w, err)
		return
	}

	bookingID := chi.URLParam(r, "bookingId")
	booking, err := h.bookings.GetByID(r.Context(), bookingID)
	if err != nil {
		WriteError(w, err)
		return
	}

	h.writeBookingDetail(w, r, http.StatusOK, booking)
}

func (h *Handler) CancelBooking(w http.ResponseWriter, r *http.Request) {
	if _, err := clientIDFromRequest(r); err != nil {
		WriteError(w, err)
		return
	}

	bookingID := chi.URLParam(r, "bookingId")
	booking, err := h.bookings.Cancel(r.Context(), bookingID)
	if err != nil {
		WriteError(w, err)
		return
	}

	h.writeBookingDetail(w, r, http.StatusOK, booking)
}

func (h *Handler) UpdateBookingRental(w http.ResponseWriter, r *http.Request) {
	if _, err := clientIDFromRequest(r); err != nil {
		WriteError(w, err)
		return
	}

	var req api.UpdateBookingRentalRequest
	if err := decodeJSON(r, &req); err != nil {
		WriteError(w, err)
		return
	}

	bookingID := chi.URLParam(r, "bookingId")
	booking, err := h.bookings.UpdateRental(r.Context(), portbooking.UpdateRentalInput{
		BookingID:        bookingID,
		UsesOwnEquipment: req.UsesOwnEquipment,
		RentalLines:      parseRentalLineInputs(req.RentalLines),
	})
	if err != nil {
		WriteError(w, err)
		return
	}

	h.writeBookingDetail(w, r, http.StatusOK, booking)
}

func (h *Handler) CreateBooking(w http.ResponseWriter, r *http.Request) {
	var req api.CreateBookingRequest
	if err := decodeJSON(r, &req); err != nil {
		WriteError(w, err)
		return
	}

	rentalLines := parseRentalLineInputs(req.RentalLines)

	booking, err := h.bookings.Create(r.Context(), portbooking.CreateInput{
		SlotID:           req.SlotId,
		UsesOwnEquipment: req.UsesOwnEquipment,
		RentalLines:      rentalLines,
	})
	if err != nil {
		WriteError(w, err)
		return
	}

	h.writeBookingDetail(w, r, http.StatusCreated, booking)
}

func (h *Handler) GetSlotRentalAvailability(w http.ResponseWriter, r *http.Request) {
	if _, err := clientIDFromRequest(r); err != nil {
		WriteError(w, err)
		return
	}

	slotID := chi.URLParam(r, "slotId")
	items, err := h.slots.GetRentalAvailability(r.Context(), slotID)
	if err != nil {
		WriteError(w, err)
		return
	}

	WriteJSON(w, http.StatusOK, ToAPISlotRentalAvailabilityList(slotID, items))
}

func (h *Handler) writeBookingDetail(w http.ResponseWriter, r *http.Request, status int, booking portbooking.Booking) {
	config, err := h.reference.GetSystemConfig(r.Context())
	if err != nil {
		WriteError(w, err)
		return
	}

	availability := h.bookingSlotAvailability(booking, config.BookingCutoffMinutes, true)
	policy := domain.BuildCancellationPolicy(domain.CancellationPolicyInput{
		BookingStatus:                booking.BookingStatus,
		SlotStartsAt:                 booking.Slot.StartsAt,
		CancellationForbiddenMinutes: config.CancellationForbiddenMinutes,
		Now:                          time.Now(),
	})

	WriteJSON(w, status, ToAPIBookingDetail(booking, availability, policy))
}

func parseRentalLineInputs(lines []api.BookingRentalLineInput) []portbooking.RentalLineInput {
	result := make([]portbooking.RentalLineInput, 0, len(lines))
	for _, line := range lines {
		quantity := line.Quantity
		if quantity < 1 {
			quantity = 1
		}
		result = append(result, portbooking.RentalLineInput{
			EquipmentTypeID: line.EquipmentTypeId,
			Quantity:        int32(quantity),
		})
	}
	return result
}

func (h *Handler) bookingSlotAvailability(booking portbooking.Booking, cutoffMinutes int32, authenticated bool) domain.BookingAvailability {
	return domain.BuildBookingAvailability(domain.AvailabilityInput{
		SlotStatus:             booking.Slot.SlotStatus,
		FormatType:             booking.Slot.Zone.FormatType,
		FreeSpots:              booking.Slot.FreeSpots,
		StartsAt:               booking.Slot.StartsAt,
		BookingCutoffMinutes:   cutoffMinutes,
		HasClearance:           true,
		HasAuthenticatedClient: authenticated,
		Now:                    time.Now(),
	})
}
