"""Purchase finalization logic.

Creating a purchase records the incoming stock (PURCHASE movements) at the
chosen warehouse and computes bill totals. Creating a purchase return moves
goods back (PURCHASE_RETURN movements) and reduces the purchase balance.
"""

from decimal import Decimal

from django.db import transaction
from django.db.models import Sum

from inventory.services import InsufficientStockError, record_stock_movement

from .models import Purchase, PurchaseReturn, money2


def _finalize_item_lines(items):
    for item in items:
        item.recompute()
        item.save()


def finalize_purchase(request, purchase, warehouse, items):
    """Persist purchase items, total the bill and apply stock-in movements."""
    with transaction.atomic():
        purchase.warehouse = warehouse
        if warehouse is not None:
            for item in items:
                _apply_stock(
                    request, purchase, warehouse, item.product,
                    item.quantity, item,
                )
        _finalize_item_lines(items)
        purchase.recompute()
        purchase.save(
            update_fields=[
                "warehouse", "subtotal", "discount", "tax",
                "total_amount", "paid_amount", "returned_amount", "due_amount",
            ]
        )


def finalize_purchase_return(request, purchase_return, items):
    """Persist return items, apply stock-out movements and settle balances."""
    with transaction.atomic():
        purchase = Purchase.objects.select_for_update().get(pk=purchase_return.purchase_id)

        when_seen = {}
        for item in items:
            key = item.product_id
            when_seen[key] = when_seen.get(key, Decimal("0"))
            original = (
                purchase.items.filter(product_id=key).aggregate(s=Sum("quantity"))["s"]
                or Decimal("0")
            )
            already = (
                PurchaseReturn.objects.filter(purchase=purchase, status="COMPLETED")
                .exclude(pk=purchase_return.pk)
                .filter(items__product_id=key)
                .aggregate(s=Sum("items__quantity"))["s"]
                or Decimal("0")
            )
            cumulative = when_seen[key] + item.quantity
            if cumulative + already > original:
                raise ValueError(
                    f"Return quantity for {item.product.product_name} exceeds purchased quantity"
                )
            when_seen[key] = cumulative

            if purchase.warehouse is not None:
                try:
                    record_stock_movement(
                        request,
                        venture=purchase.venture,
                        warehouse=purchase.warehouse,
                        product=item.product,
                        movement_type="PURCHASE_RETURN",
                        quantity=item.quantity,
                        notes=f"Return {purchase.invoice_number}",
                        reference_type="PurchaseReturn",
                        reference_id=purchase_return.id,
                    )
                except InsufficientStockError as exc:
                    raise ValueError(str(exc)) from exc

        _finalize_item_lines(items)
        total = sum((item.total for item in items), Decimal("0"))
        purchase_return.total_amount = money2(total)
        purchase_return.save(update_fields=["total_amount"])

        purchase.returned_amount = money2(
            purchase.returned_amount + purchase_return.total_amount
        )
        purchase.recompute()
        if purchase.returned_amount > 0 and purchase.due_amount == 0:
            purchase.status = Purchase.STATUS_RETURNED
        elif purchase.returned_amount > 0:
            purchase.status = Purchase.STATUS_PARTIAL
        purchase.save(
            update_fields=[
                "returned_amount", "due_amount", "status",
            ]
        )


def _apply_stock(request, purchase, warehouse, product, quantity, item):
    try:
        record_stock_movement(
            request,
            venture=purchase.venture,
            warehouse=warehouse,
            product=product,
            movement_type="PURCHASE",
            quantity=quantity,
            notes=f"PO {purchase.invoice_number}",
            reference_type="Purchase",
            reference_id=purchase.id,
        )
    except InsufficientStockError as exc:
        raise ValueError(str(exc)) from exc