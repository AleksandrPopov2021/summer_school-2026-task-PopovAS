package slot

import (
	"context"
	"time"

	"github.com/shopspring/decimal"
)

type Zone struct {
	ID           string
	Name         string
	FormatType   string
	Difficulty   string
	MaxGroupSize int32
}

type Instructor struct {
	ID            string
	FullName      string
	AverageRating *decimal.Decimal
}

type GymVenue struct {
	ID      string
	Name    string
	Address string
}

type RentalAvailability struct {
	ID                string
	SlotID            string
	EquipmentTypeID   string
	AvailableQuantity int32
	EquipmentCode     string
	EquipmentName     string
	EquipmentPrice    decimal.Decimal
}

type Slot struct {
	ID              string
	StartsAt        time.Time
	DurationMinutes int32
	Capacity        int32
	FreeSpots       int32
	TrainingPrice   decimal.Decimal
	RentalTariff    *decimal.Decimal
	SlotStatus      string
	Address         string
	Zone            Zone
	Instructor      Instructor
	Venue           GymVenue
}

type SlotDetail struct {
	Slot
	RentalAvailability []RentalAvailability
}

type ListFilter struct {
	From time.Time
	To   time.Time
}

type AlternativeFilter struct {
	ZoneID       string
	InstructorID string
	After        time.Time
	ExcludeID    string
}

type Repository interface {
	ListByPeriod(ctx context.Context, filter ListFilter) ([]Slot, error)
	GetByID(ctx context.Context, id string) (Slot, error)
	ListRentalAvailability(ctx context.Context, slotID string) ([]RentalAvailability, error)
	FindAlternatives(ctx context.Context, filter AlternativeFilter) ([]Slot, error)
}
