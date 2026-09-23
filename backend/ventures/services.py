"""Business logic for ventures.

Also hosts the venture-scoping helpers that later phases use to isolate every
venture-scoped queryset::

    from ventures.services import scope_queryset_by_venture
    qs = scope_queryset_by_venture(Customer.objects.all(), request)
"""

from django.db.models import Q

from .models import Venture


def generate_venture_code() -> str:
    """Next sequential code for a new venture (``V-0001`` style)."""
    return Venture.objects.generate_code()


def get_request_venture(request) -> Venture | None:
    """Resolve the selected venture from the ``X-Venture-Id`` header.

    Returns ``None`` when the header is absent or names an unknown venture,
    in which case callers should leave data unscoped (or require explicit
    selection, depending on the module).
    """
    header = request.headers.get("X-Venture-Id", "").strip()
    if not header:
        return None
    try:
        return Venture.objects.get(id=header)
    except (Venture.DoesNotExist, ValueError):
        return None


def scope_queryset_by_venture(queryset, request):
    """Filter a venture-scoped queryset down to the selected venture."""
    venture = get_request_venture(request)
    if venture is not None:
        queryset = queryset.filter(venture=venture)
    return queryset


def search_ventures(queryset, term: str):
    """Apply the venture keyword search used by the list endpoint."""
    return queryset.filter(
        Q(venture_code__icontains=term)
        | Q(venture_name__icontains=term)
        | Q(city__icontains=term)
    )