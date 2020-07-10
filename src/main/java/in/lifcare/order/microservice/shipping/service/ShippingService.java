package in.lifcare.order.microservice.shipping.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import in.lifcare.order.microservice.shipping.model.PlacePincode;
import in.lifcare.order.microservice.shipping.model.Shipment;

public interface ShippingService {

	/**
	 * 
	 * @param pincode
	 * @return
	 */
	public PlacePincode getPlaceInformationByPincode(String pincode);

    List<String> getSkusDeliverOnPincode(List<String> skus, String facilityCode, String pincode);

    /**
     * 
     * @param pincode
     * @param facilityCode
     * @param skusQtyList
     * @return
     */
	HashMap<String, Object> getDeliveryType(String pincode, Integer facilityCode,
			Map<String,Integer> skusQtyList);
	
	List<Shipment> getAllShipmentsByOrderIdIn(List<Long> orderIds);

}
