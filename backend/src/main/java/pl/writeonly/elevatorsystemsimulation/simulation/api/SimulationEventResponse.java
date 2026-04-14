package pl.writeonly.elevatorsystemsimulation.simulation.api;

import java.time.Instant;

public record SimulationEventResponse(
	long id,
	String type,
	int floor,
	Integer elevatorId,
	String direction,
	String description,
	Instant createdAt
) {
}
