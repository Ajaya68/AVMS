"""Shared API response helpers.

Consistent success/error envelope used across all AVMS endpoints::

    GET /api/products/  ->  {"success": true, "data": ..., "message": "..."}
    failure             ->  {"success": false, "message": "...", "errors": {}}
"""

from rest_framework.response import Response


def success(data=None, message: str = "Operation successful", status: int = 200):
    return Response(
        {"success": True, "data": data, "message": message},
        status=status,
    )


def failure(message: str = "Unable to complete operation", errors=None, status: int = 400):
    return Response(
        {"success": False, "message": message, "errors": errors or {}},
        status=status,
    )