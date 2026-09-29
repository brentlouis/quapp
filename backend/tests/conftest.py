"""Test setup: every test runs against the quapp_test database, never the real one, and
everything a test writes is rolled back when it ends, so each test starts from empty tables.

How the rollback works: each test gets one connection with a transaction open on it. The
session the test (and the app, through get_db) uses sits inside that transaction, and its own
commits only commit a savepoint. At the end the outer transaction is rolled back, and every
row the test made is gone.
"""

from collections.abc import Iterator
from datetime import date, datetime, time

import pytest
from fastapi.testclient import TestClient
from sqlalchemy import create_engine
from sqlalchemy.engine import make_url
from sqlalchemy.orm import Session

from app import models, timeutil  # noqa: F401  models registers the tables on Base
from app.config import settings
from app.database import Base, get_db
from app.main import app

# The same server and login as .env, but the database with "_test" on the end
TEST_DATABASE_URL = make_url(settings.database_url).set(
    database=f"{make_url(settings.database_url).database}_test")


@pytest.fixture(scope="session")
def engine():
    """Fresh tables once per test run, so the test schema always matches models.py."""
    engine = create_engine(TEST_DATABASE_URL)
    Base.metadata.drop_all(engine)
    Base.metadata.create_all(engine)
    yield engine
    engine.dispose()


@pytest.fixture
def db(engine) -> Iterator[Session]:
    connection = engine.connect()
    outer = connection.begin()
    # create_savepoint: session.commit() and session.rollback() act on a savepoint inside
    # `outer`, so a test (or an endpoint) can commit, or recover from an IntegrityError,
    # and still be undone at the end.
    session = Session(bind=connection, join_transaction_mode="create_savepoint",
                      expire_on_commit=False)
    yield session
    session.close()
    outer.rollback()
    connection.close()


@pytest.fixture
def client(db: Session) -> Iterator[TestClient]:
    """The API, with every request using the test's session."""
    app.dependency_overrides[get_db] = lambda: db
    # Not `with TestClient(app)`: that would run the app's startup, which creates tables on
    # the real database. The test engine has already made the test tables.
    yield TestClient(app)
    app.dependency_overrides.clear()


class Clock:
    """The time as the server sees it during a test. Starts at 10:00 AM Manila on a fixed
    day; `clock.set(…)` moves it. Code reads time through app.timeutil, so replacing
    timeutil.now and timeutil.today is enough."""

    DAY = date(2026, 9, 29)

    def __init__(self):
        self.moment = datetime.combine(self.DAY, time(10, 0), timeutil.MANILA)

    def set(self, moment: datetime | time) -> None:
        """A full moment, or just a time of day on the test's day."""
        if isinstance(moment, time):
            moment = datetime.combine(self.moment.astimezone(timeutil.MANILA).date(), moment,
                                      timeutil.MANILA)
        self.moment = moment

    def now(self) -> datetime:
        return self.moment

    def today(self) -> date:
        return self.moment.astimezone(timeutil.MANILA).date()


@pytest.fixture(autouse=True)
def clock(monkeypatch) -> Clock:
    """Every test runs on the fixed clock, so results don't depend on when pytest runs."""
    fixed = Clock()
    monkeypatch.setattr(timeutil, "now", fixed.now)
    monkeypatch.setattr(timeutil, "today", fixed.today)
    return fixed
