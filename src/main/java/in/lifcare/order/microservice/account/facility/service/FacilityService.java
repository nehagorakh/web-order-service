package in.lifcare.order.microservice.account.facility.service;

import java.util.List;
import java.util.Map;

import in.lifcare.core.model.FacilityPincodeMapping;

public interface FacilityService {

	long getFacilityId(String city) throws Exception;

	FacilityPincodeMapping getFacilityPincodeMapping(String pincode) throws Exception;

	List<Map<String, Object>> getFacilityByCategoryAndIds(List<String> category, List<Long> facilityIds);

}
