package pl.writeonly.elevatorsystemsimulation.simulation.api;

import java.time.Instant;
import java.util.List;

public record SystemStateResponse(
	int floors,
	List<Integer> floorLabels,
	List<ElevatorStateResponse> elevators,
	List<Integer> pendingHallCallsUp,
	List<Integer> pendingHallCallsDown,
	List<SimulationEventResponse> recentEvents,
	Instant updatedAt
) {
}
