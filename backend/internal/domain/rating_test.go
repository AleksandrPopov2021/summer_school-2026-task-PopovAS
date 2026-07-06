package domain_test

import (
	"testing"
	"time"

	"github.com/vertical-climbing/backend/internal/domain"
)

func TestRatingWindowExpired(t *testing.T) {
	startsAt := time.Date(2026, 1, 1, 10, 0, 0, 0, time.UTC)
	duration := int32(90)

	withinWindow := startsAt.Add(90*time.Minute + 24*time.Hour)
	if domain.RatingWindowExpired(startsAt, duration, withinWindow) {
		t.Fatal("expected window open 24h after slot end")
	}

	expired := startsAt.Add(90*time.Minute + 49*time.Hour)
	if !domain.RatingWindowExpired(startsAt, duration, expired) {
		t.Fatal("expected window closed after 48h from slot end")
	}
}

func TestValidateRatingStars(t *testing.T) {
	if err := domain.ValidateRatingStars(3); err != nil {
		t.Fatalf("expected valid stars: %v", err)
	}
	if err := domain.ValidateRatingStars(0); err == nil {
		t.Fatal("expected error for 0 stars")
	}
	if err := domain.ValidateRatingStars(6); err == nil {
		t.Fatal("expected error for 6 stars")
	}
}
