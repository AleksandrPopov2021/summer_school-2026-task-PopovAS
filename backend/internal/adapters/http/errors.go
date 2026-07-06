package httpx

import (
	"encoding/json"
	"errors"
	"net/http"

	"github.com/vertical-climbing/backend/internal/adapters/http/api"
	"github.com/vertical-climbing/backend/internal/domain"
)

func WriteJSON(w http.ResponseWriter, status int, payload any) {
	w.Header().Set("Content-Type", "application/json; charset=utf-8")
	w.WriteHeader(status)
	if payload == nil {
		return
	}
	_ = json.NewEncoder(w).Encode(payload)
}

func WriteError(w http.ResponseWriter, err error) {
	var conflictErr *domain.BookingConflictError
	if errors.As(err, &conflictErr) {
		WriteJSON(w, http.StatusConflict, api.BookingConflictResponse{
			Code:    string(conflictErr.Code),
			Message: conflictErr.Message,
			Slot:    ToAPITrainingSlotDetail(conflictErr.SlotDetail, conflictErr.Availability),
		})
		return
	}

	var appErr *domain.AppError
	if errors.As(err, &appErr) {
		WriteJSON(w, appErr.Status, api.ErrorResponse{
			Code:    string(appErr.Code),
			Message: appErr.Message,
			Details: appErr.Details,
		})
		return
	}

	WriteJSON(w, http.StatusInternalServerError, api.ErrorResponse{
		Code:    string(domain.ErrorCodeInternal),
		Message: "Внутренняя ошибка сервера",
	})
}

func NotImplemented(w http.ResponseWriter) {
	WriteJSON(w, http.StatusNotImplemented, api.ErrorResponse{
		Code:    "NOT_IMPLEMENTED",
		Message: "Эндпоинт будет реализован в следующей итерации",
	})
}
