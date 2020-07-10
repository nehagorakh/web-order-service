package in.lifcare.order.repository;

import in.lifcare.order.model.OrderItem;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

public interface OrderItemRepository extends CrudRepository<OrderItem, Long> {
	
	List<OrderItem> findByOrderId(long orderId);
	
	List<OrderItem> findByOrderIdAndIsActive(long orderId, boolean version);

	List<OrderItem> findByOrderIdOrderByCreatedAtDescIsActiveDesc(long orderId);

	List<OrderItem> findByOrderIdOrderByIsActiveDescCreatedAtDesc(long orderId);

	List<OrderItem> findByOrderIdInAndSkuIn(List<Long> orderIds, List<String> skus);
	
	List<OrderItem> findAllBySkuAndIsProductVerifiedAndStatusIn(String sku, boolean isProductVerified, List<String> statuses);
	
	OrderItem findTopByOrderIdAndSkuAndPatientIdAndIsActive(long orderId, String sku, long patientId, boolean isActive);

	void deleteByIdIn(List<Long> ids);

	List<OrderItem> findByOrderIdIn(List<Long> orderIds);

}
