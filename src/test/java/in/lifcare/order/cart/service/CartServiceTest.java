package in.lifcare.order.cart.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;

import in.lifcare.order.BaseTestCase;
import in.lifcare.order.cart.api.CartController;
import in.lifcare.order.cart.model.Cart;
import in.lifcare.order.cart.model.CartItem;
import in.lifcare.order.cart.model.CartPrescription;
import in.lifcare.order.service.OrderPrescriptionService;

public class CartServiceTest extends BaseTestCase {
	
	//@Test
	public void addCartTest() {
		Cart cart = new Cart();
		cart.setSource("MWEB");
		cart.setFacilityCode(101);
		cart = cartService.addCart(cart);
		System.out.println(cart.getUid());
	}

	//@Test
	public void addCartItemsTest() {
		String cartUid = "81a6b421-96d5-46cf-ac4a-b1d8f7254b2e";
		List<CartItem> cartItems = new ArrayList<CartItem>();
		for(int i=0;i<10;i++) {
			CartItem cartItem = new CartItem();
			cartItem.setBrand("TEST-BRAND" + i);
			cartItem.setCartUid(cartUid);
			cartItem.setDrugStrength("50" + i + " MG");
			cartItem.setMrp(10+i);
			cartItem.setTax(1 + i);
			cartItem.setSku("1110" + i);
			cartItem.setName("TEST-MEDICINE-"+i);
			cartItem.setPerPackQty(10131 + i);
			cartItem.setType("TABLET-"+i);
			cartItems.add(cartItem);
		}
		cartService.replaceCartItems(cartUid, cartItems, Boolean.FALSE);
	}

	//@Test
	public void addCartItemsTest2() {
		String cartUid = "81a6b421-96d5-46cf-ac4a-b1d8f7254b2e";
		List<CartItem> cartItems = new ArrayList<CartItem>();
		CartItem cartItem = new CartItem();
		cartItem.setSku("047235");
		cartItems.add(cartItem);
		
		cartItem = new CartItem();
		cartItem.setSku("274842");
		cartItems.add(cartItem);
		cartService.replaceCartItems(cartUid, cartItems, Boolean.FALSE);
	}

	//@Test
	public void addCartPrescriptionTest() {

	}

	//@Test
	public void fetchCartByUidTest() throws Exception {
		String cartUid = "f6035a02-2e05-403f-9a95-255ecab2f6cc";
		Cart cart = cartService.fetchCartByUid(cartUid);
		System.out.println(mapper.writeValueAsString(cart)) ;
	}

	//@Test
	public void transferCartTest() {
		String cartUid = "81a6b421-96d5-46cf-ac4a-b1d8f7254b2e";
		Long customerId = 100183363L;
		cartService.transferCart(cartUid, customerId);
	}

	//@Test
	public void cartTransferController() throws Exception {
		String cartUid = "81a6b421-96d5-46cf-ac4a-b1d8f7254b2e";
		Long customerId = 100183363L;
		Map<String, Object> customerParam = new HashMap<String, Object>();
		customerParam.put("customer_id", customerId);
		Long id = Long.parseLong(String.valueOf(customerParam.get("customer_id")));
		System.out.println("----"+id+"----") ;
	}

	@Test
	public void cartPrescriptionSaveTest() {
		List<Long> prescriptionIds = new ArrayList<Long>();
		prescriptionIds.add(559L);
		prescriptionIds.add(560L);
		List<CartPrescription> cartPrescriptions = cartPrescriptionService.getPrescriptionsByPrescriptionIds(prescriptionIds);
		orderPrescriptionService.saveCartOrderPrescriptions(100457663, cartPrescriptions);
	}
	
	//@Test
	public void addShippingAddressTest() {
	}

	//@Test
	public void discardCartTest() {
	}

	//@Test
	public void updateCartStatusTest() {
	}

	//@Test
	public void getCartByCustomerTest() {
	}

	//@Test
	public void getCartSummaryTest() {
	}

	//@Test
	public void deleteCartItemTest() {
	}

	private ObjectMapper mapper = new ObjectMapper();
	
	@Autowired
	private CartService cartService;

	@Autowired
	private OrderPrescriptionService orderPrescriptionService;

	@Autowired
	private CartPrescriptionService cartPrescriptionService;
	
}
