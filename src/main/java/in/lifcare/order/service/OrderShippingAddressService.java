package in.lifcare.order.service;

import in.lifcare.order.model.ShippingAddress;

public interface OrderShippingAddressService {

	ShippingAddress saveOrderShippingAddress(long orderId, long customerId, long shippingAddressId, String mobile, String email) throws Exception;

	ShippingAddress findByOrderId(long orderId);

	ShippingAddress getShippingAddressByShippingAddressId(long shippingAddressId);
	
	ShippingAddress save(ShippingAddress shippingAddress);

}
