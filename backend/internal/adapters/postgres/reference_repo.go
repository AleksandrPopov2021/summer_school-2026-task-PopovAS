package postgres

import (
	"context"
	"errors"
	"fmt"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/shopspring/decimal"
	"github.com/vertical-climbing/backend/internal/adapters/postgres/db"
	"github.com/vertical-climbing/backend/internal/domain"
	port "github.com/vertical-climbing/backend/internal/ports/reference"
)

type ReferenceRepository struct {
	queries *db.Queries
}

func NewReferenceRepository(pool *pgxpool.Pool) *ReferenceRepository {
	return &ReferenceRepository{queries: db.New(pool)}
}

func (r *ReferenceRepository) GetSystemConfig(ctx context.Context) (port.SystemConfig, error) {
	row, err := r.queries.GetSystemConfig(ctx)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return port.SystemConfig{}, domain.NewNotFound("Системные параметры не настроены")
		}
		return port.SystemConfig{}, fmt.Errorf("get system config: %w", err)
	}

	return port.SystemConfig{
		ReminderHoursBefore:          row.ReminderHoursBefore,
		VisitsForLoyalty:             row.VisitsForLoyalty,
		ViolationsForSanctions:       row.ViolationsForSanctions,
		BookingCutoffMinutes:         row.BookingCutoffMinutes,
		CancellationForbiddenMinutes: row.CancellationForbiddenMinutes,
	}, nil
}

func (r *ReferenceRepository) ListRentalEquipmentTypes(ctx context.Context) ([]port.RentalEquipmentType, error) {
	rows, err := r.queries.ListRentalEquipmentTypes(ctx)
	if err != nil {
		return nil, fmt.Errorf("list rental equipment types: %w", err)
	}

	items := make([]port.RentalEquipmentType, 0, len(rows))
	for _, row := range rows {
		price, err := decimal.NewFromString(row.DefaultPrice)
		if err != nil {
			return nil, fmt.Errorf("parse default_price for %s: %w", row.Code, err)
		}

		items = append(items, port.RentalEquipmentType{
			ID:           row.ID.String(),
			Code:         row.Code,
			Name:         row.Name,
			DefaultPrice: price,
		})
	}

	return items, nil
}

func NewPool(ctx context.Context, databaseURL string) (*pgxpool.Pool, error) {
	pool, err := pgxpool.New(ctx, databaseURL)
	if err != nil {
		return nil, fmt.Errorf("connect to database: %w", err)
	}

	if err := pool.Ping(ctx); err != nil {
		pool.Close()
		return nil, fmt.Errorf("ping database: %w", err)
	}

	return pool, nil
}
