package in.lifcare.order.cart.api;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import in.lifcare.core.exception.AccessNotAllowedException;
import in.lifcare.core.exception.BadRequestException;
import in.lifcare.core.model.User;
import in.lifcare.core.response.model.Response;
import in.lifcare.core.util.CommonUtil;
import in.lifcare.order.cart.model.Cart;
import in.lifcare.order.cart.model.CartItem;
import in.lifcare.order.cart.service.CartService;
import in.lifcare.order.event.CartEventService;
import in.lifcare.order.exception.CartException;

@RestController
@RequestMapping(value = "/cart")
public class CartController {

	@PreAuthorize("hasAnyRole('ROLE_AGENT','ROLE_USER') or hasPermission('hasAccess','microservice_client')")
	@RequestMapping(value="/{cart-uid}", method = RequestMethod.GET)
	public @ResponseBody Response<Cart> fetchCartSummaryByUid(@PathVariable("cart-uid") String cartUid) throws Exception {
		try {
			if (StringUtils.isBlank(cartUid)) {
				throw new BadRequestException("Invalid parameters provided");
			}
			return new Response<Cart>(cartService.getCartSummary(cartUid, null, true));
		} catch (IllegalArgumentException e) {
			throw new CartException(e.getMessage());
		} catch (Exception e) {
			throw e;
		}
	}
	
	@PreAuthorize("hasRole('ROLE_AGENT') or hasPermission('hasAccess','microservice_client')")
	@RequestMapping(value = "customer/{customer-id}/order/{order-id}/reorder", method = RequestMethod.GET)
	public @ResponseBody Response<Cart> fetchCartByUid(OAuth2Authentication auth, @PathVariable("customer-id") long customerId, @PathVariable("order-id") long orderId,
			@RequestParam("source") String source, @RequestParam(value = "repeat-days", required = false) Integer repeatDays) throws Exception {
		try {
			if (orderId <= 0) {
				throw new BadRequestException("Invalid parameters provided");
			}
			User user = CommonUtil.getOauthUser(auth);
			if (repeatDays == null) {
				repeatDays = 0;
			}
			return new Response<Cart>(cartService.getReorderCart(customerId, orderId, source, user, repeatDays));
		} catch (IllegalArgumentException e) {
			throw new CartException(e.getMessage());
		} catch (Exception e) {
			throw e;
		}
	}

	@PreAuthorize("hasRole('ROLE_AGENT') or hasPermission('hasAccess','microservice_client')")
	@RequestMapping(value="/all", method = RequestMethod.GET)
	public @ResponseBody Response<List<Cart>> fetchAllCartsByUids(@RequestParam("cart-uids") List<String> cartUids) throws Exception {
		try {
			if (cartUids == null || cartUids.isEmpty()) {
				throw new BadRequestException("Invalid parameters provided");
			}
			return new Response<List<Cart>>(cartService.getAllCartsByUids(cartUids));
		} catch (IllegalArgumentException e) {
			throw new CartException(e.getMessage());
		} catch (Exception e) {
			throw e;
		}
	}

	@PreAuthorize("hasAnyRole('ROLE_AGENT','ROLE_USER') or hasPermission('hasAccess','microservice_client')")
	@RequestMapping(method = RequestMethod.POST)
	public @ResponseBody Response<Cart> addCart(OAuth2Authentication auth, @RequestBody Cart cart) throws Exception {
		try {
			User user = null;
			Long customerId = null;
			try {
				user = CommonUtil.getOauthUser(auth);
				customerId = user != null && user.getId() > 0 ? user.getId(): null;
			} catch(Exception e) {
				//Ignore Exception - checking user for customer id
			}
			if (cart == null) {
				throw new BadRequestException("Invalid parameters provided");
			}
			if( cart.getFacilityCode() == null || cart.getSource() == null ) {
				throw new BadRequestException("Invalid Parameters : facility_code and source are mandatory");
			}
			if( Cart.TYPE.REFILL.equalsIgnoreCase(cart.getType()) ) {
				if( cart.getCustomerId() == null || cart.getPatientId() == null || cart.getCartItems() == null || cart.getCartItems().isEmpty() || cart.getShippingAddressId() == null ) {
					throw new BadRequestException("Invalid Parameters : customer_id, patient_id, cart_items and shipping_address_id are mandatory for refill type cart");
				}
				cartEventService.refillMedicineCartEvent(customerId, "", String.valueOf(cart.getId()));
			}else if (Cart.TYPE.JIVA.equalsIgnoreCase(cart.getType())) {
				if (cart.getCustomerId() == null) {
					throw new BadRequestException("Invalid Parameters : customer_id is mandatory for jiva type cart");
				}
				cart.setBusinessChannel(Cart.BUSINESS_CHANNEL.ONLINE);
				cart.setBusinessType(Cart.BUSINESS_TYPE.B2C);
				
				//Doctor clinic offline not active : DEADCODE
				if (Cart.BUSINESS_CHANNEL.OFFLINE.equalsIgnoreCase(cart.getBusinessChannel())
						&& Cart.BUSINESS_TYPE.B2C.equalsIgnoreCase(cart.getBusinessType())) {
					if (cart.getFacilityCode() <= 0 || cart.getFacilityName() == null) {
						throw new BadRequestException(
								"Invalid Parameters : facility_code and facility_name is mandatory for jiva type cart and channel clinic");
					}
				}
			}else if (Cart.TYPE.B2B.equalsIgnoreCase(cart.getType())) {
				if (cart.getCustomerId() == null) {
					throw new BadRequestException("Invalid Parameters : customer_id is mandatory for B2B type cart");
				}
				if (!Cart.PROCUREMENT_TYPE.BULK.equalsIgnoreCase(cart.getProcurementType())) {
					throw new BadRequestException("Invalid Parameters : PROCUREMENT_TYPE is should be BULK");
				}
				
			}
			if( customerId != null ) {
				cart.setCreatedBy(String.valueOf(customerId));
				cart.setUpdatedBy(String.valueOf(customerId));
			} else {
				customerId = 0L;
			}
			synchronized (customerId) {
				cart = cartService.addCart(cart);	
			
				if( customerId != null && customerId > 0 && Cart.USER_TYPE.EXTERNAL.equalsIgnoreCase(cart.getUserType()) ) {
					cart = cartService.transferCart(cart.getUid(), customerId);
				}
			}
			return new Response<Cart>(cart);
		} catch (IllegalArgumentException e) {
			throw new CartException(e.getMessage());
		} catch (Exception e) {
			throw e;
		}
	}

	@PreAuthorize("hasRole('ROLE_AGENT') or hasPermission('hasAccess','microservice_client')")
	@RequestMapping(value="/{cart-uid}/reset", method = RequestMethod.PATCH)
	public @ResponseBody Response<Cart> reset(OAuth2Authentication auth, @PathVariable("cart-uid") String cartUid) throws Exception {
		try {
			if (StringUtils.isBlank(cartUid)) {
				throw new BadRequestException("Invalid parameters provided");
			}
			User user = null;
			Long customerId = null;
			try {
				user = CommonUtil.getOauthUser(auth);
				customerId = user != null && user.getId() > 0 ? user.getId(): null;
			} catch(Exception e) {
				//Ignore Exception - checking user for customer id
			}
			synchronized (cartUid) {
				return new Response<Cart>(cartService.resetCart(cartUid, customerId));
			}
		} catch (IllegalArgumentException e) {
			throw new CartException(e.getMessage());
		} catch (Exception e) {
			throw e;
		}
	}
	
	@PreAuthorize("hasAnyRole('ROLE_AGENT','ROLE_USER') or hasPermission('hasAccess','microservice_client')")
	@RequestMapping(value="/{cart-uid}/items", method = RequestMethod.PUT)
	public @ResponseBody Response<Cart> addCartItems(OAuth2Authentication auth,@PathVariable("cart-uid") String cartUid, @RequestBody List<CartItem> cartItems) throws Exception {
		try {
			if (cartItems == null) {
				throw new BadRequestException("Invalid parameters provided");
			}
			User user = null;
			Long customerId = null;
			try {
				user = CommonUtil.getOauthUser(auth);
				customerId = user != null && user.getId() > 0 ? user.getId(): null;
			} catch(Exception e) {
				//Ignore Exception - checking user for customer id
			}
				
			synchronized (cartUid) {
				Response<Cart> response = new Response<Cart>(cartService.addCartItems(cartUid, cartItems));
				String defaultLocation = response.getPayload().getPatient() != null && response.getPayload().getPatient().getDefaultLocation() != null
						? response.getPayload().getPatient().getDefaultLocation() : "";
				double totalPayableAmount = response.getPayload().getTotalPayableAmount();
				List<CartItem> cartItemsResponse = response.getPayload().getCartItems();
				List<String> name = new ArrayList<>();
				List<String> brand = new ArrayList<>();
				List<String> skus = new ArrayList<>();
				int quantity = 0;
				for (CartItem cartItem : cartItemsResponse) {
					quantity = (int) (quantity + cartItem.getQuantity());
					name.add(cartItem.getName() != null ? cartItem.getName() : "");
					brand.add(cartItem.getBrand() != null ? cartItem.getBrand() : "");
					skus.add(cartItem.getSku() != null ? cartItem.getSku() : "");
				}
				cartEventService.addCartItems(customerId, cartUid, cartItems, defaultLocation, name, brand, skus, quantity);
				return response;
			}
		} catch (IllegalArgumentException e) {
			throw new CartException(e.getMessage());
		} catch (Exception e) {
			throw e;
		}
	}

	@PreAuthorize("hasAnyRole('ROLE_AGENT','ROLE_USER') or hasPermission('hasAccess','microservice_client')")
	@RequestMapping(value="/{cart-uid}/item", method = RequestMethod.PUT)
	public @ResponseBody Response<Cart> addCartItem(@PathVariable("cart-uid") String cartUid, @RequestBody CartItem cartItem, @RequestParam(value = "is-allowed", defaultValue = "false", required=false) boolean isAllowed) throws Exception {
		try {
			if ( cartItem == null ) {
				throw new BadRequestException("Invalid parameters provided");
			}
			synchronized (cartUid) {
				return new Response<Cart>(cartService.addCartItem(cartUid, cartItem, isAllowed));
			}
		} catch (IllegalArgumentException e) {
			throw new CartException(e.getMessage());
		} catch (Exception e) {
			throw e;
		}
	}

	@PreAuthorize("hasAnyRole('ROLE_AGENT','ROLE_USER') or hasPermission('hasAccess','microservice_client')")
	@RequestMapping(value="/{cart-uid}/item/{cart-item-id}", method = RequestMethod.PATCH)
	public @ResponseBody Response<Cart> updateCartItem(@PathVariable("cart-uid") String cartUid, @PathVariable("cart-item-id") Long cartItemId, @RequestBody CartItem cartItem, @RequestParam(value = "is-allowed", defaultValue = "false") boolean isAllowed) throws Exception {
		try {
			if ( cartItem == null || cartItemId == null || cartItemId <= 0 ) {
				throw new BadRequestException("Invalid parameters provided");
			}
			synchronized (cartUid) {
				return new Response<Cart>(cartService.updateCartItem(cartUid, cartItemId, cartItem, isAllowed));
			}
		} catch (IllegalArgumentException e) {
			throw new CartException(e.getMessage());
		} catch (Exception e) {
			throw e;
		}
	}
	
	@PreAuthorize("hasAnyRole('ROLE_AGENT','ROLE_USER') or hasPermission('hasAccess','microservice_client')")
	@RequestMapping(value="/{cart-uid}/item/{cart-item-id}", method = RequestMethod.DELETE)
	public @ResponseBody Response<Cart> deleteCartItem(@PathVariable("cart-uid") String cartUid, @PathVariable("cart-item-id") Long cartItemId) throws Exception {
		try {
			if ( StringUtils.isBlank(cartUid) || cartItemId == null ) {
				throw new BadRequestException("Invalid parameters provided");
			}
			synchronized (cartUid) {
				return new Response<Cart>(cartService.deleteCartItem(cartUid, cartItemId));
			}
		} catch (IllegalArgumentException e) {
			throw new CartException(e.getMessage());
		} catch (Exception e) {
			throw e;
		}
	}
	
	@PreAuthorize("hasAnyRole('ROLE_AGENT','ROLE_USER') or hasPermission('hasAccess','microservice_client')")
	@RequestMapping(value="/{cart-uid}/item/sku/{sku}", method = RequestMethod.DELETE)
	public @ResponseBody Response<Cart> deleteCartItem(@PathVariable("cart-uid") String cartUid, @PathVariable("sku") String sku) throws Exception {
		try {
			if ( StringUtils.isBlank(cartUid) || StringUtils.isBlank(sku) ) {
				throw new BadRequestException("Invalid parameters provided");
			}
			synchronized (cartUid) {
				return new Response<Cart>(cartService.deleteCartItemBySku(cartUid, sku));
			}
		} catch (IllegalArgumentException e) {
			throw new CartException(e.getMessage());
		} catch (Exception e) {
			throw e;
		}
	}
	
	@PreAuthorize("hasRole('ROLE_AGENT') or hasPermission('hasAccess','microservice_client')")
	@RequestMapping(value="/{cart-uid}/refill/transfer", method = RequestMethod.PATCH)
	public @ResponseBody Response<Cart> transferRefillCart(OAuth2Authentication auth, @PathVariable("cart-uid") String cartUid) throws Exception {
		try {
			if (StringUtils.isBlank(cartUid)) {
				throw new BadRequestException("Invalid parameters provided");
			}
			User user = null;
			Long customerId = null;
			try {
				user = CommonUtil.getOauthUser(auth);
				customerId = user != null ? user.getId(): null;
				if( customerId == null ) {
					throw new AccessNotAllowedException("customer id not fetched via auth resource");
				}
			} catch(Exception e) {
				throw new AccessNotAllowedException("User not authorized to access");
			}
			synchronized (cartUid) {
				return new Response<Cart>(cartService.transferRefillCart(cartUid, customerId));
			}
		} catch (IllegalArgumentException e) {
			throw new CartException(e.getMessage());
		} catch (Exception e) {
			throw e;
		}
	}

	@PreAuthorize("hasAnyRole('ROLE_AGENT','ROLE_USER') or hasPermission('hasAccess','microservice_client')")
	@RequestMapping(value="/{cart-uid}/prescription", method = RequestMethod.PUT)
	public @ResponseBody Response<Cart> addCartPrescription(OAuth2Authentication auth,@PathVariable("cart-uid") String cartUid,
			@RequestPart(name = "file", required = false) MultipartFile file,
			@RequestParam(name = "cart-prescription-ids", required = false) List<String> cartPrescriptionIds,
			@RequestParam(name = "expiry-date", required = false) Long expiryDateLong,
			@RequestParam(name = "rx-date", required = false) Long rxDateLong,
			@RequestParam(name = "patient-id", required = false) Long patientId,
			@RequestParam(name = "doctor-name", required = false) String doctorName) throws Exception {
		try {
			if ( (cartPrescriptionIds == null || cartPrescriptionIds.isEmpty() ) && (file == null || file.isEmpty()) ) {
				throw new BadRequestException("File / Cart Prescription Id's - atleast one is mandatory.");
			}
			synchronized (cartUid) {		
				Timestamp expiryDate = expiryDateLong != null ? new Timestamp(expiryDateLong) : null;
				Timestamp rxDate = rxDateLong != null ? new Timestamp(rxDateLong) : null;
				cartEventService.addPrescription(auth,cartUid);
				return new Response<Cart>(cartService.addCartPrescription(cartUid, file, cartPrescriptionIds, expiryDate, doctorName, rxDate, patientId));
			}
		} catch (IllegalArgumentException e) {
			throw new CartException(e.getMessage());
		} catch (Exception e) {
			throw e;
		}
	}

	@PreAuthorize("hasAnyRole('ROLE_AGENT','ROLE_USER') or hasPermission('hasAccess','microservice_client')")
	@RequestMapping(value="/{cart-uid}/prescription/{cart-prescription-id}", method = RequestMethod.DELETE)
	public @ResponseBody Response<Cart> deleteCartPrescription(@PathVariable("cart-uid") String cartUid, @PathVariable("cart-prescription-id") String cartPrescriptionIdStr) throws Exception {
		try {
			if ( StringUtils.isBlank(cartUid) || cartPrescriptionIdStr == null ) {
				throw new BadRequestException("Invalid parameters provided");
			}
			Long cartPrescriptionId = Long.parseLong(cartPrescriptionIdStr);
			synchronized (cartUid) {
				return new Response<Cart>(cartService.deleteCartPrescription(cartUid, cartPrescriptionId));
			}
		} catch (IllegalArgumentException e) {
			throw new CartException(e.getMessage());
		} catch (Exception e) {
			throw e;
		}
	}

	@PreAuthorize("hasAnyRole('ROLE_AGENT','ROLE_USER') or hasPermission('hasAccess','microservice_client')")
	@RequestMapping(value="/{cart-uid}/prescription", method = RequestMethod.DELETE)
	public @ResponseBody Response<Cart> deleteCartPrescription(@PathVariable("cart-uid") String cartUid, @RequestParam("cart-prescription-ids") List<Long> cartPrescriptionIds) throws Exception {
		try {
			if ( StringUtils.isBlank(cartUid) || cartPrescriptionIds == null || cartPrescriptionIds.isEmpty() ) {
				throw new BadRequestException("Invalid parameters provided");
			}
			synchronized (cartUid) {
				return new Response<Cart>(cartService.deleteCartPrescriptions(cartUid, cartPrescriptionIds));
			}
		} catch (IllegalArgumentException e) {
			throw new CartException(e.getMessage());
		} catch (Exception e) {
			throw e;
		}
	}
	
	@PreAuthorize("hasRole('ROLE_AGENT') or hasPermission('hasAccess','microservice_client')")
	@RequestMapping(value="/{cart-uid}/transfer", method = RequestMethod.PATCH)
	public @ResponseBody Response<Cart> transferCart(OAuth2Authentication auth, @PathVariable("cart-uid") String cartUid) throws Exception {
		try {
			User user = null;
			Long customerId = null;
			try {
				user = CommonUtil.getOauthUser(auth);
				customerId = user != null ? user.getId(): null;
				if( customerId == null ) {
					throw new AccessNotAllowedException("customer id not fetched via auth resource");
				}
			} catch(Exception e) {
				throw new AccessNotAllowedException("User not authorized to access");
			}
			if ( StringUtils.isBlank(cartUid) ) {
				throw new BadRequestException("Invalid parameters provided");	
			}
			synchronized (cartUid) {
				return new Response<Cart>(cartService.transferCart(cartUid, customerId));
			}
		} catch (IllegalArgumentException e) {
			throw new CartException(e.getMessage());
		} catch (Exception e) {
			throw e;
		}
	}

	@PreAuthorize("hasRole('ROLE_AGENT') or hasPermission('hasAccess','microservice_client')")
	@RequestMapping(value="/{cart-uid}/parameters", method = RequestMethod.PATCH)
	public @ResponseBody Response<Cart> updateCartParameters(@PathVariable("cart-uid") String cartUid, @RequestBody Map<String, Object> updateParams) throws Exception {
		try {
			if ( StringUtils.isBlank(cartUid) || updateParams == null || updateParams.isEmpty() ) {
				throw new BadRequestException("Invalid parameters provided");	
			}
			List<String> allowedParameters = Arrays.asList(new String[] {"coupon_code", "care_points", "facility_code", "coupon_discount"});
			Set<String> keySet = updateParams.keySet();
			//TODO Restrict update - allow only allowParameter list keys for update in cart
			synchronized (cartUid) {
				return new Response<Cart>(cartService.updateCartParameters(cartUid, updateParams));
			}
		} catch (IllegalArgumentException e) {
			throw new CartException(e.getMessage());
		} catch (Exception e) {
			throw e;
		}
	}
	
	@PreAuthorize("hasAnyRole('ROLE_AGENT','ROLE_USER') or hasPermission('hasAccess','microservice_client')")
	@RequestMapping(value = "/{cart-uid}/doctor-callback", method = RequestMethod.PATCH)
	public @ResponseBody Response<Cart> updateCartDoctorCallback(@PathVariable("cart-uid") String cartUid, @RequestBody Map<String, Object> doctorCallback) throws Exception {
		try {
			if (StringUtils.isBlank(cartUid) || doctorCallback == null || doctorCallback.isEmpty() || !doctorCallback.containsKey("doctor_callback")
					|| doctorCallback.get("doctor_callback") == null) {
				throw new BadRequestException("Invalid parameters provided");
			}

			boolean isDoctorCallback = doctorCallback.get("doctor_callback") != null ? Boolean.parseBoolean(String.valueOf(doctorCallback.get("doctor_callback"))) : false;
			synchronized (cartUid) {
				return new Response<Cart>(cartService.updateCartIsDoctorCallback(cartUid, isDoctorCallback));
			}
		} catch (IllegalArgumentException e) {
			throw new CartException(e.getMessage());
		} catch (Exception e) {
			throw e;
		}
	}

	@PreAuthorize("hasRole('ROLE_AGENT') or hasPermission('hasAccess','microservice_client')")
	@RequestMapping(value = "/{cart-uid}/shipping-charge-exempted", method = RequestMethod.PATCH)
	public @ResponseBody Response<Cart> updateCartShippingChargeExempted(@PathVariable("cart-uid") String cartUid, @RequestBody Map<String, Object> shippingChargeExempted) throws Exception {
		try {
			if (StringUtils.isBlank(cartUid) || shippingChargeExempted == null || shippingChargeExempted.isEmpty() || !shippingChargeExempted.containsKey("shipping_charge_exempted") || shippingChargeExempted.get("shipping_charge_exempted") == null) {
				throw new BadRequestException("Invalid parameters provided");
			}

			boolean isShippingChargeExempted = shippingChargeExempted.get("shipping_charge_exempted") != null ? Boolean.parseBoolean(String.valueOf(shippingChargeExempted.get("shipping_charge_exempted"))) : false;
			synchronized (cartUid) {
				return new Response<Cart>(cartService.updateCartIsShippingChargeExempted(cartUid, isShippingChargeExempted));
			}
		} catch (IllegalArgumentException e) {
			throw new CartException(e.getMessage());
		} catch (Exception e) {
			throw e;
		}
	}
	
	@PreAuthorize("hasRole('ROLE_AGENT') or hasPermission('hasAccess','microservice_client')")
	@RequestMapping(value="/{cart-uid}", method = RequestMethod.DELETE)
	public @ResponseBody Response<Boolean> discardCart(@PathVariable("cart-uid") String cartUid) throws Exception {
		try {
			if (StringUtils.isBlank(cartUid)) {
				throw new BadRequestException("Invalid parameters provided");
			}
			synchronized (cartUid) {
				return new Response<Boolean>(cartService.discardCart(cartUid));
			}
		} catch (IllegalArgumentException e) {
			throw new CartException(e.getMessage());
		} catch (Exception e) {
			throw e;
		}
	}
	
	@PreAuthorize("hasRole('ROLE_AGENT') or hasPermission('hasAccess','microservice_client')")
	@RequestMapping(value = "/{cart-uid}/status/{status}", method = RequestMethod.PATCH)
	public @ResponseBody Response<Cart> statusChange(@PathVariable("cart-uid") String cartUid, @PathVariable("status") String status) throws Exception {
		try {
			if (StringUtils.isBlank(cartUid) || StringUtils.isBlank(status)) {
				throw new BadRequestException("Invalid parameters provided");
			}
			synchronized (cartUid) {
				return new Response<Cart>(cartService.updateStatus(cartUid, status));
			}
		} catch (IllegalArgumentException e) {
			throw new CartException(e.getMessage());
		} catch (Exception e) {
			throw e;
		}
	}
	
	@PreAuthorize("hasAnyRole('ROLE_AGENT','ROLE_USER') or hasPermission('hasAccess','microservice_client')")
	@RequestMapping(value="/customer/{customer-id}/summary", method = RequestMethod.GET)
	public @ResponseBody Response<Cart> getCartSummaryByCustomer(OAuth2Authentication auth, @PathVariable("customer-id") long customerId, @RequestParam(value="cart-type", required = false) String cartType) throws Exception {
		try {
			synchronized ((Long) customerId) {
				return new Response<Cart>(cartService.getCartByCustomer(customerId, cartType));
			}
		} catch (IllegalArgumentException e) {
			throw new CartException(e.getMessage());
		} catch (Exception e) {
			throw e;
		}
	}
	
	@PreAuthorize("hasRole('ROLE_AGENT') or hasPermission('hasAccess','microservice_client')")
	@RequestMapping(value = "/customer/{customer-id}", method = RequestMethod.GET)
	public @ResponseBody Response<Page<Cart>> getAllCartByCustomerAndType(OAuth2Authentication auth, @PathVariable("customer-id") long customerId, @RequestParam("type") String type, @PageableDefault Pageable pageable)
			throws Exception {
		try {
			User user = null;
			Long loginCustomerId = null;
			try {
				user = CommonUtil.getOauthUser(auth);
				loginCustomerId = user != null ? user.getId(): null;
				if( loginCustomerId == null ) {
					throw new AccessNotAllowedException("customer id not fetched via auth resource");
				}
			} catch(Exception e) {
				throw new AccessNotAllowedException("User not authorized to access");
			}
			if (customerId <= 0 || StringUtils.isBlank(type)) {
				throw new BadRequestException("Invalid parameters provided");
			}
//			if( !loginCustomerId.equals(customerId) ) {
//				throw new AccessNotAllowedException("User not authorized to access :: login customer id mismatch : login customer id : " + loginCustomerId + " : customer id specified : " + customerId);
//			}
			synchronized ((Long) customerId) {
				return new Response<Page<Cart>>(cartService.getCartByCustomerIdAndType(customerId, type, pageable));
			}
		} catch (IllegalArgumentException e) {
			throw new CartException(e.getMessage());
		} catch (Exception e) {
			throw e;
		}
	}
	
	@PreAuthorize("hasRole('ROLE_AGENT') or hasPermission('hasAccess','microservice_client')")
	@RequestMapping(value="/{cart-uid}/summary", method = RequestMethod.GET)
	public @ResponseBody Response<Cart> getDetailedCartSummary(OAuth2Authentication auth, @PathVariable("cart-uid") String cartUid) throws Exception {
		try {
			User user = null;
			Long loginCustomerId = null;
			try {
				user = CommonUtil.getOauthUser(auth);
				loginCustomerId = user != null ? user.getId(): null;
				if( loginCustomerId == null ) {
					throw new AccessNotAllowedException("customer id not fetched via auth resource");
				}
			} catch(Exception e) {
				throw new AccessNotAllowedException("User not authorized to access");
			}
			if (StringUtils.isBlank(cartUid)) {
				throw new BadRequestException("Invalid parameters provided");
			}
			synchronized (cartUid) {
				return new Response<Cart>(cartService.getCartSummary(cartUid, loginCustomerId, true));
			}
		} catch (IllegalArgumentException e) {
			throw new CartException(e.getMessage());
		} catch (Exception e) {
			throw e;
		}
	}

	@PreAuthorize("hasRole('ROLE_AGENT') or hasPermission('hasAccess','microservice_client')")
	@RequestMapping(value="/{cart-uid}/shipping-address", method = RequestMethod.PATCH)
	public @ResponseBody Response<Cart> addShippingAddress(OAuth2Authentication auth, @PathVariable("cart-uid") String cartUid, @RequestBody Map<String, Object> shippingParam) throws Exception {
		try {
			User user = null;
			Long loginCustomerId = null;
			try {
				user = CommonUtil.getOauthUser(auth);
				loginCustomerId = user != null ? user.getId(): null;
				if( loginCustomerId == null ) {
					throw new AccessNotAllowedException("customer id not fetched via auth resource");
				}
			} catch(Exception e) {
				throw new AccessNotAllowedException("User not authorized to access");
			}
			if ( StringUtils.isBlank(cartUid) || shippingParam == null || shippingParam.isEmpty() || !shippingParam.containsKey("shipping_address_id") ) {
				throw new BadRequestException("Invalid parameters provided");	
			}
			Long shippingAddressId = shippingParam.get("shipping_address_id") != null ? Long.parseLong(String.valueOf(shippingParam.get("shipping_address_id"))) : null;
			synchronized (cartUid) {
				cartEventService.addShippingAddress(auth, cartUid);
				return new Response<Cart>(cartService.addShippingAddress(cartUid, shippingAddressId, loginCustomerId));
			}
		} catch (IllegalArgumentException e) {
			throw new CartException(e.getMessage());
		} catch (Exception e) {
			throw e;
		}
	}
	
	@PreAuthorize("hasRole('ROLE_AGENT') or hasPermission('hasAccess','microservice_client')")
	@RequestMapping(value="/{cart-uid}/assign/patient", method = RequestMethod.PATCH)
	public @ResponseBody Response<Cart> assignPatientToCart(OAuth2Authentication auth, @PathVariable("cart-uid") String cartUid, @RequestBody Map<String, Object> patientParam) throws Exception {
		try {
			User user = null;
			Long loginCustomerId = null;
			try {
				user = CommonUtil.getOauthUser(auth);
				loginCustomerId = user != null ? user.getId(): null;
				if( loginCustomerId == null ) {
					throw new AccessNotAllowedException("customer id not fetched via auth resource");
				}
			} catch(Exception e) {
				throw new AccessNotAllowedException("User not authorized to access");
			}
			if (StringUtils.isBlank(cartUid) || patientParam == null || patientParam.isEmpty() || !patientParam.containsKey("patient_id")) {
				throw new BadRequestException("Invalid parameters provided");
			}
			synchronized (cartUid) {
				Long patientId = patientParam.get("patient_id") != null ? Long.parseLong(String.valueOf(patientParam.get("patient_id"))) : null;
				cartEventService.assignPatient(auth, cartUid);
				return new Response<Cart>(cartService.assignPatientToCart(cartUid, patientId, loginCustomerId));
			}
		} catch (IllegalArgumentException e) {
			throw new CartException(e.getMessage());
		} catch (Exception e) {
			throw e;
		}
	}

	@PreAuthorize("hasRole('ROLE_AGENT') or hasPermission('hasAccess','microservice_client')")
	@RequestMapping(value="/{cart-uid}/coupon/{coupon-code}", method = RequestMethod.PATCH)
	public @ResponseBody Response<Cart> applyCouponForCart(OAuth2Authentication auth, @PathVariable("cart-uid") String cartUid, @PathVariable("coupon-code") String couponCode) {
		try {
			User user = null;
			Long loginCustomerId = null;
			String authClientId = null;
			try {
				user = CommonUtil.getOauthUser(auth);
				authClientId = CommonUtil.getClientId(auth);
				loginCustomerId = user != null ? user.getId(): null;
				if( loginCustomerId == null ) {
					throw new AccessNotAllowedException("customer id not fetched via auth resource");
				}
			} catch(Exception e) {
				throw new AccessNotAllowedException("User not authorized to access");
			}
			if (StringUtils.isBlank(cartUid) || StringUtils.isBlank(couponCode)) {
				throw new BadRequestException("Invalid parameters provided");
			}
			synchronized (cartUid) {
				return new Response<Cart>(cartService.applyCouponForCart(cartUid, couponCode, loginCustomerId, authClientId));
			}
		} catch (IllegalArgumentException e) {
			throw new CartException(e.getMessage());
		} catch (Exception e) {
			throw e;
		}
	}

	@PreAuthorize("hasRole('ROLE_AGENT') or hasPermission('hasAccess','microservice_client')")
	@RequestMapping(value="/{cart-uid}/coupon/{coupon-code}", method = RequestMethod.DELETE)
	public @ResponseBody Response<Cart> removeCouponForCart(OAuth2Authentication auth, @PathVariable("cart-uid") String cartUid, @PathVariable("coupon-code") String couponCode) {
		try {
			User user = null;
			Long loginCustomerId = null;
			String authClientId = null;
			try {
				user = CommonUtil.getOauthUser(auth);
				authClientId = CommonUtil.getClientId(auth);
				loginCustomerId = user != null ? user.getId(): null;
				if( loginCustomerId == null ) {
					throw new AccessNotAllowedException("customer id not fetched via auth resource");
				}
			} catch(Exception e) {
				throw new AccessNotAllowedException("User not authorized to access");
			}
			if (StringUtils.isBlank(cartUid) || StringUtils.isBlank(couponCode)) {
				throw new BadRequestException("Invalid parameters provided");
			}
			synchronized (cartUid) {
				return new Response<Cart>(cartService.removeCouponForCart(cartUid, couponCode, loginCustomerId, authClientId));
			}
		} catch (IllegalArgumentException e) {
			throw new CartException(e.getMessage());
		} catch (Exception e) {
			throw e;
		}
	}

	@PreAuthorize("hasRole('ROLE_AGENT') or hasPermission('hasAccess','microservice_client')")
	@RequestMapping(value="/{cart-uid}/shipping-charge", method = RequestMethod.DELETE)
	public @ResponseBody Response<Cart> removeShippingChargeForCart(OAuth2Authentication auth, @PathVariable("cart-uid") String cartUid) {
		try {
			User user = null;
			Long loginCustomerId = null;
			try {
				user = CommonUtil.getOauthUser(auth);
				loginCustomerId = user != null ? user.getId(): null;
				if( loginCustomerId == null ) {
					throw new AccessNotAllowedException("customer id not fetched via auth resource");
				}
			} catch(Exception e) {
				throw new AccessNotAllowedException("User not authorized to access");
			}
			if (StringUtils.isBlank(cartUid)) {
				throw new BadRequestException("Invalid parameters provided");
			}
			synchronized (cartUid) {
				return new Response<Cart>(cartService.removeShippingChargeForCart(cartUid, loginCustomerId));
			}
		} catch (IllegalArgumentException e) {
			throw new CartException(e.getMessage());
		} catch (Exception e) {
			throw e;
		}
	}
	
	@PreAuthorize("hasAnyRole('ROLE_AGENT','ROLE_USER') or hasPermission('hasAccess','microservice_client')")
	@Deprecated
	@RequestMapping(value="/{cart-uid}/facility/{facility-code}", method = RequestMethod.PATCH)
	public @ResponseBody Response<Cart> switchCartFacilityCode(OAuth2Authentication auth, @PathVariable("cart-uid") String cartUid, @PathVariable("facility-code") Integer facilityCode) throws Exception {
		try {
			if (StringUtils.isBlank(cartUid) || facilityCode == null) {
				throw new BadRequestException("Invalid parameters provided");
			}
			synchronized (cartUid) {
				return new Response<Cart>(cartService.switchCartFacilityCode(cartUid, facilityCode));
			}
		} catch (IllegalArgumentException e) {
			throw new CartException(e.getMessage());
		} catch (Exception e) {
			throw e;
		}
	}
	
	@PreAuthorize("hasAnyRole('ROLE_AGENT','ROLE_USER') or hasPermission('hasAccess','microservice_client')")
	@RequestMapping(value="/{cart-uid}/pincode/{pincode}", method = RequestMethod.PATCH)
	public @ResponseBody Response<Cart> switchCartPincode(OAuth2Authentication auth, @PathVariable("cart-uid") String cartUid, @PathVariable("pincode") String pincode) throws Exception {
		try {
			if (StringUtils.isBlank(cartUid) || StringUtils.isBlank(pincode)) {
				throw new BadRequestException("Invalid parameters provided");
			}
			synchronized (cartUid) {
				return new Response<Cart>(cartService.switchCartPincode(cartUid, pincode));
			}
		} catch (IllegalArgumentException e) {
			throw new CartException(e.getMessage());
		} catch (Exception e) {
			throw e;
		}
	}

	@PreAuthorize("hasRole('ROLE_AGENT') or hasPermission('hasAccess','microservice_client')")
	@RequestMapping(value = "/{cart-uid}/preferred-delivery-option/{delivery-option}", method = RequestMethod.PATCH)
	public @ResponseBody Response<Cart> updatePreferredDeliveryOption(OAuth2Authentication auth,
			@PathVariable("cart-uid") String cartUid, @PathVariable("delivery-option") String deliveryOption, @RequestBody(required = false) Map<String, Object> map) {
		try {
			User user = null;
			Long loginCustomerId = null;
			try {
				user = CommonUtil.getOauthUser(auth);
				loginCustomerId = user != null ? user.getId() : null;
				if (loginCustomerId == null) {
					throw new AccessNotAllowedException("customer id not fetched via auth resource");
				}
			} catch (Exception e) {
				throw new AccessNotAllowedException("User not authorized to access");
			}
			if (StringUtils.isBlank(cartUid) || StringUtils.isBlank(deliveryOption)) {
				throw new BadRequestException("Invalid parameters provided");
			}
			return new Response<Cart>(cartService.updatePreferredDeliveryOption(cartUid, deliveryOption, loginCustomerId, map));
		} catch (IllegalArgumentException e) {
			throw new CartException(e.getMessage());
		} catch (Exception e) {
			throw e;
		}
	}
	
	@PreAuthorize("hasRole('ROLE_AGENT') or hasPermission('hasAccess','microservice_client')")
	@RequestMapping(value = "/{cart-uid}/preferred-service-type/{service-type}", method = RequestMethod.PATCH)
	public @ResponseBody Response<Cart> updatePreferredServiceType(OAuth2Authentication auth,
			@PathVariable("cart-uid") String cartUid, @PathVariable("service-type") String serviceType, @RequestBody(required = false) Map<String, Object> map) {
		try {
			User user = null;
			Long loginCustomerId = null;
			try {
				user = CommonUtil.getOauthUser(auth);
				loginCustomerId = user != null ? user.getId() : null;
				if (loginCustomerId == null) {
					throw new AccessNotAllowedException("customer id not fetched via auth resource");
				}
			} catch (Exception e) {
				throw new AccessNotAllowedException("User not authorized to access");
			}
			if (StringUtils.isBlank(cartUid) || StringUtils.isBlank(serviceType)) {
				throw new BadRequestException("Invalid parameters provided");
			}
			return new Response<Cart>(cartService.updatePreferredServiceType(cartUid, serviceType, loginCustomerId, map));
		} catch (IllegalArgumentException e) {
			throw new CartException(e.getMessage());
		} catch (Exception e) {
			throw e;
		}
	}
	
	@PreAuthorize("hasRole('ROLE_AGENT') or hasPermission('hasAccess','microservice_client')")
	@RequestMapping(value="/{cart-uid}/packaging-type/{packaging-type}", method = RequestMethod.PATCH)
	public @ResponseBody Response<Cart> changePackagingType(OAuth2Authentication auth, @PathVariable("cart-uid") String cartUid, @PathVariable("packaging-type") String packagingType) throws Exception {
		try {
			User user = CommonUtil.getOauthUser(auth);
			if (StringUtils.isBlank(cartUid) || StringUtils.isBlank(packagingType)) {
				throw new BadRequestException("Invalid parameters provided");
			}
			synchronized (cartUid) {
				return new Response<Cart>(cartService.changePackagingType(user, cartUid, packagingType));
			}
		} catch (IllegalArgumentException e) {
			throw new CartException(e.getMessage());
		} catch (Exception e) {
			throw e;
		}
	}
	
	@PreAuthorize("hasRole('ROLE_AGENT') or hasPermission('hasAccess','microservice_client')")
	@RequestMapping(value = "/{cart-uid}/repeat-in-days", method = RequestMethod.PATCH)
	public @ResponseBody Response<Cart> updateRepeatDays(OAuth2Authentication auth, @PathVariable("cart-uid") String cartUid, @RequestBody HashMap<String, Object> repeatInfoMap) throws Exception {
		try {
			User user = CommonUtil.getOauthUser(auth);
			if (StringUtils.isBlank(cartUid) || repeatInfoMap == null || repeatInfoMap.isEmpty()) {
				throw new BadRequestException("Invalid parameters provided");
			}
			Timestamp repeatDate = null;
			int repeatDay = 0;
			try {
				repeatDate = repeatInfoMap.containsKey("repeat_date") && repeatInfoMap.get("repeat_date") != null ? new Timestamp(Long.parseLong(String.valueOf(repeatInfoMap.get("repeat_date")))) : null;
				repeatDay = repeatInfoMap.containsKey("repeat_day") && repeatInfoMap.get("repeat_day") != null ? Integer.parseInt(String.valueOf(repeatInfoMap.get("repeat_day"))) : 0;
			} catch(Exception e) {
				e.printStackTrace();
			}
			synchronized (cartUid) {
				return new Response<Cart>(cartService.updateRepeatInDays(user, cartUid, repeatDay, repeatDate));
			}
		} catch (IllegalArgumentException e) {
			throw new CartException(e.getMessage());
		} catch (Exception e) {
			throw e;
		}
	}
	
	@PreAuthorize("hasRole('ROLE_AGENT') or hasPermission('','microservice_client')")
	@RequestMapping(value = "/{cart-uid}/promised-delivery-details", method = RequestMethod.PATCH)
	public @ResponseBody Response<Cart> updatePromisedDeliveryDetails(OAuth2Authentication auth, @PathVariable("cart-uid") String cartUid, @RequestBody Map<String, Object> promisedDeliveryDetailMap) throws Exception {
		try {
			User user = CommonUtil.getOauthUser(auth);
			if (StringUtils.isBlank(cartUid) || promisedDeliveryDetailMap == null || promisedDeliveryDetailMap.isEmpty()) {
				throw new BadRequestException("Invalid parameters provided");
			}
			Timestamp promisedDeliveryDate = null;
			String promisedDeliveryTime = null;
			try {
				promisedDeliveryDate = promisedDeliveryDetailMap.containsKey("promised_delivery_date") && promisedDeliveryDetailMap.get("promised_delivery_date") != null ? new Timestamp(Long.parseLong(String.valueOf(promisedDeliveryDetailMap.get("promised_delivery_date")))) : null;
				promisedDeliveryTime = promisedDeliveryDetailMap.containsKey("promised_delivery_time") && promisedDeliveryDetailMap.get("promised_delivery_time") != null ? String.valueOf(promisedDeliveryDetailMap.get("promised_delivery_time")) : null;
			} catch(Exception e) {
				e.printStackTrace();
			}
			return new Response<Cart>(cartService.updatePromisedDeliveryDetails(user, cartUid, promisedDeliveryDate, promisedDeliveryTime));
		} catch (IllegalArgumentException e) {
			throw new CartException(e.getMessage());
		} catch (Exception e) {
			throw e;
		}
	}
	
	@PreAuthorize("hasRole('ROLE_AGENT') or hasPermission('hasAccess','microservice_client')")
	@RequestMapping(value = "/{cart-uid}/revisit-in-days", method = RequestMethod.PATCH)
	public @ResponseBody Response<Cart> updateRevisitDays(OAuth2Authentication auth,
			@PathVariable("cart-uid") String cartUid, @RequestBody HashMap<String, Object> revisitInfoMap)
			throws Exception {
		try {
			User user = CommonUtil.getOauthUser(auth);
			if (StringUtils.isBlank(cartUid) || revisitInfoMap == null || revisitInfoMap.isEmpty()) {
				throw new BadRequestException("Invalid parameters provided");
			}
			Timestamp revisitDate = null;
			int revisitDay = 0;
			try {
				revisitDate = revisitInfoMap.containsKey("revisit_date") && revisitInfoMap.get("revisit_date") != null
						? new Timestamp(Long.parseLong(String.valueOf(revisitInfoMap.get("revisit_date")))) : null;
				revisitDay = revisitInfoMap.containsKey("revisit_day") && revisitInfoMap.get("revisit_day") != null
						? Integer.parseInt(String.valueOf(revisitInfoMap.get("revisit_day"))) : 0;
			} catch (Exception e) {
				e.printStackTrace();
			}
			synchronized (cartUid) {
				return new Response<Cart>(cartService.updateRevisitInDays(user, cartUid, revisitDay, revisitDate));
			}
		} catch (IllegalArgumentException e) {
			throw new CartException(e.getMessage());
		} catch (Exception e) {
			throw e;
		}
	}
	
	@PreAuthorize("hasRole('ROLE_AGENT') or hasPermission('hasAccess','microservice_client')")
	@RequestMapping(value = "/{cart-uid}/next-refill-days", method = RequestMethod.PATCH)
	public @ResponseBody Response<Cart> updateNextRefillDays(OAuth2Authentication auth, @PathVariable("cart-uid") String cartUid,
			@RequestBody HashMap<String, Object> nextInfoMap) throws Exception {
		try {
			User user = CommonUtil.getOauthUser(auth);
			if (nextInfoMap == null || nextInfoMap.isEmpty() || StringUtils.isBlank(cartUid)) {
				throw new BadRequestException("Invalid parameters provided");
			}
			Timestamp nextRefillDate = null;
			int nextRefillDays = 0;
			try {
				nextRefillDays = nextInfoMap.containsKey("next_refill_day")
						&& nextInfoMap.get("next_refill_day") != null
								? Integer.parseInt(String.valueOf(nextInfoMap.get("next_refill_day")))
								: 0;
				nextRefillDate = nextInfoMap.containsKey("next_refill_date")
						&& nextInfoMap.get("next_refill_date") != null
								? new Timestamp(Long.parseLong(String.valueOf(nextInfoMap.get("next_refill_date"))))
								: null;
				return new Response<Cart>(cartService.updateNextRefillDays(cartUid, nextRefillDate, nextRefillDays, user));

			} catch (Exception e) {
				throw e;
			}

		} catch (Exception e) {

			throw e;
		}

	}
	
	@PreAuthorize("hasRole('ROLE_AGENT') or hasPermission('hasAccess','microservice_client')")
	@RequestMapping(value = "/{cart-uid}/appointment", method = RequestMethod.PATCH)
	public @ResponseBody Response<Cart> updateAppointment(OAuth2Authentication auth, @PathVariable("cart-uid") String cartUid,
			@RequestBody HashMap<String, Object> appointmentInfoMap) throws Exception {
		try {
			User user = CommonUtil.getOauthUser(auth);
			if (appointmentInfoMap == null || appointmentInfoMap.isEmpty()) {
				throw new BadRequestException("Invalid parameters provided");
			}
			Timestamp appointmentDate =  appointmentInfoMap.get("appointment_date") != null ?
					new Timestamp(Long.parseLong(String.valueOf(appointmentInfoMap.get("appointment_date")))) : null;
			String appointmentSlot = appointmentInfoMap.get("appointment_slot") != null ?
					String.valueOf(appointmentInfoMap.get("appointment_slot")) : null;
			long appointmentSlotId = appointmentInfoMap.get("appointment_slot_id") != null ?
					Long.parseLong(String.valueOf(appointmentInfoMap.get("appointment_slot_id"))) : 0;
			return new Response<Cart>(cartService.updateAppointmentDetails(cartUid, appointmentDate, appointmentSlotId, appointmentSlot, user));
		} catch (Exception e) {

			throw e;
		}

	}
	
	@PreAuthorize("hasRole('ROLE_AGENT') or hasPermission('hasAccess','microservice_client')")
	@RequestMapping(value = "/{cart-uid}/lab-report-delivery-option", method = RequestMethod.PATCH)
	public @ResponseBody Response<Cart> updateReportDeliveryOption(OAuth2Authentication auth,
			@PathVariable("cart-uid") String cartUid, @RequestBody Map<String, Object> map) {
		try {
			User user = null;
			Long loginCustomerId = null;
			try {
				user = CommonUtil.getOauthUser(auth);
				loginCustomerId = user != null ? user.getId() : null;
				if (loginCustomerId == null) {
					throw new AccessNotAllowedException("customer id not fetched via auth resource");
				}
			} catch (Exception e) {
				throw new AccessNotAllowedException("User not authorized to access");
			}
			if (map == null || map.isEmpty()) {
				throw new BadRequestException("Invalid parameters provided");
			}
			return new Response<Cart>(cartService.updateReportDeliveryOption(cartUid, loginCustomerId, map));
		} catch (IllegalArgumentException e) {
			throw new CartException(e.getMessage());
		} catch (Exception e) {
			throw e;
		}
	}
	
	@Autowired
	private CartService cartService;
	
	@Autowired 
	private CartEventService cartEventService;
	
}
