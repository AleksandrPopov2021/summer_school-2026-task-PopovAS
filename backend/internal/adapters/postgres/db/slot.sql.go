package db

import (
	"context"
	"time"

	"github.com/google/uuid"
)

type SlotRow struct {
	ID                      uuid.UUID
	StartsAt                time.Time
	DurationMinutes         int32
	Capacity                int32
	FreeSpots               int32
	TrainingPrice           string
	RentalTariff            *string
	SlotStatus              string
	Address                 string
	ZoneID                  uuid.UUID
	ZoneName                string
	ZoneFormatType          string
	ZoneDifficulty          string
	ZoneMaxGroupSize        int32
	InstructorID            uuid.UUID
	InstructorFullName      string
	InstructorAverageRating *string
	VenueID                 uuid.UUID
	VenueName               string
	VenueAddress            string
}

type SlotRentalAvailabilityRow struct {
	ID                     uuid.UUID
	SlotID                 uuid.UUID
	EquipmentTypeID        uuid.UUID
	AvailableQuantity      int32
	EquipmentCode          string
	EquipmentName          string
	EquipmentDefaultPrice  string
}

const listSlotsByPeriod = `-- name: ListSlotsByPeriod :many
SELECT
    s.id,
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
FROM training_slots s
JOIN training_zones z ON z.id = s.zone_id
JOIN instructors i ON i.id = s.instructor_id
JOIN gym_venues v ON v.id = s.venue_id
WHERE s.starts_at >= $1
  AND s.starts_at < $2
ORDER BY s.starts_at ASC`

func (q *Queries) ListSlotsByPeriod(ctx context.Context, from, to time.Time) ([]SlotRow, error) {
	rows, err := q.db.Query(ctx, listSlotsByPeriod, from, to)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var items []SlotRow
	for rows.Next() {
		var row SlotRow
		if err := rows.Scan(
			&row.ID,
			&row.StartsAt,
			&row.DurationMinutes,
			&row.Capacity,
			&row.FreeSpots,
			&row.TrainingPrice,
			&row.RentalTariff,
			&row.SlotStatus,
			&row.Address,
			&row.ZoneID,
			&row.ZoneName,
			&row.ZoneFormatType,
			&row.ZoneDifficulty,
			&row.ZoneMaxGroupSize,
			&row.InstructorID,
			&row.InstructorFullName,
			&row.InstructorAverageRating,
			&row.VenueID,
			&row.VenueName,
			&row.VenueAddress,
		); err != nil {
			return nil, err
		}
		items = append(items, row)
	}
	return items, rows.Err()
}

const getSlotByID = `-- name: GetSlotByID :one
SELECT
    s.id,
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
FROM training_slots s
JOIN training_zones z ON z.id = s.zone_id
JOIN instructors i ON i.id = s.instructor_id
JOIN gym_venues v ON v.id = s.venue_id
WHERE s.id = $1`

func (q *Queries) GetSlotByID(ctx context.Context, id uuid.UUID) (SlotRow, error) {
	row := q.db.QueryRow(ctx, getSlotByID, id)
	var item SlotRow
	err := row.Scan(
		&item.ID,
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

const listSlotRentalAvailability = `-- name: ListSlotRentalAvailability :many
SELECT
    sra.id,
    sra.slot_id,
    sra.equipment_type_id,
    sra.available_quantity,
    ret.code AS equipment_code,
    ret.name AS equipment_name,
    ret.default_price::text AS equipment_default_price
FROM slot_rental_availability sra
JOIN rental_equipment_types ret ON ret.id = sra.equipment_type_id
WHERE sra.slot_id = $1
ORDER BY ret.code`

func (q *Queries) ListSlotRentalAvailability(ctx context.Context, slotID uuid.UUID) ([]SlotRentalAvailabilityRow, error) {
	rows, err := q.db.Query(ctx, listSlotRentalAvailability, slotID)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var items []SlotRentalAvailabilityRow
	for rows.Next() {
		var row SlotRentalAvailabilityRow
		if err := rows.Scan(
			&row.ID,
			&row.SlotID,
			&row.EquipmentTypeID,
			&row.AvailableQuantity,
			&row.EquipmentCode,
			&row.EquipmentName,
			&row.EquipmentDefaultPrice,
		); err != nil {
			return nil, err
		}
		items = append(items, row)
	}
	return items, rows.Err()
}

const findAlternativeSlots = `-- name: FindAlternativeSlots :many
SELECT
    s.id,
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
FROM training_slots s
JOIN training_zones z ON z.id = s.zone_id
JOIN instructors i ON i.id = s.instructor_id
JOIN gym_venues v ON v.id = s.venue_id
WHERE s.zone_id = $1
  AND s.instructor_id = $2
  AND s.slot_status = 'active'
  AND s.free_spots > 0
  AND s.starts_at > $3
  AND s.id != $4
ORDER BY s.starts_at ASC
LIMIT 20`

func (q *Queries) FindAlternativeSlots(
	ctx context.Context,
	zoneID, instructorID uuid.UUID,
	after time.Time,
	excludeID uuid.UUID,
) ([]SlotRow, error) {
	rows, err := q.db.Query(ctx, findAlternativeSlots, zoneID, instructorID, after, excludeID)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	items := make([]SlotRow, 0)
	for rows.Next() {
		var item SlotRow
		if err := rows.Scan(
			&item.ID,
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
		); err != nil {
			return nil, err
		}
		items = append(items, item)
	}
	return items, rows.Err()
}

type ClearanceRow struct {
	ID           uuid.UUID
	ClientID     uuid.UUID
	InstructorID *uuid.UUID
	IsGranted    bool
	GrantedAt    *time.Time
}

const listClearancesByClientID = `-- name: ListClearancesByClientID :many
SELECT
    id,
    client_id,
    instructor_id,
    is_granted,
    granted_at
FROM instructor_clearances
WHERE client_id = $1
ORDER BY granted_at DESC NULLS LAST`

func (q *Queries) ListClearancesByClientID(ctx context.Context, clientID uuid.UUID) ([]ClearanceRow, error) {
	rows, err := q.db.Query(ctx, listClearancesByClientID, clientID)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var items []ClearanceRow
	for rows.Next() {
		var row ClearanceRow
		if err := rows.Scan(
			&row.ID,
			&row.ClientID,
			&row.InstructorID,
			&row.IsGranted,
			&row.GrantedAt,
		); err != nil {
			return nil, err
		}
		items = append(items, row)
	}
	return items, rows.Err()
}

const clientHasGrantedClearance = `-- name: ClientHasGrantedClearance :one
SELECT EXISTS (
    SELECT 1
    FROM instructor_clearances
    WHERE client_id = $1
      AND is_granted = TRUE
) AS has_clearance`

func (q *Queries) ClientHasGrantedClearance(ctx context.Context, clientID uuid.UUID) (bool, error) {
	row := q.db.QueryRow(ctx, clientHasGrantedClearance, clientID)
	var hasClearance bool
	err := row.Scan(&hasClearance)
	return hasClearance, err
}
