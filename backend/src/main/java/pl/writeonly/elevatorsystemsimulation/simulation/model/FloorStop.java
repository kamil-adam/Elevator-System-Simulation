package pl.writeonly.elevatorsystemsimulation.simulation.model;

import java.util.EnumSet;

public record FloorStop(int floor, EnumSet<StopReason> reasons) {

	public FloorStop {
		reasons = reasons.clone();
	}
}
