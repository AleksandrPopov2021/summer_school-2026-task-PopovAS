package reference

import (
	"context"

	"github.com/shopspring/decimal"
)

type SystemConfig struct {
	ReminderHoursBefore          int32
	VisitsForLoyalty             int32
	ViolationsForSanctions       int32
	BookingCutoffMinutes         int32
	CancellationForbiddenMinutes int32
}

type RentalEquipmentType struct {
	ID           string
	Code         string
	Name         string
	DefaultPrice decimal.Decimal
}

type Repository interface {
	GetSystemConfig(ctx context.Context) (SystemConfig, error)
	ListRentalEquipmentTypes(ctx context.Context) ([]RentalEquipmentType, error)
}
