package clearance

import (
	"context"

	"github.com/vertical-climbing/backend/internal/domain"
	portauth "github.com/vertical-climbing/backend/internal/ports/auth"
	portclearance "github.com/vertical-climbing/backend/internal/ports/clearance"
)

type Service struct {
	repo portclearance.Repository
}

func NewService(repo portclearance.Repository) *Service {
	return &Service{repo: repo}
}

func (s *Service) ListForCurrentClient(ctx context.Context) ([]portclearance.Clearance, error) {
	clientID, ok := portauth.ClientIDFromContext(ctx)
	if !ok {
		return nil, domain.NewUnauthorized("Требуется авторизация")
	}
	return s.repo.ListByClientID(ctx, clientID)
}
