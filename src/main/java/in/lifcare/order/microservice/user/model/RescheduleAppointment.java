package in.lifcare.order.microservice.user.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import in.lifcare.core.model.User;
import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class RescheduleAppointment {
	
	public RescheduleAppointment(long slotId, long userId, User user) {
		this.slotId = slotId;
		this.userId = userId;
		this.user = user;
	}
	
	private long slotId;
	
	private long userId;

	private User user;
}
