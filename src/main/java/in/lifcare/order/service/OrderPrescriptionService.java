package in.lifcare.order.service;

import in.lifcare.order.cart.model.CartPrescription;
import in.lifcare.order.model.OrderPrescription;

import java.util.List;

public interface OrderPrescriptionService {

	List<OrderPrescription> findByOrderId(long orderId);

	List<OrderPrescription> saveOrderPrescriptions(long orderId, long patientId, List<Long> prescriptionIds);

	List<OrderPrescription> saveCartOrderPrescriptions(long orderId, List<CartPrescription> cartPrescriptions);
	
	List<OrderPrescription> updateOrderPrescriptions(long orderId, long patientId, List<Long> prescriptionIds);
	
}
