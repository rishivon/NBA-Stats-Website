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
- Docker Desktop, recommended for local PostgreSQL and Redis

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

Create a local environment file:

```bash
cd ..
cp .env.example .env
```

The checked-in `.env.example` contains local-only placeholders. Keep real Supabase or production credentials in your private `.env` file only. `.env` is ignored by Git.

## Local PostgreSQL and Redis with Docker

Use Docker when you want to run the full persistence and cache stack locally.

Start Postgres and Redis:

```bash
docker compose -f docker-compose.local.yml up -d
```

This starts:

- Postgres on `localhost:5432`
- Redis on `localhost:6379`

The local Postgres container automatically runs `nba-visualizer-backend/src/main/resources/db/supabase_schema.sql` the first time its Docker volume is created. The local Docker ports are bound to `127.0.0.1` only.

Check that both services are healthy:

```bash
docker compose -f docker-compose.local.yml ps
docker exec nba-visualizer-postgres pg_isready -U nba_visualizer -d nba_visualizer
docker exec nba-visualizer-redis redis-cli ping
```

Expected results:

- Postgres reports `accepting connections`
- Redis returns `PONG`

Stop the local infrastructure:

```bash
docker compose -f docker-compose.local.yml down
```

Reset the local Postgres data and recreate tables from scratch:

```bash
docker compose -f docker-compose.local.yml down -v
docker compose -f docker-compose.local.yml up -d
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
set -a; source ../.env; set +a
python main.py
```

Terminal 2, Spring Boot backend:

```bash
cd nba-visualizer-backend
set -a; source ../.env; set +a
mvn spring-boot:run
```

Terminal 3, Next.js frontend:

```bash
cd nba-visualizer-frontend
npm run dev
```

## Optional Redis

Redis is used as the first cache layer for standings and team metadata. If Redis is not running, the backend and proxy continue with database-backed behavior.

The recommended local setup is:

```bash
docker compose -f docker-compose.local.yml up -d redis
```

Default local Redis settings:

```bash
export REDIS_HOST=localhost
export REDIS_PORT=6379
export REDIS_URL=redis://localhost:6379
```

## Optional PostgreSQL or Supabase

For local PostgreSQL, use Docker:

```bash
docker compose -f docker-compose.local.yml up -d postgres
cp .env.example .env
```

For Supabase, create the tables in `nba-visualizer-backend/src/main/resources/db/supabase_schema.sql`, then set these values in your private `.env` file:

```bash
DATABASE_URL='jdbc:postgresql://YOUR_HOST:5432/postgres'
DATABASE_USERNAME='YOUR_USERNAME'
DATABASE_PASSWORD='YOUR_PASSWORD'
JPA_DDL_AUTO='validate'
```

If these variables are not set, the backend writes to a local H2 database under `nba-visualizer-backend/data/`.

Do not commit `.env` or real database credentials.

## Useful URLs

- Frontend: `http://localhost:3000`
- Backend standings API: `http://localhost:8080/api/standings`
- Backend teams API: `http://localhost:8080/api/teams`
- Proxy health check: `http://localhost:8000/health`
- Proxy standings API: `http://localhost:8000/standings`

## Confirm Persistence and Cache

After the Docker infrastructure and app services are running, request standings:

```bash
curl http://localhost:8080/api/standings
```

Confirm rows were persisted to Postgres:

```bash
docker exec nba-visualizer-postgres psql -U nba_visualizer -d nba_visualizer -c "select season, count(*) from standings group by season;"
```

Confirm Redis has cached keys:

```bash
docker exec nba-visualizer-redis redis-cli keys '*'
```

You should see keys such as `standings:2026` or `teams:all` after the backend serves those endpoints.

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
