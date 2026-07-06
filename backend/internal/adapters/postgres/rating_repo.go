package postgres

import (
	"context"
	"errors"
	"fmt"
	"time"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/vertical-climbing/backend/internal/adapters/postgres/db"
	"github.com/vertical-climbing/backend/internal/domain"
	portrating "github.com/vertical-climbing/backend/internal/ports/rating"
)

type RatingRepository struct {
	pool *pgxpool.Pool
	now  func() time.Time
}

func NewRatingRepository(pool *pgxpool.Pool) *RatingRepository {
	return &RatingRepository{
		pool: pool,
		now:  time.Now,
	}
}

func (r *RatingRepository) Create(ctx context.Context, clientID, bookingID string, stars int32) (portrating.Rating, error) {
	if err := domain.ValidateRatingStars(stars); err != nil {
		return portrating.Rating{}, err
	}

	parsedClientID, err := uuid.Parse(clientID)
	if err != nil {
		return portrating.Rating{}, domain.NewUnauthorized("Требуется авторизация")
	}
	parsedBookingID, err := uuid.Parse(bookingID)
	if err != nil {
		return portrating.Rating{}, domain.NewNotFound("Запись не найдена")
	}

	tx, err := r.pool.Begin(ctx)
	if err != nil {
		return portrating.Rating{}, fmt.Errorf("begin transaction: %w", err)
	}
	defer tx.Rollback(ctx)

	queries := db.New(tx)

	booking, err := queries.GetBookingForRating(ctx, parsedBookingID, parsedClientID)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return portrating.Rating{}, domain.NewNotFound("Запись не найдена")
		}
		return portrating.Rating{}, fmt.Errorf("get booking for rating: %w", err)
	}

	switch booking.BookingStatus {
	case domain.BookingStatusCancelledByGym:
		return portrating.Rating{}, domain.NewRatingNotAllowedGymCancelled()
	case domain.BookingStatusCompleted:
	default:
		return portrating.Rating{}, domain.NewRatingWindowExpired()
	}

	if domain.RatingWindowExpired(booking.StartsAt, booking.DurationMinutes, r.now()) {
		return portrating.Rating{}, domain.NewRatingWindowExpired()
	}

	exists, err := queries.RatingExistsForBooking(ctx, parsedBookingID)
	if err != nil {
		return portrating.Rating{}, fmt.Errorf("check existing rating: %w", err)
	}
	if exists {
		return portrating.Rating{}, domain.NewRatingAlreadySubmitted()
	}

	row, err := queries.InsertInstructorRating(ctx, db.InsertInstructorRatingParams{
		ClientID:     parsedClientID,
		InstructorID: booking.InstructorID,
		BookingID:    parsedBookingID,
		Stars:        stars,
	})
	if err != nil {
		if isUniqueViolation(err) {
			return portrating.Rating{}, domain.NewRatingAlreadySubmitted()
		}
		return portrating.Rating{}, fmt.Errorf("insert rating: %w", err)
	}

	if err := queries.RecalculateInstructorAverageRating(ctx, booking.InstructorID); err != nil {
		return portrating.Rating{}, fmt.Errorf("recalculate instructor rating: %w", err)
	}

	if err := tx.Commit(ctx); err != nil {
		return portrating.Rating{}, fmt.Errorf("commit transaction: %w", err)
	}

	return portrating.Rating{
		ID:           row.ID.String(),
		ClientID:     row.ClientID.String(),
		InstructorID: row.InstructorID.String(),
		BookingID:    row.BookingID.String(),
		Stars:        row.Stars,
		RatedAt:      row.RatedAt,
	}, nil
}
