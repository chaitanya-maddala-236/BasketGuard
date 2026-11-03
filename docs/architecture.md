# Architecture

```text
Android app
Compose screens → AppViewModel → BasketRepository
                                  ├── Room (offline basket)
                                  ├── DataStore (JWT session)
                                  ├── Retrofit (BasketGuard API)
                                  └── WorkManager (network-constrained cart sync)

FastAPI modular monolith
API routes → SQLAlchemy session → PostgreSQL (authoritative inventory)
                                └→ SQLite (local development fallback)
```

The Android MVP uses one Gradle application module with package boundaries for UI, domain, local storage, network access, and repository coordination. Hilt provides the Room database, Retrofit client, and repository as application-scoped dependencies, and injects the ViewModel into the activity. The repository is the only data boundary consumed by the ViewModel.

The API keeps inventory operations transactional. Reservation and checkout use conditional SQL updates so concurrent requests compete on the same inventory row. Redis is intentionally not used as inventory truth. A unique `(user_id, idempotency_key)` constraint protects checkout retries.

## Runtime

- Local API: `uvicorn app.main:app --reload` from `backend/`.
- Emulator API base: `http://10.0.2.2:8000/api/v1/`.
- Compose stack: PostgreSQL 17 + FastAPI via `docker compose up --build`.
- API reference: `/docs` (Swagger UI) and `/openapi.json`.
