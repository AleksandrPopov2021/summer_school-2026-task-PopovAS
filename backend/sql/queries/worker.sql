-- name: ListBookingsForDayBeforeReminder :many
SELECT
    b.id AS booking_id,
    b.client_id,
    b.slot_id
FROM bookings b
JOIN training_slots s ON s.id = b.slot_id
WHERE b.booking_status = 'booked'
  AND s.starts_at >= sqlc.arg(now)::timestamptz + interval '24 hours' - interval '15 minutes'
  AND s.starts_at <= sqlc.arg(now)::timestamptz + interval '24 hours' + interval '15 minutes'
  AND NOT EXISTS (
      SELECT 1
      FROM sent_push_notifications spn
      WHERE spn.booking_id = b.id
        AND spn.notification_type = 'reminder_day_before'
  );

-- name: ListBookingsForHoursBeforeReminder :many
SELECT
    b.id AS booking_id,
    b.client_id,
    b.slot_id
FROM bookings b
JOIN training_slots s ON s.id = b.slot_id
WHERE b.booking_status = 'booked'
  AND s.starts_at >= sqlc.arg(now)::timestamptz + (sqlc.arg(hours_before)::text || ' hours')::interval - interval '15 minutes'
  AND s.starts_at <= sqlc.arg(now)::timestamptz + (sqlc.arg(hours_before)::text || ' hours')::interval + interval '15 minutes'
  AND NOT EXISTS (
      SELECT 1
      FROM sent_push_notifications spn
      WHERE spn.booking_id = b.id
        AND spn.notification_type = 'reminder_hours_before'
  );

-- name: ListBookingsToComplete :many
SELECT
    b.id AS booking_id,
    b.client_id,
    b.slot_id
FROM bookings b
JOIN training_slots s ON s.id = b.slot_id
WHERE b.booking_status = 'booked'
  AND s.starts_at + (s.duration_minutes || ' minutes')::interval <= sqlc.arg(now)::timestamptz;

-- name: CompleteBooking :execrows
UPDATE bookings
SET booking_status = 'completed'
WHERE id = $1
  AND booking_status = 'booked';

-- name: RecordSentPushNotification :exec
INSERT INTO sent_push_notifications (booking_id, notification_type)
VALUES ($1, $2)
ON CONFLICT (booking_id, notification_type) DO NOTHING;

-- name: HasSentPushNotification :one
SELECT EXISTS (
    SELECT 1
    FROM sent_push_notifications
    WHERE booking_id = $1
      AND notification_type = $2
) AS sent;
