import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import './App.css'

type Direction = 'UP' | 'DOWN' | 'IDLE'
type DoorState = 'OPEN' | 'CLOSED'

type ElevatorState = {
  id: number
  currentFloor: number
  direction: Direction
  doorState: DoorState
  pendingStops: number[]
}

type SimulationEvent = {
  id: number
  type: string
  floor: number
  elevatorId: number | null
  direction: string | null
  description: string
  createdAt: string
}

type SystemState = {
  floors: number
  floorLabels: number[]
  elevators: ElevatorState[]
  pendingHallCallsUp: number[]
  pendingHallCallsDown: number[]
  recentEvents: SimulationEvent[]
  updatedAt: string
}

const apiBaseUrl = (import.meta.env.VITE_API_URL as string | undefined)?.replace(/\/$/, '') ?? ''

function App() {
  const [snapshot, setSnapshot] = useState<SystemState | null>(null)
  const [hallFloor, setHallFloor] = useState(0)
  const [hallDirection, setHallDirection] = useState<'UP' | 'DOWN'>('UP')
  const [feedback, setFeedback] = useState('Connecting to the simulation...')
  const [connectionLabel, setConnectionLabel] = useState('Connecting...')

  useEffect(() => {
    void (async () => {
      const response = await fetch(apiUrl('/api/state'))
      const nextSnapshot = (await response.json()) as SystemState
      setSnapshot(nextSnapshot)
      setConnectionLabel('Initial snapshot loaded')
    })()

    const eventSource = new EventSource(streamUrl())
    eventSource.addEventListener('snapshot', (event) => {
      const nextSnapshot = JSON.parse(event.data) as SystemState
      setSnapshot(nextSnapshot)
      setConnectionLabel('Live stream connected')
      setFeedback('Simulation synced in real time.')
    })
    eventSource.onerror = () => {
      setConnectionLabel('Reconnecting...')
    }

    return () => eventSource.close()
  }, [])

  useEffect(() => {
    if (snapshot && !snapshot.floorLabels.includes(hallFloor)) {
      setHallFloor(snapshot.floorLabels.at(-1) ?? 0)
    }
  }, [hallFloor, snapshot])

  async function handleHallCall(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    await postJson('/api/calls', {
      floor: hallFloor,
      direction: hallDirection,
    }, `Hall call registered on floor ${hallFloor}.`)
  }

  async function handleCabRequest(elevatorId: number, floor: number) {
    await postJson(
      `/api/elevators/${elevatorId}/requests`,
      { floor },
      `Destination floor ${floor} sent to elevator ${elevatorId}.`,
    )
  }

  async function postJson(path: string, payload: object, successMessage: string) {
    const response = await fetch(apiUrl(path), {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(payload),
    })

    const responseBody = (await response.json()) as SystemState | { message?: string }
    if (!response.ok) {
      setFeedback('message' in responseBody && responseBody.message ? responseBody.message : 'Request failed.')
      return
    }

    setSnapshot(responseBody as SystemState)
    setFeedback(successMessage)
  }

  if (!snapshot) {
    return <main className="app-shell loading-state">Loading simulation...</main>
  }

  const floorsDescending = [...snapshot.floorLabels].reverse()

  return (
    <main className="app-shell">
      <section className="hero panel">
        <p className="eyebrow">Spring Boot + PostgreSQL + React + Docker</p>
        <h1>Elevator System Simulation</h1>
        <p className="lede">
          A real-time dispatching simulator with persistent request history, live car positions and
          interactive cabin controls.
        </p>
        <div className="status-row">
          <span className="pill">{connectionLabel}</span>
          <span className="pill pill-muted">
            Updated {new Date(snapshot.updatedAt).toLocaleTimeString()}
          </span>
        </div>
      </section>

      <section className="panel controls-panel">
        <div>
          <h2>Hall Call</h2>
          <p>Request an elevator from any floor and direction.</p>
        </div>
        <form className="controls-form" onSubmit={handleHallCall}>
          <label>
            <span>Floor</span>
            <select value={hallFloor} onChange={(event) => setHallFloor(Number(event.target.value))}>
              {floorsDescending.map((floor) => (
                <option key={floor} value={floor}>
                  Floor {floor}
                </option>
              ))}
            </select>
          </label>
          <label>
            <span>Direction</span>
            <select
              value={hallDirection}
              onChange={(event) => setHallDirection(event.target.value as 'UP' | 'DOWN')}
            >
              <option value="UP">Up</option>
              <option value="DOWN">Down</option>
            </select>
          </label>
          <button type="submit">Call elevator</button>
        </form>
        <p className="feedback">{feedback}</p>
      </section>

      <section className="content-grid">
        <section className="panel building-panel">
          <div className="section-heading">
            <div>
              <h2>Building Overview</h2>
              <p>Live shaft view for all floors and elevators.</p>
            </div>
          </div>

          <div className="building-grid">
            {floorsDescending.map((floor) => (
              <div key={floor} className="floor-row">
                <div className="floor-label">Floor {floor}</div>
                <div
                  className="shaft-row"
                  style={{ gridTemplateColumns: `repeat(${snapshot.elevators.length}, minmax(0, 1fr))` }}
                >
                  {snapshot.elevators.map((elevator) => (
                    <div key={`${floor}-${elevator.id}`} className="elevator-shaft">
                      {elevator.currentFloor === floor ? (
                        <div className={`car ${elevator.doorState === 'OPEN' ? 'open' : ''}`}>
                          <span>E{elevator.id}</span>
                          <small>{formatDirection(elevator.direction)}</small>
                        </div>
                      ) : null}
                    </div>
                  ))}
                </div>
                <div className="floor-calls">
                  {snapshot.pendingHallCallsUp.includes(floor) ? <span>UP</span> : null}
                  {snapshot.pendingHallCallsDown.includes(floor) ? <span>DOWN</span> : null}
                  {!snapshot.pendingHallCallsUp.includes(floor) && !snapshot.pendingHallCallsDown.includes(floor) ? (
                    <span>Idle</span>
                  ) : null}
                </div>
              </div>
            ))}
          </div>
        </section>

        <section className="side-column">
          <section className="panel">
            <div className="section-heading">
              <div>
                <h2>Cab Controls</h2>
                <p>Send destination requests directly from each elevator car.</p>
              </div>
            </div>
            <div className="elevator-card-list">
              {snapshot.elevators.map((elevator) => (
                <ElevatorCard
                  key={elevator.id}
                  elevator={elevator}
                  floors={floorsDescending}
                  onRequest={handleCabRequest}
                />
              ))}
            </div>
          </section>

          <section className="panel">
            <div className="section-heading">
              <div>
                <h2>Event Log</h2>
                <p>Persisted request history stored in PostgreSQL.</p>
              </div>
            </div>
            <div className="event-list">
              {snapshot.recentEvents.map((event) => (
                <article key={event.id} className="event-item">
                  <div className="event-meta">
                    <span>{event.type}</span>
                    <span>{new Date(event.createdAt).toLocaleTimeString()}</span>
                  </div>
                  <strong>{event.description}</strong>
                </article>
              ))}
              {snapshot.recentEvents.length === 0 ? (
                <p className="empty-state">No requests recorded yet.</p>
              ) : null}
            </div>
          </section>
        </section>
      </section>
    </main>
  )
}

function ElevatorCard({
  elevator,
  floors,
  onRequest,
}: {
  elevator: ElevatorState
  floors: number[]
  onRequest: (elevatorId: number, floor: number) => Promise<void>
}) {
  const [selectedFloor, setSelectedFloor] = useState(floors[0] ?? 0)

  return (
    <article className="elevator-card">
      <div className="elevator-header">
        <div>
          <h3>Elevator {elevator.id}</h3>
          <p>{formatDirection(elevator.direction)}</p>
        </div>
        <span className="pill pill-muted">{elevator.doorState.toLowerCase()}</span>
      </div>

      <div className="stats">
        <span>Current floor: {elevator.currentFloor}</span>
        <span>Queue: {elevator.pendingStops.join(', ') || 'empty'}</span>
      </div>

      <form
        className="controls-form controls-form-inline"
        onSubmit={(event) => {
          event.preventDefault()
          void onRequest(elevator.id, selectedFloor)
        }}
      >
        <label>
          <span>Destination</span>
          <select value={selectedFloor} onChange={(event) => setSelectedFloor(Number(event.target.value))}>
            {floors.map((floor) => (
              <option key={floor} value={floor}>
                Floor {floor}
              </option>
            ))}
          </select>
        </label>
        <button type="submit">Send</button>
      </form>
    </article>
  )
}

function apiUrl(path: string) {
  return `${apiBaseUrl}${path}`
}

function streamUrl() {
  if (!apiBaseUrl) {
    return '/api/stream'
  }

  return `${apiBaseUrl}/api/stream`
}

function formatDirection(direction: Direction) {
  if (direction === 'UP') {
    return 'Moving up'
  }
  if (direction === 'DOWN') {
    return 'Moving down'
  }
  return 'Idle'
}

export default App
