"""Role-based permission classes.

Authorization is always enforced here, in the backend. Views declare the
capability they require::

    class CustomerListCreateView(APIView):
        permission_classes = [IsAuthenticated, HasPermission("customers.view")]

or set ``required_permission`` on the view with ``HasPermissionFromView``.
Superusers bypass every check.
"""

from rest_framework.permissions import BasePermission


class HasPermission(BasePermission):
    """Requires a specific capability code, e.g. ``HasPermission("users.view")``."""

    message = "You do not have permission to perform this action."

    def __init__(self, code: str | None = None):
        self.code = code

    def has_permission(self, request, view):
        if not request.user or not request.user.is_authenticated:
            return False
        code = self.code
        if not code:
            return True
        return request.user.has_permission_code(code)


class HasPermissionFromView(BasePermission):
    """Reads ``view.required_permission`` to avoid repeating the class call."""

    message = "You do not have permission to perform this action."

    def has_permission(self, request, view):
        if not request.user or not request.user.is_authenticated:
            return False
        code = getattr(view, "required_permission", None)
        if not code:
            return True
        return request.user.has_permission_code(code)


class IsAdmin(BasePermission):
    """Only superusers / ADMIN role holders."""

    message = "Administrator access required."

    def has_permission(self, request, view):
        if not request.user or not request.user.is_authenticated:
            return False
        return request.user.is_superuser or request.user.has_permission_code(
            "users.manage"
        )