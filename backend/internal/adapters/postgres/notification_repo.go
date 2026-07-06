package postgres

import (
	"context"
	"errors"
	"fmt"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/vertical-climbing/backend/internal/adapters/postgres/db"
	"github.com/vertical-climbing/backend/internal/domain"
	portnotification "github.com/vertical-climbing/backend/internal/ports/notification"
)

type NotificationRepository struct {
	pool *pgxpool.Pool
}

func NewNotificationRepository(pool *pgxpool.Pool) *NotificationRepository {
	return &NotificationRepository{pool: pool}
}

func (r *NotificationRepository) GetByClientID(ctx context.Context, clientID string) (portnotification.Preferences, error) {
	id, err := uuid.Parse(clientID)
	if err != nil {
		return portnotification.Preferences{}, domain.NewNotFound("Настройки уведомлений не найдены")
	}

	queries := db.New(r.pool)
	row, err := queries.GetNotificationPreferencesByClientID(ctx, id)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return portnotification.Preferences{}, domain.NewNotFound("Настройки уведомлений не найдены")
		}
		return portnotification.Preferences{}, fmt.Errorf("get notification preferences: %w", err)
	}

	return mapNotificationRow(row), nil
}

func (r *NotificationRepository) Update(
	ctx context.Context,
	clientID string,
	input portnotification.UpdateInput,
) (portnotification.Preferences, error) {
	id, err := uuid.Parse(clientID)
	if err != nil {
		return portnotification.Preferences{}, domain.NewNotFound("Настройки уведомлений не найдены")
	}

	queries := db.New(r.pool)
	row, err := queries.UpdateNotificationPreferences(ctx, db.UpdateNotificationPreferencesParams{
		ClientID:                   id,
		BookingConfirmationEnabled: input.BookingConfirmationEnabled,
		RatingInvitationEnabled:    input.RatingInvitationEnabled,
	})
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return portnotification.Preferences{}, domain.NewNotFound("Настройки уведомлений не найдены")
		}
		return portnotification.Preferences{}, fmt.Errorf("update notification preferences: %w", err)
	}

	return mapNotificationRow(row), nil
}

func mapNotificationRow(row db.NotificationPreferencesRow) portnotification.Preferences {
	return portnotification.Preferences{
		ID:                         row.ID.String(),
		ClientID:                   row.ClientID.String(),
		BookingConfirmationEnabled: row.BookingConfirmationEnabled,
		RatingInvitationEnabled:    row.RatingInvitationEnabled,
		RemindersEnabled:           row.RemindersEnabled,
		GymCancellationEnabled:     row.GymCancellationEnabled,
	}
}
