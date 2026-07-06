package slot

import (
	"context"
	"time"

	"github.com/vertical-climbing/backend/internal/domain"
	portauth "github.com/vertical-climbing/backend/internal/ports/auth"
	portclearance "github.com/vertical-climbing/backend/internal/ports/clearance"
	portref "github.com/vertical-climbing/backend/internal/ports/reference"
	portslot "github.com/vertical-climbing/backend/internal/ports/slot"
)

type Summary struct {
	Slot         portslot.Slot
	Availability domain.BookingAvailability
}

type Service struct {
	slots      portslot.Repository
	reference  portref.Repository
	clearances portclearance.Repository
	now        func() time.Time
}

func NewService(
	slots portslot.Repository,
	reference portref.Repository,
	clearances portclearance.Repository,
) *Service {
	return &Service{
		slots:      slots,
		reference:  reference,
		clearances: clearances,
		now:        time.Now,
	}
}

func (s *Service) List(
	ctx context.Context,
	fromValue, toValue, dateValue string,
) ([]Summary, error) {
	from, to, err := domain.ParseSlotPeriod(fromValue, toValue, dateValue, s.now())
	if err != nil {
		return nil, err
	}

	config, err := s.reference.GetSystemConfig(ctx)
	if err != nil {
		return nil, err
	}

	slots, err := s.slots.ListByPeriod(ctx, portslot.ListFilter{From: from, To: to})
	if err != nil {
		return nil, err
	}

	hasClearance, authenticated := s.resolveClearance(ctx)
	return s.buildSummaries(slots, config.BookingCutoffMinutes, hasClearance, authenticated), nil
}

func (s *Service) GetDetail(ctx context.Context, slotID string) (portslot.SlotDetail, domain.BookingAvailability, error) {
	slot, err := s.slots.GetByID(ctx, slotID)
	if err != nil {
		return portslot.SlotDetail{}, domain.BookingAvailability{}, err
	}

	rental, err := s.slots.ListRentalAvailability(ctx, slotID)
	if err != nil {
		return portslot.SlotDetail{}, domain.BookingAvailability{}, err
	}

	config, err := s.reference.GetSystemConfig(ctx)
	if err != nil {
		return portslot.SlotDetail{}, domain.BookingAvailability{}, err
	}

	hasClearance, authenticated := s.resolveClearance(ctx)
	availability := s.buildAvailability(slot, config.BookingCutoffMinutes, hasClearance, authenticated)

	return portslot.SlotDetail{
		Slot:               slot,
		RentalAvailability: rental,
	}, availability, nil
}

func (s *Service) GetRentalAvailability(ctx context.Context, slotID string) ([]portslot.RentalAvailability, error) {
	if _, err := s.slots.GetByID(ctx, slotID); err != nil {
		return nil, err
	}
	return s.slots.ListRentalAvailability(ctx, slotID)
}

func (s *Service) resolveClearance(ctx context.Context) (hasClearance bool, authenticated bool) {
	clientID, ok := portauth.ClientIDFromContext(ctx)
	if !ok {
		return false, false
	}

	granted, err := s.clearances.HasGrantedClearance(ctx, clientID)
	if err != nil {
		return false, true
	}
	return granted, true
}

func (s *Service) buildSummaries(
	slots []portslot.Slot,
	cutoffMinutes int32,
	hasClearance bool,
	authenticated bool,
) []Summary {
	summaries := make([]Summary, 0, len(slots))
	for _, slot := range slots {
		summaries = append(summaries, Summary{
			Slot: slot,
			Availability: s.buildAvailability(slot, cutoffMinutes, hasClearance, authenticated),
		})
	}
	return summaries
}

func (s *Service) buildAvailability(
	slot portslot.Slot,
	cutoffMinutes int32,
	hasClearance bool,
	authenticated bool,
) domain.BookingAvailability {
	return domain.BuildBookingAvailability(domain.AvailabilityInput{
		SlotStatus:             slot.SlotStatus,
		FormatType:             slot.Zone.FormatType,
		FreeSpots:              slot.FreeSpots,
		StartsAt:               slot.StartsAt,
		BookingCutoffMinutes:   cutoffMinutes,
		HasClearance:           hasClearance,
		HasAuthenticatedClient: authenticated,
		Now:                    s.now(),
	})
}
