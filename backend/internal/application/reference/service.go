package reference

import (
	"context"

	port "github.com/vertical-climbing/backend/internal/ports/reference"
)

type Service struct {
	repo port.Repository
}

func NewService(repo port.Repository) *Service {
	return &Service{repo: repo}
}

func (s *Service) GetSystemConfig(ctx context.Context) (port.SystemConfig, error) {
	return s.repo.GetSystemConfig(ctx)
}

func (s *Service) ListRentalEquipmentTypes(ctx context.Context) ([]port.RentalEquipmentType, error) {
	return s.repo.ListRentalEquipmentTypes(ctx)
}
