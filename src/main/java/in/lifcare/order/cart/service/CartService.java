package in.lifcare.order.cart.service;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import in.lifcare.core.model.User;
import in.lifcare.order.cart.model.Cart;
import in.lifcare.order.cart.model.CartItem;
import in.lifcare.order.model.Order;
import in.lifcare.order.model.OrderDeliveryObject;

/**
 * 
 * @author karan
 *
 */
public interface CartService {

	/**
	 * 
	 * @param cart
	 * @return
	 */
	Cart addCart(Cart cart);

	/**
	 * 
	 * @param cart
	 * @return
	 */
	Cart saveCart(Cart cart);
	
	/**
	 * 
	 * @param cartUid
	 * @param cartItems
	 * @param isAutoSeggestionQty TODO
	 * @return
	 */
	Cart replaceCartItems(String cartUid, List<CartItem> cartItems, boolean isAutoSeggestionQty);

	/**
	 * 
	 * @param cartUid
	 * @param cartItem
	 * @return
	 */
	Cart addCartItem(String cartUid, CartItem cartItem, boolean isAllowed);

	/**
	 * 
	 * @param cartUid
	 * @param file
	 * @param cartPrescriptionIds
	 * @param expiryDate 
	 * @param doctorName 
	 * @param rxDate TODO
	 * @param patientId 
	 * @return
	 * @throws Exception
	 */
	Cart addCartPrescription(String cartUid, MultipartFile file, List<String> cartPrescriptionIds, Timestamp expiryDate, String doctorName, Timestamp rxDate, Long patientId) throws Exception;

	/**
	 * 
	 * @param cartUid
	 * @return
	 */
	Cart fetchCartByUid(String cartUid);

	/**
	 * 
	 * @param cartUid
	 * @param customerId
	 * @return
	 */
	Cart transferCart(String cartUid, Long customerId);

	/**
	 * 
	 * @param cartUid
	 * @param shippingAddressId
	 * @param loginCustomerId
	 * @return
	 */
	Cart addShippingAddress(String cartUid, Long shippingAddressId, Long loginCustomerId);

	/**
	 * 
	 * @param cartUid
	 * @return
	 */
	Boolean discardCart(String cartUid);

	/**
	 * 
	 * @param cartUid
	 * @param status
	 * @return
	 */
	Cart updateStatus(String cartUid, String status);

	/**
	 * 
	 * @param customerId
	 * @param productCategory 
	 * @return
	 */
	Cart getCartByCustomer(Long customerId, String productCategory);

	/**
	 * 
	 * @param cartUid
	 * @param loginCustomerId
	 * @param autoApplyCoupon
	 * @return
	 */
	Cart getCartSummary(String cartUid, Long loginCustomerId, boolean autoApplyCoupon);

	/**
	 * 
	 * @param cartUid
	 * @param cartItemId
	 * @return
	 */
	Cart deleteCartItem(String cartUid, Long cartItemId);

	/**
	 * 
	 * @param cart
	 */
	void validateActiveCart(Cart cart);

	/**
	 * 
	 * @param cartUid
	 * @param cartPrescriptionId
	 * @return
	 */
	Cart deleteCartPrescription(String cartUid, Long cartPrescriptionId);

	/**
	 * 
	 * @param cartUid
	 * @param patientId
	 * @param loginCustomerId
	 * @return
	 */
	Cart assignPatientToCart(String cartUid, Long patientId, Long loginCustomerId);

	/**
	 * 
	 * @param cartUidMergeFrom
	 * @param cartUidMergeTo
	 * @return
	 */
	Cart mergeCart(String cartUidMergeFrom, String cartUidMergeTo);

	/**
	 * 
	 * @param cartUid
	 * @param updateParams
	 * @return
	 * @throws Exception
	 */
	Cart updateCartParameters(String cartUid, Map<String, Object> updateParams) throws Exception;

	/**
	 * 
	 * @param cart
	 * @return
	 */
	Cart createRefillCart(Cart cart);

	/**
	 * 
	 * @param cart
	 * @return
	 */
	Cart createJivaCart(Cart cart);
	
	/**
	 * 
	 * @param cartUid
	 * @param cartPrescriptionIds
	 * @return
	 */
	Cart deleteCartPrescriptions(String cartUid, List<Long> cartPrescriptionIds);

	/**
	 * 
	 * @param customerId
	 * @param pageable TODO
	 * @return
	 */
	Page<Cart> getCartByCustomerIdAndType(long customerId, String type, Pageable pageable);

	/**
	 * 
	 * @param cartUids
	 * @return
	 */
	List<Cart> fetchCartsByUids(List<String> cartUids);

	/**
	 * 
	 * @param cartUid
	 * @return
	 */
	Cart getCartByUid(String cartUid);
	
	/**
	 * 
	 * @param cartUid
	 * @param type
	 * @return
	 */
	Cart updateCartType(String cartUid, String type);

	/**
	 * 
	 * @param cartUid
	 * @param isDoctorCallback
	 * @return
	 */
	Cart updateCartIsDoctorCallback(String cartUid, boolean isDoctorCallback);
	 
	/**
	 * 
	 * @param cart
	 */
	void validateCart(Cart cart);

	/**
	 * 
	 * @param customerId
	 * @return
	 */
	Page<Cart> getRefillCarts(long customerId);

	/**
	 * 
	 * @param cartUid
	 * @param couponCode
	 * @param loginCustomerId
	 * @param authClientId 
	 * @return
	 */
	Cart applyCouponForCart(String cartUid, String couponCode, Long loginCustomerId, String authClientId);

	/**
	 * 
	 * @param cartUid
	 * @param couponCode
	 * @param loginCustomerId
	 * @param authClientId 
	 * @return
	 */
	Cart removeCouponForCart(String cartUid, String couponCode, Long loginCustomerId, String authClientId);

	/**
	 * 
	 * @param cartUid
	 * @param facilityCode
	 * @return
	 */
	Cart switchCartFacilityCode(String cartUid, Integer facilityCode);

	/**
	 * 
	 * @param cartUid
	 * @param sku
	 * @return
	 */
	Cart deleteCartItemBySku(String cartUid, String sku);

	/**
	 * 
	 * @param cartUid
	 * @param customerId
	 * @return
	 */
	Cart resetCart(String cartUid, Long customerId);
	
	/**
	 * 
	 * @param refillCartUid
	 * @param loginCustomerId
	 * @return
	 */
	Cart transferRefillCart(String refillCartUid, long loginCustomerId);

	/**
	 * 
	 * @param cartUid
	 * @param deliveryPreference
	 * @param loginCustomerId
	 * @param map TODO
	 * @return
	 */
	Cart updatePreferredDeliveryOption(String cartUid, String deliveryPreference, Long loginCustomerId, Map<String, Object> map);

	/**
	 * 
	 * @param cart
	 * @return
	 */
	OrderDeliveryObject getCartDeliveryObject(Cart cart);
	
	/**
	 * 
	 * @param cartUid
	 * @param serviceType
	 * @param loginCustomerId
	 * @param map TODO
	 * @return
	 */
	Cart updatePreferredServiceType(String cartUid, String serviceType, Long loginCustomerId, Map<String, Object> map);
	
	/** @param customerId
	 * @param orderId
	 * @param source
	 * @param user TODO
	 * @param repeatDays TODO
	 * @return
	 * @throws Exception
	 */
	Cart getReorderCart(long customerId, long orderId, String source, User user, int repeatDays) throws Exception;

	/**
	 * 
	 * @param user
	 * @param cartUid
	 * @param packagingType
	 * @return
	 */
	Cart changePackagingType(User user, String cartUid, String packagingType);

	/**
	 * 
	 * @param user
	 * @param cartUid
	 * @param repeatDays
	 * @param repeatDate 
	 * @return
	 */
	Cart updateRepeatInDays(User user, String cartUid, int repeatDays, Timestamp repeatDate);

	/**
	 * 
	 * @param cartUid
	 * @param loginCustomerId
	 * @return
	 */
	Cart removeShippingChargeForCart(String cartUid, Long loginCustomerId);

	/**
	 * 
	 * @param cartUid
	 * @param cartItemId 
	 * @param cartItem
	 * @return
	 * @throws Exception 
	 */
	Cart updateCartItem(String cartUid, Long cartItemId, CartItem cartItem, boolean isAllowed) throws Exception;

	/**
	 * 
	 * @param cartUid
	 * @param isShippingChargeExempted
	 * @return
	 */
	Cart updateCartIsShippingChargeExempted(String cartUid, boolean isShippingChargeExempted);

	/**
	 * 
	 * @param user
	 * @param cartUid
	 * @param promisedDeliveryDate
	 * @param promisedDeliveryTime
	 * @return
	 */
	Cart updatePromisedDeliveryDetails(User user, String cartUid, Timestamp promisedDeliveryDate, String promisedDeliveryTime);

	Timestamp getPromisedDeliveryDateForUrgentOrder();

	Cart getCartSummary(Cart cart, Long loginCustomerId, boolean autoApplyCoupon);
	
	/**
	 * 
	 * @param cartUid
	 * @param cartItems
	 * @return
	 */
	Cart addCartItems(String cartUid, List<CartItem> cartItems);
	
	List<Cart> getAllCartsByUids(List<String> cartUids);
	
	/**
	 * 
	 * @param user
	 * @param cartUid
	 * @param revisitDays
	 * @param revisitDate
	 * @return
	 */
	Cart updateRevisitInDays(User user, String cartUid, int revisitDays, Timestamp revisitDate);

	Cart updateNextRefillDays(String cartUid, Timestamp nextRefillDate, int nextRefillDays, User user);
	
	Cart switchCartPincode(String cartUid, String pincode);
	
	Cart updateAppointmentDetails(String cartUid, Timestamp appointmentDate, long appointmentSlotId, String appointmentSlot, User user);
	
	Cart updateReportDeliveryOption(String cartUid, Long loginCustomerId, Map<String, Object> map);

	void validateCartServiceability(Cart cart);

	Cart createB2BBulkCart(Cart cart);

}
