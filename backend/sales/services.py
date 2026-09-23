"""Sale finalization logic.

Creating a sale moves stock OUT (SALE movements) from the chosen warehouse and
computes bill totals (customer balance = due). Creating a sale return moves
goods back IN (SALES_RETURN movements) and settles the bill balances.
"""

from decimal import Decimal

from django.db import transaction
from django.db.models import Sum

from inventory.services import InsufficientStockError, record_stock_movement
from notifications.models import Notification
from notifications.services import notify_venture_users

from .models import Sale, SaleReturn, money2


def _finalize_item_lines(items):
    for item in items:
        item.recompute()
        item.save()


def finalize_sale(request, sale, warehouse, items):
    with transaction.atomic():
        sale.warehouse = warehouse
        sale.save()
        if warehouse is not None:
            for item in items:
                try:
                    record_stock_movement(
                        request,
                        venture=sale.venture,
                        warehouse=warehouse,
                        product=item.product,
                        movement_type="SALE",
                        quantity=item.quantity,
                        notes=f"SI {sale.invoice_number}",
                        reference_type="Sale",
                        reference_id=sale.id,
                    )
                except InsufficientStockError as exc:
                    raise ValueError(str(exc)) from exc
        _finalize_item_lines(items)
        sale.recompute()
        sale.save(
            update_fields=[
                "warehouse", "subtotal", "discount", "tax",
                "total_amount", "paid_amount", "returned_amount", "due_amount",
            ]
        )
        notify_venture_users(
            sale.venture,
            Notification.TYPE_SALE_CREATED,
            f"New sale {sale.invoice_number} recorded for {sale.total_amount}",
            perm_code="sales.manage",
        )


def finalize_sale_return(request, sale_return, items):
    with transaction.atomic():
        sale = Sale.objects.select_for_update().get(pk=sale_return.sale_id)

        when_seen = {}
        for item in items:
            key = item.product_id
            when_seen[key] = when_seen.get(key, Decimal("0"))
            original = (
                sale.items.filter(product_id=key).aggregate(s=Sum("quantity"))["s"]
                or Decimal("0")
            )
            already = (
                SaleReturn.objects.filter(sale=sale, status="COMPLETED")
                .exclude(pk=sale_return.pk)
                .filter(items__product_id=key)
                .aggregate(s=Sum("items__quantity"))["s"]
                or Decimal("0")
            )
            cumulative = when_seen[key] + item.quantity
            if cumulative + already > original:
                raise ValueError(
                    f"Return quantity for {item.product.product_name} exceeds sold quantity"
                )
            when_seen[key] = cumulative

            if sale.warehouse is not None:
                try:
                    record_stock_movement(
                        request,
                        venture=sale.venture,
                        warehouse=sale.warehouse,
                        product=item.product,
                        movement_type="SALES_RETURN",
                        quantity=item.quantity,
                        notes=f"Return {sale.invoice_number}",
                        reference_type="SaleReturn",
                        reference_id=sale_return.id,
                    )
                except InsufficientStockError as exc:
                    raise ValueError(str(exc)) from exc

        _finalize_item_lines(items)
        total = sum((item.total for item in items), Decimal("0"))
        sale_return.total_amount = money2(total)
        sale_return.save(update_fields=["total_amount"])

        sale.returned_amount = money2(sale.returned_amount + sale_return.total_amount)
        sale.recompute()
        if sale.returned_amount >= sale.total_amount:
            sale.status = Sale.STATUS_RETURNED
        elif sale.returned_amount > 0:
            sale.status = Sale.STATUS_PARTIAL
        sale.save(
            update_fields=[
                "returned_amount", "due_amount", "status",
            ]
        )