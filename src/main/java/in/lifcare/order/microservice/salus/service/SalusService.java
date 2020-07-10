package in.lifcare.order.microservice.salus.service;

import java.util.Map;

import in.lifcare.core.model.OrderStateStatusRequest;

public interface SalusService {

	Boolean markOrderPaymentConfirmed(long orderId);

	Boolean markOrderPrescriptionProcessed(long orderId);

	Boolean orderStatusChange(Map<Long, OrderStateStatusRequest> orderOrderStateStatusRequests);

	Boolean orderStatusChange(Long orderId, OrderStateStatusRequest orderStateStatusRequest);
	
	Boolean updateLabOrderStateStatus(long orderId, OrderStateStatusRequest orderStateStatusRequest);

}
