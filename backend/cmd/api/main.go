package main

import (
	"context"
	"errors"
	"log/slog"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	jwtauth "github.com/vertical-climbing/backend/internal/adapters/auth"
	appcfg "github.com/vertical-climbing/backend/internal/adapters/config"
	httpadapter "github.com/vertical-climbing/backend/internal/adapters/http"
	pushadapter "github.com/vertical-climbing/backend/internal/adapters/push"
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

func main() {
	slog.SetDefault(slog.New(slog.NewJSONHandler(os.Stdout, &slog.HandlerOptions{Level: slog.LevelInfo})))

	cfg, err := appcfg.Load()
	if err != nil {
		slog.Error("load config", "error", err)
		os.Exit(1)
	}

	tokenService, err := jwtauth.NewService(cfg.JWTSecret)
	if err != nil {
		slog.Error("init jwt", "error", err)
		os.Exit(1)
	}

	ctx := context.Background()
	pool, err := postgres.NewPool(ctx, cfg.DatabaseURL)
	if err != nil {
		slog.Error("connect database", "error", err)
		os.Exit(1)
	}
	defer pool.Close()

	pushSender, err := pushadapter.NewSender(cfg.PushSender)
	if err != nil {
		slog.Error("init push sender", "error", err)
		os.Exit(1)
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

	referenceService := appref.NewService(referenceRepo)
	clientService := appclient.NewService(clientRepo, tokenService)
	notificationService := appnotification.NewService(notificationRepo)
	deviceService := appdevice.NewService(deviceRepo)
	pushService := apppush.NewService(pushSender, deviceRepo, notificationRepo, workerRepo)
	slotService := appslot.NewService(slotRepo, referenceRepo, clearanceRepo)
	clearanceService := appclearance.NewService(clearanceRepo)
	bookingService := appbooking.NewService(bookingRepo, referenceRepo, pushService)
	ratingService := apprating.NewService(ratingRepo)

	handler := httpadapter.NewHandler(
		referenceService,
		clientService,
		notificationService,
		deviceService,
		slotService,
		clearanceService,
		bookingService,
		ratingService,
	)
	router := httpadapter.NewRouter(cfg, httpadapter.Dependencies{
		Handler: handler,
		Tokens:  tokenService,
	})

	server := &http.Server{
		Addr:              cfg.HTTPAddr(),
		Handler:           router,
		ReadHeaderTimeout: 5 * time.Second,
	}

	go func() {
		slog.Info("server starting", "addr", cfg.HTTPAddr())
		if err := server.ListenAndServe(); err != nil && !errors.Is(err, http.ErrServerClosed) {
			slog.Error("server failed", "error", err)
			os.Exit(1)
		}
	}()

	stop := make(chan os.Signal, 1)
	signal.Notify(stop, syscall.SIGINT, syscall.SIGTERM)
	<-stop

	shutdownCtx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()

	if err := server.Shutdown(shutdownCtx); err != nil {
		slog.Error("server shutdown", "error", err)
		os.Exit(1)
	}

	slog.Info("server stopped")
}
