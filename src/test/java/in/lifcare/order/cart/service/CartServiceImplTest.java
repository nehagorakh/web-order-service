package in.lifcare.order.cart.service;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.databind.ObjectMapper;

import in.lifcare.core.response.model.Response;
import in.lifcare.core.util.MicroserviceClient;
import in.lifcare.order.OrderApp;
import in.lifcare.order.cart.model.Cart;
import in.lifcare.order.cart.model.CartItem;
import in.lifcare.order.cart.repository.CartItemRepository;
import in.lifcare.order.cart.repository.CartRepository;
import in.lifcare.order.exception.CartNotFoundException;
import in.lifcare.order.microservice.account.customer.model.Customer;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = { OrderApp.class })
public class CartServiceImplTest {

	Cart cart = new Cart();
	Customer customer = new Customer();
	Cart cart1 = new Cart();

	List<CartItem> cartItems = new ArrayList<CartItem>();
	CartItem cartItem = new CartItem();
	CartItem cartItem2 = new CartItem();

	/*
	 * adding lab cart to customer success
	 */
	@Test
	public void addCartTest() {
		cart.setType("LAB");
		Mockito.when(cartRepository.save(cart)).thenReturn(cart);
		Cart testCart = cartService.addCart(cart);
		assertTrue(testCart.getCategory().equalsIgnoreCase("LAB"));
		// System.out.println(testCart.getCategory() + " " +
		// testCart.getPaymentMethod() + " " + testCart.getType() + " "
		// + testCart.getUserType());
	}

	/*
	 * invalid input expected = IllegalArgumentException
	 */
	@Test(expected = IllegalArgumentException.class)
	public void addCartTest1() {

		cart = null;
		Cart testCart = cartService.addCart(cart);

	}

	/*
	 * cart Validation failed
	 * 
	 */
	@Test(expected = IllegalArgumentException.class)
	public void addCartTest2() {
		cart.setType("Raw");
		Cart testCart = cartService.addCart(cart);
	}

	/*
	 * adding cartItem Success
	 */
	@Test
	public void addCartItemTest() {
		cart.setType("LAB");
		cart.setUid("12345");
		cart.setItemCount(0);
		cart.setCategory(Cart.CATEGORY.LAB);
		cart.setStatus(Cart.STATUS.CREATED);
		cartItem.setCartUid("1234");
		cartItem.setSku("l0001");
		cartItem.setProductCategory(CartItem.PRODUCT_CATEGORY.LAB_TEST);
		// cartItem2.setCartUid("1234");
		cartItem2.setSku("l0004");
		cartItem2.setProductCategory(CartItem.PRODUCT_CATEGORY.LAB_TEST);
		Mockito.when(cartItemRepository.save(cartItem2)).thenReturn(cartItem2);
		Mockito.when(cartRepository.save(cart)).thenReturn(cart);
		Mockito.when(cartRepository.findOneByUid("12345")).thenReturn(cart);
		Mockito.when(cartItemRepository.findTopBySkuAndCartUidAndPatientId("l10001", "1234", null)).thenReturn(cartItem);
		Cart testCart = cartService.addCartItem("12345", cartItem2, true);
		// System.out.println(testCart.getItemCount());
		// System.out.println(testCart);
		// System.out.println(testCart.getCartItems());
		// System.out.println();
		assertTrue(testCart.getItemCount() == 1);
	}

	/*
	 * Adding Medicine type cartItem in Lab type cart expected =
	 * IllegalArgumentException
	 */
	@Test(expected = IllegalArgumentException.class)
	public void addCartItemTest1() {
		cart.setType("LAB");
		cart.setUid("1234");
		cart.setCategory(Cart.CATEGORY.LAB);
		cart.setStatus(Cart.STATUS.CREATED);
		cartItem.setCartUid("1234");
		cartItem.setProductCategory(CartItem.PRODUCT_CATEGORY.MEDICINE);
		Mockito.when(cartRepository.findOneByUid("1234")).thenReturn(cart);
		Cart testCart = cartService.addCartItem("1234", cartItem, true);

	}

	/*
	 * Adding conflicting cartItem to illegal cart type expected =
	 * IllegalArgumentException
	 */
	@Test(expected = IllegalArgumentException.class)
	public void addCartItemTest2() {
		cart.setType(Cart.TYPE.NORMAL);
		cart.setUid("1234");
		cart.setCategory(Cart.CATEGORY.MEDICINE);
		cart.setStatus(Cart.STATUS.CREATED);
		cartItem.setSku("l0001");
		cartItem.setCartUid("1234");
		cartItem.setProductCategory(CartItem.PRODUCT_CATEGORY.LAB_TEST);
		Mockito.when(cartRepository.findOneByUid("1234")).thenReturn(cart);
		Cart testCart = cartService.addCartItem("1234", cartItem, true);
	}
	
	/*
	 * Cart Not available
	 * expected = CartNotFoundException
	 */
	@Test(expected = CartNotFoundException.class)
	public void addCartItemTest3() {
		cart.setType(Cart.TYPE.NORMAL);
		cart.setUid("1234");
		cart.setCategory(Cart.CATEGORY.MEDICINE);
		cart.setStatus(Cart.STATUS.CREATED);
		cartItem.setSku("l0001");
		cartItem.setCartUid("1234");
		cartItem.setProductCategory(CartItem.PRODUCT_CATEGORY.LAB_TEST);
		Mockito.when(cartRepository.findOneByUid("1234")).thenReturn(null);
		Cart testCart = cartService.addCartItem("1234", cartItem, true);
	}


	/*
	 * Conflicting multiple item add test expected = IllegalArgumentException
	 */
	@Test(expected = IllegalArgumentException.class)
	public void addCartItemsTest() {
		cart.setType(Cart.TYPE.NORMAL);
		cart.setUid("1234");
		cart.setCategory(Cart.CATEGORY.LAB);
		cart.setStatus(Cart.STATUS.CREATED);
		cartItem.setSku("l0001");

		cartItem.setProductCategory(CartItem.PRODUCT_CATEGORY.LAB_TEST);

		cartItem2.setSku("l0004");
		cartItem2.setProductCategory(CartItem.PRODUCT_CATEGORY.MEDICINE);
		cartItems.add(cartItem);
		cartItems.add(cartItem2);

		Mockito.when(cartRepository.findOneByUid("1234")).thenReturn(cart);
		Cart testCart = cartService.addCartItems("1234", cartItems);
	}

	/*
	 * Adding multiple items to the cart
	 */
	@Test
	public void addCartItemsTest1() {
		cart.setType(Cart.TYPE.NORMAL);
		cart.setUid("1234");
		cart.setCategory(Cart.CATEGORY.LAB);
		cart.setStatus(Cart.STATUS.CREATED);
		cartItem.setSku("l0001");

		cartItem.setProductCategory(CartItem.PRODUCT_CATEGORY.LAB_TEST);

		cartItem2.setSku("l0004");
		cartItem2.setProductCategory(CartItem.PRODUCT_CATEGORY.LAB_TEST);
		cartItems.add(cartItem);
		cartItems.add(cartItem2);

		Mockito.when(cartRepository.save(cart)).thenReturn(cart);
		Mockito.when(cartItemRepository.save(cartItems)).thenReturn(cartItems);
		Mockito.when(cartRepository.findOneByUid("1234")).thenReturn(cart);
		Cart testCart = cartService.addCartItems("1234", cartItems);
		// System.out.println(testCart);
		// System.out.println(testCart.getCartItems());
		assertTrue(testCart.getItemCount() == 2);
	}

	/*
	 * Adding lab Items in Medicine type cart expected = IllegalArgumentException
	 */
	@Test(expected = IllegalArgumentException.class)
	public void addCartItemsTest2() {
		cart.setType(Cart.TYPE.NORMAL);
		cart.setUid("1234");
		cart.setCategory(Cart.CATEGORY.MEDICINE);
		cart.setStatus(Cart.STATUS.CREATED);
		cartItem.setSku("l0001");

		cartItem.setProductCategory(CartItem.PRODUCT_CATEGORY.LAB_TEST);

		cartItem2.setSku("l0004");
		cartItem2.setProductCategory(CartItem.PRODUCT_CATEGORY.LAB_TEST);
		cartItems.add(cartItem);
		cartItems.add(cartItem2);

		Mockito.when(cartRepository.save(cart)).thenReturn(cart);
		Mockito.when(cartItemRepository.save(cartItems)).thenReturn(cartItems);
		Mockito.when(cartRepository.findOneByUid("1234")).thenReturn(cart);
		Cart testCart = cartService.addCartItems("1234", cartItems);
	}

	/*
	 * Adding Medicine Items in lab type cart expected = IllegalArgumentException
	 */
	@Test(expected = IllegalArgumentException.class)
	public void addCartItemsTest3() {
		cart.setType(Cart.TYPE.NORMAL);
		cart.setUid("1234");
		cart.setCategory(Cart.CATEGORY.LAB);
		cart.setStatus(Cart.STATUS.CREATED);
		cartItem.setSku("l0001");

		cartItem.setProductCategory(CartItem.PRODUCT_CATEGORY.MEDICINE);

		cartItem2.setSku("l0004");
		cartItem2.setProductCategory(CartItem.PRODUCT_CATEGORY.MEDICINE);
		cartItems.add(cartItem);
		cartItems.add(cartItem2);

		Mockito.when(cartRepository.save(cart)).thenReturn(cart);
		Mockito.when(cartItemRepository.save(cartItems)).thenReturn(cartItems);
		Mockito.when(cartRepository.findOneByUid("1234")).thenReturn(cart);
		Cart testCart = cartService.addCartItems("1234", cartItems);
	}

	/*
	 * Updating cart Values
	 */
	@Test
	public void updateCartParamTest1() throws Exception {
		cartItem.setCartUid("1111");
		cartItem.setSku("l0001");
		cartItem.setProductCategory(CartItem.PRODUCT_CATEGORY.LAB_TEST);
		cart.setCategory(Cart.CATEGORY.LAB);
		cart.setCustomerFirstName("firstname");
		cart.setCustomerId((long) 12345);
		//cart.setType(Cart.TYPE.);
		cart.setUid("1111");
		cart.setStatus(Cart.STATUS.CREATED);
		cart.setItemCount(3);
		cart.setPatientId((long) 12345);
		cart.setOrderId((long) 123);
		cart.setCartItems(Arrays.asList(cartItem));
		Map<String, Object> params = new HashMap<>();
		params.put("customer_first_name", "Rahul Singh");
		Mockito.when(cartRepository.findOneByUid("1111")).thenReturn(cart);
		Mockito.when(cartRepository.save(cart)).thenReturn(cart);
		Mockito.when(cartItemRepository.findByCartUid("1111")).thenReturn(Arrays.asList(cartItem));
		Cart testCart = cartService.updateCartParameters("1111", params);
		assertTrue(testCart.getCustomerFirstName().equals("Rahul Singh"));
		System.out.println(mapper.writeValueAsString(testCart));

	}

	
	
	/*
	 * Resetting cart values
	 */
	@Test
	public void resetCartTest() {
		cartItem.setCartUid("1111");
		cartItem.setSku("l0001");
		cartItem.setProductCategory(CartItem.PRODUCT_CATEGORY.LAB_TEST);
		cart.setCategory(Cart.CATEGORY.LAB);
		cart.setCustomerFirstName("firstname");
		cart.setCustomerId((long) 12345);
		cart.setUid("1111");
		cart.setStatus(Cart.STATUS.CREATED);
		cart.setItemCount(3);
		cart.setPatientId((long) 12345);
		cart.setOrderId((long) 123);
		cart.setCartItems(Arrays.asList(cartItem));

		Mockito.when(cartRepository.findOneByUid("1111")).thenReturn(cart);
		Mockito.when(cartRepository.save(cart)).thenReturn(cart);
		Mockito.when(cartItemRepository.findByCartUid("1111")).thenReturn(Arrays.asList(cartItem));
		Cart testCart = cartService.resetCart("1111", (long) 12345);
		assertTrue(testCart.getCustomerFirstName() == null);
		// System.out.println(testCart);
	}

	
	/*
	 * Merging new cart to the previous one
	 */
	@Test
	public void transferCartTest() {
		customer.setFirstName("firstName");
		customer.setEmail("r333@gmail.com");
		cartItem.setCartUid("1111");
		cartItem.setSku("l0001");
		cartItem.setProductCategory(CartItem.PRODUCT_CATEGORY.LAB_TEST);

		cartItem2.setSku("l0004");
		cartItem2.setProductCategory(CartItem.PRODUCT_CATEGORY.LAB_TEST);

		cart1.setCategory(Cart.CATEGORY.LAB);
		cart1.setCustomerFirstName("firstname");
		cart1.setCustomerId((long) 12345);
		cart1.setUid("1112");
		cart1.setStatus(Cart.STATUS.CREATED);
		cart1.setItemCount(3);
		cart1.setPatientId((long) 12345);
		cart1.setOrderId((long) 123);
		cart1.setCartItems(Arrays.asList(cartItem2));
		cart1.setUserType(Cart.USER_TYPE.EXTERNAL);

		cart.setCategory(Cart.CATEGORY.LAB);
		cart.setCustomerFirstName("firstname");
		cart.setCustomerId((long) 12345);
		cart.setUid("1111");
		cart.setStatus(Cart.STATUS.CREATED);
		cart.setItemCount(3);
		cart.setPatientId((long) 12345);
		cart.setOrderId((long) 123);
		cart.setCartItems(Arrays.asList(cartItem));
		cart.setUserType(Cart.USER_TYPE.EXTERNAL);
		customerResponse.setPayload(customer);
	//	Mockito.when(cartRepository.findTopByCustomerIdAndStatusAndTypeAndUserTypeOrderByCreatedAtDesc((long) 12345,
	//			Cart.STATUS.CREATED, cart1.getType(), cart1.getUserType())).thenReturn(cart1);
		Mockito.when(microServiceClient.getForObject(Mockito.anyString(), Matchers.any(Class.class)))
				.thenReturn(customerResponse);
		Mockito.when(cartRepository.findOneByUid("1112")).thenReturn(cart1);
		Mockito.when(cartRepository.findOneByUid("1111")).thenReturn(cart);
		Mockito.when(cartRepository.save(cart)).thenReturn(cart);
		Mockito.when(cartItemRepository.findByCartUid("1111")).thenReturn(Arrays.asList(cartItem));
		Cart testCart = cartService.transferCart("1111", (long) 12345);
		// System.out.println(testCart);

		assertTrue(testCart.getItemCount() == 4);

	}
	
	/*anonymous cart
	 * creating cart
	 * adding cartItem 
	 * merging cartItems with The previous cart
	 */
	@Test
	public void cartToOrderTest() throws Exception
	{
		Cart testCart;
		
	//	cart.setType(Cart.TYPE.LAB);
		cart.setUid("1234");
		Mockito.when(cartRepository.save(cart)).thenReturn(cart);
		Mockito.when(cartRepository.findOneByUid("1234")).thenReturn(cart);

		testCart = cartService.addCart(cart);
		testCart.setUid("1122");
		
		Mockito.when(cartRepository.save(testCart)).thenReturn(testCart);
		Mockito.when(cartRepository.findOneByUid("1122")).thenReturn(testCart);
		Mockito.when(cartItemRepository.findTopBySkuAndCartUidAndPatientId("I404", "1122", null)).thenReturn(null);
		Mockito.when(cartItemRepository.findByCartUid("1122")).thenReturn(Arrays.asList(cartItem));
		
		cartItem.setProductCategory(CartItem.PRODUCT_CATEGORY.LAB_TEST);
		cartItem.setSku("I404");
		
		testCart = cartService.addCartItem("1122", cartItem, true);
		
		cartItem2.setSku("I403");
		cartItem2.setProductCategory(CartItem.PRODUCT_CATEGORY.LAB_TEST);

	//	cart1.setType(Cart.TYPE.LAB);
		cart1.setCustomerFirstName("firstname");
		cart1.setCustomerId((long) 12345);
		cart1.setUid("1235");
		cart1.setItemCount(1);
		cart1.setUserType(Cart.USER_TYPE.EXTERNAL);
		cart1.setStatus(Cart.STATUS.CREATED);
		cart1.setCartItems(Arrays.asList(cartItem2));
		customer.setFirstName("firstName");
		customer.setEmail("r333@gmail.com");
		customer.setId((long)12345);
		customerResponse.setPayload(customer);

		Mockito.when(cartRepository.findOneByUid("1122")).thenReturn(testCart);
		Mockito.when(cartRepository.findOneByUid("1235")).thenReturn(cart1);
		//Mockito.when(cartRepository.findTopByCustomerIdAndStatusAndTypeAndUserTypeOrderByCreatedAtDesc((long) 12345,
		//		Cart.STATUS.CREATED, cart1.getType(), cart1.getUserType())).thenReturn(cart1);
		Mockito.when(microServiceClient.getForObject(Mockito.anyString(), Matchers.any(Class.class)))
				.thenReturn(customerResponse);
		Mockito.when(cartItemRepository.findByCartUid("1122")).thenReturn((testCart.getCartItems()));

		Mockito.when(cartRepository.save(testCart)).thenReturn(testCart);
	

		testCart = cartService.transferCart("1122",(long)12345);
		 
		assertTrue(testCart.getItemCount()==2);

		
	}

	@MockBean
	MicroserviceClient microServiceClient;
	Response<Customer> customerResponse = new Response<>();

	@MockBean
	CartRepository cartRepository;

	@MockBean
	CartItemRepository cartItemRepository;

	ObjectMapper mapper = new ObjectMapper();

	@Autowired
	CartService cartService;

}
