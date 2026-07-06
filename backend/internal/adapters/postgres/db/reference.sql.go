package db

import (
	"context"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgconn"
	"github.com/jackc/pgx/v5/pgxpool"
)

type DBTX interface {
	Exec(context.Context, string, ...interface{}) (pgconn.CommandTag, error)
	Query(context.Context, string, ...interface{}) (pgx.Rows, error)
	QueryRow(context.Context, string, ...interface{}) pgx.Row
}

func New(db DBTX) *Queries {
	return &Queries{db: db}
}

type Queries struct {
	db DBTX
}

func (q *Queries) WithTx(tx pgx.Tx) *Queries {
	return &Queries{db: tx}
}

type SystemConfigRow struct {
	ReminderHoursBefore          int32 `json:"reminder_hours_before"`
	VisitsForLoyalty             int32 `json:"visits_for_loyalty"`
	ViolationsForSanctions       int32 `json:"violations_for_sanctions"`
	BookingCutoffMinutes         int32 `json:"booking_cutoff_minutes"`
	CancellationForbiddenMinutes int32 `json:"cancellation_forbidden_minutes"`
}

type RentalEquipmentTypeRow struct {
	ID           uuid.UUID `json:"id"`
	Code         string    `json:"code"`
	Name         string    `json:"name"`
	DefaultPrice string    `json:"default_price"`
}

const getSystemConfig = `-- name: GetSystemConfig :one
SELECT
    reminder_hours_before,
    visits_for_loyalty,
    violations_for_sanctions,
    booking_cutoff_minutes,
    cancellation_forbidden_minutes
FROM system_config
ORDER BY updated_at DESC
LIMIT 1`

func (q *Queries) GetSystemConfig(ctx context.Context) (SystemConfigRow, error) {
	row := q.db.QueryRow(ctx, getSystemConfig)
	var i SystemConfigRow
	err := row.Scan(
		&i.ReminderHoursBefore,
		&i.VisitsForLoyalty,
		&i.ViolationsForSanctions,
		&i.BookingCutoffMinutes,
		&i.CancellationForbiddenMinutes,
	)
	return i, err
}

const listRentalEquipmentTypes = `-- name: ListRentalEquipmentTypes :many
SELECT id, code, name, default_price::text
FROM rental_equipment_types
ORDER BY code`

func (q *Queries) ListRentalEquipmentTypes(ctx context.Context) ([]RentalEquipmentTypeRow, error) {
	rows, err := q.db.Query(ctx, listRentalEquipmentTypes)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var items []RentalEquipmentTypeRow
	for rows.Next() {
		var i RentalEquipmentTypeRow
		if err := rows.Scan(&i.ID, &i.Code, &i.Name, &i.DefaultPrice); err != nil {
			return nil, err
		}
		items = append(items, i)
	}
	if err := rows.Err(); err != nil {
		return nil, err
	}
	return items, nil
}

// Compile-time check that pgxpool.Pool implements DBTX.
var _ DBTX = (*pgxpool.Pool)(nil)
