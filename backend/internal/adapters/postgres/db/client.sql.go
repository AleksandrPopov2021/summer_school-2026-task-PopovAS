package db

import (
	"context"
	"time"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5"
)

type ClientRow struct {
	ID                    uuid.UUID
	FullName              string
	Phone                 string
	BirthDate             time.Time
	RiskConsentAccepted   bool
	CompletedVisitsCount  int32
	IsLoyalClient         bool
	LoyaltyDiscount       *string
	LateCancellationCount int32
	NoShowCount           int32
}

type NotificationPreferencesRow struct {
	ID                         uuid.UUID
	ClientID                   uuid.UUID
	BookingConfirmationEnabled bool
	RatingInvitationEnabled    bool
	RemindersEnabled           bool
	GymCancellationEnabled     bool
}

const getClientByID = `-- name: GetClientByID :one
SELECT
    id,
    full_name,
    phone,
    birth_date,
    risk_consent_accepted,
    completed_visits_count,
    is_loyal_client,
    loyalty_discount::text AS loyalty_discount,
    late_cancellation_count,
    no_show_count
FROM clients
WHERE id = $1`

func (q *Queries) GetClientByID(ctx context.Context, id uuid.UUID) (ClientRow, error) {
	row := q.db.QueryRow(ctx, getClientByID, id)
	return scanClientRow(row)
}

const getClientByPhone = `-- name: GetClientByPhone :one
SELECT id
FROM clients
WHERE phone = $1`

func (q *Queries) GetClientByPhone(ctx context.Context, phone string) (uuid.UUID, error) {
	row := q.db.QueryRow(ctx, getClientByPhone, phone)
	var id uuid.UUID
	err := row.Scan(&id)
	return id, err
}

const createClient = `-- name: CreateClient :one
INSERT INTO clients (full_name, phone, birth_date)
VALUES ($1, $2, $3)
RETURNING
    id,
    full_name,
    phone,
    birth_date,
    risk_consent_accepted,
    completed_visits_count,
    is_loyal_client,
    loyalty_discount::text AS loyalty_discount,
    late_cancellation_count,
    no_show_count`

func (q *Queries) CreateClient(ctx context.Context, fullName, phone string, birthDate time.Time) (ClientRow, error) {
	row := q.db.QueryRow(ctx, createClient, fullName, phone, birthDate)
	return scanClientRow(row)
}

const updateClientRiskConsent = `-- name: UpdateClientRiskConsent :one
UPDATE clients
SET risk_consent_accepted = TRUE
WHERE id = $1
RETURNING
    id,
    full_name,
    phone,
    birth_date,
    risk_consent_accepted,
    completed_visits_count,
    is_loyal_client,
    loyalty_discount::text AS loyalty_discount,
    late_cancellation_count,
    no_show_count`

func (q *Queries) UpdateClientRiskConsent(ctx context.Context, id uuid.UUID) (ClientRow, error) {
	row := q.db.QueryRow(ctx, updateClientRiskConsent, id)
	return scanClientRow(row)
}

const createNotificationPreferences = `-- name: CreateNotificationPreferences :one
INSERT INTO notification_preferences (
    client_id,
    booking_confirmation_enabled,
    rating_invitation_enabled,
    reminders_enabled,
    gym_cancellation_enabled
) VALUES ($1, TRUE, TRUE, TRUE, TRUE)
RETURNING
    id,
    client_id,
    booking_confirmation_enabled,
    rating_invitation_enabled,
    reminders_enabled,
    gym_cancellation_enabled`

func (q *Queries) CreateNotificationPreferences(ctx context.Context, clientID uuid.UUID) (NotificationPreferencesRow, error) {
	row := q.db.QueryRow(ctx, createNotificationPreferences, clientID)
	return scanNotificationPreferencesRow(row)
}

const getNotificationPreferencesByClientID = `-- name: GetNotificationPreferencesByClientID :one
SELECT
    id,
    client_id,
    booking_confirmation_enabled,
    rating_invitation_enabled,
    reminders_enabled,
    gym_cancellation_enabled
FROM notification_preferences
WHERE client_id = $1`

func (q *Queries) GetNotificationPreferencesByClientID(ctx context.Context, clientID uuid.UUID) (NotificationPreferencesRow, error) {
	row := q.db.QueryRow(ctx, getNotificationPreferencesByClientID, clientID)
	return scanNotificationPreferencesRow(row)
}

type UpdateNotificationPreferencesParams struct {
	ClientID                   uuid.UUID
	BookingConfirmationEnabled *bool
	RatingInvitationEnabled    *bool
}

const updateNotificationPreferences = `-- name: UpdateNotificationPreferences :one
UPDATE notification_preferences
SET
    booking_confirmation_enabled = COALESCE($2, booking_confirmation_enabled),
    rating_invitation_enabled = COALESCE($3, rating_invitation_enabled)
WHERE client_id = $1
RETURNING
    id,
    client_id,
    booking_confirmation_enabled,
    rating_invitation_enabled,
    reminders_enabled,
    gym_cancellation_enabled`

func (q *Queries) UpdateNotificationPreferences(ctx context.Context, arg UpdateNotificationPreferencesParams) (NotificationPreferencesRow, error) {
	row := q.db.QueryRow(ctx, updateNotificationPreferences, arg.ClientID, arg.BookingConfirmationEnabled, arg.RatingInvitationEnabled)
	return scanNotificationPreferencesRow(row)
}

func scanClientRow(row pgx.Row) (ClientRow, error) {
	var i ClientRow
	err := row.Scan(
		&i.ID,
		&i.FullName,
		&i.Phone,
		&i.BirthDate,
		&i.RiskConsentAccepted,
		&i.CompletedVisitsCount,
		&i.IsLoyalClient,
		&i.LoyaltyDiscount,
		&i.LateCancellationCount,
		&i.NoShowCount,
	)
	return i, err
}

func scanNotificationPreferencesRow(row pgx.Row) (NotificationPreferencesRow, error) {
	var i NotificationPreferencesRow
	err := row.Scan(
		&i.ID,
		&i.ClientID,
		&i.BookingConfirmationEnabled,
		&i.RatingInvitationEnabled,
		&i.RemindersEnabled,
		&i.GymCancellationEnabled,
	)
	return i, err
}

const incrementLateCancellationCount = `-- name: IncrementLateCancellationCount :exec
UPDATE clients
SET late_cancellation_count = late_cancellation_count + 1
WHERE id = $1`

func (q *Queries) IncrementLateCancellationCount(ctx context.Context, id uuid.UUID) error {
	_, err := q.db.Exec(ctx, incrementLateCancellationCount, id)
	return err
}

const incrementCompletedVisits = `-- name: IncrementCompletedVisits :one
UPDATE clients
SET completed_visits_count = completed_visits_count + 1
WHERE id = $1
RETURNING
    id,
    full_name,
    phone,
    birth_date,
    risk_consent_accepted,
    completed_visits_count,
    is_loyal_client,
    loyalty_discount::text AS loyalty_discount,
    late_cancellation_count,
    no_show_count`

func (q *Queries) IncrementCompletedVisits(ctx context.Context, id uuid.UUID) (ClientRow, error) {
	row := q.db.QueryRow(ctx, incrementCompletedVisits, id)
	return scanClientRow(row)
}

const updateClientLoyalty = `-- name: UpdateClientLoyalty :one
UPDATE clients
SET
    is_loyal_client = $2,
    loyalty_discount = $3
WHERE id = $1
RETURNING
    id,
    full_name,
    phone,
    birth_date,
    risk_consent_accepted,
    completed_visits_count,
    is_loyal_client,
    loyalty_discount::text AS loyalty_discount,
    late_cancellation_count,
    no_show_count`

func (q *Queries) UpdateClientLoyalty(ctx context.Context, id uuid.UUID, isLoyalClient bool, loyaltyDiscount *string) (ClientRow, error) {
	row := q.db.QueryRow(ctx, updateClientLoyalty, id, isLoyalClient, loyaltyDiscount)
	return scanClientRow(row)
}
