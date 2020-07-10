package in.lifcare.order.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Data;

/**
 * @author rahul
 *
 */
@Data
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderDeliveryObject {

	private String deliveryOption;

	private String serviceType;

	private String deliveryOptionChangeReason;

	private String serviceTypeChangeReason;
	
	private float urgentDeliveryCharge;
	
	private float deliveryCharge;

	public interface DELIVERY_OPTION_CHANGE_REASON {
		String TIME_LIMIT_EXCEEDED = "TIME_LIMIT_EXCEEDED";
		String ORDER_COUNT_EXCEEDED = "ORDER_COUNT_EXCEEDED";
		String PINCODE_IS_NOT_APPLICABLE_FOR_URGENT_DELIVERY = "PINCODE_IS_NOT_APPLICABLE_FOR_URGENT_DELIVERY";
		String COMMITED_DELIVERY_TIME_LINE_EXCEEDED = "COMMITED_DELIVERY_TIME_LINE_EXCEEDED";
		String NEWLY_ADDED_ITEMS_NOT_APPLICABLE_FOR_URGENT_DELIVERY = "NEWLY_ADDED_ITEMS_NOT_APPLICABLE_FOR_URGENT_DELIVERY";
	}
	
	public interface SERVICE_TYPE_CHANGE_REASON {
		String TIME_LIMIT_EXCEEDED = "TIME_LIMIT_EXCEEDED";
		String ORDER_COUNT_EXCEEDED = "ORDER_COUNT_EXCEEDED";
		String PINCODE_IS_NOT_APPLICABLE_FOR_LF_ASSURED = "PINCODE_IS_NOT_APPLICABLE_FOR_LF_ASSURED";
		String COMMITED_DELIVERY_TIME_LINE_EXCEEDED = "COMMITED_DELIVERY_TIME_LINE_EXCEEDED";
		String NEWLY_ADDED_ITEMS_NOT_APPLICABLE_FOR_LF_ASSURED = "NEWLY_ADDED_ITEMS_NOT_APPLICABLE_FOR_LF_ASSURED";
	}

}
