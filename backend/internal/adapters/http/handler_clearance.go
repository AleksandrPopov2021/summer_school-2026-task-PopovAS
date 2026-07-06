package httpx

import (
	"net/http"

	"github.com/vertical-climbing/backend/internal/adapters/http/api"
)

func (h *Handler) GetClientClearances(w http.ResponseWriter, r *http.Request) {
	items, err := h.clearances.ListForCurrentClient(r.Context())
	if err != nil {
		WriteError(w, err)
		return
	}

	response := api.InstructorClearanceList{
		Items: make([]api.InstructorClearance, 0, len(items)),
	}
	for _, item := range items {
		response.Items = append(response.Items, ToAPIInstructorClearance(item))
	}

	WriteJSON(w, http.StatusOK, response)
}
