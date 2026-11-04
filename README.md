# BasketGuard

**A quick-commerce Android app that helps shoppers handle stockouts on their terms.**

BasketGuard addresses a familiar grocery-delivery problem: an item can sell out after someone builds a basket, leaving them with a canceled line or an unwanted replacement. Its **Basket Rescue** flow finds same-use products that meet the requested quantity and a shopper-selected price cap. The shopper chooses to swap, keep the units still available, or remove the item.

> Independent portfolio project inspired by quick-commerce services. Not affiliated with Blinkit.

**Kotlin · Jetpack Compose · FastAPI · SQLAlchemy · PostgreSQL · Room · Retrofit · WorkManager · Docker**

## Resume highlights

- Built a native Android grocery-shopping app with a Compose storefront, search, category browsing, product availability, basket, profile, checkout, and order history.
- Developed **Basket Rescue**, a stockout recovery flow that recommends in-stock, same-use alternatives for the full requested quantity and enforces a saved **0%, 15%, or 30%** unit-price increase limit.
- Implemented a **19-route REST API** with JWT authentication, cart validation and sync, inventory reservations, idempotent checkout, order history, and customer-approved substitutions.
- Used conditional inventory updates and database transactions to prevent overselling; persisted guest baskets in Room and scheduled background cart sync.
- Verified core API behavior with **8 integration tests** covering stockout recommendations, oversell prevention, transaction rollback, reservations, idempotency, and substitution approval.

### Project scope in numbers

| Measure | Implemented |
|---|---:|
| Seed catalog | 14 products across 7 categories |
| API | 19 REST routes |
| Database | 12 SQLAlchemy models |
| Shopper swap budgets | 3 options: 0%, +15%, +30% |
| Recommendations | Up to 5 per item, filtered by live stock and quantity |
| Backend integration tests | 8 |

These are implementation counts, not claims of production usage or measured business impact.

## Basket Rescue flow

1. Validate the basket against current store inventory before checkout.
2. If availability changed, find alternatives from the product’s substitution group.
3. Filter options by the selected store, full requested quantity, and maximum unit price.
4. Let the shopper choose an alternative, keep the available quantity, or remove the item.
5. Revalidate and place the order only after every basket issue is resolved.

The demo catalog includes sold-out wild blueberries. Open **Berries** and tap **Find swap**: the strict budget shows cheaper strawberries, while the balanced budget also admits raspberries. When a previously added item sells out, the checkout rescue panel presents the same choices against current inventory.

## Tech stack

- **Android:** Kotlin, Jetpack Compose, MVVM, Hilt, Room, DataStore, Retrofit, WorkManager
- **Backend:** Python, FastAPI, Pydantic, SQLAlchemy, JWT authentication
- **Data and local development:** PostgreSQL, SQLite, Docker Compose
- **Verification:** pytest and FastAPI `TestClient`

## Run locally

### Start the API and PostgreSQL

From the repository root:

```powershell
docker compose up --build
```

The API seeds the demo store and catalog on startup. Open [http://127.0.0.1:8000/docs](http://127.0.0.1:8000/docs) to explore the API.

To run the API with local SQLite instead:

```powershell
cd backend
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
uvicorn app.main:app --reload
```

### Launch Android

Open the `android` directory in Android Studio, let Gradle sync, and run the `app` configuration on an emulator. The default API URL, `http://10.0.2.2:8000/api/v1/`, connects the emulator to the host machine. The app supports Android 8.0 (API 26) and newer.

Build a debug APK with JDK 17:

```powershell
cd android
.\gradlew.bat :app:assembleDebug
```

### Run backend tests

```powershell
cd backend
pip install -r requirements-dev.txt
python -m pytest -q
```

## Repository layout

```text
android/                  Kotlin Android app
backend/app/              FastAPI API and SQLAlchemy models
backend/tests/             Inventory, checkout, and substitution tests
docs/                      Product brief, architecture, schema, and API design
docker-compose.yml         PostgreSQL and API for local development
```

## Current scope

Payments are mocked and order tracking uses a basic status model. The API initializes tables at startup for this portfolio project; a production rollout would need versioned Alembic migrations, real payment integration, push notifications, and operational store inventory tooling.
