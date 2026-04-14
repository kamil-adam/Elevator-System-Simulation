package pl.writeonly.elevatorsystemsimulation.simulation.service;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.IntStream;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.annotation.PostConstruct;
import pl.writeonly.elevatorsystemsimulation.simulation.api.ElevatorStateResponse;
import pl.writeonly.elevatorsystemsimulation.simulation.api.SimulationEventResponse;
import pl.writeonly.elevatorsystemsimulation.simulation.api.SystemStateResponse;
import pl.writeonly.elevatorsystemsimulation.simulation.config.SimulationProperties;
import pl.writeonly.elevatorsystemsimulation.simulation.model.Direction;
import pl.writeonly.elevatorsystemsimulation.simulation.model.ElevatorCar;
import pl.writeonly.elevatorsystemsimulation.simulation.persistence.EventType;
import pl.writeonly.elevatorsystemsimulation.simulation.persistence.SimulationEvent;
import pl.writeonly.elevatorsystemsimulation.simulation.persistence.SimulationEventRepository;

@Service
public class ElevatorSystemService {

	private final SimulationProperties properties;
	private final SimulationEventRepository eventRepository;
	private final ReentrantLock stateLock = new ReentrantLock();
	private final List<ElevatorCar> elevators = new ArrayList<>();
	private final Set<Integer> pendingHallCallsUp = ConcurrentHashMap.newKeySet();
	private final Set<Integer> pendingHallCallsDown = ConcurrentHashMap.newKeySet();
	private final List<SseEmitter> subscribers = new CopyOnWriteArrayList<>();

	public ElevatorSystemService(SimulationProperties properties, SimulationEventRepository eventRepository) {
		this.properties = properties;
		this.eventRepository = eventRepository;
	}

	@PostConstruct
	void initialize() {
		for (int i = 0; i < properties.elevators(); i++) {
			elevators.add(new ElevatorCar(i + 1, 0));
		}
	}

	@Scheduled(fixedDelayString = "${simulation.tick-millis}")
	public void simulationTick() {
		stateLock.lock();
		try {
			elevators.forEach(ElevatorCar::step);
			pushSnapshotUnsafe();
		} finally {
			stateLock.unlock();
		}
	}

	public SystemStateResponse getState() {
		stateLock.lock();
		try {
			return snapshotUnsafe();
		} finally {
			stateLock.unlock();
		}
	}

	public SystemStateResponse callElevator(int floor, Direction direction) {
		validateFloor(floor);
		if (direction == Direction.IDLE) {
			throw new IllegalArgumentException("direction must be UP or DOWN");
		}

		stateLock.lock();
		try {
			ElevatorCar assignedElevator = chooseElevator(floor, direction);
			assignedElevator.addHallCall(floor, direction);
			if (direction == Direction.UP) {
				pendingHallCallsUp.add(floor);
			} else {
				pendingHallCallsDown.add(floor);
			}
			eventRepository.save(new SimulationEvent(
				EventType.HALL_CALL,
				floor,
				assignedElevator.id(),
				direction.name(),
				"Hall call on floor %d assigned to elevator %d".formatted(floor, assignedElevator.id()),
				Instant.now()
			));
			pushSnapshotUnsafe();
			return snapshotUnsafe();
		} finally {
			stateLock.unlock();
		}
	}

	public SystemStateResponse selectFloor(int elevatorId, int floor) {
		validateFloor(floor);

		stateLock.lock();
		try {
			ElevatorCar elevator = getElevatorById(elevatorId);
			elevator.addCabRequest(floor);
			eventRepository.save(new SimulationEvent(
				EventType.CAB_REQUEST,
				floor,
				elevator.id(),
				null,
				"Cabin request to floor %d added in elevator %d".formatted(floor, elevator.id()),
				Instant.now()
			));
			pushSnapshotUnsafe();
			return snapshotUnsafe();
		} finally {
			stateLock.unlock();
		}
	}

	public SseEmitter subscribe() {
		SseEmitter emitter = new SseEmitter(0L);
		subscribers.add(emitter);
		emitter.onCompletion(() -> subscribers.remove(emitter));
		emitter.onTimeout(() -> subscribers.remove(emitter));
		emitter.onError(error -> subscribers.remove(emitter));

		try {
			emitter.send(SseEmitter.event().name("snapshot").data(getState()));
		} catch (IOException exception) {
			emitter.completeWithError(exception);
		}

		return emitter;
	}

	private ElevatorCar chooseElevator(int floor, Direction requestDirection) {
		return elevators.stream()
			.min(Comparator
				.comparingInt((ElevatorCar elevator) -> score(elevator, floor, requestDirection))
				.thenComparingInt(ElevatorCar::pendingCount)
				.thenComparingInt(ElevatorCar::id))
			.orElseThrow();
	}

	private int score(ElevatorCar elevator, int floor, Direction requestDirection) {
		int distance = Math.abs(elevator.currentFloor() - floor);
		if (elevator.direction() == Direction.IDLE) {
			return distance;
		}

		if (elevator.direction() == requestDirection) {
			boolean onTheWay = requestDirection == Direction.UP
				? elevator.currentFloor() <= floor
				: elevator.currentFloor() >= floor;
			if (onTheWay) {
				return distance;
			}
		}

		return distance + properties.floors() + elevator.pendingCount() * 2;
	}

	private ElevatorCar getElevatorById(int elevatorId) {
		return elevators.stream()
			.filter(elevator -> elevator.id() == elevatorId)
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("unknown elevator id: " + elevatorId));
	}

	private void validateFloor(int floor) {
		if (floor < 0 || floor >= properties.floors()) {
			throw new IllegalArgumentException("floor must be between 0 and " + (properties.floors() - 1));
		}
	}

	private SystemStateResponse snapshotUnsafe() {
		reconcileHallCallsUnsafe();
		List<ElevatorStateResponse> elevatorSnapshots = elevators.stream()
			.map(elevator -> new ElevatorStateResponse(
				elevator.id(),
				elevator.currentFloor(),
				elevator.direction(),
				elevator.doorState(),
				List.copyOf(elevator.pendingStops())
			))
			.toList();

		return new SystemStateResponse(
			properties.floors(),
			IntStream.range(0, properties.floors()).boxed().toList(),
			elevatorSnapshots,
			pendingHallCallsUp.stream().sorted().toList(),
			pendingHallCallsDown.stream().sorted().toList(),
			eventRepository.findTop20ByOrderByCreatedAtDescIdDesc().stream()
				.map(event -> new SimulationEventResponse(
					event.getId(),
					event.getType().name(),
					event.getFloor(),
					event.getElevatorId(),
					event.getDirection(),
					event.getDescription(),
					event.getCreatedAt()
				))
				.toList(),
			Instant.now()
		);
	}

	private void reconcileHallCallsUnsafe() {
		pendingHallCallsUp.removeIf(this::isServedByAnyElevator);
		pendingHallCallsDown.removeIf(this::isServedByAnyElevator);
	}

	private boolean isServedByAnyElevator(int floor) {
		return elevators.stream().noneMatch(elevator -> elevator.servesFloor(floor));
	}

	private void pushSnapshotUnsafe() {
		SystemStateResponse snapshot = snapshotUnsafe();
		subscribers.removeIf(emitter -> !sendSnapshot(emitter, snapshot));
	}

	private boolean sendSnapshot(SseEmitter emitter, SystemStateResponse snapshot) {
		try {
			emitter.send(SseEmitter.event().name("snapshot").data(snapshot));
			return true;
		} catch (IOException exception) {
			emitter.complete();
			return false;
		}
	}
}
