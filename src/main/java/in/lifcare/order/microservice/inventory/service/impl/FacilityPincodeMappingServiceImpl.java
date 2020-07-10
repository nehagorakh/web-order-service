package in.lifcare.order.microservice.inventory.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import in.lifcare.core.constant.APIEndPoint;
import in.lifcare.core.model.FacilityPincodeMapping;
import in.lifcare.core.response.model.Response;
import in.lifcare.core.util.MicroserviceClient;
import in.lifcare.order.microservice.inventory.service.FacilityPincodeMappingService;

@Service
public class FacilityPincodeMappingServiceImpl implements FacilityPincodeMappingService {

	@Override
	public List<FacilityPincodeMapping> getPlacePincodeByPincode(String pincode) {
		try {
			Response<List> facilityResponse = microserviceClient.getForObject(APIEndPoint.INVENTORY_SERVICE + "/pincode/" + pincode, Response.class);
			return mapper.readValue(mapper.writeValueAsString(facilityResponse.getPayload()) ,mapper.getTypeFactory().constructCollectionType(List.class, FacilityPincodeMapping.class));
		} catch (Exception e) {
			return null;
		}
	}
	
	private ObjectMapper mapper = new ObjectMapper();
	
	@Autowired
	private MicroserviceClient<Response> microserviceClient;
}
