package httpx

import (
	"encoding/json"
	"io"
	"net/http"
	"strings"

	"github.com/vertical-climbing/backend/internal/adapters/http/api"
	"github.com/vertical-climbing/backend/internal/domain"
	portclient "github.com/vertical-climbing/backend/internal/ports/client"
)

func (h *Handler) RegisterClient(w http.ResponseWriter, r *http.Request) {
	var req api.ClientRegistrationRequest
	if err := decodeJSON(r, &req); err != nil {
		WriteError(w, err)
		return
	}

	birthDate, err := domain.ParseBirthDate(req.BirthDate)
	if err != nil {
		WriteError(w, err)
		return
	}

	result, err := h.client.Register(r.Context(), portclient.RegisterInput{
		Phone:     strings.TrimSpace(req.Phone),
		FullName:  strings.TrimSpace(req.FullName),
		BirthDate: birthDate,
	})
	if err != nil {
		WriteError(w, err)
		return
	}

	WriteJSON(w, http.StatusCreated, api.ClientRegistrationResponse{
		AccessToken: result.AccessToken,
		TokenType:   api.TokenTypeBearer,
		Client:      ToAPIClient(result.Client),
	})
}

func (h *Handler) GetCurrentClient(w http.ResponseWriter, r *http.Request) {
	clientID, err := clientIDFromRequest(r)
	if err != nil {
		WriteError(w, err)
		return
	}

	client, err := h.client.GetCurrent(r.Context(), clientID)
	if err != nil {
		WriteError(w, err)
		return
	}

	WriteJSON(w, http.StatusOK, ToAPIClient(client))
}

func (h *Handler) UpdateCurrentClient(w http.ResponseWriter, r *http.Request) {
	clientID, err := clientIDFromRequest(r)
	if err != nil {
		WriteError(w, err)
		return
	}

	var req api.ClientUpdateRequest
	if err := decodeJSON(r, &req); err != nil {
		WriteError(w, err)
		return
	}

	client, err := h.client.UpdateRiskConsent(r.Context(), clientID, req.RiskConsentAccepted)
	if err != nil {
		WriteError(w, err)
		return
	}

	WriteJSON(w, http.StatusOK, ToAPIClient(client))
}

func (h *Handler) GetNotificationPreferences(w http.ResponseWriter, r *http.Request) {
	clientID, err := clientIDFromRequest(r)
	if err != nil {
		WriteError(w, err)
		return
	}

	prefs, err := h.notification.Get(r.Context(), clientID)
	if err != nil {
		WriteError(w, err)
		return
	}

	WriteJSON(w, http.StatusOK, ToAPINotificationPreferences(prefs))
}

func (h *Handler) UpdateNotificationPreferences(w http.ResponseWriter, r *http.Request) {
	clientID, err := clientIDFromRequest(r)
	if err != nil {
		WriteError(w, err)
		return
	}

	var req api.NotificationPreferencesUpdateRequest
	if err := decodeJSON(r, &req); err != nil {
		WriteError(w, err)
		return
	}

	prefs, err := h.notification.Update(
		r.Context(),
		clientID,
		req.BookingConfirmationEnabled,
		req.RatingInvitationEnabled,
		req.RemindersEnabled,
		req.GymCancellationEnabled,
	)
	if err != nil {
		WriteError(w, err)
		return
	}

	WriteJSON(w, http.StatusOK, ToAPINotificationPreferences(prefs))
}

func decodeJSON(r *http.Request, dst any) error {
	defer r.Body.Close()

	decoder := json.NewDecoder(r.Body)
	decoder.DisallowUnknownFields()

	if err := decoder.Decode(dst); err != nil {
		if err == io.EOF {
			return domain.NewBadRequest("Пустое тело запроса")
		}
		return domain.NewBadRequest("Некорректный JSON")
	}

	if err := decoder.Decode(&struct{}{}); err != io.EOF {
		return domain.NewBadRequest("Некорректный JSON")
	}

	return nil
}
