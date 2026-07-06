package booking

import (
	"context"
	"time"

	"github.com/vertical-climbing/backend/internal/domain"
	portauth "github.com/vertical-climbing/backend/internal/ports/auth"
	portbooking "github.com/vertical-climbing/backend/internal/ports/booking"
	portpush "github.com/vertical-climbing/backend/internal/ports/push"
	portref "github.com/vertical-climbing/backend/internal/ports/reference"
)

type Service struct {
	repo      portbooking.Repository
	reference portref.Repository
	notifier  portpush.Notifier
}

func NewService(repo portbooking.Repository, reference portref.Repository, notifier portpush.Notifier) *Service {
	return &Service{repo: repo, reference: reference, notifier: notifier}
}

func (s *Service) Create(ctx context.Context, input portbooking.CreateInput) (portbooking.Booking, error) {
	clientID, ok := portauth.ClientIDFromContext(ctx)
	if !ok {
		return portbooking.Booking{}, domain.NewUnauthorized("Требуется авторизация")
	}

	if input.SlotID == "" {
		return portbooking.Booking{}, domain.NewBadRequest("Необходимо указать slot_id")
	}

	input.ClientID = clientID
	booking, err := s.repo.Create(ctx, input)
	if err != nil {
		return portbooking.Booking{}, err
	}

	if s.notifier != nil {
		s.notifier.NotifyBookingCreated(ctx, clientID, booking.ID, booking.SlotID)
	}

	return booking, nil
}

func (s *Service) List(ctx context.Context, status string) ([]portbooking.Booking, error) {
	clientID, ok := portauth.ClientIDFromContext(ctx)
	if !ok {
		return nil, domain.NewUnauthorized("Требуется авторизация")
	}

	if status == "" {
		status = domain.BookingStatusBooked
	}

	return s.repo.ListByClient(ctx, portbooking.ListFilter{
		ClientID: clientID,
		Status:   status,
	})
}

func (s *Service) GetByID(ctx context.Context, bookingID string) (portbooking.Booking, error) {
	clientID, ok := portauth.ClientIDFromContext(ctx)
	if !ok {
		return portbooking.Booking{}, domain.NewUnauthorized("Требуется авторизация")
	}

	if bookingID == "" {
		return portbooking.Booking{}, domain.NewNotFound("Запись не найдена")
	}

	return s.repo.GetByIDForClient(ctx, bookingID, clientID)
}

func (s *Service) Cancel(ctx context.Context, bookingID string) (portbooking.Booking, error) {
	clientID, ok := portauth.ClientIDFromContext(ctx)
	if !ok {
		return portbooking.Booking{}, domain.NewUnauthorized("Требуется авторизация")
	}

	if bookingID == "" {
		return portbooking.Booking{}, domain.NewNotFound("Запись не найдена")
	}

	return s.repo.Cancel(ctx, bookingID, clientID)
}

func (s *Service) UpdateRental(ctx context.Context, input portbooking.UpdateRentalInput) (portbooking.Booking, error) {
	clientID, ok := portauth.ClientIDFromContext(ctx)
	if !ok {
		return portbooking.Booking{}, domain.NewUnauthorized("Требуется авторизация")
	}

	if input.BookingID == "" {
		return portbooking.Booking{}, domain.NewNotFound("Запись не найдена")
	}

	input.ClientID = clientID
	return s.repo.UpdateRental(ctx, input)
}

func (s *Service) CancelSlotByGym(ctx context.Context, input portbooking.CancelSlotByGymInput) (portbooking.CancelSlotByGymResult, error) {
	if input.SlotID == "" {
		return portbooking.CancelSlotByGymResult{}, domain.NewNotFound("Слот не найден")
	}
	if input.CancellationReasonID == "" {
		return portbooking.CancelSlotByGymResult{}, domain.NewBadRequest("Необходимо указать cancellation_reason_id")
	}

	result, err := s.repo.CancelSlotByGym(ctx, input)
	if err != nil {
		return portbooking.CancelSlotByGymResult{}, err
	}

	if s.notifier != nil {
		for _, booking := range result.CancelledBookings {
			s.notifier.NotifyGymCancellation(ctx, booking.ClientID, booking.BookingID, result.SlotID)
		}
	}

	return result, nil
}

func (s *Service) FindAlternative(ctx context.Context, cancelledSlotID, bookingID string) (portbooking.FindAlternativeResult, error) {
	clientID, ok := portauth.ClientIDFromContext(ctx)
	if !ok {
		return portbooking.FindAlternativeResult{}, domain.NewUnauthorized("Требуется авторизация")
	}
	if cancelledSlotID == "" {
		return portbooking.FindAlternativeResult{}, domain.NewBadRequest("Необходимо указать cancelled_slot_id")
	}

	return s.repo.FindAlternative(ctx, portbooking.FindAlternativeInput{
		ClientID:        clientID,
		CancelledSlotID: cancelledSlotID,
		BookingID:       bookingID,
	})
}

func (s *Service) CancellationPolicyFor(ctx context.Context, booking portbooking.Booking) (domain.CancellationPolicy, error) {
	config, err := s.reference.GetSystemConfig(ctx)
	if err != nil {
		return domain.CancellationPolicy{}, err
	}

	return domain.BuildCancellationPolicy(domain.CancellationPolicyInput{
		BookingStatus:                booking.BookingStatus,
		SlotStartsAt:                 booking.Slot.StartsAt,
		CancellationForbiddenMinutes: config.CancellationForbiddenMinutes,
		Now:                          time.Now(),
	}), nil
}
