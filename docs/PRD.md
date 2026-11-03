# Product brief

## Goal

BasketGuard is a native grocery shopping app that makes stock availability visible before customers pay and lets them choose any replacement themselves.

## Users

- Shoppers discovering groceries, building a basket, and tracking orders.
- Store inventory systems publishing current sellable quantities.
- Order services reserving and consuming stock safely.

## MVP flows

1. Browse the seeded catalog, search, and filter by category.
2. Save basket changes locally so the basket survives an app restart or network loss.
3. Validate the basket against current store stock before checkout.
4. Sign in, submit a mock-payment order, and receive an idempotent order response.
5. View order history and ask for or approve/reject an item replacement.

## Product rules

- Available stock equals `stock_on_hand - reserved_qty`.
- A checkout succeeds only when every requested quantity can be atomically consumed.
- A reservation expires after ten minutes unless it is consumed or released.
- A repeated checkout with the same user and idempotency key returns the original order.
- A customer must approve a replacement; an unapproved replacement is never substituted silently.
- Offline basket changes stay in Room and are synchronized when a network is available.

## Follow-up scope

Real payments, FCM delivery, store admin tools, location selection, production-grade database migrations, and richer order-state updates are outside this MVP.
