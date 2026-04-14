package pl.writeonly.elevatorsystemsimulation.simulation.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record SelectFloorRequest(
	@Min(0) @Max(200) int floor
) {
}
