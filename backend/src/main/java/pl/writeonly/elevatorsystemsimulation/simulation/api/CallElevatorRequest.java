package pl.writeonly.elevatorsystemsimulation.simulation.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import pl.writeonly.elevatorsystemsimulation.simulation.model.Direction;

public record CallElevatorRequest(
	@Min(0) @Max(200) int floor,
	@NotNull Direction direction
) {
}
