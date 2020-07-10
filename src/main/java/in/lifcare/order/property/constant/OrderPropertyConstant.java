package in.lifcare.order.property.constant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("singleton")
public class OrderPropertyConstant {

	public static Integer BULK_ORDER_DAYS;

	public static Double DEFAULT_CONSUMPTION;
	
	public static Integer MAX_ORDERED_QTY;
	
	public static Integer MAX_BULK_ORDERED_QTY;

	public static Integer PAYMENT_CONFIRMATION_TIME;

	public static Integer PAYMENT_CANCELLATION_TIME;	

	public static String CUSTOMER_CARE_NUMBER_FACILITY_100;

	public static String CUSTOMER_CARE_NUMBER_FACILITY_101;
	
	public static String CUSTOMER_CARE_NUMBER_FACILITY_121;
	
	public Integer getBulkOrderDays() {
		return BULK_ORDER_DAYS;
	}

	public Double getDefaultConsumption() {
		return OrderPropertyConstant.DEFAULT_CONSUMPTION;
	}
	
	@Autowired//
    public OrderPropertyConstant(
    		@Value("${bulk-ordered.days}") Integer bulkOrderDays,
    		@Value("${default-consumption}") Double defaultConsumption,
    		@Value("${default-max-order-qty}") Integer maxOrderQty,
    		@Value("${default-bulk-max-order-qty}") Integer bulkMaxOrderQty,
    		@Value("${payment-confirmation-time}") Integer paymentConfirmationTime,
    		@Value("${payment-cancellation-time}") Integer paymentCancellationTime,
    		@Value("${customer-care-number.facility-100}") String customerCareNumFacility100,
    		@Value("${customer-care-number.facility-101}") String customerCareNumFacility101) {
		OrderPropertyConstant.BULK_ORDER_DAYS = bulkOrderDays;
		OrderPropertyConstant.DEFAULT_CONSUMPTION = defaultConsumption;
		OrderPropertyConstant.MAX_ORDERED_QTY = maxOrderQty;
		OrderPropertyConstant.MAX_BULK_ORDERED_QTY = bulkMaxOrderQty;
		OrderPropertyConstant.PAYMENT_CONFIRMATION_TIME = paymentConfirmationTime;
		OrderPropertyConstant.PAYMENT_CANCELLATION_TIME = paymentCancellationTime;
		OrderPropertyConstant.CUSTOMER_CARE_NUMBER_FACILITY_100 = customerCareNumFacility100;
		OrderPropertyConstant.CUSTOMER_CARE_NUMBER_FACILITY_101 = customerCareNumFacility101;
		OrderPropertyConstant.CUSTOMER_CARE_NUMBER_FACILITY_121 = customerCareNumFacility101;
    }
	
}
