from app.config import Settings


def test_a_hosts_postgres_address_is_pointed_at_psycopg_3():
    # Neon's connection string, pasted unchanged
    neon = Settings(database_url="postgresql://quapp:secret@ep-x.neon.tech/quapp?sslmode=require")
    assert neon.database_url == "postgresql+psycopg://quapp:secret@ep-x.neon.tech/quapp?sslmode=require"
    # The short spelling some hosts use
    assert Settings(database_url="postgres://u:p@h/db").database_url == "postgresql+psycopg://u:p@h/db"


def test_an_address_that_names_its_driver_is_left_alone():
    local = "postgresql+psycopg://postgres:secret@localhost:5432/quapp"
    assert Settings(database_url=local).database_url == local
