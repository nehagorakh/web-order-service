package in.lifcare.order.microservice.payment.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class PaymentChannelData {

	private String name;

	private String method;
	
	private String imageUrl;

	private String description;

	private String tag;

	private List<PaymentChannelDataOption> options;

}
