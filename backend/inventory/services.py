"""Stock movement services.

Applies the quantity deltas for a movement to the affected inventory rows
under a row lock so concurrent movements cannot oversell. Transfer movements
write an inbound/outbound pair linked through ``paired_movement``.
"""

from datetime import date as date_type
from decimal import Decimal

from django.db import transaction

from .models import Inventory, StockMovement


class InsufficientStockError(Exception):
    def __init__(self, product, warehouse, available=None, requested=None):
        self.product = product
        self.warehouse = warehouse
        self.available = available
        self.requested = requested
        super().__init__(
            f"Insufficient stock for {product.venture.venture_code}:{product.sku} "
            f"in {warehouse.warehouse_code} (available {available or 0}, "
            f"requested {requested or 0})"
        )


def _inventory_row(warehouse, product, *, lock=False):
    rows = Inventory.objects.select_for_update() if lock else Inventory.objects
    inventory = (
        rows.filter(warehouse=warehouse, product=product).first()
        or Inventory(venture=product.venture, warehouse=warehouse, product=product)
    )
    if not inventory.pk:
        inventory.reorder_level = product.reorder_level
    return inventory


def _decrement(inventory, amount):
    if inventory.quantity - amount < 0:
        raise InsufficientStockError(
            inventory.product, inventory.warehouse,
            available=inventory.quantity, requested=amount,
        )
    inventory.quantity -= amount


def record_stock_movement(
    request=None,
    *,
    venture,
    warehouse,
    product,
    movement_type,
    quantity,
    movement_date=None,
    notes="",
    reference_type="",
    reference_id="",
    destination_warehouse=None,
):
    """Create a :class:`StockMovement` and apply its inventory delta.

    Returns the ``(movement, paired_movement_or_None)`` tuple. ``TRANSFER_OUT``
    requires ``destination_warehouse`` and creates the paired ``TRANSFER_IN``.
    """
    amount = Decimal(str(quantity))
    if amount <= 0:
        raise ValueError("Quantity must be greater than zero")
    if product.venture_id != venture.id:
        raise ValueError("Product does not belong to the given venture")
    if warehouse.venture_id != venture.id:
        raise ValueError("Warehouse does not belong to the given venture")

    moved_on = movement_date or date_type.today()
    user = getattr(request, "user", None) if request else None
    is_authenticated = getattr(user, "is_authenticated", False)

    with transaction.atomic():
        if movement_type in StockMovement.OUT_DECREASING:
            moving_out = True
        elif movement_type in StockMovement.IN_CREASING:
            moving_out = False
        else:
            raise ValueError(f"Unknown movement type: {movement_type}")

        movement = StockMovement(
            venture=venture,
            warehouse=warehouse,
            product=product,
            movement_type=movement_type,
            quantity=amount,
            movement_date=moved_on,
            notes=notes,
            reference_type=reference_type,
            reference_id=reference_id,
            destination_warehouse=destination_warehouse,
        )
        if is_authenticated:
            movement.created_by = user
        movement.save()
        movement.refresh_from_db()

        paired = None
        delta = -amount if moving_out else amount

        if movement_type == StockMovement.MOVE_TRANSFER_OUT:
            if destination_warehouse is None:
                raise ValueError("destination_warehouse is required for transfers")
            if destination_warehouse.venture_id != venture.id or destination_warehouse.id == warehouse.id:
                raise ValueError("Destination must be another warehouse in the same venture")
            warehouse_row = _inventory_row(warehouse, product, lock=True)
            _decrement(warehouse_row, amount)
            warehouse_row.reorder_level = product.reorder_level
            warehouse_row.save()

            destination_row = _inventory_row(destination_warehouse, product, lock=True)
            destination_row.quantity += amount
            destination_row.reorder_level = product.reorder_level
            destination_row.save()

            paired = StockMovement(
                venture=venture,
                warehouse=destination_warehouse,
                product=product,
                movement_type=StockMovement.MOVE_TRANSFER_IN,
                quantity=amount,
                movement_date=moved_on,
                notes=notes,
                reference_type=reference_type,
                reference_id=reference_id,
            )
            if is_authenticated:
                paired.created_by = user
            paired.save()
            movement.paired_movement = paired
            paired.paired_movement = movement
            movement.save(update_fields=["paired_movement"])
            paired.save(update_fields=["paired_movement"])
        else:
            row = _inventory_row(warehouse, product, lock=True)
            if moving_out:
                _decrement(row, amount)
            else:
                row.quantity += amount
            row.reorder_level = product.reorder_level
            row.save()

        movement.refresh_from_db()
        return movement, paired