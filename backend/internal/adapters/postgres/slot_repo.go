package postgres

import (
	"context"
	"errors"
	"fmt"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/shopspring/decimal"
	"github.com/vertical-climbing/backend/internal/adapters/postgres/db"
	"github.com/vertical-climbing/backend/internal/domain"
	portslot "github.com/vertical-climbing/backend/internal/ports/slot"
)

type SlotRepository struct {
	pool *pgxpool.Pool
}

func NewSlotRepository(pool *pgxpool.Pool) *SlotRepository {
	return &SlotRepository{pool: pool}
}

func (r *SlotRepository) ListByPeriod(ctx context.Context, filter portslot.ListFilter) ([]portslot.Slot, error) {
	queries := db.New(r.pool)
	rows, err := queries.ListSlotsByPeriod(ctx, filter.From, filter.To)
	if err != nil {
		return nil, fmt.Errorf("list slots: %w", err)
	}

	items := make([]portslot.Slot, 0, len(rows))
	for _, row := range rows {
		slot, err := mapSlotRow(row)
		if err != nil {
			return nil, err
		}
		items = append(items, slot)
	}
	return items, nil
}

func (r *SlotRepository) GetByID(ctx context.Context, id string) (portslot.Slot, error) {
	slotID, err := uuid.Parse(id)
	if err != nil {
		return portslot.Slot{}, domain.NewNotFound("Слот не найден")
	}

	queries := db.New(r.pool)
	row, err := queries.GetSlotByID(ctx, slotID)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return portslot.Slot{}, domain.NewNotFound("Слот не найден")
		}
		return portslot.Slot{}, fmt.Errorf("get slot: %w", err)
	}

	return mapSlotRow(row)
}

func (r *SlotRepository) ListRentalAvailability(ctx context.Context, slotID string) ([]portslot.RentalAvailability, error) {
	id, err := uuid.Parse(slotID)
	if err != nil {
		return nil, domain.NewNotFound("Слот не найден")
	}

	queries := db.New(r.pool)
	rows, err := queries.ListSlotRentalAvailability(ctx, id)
	if err != nil {
		return nil, fmt.Errorf("list slot rental availability: %w", err)
	}

	items := make([]portslot.RentalAvailability, 0, len(rows))
	for _, row := range rows {
		price, err := decimal.NewFromString(row.EquipmentDefaultPrice)
		if err != nil {
			return nil, fmt.Errorf("parse equipment price: %w", err)
		}
		items = append(items, portslot.RentalAvailability{
			ID:                row.ID.String(),
			SlotID:            row.SlotID.String(),
			EquipmentTypeID:   row.EquipmentTypeID.String(),
			AvailableQuantity: row.AvailableQuantity,
			EquipmentCode:     row.EquipmentCode,
			EquipmentName:     row.EquipmentName,
			EquipmentPrice:    price,
		})
	}
	return items, nil
}

func (r *SlotRepository) FindAlternatives(ctx context.Context, filter portslot.AlternativeFilter) ([]portslot.Slot, error) {
	zoneID, err := uuid.Parse(filter.ZoneID)
	if err != nil {
		return nil, domain.NewBadRequest("Некорректный zone_id")
	}
	instructorID, err := uuid.Parse(filter.InstructorID)
	if err != nil {
		return nil, domain.NewBadRequest("Некорректный instructor_id")
	}
	excludeID, err := uuid.Parse(filter.ExcludeID)
	if err != nil {
		return nil, domain.NewBadRequest("Некорректный slot_id")
	}

	queries := db.New(r.pool)
	rows, err := queries.FindAlternativeSlots(ctx, zoneID, instructorID, filter.After, excludeID)
	if err != nil {
		return nil, fmt.Errorf("find alternative slots: %w", err)
	}

	items := make([]portslot.Slot, 0, len(rows))
	for _, row := range rows {
		slot, err := mapSlotRow(row)
		if err != nil {
			return nil, err
		}
		items = append(items, slot)
	}
	return items, nil
}

func mapSlotRow(row db.SlotRow) (portslot.Slot, error) {
	trainingPrice, err := decimal.NewFromString(row.TrainingPrice)
	if err != nil {
		return portslot.Slot{}, fmt.Errorf("parse training price: %w", err)
	}

	var rentalTariff *decimal.Decimal
	if row.RentalTariff != nil && *row.RentalTariff != "" {
		value, err := decimal.NewFromString(*row.RentalTariff)
		if err != nil {
			return portslot.Slot{}, fmt.Errorf("parse rental tariff: %w", err)
		}
		rentalTariff = &value
	}

	var averageRating *decimal.Decimal
	if row.InstructorAverageRating != nil && *row.InstructorAverageRating != "" {
		value, err := decimal.NewFromString(*row.InstructorAverageRating)
		if err != nil {
			return portslot.Slot{}, fmt.Errorf("parse instructor rating: %w", err)
		}
		averageRating = &value
	}

	return portslot.Slot{
		ID:              row.ID.String(),
		StartsAt:        row.StartsAt,
		DurationMinutes: row.DurationMinutes,
		Capacity:        row.Capacity,
		FreeSpots:       row.FreeSpots,
		TrainingPrice:   trainingPrice,
		RentalTariff:    rentalTariff,
		SlotStatus:      row.SlotStatus,
		Address:         row.Address,
		Zone: portslot.Zone{
			ID:           row.ZoneID.String(),
			Name:         row.ZoneName,
			FormatType:   row.ZoneFormatType,
			Difficulty:   row.ZoneDifficulty,
			MaxGroupSize: row.ZoneMaxGroupSize,
		},
		Instructor: portslot.Instructor{
			ID:            row.InstructorID.String(),
			FullName:      row.InstructorFullName,
			AverageRating: averageRating,
		},
		Venue: portslot.GymVenue{
			ID:      row.VenueID.String(),
			Name:    row.VenueName,
			Address: row.VenueAddress,
		},
	}, nil
}
