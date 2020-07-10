/**
 * 
 */
package in.lifcare.order.api;

import java.util.Collections;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import in.lifcare.core.constant.APIEndPoint;
import in.lifcare.order.BaseTestCase;

/**
 * @author Manoj-Mac
 *
 */
public class OrderControllerTest extends BaseTestCase {

	private interface TestAgentUser{
		String testUserName = "manoj.sharma@lifcare.in"; 
		String testUserPassword = "2110"; 
		String accessToken = "Bearer d7505e95-f653-45fe-858e-d705ec85b35a"; 
		String validPermission = "ORDER_VIEW"; 
		String inValidPermission = "ORDER_VIEW_INVALID"; 
	}
	
	private interface TestCustomerUser{
		String testUserName = "7877173949"; 
		String testUserPassword = "1234"; 
		String accessToken = "Bearer c11461a5-ebd6-4073-9c68-3e5906245c62"; 
		String customerId = "100038005"; 
		String orderId = "100399909";
		String orderIdOfDifferntCustomer = "100399910";
	}
	private interface TestMicroserviceUser{
		String testUserName = "order-service"; 
		String testUserPassword = "";
		String accessToken = "Bearer 48eeffda-26d6-4e42-b1f5-3e5c2b2bf8ca";
		Object validPermission = "microservice_client";
		Object inValidPermission = "microservice_client_test";
	}
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}
	
	/**
	 * 
	 * Expected : false 
	 */
	@Test
	public void getOrderTest1(){
		try{
			Object order = getOrderServiceTest(TestCustomerUser.accessToken, TestCustomerUser.orderId);
		}catch (Exception e) {
			Assert.assertTrue(false);
		}
		Assert.assertTrue(true);
	}
	
	/**
	 * Request by Customer
	 * Invailed Access token 
	 * 
	 */
	@Test
	public void getOrderTest2(){
		try{
			Object order = getOrderServiceTest(TestCustomerUser.accessToken + "a", TestCustomerUser.orderId);
		}catch (Exception e) {
			Assert.assertTrue(true);
		}
		Assert.assertTrue(false);
	}
	
	/**
	 * order id from different customer
	 * 
	 */
	@Test
	public void getOrderTest3(){
		try{
			Object order = getOrderServiceTest(TestCustomerUser.accessToken, TestCustomerUser.orderIdOfDifferntCustomer);
		}catch (Exception e) {
			Assert.assertTrue(true);
		}
		Assert.assertTrue(false);
	}
	
	private Object getOrderServiceTest(String accessToken, String urlPostFix){
		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
		headers.add("Authorization", accessToken);
		HttpEntity<String> entity = new HttpEntity<>("parameters", headers);
		return restTemplate.exchange(APIEndPoint.ORDER_SERVICE + "/" + urlPostFix, HttpMethod.GET, entity, Object.class);
	}
	
}
