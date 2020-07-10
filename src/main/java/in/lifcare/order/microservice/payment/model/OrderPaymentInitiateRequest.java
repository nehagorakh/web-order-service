package in.lifcare.order.microservice.payment.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class OrderPaymentInitiateRequest {

	private long orderId;

	private float amount;

	private float couponCashbackEligibleAmount;
	
	private long customerId;

	private String paymentMethod;

	private String paymentSubMethod;

}
