package httpx_test

import (
	"encoding/json"
	"errors"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/vertical-climbing/backend/internal/adapters/http/api"
	httpx "github.com/vertical-climbing/backend/internal/adapters/http"
	"github.com/vertical-climbing/backend/internal/domain"
)

func TestWriteError_AppError(t *testing.T) {
	rec := httptest.NewRecorder()
	httpx.WriteError(rec, domain.NewNotFound("не найдено"))

	if rec.Code != http.StatusNotFound {
		t.Fatalf("expected 404, got %d", rec.Code)
	}

	var body api.ErrorResponse
	if err := json.NewDecoder(rec.Body).Decode(&body); err != nil {
		t.Fatalf("decode response: %v", err)
	}

	if body.Code != string(domain.ErrorCodeNotFound) {
		t.Fatalf("expected NOT_FOUND code, got %s", body.Code)
	}
	if body.Message != "не найдено" {
		t.Fatalf("unexpected message: %s", body.Message)
	}
}

func TestWriteError_Internal(t *testing.T) {
	rec := httptest.NewRecorder()
	httpx.WriteError(rec, errors.New("boom"))

	if rec.Code != http.StatusInternalServerError {
		t.Fatalf("expected 500, got %d", rec.Code)
	}
}
