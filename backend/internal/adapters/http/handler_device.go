package httpx

import (
	"net/http"
	"strings"

	"github.com/vertical-climbing/backend/internal/adapters/http/api"
)

func (h *Handler) RegisterPushToken(w http.ResponseWriter, r *http.Request) {
	if _, err := clientIDFromRequest(r); err != nil {
		WriteError(w, err)
		return
	}

	var req api.PushTokenRequest
	if err := decodeJSON(r, &req); err != nil {
		WriteError(w, err)
		return
	}

	if err := h.devices.RegisterPushToken(
		r.Context(),
		strings.TrimSpace(req.Token),
		strings.TrimSpace(string(req.Platform)),
	); err != nil {
		WriteError(w, err)
		return
	}

	w.WriteHeader(http.StatusNoContent)
}
