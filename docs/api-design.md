# API design

All routes are prefixed by `/api/v1`. FastAPI publishes the request and response schemas at `/docs`.

| Method | Route | Purpose |
|---|---|---|
| POST | `/auth/register` | Create account and return JWT |
| POST | `/auth/login` | Authenticate and return JWT |
| GET | `/categories` | List catalog categories |
| GET | `/products?q=&category_id=&store_id=` | Search and filter available catalog |
| GET | `/products/{id}` | Product details |
| GET | `/products/{id}/alternatives?quantity=&max_price=` | Recommend same-use products in stock for the full quantity and below the shopper’s unit-price cap |
| POST | `/ai/basket-plan` | Generate a meal basket from live store inventory under a shopper budget; requires JWT and backend AI configuration |
| GET | `/stores/{id}/inventory` | Current on-hand, reserved, and available stock |
| POST | `/cart/validate` | Validate cart quantities against current stock |
| GET / PUT | `/cart` | Read or synchronize signed-in cart |
| POST | `/reservations` | Atomically reserve items for ten minutes |
| DELETE | `/reservations/{id}` | Release a reservation group |
| POST | `/orders` | Create an idempotent mock-payment order |
| GET | `/orders` | List the signed-in user's orders |
| GET | `/orders/{id}` | Fetch an owned order |
| POST | `/substitutions` | Offer a replacement for an order item |
| GET | `/orders/{id}/substitutions` | List replacement choices for an order |
| POST | `/substitutions/{id}/decision` | Approve or reject a replacement |

Protected routes require `Authorization: Bearer <token>`. Checkout requires `Idempotency-Key` (8–128 characters). Inventory conflicts return HTTP 409 so the client can ask the shopper to update the basket.
