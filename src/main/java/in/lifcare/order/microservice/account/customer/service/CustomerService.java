package in.lifcare.order.microservice.account.customer.service;

import java.util.List;
import java.util.Map;

import in.lifcare.core.model.Membership;
import in.lifcare.order.microservice.account.customer.model.Customer;
import in.lifcare.order.microservice.account.patient.model.Patient;
import in.lifcare.order.model.OrderPatientPrescription;
import in.lifcare.order.model.ShippingAddress;

/**
 * 
 * @author karan
 *
 */
public interface CustomerService {

	/**
	 * 
	 * @param customerId
	 * @return
	 */
	public Customer getCustomerDetail(Long customerId);

	/**
	 * 
	 * @param customerId
	 * @param patientId
	 * @return
	 */
	public Patient getPatientByCustomerIdAndPatientId(Long customerId, Long patientId);

	/**
	 * 
	 * @param customerId
	 * @param shippingAddressId
	 * @return
	 */
	public ShippingAddress getShippingAddressByCustomerIdAndShippingAddressId(Long customerId, Long shippingAddressId);

	Map<Long, Patient> getPatients(long customerId, List<Long> patientIds);
	
	List<OrderPatientPrescription> getPrescriptions(long customerId, List<Long> patientIds);

	List<ShippingAddress> getShippingAddressesByCustomerId(Long customerId);

	public List<Membership> getMemberships(Long customerId);
	
}
