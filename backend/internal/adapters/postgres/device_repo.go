package postgres

import (
	"context"
	"fmt"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/vertical-climbing/backend/internal/adapters/postgres/db"
	"github.com/vertical-climbing/backend/internal/domain"
	portdevice "github.com/vertical-climbing/backend/internal/ports/device"
)

type DeviceRepository struct {
	pool *pgxpool.Pool
}

func NewDeviceRepository(pool *pgxpool.Pool) *DeviceRepository {
	return &DeviceRepository{pool: pool}
}

func (r *DeviceRepository) UpsertPushToken(ctx context.Context, clientID, token, platform string) error {
	id, err := uuid.Parse(clientID)
	if err != nil {
		return domain.NewUnauthorized("Требуется авторизация")
	}

	queries := db.New(r.pool)
	if err := queries.UpsertDevicePushToken(ctx, id, token, platform); err != nil {
		return fmt.Errorf("upsert push token: %w", err)
	}
	return nil
}

func (r *DeviceRepository) ListTokensByClientID(ctx context.Context, clientID string) ([]portdevice.Token, error) {
	id, err := uuid.Parse(clientID)
	if err != nil {
		return nil, domain.NewNotFound("Клиент не найден")
	}

	queries := db.New(r.pool)
	rows, err := queries.ListDevicePushTokensByClientID(ctx, id)
	if err != nil {
		return nil, fmt.Errorf("list push tokens: %w", err)
	}

	tokens := make([]portdevice.Token, 0, len(rows))
	for _, row := range rows {
		tokens = append(tokens, portdevice.Token{
			Token:    row.Token,
			Platform: row.Platform,
		})
	}
	return tokens, nil
}
