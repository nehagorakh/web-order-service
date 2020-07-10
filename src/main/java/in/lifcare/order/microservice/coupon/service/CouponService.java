package in.lifcare.order.microservice.coupon.service;

import in.lifcare.order.cart.model.Cart;
import in.lifcare.order.microservice.coupon.model.Coupon;

/**
 * 
 * @author karan
 *
 */
public interface CouponService {

	/**
	 * 
	 * @param customerId
	 * @param patientId
	 * @return
	 */
	public Coupon getApplicableCouponByCustomerIdAndPatientId(Long customerId, Long patientId);

	/**
	 * 
	 * @param couponCode
	 * @param cart
	 * @param authClientId 
	 * @return
	 */
	public Cart checkCouponForCart(String couponCode, Cart cart, String authClientId);

	/**
	 * 
	 * @param cart
	 * @param authClientId 
	 * @return
	 */
	public Cart removeOrApplyDefaultCouponForCart(Cart cart, String authClientId);
	
}
