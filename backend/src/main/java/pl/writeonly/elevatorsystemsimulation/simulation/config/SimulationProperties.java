package pl.writeonly.elevatorsystemsimulation.simulation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "simulation")
public record SimulationProperties(
	int floors,
	int elevators,
	int tickMillis,
	String allowedOrigin
) {

	public SimulationProperties {
		if (floors < 2) {
			throw new IllegalArgumentException("floors must be at least 2");
		}
		if (elevators < 1) {
			throw new IllegalArgumentException("elevators must be at least 1");
		}
		if (tickMillis < 100) {
			throw new IllegalArgumentException("tickMillis must be at least 100");
		}
	}
}
