package in.lifcare.order.service.impl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import in.lifcare.core.constant.APIEndPoint;
import in.lifcare.core.exception.NotFoundException;
import in.lifcare.core.response.model.Response;
import in.lifcare.core.util.MicroserviceClient;
import in.lifcare.order.model.ShippingAddress;
import in.lifcare.order.repository.ShippingAddressRepository;
import in.lifcare.order.service.OrderShippingAddressService;

@Service
public class OrderShippingAddressServiceImpl implements OrderShippingAddressService {

	public ShippingAddress saveOrderShippingAddress(long orderId, long customerId, long shippingAddressId, String mobile, String email) throws Exception {

		/*
		 * ParameterizedTypeReference<Response<ShippingAddress>> myBean = new
		 * ParameterizedTypeReference<Response<ShippingAddress>>() { };
		 * ResponseEntity<Response<ShippingAddress>> responseEntity =
		 * restTemplate.exchange(APIEndPoint.ACCOUNT_SERVICE + "/customer/" +
		 * customerId + "/shipping-address/" + shippingAddressId,
		 * HttpMethod.GET, null, myBean);
		 * 
		 * Response<ShippingAddress> response = responseEntity.getBody();
		 * 
		 * ShippingAddress shippingAddress = (ShippingAddress)
		 * response.populatePayloadUsingJson(ShippingAddress.class);
		 * 
		 * shippingAddress.setOrderId(orderId); return
		 * shippingAddressRepository.save(shippingAddress);
		 */

		/*HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("Authorization", "Bearer 7959c30f-d4f6-4a1c-ae14-232e052b5341");

		HttpEntity<String> entity = new HttpEntity<String>(headers);
		
		@SuppressWarnings("rawtypes")
		ResponseEntity<Response> responseEntity = restTemplate.exchange(APIEndPoint.ACCOUNT_SERVICE + "/customer/" + customerId + "/shipping-address/" + shippingAddressId, HttpMethod.GET, entity, Response.class);

		Response<?> response = responseEntity.getBody();
		@SuppressWarnings("unchecked")*/
		Response<?> response = microserviceClient.getForObject(APIEndPoint.ACCOUNT_SERVICE + "/customer/" + customerId + "/shipping-address/" + shippingAddressId, Response.class);

		ShippingAddress shippingAddress = (ShippingAddress) response.populatePayloadUsingJson(ShippingAddress.class);

		shippingAddress.setId(0);
		shippingAddress.setOrderId(orderId);
		if (StringUtils.isNotBlank(mobile)) shippingAddress.setMobile(mobile);
		if (StringUtils.isNotBlank(email)) shippingAddress.setEmail(email);

		return shippingAddressRepository.save(shippingAddress);

	}
	
	@Override
	public ShippingAddress getShippingAddressByShippingAddressId(long shippingAddressId) {
		if(shippingAddressId <= 0){
			throw new IllegalArgumentException("invalied order-id provided : " + shippingAddressId);
		}
		ShippingAddress shippingAddress = shippingAddressRepository.findTopById(shippingAddressId);
		if(shippingAddress != null){
			return shippingAddress;
		}
		throw new NotFoundException("shipping address not found for order-id : " + shippingAddressId);
	}

	public ShippingAddress findByOrderId(long orderId) {
		if(orderId <= 0){
			throw new IllegalArgumentException("invalied order-id provided : " + orderId);
		}
		ShippingAddress shippingAddress = shippingAddressRepository.findTopByOrderId(orderId);
		if(shippingAddress != null){
			return shippingAddress;
		}
		throw new NotFoundException("shipping address not found for order-id : " + orderId);
	}
	
	@Override
	public ShippingAddress save(ShippingAddress shippingAddress) {
		if (shippingAddress == null) {
			throw new IllegalArgumentException("Invalid param provided!");
		}
		return shippingAddressRepository.save(shippingAddress);
	}

	@Autowired
	private ShippingAddressRepository shippingAddressRepository;
	
	@SuppressWarnings("rawtypes")
	@Autowired
	private MicroserviceClient<Response> microserviceClient;

}
