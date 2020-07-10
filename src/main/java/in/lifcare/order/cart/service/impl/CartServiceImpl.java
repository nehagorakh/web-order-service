package in.lifcare.order.cart.service.impl;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.beanutils.BeanUtilsBean2;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.converters.SqlTimestampConverter;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Days;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.google.common.base.CaseFormat;

import in.lifcare.core.exception.AccessNotAllowedException;
import in.lifcare.core.exception.BadRequestException;
import in.lifcare.core.exception.NotFoundException;
import in.lifcare.core.model.Facility;
import in.lifcare.core.model.FacilityPincodeMapping;
import in.lifcare.core.model.ProductResponse;
import in.lifcare.core.model.SellerDetail;
import in.lifcare.core.model.User;
import in.lifcare.core.model.Membership;
import in.lifcare.core.util.CommonUtil;
import in.lifcare.order.cart.model.Cart;
import in.lifcare.order.cart.model.CartItem;
import in.lifcare.order.cart.model.CartItem.PRODUCT_CATEGORY;
import in.lifcare.order.cart.model.CartPrescription;
import in.lifcare.order.cart.repository.CartRepository;
import in.lifcare.order.cart.service.CartItemService;
import in.lifcare.order.cart.service.CartPrescriptionService;
import in.lifcare.order.cart.service.CartService;
import in.lifcare.order.exception.CartItemNotFoundException;
import in.lifcare.order.exception.CartNotFoundException;
import in.lifcare.order.exception.InvalidCartStatusException;
import in.lifcare.order.exception.MaxOrderedQuantityExceeded;
import in.lifcare.order.exception.MaxPermissibleLimitReached;
import in.lifcare.order.exception.PrescriptionExpiredException;
import in.lifcare.order.exception.PrescriptionRxDateNotFound;
import in.lifcare.order.exception.UnserviceablePincodeException;
import in.lifcare.order.microservice.account.customer.model.Customer;
import in.lifcare.order.microservice.account.customer.model.RefillLead;
import in.lifcare.order.microservice.account.customer.service.CustomerService;
import in.lifcare.order.microservice.account.customer.service.RefillLeadService;
import in.lifcare.order.microservice.account.facility.service.FacilityService;
import in.lifcare.order.microservice.account.patient.model.Patient;
import in.lifcare.order.microservice.account.prescription.model.Prescription;
import in.lifcare.order.microservice.account.prescription.service.PrescriptionService;
import in.lifcare.order.microservice.coupon.service.CouponService;
import in.lifcare.order.microservice.inventory.service.FacilityPincodeMappingService;
import in.lifcare.order.microservice.payment.service.PaymentService;
import in.lifcare.order.microservice.shipping.model.PlacePincode;
import in.lifcare.order.microservice.shipping.service.ShippingService;
import in.lifcare.order.model.Order;
import in.lifcare.order.model.OrderDeliveryObject;
import in.lifcare.order.model.OrderItem;
import in.lifcare.order.model.ShippingAddress;
import in.lifcare.order.service.OrderService;
import in.lifcare.order.service.OrderShippingAddressService;


@Service
public class CartServiceImpl implements CartService {

	private static final String SHIPPING_CHARGE_KEY = "shipping_charge";
	private static final String MIN_MRP_SHIPPING_CHARGE_KEY = "minimum_mrp";
	private static final int DEFAULT_FACILITY_ID = 101;
	
	@Value("${expressDelivery.startTime}")
	private String expressDeliveryStartTime;
	
	@Value("${bulk-medicine-upload-sku-limit}")
	private int bulkMedicineUploadSkuLimit;
	
	@Value("${expressDelivery.endTime}")
	private String expressDeliveryEndTime;
	
	@Value("${expressDelivery.expressOrderCount}")
	private int expressOrderCount;
	
	@Override
	//@Transactional(rollbackFor = Exception.class)
	public Cart addCart(Cart cart) {
		if(cart == null) {
			throw new IllegalArgumentException("Invalid cart object provided");
		}
		validateCart(cart);
		cart.setStatus(Cart.STATUS.CREATED);
		if(cart.getBusinessChannel() == null){
			cart.setBusinessChannel(Cart.BUSINESS_CHANNEL.ONLINE);
		}
		if(cart.getProcurementType() == null){
			cart.setProcurementType(Cart.PROCUREMENT_TYPE.NORMAL);
		}
		if(cart.getBusinessType() == null){
			cart.setBusinessType(Cart.BUSINESS_TYPE.B2C);
		}
		if( Cart.TYPE.REFILL.equalsIgnoreCase(cart.getType()) && Cart.CATEGORY.MEDICINE.equalsIgnoreCase(cart.getCategory()) ) {
			return createRefillCart(cart);
		}
		if( Cart.TYPE.JIVA.equalsIgnoreCase(cart.getType()) ) {
			return createJivaCart(cart);
		}
		cart.setUserType(Cart.USER_TYPE.EXTERNAL);
		if (StringUtils.isNotBlank(cart.getPincode())){
			cart.setFacilityCode(getFacilityIdByPincodeAndCartCategory(cart.getPincode(), cart.getCategory()));
		}
		if (Cart.TYPE.B2B.equalsIgnoreCase(cart.getType()) && Cart.PROCUREMENT_TYPE.BULK.equalsIgnoreCase(cart.getProcurementType())) {
			return createB2BBulkCart(cart);
		}
		return saveCart(cart);
	}
	
	@Override
	public Cart createB2BBulkCart(Cart cart) {
		if( cart == null) {
			throw new IllegalArgumentException("cart object is empty");
		}
		if( cart.getCustomerId() == null ) {
			throw new IllegalArgumentException("Invalid Paramters : customer_id is mandatory for B2B type cart");
		}
		Long customerId = cart.getCustomerId();
		
		Customer customer = customerService.getCustomerDetail(customerId);
		if (customer == null) {
			throw new NotFoundException("No customer found for customer id : " + cart.getCustomerId() + " and cart uid : " + cart.getUid());
		}
		cart.setPatientId(customer.getPatients().get(0).getId());
		List<ShippingAddress> shippingAddresses = customerService.getShippingAddressesByCustomerId(customerId);
		cart.setShippingAddressId(shippingAddresses.get(0).getId());
		
		List<CartItem> cartItems = cart.getCartItems();
		validateActiveCart(cart);
		Cart oldCart = cartRepository.findTopByCustomerIdAndStatusAndTypeAndCategoryAndUserTypeOrderByCreatedAtDesc(cart.getCustomerId(), Cart.STATUS.CREATED, Cart.TYPE.B2B, cart.getCategory(), Cart.USER_TYPE.INTERNAL);
		if( oldCart != null ) {
			
			cart.setId(oldCart.getId());
			cart.setUid(oldCart.getUid());
		}
		
		cart.setCustomerFirstName(customer.getFirstName());
		cart.setCustomerLastName(customer.getLastName());
		cart.setPatientFirstName(customer.getPatients().get(0).getFirstName());
		cart.setPatientLastName(customer.getPatients().get(0).getLastName());
		cart.setPincode(String.valueOf(shippingAddresses.get(0).getPincode()));
		cart.setFacilityCode(getFacilityIdByPincodeAndCartCategory(cart.getPincode(), cart.getCategory()));
		cart.setUserType(Cart.USER_TYPE.INTERNAL);
		cart = saveCart(cart);
		if( cartItems != null && !cartItems.isEmpty() ) {
			replaceCartItems(cart.getUid(), cartItems, Boolean.FALSE);
		}
		return getCartSummary(cart.getUid(), customerId, false);
	}

	@Override
	public Cart createJivaCart(Cart cart) {
		if( cart == null) {
			throw new IllegalArgumentException("cart object is empty");
		}
		if( cart.getCustomerId() == null ) {
			throw new IllegalArgumentException("Invalid Paramters : customer_id is mandatory for jiva type cart");
		}
		if (Cart.BUSINESS_CHANNEL.OFFLINE.equalsIgnoreCase(cart.getBusinessChannel()) && Cart.BUSINESS_TYPE.B2C.equalsIgnoreCase(cart.getBusinessType())) {
			if (cart.getFacilityCode() <= 0 || cart.getFacilityName() == null) {
				throw new IllegalArgumentException("Invalid Parameters : facility_code and facility_name is mandatory for jiva type cart and channel clinic");
			}
		}
		Long customerId = cart.getCustomerId();
		List<CartItem> cartItems = cart.getCartItems();
		validateActiveCart(cart);
		Cart oldUserCreatedJivaCart = cartRepository.findTopByCustomerIdAndStatusAndTypeAndCategoryAndUserTypeOrderByCreatedAtDesc(cart.getCustomerId(), Cart.STATUS.CREATED, Cart.TYPE.JIVA, cart.getCategory(), Cart.USER_TYPE.INTERNAL);
		if( oldUserCreatedJivaCart != null ) {
			if( !Cart.SOURCE_TYPE.REFILL_CRM.equalsIgnoreCase(cart.getSourceType()) ) {
				return getCartSummary(oldUserCreatedJivaCart.getUid(), customerId, true);
			}
			cart.setId(oldUserCreatedJivaCart.getId());
			cart.setUid(oldUserCreatedJivaCart.getUid());
		}
		Customer customer = customerService.getCustomerDetail(customerId);
		if (customer == null) {
			throw new NotFoundException("No customer found for customer id : " + cart.getCustomerId() + " and cart uid : " + cart.getUid());
		}
		cart.setCustomerFirstName(customer.getFirstName());
		cart.setCustomerLastName(customer.getLastName());
		if( cart.getPatientId() != null && cart.getPatientId() > 0 ) {
			Patient patient = customerService.getPatientByCustomerIdAndPatientId(customerId, cart.getPatientId());
			if( patient == null ) {
				throw new NotFoundException("No patient found for customer id : " + customerId + " and cart uid : " + cart.getUid());
			}
			cart.setPatientFirstName(patient.getFirstName());
			cart.setPatientLastName(patient.getLastName());
		}
		cart.setUserType(Cart.USER_TYPE.INTERNAL);
		cart = saveCart(cart);
		if( cartItems != null && !cartItems.isEmpty() ) {
			replaceCartItems(cart.getUid(), cartItems, Boolean.FALSE);
		}
		return getCartSummary(cart.getUid(), customerId, true);
	}

	@Override
	public Cart saveCart(Cart cart) {
		if(cart == null) {
			throw new IllegalArgumentException("Invalid cart object provided");
		}
		validateCart(cart);
		cartRepository.save(cart);
		return cart;
	}
	
	@Override
	public Cart createRefillCart(Cart cart) {
		if( cart == null) {
			throw new IllegalArgumentException("cart object is empty");
		}
		if( cart.getCustomerId() == null || cart.getPatientId() == null || cart.getCartItems() == null || cart.getCartItems().isEmpty() || cart.getShippingAddressId() == null ) {
			throw new IllegalArgumentException("Invalid Paramters : customer_id, patient_id, cart_items and shipping_address_id are mandatory for refill type cart");
		}
		Long customerId = cart.getCustomerId();
		Long patientId = cart.getPatientId();
		List<CartItem> cartItems = cart.getCartItems();
		String cartUid = cart.getUid();
		validateActiveCart(cart);
		if( StringUtils.isBlank(cart.getCustomerFirstName()) ) {
			Customer customer = customerService.getCustomerDetail(customerId);
			if( customer == null ) {
				throw new NotFoundException("No customer found for customer id : " + cart.getCustomerId() + " and cart uid : " + cart.getUid());
			}
			cart.setCustomerFirstName(customer.getFirstName());
			cart.setCustomerLastName(customer.getLastName());
		}
		if( StringUtils.isBlank(cart.getPatientFirstName()) ) {
			Patient patient = customerService.getPatientByCustomerIdAndPatientId(customerId, patientId);
			if( patient == null ) {
				throw new NotFoundException("No patient found for customer id : " + customerId + " and cart uid : " + cartUid);
			}
			cart.setPatientFirstName(patient.getFirstName());
			cart.setPatientLastName(patient.getLastName());
		}
		cart.setUserType(Cart.USER_TYPE.INTERNAL);
		saveCart(cart);
		replaceCartItems(cartUid, cartItems, Boolean.FALSE);
		return fetchCartByUid(cart.getUid());
	}

	@Override
	//@Transactional(rollbackFor = Exception.class)
	public Cart resetCart(String cartUid, Long customerId) {
		try {
			if (StringUtils.isBlank(cartUid)) {
				throw new IllegalArgumentException("Invalid cart uid specified");
			}
			Cart cart = getCartByUid(cartUid);
			if (cart == null) {
				throw new CartNotFoundException("No cart found with cart uid :: " + cartUid);
			}
			validateActiveCart(cart);
			if( customerId != null && cart.getCustomerId() != null && customerId > 0 && !cart.getCustomerId().equals(customerId) ) {
				throw new IllegalArgumentException("Cart has been already owned by customer id : " + cart.getCustomerId());
			}
			if (cart.getCartItems() != null && !cart.getCartItems().isEmpty()) {
				cartItemService.deleteCartItems(cart.getCartItems());
			}
			if (cart.getCartPrescriptions() != null && !cart.getCartPrescriptions().isEmpty()) {
				cartPrescriptionService.deleteCartPrescriptions(cart.getCartPrescriptions());
			}
			cart.setPatientId(null);
			cart.setShippingAddressId(null);
			cart.setCustomerId(null);
			cart.setCustomerFirstName(null);
			cart.setCustomerLastName(null);
			cart.setPatientFirstName(null);
			cart.setPatientLastName(null);
			cart = getCartSummary(cart.getUid(), cart.getCustomerId(), true);
			saveCart(cart);
			return cart;
		} catch(Exception e) {
			throw e;
		}
	}
	
	@Override
	//@Transactional(rollbackFor = Exception.class)
	public Cart replaceCartItems(String cartUid, List<CartItem> cartItems, boolean isAutoSeggestionQty) {
		if (StringUtils.isBlank(cartUid) || cartItems == null) {
			throw new IllegalArgumentException("Invalid cart uid / cart items");
		}
		if (!cartItems.isEmpty() && cartItems.size() > bulkMedicineUploadSkuLimit) {
			throw new IllegalArgumentException("Cart limit exceeds.");
		}
		try {
			Cart cart = fetchCartByUid(cartUid);
			validateActiveCart(cart);
			String cartCategory = cart.getCategory();
			cartItems.forEach(c -> {
				if (!CartItem.PRODUCT_CATEGORY.MEMBERSHIP_CARD.equalsIgnoreCase((c.getProductCategory())) && !cartCategory.contains(c.getProductCategory())) {
					throw new IllegalArgumentException("Invalid product category in cart item : Allowed product category : " + cartCategory);
				}
			});

			cartItems.stream().filter(Objects::nonNull).forEach(item -> {
				if(CartItem.PRODUCT_CATEGORY.MEMBERSHIP_CARD.equalsIgnoreCase(item.getProductCategory())){
					List<Membership> memberships = customerService.getMemberships(cart.getCustomerId());
					Map<String, Membership> activeMembershipMap = null;
					if(memberships != null && !memberships.isEmpty()) {
						activeMembershipMap = memberships.stream().filter(Objects::nonNull).filter(m -> m.isMembershipActive() && !m.isValidForRenewal()).collect(Collectors.toMap(Membership::getSku, Function.identity()));
					}
					if(activeMembershipMap != null && activeMembershipMap.containsKey(item.getSku()) && activeMembershipMap.get(item.getSku()) != null) {
					   item.setValidToBuy(false);
					   item.setComment("Already an active membership, remove this");
					}
					item.setQuantity(1);
					cart.setMembershipAdded(true);
				}
			});
			
//			long membershipCardCount = cartItems.stream().filter(Objects::nonNull).filter(c -> CartItem.PRODUCT_CATEGORY.MEMBERSHIP_CARD.equalsIgnoreCase(c.getProductCategory())).count();
//			if (membershipCardCount > 1) {
//				throw new IllegalArgumentException("Only one membership allowed to add");
//			}
//			boolean isMembershipCardAdded = cartItems.stream().filter(Objects::nonNull).anyMatch(c -> CartItem.PRODUCT_CATEGORY.MEMBERSHIP_CARD.equalsIgnoreCase(c.getProductCategory()));
//			if(isMembershipCardAdded) {
//			cart.setMembershipAdded(true);
//			}

			List<CartItem> existCartItems = cart.getCartItems();

			// If existing cart items is not empty then delete all items
			if (existCartItems != null && !existCartItems.isEmpty()) {
				cartItemService.deleteCartItemsWithNewTransection(existCartItems);
			}
			cart.setItemCount(cartItems.size());
			double totalMrp = 0;
			double totalSalePrice = 0;
			cartItems = cartItemService.fetchUpdatedCartItemInventoryStock(cart.getFacilityCode(), cart.getPatientId(), cartItems, cart.getRepeatDay(), isAutoSeggestionQty, cart.getPincode(),
					cart.getBusinessType());
			if (!cartItems.isEmpty()) {
				for (CartItem cartItem : cartItems) {
					cartItem.setCartUid(cartUid);
					cartItem.setPatientId(cartItem.getPatientId() != null ? cartItem.getPatientId() : cart.getPatientId());

					// cartItem.setCreatedBy(cart.getCreatedBy());
					// cartItem.setUpdatedBy(cart.getUpdatedBy());
					try {
						totalMrp = totalMrp + (cartItem.getMrp() * cartItem.getQuantity());
						totalSalePrice = totalSalePrice + (cartItem.getSellingPrice() * cartItem.getQuantity());
					} catch (Exception e) {
						LOGGER.error("Error in fetching inventory stock for sku : {} : cart uid : {} :: Exception : {}", cartItem.getSku(), cartUid, e.getMessage());
						cartItem.setLog(e.getMessage());
					}
				}
				cartItems = setPatientNameInCartItems(cart.getCustomerId(), cartItems);
				cartItems = cartItemService.saveCartItems(cartItems);
				cart.setPatientId(cartItems.get(0).getPatientId());
			}
			cart.setTotalMrp(totalMrp);
			cart.setTotalSalePrice(totalSalePrice);
			// FIXME: MOVE TO GETTER SETTER CENTRALISED
			cart.setCartItems(cartItems);

			saveCart(cart);
			return cart;
		} catch (Exception e) {
			throw e;
		}
	}

    @Override
	//@Transactional(rollbackFor = Exception.class)
	public Cart addCartItem(String cartUid, CartItem cartItem, boolean isAllowed) {
//		if (StringUtils.isBlank(cartUid) || cartItem == null || StringUtils.isBlank(cartItem.getSku())) {
		if (StringUtils.isBlank(cartUid) || cartItem == null) {
			throw new IllegalArgumentException("Invalid cart uid / cart item");
		}
		try {
			Cart cart = getCartByUid(cartUid);
			validateActiveCart(cart);
			cartItem.setPatientId(cartItem.getPatientId() != null ? cartItem.getPatientId() : cart.getPatientId());
			String cartCategory = cart.getCategory();
			if(!CartItem.PRODUCT_CATEGORY.MEMBERSHIP_CARD.equalsIgnoreCase(cartItem.getProductCategory()) && !cartCategory.contains(cartItem.getProductCategory()) ) {
				throw new IllegalArgumentException("Invalid product category in cart item : Allowed product category : " + cartCategory);
			}
			if(cartItem.getProductCategory().equalsIgnoreCase(CartItem.PRODUCT_CATEGORY.MEMBERSHIP_CARD)) {
				List<CartItem> cartItems = cartItemService.getCartItemsByCartUid(cart.getUid());
				boolean isMembershipCardExists = cartItems.stream().anyMatch(c -> CartItem.PRODUCT_CATEGORY.MEMBERSHIP_CARD.equalsIgnoreCase(c.getProductCategory()));
				if(isMembershipCardExists) {
					throw new IllegalArgumentException("Already a membership added. Please remove first to add");
				}
				cartItem.setQuantity(1);
				cart.setMembershipAdded(true);
			}
			
			double totalMrp = cart.getTotalMrp();
			double totalSalePrice = cart.getTotalSalePrice();
			long itemCount = cart.getItemCount();
			cartItem.setPatientId(cartItem.getPatientId() == null || cartItem.getPatientId() == 0 ? cart.getPatientId() : cartItem.getPatientId() );
			CartItem existCartItem = cartItemService.findBySkuAndCartUidAndPatientId(cartItem.getSku(), cartItem.getPatientId(), cartUid);
			if( existCartItem != null ) {
				cartItem.setId(existCartItem.getId());
				totalMrp = totalMrp - (existCartItem.getMrp() * existCartItem.getQuantity());
				totalSalePrice = totalSalePrice - (existCartItem.getSellingPrice() * existCartItem.getQuantity());
				totalMrp = totalMrp >=0 ? totalMrp : 0;
				totalSalePrice = totalSalePrice >=0 ? totalSalePrice : 0;
			} else {
				itemCount++;
			}
			cartItem.setPatientId(cartItem.getPatientId() != null ? cartItem.getPatientId() : cart.getPatientId());
			cartItem.setCartUid(cartUid);
			try {
				List<CartItem> cartItems = cartItemService.fetchUpdatedCartItemInventoryStock(cart.getFacilityCode(), cartItem.getPatientId(), Arrays.asList(cartItem), cart.getRepeatDay(), Boolean.TRUE, cart.getPincode(), cart.getBusinessType());
				cartItem = cartItems != null ? cartItems.get(0) : cartItem ;
				if (cartItem.getMaxOrderQuantity() != null && cartItem.getMaxOrderQuantity() > 0 && cartItem.getQuantity() > cartItem.getMaxOrderQuantity() && !Cart.SOURCE.MWEB.equalsIgnoreCase(cart.getSource())) {
					throw new MaxPermissibleLimitReached("Max permissible quantity limit reached for medicine : " + cartItem.getName());
				} else if (cartItem.isExcessiveOrderedQuantity() && !isAllowed && !Cart.SOURCE.MWEB.equalsIgnoreCase(cart.getSource())) {
					throw new MaxOrderedQuantityExceeded("Max ordered quantity for customer " +cart.getCustomerFullName()+ " exceeded for medicine : " + cartItem.getName());
				}  
			} catch (MaxOrderedQuantityExceeded | MaxPermissibleLimitReached e) {
				throw e;
			}  catch(Exception e) {
				LOGGER.error("Error in fetching inventory stock for sku : {} : cart uid : {} :: Exception : {}", cartItem.getSku(), cartUid, e.getMessage());
				cartItem.setLog(e.getMessage());
			}
			// next release task
			/*if (cartItem.getPrescriptionId() != null && cartItem.getValidDays() != null) {
				try {
					checkPrescriptionVelidityForSku(cart.getPatientId(), cartItem.getPrescriptionId(), cartItem.getValidDays());	
				} catch (PrescriptionExpiredException e) {
					throw e;
				}
				
			}*/
			
			
			totalMrp = totalMrp + (cartItem.getMrp() * cartItem.getQuantity());
			totalSalePrice = totalSalePrice + (cartItem.getSellingPrice() * cartItem.getQuantity());
			cart.setTotalMrp(totalMrp);
			cart.setTotalSalePrice(totalSalePrice);
			if (itemCount > bulkMedicineUploadSkuLimit) {
				throw new IllegalArgumentException("Cart limit exceeds.");
			}
			cart.setItemCount(itemCount);
			cart.setCartItems(Arrays.asList(cartItem));
			cartItem = setPatientNameInCartItems(cart.getCustomerId(), Arrays.asList(cartItem)).get(0);
			cart.setPatientId(cartItem.getPatientId());
			saveCart(cart);
			cartItemService.saveCartItem(cartItem);
			return getCartSummary(cart.getUid(), cart.getCustomerId(), true);
		} catch (Exception e) {
			throw e;
		}
	}
	
	private List<CartItem> setPatientNameInCartItems(Long customerId, List<CartItem> cartItems) {
		if (customerId != null && customerId >= 0 && cartItems != null && !cartItems.isEmpty()) {
			List<Long> patientIds = cartItems.stream().filter(cartItem -> cartItem.getPatientId() != null && cartItem.getPatientId() > 0).map(CartItem::getPatientId)
					.collect(Collectors.toList());
			if (patientIds != null && !patientIds.isEmpty()) {
				Map<Long, Patient> map = customerService.getPatients(customerId, patientIds);
				if (map != null) {
					cartItems.parallelStream().filter(cI -> map.get(cI.getPatientId()) != null).map(cI -> {
						Patient patient = map.get(cI.getPatientId());
						cI.setPatientFirstName(patient.getFirstName());
						cI.setPatientLastName(patient.getLastName());
						return cI;
					}).collect(Collectors.toList());
				}
			}
		}
		return cartItems;
	}
	
	private void checkPrescriptionVelidityForSku(long patientId, Long prescriptionId, Integer validDays) throws PrescriptionRxDateNotFound, PrescriptionExpiredException {
		if (prescriptionId != null) {
			Prescription prescription = null;
			try {
				prescription = prescriptionService.getPrescriptionByPatientId(patientId, String.valueOf(prescriptionId));
			} catch (Exception e) {
				LOGGER.error("Prescription not found for given patientId {} and prescriptionId {} due to {}", patientId, prescriptionId, e.getMessage());
			}
			if (prescription.getRxDate() == null) {
				throw new PrescriptionRxDateNotFound("Prescription rx-date can not be null or empty!");
			}
			if (prescription != null && validDays != null) {
				if (!isPrescriptionValid(prescription.getRxDate(), validDays)) {
					throw new PrescriptionExpiredException("Prescription has expired!");
				}
			}
		}
	}
	
	private static boolean isPrescriptionValid(Date prescriptionDate, Integer validateDays) {
		if (prescriptionDate != null && validateDays != null) {
			DateTime a = new DateTime(prescriptionDate.getTime()).plusDays(validateDays);
			return new DateTime().isBefore(a.getMillis());
		}
		return false;
	}

	@Override
	//@Transactional(rollbackFor = Exception.class)
	public Cart addCartPrescription(String cartUid, MultipartFile file, List<String> cartPrescriptionIds, Timestamp expiryDate, String doctorName, Timestamp rxDate, Long patientId) throws Exception {
		if (StringUtils.isBlank(cartUid)) {
			throw new IllegalArgumentException("Invalid cart uid");
		}
		if ( (cartPrescriptionIds == null || cartPrescriptionIds.isEmpty()) && (file == null || file.isEmpty()) ) {
			throw new IllegalArgumentException("File / Cart Prescription Ids - atleast one is mandatory.");
		}
		try {
			Cart cart = fetchCartByUid(cartUid);
			if (cart == null) {
				throw new CartNotFoundException("No cart found with cart uid :: " + cartUid);
			}
			validateActiveCart(cart);
			if(patientId == null || patientId <= 0) {
				patientId = cart.getPatientId() != null ? cart.getPatientId() : 0;
//				patientId = cart.getCartItems() != null && !cart.getCartItems().isEmpty() && cart.getCartItems().get(0).getPatientId() != null && cart.getCartItems().get(0).getPatientId() > 0
//						? cart.getCartItems().get(0).getPatientId()
//						: cart.getPatientId() != null || cart.getPatientId() > 0 ? cart.getPatientId() : 0;
				
			}
			List<CartPrescription> cartPrescriptions = cartPrescriptionService.addOrUploadCartPrescriptions(cartUid, patientId, file, cartPrescriptionIds, expiryDate, doctorName, rxDate);
			if( cartPrescriptions == null || cartPrescriptions.isEmpty() ) {
				throw new IllegalArgumentException("Error occured while generating cart prescription for cart uid :: " + cartUid);
			}
			if( cart.getCartPrescriptions() == null ) {
				cart.setCartPrescriptions(new ArrayList<CartPrescription>());
			}
			cart.getCartPrescriptions().addAll(cartPrescriptions);
			cart.setPrescriptionCount(cart.getPrescriptionCount() + cartPrescriptions.size());
			saveCart(cart);
			return getCartSummary(cart, cart.getCustomerId(), Cart.PROCUREMENT_TYPE.NORMAL.equalsIgnoreCase(cart.getProcurementType()));
		} catch (Exception e) {
			throw e;
		}
	}

	@Override
	public Cart fetchCartByUid(String cartUid) {
		if(StringUtils.isBlank(cartUid)) {
			throw new IllegalArgumentException("Invalid cart uid specified");
		}
		
		String clientId = CommonUtil.getClientFromSession();
		//FIXME : retreive cart by uid and status
		Cart cart = getCartByUid(cartUid);
		if( cart == null ) {
			throw new CartNotFoundException("No cart found with cart uid :: " + cartUid);
		}
		List<CartItem> cartItems = cartItemService.getCartItemsByCartUid(cartUid);
		try {
			//FIXME : use stream instead of loop
			if (cartItems != null && !cartItems.isEmpty()) {
				double totalMrp = 0;
				double totalSalePrice = 0;
				double discount = 0;
				cartItems = cartItemService.fetchUpdatedCartItemInventoryStock(cart.getFacilityCode(), cart.getPatientId(), cartItems, cart.getRepeatDay(), Boolean.FALSE, cart.getPincode(), cart.getBusinessType());
				for (CartItem cartItem : cartItems) {
					double cartItemDiscountPer = cartItem.getDiscount();
					double cartItemSellingPrice = cartItem.getSellingPrice();
					cartItem.setCartUid(cartUid);
					if( Cart.SOURCE.JIVA.equalsIgnoreCase(cart.getSource()) ) {
						cartItem.setDiscount(cartItemDiscountPer);
						cartItem.setSellingPrice(cartItemSellingPrice);
					}
					totalMrp = totalMrp + (cartItem.getMrp() * cartItem.getQuantity());
					totalSalePrice = totalSalePrice + (cartItem.getSellingPrice() * cartItem.getQuantity());
				}
				if( totalMrp > 0 && totalMrp > totalSalePrice ) {
					discount = totalMrp - totalSalePrice;
				}
				cart.setDiscount(discount);
				cart.setTotalMrp(totalMrp);
				cart.setTotalSalePrice(totalSalePrice);
			}
		} catch (Exception e) {
			LOGGER.error("Error in medicine stock update : cart uid : {} : Exception : {}", cartUid, e.getMessage());
		}
		cartItems.stream().filter(Objects::nonNull).forEach(item -> {
			if(CartItem.PRODUCT_CATEGORY.MEMBERSHIP_CARD.equalsIgnoreCase(item.getProductCategory())){
				List<Membership> memberships = customerService.getMemberships(cart.getCustomerId());
				Map<String, Membership> activeMembershipMap = null;
				if(memberships!= null && !memberships.isEmpty()) {
					activeMembershipMap = memberships.stream().filter(Objects::nonNull).filter(m -> m.isMembershipActive() && !m.isValidForRenewal()).collect(Collectors.toMap(Membership::getSku, Function.identity()));
				}
				if(activeMembershipMap != null && activeMembershipMap.containsKey(item.getSku()) && activeMembershipMap.get(item.getSku()) != null) {
				   item.setValidToBuy(false);
				   item.setComment("Already an active membership, remove this");
				}
				item.setQuantity(1);
				cart.setMembershipAdded(true);
			}
		});
	//	cart.setCartItems(cartItems);
		
		List<CartItem> updateCartItems = new ArrayList<>();
		
		cartItems.forEach(item -> {
			if(item != null ) {
			//    System.out.println(" Removing inactive items from cart for client-id " + clientId + " and item status is : " + item.getProductStatus());
				updateCartItems.add(item);
				if(StringUtils.isNotBlank(item.getProductStatus()) && !item.getProductStatus().equalsIgnoreCase(ProductResponse.STATUS.ACTIVE) && StringUtils.isNotBlank(clientId) && !clientId.equalsIgnoreCase("jiva-client") && !cart.getBusinessType().equalsIgnoreCase("B2B")) {
					updateCartItems.remove(item);
				}
			}
		});
		cart.setCartItems(updateCartItems);
		List<CartPrescription> cartPrescriptions = cartPrescriptionService.getCartPrescriptionsByCartUid(cartUid);
		cart.setCartPrescriptions(cartPrescriptions);
		return cart;
	}
	
	@Override
	public List<Cart> getAllCartsByUids(List<String> cartUids) {
		if (cartUids == null || cartUids.isEmpty()) {
			throw new IllegalArgumentException("Invalid cart uids specified");
		}
		// FIXME : retreive cart by uid and status
		List<Cart> carts = cartRepository.findAllByUidIn(cartUids);
		if (carts == null || carts.isEmpty()) {
			throw new CartNotFoundException("No cart found with cart uids.");
		}
		try {
			// FIXME : use stream instead of loop
			List<CartItem> allCartItems = cartItemService.getCartItemsByCartUidIn(cartUids);
			if (allCartItems != null && !allCartItems.isEmpty()) {
				Map<String, List<CartItem>> cartAndCartItemsMap = allCartItems.parallelStream().collect(Collectors.groupingBy(CartItem::getCartUid));
				if (cartAndCartItemsMap != null && !cartAndCartItemsMap.isEmpty()) {
					carts.parallelStream().forEach(cart -> {
						double totalMrp = 0.0;
						double totalSalePrice = 0.0;
						double discount = 0.0;
						List<CartItem> cartItems = cartAndCartItemsMap.get(cart.getUid());
						totalMrp = cartItems.parallelStream().filter(cartItem -> cartItem.getQuantity() > 0).mapToDouble(cartItem -> (cartItem.getMrp() * cartItem.getQuantity())).sum();
						totalSalePrice = cartItems.parallelStream().filter(cartItem -> cartItem.getQuantity() > 0).mapToDouble(cartItem -> (cartItem.getSellingPrice() * cartItem.getQuantity())).sum();

						if (totalMrp > 0 && totalMrp > totalSalePrice) {
							discount = totalMrp - totalSalePrice;
						}
						cart.setDiscount(discount);
						cart.setTotalMrp(totalMrp);
						cart.setTotalSalePrice(totalSalePrice);
						cart.setCartItems(cartItems);
						cart.setCartPrescriptions(cartPrescriptionService.getCartPrescriptionsByCartUid(cart.getUid()));
					});
				}
			}
//			
//			
//			carts.parallelStream().forEach(cart -> {
//				double totalMrp = 0.0;
//				double totalSalePrice = 0.0;
//				double discount = 0.0;
//				List<CartItem> cartItems = cartItemService.getCartItemsByCartUid(cart.getUid());
//				totalMrp = cartItems.stream().filter(cartItem -> cartItem.getQuantity() > 0).mapToDouble(cartItem -> (cartItem.getMrp()*cartItem.getQuantity())).sum();
//				totalSalePrice = cartItems.stream().filter(cartItem -> cartItem.getQuantity() > 0).mapToDouble(cartItem -> (cartItem.getSellingPrice()*cartItem.getQuantity())).sum();
//
//				if (totalMrp > 0 && totalMrp > totalSalePrice) {
//					discount = totalMrp - totalSalePrice;
//				}
//				cart.setDiscount(discount);
//				cart.setTotalMrp(totalMrp);
//				cart.setTotalSalePrice(totalSalePrice);
//				cart.setCartItems(cartItems);
//				cart.setCartPrescriptions(cartPrescriptionService.getCartPrescriptionsByCartUid(cart.getUid()));
//			});
		} catch (Exception e) {
			LOGGER.error("Error in: getting items Exception : {}", e.getMessage());
		}
		return carts;
	}

	@Override
	public Cart getCartByUid(String cartUid) {
		if(StringUtils.isBlank(cartUid)) {
			throw new IllegalArgumentException("Invalid cart uid specified");
		}
		Cart cart = cartRepository.findOneByUid(cartUid);
		if( cart == null ) {
			throw new CartNotFoundException("No cart found with cart uid :: " + cartUid);
		}
		cart.setMembershipAdded(false);
		return cart;
	}
	
	@SuppressWarnings("serial")
	@Override
	public List<Cart> fetchCartsByUids(List<String> cartUids) {
		if (cartUids == null || cartUids.isEmpty()) {
			throw new IllegalArgumentException("Invalid parameters provided");
		}
		List<String> statuses = new ArrayList<String>() {{
			add(Cart.STATUS.CREATED);
		}};
		List<Cart> carts = cartRepository.findAllByUidInAndStatusIn(cartUids, statuses);
		if( carts == null  || carts.isEmpty()) {
			throw new CartNotFoundException("No cart found for specified uid's");
		}
		Cart cart = null;
		carts = new ArrayList<Cart>();
		for(String cartUid : cartUids) {
			try {
				cart = fetchCartByUid(cartUid);
				if( cart != null ) {
					carts.add(cart);
				}
			} catch (Exception e) {
				LOGGER.error("Error in fetching cart info for cart uid : {} : Exception : {}", cart.getUid(), e.getMessage());
			}
		}
		return carts;
	}
	
	@Override
	public Cart updateCartType(String cartUid, String type) {
		if(StringUtils.isBlank(cartUid) || StringUtils.isBlank(type) ) {
			throw new IllegalArgumentException("Not valid input params. ");
		}
		if(!Cart.TYPE.VALID_CART_TYPE_LIST.contains(type) ) {
			throw new IllegalArgumentException("Not valid cart type. ");
		}
		Cart cart = fetchCartByUid(cartUid);
		if (cart == null) {
			throw new CartNotFoundException("No cart found with cart uid :: " + cartUid);
		}
		validateActiveCart(cart);
		cart.setType(type);
		saveCart(cart);
		return getCartSummary(cart, cart.getCustomerId(), Cart.PROCUREMENT_TYPE.NORMAL.equalsIgnoreCase(cart.getProcurementType()));
	}
	
	@Override
	public Cart updateCartIsDoctorCallback(String cartUid, boolean isDoctorCallback) {
		if(StringUtils.isBlank(cartUid) ) {
			throw new IllegalArgumentException("Not valid input params. ");
		}
		
		Cart cart = fetchCartByUid(cartUid);
		if (cart == null) {
			throw new CartNotFoundException("No cart found with cart uid :: " + cartUid);
		}
		validateActiveCart(cart);
		cart.setDoctorCallback(isDoctorCallback);
		saveCart(cart);
		return getCartSummary(cart, cart.getCustomerId(), Cart.PROCUREMENT_TYPE.NORMAL.equalsIgnoreCase(cart.getProcurementType()));
	}

	@Override
	public Cart updateCartIsShippingChargeExempted(String cartUid, boolean isShippingChargeExempted) {
		if(StringUtils.isBlank(cartUid) ) {
			throw new IllegalArgumentException("Not valid input params. ");
		}
		Cart cart = fetchCartByUid(cartUid);
		if (cart == null) {
			throw new CartNotFoundException("No cart found with cart uid :: " + cartUid);
		}
		validateActiveCart(cart);
		cart.setShippingChargeExempted(isShippingChargeExempted);
		saveCart(cart);
		return getCartSummary(cart, cart.getCustomerId(), Cart.PROCUREMENT_TYPE.NORMAL.equalsIgnoreCase(cart.getProcurementType()));
	}
	
	@Override
	//@Transactional(rollbackFor = Exception.class)
	public Cart transferCart(String cartUid, Long customerId) {
		try {
			if (StringUtils.isBlank(cartUid)) {
				throw new IllegalArgumentException("Invalid cart uid specified");
			}
			if (customerId == null) {
				throw new IllegalArgumentException("Invalid customer id specified");
			}
			Cart cart = getCartByUid(cartUid);
			if (cart == null) {
				throw new CartNotFoundException("No cart found with cart uid :: " + cartUid);
			}
			if( !Cart.USER_TYPE.EXTERNAL.equalsIgnoreCase(cart.getUserType()) ) {
				throw new IllegalArgumentException("Invalid user type for transfer : cart user : " + cart.getUserType());
			}
			validateActiveCart(cart);
			if( cart.getCustomerId() != null && !cart.getCustomerId().equals(customerId) ) {
				throw new IllegalArgumentException("Cart has been already owned by customer id : " + cart.getCustomerId());
			}
			Customer customer = customerService.getCustomerDetail(customerId);
			if( customer == null ) {
				throw new NotFoundException("No customer found for customer id : " + customerId + " and cart uid : " + cartUid);
			}
			Cart existCart = cartRepository.findTopByCustomerIdAndStatusAndTypeAndCategoryAndUserTypeOrderByCreatedAtDesc(customerId, Cart.STATUS.CREATED, cart.getType(), cart.getCategory(), cart.getUserType());
			if (existCart != null && !cartUid.equalsIgnoreCase(existCart.getUid())) {
				cart = mergeCart(cartUid, existCart.getUid());
			}
			cart.setCustomerId(customerId);
			cart.setCreatedBy(String.valueOf(customerId));
			cart.setUpdatedBy(String.valueOf(customerId));
			cart.setCustomerFirstName(customer.getFirstName());
			cart.setCustomerLastName(customer.getLastName());
			saveCart(cart);
			return getCartSummary(cart.getUid(), cart.getCustomerId(), true);
		} catch (Exception e) {
			throw e;
		}
	}
	
	@Override
	public Cart addShippingAddress(String cartUid, Long shippingAddressId, Long loginCustomerId) {
		if(StringUtils.isBlank(cartUid)) {
			throw new IllegalArgumentException("Invalid cart uid specified");
		}
		if(shippingAddressId == null) {
			throw new IllegalArgumentException("Invalid shipping address id specified");
		}
		Cart cart = getCartByUid(cartUid);
		if( cart == null ) {
			throw new CartNotFoundException("No cart found with cart uid :: " + cartUid);
		}
		if( cart.getCustomerId() == null ) {
			throw new IllegalArgumentException("Cart doesn't have any customer associated for adding shipment address");
		}
		if( loginCustomerId != null && loginCustomerId > 0 && !(loginCustomerId.equals(cart.getCustomerId())) && Cart.USER_TYPE.EXTERNAL.equalsIgnoreCase(cart.getUserType()) ) {
			throw new AccessNotAllowedException("User not authorized to edit shipping address of cart :: customer id mismatch : login customer id : " + loginCustomerId + " : cart customer id : " + cart.getCustomerId());
		}
		validateActiveCart(cart);
		ShippingAddress shippingAddress = customerService.getShippingAddressByCustomerIdAndShippingAddressId(cart.getCustomerId(), shippingAddressId);
		if( shippingAddress == null ) {
			throw new NotFoundException("No shipping address found for shipping address id : " + shippingAddressId + " and cart uid : " + cartUid);
		}
		//
		if (!(Cart.BUSINESS_CHANNEL.OFFLINE.equalsIgnoreCase(cart.getBusinessChannel())
				&& Cart.BUSINESS_TYPE.B2C.equalsIgnoreCase(cart.getBusinessType()))) {
			int facilityCode = 0;
			try {
				facilityCode = getFacilityIdByPincodeAndCartCategory(String.valueOf(shippingAddress.getPincode()), cart.getCategory());
			} catch (Exception e) {
				LOGGER.error("Error in getting facility for city : {} : Exception : {}", shippingAddress.getCity(), e.getMessage());
			}
			cart.setFacilityCode(facilityCode);
		}
		cart.setPincode(String.valueOf(shippingAddress.getPincode()));
		cart.setShippingAddressId(shippingAddressId);
		validateCartServiceability(cart);
		saveCart(cart);
		//false autoapply is there since don't want coupon to get saved
		cart = getCartSummary(cart.getUid(), cart.getCustomerId(), true);
		if( cart.isNotAvailableLfAssured() || cart.isNotAvailableLfAssured() ) {
			if( cart.isNotAvailableUrgent() ) {
				cart.setPreferredDeliveryOption(Cart.DELIVERY_OPTION.NORMAL);
			}
			if( cart.isNotAvailableLfAssured() ) {
				cart.setPreferredServiceType(Cart.SERVICE_TYPE.NORMAL);
			}
			saveCart(cart);
		}
		return cart;
	}

	@Override
	public Boolean discardCart(String cartUid) {
		if(StringUtils.isBlank(cartUid)) {
			throw new IllegalArgumentException("Invalid cart uid specified");
		}
		Cart cart = getCartByUid(cartUid);
		if( cart == null ) {
			throw new CartNotFoundException("No cart found with cart uid :: " + cartUid);
		}
		validateActiveCart(cart);
		cart.setStatus(Cart.STATUS.DISCARDED);
		saveCart(cart);
		return true;
	}

	@Override
	public void validateActiveCart(Cart cart) {
		if( cart == null ) {
			throw new CartNotFoundException("No cart found for validation");
		}
		List<String> allowedStatus = Arrays.asList(Cart.STATUS.CREATED);
		if( !allowedStatus.contains(cart.getStatus()) ) {
			throw new InvalidCartStatusException("Invalid Cart Status :: " + cart.getStatus());
		}
	}

	@Override
	public void validateCart(Cart cart) {
		if( cart == null ) {
			throw new CartNotFoundException("No cart found for validation");
		}
		if( StringUtils.isNotBlank(cart.getStatus()) && !Cart.STATUS.VALID_CART_STATUS_LIST.contains(cart.getStatus()) ) {
			throw new IllegalArgumentException("Cart status not allowed :: " + cart.getStatus());
		}
		if( StringUtils.isNotBlank(cart.getSource()) && !Cart.SOURCE.VALID_CART_SOURCE_LIST.contains(cart.getSource()) ) {
			throw new IllegalArgumentException("Cart source not allowed :: " + cart.getSource());
		}
		if( StringUtils.isNotBlank(cart.getType()) && !Cart.TYPE.VALID_CART_TYPE_LIST.contains(cart.getType()) ) {
			throw new IllegalArgumentException("Cart type not allowed :: " + cart.getType());
		}
		if( StringUtils.isNotBlank(cart.getPaymentMethod()) && !Cart.PAYMENT_METHOD.VALID_PAYMENT_METHOD_LIST.contains(cart.getPaymentMethod()) ) {
			throw new IllegalArgumentException("Cart payment method not allowed :: " + cart.getPaymentMethod());
		}	
//		if( !Cart.FACILITY_CODE.VALID_CART_FACILITY_CODE_LIST.contains(cart.getFacilityCode()) ) {
//			throw new IllegalArgumentException("Cart facility code not allowed :: " + cart.getFacilityCode() + " :: Allowed facility codes : " + StringUtils.join(Cart.FACILITY_CODE.VALID_CART_FACILITY_CODE_LIST, ','));
//		}
		if( StringUtils.isNotBlank(cart.getSourceType()) && !Cart.SOURCE_TYPE.VALID_CART_SOURCE_TYPE_LIST.contains(cart.getSourceType()) ) {
			throw new IllegalArgumentException("Cart source type not allowed :: " + cart.getSourceType());
		}
		if( StringUtils.isNotBlank(cart.getUserType()) && !Cart.USER_TYPE.VALID_USER_TYPE_LIST.contains(cart.getUserType()) ) {
			throw new IllegalArgumentException("Cart user type not allowed :: " + cart.getUserType());
		}
		if( StringUtils.isNotBlank(cart.getCategory()) && !Cart.CATEGORY.VALID_CATEGORY_LIST.contains(cart.getCategory()) ) {
			throw new IllegalArgumentException("Cart category not allowed :: " + cart.getCategory());
		}
	}

	@Override
	public void validateCartServiceability(Cart cart) {
		if (cart == null) {
			throw new CartNotFoundException("No cart found for validation");
		}
		if (StringUtils.isNotBlank(cart.getPincode())) {
			try {
				PlacePincode placePincode = shippingService.getPlaceInformationByPincode(cart.getPincode());
				if (placePincode == null) {
					throw new NotFoundException("No place info found for pincode " + cart.getPincode());
				}
				if (!placePincode.getIsActive()) {
					throw new UnserviceablePincodeException("Not Serviceable at pincode : " + cart.getPincode());
				}
				if (Cart.CATEGORY.LAB.equalsIgnoreCase(cart.getCategory())) {
					if (!placePincode.getIsLabOrderAvailable()) {
						throw new UnserviceablePincodeException("Lab order Not Serviceable at pincode : " + cart.getPincode());
					}
				}
			} catch (Exception e) {
				LOGGER.error("Error in serviceability check for pincode {} : Exception : {}", cart.getPincode(), e.getMessage());
				throw e;
			}
		}
	}
	
	@Override
	public Cart updateStatus(String cartUid, String status) {
		if(StringUtils.isBlank(cartUid)) {
			throw new IllegalArgumentException("Invalid cart uid specified");
		}
		Cart cart = fetchCartByUid(cartUid);
		if( cart == null ) {
			throw new CartNotFoundException("No cart found with cart uid :: " + cartUid);
		}
		cart.setStatus(status);
		saveCart(cart);
		return getCartSummary(cart, cart.getCustomerId(), Cart.PROCUREMENT_TYPE.NORMAL.equalsIgnoreCase(cart.getProcurementType()));
	}

	@Override
	public Cart getCartByCustomer(Long customerId, String cartType) {
		if(customerId == null) {
			throw new IllegalArgumentException("Invalid customer id specified");
		}
		if( StringUtils.isBlank(cartType) ) {
			cartType = Cart.TYPE.NORMAL;
		}
		Cart cart = cartRepository.findTopByCustomerIdAndStatusAndTypeOrderByCreatedAtDesc(customerId, Cart.STATUS.CREATED, cartType);
		if( cart == null ) {
			throw new CartNotFoundException("No cart found with customer id :: " + customerId);
		}
		return getCartSummary(cart.getUid(), cart.getCustomerId(), true);
	}

	@Override
	public Cart getCartSummary(String cartUid, Long loginCustomerId, boolean autoApplyCoupon) {
		if(StringUtils.isBlank(cartUid)) {
			throw new IllegalArgumentException("Invalid cart uid specified");
		}
		Cart cart = fetchCartByUid(cartUid);
		if( cart == null ) {
			throw new CartNotFoundException("No cart found with cart uid :: " + cartUid);
		}
		return getCartSummary(cart, loginCustomerId, (autoApplyCoupon && Cart.PROCUREMENT_TYPE.NORMAL.equalsIgnoreCase(cart.getProcurementType())));
	}
	
	@Override
	public Cart getCartSummary(Cart cart, Long loginCustomerId, boolean autoApplyCoupon) {
		if( cart == null ) {
			throw new CartNotFoundException("No cart specified");
		}
		Long customerId = cart.getCustomerId();
//		if( customerId == null ) {
//			throw new IllegalArgumentException("Cart doesn't have any customer associated for fetching cart summary");
//		}
		if( loginCustomerId != null && loginCustomerId > 0 && !(loginCustomerId.equals(cart.getCustomerId())) && Cart.USER_TYPE.EXTERNAL.equalsIgnoreCase(cart.getUserType()) ) {
			throw new AccessNotAllowedException("User not authorized to fetch cart summary :: customer id mismatch : login customer id : " + loginCustomerId + " : cart customer id : " + customerId);
		}
	

		//Apply AutoApply / Coupon Discount
		if(customerId != null && autoApplyCoupon) {
			
			if(StringUtils.isBlank(cart.getCouponCode())) {
					try {
						cart = couponService.removeOrApplyDefaultCouponForCart(cart, null);
					} catch (Exception e) {
						cart.setCouponCode("");
						cart.setCouponDiscount(0.0f);
						cart.setCouponDescription("");
						LOGGER.error("Error in applying default coupon for customer : " + customerId + " : cart uid : {} : Exception : {}", cart.getUid(), e.getMessage());
					}					
			} else {
				try {
					cart = couponService.checkCouponForCart(cart.getCouponCode(), cart, "");
				} catch (Exception e) {
					cart.setCouponCode("");
					cart.setCouponDiscount(0.0f);
					cart.setCouponDescription("");
					LOGGER.error("Error in applying coupon for customer : " + customerId + " : cart uid : {} : Exception : {}", cart.getUid(), e.getMessage());
				}				
			}
		}
		
		//Set Patient Details
		if(cart.getPatientId() != null) {
			Patient patient = null;
			try {
			patient = customerService.getPatientByCustomerIdAndPatientId(customerId, cart.getPatientId());
			cart.setPatient(patient);
			}catch (Exception e) {
				LOGGER.error("Error in getPatientByCustomerIdAndPatientId CustomerId : " + customerId + " PatientId ", cart.getPatientId(), e.getMessage());
			}
		}
		
		//Decide DeliveryType Based on Medicines
		Boolean isAvailableForUrgentForMedicines = true;
		Boolean isAvailableForLfAssuredForMedicine = true;
		cart.setDeliveryOption(Cart.DELIVERY_OPTION.NORMAL);
		cart.setServiceType(Cart.SERVICE_TYPE.NORMAL);
		cart.setUrgentDeliveryCharge(0.00);
		if (cart.getCartItems() != null && !cart.getCartItems().isEmpty()) {
			for (CartItem cartIt : cart.getCartItems()) {
				if (Cart.DELIVERY_OPTION.NORMAL.equalsIgnoreCase(cartIt.getAvailableDeliveryOption())) {
					isAvailableForUrgentForMedicines = false;
					break;
				}
			}
			if (isAvailableForUrgentForMedicines) {
				cart.setDeliveryOption(Cart.DELIVERY_OPTION.URGENT);
				cart.setUrgentDeliveryCharge(0.00);
			}
		}

		if (cart.getCartItems() != null && !cart.getCartItems().isEmpty()) {
			for (CartItem cartIt : cart.getCartItems()) {
				if (Cart.SERVICE_TYPE.NORMAL.equalsIgnoreCase(cartIt.getAvailableServiceType())) {
					isAvailableForLfAssuredForMedicine = false;
					break;
				}
			}
			if (isAvailableForLfAssuredForMedicine) {
				if (cart.getPreferredServiceType() != null) {
					cart.setServiceType(cart.getPreferredServiceType());					
				} else {
					cart.setServiceType(Cart.SERVICE_TYPE.LF_ASSURED);
				}
			}
		}		
		//Fetch Shipping Address if cart has shipping address id
		ShippingAddress shippingAddress = null;
		String pincode = null;
		PlacePincode placePincode = null;
		int deliveryTatDays = 2;
		boolean isCODEligibleOrder = true;
		Long shippingAddressId = cart.getShippingAddressId();
		if (shippingAddressId != null && customerId != null) {
			try {
				shippingAddress = customerService.getShippingAddressByCustomerIdAndShippingAddressId(customerId,
						shippingAddressId);
				if (shippingAddress == null) {
					throw new NotFoundException("No shipping address found for customer id : " + customerId
							+ " : shipping address id : " + shippingAddressId + " and cart uid : " + cart.getUid());
				}
				cart.setShippingAddress(shippingAddress);

				// Calculate Facility Code
				if (!(Cart.BUSINESS_CHANNEL.OFFLINE.equalsIgnoreCase(cart.getBusinessChannel())
						&& Cart.BUSINESS_TYPE.B2C.equalsIgnoreCase(cart.getBusinessType()))) {
					try {
						int facilityCode = getFacilityIdByPincodeAndCartCategory(String.valueOf(shippingAddress.getPincode()), cart.getCategory());
						if (facilityCode > 0) {
							cart.setFacilityCode(facilityCode);
						}
					} catch (Exception e) {
						LOGGER.error("Error in getting facility for city : {} : Exception : {}", shippingAddress.getCity(),
								e.getMessage());
					}
				}
				// Calculate estimate delivery date based on pincode
				pincode = String.valueOf(shippingAddress.getPincode());
				if (pincode != null) {
					try {
						placePincode = shippingService.getPlaceInformationByPincode(pincode);
					} catch (Exception e) {
						LOGGER.error("Error in getting place pincode with pincode {}", pincode);
					}
					if (Cart.CATEGORY.LAB.equalsIgnoreCase(cart.getCategory())) {
						if (cart.getAppointmentDate() != null) {
							Timestamp appointmentDate = cart.getAppointmentDate();
							DateTime currentDate = DateTime.now(DateTimeZone.UTC);
							int days = Days.daysBetween(currentDate ,new DateTime(appointmentDate.getTime())).getDays();
							deliveryTatDays = placePincode != null && placePincode.getDeliveryDay() != null
									&& placePincode.getDeliveryDay() > 0 ? placePincode.getDeliveryDay() + days : 2 + days;
						}

						if (cart.isReportHardCopyRequired() && placePincode.getIsReportHardCopyAvailable() != null && placePincode.getIsReportHardCopyAvailable()) {
							cart.setReportDeliveryCharge(placePincode.getReportDeliveryCharge());
						} else {
							cart.setReportDeliveryCharge(0.0);
							cart.setReportHardCopyRequired(false); 
						}
						cart.setShippingChargeExempted(true);
					} else {
						deliveryTatDays = placePincode != null && placePincode.getDeliveryDay() != null
								&& placePincode.getDeliveryDay() > 0 ? placePincode.getDeliveryDay() : 2;
						
						// Calculate shipping charge and min mrp shipping fee
						Map<String, Object> shippingFeeInfoMap = orderService.getApplicableShippingCharge(customerId,
								shippingAddress.getPincode(), Float.parseFloat(String.valueOf(cart.getTotalMrp())));
						cart.setShippingFee(Double.parseDouble(shippingFeeInfoMap.containsKey(SHIPPING_CHARGE_KEY)
								&& shippingFeeInfoMap.get(SHIPPING_CHARGE_KEY) != null && !cart.isShippingChargeExempted()
										? String.valueOf(shippingFeeInfoMap.get(SHIPPING_CHARGE_KEY)) : "0"));
						cart.setMinMrpShippingFee(
								Double.parseDouble(shippingFeeInfoMap.containsKey(MIN_MRP_SHIPPING_CHARGE_KEY)
										&& shippingFeeInfoMap.get(MIN_MRP_SHIPPING_CHARGE_KEY) != null
												? String.valueOf(shippingFeeInfoMap.get(MIN_MRP_SHIPPING_CHARGE_KEY))
												: "0"));
					}
					
					if (cart.getEstimateDeliveryDate() == null) {
						DateTime estimateDeliveryDate = DateTime.now(DateTimeZone.UTC).plusDays(deliveryTatDays);
						cart.setEstimateDeliveryDate(new Timestamp(estimateDeliveryDate.getMillis()));
						if (cart.getPromisedDeliveryDate() == null) {
							cart.setPromisedDeliveryDate(cart.getEstimateDeliveryDate());
						}
					}
					cart.setSellerDetail(getSellerDetails(pincode, cart.getBusinessType()));

					// Check if line item has any price as 0 then set shipping
					// charge as 0
					//TODO remove code : is exepmted when one cart item is zero
					/*
					 * if (cart.getCartItems() != null && !cart.getCartItems().isEmpty()) { for
					 * (CartItem cartIt : cart.getCartItems()) { if ((int) cartIt.getMrp() == 0) {
					 * cart.setShippingFee(0); cart.setShippingChargeExempted(true); break; } } }
					 */

				}
			} catch (UnserviceablePincodeException e) {
				throw e;
			} catch (Exception e) {
				LOGGER.error("Error in fetching shipping address : cart uid : {} : Exception : {}", cart.getUid(),
						e.getMessage());
			}
			Boolean isAvailableForUrgent = false;
			Boolean isAvailableForLfAssured = false;
			cart.setDeliveryOption(Cart.DELIVERY_OPTION.NORMAL);
			cart.setServiceType(Cart.SERVICE_TYPE.NORMAL);
			cart.setUrgentDeliveryCharge(0.00);
			OrderDeliveryObject checkForUrgentDelivery = getCartDeliveryObject(cart);
			double urgentdeliveryCharge = 0;
			if (placePincode != null) {
				urgentdeliveryCharge = placePincode.getUrgentDeliveryCharge();
			}
			List<String> availableDeliveryOption = new ArrayList<>();
			availableDeliveryOption.add(Cart.DELIVERY_OPTION.NORMAL);
			if (checkForUrgentDelivery != null) {
				// cart.setPreferredServiceType(checkForUrgentDelivery.getServiceType());
				cart.setServiceType(checkForUrgentDelivery.getServiceType());
				if (Cart.DELIVERY_OPTION.URGENT.equalsIgnoreCase(checkForUrgentDelivery.getDeliveryOption())) {
					availableDeliveryOption.add(Cart.DELIVERY_OPTION.URGENT);
					isAvailableForUrgent = true;
				}
				if (Cart.SERVICE_TYPE.LF_ASSURED.equalsIgnoreCase(checkForUrgentDelivery.getServiceType())) {
					isAvailableForLfAssured = true;
				}
				if (Cart.DELIVERY_OPTION.URGENT.equalsIgnoreCase(cart.getPreferredDeliveryOption())) {
					if (isAvailableForUrgent) {
						cart.setDeliveryOption(Cart.DELIVERY_OPTION.URGENT);
						cart.setUrgentDeliveryCharge(urgentdeliveryCharge);
						cart.setDeliveryOptionChangeReason(null);
						cart.setServiceType(Cart.SERVICE_TYPE.NORMAL);
						deliveryTatDays = 1;
						cart.setEstimateDeliveryDate(getPromisedDeliveryDateForUrgentOrder());
						cart.setPromisedDeliveryDate(getPromisedDeliveryDateForUrgentOrder());
						if(cart.getPromisedDeliveryDate().before(new Date())){
							DateTime promisedDeliveryDate = DateTime.now(DateTimeZone.UTC).plusDays(1);
							Calendar cal = Calendar.getInstance();
							cal.setTime(promisedDeliveryDate.toDate());
							cal.set(Calendar.HOUR_OF_DAY, 23);
							cal.set(Calendar.MINUTE, 59);
							cal.set(Calendar.SECOND, 58);
							cal.set(Calendar.MILLISECOND, 00);
							cal.setTimeZone(TimeZone.getTimeZone("Asia/Calcutta"));		
							cart.setEstimateDeliveryDate(new Timestamp(cal.getTimeInMillis()));
							cart.setPromisedDeliveryDate(new Timestamp(cal.getTimeInMillis()));
						}
						//cart.setPreferredServiceType(Cart.SERVICE_TYPE.NORMAL);
					} else {
						cart.setDeliveryOption(Cart.DELIVERY_OPTION.NORMAL);
						cart.setDeliveryOptionChangeReason(cart.getDeliveryOptionChangeReason());
						cart.setUrgentDeliveryCharge(0.00);
						//cart.setPreferredDeliveryOption(Cart.DELIVERY_OPTION.NORMAL);
						cart.setNotAvailableUrgent(true);
					}
				} else if (Cart.BUSINESS_CHANNEL.OFFLINE.equalsIgnoreCase(cart.getBusinessChannel())
						&& Cart.BUSINESS_TYPE.B2C.equalsIgnoreCase(cart.getBusinessType())) {
					DateTime promisedDeliveryDate = DateTime.now(DateTimeZone.UTC).plusDays(0);
					Calendar cal = Calendar.getInstance();
					cal.setTime(promisedDeliveryDate.toDate());
					cal.setTimeZone(TimeZone.getTimeZone("Asia/Calcutta"));
					cart.setEstimateDeliveryDate(new Timestamp(cal.getTimeInMillis()));
					cart.setPromisedDeliveryDate(new Timestamp(cal.getTimeInMillis()));
				}
				if (Cart.SERVICE_TYPE.LF_ASSURED.equalsIgnoreCase(cart.getPreferredServiceType())) {
					if (isAvailableForLfAssured) {
						cart.setServiceType(Cart.SERVICE_TYPE.LF_ASSURED);
						cart.setServiceTypeChangeReason(null);
					} else {
						cart.setServiceTypeChangeReason(cart.getServiceTypeChangeReason());
						cart.setServiceType(Cart.SERVICE_TYPE.NORMAL);
						//cart.setPreferredServiceType(Cart.SERVICE_TYPE.NORMAL);
						cart.setNotAvailableLfAssured(true);
					}
				}
			}
			if (Cart.SERVICE_TYPE.NORMAL.equalsIgnoreCase(cart.getPreferredServiceType())) {
				cart.setServiceType(Cart.SERVICE_TYPE.NORMAL);
			}
			cart.setAvailableDeliveryOption(availableDeliveryOption);
			setDeliveryOptionForCartItems(cart.getCartItems(), cart.getDeliveryOption(), cart.getServiceType(),
					deliveryTatDays);
		}
		

		// Wallet Applicable points for customer
		try {
			if (customerId != null) {
				HashMap<String, Object> walletInfo = new HashMap<String, Object>();
				double applicableWalletAmount = cart.getTotalMrp() + cart.getShippingFee() - cart.getDiscount() - cart.getCouponDiscount() + cart.getUrgentDeliveryCharge();
				double couponCashbackEligibleAmount = 0;
				if(cart.getCategory().equals(Cart.CATEGORY.LAB)){
					couponCashbackEligibleAmount = applicableWalletAmount;
				}
				walletInfo = paymentService.getWalletDetails(customerId, applicableWalletAmount,couponCashbackEligibleAmount);
				if ( walletInfo.containsKey("available_cash") && walletInfo.get("available_cash") != null ) {
					cart.setAvailableCash(Double.parseDouble(String.valueOf(walletInfo.get("available_cash"))));
				}
				if (walletInfo.containsKey("applicable_cash") && walletInfo.get("applicable_cash") != null ) {
					cart.setRedeemableCash(Double.parseDouble(String.valueOf(walletInfo.get("applicable_cash"))));
				}
				if (walletInfo.containsKey("available_care_point") && walletInfo.get("available_care_point") != null ) {
					cart.setAvailableCarePoints(Integer.parseInt(String.valueOf(walletInfo.get("available_care_point"))));
				}
				if (walletInfo.containsKey("applicable_point") && walletInfo.get("applicable_point") != null) {
					cart.setRedeemableCarePoints(Integer.parseInt(String.valueOf(walletInfo.get("applicable_point"))));
				}
				if (walletInfo.containsKey("available_coupon_cashback") && walletInfo.get("available_coupon_cashback") != null ) {
					cart.setAvailableCouponCashback(Float.parseFloat(String.valueOf(walletInfo.get("available_coupon_cashback"))));
				}
				if (walletInfo.containsKey("applicable_coupon_cashback") && walletInfo.get("applicable_coupon_cashback") != null ) {
					cart.setRedeemableCouponCashback(Float.parseFloat(String.valueOf(walletInfo.get("applicable_coupon_cashback"))));
				}
				cart.setRedeemedCarePoints(0);
				cart.setRedeemedCash(0);
				cart.setRedeemedCouponCashback(0);
				if( cart.getTotalSalePrice() <= 0 ) {
					cart.setRedeemableCarePoints(0);
					cart.setRedeemableCash(0);
					cart.setRedeemableCouponCashback(0);
				}
			}
		} catch (Exception e) {
			LOGGER.error("Error in fetching wallet points for customer : " + customerId
					+ " : cart uid : {} : Exception : {}", cart.getUid(), e.getMessage());
		}

		//Get Payment Channels
		cart.setPaymentChannels(paymentService.getPaymentChannels(isCODEligibleOrder));
		if( StringUtils.isBlank(cart.getPaymentMethod()) ) {
			cart.setPaymentMethod(Cart.PAYMENT_METHOD.COD);
		}
		
		cart.setTotalSalePrice(
				cart.getTotalMrp() + cart.getShippingFee() - cart.getDiscount() - cart.getCouponDiscount()
						- cart.getRedeemedCarePoints() - cart.getRedeemedCash() - cart.getRedeemedCouponCashback() + cart.getUrgentDeliveryCharge() + cart.getReportDeliveryCharge());
		if (cart.getTotalSalePrice() <= 0) {
			cart.setTotalSalePrice(0);
		}
		cart.setTotalPayableAmount(cart.getTotalMrp() + cart.getShippingFee() - cart.getDiscount() - cart.getCouponDiscount()
				- cart.getRedeemableCarePoints() - cart.getRedeemableCash() - cart.getRedeemableCouponCashback() + cart.getUrgentDeliveryCharge() + cart.getReportDeliveryCharge());
		cart.calculateCouponCashback();
		if (cart.getTotalPayableAmount() <= 0) {
			cart.setTotalPayableAmount(0);
		}
		return cart;
	}

	private SellerDetail getSellerDetails(String pincode, String businessType) {
		SellerDetail sellerDetail = null;
		try {
			FacilityPincodeMapping facilityPincodeMapping = facilityService.getFacilityPincodeMapping(pincode);
			if (facilityPincodeMapping != null) {
				sellerDetail = new SellerDetail();
				sellerDetail.setWholesaleFacilityId(facilityPincodeMapping.getWholesaleFacilityId());
				sellerDetail.setWholesaleFaciltyName(facilityPincodeMapping.getWholesaleFaciltyName());
				if (!Cart.BUSINESS_TYPE.B2B.equalsIgnoreCase(businessType)) {
					sellerDetail.setRetailFacilityId(facilityPincodeMapping.getRetailFacilityId());
					sellerDetail.setRetailFaciltyName(facilityPincodeMapping.getRetailFaciltyName());
				}
			}
		} catch (Exception e) {
			LOGGER.error("Error in getting seller Details for pincode " + pincode + " : Exception : " + e.getMessage());
		}
		return sellerDetail;
	}

	@Override
	public Timestamp getPromisedDeliveryDateForUrgentOrder() {
		DateTime promisedDeliveryDate = DateTime.now(DateTimeZone.UTC).plusDays(0);
		Calendar cal = Calendar.getInstance();
		cal.setTime(promisedDeliveryDate.toDate());
		cal.set(Calendar.HOUR_OF_DAY, 23);
		cal.set(Calendar.MINUTE, 59);
		cal.set(Calendar.SECOND, 58);
		cal.set(Calendar.MILLISECOND, 00);
		cal.setTimeZone(TimeZone.getTimeZone("Asia/Calcutta"));
		return new Timestamp(cal.getTimeInMillis());
	}
	
	private void setDeliveryOptionForCartItems(List<CartItem> cartItems, String deliveryOption, String serviceType,
			int deliveryTatDays) {
		if (cartItems != null && !cartItems.isEmpty()) {
			for (CartItem cartIt : cartItems) {
				cartIt.setAvailableDeliveryOption(deliveryOption);
				cartIt.setAvailableServiceType(serviceType);
				cartIt.setMinDeliveryDay(deliveryTatDays > 0 ? deliveryTatDays - 1 : 0);
				cartIt.setMaxDeliveryDay(deliveryTatDays);
			}
		}
	}

	@Override
	//@Transactional(rollbackFor = Exception.class)
	public Cart deleteCartItem(String cartUid, Long cartItemId) {
		try {
			if(StringUtils.isBlank(cartUid)) {
				throw new IllegalArgumentException("Invalid cart uid specified");
			}
			Cart cart = getCartByUid(cartUid);
			if (cart == null) {
				throw new CartNotFoundException("No cart found with cart uid :: " + cartUid);
			}
			validateActiveCart(cart);
			if( cartItemId == null ) {
				throw new IllegalArgumentException("Invalid cart item id specified");
			}
			CartItem cartItem = cartItemService.findByIdAndCartUid(cartItemId, cartUid);
			if (cartItem == null) {
				throw new CartItemNotFoundException("No cart item found with cart uid :: " + cartUid + " and cart item id :: " + cartItemId);
			}
			cartItemService.deleteCartItem(cartItemId);
			if(cart.isMembershipAdded() && cartItem.getProductCategory().equalsIgnoreCase(PRODUCT_CATEGORY.MEMBERSHIP_CARD)) {
				cart.setMembershipAdded(false);
				
			}
			double totalMrp = cart.getTotalMrp() - (cartItem.getQuantity() * cartItem.getMrp());
			totalMrp = totalMrp > 0 ? totalMrp : 0;
			double totalSalePrice = cart.getTotalSalePrice() - (cartItem.getQuantity() * cartItem.getSellingPrice());
			totalSalePrice = totalSalePrice > 0 ? totalSalePrice : 0;

			cart.setTotalMrp(totalMrp);
			cart.setTotalSalePrice(totalSalePrice);
			cart.setItemCount(cart.getItemCount() > 0 ? cart.getItemCount() - 1 : 0);
			saveCart(cart);
			return getCartSummary(cart.getUid(), cart.getCustomerId(), true);
		} catch (Exception e) {
			throw e;
		}
	}
	
	@Override
	public Cart updateCartItem(String cartUid, Long cartItemId, CartItem cartItem, boolean isAllowed) throws Exception {
		try {
			if(StringUtils.isBlank(cartUid)) {
				throw new IllegalArgumentException("Invalid cart uid specified");
			}
			Cart cart = getCartByUid(cartUid);
			if (cart == null) {
				throw new CartNotFoundException("No cart found with cart uid :: " + cartUid);
			}
			validateActiveCart(cart);
			if( cartItemId == null ) { 
				throw new IllegalArgumentException("Invalid cart item id specified");
			}
			CartItem cartItemObj = cartItemService.findByIdAndCartUid(cartItemId, cartUid);
			if (cartItemObj == null) {
				throw new CartItemNotFoundException("No cart item found with cart uid :: " + cartUid + " and cart item id :: " + cartItemId);
			}
			long oldCartItemQty = cartItemObj.getQuantity();
			double oldCartItemMrp = cartItemObj.getMrp();
			double oldCartItemSalePrice = cartItemObj.getSellingPrice();
			cartItemObj.setQuantity(cartItem.getQuantity() > 0 ? cartItem.getQuantity() : cartItemObj.getQuantity());
			cartItemObj.setPatientId(cartItem.getPatientId() > 0 ? cartItem.getPatientId() : cartItemObj.getPatientId());
			cartItemObj.setAutoSuggestQty(Boolean.FALSE);
			List<CartItem> cartItems = setPatientNameInCartItems(cart.getCustomerId(), Arrays.asList(cartItemObj));
			cartItems = cartItemService.fetchUpdatedCartItemInventoryStock(cart.getFacilityCode(), cart.getPatientId(), cartItems, cart.getRepeatDay(), Boolean.FALSE, cart.getPincode(), cart.getBusinessType());
			cartItem = cartItems != null ? cartItems.get(0) : cartItemObj ;
			if ( cartItem.getMaxOrderQuantity() != null && cartItem.getMaxOrderQuantity() > 0 && cartItem.getQuantity() > cartItem.getMaxOrderQuantity() && Cart.SOURCE.MWEB.equalsIgnoreCase(cart.getSource())) {
				throw new MaxPermissibleLimitReached("Max permissible quantity limit reached for medicine : " + cartItem.getName());
			} else if (cartItem.isExcessiveOrderedQuantity() && !isAllowed && Cart.SOURCE.MWEB.equalsIgnoreCase(cart.getSource())) {
				throw new MaxOrderedQuantityExceeded("Max ordered quantity for customer " +cart.getCustomerFullName()+ " exceeded for medicine : " + cartItem.getName());
			}
			cartItemService.saveCartItem(cartItem);
			double totalMrp = cart.getTotalMrp() - (oldCartItemQty * oldCartItemMrp);
			double totalSalePrice = cart.getTotalSalePrice() - (oldCartItemSalePrice * oldCartItemQty);
			totalMrp = totalMrp + (cartItem.getQuantity() * cartItem.getMrp());
			totalSalePrice = totalSalePrice + (cartItem.getQuantity() * cartItem.getSellingPrice());
			totalMrp = totalMrp > 0 ? totalMrp : 0;
			totalSalePrice = totalSalePrice > 0 ? totalSalePrice : 0;
			cart.setTotalMrp(totalMrp);
			cart.setTotalSalePrice(totalSalePrice);
			saveCart(cart);
			return getCartSummary(cart.getUid(), cart.getCustomerId(), true);
		} catch (MaxOrderedQuantityExceeded | MaxPermissibleLimitReached e) {
			throw e;
		}  catch (Exception e) {
			throw e;
		}
	}
	
	@Override
	//@Transactional(rollbackFor = Exception.class)
	public Cart deleteCartItemBySku(String cartUid, String sku) {
		try {
			if(StringUtils.isBlank(cartUid)) {
				throw new IllegalArgumentException("Invalid cart uid specified");
			}
			Cart cart = getCartByUid(cartUid);
			if (cart == null) {
				throw new CartNotFoundException("No cart found with cart uid :: " + cartUid);
			}
			validateActiveCart(cart);
			if( StringUtils.isBlank(sku) ) {
				throw new IllegalArgumentException("Invalid cart sku specified");
			}
			CartItem cartItem = cartItemService.findBySkuAndCartUidAndPatientId(sku, cart.getPatientId(), cartUid);
			if (cartItem == null) {
				throw new CartItemNotFoundException("No cart item found with cart uid :: " + cartUid + " and sku :: " + sku);
			}
			cartItemService.deleteCartItem(cartItem.getId());
			
			if(cart.isMembershipAdded() && cartItem.getProductCategory().equalsIgnoreCase(PRODUCT_CATEGORY.MEMBERSHIP_CARD)) {
				cart.setMembershipAdded(false);	
			}
			double totalMrp = cart.getTotalMrp() - (cartItem.getQuantity() * cartItem.getMrp());
			totalMrp = totalMrp > 0 ? totalMrp : 0;
			double totalSalePrice = cart.getTotalSalePrice() - (cartItem.getQuantity() * cartItem.getSellingPrice());
			totalSalePrice = totalSalePrice > 0 ? totalSalePrice : 0;

			cart.setTotalMrp(totalMrp);
			cart.setTotalSalePrice(totalSalePrice);
			cart.setItemCount(cart.getItemCount() > 0 ? cart.getItemCount() - 1 : 0);
			saveCart(cart);
			return getCartSummary(cart.getUid(), cart.getCustomerId(), true);
		} catch (Exception e) {
			throw e;
		}
	}
	
	@Override
	//@Transactional(rollbackFor = Exception.class)
	public Cart transferRefillCart(String refillCartUid, long loginCustomerId) {
		Cart refillCart = fetchCartByUid(refillCartUid);
		validateActiveCart(refillCart);
		if( refillCart.getCustomerId() == null ) {
			throw new IllegalArgumentException("Refill Cart doesn't have any customer associated for adding shipment address");
		}
		if( loginCustomerId > 0 && !refillCart.getCustomerId().equals(loginCustomerId) ) {
			throw new AccessNotAllowedException("User not authorized to transfer refill cart :: customer id mismatch : login customer id : " + loginCustomerId + " : cart customer id : " + refillCart.getCustomerId());
		}
		Cart activeNormalRefillCart = new Cart();
		try {
			activeNormalRefillCart = getCartByCustomer(loginCustomerId, Cart.CATEGORY.MEDICINE);
			if( activeNormalRefillCart == null ) {
				throw new CartNotFoundException("No active cart found for customer : " + loginCustomerId);
			}
			activeNormalRefillCart = resetCart(activeNormalRefillCart.getUid(), refillCart.getCustomerId());
		} catch(CartNotFoundException e) {
			activeNormalRefillCart.setFacilityCode(refillCart.getFacilityCode());
			activeNormalRefillCart.setSource(Cart.SOURCE.MWEB);
			activeNormalRefillCart = addCart(activeNormalRefillCart);
		}
		activeNormalRefillCart.setSourceType(Cart.SOURCE_TYPE.REFILL);
		activeNormalRefillCart.setFacilityCode(refillCart.getFacilityCode());
		activeNormalRefillCart.setCustomerId(refillCart.getCustomerId());
		activeNormalRefillCart.setPatientId(refillCart.getPatientId());
		activeNormalRefillCart.setCustomerFirstName(refillCart.getCustomerFirstName());
		activeNormalRefillCart.setCustomerLastName(refillCart.getCustomerLastName());
		activeNormalRefillCart.setPatientFirstName(refillCart.getPatientFirstName());
		activeNormalRefillCart.setPatientLastName(refillCart.getPatientLastName());
		activeNormalRefillCart.setShippingAddressId(refillCart.getShippingAddressId());
		saveCart(activeNormalRefillCart);
		activeNormalRefillCart = mergeCart(refillCart.getUid(), activeNormalRefillCart.getUid());
		refillCart.setStatus(Cart.STATUS.CREATED);
		saveCart(refillCart);
		return fetchCartByUid(activeNormalRefillCart.getUid());
	}
	
	@Override
	//@Transactional(rollbackFor = Exception.class)
	public Cart deleteCartPrescription(String cartUid, Long cartPrescriptionId) {
		try {
			if(StringUtils.isBlank(cartUid)) {
				throw new IllegalArgumentException("Invalid cart uid specified");
			}
			Cart cart = getCartByUid(cartUid);
			if (cart == null) {
				throw new CartNotFoundException("No cart found with cart uid :: " + cartUid);
			}
			validateActiveCart(cart);
			if( cartPrescriptionId == null ) {
				throw new IllegalArgumentException("Invalid cart prescription id specified");
			}
			CartPrescription cartPrescription = cartPrescriptionService.getPrescriptionByIdAndCartUid(cartPrescriptionId, cartUid);
			if (cartPrescription == null) {
				LOGGER.error("No cart prescription found with cart uid :: " + cartUid + " and cart prescription id :: " + cartPrescriptionId);
			} else {
				cartPrescriptionService.deleteCartPrescription(cartPrescriptionId);
				cart.setPrescriptionCount(cart.getPrescriptionCount() > 0 ? cart.getPrescriptionCount() - 1 : 0);
				saveCart(cart);
			}
			return getCartSummary(cart.getUid(), cart.getCustomerId(), true);
		} catch (Exception e) {
			throw e;
		}
	}

	@Override
	public Cart deleteCartPrescriptions(String cartUid, List<Long> cartPrescriptionIds) {
		try {
			if(StringUtils.isBlank(cartUid)) {
				throw new IllegalArgumentException("Invalid cart uid specified");
			}
			Cart cart = getCartByUid(cartUid);
			if (cart == null) {
				throw new CartNotFoundException("No cart found with cart uid :: " + cartUid);
			}
			validateActiveCart(cart);
			if( cartPrescriptionIds == null || cartPrescriptionIds.isEmpty() ) {
				throw new IllegalArgumentException("Invalid cart prescription ids specified");
			}
			List<CartPrescription> cartPrescriptions = cartPrescriptionService.getPrescriptionsByPrescriptionIds(cartPrescriptionIds);
			if (cartPrescriptions == null || cartPrescriptions.isEmpty()) {
				LOGGER.error("No cart prescriptions found specified with cart uid :: " + cartUid);
			} else {
				cartPrescriptionService.deleteCartPrescriptions(cartPrescriptions);
				long prescriptionNewCount = cart.getPrescriptionCount() - cartPrescriptions.size();
				cart.setPrescriptionCount(cart.getPrescriptionCount() > 0 && prescriptionNewCount > 0 ? prescriptionNewCount: 0);
				saveCart(cart);
			}
			return getCartSummary(cart.getUid(), cart.getCustomerId(), true);
		} catch (Exception e) {
			throw e;
		}
	}
	
	@Override
	public Page<Cart> getCartByCustomerIdAndType(long customerId, String type, Pageable pageable) {
		if (customerId <= 0 || StringUtils.isBlank(type) || !Cart.TYPE.VALID_CART_TYPE_LIST.contains(type) || pageable == null) {
			throw new IllegalArgumentException("Invalid parameters provided");
		}
		List<String> statuses = new ArrayList<String>();
		statuses.add(Cart.STATUS.CREATED);
		if(type.equals(Cart.TYPE.REFILL)) {
			return getRefillCarts(customerId);
		}
		Page<Cart> carts = cartRepository.findAllByCustomerIdAndTypeAndStatusIn(customerId, type, statuses, pageable);
		for( int i = 0; i < carts.getSize(); i++ ) {
			try {
				carts.getContent().set(i, fetchCartByUid(carts.getContent().get(i).getUid()));
			} catch(Exception e) {
				LOGGER.error(e.getMessage());
			}
		}
		if (carts == null || !carts.hasContent() || carts.getContent() == null) {
			throw new CartNotFoundException("No cart found for customer-id: " + customerId + " and type: " + type);
		}
		return carts;

	}
	
	@Override
	public Page<Cart> getRefillCarts(long customerId) {
		Page<RefillLead> refillLeads = refillLeadService.getRefillLead(customerId);
		if(refillLeads == null || refillLeads.getContent() == null) {
			return new PageImpl<Cart>(new ArrayList<Cart>(), null, 0);
		}
		List<String> cartUIds = new ArrayList<String>();
		Map<String, Timestamp> cartRefillDate = new HashMap<String, Timestamp>();
		for (RefillLead refillLead : refillLeads.getContent()) {
			if (StringUtils.isNotBlank(refillLead.getCartId())) {
				cartUIds.add(refillLead.getCartId());
				cartRefillDate.put(refillLead.getCartId(), refillLead.getOrderDueDate());
			}
		}
		List<Cart> carts = this.fetchCartsByUids(cartUIds);
		for(Cart cart : carts) {
			cart.setRefillDate(cartRefillDate.get(cart.getUid()));
			long milliseconds1 = cartRefillDate.get(cart.getUid()).getTime();
			long milliseconds2 = System.currentTimeMillis();
			long diff = milliseconds1 - milliseconds2;
			long diffDays = diff / (24 * 60 * 60 * 1000);
			cart.setRefillDays(diffDays);
		}
		Pageable pageable = new PageRequest(refillLeads.getNumber(), refillLeads.getSize());
		return new PageImpl<Cart>(carts, pageable, refillLeads.getTotalElements());
		
	}

	@Override
	//@Transactional(rollbackFor = Exception.class)
	public Cart assignPatientToCart(String cartUid, Long patientId, Long loginCustomerId) {
		try {
			if(StringUtils.isBlank(cartUid)) {
				throw new IllegalArgumentException("Invalid cart uid specified");
			}
			if(patientId == null) {
				throw new IllegalArgumentException("Invalid patient id specified");
			}
			Cart cart = getCartByUid(cartUid);
			if (cart == null) {
				throw new CartNotFoundException("No cart found with cart uid :: " + cartUid);
			}
			validateActiveCart(cart);
			Long customerId = cart.getCustomerId();
			if( customerId == null || customerId <= 0 ) {
				throw new IllegalArgumentException("No customer id found for cart uid :: " + cartUid);
			}
			Cart oldCart = cartRepository.findTopByCustomerIdAndStatusAndTypeAndCategoryAndUserTypeOrderByCreatedAtDesc(customerId, Cart.STATUS.CREATED, cart.getType(), cart.getCategory(), cart.getUserType());
			if (oldCart != null && !cartUid.equalsIgnoreCase(oldCart.getUid()) ) {
				cart = mergeCart(cartUid, oldCart.getUid());
			}
			if (cart == null) {
				throw new CartNotFoundException("No cart found with cart uid :: " + cartUid);
			}
			Patient patient = customerService.getPatientByCustomerIdAndPatientId(customerId, patientId);
			if( patient == null ) {
				throw new NotFoundException("No patient found for customer id : " + customerId + " and cart uid : " + cartUid);
			}
			cartPrescriptionService.updatePatientIdInPrescriptionsByCartUid(cart.getUid(), patientId);
			cart.setPatientId(patientId);
			cart.setPatientFirstName(patient.getFirstName());
			cart.setPatientLastName(patient.getLastName());
			List<CartItem> cartItems = cartItemService.getCartItemsByCartUid(cart.getUid());
			if(cartItems != null && !cartItems.isEmpty()) {
				cartItems.parallelStream().forEach(oi -> {
					{
						oi.setPatientId(patientId);
					}
				});
				cartItemService.saveCartItems(cartItems);
			}
			saveCart(cart);
			return getCartSummary(cart.getUid(), cart.getCustomerId(), true);
		} catch (Exception e) {
			throw e;
		}
	}
	
	@Override
	//@Transactional(rollbackFor = Exception.class)
	public Cart mergeCart(String cartUidMergeFrom, String cartUidMergeTo) {
		if (StringUtils.isBlank(cartUidMergeFrom) || StringUtils.isBlank(cartUidMergeTo)) {
			throw new IllegalArgumentException("Invalid cart uid's provided for merging");
		}
		Cart mergedCart = fetchCartByUid(cartUidMergeTo);
		if (mergedCart == null) {
			throw new CartNotFoundException("No cart found to be merged with cart uid :: " + cartUidMergeTo);
		}
		if( cartUidMergeFrom.equalsIgnoreCase(cartUidMergeTo) ) {
			return mergedCart;
		}
		Cart cartToMerge = fetchCartByUid(cartUidMergeFrom);
		if (cartToMerge == null) {
			throw new CartNotFoundException("No cart found to merge from with cart uid :: " + cartUidMergeFrom);
		}
		if (cartToMerge.getPatientId() != null && mergedCart.getPatientId() != null && !cartToMerge.getPatientId().equals(mergedCart.getPatientId())) {
			discardCart(cartToMerge.getUid());
			return mergedCart;
		}
		if( mergedCart.getCartItems() == null ) {
			mergedCart.setCartItems(new ArrayList<CartItem>());
		}
		if( mergedCart.getCartPrescriptions() == null ) {
			mergedCart.setCartPrescriptions(new ArrayList<CartPrescription>());
		}
		Map<String, CartItem> oldCartItemsMap = new HashMap<String, CartItem>();
		int extraItemCount = 0;
		int extraPrescriptionCount = 0;
		CartItem cartItemToMerge = null;
		if (cartToMerge.getCartItems() != null && !cartToMerge.getCartItems().isEmpty()) {
			for (int i = 0; i < cartToMerge.getCartItems().size(); i++) {
				try {
					cartItemToMerge = new CartItem();
					ConvertUtils.register(new SqlTimestampConverter(null), Timestamp.class);
					BeanUtilsBean2.getInstance().copyProperties(cartItemToMerge, cartToMerge.getCartItems().get(i));
					cartItemToMerge.setId(0);
					cartItemToMerge.setCartUid(cartUidMergeTo);
					oldCartItemsMap.put(cartToMerge.getCartItems().get(i).getSku(), cartItemToMerge);
				} catch( Exception e ) {
					LOGGER.error("Error occured while changing cart items : exception : " + e.getMessage());
				}
			}
		}
		if (cartToMerge.getCartPrescriptions() != null && !cartToMerge.getCartPrescriptions().isEmpty()) {
			extraPrescriptionCount = cartToMerge.getCartPrescriptions().size();
			CartPrescription cartPrecriptionToMerge = null;
			for (int i = 0; i < cartToMerge.getCartPrescriptions().size(); i++) {
				try {
					cartPrecriptionToMerge = new CartPrescription();
					ConvertUtils.register(new SqlTimestampConverter(null), Timestamp.class);
					BeanUtilsBean2.getInstance().copyProperties(cartPrecriptionToMerge, cartToMerge.getCartPrescriptions().get(i));
					cartPrecriptionToMerge.setId(0);
					cartPrecriptionToMerge.setCartUid(mergedCart.getUid());
					mergedCart.getCartPrescriptions().add(cartPrecriptionToMerge);
				} catch( Exception e ) {
					LOGGER.error("Error occured while changing cart prescriptions : exception : " + e.getMessage());
				}
			}
		}
		if (mergedCart.getCartItems() != null && !mergedCart.getCartItems().isEmpty()) {
			for (CartItem cartItem : mergedCart.getCartItems()) {
				if (oldCartItemsMap.containsKey(cartItem.getSku())) {
					cartItem.setQuantity(oldCartItemsMap.get(cartItem.getSku()).getQuantity() + cartItem.getQuantity());
					oldCartItemsMap.remove(cartItem.getSku());
				}
			}
		}
		extraItemCount = oldCartItemsMap.size();
		Collection<CartItem> oldCartItemCollection = oldCartItemsMap.values();
		List<CartItem> oldCartItems = new ArrayList<CartItem>(oldCartItemCollection);
		mergedCart.getCartItems().addAll(oldCartItems);
		cartPrescriptionService.savePrescriptions(mergedCart.getCartPrescriptions());
		cartItemService.saveCartItems(mergedCart.getCartItems());
		mergedCart.setPrescriptionCount(mergedCart.getPrescriptionCount() + extraPrescriptionCount);
		mergedCart.setItemCount(mergedCart.getItemCount() + extraItemCount);
		mergedCart.setTotalMrp(mergedCart.getTotalMrp() + cartToMerge.getTotalMrp());
		mergedCart.setTotalSalePrice(mergedCart.getTotalSalePrice() + cartToMerge.getTotalSalePrice());
		saveCart(mergedCart);
		discardCart(cartToMerge.getUid());
		return fetchCartByUid(cartUidMergeTo);
	}

	@Override
	public Cart updateCartParameters(String cartUid, Map<String, Object> updateParams) throws Exception {
		if( updateParams == null || updateParams.isEmpty() ) {
			throw new IllegalArgumentException("No update parameter's found to update for cart uid : " + cartUid);
		}
		try {
			Map<String, Object> params = new HashMap<String, Object>();
			updateParams.forEach((k, v) -> params.put(CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, k), v));
			Cart cart = fetchCartByUid(cartUid);
			if (cart == null) {
				throw new CartNotFoundException("No cart found with cart uid :: " + cartUid);
			}
			validateActiveCart(cart);
			ConvertUtils.register(new SqlTimestampConverter(null), Timestamp.class);
			BeanUtilsBean2.getInstance().copyProperties(cart, params);
			saveCart(cart);
			return getCartSummary(cart, cart.getCustomerId(), Cart.PROCUREMENT_TYPE.NORMAL.equalsIgnoreCase(cart.getProcurementType()));
		} catch (Exception e) {
			throw e;
		}
	}

	@Override
	public Cart applyCouponForCart(String cartUid, String couponCode, Long loginCustomerId, String authClientId) {
		if(StringUtils.isBlank(couponCode)) {
			throw new IllegalArgumentException("No coupon code specified to apply");
		}
		Cart cart = fetchCartByUid(cartUid);
		if (cart == null) {
			throw new CartNotFoundException("No cart found with cart uid :: " + cartUid);
		}
		validateActiveCart(cart);
		Long customerId = cart.getCustomerId();
		if( customerId == null || customerId <= 0 ) {
			throw new IllegalArgumentException("No customer id found for cart uid :: " + cartUid + ", customer id is mandatory for applying coupon");
		}
		if( loginCustomerId != null && loginCustomerId > 0 && !(loginCustomerId.equals(cart.getCustomerId())) && Cart.USER_TYPE.EXTERNAL.equalsIgnoreCase(cart.getUserType()) ) {
			throw new AccessNotAllowedException("User not authorized to apply coupon to cart :: customer id mismatch : login customer id : " + loginCustomerId + " : cart customer id : " + customerId);
		}
	
		cart = couponService.checkCouponForCart(couponCode, cart, authClientId);
		saveCart(cart);
		return getCartSummary(cart, loginCustomerId, false);
	}

	@Override
	public Cart removeCouponForCart(String cartUid, String couponCode, Long loginCustomerId, String authClientId) {
		if(StringUtils.isBlank(couponCode)) {
			throw new IllegalArgumentException("No coupon code specified for removal");
		}
		Cart cart = fetchCartByUid(cartUid);
		if (cart == null) {
			throw new CartNotFoundException("No cart found with cart uid :: " + cartUid);
		}
		validateActiveCart(cart);
		Long customerId = cart.getCustomerId();
		if( customerId == null || customerId <= 0 ) {
			throw new IllegalArgumentException("No customer id found for cart uid :: " + cartUid);
		}
		if( loginCustomerId != null && loginCustomerId > 0 && !(loginCustomerId.equals(cart.getCustomerId())) && Cart.USER_TYPE.EXTERNAL.equalsIgnoreCase(cart.getUserType()) ) {
			throw new AccessNotAllowedException("User not authorized to apply coupon to cart :: customer id mismatch : login customer id : " + loginCustomerId + " : cart customer id : " + customerId);
		}
//		if( StringUtils.isBlank(cart.getCouponCode()) ) {
//			throw new IllegalArgumentException("No coupon code specified for removal");
//		}
//		if( !couponCode.equalsIgnoreCase(cart.getCouponCode()) ) {
//			throw new IllegalArgumentException("Coupon specified doesn't match cart coupon : " + cart.getCouponCode());
//		}
		cart.setCouponCode(StringUtils.EMPTY);
		cart.setCouponDescription(StringUtils.EMPTY);
		cart.setCouponDiscount(0);
		cart.setShortCouponDescription(StringUtils.EMPTY);
		saveCart(cart);
		cart = couponService.removeOrApplyDefaultCouponForCart(cart, authClientId);
		saveCart(cart);
		return getCartSummary(cart, loginCustomerId, false);
	}

	@Override
	public Cart switchCartFacilityCode(String cartUid, Integer facilityCode) {
		if(facilityCode == null) {
			throw new IllegalArgumentException("Invalid facility code specified");
		}
		Cart cart = getCartByUid(cartUid);
		if (cart == null) {
			throw new CartNotFoundException("No cart found with cart uid :: " + cartUid);
		}
		validateActiveCart(cart);
		if( !Cart.FACILITY_CODE.VALID_CART_FACILITY_CODE_LIST.contains(facilityCode) ) {
			throw new IllegalArgumentException("Invalid cart facility code :: " + facilityCode);
		}
		cart.setFacilityCode(facilityCode);
		saveCart(cart);
		return getCartSummary(cart.getUid(), cart.getCustomerId(), true);
	}

	@Override
	public Cart removeShippingChargeForCart(String cartUid, Long loginCustomerId) {
		if(StringUtils.isBlank(cartUid)) {
			throw new IllegalArgumentException("Invalid cart uid specified");
		}
		Cart cart = fetchCartByUid(cartUid);
		if (cart == null) {
			throw new CartNotFoundException("No cart found with cart uid :: " + cartUid);
		}
		validateActiveCart(cart);
		if( cart.getShippingAddressId() == null || cart.getShippingAddressId() <= 0 ) {
			throw new IllegalArgumentException("No cart shipping address id is associated with cart uid :: " + cartUid + " to remove shipping charge");
		}
		cart.setShippingChargeExempted(true);
		saveCart(cart);
		return getCartSummary(cart, cart.getCustomerId(), Cart.PROCUREMENT_TYPE.NORMAL.equalsIgnoreCase(cart.getProcurementType()));
	}
	
	@Override
	public Cart updatePreferredDeliveryOption(String cartUid, String deliveryOption, Long loginCustomerId, Map<String, Object> map) {
		if (StringUtils.isBlank(deliveryOption)) {
			throw new IllegalArgumentException("No delivery preference specified");
		}
		Cart cart = fetchCartByUid(cartUid);
		if (cart == null) {
			throw new CartNotFoundException("No cart found with cart uid :: " + cartUid);
		}
		validateActiveCart(cart);
		Long customerId = cart.getCustomerId();
		if (customerId == null || customerId <= 0) {
			throw new IllegalArgumentException("No customer id found for cart uid :: " + cartUid
					+ ", customer id is mandatory for selecting delivery preference");
		}
//		if (loginCustomerId != null && !(loginCustomerId.equals(customerId))) {
//			throw new AccessNotAllowedException(
//					"User not authorized to select delivery preference to cart :: customer id mismatch : login customer id : "
//							+ loginCustomerId + " : cart customer id : " + customerId);
//		}
		if (map != null && ! map.isEmpty()){
			String reason = map.containsKey("delivery_option_change_reason") ? (String) map.get("delivery_option_change_reason") : null;
			String comment = map.containsKey("delivery_option_change_comment") ? (String) map.get("delivery_option_change_comment") : null;
			if (reason != null) {
				reason += comment != null ? " (" + comment + ")" : "";
			} else {
				reason = comment != null ? comment : "";
			}
			if (StringUtils.isNotBlank(reason)) {
				 cart.setManualDeliveryOptionChangeReason(reason);
			}
		}
		cart.setPreferredDeliveryOption(deliveryOption);
		saveCart(cart);
		return getCartSummary(cart, loginCustomerId, Cart.PROCUREMENT_TYPE.NORMAL.equalsIgnoreCase(cart.getProcurementType()));
	}

	public Cart getReorderCart(long customerId, long orderId, String source, User user, int repeatDays) throws Exception {
		Order order = orderService.get(orderId);
		if (Cart.SOURCE.JIVA.equals(source)) {
			Cart cart = cartRepository.findTopByCustomerIdAndPatientIdAndTypeAndStatusOrderByCreatedAtDesc(order.getCustomerId(), order.getPatientId(), Cart.TYPE.JIVA, Cart.STATUS.CREATED);
			if(cart == null) {
				//throw new CartNotFoundException("No valid cart found with customer-id :: " + order.getCustomerId() + " patient-id :: " + order.getPatientId());
				cart = new Cart();
				cart.setSource(source);
				cart.setType(Cart.TYPE.JIVA);
				cart.setCustomerId(order.getCustomerId());
				cart.setPatientId(order.getPatientId());
				cart.setFacilityCode(cart.getFacilityCode() != null ? cart.getFacilityCode() : 0);
				cart.setStatus(Cart.STATUS.CREATED);
				cart.setCreatedBy(String.valueOf(user.getId()));
				cart = createJivaCart(cart);
			}
			cart.setUpdatedBy(String.valueOf(user.getId()));
			cart.setRepeatDay(repeatDays);
			cart.setCategory(StringUtils.isNotBlank(order.getCategory()) ? order.getCategory() : Order.CATEGORY.MEDICINE);
			List<CartItem> cartItems = new ArrayList<CartItem>();
			for (OrderItem orderItem : order.getOrderItems()) {
				if (!orderItem.getProductCategory().equalsIgnoreCase(OrderItem.PRODUCT_CATEGORY.MEMBERSHIP_CARD)) {
					CartItem cartItem = new CartItem();
					cartItem.setSku(orderItem.getSku());
					cartItem.setName(orderItem.getName());
					cartItem.setPatientId(orderItem.getPatientId() != null ? cartItem.getPatientId() : cart.getPatientId());
					cartItem.setBrand(orderItem.getBrandName());
					cartItem.setQuantity(orderItem.getOrderedQuantity());
					cartItem.setProductCategory(StringUtils.isNotBlank(orderItem.getProductCategory()) ? orderItem.getProductCategory() : cart.getCategory());
					cartItems.add(cartItem);
				}
			}
			
//			saveCart(cart);
			cart.setCouponCode(null);
			cart.setCouponDescription(null);
			cart.setCouponDiscount(0);
			cart.setShortCouponDescription(null);
			return addCartItems(cart.getUid(), cartItems);
		}
		return null;
	}
	
	@Override
	public Cart changePackagingType(User user, String cartUid, String packagingType) {
		if(StringUtils.isBlank(cartUid)) {
			throw new IllegalArgumentException("Invalid cart uid specified");
		}
		Cart cart = fetchCartByUid(cartUid);
		cart.setPackagingType(packagingType);
		cart.setUpdatedBy(String.valueOf(user.getId()));
		saveCart(cart);
		return getCartSummary(cart, cart.getCustomerId(), Cart.PROCUREMENT_TYPE.NORMAL.equalsIgnoreCase(cart.getProcurementType()));
	}
	
	@Override
	public Cart updateRepeatInDays(User user, String cartUid, int repeatDay, Timestamp repeatDate) {
		if (StringUtils.isBlank(cartUid)) {
			throw new IllegalArgumentException("Invalid cart uid specified");
		}
		if( repeatDay < 0 && repeatDate == null ) {
			throw new IllegalArgumentException("Invalid repeat day or repeat date provided");
		}
		if( repeatDate != null && repeatDay == 0 ) {
			Date currDate = new Date();
			repeatDay = (int) ((repeatDate.getTime() - currDate.getTime()) / (1000  * 60 * 60 * 24));
			repeatDay = repeatDay > 0 ? repeatDay:0;
		}
		Cart cart = fetchCartByUid(cartUid);
		validateActiveCart(cart);
		cart.setRepeatDay(repeatDay);
		cart.setRepeatDate(repeatDate);
		cart.setUpdatedBy(String.valueOf(user.getId()));
		saveCart(cart);
		return getCartSummary(cart, cart.getCustomerId(), Cart.PROCUREMENT_TYPE.NORMAL.equalsIgnoreCase(cart.getProcurementType()));
	}

	// Check for Delivery type urgent possible or not
	@Override
	public OrderDeliveryObject getCartDeliveryObject(Cart cart) {
		OrderDeliveryObject orderDeliveryObject = new OrderDeliveryObject();
		orderDeliveryObject.setDeliveryOption(Cart.DELIVERY_OPTION.NORMAL);
		orderDeliveryObject.setServiceType(Cart.SERVICE_TYPE.NORMAL);
		if(StringUtils.isNotBlank(cart.getBusinessChannel())
				&& Order.BUSINESS_CHANNEL.OFFLINE.equalsIgnoreCase(cart.getBusinessChannel())
				&& StringUtils.isNotBlank(cart.getBusinessType())
				&& (Order.BUSINESS_TYPE.B2C.equalsIgnoreCase(cart.getBusinessType())
						|| Order.BUSINESS_TYPE.B2B.equalsIgnoreCase(cart.getBusinessType()))) {
			return orderDeliveryObject;
		}
		try {
			HashMap<String, Object> deliveryTypeHashMap = null;
			HashMap<String, Integer> skusQtyList = new HashMap<String, Integer>();
			if (cart != null && !cart.getCartItems().isEmpty()) {
				for (CartItem CartItem : cart.getCartItems()) {
					skusQtyList.put(CartItem.getSku(), (int) CartItem.getQuantity());
				}
			}
			if (cart.getShippingAddress() != null) {
				deliveryTypeHashMap = shippingService.getDeliveryType(
						String.valueOf(cart.getShippingAddress().getPincode()), cart.getFacilityCode(), skusQtyList);
			}
			if (deliveryTypeHashMap != null) {
				if (deliveryTypeHashMap.containsKey(Cart.ORDER_DELIVERY_TYPE.IS_LC_ASSURED_DELIVERY) && Boolean.TRUE
						.equals(deliveryTypeHashMap.get(Cart.ORDER_DELIVERY_TYPE.IS_LC_ASSURED_DELIVERY))) {
					orderDeliveryObject.setServiceType(Cart.SERVICE_TYPE.LF_ASSURED);
				}
				if (deliveryTypeHashMap.containsKey(Cart.ORDER_DELIVERY_TYPE.IS_URGENT_DELIVERY)
						&& Boolean.TRUE.equals(deliveryTypeHashMap.get(Cart.ORDER_DELIVERY_TYPE.IS_URGENT_DELIVERY))) {
					orderDeliveryObject.setDeliveryOption(Cart.DELIVERY_OPTION.URGENT);
					// check for time>  12 :00  i.e  18: 30 and  < 4: 00 pm i.e 10:30:00
					Boolean resultForUrgentdelivery = isUrgentOrdersAllowed(expressDeliveryStartTime, expressDeliveryEndTime);
					LOGGER.info("FINAL RESULT FOR IS ELIGIBILITY IS "+ resultForUrgentdelivery);
					if (Boolean.FALSE.equals(resultForUrgentdelivery)) {
						LOGGER.info("ORDER NOT ELIGIBLE FOR EXPRESS TIME LIMIT EXCEEDED  "+ resultForUrgentdelivery);
						orderDeliveryObject.setServiceType(orderDeliveryObject.getServiceType());
						orderDeliveryObject.setDeliveryOption(Cart.DELIVERY_OPTION.NORMAL);
						orderDeliveryObject.setDeliveryOptionChangeReason(
								OrderDeliveryObject.DELIVERY_OPTION_CHANGE_REASON.TIME_LIMIT_EXCEEDED);
					}
					// check for order count < 50
					try{
						String urgentOrderCount = redisTemplate.opsForValue().get("expressOrderCount");
						LOGGER.info("URGENT ORDER STRING COUNT IS  "+ urgentOrderCount);
						if (urgentOrderCount != null) {
							int count = Integer.parseInt(redisTemplate.opsForValue().get("expressOrderCount"));
							LOGGER.info("URGENT ORDER INTEGER COUNT IS  "+ count);
							if (count >= expressOrderCount) {
								LOGGER.info("URGENT ORDER INTEGER COUNT IS GREATER THAN 100 SO NORMAL  "+ count);
								orderDeliveryObject.setDeliveryOption(Cart.DELIVERY_OPTION.NORMAL);
								orderDeliveryObject.setDeliveryOptionChangeReason(
										OrderDeliveryObject.DELIVERY_OPTION_CHANGE_REASON.ORDER_COUNT_EXCEEDED);
							}
						}
					} catch(Exception e){
						LOGGER.error("UNABLE TO FIND REDIS ORDER COUNT");
					}
				}
			}
		} catch (Exception e) {
			LOGGER.error("Error in fetching delivery type for customer : cart uid : {} : Exception : {}", cart.getId(),
					e.getMessage());
		}
		return orderDeliveryObject;
	}
	

	@Override
	public Cart updatePreferredServiceType(String cartUid, String serviceType, Long loginCustomerId, Map<String, Object> map) {
		if (StringUtils.isBlank(serviceType)) {
			throw new IllegalArgumentException("No service type specified");
		}
		Cart cart = fetchCartByUid(cartUid);
		validateActiveCart(cart);
		Long customerId = cart.getCustomerId();
		if (customerId == null || customerId <= 0) {
			throw new IllegalArgumentException("No customer id found for cart uid :: " + cartUid
					+ ", customer id is mandatory for selecting delivery preference");
		}
		
		if (map != null && ! map.isEmpty()){
			String reason = map.containsKey("service_type_change_reason") ? (String) map.get("service_type_change_reason") : null;
			String comment = map.containsKey("service_type_change_comment") ? (String) map.get("service_type_change_comment") : null;
			if (reason != null) {
				reason += comment != null ? " (" + comment + ")" : "";
			} else {
				reason = comment != null ? comment : "";
			}
			if (StringUtils.isNotBlank(reason)) {
				 cart.setManualServiceTypeChangeReason(reason);
			}
			
		}
		
		cart.setPreferredServiceType(serviceType);
		cart.setServiceType(serviceType);
		saveCart(cart);
		return getCartSummary(cart, loginCustomerId, Cart.PROCUREMENT_TYPE.NORMAL.equalsIgnoreCase(cart.getProcurementType()));
	}

	// check for time>  00:00  i.e  18: 30 and  < 4: 00 pm i.e 10:30:00
	private boolean isUrgentOrdersAllowed(String minS, String maxS) {
		SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
		String currS = formatter.format(new Date());
		LocalTime minT = LocalTime.parse(minS);
		LocalTime maxT = LocalTime.parse(maxS);
		LocalTime currT = LocalTime.parse(currS);
		LOGGER.info("OK CHECKING MINS__"+ minS+ "__MAXS__"+maxS +"__MINT __"+ minT + "__CURRT__"+currT+ "__MAXT__"+ maxT);
		LOGGER.debug("OK CHECKING MINS__"+ minS+ "__MAXS__"+maxS +"__MINT __"+ minT + "__CURRT__"+currT+ "__MAXT__"+ maxT);
		System.out.println("OK CHECKING MINS__"+ minS+ "__MAXS__"+maxS +"__MINT __"+ minT + "__CURRT__"+currT+ "__MAXT__"+ maxT);
		boolean result = minT.isBefore(maxT) ? (currT.isAfter(minT) && currT.isBefore(maxT))
				: !(currT.isAfter(maxT) && currT.isBefore(minT));
		System.out.println("RESULT FOR IS ELIGIBILITY IS "+ result);
		LOGGER.info("RESULT FOR IS ELIGIBILITY IS "+ result);
		LOGGER.debug("RESULT FOR IS ELIGIBILITY IS "+ result);
		return result;
	}
	
	@Override
	public Cart updatePromisedDeliveryDetails(User user, String cartUid, Timestamp promisedDeliveryDate, String promisedDeliveryTime) {
		if (StringUtils.isBlank(cartUid)) {
			throw new IllegalArgumentException("Invalid cart uid specified");
		}
		if( promisedDeliveryDate == null && StringUtils.isBlank(promisedDeliveryTime) ) {
			throw new IllegalArgumentException("Invalid promised delivery date or time provided");
		}
		Cart cart = fetchCartByUid(cartUid);
		validateActiveCart(cart);
		if( promisedDeliveryDate != null ) {
			promisedDeliveryDate = new Timestamp(promisedDeliveryDate.getTime() + (long)5.5*60*60*1000);
			cart.setPromisedDeliveryDate(promisedDeliveryDate);
		}
		if( StringUtils.isNotBlank(promisedDeliveryTime) ) {
			cart.setPromisedDeliveryTime(promisedDeliveryTime);
		}
		cart.setUpdatedBy(String.valueOf(user.getId()));
		saveCart(cart);
		return getCartSummary(cart, cart.getCustomerId(), Cart.PROCUREMENT_TYPE.NORMAL.equalsIgnoreCase(cart.getProcurementType()));
	}
	

	@Override
	public Cart addCartItems(String cartUid, List<CartItem> cartItems) {
		Cart cart = replaceCartItems(cartUid, cartItems, Boolean.TRUE);
		return getCartSummary(cart, cart.getCustomerId(), Cart.PROCUREMENT_TYPE.NORMAL.equalsIgnoreCase(cart.getProcurementType()));
	}
	
	@Override
	public Cart updateRevisitInDays(User user, String cartUid, int revisitDay, Timestamp revisitDate) {
		if (StringUtils.isBlank(cartUid)) {
			throw new IllegalArgumentException("Invalid cart uid specified");
		}
		if (revisitDay < 0 && revisitDate == null) {
			throw new IllegalArgumentException("Invalid repeat day or repeat date provided");
		}
		if (revisitDate != null && revisitDay == 0) {
			Date currDate = new Date();
			revisitDay = (int) ((revisitDate.getTime() - currDate.getTime()) / (1000 * 60 * 60 * 24));
			revisitDay = revisitDay > 0 ? revisitDay : 0;
		}
		Cart cart = fetchCartByUid(cartUid);
		validateActiveCart(cart);
		cart.setRevisitDay(revisitDay);
		cart.setRevisitDate(revisitDate);
		cart.setUpdatedBy(String.valueOf(user.getId()));
		saveCart(cart);
		return getCartSummary(cart, cart.getCustomerId(), Cart.PROCUREMENT_TYPE.NORMAL.equalsIgnoreCase(cart.getProcurementType()));
	}
	
	@Override
	public Cart updateNextRefillDays(String cartUid, Timestamp nextRefillDate, int nextRefillDays, User user) {
		if (StringUtils.isBlank(cartUid)) {
			throw new IllegalArgumentException("Invalid cart uid specified");
		}
		if (nextRefillDays < 0 && nextRefillDate == null) {
			throw new IllegalArgumentException("Invalid next refill day or next refill date provided");
		}
		if (nextRefillDate != null && nextRefillDays == 0) {
			Date currDate = new Date();
			nextRefillDays = (int) Math.ceil(((nextRefillDate.getTime() - currDate.getTime()) / (1000 * 60 * 60 * 24)));
			nextRefillDays = nextRefillDays > 0 ? nextRefillDays : 0;
		}
		Cart cart = fetchCartByUid(cartUid);
		validateActiveCart(cart);
		cart.setNextRefillDay(nextRefillDays);
		cart.setNextRefillDate(nextRefillDate);
		cart.setUpdatedBy(String.valueOf(user.getId()));
		saveCart(cart);
		return getCartSummary(cart, cart.getCustomerId(), Cart.PROCUREMENT_TYPE.NORMAL.equalsIgnoreCase(cart.getProcurementType()));
	}
	

	@Override
	public Cart switchCartPincode(String cartUid, String pincode) {
		if (StringUtils.isBlank(cartUid) || StringUtils.isBlank(pincode)) {
			throw new IllegalArgumentException("Invalid param provided!");
		}
		
		Cart cart = getCartByUid(cartUid);
		if (cart == null) {
			throw new CartNotFoundException("No cart found with cart uid :: " + cartUid);
		}
		int facilityId = getFacilityIdByPincodeAndCartCategory(pincode, cart.getCategory());
		validateActiveCart(cart);
		cart.setFacilityCode(facilityId);
		cart.setPincode(pincode);
		saveCart(cart);
		return getCartSummary(cart.getUid(), cart.getCustomerId(), true);
	}
	
	private int getFacilityIdByPincodeAndCartCategory(String pincode, String category) {
		Integer facilityId = DEFAULT_FACILITY_ID;
		List<FacilityPincodeMapping> facilityPincodeMapping = this.facilityPincodeMappingService.getPlacePincodeByPincode(pincode);
		List<Long> facilityIds = facilityPincodeMapping.stream().filter(Objects::nonNull).map(FacilityPincodeMapping::getFacilityId).collect(Collectors.toList());
		List<Map<String, Object>> facilities = facilityService.getFacilityByCategoryAndIds(Arrays.asList(category), facilityIds);
		
		if (facilities != null && !facilities.isEmpty()) {
			facilityIds = facilities.parallelStream().map(map -> {
				Number id =  map.get("id") != null ? (Number) map.get("id") : 0L;
				return id.longValue();
			}).collect(Collectors.toList());
		facilityId = facilityIds.get(0).intValue();
		}
		return facilityId;
	}
	

	@Override
	public Cart updateAppointmentDetails(String cartUid, Timestamp appointmentDate, long appointmentSlotId, String appointmentSlot, User user) {
		if (StringUtils.isBlank(cartUid) || appointmentDate == null || appointmentSlotId <= 0) {
			throw new IllegalArgumentException("Invalid param provided");
		}
		Cart cart = fetchCartByUid(cartUid);
		validateActiveCart(cart);
		cart.setAppointmentDate(appointmentDate);
		cart.setAppointmentSlotId(appointmentSlotId);
		cart.setAppointmentSlot(appointmentSlot);
		cart.setUpdatedBy(String.valueOf(user.getId()));
		saveCart(cart);
		return getCartSummary(cart, cart.getCustomerId(), Cart.PROCUREMENT_TYPE.NORMAL.equalsIgnoreCase(cart.getProcurementType()));
	}
	

	@Override
	public Cart updateReportDeliveryOption(String cartUid, Long loginCustomerId, Map<String, Object> map) {
		if (map == null || map.isEmpty()) {
			throw new IllegalArgumentException("Invalid param provided");
		}
		Cart cart = fetchCartByUid(cartUid);
		if (cart == null) {
			throw new CartNotFoundException("No cart found with cart uid :: " + cartUid);
		}
		validateActiveCart(cart);
		Long customerId = cart.getCustomerId();
		if (customerId == null || customerId <= 0) {
			throw new IllegalArgumentException("No customer id found for cart uid :: " + cartUid + ", customer id is mandatory for selecting delivery preference");
		}
		Boolean isReportHardCopyRequired = map.containsKey("is_report_hard_copy_required") ? (Boolean) map.get("is_report_hard_copy_required") : null;
		if (isReportHardCopyRequired == null) {
			throw new IllegalArgumentException("Invalid key provided for ReportDeliveryOption");
		}
		cart.setReportDeliveryCharge(0.0);
		if (isReportHardCopyRequired) {
			try {
				PlacePincode placePincode = shippingService.getPlaceInformationByPincode(cart.getPincode());
				if (placePincode == null) {
					throw new NotFoundException("No place info found for pincode " + cart.getPincode());
				}
				if (placePincode.getIsReportHardCopyAvailable() == null || !placePincode.getIsReportHardCopyAvailable()) {
					throw new IllegalArgumentException("We do not serve hard copy of the report at this pincode : " + cart.getPincode());
				}
				cart.setReportDeliveryCharge(placePincode.getReportDeliveryCharge());
			} catch (Exception e) {
				throw e;
			}
		}
		cart.setReportHardCopyRequired(isReportHardCopyRequired);
		if (map.get("email") != null || map.get("mobile") != null) {
			if (map.get("email") != null && StringUtils.isNotBlank(String.valueOf(map.get("email")))) {
				cart.setEmail(String.valueOf(map.get("email")));
			}
			if (map.get("mobile") != null && StringUtils.isNotBlank(String.valueOf(map.get("mobile")))) {
				cart.setMobile(String.valueOf(map.get("mobile")));
			}
		}
		cart = saveCart(cart);
		return getCartSummary(cart, loginCustomerId, Cart.PROCUREMENT_TYPE.NORMAL.equalsIgnoreCase(cart.getProcurementType()));
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(CartServiceImpl.class);

	
	@Autowired
	private RedisTemplate<String, String> redisTemplate;
	
	@Autowired
	private OrderService orderService;
	
	@Autowired
	private RefillLeadService refillLeadService;
	
	@Autowired
	private CouponService couponService;
	
	@Autowired
	private PaymentService paymentService;
	
	@Autowired
	private ShippingService shippingService;
	
	@Autowired
	private CustomerService customerService;
	
	@Autowired
	private CartRepository cartRepository;
	
	@Autowired
	private CartItemService cartItemService;
	
	@Autowired
	private CartPrescriptionService cartPrescriptionService;
	
	@Autowired
	private FacilityService facilityService;
	
	@Autowired
	private PrescriptionService prescriptionService;
	
	@Autowired
	private FacilityPincodeMappingService facilityPincodeMappingService;
	
	@Autowired
	private OrderShippingAddressService orderShippingAddressService;

}
