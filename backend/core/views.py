"""Health check endpoints.

The plain health endpoint never touches the database, so it also works
while the database server is starting. The db variant verifies the
database connection and reports 503 when it is unreachable.
"""

from django.db import connection
from rest_framework.views import APIView

from core.responses import failure, success


class HealthView(APIView):
    authentication_classes = []
    permission_classes = []
    throttle_classes = []

    def get(self, request):
        try:
            connection.ensure_connection()
            db_status = "ok"
        except Exception:  # noqa: BLE001 - deliberately surface only status
            db_status = "unavailable"

        payload = {
            "status": "ok" if db_status == "ok" else "degraded",
            "service": "avms-backend",
            "version": "0.1.0",
            "database": db_status,
        }
        if db_status == "ok":
            return success(payload, message="Service healthy")
        return failure(
            message="Service running but database is unavailable",
            errors={"database": "Connection failed"},
            status=503,
        )