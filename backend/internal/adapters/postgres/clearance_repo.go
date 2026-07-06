package postgres

import (
	"context"
	"fmt"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/vertical-climbing/backend/internal/adapters/postgres/db"
	portclearance "github.com/vertical-climbing/backend/internal/ports/clearance"
)

type ClearanceRepository struct {
	pool *pgxpool.Pool
}

func NewClearanceRepository(pool *pgxpool.Pool) *ClearanceRepository {
	return &ClearanceRepository{pool: pool}
}

func (r *ClearanceRepository) ListByClientID(ctx context.Context, clientID string) ([]portclearance.Clearance, error) {
	id, err := uuid.Parse(clientID)
	if err != nil {
		return []portclearance.Clearance{}, nil
	}

	queries := db.New(r.pool)
	rows, err := queries.ListClearancesByClientID(ctx, id)
	if err != nil {
		return nil, fmt.Errorf("list clearances: %w", err)
	}

	items := make([]portclearance.Clearance, 0, len(rows))
	for _, row := range rows {
		var instructorID *string
		if row.InstructorID != nil {
			value := row.InstructorID.String()
			instructorID = &value
		}
		items = append(items, portclearance.Clearance{
			ID:           row.ID.String(),
			ClientID:     row.ClientID.String(),
			InstructorID: instructorID,
			IsGranted:    row.IsGranted,
			GrantedAt:    row.GrantedAt,
		})
	}
	return items, nil
}

func (r *ClearanceRepository) HasGrantedClearance(ctx context.Context, clientID string) (bool, error) {
	id, err := uuid.Parse(clientID)
	if err != nil {
		return false, nil
	}

	queries := db.New(r.pool)
	return queries.ClientHasGrantedClearance(ctx, id)
}
