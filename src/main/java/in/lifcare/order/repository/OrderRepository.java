package in.lifcare.order.repository;

import in.lifcare.order.model.Order;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends CrudRepository<Order, Long> {
	
	Page<Order> findAllByCustomerIdAndCategory(long customerId, String category, Pageable pageable);
	
	Page<Order> findByCustomerIdAndCreatedAtGreaterThan(long customerId, Date dateTime, Pageable pageable);

	Long countByCustomerIdAndStateIn(long customerId,List<String> state);
	
	Long countByCustomerIdAndStateNotIn(long customerId,List<String> state);

	Page<Order> findAllByPatientIdAndCategory(long patientId, String category, Pageable pageable);
	
	Order findById(long orderId);

	Order findByDisplayOrderId(String orderId);

	Order findByOrderNumber(String orderId);
	
	Order findByDisplayOrderIdOrId(String orderNumber, Long orderId);
	
	Order findTopByPatientIdAndStatusNotInOrderByIdAsc(long patientId, List<String> statuses);

	Page<Order> findByCustomerIdAndStatusIn(long customerId, List<String> statuses, Pageable pageable);

	Page<Order> findByServiceTypeAndPromisedDeliveryDateBeforeAndStatusIn(String serviceType, Timestamp currentDateTime,
			List<String> statuses, Pageable pageable);

	Page<Order> findByDeliveryOptionAndPromisedDeliveryDateBeforeAndStatusIn(String deliveryOption,
			Timestamp currentDateTime, List<String> statuses, Pageable pageable);
	
	Long countByCustomerIdAndStateInAndStatusIn(long customerId, List<String> shippingChargeApplicableState,
			List<String> shippingChargeApplicableStatus);

	List<Order> findByCustomerIdAndCreatedAtGreaterThan(long customerId, Date createdAt);
	
	List<Order> findByPatientIdAndCreatedAtGreaterThan(long patientId, Date createdAt);

	@Query(value = "select distinct(customer_id) from `order` where created_at>?1", nativeQuery = true)
	List<Long> findDistinctCustomerIdByCreatedAtAfter(Timestamp createdAtAfter);

	Page<Order> findByCreatedAtBetween(Timestamp createdAtBegins, Timestamp createdAtEnds, Pageable pageable);

	List<Order> findAllByParentId(long parentId);

	Page<Order> findAll(Specification<Order> specification, Pageable pageable);

	Order findByParentIdAndId(long parentId, long childOrderId);

	List<Order> findAllByIdIn(List<Long> parentOrderIds);

	Page<Order> findAllByCustomerId(long customerId, Pageable pageable);
	
	List<Order> findByCourierTypeAndPromisedDeliveryDateBeforeAndStatusIn(String courierType, Timestamp currentDateTime,
			List<String> statuses);
	
	List<Order> findAllByCustomerId(long customerId);

	List<Order> findByCourierTypeAndPromisedDeliveryDateBeforeAndStatusInAndStateIn(String courierType,
			Timestamp currentDateTime, List<String> statuses, List<String> state);

	List<Order> findAllByMergeWithId(long orderId);
}
