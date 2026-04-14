package pl.writeonly.elevatorsystemsimulation.simulation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pl.writeonly.elevatorsystemsimulation.simulation.config.SimulationProperties;
import pl.writeonly.elevatorsystemsimulation.simulation.model.Direction;
import pl.writeonly.elevatorsystemsimulation.simulation.persistence.SimulationEvent;
import pl.writeonly.elevatorsystemsimulation.simulation.persistence.SimulationEventRepository;

class ElevatorSystemServiceTest {

	private ElevatorSystemService service;

	@BeforeEach
	void setUp() {
		SimulationEventRepository eventRepository = mock(SimulationEventRepository.class);
		when(eventRepository.save(any(SimulationEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(eventRepository.findTop20ByOrderByCreatedAtDescIdDesc()).thenReturn(List.of());

		service = new ElevatorSystemService(
			new SimulationProperties(8, 2, 250, "http://localhost:5173"),
			eventRepository
		);
		service.initialize();
	}

	@Test
	void shouldAssignHallCallToNearestElevatorAndEventuallyServeIt() {
		service.callElevator(3, Direction.UP);

		service.simulationTick();
		service.simulationTick();
		service.simulationTick();

		var state = service.getState();

		assertThat(state.elevators())
			.anySatisfy(elevator -> {
				assertThat(elevator.currentFloor()).isEqualTo(3);
				assertThat(elevator.doorState().name()).isEqualTo("OPEN");
			});
		assertThat(state.pendingHallCallsUp()).doesNotContain(3);
	}

	@Test
	void shouldQueueCabRequestAfterHallPickup() {
		service.callElevator(2, Direction.UP);

		service.simulationTick();
		service.simulationTick();

		int assignedElevatorId = service.getState().elevators().stream()
			.filter(elevator -> elevator.currentFloor() == 2)
			.findFirst()
			.orElseThrow()
			.id();

		service.selectFloor(assignedElevatorId, 6);
		service.simulationTick();
		service.simulationTick();
		service.simulationTick();
		service.simulationTick();

		var state = service.getState();
		assertThat(state.elevators())
			.anySatisfy(elevator -> {
				assertThat(elevator.id()).isEqualTo(assignedElevatorId);
				assertThat(elevator.currentFloor()).isEqualTo(6);
			});
	}

	@Test
	void shouldRejectInvalidFloor() {
		assertThatThrownBy(() -> service.callElevator(10, Direction.UP))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("floor must be between 0 and 7");
	}
}
