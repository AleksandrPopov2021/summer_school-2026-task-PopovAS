package postgres

import (
	"context"
	"errors"
	"fmt"
	"time"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/shopspring/decimal"
	"github.com/vertical-climbing/backend/internal/adapters/postgres/db"
	"github.com/vertical-climbing/backend/internal/domain"
	portbooking "github.com/vertical-climbing/backend/internal/ports/booking"
	portclearance "github.com/vertical-climbing/backend/internal/ports/clearance"
	portclient "github.com/vertical-climbing/backend/internal/ports/client"
	portref "github.com/vertical-climbing/backend/internal/ports/reference"
	portslot "github.com/vertical-climbing/backend/internal/ports/slot"
)

type BookingRepository struct {
	pool         *pgxpool.Pool
	clients      portclient.Repository
	clearances   portclearance.Repository
	reference    portref.Repository
	slotRepo     *SlotRepository
	now          func() time.Time
}

func NewBookingRepository(
	pool *pgxpool.Pool,
	clients portclient.Repository,
	clearances portclearance.Repository,
	reference portref.Repository,
	slotRepo *SlotRepository,
) *BookingRepository {
	return &BookingRepository{
		pool:       pool,
		clients:    clients,
		clearances: clearances,
		reference:  reference,
		slotRepo:   slotRepo,
		now:        time.Now,
	}
}

func (r *BookingRepository) Create(ctx context.Context, input portbooking.CreateInput) (portbooking.Booking, error) {
	client, err := r.clients.GetByID(ctx, input.ClientID)
	if err != nil {
		return portbooking.Booking{}, err
	}
	if !client.RiskConsentAccepted {
		return portbooking.Booking{}, domain.NewRiskConsentRequired()
	}

	config, err := r.reference.GetSystemConfig(ctx)
	if err != nil {
		return portbooking.Booking{}, err
	}

	if domain.BookingForbiddenBySanctions(
		client.LateCancellationCount,
		client.NoShowCount,
		config.ViolationsForSanctions,
	) {
		return portbooking.Booking{}, domain.NewBookingForbiddenSanctions()
	}

	hasClearance, err := r.clearances.HasGrantedClearance(ctx, input.ClientID)
	if err != nil {
		return portbooking.Booking{}, fmt.Errorf("check clearance: %w", err)
	}

	tx, err := r.pool.Begin(ctx)
	if err != nil {
		return portbooking.Booking{}, fmt.Errorf("begin transaction: %w", err)
	}
	defer tx.Rollback(ctx)

	queries := db.New(tx)

	slotID, err := uuid.Parse(input.SlotID)
	if err != nil {
		return portbooking.Booking{}, domain.NewNotFound("Слот не найден")
	}
	clientID, err := uuid.Parse(input.ClientID)
	if err != nil {
		return portbooking.Booking{}, domain.NewUnauthorized("Требуется авторизация")
	}

	slotRow, err := r.lockSlot(ctx, tx, slotID)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return portbooking.Booking{}, domain.NewNotFound("Слот не найден")
		}
		return portbooking.Booking{}, fmt.Errorf("lock slot: %w", err)
	}

	slot, err := mapSlotRow(slotRow)
	if err != nil {
		return portbooking.Booking{}, err
	}

	if err := r.validateSlotForBooking(slot, config.BookingCutoffMinutes, hasClearance); err != nil {
		return portbooking.Booking{}, err
	}

	forbidden, err := queries.HasRebookingForbidden(ctx, clientID, slotID)
	if err != nil {
		return portbooking.Booking{}, fmt.Errorf("check rebooking forbidden: %w", err)
	}
	if forbidden {
		return portbooking.Booking{}, domain.NewRebookingForbidden()
	}

	rentalPrices, err := r.resolveRentalLines(ctx, queries, input.RentalLines)
	if err != nil {
		return portbooking.Booking{}, err
	}

	if err := r.reserveRental(ctx, tx, slotID, input.RentalLines); err != nil {
		return portbooking.Booking{}, err
	}

	if err := r.decrementFreeSpots(ctx, tx, slotID); err != nil {
		// Освобождаем блокировку строки и соединение до построения ответа-конфликта:
		// buildConflict берёт отдельное соединение из пула, а под высокой конкуренцией
		// удержание FOR UPDATE-блокировки привело бы к взаимоблокировке пула.
		_ = tx.Rollback(ctx)
		return portbooking.Booking{}, r.buildConflict(ctx, slotID, config.BookingCutoffMinutes, hasClearance, true)
	}

	paymentCalc := domain.CalculatePayment(
		slot.TrainingPrice,
		rentalPrices,
		domain.LoyaltyInfo{
			IsLoyalClient: client.IsLoyalClient,
			DiscountRate:  client.LoyaltyDiscount,
		},
	)

	bookingID := uuid.New()
	createdAt := r.now()

	if _, err := tx.Exec(ctx, `
		INSERT INTO bookings (id, client_id, slot_id, booking_status, uses_own_equipment)
		VALUES ($1, $2, $3, 'booked', $4)`,
		bookingID, clientID, slotID, input.UsesOwnEquipment,
	); err != nil {
		return portbooking.Booking{}, fmt.Errorf("insert booking: %w", err)
	}

	rentalLines := make([]portbooking.RentalLine, 0, len(input.RentalLines))
	for i, lineInput := range input.RentalLines {
		lineID := uuid.New()
		price := rentalPrices[i]
		equipmentID, _ := uuid.Parse(lineInput.EquipmentTypeID)
		equipment, err := queries.GetEquipmentTypePrice(ctx, equipmentID)
		if err != nil {
			return portbooking.Booking{}, fmt.Errorf("load equipment type: %w", err)
		}

		if _, err := tx.Exec(ctx, `
			INSERT INTO booking_rental_lines (id, booking_id, equipment_type_id, quantity, unit_price)
			VALUES ($1, $2, $3, $4, $5)`,
			lineID, bookingID, equipmentID, lineInput.Quantity, price.UnitPrice.String(),
		); err != nil {
			return portbooking.Booking{}, fmt.Errorf("insert rental line: %w", err)
		}

		rentalLines = append(rentalLines, portbooking.RentalLine{
			ID:              lineID.String(),
			BookingID:       bookingID.String(),
			EquipmentTypeID: lineInput.EquipmentTypeID,
			Quantity:        lineInput.Quantity,
			UnitPrice:       price.UnitPrice,
			EquipmentCode:   equipment.Code,
			EquipmentName:   equipment.Name,
		})
	}

	paymentID := uuid.New()
	var discountValue any
	if paymentCalc.DiscountAmount != nil {
		discountValue = paymentCalc.DiscountAmount.String()
	}

	if _, err := tx.Exec(ctx, `
		INSERT INTO payments (
			id, booking_id, training_amount, rental_amount, discount_amount, total_amount, payment_status
		) VALUES ($1, $2, $3, $4, $5, $6, 'unpaid')`,
		paymentID,
		bookingID,
		paymentCalc.TrainingAmount.String(),
		paymentCalc.RentalAmount.String(),
		discountValue,
		paymentCalc.TotalAmount.String(),
	); err != nil {
		return portbooking.Booking{}, fmt.Errorf("insert payment: %w", err)
	}

	if err := tx.Commit(ctx); err != nil {
		return portbooking.Booking{}, fmt.Errorf("commit transaction: %w", err)
	}

	slot.FreeSpots--

	return portbooking.Booking{
		ID:                 bookingID.String(),
		ClientID:           input.ClientID,
		SlotID:             input.SlotID,
		BookingStatus:      "booked",
		CreatedAt:          createdAt,
		UsesOwnEquipment:   input.UsesOwnEquipment,
		RebookingForbidden: false,
		RentalLines:        rentalLines,
		Payment: portbooking.Payment{
			ID:             paymentID.String(),
			BookingID:      bookingID.String(),
			TrainingAmount: paymentCalc.TrainingAmount,
			RentalAmount:   paymentCalc.RentalAmount,
			DiscountAmount: paymentCalc.DiscountAmount,
			TotalAmount:    paymentCalc.TotalAmount,
			PaymentStatus:  "unpaid",
		},
		Slot: slot,
	}, nil
}

func (r *BookingRepository) ListByClient(ctx context.Context, filter portbooking.ListFilter) ([]portbooking.Booking, error) {
	clientID, err := uuid.Parse(filter.ClientID)
	if err != nil {
		return nil, domain.NewUnauthorized("Требуется авторизация")
	}

	queries := db.New(r.pool)
	rows, err := queries.ListBookingsByClient(ctx, clientID, filter.Status)
	if err != nil {
		return nil, fmt.Errorf("list bookings: %w", err)
	}

	items := make([]portbooking.Booking, 0, len(rows))
	for _, row := range rows {
		booking, err := mapBookingRow(row, nil)
		if err != nil {
			return nil, err
		}
		items = append(items, booking)
	}
	return items, nil
}

func (r *BookingRepository) GetByIDForClient(ctx context.Context, bookingID, clientID string) (portbooking.Booking, error) {
	row, err := r.loadBookingRow(ctx, bookingID, clientID)
	if err != nil {
		return portbooking.Booking{}, err
	}

	queries := db.New(r.pool)
	rentalRows, err := queries.ListBookingRentalLinesByBookingID(ctx, row.ID)
	if err != nil {
		return portbooking.Booking{}, fmt.Errorf("list rental lines: %w", err)
	}

	rentalLines, err := mapBookingRentalLines(rentalRows)
	if err != nil {
		return portbooking.Booking{}, err
	}

	return mapBookingRow(row, rentalLines)
}

func (r *BookingRepository) Cancel(ctx context.Context, bookingID, clientID string) (portbooking.Booking, error) {
	parsedBookingID, err := uuid.Parse(bookingID)
	if err != nil {
		return portbooking.Booking{}, domain.NewNotFound("Запись не найдена")
	}
	parsedClientID, err := uuid.Parse(clientID)
	if err != nil {
		return portbooking.Booking{}, domain.NewUnauthorized("Требуется авторизация")
	}

	config, err := r.reference.GetSystemConfig(ctx)
	if err != nil {
		return portbooking.Booking{}, err
	}

	tx, err := r.pool.Begin(ctx)
	if err != nil {
		return portbooking.Booking{}, fmt.Errorf("begin transaction: %w", err)
	}
	defer tx.Rollback(ctx)

	var (
		status   string
		slotID   uuid.UUID
		startsAt time.Time
	)
	err = tx.QueryRow(ctx, `
		SELECT b.booking_status, b.slot_id, s.starts_at
		FROM bookings b
		JOIN training_slots s ON s.id = b.slot_id
		WHERE b.id = $1 AND b.client_id = $2
		FOR UPDATE OF b`,
		parsedBookingID, parsedClientID,
	).Scan(&status, &slotID, &startsAt)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return portbooking.Booking{}, domain.NewNotFound("Запись не найдена")
		}
		return portbooking.Booking{}, fmt.Errorf("lock booking: %w", err)
	}

	if status != domain.BookingStatusBooked {
		return portbooking.Booking{}, domain.NewBookingNotCancellable()
	}

	policy := domain.BuildCancellationPolicy(domain.CancellationPolicyInput{
		BookingStatus:                status,
		SlotStartsAt:                 startsAt,
		CancellationForbiddenMinutes: config.CancellationForbiddenMinutes,
		Now:                          r.now(),
	})
	if !policy.CanCancel {
		return portbooking.Booking{}, domain.NewCancellationForbidden()
	}

	cancelledAt := r.now()
	if _, err := tx.Exec(ctx, `
		UPDATE bookings
		SET booking_status = 'cancelled_by_client', cancelled_at = $2
		WHERE id = $1`,
		parsedBookingID, cancelledAt,
	); err != nil {
		return portbooking.Booking{}, fmt.Errorf("update booking status: %w", err)
	}

	if _, err := tx.Exec(ctx, `
		UPDATE training_slots
		SET free_spots = free_spots + 1
		WHERE id = $1`,
		slotID,
	); err != nil {
		return portbooking.Booking{}, fmt.Errorf("release slot spot: %w", err)
	}

	rentalRows, err := db.New(tx).ListBookingRentalLinesByBookingID(ctx, parsedBookingID)
	if err != nil {
		return portbooking.Booking{}, fmt.Errorf("list rental lines: %w", err)
	}
	if err := r.releaseRental(ctx, tx, slotID, rentalRows); err != nil {
		return portbooking.Booking{}, err
	}

	if policy.WarningLevel == domain.CancellationWarningLateCancellation {
		if err := db.New(tx).IncrementLateCancellationCount(ctx, parsedClientID); err != nil {
			return portbooking.Booking{}, fmt.Errorf("increment late cancellation count: %w", err)
		}
	}

	if err := tx.Commit(ctx); err != nil {
		return portbooking.Booking{}, fmt.Errorf("commit transaction: %w", err)
	}

	return r.GetByIDForClient(ctx, bookingID, clientID)
}

func (r *BookingRepository) UpdateRental(ctx context.Context, input portbooking.UpdateRentalInput) (portbooking.Booking, error) {
	client, err := r.clients.GetByID(ctx, input.ClientID)
	if err != nil {
		return portbooking.Booking{}, err
	}

	parsedBookingID, err := uuid.Parse(input.BookingID)
	if err != nil {
		return portbooking.Booking{}, domain.NewNotFound("Запись не найдена")
	}
	parsedClientID, err := uuid.Parse(input.ClientID)
	if err != nil {
		return portbooking.Booking{}, domain.NewUnauthorized("Требуется авторизация")
	}

	tx, err := r.pool.Begin(ctx)
	if err != nil {
		return portbooking.Booking{}, fmt.Errorf("begin transaction: %w", err)
	}
	defer tx.Rollback(ctx)

	queries := db.New(tx)

	var (
		status        string
		slotID        uuid.UUID
		trainingPrice string
	)
	err = tx.QueryRow(ctx, `
		SELECT b.booking_status, b.slot_id, s.training_price::text
		FROM bookings b
		JOIN training_slots s ON s.id = b.slot_id
		WHERE b.id = $1 AND b.client_id = $2
		FOR UPDATE OF b`,
		parsedBookingID, parsedClientID,
	).Scan(&status, &slotID, &trainingPrice)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return portbooking.Booking{}, domain.NewNotFound("Запись не найдена")
		}
		return portbooking.Booking{}, fmt.Errorf("lock booking: %w", err)
	}

	if status != domain.BookingStatusBooked {
		return portbooking.Booking{}, domain.NewBookingRentalUpdateForbidden()
	}

	currentLines, err := queries.ListBookingRentalLinesByBookingID(ctx, parsedBookingID)
	if err != nil {
		return portbooking.Booking{}, fmt.Errorf("list rental lines: %w", err)
	}

	if err := r.releaseRental(ctx, tx, slotID, currentLines); err != nil {
		return portbooking.Booking{}, err
	}

	rentalPrices, err := r.resolveRentalLines(ctx, queries, input.RentalLines)
	if err != nil {
		return portbooking.Booking{}, err
	}

	if err := r.reserveRental(ctx, tx, slotID, input.RentalLines); err != nil {
		return portbooking.Booking{}, err
	}

	if _, err := tx.Exec(ctx, `DELETE FROM booking_rental_lines WHERE booking_id = $1`, parsedBookingID); err != nil {
		return portbooking.Booking{}, fmt.Errorf("delete rental lines: %w", err)
	}

	for i, lineInput := range input.RentalLines {
		lineID := uuid.New()
		price := rentalPrices[i]
		equipmentID, err := uuid.Parse(lineInput.EquipmentTypeID)
		if err != nil {
			return portbooking.Booking{}, domain.NewBadRequest("Некорректный equipment_type_id")
		}

		if _, err := tx.Exec(ctx, `
			INSERT INTO booking_rental_lines (id, booking_id, equipment_type_id, quantity, unit_price)
			VALUES ($1, $2, $3, $4, $5)`,
			lineID, parsedBookingID, equipmentID, lineInput.Quantity, price.UnitPrice.String(),
		); err != nil {
			return portbooking.Booking{}, fmt.Errorf("insert rental line: %w", err)
		}
	}

	if _, err := tx.Exec(ctx, `
		UPDATE bookings
		SET uses_own_equipment = $2
		WHERE id = $1`,
		parsedBookingID, input.UsesOwnEquipment,
	); err != nil {
		return portbooking.Booking{}, fmt.Errorf("update booking: %w", err)
	}

	trainingAmount, err := decimal.NewFromString(trainingPrice)
	if err != nil {
		return portbooking.Booking{}, fmt.Errorf("parse training price: %w", err)
	}

	paymentCalc := domain.CalculatePayment(
		trainingAmount,
		rentalPrices,
		domain.LoyaltyInfo{
			IsLoyalClient: client.IsLoyalClient,
			DiscountRate:  client.LoyaltyDiscount,
		},
	)

	var discountValue any
	if paymentCalc.DiscountAmount != nil {
		discountValue = paymentCalc.DiscountAmount.String()
	}

	if _, err := tx.Exec(ctx, `
		UPDATE payments
		SET training_amount = $2,
		    rental_amount = $3,
		    discount_amount = $4,
		    total_amount = $5
		WHERE booking_id = $1`,
		parsedBookingID,
		paymentCalc.TrainingAmount.String(),
		paymentCalc.RentalAmount.String(),
		discountValue,
		paymentCalc.TotalAmount.String(),
	); err != nil {
		return portbooking.Booking{}, fmt.Errorf("update payment: %w", err)
	}

	if err := tx.Commit(ctx); err != nil {
		return portbooking.Booking{}, fmt.Errorf("commit transaction: %w", err)
	}

	return r.GetByIDForClient(ctx, input.BookingID, input.ClientID)
}

func (r *BookingRepository) CancelSlotByGym(ctx context.Context, input portbooking.CancelSlotByGymInput) (portbooking.CancelSlotByGymResult, error) {
	slotID, err := uuid.Parse(input.SlotID)
	if err != nil {
		return portbooking.CancelSlotByGymResult{}, domain.NewNotFound("Слот не найден")
	}
	reasonID, err := uuid.Parse(input.CancellationReasonID)
	if err != nil {
		return portbooking.CancelSlotByGymResult{}, domain.NewBadRequest("Некорректный cancellation_reason_id")
	}

	tx, err := r.pool.Begin(ctx)
	if err != nil {
		return portbooking.CancelSlotByGymResult{}, fmt.Errorf("begin transaction: %w", err)
	}
	defer tx.Rollback(ctx)

	queries := db.New(tx)

	exists, err := queries.CancellationReasonExists(ctx, reasonID)
	if err != nil {
		return portbooking.CancelSlotByGymResult{}, fmt.Errorf("check cancellation reason: %w", err)
	}
	if !exists {
		return portbooking.CancelSlotByGymResult{}, domain.NewBadRequest("Неизвестная причина отмены")
	}

	var slotStatus string
	err = tx.QueryRow(ctx, `
		SELECT slot_status FROM training_slots WHERE id = $1 FOR UPDATE`,
		slotID,
	).Scan(&slotStatus)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return portbooking.CancelSlotByGymResult{}, domain.NewNotFound("Слот не найден")
		}
		return portbooking.CancelSlotByGymResult{}, fmt.Errorf("lock slot: %w", err)
	}
	if slotStatus != domain.SlotStatusActive {
		return portbooking.CancelSlotByGymResult{}, domain.NewUnprocessable(
			domain.ErrorCodeNoFreeSpots,
			"Слот уже отменён или недоступен",
		)
	}

	if _, err := tx.Exec(ctx, `
		UPDATE training_slots SET slot_status = 'cancelled_by_gym' WHERE id = $1`,
		slotID,
	); err != nil {
		return portbooking.CancelSlotByGymResult{}, fmt.Errorf("cancel slot: %w", err)
	}

	bookings, err := queries.ListActiveBookingsBySlotID(ctx, slotID)
	if err != nil {
		return portbooking.CancelSlotByGymResult{}, fmt.Errorf("list active bookings: %w", err)
	}

	cancelledAt := r.now()
	cancelledBookings := make([]portbooking.CancelledBooking, 0, len(bookings))
	for _, booking := range bookings {
		rentalRows, err := queries.ListBookingRentalLinesByBookingID(ctx, booking.ID)
		if err != nil {
			return portbooking.CancelSlotByGymResult{}, fmt.Errorf("list rental lines: %w", err)
		}
		if err := r.releaseRental(ctx, tx, slotID, rentalRows); err != nil {
			return portbooking.CancelSlotByGymResult{}, err
		}

		if _, err := tx.Exec(ctx, `
			UPDATE bookings
			SET booking_status = 'cancelled_by_gym',
			    rebooking_forbidden = TRUE,
			    cancellation_reason_id = $2,
			    cancelled_at = $3
			WHERE id = $1`,
			booking.ID, reasonID, cancelledAt,
		); err != nil {
			return portbooking.CancelSlotByGymResult{}, fmt.Errorf("cancel booking: %w", err)
		}

		if booking.PaymentStatus == "paid" {
			if _, err := tx.Exec(ctx, `
				UPDATE payments SET payment_status = 'refund' WHERE booking_id = $1`,
				booking.ID,
			); err != nil {
				return portbooking.CancelSlotByGymResult{}, fmt.Errorf("update payment: %w", err)
			}
		}

		if _, err := tx.Exec(ctx, `
			UPDATE training_slots SET free_spots = free_spots + 1 WHERE id = $1`,
			slotID,
		); err != nil {
			return portbooking.CancelSlotByGymResult{}, fmt.Errorf("release slot spot: %w", err)
		}

		cancelledBookings = append(cancelledBookings, portbooking.CancelledBooking{
			BookingID: booking.ID.String(),
			ClientID:  booking.ClientID.String(),
		})
	}

	if err := tx.Commit(ctx); err != nil {
		return portbooking.CancelSlotByGymResult{}, fmt.Errorf("commit transaction: %w", err)
	}

	return portbooking.CancelSlotByGymResult{
		SlotID:                 input.SlotID,
		CancelledBookingsCount: len(bookings),
		CancelledBookings:      cancelledBookings,
	}, nil
}

func (r *BookingRepository) FindAlternative(ctx context.Context, input portbooking.FindAlternativeInput) (portbooking.FindAlternativeResult, error) {
	cancelledSlot, err := r.slotRepo.GetByID(ctx, input.CancelledSlotID)
	if err != nil {
		return portbooking.FindAlternativeResult{}, err
	}

	required := make(map[string]int32)
	if input.BookingID != "" {
		booking, err := r.GetByIDForClient(ctx, input.BookingID, input.ClientID)
		if err != nil {
			return portbooking.FindAlternativeResult{}, err
		}
		if booking.SlotID != input.CancelledSlotID {
			return portbooking.FindAlternativeResult{}, domain.NewNotFound("Запись не найдена")
		}
		for _, line := range booking.RentalLines {
			required[line.EquipmentTypeID] += line.Quantity
		}
	}

	candidates, err := r.slotRepo.FindAlternatives(ctx, portslot.AlternativeFilter{
		ZoneID:       cancelledSlot.Zone.ID,
		InstructorID: cancelledSlot.Instructor.ID,
		After:        r.now(),
		ExcludeID:    input.CancelledSlotID,
	})
	if err != nil {
		return portbooking.FindAlternativeResult{}, err
	}

	for _, candidate := range candidates {
		if len(required) == 0 {
			slot := candidate
			return portbooking.FindAlternativeResult{Found: true, Slot: &slot}, nil
		}

		rental, err := r.slotRepo.ListRentalAvailability(ctx, candidate.ID)
		if err != nil {
			return portbooking.FindAlternativeResult{}, err
		}

		availability := make(map[string]int32, len(rental))
		for _, item := range rental {
			availability[item.EquipmentTypeID] = item.AvailableQuantity
		}
		if domain.RentalStockCovers(availability, required) {
			slot := candidate
			return portbooking.FindAlternativeResult{Found: true, Slot: &slot}, nil
		}
	}

	return portbooking.FindAlternativeResult{Found: false}, nil
}

func (r *BookingRepository) loadBookingRow(ctx context.Context, bookingID, clientID string) (db.BookingRow, error) {
	parsedBookingID, err := uuid.Parse(bookingID)
	if err != nil {
		return db.BookingRow{}, domain.NewNotFound("Запись не найдена")
	}
	parsedClientID, err := uuid.Parse(clientID)
	if err != nil {
		return db.BookingRow{}, domain.NewUnauthorized("Требуется авторизация")
	}

	row, err := db.New(r.pool).GetBookingByIDForClient(ctx, parsedBookingID, parsedClientID)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return db.BookingRow{}, domain.NewNotFound("Запись не найдена")
		}
		return db.BookingRow{}, fmt.Errorf("get booking: %w", err)
	}
	return row, nil
}

func mapBookingRow(row db.BookingRow, rentalLines []portbooking.RentalLine) (portbooking.Booking, error) {
	slot, err := mapSlotRow(db.SlotRow{
		ID:                      row.SlotID,
		StartsAt:                row.StartsAt,
		DurationMinutes:         row.DurationMinutes,
		Capacity:                row.Capacity,
		FreeSpots:               row.FreeSpots,
		TrainingPrice:           row.TrainingPrice,
		RentalTariff:            row.RentalTariff,
		SlotStatus:              row.SlotStatus,
		Address:                 row.Address,
		ZoneID:                  row.ZoneID,
		ZoneName:                row.ZoneName,
		ZoneFormatType:          row.ZoneFormatType,
		ZoneDifficulty:          row.ZoneDifficulty,
		ZoneMaxGroupSize:        row.ZoneMaxGroupSize,
		InstructorID:            row.InstructorID,
		InstructorFullName:      row.InstructorFullName,
		InstructorAverageRating: row.InstructorAverageRating,
		VenueID:                 row.VenueID,
		VenueName:               row.VenueName,
		VenueAddress:            row.VenueAddress,
	})
	if err != nil {
		return portbooking.Booking{}, err
	}

	trainingAmount, err := decimal.NewFromString(row.PaymentTrainingAmount)
	if err != nil {
		return portbooking.Booking{}, fmt.Errorf("parse training amount: %w", err)
	}
	rentalAmount, err := decimal.NewFromString(row.PaymentRentalAmount)
	if err != nil {
		return portbooking.Booking{}, fmt.Errorf("parse rental amount: %w", err)
	}
	totalAmount, err := decimal.NewFromString(row.PaymentTotalAmount)
	if err != nil {
		return portbooking.Booking{}, fmt.Errorf("parse total amount: %w", err)
	}

	var discountAmount *decimal.Decimal
	if row.PaymentDiscountAmount != nil && *row.PaymentDiscountAmount != "" {
		discount, err := decimal.NewFromString(*row.PaymentDiscountAmount)
		if err != nil {
			return portbooking.Booking{}, fmt.Errorf("parse discount amount: %w", err)
		}
		discountAmount = &discount
	}

	booking := portbooking.Booking{
		ID:                 row.ID.String(),
		ClientID:           row.ClientID.String(),
		SlotID:             row.SlotID.String(),
		BookingStatus:      row.BookingStatus,
		CreatedAt:          row.CreatedAt,
		CancelledAt:        row.CancelledAt,
		UsesOwnEquipment:   row.UsesOwnEquipment,
		RebookingForbidden: row.RebookingForbidden,
		RentalLines:        rentalLines,
		Payment: portbooking.Payment{
			ID:             row.PaymentID.String(),
			BookingID:      row.ID.String(),
			TrainingAmount: trainingAmount,
			RentalAmount:   rentalAmount,
			DiscountAmount: discountAmount,
			TotalAmount:    totalAmount,
			PaymentStatus:  row.PaymentStatus,
		},
		Slot: slot,
	}

	if row.CancellationReasonID != nil && row.CancellationReasonCode != nil && row.CancellationReasonTitle != nil && row.CancellationReasonApology != nil {
		booking.CancellationReason = &portbooking.CancellationReason{
			ID:          row.CancellationReasonID.String(),
			Code:        *row.CancellationReasonCode,
			Title:       *row.CancellationReasonTitle,
			ApologyText: *row.CancellationReasonApology,
		}
	}

	return booking, nil
}

func mapBookingRentalLines(rows []db.BookingRentalLineRow) ([]portbooking.RentalLine, error) {
	lines := make([]portbooking.RentalLine, 0, len(rows))
	for _, row := range rows {
		unitPrice, err := decimal.NewFromString(row.UnitPrice)
		if err != nil {
			return nil, fmt.Errorf("parse rental unit price: %w", err)
		}
		lines = append(lines, portbooking.RentalLine{
			ID:              row.ID.String(),
			BookingID:       row.BookingID.String(),
			EquipmentTypeID: row.EquipmentTypeID.String(),
			Quantity:        row.Quantity,
			UnitPrice:       unitPrice,
			EquipmentCode:   row.EquipmentCode,
			EquipmentName:   row.EquipmentName,
		})
	}
	return lines, nil
}

func (r *BookingRepository) releaseRental(ctx context.Context, tx pgx.Tx, slotID uuid.UUID, lines []db.BookingRentalLineRow) error {
	for _, line := range lines {
		if _, err := tx.Exec(ctx, `
			UPDATE slot_rental_availability
			SET available_quantity = available_quantity + $1
			WHERE slot_id = $2 AND equipment_type_id = $3`,
			line.Quantity, slotID, line.EquipmentTypeID,
		); err != nil {
			return fmt.Errorf("release rental: %w", err)
		}
	}
	return nil
}

func (r *BookingRepository) validateSlotForBooking(
	slot portslot.Slot,
	cutoffMinutes int32,
	hasClearance bool,
) error {
	availability := domain.BuildBookingAvailability(domain.AvailabilityInput{
		SlotStatus:             slot.SlotStatus,
		FormatType:             slot.Zone.FormatType,
		FreeSpots:              slot.FreeSpots,
		StartsAt:               slot.StartsAt,
		BookingCutoffMinutes:   cutoffMinutes,
		HasClearance:           hasClearance,
		HasAuthenticatedClient: true,
		Now:                    r.now(),
	})

	if slot.SlotStatus != domain.SlotStatusActive {
		return domain.NewUnprocessable(domain.ErrorCodeNoFreeSpots, "Слот недоступен для записи")
	}
	// Наличие свободных мест проверяется атомарно в decrementFreeSpots:
	// проигравший гонку (BR-004) получает 409 BookingConflictResponse
	// с актуальным состоянием слота (FR-014), а не 422.
	if !availability.WithinBookingWindow {
		return domain.NewBookingCutoffExceeded()
	}
	if availability.ClearanceRequired && !availability.ClearanceGranted {
		return domain.NewInstructorClearanceRequired()
	}
	return nil
}

func (r *BookingRepository) resolveRentalLines(
	ctx context.Context,
	queries *db.Queries,
	lines []portbooking.RentalLineInput,
) ([]domain.RentalLinePrice, error) {
	if len(lines) == 0 {
		return nil, nil
	}

	prices := make([]domain.RentalLinePrice, 0, len(lines))
	seen := make(map[string]int32)

	for _, line := range lines {
		if line.Quantity < 1 {
			return nil, domain.NewBadRequest("Количество проката должно быть не меньше 1")
		}
		seen[line.EquipmentTypeID] += line.Quantity

		equipmentID, err := uuid.Parse(line.EquipmentTypeID)
		if err != nil {
			return nil, domain.NewBadRequest("Некорректный equipment_type_id")
		}

		equipment, err := queries.GetEquipmentTypePrice(ctx, equipmentID)
		if err != nil {
			if errors.Is(err, pgx.ErrNoRows) {
				return nil, domain.NewBadRequest("Неизвестный тип прокатного снаряжения")
			}
			return nil, fmt.Errorf("get equipment price: %w", err)
		}

		unitPrice, err := decimal.NewFromString(equipment.DefaultPrice)
		if err != nil {
			return nil, fmt.Errorf("parse equipment price: %w", err)
		}

		prices = append(prices, domain.RentalLinePrice{
			Quantity:  line.Quantity,
			UnitPrice: unitPrice,
		})
	}

	return prices, nil
}

func (r *BookingRepository) reserveRental(
	ctx context.Context,
	tx pgx.Tx,
	slotID uuid.UUID,
	lines []portbooking.RentalLineInput,
) error {
	aggregated := make(map[string]int32)
	for _, line := range lines {
		aggregated[line.EquipmentTypeID] += line.Quantity
	}

	for equipmentTypeID, quantity := range aggregated {
		equipmentID, err := uuid.Parse(equipmentTypeID)
		if err != nil {
			return domain.NewBadRequest("Некорректный equipment_type_id")
		}

		var updatedID uuid.UUID
		err = tx.QueryRow(ctx, `
			UPDATE slot_rental_availability
			SET available_quantity = available_quantity - $1
			WHERE slot_id = $2
			  AND equipment_type_id = $3
			  AND available_quantity >= $1
			RETURNING id`,
			quantity, slotID, equipmentID,
		).Scan(&updatedID)
		if err != nil {
			if errors.Is(err, pgx.ErrNoRows) {
				return domain.NewRentalUnavailable()
			}
			return fmt.Errorf("reserve rental: %w", err)
		}
	}

	return nil
}

func (r *BookingRepository) decrementFreeSpots(ctx context.Context, tx pgx.Tx, slotID uuid.UUID) error {
	var freeSpots int32
	err := tx.QueryRow(ctx, `
		UPDATE training_slots
		SET free_spots = free_spots - 1
		WHERE id = $1
		  AND free_spots > 0
		  AND slot_status = 'active'
		RETURNING free_spots`,
		slotID,
	).Scan(&freeSpots)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return domain.NewNoFreeSpots()
		}
		return fmt.Errorf("decrement free spots: %w", err)
	}
	return nil
}

func (r *BookingRepository) lockSlot(ctx context.Context, tx pgx.Tx, slotID uuid.UUID) (db.SlotRow, error) {
	row := tx.QueryRow(ctx, lockSlotForUpdate, slotID)
	var item db.SlotRow
	err := row.Scan(
		&item.ID,
		&item.StartsAt,
		&item.DurationMinutes,
		&item.Capacity,
		&item.FreeSpots,
		&item.TrainingPrice,
		&item.RentalTariff,
		&item.SlotStatus,
		&item.Address,
		&item.ZoneID,
		&item.ZoneName,
		&item.ZoneFormatType,
		&item.ZoneDifficulty,
		&item.ZoneMaxGroupSize,
		&item.InstructorID,
		&item.InstructorFullName,
		&item.InstructorAverageRating,
		&item.VenueID,
		&item.VenueName,
		&item.VenueAddress,
	)
	return item, err
}

func (r *BookingRepository) buildConflict(
	ctx context.Context,
	slotID uuid.UUID,
	cutoffMinutes int32,
	hasClearance bool,
	authenticated bool,
) error {
	slot, err := r.slotRepo.GetByID(ctx, slotID.String())
	if err != nil {
		return domain.NewConflict(domain.ErrorCodeBookingConflict, "Не удалось забронировать слот")
	}

	rental, err := r.slotRepo.ListRentalAvailability(ctx, slotID.String())
	if err != nil {
		return domain.NewConflict(domain.ErrorCodeBookingConflict, "Не удалось забронировать слот")
	}

	availability := domain.BuildBookingAvailability(domain.AvailabilityInput{
		SlotStatus:             slot.SlotStatus,
		FormatType:             slot.Zone.FormatType,
		FreeSpots:              slot.FreeSpots,
		StartsAt:               slot.StartsAt,
		BookingCutoffMinutes:   cutoffMinutes,
		HasClearance:           hasClearance,
		HasAuthenticatedClient: authenticated,
		Now:                    r.now(),
	})

	return domain.NewBookingConflict(portslot.SlotDetail{
		Slot:               slot,
		RentalAvailability: rental,
	}, availability)
}

const lockSlotForUpdate = `
SELECT
    s.id,
    s.starts_at,
    s.duration_minutes,
    s.capacity,
    s.free_spots,
    s.training_price::text AS training_price,
    s.rental_tariff::text AS rental_tariff,
    s.slot_status,
    s.address,
    z.id AS zone_id,
    z.name AS zone_name,
    z.format_type AS zone_format_type,
    z.difficulty AS zone_difficulty,
    z.max_group_size AS zone_max_group_size,
    i.id AS instructor_id,
    i.full_name AS instructor_full_name,
    i.average_rating::text AS instructor_average_rating,
    v.id AS venue_id,
    v.name AS venue_name,
    v.address AS venue_address
FROM training_slots s
JOIN training_zones z ON z.id = s.zone_id
JOIN instructors i ON i.id = s.instructor_id
JOIN gym_venues v ON v.id = s.venue_id
WHERE s.id = $1
FOR UPDATE OF s`
