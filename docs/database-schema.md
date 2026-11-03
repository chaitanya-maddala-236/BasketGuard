# Database schema

| Table | Important fields and constraints |
|---|---|
| `users` | UUID id, unique email, password hash |
| `stores` | id, name, location |
| `categories` | id, unique name, icon |
| `products` | id, category FK, price, unit, image URL, active, substitution group |
| `inventory` | store FK, product FK, nonnegative stock and reservations, unique store/product |
| `carts` | user FK, one cart per user |
| `cart_items` | cart FK, product FK, positive quantity, unique cart/product |
| `reservations` | group id, user/store/product FKs, quantity, expiry, status |
| `orders` | user/store FKs, status, address, total, unique user/idempotency key |
| `order_items` | order/product FKs, positive quantity, final unit price |
| `substitutions` | order item/replacement product FKs, approval status, unit-price difference |
| `addresses` | user FK, address and postal code |

`Inventory.stock_on_hand` counts physical sellable units. `reserved_qty` is a subset of on-hand stock, not an additional quantity. Available inventory is derived, never stored independently. PostgreSQL constraints and transactions are the production concurrency boundary.

Basket Rescue groups products by `substitution_group`; recommendations must be active, available for the requested quantity at the selected store, and at or below the shopper’s maximum unit price.

For the MVP, SQLAlchemy creates tables on API startup. Add Alembic migration revisions before deploying schema changes to persistent environments.
