package in.lifcare.order.microservice.account.customer.model;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class RefillLead {

	private long id;
	private long customerId;
	private long patientId;
	private String cartId;
	private String customerFirstName;
	private String customerLastName;
	private String patientFirstName;
	private String patientLastName;
	private String customerMobile;
	private String patientMobile;
	private Timestamp orderDueDate;
	private Timestamp firstCallDate;
	private Timestamp firstContactDate;
	private String status;
	private boolean isHold;
	private Timestamp scheduledAt;
	private long rescheduledCount;
	private long attemptCount;
	private Timestamp closedAt;
	private boolean isReconciled;
	
	public static interface STATUS {
		String NEW = "NEW";
		String AGENT_RESHEDULED = "AGENT_RESHEDULED";
		String CUSTOMER_RESHEDULED = "CUSTOMER_RESHEDULED";
		String DND = "DND";
		String CLOSED = "CLOSED";
		String COMPLETED = "COMPLETED";
		String AUTO_COMPLETED = "AUTO_COMPLETED";
		
		List<String> OPEN_STATUSES = new ArrayList<String>(Arrays.asList(NEW, AGENT_RESHEDULED, CUSTOMER_RESHEDULED));
		List<String> CLOSED_STATUSES = new ArrayList<String>(Arrays.asList(CLOSED, COMPLETED, AUTO_COMPLETED));

	}
	
}
