# NBA-Stats-Website

Website for NBA stats, analysis, projections, and more.

## Local Development

The app runs as three local services:

- FastAPI NBA stats proxy on `http://localhost:8000`
- Spring Boot backend on `http://localhost:8080`
- Next.js frontend on `http://localhost:3000`

Open the website at `http://localhost:3000`.

## Prerequisites

- Java 17
- Maven
- Node.js and npm
- Python 3
- Redis, optional but recommended for cache testing
- PostgreSQL/Supabase, optional for production-like persistence

The backend uses a local H2 database fallback when `DATABASE_URL` is not set, so you can start the site locally without PostgreSQL.

## First-Time Setup

Install frontend dependencies:

```bash
cd nba-visualizer-frontend
npm install
```

Create the proxy virtual environment and install Python dependencies:

```bash
cd ../nba-stats-proxy
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
```

## Quick Start

From the repository root:

```bash
chmod +x start-services.sh
./start-services.sh
```

This starts the proxy, backend, and frontend in order. Press `Ctrl+C` in that terminal to stop the services.

## Manual Startup

Use three separate terminals if you want to run each service manually.

Terminal 1, Python proxy:

```bash
cd nba-stats-proxy
source venv/bin/activate
python main.py
```

Terminal 2, Spring Boot backend:

```bash
cd nba-visualizer-backend
mvn spring-boot:run
```

Terminal 3, Next.js frontend:

```bash
cd nba-visualizer-frontend
npm run dev
```

## Optional Redis

Redis is used as the first cache layer for standings and team metadata. If Redis is not running, the backend and proxy continue with database-backed behavior.

Start Redis with Docker:

```bash
docker run --rm -p 6379:6379 redis:7-alpine
```

Default local Redis settings:

```bash
export REDIS_HOST=localhost
export REDIS_PORT=6379
export REDIS_URL=redis://localhost:6379
```

## Optional PostgreSQL or Supabase

For production-like persistence, create the tables in `nba-visualizer-backend/src/main/resources/db/supabase_schema.sql`, then set:

```bash
export DATABASE_URL='jdbc:postgresql://YOUR_HOST:5432/postgres'
export DATABASE_USERNAME='YOUR_USERNAME'
export DATABASE_PASSWORD='YOUR_PASSWORD'
export JPA_DDL_AUTO=validate
```

If these variables are not set, the backend writes to a local H2 database under `nba-visualizer-backend/data/`.

## Useful URLs

- Frontend: `http://localhost:3000`
- Backend standings API: `http://localhost:8080/api/standings`
- Backend teams API: `http://localhost:8080/api/teams`
- Proxy health check: `http://localhost:8000/health`
- Proxy standings API: `http://localhost:8000/standings`

## Verification

Backend:

```bash
cd nba-visualizer-backend
mvn test
```

Frontend:

```bash
cd nba-visualizer-frontend
npm run lint
```
