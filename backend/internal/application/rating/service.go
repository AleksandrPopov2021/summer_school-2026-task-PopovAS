package rating

import (
	"context"

	"github.com/vertical-climbing/backend/internal/domain"
	portauth "github.com/vertical-climbing/backend/internal/ports/auth"
	portrating "github.com/vertical-climbing/backend/internal/ports/rating"
)

type Service struct {
	repo portrating.Repository
}

func NewService(repo portrating.Repository) *Service {
	return &Service{repo: repo}
}

func (s *Service) Create(ctx context.Context, bookingID string, stars int32) (portrating.Rating, error) {
	clientID, ok := portauth.ClientIDFromContext(ctx)
	if !ok {
		return portrating.Rating{}, domain.NewUnauthorized("Требуется авторизация")
	}
	if bookingID == "" {
		return portrating.Rating{}, domain.NewBadRequest("Необходимо указать booking_id")
	}

	return s.repo.Create(ctx, clientID, bookingID, stars)
}
