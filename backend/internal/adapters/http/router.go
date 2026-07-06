package httpx

import (
	"log/slog"
	"net/http"

	"github.com/go-chi/chi/v5"
	"github.com/go-chi/chi/v5/middleware"
	"github.com/go-chi/cors"
	appcfg "github.com/vertical-climbing/backend/internal/adapters/config"
	"github.com/vertical-climbing/backend/internal/adapters/http/api"
	portauth "github.com/vertical-climbing/backend/internal/ports/auth"
)

type Dependencies struct {
	Handler *Handler
	Tokens  portauth.TokenService
}

func NewRouter(cfg appcfg.Config, deps Dependencies) http.Handler {
	router := chi.NewRouter()

	router.Use(middleware.Recoverer)
	router.Use(RequestID)
	router.Use(AccessLog(func(msg string, args ...any) {
		slog.Info(msg, args...)
	}))
	router.Use(cors.Handler(cors.Options{
		AllowedOrigins:   cfg.CORSAllowedOrigins,
		AllowedMethods:   []string{"GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"},
		AllowedHeaders:   []string{"Accept", "Authorization", "Content-Type", "X-Request-ID"},
		ExposedHeaders:   []string{"X-Request-ID"},
		AllowCredentials: true,
		MaxAge:           300,
	}))

	router.Get("/healthz", func(w http.ResponseWriter, r *http.Request) {
		WriteJSON(w, http.StatusOK, map[string]string{"status": "ok"})
	})

	router.Route("/v1", func(r chi.Router) {
		api.RegisterRoutes(r, deps.Handler, BearerAuth(deps.Tokens), OptionalBearerAuth(deps.Tokens))
	})

	router.Route("/internal", func(r chi.Router) {
		r.Use(InternalAPIKeyAuth(cfg.InternalAPIKey))
		r.Post("/slots/{slotId}/cancel-by-gym", deps.Handler.CancelSlotByGym)
	})

	return router
}
