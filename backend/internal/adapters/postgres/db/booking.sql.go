package db

import (
	"context"
	"time"

	"github.com/google/uuid"
)

type EquipmentTypePriceRow struct {
	ID           uuid.UUID
	Code         string
	Name         string
	DefaultPrice string
}

type BookingRow struct {
	ID                        uuid.UUID
	ClientID                  uuid.UUID
	SlotID                    uuid.UUID
	BookingStatus             string
	CreatedAt                 time.Time
	CancelledAt               *time.Time
	UsesOwnEquipment          bool
	RebookingForbidden        bool
	CancellationReasonID      *uuid.UUID
	CancellationReasonCode    *string
	CancellationReasonTitle   *string
	CancellationReasonApology *string
	PaymentID                 uuid.UUID
	PaymentTrainingAmount     string
	PaymentRentalAmount       string
	PaymentDiscountAmount     *string
	PaymentTotalAmount        string
	PaymentStatus             string
	StartsAt                  time.Time
	DurationMinutes           int32
	Capacity                  int32
	FreeSpots                 int32
	TrainingPrice             string
	RentalTariff              *string
	SlotStatus                string
	Address                   string
	ZoneID                    uuid.UUID
	ZoneName                  string
	ZoneFormatType            string
	ZoneDifficulty            string
	ZoneMaxGroupSize          int32
	InstructorID              uuid.UUID
	InstructorFullName        string
	InstructorAverageRating   *string
	VenueID                   uuid.UUID
	VenueName                 string
	VenueAddress              string
}

type BookingRentalLineRow struct {
	ID              uuid.UUID
	BookingID       uuid.UUID
	EquipmentTypeID uuid.UUID
	Quantity        int32
	UnitPrice       string
	EquipmentCode   string
	EquipmentName   string
}

const hasRebookingForbidden = `-- name: HasRebookingForbidden :one
SELECT EXISTS (
    SELECT 1
    FROM bookings
    WHERE client_id = $1
      AND slot_id = $2
      AND rebooking_forbidden = TRUE
) AS forbidden`

func (q *Queries) HasRebookingForbidden(ctx context.Context, clientID, slotID uuid.UUID) (bool, error) {
	row := q.db.QueryRow(ctx, hasRebookingForbidden, clientID, slotID)
	var forbidden bool
	err := row.Scan(&forbidden)
	return forbidden, err
}

const getEquipmentTypePrice = `-- name: GetEquipmentTypePrice :one
SELECT id, code, name, default_price::text
FROM rental_equipment_types
WHERE id = $1`

func (q *Queries) GetEquipmentTypePrice(ctx context.Context, id uuid.UUID) (EquipmentTypePriceRow, error) {
	row := q.db.QueryRow(ctx, getEquipmentTypePrice, id)
	var item EquipmentTypePriceRow
	err := row.Scan(&item.ID, &item.Code, &item.Name, &item.DefaultPrice)
	return item, err
}

const listBookingsByClient = `-- name: ListBookingsByClient :many
SELECT
    b.id,
    b.client_id,
    b.slot_id,
    b.booking_status,
    b.created_at,
    b.cancelled_at,
    b.uses_own_equipment,
    b.rebooking_forbidden,
    b.cancellation_reason_id,
    cr.code AS cancellation_reason_code,
    cr.title AS cancellation_reason_title,
    cr.apology_text AS cancellation_reason_apology,
    p.id AS payment_id,
    p.training_amount::text AS payment_training_amount,
    p.rental_amount::text AS payment_rental_amount,
    p.discount_amount::text AS payment_discount_amount,
    p.total_amount::text AS payment_total_amount,
    p.payment_status,
    s.starts_at,
    s.duration_minutes,
    s.capacity,
    s.free_spots,
    s.training_price::text AS training_price,
    s.rental_tariff::text AS rental_tariff,
    s.slot_status,
    s.address,
    z.id AS zone_id,
    z.name AS zone_name,
    z.format_type AS zone_format_type,
    z.difficulty AS zone_difficulty,
    z.max_group_size AS zone_max_group_size,
    i.id AS instructor_id,
    i.full_name AS instructor_full_name,
    i.average_rating::text AS instructor_average_rating,
    v.id AS venue_id,
    v.name AS venue_name,
    v.address AS venue_address
FROM bookings b
JOIN training_slots s ON s.id = b.slot_id
JOIN training_zones z ON z.id = s.zone_id
JOIN instructors i ON i.id = s.instructor_id
JOIN gym_venues v ON v.id = s.venue_id
JOIN payments p ON p.booking_id = b.id
LEFT JOIN cancellation_reasons cr ON cr.id = b.cancellation_reason_id
WHERE b.client_id = $1
  AND b.booking_status = $2
ORDER BY s.starts_at ASC`

func (q *Queries) ListBookingsByClient(ctx context.Context, clientID uuid.UUID, status string) ([]BookingRow, error) {
	rows, err := q.db.Query(ctx, listBookingsByClient, clientID, status)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	items := make([]BookingRow, 0)
	for rows.Next() {
		item, err := scanBookingRow(rows)
		if err != nil {
			return nil, err
		}
		items = append(items, item)
	}
	return items, rows.Err()
}

const getBookingByIDForClient = `-- name: GetBookingByIDForClient :one
SELECT
    b.id,
    b.client_id,
    b.slot_id,
    b.booking_status,
    b.created_at,
    b.cancelled_at,
    b.uses_own_equipment,
    b.rebooking_forbidden,
    b.cancellation_reason_id,
    cr.code AS cancellation_reason_code,
    cr.title AS cancellation_reason_title,
    cr.apology_text AS cancellation_reason_apology,
    p.id AS payment_id,
    p.training_amount::text AS payment_training_amount,
    p.rental_amount::text AS payment_rental_amount,
    p.discount_amount::text AS payment_discount_amount,
    p.total_amount::text AS payment_total_amount,
    p.payment_status,
    s.starts_at,
    s.duration_minutes,
    s.capacity,
    s.free_spots,
    s.training_price::text AS training_price,
    s.rental_tariff::text AS rental_tariff,
    s.slot_status,
    s.address,
    z.id AS zone_id,
    z.name AS zone_name,
    z.format_type AS zone_format_type,
    z.difficulty AS zone_difficulty,
    z.max_group_size AS zone_max_group_size,
    i.id AS instructor_id,
    i.full_name AS instructor_full_name,
    i.average_rating::text AS instructor_average_rating,
    v.id AS venue_id,
    v.name AS venue_name,
    v.address AS venue_address
FROM bookings b
JOIN training_slots s ON s.id = b.slot_id
JOIN training_zones z ON z.id = s.zone_id
JOIN instructors i ON i.id = s.instructor_id
JOIN gym_venues v ON v.id = s.venue_id
JOIN payments p ON p.booking_id = b.id
LEFT JOIN cancellation_reasons cr ON cr.id = b.cancellation_reason_id
WHERE b.id = $1
  AND b.client_id = $2`

func (q *Queries) GetBookingByIDForClient(ctx context.Context, bookingID, clientID uuid.UUID) (BookingRow, error) {
	row := q.db.QueryRow(ctx, getBookingByIDForClient, bookingID, clientID)
	return scanBookingRow(row)
}

const listBookingRentalLinesByBookingID = `-- name: ListBookingRentalLinesByBookingID :many
SELECT
    brl.id,
    brl.booking_id,
    brl.equipment_type_id,
    brl.quantity,
    brl.unit_price::text AS unit_price,
    ret.code AS equipment_code,
    ret.name AS equipment_name
FROM booking_rental_lines brl
JOIN rental_equipment_types ret ON ret.id = brl.equipment_type_id
WHERE brl.booking_id = $1
ORDER BY ret.code`

func (q *Queries) ListBookingRentalLinesByBookingID(ctx context.Context, bookingID uuid.UUID) ([]BookingRentalLineRow, error) {
	rows, err := q.db.Query(ctx, listBookingRentalLinesByBookingID, bookingID)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	items := make([]BookingRentalLineRow, 0)
	for rows.Next() {
		var item BookingRentalLineRow
		if err := rows.Scan(
			&item.ID,
			&item.BookingID,
			&item.EquipmentTypeID,
			&item.Quantity,
			&item.UnitPrice,
			&item.EquipmentCode,
			&item.EquipmentName,
		); err != nil {
			return nil, err
		}
		items = append(items, item)
	}
	return items, rows.Err()
}

type scannable interface {
	Scan(dest ...any) error
}

func scanBookingRow(row scannable) (BookingRow, error) {
	var item BookingRow
	err := row.Scan(
		&item.ID,
		&item.ClientID,
		&item.SlotID,
		&item.BookingStatus,
		&item.CreatedAt,
		&item.CancelledAt,
		&item.UsesOwnEquipment,
		&item.RebookingForbidden,
		&item.CancellationReasonID,
		&item.CancellationReasonCode,
		&item.CancellationReasonTitle,
		&item.CancellationReasonApology,
		&item.PaymentID,
		&item.PaymentTrainingAmount,
		&item.PaymentRentalAmount,
		&item.PaymentDiscountAmount,
		&item.PaymentTotalAmount,
		&item.PaymentStatus,
		&item.StartsAt,
		&item.DurationMinutes,
		&item.Capacity,
		&item.FreeSpots,
		&item.TrainingPrice,
		&item.RentalTariff,
		&item.SlotStatus,
		&item.Address,
		&item.ZoneID,
		&item.ZoneName,
		&item.ZoneFormatType,
		&item.ZoneDifficulty,
		&item.ZoneMaxGroupSize,
		&item.InstructorID,
		&item.InstructorFullName,
		&item.InstructorAverageRating,
		&item.VenueID,
		&item.VenueName,
		&item.VenueAddress,
	)
	return item, err
}

type ActiveBookingRow struct {
	ID            uuid.UUID
	ClientID      uuid.UUID
	PaymentStatus string
}

const listActiveBookingsBySlotID = `-- name: ListActiveBookingsBySlotID :many
SELECT
    b.id,
    b.client_id,
    p.payment_status
FROM bookings b
JOIN payments p ON p.booking_id = b.id
WHERE b.slot_id = $1
  AND b.booking_status = 'booked'`

func (q *Queries) ListActiveBookingsBySlotID(ctx context.Context, slotID uuid.UUID) ([]ActiveBookingRow, error) {
	rows, err := q.db.Query(ctx, listActiveBookingsBySlotID, slotID)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	items := make([]ActiveBookingRow, 0)
	for rows.Next() {
		var item ActiveBookingRow
		if err := rows.Scan(&item.ID, &item.ClientID, &item.PaymentStatus); err != nil {
			return nil, err
		}
		items = append(items, item)
	}
	return items, rows.Err()
}

const cancellationReasonExists = `-- name: CancellationReasonExists :one
SELECT EXISTS (
    SELECT 1 FROM cancellation_reasons WHERE id = $1
) AS exists`

func (q *Queries) CancellationReasonExists(ctx context.Context, id uuid.UUID) (bool, error) {
	row := q.db.QueryRow(ctx, cancellationReasonExists, id)
	var exists bool
	err := row.Scan(&exists)
	return exists, err
}
