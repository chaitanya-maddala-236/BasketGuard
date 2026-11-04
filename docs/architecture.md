# Architecture

```text
Android app
Compose UI → presentation ViewModel (StateFlow) → shared KMP domain / use cases
                                                  ↑
data adapters: Room · DataStore · Retrofit · WorkManager · optional Firebase Analytics

FastAPI modular monolith
Routes → SQLAlchemy → PostgreSQL inventory / SQLite development fallback
   └── AI basket planner → OpenAI Responses API (strict JSON schema)
```

The Android app keeps Compose rendering and StateFlow presentation state separate from data adapters. The `shared` KMP module contains platform-independent grocery models, Basket Rescue ranking, and the AI-plan use case; Android consumes the module and declares Android and iOS targets. Repository adapters map API and Room models into shared domain models. Hilt wires the app-scoped data adapters and use case.

The AI planner sends the shopper's meal request, budget, and currently available catalog to the backend model provider. The model can suggest product IDs and quantities; the server resolves every ID against the database, checks live stock and recomputes all line totals before returning a plan. The API key remains on the backend. Firebase Analytics records non-identifying rescue and plan events when a Firebase project configuration is supplied; otherwise the adapter is a no-op.

The API keeps inventory operations transactional. Reservation and checkout use conditional SQL updates so concurrent requests compete on the same inventory row. Redis is intentionally not used as inventory truth. A unique `(user_id, idempotency_key)` constraint protects checkout retries.

## Runtime

- Local API: `uvicorn app.main:app --reload` from `backend/`.
- Emulator API base: `http://10.0.2.2:8000/api/v1/`.
- Compose stack: PostgreSQL 17 + FastAPI via `docker compose up --build`.
- API reference: `/docs` (Swagger UI) and `/openapi.json`.
