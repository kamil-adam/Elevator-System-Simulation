# Elevator System Simulation

Recruitment challenge solution built with the exact stack you want: Spring Boot, PostgreSQL, Docker and React.js.

## Stack

- Backend: Spring Boot 4, Java 21, REST + Server-Sent Events
- Database: PostgreSQL 17
- Frontend: React 19 + Vite + TypeScript
- Containers: Docker + Docker Compose

## What the app does

- simulates multiple elevators in a configurable building,
- accepts hall calls from floors,
- accepts cabin destination requests per elevator,
- dispatches requests to the most suitable elevator,
- streams live system updates to the React frontend,
- persists recent request events in PostgreSQL for audit/history.

## Architecture

### Backend

The backend is the core of the challenge.

- `ElevatorSystemService` owns the in-memory simulation state.
- Every simulation tick moves elevators, opens/closes doors and processes stops.
- Hall calls are assigned using a scoring strategy based on:
  - distance,
  - current direction,
  - whether the elevator is already moving toward the request,
  - current queue size.
- State access is protected with `ReentrantLock`, so concurrent API calls and scheduled ticks do not corrupt the simulation.
- User-triggered events are stored in PostgreSQL using Spring Data JPA.

Main API endpoints:

- `GET /api/state`
- `POST /api/calls`
- `POST /api/elevators/{id}/requests`
- `GET /api/stream`

### Frontend

The frontend is a separate React application.

- shows the building floors and all elevator shafts,
- visualizes current elevator position, direction and door status,
- lets the user call an elevator from any floor,
- lets the user select cabin destinations,
- listens to backend updates in real time through SSE,
- shows a persisted request history panel coming from PostgreSQL-backed events.

### PostgreSQL

PostgreSQL is used to persist simulation events such as:

- hall calls,
- cabin requests.

This keeps the simulator fast by running movement logic in memory, while still giving the system a persistent audit trail that survives restarts.

## Run with Docker

```bash
docker compose up --build
```

Apps:

- Frontend: [http://localhost:3000](http://localhost:3000)
- Backend API: [http://localhost:8080/api/state](http://localhost:8080/api/state)
- PostgreSQL: `localhost:5432`

## Run locally without Docker

### 1. Start PostgreSQL

Create a database named `elevator_simulation` with:

- user: `elevator`
- password: `elevator`

### 2. Start backend

```bash
cd backend
./gradlew bootRun
```

### 3. Start frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend dev server runs on [http://localhost:5173](http://localhost:5173).

## Configuration

Backend configuration lives in [backend/src/main/resources/application.yaml](/Users/kamilzabinski/IdeaProjects/kamil-adam/Elevator-System-Simulation/backend/src/main/resources/application.yaml).

Key options:

- number of floors,
- number of elevators,
- simulation tick speed,
- allowed frontend origin,
- PostgreSQL connection settings.

## Tests

Backend tests:

```bash
cd backend
./gradlew test
```

Frontend production build:

```bash
cd frontend
npm run build
```

## Notes for the challenge

This solution intentionally prioritizes backend quality, because that is the main focus of the task:

- the simulation engine is isolated and testable,
- the dispatching strategy is easy to explain and improve,
- the frontend is real-time and clean, but does not overcomplicate the solution,
- Docker makes the stack easy to review and run.

## Next improvements

- separate passenger trips from raw floor requests,
- add elevator capacity and traffic heuristics,
- persist full simulation snapshots,
- add integration tests for HTTP and frontend flows.
