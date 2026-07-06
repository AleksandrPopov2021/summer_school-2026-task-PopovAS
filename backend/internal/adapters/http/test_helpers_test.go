package httpx_test

import (
	"context"
	"net/http/httptest"
	"os"
	"testing"

	jwtauth "github.com/vertical-climbing/backend/internal/adapters/auth"
	appcfg "github.com/vertical-climbing/backend/internal/adapters/config"
	pushadapter "github.com/vertical-climbing/backend/internal/adapters/push"
	httpx "github.com/vertical-climbing/backend/internal/adapters/http"
	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/vertical-climbing/backend/internal/adapters/postgres"
	appbooking "github.com/vertical-climbing/backend/internal/application/booking"
	appclearance "github.com/vertical-climbing/backend/internal/application/clearance"
	appclient "github.com/vertical-climbing/backend/internal/application/client"
	appdevice "github.com/vertical-climbing/backend/internal/application/device"
	appnotification "github.com/vertical-climbing/backend/internal/application/notification"
	apppush "github.com/vertical-climbing/backend/internal/application/push"
	appref "github.com/vertical-climbing/backend/internal/application/reference"
	apprating "github.com/vertical-climbing/backend/internal/application/rating"
	appslot "github.com/vertical-climbing/backend/internal/application/slot"
)

func newIntegrationServer(t *testing.T) (*httptest.Server, *pgxpool.Pool) {
	t.Helper()

	databaseURL := os.Getenv("DATABASE_URL")
	if databaseURL == "" {
		t.Skip("DATABASE_URL is not set")
	}

	ctx := context.Background()
	pool, err := postgres.NewPool(ctx, databaseURL)
	if err != nil {
		t.Fatalf("connect database: %v", err)
	}
	t.Cleanup(pool.Close)

	tokenService, err := jwtauth.NewService("integration-test-secret")
	if err != nil {
		t.Fatalf("init jwt: %v", err)
	}

	referenceRepo := postgres.NewReferenceRepository(pool)
	clientRepo := postgres.NewClientRepository(pool)
	notificationRepo := postgres.NewNotificationRepository(pool)
	deviceRepo := postgres.NewDeviceRepository(pool)
	workerRepo := postgres.NewWorkerRepository(pool)
	slotRepo := postgres.NewSlotRepository(pool)
	clearanceRepo := postgres.NewClearanceRepository(pool)
	bookingRepo := postgres.NewBookingRepository(pool, clientRepo, clearanceRepo, referenceRepo, slotRepo)
	ratingRepo := postgres.NewRatingRepository(pool)

	pushService := apppush.NewService(pushadapter.NewNoopSender(), deviceRepo, notificationRepo, workerRepo)

	handler := httpx.NewHandler(
		appref.NewService(referenceRepo),
		appclient.NewService(clientRepo, tokenService),
		appnotification.NewService(notificationRepo),
		appdevice.NewService(deviceRepo),
		appslot.NewService(slotRepo, referenceRepo, clearanceRepo),
		appclearance.NewService(clearanceRepo),
		appbooking.NewService(bookingRepo, referenceRepo, pushService),
		apprating.NewService(ratingRepo),
	)

	server := httptest.NewServer(httpx.NewRouter(appcfg.Config{
		CORSAllowedOrigins: []string{"http://localhost"},
		InternalAPIKey:     "dev-internal-key",
	}, httpx.Dependencies{
		Handler: handler,
		Tokens:  tokenService,
	}))
	t.Cleanup(server.Close)

	return server, pool
}
