package httpx

import (
	"net/http"

	"github.com/vertical-climbing/backend/internal/domain"
)

func InternalAPIKeyAuth(expectedKey string) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			if expectedKey == "" {
				WriteError(w, domain.NewUnauthorized("Internal API отключён"))
				return
			}
			if r.Header.Get("X-API-Key") != expectedKey {
				WriteError(w, domain.NewUnauthorized("Неверный API-ключ"))
				return
			}
			next.ServeHTTP(w, r)
		})
	}
}
