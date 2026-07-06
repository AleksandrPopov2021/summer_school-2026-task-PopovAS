package httpx_test

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"net/http/httptest"
	"sync"
	"testing"
	"time"

	"github.com/vertical-climbing/backend/internal/adapters/http/api"
	"github.com/jackc/pgx/v5/pgxpool"
)

func TestCreateBookingIntegration(t *testing.T) {
	server, pool := newIntegrationServer(t)

	client, token := registerBookingClient(t, server, fmt.Sprintf("+7901%07d", time.Now().UnixNano()%10_000_000))
	t.Cleanup(func() { cleanupBookingClient(t, pool, client.Phone) })

	consentResp := doJSONRequest(t, server.Client(), http.MethodPatch, server.URL+"/v1/clients/me", token, map[string]bool{
		"risk_consent_accepted": true,
	})
	if consentResp.StatusCode != http.StatusOK {
		t.Fatalf("patch consent status: %d body=%s", consentResp.StatusCode, readBody(t, consentResp))
	}

	slotID := findBoulderSlotWithSpots(t, server)
	bookResp := doJSONRequest(t, server.Client(), http.MethodPost, server.URL+"/v1/bookings", token, map[string]any{
		"slot_id":            slotID,
		"uses_own_equipment": true,
	})
	if bookResp.StatusCode != http.StatusCreated {
		t.Fatalf("create booking status: %d body=%s", bookResp.StatusCode, readBody(t, bookResp))
	}

	var booking api.BookingDetail
	decodeJSONResponse(t, bookResp, &booking)
	if booking.BookingStatus != api.BookingStatusBooked {
		t.Fatalf("expected booked status")
	}
	if booking.Payment.PaymentStatus != api.PaymentStatusUnpaid {
		t.Fatalf("expected unpaid payment")
	}
}

func TestCreateBookingWithoutConsentIntegration(t *testing.T) {
	server, pool := newIntegrationServer(t)

	phone := fmt.Sprintf("+7902%07d", time.Now().UnixNano()%10_000_000)
	_, token := registerBookingClient(t, server, phone)
	t.Cleanup(func() { cleanupBookingClient(t, pool, phone) })

	slotID := findBoulderSlotWithSpots(t, server)
	bookResp := doJSONRequest(t, server.Client(), http.MethodPost, server.URL+"/v1/bookings", token, map[string]any{
		"slot_id":            slotID,
		"uses_own_equipment": true,
	})
	if bookResp.StatusCode != http.StatusForbidden {
		t.Fatalf("expected 403, got %d body=%s", bookResp.StatusCode, readBody(t, bookResp))
	}
}

func TestCreateBookingWithoutClearanceOnRopeIntegration(t *testing.T) {
	server, pool := newIntegrationServer(t)

	client, token := registerBookingClient(t, server, fmt.Sprintf("+7903%07d", time.Now().UnixNano()%10_000_000))
	slotID := insertRopeSlotWithSpots(t, pool)
	t.Cleanup(func() {
		cleanupBookingClient(t, pool, client.Phone)
		_, _ = pool.Exec(context.Background(), "DELETE FROM training_slots WHERE id = $1", slotID)
	})

	consentResp := doJSONRequest(t, server.Client(), http.MethodPatch, server.URL+"/v1/clients/me", token, map[string]bool{
		"risk_consent_accepted": true,
	})
	if consentResp.StatusCode != http.StatusOK {
		t.Fatalf("patch consent status: %d", consentResp.StatusCode)
	}

	bookResp := doJSONRequest(t, server.Client(), http.MethodPost, server.URL+"/v1/bookings", token, map[string]any{
		"slot_id":            slotID,
		"uses_own_equipment": true,
	})
	if bookResp.StatusCode != http.StatusForbidden {
		t.Fatalf("expected 403, got %d body=%s", bookResp.StatusCode, readBody(t, bookResp))
	}

	var apiErr api.ErrorResponse
	decodeJSONResponse(t, bookResp, &apiErr)
	if apiErr.Code != "INSTRUCTOR_CLEARANCE_REQUIRED" {
		t.Fatalf("expected INSTRUCTOR_CLEARANCE_REQUIRED, got %s", apiErr.Code)
	}
}

func TestCreateBookingParallelLastSpotIntegration(t *testing.T) {
	server, pool := newIntegrationServer(t)

	slotID := insertSingleSpotSlot(t, pool)

	clientA, tokenA := registerBookingClient(t, server, fmt.Sprintf("+7904%07d", time.Now().UnixNano()%10_000_000))
	clientB, tokenB := registerBookingClient(t, server, fmt.Sprintf("+7905%07d", (time.Now().UnixNano()+1)%10_000_000))
	t.Cleanup(func() {
		cleanupBookingClient(t, pool, clientA.Phone)
		cleanupBookingClient(t, pool, clientB.Phone)
		_, _ = pool.Exec(context.Background(), "DELETE FROM training_slots WHERE id = $1", slotID)
	})

	for _, token := range []string{tokenA, tokenB} {
		consentResp := doJSONRequest(t, server.Client(), http.MethodPatch, server.URL+"/v1/clients/me", token, map[string]bool{
			"risk_consent_accepted": true,
		})
		if consentResp.StatusCode != http.StatusOK {
			t.Fatalf("patch consent status: %d", consentResp.StatusCode)
		}
	}

	body, _ := json.Marshal(map[string]any{
		"slot_id":            slotID,
		"uses_own_equipment": true,
	})

	var wg sync.WaitGroup
	results := make(chan int, 2)
	for _, token := range []string{tokenA, tokenB} {
		wg.Add(1)
		go func(token string) {
			defer wg.Done()
			req, err := http.NewRequest(http.MethodPost, server.URL+"/v1/bookings", bytes.NewReader(body))
			if err != nil {
				results <- 0
				return
			}
			req.Header.Set("Content-Type", "application/json")
			req.Header.Set("Authorization", "Bearer "+token)
			resp, err := server.Client().Do(req)
			if err != nil {
				results <- 0
				return
			}
			defer resp.Body.Close()
			results <- resp.StatusCode
		}(token)
	}
	wg.Wait()
	close(results)

	var created, conflict int
	for status := range results {
		switch status {
		case http.StatusCreated:
			created++
		case http.StatusConflict:
			conflict++
		}
	}

	if created != 1 || conflict != 1 {
		t.Fatalf("expected one 201 and one 409, got created=%d conflict=%d", created, conflict)
	}
}

func TestCreateBookingForbiddenBySanctionsIntegration(t *testing.T) {
	server, pool := newIntegrationServer(t)

	client, token := registerBookingClient(t, server, fmt.Sprintf("+7917%07d", time.Now().UnixNano()%10_000_000))
	t.Cleanup(func() { cleanupBookingClient(t, pool, client.Phone) })

	consentResp := doJSONRequest(t, server.Client(), http.MethodPatch, server.URL+"/v1/clients/me", token, map[string]bool{
		"risk_consent_accepted": true,
	})
	if consentResp.StatusCode != http.StatusOK {
		t.Fatalf("patch consent status: %d", consentResp.StatusCode)
	}

	_, err := pool.Exec(context.Background(), `
		UPDATE clients
		SET late_cancellation_count = 2, no_show_count = 1
		WHERE id = $1`, client.Id)
	if err != nil {
		t.Fatalf("set violation counters: %v", err)
	}

	slotID := findBoulderSlotWithSpots(t, server)
	bookResp := doJSONRequest(t, server.Client(), http.MethodPost, server.URL+"/v1/bookings", token, map[string]any{
		"slot_id":            slotID,
		"uses_own_equipment": true,
	})
	if bookResp.StatusCode != http.StatusForbidden {
		t.Fatalf("expected 403, got %d body=%s", bookResp.StatusCode, readBody(t, bookResp))
	}

	var apiErr api.ErrorResponse
	decodeJSONResponse(t, bookResp, &apiErr)
	if apiErr.Code != "BOOKING_FORBIDDEN_SANCTIONS" {
		t.Fatalf("expected BOOKING_FORBIDDEN_SANCTIONS, got %s", apiErr.Code)
	}
}

func TestCreateBookingFiftyConcurrentLastSpotIntegration(t *testing.T) {
	if testing.Short() {
		t.Skip("skipping load test in short mode")
	}

	server, pool := newIntegrationServer(t)

	slotID := insertSingleSpotSlot(t, pool)
	client, token := registerBookingClient(t, server, fmt.Sprintf("+7918%07d", time.Now().UnixNano()%10_000_000))
	t.Cleanup(func() {
		cleanupBookingClient(t, pool, client.Phone)
		_, _ = pool.Exec(context.Background(), "DELETE FROM training_slots WHERE id = $1", slotID)
	})

	consentResp := doJSONRequest(t, server.Client(), http.MethodPatch, server.URL+"/v1/clients/me", token, map[string]bool{
		"risk_consent_accepted": true,
	})
	if consentResp.StatusCode != http.StatusOK {
		t.Fatalf("patch consent status: %d", consentResp.StatusCode)
	}

	body, err := json.Marshal(map[string]any{
		"slot_id":            slotID,
		"uses_own_equipment": true,
	})
	if err != nil {
		t.Fatalf("marshal body: %v", err)
	}

	const workers = 50
	var wg sync.WaitGroup
	results := make(chan int, workers)

	for i := 0; i < workers; i++ {
		wg.Add(1)
		go func() {
			defer wg.Done()
			req, err := http.NewRequest(http.MethodPost, server.URL+"/v1/bookings", bytes.NewReader(body))
			if err != nil {
				results <- 0
				return
			}
			req.Header.Set("Content-Type", "application/json")
			req.Header.Set("Authorization", "Bearer "+token)
			resp, err := server.Client().Do(req)
			if err != nil {
				results <- 0
				return
			}
			results <- resp.StatusCode
			resp.Body.Close()
		}()
	}

	wg.Wait()
	close(results)

	created, conflict, other := 0, 0, 0
	for status := range results {
		switch status {
		case http.StatusCreated:
			created++
		case http.StatusConflict:
			conflict++
		case 0:
			other++
		default:
			other++
		}
	}

	if created != 1 || conflict != workers-1 {
		t.Fatalf("expected 1 created and %d conflicts, got created=%d conflict=%d other=%d", workers-1, created, conflict, other)
	}
}

func registerBookingClient(t *testing.T, server *httptest.Server, phone string) (api.Client, string) {
	t.Helper()
	resp := doJSONRequest(t, server.Client(), http.MethodPost, server.URL+"/v1/clients", "", map[string]string{
		"phone":      phone,
		"full_name":  "Booking Test",
		"birth_date": "1994-01-01",
	})
	if resp.StatusCode != http.StatusCreated {
		t.Fatalf("register status: %d body=%s", resp.StatusCode, readBody(t, resp))
	}
	var registration api.ClientRegistrationResponse
	decodeJSONResponse(t, resp, &registration)
	return registration.Client, registration.AccessToken
}

func findBoulderSlotWithSpots(t *testing.T, server *httptest.Server) string {
	t.Helper()
	resp := doJSONRequest(t, server.Client(), http.MethodGet, server.URL+"/v1/slots", "", nil)
	if resp.StatusCode != http.StatusOK {
		t.Fatalf("list slots status: %d", resp.StatusCode)
	}
	var payload api.TrainingSlotList
	decodeJSONResponse(t, resp, &payload)
	for _, item := range payload.Items {
		if item.Zone.FormatType == api.FormatTypeBouldering &&
			item.FreeSpots > 0 &&
			item.SlotStatus == api.SlotStatusActive &&
			item.Availability.WithinBookingWindow {
			return item.Id
		}
	}
	t.Fatalf("no boulder slot with free spots")
	return ""
}

func findRopeSlotWithSpots(t *testing.T, server *httptest.Server) string {
	t.Helper()
	resp := doJSONRequest(t, server.Client(), http.MethodGet, server.URL+"/v1/slots", "", nil)
	if resp.StatusCode != http.StatusOK {
		t.Fatalf("list slots status: %d", resp.StatusCode)
	}
	var payload api.TrainingSlotList
	decodeJSONResponse(t, resp, &payload)
	for _, item := range payload.Items {
		if item.Zone.FormatType == api.FormatTypeRopeRoutes &&
			item.FreeSpots > 0 &&
			item.SlotStatus == api.SlotStatusActive &&
			item.Availability.WithinBookingWindow {
			return item.Id
		}
	}
	t.Skip("no rope slot with free spots in seed")
	return ""
}

func insertSingleSpotSlot(t *testing.T, pool *pgxpool.Pool) string {
	t.Helper()
	slotID := fmt.Sprintf("ed000000-0000-4000-8000-%012d", time.Now().UnixNano()%1_000_000_000_000)
	startsAt := time.Now().Add(3 * time.Hour)
	_, err := pool.Exec(context.Background(), `
		INSERT INTO training_slots (
			id, starts_at, duration_minutes, capacity, free_spots,
			training_price, rental_tariff, slot_status, address,
			zone_id, instructor_id, venue_id
		) VALUES (
			$1, $2, 90, 1, 1, 1200.00, 400.00, 'active', $3,
			'b0000000-0000-4000-8000-000000000001',
			'c0000000-0000-4000-8000-000000000001',
			'a0000000-0000-4000-8000-000000000001'
		)`,
		slotID, startsAt, "г. Москва, ул. Скалолазная, д. 1",
	)
	if err != nil {
		t.Fatalf("insert test slot: %v", err)
	}
	return slotID
}

func insertRopeSlotWithSpots(t *testing.T, pool *pgxpool.Pool) string {
	t.Helper()
	slotID := fmt.Sprintf("ee000000-0000-4000-8000-%012d", time.Now().UnixNano()%1_000_000_000_000)
	startsAt := time.Now().Add(3 * time.Hour)
	_, err := pool.Exec(context.Background(), `
		INSERT INTO training_slots (
			id, starts_at, duration_minutes, capacity, free_spots,
			training_price, rental_tariff, slot_status, address,
			zone_id, instructor_id, venue_id
		) VALUES (
			$1, $2, 90, 8, 5, 1500.00, 500.00, 'active', $3,
			'b0000000-0000-4000-8000-000000000002',
			'c0000000-0000-4000-8000-000000000003',
			'a0000000-0000-4000-8000-000000000001'
		)`,
		slotID, startsAt, "г. Москва, ул. Скалолазная, д. 1",
	)
	if err != nil {
		t.Fatalf("insert rope test slot: %v", err)
	}
	return slotID
}

func TestCancelBookingIntegration(t *testing.T) {
	server, pool := newIntegrationServer(t)

	client, token := registerBookingClient(t, server, fmt.Sprintf("+7906%07d", time.Now().UnixNano()%10_000_000))
	t.Cleanup(func() { cleanupBookingClient(t, pool, client.Phone) })

	consentResp := doJSONRequest(t, server.Client(), http.MethodPatch, server.URL+"/v1/clients/me", token, map[string]bool{
		"risk_consent_accepted": true,
	})
	if consentResp.StatusCode != http.StatusOK {
		t.Fatalf("patch consent status: %d", consentResp.StatusCode)
	}

	slotID, freeSpotsBefore := insertTimedSlot(t, pool, 3*time.Hour, 4)
	bookingID := createBookingOnSlot(t, server, token, slotID)

	cancelResp := doJSONRequest(t, server.Client(), http.MethodDelete, server.URL+"/v1/bookings/"+bookingID, token, nil)
	if cancelResp.StatusCode != http.StatusOK {
		t.Fatalf("cancel booking status: %d body=%s", cancelResp.StatusCode, readBody(t, cancelResp))
	}

	var cancelled api.BookingDetail
	decodeJSONResponse(t, cancelResp, &cancelled)
	if cancelled.BookingStatus != api.BookingStatusCancelledByClient {
		t.Fatalf("expected cancelled_by_client, got %s", cancelled.BookingStatus)
	}
	if cancelled.CancellationPolicy != nil && cancelled.CancellationPolicy.CanCancel {
		t.Fatalf("expected can_cancel false after cancellation")
	}

	freeSpotsAfter := getSlotFreeSpots(t, pool, slotID)
	if freeSpotsAfter != freeSpotsBefore {
		t.Fatalf("expected free spots restored: before=%d after=%d", freeSpotsBefore, freeSpotsAfter)
	}
}

func TestCancelBookingLateCancellationIntegration(t *testing.T) {
	server, pool := newIntegrationServer(t)

	client, token := registerBookingClient(t, server, fmt.Sprintf("+7907%07d", time.Now().UnixNano()%10_000_000))
	t.Cleanup(func() { cleanupBookingClient(t, pool, client.Phone) })

	consentResp := doJSONRequest(t, server.Client(), http.MethodPatch, server.URL+"/v1/clients/me", token, map[string]bool{
		"risk_consent_accepted": true,
	})
	if consentResp.StatusCode != http.StatusOK {
		t.Fatalf("patch consent status: %d", consentResp.StatusCode)
	}

	slotID := insertTimedSlotOnly(t, pool, 90*time.Minute, 3)
	bookingID := createBookingOnSlot(t, server, token, slotID)

	cancelResp := doJSONRequest(t, server.Client(), http.MethodDelete, server.URL+"/v1/bookings/"+bookingID, token, nil)
	if cancelResp.StatusCode != http.StatusOK {
		t.Fatalf("cancel booking status: %d body=%s", cancelResp.StatusCode, readBody(t, cancelResp))
	}

	meResp := doJSONRequest(t, server.Client(), http.MethodGet, server.URL+"/v1/clients/me", token, nil)
	if meResp.StatusCode != http.StatusOK {
		t.Fatalf("get client status: %d", meResp.StatusCode)
	}
	var me api.Client
	decodeJSONResponse(t, meResp, &me)
	if me.LateCancellationCount != 1 {
		t.Fatalf("expected late_cancellation_count=1, got %d", me.LateCancellationCount)
	}
}

func TestCancelBookingForbiddenIntegration(t *testing.T) {
	server, pool := newIntegrationServer(t)

	client, token := registerBookingClient(t, server, fmt.Sprintf("+7908%07d", time.Now().UnixNano()%10_000_000))
	t.Cleanup(func() { cleanupBookingClient(t, pool, client.Phone) })

	consentResp := doJSONRequest(t, server.Client(), http.MethodPatch, server.URL+"/v1/clients/me", token, map[string]bool{
		"risk_consent_accepted": true,
	})
	if consentResp.StatusCode != http.StatusOK {
		t.Fatalf("patch consent status: %d", consentResp.StatusCode)
	}

	slotID := insertTimedSlotOnly(t, pool, 59*time.Minute, 3)
	bookingID := createBookingOnSlot(t, server, token, slotID)

	cancelResp := doJSONRequest(t, server.Client(), http.MethodDelete, server.URL+"/v1/bookings/"+bookingID, token, nil)
	if cancelResp.StatusCode != http.StatusForbidden {
		t.Fatalf("expected 403, got %d body=%s", cancelResp.StatusCode, readBody(t, cancelResp))
	}
}

func TestListBookingsIntegration(t *testing.T) {
	server, pool := newIntegrationServer(t)

	client, token := registerBookingClient(t, server, fmt.Sprintf("+7909%07d", time.Now().UnixNano()%10_000_000))
	t.Cleanup(func() { cleanupBookingClient(t, pool, client.Phone) })

	consentResp := doJSONRequest(t, server.Client(), http.MethodPatch, server.URL+"/v1/clients/me", token, map[string]bool{
		"risk_consent_accepted": true,
	})
	if consentResp.StatusCode != http.StatusOK {
		t.Fatalf("patch consent status: %d", consentResp.StatusCode)
	}

	slotID := insertTimedSlotOnly(t, pool, 4*time.Hour, 3)
	createBookingOnSlot(t, server, token, slotID)

	listResp := doJSONRequest(t, server.Client(), http.MethodGet, server.URL+"/v1/bookings", token, nil)
	if listResp.StatusCode != http.StatusOK {
		t.Fatalf("list bookings status: %d body=%s", listResp.StatusCode, readBody(t, listResp))
	}

	var list api.BookingList
	decodeJSONResponse(t, listResp, &list)
	if len(list.Items) == 0 {
		t.Fatalf("expected at least one booked item")
	}
	if list.Items[0].CancellationPolicy == nil {
		t.Fatalf("expected cancellation_policy in booking summary")
	}
}

const (
	testEquipmentShoesID        = "d0000000-0000-4000-8000-000000000001"
	testEquipmentHarnessID      = "d0000000-0000-4000-8000-000000000002"
	testCancellationReasonID    = "e0000000-0000-4000-8000-000000000001"
	testInternalAPIKey          = "dev-internal-key"
)

func TestUpdateBookingRentalIntegration(t *testing.T) {
	server, pool := newIntegrationServer(t)

	client, token := registerBookingClient(t, server, fmt.Sprintf("+7910%07d", time.Now().UnixNano()%10_000_000))
	t.Cleanup(func() { cleanupBookingClient(t, pool, client.Phone) })

	consentResp := doJSONRequest(t, server.Client(), http.MethodPatch, server.URL+"/v1/clients/me", token, map[string]bool{
		"risk_consent_accepted": true,
	})
	if consentResp.StatusCode != http.StatusOK {
		t.Fatalf("patch consent status: %d", consentResp.StatusCode)
	}

	slotID := insertSlotWithRentalStock(t, pool, 4*time.Hour, 3, testEquipmentShoesID, 5)
	bookingID := createBookingOnSlot(t, server, token, slotID)

	patchResp := doJSONRequest(t, server.Client(), http.MethodPatch, server.URL+"/v1/bookings/"+bookingID+"/rental", token, map[string]any{
		"uses_own_equipment": false,
		"rental_lines": []map[string]any{
			{"equipment_type_id": testEquipmentShoesID, "quantity": 1},
		},
	})
	if patchResp.StatusCode != http.StatusOK {
		t.Fatalf("update rental status: %d body=%s", patchResp.StatusCode, readBody(t, patchResp))
	}

	var updated api.BookingDetail
	decodeJSONResponse(t, patchResp, &updated)
	if len(updated.RentalLines) != 1 {
		t.Fatalf("expected one rental line, got %d", len(updated.RentalLines))
	}
	if updated.RentalLines[0].EquipmentTypeId != testEquipmentShoesID {
		t.Fatalf("unexpected equipment type: %s", updated.RentalLines[0].EquipmentTypeId)
	}
	if updated.Payment.RentalAmount.IsZero() {
		t.Fatalf("expected non-zero rental amount")
	}
}

func TestUpdateBookingRentalUnavailableIntegration(t *testing.T) {
	server, pool := newIntegrationServer(t)

	client, token := registerBookingClient(t, server, fmt.Sprintf("+7911%07d", time.Now().UnixNano()%10_000_000))
	t.Cleanup(func() { cleanupBookingClient(t, pool, client.Phone) })

	consentResp := doJSONRequest(t, server.Client(), http.MethodPatch, server.URL+"/v1/clients/me", token, map[string]bool{
		"risk_consent_accepted": true,
	})
	if consentResp.StatusCode != http.StatusOK {
		t.Fatalf("patch consent status: %d", consentResp.StatusCode)
	}

	slotID := insertSlotWithRentalStock(t, pool, 4*time.Hour, 3, testEquipmentShoesID, 1)
	bookingID := createBookingWithRental(t, server, token, slotID, testEquipmentShoesID, 1)

	patchResp := doJSONRequest(t, server.Client(), http.MethodPatch, server.URL+"/v1/bookings/"+bookingID+"/rental", token, map[string]any{
		"uses_own_equipment": false,
		"rental_lines": []map[string]any{
			{"equipment_type_id": testEquipmentShoesID, "quantity": 2},
		},
	})
	if patchResp.StatusCode != http.StatusUnprocessableEntity {
		t.Fatalf("expected 422, got %d body=%s", patchResp.StatusCode, readBody(t, patchResp))
	}
}

func TestUpdateBookingRentalForbiddenIntegration(t *testing.T) {
	server, pool := newIntegrationServer(t)

	client, token := registerBookingClient(t, server, fmt.Sprintf("+7912%07d", time.Now().UnixNano()%10_000_000))
	t.Cleanup(func() { cleanupBookingClient(t, pool, client.Phone) })

	consentResp := doJSONRequest(t, server.Client(), http.MethodPatch, server.URL+"/v1/clients/me", token, map[string]bool{
		"risk_consent_accepted": true,
	})
	if consentResp.StatusCode != http.StatusOK {
		t.Fatalf("patch consent status: %d", consentResp.StatusCode)
	}

	slotID := insertTimedSlotOnly(t, pool, 4*time.Hour, 3)
	bookingID := createBookingOnSlot(t, server, token, slotID)

	cancelResp := doJSONRequest(t, server.Client(), http.MethodDelete, server.URL+"/v1/bookings/"+bookingID, token, nil)
	if cancelResp.StatusCode != http.StatusOK {
		t.Fatalf("cancel booking status: %d", cancelResp.StatusCode)
	}

	patchResp := doJSONRequest(t, server.Client(), http.MethodPatch, server.URL+"/v1/bookings/"+bookingID+"/rental", token, map[string]any{
		"uses_own_equipment": false,
		"rental_lines": []map[string]any{
			{"equipment_type_id": testEquipmentShoesID, "quantity": 1},
		},
	})
	if patchResp.StatusCode != http.StatusForbidden {
		t.Fatalf("expected 403, got %d body=%s", patchResp.StatusCode, readBody(t, patchResp))
	}

	_ = slotID
}

func createBookingWithRental(t *testing.T, server *httptest.Server, token, slotID, equipmentID string, quantity int) string {
	t.Helper()
	bookResp := doJSONRequest(t, server.Client(), http.MethodPost, server.URL+"/v1/bookings", token, map[string]any{
		"slot_id":            slotID,
		"uses_own_equipment": true,
		"rental_lines": []map[string]any{
			{"equipment_type_id": equipmentID, "quantity": quantity},
		},
	})
	if bookResp.StatusCode != http.StatusCreated {
		t.Fatalf("create booking status: %d body=%s", bookResp.StatusCode, readBody(t, bookResp))
	}
	var booking api.BookingDetail
	decodeJSONResponse(t, bookResp, &booking)
	return booking.Id
}

func insertSlotWithRentalStock(t *testing.T, pool *pgxpool.Pool, startsIn time.Duration, freeSpots int32, equipmentID string, quantity int32) string {
	t.Helper()
	slotID := insertTimedSlotOnly(t, pool, startsIn, freeSpots)
	_, err := pool.Exec(context.Background(), `
		INSERT INTO slot_rental_availability (id, slot_id, equipment_type_id, available_quantity)
		VALUES ($1, $2, $3, $4)`,
		fmt.Sprintf("ef000000-0000-4000-8000-%012d", time.Now().UnixNano()%1_000_000_000_000),
		slotID, equipmentID, quantity,
	)
	if err != nil {
		t.Fatalf("insert rental availability: %v", err)
	}
	return slotID
}

func createBookingOnSlot(t *testing.T, server *httptest.Server, token, slotID string) string {
	t.Helper()
	bookResp := doJSONRequest(t, server.Client(), http.MethodPost, server.URL+"/v1/bookings", token, map[string]any{
		"slot_id":            slotID,
		"uses_own_equipment": true,
	})
	if bookResp.StatusCode != http.StatusCreated {
		t.Fatalf("create booking status: %d body=%s", bookResp.StatusCode, readBody(t, bookResp))
	}
	var booking api.BookingDetail
	decodeJSONResponse(t, bookResp, &booking)
	return booking.Id
}

func insertTimedSlot(t *testing.T, pool *pgxpool.Pool, startsIn time.Duration, freeSpots int32) (string, int32) {
	t.Helper()
	slotID := insertTimedSlotOnly(t, pool, startsIn, freeSpots)
	return slotID, freeSpots
}

func insertTimedSlotOnly(t *testing.T, pool *pgxpool.Pool, startsIn time.Duration, freeSpots int32) string {
	t.Helper()
	slotID := fmt.Sprintf("ed000000-0000-4000-8000-%012d", time.Now().UnixNano()%1_000_000_000_000)
	startsAt := time.Now().Add(startsIn)
	_, err := pool.Exec(context.Background(), `
		INSERT INTO training_slots (
			id, starts_at, duration_minutes, capacity, free_spots,
			training_price, rental_tariff, slot_status, address,
			zone_id, instructor_id, venue_id
		) VALUES (
			$1, $2, 90, $3, $3, 1200.00, 400.00, 'active', $4,
			'b0000000-0000-4000-8000-000000000001',
			'c0000000-0000-4000-8000-000000000001',
			'a0000000-0000-4000-8000-000000000001'
		)`,
		slotID, startsAt, freeSpots, "г. Москва, ул. Скалолазная, д. 1",
	)
	if err != nil {
		t.Fatalf("insert test slot: %v", err)
	}
	t.Cleanup(func() { cleanupSlot(pool, slotID) })
	return slotID
}

// cleanupSlot removes a test-created slot and everything referencing it so that
// leftover slots do not pollute the seeded schedule used by other tests.
func cleanupSlot(pool *pgxpool.Pool, slotID string) {
	ctx := context.Background()
	_, _ = pool.Exec(ctx, `DELETE FROM instructor_ratings WHERE booking_id IN (SELECT id FROM bookings WHERE slot_id = $1)`, slotID)
	_, _ = pool.Exec(ctx, `DELETE FROM payments WHERE booking_id IN (SELECT id FROM bookings WHERE slot_id = $1)`, slotID)
	_, _ = pool.Exec(ctx, `DELETE FROM booking_rental_lines WHERE booking_id IN (SELECT id FROM bookings WHERE slot_id = $1)`, slotID)
	_, _ = pool.Exec(ctx, `DELETE FROM bookings WHERE slot_id = $1`, slotID)
	_, _ = pool.Exec(ctx, `DELETE FROM slot_rental_availability WHERE slot_id = $1`, slotID)
	_, _ = pool.Exec(ctx, `DELETE FROM training_slots WHERE id = $1`, slotID)
}

func insertTestInstructor(t *testing.T, pool *pgxpool.Pool) string {
	t.Helper()
	instructorID := fmt.Sprintf("c0000000-0000-4000-8000-%012d", time.Now().UnixNano()%1_000_000_000_000)
	_, err := pool.Exec(context.Background(), `
		INSERT INTO instructors (id, full_name) VALUES ($1, $2)`,
		instructorID, "Test Instructor",
	)
	if err != nil {
		t.Fatalf("insert test instructor: %v", err)
	}
	return instructorID
}

func insertIsolatedTimedSlot(t *testing.T, pool *pgxpool.Pool, startsIn time.Duration, freeSpots int32) string {
	t.Helper()
	return insertTimedSlotForInstructor(t, pool, insertTestInstructor(t, pool), startsIn, freeSpots)
}

func insertTimedSlotForInstructor(t *testing.T, pool *pgxpool.Pool, instructorID string, startsIn time.Duration, freeSpots int32) string {
	t.Helper()
	slotID := fmt.Sprintf("ed000000-0000-4000-8000-%012d", time.Now().UnixNano()%1_000_000_000_000)
	startsAt := time.Now().Add(startsIn)
	_, err := pool.Exec(context.Background(), `
		INSERT INTO training_slots (
			id, starts_at, duration_minutes, capacity, free_spots,
			training_price, rental_tariff, slot_status, address,
			zone_id, instructor_id, venue_id
		) VALUES (
			$1, $2, 90, $3, $3, 1200.00, 400.00, 'active', $4,
			'b0000000-0000-4000-8000-000000000001',
			$5,
			'a0000000-0000-4000-8000-000000000001'
		)`,
		slotID, startsAt, freeSpots, "г. Москва, ул. Скалолазная, д. 1", instructorID,
	)
	if err != nil {
		t.Fatalf("insert test slot: %v", err)
	}
	t.Cleanup(func() { cleanupSlot(pool, slotID) })
	return slotID
}

func getSlotFreeSpots(t *testing.T, pool *pgxpool.Pool, slotID string) int32 {
	t.Helper()
	var freeSpots int32
	err := pool.QueryRow(context.Background(), "SELECT free_spots FROM training_slots WHERE id = $1", slotID).Scan(&freeSpots)
	if err != nil {
		t.Fatalf("get slot free spots: %v", err)
	}
	return freeSpots
}

func TestGymCancelBookingIntegration(t *testing.T) {
	server, pool := newIntegrationServer(t)

	client, token := registerBookingClient(t, server, fmt.Sprintf("+7913%07d", time.Now().UnixNano()%10_000_000))
	t.Cleanup(func() { cleanupBookingClient(t, pool, client.Phone) })

	consentResp := doJSONRequest(t, server.Client(), http.MethodPatch, server.URL+"/v1/clients/me", token, map[string]bool{
		"risk_consent_accepted": true,
	})
	if consentResp.StatusCode != http.StatusOK {
		t.Fatalf("patch consent status: %d", consentResp.StatusCode)
	}

	slotID := insertTimedSlotOnly(t, pool, 4*time.Hour, 3)
	bookingID := createBookingOnSlot(t, server, token, slotID)

	_, err := pool.Exec(context.Background(), `
		UPDATE payments SET payment_status = 'paid' WHERE booking_id = $1`, bookingID)
	if err != nil {
		t.Fatalf("mark payment paid: %v", err)
	}

	cancelResp := doInternalJSONRequest(t, server.Client(), http.MethodPost,
		server.URL+"/internal/slots/"+slotID+"/cancel-by-gym", testInternalAPIKey, map[string]string{
			"cancellation_reason_id": testCancellationReasonID,
		})
	if cancelResp.StatusCode != http.StatusOK {
		t.Fatalf("gym cancel status: %d body=%s", cancelResp.StatusCode, readBody(t, cancelResp))
	}

	getResp := doJSONRequest(t, server.Client(), http.MethodGet, server.URL+"/v1/bookings/"+bookingID, token, nil)
	if getResp.StatusCode != http.StatusOK {
		t.Fatalf("get booking status: %d body=%s", getResp.StatusCode, readBody(t, getResp))
	}

	var booking api.BookingDetail
	decodeJSONResponse(t, getResp, &booking)
	if booking.BookingStatus != api.BookingStatusCancelledByGym {
		t.Fatalf("expected cancelled_by_gym, got %s", booking.BookingStatus)
	}
	if !booking.RebookingForbidden {
		t.Fatalf("expected rebooking_forbidden=true")
	}
	if booking.CancellationReason == nil {
		t.Fatalf("expected cancellation_reason")
	}
	if booking.Payment.PaymentStatus != api.PaymentStatusRefund {
		t.Fatalf("expected refund payment status, got %s", booking.Payment.PaymentStatus)
	}
}

func TestRebookingForbiddenAfterGymCancelIntegration(t *testing.T) {
	server, pool := newIntegrationServer(t)

	client, token := registerBookingClient(t, server, fmt.Sprintf("+7914%07d", time.Now().UnixNano()%10_000_000))
	t.Cleanup(func() { cleanupBookingClient(t, pool, client.Phone) })

	consentResp := doJSONRequest(t, server.Client(), http.MethodPatch, server.URL+"/v1/clients/me", token, map[string]bool{
		"risk_consent_accepted": true,
	})
	if consentResp.StatusCode != http.StatusOK {
		t.Fatalf("patch consent status: %d", consentResp.StatusCode)
	}

	slotID := insertTimedSlotOnly(t, pool, 4*time.Hour, 3)
	createBookingOnSlot(t, server, token, slotID)

	cancelResp := doInternalJSONRequest(t, server.Client(), http.MethodPost,
		server.URL+"/internal/slots/"+slotID+"/cancel-by-gym", testInternalAPIKey, map[string]string{
			"cancellation_reason_id": testCancellationReasonID,
		})
	if cancelResp.StatusCode != http.StatusOK {
		t.Fatalf("gym cancel status: %d", cancelResp.StatusCode)
	}

	rebookResp := doJSONRequest(t, server.Client(), http.MethodPost, server.URL+"/v1/bookings", token, map[string]any{
		"slot_id":            slotID,
		"uses_own_equipment": true,
	})
	if rebookResp.StatusCode != http.StatusUnprocessableEntity {
		t.Fatalf("expected 422, got %d body=%s", rebookResp.StatusCode, readBody(t, rebookResp))
	}
}

func TestFindAlternativeSlotFoundIntegration(t *testing.T) {
	server, pool := newIntegrationServer(t)

	client, token := registerBookingClient(t, server, fmt.Sprintf("+7915%07d", time.Now().UnixNano()%10_000_000))
	t.Cleanup(func() { cleanupBookingClient(t, pool, client.Phone) })

	consentResp := doJSONRequest(t, server.Client(), http.MethodPatch, server.URL+"/v1/clients/me", token, map[string]bool{
		"risk_consent_accepted": true,
	})
	if consentResp.StatusCode != http.StatusOK {
		t.Fatalf("patch consent status: %d", consentResp.StatusCode)
	}

	instructorID := insertTestInstructor(t, pool)
	cancelledSlotID := insertTimedSlotForInstructor(t, pool, instructorID, 4*time.Hour, 3)
	alternativeSlotID := insertTimedSlotForInstructor(t, pool, instructorID, 5*time.Hour, 2)
	bookingID := createBookingOnSlot(t, server, token, cancelledSlotID)

	cancelResp := doInternalJSONRequest(t, server.Client(), http.MethodPost,
		server.URL+"/internal/slots/"+cancelledSlotID+"/cancel-by-gym", testInternalAPIKey, map[string]string{
			"cancellation_reason_id": testCancellationReasonID,
		})
	if cancelResp.StatusCode != http.StatusOK {
		t.Fatalf("gym cancel status: %d", cancelResp.StatusCode)
	}

	altResp := doJSONRequest(t, server.Client(), http.MethodGet,
		server.URL+"/v1/slots/alternatives?cancelled_slot_id="+cancelledSlotID+"&booking_id="+bookingID, token, nil)
	if altResp.StatusCode != http.StatusOK {
		t.Fatalf("find alternative status: %d body=%s", altResp.StatusCode, readBody(t, altResp))
	}

	var payload api.AlternativeSlotResponse
	decodeJSONResponse(t, altResp, &payload)
	if !payload.Found || payload.AlternativeSlot == nil {
		t.Fatalf("expected alternative slot found")
	}
	if payload.AlternativeSlot.Id != alternativeSlotID {
		t.Fatalf("expected alternative %s, got %s", alternativeSlotID, payload.AlternativeSlot.Id)
	}
}

func TestFindAlternativeSlotNotFoundIntegration(t *testing.T) {
	server, pool := newIntegrationServer(t)

	client, token := registerBookingClient(t, server, fmt.Sprintf("+7916%07d", time.Now().UnixNano()%10_000_000))
	t.Cleanup(func() { cleanupBookingClient(t, pool, client.Phone) })

	consentResp := doJSONRequest(t, server.Client(), http.MethodPatch, server.URL+"/v1/clients/me", token, map[string]bool{
		"risk_consent_accepted": true,
	})
	if consentResp.StatusCode != http.StatusOK {
		t.Fatalf("patch consent status: %d", consentResp.StatusCode)
	}

	cancelledSlotID := insertIsolatedTimedSlot(t, pool, 4*time.Hour, 3)
	bookingID := createBookingOnSlot(t, server, token, cancelledSlotID)

	cancelResp := doInternalJSONRequest(t, server.Client(), http.MethodPost,
		server.URL+"/internal/slots/"+cancelledSlotID+"/cancel-by-gym", testInternalAPIKey, map[string]string{
			"cancellation_reason_id": testCancellationReasonID,
		})
	if cancelResp.StatusCode != http.StatusOK {
		t.Fatalf("gym cancel status: %d", cancelResp.StatusCode)
	}

	altResp := doJSONRequest(t, server.Client(), http.MethodGet,
		server.URL+"/v1/slots/alternatives?cancelled_slot_id="+cancelledSlotID+"&booking_id="+bookingID, token, nil)
	if altResp.StatusCode != http.StatusOK {
		t.Fatalf("find alternative status: %d body=%s", altResp.StatusCode, readBody(t, altResp))
	}

	var payload api.AlternativeSlotResponse
	decodeJSONResponse(t, altResp, &payload)
	if payload.Found {
		t.Fatalf("expected no alternative slot")
	}
}

func doInternalJSONRequest(t *testing.T, client *http.Client, method, url, apiKey string, body any) *http.Response {
	t.Helper()

	var reader *bytes.Reader
	if body == nil {
		reader = bytes.NewReader(nil)
	} else {
		payload, err := json.Marshal(body)
		if err != nil {
			t.Fatalf("marshal body: %v", err)
		}
		reader = bytes.NewReader(payload)
	}

	req, err := http.NewRequest(method, url, reader)
	if err != nil {
		t.Fatalf("new request: %v", err)
	}
	if body != nil {
		req.Header.Set("Content-Type", "application/json")
	}
	req.Header.Set("X-API-Key", apiKey)

	resp, err := client.Do(req)
	if err != nil {
		t.Fatalf("do request: %v", err)
	}
	return resp
}

func cleanupBookingClient(t *testing.T, pool *pgxpool.Pool, phone string) {
	t.Helper()
	_, err := pool.Exec(context.Background(), "DELETE FROM clients WHERE phone = $1", phone)
	if err != nil {
		t.Logf("cleanup client: %v", err)
	}
}
