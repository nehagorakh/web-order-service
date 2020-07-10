package in.lifcare.order.microservice.coupon.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;

import in.lifcare.core.constant.APIEndPoint;
import in.lifcare.core.exception.NotFoundException;
import in.lifcare.core.response.model.Error;
import in.lifcare.core.response.model.Response;
import in.lifcare.core.util.MicroserviceClient;
import in.lifcare.order.cart.model.Cart;
import in.lifcare.order.cart.model.CartItem;
import in.lifcare.order.event.CartEventService;
import in.lifcare.order.microservice.account.customer.service.CustomerService;
import in.lifcare.order.microservice.coupon.model.Coupon;
import in.lifcare.order.microservice.coupon.service.CouponService;
import in.lifcare.order.microservice.shipping.service.ShippingService;
import in.lifcare.order.model.ShippingAddress;

/**
 * 
 * @author karan
 *
 */
@SuppressWarnings({"unchecked", "rawtypes"})
@Service
public class CouponServiceImpl implements CouponService {

	List<String> sources = Arrays.asList(Cart.SOURCE.ANDROID, Cart.SOURCE.IOS, Cart.SOURCE.MWEB, Cart.SOURCE.MSITE);
	List<Integer> validDiscounts = new ArrayList<Integer>(Arrays.asList(20, 25));
	
	@Override
	public Coupon getApplicableCouponByCustomerIdAndPatientId(Long customerId, Long patientId) {
		if( customerId == null || patientId == null ) {
			throw new IllegalArgumentException("Invalid parameters : customer id / patient id is not specified");
		}
		Response<?> couponResponse = microserviceClient.getForObject(APIEndPoint.COUPON + "/customer/" + customerId + "/patient/" + patientId + "/applicable-coupon", Response.class);
		Coupon coupon = (Coupon) couponResponse.populatePayloadUsingJson(Coupon.class);
		if (coupon == null) {
			throw new NotFoundException("No applicable coupon response received for customer id : " + customerId + " and patient id : " + patientId);
		}
		return coupon;
	}

	@Override
	public Cart checkCouponForCart(String couponCode, Cart cart, String authClientId) {
		if( StringUtils.isBlank(couponCode) || cart == null ) {
			throw new IllegalArgumentException("Invalid parameters : coupon code / cart not specified");
		}
		String authClientParam = "";
		if( StringUtils.isNotBlank(authClientId) ) {
			authClientParam = "?auth-client-id=" + authClientId;
		}
		if(cart.getShippingAddress() == null) { 
			if(cart.getShippingAddressId() != null && cart.getShippingAddressId() > 0 && cart.getCustomerId() != null && cart.getCustomerId() > 0) {
				ShippingAddress shippingAddress = customerService.getShippingAddressByCustomerIdAndShippingAddressId(cart.getCustomerId(), cart.getShippingAddressId());
				if(shippingAddress != null) {
					cart.setShippingAddress(shippingAddress);
				}
			}
		}
		try {
			Response<?> checkCouponResponse = microserviceClient.postForObject(APIEndPoint.COUPON + "/" + couponCode + "/cart/check" + authClientParam, cart, Response.class);
			Cart cartRes = (Cart) checkCouponResponse.populatePayloadUsingJson(Cart.class);
			if (cartRes == null) {
				throw new NotFoundException("No cart response received for coupon code");
			}
			if( !StringUtils.isBlank(cartRes.getCouponCode()) ) {
				cart.setCouponCode(cartRes.getCouponCode());
				cart.setCouponDiscount(cartRes.getCouponDiscount());
				cart.setCouponCashback(cartRes.getCouponCashback());
				cart.setCashbackPercentage(cartRes.getCashbackPercentage());
				cart.setTotalSalePrice(cartRes.getTotalSalePrice());
				cart.setDiscount(cartRes.getDiscount());
				cart.setShortCouponDescription(cartRes.getShortCouponDescription());
				cart.setCouponDescription(cartRes.getCouponDescription());
				if( cart.getCartItems() != null && !cart.getCartItems().isEmpty() && cartRes.getCartItems() != null && !cartRes.getCartItems().isEmpty() ) {
					Map<String, CartItem> cartResItemMap = new HashMap<String, CartItem>();
					for( int i = 0; i < cartRes.getCartItems().size(); i++ ) {
						cartResItemMap.put(cartRes.getCartItems().get(i).getSku(), cartRes.getCartItems().get(i));
					}
					for( int i = 0; i < cart.getCartItems().size(); i++ ) {
						if( cartResItemMap.containsKey(cart.getCartItems().get(i).getSku()) ) {
							cart.getCartItems().get(i).setDiscount(cartResItemMap.get(cart.getCartItems().get(i).getSku()).getDiscount());
							cart.getCartItems().get(i).setSellingPrice(cartResItemMap.get(cart.getCartItems().get(i).getSku()).getSellingPrice());
						}
					}
				}
				cartEventService.applyPromo(couponCode, String.valueOf(cart.getId()), cart.getCustomerId(),"","SUCCESS");
			} else {
				cartEventService.applyPromo(couponCode, String.valueOf(cart.getId()), cart.getCustomerId(),"","FAIL");
				throw new IllegalArgumentException("Coupon code : " + couponCode + " is invalid");
			}
			return cart;
		} catch (Exception e) {
			boolean flag = false;
			Error err = new Error();
			err.setMessage(e.getMessage());
			try {
				Response<Error> error = mapper.readValue(e.getMessage(), Response.class);
				err = (Error) error.populateErrorUsingJson(Error.class);
				flag = true;
			} catch(Exception ex) {
				e.printStackTrace();
			}
			if( flag ) {
				throw new IllegalArgumentException(err.getMessage());
			}
			throw e;
		}
	}	

	@Override
	@HystrixCommand(fallbackMethod = "removeOrApplyDefaultCouponForCartFallback")
	public Cart removeOrApplyDefaultCouponForCart(Cart cart, String authClientId) {
		if( cart == null ) {
			throw new IllegalArgumentException("Invalid parameters : cart not specified");
		}
		try {
			String authClientParam = "";
			if( StringUtils.isNotBlank(authClientId) ) {
				authClientParam = "?auth-client-id=" + authClientId;
			}
			if(cart.getShippingAddress() == null) {
				if(cart.getShippingAddressId() != null && cart.getShippingAddressId() > 0  && cart.getCustomerId() != null && cart.getCustomerId() > 0) {
					ShippingAddress shippingAddress = customerService.getShippingAddressByCustomerIdAndShippingAddressId(cart.getCustomerId(), cart.getShippingAddressId());
					if(shippingAddress != null) {
						cart.setShippingAddress(shippingAddress);
					}
				}
			}
			Response<?> checkCouponResponse = microserviceClient.postForObject(APIEndPoint.COUPON + "/DEFAULT/cart/apply-default" + authClientParam, cart, Response.class);
			Cart cartRes = (Cart) checkCouponResponse.populatePayloadUsingJson(Cart.class);
			if (cartRes == null) {
				throw new NotFoundException("No cart response received for coupon code");
			}
			cart.setCouponCode(StringUtils.isNotBlank(cartRes.getCouponCode()) ? cartRes.getCouponCode() : null);
			cart.setCouponDiscount(cartRes.getCouponDiscount());
			cart.setCouponCashback(cartRes.getCouponCashback());
			cart.setCashbackPercentage(cartRes.getCashbackPercentage());
			cart.setTotalSalePrice(cartRes.getTotalSalePrice());
			cart.setDiscount(cartRes.getDiscount());
			cart.setShortCouponDescription(cartRes.getShortCouponDescription());
			cart.setCouponDescription(cartRes.getCouponDescription());
			if (cart.getCartItems() != null && !cart.getCartItems().isEmpty() && cartRes.getCartItems() != null && !cartRes.getCartItems().isEmpty()) {
				Map<String, CartItem> cartResItemMap = new HashMap<String, CartItem>();
				for (int i = 0; i < cartRes.getCartItems().size(); i++) {
					cartResItemMap.put(cartRes.getCartItems().get(i).getSku(), cartRes.getCartItems().get(i));
				}
				for (int i = 0; i < cart.getCartItems().size(); i++) {
					if (cartResItemMap.containsKey(cart.getCartItems().get(i).getSku())) {
						cart.getCartItems().get(i).setDiscount(cartResItemMap.get(cart.getCartItems().get(i).getSku()).getDiscount());
						cart.getCartItems().get(i).setSellingPrice(cartResItemMap.get(cart.getCartItems().get(i).getSku()).getSellingPrice());
					}
				}
			}
			return cart;
		} catch (Exception e) {
			boolean flag = false;
			Error err = new Error();
			err.setMessage(e.getMessage());
			try {
				Response<Error> error = mapper.readValue(e.getMessage(), Response.class);
				err = (Error) error.populateErrorUsingJson(Error.class);
				flag = true;
			} catch(Exception ex) {
				e.printStackTrace();
			}
			if( flag ) {
				throw new IllegalArgumentException(err.getMessage());
			}
			throw e;
		}
	}
	
	public Cart removeOrApplyDefaultCouponForCartFallback(Cart cart, String authClientId) {
		if( cart == null ) {
			throw new IllegalArgumentException("Invalid parameters : coupon code / cart not specified");
		}
		try {
			return couponServiceNotAvailable(cart);
		} catch (Exception e) {
			throw e;
		}
	}
	
	@SuppressWarnings("unlikely-arg-type")
	public Cart couponServiceNotAvailable(Cart cart){
		if(cart != null && cart.getCartItems() != null){
			float additionalDiscountPercentage = 0;
			float totalSalePrice = 0;
			float totalMrp = 0;
			if(cart.getSource() != null && sources.contains(cart.getSource())){
				additionalDiscountPercentage = 2;
			}
			for(CartItem cartItem : cart.getCartItems()){
				float lineItemDiscountInPercentage = 0;
				float salePrice = 0;
				if (validDiscounts.contains(cartItem.getDiscount())) {
					lineItemDiscountInPercentage = (float) cartItem.getDiscount();
				}
				if( lineItemDiscountInPercentage == 0 ) {
					salePrice = (float) (cartItem.getSellingPrice() * (100 - additionalDiscountPercentage)/100.0);
					if( cartItem.getMrp() > 0 ) {
						lineItemDiscountInPercentage = (float) ((cartItem.getMrp() - salePrice) * (100.0 / cartItem.getMrp()));
						lineItemDiscountInPercentage = (float) Math.round(lineItemDiscountInPercentage);
					}
				}
				salePrice = (float) (cartItem.getMrp() * (100 - lineItemDiscountInPercentage) / 100.0);
				totalMrp += cartItem.getMrp() * cartItem.getQuantity();
				cartItem.setDiscount(lineItemDiscountInPercentage);
				cartItem.setSellingPrice(salePrice);
				totalSalePrice += cartItem.getSellingPrice() * cartItem.getQuantity();
			}
			cart.setTotalSalePrice(totalSalePrice);
			cart.setTotalMrp(totalMrp);
			float $discount = totalMrp - totalSalePrice;
			cart.setDiscount($discount > 0 ? $discount : 0);
		}
		return cart;
	}
	
	private ObjectMapper mapper = new ObjectMapper();

	@Autowired
	private MicroserviceClient<Response> microserviceClient;
	
	@Autowired
	private CustomerService customerService;
	
	@Autowired
	private CartEventService cartEventService;

}
