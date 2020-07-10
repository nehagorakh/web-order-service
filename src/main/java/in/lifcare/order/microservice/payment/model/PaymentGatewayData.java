package in.lifcare.order.microservice.payment.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class PaymentGatewayData {

	private long orderId;
	
	private String name;

	private String method;

	private String subMethod;

	private double amount;

	private String refTransactionId;

	private Map<String, Object> additionalData;

}