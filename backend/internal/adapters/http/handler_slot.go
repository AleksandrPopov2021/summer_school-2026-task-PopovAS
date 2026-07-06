package httpx

import (
	"net/http"

	"github.com/go-chi/chi/v5"
	"github.com/vertical-climbing/backend/internal/adapters/http/api"
	portbooking "github.com/vertical-climbing/backend/internal/ports/booking"
)

func (h *Handler) ListSlots(w http.ResponseWriter, r *http.Request) {
	query := r.URL.Query()
	items, err := h.slots.List(r.Context(), query.Get("from"), query.Get("to"), query.Get("date"))
	if err != nil {
		WriteError(w, err)
		return
	}

	response := api.TrainingSlotList{
		Items: make([]api.TrainingSlotSummary, 0, len(items)),
	}
	for _, item := range items {
		response.Items = append(response.Items, ToAPITrainingSlotSummary(item.Slot, item.Availability))
	}

	WriteJSON(w, http.StatusOK, response)
}

func (h *Handler) GetSlot(w http.ResponseWriter, r *http.Request) {
	slotID := chi.URLParam(r, "slotId")
	detail, availability, err := h.slots.GetDetail(r.Context(), slotID)
	if err != nil {
		WriteError(w, err)
		return
	}

	WriteJSON(w, http.StatusOK, ToAPITrainingSlotDetail(detail, availability))
}

func (h *Handler) FindAlternativeSlot(w http.ResponseWriter, r *http.Request) {
	if _, err := clientIDFromRequest(r); err != nil {
		WriteError(w, err)
		return
	}

	query := r.URL.Query()
	result, err := h.bookings.FindAlternative(
		r.Context(),
		query.Get("cancelled_slot_id"),
		query.Get("booking_id"),
	)
	if err != nil {
		WriteError(w, err)
		return
	}

	if !result.Found || result.Slot == nil {
		WriteJSON(w, http.StatusOK, api.AlternativeSlotResponse{Found: false})
		return
	}

	config, err := h.reference.GetSystemConfig(r.Context())
	if err != nil {
		WriteError(w, err)
		return
	}

	availability := h.bookingSlotAvailability(
		portbooking.Booking{Slot: *result.Slot},
		config.BookingCutoffMinutes,
		true,
	)
	WriteJSON(w, http.StatusOK, ToAPIAlternativeSlotResponse(*result.Slot, availability))
}
