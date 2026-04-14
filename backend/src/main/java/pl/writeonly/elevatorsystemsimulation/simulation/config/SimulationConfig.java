package pl.writeonly.elevatorsystemsimulation.simulation.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SimulationProperties.class)
public class SimulationConfig {
}
