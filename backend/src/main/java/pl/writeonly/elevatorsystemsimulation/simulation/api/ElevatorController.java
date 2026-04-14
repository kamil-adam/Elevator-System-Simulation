package pl.writeonly.elevatorsystemsimulation.simulation.api;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.validation.Valid;
import pl.writeonly.elevatorsystemsimulation.simulation.service.ElevatorSystemService;

@Validated
@RestController
@RequestMapping("/api")
public class ElevatorController {

	private final ElevatorSystemService elevatorSystemService;

	public ElevatorController(ElevatorSystemService elevatorSystemService) {
		this.elevatorSystemService = elevatorSystemService;
	}

	@GetMapping("/state")
	public SystemStateResponse getState() {
		return elevatorSystemService.getState();
	}

	@PostMapping("/calls")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public SystemStateResponse callElevator(@Valid @RequestBody CallElevatorRequest request) {
		return elevatorSystemService.callElevator(request.floor(), request.direction());
	}

	@PostMapping("/elevators/{elevatorId}/requests")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public SystemStateResponse selectFloor(
		@PathVariable int elevatorId,
		@Valid @RequestBody SelectFloorRequest request
	) {
		return elevatorSystemService.selectFloor(elevatorId, request.floor());
	}

	@GetMapping("/stream")
	public SseEmitter stream() {
		return elevatorSystemService.subscribe();
	}

	@ExceptionHandler(IllegalArgumentException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ErrorResponse handleIllegalArgument(IllegalArgumentException exception) {
		return new ErrorResponse(exception.getMessage());
	}

	record ErrorResponse(String message) {
	}
}
