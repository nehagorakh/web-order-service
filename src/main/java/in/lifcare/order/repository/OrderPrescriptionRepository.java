package in.lifcare.order.repository;

import java.util.List;

import in.lifcare.order.model.OrderPrescription;

import org.springframework.data.repository.CrudRepository;

public interface OrderPrescriptionRepository extends CrudRepository<OrderPrescription, Long> {
	
	void deleteByOrderId(long orderId);
	
	List<OrderPrescription> findByOrderId(long orderId);
	
	OrderPrescription findTopByOrderIdAndPrescriptionId(long orderId, long prescriptionId);

}
