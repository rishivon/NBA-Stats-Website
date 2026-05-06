import os
import random
import time
import json
import numpy as np
import sqlite3
import asyncio
from datetime import datetime
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from nba_api.stats.endpoints import leaguestandingsv3, shotchartdetail
from nba_api.stats.static import teams as nba_static_teams
import logging

try:
    import redis.asyncio as aioredis
except ModuleNotFoundError:
    aioredis = None

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Application
app = FastAPI(title="NBA Stats Proxy", version="1.1.0")

# Environment
REDIS_URL = os.getenv("REDIS_URL", "redis://localhost:6379")
DB_PATH = os.getenv("DB_PATH", "./nba_stats.db")
redis_client = None

# Database helpers (sync operations executed via asyncio.to_thread)
def _init_db_sync():
    conn = sqlite3.connect(DB_PATH)
    conn.execute(
        """
        CREATE TABLE IF NOT EXISTS standings (
            season TEXT PRIMARY KEY,
            data TEXT NOT NULL,
            updated_at TEXT NOT NULL
        )
        """
    )
    conn.commit()
    conn.close()

def _save_standings_sync(season_label: str, data_json: str):
    conn = sqlite3.connect(DB_PATH)
    conn.execute(
        "INSERT OR REPLACE INTO standings (season, data, updated_at) VALUES (?, ?, ?)",
        (season_label, data_json, datetime.utcnow().isoformat())
    )
    conn.commit()
    conn.close()

def _get_standings_sync(season_label: str):
    conn = sqlite3.connect(DB_PATH)
    cur = conn.cursor()
    cur.execute("SELECT data, updated_at FROM standings WHERE season = ?", (season_label,))
    row = cur.fetchone()
    conn.close()
    return row if row else None

async def init_db():
    await asyncio.to_thread(_init_db_sync)

async def save_standings_to_db(season_label: str, payload: dict):
    data_json = json.dumps(payload)
    await asyncio.to_thread(_save_standings_sync, season_label, data_json)

async def get_standings_from_db(season_label: str):
    row = await asyncio.to_thread(_get_standings_sync, season_label)
    if row:
        data_json, updated_at = row
        try:
            return json.loads(data_json)
        except Exception:
            return None
    return None

async def init_redis():
    global redis_client
    if aioredis is None:
        logger.warning("redis package is not installed; continuing without Redis cache")
        redis_client = None
        return

    try:
        redis_client = aioredis.from_url(REDIS_URL, decode_responses=True)
        # test connection
        await redis_client.ping()
        logger.info("Connected to Redis")
    except Exception as e:
        logger.warning(f"Redis unavailable ({e}), continuing without cache")
        redis_client = None

@app.on_event("startup")
async def startup_event():
    await init_db()
    await init_redis()

# Add CORS middleware to allow requests from frontend
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:3000", "http://localhost:8080"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Anti-ban headers configuration for nba_api
HEADERS = {
    "User-Agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36",
    "Host": "stats.nba.com",
    "Referer": "https://www.nba.com/",
    "Accept": "application/json",
    "Accept-Language": "en-US,en;q=0.9",
    "Accept-Encoding": "gzip, deflate, br",
    "Connection": "keep-alive",
    "Sec-Fetch-Dest": "empty",
    "Sec-Fetch-Mode": "cors",
    "Sec-Fetch-Site": "same-site",
}


def add_random_delay():
    """Add random delay between 0.5 and 1.5 seconds to avoid rate limiting."""
    delay = random.uniform(0.5, 1.5)
    time.sleep(delay)


def sanitize_data(data):
    """Replace NaN and infinite values with None for JSON serialization."""
    records = []
    for record in data:
        sanitized = {}
        for key, value in record.items():
            if isinstance(value, float):
                if np.isnan(value) or np.isinf(value):
                    sanitized[key] = None
                else:
                    sanitized[key] = value
            else:
                sanitized[key] = value
        records.append(sanitized)
    return records


@app.get("/standings")
async def get_standings(season: int | None = None):
    """
    Fetch NBA standings data with caching and persistence.
    """
    try:
        season_label = None
        if season is not None:
            season_label = f"{season - 1}-{str(season)[-2:]}"
            logger.info(f"Requested standings for season {season_label}")
        else:
            season_label = "current"
            logger.info("Requested current standings")

        cache_key = f"standings:{season_label}"

        # 1) Try Redis cache
        if redis_client:
            try:
                cached = await redis_client.get(cache_key)
                if cached:
                    logger.info("Returning standings from Redis cache")
                    return json.loads(cached)
            except Exception as e:
                logger.warning(f"Redis get failed: {e}")

        # 2) Try persistent DB
        db_result = await get_standings_from_db(season_label)
        if db_result:
            logger.info("Returning standings from DB")
            # prime redis cache
            if redis_client:
                try:
                    await redis_client.set(cache_key, json.dumps(db_result), ex=600)
                except Exception:
                    pass
            return db_result

        # 3) Fallback to NBA API
        add_random_delay()
        logger.info("Fetching standings from NBA API")

        standings_request = leaguestandingsv3.LeagueStandingsV3(
            season=(None if season_label == 'current' else season_label),
            headers=HEADERS
        ) if season is not None else leaguestandingsv3.LeagueStandingsV3(headers=HEADERS)

        standings = standings_request.get_data_frames()[0]
        standings_list = standings.to_dict(orient='records')
        standings_list = sanitize_data(standings_list)

        response = {
            "data": standings_list,
            "count": len(standings_list),
            "status": "success",
            "fetched_at": datetime.utcnow().isoformat()
        }

        # save to DB and Redis (best-effort)
        try:
            await save_standings_to_db(season_label, response)
        except Exception as e:
            logger.warning(f"Failed to save standings to DB: {e}")

        if redis_client:
            try:
                await redis_client.set(cache_key, json.dumps(response), ex=600)
            except Exception as e:
                logger.warning(f"Failed to set Redis cache: {e}")

        logger.info(f"Successfully fetched {len(standings_list)} standing records from NBA API")
        return response

    except Exception as e:
        logger.error(f"Error fetching standings: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Failed to fetch standings: {str(e)}")


@app.get("/teams")
async def get_teams():
    """
    Return stable NBA team metadata for backend persistence seeding.
    """
    try:
        cache_key = "teams:metadata"

        if redis_client:
            try:
                cached = await redis_client.get(cache_key)
                if cached:
                    logger.info("Returning teams from Redis cache")
                    return json.loads(cached)
            except Exception as e:
                logger.warning(f"Redis get failed: {e}")

        add_random_delay()
        all_teams = nba_static_teams.get_teams()
        response = [
            {
                "id": team.get("id"),
                "abbreviation": team.get("abbreviation"),
                "city": team.get("city"),
                "conference": team.get("conference"),
                "division": team.get("division"),
                "fullName": team.get("full_name"),
                "name": team.get("nickname"),
            }
            for team in all_teams
        ]

        if redis_client:
            try:
                await redis_client.set(cache_key, json.dumps(response), ex=2592000)
            except Exception as e:
                logger.warning(f"Failed to set teams Redis cache: {e}")

        return response
    except Exception as e:
        logger.error(f"Error fetching teams: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Failed to fetch teams: {str(e)}")


@app.get("/player-shots/{player_id}")
async def get_player_shots(player_id: int):
    """
    Fetch shot chart details for a specific player from nba_api.
    
    Args:
        player_id: NBA player ID
        
    Returns:
        dict: Shot chart data in JSON format
    """
    try:
        add_random_delay()
        logger.info(f"Fetching shot chart for player {player_id}")
        
        # Fetch shot chart using nba_api with custom headers
        shots = shotchartdetail.ShotChartDetail(
            team_id=0,
            player_id=player_id,
            headers=HEADERS
        ).get_data_frames()[0]
        
        # Convert DataFrame to list of dicts for JSON serialization
        shots_list = shots.to_dict(orient='records')
        
        # Sanitize data
        shots_list = sanitize_data(shots_list)
        
        logger.info(f"Successfully fetched {len(shots_list)} shot records for player {player_id}")
        
        return {
            "data": shots_list,
            "count": len(shots_list),
            "player_id": player_id,
            "status": "success"
        }
    except Exception as e:
        logger.error(f"Error fetching shots for player {player_id}: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Failed to fetch shots: {str(e)}")


@app.get("/health")
async def health_check():
    """Health check endpoint."""
    return {"status": "healthy", "service": "NBA Stats Proxy"}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
