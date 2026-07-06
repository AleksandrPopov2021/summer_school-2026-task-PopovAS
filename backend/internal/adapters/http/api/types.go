package api

import (
	"github.com/shopspring/decimal"
	"time"
)

// ErrorResponse matches OpenAPI ErrorResponse (NFR-003).
type ErrorResponse struct {
	Code    string         `json:"code"`
	Message string         `json:"message"`
	Details map[string]any `json:"details,omitempty"`
}

type RentalEquipmentCode string

const (
	RentalEquipmentCodeShoes   RentalEquipmentCode = "shoes"
	RentalEquipmentCodeHarness RentalEquipmentCode = "harness"
	RentalEquipmentCodeHelmet  RentalEquipmentCode = "helmet"
	RentalEquipmentCodeChalk    RentalEquipmentCode = "chalk"
)

type RentalEquipmentType struct {
	Id           string              `json:"id"`
	Code         RentalEquipmentCode `json:"code"`
	Name         string              `json:"name"`
	DefaultPrice decimal.Decimal     `json:"default_price"`
}

type RentalEquipmentTypeList struct {
	Items []RentalEquipmentType `json:"items"`
}

type SystemConfig struct {
	ReminderHoursBefore          int `json:"reminder_hours_before"`
	VisitsForLoyalty             int `json:"visits_for_loyalty"`
	ViolationsForSanctions       int `json:"violations_for_sanctions"`
	BookingCutoffMinutes         int `json:"booking_cutoff_minutes"`
	CancellationForbiddenMinutes int `json:"cancellation_forbidden_minutes"`
}

type ClientRegistrationRequest struct {
	Phone     string `json:"phone"`
	FullName  string `json:"full_name"`
	BirthDate string `json:"birth_date"`
}

type ClientRegistrationResponse struct {
	AccessToken string `json:"access_token"`
	TokenType   string `json:"token_type"`
	Client      Client `json:"client"`
}

type ClientUpdateRequest struct {
	RiskConsentAccepted *bool `json:"risk_consent_accepted"`
}

type Client struct {
	Id                    string           `json:"id"`
	FullName              string           `json:"full_name"`
	Phone                 string           `json:"phone"`
	BirthDate             string           `json:"birth_date"`
	RiskConsentAccepted   bool             `json:"risk_consent_accepted"`
	CompletedVisitsCount  int              `json:"completed_visits_count"`
	IsLoyalClient         bool             `json:"is_loyal_client"`
	LoyaltyDiscount       *decimal.Decimal `json:"loyalty_discount"`
	LateCancellationCount int              `json:"late_cancellation_count"`
	NoShowCount           int              `json:"no_show_count"`
}

type NotificationPreferences struct {
	Id                         string `json:"id"`
	ClientId                   string `json:"client_id"`
	BookingConfirmationEnabled bool   `json:"booking_confirmation_enabled"`
	RatingInvitationEnabled    bool   `json:"rating_invitation_enabled"`
	RemindersEnabled           bool   `json:"reminders_enabled"`
	GymCancellationEnabled     bool   `json:"gym_cancellation_enabled"`
}

type NotificationPreferencesUpdateRequest struct {
	BookingConfirmationEnabled *bool `json:"booking_confirmation_enabled"`
	RatingInvitationEnabled    *bool `json:"rating_invitation_enabled"`
	RemindersEnabled           *bool `json:"reminders_enabled"`
	GymCancellationEnabled     *bool `json:"gym_cancellation_enabled"`
}

const TokenTypeBearer = "Bearer"

type PushPlatform string

const (
	PushPlatformIOS     PushPlatform = "ios"
	PushPlatformAndroid PushPlatform = "android"
)

type PushTokenRequest struct {
	Token    string       `json:"token"`
	Platform PushPlatform `json:"platform"`
}

type SlotStatus string

const (
	SlotStatusActive         SlotStatus = "active"
	SlotStatusCancelledByGym SlotStatus = "cancelled_by_gym"
)

type FormatType string

const (
	FormatTypeBouldering FormatType = "bouldering_instruction"
	FormatTypeRopeRoutes FormatType = "rope_routes"
)

type Difficulty string

const (
	DifficultyBeginner    Difficulty = "beginner"
	DifficultyExperienced Difficulty = "experienced"
)

type BookingAvailability struct {
	CanBook             bool  `json:"can_book"`
	HasFreeSpots        bool  `json:"has_free_spots"`
	FreeSpots           *int  `json:"free_spots,omitempty"`
	WithinBookingWindow bool  `json:"within_booking_window"`
	ClearanceRequired   bool  `json:"clearance_required"`
	ClearanceGranted    bool  `json:"clearance_granted"`
}

type TrainingZone struct {
	Id           string     `json:"id"`
	Name         string     `json:"name"`
	FormatType   FormatType `json:"format_type"`
	Difficulty   Difficulty `json:"difficulty"`
	MaxGroupSize int        `json:"max_group_size"`
}

type Instructor struct {
	Id            string           `json:"id"`
	FullName      string           `json:"full_name"`
	AverageRating *decimal.Decimal `json:"average_rating"`
}

type GymVenue struct {
	Id      string `json:"id"`
	Name    string `json:"name"`
	Address string `json:"address"`
}

type TrainingSlotSummary struct {
	Id              string              `json:"id"`
	StartsAt        time.Time           `json:"starts_at"`
	DurationMinutes int                 `json:"duration_minutes"`
	Capacity        int                 `json:"capacity"`
	FreeSpots       int                 `json:"free_spots"`
	TrainingPrice   decimal.Decimal     `json:"training_price"`
	RentalTariff    *decimal.Decimal    `json:"rental_tariff"`
	SlotStatus      SlotStatus          `json:"slot_status"`
	Address         string              `json:"address"`
	Zone            TrainingZone        `json:"zone"`
	Instructor      Instructor          `json:"instructor"`
	Venue           *GymVenue           `json:"venue,omitempty"`
	Availability    BookingAvailability `json:"availability"`
}

type SlotRentalAvailability struct {
	Id                string              `json:"id"`
	SlotId            string              `json:"slot_id"`
	EquipmentTypeId   string              `json:"equipment_type_id"`
	AvailableQuantity int                 `json:"available_quantity"`
	EquipmentType     RentalEquipmentType `json:"equipment_type"`
}

type TrainingSlotDetail struct {
	TrainingSlotSummary
	RentalAvailability []SlotRentalAvailability `json:"rental_availability"`
}

type TrainingSlotList struct {
	Items []TrainingSlotSummary `json:"items"`
}

type InstructorClearance struct {
	Id           string     `json:"id"`
	ClientId     string     `json:"client_id"`
	InstructorId *string    `json:"instructor_id"`
	IsGranted    bool       `json:"is_granted"`
	GrantedAt    *time.Time `json:"granted_at"`
}

type InstructorClearanceList struct {
	Items []InstructorClearance `json:"items"`
}

type BookingStatus string

const (
	BookingStatusBooked            BookingStatus = "booked"
	BookingStatusCancelledByClient BookingStatus = "cancelled_by_client"
	BookingStatusCancelledByGym    BookingStatus = "cancelled_by_gym"
	BookingStatusCompleted         BookingStatus = "completed"
	BookingStatusNoShow            BookingStatus = "no_show"
)

type PaymentStatus string

const PaymentStatusUnpaid PaymentStatus = "unpaid"

const PaymentStatusRefund PaymentStatus = "refund"

type BookingRentalLineInput struct {
	EquipmentTypeId string `json:"equipment_type_id"`
	Quantity        int    `json:"quantity"`
}

type CreateBookingRequest struct {
	SlotId             string                   `json:"slot_id"`
	UsesOwnEquipment   bool                     `json:"uses_own_equipment"`
	RentalLines        []BookingRentalLineInput `json:"rental_lines,omitempty"`
}

type UpdateBookingRentalRequest struct {
	UsesOwnEquipment bool                     `json:"uses_own_equipment"`
	RentalLines      []BookingRentalLineInput `json:"rental_lines,omitempty"`
}

type BookingRentalLine struct {
	Id              string              `json:"id"`
	BookingId       string              `json:"booking_id"`
	EquipmentTypeId string              `json:"equipment_type_id"`
	Quantity        int                 `json:"quantity"`
	UnitPrice       decimal.Decimal     `json:"unit_price"`
	EquipmentType   RentalEquipmentType `json:"equipment_type"`
}

type PaymentInfo struct {
	Id             string           `json:"id"`
	BookingId      string           `json:"booking_id"`
	TrainingAmount decimal.Decimal  `json:"training_amount"`
	RentalAmount   decimal.Decimal  `json:"rental_amount"`
	DiscountAmount *decimal.Decimal `json:"discount_amount"`
	TotalAmount    decimal.Decimal  `json:"total_amount"`
	PaymentStatus  PaymentStatus    `json:"payment_status"`
}

type CancellationPolicyWarningLevel string

const (
	WarningLevelNone             CancellationPolicyWarningLevel = "none"
	WarningLevelLateCancellation CancellationPolicyWarningLevel = "late_cancellation"
	WarningLevelForbidden        CancellationPolicyWarningLevel = "forbidden"
)

type CancellationPolicy struct {
	CanCancel         bool                           `json:"can_cancel"`
	MinutesUntilStart int                            `json:"minutes_until_start"`
	WarningLevel      CancellationPolicyWarningLevel `json:"warning_level"`
}

type CancellationReason struct {
	Id          string `json:"id"`
	Code        string `json:"code"`
	Title       string `json:"title"`
	ApologyText string `json:"apology_text"`
}

type BookingSummary struct {
	Id                 string              `json:"id"`
	SlotId             string              `json:"slot_id"`
	BookingStatus      BookingStatus       `json:"booking_status"`
	CreatedAt          time.Time           `json:"created_at"`
	CancelledAt        *time.Time          `json:"cancelled_at,omitempty"`
	UsesOwnEquipment   bool                `json:"uses_own_equipment"`
	RebookingForbidden bool                `json:"rebooking_forbidden"`
	Slot               TrainingSlotSummary `json:"slot"`
	Payment            PaymentInfo         `json:"payment"`
	CancellationPolicy *CancellationPolicy `json:"cancellation_policy,omitempty"`
}

type BookingList struct {
	Items []BookingSummary `json:"items"`
}

type AlternativeSlotResponse struct {
	Found           bool                 `json:"found"`
	AlternativeSlot *TrainingSlotSummary `json:"alternative_slot,omitempty"`
}

type BookingDetail struct {
	BookingSummary
	RentalLines        []BookingRentalLine `json:"rental_lines"`
	CancellationReason *CancellationReason `json:"cancellation_reason,omitempty"`
}

type BookingConflictResponse struct {
	Code    string              `json:"code"`
	Message string              `json:"message"`
	Slot    TrainingSlotDetail  `json:"slot"`
}

type SlotRentalAvailabilityList struct {
	SlotId string                   `json:"slot_id"`
	Items  []SlotRentalAvailability `json:"items"`
}

type CreateRatingRequest struct {
	BookingId string `json:"booking_id"`
	Stars     int    `json:"stars"`
}

type InstructorRating struct {
	Id           string    `json:"id"`
	ClientId     string    `json:"client_id"`
	InstructorId string    `json:"instructor_id"`
	BookingId    string    `json:"booking_id"`
	Stars        int       `json:"stars"`
	RatedAt      time.Time `json:"rated_at"`
}
