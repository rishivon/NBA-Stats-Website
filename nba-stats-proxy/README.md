# NBA Stats Proxy

A FastAPI-based proxy service that fetches NBA statistics from the official NBA Stats API using the `nba_api` library. This service provides anti-ban headers and rate-limiting delays to avoid triggering NBA's anti-scraping measures.

## Features

✅ **Anti-Ban Headers Configuration**
- Realistic User-Agent header (Chrome/Mac)
- Proper Host and Referer headers
- Comprehensive Accept-* headers

✅ **Rate Limiting Protection**
- Random delay of 0.5-1.5 seconds between requests
- Prevents overwhelming NBA servers

✅ **CORS Support**
- Allows requests from `localhost:3000` (frontend) and `localhost:8080` (backend)
- Properly configured CORS middleware

✅ **Data Sanitization**
- Removes NaN and infinite float values
- Ensures JSON serialization compatibility

## Available Endpoints

### GET /standings
Returns current NBA standings data from the official NBA API.

**Response:**
```json
{
  "data": [
    {
      "TeamID": 1610612760,
      "TeamCity": "Oklahoma City",
      "TeamName": "Thunder",
      "Conference": "West",
      "WINS": 64,
      "LOSSES": 18,
      "WinPCT": 0.78,
      "L10": "7-3",
      "CurrentStreak": -2,
      ...
    }
  ],
  "count": 30,
  "status": "success"
}
```

### GET /player-shots/{player_id}
Returns shot chart details for a specific player.

**Parameters:**
- `player_id` (integer): NBA player ID

**Response:**
```json
{
  "data": [...shot records...],
  "count": 1542,
  "player_id": 201950,
  "status": "success"
}
```

### GET /health
Health check endpoint for monitoring service status.

**Response:**
```json
{
  "status": "healthy",
  "service": "NBA Stats Proxy"
}
```

## Setup

1. **Create Python Virtual Environment:**
   ```bash
   python3 -m venv venv
   source venv/bin/activate
   ```

2. **Install Dependencies:**
   ```bash
   pip install -r requirements.txt
   ```

3. **Start the Proxy:**
   ```bash
   python main.py
   ```

The proxy will start on `http://localhost:8000`

## Dependencies

- **fastapi**: Modern async web framework
- **uvicorn**: ASGI server for FastAPI
- **nba-api**: Official NBA Stats API wrapper
- **pandas**: Data manipulation (required by nba_api)
- **numpy**: Numerical computing (required by pandas)
- **python-multipart**: Form data support

## How It Works

```
NBA Stats API (stats.nba.com)
        ↑
        │ (with anti-ban headers)
        │
   nba_api library
        ↑
        │ (DataFrame conversion)
        │
   data sanitization (remove NaN/inf)
        ↑
        │ (JSON response)
        │
   FastAPI endpoint → Backend → Frontend
```

## Anti-Ban Headers

The proxy includes realistic browser headers to avoid triggering NBA's anti-scraping measures:

```python
HEADERS = {
    "User-Agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)...",
    "Host": "stats.nba.com",
    "Referer": "https://www.nba.com/",
    "Accept": "application/json",
    "Accept-Language": "en-US,en;q=0.9",
    ...
}
```

## Rate Limiting

Each request includes a random delay of 0.5-1.5 seconds to:
- Avoid hitting rate limits
- Appear more like a normal browser user
- Respect NBA's servers

## Error Handling

- Invalid player IDs return 500 error with descriptive message
- Network errors are logged and returned as errors
- DataFrame parsing errors are caught and reported

## Architecture Integration

This proxy acts as a bridge between:
- **Frontend** (Next.js on :3000) → Requests data
- **Backend** (Spring Boot on :8080) → Fetches from proxy
- **Proxy** (FastAPI on :8000) → Fetches from NBA API

The backend's `BallDontLieClient` has been updated to call this proxy instead of external APIs, making the entire system:
1. More reliable (using official NBA API)
2. More flexible (can add more endpoints easily)
3. Centralized (single proxy for all NBA data)

## Logging

The proxy includes comprehensive logging:
- INFO: Successful requests and data fetching
- WARNING: Missing or incomplete data
- ERROR: API failures and parsing errors

Logs are sent to console with timestamp and log level.

## Future Enhancements

- Add caching layer (Redis) for standings
- Implement WebSocket support for real-time updates
- Add more endpoints (player stats, game data, etc.)
- Rate limit per IP address
- Metrics/monitoring endpoint
