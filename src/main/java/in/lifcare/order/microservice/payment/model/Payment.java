package in.lifcare.order.microservice.payment.model;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Payment {

	@Id
	@GeneratedValue
	@Column(updatable = false)
    private long id;

	private long orderId;

	private long customerId;
	
	private double amount;

	private String refTransactionId;

	private String type;

	private String gateway;

	private String status;

	private String method;

	private String subMethod;
	
	private String log;
	
	@CreationTimestamp
	@Column(updatable = false)
	private Timestamp createdAt;

	@UpdateTimestamp
	private Timestamp updatedAt;

	public interface STATUS {
		String INITIATED = "INITIATED";
		String COMPLETED = "COMPLETED";
		String FAILED = "FAILED";
		String DISCARDED = "DISCARDED";
		List<String> VALID_STATUS_LIST = Arrays.asList(new String[] {INITIATED, COMPLETED, FAILED, DISCARDED});
	}

}