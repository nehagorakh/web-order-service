package in.lifcare.order.microservice.inventory.service;

import java.util.List;

import in.lifcare.core.model.FacilityPincodeMapping;

public interface FacilityPincodeMappingService {

	List<FacilityPincodeMapping> getPlacePincodeByPincode(String pincode);

}
