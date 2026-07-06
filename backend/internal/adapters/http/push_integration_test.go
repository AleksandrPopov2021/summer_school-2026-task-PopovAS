package httpx_test

import (
	"context"
	"fmt"
	"net/http"
	"testing"
	"time"

	pushadapter "github.com/vertical-climbing/backend/internal/adapters/push"
	"github.com/vertical-climbing/backend/internal/adapters/postgres"
	"github.com/vertical-climbing/backend/internal/adapters/http/api"
	apppush "github.com/vertical-climbing/backend/internal/application/push"
	appref "github.com/vertical-climbing/backend/internal/application/reference"
	appworker "github.com/vertical-climbing/backend/internal/application/worker"
	"github.com/jackc/pgx/v5/pgxpool"
)

func TestRegisterPushTokenIntegration(t *testing.T) {
	server, pool := newIntegrationServer(t)

	phone := fmt.Sprintf("+7911%07d", time.Now().UnixNano()%10_000_000)
	_, token := registerBookingClient(t, server, phone)
	t.Cleanup(func() { cleanupBookingClient(t, pool, phone) })

	resp := doJSONRequest(t, server.Client(), http.MethodPut, server.URL+"/v1/devices/push-token", token, map[string]string{
		"token":    "integration-test-token",
		"platform": "android",
	})
	if resp.StatusCode != http.StatusNoContent {
		t.Fatalf("register push token status: %d body=%s", resp.StatusCode, readBody(t, resp))
	}

	var count int
	err := pool.QueryRow(context.Background(), `
		SELECT COUNT(*)
		FROM device_push_tokens dpt
		JOIN clients c ON c.id = dpt.client_id
		WHERE c.phone = $1 AND dpt.token = $2 AND dpt.platform = 'android'`,
		phone, "integration-test-token",
	).Scan(&count)
	if err != nil {
		t.Fatalf("query push token: %v", err)
	}
	if count != 1 {
		t.Fatalf("expected one push token row, got %d", count)
	}
}

func TestRegisterPushTokenUnauthorizedIntegration(t *testing.T) {
	server, _ := newIntegrationServer(t)

	resp := doJSONRequest(t, server.Client(), http.MethodPut, server.URL+"/v1/devices/push-token", "", map[string]string{
		"token":    "integration-test-token",
		"platform": "android",
	})
	if resp.StatusCode != http.StatusUnauthorized {
		t.Fatalf("expected 401, got %d body=%s", resp.StatusCode, readBody(t, resp))
	}
}

func TestCompleteBookingGrantsLoyaltyIntegration(t *testing.T) {
	server, pool := newIntegrationServer(t)

	phone := fmt.Sprintf("+7912%07d", time.Now().UnixNano()%10_000_000)
	client, token := registerBookingClient(t, server, phone)
	t.Cleanup(func() { cleanupBookingClient(t, pool, phone) })

	consentResp := doJSONRequest(t, server.Client(), http.MethodPatch, server.URL+"/v1/clients/me", token, map[string]bool{
		"risk_consent_accepted": true,
	})
	if consentResp.StatusCode != http.StatusOK {
		t.Fatalf("patch consent status: %d", consentResp.StatusCode)
	}

	slotID := insertPastBookableSlot(t, pool)
	bookResp := doJSONRequest(t, server.Client(), http.MethodPost, server.URL+"/v1/bookings", token, map[string]any{
		"slot_id":            slotID,
		"uses_own_equipment": true,
	})
	if bookResp.StatusCode != http.StatusCreated {
		t.Fatalf("create booking status: %d body=%s", bookResp.StatusCode, readBody(t, bookResp))
	}

	_, err := pool.Exec(context.Background(), `
		UPDATE training_slots
		SET starts_at = $2
		WHERE id = $1`, slotID, time.Now().UTC().Add(-2*time.Hour))
	if err != nil {
		t.Fatalf("move slot to past: %v", err)
	}

	_, err = pool.Exec(context.Background(), `
		UPDATE clients
		SET completed_visits_count = 9, is_loyal_client = FALSE, loyalty_discount = NULL
		WHERE id = $1`, client.Id)
	if err != nil {
		t.Fatalf("seed visits: %v", err)
	}

	referenceRepo := postgres.NewReferenceRepository(pool)
	notificationRepo := postgres.NewNotificationRepository(pool)
	deviceRepo := postgres.NewDeviceRepository(pool)
	workerRepo := postgres.NewWorkerRepository(pool)
	pushService := apppush.NewService(pushadapter.NewNoopSender(), deviceRepo, notificationRepo, workerRepo)
	workerService := appworker.NewService(workerRepo, appref.NewService(referenceRepo), pushService)

	if err := workerService.CompleteBookings(context.Background()); err != nil {
		t.Fatalf("complete bookings: %v", err)
	}

	meResp := doJSONRequest(t, server.Client(), http.MethodGet, server.URL+"/v1/clients/me", token, nil)
	if meResp.StatusCode != http.StatusOK {
		t.Fatalf("get me status: %d body=%s", meResp.StatusCode, readBody(t, meResp))
	}

	var profile api.Client
	decodeJSONResponse(t, meResp, &profile)
	if profile.CompletedVisitsCount != 10 {
		t.Fatalf("expected 10 completed visits, got %d", profile.CompletedVisitsCount)
	}
	if !profile.IsLoyalClient {
		t.Fatal("expected loyal client badge after completion")
	}
	if profile.LoyaltyDiscount == nil {
		t.Fatal("expected loyalty discount after completion")
	}
}

func insertPastBookableSlot(t *testing.T, pool *pgxpool.Pool) string {
	t.Helper()

	var slotID string
	startsAt := time.Now().UTC().Add(24 * time.Hour)
	err := pool.QueryRow(context.Background(), `
		INSERT INTO training_slots (
			zone_id, instructor_id, venue_id, starts_at, duration_minutes,
			capacity, free_spots, training_price, slot_status, address
		)
		SELECT
			z.id,
			i.id,
			v.id,
			$1,
			90,
			8,
			1,
			1500.00,
			'active',
			v.address
		FROM training_zones z
		CROSS JOIN instructors i
		CROSS JOIN gym_venues v
		WHERE z.format_type = 'bouldering_instruction'
		LIMIT 1
		RETURNING id`, startsAt).Scan(&slotID)
	if err != nil {
		t.Fatalf("insert past slot: %v", err)
	}

	t.Cleanup(func() { cleanupSlot(pool, slotID) })

	return slotID
}
