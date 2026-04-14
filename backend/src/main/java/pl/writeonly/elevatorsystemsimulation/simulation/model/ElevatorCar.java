package pl.writeonly.elevatorsystemsimulation.simulation.model;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;

public class ElevatorCar {

	private final int id;
	private int currentFloor;
	private Direction direction;
	private DoorState doorState;
	private final NavigableSet<Integer> upStops;
	private final NavigableSet<Integer> downStops;

	public ElevatorCar(int id, int startingFloor) {
		this.id = id;
		this.currentFloor = startingFloor;
		this.direction = Direction.IDLE;
		this.doorState = DoorState.CLOSED;
		this.upStops = new TreeSet<>();
		this.downStops = new TreeSet<>();
	}

	public int id() {
		return id;
	}

	public int currentFloor() {
		return currentFloor;
	}

	public Direction direction() {
		return direction;
	}

	public DoorState doorState() {
		return doorState;
	}

	public void addHallCall(int floor, Direction requestedDirection) {
		if (requestedDirection == Direction.DOWN) {
			downStops.add(floor);
			return;
		}

		upStops.add(floor);
	}

	public void addCabRequest(int floor) {
		if (floor == currentFloor) {
			return;
		}

		if (floor > currentFloor) {
			upStops.add(floor);
			return;
		}

		downStops.add(floor);
	}

	public List<Integer> pendingStops() {
		List<Integer> stops = new ArrayList<>(upStops);
		stops.addAll(downStops.descendingSet());
		return stops;
	}

	public int pendingCount() {
		return upStops.size() + downStops.size();
	}

	public boolean servesFloor(int floor) {
		return upStops.contains(floor) || downStops.contains(floor);
	}

	public void step() {
		if (doorState == DoorState.OPEN) {
			doorState = DoorState.CLOSED;
		}

		updateDirectionIfIdle();
		if (direction == Direction.IDLE) {
			return;
		}

		moveOneFloor();
		if (shouldStopAtCurrentFloor()) {
			openDoorsAtCurrentFloor();
		} else if (!hasStopsInCurrentDirection()) {
			flipDirectionIfNeeded();
		}
	}

	public List<FloorStop> stopsAtCurrentFloor() {
		List<FloorStop> floorStops = new ArrayList<>();
		if (upStops.remove(currentFloor)) {
			floorStops.add(new FloorStop(currentFloor, EnumSet.of(StopReason.HALL_CALL, StopReason.CAB_REQUEST)));
		}
		if (downStops.remove(currentFloor)) {
			if (floorStops.isEmpty()) {
				floorStops.add(new FloorStop(currentFloor, EnumSet.of(StopReason.HALL_CALL, StopReason.CAB_REQUEST)));
			}
		}
		return floorStops;
	}

	private void updateDirectionIfIdle() {
		if (direction != Direction.IDLE) {
			return;
		}

		if (!upStops.isEmpty()) {
			direction = upStops.first() >= currentFloor ? Direction.UP : Direction.DOWN;
			return;
		}

		if (!downStops.isEmpty()) {
			direction = downStops.last() <= currentFloor ? Direction.DOWN : Direction.UP;
		}
	}

	private void moveOneFloor() {
		if (direction == Direction.UP) {
			currentFloor += 1;
		} else if (direction == Direction.DOWN) {
			currentFloor -= 1;
		}
	}

	private boolean shouldStopAtCurrentFloor() {
		return upStops.contains(currentFloor) || downStops.contains(currentFloor);
	}

	private boolean hasStopsInCurrentDirection() {
		if (direction == Direction.UP) {
			return upStops.ceiling(currentFloor) != null || downStops.ceiling(currentFloor) != null;
		}

		if (direction == Direction.DOWN) {
			return upStops.floor(currentFloor) != null || downStops.floor(currentFloor) != null;
		}

		return false;
	}

	private void flipDirectionIfNeeded() {
		if (direction == Direction.UP && (!downStops.isEmpty() || upStops.floor(currentFloor) != null)) {
			direction = Direction.DOWN;
			return;
		}

		if (direction == Direction.DOWN && (!upStops.isEmpty() || downStops.ceiling(currentFloor) != null)) {
			direction = Direction.UP;
			return;
		}

		direction = Direction.IDLE;
	}

	private void openDoorsAtCurrentFloor() {
		stopsAtCurrentFloor();
		doorState = DoorState.OPEN;

		if (!hasAnyStops()) {
			direction = Direction.IDLE;
			return;
		}

		if (!hasStopsInCurrentDirection()) {
			flipDirectionIfNeeded();
		}
	}

	private boolean hasAnyStops() {
		return !upStops.isEmpty() || !downStops.isEmpty();
	}

	public enum DoorState {
		OPEN,
		CLOSED
	}
}
