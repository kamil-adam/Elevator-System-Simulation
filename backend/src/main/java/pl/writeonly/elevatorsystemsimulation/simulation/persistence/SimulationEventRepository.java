package pl.writeonly.elevatorsystemsimulation.simulation.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SimulationEventRepository extends JpaRepository<SimulationEvent, Long> {

	List<SimulationEvent> findTop20ByOrderByCreatedAtDescIdDesc();
}
