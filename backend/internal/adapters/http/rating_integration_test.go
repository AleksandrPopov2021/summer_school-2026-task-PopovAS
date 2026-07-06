package httpx_test

import (
	"context"
	"fmt"
	"net/http"
	"testing"
	"time"

	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/vertical-climbing/backend/internal/adapters/http/api"
)

func TestCreateInstructorRatingIntegration(t *testing.T) {
	server, pool := newIntegrationServer(t)

	phone := fmt.Sprintf("+7921%07d", time.Now().UnixNano()%10_000_000)
	client, token := registerBookingClient(t, server, phone)
	t.Cleanup(func() { cleanupBookingClient(t, pool, phone) })

	bookingID := insertCompletedBooking(t, pool, client.Id)

	resp := doJSONRequest(t, server.Client(), http.MethodPost, server.URL+"/v1/ratings", token, map[string]any{
		"booking_id": bookingID,
		"stars":      5,
	})
	if resp.StatusCode != http.StatusCreated {
		t.Fatalf("create rating status: %d body=%s", resp.StatusCode, readBody(t, resp))
	}

	var rating api.InstructorRating
	decodeJSONResponse(t, resp, &rating)
	if rating.Stars != 5 || rating.BookingId != bookingID {
		t.Fatalf("unexpected rating response: %+v", rating)
	}

	var avgRating string
	err := pool.QueryRow(context.Background(), `
		SELECT average_rating::text
		FROM instructors i
		JOIN training_slots s ON s.instructor_id = i.id
		JOIN bookings b ON b.slot_id = s.id
		WHERE b.id = $1`, bookingID).Scan(&avgRating)
	if err != nil {
		t.Fatalf("query instructor average: %v", err)
	}
	if avgRating != "5.00" {
		t.Fatalf("expected average_rating 5.00, got %s", avgRating)
	}

	dupResp := doJSONRequest(t, server.Client(), http.MethodPost, server.URL+"/v1/ratings", token, map[string]any{
		"booking_id": bookingID,
		"stars":      4,
	})
	if dupResp.StatusCode != http.StatusConflict {
		t.Fatalf("duplicate rating status: %d body=%s", dupResp.StatusCode, readBody(t, dupResp))
	}
}

func TestCreateInstructorRatingGymCancelledIntegration(t *testing.T) {
	server, pool := newIntegrationServer(t)

	phone := fmt.Sprintf("+7922%07d", time.Now().UnixNano()%10_000_000)
	client, token := registerBookingClient(t, server, phone)
	t.Cleanup(func() { cleanupBookingClient(t, pool, phone) })

	bookingID := insertCompletedBooking(t, pool, client.Id)
	_, err := pool.Exec(context.Background(), `
		UPDATE bookings SET booking_status = 'cancelled_by_gym' WHERE id = $1`, bookingID)
	if err != nil {
		t.Fatalf("mark gym cancelled: %v", err)
	}

	resp := doJSONRequest(t, server.Client(), http.MethodPost, server.URL+"/v1/ratings", token, map[string]any{
		"booking_id": bookingID,
		"stars":      3,
	})
	if resp.StatusCode != http.StatusForbidden {
		t.Fatalf("expected 403, got %d body=%s", resp.StatusCode, readBody(t, resp))
	}

	var apiErr api.ErrorResponse
	decodeJSONResponse(t, resp, &apiErr)
	if apiErr.Code != "RATING_NOT_ALLOWED_GYM_CANCELLED" {
		t.Fatalf("expected RATING_NOT_ALLOWED_GYM_CANCELLED, got %s", apiErr.Code)
	}
}

func TestCreateInstructorRatingExpiredWindowIntegration(t *testing.T) {
	server, pool := newIntegrationServer(t)

	phone := fmt.Sprintf("+7923%07d", time.Now().UnixNano()%10_000_000)
	client, token := registerBookingClient(t, server, phone)
	t.Cleanup(func() { cleanupBookingClient(t, pool, phone) })

	bookingID := insertCompletedBooking(t, pool, client.Id)
	_, err := pool.Exec(context.Background(), `
		UPDATE training_slots
		SET starts_at = $2
		FROM bookings b
		WHERE b.slot_id = training_slots.id AND b.id = $1`,
		bookingID, time.Now().UTC().Add(-72*time.Hour))
	if err != nil {
		t.Fatalf("move slot to past: %v", err)
	}

	resp := doJSONRequest(t, server.Client(), http.MethodPost, server.URL+"/v1/ratings", token, map[string]any{
		"booking_id": bookingID,
		"stars":      4,
	})
	if resp.StatusCode != http.StatusForbidden {
		t.Fatalf("expected 403, got %d body=%s", resp.StatusCode, readBody(t, resp))
	}

	var apiErr api.ErrorResponse
	decodeJSONResponse(t, resp, &apiErr)
	if apiErr.Code != "RATING_WINDOW_EXPIRED" {
		t.Fatalf("expected RATING_WINDOW_EXPIRED, got %s", apiErr.Code)
	}
}

func insertCompletedBooking(t *testing.T, pool *pgxpool.Pool, clientID string) string {
	t.Helper()

	slotID := fmt.Sprintf("ef000000-0000-4000-8000-%012d", time.Now().UnixNano()%1_000_000_000_000)
	bookingID := fmt.Sprintf("b0000000-0000-4000-8000-%012d", (time.Now().UnixNano()+1)%1_000_000_000_000)
	startsAt := time.Now().UTC().Add(-2 * time.Hour)

	_, err := pool.Exec(context.Background(), `
		INSERT INTO training_slots (
			id, starts_at, duration_minutes, capacity, free_spots,
			training_price, rental_tariff, slot_status, address,
			zone_id, instructor_id, venue_id
		) VALUES (
			$1, $2, 90, 8, 7, 1500.00, 400.00, 'active', 'г. Москва, ул. Скалолазная, д. 1',
			'b0000000-0000-4000-8000-000000000001',
			'c0000000-0000-4000-8000-000000000001',
			'a0000000-0000-4000-8000-000000000001'
		)`,
		slotID, startsAt,
	)
	if err != nil {
		t.Fatalf("insert slot: %v", err)
	}

	_, err = pool.Exec(context.Background(), `
		INSERT INTO bookings (id, client_id, slot_id, booking_status, uses_own_equipment)
		VALUES ($1, $2, $3, 'completed', TRUE)`,
		bookingID, clientID, slotID,
	)
	if err != nil {
		t.Fatalf("insert booking: %v", err)
	}

	_, err = pool.Exec(context.Background(), `
		INSERT INTO payments (booking_id, training_amount, rental_amount, total_amount, payment_status)
		VALUES ($1, 1500.00, 0, 1500.00, 'unpaid')`,
		bookingID,
	)
	if err != nil {
		t.Fatalf("insert payment: %v", err)
	}

	t.Cleanup(func() {
		_, _ = pool.Exec(context.Background(), "UPDATE instructors SET average_rating = NULL WHERE id = 'c0000000-0000-4000-8000-000000000001'")
		_, _ = pool.Exec(context.Background(), "DELETE FROM instructor_ratings WHERE booking_id = $1", bookingID)
		_, _ = pool.Exec(context.Background(), "DELETE FROM payments WHERE booking_id = $1", bookingID)
		_, _ = pool.Exec(context.Background(), "DELETE FROM bookings WHERE id = $1", bookingID)
		_, _ = pool.Exec(context.Background(), "DELETE FROM training_slots WHERE id = $1", slotID)
	})

	return bookingID
}
