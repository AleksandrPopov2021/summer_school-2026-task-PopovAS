-- name: GetClientByID :one
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
WHERE id = $1;

-- name: GetClientByPhone :one
SELECT id
FROM clients
WHERE phone = $1;

-- name: CreateClient :one
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
    no_show_count;

-- name: UpdateClientRiskConsent :one
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
    no_show_count;

-- name: CreateNotificationPreferences :one
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
    gym_cancellation_enabled;

-- name: GetNotificationPreferencesByClientID :one
SELECT
    id,
    client_id,
    booking_confirmation_enabled,
    rating_invitation_enabled,
    reminders_enabled,
    gym_cancellation_enabled
FROM notification_preferences
WHERE client_id = $1;

-- name: UpdateNotificationPreferences :one
UPDATE notification_preferences
SET
    booking_confirmation_enabled = COALESCE(sqlc.narg('booking_confirmation_enabled'), booking_confirmation_enabled),
    rating_invitation_enabled = COALESCE(sqlc.narg('rating_invitation_enabled'), rating_invitation_enabled)
WHERE client_id = $1
RETURNING
    id,
    client_id,
    booking_confirmation_enabled,
    rating_invitation_enabled,
    reminders_enabled,
    gym_cancellation_enabled;

-- name: IncrementLateCancellationCount :exec
UPDATE clients
SET late_cancellation_count = late_cancellation_count + 1
WHERE id = $1;

-- name: IncrementCompletedVisits :one
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
    no_show_count;

-- name: UpdateClientLoyalty :one
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
    no_show_count;
