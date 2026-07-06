package db

import (
	"context"
	"time"

	"github.com/google/uuid"
)

type BookingForRatingRow struct {
	BookingID       uuid.UUID
	ClientID        uuid.UUID
	BookingStatus   string
	SlotID          uuid.UUID
	InstructorID    uuid.UUID
	StartsAt        time.Time
	DurationMinutes int32
}

const getBookingForRating = `-- name: GetBookingForRating :one
SELECT
    b.id AS booking_id,
    b.client_id,
    b.booking_status,
    b.slot_id,
    s.instructor_id,
    s.starts_at,
    s.duration_minutes
FROM bookings b
JOIN training_slots s ON s.id = b.slot_id
WHERE b.id = $1
  AND b.client_id = $2`

func (q *Queries) GetBookingForRating(ctx context.Context, bookingID, clientID uuid.UUID) (BookingForRatingRow, error) {
	row := q.db.QueryRow(ctx, getBookingForRating, bookingID, clientID)
	var item BookingForRatingRow
	err := row.Scan(
		&item.BookingID,
		&item.ClientID,
		&item.BookingStatus,
		&item.SlotID,
		&item.InstructorID,
		&item.StartsAt,
		&item.DurationMinutes,
	)
	return item, err
}

const ratingExistsForBooking = `-- name: RatingExistsForBooking :one
SELECT EXISTS (
    SELECT 1 FROM instructor_ratings WHERE booking_id = $1
) AS exists`

func (q *Queries) RatingExistsForBooking(ctx context.Context, bookingID uuid.UUID) (bool, error) {
	row := q.db.QueryRow(ctx, ratingExistsForBooking, bookingID)
	var exists bool
	err := row.Scan(&exists)
	return exists, err
}

type InsertInstructorRatingParams struct {
	ClientID     uuid.UUID
	InstructorID uuid.UUID
	BookingID    uuid.UUID
	Stars        int32
}

type InstructorRatingRow struct {
	ID           uuid.UUID
	ClientID     uuid.UUID
	InstructorID uuid.UUID
	BookingID    uuid.UUID
	Stars        int32
	RatedAt      time.Time
}

const insertInstructorRating = `-- name: InsertInstructorRating :one
INSERT INTO instructor_ratings (client_id, instructor_id, booking_id, stars)
VALUES ($1, $2, $3, $4)
RETURNING
    id,
    client_id,
    instructor_id,
    booking_id,
    stars,
    rated_at`

func (q *Queries) InsertInstructorRating(ctx context.Context, arg InsertInstructorRatingParams) (InstructorRatingRow, error) {
	row := q.db.QueryRow(ctx, insertInstructorRating, arg.ClientID, arg.InstructorID, arg.BookingID, arg.Stars)
	var item InstructorRatingRow
	err := row.Scan(
		&item.ID,
		&item.ClientID,
		&item.InstructorID,
		&item.BookingID,
		&item.Stars,
		&item.RatedAt,
	)
	return item, err
}

const recalculateInstructorAverageRating = `-- name: RecalculateInstructorAverageRating :exec
UPDATE instructors
SET average_rating = (
    SELECT ROUND(AVG(stars)::numeric, 2)
    FROM instructor_ratings
    WHERE instructor_id = $1
)
WHERE id = $1`

func (q *Queries) RecalculateInstructorAverageRating(ctx context.Context, instructorID uuid.UUID) error {
	_, err := q.db.Exec(ctx, recalculateInstructorAverageRating, instructorID)
	return err
}
