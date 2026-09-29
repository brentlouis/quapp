"""The Quapp API. Start it from backend/:

    uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload

--host 0.0.0.0 listens on every network interface, so a phone can reach it, not just this PC.
--reload restarts on every saved change; leave it off for the demo.
"""

from collections.abc import AsyncIterator
from contextlib import asynccontextmanager

from fastapi import Depends, FastAPI
from fastapi.exceptions import RequestValidationError
from sqlalchemy import text
from sqlalchemy.orm import Session

from app import models  # noqa: F401  imported so every table is registered on Base
from app.database import Base, engine, get_db
from app.errors import ApiError, api_error_handler, validation_error_handler
from app.routers import auth, queues, tickets


@asynccontextmanager
async def lifespan(app: FastAPI) -> AsyncIterator[None]:
    # Creates any missing tables at startup. It never changes a table that already exists:
    # after a schema change, drop the table (or switch to Alembic) — BACKEND.md step 1.3.
    Base.metadata.create_all(engine)
    yield


app = FastAPI(title="Quapp API", lifespan=lifespan)

# Every error goes out in the MODELS.md "Errors" shape
app.add_exception_handler(ApiError, api_error_handler)
app.add_exception_handler(RequestValidationError, validation_error_handler)

app.include_router(auth.router)
app.include_router(queues.router)
app.include_router(tickets.router)


@app.get("/health")
def health(db: Session = Depends(get_db)) -> dict:
    """Is the server up, and can it reach the database? The first thing to open from the phone."""
    db.execute(text("SELECT 1"))
    return {"status": "ok"}
