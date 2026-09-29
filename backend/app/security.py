"""Passwords and tokens."""

import secrets

import bcrypt

# bcrypt only reads the first 72 bytes of a password, and bcrypt 5 refuses longer ones
# outright. RegisterIn keeps passwords within this, so nothing past it is silently ignored.
MAX_PASSWORD_BYTES = 72


def hash_password(password: str) -> str:
    """bcrypt with a fresh random salt; the salt is stored inside the hash itself."""
    return bcrypt.hashpw(password.encode(), bcrypt.gensalt()).decode()


def check_password(password: str, password_hash: str) -> bool:
    if len(password.encode()) > MAX_PASSWORD_BYTES:
        return False  # never a valid password here, and bcrypt would raise on it
    return bcrypt.checkpw(password.encode(), password_hash.encode())


def new_token() -> str:
    """A random, unguessable token: 32 bytes from the OS's secure random source."""
    return secrets.token_urlsafe(32)
