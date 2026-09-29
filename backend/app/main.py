"""The Quapp API. Start it from backend/:

    uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload

--host 0.0.0.0 listens on every network interface, so a phone can reach it, not just this PC.
--reload restarts on every saved change; leave it off for the demo.
"""

from fastapi import FastAPI

from app.config import settings  # noqa: F401  imported so a bad .env fails at startup

app = FastAPI(title="Quapp API")


@app.get("/health")
def health() -> dict:
    """Is the server up? The first thing to open from the phone."""
    return {"status": "ok"}
