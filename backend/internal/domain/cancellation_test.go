package domain_test

import (
	"testing"
	"time"

	"github.com/vertical-climbing/backend/internal/domain"
)

func TestBuildCancellationPolicy_None(t *testing.T) {
	now := time.Date(2026, 7, 5, 10, 0, 0, 0, time.UTC)
	startsAt := now.Add(3 * time.Hour)

	policy := domain.BuildCancellationPolicy(domain.CancellationPolicyInput{
		BookingStatus:                domain.BookingStatusBooked,
		SlotStartsAt:                 startsAt,
		CancellationForbiddenMinutes: 60,
		Now:                          now,
	})

	if !policy.CanCancel || policy.WarningLevel != domain.CancellationWarningNone {
		t.Fatalf("expected none policy, got %+v", policy)
	}
	if policy.MinutesUntilStart != 180 {
		t.Fatalf("expected 180 minutes, got %d", policy.MinutesUntilStart)
	}
}

func TestBuildCancellationPolicy_LateCancellation(t *testing.T) {
	now := time.Date(2026, 7, 5, 10, 0, 0, 0, time.UTC)
	startsAt := now.Add(90 * time.Minute)

	policy := domain.BuildCancellationPolicy(domain.CancellationPolicyInput{
		BookingStatus:                domain.BookingStatusBooked,
		SlotStartsAt:                 startsAt,
		CancellationForbiddenMinutes: 60,
		Now:                          now,
	})

	if !policy.CanCancel || policy.WarningLevel != domain.CancellationWarningLateCancellation {
		t.Fatalf("expected late_cancellation policy, got %+v", policy)
	}
}

func TestBuildCancellationPolicy_Forbidden(t *testing.T) {
	now := time.Date(2026, 7, 5, 10, 0, 0, 0, time.UTC)
	startsAt := now.Add(59 * time.Minute)

	policy := domain.BuildCancellationPolicy(domain.CancellationPolicyInput{
		BookingStatus:                domain.BookingStatusBooked,
		SlotStartsAt:                 startsAt,
		CancellationForbiddenMinutes: 60,
		Now:                          now,
	})

	if policy.CanCancel || policy.WarningLevel != domain.CancellationWarningForbidden {
		t.Fatalf("expected forbidden policy, got %+v", policy)
	}
}

func TestBuildCancellationPolicy_NotBooked(t *testing.T) {
	now := time.Date(2026, 7, 5, 10, 0, 0, 0, time.UTC)
	startsAt := now.Add(3 * time.Hour)

	policy := domain.BuildCancellationPolicy(domain.CancellationPolicyInput{
		BookingStatus:                domain.BookingStatusCancelledByClient,
		SlotStartsAt:                 startsAt,
		CancellationForbiddenMinutes: 60,
		Now:                          now,
	})

	if policy.CanCancel {
		t.Fatalf("expected can_cancel false for cancelled booking")
	}
}
