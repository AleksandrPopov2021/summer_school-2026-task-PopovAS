package domain

import (
	"math"
	"time"
)

const (
	CancellationWarningNone             = "none"
	CancellationWarningLateCancellation = "late_cancellation"
	CancellationWarningForbidden        = "forbidden"

	// LateCancellationThresholdMinutes is the upper bound of the late-cancellation window (BR-010/BR-011).
	LateCancellationThresholdMinutes = 120
)

type CancellationPolicy struct {
	CanCancel         bool
	MinutesUntilStart int
	WarningLevel      string
}

type CancellationPolicyInput struct {
	BookingStatus                string
	SlotStartsAt                 time.Time
	CancellationForbiddenMinutes int32
	Now                          time.Time
}

func BuildCancellationPolicy(input CancellationPolicyInput) CancellationPolicy {
	minutesUntilStart := int(math.Floor(input.SlotStartsAt.Sub(input.Now).Minutes()))
	if minutesUntilStart < 0 {
		minutesUntilStart = 0
	}

	if input.BookingStatus != BookingStatusBooked {
		return CancellationPolicy{
			CanCancel:         false,
			MinutesUntilStart: minutesUntilStart,
			WarningLevel:      CancellationWarningForbidden,
		}
	}

	forbiddenMinutes := int(input.CancellationForbiddenMinutes)
	if minutesUntilStart < forbiddenMinutes {
		return CancellationPolicy{
			CanCancel:         false,
			MinutesUntilStart: minutesUntilStart,
			WarningLevel:      CancellationWarningForbidden,
		}
	}

	if minutesUntilStart < LateCancellationThresholdMinutes {
		return CancellationPolicy{
			CanCancel:         true,
			MinutesUntilStart: minutesUntilStart,
			WarningLevel:      CancellationWarningLateCancellation,
		}
	}

	return CancellationPolicy{
		CanCancel:         true,
		MinutesUntilStart: minutesUntilStart,
		WarningLevel:      CancellationWarningNone,
	}
}
