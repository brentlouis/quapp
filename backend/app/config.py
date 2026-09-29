"""The server's settings, read from backend/.env (and from real environment variables,
which win over the file). Anything secret or machine-specific lives here, never in code."""

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    # e.g. postgresql+psycopg://postgres:secret@localhost:5432/quapp
    database_url: str

    # .env is found relative to where uvicorn is started: run it from backend/.
    # extra="ignore" lets .env hold settings later steps add before this class knows them.
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")


# One shared instance. A missing DATABASE_URL fails here, at startup, with a clear error,
# instead of later on the first request.
settings = Settings()
