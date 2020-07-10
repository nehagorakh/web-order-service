package in.lifcare.order.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Data;

/**
 * @author rahul
 *
 */
@Data
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class StatusUpdateRequest {
	
	String reason;
	
	String comments;
}
