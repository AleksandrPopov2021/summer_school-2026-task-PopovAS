package booking

import (
	"context"
	"time"

	"github.com/shopspring/decimal"
	portslot "github.com/vertical-climbing/backend/internal/ports/slot"
)

type RentalLineInput struct {
	EquipmentTypeID string
	Quantity        int32
}

type RentalLine struct {
	ID              string
	BookingID       string
	EquipmentTypeID string
	Quantity        int32
	UnitPrice       decimal.Decimal
	EquipmentCode   string
	EquipmentName   string
}

type Payment struct {
	ID             string
	BookingID      string
	TrainingAmount decimal.Decimal
	RentalAmount   decimal.Decimal
	DiscountAmount *decimal.Decimal
	TotalAmount    decimal.Decimal
	PaymentStatus  string
}

type CancellationReason struct {
	ID          string
	Code        string
	Title       string
	ApologyText string
}

type Booking struct {
	ID                 string
	ClientID           string
	SlotID             string
	BookingStatus      string
	CreatedAt          time.Time
	CancelledAt        *time.Time
	UsesOwnEquipment   bool
	RebookingForbidden bool
	CancellationReason *CancellationReason
	RentalLines        []RentalLine
	Payment            Payment
	Slot               portslot.Slot
}

type CreateInput struct {
	ClientID         string
	SlotID           string
	UsesOwnEquipment bool
	RentalLines      []RentalLineInput
}

type UpdateRentalInput struct {
	ClientID         string
	BookingID        string
	UsesOwnEquipment bool
	RentalLines      []RentalLineInput
}

type ListFilter struct {
	ClientID string
	Status   string
}

type CancelSlotByGymInput struct {
	SlotID               string
	CancellationReasonID string
}

type CancelSlotByGymResult struct {
	SlotID                 string
	CancelledBookingsCount int
	CancelledBookings      []CancelledBooking
}

type CancelledBooking struct {
	BookingID string
	ClientID  string
}

type FindAlternativeInput struct {
	ClientID        string
	CancelledSlotID string
	BookingID       string
}

type FindAlternativeResult struct {
	Found bool
	Slot  *portslot.Slot
}

type Repository interface {
	Create(ctx context.Context, input CreateInput) (Booking, error)
	ListByClient(ctx context.Context, filter ListFilter) ([]Booking, error)
	GetByIDForClient(ctx context.Context, bookingID, clientID string) (Booking, error)
	Cancel(ctx context.Context, bookingID, clientID string) (Booking, error)
	UpdateRental(ctx context.Context, input UpdateRentalInput) (Booking, error)
	CancelSlotByGym(ctx context.Context, input CancelSlotByGymInput) (CancelSlotByGymResult, error)
	FindAlternative(ctx context.Context, input FindAlternativeInput) (FindAlternativeResult, error)
}
