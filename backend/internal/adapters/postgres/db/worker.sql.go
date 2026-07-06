package db

import (
	"context"
	"time"

	"github.com/google/uuid"
)

type DevicePushTokenRow struct {
	Token    string
	Platform string
}

const upsertDevicePushToken = `-- name: UpsertDevicePushToken :exec
INSERT INTO device_push_tokens (client_id, token, platform, updated_at)
VALUES ($1, $2, $3, NOW())
ON CONFLICT (client_id, platform) DO UPDATE
SET token = EXCLUDED.token,
    updated_at = NOW()`

func (q *Queries) UpsertDevicePushToken(ctx context.Context, clientID uuid.UUID, token, platform string) error {
	_, err := q.db.Exec(ctx, upsertDevicePushToken, clientID, token, platform)
	return err
}

const listDevicePushTokensByClientID = `-- name: ListDevicePushTokensByClientID :many
SELECT token, platform
FROM device_push_tokens
WHERE client_id = $1`

func (q *Queries) ListDevicePushTokensByClientID(ctx context.Context, clientID uuid.UUID) ([]DevicePushTokenRow, error) {
	rows, err := q.db.Query(ctx, listDevicePushTokensByClientID, clientID)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	items := make([]DevicePushTokenRow, 0)
	for rows.Next() {
		var item DevicePushTokenRow
		if err := rows.Scan(&item.Token, &item.Platform); err != nil {
			return nil, err
		}
		items = append(items, item)
	}
	return items, rows.Err()
}

type ReminderTargetRow struct {
	BookingID uuid.UUID
	ClientID  uuid.UUID
	SlotID    uuid.UUID
}

const listBookingsForDayBeforeReminder = `-- name: ListBookingsForDayBeforeReminder :many
SELECT
    b.id AS booking_id,
    b.client_id,
    b.slot_id
FROM bookings b
JOIN training_slots s ON s.id = b.slot_id
WHERE b.booking_status = 'booked'
  AND s.starts_at >= $1::timestamptz + interval '24 hours' - interval '15 minutes'
  AND s.starts_at <= $1::timestamptz + interval '24 hours' + interval '15 minutes'
  AND NOT EXISTS (
      SELECT 1
      FROM sent_push_notifications spn
      WHERE spn.booking_id = b.id
        AND spn.notification_type = 'reminder_day_before'
  )`

func (q *Queries) ListBookingsForDayBeforeReminder(ctx context.Context, now time.Time) ([]ReminderTargetRow, error) {
	rows, err := q.db.Query(ctx, listBookingsForDayBeforeReminder, now)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	return scanReminderTargetRows(rows)
}

type ListBookingsForHoursBeforeReminderParams struct {
	Now         time.Time
	HoursBefore int32
}

const listBookingsForHoursBeforeReminder = `-- name: ListBookingsForHoursBeforeReminder :many
SELECT
    b.id AS booking_id,
    b.client_id,
    b.slot_id
FROM bookings b
JOIN training_slots s ON s.id = b.slot_id
WHERE b.booking_status = 'booked'
  AND s.starts_at >= $1::timestamptz + ($2::text || ' hours')::interval - interval '15 minutes'
  AND s.starts_at <= $1::timestamptz + ($2::text || ' hours')::interval + interval '15 minutes'
  AND NOT EXISTS (
      SELECT 1
      FROM sent_push_notifications spn
      WHERE spn.booking_id = b.id
        AND spn.notification_type = 'reminder_hours_before'
  )`

func (q *Queries) ListBookingsForHoursBeforeReminder(ctx context.Context, arg ListBookingsForHoursBeforeReminderParams) ([]ReminderTargetRow, error) {
	rows, err := q.db.Query(ctx, listBookingsForHoursBeforeReminder, arg.Now, arg.HoursBefore)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	return scanReminderTargetRows(rows)
}

type CompleteTargetRow struct {
	BookingID uuid.UUID
	ClientID  uuid.UUID
	SlotID    uuid.UUID
}

const listBookingsToComplete = `-- name: ListBookingsToComplete :many
SELECT
    b.id AS booking_id,
    b.client_id,
    b.slot_id
FROM bookings b
JOIN training_slots s ON s.id = b.slot_id
WHERE b.booking_status = 'booked'
  AND s.starts_at + (s.duration_minutes || ' minutes')::interval <= $1::timestamptz`

func (q *Queries) ListBookingsToComplete(ctx context.Context, now time.Time) ([]CompleteTargetRow, error) {
	rows, err := q.db.Query(ctx, listBookingsToComplete, now)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	items := make([]CompleteTargetRow, 0)
	for rows.Next() {
		var item CompleteTargetRow
		if err := rows.Scan(&item.BookingID, &item.ClientID, &item.SlotID); err != nil {
			return nil, err
		}
		items = append(items, item)
	}
	return items, rows.Err()
}

const completeBooking = `-- name: CompleteBooking :execrows
UPDATE bookings
SET booking_status = 'completed'
WHERE id = $1
  AND booking_status = 'booked'`

func (q *Queries) CompleteBooking(ctx context.Context, bookingID uuid.UUID) (int64, error) {
	result, err := q.db.Exec(ctx, completeBooking, bookingID)
	if err != nil {
		return 0, err
	}
	return result.RowsAffected(), nil
}

const recordSentPushNotification = `-- name: RecordSentPushNotification :exec
INSERT INTO sent_push_notifications (booking_id, notification_type)
VALUES ($1, $2)
ON CONFLICT (booking_id, notification_type) DO NOTHING`

func (q *Queries) RecordSentPushNotification(ctx context.Context, bookingID uuid.UUID, notificationType string) error {
	_, err := q.db.Exec(ctx, recordSentPushNotification, bookingID, notificationType)
	return err
}

const hasSentPushNotification = `-- name: HasSentPushNotification :one
SELECT EXISTS (
    SELECT 1
    FROM sent_push_notifications
    WHERE booking_id = $1
      AND notification_type = $2
) AS sent`

func (q *Queries) HasSentPushNotification(ctx context.Context, bookingID uuid.UUID, notificationType string) (bool, error) {
	row := q.db.QueryRow(ctx, hasSentPushNotification, bookingID, notificationType)
	var sent bool
	err := row.Scan(&sent)
	return sent, err
}

func scanReminderTargetRows(rows interface {
	Next() bool
	Scan(dest ...any) error
	Err() error
}) ([]ReminderTargetRow, error) {
	items := make([]ReminderTargetRow, 0)
	for rows.Next() {
		var item ReminderTargetRow
		if err := rows.Scan(&item.BookingID, &item.ClientID, &item.SlotID); err != nil {
			return nil, err
		}
		items = append(items, item)
	}
	return items, rows.Err()
}
