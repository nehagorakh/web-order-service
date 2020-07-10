package in.lifcare.order.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderItemSummary {

	private String sku;

	private String name;

	private String brand;
	
	private int  quantity;

	private float mrp;

	private float salePrice;
	
}
