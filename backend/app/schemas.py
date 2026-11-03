from __future__ import annotations

from datetime import datetime
from pydantic import BaseModel, ConfigDict, EmailStr, Field


class RegisterRequest(BaseModel):
    name: str = Field(min_length=2, max_length=120)
    email: EmailStr
    password: str = Field(min_length=8, max_length=128)


class LoginRequest(BaseModel):
    email: EmailStr
    password: str


class TokenResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"
    user_id: str
    name: str


class CategoryOut(BaseModel):
    id: int
    name: str
    icon: str
    model_config = ConfigDict(from_attributes=True)


class ProductOut(BaseModel):
    id: int
    name: str
    category_id: int
    description: str
    price: float
    unit: str
    emoji: str
    image_url: str | None = None
    available_stock: int = 0


class AlternativeOut(ProductOut):
    price_difference: float
    reason: str


class BasketRescueOut(BaseModel):
    product_id: int
    requested_quantity: int
    max_price: float
    alternatives: list[AlternativeOut]


class InventoryOut(BaseModel):
    product_id: int
    stock_on_hand: int
    reserved_qty: int
    available_stock: int


class CartLine(BaseModel):
    product_id: int = Field(gt=0)
    quantity: int = Field(gt=0, le=99)


class CartSyncRequest(BaseModel):
    items: list[CartLine] = Field(max_length=100)


class CartSyncResponse(BaseModel):
    items: list[CartLine]
    updated_at: datetime


class CartValidationRequest(BaseModel):
    store_id: int = 1
    items: list[CartLine] = Field(min_length=1, max_length=100)


class CartValidationItem(BaseModel):
    product_id: int
    requested: int
    available: int
    valid: bool


class CartValidationResponse(BaseModel):
    valid: bool
    items: list[CartValidationItem]
    unavailable: list[int]


class ReservationRequest(BaseModel):
    store_id: int = 1
    items: list[CartLine] = Field(min_length=1, max_length=100)


class ReservationOut(BaseModel):
    reservation_id: str
    store_id: int
    expires_at: datetime
    items: list[CartLine]
    status: str = "active"


class OrderRequest(BaseModel):
    store_id: int = 1
    address: str = Field(min_length=8, max_length=400)
    payment_method: str = Field(default="mock_card", pattern="^mock_card$")
    reservation_id: str | None = None
    items: list[CartLine] = Field(min_length=1, max_length=100)


class OrderItemOut(BaseModel):
    id: int
    product_id: int
    product_name: str
    quantity: int
    final_price: float


class OrderOut(BaseModel):
    id: str
    status: str
    total_amount: float
    address: str
    created_at: datetime
    items: list[OrderItemOut]


class SubstitutionCreate(BaseModel):
    order_item_id: int
    replacement_product_id: int


class SubstitutionDecision(BaseModel):
    decision: str = Field(pattern="^(approve|reject)$")


class SubstitutionOut(BaseModel):
    id: str
    order_item_id: int
    replacement_product_id: int
    replacement_product_name: str
    status: str
    price_difference: float
