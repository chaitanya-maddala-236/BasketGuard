from __future__ import annotations

from datetime import datetime, timezone
from decimal import Decimal
from uuid import uuid4

from sqlalchemy import (CheckConstraint, DateTime, ForeignKey, Index, Integer,
                        Numeric, String, Text, UniqueConstraint, func)
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.db import Base


def utcnow() -> datetime:
    return datetime.now(timezone.utc)


class User(Base):
    __tablename__ = "users"
    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=lambda: str(uuid4()))
    name: Mapped[str] = mapped_column(String(120))
    email: Mapped[str] = mapped_column(String(254), unique=True, index=True)
    password_hash: Mapped[str] = mapped_column(String(255))
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow)


class Store(Base):
    __tablename__ = "stores"
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    name: Mapped[str] = mapped_column(String(120))
    location: Mapped[str] = mapped_column(String(180))


class Category(Base):
    __tablename__ = "categories"
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    name: Mapped[str] = mapped_column(String(80), unique=True)
    icon: Mapped[str] = mapped_column(String(12), default="🛍️")


class Product(Base):
    __tablename__ = "products"
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    name: Mapped[str] = mapped_column(String(140), index=True)
    category_id: Mapped[int] = mapped_column(ForeignKey("categories.id"), index=True)
    description: Mapped[str] = mapped_column(Text, default="Freshly picked for your everyday basket.")
    price: Mapped[Decimal] = mapped_column(Numeric(10, 2))
    unit: Mapped[str] = mapped_column(String(60))
    emoji: Mapped[str] = mapped_column(String(12), default="🛒")
    image_url: Mapped[str | None] = mapped_column(String(500), nullable=True)
    active: Mapped[bool] = mapped_column(default=True)
    category: Mapped[Category] = relationship()


class Inventory(Base):
    __tablename__ = "inventory"
    __table_args__ = (UniqueConstraint("store_id", "product_id", name="uq_inventory_store_product"),
                      CheckConstraint("stock_on_hand >= 0", name="ck_inventory_stock_nonnegative"),
                      CheckConstraint("reserved_qty >= 0", name="ck_inventory_reserved_nonnegative"),
                      Index("ix_inventory_product_store", "product_id", "store_id"))
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    store_id: Mapped[int] = mapped_column(ForeignKey("stores.id", ondelete="CASCADE"))
    product_id: Mapped[int] = mapped_column(ForeignKey("products.id", ondelete="CASCADE"))
    stock_on_hand: Mapped[int] = mapped_column(Integer, default=0)
    reserved_qty: Mapped[int] = mapped_column(Integer, default=0)
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow, onupdate=utcnow)


class Cart(Base):
    __tablename__ = "carts"
    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=lambda: str(uuid4()))
    user_id: Mapped[str] = mapped_column(ForeignKey("users.id", ondelete="CASCADE"), unique=True)
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow, onupdate=utcnow)


class CartItem(Base):
    __tablename__ = "cart_items"
    __table_args__ = (UniqueConstraint("cart_id", "product_id", name="uq_cart_product"),
                      CheckConstraint("quantity > 0", name="ck_cart_item_quantity_positive"))
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    cart_id: Mapped[str] = mapped_column(ForeignKey("carts.id", ondelete="CASCADE"), index=True)
    product_id: Mapped[int] = mapped_column(ForeignKey("products.id"))
    quantity: Mapped[int] = mapped_column(Integer)


class Reservation(Base):
    __tablename__ = "reservations"
    __table_args__ = (CheckConstraint("quantity > 0", name="ck_reservation_quantity_positive"),
                      Index("ix_reservations_expiry_status", "status", "expires_at"))
    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=lambda: str(uuid4()))
    group_id: Mapped[str] = mapped_column(String(36), index=True, default=lambda: str(uuid4()))
    user_id: Mapped[str] = mapped_column(ForeignKey("users.id", ondelete="CASCADE"), index=True)
    store_id: Mapped[int] = mapped_column(ForeignKey("stores.id"))
    order_id: Mapped[str | None] = mapped_column(ForeignKey("orders.id", ondelete="SET NULL"), nullable=True)
    product_id: Mapped[int] = mapped_column(ForeignKey("products.id"))
    quantity: Mapped[int] = mapped_column(Integer)
    expires_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), index=True)
    status: Mapped[str] = mapped_column(String(20), default="active", index=True)


class Order(Base):
    __tablename__ = "orders"
    __table_args__ = (UniqueConstraint("user_id", "idempotency_key", name="uq_order_user_idempotency"),
                      Index("ix_orders_user_created", "user_id", "created_at"))
    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=lambda: str(uuid4()))
    user_id: Mapped[str] = mapped_column(ForeignKey("users.id", ondelete="CASCADE"), index=True)
    store_id: Mapped[int] = mapped_column(ForeignKey("stores.id"))
    status: Mapped[str] = mapped_column(String(30), default="confirmed")
    total_amount: Mapped[Decimal] = mapped_column(Numeric(10, 2))
    idempotency_key: Mapped[str] = mapped_column(String(128))
    address: Mapped[str] = mapped_column(String(400))
    payment_method: Mapped[str] = mapped_column(String(30), default="mock_card")
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow, server_default=func.now())
    items: Mapped[list[OrderItem]] = relationship(cascade="all, delete-orphan", lazy="selectin")


class OrderItem(Base):
    __tablename__ = "order_items"
    __table_args__ = (CheckConstraint("quantity > 0", name="ck_order_item_quantity_positive"),)
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    order_id: Mapped[str] = mapped_column(ForeignKey("orders.id", ondelete="CASCADE"), index=True)
    product_id: Mapped[int] = mapped_column(ForeignKey("products.id"))
    quantity: Mapped[int] = mapped_column(Integer)
    final_price: Mapped[Decimal] = mapped_column(Numeric(10, 2))
    product: Mapped[Product] = relationship()


class Substitution(Base):
    __tablename__ = "substitutions"
    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=lambda: str(uuid4()))
    order_item_id: Mapped[int] = mapped_column(ForeignKey("order_items.id", ondelete="CASCADE"), index=True)
    replacement_product_id: Mapped[int] = mapped_column(ForeignKey("products.id"))
    status: Mapped[str] = mapped_column(String(20), default="pending")
    price_difference: Mapped[Decimal] = mapped_column(Numeric(10, 2), default=Decimal("0.00"))
    replacement_product: Mapped[Product] = relationship()


class Address(Base):
    __tablename__ = "addresses"
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    user_id: Mapped[str] = mapped_column(ForeignKey("users.id", ondelete="CASCADE"), index=True)
    label: Mapped[str] = mapped_column(String(40), default="Home")
    address: Mapped[str] = mapped_column(String(400))
    postal_code: Mapped[str] = mapped_column(String(20))
