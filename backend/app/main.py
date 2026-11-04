from __future__ import annotations

from contextlib import asynccontextmanager
from datetime import datetime, timedelta, timezone
from decimal import Decimal
import logging
from uuid import uuid4

from fastapi import Depends, FastAPI, Header, HTTPException, Query, Response, status
from sqlalchemy import delete, func, inspect, select, text, update
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session, joinedload

from app.db import Base, SessionLocal, engine, get_db
from app.models import (Address, Cart, CartItem, Category, Inventory, Order,
                        OrderItem, Product, Reservation, Store, Substitution, User)
from app.schemas import (AlternativeOut, BasketRescueOut, CartLine, CartSyncRequest, CartSyncResponse, CartValidationItem,
                         CartValidationRequest, CartValidationResponse, CategoryOut,
                         BasketPlanItemOut, BasketPlanOut, BasketPlanRequest,
                         LoginRequest, OrderItemOut, OrderOut, OrderRequest, ProductOut,
                         RegisterRequest, ReservationOut, ReservationRequest,
                         SubstitutionCreate, SubstitutionDecision, SubstitutionOut,
                         TokenResponse)
from app.security import create_access_token, current_user, hash_password, verify_password
from app.services.ai_basket_planner import (
    AIPlannerResponseError,
    AIPlannerUnavailable,
    create_ai_basket_draft,
)


SEED_CATEGORIES = [
    ("Fresh produce", "🥑"), ("Dairy & eggs", "🥛"), ("Bakery", "🥖"),
    ("Pantry", "🥫"), ("Snacks", "🍿"), ("Beverages", "🧃"), ("Berries", "🫐"),
]
SEED_PRODUCTS = [
    ("Avocado", 1, "Creamy, ripe Hass avocado", "79.00", "1 piece", "🥑", 24, "avocado"),
    ("Farm eggs", 2, "Free-range, protein-rich eggs", "99.00", "6 pieces", "🥚", 18, "eggs"),
    ("Sourdough loaf", 3, "Slow-fermented, baked this morning", "149.00", "400 g", "🍞", 8, "bread"),
    ("Oat milk", 2, "Smooth, unsweetened oat drink", "129.00", "1 litre", "🥛", 5, "plant-milk"),
    ("Almond butter", 4, "Roasted almonds, no added sugar", "249.00", "200 g", "🥜", 11, "nut-butter"),
    ("Baby spinach", 1, "Tender leaves, washed and ready", "59.00", "100 g", "🥬", 16, "leafy-greens"),
    ("Sea salt chips", 5, "Crispy kettle-cooked potato chips", "49.00", "120 g", "🥔", 20, "chips"),
    ("Cold brew coffee", 6, "Slow-steeped, ready to pour", "119.00", "250 ml", "☕", 3, "cold-brew"),
    ("Greek yogurt", 2, "Thick, creamy, high protein", "89.00", "400 g", "🍶", 14, "yogurt"),
    ("Wild blueberries", 7, "Sweet-tart berries, frozen fresh", "169.00", "250 g", "🫐", 0, "berries"),
    ("Sweet strawberries", 7, "Ripe berries from a nearby farm", "139.00", "250 g", "🍓", 8, "berries"),
    ("Fresh raspberries", 7, "Delicate, sweet-tart berry punnet", "189.00", "125 g", "🍇", 5, "berries"),
    ("Barista soy milk", 2, "Creamy plant milk, locally made", "109.00", "1 litre", "🥛", 9, "plant-milk"),
    ("Multigrain loaf", 3, "Soft loaf with oats and seeds", "119.00", "400 g", "🥖", 7, "bread"),
]
logger = logging.getLogger(__name__)


def init_db() -> None:
    # Keep databases created by the previous MVP usable after adding rescue metadata.
    if "products" in inspect(engine).get_table_names():
        columns = {column["name"] for column in inspect(engine).get_columns("products")}
        if "substitution_group" not in columns:
            with engine.begin() as connection:
                connection.execute(text(
                    "ALTER TABLE products ADD COLUMN substitution_group VARCHAR(60) NOT NULL DEFAULT 'general'"
                ))
    Base.metadata.create_all(bind=engine)
    with SessionLocal() as db:
        store = db.get(Store, 1)
        if store is None:
            store = Store(id=1, name="BasketGuard Market", location="Central market · 2.1 km")
            db.add(store)
        for category_id, (name, icon) in enumerate(SEED_CATEGORIES, start=1):
            if db.get(Category, category_id) is None:
                db.add(Category(id=category_id, name=name, icon=icon))
        db.flush()
        for name, category_id, description, price, unit, emoji, stock, group in SEED_PRODUCTS:
            product = db.scalar(select(Product).where(Product.name == name))
            if product is None:
                product = Product(name=name, category_id=category_id, description=description,
                                  price=Decimal(price), unit=unit, emoji=emoji,
                                  substitution_group=group)
                db.add(product)
                db.flush()
            else:
                product.category_id = category_id
                product.substitution_group = group
            inventory = db.scalar(select(Inventory).where(
                Inventory.store_id == store.id, Inventory.product_id == product.id
            ))
            if inventory is None:
                db.add(Inventory(store_id=store.id, product_id=product.id,
                                 stock_on_hand=stock, reserved_qty=0))
        db.commit()


@asynccontextmanager
async def lifespan(_: FastAPI):
    init_db()
    yield


app = FastAPI(title="BasketGuard API", version="1.0.0", description=
              "Inventory-aware grocery ordering API with reservations and idempotent checkout.",
              lifespan=lifespan)


def available_for(db: Session, product_id: int, store_id: int) -> int:
    row = db.scalar(select(Inventory).where(Inventory.product_id == product_id,
                                            Inventory.store_id == store_id))
    return max(0, row.stock_on_hand - row.reserved_qty) if row else 0


def product_out(db: Session, product: Product, store_id: int = 1) -> ProductOut:
    return ProductOut(id=product.id, name=product.name, category_id=product.category_id,
                      description=product.description, price=product.price,
                      unit=product.unit, emoji=product.emoji, image_url=product.image_url,
                      available_stock=available_for(db, product.id, store_id),
                      substitution_group=product.substitution_group)


def order_out(order: Order) -> OrderOut:
    return OrderOut(id=order.id, status=order.status, total_amount=order.total_amount,
                    address=order.address, created_at=order.created_at,
                    items=[OrderItemOut(product_id=item.product_id,
                                        id=item.id,
                                        product_name=item.product.name,
                                        quantity=item.quantity,
                                        final_price=item.final_price)
                           for item in order.items])


def substitution_out(sub: Substitution) -> SubstitutionOut:
    return SubstitutionOut(id=sub.id, order_item_id=sub.order_item_id,
                           replacement_product_id=sub.replacement_product_id,
                           replacement_product_name=sub.replacement_product.name,
                           status=sub.status, price_difference=sub.price_difference)


def release_expired(db: Session) -> None:
    now = datetime.now(timezone.utc)
    expired = db.scalars(select(Reservation).where(Reservation.status == "active",
                                                   Reservation.expires_at <= now)).all()
    for reservation in expired:
        db.execute(update(Inventory).where(Inventory.store_id == reservation.store_id,
                                           Inventory.product_id == reservation.product_id,
                                           Inventory.reserved_qty >= reservation.quantity)
                   .values(reserved_qty=Inventory.reserved_qty - reservation.quantity))
        reservation.status = "expired"


def aggregate_items(items: list[CartLine]) -> dict[int, int]:
    totals: dict[int, int] = {}
    for item in items:
        totals[item.product_id] = totals.get(item.product_id, 0) + item.quantity
    if any(quantity > 99 for quantity in totals.values()):
        raise HTTPException(status_code=422, detail="A product quantity cannot exceed 99.")
    return totals


@app.get("/api/v1/health")
def health():
    return {"status": "ok", "service": "basketguard-api"}


@app.post("/api/v1/auth/register", response_model=TokenResponse, status_code=201)
def register(payload: RegisterRequest, db: Session = Depends(get_db)):
    email = str(payload.email).lower()
    if db.scalar(select(User.id).where(func.lower(User.email) == email)):
        raise HTTPException(status_code=409, detail="An account with this email already exists.")
    user = User(name=payload.name.strip(), email=email,
                password_hash=hash_password(payload.password))
    db.add(user)
    try:
        db.commit()
    except Exception:
        db.rollback()
        raise HTTPException(status_code=409, detail="An account with this email already exists.")
    return TokenResponse(access_token=create_access_token(user), user_id=user.id, name=user.name)


@app.post("/api/v1/auth/login", response_model=TokenResponse)
def login(payload: LoginRequest, db: Session = Depends(get_db)):
    user = db.scalar(select(User).where(func.lower(User.email) == str(payload.email).lower()))
    if user is None or not verify_password(payload.password, user.password_hash):
        raise HTTPException(status_code=401, detail="Email or password is incorrect.")
    return TokenResponse(access_token=create_access_token(user), user_id=user.id, name=user.name)


@app.get("/api/v1/categories", response_model=list[CategoryOut])
def list_categories(db: Session = Depends(get_db)):
    return db.scalars(select(Category).order_by(Category.id)).all()


@app.get("/api/v1/products", response_model=list[ProductOut])
def list_products(q: str | None = None, category_id: int | None = None,
                  store_id: int = 1, db: Session = Depends(get_db)):
    statement = select(Product).where(Product.active.is_(True)).options(joinedload(Product.category))
    if q and q.strip():
        statement = statement.where(Product.name.ilike(f"%{q.strip()}%"))
    if category_id:
        statement = statement.where(Product.category_id == category_id)
    products = db.scalars(statement.order_by(Product.id)).unique().all()
    return [product_out(db, product, store_id) for product in products]


@app.get("/api/v1/products/{product_id}", response_model=ProductOut)
def get_product(product_id: int, store_id: int = 1, db: Session = Depends(get_db)):
    product = db.get(Product, product_id)
    if product is None or not product.active:
        raise HTTPException(status_code=404, detail="Product not found.")
    return product_out(db, product, store_id)


@app.get("/api/v1/products/{product_id}/alternatives", response_model=BasketRescueOut)
def recommend_alternatives(
    product_id: int,
    store_id: int = 1,
    quantity: int = Query(default=1, ge=1, le=99),
    max_price: Decimal | None = Query(default=None, gt=0),
    db: Session = Depends(get_db),
):
    """Return in-stock, same-use alternatives that stay within the shopper's price cap."""
    source = db.get(Product, product_id)
    if source is None or not source.active:
        raise HTTPException(status_code=404, detail="Product not found.")
    if db.get(Store, store_id) is None:
        raise HTTPException(status_code=404, detail="Store not found.")

    ceiling = max_price if max_price is not None else source.price * Decimal("1.15")
    candidates = db.scalars(select(Product).where(
        Product.active.is_(True),
        Product.id != source.id,
        Product.substitution_group == source.substitution_group,
    )).all()
    available = [candidate for candidate in candidates
                 if candidate.price <= ceiling and available_for(db, candidate.id, store_id) >= quantity]
    available.sort(key=lambda candidate: (
        candidate.price > source.price,
        abs(candidate.price - source.price),
        -available_for(db, candidate.id, store_id),
        candidate.name,
    ))
    recommendations = []
    for candidate in available[:5]:
        stock = available_for(db, candidate.id, store_id)
        price_delta = candidate.price - source.price
        reason = (f"Same kind · {stock} in stock" if price_delta <= 0
                  else f"Same kind · within your ₹{ceiling - source.price:.0f} swap cap")
        recommendations.append(AlternativeOut(
            **product_out(db, candidate, store_id).model_dump(),
            price_difference=float(price_delta),
            reason=reason,
        ))
    return BasketRescueOut(
        product_id=source.id,
        requested_quantity=quantity,
        max_price=float(ceiling),
        alternatives=recommendations,
    )


@app.post("/api/v1/ai/basket-plan", response_model=BasketPlanOut)
async def create_basket_plan(
    payload: BasketPlanRequest,
    user: User = Depends(current_user),
    db: Session = Depends(get_db),
):
    del user
    prompt = payload.prompt.strip()
    if len(prompt) < 8:
        raise HTTPException(status_code=422, detail="Describe the meal or basket you need in at least 8 characters.")
    if db.get(Store, payload.store_id) is None:
        raise HTTPException(status_code=404, detail="Store not found.")

    catalog = db.scalars(select(Product).where(Product.active.is_(True))
                         .options(joinedload(Product.category))).unique().all()
    available_by_id = {
        product.id: available_for(db, product.id, payload.store_id)
        for product in catalog
    }
    available_catalog = [
        {
            "id": product.id,
            "name": product.name,
            "category": product.category.name if product.category else "",
            "description": product.description,
            "price_inr": float(product.price),
            "unit": product.unit,
            "available_stock": available_by_id[product.id],
        }
        for product in catalog if available_by_id[product.id] > 0
    ]
    if not available_catalog:
        raise HTTPException(status_code=422, detail="This store has no available products to plan with.")

    try:
        draft = await create_ai_basket_draft(prompt, payload.budget, available_catalog)
    except AIPlannerUnavailable as error:
        raise HTTPException(status_code=503, detail=str(error)) from error
    except AIPlannerResponseError as error:
        raise HTTPException(status_code=502, detail=str(error)) from error
    except Exception as error:
        logger.exception("AI basket planner request failed")
        raise HTTPException(status_code=502, detail="The AI planner is temporarily unavailable. Try again.") from error

    by_id = {product.id: product for product in catalog}
    seen: set[int] = set()
    output_items = []
    estimated_total = Decimal("0")
    for suggestion in draft.items:
        product = by_id.get(suggestion.product_id)
        if product is None or not product.active or suggestion.product_id in seen:
            raise HTTPException(status_code=502, detail="The AI planner selected an invalid or repeated catalog item.")
        seen.add(product.id)
        available = available_for(db, product.id, payload.store_id)
        if available < suggestion.quantity:
            raise HTTPException(status_code=409, detail="Store stock changed while building this plan. Generate it again.")
        line_total = product.price * suggestion.quantity
        estimated_total += line_total
        output_items.append(BasketPlanItemOut(
            product_id=product.id,
            name=product.name,
            category_id=product.category_id,
            category=product.category.name if product.category else "",
            description=product.description,
            price=float(product.price),
            unit=product.unit,
            emoji=product.emoji,
            substitution_group=product.substitution_group,
            quantity=suggestion.quantity,
            available_stock=available,
            line_total=float(line_total),
            reason=suggestion.reason,
        ))
    if estimated_total > payload.budget:
        raise HTTPException(status_code=502, detail="The AI plan exceeded the budget and was rejected. Try again.")

    return BasketPlanOut(
        title=draft.title,
        summary=draft.summary,
        budget=float(payload.budget),
        estimated_total=float(estimated_total),
        remaining_budget=float(payload.budget - estimated_total),
        items=output_items,
        ai_powered=True,
    )


@app.get("/api/v1/stores/{store_id}/inventory")
def store_inventory(store_id: int, db: Session = Depends(get_db)):
    if db.get(Store, store_id) is None:
        raise HTTPException(status_code=404, detail="Store not found.")
    rows = db.scalars(select(Inventory).where(Inventory.store_id == store_id)).all()
    return [{"product_id": row.product_id, "stock_on_hand": row.stock_on_hand,
             "reserved_qty": row.reserved_qty,
             "available_stock": max(0, row.stock_on_hand - row.reserved_qty)} for row in rows]


@app.post("/api/v1/cart/validate", response_model=CartValidationResponse)
def validate_cart(payload: CartValidationRequest, db: Session = Depends(get_db)):
    if db.get(Store, payload.store_id) is None:
        raise HTTPException(status_code=404, detail="Store not found.")
    totals = aggregate_items(payload.items)
    results = []
    for product_id, requested in totals.items():
        available = available_for(db, product_id, payload.store_id)
        results.append(CartValidationItem(product_id=product_id, requested=requested,
                                          available=available,
                                          valid=available >= requested and db.get(Product, product_id) is not None))
    unavailable = [line.product_id for line in results if not line.valid]
    return CartValidationResponse(valid=not unavailable, items=results, unavailable=unavailable)


@app.get("/api/v1/cart", response_model=CartSyncResponse)
def get_cart(user: User = Depends(current_user), db: Session = Depends(get_db)):
    cart = db.scalar(select(Cart).where(Cart.user_id == user.id))
    if cart is None:
        return CartSyncResponse(items=[], updated_at=datetime.now(timezone.utc))
    items = db.scalars(select(CartItem).where(CartItem.cart_id == cart.id)).all()
    return CartSyncResponse(items=[CartLine(product_id=item.product_id, quantity=item.quantity)
                                   for item in items], updated_at=cart.updated_at)


@app.put("/api/v1/cart", response_model=CartSyncResponse)
def sync_cart(payload: CartSyncRequest, user: User = Depends(current_user),
              db: Session = Depends(get_db)):
    totals = aggregate_items(payload.items)
    try:
        cart = db.scalar(select(Cart).where(Cart.user_id == user.id))
        if cart is None:
            cart = Cart(user_id=user.id)
            db.add(cart)
            db.flush()
        db.execute(delete(CartItem).where(CartItem.cart_id == cart.id))
        for product_id, quantity in totals.items():
            if db.get(Product, product_id) is None:
                raise HTTPException(status_code=404, detail=f"Product {product_id} was removed from the catalog.")
            db.add(CartItem(cart_id=cart.id, product_id=product_id, quantity=quantity))
        cart.updated_at = datetime.now(timezone.utc)
        db.commit()
        return CartSyncResponse(items=[CartLine(product_id=k, quantity=v) for k, v in totals.items()],
                                updated_at=cart.updated_at)
    except Exception:
        db.rollback()
        raise


@app.post("/api/v1/reservations", response_model=ReservationOut, status_code=201)
def create_reservation(payload: ReservationRequest, user: User = Depends(current_user),
                       db: Session = Depends(get_db)):
    totals = aggregate_items(payload.items)
    if db.get(Store, payload.store_id) is None:
        raise HTTPException(status_code=404, detail="Store not found.")
    group_id = str(uuid4())
    expires_at = datetime.now(timezone.utc) + timedelta(minutes=10)
    try:
        release_expired(db)
        for product_id, quantity in totals.items():
            changed = db.execute(update(Inventory)
                .where(Inventory.store_id == payload.store_id,
                       Inventory.product_id == product_id,
                       Inventory.stock_on_hand - Inventory.reserved_qty >= quantity)
                .values(reserved_qty=Inventory.reserved_qty + quantity))
            if changed.rowcount != 1:
                raise HTTPException(status_code=409,
                    detail={"message": "Some items are no longer available.", "product_id": product_id,
                            "available": available_for(db, product_id, payload.store_id)})
            db.add(Reservation(group_id=group_id, user_id=user.id, store_id=payload.store_id,
                               product_id=product_id, quantity=quantity, expires_at=expires_at))
        db.commit()
    except Exception:
        db.rollback()
        raise
    return ReservationOut(reservation_id=group_id, store_id=payload.store_id,
                          expires_at=expires_at,
                          items=[CartLine(product_id=k, quantity=v) for k, v in totals.items()])


@app.delete("/api/v1/reservations/{reservation_id}", status_code=204)
def release_reservation(reservation_id: str, user: User = Depends(current_user),
                        db: Session = Depends(get_db)):
    try:
        reservations = db.scalars(select(Reservation).where(Reservation.group_id == reservation_id,
                                                             Reservation.user_id == user.id,
                                                             Reservation.status == "active")).all()
        if not reservations:
            raise HTTPException(status_code=404, detail="Active reservation not found.")
        for reservation in reservations:
            db.execute(update(Inventory).where(Inventory.store_id == reservation.store_id,
                                               Inventory.product_id == reservation.product_id,
                                               Inventory.reserved_qty >= reservation.quantity)
                       .values(reserved_qty=Inventory.reserved_qty - reservation.quantity))
            reservation.status = "released"
        db.commit()
        return Response(status_code=204)
    except Exception:
        db.rollback()
        raise


@app.post("/api/v1/orders", response_model=OrderOut, status_code=201)
def create_order(payload: OrderRequest, response: Response,
                 idempotency_key: str = Header(alias="Idempotency-Key", min_length=8, max_length=128),
                 user: User = Depends(current_user), db: Session = Depends(get_db)):
    prior = db.execute(select(Order).where(Order.user_id == user.id,
                                            Order.idempotency_key == idempotency_key)
                       .options(joinedload(Order.items).joinedload(OrderItem.product)))\
        .unique().scalar_one_or_none()
    if prior:
        response.status_code = status.HTTP_200_OK
        return order_out(prior)
    if db.get(Store, payload.store_id) is None:
        raise HTTPException(status_code=404, detail="Store not found.")
    totals = aggregate_items(payload.items)
    try:
        release_expired(db)
        reservations: list[Reservation] = []
        reserved_by_product: dict[int, int] = {}
        if payload.reservation_id:
            reservations = db.scalars(select(Reservation).where(
                Reservation.group_id == payload.reservation_id,
                Reservation.user_id == user.id, Reservation.store_id == payload.store_id,
                Reservation.status == "active", Reservation.expires_at > datetime.now(timezone.utc))
            ).all()
            if not reservations:
                raise HTTPException(status_code=409, detail="Your stock hold expired. Please refresh your basket.")
            for row in reservations:
                reserved_by_product[row.product_id] = reserved_by_product.get(row.product_id, 0) + row.quantity
        order = Order(user_id=user.id, store_id=payload.store_id, status="confirmed",
                      total_amount=Decimal("0.00"), idempotency_key=idempotency_key,
                      address=payload.address.strip(), payment_method=payload.payment_method)
        db.add(order)
        # Claim the unique idempotency key before touching inventory. Concurrent retries
        # then wait on this row and either replay the committed order or proceed after rollback.
        db.flush()
        total = Decimal("0.00")
        for product_id, quantity in totals.items():
            product = db.get(Product, product_id)
            if product is None or not product.active:
                raise HTTPException(status_code=409, detail=f"Product {product_id} is no longer available.")
            held = min(quantity, reserved_by_product.get(product_id, 0))
            if held:
                result = db.execute(update(Inventory).where(
                    Inventory.store_id == payload.store_id, Inventory.product_id == product_id,
                    Inventory.stock_on_hand >= held, Inventory.reserved_qty >= held)
                    .values(stock_on_hand=Inventory.stock_on_hand - held,
                            reserved_qty=Inventory.reserved_qty - held))
                if result.rowcount != 1:
                    raise HTTPException(status_code=409, detail="A reserved item changed. Please retry.")
            remainder = quantity - held
            if remainder:
                result = db.execute(update(Inventory).where(
                    Inventory.store_id == payload.store_id, Inventory.product_id == product_id,
                    Inventory.stock_on_hand - Inventory.reserved_qty >= remainder)
                    .values(stock_on_hand=Inventory.stock_on_hand - remainder))
                if result.rowcount != 1:
                    raise HTTPException(status_code=409,
                        detail={"message": f"{product.name} is no longer available in that quantity.",
                                "product_id": product_id,
                                "available": available_for(db, product_id, payload.store_id)})
            item_total = product.price * quantity
            total += item_total
            order.items.append(OrderItem(product_id=product_id, quantity=quantity,
                                         final_price=product.price))
        order.total_amount = total
        remaining_reserved = dict(totals)
        for reservation in reservations:
            # Any held units not used by the order become available again.
            used = min(reservation.quantity, remaining_reserved.get(reservation.product_id, 0))
            remaining_reserved[reservation.product_id] = remaining_reserved.get(reservation.product_id, 0) - used
            excess = reservation.quantity - used
            if excess:
                db.execute(update(Inventory).where(Inventory.store_id == reservation.store_id,
                                                   Inventory.product_id == reservation.product_id,
                                                   Inventory.reserved_qty >= excess)
                           .values(reserved_qty=Inventory.reserved_qty - excess))
            reservation.status = "consumed"
            reservation.order_id = order.id
        db.add(Address(user_id=user.id, address=payload.address.strip(), postal_code=""))
        db.commit()
        db.refresh(order)
        return order_out(order)
    except IntegrityError:
        db.rollback()
        prior = db.execute(select(Order).where(Order.user_id == user.id,
                                                Order.idempotency_key == idempotency_key)
                           .options(joinedload(Order.items).joinedload(OrderItem.product)))\
            .unique().scalar_one_or_none()
        if prior is not None:
            response.status_code = status.HTTP_200_OK
            return order_out(prior)
        raise
    except Exception:
        db.rollback()
        raise


@app.get("/api/v1/orders", response_model=list[OrderOut])
def list_orders(user: User = Depends(current_user), db: Session = Depends(get_db)):
    orders = db.scalars(select(Order).where(Order.user_id == user.id)
                        .options(joinedload(Order.items).joinedload(OrderItem.product))
                        .order_by(Order.created_at.desc())).unique().all()
    return [order_out(order) for order in orders]


@app.get("/api/v1/orders/{order_id}", response_model=OrderOut)
def get_order(order_id: str, user: User = Depends(current_user), db: Session = Depends(get_db)):
    order = db.execute(select(Order).where(Order.id == order_id, Order.user_id == user.id)
                       .options(joinedload(Order.items).joinedload(OrderItem.product)))\
        .unique().scalar_one_or_none()
    if order is None:
        raise HTTPException(status_code=404, detail="Order not found.")
    return order_out(order)


@app.get("/api/v1/orders/{order_id}/substitutions", response_model=list[SubstitutionOut])
def list_order_substitutions(order_id: str, user: User = Depends(current_user),
                             db: Session = Depends(get_db)):
    owns_order = db.scalar(select(Order.id).where(Order.id == order_id, Order.user_id == user.id))
    if owns_order is None:
        raise HTTPException(status_code=404, detail="Order not found.")
    substitutions = db.scalars(select(Substitution).join(OrderItem)
        .where(OrderItem.order_id == order_id)
        .options(joinedload(Substitution.replacement_product))).all()
    return [substitution_out(sub) for sub in substitutions]


@app.post("/api/v1/substitutions", response_model=SubstitutionOut, status_code=201)
def create_substitution(payload: SubstitutionCreate, user: User = Depends(current_user),
                        db: Session = Depends(get_db)):
    order_item = db.scalar(select(OrderItem).join(Order).where(
        OrderItem.id == payload.order_item_id, Order.user_id == user.id))
    replacement = db.get(Product, payload.replacement_product_id)
    if order_item is None:
        raise HTTPException(status_code=404, detail="Order item not found.")
    if replacement is None or not replacement.active:
        raise HTTPException(status_code=404, detail="Replacement product not found.")
    sub = Substitution(order_item_id=order_item.id,
                       replacement_product_id=replacement.id, status="pending",
                       price_difference=replacement.price - order_item.final_price)
    db.add(sub)
    db.commit()
    db.refresh(sub)
    return substitution_out(sub)


@app.post("/api/v1/substitutions/{substitution_id}/decision", response_model=SubstitutionOut)
def decide_substitution(substitution_id: str, payload: SubstitutionDecision,
                        user: User = Depends(current_user), db: Session = Depends(get_db)):
    try:
        sub = db.scalar(select(Substitution).join(OrderItem).join(Order).where(
            Substitution.id == substitution_id, Order.user_id == user.id)
            .options(joinedload(Substitution.replacement_product)))
        if sub is None:
            raise HTTPException(status_code=404, detail="Substitution not found.")
        wanted = "approved" if payload.decision == "approve" else "rejected"
        if sub.status != "pending":
            if sub.status == wanted:
                return substitution_out(sub)
            raise HTTPException(status_code=409, detail="This substitution already has a decision.")
        if wanted == "approved":
            order_item = db.get(OrderItem, sub.order_item_id)
            order = db.get(Order, order_item.order_id)
            inventory = db.execute(update(Inventory).where(
                Inventory.store_id == order.store_id,
                Inventory.product_id == sub.replacement_product_id,
                Inventory.stock_on_hand - Inventory.reserved_qty >= order_item.quantity)
                .values(stock_on_hand=Inventory.stock_on_hand - order_item.quantity))
            if inventory.rowcount != 1:
                raise HTTPException(status_code=409, detail="The replacement just sold out. Choose another item.")
            order.total_amount += sub.price_difference * order_item.quantity
            order_item.product_id = sub.replacement_product_id
            order_item.final_price = sub.replacement_product.price
        sub.status = wanted
        db.commit()
        db.refresh(sub)
        return substitution_out(sub)
    except Exception:
        db.rollback()
        raise
