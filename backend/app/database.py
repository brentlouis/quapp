"""The connection to Postgres, and the base class every table extends."""

from collections.abc import Iterator

from sqlalchemy import MetaData, create_engine
from sqlalchemy.orm import DeclarativeBase, Session, sessionmaker

from app.config import settings

# pool_pre_ping checks a pooled connection still works before handing it out, so a Postgres
# restart doesn't make the next request fail.
engine = create_engine(settings.database_url, pool_pre_ping=True)

# expire_on_commit=False: after commit, objects keep their values, so an endpoint can still
# return the row it just saved without SQLAlchemy reloading it.
SessionLocal = sessionmaker(bind=engine, autoflush=False, expire_on_commit=False)

# Predictable names for indexes and constraints ("ck_queues_radius" instead of whatever
# Postgres invents), so error messages and tests can refer to them.
NAMING = {
    "ix": "ix_%(table_name)s_%(column_0_N_name)s",
    "uq": "uq_%(table_name)s_%(column_0_N_name)s",
    "ck": "ck_%(table_name)s_%(constraint_name)s",
    "fk": "fk_%(table_name)s_%(column_0_name)s_%(referred_table_name)s",
    "pk": "pk_%(table_name)s",
}


class Base(DeclarativeBase):
    metadata = MetaData(naming_convention=NAMING)


def get_db() -> Iterator[Session]:
    """FastAPI dependency: one session per request, always closed afterwards.

    Endpoints ask for it with `db: Session = Depends(get_db)`. Tests swap it for a session
    on the test database (tests/conftest.py).
    """
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
