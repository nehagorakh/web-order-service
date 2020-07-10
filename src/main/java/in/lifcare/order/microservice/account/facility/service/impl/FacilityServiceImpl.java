package in.lifcare.order.microservice.account.facility.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import in.lifcare.core.constant.APIEndPoint;
import in.lifcare.core.exception.FacilityNotFoundException;
import in.lifcare.core.model.FacilityPincodeMapping;
import in.lifcare.core.response.model.Response;
import in.lifcare.core.util.MicroserviceClient;
import in.lifcare.order.microservice.account.facility.service.FacilityService;

@Service
public class FacilityServiceImpl implements FacilityService {
	
	@Override
	public long getFacilityId(String city) throws Exception {
		if (city == null) {
			throw new IllegalArgumentException("Invalid parameters provided");
		}
		try {
			Long response = restTemplate.getForObject(APIEndPoint.ACCOUNT_SERVICE + "/facility/city/" + city,
					Long.class);
			if (response > 0) {
				return response;
			}
			throw new FacilityNotFoundException("Facility can't be found");
		} catch (Exception e) {
			throw e;
		}
	}
	
	@Override
	public FacilityPincodeMapping getFacilityPincodeMapping(String pincode) throws Exception {
		if (StringUtils.isBlank(pincode)) {
			throw new IllegalArgumentException("Invalid parameters provided");
		}
		FacilityPincodeMapping facilityPincodeMapping = null;
		try {
			Response<?> response = microserviceClient.getForObject(APIEndPoint.ORDER_PROCESSING_SERVICE + "/" + pincode,
					Response.class);
			try {
				facilityPincodeMapping = (FacilityPincodeMapping) response
						.populatePayloadUsingJson(FacilityPincodeMapping.class);
				if (facilityPincodeMapping == null) {
					throw new FacilityNotFoundException("Facility can't be found");
				}
			} catch (Exception e) {
				return facilityPincodeMapping;
			}
		} catch (Exception e) {
			throw e;
		}
		return facilityPincodeMapping;
	}
	
	
	@Override
	public List<Map<String, Object>> getFacilityByCategoryAndIds(List<String> category, List<Long> facilityIds) {
		String facilityIdsStr = facilityIds.stream().map(fI -> String.valueOf(fI)).collect(Collectors.joining(","));
		UriComponentsBuilder targetUrl = UriComponentsBuilder.fromUriString(APIEndPoint.ACCOUNT_SERVICE).path("/facility/all")
				.queryParam("classification", String.join(",", category))
				.queryParam("facility-ids", facilityIdsStr);
		@SuppressWarnings("unchecked")
		Response<List<Map<String, Object>>> facilityResponse = microserviceClient.getForObject(targetUrl.build().toString(), Response.class);
		if (facilityResponse == null || facilityResponse.getPayload() == null) {
			return new ArrayList<Map<String, Object>>();
		}
		return facilityResponse.getPayload();
	}
	
	
	@Autowired
	private RestTemplate restTemplate;
	
	@Autowired
	private MicroserviceClient<Response> microserviceClient;

}
