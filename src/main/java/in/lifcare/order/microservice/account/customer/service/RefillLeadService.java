package in.lifcare.order.microservice.account.customer.service;

import org.springframework.data.domain.Page;

import in.lifcare.order.microservice.account.customer.model.RefillLead;

public interface RefillLeadService {

	Page<RefillLead> getRefillLead(long customerId);

}
