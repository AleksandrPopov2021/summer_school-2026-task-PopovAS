package httpx_test

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"net/http/httptest"
	"os"
	"testing"
	"time"

	jwtauth "github.com/vertical-climbing/backend/internal/adapters/auth"
	appcfg "github.com/vertical-climbing/backend/internal/adapters/config"
	pushadapter "github.com/vertical-climbing/backend/internal/adapters/push"
	httpx "github.com/vertical-climbing/backend/internal/adapters/http"
	"github.com/vertical-climbing/backend/internal/adapters/http/api"
	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/vertical-climbing/backend/internal/adapters/postgres"
	appclient "github.com/vertical-climbing/backend/internal/application/client"
	appbooking "github.com/vertical-climbing/backend/internal/application/booking"
	appclearance "github.com/vertical-climbing/backend/internal/application/clearance"
	appdevice "github.com/vertical-climbing/backend/internal/application/device"
	appnotification "github.com/vertical-climbing/backend/internal/application/notification"
	apppush "github.com/vertical-climbing/backend/internal/application/push"
	appref "github.com/vertical-climbing/backend/internal/application/reference"
	apprating "github.com/vertical-climbing/backend/internal/application/rating"
	appslot "github.com/vertical-climbing/backend/internal/application/slot"
)

func TestClientFlowIntegration(t *testing.T) {
	databaseURL := os.Getenv("DATABASE_URL")
	if databaseURL == "" {
		t.Skip("DATABASE_URL is not set")
	}

	ctx := context.Background()
	pool, err := postgres.NewPool(ctx, databaseURL)
	if err != nil {
		t.Fatalf("connect database: %v", err)
	}
	defer pool.Close()

	phone := fmt.Sprintf("+7900%07d", time.Now().UnixNano()%10_000_000)
	cleanupClient(t, pool, phone)

	tokenService, err := jwtauth.NewService("integration-test-secret")
	if err != nil {
		t.Fatalf("init jwt: %v", err)
	}

	referenceRepo := postgres.NewReferenceRepository(pool)
	clientRepo := postgres.NewClientRepository(pool)
	notificationRepo := postgres.NewNotificationRepository(pool)
	deviceRepo := postgres.NewDeviceRepository(pool)
	workerRepo := postgres.NewWorkerRepository(pool)
	slotRepo := postgres.NewSlotRepository(pool)
	clearanceRepo := postgres.NewClearanceRepository(pool)
	bookingRepo := postgres.NewBookingRepository(pool, clientRepo, clearanceRepo, referenceRepo, slotRepo)
	ratingRepo := postgres.NewRatingRepository(pool)
	pushService := apppush.NewService(pushadapter.NewNoopSender(), deviceRepo, notificationRepo, workerRepo)
	handler := httpx.NewHandler(
		appref.NewService(referenceRepo),
		appclient.NewService(clientRepo, tokenService),
		appnotification.NewService(notificationRepo),
		appdevice.NewService(deviceRepo),
		appslot.NewService(slotRepo, referenceRepo, clearanceRepo),
		appclearance.NewService(clearanceRepo),
		appbooking.NewService(bookingRepo, referenceRepo, pushService),
		apprating.NewService(ratingRepo),
	)

	server := httptest.NewServer(httpx.NewRouter(appcfg.Config{
		CORSAllowedOrigins: []string{"http://localhost"},
		InternalAPIKey:     "dev-internal-key",
	}, httpx.Dependencies{
		Handler: handler,
		Tokens:  tokenService,
	}))
	defer server.Close()

	registerBody := map[string]string{
		"phone":      phone,
		"full_name":  "Иванов Иван Иванович",
		"birth_date": "1995-03-15",
	}
	registerResp := doJSONRequest(t, server.Client(), http.MethodPost, server.URL+"/v1/clients", "", registerBody)
	if registerResp.StatusCode != http.StatusCreated {
		t.Fatalf("register status: %d body=%s", registerResp.StatusCode, readBody(t, registerResp))
	}

	var registration api.ClientRegistrationResponse
	decodeJSONResponse(t, registerResp, &registration)
	if registration.TokenType != api.TokenTypeBearer {
		t.Fatalf("unexpected token type: %s", registration.TokenType)
	}
	if registration.AccessToken == "" {
		t.Fatalf("expected access token")
	}
	if registration.Client.RiskConsentAccepted {
		t.Fatalf("expected risk consent false on registration")
	}

	meResp := doJSONRequest(t, server.Client(), http.MethodGet, server.URL+"/v1/clients/me", registration.AccessToken, nil)
	if meResp.StatusCode != http.StatusOK {
		t.Fatalf("me status: %d body=%s", meResp.StatusCode, readBody(t, meResp))
	}

	var me api.Client
	decodeJSONResponse(t, meResp, &me)
	if me.Phone != phone {
		t.Fatalf("expected phone %s, got %s", phone, me.Phone)
	}

	patchResp := doJSONRequest(t, server.Client(), http.MethodPatch, server.URL+"/v1/clients/me", registration.AccessToken, map[string]bool{
		"risk_consent_accepted": true,
	})
	if patchResp.StatusCode != http.StatusOK {
		t.Fatalf("patch status: %d body=%s", patchResp.StatusCode, readBody(t, patchResp))
	}

	var patched api.Client
	decodeJSONResponse(t, patchResp, &patched)
	if !patched.RiskConsentAccepted {
		t.Fatalf("expected risk consent accepted")
	}

	prefsResp := doJSONRequest(t, server.Client(), http.MethodGet, server.URL+"/v1/clients/me/notification-preferences", registration.AccessToken, nil)
	if prefsResp.StatusCode != http.StatusOK {
		t.Fatalf("prefs status: %d body=%s", prefsResp.StatusCode, readBody(t, prefsResp))
	}

	var prefs api.NotificationPreferences
	decodeJSONResponse(t, prefsResp, &prefs)
	if !prefs.RemindersEnabled || !prefs.GymCancellationEnabled {
		t.Fatalf("expected mandatory notification flags to be true")
	}

	t.Cleanup(func() {
		cleanupClient(t, pool, phone)
	})
}

func cleanupClient(t *testing.T, pool *pgxpool.Pool, phone string) {
	t.Helper()
	_, err := pool.Exec(context.Background(), "DELETE FROM clients WHERE phone = $1", phone)
	if err != nil {
		t.Logf("cleanup client: %v", err)
	}
}

func doJSONRequest(t *testing.T, client *http.Client, method, url, token string, body any) *http.Response {
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
	if token != "" {
		req.Header.Set("Authorization", "Bearer "+token)
	}

	resp, err := client.Do(req)
	if err != nil {
		t.Fatalf("do request: %v", err)
	}
	return resp
}

func decodeJSONResponse(t *testing.T, resp *http.Response, dst any) {
	t.Helper()
	defer resp.Body.Close()
	if err := json.NewDecoder(resp.Body).Decode(dst); err != nil {
		t.Fatalf("decode response: %v", err)
	}
}

func readBody(t *testing.T, resp *http.Response) string {
	t.Helper()
	defer resp.Body.Close()
	var buf bytes.Buffer
	_, _ = buf.ReadFrom(resp.Body)
	return buf.String()
}
