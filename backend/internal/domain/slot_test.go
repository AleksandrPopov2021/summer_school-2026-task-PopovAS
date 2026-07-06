package domain_test

import (
	"testing"
	"time"

	"github.com/vertical-climbing/backend/internal/domain"
)

func TestBuildBookingAvailability_Bouldering(t *testing.T) {
	now := time.Date(2026, 7, 5, 10, 0, 0, 0, time.UTC)
	startsAt := now.Add(2 * time.Hour)

	availability := domain.BuildBookingAvailability(domain.AvailabilityInput{
		SlotStatus:             domain.SlotStatusActive,
		FormatType:             domain.FormatTypeBouldering,
		FreeSpots:              3,
		StartsAt:               startsAt,
		BookingCutoffMinutes:   30,
		HasClearance:           false,
		HasAuthenticatedClient: false,
		Now:                    now,
	})

	if !availability.CanBook {
		t.Fatalf("expected can_book true for bouldering")
	}
	if availability.ClearanceRequired {
		t.Fatalf("expected clearance not required")
	}
	if !availability.ClearanceGranted {
		t.Fatalf("expected clearance granted for bouldering")
	}
}

func TestBuildBookingAvailability_RopeWithoutClearance(t *testing.T) {
	now := time.Date(2026, 7, 5, 10, 0, 0, 0, time.UTC)
	startsAt := now.Add(2 * time.Hour)

	availability := domain.BuildBookingAvailability(domain.AvailabilityInput{
		SlotStatus:             domain.SlotStatusActive,
		FormatType:             domain.FormatTypeRopeRoutes,
		FreeSpots:              2,
		StartsAt:               startsAt,
		BookingCutoffMinutes:   30,
		HasClearance:           false,
		HasAuthenticatedClient: true,
		Now:                    now,
	})

	if availability.CanBook {
		t.Fatalf("expected can_book false without clearance")
	}
	if !availability.ClearanceRequired || availability.ClearanceGranted {
		t.Fatalf("expected clearance required and not granted")
	}
}

func TestBuildBookingAvailability_NoFreeSpots(t *testing.T) {
	now := time.Date(2026, 7, 5, 10, 0, 0, 0, time.UTC)

	availability := domain.BuildBookingAvailability(domain.AvailabilityInput{
		SlotStatus:             domain.SlotStatusActive,
		FormatType:             domain.FormatTypeBouldering,
		FreeSpots:              0,
		StartsAt:               now.Add(2 * time.Hour),
		BookingCutoffMinutes:   30,
		HasAuthenticatedClient: true,
		Now:                    now,
	})

	if availability.CanBook || availability.HasFreeSpots {
		t.Fatalf("expected no free spots")
	}
}

func TestBuildBookingAvailability_CutoffExceeded(t *testing.T) {
	now := time.Date(2026, 7, 5, 10, 0, 0, 0, time.UTC)

	availability := domain.BuildBookingAvailability(domain.AvailabilityInput{
		SlotStatus:             domain.SlotStatusActive,
		FormatType:             domain.FormatTypeBouldering,
		FreeSpots:              5,
		StartsAt:               now.Add(20 * time.Minute),
		BookingCutoffMinutes:   30,
		HasAuthenticatedClient: true,
		Now:                    now,
	})

	if availability.CanBook || availability.WithinBookingWindow {
		t.Fatalf("expected booking window closed")
	}
}

func TestBuildBookingAvailability_CancelledSlot(t *testing.T) {
	now := time.Date(2026, 7, 5, 10, 0, 0, 0, time.UTC)

	availability := domain.BuildBookingAvailability(domain.AvailabilityInput{
		SlotStatus:             domain.SlotStatusCancelledByGym,
		FormatType:             domain.FormatTypeBouldering,
		FreeSpots:              5,
		StartsAt:               now.Add(3 * time.Hour),
		BookingCutoffMinutes:   30,
		HasAuthenticatedClient: true,
		Now:                    now,
	})

	if availability.CanBook {
		t.Fatalf("expected can_book false for cancelled slot")
	}
}

func TestParseSlotPeriod_DefaultSevenDays(t *testing.T) {
	now := time.Date(2026, 7, 5, 15, 30, 0, 0, time.UTC)
	from, to, err := domain.ParseSlotPeriod("", "", "", now)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}

	if from != time.Date(2026, 7, 5, 0, 0, 0, 0, time.UTC) {
		t.Fatalf("unexpected from: %v", from)
	}
	if to != time.Date(2026, 7, 13, 0, 0, 0, 0, time.UTC) {
		t.Fatalf("unexpected to: %v", to)
	}
}
