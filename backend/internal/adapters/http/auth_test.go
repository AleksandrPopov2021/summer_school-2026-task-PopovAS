package httpx_test

import (
	"net/http"
	"net/http/httptest"
	"testing"

	jwtauth "github.com/vertical-climbing/backend/internal/adapters/auth"
	httpx "github.com/vertical-climbing/backend/internal/adapters/http"
)

func TestBearerAuthMiddleware(t *testing.T) {
	tokenService, err := jwtauth.NewService("middleware-test-secret")
	if err != nil {
		t.Fatalf("new jwt service: %v", err)
	}

	handler := httpx.BearerAuth(tokenService)(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusNoContent)
	}))

	t.Run("missing token", func(t *testing.T) {
		req := httptest.NewRequest(http.MethodGet, "/v1/clients/me", nil)
		rec := httptest.NewRecorder()
		handler.ServeHTTP(rec, req)

		if rec.Code != http.StatusUnauthorized {
			t.Fatalf("expected 401, got %d", rec.Code)
		}
	})

	t.Run("valid token", func(t *testing.T) {
		token, err := tokenService.Issue("client-123")
		if err != nil {
			t.Fatalf("issue token: %v", err)
		}

		req := httptest.NewRequest(http.MethodGet, "/v1/clients/me", nil)
		req.Header.Set("Authorization", "Bearer "+token)
		rec := httptest.NewRecorder()
		handler.ServeHTTP(rec, req)

		if rec.Code != http.StatusNoContent {
			t.Fatalf("expected 204, got %d", rec.Code)
		}
	})
}
