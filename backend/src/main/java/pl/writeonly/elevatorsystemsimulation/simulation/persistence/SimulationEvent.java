package pl.writeonly.elevatorsystemsimulation.simulation.persistence;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "simulation_events")
public class SimulationEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private EventType type;

	@Column(nullable = false)
	private int floor;

	@Column(name = "elevator_id")
	private Integer elevatorId;

	@Column(length = 16)
	private String direction;

	@Column(nullable = false, length = 255)
	private String description;

	@Column(nullable = false)
	private Instant createdAt;

	protected SimulationEvent() {
	}

	public SimulationEvent(
		EventType type,
		int floor,
		Integer elevatorId,
		String direction,
		String description,
		Instant createdAt
	) {
		this.type = type;
		this.floor = floor;
		this.elevatorId = elevatorId;
		this.direction = direction;
		this.description = description;
		this.createdAt = createdAt;
	}

	public Long getId() {
		return id;
	}

	public EventType getType() {
		return type;
	}

	public int getFloor() {
		return floor;
	}

	public Integer getElevatorId() {
		return elevatorId;
	}

	public String getDirection() {
		return direction;
	}

	public String getDescription() {
		return description;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
