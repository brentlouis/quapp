"""Errors in the shape MODELS.md "Errors" promises the app:

    {"error": "DEVICE_LIMIT", "message": "This phone already has 2 accounts."}

Endpoints raise ApiError; main.py turns it (and FastAPI's own validation errors) into that body.
"""

from typing import Any

from fastapi import Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse


class ApiError(Exception):
    """raise ApiError(409, "PHONE_TAKEN", "…") from anywhere in an endpoint."""

    def __init__(self, status: int, error: str, message: str, **extra: Any):
        super().__init__(message)
        self.status = status
        self.error = error
        self.message = message
        self.extra = extra


async def api_error_handler(request: Request, exc: ApiError) -> JSONResponse:
    return JSONResponse(status_code=exc.status,
                        content={"error": exc.error, "message": exc.message, **exc.extra})


async def validation_error_handler(request: Request, exc: RequestValidationError) -> JSONResponse:
    """FastAPI's 422 lists errors in its own nested format; the app gets ours instead, with
    one message per field: {"error": "INVALID_INPUT", "message": "…", "fields": {"phone": "…"}}."""
    fields: dict[str, str] = {}
    for problem in exc.errors():
        # loc is ("body", "phone") for a field in the JSON body; keep the field's name
        name = str(problem["loc"][-1]) if problem.get("loc") else "body"
        # Our own validators raise ValueError("…"); Pydantic prefixes it with "Value error, "
        fields.setdefault(name, problem["msg"].removeprefix("Value error, "))
    first = next(iter(fields.values()), "Some fields aren't valid.")
    return JSONResponse(status_code=422,
                        content={"error": "INVALID_INPUT", "message": first, "fields": fields})
