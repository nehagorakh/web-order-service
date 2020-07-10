package in.lifcare.order.model;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderPrice {
	
	private float totalSalePrice;
	
	private float totalMrp;

	private float totalTaxAmount;

	private float couponDiscount;
	
	private int redeemedCarePoints;

	private float totalDiscount;

	private float discount;
	
	private float shippingCharge;
	
	private double urgentDeliveryCharge;
	
	private float redeemedCash;
	
	private Double externalInvoiceAmount;
	
	private double podAmount;

	private double gatewayAmount;
	
	private Map<Integer, List<OrderItem>> subOrderItems;

}
