from django.contrib.auth import get_user_model

from .models import Notification

User = get_user_model()


def notify(user, type_: str, message: str, link: str = "") -> Notification:
    """Create a notification for one user."""
    return Notification.objects.create(
        user=user, type=type_, message=message[:255], link=link
    )


def notify_venture_users(venture, type_: str, message: str, perm_code: str = "") -> int:
    """Notify every user who can act on the venture: superusers plus anyone
    holding ``perm_code`` (all lookups if ``perm_code`` is empty)."""
    created = 0
    users = User.objects.filter(is_active=True)
    for user in users:
        if user.is_superuser or not perm_code or user.has_permission_code(perm_code):
            notify(user, type_, message)
            created += 1
    return created