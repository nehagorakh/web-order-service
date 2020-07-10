package in.lifcare.order.cart.service;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestTemplate;


import in.lifcare.order.BaseTestCase;

public class TestCheck extends BaseTestCase {

		@Test
		public void addCartPrescriptionTest() {
			
			Map<String, Integer> xx = new LinkedHashMap<String, Integer>();
			xx.put("mac", 10);
			restTemplate.postForEntity("http://localhost:9002/v1/account/caller-info/test", xx, Map.class);

		}
		
		@Autowired
		private RestTemplate restTemplate;
}
