package in.lifcare.order.microservice.account.customer.service.impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import in.lifcare.core.constant.APIEndPoint;
import in.lifcare.core.exception.NotFoundException;
import in.lifcare.core.model.Membership;
import in.lifcare.core.model.PatientMedicine;
import in.lifcare.core.response.model.Response;
import in.lifcare.core.util.CommonUtil;
import in.lifcare.core.util.MicroserviceClient;
import in.lifcare.order.microservice.account.customer.model.Customer;
import in.lifcare.order.microservice.account.customer.service.CustomerService;
import in.lifcare.order.microservice.account.patient.model.Patient;
import in.lifcare.order.model.OrderPatientPrescription;
import in.lifcare.order.model.ShippingAddress;

/**
 * 
 * @author karan
 *
 */
@SuppressWarnings({"rawtypes", "unchecked"})
@Service
public class CustomerServiceImpl implements CustomerService {

	@Override
	public Customer getCustomerDetail(Long customerId) {
		if( customerId == null ) {
			throw new IllegalArgumentException("Invalid parameters : customer id is not specified");
		}
		Response<Customer> customerResponse = microserviceClient.getForObject(APIEndPoint.ACCOUNT_SERVICE + "/customer/" + customerId, Response.class);
		Customer customer = (Customer) customerResponse.populatePayloadUsingJson(Customer.class);
		if (customer == null) {
			throw new NotFoundException("No customer found for customer id : " + customerId);
		}
		return customer;
	}

	@Override
	public Patient getPatientByCustomerIdAndPatientId(Long customerId, Long patientId) {
		if( customerId == null || patientId == null ) {
			throw new IllegalArgumentException("Invalid parameters : customer id or patient id is not specified");
		}
		Response<?> response = microserviceClient.getForObject(APIEndPoint.ACCOUNT_SERVICE + "/customer/" + customerId + "/patient/" + patientId, Response.class);
		Patient patient = (Patient) response.populatePayloadUsingJson(Patient.class);
		if( patient == null ) {
			throw new NotFoundException("No patient found for customer id : " + customerId);
		}
		return patient;
	}

	@Override
	public ShippingAddress getShippingAddressByCustomerIdAndShippingAddressId(Long customerId, Long shippingAddressId) {
		if( customerId == null || shippingAddressId == null ) {
			throw new IllegalArgumentException("Invalid parameters : customer id or shipping address id is not specified");
		}
		Response<?> response = microserviceClient.getForObject(APIEndPoint.ACCOUNT_SERVICE + "/customer/" + customerId + "/shipping-address/" + shippingAddressId, Response.class);
		ShippingAddress shippingAddress = (ShippingAddress) response.populatePayloadUsingJson(ShippingAddress.class);
		if( shippingAddress == null ) {
			throw new NotFoundException("No shipping address found for customer id : " + customerId);
		}
		return shippingAddress;
	}
	
	@Override
	public List<ShippingAddress> getShippingAddressesByCustomerId(Long customerId) {
		if( customerId == null && customerId <= 0) {
			throw new IllegalArgumentException("Invalid parameters : customer id");
		}
		Response<?> response = microserviceClient.getForObject(APIEndPoint.ACCOUNT_SERVICE + "/customer/" + customerId + "/shipping-addresses", Response.class);
		ShippingAddress[] shippingAddresses = (ShippingAddress[]) response.populatePayloadUsingJson(ShippingAddress[].class);
		if( shippingAddresses == null ) {
			throw new NotFoundException("No shipping address found for customer id : " + customerId);
		}
		return Arrays.asList(shippingAddresses);
	}
	
	@Override
	public Map<Long, Patient> getPatients(long customerId, List<Long> patientIds) {
		UriComponentsBuilder targetUrl = UriComponentsBuilder.fromUriString(APIEndPoint.ACCOUNT_SERVICE).path("/customer/" + customerId + "/patients")
				.queryParam("patient-ids", StringUtils.join(patientIds, ','));
		
		ResponseEntity<Response> response = microserviceClient.exchange(targetUrl.build().toString(), HttpMethod.GET, null, Response.class);
		Patient[] patients = (Patient[]) response.getBody().populatePayloadUsingJson(Patient[].class);
		if (patients != null && patients.length > 0) {
			return Arrays.asList(patients).stream().collect(Collectors.toMap(Patient::getId, Function.identity()));
		}
		return new HashMap<Long, Patient>();
	}

	@Override
	public List<OrderPatientPrescription> getPrescriptions(long customerId, List<Long> patientIds) {
		if( patientIds == null || patientIds.isEmpty()) {
			throw new IllegalArgumentException("Invalid parameters : patient ids is not specified");
		}
		UriComponentsBuilder targetUrl = UriComponentsBuilder.fromUriString(APIEndPoint.ACCOUNT_SERVICE).path("/customer/" + customerId + "/patient-prescriptions")
				.queryParam("patient-ids", StringUtils.join(patientIds, ','))
				.queryParam("is-only-active", true)
				.queryParam("prescription-per-patient", 10);
		ResponseEntity<Response> response = microserviceClient.exchange(targetUrl.build().toString(), HttpMethod.GET, null, Response.class);
		Page<OrderPatientPrescription> priscriptions = (Page<OrderPatientPrescription>) response.getBody().populatePageableUsingJson(OrderPatientPrescription.class);
		if (priscriptions != null && priscriptions.getContent().size() > 0) {
			return priscriptions.getContent();
		}
		return null;
	}
	
	@Override
	public List<Membership> getMemberships(Long customerId) {
		String clientId = CommonUtil.getClientFromSession();
		if (customerId != null) {
			UriComponentsBuilder targetUrl = UriComponentsBuilder.fromUriString(APIEndPoint.ACCOUNT_SERVICE).path("/customer/memberships").queryParam("customer-id", customerId).queryParam("client-id",
					clientId);
			ResponseEntity<Response> response = microserviceClient.exchange(targetUrl.build().toString(), HttpMethod.GET, null, Response.class);
			if (response.getBody().getPayload() != null) {
				Membership[] memberships = (Membership[]) response.getBody().populatePayloadUsingJson(Membership[].class);
				if (memberships != null && memberships.length > 0) {
					return Arrays.asList(memberships);
				}
			}
		}
		return null;
	}

	@Autowired
	private MicroserviceClient<Response> microserviceClient;

}
