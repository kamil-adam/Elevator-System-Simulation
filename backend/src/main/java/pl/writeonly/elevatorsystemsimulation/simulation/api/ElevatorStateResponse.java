package pl.writeonly.elevatorsystemsimulation.simulation.api;

import java.util.List;

import pl.writeonly.elevatorsystemsimulation.simulation.model.Direction;
import pl.writeonly.elevatorsystemsimulation.simulation.model.ElevatorCar.DoorState;

public record ElevatorStateResponse(
	int id,
	int currentFloor,
	Direction direction,
	DoorState doorState,
	List<Integer> pendingStops
) {
}
