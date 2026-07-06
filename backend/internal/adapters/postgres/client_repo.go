package postgres

import (
	"context"
	"errors"
	"fmt"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgconn"
	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/shopspring/decimal"
	"github.com/vertical-climbing/backend/internal/adapters/postgres/db"
	"github.com/vertical-climbing/backend/internal/domain"
	portclient "github.com/vertical-climbing/backend/internal/ports/client"
)

type ClientRepository struct {
	pool *pgxpool.Pool
}

func NewClientRepository(pool *pgxpool.Pool) *ClientRepository {
	return &ClientRepository{pool: pool}
}

func (r *ClientRepository) ExistsByPhone(ctx context.Context, phone string) (bool, error) {
	queries := db.New(r.pool)
	_, err := queries.GetClientByPhone(ctx, phone)
	if err == nil {
		return true, nil
	}
	if errors.Is(err, pgx.ErrNoRows) {
		return false, nil
	}
	return false, fmt.Errorf("check client phone: %w", err)
}

func (r *ClientRepository) Register(ctx context.Context, input portclient.RegisterInput) (portclient.Client, error) {
	tx, err := r.pool.Begin(ctx)
	if err != nil {
		return portclient.Client{}, fmt.Errorf("begin transaction: %w", err)
	}
	defer tx.Rollback(ctx)

	queries := db.New(tx)

	row, err := queries.CreateClient(ctx, input.FullName, input.Phone, input.BirthDate)
	if err != nil {
		if isUniqueViolation(err) {
			return portclient.Client{}, domain.NewClientAlreadyExists()
		}
		return portclient.Client{}, fmt.Errorf("create client: %w", err)
	}

	if _, err := queries.CreateNotificationPreferences(ctx, row.ID); err != nil {
		return portclient.Client{}, fmt.Errorf("create notification preferences: %w", err)
	}

	if err := tx.Commit(ctx); err != nil {
		return portclient.Client{}, fmt.Errorf("commit transaction: %w", err)
	}

	return mapClientRow(row)
}

func (r *ClientRepository) GetByID(ctx context.Context, id string) (portclient.Client, error) {
	clientID, err := uuid.Parse(id)
	if err != nil {
		return portclient.Client{}, domain.NewNotFound("Клиент не найден")
	}

	queries := db.New(r.pool)
	row, err := queries.GetClientByID(ctx, clientID)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return portclient.Client{}, domain.NewNotFound("Клиент не найден")
		}
		return portclient.Client{}, fmt.Errorf("get client: %w", err)
	}

	return mapClientRow(row)
}

func (r *ClientRepository) UpdateRiskConsent(ctx context.Context, id string) (portclient.Client, error) {
	clientID, err := uuid.Parse(id)
	if err != nil {
		return portclient.Client{}, domain.NewNotFound("Клиент не найден")
	}

	queries := db.New(r.pool)
	row, err := queries.UpdateClientRiskConsent(ctx, clientID)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return portclient.Client{}, domain.NewNotFound("Клиент не найден")
		}
		return portclient.Client{}, fmt.Errorf("update risk consent: %w", err)
	}

	return mapClientRow(row)
}

func (r *ClientRepository) IncrementLateCancellationCount(ctx context.Context, id string) error {
	clientID, err := uuid.Parse(id)
	if err != nil {
		return domain.NewNotFound("Клиент не найден")
	}

	queries := db.New(r.pool)
	if err := queries.IncrementLateCancellationCount(ctx, clientID); err != nil {
		return fmt.Errorf("increment late cancellation count: %w", err)
	}
	return nil
}

func mapClientRow(row db.ClientRow) (portclient.Client, error) {
	client := portclient.Client{
		ID:                    row.ID.String(),
		FullName:              row.FullName,
		Phone:                 row.Phone,
		BirthDate:             row.BirthDate,
		RiskConsentAccepted:   row.RiskConsentAccepted,
		CompletedVisitsCount:  row.CompletedVisitsCount,
		IsLoyalClient:         row.IsLoyalClient,
		LateCancellationCount: row.LateCancellationCount,
		NoShowCount:           row.NoShowCount,
	}

	if row.LoyaltyDiscount != nil && *row.LoyaltyDiscount != "" {
		discount, err := decimal.NewFromString(*row.LoyaltyDiscount)
		if err != nil {
			return portclient.Client{}, fmt.Errorf("parse loyalty discount: %w", err)
		}
		client.LoyaltyDiscount = &discount
	}

	return client, nil
}

func isUniqueViolation(err error) bool {
	var pgErr *pgconn.PgError
	return errors.As(err, &pgErr) && pgErr.Code == "23505"
}
