package in.lifcare.order.microservice.account.customer.service.impl;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import in.lifcare.core.constant.APIEndPoint;
import in.lifcare.core.exception.ApplicationGenericException;
import in.lifcare.core.response.model.Response;
import in.lifcare.core.util.MicroserviceClient;
import in.lifcare.order.microservice.account.customer.model.RefillLead;
import in.lifcare.order.microservice.account.customer.service.RefillLeadService;

@SuppressWarnings({"rawtypes", "unchecked"})
@Service
public class RefillLeadServiceImpl implements RefillLeadService {

	@Override
	public Page<RefillLead> getRefillLead(long customerId) {
		UriComponentsBuilder targetUrl = UriComponentsBuilder.fromUriString(APIEndPoint.ACCOUNT_SERVICE).path("/refill/lead/customer/" + customerId);
		try {
			System.out.println(targetUrl.build().toString());
			ResponseEntity<Response> response = microserviceClient.exchange(targetUrl.build().toString(), HttpMethod.GET, null, Response.class);
			Page<RefillLead> refillLeads = (Page<RefillLead>) response.getBody().populatePageableUsingJson(RefillLead.class);
			return refillLeads;
		} catch (ApplicationGenericException e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}
	
	private Logger log = Logger.getLogger(RefillLeadService.class);
	
	@Autowired
	private MicroserviceClient<Response> microserviceClient;
}
