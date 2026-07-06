package httpx

import (
	"net/http"
	"strings"

	"github.com/vertical-climbing/backend/internal/domain"
	portauth "github.com/vertical-climbing/backend/internal/ports/auth"
)

func BearerAuth(tokens portauth.TokenService) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			header := r.Header.Get("Authorization")
			if !strings.HasPrefix(header, "Bearer ") {
				WriteError(w, domain.NewUnauthorized("Требуется авторизация"))
				return
			}

			token := strings.TrimSpace(strings.TrimPrefix(header, "Bearer "))
			if token == "" {
				WriteError(w, domain.NewUnauthorized("Требуется авторизация"))
				return
			}

			clientID, err := tokens.Parse(token)
			if err != nil {
				WriteError(w, err)
				return
			}

			next.ServeHTTP(w, r.WithContext(portauth.WithClientID(r.Context(), clientID)))
		})
	}
}

func OptionalBearerAuth(tokens portauth.TokenService) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			header := r.Header.Get("Authorization")
			if !strings.HasPrefix(header, "Bearer ") {
				next.ServeHTTP(w, r)
				return
			}

			token := strings.TrimSpace(strings.TrimPrefix(header, "Bearer "))
			if token == "" {
				next.ServeHTTP(w, r)
				return
			}

			clientID, err := tokens.Parse(token)
			if err != nil {
				next.ServeHTTP(w, r)
				return
			}

			next.ServeHTTP(w, r.WithContext(portauth.WithClientID(r.Context(), clientID)))
		})
	}
}

func clientIDFromRequest(r *http.Request) (string, error) {
	clientID, ok := portauth.ClientIDFromContext(r.Context())
	if !ok {
		return "", domain.NewUnauthorized("Требуется авторизация")
	}
	return clientID, nil
}
