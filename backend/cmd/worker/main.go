package main

import (
	"context"
	"log/slog"
	"os"
	"os/signal"
	"syscall"
	"time"

	appcfg "github.com/vertical-climbing/backend/internal/adapters/config"
	pushadapter "github.com/vertical-climbing/backend/internal/adapters/push"
	"github.com/vertical-climbing/backend/internal/adapters/postgres"
	apppush "github.com/vertical-climbing/backend/internal/application/push"
	appref "github.com/vertical-climbing/backend/internal/application/reference"
	appworker "github.com/vertical-climbing/backend/internal/application/worker"
)

func main() {
	slog.SetDefault(slog.New(slog.NewJSONHandler(os.Stdout, &slog.HandlerOptions{Level: slog.LevelInfo})))

	cfg, err := appcfg.Load()
	if err != nil {
		slog.Error("load config", "error", err)
		os.Exit(1)
	}

	interval, err := time.ParseDuration(cfg.WorkerInterval)
	if err != nil {
		slog.Error("parse worker interval", "error", err, "value", cfg.WorkerInterval)
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
	notificationRepo := postgres.NewNotificationRepository(pool)
	deviceRepo := postgres.NewDeviceRepository(pool)
	workerRepo := postgres.NewWorkerRepository(pool)

	pushService := apppush.NewService(pushSender, deviceRepo, notificationRepo, workerRepo)
	referenceService := appref.NewService(referenceRepo)
	workerService := appworker.NewService(workerRepo, referenceService, pushService)

	stop := make(chan os.Signal, 1)
	signal.Notify(stop, syscall.SIGINT, syscall.SIGTERM)

	ticker := time.NewTicker(interval)
	defer ticker.Stop()

	slog.Info("worker starting", "interval", interval.String(), "push_sender", cfg.PushSender)

	run := func() {
		if err := workerService.RunAll(ctx); err != nil {
			slog.Error("worker tick failed", "error", err)
		}
	}

	run()

	for {
		select {
		case <-ticker.C:
			run()
		case <-stop:
			slog.Info("worker stopped")
			return
		}
	}
}
