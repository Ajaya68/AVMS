"""Lightweight audit logging helper.

Call from anywhere that needs to record an event::

    from audit.services import log_audit
    log_audit(request, AuditLog.ACTION_USER_CREATED, module="auth",
              object_type="User", object_id=user.id,
              description="Created user")
"""

from rest_framework.request import Request

from .models import AuditLog


def _client_ip(request) -> str | None:
    if not request:
        return None
    if isinstance(request, Request):
        request = request._request  # noqa: SLF001 - DRF wrapper -> Django request
    forwarded = request.META.get("HTTP_X_FORWARDED_FOR")
    if forwarded:
        return forwarded.split(",")[0].strip() or None
    return request.META.get("REMOTE_ADDR") or None


def log_audit(
    request=None,
    *,
    action: str,
    module: str,
    object_type: str = "",
    object_id=None,
    description: str = "",
    user=None,
):
    entry = AuditLog(
        action=action,
        module=module,
        object_type=object_type,
        object_id=str(object_id) if object_id is not None else "",
        description=description,
        ip_address=_client_ip(request),
    )
    if user is not None:
        entry.user = user
    elif request is not None:
        request_user = getattr(request, "user", None)
        if request_user is not None and getattr(request_user, "is_authenticated", False):
            entry.user = request_user
    entry.save()
    return entry