package in.lifcare.order.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import in.lifcare.order.microservice.payment.model.PaymentGatewayData;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class OrderPaymentGatewayObject {

	private Order order;

	private PaymentGatewayData paymentGateway;

	private Object error;

	private boolean isOrderPlaced;
	
}
