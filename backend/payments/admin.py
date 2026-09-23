from django.contrib import admin

from .models import Payment


@admin.register(Payment)
class PaymentAdmin(admin.ModelAdmin):
    list_display = [
        "payment_type", "reference_type", "reference_id", "amount",
        "payment_date", "payment_method", "venture",
    ]
    list_filter = ["payment_type", "reference_type", "payment_method", "venture"]
    search_fields = ["transaction_reference", "notes"]