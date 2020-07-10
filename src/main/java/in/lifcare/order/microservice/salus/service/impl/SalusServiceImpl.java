package in.lifcare.order.microservice.salus.service.impl;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import in.lifcare.core.constant.APIEndPoint;
import in.lifcare.core.model.OrderStateStatusRequest;
import in.lifcare.core.response.model.Response;
import in.lifcare.core.util.MicroserviceClient;
import in.lifcare.order.microservice.salus.service.SalusService;

@SuppressWarnings({"unchecked", "rawtypes"})
@Service
public class SalusServiceImpl implements SalusService {

	@Override
	public Boolean markOrderPaymentConfirmed(long orderId) {
		try {
			Response<Boolean> response = microserviceClient.patchForObject(APIEndPoint.SALUS + "/" + orderId + "/mark-payment-confirmed", null, Response.class);
			if ( response != null && response.getPayload() != null && response.getPayload()) {
				return true;
			}
		} catch (Exception e) {
			LOGGER.error("Error in marking order : {} payment confirmed : Exception : {}",orderId, e.getMessage());
		}
		return false;
	}
	
	@Override
	public Boolean markOrderPrescriptionProcessed(long orderId) {
		try {
			Response<Boolean> response = microserviceClient.patchForObject(APIEndPoint.SALUS + "/" + orderId + "/mark-prescription-processed", null, Response.class);
			if ( response != null && response.getPayload() != null && response.getPayload()) {
				return true;
			}
		} catch (Exception e) {
			LOGGER.error("Error in marking order : {} prescription processed : Exception : {}", orderId, e.getMessage());
		}
		return false;
	}
	
	@Override
	public Boolean orderStatusChange(Map<Long, OrderStateStatusRequest> orderOrderStateStatusRequests) {
		for (Long orderId : orderOrderStateStatusRequests.keySet()) {
			orderStatusChange(orderId, orderOrderStateStatusRequests.get(orderId));
		}
		return true;
	}

	@Override
	public Boolean orderStatusChange(Long orderId, OrderStateStatusRequest orderStateStatusRequest) {
		try {
			Response<Boolean> response = microserviceClient.patchForObject(APIEndPoint.SALUS + "/" + orderId + "/status", orderStateStatusRequest, Response.class);
			if (response != null && response.getPayload() != null && response.getPayload()) {
				return true;
			}
		} catch (Exception e) {
			LOGGER.error("Error in marking order : {} prescription processed : Exception : {}", orderId, e.getMessage());
		}
		return false;
	}
	
	@Override
	public Boolean updateLabOrderStateStatus(long orderId, OrderStateStatusRequest orderStateStatusRequest) {
		try {
			Response<Boolean> response = microserviceClient.patchForObject(
					APIEndPoint.SALUS + "/" + orderId + "/lab-order-status", orderStateStatusRequest, Response.class);
			if (response != null && response.getPayload() != null && response.getPayload()) {
				return true;
			}
		} catch (Exception e) {
			LOGGER.error("Error in updating order : {}  status  : Exception : {}", orderId, e.getMessage());
		}
		return false;
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(SalusServiceImpl.class);
	
	@Autowired
	private MicroserviceClient<Response> microserviceClient;
	
}