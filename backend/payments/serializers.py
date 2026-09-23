from rest_framework import serializers

from purchases.models import Purchase
from sales.models import Sale

from .models import Payment
from .services import apply_payment


class PaymentSerializer(serializers.ModelSerializer):
    venture_name = serializers.CharField(source="venture.venture_code", read_only=True)
    reference_label = serializers.SerializerMethodField()

    class Meta:
        model = Payment
        fields = [
            "id", "venture", "venture_name", "payment_type", "reference_type",
            "reference_id", "reference_label", "amount", "payment_date",
            "payment_method", "transaction_reference", "notes", "created_by",
            "created_at",
        ]
        read_only_fields = ["created_by", "created_at"]

    def get_reference_label(self, obj):
        model = Sale if obj.reference_type == Payment.REF_SALE else Purchase
        row = model.objects.filter(
            pk=obj.reference_id, venture=obj.venture
        ).values_list("invoice_number", flat=True).first()
        return row or f"{obj.reference_type} #{obj.reference_id}"

    def validate(self, attrs):
        ptype = attrs.get("payment_type")
        rtype = attrs.get("reference_type")
        if ptype and rtype:
            if ptype == Payment.TYPE_RECEIVED and rtype == Payment.REF_PURCHASE:
                raise serializers.ValidationError(
                    {"reference_type": ["Received payments must reference a sale"]}
                )
            if ptype == Payment.TYPE_PAID and rtype == Payment.REF_SALE:
                raise serializers.ValidationError(
                    {"reference_type": ["Paid payments must reference a purchase"]}
                )
        return attrs

    def create(self, validated_data):
        request = self.context.get("request")
        user = getattr(request, "user", None)
        if user is not None and getattr(user, "is_authenticated", False):
            validated_data["created_by"] = user

        payment = Payment(**validated_data)
        apply_payment(payment)
        payment.save()
        return payment