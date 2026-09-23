from datetime import date, timedelta
from decimal import ROUND_HALF_UP, Decimal

from collections import OrderedDict
from django.db.models import Count, F, Sum
from rest_framework.permissions import IsAuthenticated
from rest_framework.views import APIView

from accounts.permissions import HasPermissionFromView
from core.responses import success
from expenses.models import Expense
from inventory.models import Inventory
from payments.models import Payment
from products.models import Product
from purchases.models import Purchase
from sales.models import Sale, SaleItem
from ventures.services import get_request_venture


def money2(value) -> Decimal:
    return Decimal(value).quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)


def _period(request, default_days: int = 30):
    today = date.today()
    from_date = request.query_params.get("from", (today - timedelta(days=default_days)).isoformat())
    to_date = request.query_params.get("to", today.isoformat())
    return from_date, to_date


class _ReportView(APIView):
    permission_classes = [IsAuthenticated, HasPermissionFromView]
    required_permission = "reports.view"

    def _venture(self, request):
        return get_request_venture(request)


class SalesReportView(_ReportView):
    def get(self, request):
        venture = self._venture(request)
        from_date, to_date = _period(request)
        sales = Sale.objects.filter(
            venture=venture, sale_date__gte=from_date, sale_date__lte=to_date
        ).exclude(status=Sale.STATUS_CANCELLED)

        sales_list = list(sales)
        total_amount = sum((s.total_amount for s in sales_list), Decimal("0"))
        returned_amount = sum((s.returned_amount for s in sales_list), Decimal("0"))

        items = SaleItem.objects.filter(
            sale__in=sales_list
        ).aggregate(items_sold=Sum("quantity"))
        items_sold = money2(items.get("items_sold") or 0)

        top_products = list(
            SaleItem.objects.filter(sale__in=sales_list)
            .values("product__id", "product__sku", "product__product_name")
            .annotate(quantity=Sum("quantity"), revenue=Sum("total"))
            .order_by("-revenue")[:10]
        )
        for p in top_products:
            p["product_name"] = p.pop("product__product_name")
            p["quantity"] = money2(p["quantity"])
            p["revenue"] = money2(p["revenue"])

        series = {}
        for s in sales.order_by("sale_date").only("id", "sale_date", "total_amount"):
            key = s.sale_date.isoformat()
            series.setdefault(key, {"amount": Decimal("0"), "count": 0})
            series[key]["amount"] += s.total_amount
            series[key]["count"] += 1

        return success(
            {
                "period": {"from": from_date, "to": to_date},
                "summary": {
                    "count": len(sales_list),
                    "items_sold": items_sold,
                    "total_amount": money2(total_amount),
                    "returned_amount": money2(returned_amount),
                    "net_amount": money2(total_amount - returned_amount),
                },
                "top_products": top_products,
                "series": [
                    {"label": k, "amount": money2(v["amount"]), "count": v["count"]}
                    for k, v in series.items()
                ],
            },
            message="Sales report fetched",
        )


class PurchasesReportView(_ReportView):
    def get(self, request):
        venture = self._venture(request)
        from_date, to_date = _period(request)
        purchases = Purchase.objects.filter(
            venture=venture, purchase_date__gte=from_date, purchase_date__lte=to_date
        ).exclude(status=Purchase.STATUS_CANCELLED)

        purchases_list = list(purchases)
        total_amount = sum((p.total_amount for p in purchases_list), Decimal("0"))
        returned_amount = sum((p.returned_amount for p in purchases_list), Decimal("0"))

        top_suppliers = list(
            purchases.values("supplier__id", "supplier__name")
            .annotate(amount=Sum("total_amount"), count=Count("id"))
            .order_by("-amount")[:10]
        )
        for s in top_suppliers:
            s["supplier_name"] = s.pop("supplier__name")
            s["amount"] = money2(s["amount"])

        series = {}
        for p in purchases.order_by("purchase_date").only("id", "purchase_date", "total_amount"):
            key = p.purchase_date.isoformat()
            series.setdefault(key, {"amount": Decimal("0"), "count": 0})
            series[key]["amount"] += p.total_amount
            series[key]["count"] += 1

        return success(
            {
                "period": {"from": from_date, "to": to_date},
                "summary": {
                    "count": len(purchases_list),
                    "total_amount": money2(total_amount),
                    "returned_amount": money2(returned_amount),
                    "net_amount": money2(total_amount - returned_amount),
                },
                "top_suppliers": top_suppliers,
                "series": [
                    {"label": k, "amount": money2(v["amount"]), "count": v["count"]}
                    for k, v in series.items()
                ],
            },
            message="Purchases report fetched",
        )


class InventoryReportView(_ReportView):
    def get(self, request):
        venture = self._venture(request)
        stocks = Inventory.objects.filter(venture=venture).select_related(
            "product", "warehouse"
        )
        items, low_stock = [], []
        total_quantity = Decimal("0")
        stock_value = Decimal("0")
        for stock in stocks:
            qty = stock.available
            value = money2(qty * stock.product.purchase_price)
            total_quantity += qty
            stock_value += value
            row = {
                "product_code": stock.product.sku,
                "product_name": stock.product.product_name,
                "warehouse": stock.warehouse.warehouse_name,
                "quantity": money2(qty),
                "unit_cost": money2(stock.product.purchase_price),
                "valuation": money2(value),
            }
            items.append(row)
            if stock.is_low:
                low_stock.append({**row, "reorder_level": money2(stock.reorder_level)})

        value_products = Product.objects.filter(
            venture=venture, status=Product.STATUS_ACTIVE
        ).count()

        items.sort(key=lambda r: r["product_name"])
        return success(
            {
                "summary": {
                    "stock_products": len(stocks.values_list("product", flat=True).distinct()),
                    "active_products": value_products,
                    "total_quantity": money2(total_quantity),
                    "stock_value": money2(stock_value),
                },
                "items": items,
                "low_stock": low_stock,
            },
            message="Inventory report fetched",
        )


class FinancialReportView(_ReportView):
    def get(self, request):
        venture = self._venture(request)
        from_date, to_date = _period(request)
        sales = Sale.objects.filter(
            venture=venture, sale_date__gte=from_date, sale_date__lte=to_date
        ).exclude(status=Sale.STATUS_CANCELLED)
        purchases = Purchase.objects.filter(
            venture=venture, purchase_date__gte=from_date, purchase_date__lte=to_date
        ).exclude(status=Purchase.STATUS_CANCELLED)
        expenses = Expense.objects.filter(
            venture=venture, expense_date__gte=from_date, expense_date__lte=to_date
        )
        payments = Payment.objects.filter(
            venture=venture, payment_date__gte=from_date, payment_date__lte=to_date
        )

        revenue = sum((s.total_amount for s in sales), Decimal("0")) - sum(
            (s.returned_amount for s in sales), Decimal("0")
        )
        purchase_cost = sum((p.total_amount for p in purchases), Decimal("0")) - sum(
            (p.returned_amount for p in purchases), Decimal("0")
        )
        expense_total = sum((e.amount for e in expenses), Decimal("0"))

        cogs = SaleItem.objects.filter(sale__in=list(sales)).aggregate(
            value=Sum(F("quantity") * F("product__purchase_price"))
        )
        cogs_total = money2(cogs.get("value") or 0)

        all_sales = Sale.objects.filter(venture=venture).exclude(status=Sale.STATUS_CANCELLED)
        all_purchases = Purchase.objects.filter(venture=venture).exclude(status=Purchase.STATUS_CANCELLED)

        paid_in = sum(
            (p.amount for p in payments.filter(payment_type=Payment.TYPE_RECEIVED)),
            Decimal("0"),
        )
        paid_out = sum(
            (p.amount for p in payments.filter(payment_type=Payment.TYPE_PAID)),
            Decimal("0"),
        )

        receivables = sum((s.due_amount for s in all_sales), Decimal("0"))
        payables = sum((p.due_amount for p in all_purchases), Decimal("0"))

        return success(
            {
                "period": {"from": from_date, "to": to_date},
                "summary": {
                    "revenue": money2(revenue),
                    "purchases": money2(purchase_cost),
                    "expenses": money2(expense_total),
                    "cogs": cogs_total,
                    "gross_margin": money2(revenue - cogs_total),
                    "net_profit": money2(revenue - cogs_total - expense_total),
                },
                "outstanding": {
                    "receivables": money2(receivables),
                    "payables": money2(payables),
                },
                "cash_flow": {
                    "received": money2(paid_in),
                    "paid": money2(paid_out),
                    "net": money2(paid_in - paid_out),
                },
            },
            message="Financial report fetched",
        )