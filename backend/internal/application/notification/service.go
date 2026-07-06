package notification

import (
	"context"

	"github.com/vertical-climbing/backend/internal/domain"
	portnotification "github.com/vertical-climbing/backend/internal/ports/notification"
)

type Service struct {
	repo portnotification.Repository
}

func NewService(repo portnotification.Repository) *Service {
	return &Service{repo: repo}
}

func (s *Service) Get(ctx context.Context, clientID string) (portnotification.Preferences, error) {
	if clientID == "" {
		return portnotification.Preferences{}, domain.NewUnauthorized("Требуется авторизация")
	}
	return s.repo.GetByClientID(ctx, clientID)
}

func (s *Service) Update(
	ctx context.Context,
	clientID string,
	bookingConfirmation *bool,
	ratingInvitation *bool,
	reminders *bool,
	gymCancellation *bool,
) (portnotification.Preferences, error) {
	if clientID == "" {
		return portnotification.Preferences{}, domain.NewUnauthorized("Требуется авторизация")
	}
	if err := domain.ValidateNotificationPreferencesUpdate(
		bookingConfirmation,
		ratingInvitation,
		reminders,
		gymCancellation,
	); err != nil {
		return portnotification.Preferences{}, err
	}

	return s.repo.Update(ctx, clientID, portnotification.UpdateInput{
		BookingConfirmationEnabled: bookingConfirmation,
		RatingInvitationEnabled:    ratingInvitation,
	})
}
