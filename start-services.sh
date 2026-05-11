#!/bin/bash

# NBA Stats Website - Startup Guide
# This script starts all three services in the correct order

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

WORKSPACE_DIR="$(pwd)"

if [ -f "$WORKSPACE_DIR/.env" ]; then
  set -a
  # shellcheck disable=SC1091
  source "$WORKSPACE_DIR/.env"
  set +a
fi

echo -e "${BLUE}Starting NBA Stats Website...${NC}\n"

# Start Python Proxy (Port 8000)
echo -e "${GREEN}[1/3] Starting NBA Stats Proxy (Python FastAPI)${NC}"
echo "     Listening on: http://localhost:8000"
echo "     Running: nba_api via proxy with anti-ban headers"
cd "$WORKSPACE_DIR/nba-stats-proxy"
./venv/bin/python main.py &
PROXY_PID=$!
sleep 3
echo -e "${GREEN}✓ Proxy started (PID: $PROXY_PID)${NC}\n"

# Start Java Backend (Port 8080)
echo -e "${GREEN}[2/3] Starting NBA Visualizer Backend (Spring Boot)${NC}"
echo "     Listening on: http://localhost:8080"
echo "     API Endpoint: http://localhost:8080/api/standings"
cd "$WORKSPACE_DIR/nba-visualizer-backend"
mvn -q spring-boot:run &
BACKEND_PID=$!
sleep 5
echo -e "${GREEN}✓ Backend started (PID: $BACKEND_PID)${NC}\n"

# Start Frontend (Port 3000)
echo -e "${GREEN}[3/3] Starting NBA Visualizer Frontend (Next.js)${NC}"
echo "     Listening on: http://localhost:3000"
cd "$WORKSPACE_DIR/nba-visualizer-frontend"
npm run dev &
FRONTEND_PID=$!
sleep 3
echo -e "${GREEN}✓ Frontend started (PID: $FRONTEND_PID)${NC}\n"

echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}All services are running!${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}\n"

echo "🌐 Frontend: http://localhost:3000"
echo "⚙️  Backend:  http://localhost:8080"
echo "🐍 Proxy:    http://localhost:8000\n"

echo "Press Ctrl+C to stop all services...\n"

# Wait for all background processes
wait

# Cleanup on exit
echo -e "\n${BLUE}Shutting down services...${NC}"
kill $PROXY_PID $BACKEND_PID $FRONTEND_PID 2>/dev/null
echo -e "${GREEN}All services stopped${NC}"
