package httpx

import (
	"net/http"

	"github.com/go-chi/chi/v5"
	portbooking "github.com/vertical-climbing/backend/internal/ports/booking"
)

type CancelSlotByGymRequest struct {
	CancellationReasonID string `json:"cancellation_reason_id"`
}

type CancelSlotByGymResponse struct {
	SlotID                 string `json:"slot_id"`
	CancelledBookingsCount int    `json:"cancelled_bookings_count"`
}

func (h *Handler) CancelSlotByGym(w http.ResponseWriter, r *http.Request) {
	var req CancelSlotByGymRequest
	if err := decodeJSON(r, &req); err != nil {
		WriteError(w, err)
		return
	}

	slotID := chi.URLParam(r, "slotId")
	result, err := h.bookings.CancelSlotByGym(r.Context(), portbooking.CancelSlotByGymInput{
		SlotID:               slotID,
		CancellationReasonID: req.CancellationReasonID,
	})
	if err != nil {
		WriteError(w, err)
		return
	}

	WriteJSON(w, http.StatusOK, CancelSlotByGymResponse{
		SlotID:                 result.SlotID,
		CancelledBookingsCount: result.CancelledBookingsCount,
	})
}
