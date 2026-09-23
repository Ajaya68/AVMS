"""Payment application logic.

A RECEIVED payment settles a sale's due (money in); a PAID payment settles a
purchase's due (money out). Applying adds to the bill's paid_amount and the
payment can be reversed on delete.
"""

from django.db import transaction

from purchases.models import Purchase
from sales.models import Sale

from .models import Payment, money2


def _resolve_bill(payment):
    if payment.reference_type == Payment.REF_SALE:
        bill = Sale.objects.select_for_update().filter(
            pk=payment.reference_id, venture=payment.venture
        ).first()
        kind = "sale"
    else:
        bill = Purchase.objects.select_for_update().filter(
            pk=payment.reference_id, venture=payment.venture
        ).first()
        kind = "purchase"

    if bill is None:
        raise ValueError(f"{kind.title()} not found in the selected venture")
    return bill


def apply_payment(payment):
    with transaction.atomic():
        bill = _resolve_bill(payment)
        amount = money2(payment.amount)
        if amount <= 0:
            raise ValueError("Payment amount must be greater than zero")
        remaining = money2(bill.total_amount - bill.paid_amount - bill.returned_amount)
        if amount > remaining:
            raise ValueError(
                f"Payment exceeds the outstanding balance ({remaining})"
            )
        bill.paid_amount = money2(bill.paid_amount + amount)
        bill.recompute()
        bill.save(update_fields=["paid_amount", "due_amount"])
        # keep payment_amount consistent with what was actually applied
        payment.amount = amount


def reverse_payment(payment):
    with transaction.atomic():
        bill = _resolve_bill(payment)
        bill.paid_amount = money2(bill.paid_amount - money2(payment.amount))
        if bill.paid_amount < 0:
            bill.paid_amount = money2(0)
        bill.recompute()
        bill.save(update_fields=["paid_amount", "due_amount"])