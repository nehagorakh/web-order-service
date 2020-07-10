package in.lifcare.order.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import in.lifcare.order.model.OrderAdditionalItem;

public interface OrderAdditionalItemRepository extends CrudRepository<OrderAdditionalItem, Long> {

	List<OrderAdditionalItem> findByOrderIdAndMedicineTypeNotInAndStatusNotIn(long orderId, List<String> notAllowedMedicineTypes, List<String> notAllowedStatus);

}
