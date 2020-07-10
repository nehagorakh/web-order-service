package in.lifcare.order.microservice.shipping.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import in.lifcare.core.constant.APIEndPoint;
import in.lifcare.core.response.model.Response;
import in.lifcare.core.util.MicroserviceClient;
import in.lifcare.order.microservice.shipping.model.PlacePincode;
import in.lifcare.order.microservice.shipping.model.Shipment;
import in.lifcare.order.microservice.shipping.service.ShippingService;

@SuppressWarnings({"rawtypes"})
@Service
public class ShippingServiceImpl implements ShippingService {

	@Autowired
	public ShippingServiceImpl(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}
	
	@Override
	@Cacheable(value="order_shipping_place_pincode")
	public PlacePincode getPlaceInformationByPincode(String pincode) {
		if (pincode == null) {
			throw new IllegalArgumentException("Invalid pincode specified");
		}
		try {
			Response<?> response = microserviceClient.getForObject(APIEndPoint.SHIPPING_SERVICE + "/place/pincode/" + pincode, Response.class);
			if ( response.getPayload() == null ) {
				throw new IllegalArgumentException("No response received from shipping service");
			}
			return (PlacePincode) response.populatePayloadUsingJson(PlacePincode.class);
		} catch (Exception e) {
			LOGGER.error("Error occured while fetching place info : pincode : " + pincode + " : Exception : " + e.getMessage());
			throw e;
		}
	}
	

	@SuppressWarnings("unchecked")
	@Override
	public List<String> getSkusDeliverOnPincode(List<String> skus, String state, String pincode) {
		Assert.notEmpty(skus, "Sku list can not be null or empty");
		Assert.hasLength(state, "State can not be null or empty");
		Assert.hasLength(pincode, "Pincode can not be null or empty");
		String targetUrl = UriComponentsBuilder.fromUriString(APIEndPoint.SHIPPING_SERVICE)
				.path("/item/pincode/" + pincode).queryParam("sku-ids", StringUtils.join(skus, ","))
				.queryParam("state", state).build().toUriString();

		ResponseEntity<Response> response = restTemplate.exchange(targetUrl, HttpMethod.GET, null, Response.class);
		if (response.getBody() != null && response.getBody().getPayload() != null) {
			return (List<String>) response.getBody().getPayload();
		}
		return new ArrayList<>();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public HashMap<String, Object> getDeliveryType(String pincode, Integer facilityId,
			Map<String,Integer> skusQtyList) {
		Assert.hasLength(pincode, "Pincode can not be null or empty");
		String targetUrl = UriComponentsBuilder.fromUriString(APIEndPoint.SHIPPING_SERVICE)
				.path("/delivery-type/pincode/" + pincode).queryParam("facility-id", facilityId).build().toUriString();
		Response<?> response = microserviceClient.postForObject(targetUrl, skusQtyList, Response.class);
		if (response != null && response.getPayload() != null) {
			return (HashMap<String, Object>) response.populatePayloadUsingJson(Object.class);
		}
		return new HashMap<String, Object>();
	}
	
	@Override
	public List<Shipment> getAllShipmentsByOrderIdIn(List<Long> orderIds) {
		if (orderIds == null || orderIds.isEmpty()) {
			throw new IllegalArgumentException();
		}
		try {
			String orderIdString = orderIds.parallelStream().map(number -> String.valueOf(number)).collect(Collectors.joining(","));
			
			String targetUrl = UriComponentsBuilder.fromUriString(APIEndPoint.SHIPPING_SERVICE).path("/shipment/all").queryParam("order-ids", orderIdString).build().toUriString();
			System.out.println(targetUrl);
			Response<?> response = microserviceClient.getForObject(targetUrl, Response.class);
			if (response != null && response.getPayload() != null) {

				Shipment[] shipments = (Shipment[]) response.populatePayloadUsingJson(Shipment[].class);

				return Arrays.asList(shipments);
			}
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}
		return null;
	}

	private final RestTemplate restTemplate;

	private static final Logger LOGGER = LoggerFactory.getLogger(ShippingServiceImpl.class);
	
	@Autowired
	private MicroserviceClient<Response> microserviceClient;

}
