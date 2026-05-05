import random
import time
import json
import numpy as np
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from nba_api.stats.endpoints import leaguestandingsv3, shotchartdetail
import logging

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(title="NBA Stats Proxy", version="1.0.0")

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
    Fetch NBA standings data from nba_api.
    
    Returns:
        dict: League standings data in JSON format
    """
    try:
        add_random_delay()
        season_label = None
        if season is not None:
            season_label = f"{season - 1}-{str(season)[-2:]}"
            logger.info(f"Fetching standings for season {season_label} from NBA API")
        else:
            logger.info("Fetching current standings from NBA API")

        # Fetch standings using nba_api with custom headers
        standings_request = leaguestandingsv3.LeagueStandingsV3(
            season=season_label,
            headers=HEADERS
        ) if season_label else leaguestandingsv3.LeagueStandingsV3(headers=HEADERS)

        standings = standings_request.get_data_frames()[0]
        
        # Convert DataFrame to list of dicts for JSON serialization
        standings_list = standings.to_dict(orient='records')
        
        # Sanitize data to remove NaN and infinite values
        standings_list = sanitize_data(standings_list)
        
        logger.info(f"Successfully fetched {len(standings_list)} standing records")
        
        return {
            "data": standings_list,
            "count": len(standings_list),
            "status": "success"
        }
    except Exception as e:
        logger.error(f"Error fetching standings: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Failed to fetch standings: {str(e)}")


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

