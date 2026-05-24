import os
import random
import re
import time
import json
import html
import numpy as np
import sqlite3
import asyncio
from datetime import datetime
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from nba_api.stats.endpoints import commonteamroster, leaguedashplayerstats, leaguedashteamstats, leaguegamefinder, leaguestandingsv3, shotchartdetail, teamgamelog
from nba_api.stats.static import teams as nba_static_teams
import logging

try:
    import redis.asyncio as aioredis
except ModuleNotFoundError:
    aioredis = None

try:
    import requests
    from bs4 import BeautifulSoup
except ModuleNotFoundError:
    requests = None
    BeautifulSoup = None

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

ESPN_HTML_HEADERS = {
    "User-Agent": "curl/8.7.1",
    "Accept": "*/*",
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


def season_label_from_end_year(season: int | None):
    if season is None:
        today = datetime.utcnow()
        season = today.year + 1 if today.month >= 10 else today.year
    return f"{season - 1}-{str(season)[-2:]}"


def get_team_by_id(team_id: int):
    return next((team for team in nba_static_teams.get_teams() if team.get("id") == team_id), None)


DEPTH_POSITIONS = ["PG", "SG", "SF", "PF", "C"]


ESPN_SLUG_OVERRIDES = {
    "LAC": "la-clippers",
}

ESPN_ABBREV_OVERRIDES = {
    "GSW": "gs",
    "NOP": "no",
    "NYK": "ny",
    "SAS": "sa",
    "UTA": "utah",
    "WAS": "wsh",
}


def espn_team_code(team):
    return ESPN_ABBREV_OVERRIDES.get(team.get("abbreviation"), team.get("abbreviation", "").lower())


def espn_team_slug(team):
    override = ESPN_SLUG_OVERRIDES.get(team.get("abbreviation"))
    if override:
        return override
    full_name = team.get("full_name") or f"{team.get('city', '')} {team.get('nickname', '')}"
    return re.sub(r"[^a-z0-9]+", "-", full_name.lower()).strip("-")


def extract_json_value(source: str, marker: str):
    marker_index = source.find(marker)
    if marker_index < 0:
        return None

    start = source.find("[", marker_index)
    opening = "["
    closing = "]"
    if start < 0:
        start = source.find("{", marker_index)
        opening = "{"
        closing = "}"
    if start < 0:
        return None

    depth = 0
    in_string = False
    escaped = False
    for index in range(start, len(source)):
        char = source[index]
        if in_string:
            if escaped:
                escaped = False
            elif char == "\\":
                escaped = True
            elif char == '"':
                in_string = False
            continue
        if char == '"':
            in_string = True
        elif char == opening:
            depth += 1
        elif char == closing:
            depth -= 1
            if depth == 0:
                return json.loads(html.unescape(source[start:index + 1]))
    return None


def extract_espn_json_array(soup, marker: str):
    for script in soup.find_all("script"):
        content = script.string or script.get_text()
        if marker not in content:
            continue
        try:
            return extract_json_value(content, marker)
        except Exception as e:
            logger.warning(f"Failed to parse ESPN embedded JSON for {marker}: {e}")
    return None


def format_salary(value):
    if value is None or value == 0:
        return None
    if isinstance(value, str):
        return value
    try:
        return f"${int(value):,}"
    except Exception:
        return str(value)


def clean_cbs_player_name(value: str | None):
    cleaned = clean_player_name(value)
    if not cleaned:
        return None
    compact_match = re.match(r"^[A-Z]\.\s+\S+\s+(.+\s+\S+)$", cleaned)
    return compact_match.group(1) if compact_match else cleaned


def clean_player_name(value: str | None):
    if not value:
        return None
    cleaned = re.sub(r"\s+", " ", value).strip()
    cleaned = re.sub(r"\b(Q|O|DD|D|IR|DTD|GTD|OUT)\b$", "", cleaned).strip()
    cleaned = re.sub(r"\s+(Questionable|Out|Day-To-Day|Injured Reserve)$", "", cleaned, flags=re.IGNORECASE).strip()
    return cleaned or None


def extract_player_status(value: str | None):
    if not value:
        return None
    status_match = re.search(r"\b(DD|DTD|GTD|Q|O|OUT|IR)\b$", value.strip(), re.IGNORECASE)
    if not status_match:
        return None
    status = status_match.group(1).upper()
    return {"DTD": "DD", "GTD": "Q", "OUT": "O"}.get(status, status)


def normalize_depth_position(value: str | None):
    if not value:
        return None
    normalized = value.strip().upper()
    return normalized if normalized in DEPTH_POSITIONS else None


def parse_depth_chart_rows(soup, team_id: int):
    groups = extract_espn_json_array(soup, '"dethTeamGroups":')
    if groups:
        depth_chart = []
        for group in groups:
            for row in group.get("rows", []):
                if not row:
                    continue
                position = normalize_depth_position(row[0])
                if not position:
                    continue
                for depth_index, player in enumerate(row[1:6], start=1):
                    if not isinstance(player, dict):
                        continue
                    player_name = player.get("displayName") or player.get("name")
                    if not player_name:
                        continue
                    injuries = player.get("injuries") or []
                    depth_chart.append({
                        "teamId": team_id,
                        "position": position,
                        "depthOrder": depth_index,
                        "playerName": player_name,
                        "status": ", ".join(injuries) if injuries else None,
                    })
        if depth_chart:
            return depth_chart

    depth_chart = []
    for table in soup.select("table"):
        headers = [cell.get_text(" ", strip=True).upper() for cell in table.select("thead th")]
        position_headers = [normalize_depth_position(header) for header in headers]
        position_headers = [position for position in position_headers if position]

        body_rows = table.select("tbody tr")
        if position_headers:
            for depth_index, row in enumerate(body_rows, start=1):
                cells = [cell.get_text(" ", strip=True) for cell in row.select("td")]
                for index, position in enumerate(position_headers):
                    if index < len(cells):
                        player_name = clean_player_name(cells[index])
                        if player_name and player_name.upper() not in DEPTH_POSITIONS:
                            depth_chart.append({
                                "teamId": team_id,
                                "position": position,
                                "depthOrder": depth_index,
                                "playerName": player_name,
                                "status": extract_player_status(cells[index]),
                            })

        for row in body_rows:
            cells = [cell.get_text(" ", strip=True) for cell in row.select("td, th")]
            if len(cells) < 2:
                continue
            position = normalize_depth_position(cells[0])
            if not position:
                continue
            for depth_index, player in enumerate(cells[1:], start=1):
                player_name = clean_player_name(player)
                if player_name:
                    depth_chart.append({
                        "teamId": team_id,
                        "position": position,
                        "depthOrder": depth_index,
                        "playerName": player_name,
                        "status": extract_player_status(player),
                    })

    deduped = {}
    for item in depth_chart:
        key = (item["position"], item["depthOrder"])
        deduped.setdefault(key, item)
    return list(deduped.values())


def parse_espn_roster(soup):
    athletes = extract_espn_json_array(soup, '"athletes":')
    if not athletes:
        return {}
    roster = {}
    for athlete in athletes:
        name = athlete.get("displayName") or athlete.get("name")
        if not name:
            continue
        roster[clean_player_name(name).lower()] = {
            "salary": format_salary(athlete.get("salary")),
            "age": athlete.get("age"),
            "headshot": athlete.get("headshot"),
        }
    return roster


def team_name_candidates(team):
    values = {
        team.get("city"),
        team.get("nickname"),
        team.get("full_name"),
        f"{team.get('city', '')} {team.get('nickname', '')}".strip(),
    }
    abbreviation = team.get("abbreviation")
    if abbreviation == "LAC":
        values.add("LA Clippers")
    if abbreviation == "NYK":
        values.add("New York")
    if abbreviation == "BKN":
        values.add("Brooklyn")
    return {value.lower() for value in values if value}


def cbs_team_slug(team):
    return re.sub(r"[^a-z0-9]+", "-", (team.get("full_name") or "").lower()).strip("-")


def parse_cbs_injuries(soup, team_id: int, team):
    candidates = team_name_candidates(team)
    injuries = []

    for row in soup.select("tbody tr"):
        cells = [cell.get_text(" ", strip=True) for cell in row.select("td")]
        if len(cells) >= 5 and cells[0].lower() != "player":
            injuries.append({
                "teamId": team_id,
                "playerName": clean_cbs_player_name(cells[0]),
                "position": cells[1],
                "injury": cells[3],
                "expectedReturn": cells[4],
                "status": cells[4],
            })

    if injuries:
        return [item for item in injuries if item.get("playerName")]

    for heading in soup.select("h1, h2, h3, h4, h5"):
        heading_text = heading.get_text(" ", strip=True)
        if not heading_text or heading_text.lower() not in candidates:
            continue

        table = heading.find_next("table")
        if table is None:
            continue

        for row in table.select("tbody tr"):
            cells = [cell.get_text(" ", strip=True) for cell in row.select("td")]
            if len(cells) < 4:
                continue
            injuries.append({
                "teamId": team_id,
                "playerName": clean_cbs_player_name(cells[0]),
                "position": cells[1] if len(cells) > 1 else None,
                "injury": cells[3] if len(cells) > 3 else None,
                "expectedReturn": cells[4] if len(cells) > 4 else None,
                "status": cells[4] if len(cells) > 4 else None,
            })

    return [item for item in injuries if item.get("playerName")]


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


@app.get("/team-roster/{team_id}")
async def get_team_roster(team_id: int, season: int | None = None):
    try:
        season_label = season_label_from_end_year(season)
        cache_key = f"team_roster:v2:{team_id}:{season_label}"

        if redis_client:
            cached = await redis_client.get(cache_key)
            if cached:
                return json.loads(cached)

        add_random_delay()
        roster = commonteamroster.CommonTeamRoster(
            team_id=team_id,
            season=season_label,
            headers=HEADERS
        ).get_data_frames()[0]
        records = sanitize_data(roster.to_dict(orient="records"))
        salary_by_name = {}
        team = get_team_by_id(team_id)
        if team and requests is not None and BeautifulSoup is not None:
            try:
                roster_url = f"https://www.espn.com/nba/team/roster/_/name/{espn_team_code(team)}"
                roster_response = requests.get(roster_url, headers=ESPN_HTML_HEADERS, timeout=8)
                roster_response.raise_for_status()
                salary_by_name = parse_espn_roster(BeautifulSoup(roster_response.text, "html.parser"))
            except Exception as e:
                logger.warning(f"Failed to augment roster salaries for team {team_id}: {e}")

        players = [
            {
                "playerId": row.get("PLAYER_ID"),
                "teamId": team_id,
                "season": season,
                "fullName": row.get("PLAYER"),
                "firstName": (row.get("PLAYER") or "").split(" ")[0] if row.get("PLAYER") else None,
                "lastName": " ".join((row.get("PLAYER") or "").split(" ")[1:]) if row.get("PLAYER") else None,
                "position": row.get("POSITION"),
                "jersey": row.get("NUM"),
                "height": row.get("HEIGHT"),
                "weight": str(row.get("WEIGHT")) if row.get("WEIGHT") is not None else None,
                "salary": salary_by_name.get(clean_player_name(row.get("PLAYER")).lower(), {}).get("salary") if row.get("PLAYER") else None,
            }
            for row in records
        ]
        response = {"data": players, "count": len(players), "status": "success", "season": season_label}
        if redis_client:
            await redis_client.set(cache_key, json.dumps(response), ex=43200)
        return response
    except Exception as e:
        logger.error(f"Error fetching roster for team {team_id}: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Failed to fetch roster: {str(e)}")


@app.get("/team-stats/{team_id}")
async def get_team_stats(team_id: int, season: int | None = None):
    try:
        season_label = season_label_from_end_year(season)
        cache_key = f"team_stats:{team_id}:{season_label}"

        if redis_client:
            cached = await redis_client.get(cache_key)
            if cached:
                return json.loads(cached)

        add_random_delay()
        stats = leaguedashteamstats.LeagueDashTeamStats(
            season=season_label,
            team_id_nullable=str(team_id),
            per_mode_detailed="PerGame",
            rank="Y",
            headers=HEADERS
        ).get_data_frames()[0]
        records = sanitize_data(stats.to_dict(orient="records"))
        row = records[0] if records else {}
        response = {
            "data": {
                "teamId": team_id,
                "season": season,
                "pts": row.get("PTS"),
                "reb": row.get("REB"),
                "ast": row.get("AST"),
                "stl": row.get("STL"),
                "blk": row.get("BLK"),
                "plusMinus": row.get("PLUS_MINUS"),
                "ptsRank": row.get("PTS_RANK"),
                "rebRank": row.get("REB_RANK"),
                "astRank": row.get("AST_RANK"),
                "stlRank": row.get("STL_RANK"),
                "blkRank": row.get("BLK_RANK"),
                "plusMinusRank": row.get("PLUS_MINUS_RANK"),
            },
            "status": "success",
            "season": season_label
        }
        if redis_client:
            await redis_client.set(cache_key, json.dumps(response), ex=43200)
        return response
    except Exception as e:
        logger.error(f"Error fetching team stats for team {team_id}: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Failed to fetch team stats: {str(e)}")


@app.get("/league-team-stats")
async def get_league_team_stats(season: int | None = None):
    try:
        season_label = season_label_from_end_year(season)
        cache_key = f"league_team_stats:{season_label}"

        if redis_client:
            cached = await redis_client.get(cache_key)
            if cached:
                return json.loads(cached)

        add_random_delay()
        stats = leaguedashteamstats.LeagueDashTeamStats(
            season=season_label,
            per_mode_detailed="PerGame",
            rank="Y",
            headers=HEADERS
        ).get_data_frames()[0]
        records = sanitize_data(stats.to_dict(orient="records"))
        teams = [
            {
                "teamId": row.get("TEAM_ID"),
                "season": season,
                "pts": row.get("PTS"),
                "reb": row.get("REB"),
                "ast": row.get("AST"),
                "stl": row.get("STL"),
                "blk": row.get("BLK"),
                "plusMinus": row.get("PLUS_MINUS"),
                "ptsRank": row.get("PTS_RANK"),
                "rebRank": row.get("REB_RANK"),
                "astRank": row.get("AST_RANK"),
                "stlRank": row.get("STL_RANK"),
                "blkRank": row.get("BLK_RANK"),
                "plusMinusRank": row.get("PLUS_MINUS_RANK"),
            }
            for row in records
        ]
        response = {"data": teams, "count": len(teams), "status": "success", "season": season_label}
        if redis_client:
            await redis_client.set(cache_key, json.dumps(response), ex=43200)
        return response
    except Exception as e:
        logger.error(f"Error fetching league team stats: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Failed to fetch league team stats: {str(e)}")


@app.get("/team-player-stats/{team_id}")
async def get_team_player_stats(team_id: int, season: int | None = None):
    try:
        season_label = season_label_from_end_year(season)
        cache_key = f"team_player_stats:{team_id}:{season_label}"

        if redis_client:
            cached = await redis_client.get(cache_key)
            if cached:
                return json.loads(cached)

        add_random_delay()
        stats = leaguedashplayerstats.LeagueDashPlayerStats(
            season=season_label,
            team_id_nullable=str(team_id),
            per_mode_detailed="PerGame",
            rank="N",
            headers=HEADERS
        ).get_data_frames()[0]
        records = sanitize_data(stats.to_dict(orient="records"))
        players = [
            {
                "playerId": row.get("PLAYER_ID"),
                "teamId": row.get("TEAM_ID") or team_id,
                "season": season,
                "playerName": row.get("PLAYER_NAME"),
                "pts": row.get("PTS"),
                "reb": row.get("REB"),
                "ast": row.get("AST"),
                "stl": row.get("STL"),
                "blk": row.get("BLK"),
                "plusMinus": row.get("PLUS_MINUS"),
            }
            for row in records
        ]
        response = {"data": players, "count": len(players), "status": "success", "season": season_label}
        if redis_client:
            await redis_client.set(cache_key, json.dumps(response), ex=43200)
        return response
    except Exception as e:
        logger.error(f"Error fetching player stats for team {team_id}: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Failed to fetch player stats: {str(e)}")


@app.get("/team-game-log/{team_id}")
async def get_team_game_log(team_id: int, season: int | None = None):
    try:
        season_label = season_label_from_end_year(season)
        cache_key = f"team_game_log:v2:{team_id}:{season_label}"

        if redis_client:
            cached = await redis_client.get(cache_key)
            if cached:
                return json.loads(cached)

        add_random_delay()
        game_log = teamgamelog.TeamGameLog(
            team_id=team_id,
            season=season_label,
            season_type_all_star="Regular Season",
            headers=HEADERS
        ).get_data_frames()[0]
        finder = leaguegamefinder.LeagueGameFinder(
            team_id_nullable=team_id,
            season_nullable=season_label,
            season_type_nullable="Regular Season",
            headers=HEADERS
        ).get_data_frames()[0]
        plus_minus_by_game = {
            row.get("GAME_ID"): row.get("PLUS_MINUS")
            for row in sanitize_data(finder.to_dict(orient="records"))
        }
        records = sanitize_data(game_log.to_dict(orient="records"))
        for record in records:
            record["PLUS_MINUS"] = plus_minus_by_game.get(record.get("Game_ID") or record.get("GAME_ID"))
        response = {"data": records, "count": len(records), "status": "success", "season": season_label}
        if redis_client:
            await redis_client.set(cache_key, json.dumps(response), ex=43200)
        return response
    except Exception as e:
        logger.error(f"Error fetching game log for team {team_id}: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Failed to fetch game log: {str(e)}")


@app.get("/team-injuries/{team_id}")
async def get_team_injuries(team_id: int):
    try:
        cache_key = f"team_injuries:v2:{team_id}"
        if redis_client:
            cached = await redis_client.get(cache_key)
            if cached:
                return json.loads(cached)

        if requests is None or BeautifulSoup is None:
            logger.warning("Injury scraper dependencies are unavailable; returning empty injury report")
            return {"data": [], "count": 0, "status": "degraded"}

        team = get_team_by_id(team_id)
        if not team:
            return {"data": [], "count": 0, "status": "not_found"}

        try:
            logger.info(f"Fetching injury report from CBS Sports for team {team_id}")
            cbs_url = f"https://www.cbssports.com/nba/teams/{team.get('abbreviation')}/{cbs_team_slug(team)}/injuries/"
            cbs_response = requests.get(
                cbs_url,
                headers={"User-Agent": HEADERS["User-Agent"]},
                timeout=8
            )
            cbs_response.raise_for_status()
            cbs_soup = BeautifulSoup(cbs_response.text, "html.parser")
            cbs_injuries = parse_cbs_injuries(cbs_soup, team_id, team)
            if cbs_injuries:
                payload = {"data": cbs_injuries, "count": len(cbs_injuries), "status": "success", "source": "cbs"}
                if redis_client:
                    await redis_client.set(cache_key, json.dumps(payload), ex=14400)
                return payload
        except Exception as e:
            logger.warning(f"CBS injury scraper failed for team {team_id}: {str(e)}")

        slug = espn_team_slug(team)
        if not slug:
            return {"data": [], "count": 0, "status": "not_supported"}

        url = f"https://www.espn.com/nba/team/injuries/_/name/{espn_team_code(team)}/{slug}"
        logger.info(f"Fetching injury report from ESPN for team {team_id}")
        response = requests.get(url, headers=ESPN_HTML_HEADERS, timeout=8)
        response.raise_for_status()

        soup = BeautifulSoup(response.text, "html.parser")
        injuries = []
        for row in soup.select("tr"):
            cells = [cell.get_text(" ", strip=True) for cell in row.select("td")]
            if len(cells) >= 3 and cells[0] and cells[0].lower() != "player":
                injuries.append({
                    "teamId": team_id,
                    "playerName": cells[0],
                    "injury": cells[1],
                    "expectedReturn": cells[2],
                    "status": cells[3] if len(cells) > 3 else cells[2],
                })

        payload = {"data": injuries, "count": len(injuries), "status": "success"}
        if redis_client:
            await redis_client.set(cache_key, json.dumps(payload), ex=14400)
        return payload
    except Exception as e:
        logger.warning(f"Injury scraper failed for team {team_id}: {str(e)}")
        return {"data": [], "count": 0, "status": "scrape_failed", "message": str(e)}


@app.get("/team-depth-chart/{team_id}")
async def get_team_depth_chart(team_id: int):
    try:
        cache_key = f"team_depth_chart:v2:{team_id}"
        if redis_client:
            cached = await redis_client.get(cache_key)
            if cached:
                return json.loads(cached)

        if requests is None or BeautifulSoup is None:
            logger.warning("Depth chart scraper dependencies are unavailable; returning empty depth chart")
            return {"data": [], "count": 0, "status": "degraded"}

        team = get_team_by_id(team_id)
        if not team:
            return {"data": [], "count": 0, "status": "not_found"}

        slug = espn_team_slug(team)
        if not slug:
            return {"data": [], "count": 0, "status": "not_supported"}

        url = f"https://www.espn.com/nba/team/depth/_/name/{espn_team_code(team)}"
        logger.info(f"Fetching depth chart from ESPN for team {team_id}")
        response = requests.get(url, headers=ESPN_HTML_HEADERS, timeout=8)
        response.raise_for_status()

        soup = BeautifulSoup(response.text, "html.parser")
        depth_chart = parse_depth_chart_rows(soup, team_id)
        payload = {"data": depth_chart, "count": len(depth_chart), "status": "success"}
        if redis_client:
            await redis_client.set(cache_key, json.dumps(payload), ex=86400)
        return payload
    except Exception as e:
        logger.warning(f"Depth chart scraper failed for team {team_id}: {str(e)}")
        return {"data": [], "count": 0, "status": "scrape_failed", "message": str(e)}


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
