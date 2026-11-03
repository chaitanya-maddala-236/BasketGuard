from __future__ import annotations

import os
from pathlib import Path
from dotenv import load_dotenv

ROOT = Path(__file__).resolve().parents[1]
load_dotenv(ROOT.parent / ".env")
DATABASE_URL = os.getenv("DATABASE_URL", f"sqlite:///{ROOT / 'basketguard.db'}")
JWT_SECRET = os.getenv("JWT_SECRET", "basketguard-local-development-secret-change-me")
ACCESS_TOKEN_MINUTES = int(os.getenv("ACCESS_TOKEN_MINUTES", "60"))
JWT_ALGORITHM = "HS256"
