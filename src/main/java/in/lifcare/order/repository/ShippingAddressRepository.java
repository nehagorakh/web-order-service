package in.lifcare.order.repository;

import in.lifcare.order.model.ShippingAddress;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

public interface ShippingAddressRepository extends CrudRepository<ShippingAddress, Long>{
	
	ShippingAddress findTopByOrderId(long orderId);

	ShippingAddress findTopById(long shippingAddressId);

	List<ShippingAddress> findAllByOrderIdIn(List<Long> childOrderIds);

}
