from __future__ import annotations

from decimal import Decimal

import pytest
from fastapi.testclient import TestClient
from sqlalchemy import create_engine, event
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool

from app import main
from app.db import Base, get_db
from app.models import Category, Inventory, Product, Store


@pytest.fixture
def client(monkeypatch):
    test_engine = create_engine(
        "sqlite://",
        connect_args={"check_same_thread": False},
        poolclass=StaticPool,
    )

    @event.listens_for(test_engine, "connect")
    def enable_foreign_keys(connection, _record):
        cursor = connection.cursor()
        cursor.execute("PRAGMA foreign_keys=ON")
        cursor.close()

    TestingSession = sessionmaker(bind=test_engine, autoflush=False, expire_on_commit=False)
    Base.metadata.create_all(bind=test_engine)
    with TestingSession() as db:
        db.add_all([
            Store(id=1, name="Test store", location="Nearby"),
            Category(id=1, name="Produce", icon="🥑"),
        ])
        db.flush()
        first = Product(id=1, name="Avocado", category_id=1, description="Ripe",
                        price=Decimal("1.50"), unit="piece", emoji="🥑",
                        substitution_group="avocado")
        replacement = Product(id=2, name="Spinach", category_id=1, description="Fresh",
                              price=Decimal("2.00"), unit="bag", emoji="🥬",
                              substitution_group="leafy-greens")
        close_match = Product(id=3, name="Green avocado", category_id=1, description="Ripe",
                              price=Decimal("1.60"), unit="piece", emoji="🥑",
                              substitution_group="avocado")
        over_budget = Product(id=4, name="Organic avocado", category_id=1, description="Organic",
                              price=Decimal("2.25"), unit="piece", emoji="🥑",
                              substitution_group="avocado")
        too_few = Product(id=5, name="Mini avocado", category_id=1, description="Ripe",
                          price=Decimal("1.25"), unit="piece", emoji="🥑",
                          substitution_group="avocado")
        db.add_all([first, replacement, close_match, over_budget, too_few])
        db.flush()
        db.add_all([
            Inventory(store_id=1, product_id=1, stock_on_hand=5, reserved_qty=0),
            Inventory(store_id=1, product_id=2, stock_on_hand=10, reserved_qty=0),
            Inventory(store_id=1, product_id=3, stock_on_hand=4, reserved_qty=0),
            Inventory(store_id=1, product_id=4, stock_on_hand=4, reserved_qty=0),
            Inventory(store_id=1, product_id=5, stock_on_hand=1, reserved_qty=0),
        ])
        db.commit()

    def override_get_db():
        db = TestingSession()
        try:
            yield db
        finally:
            db.close()

    monkeypatch.setattr(main, "init_db", lambda: None)
    main.app.dependency_overrides[get_db] = override_get_db
    with TestClient(main.app) as test_client:
        yield test_client
    main.app.dependency_overrides.clear()
    Base.metadata.drop_all(bind=test_engine)
    test_engine.dispose()


def signed_in(client: TestClient) -> dict[str, str]:
    response = client.post("/api/v1/auth/register", json={
        "name": "Basket Tester",
        "email": "basket@example.com",
        "password": "secure-pass-123",
    })
    assert response.status_code == 201
    return {"Authorization": f"Bearer {response.json()['access_token']}"}


def order_payload(product_id: int, quantity: int) -> dict:
    return {
        "store_id": 1,
        "address": "12 Green Street, Central City",
        "payment_method": "mock_card",
        "items": [{"product_id": product_id, "quantity": quantity}],
    }


def available(client: TestClient, product_id: int = 1) -> int:
    rows = client.get("/api/v1/stores/1/inventory").json()
    return next(row["available_stock"] for row in rows if row["product_id"] == product_id)


def test_checkout_cannot_oversell_inventory(client: TestClient):
    headers = signed_in(client)
    first = client.post("/api/v1/orders", headers={**headers, "Idempotency-Key": "checkout-one"},
                        json=order_payload(1, 3))
    second = client.post("/api/v1/orders", headers={**headers, "Idempotency-Key": "checkout-two"},
                         json=order_payload(1, 3))

    assert first.status_code == 201
    assert second.status_code == 409
    assert available(client) == 2


def test_basket_rescue_respects_budget_and_requested_quantity(client: TestClient):
    response = client.get("/api/v1/products/1/alternatives", params={
        "store_id": 1,
        "quantity": 2,
        "max_price": 2.00,
    })

    assert response.status_code == 200
    body = response.json()
    assert body["product_id"] == 1
    assert body["requested_quantity"] == 2
    assert [item["id"] for item in body["alternatives"]] == [3]
    assert body["alternatives"][0]["available_stock"] == 4


def test_checkout_retry_returns_original_order_without_consuming_stock_twice(client: TestClient):
    headers = signed_in(client)
    request_headers = {**headers, "Idempotency-Key": "checkout-retry-001"}
    payload = order_payload(1, 2)

    first = client.post("/api/v1/orders", headers=request_headers, json=payload)
    replay = client.post("/api/v1/orders", headers=request_headers, json=payload)

    assert first.status_code == 201
    assert replay.status_code == 200
    assert replay.json()["id"] == first.json()["id"]
    assert available(client) == 3


def test_failed_multi_item_checkout_rolls_back_all_inventory_changes(client: TestClient):
    headers = signed_in(client)
    response = client.post(
        "/api/v1/orders",
        headers={**headers, "Idempotency-Key": "checkout-atomic-001"},
        json={
            "store_id": 1,
            "address": "12 Green Street, Central City",
            "payment_method": "mock_card",
            "items": [
                {"product_id": 1, "quantity": 2},
                {"product_id": 2, "quantity": 11},
            ],
        },
    )

    assert response.status_code == 409
    assert available(client, 1) == 5
    assert available(client, 2) == 10


def test_reservation_holds_stock_and_release_restores_it(client: TestClient):
    headers = signed_in(client)
    held = client.post("/api/v1/reservations", headers=headers,
                       json={"store_id": 1, "items": [{"product_id": 1, "quantity": 4}]})

    assert held.status_code == 201
    assert available(client) == 1
    refused = client.post("/api/v1/reservations", headers=headers,
                          json={"store_id": 1, "items": [{"product_id": 1, "quantity": 2}]})
    assert refused.status_code == 409

    released = client.delete(f"/api/v1/reservations/{held.json()['reservation_id']}", headers=headers)
    assert released.status_code == 204
    assert available(client) == 5


def test_checkout_consumes_reserved_units_and_releases_unused_hold(client: TestClient):
    headers = signed_in(client)
    held = client.post("/api/v1/reservations", headers=headers,
                       json={"store_id": 1, "items": [{"product_id": 1, "quantity": 4}]})
    assert held.status_code == 201

    checkout = client.post(
        "/api/v1/orders",
        headers={**headers, "Idempotency-Key": "checkout-reserved-001"},
        json={**order_payload(1, 3), "reservation_id": held.json()["reservation_id"]},
    )

    assert checkout.status_code == 201
    assert available(client) == 2
    row = next(row for row in client.get("/api/v1/stores/1/inventory").json() if row["product_id"] == 1)
    assert row["reserved_qty"] == 0


def test_substitution_changes_order_only_after_customer_approval(client: TestClient):
    headers = signed_in(client)
    created = client.post("/api/v1/orders", headers={**headers, "Idempotency-Key": "swap-order-001"},
                          json=order_payload(1, 2))
    item_id = created.json()["items"][0]["id"]
    offered = client.post("/api/v1/substitutions", headers=headers,
                          json={"order_item_id": item_id, "replacement_product_id": 2})
    assert offered.status_code == 201
    assert offered.json()["status"] == "pending"

    declined = client.post(f"/api/v1/substitutions/{offered.json()['id']}/decision",
                           headers=headers, json={"decision": "reject"})
    unchanged = client.get(f"/api/v1/orders/{created.json()['id']}", headers=headers).json()
    assert declined.status_code == 200
    assert unchanged["items"][0]["product_id"] == 1
    assert unchanged["total_amount"] == 3.0


def test_approved_substitution_updates_line_and_total_once(client: TestClient):
    headers = signed_in(client)
    created = client.post("/api/v1/orders", headers={**headers, "Idempotency-Key": "swap-order-002"},
                          json=order_payload(1, 2))
    item_id = created.json()["items"][0]["id"]
    offered = client.post("/api/v1/substitutions", headers=headers,
                          json={"order_item_id": item_id, "replacement_product_id": 2}).json()
    decision_url = f"/api/v1/substitutions/{offered['id']}/decision"

    approved = client.post(decision_url, headers=headers, json={"decision": "approve"})
    replay = client.post(decision_url, headers=headers, json={"decision": "approve"})
    changed = client.get(f"/api/v1/orders/{created.json()['id']}", headers=headers).json()

    assert approved.status_code == 200
    assert replay.status_code == 200
    assert changed["items"][0]["product_id"] == 2
    assert changed["total_amount"] == 4.0
    assert available(client, 2) == 8
