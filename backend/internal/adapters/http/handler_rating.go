package httpx

import (
	"net/http"

	"github.com/vertical-climbing/backend/internal/adapters/http/api"
)

func (h *Handler) CreateInstructorRating(w http.ResponseWriter, r *http.Request) {
	if _, err := clientIDFromRequest(r); err != nil {
		WriteError(w, err)
		return
	}

	var req api.CreateRatingRequest
	if err := decodeJSON(r, &req); err != nil {
		WriteError(w, err)
		return
	}

	rating, err := h.ratings.Create(r.Context(), req.BookingId, int32(req.Stars))
	if err != nil {
		WriteError(w, err)
		return
	}

	WriteJSON(w, http.StatusCreated, ToAPIInstructorRating(rating))
}
