package config

import (
	"fmt"
	"os"
	"strings"

	"github.com/joho/godotenv"
)

type Config struct {
	HTTPHost           string
	HTTPPort           string
	DatabaseURL        string
	CORSAllowedOrigins []string
	JWTSecret          string
	InternalAPIKey     string
	PushSender         string
	WorkerInterval     string
}

func Load() (Config, error) {
	_ = godotenv.Load()

	cfg := Config{
		HTTPHost:    envOrDefault("HTTP_HOST", "0.0.0.0"),
		HTTPPort:    envOrDefault("HTTP_PORT", "8080"),
		DatabaseURL: envOrDefault("DATABASE_URL", "postgres://vertical:vertical@localhost:5432/vertical?sslmode=disable"),
		JWTSecret:      envOrDefault("JWT_SECRET", "change-me-in-production"),
		InternalAPIKey: envOrDefault("INTERNAL_API_KEY", "dev-internal-key"),
		PushSender:     envOrDefault("PUSH_SENDER", "logging"),
		WorkerInterval: envOrDefault("WORKER_INTERVAL", "1m"),
	}

	origins := envOrDefault("CORS_ALLOWED_ORIGINS", "http://localhost:3000")
	for _, origin := range strings.Split(origins, ",") {
		origin = strings.TrimSpace(origin)
		if origin != "" {
			cfg.CORSAllowedOrigins = append(cfg.CORSAllowedOrigins, origin)
		}
	}

	if cfg.DatabaseURL == "" {
		return Config{}, fmt.Errorf("DATABASE_URL is required")
	}

	return cfg, nil
}

func (c Config) HTTPAddr() string {
	return fmt.Sprintf("%s:%s", c.HTTPHost, c.HTTPPort)
}

func envOrDefault(key, fallback string) string {
	if value := os.Getenv(key); value != "" {
		return value
	}
	return fallback
}
