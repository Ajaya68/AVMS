"""Shared business services for AVMS apps."""

from django.db import transaction
from django.db.models import F

from .models import SequenceCounter


def generate_code(module: str, venture_id: int | None, prefix: str) -> str:
    """Next sequential code for ``module`` scoped to ``venture_id``.

    Returns codes in ``PREFIX-0001`` style. Uses a single counter row per
    (module, venture) updated under a row lock so parallel creations never
    collide. Counter rows persist across record deletes, so a deleted record's
    code is never reused.
    """
    with transaction.atomic():
        counter = (
            SequenceCounter.objects.select_for_update()
            .filter(module=module, venture_id=venture_id)
            .first()
        )
        if counter is None:
            counter = SequenceCounter.objects.create(
                module=module, venture_id=venture_id, next_value=1
            )
        value = counter.next_value
        counter.next_value = F("next_value") + 1
        counter.save(update_fields=["next_value"])
    return f"{prefix}-{value:04d}"