# BasketGuard

Inventory-aware quick-commerce Android app and API. BasketGuard’s distinctive feature is **Basket Rescue**: when stock changes at checkout, the shopper gets same-use alternatives that are in stock for the full quantity and fit a saved price cap (strict, +15%, or +30%). The shopper decides whether to swap, keep the available quantity, or remove the item. The Android client is Kotlin + Jetpack Compose with a Room-backed offline cart. The FastAPI backend uses PostgreSQL in Docker and SQLite when run locally without configuration.

## Start the API

```powershell
cd backend
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
uvicorn app.main:app --reload
```

Open `http://127.0.0.1:8000/docs` for the generated OpenAPI UI. The API seeds a demo store and catalog on first start. Local development uses `backend/basketguard.db`.

Run the backend business-logic tests with `pip install -r requirements-dev.txt` followed by `python -m pytest` from `backend/`.

To run PostgreSQL and the API together:

```powershell
docker compose up --build
```

Copy `.env.example` to `.env` before a non-local deployment and replace `JWT_SECRET`. Docker Compose uses its own development secret.

## Open the Android app

Open the `android` directory in Android Studio, allow Gradle sync, and launch the `app` configuration on an emulator (min SDK 26). The default API URL is `http://10.0.2.2:8000/api/v1/`, which reaches the host from the Android emulator. Change `API_BASE_URL` in `android/gradle.properties` for a physical device.

With JDK 17 installed, build the debug APK from PowerShell with `cd android; .\gradlew.bat :app:assembleDebug`.

The app starts in guest mode so the catalog and Room cart work without a server or account. Sign-in and registration are available from Profile; checkout and server order history require an account and a running API. Cart edits are saved locally immediately.

## Try the differentiator

Open **Berries** and tap **Find swap** on the sold-out wild blueberries. Strict shows the cheaper strawberries; Balanced (+15%) also includes raspberries. Both are filtered by current store stock and the shopper’s chosen unit-price cap. For cart items that sell out after adding, checkout validates live stock and opens the same choice flow with options that cover the full requested quantity; the shopper can also keep the available count or remove the item.

## What is implemented

- Native Compose storefront, search, category filters, product availability, cart, profile, and checkout UI with Hilt dependency injection.
- Basket Rescue recommendations based on shared substitution groups, live inventory, requested quantity, and the shopper’s price cap; the choice is explicit and updates the local cart.
- Room persistence for guest and signed-in cart contents.
- FastAPI registration/login with JWT, product/category catalog, basket-rescue recommendations, cart validation/sync, inventory reservations with expiration/release, idempotent checkout, order history/details, and substitution decisions.
- SQLAlchemy schema for users, stores, categories, products, inventory, carts, cart items, reservations, orders, order items, substitutions, and addresses.
- Conditional inventory updates and database transactions prevent concurrent checkout from overselling. PostgreSQL is the authoritative production database; SQLite is intended for local development.

## Current MVP boundaries

Payments are mocked as `mock_card`; order tracking uses a small status model and there is no push-notification/FCM setup yet. Database tables are initialized at startup for this portfolio MVP; a versioned Alembic migration history should be added before production rollout.

## Repository map

```text
android/                 Kotlin Android application
backend/app/              FastAPI API, SQLAlchemy models and services
docker-compose.yml         PostgreSQL + API for local development
docs/                      Product, architecture, schema and API notes
```
