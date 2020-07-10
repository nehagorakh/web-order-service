package in.lifcare.order.api;

import java.net.URLDecoder;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import in.lifcare.auth.gateway.response.annotation.Mask;
import in.lifcare.core.constant.APIEndPoint;
import in.lifcare.core.constant.OrderEvent;
import in.lifcare.core.constant.OrderStatus;
import in.lifcare.core.exception.BadRequestException;
import in.lifcare.core.fareye.model.FarEyeProcessData;
import in.lifcare.core.model.Agent;
import in.lifcare.core.model.DispatchEvent;
import in.lifcare.core.model.OrderStateStatusRequest;
import in.lifcare.core.model.UpdateOrderObject;
import in.lifcare.core.model.User;
import in.lifcare.core.response.model.Error;
import in.lifcare.core.response.model.Response;
import in.lifcare.core.util.CommonUtil;
import in.lifcare.order.cart.model.Cart;
import in.lifcare.order.cart.service.CartService;
import in.lifcare.order.event.OrderEventService;
import in.lifcare.order.exception.CartException;
import in.lifcare.order.exception.PrescriptionExpiredException;
import in.lifcare.order.microservice.payment.model.OrderPaymentGatewayVerifyRequest;
import in.lifcare.order.microservice.payment.model.PaymentChannelData;
import in.lifcare.order.model.Order;
import in.lifcare.order.model.OrderAdditionalItem;
import in.lifcare.order.model.OrderCreationCartRequest;
import in.lifcare.order.model.OrderDeliveryObject;
import in.lifcare.order.model.OrderItem;
import in.lifcare.order.model.OrderPatientPrescription;
import in.lifcare.order.model.OrderPaymentGatewayObject;
import in.lifcare.order.model.OrderPaymentRequest;
import in.lifcare.order.model.OrderSearchResponse;
import in.lifcare.order.model.OrderSummary;
import in.lifcare.order.model.Reason;
import in.lifcare.order.model.ShippingAddress;
import in.lifcare.order.model.StatusUpdateRequest;
import in.lifcare.order.service.OrderAdditionalItemService;
import in.lifcare.order.service.OrderItemService;
import in.lifcare.order.service.OrderService;
import in.lifcare.order.service.OrderShippingAddressService;

@RestController
@RequestMapping(value = "/order")
public class OrderController {

	@Mask(maskParameters={"shipping_address.mobile","patient.mobile"})
	@PostAuthorize("hasRole('ROLE_AGENT') or hasPermission(returnObject.payload.customerId,'microservice_client')")
	@RequestMapping(value = "/{order-id}", method = RequestMethod.GET)
	public @ResponseBody Response<Order> get(@PathVariable("order-id") long orderId) throws Exception {
		try {
			if (orderId > 0) {

				// New order
				return new Response<Order>(orderService.get(orderId));
			}
			throw new BadRequestException("Invalid parameters provided");

		} catch (Exception e) {
			throw e;
		}

	}

	@PostAuthorize("hasRole('ROLE_AGENT') or hasPermission(returnObject.payload.customerId,'microservice_client')")
	@RequestMapping(method = RequestMethod.GET)
	public @ResponseBody Response<Order> getOrder(@RequestParam("order_id") long orderId) throws Exception {
		try {
			if (orderId > 0) {

				// New order
				return new Response<Order>(orderService.getOrder(orderId));
			}
			throw new BadRequestException("Invalid parameters provided");

		} catch (Exception e) {
			throw e;
		}

	}
	
	@PostAuthorize("hasRole('ROLE_AGENT') or hasPermission(returnObject.payload.customerId,'microservice_client')")
	@RequestMapping(value = "/merge-search", method = RequestMethod.GET)
	public @ResponseBody Response<Page<OrderSearchResponse>> searchOrder(@RequestParam(name = "parent-id", required = false) Long parentId,
			@RequestParam(name = "child-order-id", required = false) Long childOrderId, @RequestParam(name = "rfc", required = false , defaultValue = "100") Long childFacilityId, 
			@RequestParam(name = "child-order-statuses", required = false) List<String> childOrderStatuses,
			@RequestParam(name = "delivery-date-from", required = false) Long deliveryDateFrom, @RequestParam(name = "delivery-date-to", required = false) Long deliveryDateTo,
			@RequestParam(name = "dispatch-date-from", required = false) Long dispatchDateFrom, @RequestParam(name = "dispatch-date-to", required = false) Long dispatchDateTo,
			@PageableDefault(page = 0, size = 30) Pageable pageable) throws Exception {
		try {
			List<String> status = new ArrayList<String>();
			status.add("READY_TO_MERGED");
			return new Response<Page<OrderSearchResponse>>(
					orderService.searchOrder(parentId, childOrderId, childFacilityId, status, deliveryDateFrom, deliveryDateTo, dispatchDateFrom, dispatchDateTo, pageable));
		} catch (Exception e) {
			throw e;
		}

	}
	
	@PostAuthorize("hasRole('ROLE_AGENT') or hasPermission(returnObject.payload.customerId,'microservice_client')")
	@RequestMapping(value = "/{order-id}/child/{child-order-id}", method = RequestMethod.GET)
	public @ResponseBody Response<Order> getChildOrder(@PathVariable("order-id") long parentId, @PathVariable("child-order-id") long childOrderId) throws Exception {
		try {
			return new Response<Order>(orderService.getChildOrder(parentId, childOrderId));
		} catch (Exception e) {
			throw e;
		}

	}

	@PreAuthorize("hasRole('ROLE_AGENT') or hasPermission('hasAccess','microservice_client')")
	@RequestMapping(value = "/{order-id}/items", method = RequestMethod.GET)
	public @ResponseBody Response<List<OrderItem>> getItems(@PathVariable("order-id") long orderId) throws Exception {
		try {
			if (orderId > 0) {

				// New order
				return new Response<List<OrderItem>>(orderService.getItems(orderId));
			}
			throw new BadRequestException("Invalid parameters provided");

		} catch (Exception e) {
			throw e;
		}

	}
	
	@PreAuthorize("hasRole('ROLE_AGENT') or hasPermission('hasAccess','microservice_client')")
	@RequestMapping(value = "/{order-id}/items/auto/update-prescription", method = RequestMethod.PATCH)
	public @ResponseBody Response<Boolean> autoUpdateItemsPrescription(@PathVariable("order-id") long orderId) throws Exception {
		try {
			if (orderId > 0) {

				return new Response<Boolean>(orderService.updateItemsPrescription(orderId));
			}
			throw new BadRequestException("Invalid parameters provided");

		} catch (Exception e) {
			throw e;
		}

	}
	
	@PreAuthorize("hasRole('ROLE_AGENT') or hasPermission('','microservice_client')")
	@RequestMapping(value = "/customer-ids", method = RequestMethod.GET)
	public @ResponseBody Response<List<Long>> getCustomerIds(@RequestParam("created-at-gte") long createdAtGte) throws Exception {
		try {
			return new Response<List<Long>>(orderService.getCustomerIdsByCreatedAtAfter(new Timestamp(createdAtGte)));

		} catch (Exception e) {
			throw e;
		}

	}

	@PreAuthorize("hasRole('ROLE_AGENT') or hasPermission(#customerId,'microservice_client')")
	@RequestMapping(value = "/customer/{customer-id}/recent-orders", method = RequestMethod.GET)
	public @ResponseBody Response<Page<Order>> getRecentOrders(@PathVariable("customer-id") long customerId,
			@PageableDefault(page = 0, size = 30, sort = { "createdAt" }, direction = Direction.DESC) Pageable pageable)
			throws Exception {
		try {
			if (customerId > 0) {
				// New order
				return new Response<Page<Order>>(orderService.recentOrders(customerId, pageable));
			}
			throw new BadRequestException("Invalid parameters provided");

		} catch (Exception e) {
			throw e;
		}

	}
	

	@PreAuthorize("hasRole('ROLE_AGENT') or hasPermission('','microservice_client')")
	@RequestMapping(value = "/created-between", method = RequestMethod.GET)
	public @ResponseBody Response<Page<Order>> getOrdersCreatedAtBetween(@RequestParam(value = "created-at-begins") long createdAtBegins, @RequestParam(value = "created-at-ends") long createdAtEnds,
			@PageableDefault(page = 0, size = 30, sort = { "createdAt" }, direction = Direction.DESC) Pageable pageable) throws Exception {
		try {
			if (createdAtBegins <= 0 || createdAtEnds <= 0) {
				throw new BadRequestException("Invalid parameters provided");
			}
			return new Response<Page<Order>>(orderService.getOrdersCreatedAtBetween(new Timestamp(createdAtBegins), new Timestamp(createdAtEnds), pageable));
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e.getMessage());
		} catch (Exception e) {
			throw e;
		}
	}

	@PreAuthorize("hasRole('ROLE_AGENT') or hasPermission(#customerId,'microservice_client')")
	@RequestMapping(value = "/customer/{customer-id}/order-summary", method = RequestMethod.GET)
	public @ResponseBody Response<Page<OrderSummary>> getOrdersSummary(@PathVariable("customer-id") long customerId,
			@RequestParam(value="category", defaultValue="MEDICINE") String category,
			@PageableDefault(page = 0, size = 30, sort = { "createdAt" }, direction = Direction.DESC) Pageable pageable) throws Exception {
		try {
			if (customerId <= 0) {
				throw new BadRequestException("Invalid parameters provided");
			}
			return new Response<Page<OrderSummary>>(orderService.getOrdersSummary(customerId, category, pageable));
		} catch (IllegalArgumentException e) {
			throw e;
		} catch (Exception e) {
			throw e;
		}
	}
	
	@PreAuthorize("hasRole('ROLE_AGENT') or hasPermission(#customerId,'microservice_client')")
	@RequestMapping(value = "/customer/{customer-id}/active-orders", method = RequestMethod.GET)
	public @ResponseBody Response<Page<Order>> getActiveOrders(@PathVariable("customer-id") long customerId,
			@PageableDefault(page = 0, size = 30, sort = { "createdAt" }, direction = Direction.DESC) Pageable pageable)
			throws Exception {
		try {
			if (customerId > 0) {
				// New order
				return new Response<Page<Order>>(orderService.activeOrders(customerId, pageable));
			}
			throw new BadRequestException("Invalid parameters provided");

		} catch (Exception e) {
			throw e;
		}

	}
	//New Auth add
	@PreAuthorize("hasRole('ROLE_AGENT') or hasPermission(#customerId,'microservice_client')")
	@RequestMapping(value = "/customer/{customer-id}/orders", method = RequestMethod.GET)
	public @ResponseBody Response<Page<Order>> getOrders(@PathVariable("customer-id") long customerId,
			@RequestParam(value="category", required = false) String category,
			@PageableDefault(page = 0, size = 30, sort = { "createdAt" }, direction = Direction.DESC) Pageable pageable)
			throws Exception {
		try {
			if (customerId > 0) {
				// New order
				return new Response<Page<Order>>(orderService.getOrders(customerId, category, pageable));
			}
			throw new BadRequestException("Invalid parameters provided");

		} catch (Exception e) {
			throw e;
		}

	}

	@PreAuthorize("hasRole('ROLE_AGENT') or hasPermission('hasAccess','microservice_client')")
	@RequestMapping(value = "/patient/{patient-id}/orders", method = RequestMethod.GET)
	public @ResponseBody Response<Page<Order>> getOrdersByPatient(@PathVariable("patient-id") long patientId,
			@RequestParam(value="category", required = false, defaultValue = "MEDICINE") String category,
			@PageableDefault(page = 0, size = 30, sort = { "createdAt" }, direction = Direction.DESC) Pageable pageable)
			throws Exception {
		try {
			if (patientId > 0) {
				// New order
				return new Response<Page<Order>>(orderService.getOrdersByPatient(patientId, category, pageable));
			}
			throw new BadRequestException("Invalid parameters provided");

		} catch (Exception e) {
			throw e;
		}

	}

	@PreAuthorize("hasRole('ROLE_AGENT') or hasPermission('hasAccess','microservice_client')")
	@RequestMapping(value = "/{order-id}/repeat-in-days", method = RequestMethod.PUT)
	public @ResponseBody Response<Order> repeat(@PathVariable("order-id") long orderId,
			@RequestBody HashMap<String, Object> repeatInfoMap) throws Exception {
		try {
			Timestamp nextRepeatDate = null;
			int nextRepeatDays = 0;
			if (orderId > 0) {
				if (repeatInfoMap != null && !repeatInfoMap.isEmpty()) {
					if (repeatInfoMap.containsKey("repeat_day")) {
						nextRepeatDays = repeatInfoMap.containsKey("repeat_day")
								&& repeatInfoMap.get("repeat_day") != null
								? Integer.parseInt(String.valueOf(repeatInfoMap.get("repeat_day")))
								: 0;
					} else {
						nextRepeatDays = repeatInfoMap.containsKey("next_refill_day")
								&& repeatInfoMap.get("next_refill_day") != null
								? Integer.parseInt(String.valueOf(repeatInfoMap.get("next_refill_day")))
								: 0;
					}
					nextRepeatDate = repeatInfoMap.containsKey("next_refill_date")
							&& repeatInfoMap.get("next_refill_date") != null
							? new Timestamp(Long.parseLong(String.valueOf(repeatInfoMap.get("next_refill_date"))))
							: null;
					// We only get repeatDay and not entire object.
					Order order = orderService.updateOrderNextRefillDay(orderId, nextRepeatDate, nextRepeatDays);
					orderEventService.repeatOrderEvent(order.getCustomerId(), order.getId());
					return new Response<Order>(order);
				}

			}
			throw new BadRequestException("Invalid parameters provided");

		} catch (Exception e) {
			throw e;
		}

	}

	@RequestMapping(method = RequestMethod.POST)
	@PostAuthorize("hasRole('ROLE_AGENT') or hasPermission(returnObject.payload.customerId,'microservice_client')")
	public @ResponseBody Response<Order> add(OAuth2Authentication auth, @RequestBody Order order) throws Exception {
		try {
			if (order != null) {

				// New order
				User user = new User();
				try {
					user = CommonUtil.getOauthUser(auth);
				} catch (Exception e) {
					user.setFirstName(order.getCreatedBy());
				}
				order = orderService.add(order, user);
				try {
					if (Order.BUSINESS_CHANNEL.OFFLINE.equalsIgnoreCase(order.getBusinessChannel())
							&& (Order.BUSINESS_TYPE.B2C.equalsIgnoreCase(order.getBusinessType())
									|| Order.BUSINESS_TYPE.B2B.equalsIgnoreCase(order.getBusinessType()))) {
						orderEventService.createOrderEvent(order, order.getId() + order.getUpdatedAt().toString(),
								OrderEvent.ORDER_NEW_VERIFIED, user);
					} else {
						orderEventService.createOrderEvent(order, order.getId() + order.getUpdatedAt().toString(),
								OrderEvent.ORDER_NEW_NEW, user);
					}
				} catch(Exception e){
					LOGGER.debug("EVENT Cannot Be Created {} {}",order.getId(), e);
				}
				return new Response<Order>(order);
			}
			throw new BadRequestException("Invalid parameters provided");

		} catch (Exception e) {
			throw e;
		}

	}
	
	@RequestMapping(value = "/create/source/{source}", method = RequestMethod.POST)
	@PostAuthorize("hasRole('ROLE_AGENT') or hasPermission(returnObject.payload.customerId,'microservice_client')")
	public @ResponseBody Response<Order> addOrderPos(OAuth2Authentication auth, @RequestBody Order order, @PathVariable("source") String source) throws Exception {
		try {
			if (order != null && StringUtils.isNotBlank(source) && Order.SOURCE.POS.equalsIgnoreCase(source)) {

				// New order
				User user = new User();
				try {
					if (order.getUser() != null) {
						user = order.getUser();
					} else {
						user = CommonUtil.getOauthUser(auth);
					}
				} catch (Exception e) {
					user.setFirstName(order.getCreatedBy());
				}
					order = orderService.addPosOrder(order, user);
				try {
					LOGGER.info("Order info object preview {}" ,order);
					orderEventService.createOrderEvent(order, order.getId() + order.getUpdatedAt().toString(),
							"ORDER_" + StringUtils.upperCase(order.getState()) + "_" + StringUtils.upperCase(order.getStatus()), user);
					orderEventService.createOrderEvent(order, order.getId() + order.getUpdatedAt().toString(), OrderEvent.NEW_POS_ORDER_CREATED, user);
				} catch(Exception e){
					LOGGER.debug("EVENT Cannot Be Created {} {}",order.getId(), e.getMessage());
				}
				return new Response<Order>(order);
			}
			throw new BadRequestException("Invalid parameters provided");

		} catch (Exception e) {
			throw e;
		}

	}

	/**
	 * 
	 * @param auth
	 * @param orderId
	 * @param orderPaymentRequest
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/payment/initiate", method = RequestMethod.POST)
	@PreAuthorize("hasRole('ROLE_USER')")
	public @ResponseBody Response<OrderPaymentGatewayObject> initiateOrderPayment(OAuth2Authentication auth, @RequestBody OrderPaymentRequest orderPaymentRequest) throws Exception {
		try {
			if( orderPaymentRequest == null ) {
				throw new BadRequestException("Invalid order payment request");
			}
			long orderId = orderPaymentRequest.getOrderId();
			if( orderId <= 0 ) {
				throw new BadRequestException("Invalid order id specified in request");
			}
			if( StringUtils.isBlank(orderPaymentRequest.getPaymentMethod()) ) {
				throw new BadRequestException("Invalid payment method specified in request");
			}
			User user = new User();
			try {
				user = CommonUtil.getOauthUser(auth);
			} catch (Exception e) {
				LOGGER.error("Error in fetching user from auth service : Exception : {}", e.getMessage() );
			}
			Order order = orderService.getOrder(orderId);
			if( !( OrderStatus.STATE_NEW.PAYMENT_PENDING.equalsIgnoreCase(order.getStatus()) || OrderStatus.STATE_NEW.PAYMENT_FAILED.equalsIgnoreCase(order.getStatus()) ) ) {
				throw new IllegalArgumentException("Invalid order status for payment initiation : status : " + order.getStatus());
			}
			OrderPaymentGatewayObject orderPaymentGatewayObject = orderService.initiatePayment(order, orderPaymentRequest.getPaymentMethod(), orderPaymentRequest.getPaymentSubMethod(), true);
			try {
				String orderEvent = null;
				if (Order.ORDER_TYPE.COD.equalsIgnoreCase(order.getOrderType())) {
					orderEventService.paymentFailedPushForCodOrderEvent(order);
					orderEvent = OrderEvent.ORDER_NEW_NEW;
				}
				if (Order.BUSINESS_CHANNEL.OFFLINE.equalsIgnoreCase(order.getBusinessChannel())
						&& (Order.BUSINESS_TYPE.B2C.equalsIgnoreCase(order.getBusinessType()) || Order.BUSINESS_TYPE.B2B.equalsIgnoreCase(order.getBusinessType()))) {
					orderEvent = OrderEvent.ORDER_NEW_VERIFIED;
				}
				if( StringUtils.isNotBlank(orderEvent) ) {
					orderEventService.createOrderEvent(order, order.getId() + order.getUpdatedAt().toString(), orderEvent, user);
				}
			} catch (Exception e) {
				LOGGER.debug("EVENT Cannot Be Created {} {}", order.getId(), e);
			}
			orderPaymentGatewayObject.setOrderPlaced(true);
			return new Response<OrderPaymentGatewayObject>(orderPaymentGatewayObject);
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e.getMessage());
		} catch (Exception e) {
			throw e;
		}
	}

	/**
	 * 
	 * @param auth
	 * @param cartParam
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/cart", method = RequestMethod.POST)
	@PreAuthorize("hasRole('ROLE_USER')")
	public @ResponseBody Response<Order> createCartOrder(OAuth2Authentication auth, @RequestBody OrderCreationCartRequest orderCreationCartRequest) throws Exception {
		try {
			String cartUid = orderCreationCartRequest.getCartUid();
			if( StringUtils.isBlank(cartUid) ) {
				throw new BadRequestException("Invalid cart_uid specified in request");
			}
			synchronized (cartUid) {
				if( StringUtils.isNotBlank(orderCreationCartRequest.getPaymentMethod()) && !Order.ORDER_TYPE.COD.equalsIgnoreCase(orderCreationCartRequest.getPaymentMethod()) ) {
					throw new BadRequestException("Invalid payment method : " + orderCreationCartRequest.getPaymentMethod() + " : COD payment only allowed");
				}
				if( StringUtils.isBlank(orderCreationCartRequest.getPaymentMethod()) ) {
					orderCreationCartRequest.setPaymentMethod(Order.ORDER_TYPE.COD);
				}
				User user = new User();
				try {
					user = CommonUtil.getOauthUser(auth);
				} catch (Exception e) {
					LOGGER.error("Error in fetching user from auth service : Exception : {}", e.getMessage() );
				}
				boolean orderExist = false;
				Cart cart = cartService.getCartByUid(cartUid);
				long cartOrderId = cart != null && cart.getOrderId() != null && cart.getOrderId() > 0 ?  cart.getOrderId() : 0;
				if( cartOrderId > 0 ) {
					orderExist = true;
				}
				Order order = null;
				String paymentMethod = orderCreationCartRequest.getPaymentMethod();
				String subPaymentMethod = orderCreationCartRequest.getPaymentSubMethod();
				if( orderExist ) {
					order = orderService.getOrder(cartOrderId);
					paymentMethod = order.getPaymentMethod();
					subPaymentMethod = order.getPaymentSubMethod();
				} else {
					order = orderService.createCartOrder(orderCreationCartRequest, user);
				}
				if (!Order.PROCUREMENT_TYPE.BULK.equalsIgnoreCase(order.getProcurementType())) {
					orderService.initiatePayment(order, paymentMethod, subPaymentMethod, false);
				}
				try {
					String orderEvent = OrderEvent.ORDER_NEW_PAYMENT_PENDING;
					if (Order.ORDER_TYPE.COD.equalsIgnoreCase(order.getOrderType())) {
						orderEvent = OrderEvent.ORDER_NEW_NEW;
					}
					if (Order.BUSINESS_CHANNEL.OFFLINE.equalsIgnoreCase(order.getBusinessChannel())
							&& (Order.BUSINESS_TYPE.B2C.equalsIgnoreCase(order.getBusinessType()) || Order.BUSINESS_TYPE.B2B.equalsIgnoreCase(order.getBusinessType()))) {
						orderEvent = OrderEvent.ORDER_NEW_VERIFIED;
					}
					orderEventService.createOrderEvent(order, order.getId() + order.getUpdatedAt().toString(), orderEvent, user);
				} catch (Exception e) {
					LOGGER.debug("EVENT Cannot Be Created {} {}", order.getId(), e);
				}
				return new Response<Order>(order);
			}
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e.getMessage());
		} catch (Exception e) {
			throw e;
		}

	}

	@RequestMapping(value = "/cart/create", method = RequestMethod.POST)
	@PreAuthorize("hasRole('ROLE_USER')")
	public @ResponseBody Response<OrderPaymentGatewayObject> createCartOrderNew(OAuth2Authentication auth, @RequestBody OrderCreationCartRequest orderCreationCartRequest) throws Exception {
		try {
			String cartUid = orderCreationCartRequest.getCartUid();
			if( StringUtils.isBlank(cartUid) ) {
				throw new BadRequestException("Invalid cart_uid specified in request");
			}
			synchronized (cartUid) {
				if( StringUtils.isBlank(orderCreationCartRequest.getPaymentMethod()) ) {
					orderCreationCartRequest.setPaymentMethod(Order.ORDER_TYPE.COD);
				}
				User user = new User();
				try {
					user = CommonUtil.getOauthUser(auth);
				} catch (Exception e) {
					LOGGER.error("Error in fetching user from auth service : Exception : {}", e.getMessage() );
				}
				boolean orderExist = false;
				Cart cart = cartService.getCartByUid(cartUid);
				long cartOrderId = cart != null && cart.getOrderId() != null && cart.getOrderId() > 0 ?  cart.getOrderId() : 0;
				if( cartOrderId > 0 ) {
					orderExist = true;
				}
				Order order = null;
				String paymentMethod = orderCreationCartRequest.getPaymentMethod();
				String subPaymentMethod = orderCreationCartRequest.getPaymentSubMethod();
				if( orderExist ) {
					order = orderService.getOrder(cartOrderId);
					paymentMethod = order.getPaymentMethod();
					subPaymentMethod = order.getPaymentSubMethod();
				} else {
					order = orderService.createCartOrder(orderCreationCartRequest, user);
				}
				OrderPaymentGatewayObject orderPaymentGatewayObject = orderService.initiatePayment(order, paymentMethod, subPaymentMethod, false);
				try {
					String orderEvent = OrderEvent.ORDER_NEW_PAYMENT_PENDING;
					if (Order.ORDER_TYPE.COD.equalsIgnoreCase(order.getOrderType())) {
						orderEvent = OrderEvent.ORDER_NEW_NEW;
					}
					if (Order.BUSINESS_CHANNEL.OFFLINE.equalsIgnoreCase(order.getBusinessChannel())
							&& (Order.BUSINESS_TYPE.B2C.equalsIgnoreCase(order.getBusinessType()) || Order.BUSINESS_TYPE.B2B.equalsIgnoreCase(order.getBusinessType()))) {
						orderEvent = OrderEvent.ORDER_NEW_VERIFIED;
					}
					orderEventService.createOrderEvent(order, order.getId() + order.getUpdatedAt().toString(), orderEvent, user);
				} catch (Exception e) {
					LOGGER.debug("EVENT Cannot Be Created {} {}", order.getId(), e);
				}
				orderPaymentGatewayObject.setOrderPlaced(orderExist);
				return new Response<OrderPaymentGatewayObject>(orderPaymentGatewayObject);
			}
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e.getMessage());
		} catch (Exception e) {
			throw e;
		}

	}
	
	/**
	 * 
	 * @param auth
	 * @param orderId
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/{order-id}/payment/channels", method = RequestMethod.GET)
	@PreAuthorize("hasRole('ROLE_USER')")
	public @ResponseBody Response<List<PaymentChannelData>> fetchOrderPaymentChannels(OAuth2Authentication auth, @PathVariable("order-id") long orderId) throws Exception {
		try {
			if( orderId <= 0 ) {
				throw new BadRequestException("Invalid input parameters.");
			}
			return new Response<List<PaymentChannelData>>(orderService.fetchOrderPaymentChannels(orderId));
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e.getMessage());
		} catch (Exception e) {
			throw e;
		}
	}

	@RequestMapping(value = "/{order-id}/payment/verify", method = RequestMethod.POST)
	@PreAuthorize("hasRole('ROLE_USER')")
	public @ResponseBody Response<OrderPaymentGatewayObject> verifyOrderPayment(OAuth2Authentication auth, @PathVariable("order-id") long orderId, @RequestBody OrderPaymentGatewayVerifyRequest orderPaymentGatewayVerifyRequest) throws Exception {
		try {
			User user = new User();
			try {
				user = CommonUtil.getOauthUser(auth);
			} catch (Exception e) {
				LOGGER.error("Error in fetching user from auth service : Exception : {}", e.getMessage() );
			}
			if( orderId <= 0 ) {
				throw new BadRequestException("Invalid order id specified");
			}
			if( orderPaymentGatewayVerifyRequest == null ) {
				throw new BadRequestException("Invalid orderPaymentGatewayVerifyRequest specified");
			}
	 		if (StringUtils.isBlank(orderPaymentGatewayVerifyRequest.getGatewayName())) {
				throw new BadRequestException("Invalid payment gateway specified in orderPaymentGatewayVerifyRequest");
			}
	 		if (orderPaymentGatewayVerifyRequest.getGatewayData() == null || orderPaymentGatewayVerifyRequest.getGatewayData().isEmpty()) {
				throw new BadRequestException("No payment gateway data specified for verification");
			}
			return new Response<OrderPaymentGatewayObject>(orderService.verifyOrderPayment(orderId, orderPaymentGatewayVerifyRequest, user));
		} catch (IllegalArgumentException e) {
			throw new BadRequestException(e.getMessage());
		} catch (Exception e) {
			throw e;
		}
	}
	
	/**
	 * 
	 * @param orderId
	 * @param shippingAddress
	 * @return
	 * @throws Exception
	 */
	@Mask(maskParameters={"mobile"})
	@RequestMapping(method = RequestMethod.PATCH, value = "/{order-id}/shipping-address")
	public @ResponseBody Response<ShippingAddress> update(@PathVariable("order-id") long orderId,
			@RequestBody HashMap<String, Object> shippingAddress) throws Exception {
		try {
			if (orderId > 0) {
				Order orderModel = orderService.get(orderId);
				if (orderModel != null) {
					return new Response<ShippingAddress>(
							orderService.partialOrderShippingAddress(orderId, shippingAddress));
				}
				throw new BadRequestException("Invalid parameters provided");
			}
			throw new BadRequestException("Invalid parameters provided");
		} catch (Exception e) {
			throw e;
		}
	}
	
	/**
	 * 
	 * @param orderId
	 * @param shippingAddress
	 * @return
	 * @throws Exception
	 */
	@Mask(maskParameters={"mobile"})
	@PreAuthorize("hasRole('ROLE_AGENT') or hasPermission('','microservice_client')")
	@RequestMapping(method = RequestMethod.PUT, value = "/{order-id}/shipping-address/{shipping-address-id}")
	public @ResponseBody Response<ShippingAddress> updateShippingAddress(OAuth2Authentication auth, @PathVariable("order-id") long orderId, @PathVariable("shipping-address-id") long shippingAddressId,
			@RequestBody ShippingAddress shippingAddress,
			@RequestParam(value = "is-delivery-option-change-allowed", required = false) boolean isDeliveryOptionChangeAllowed,
			@RequestParam(value = "is-service-type-change-allowed", required = false) boolean isServiceTypeChangeAllowed) throws Exception {
		try {
			if (orderId > 0 && shippingAddressId > 0 && shippingAddress != null && shippingAddress.getOrderId() == orderId) {
				User user = CommonUtil.getOauthUser(auth);
				return new Response<ShippingAddress>(orderService.updateShippingAddress(user, orderId,
						shippingAddressId, shippingAddress, isDeliveryOptionChangeAllowed, isServiceTypeChangeAllowed));
			}
			throw new BadRequestException("Invalid parameters provided");
		} catch (IllegalArgumentException e) {
			LOGGER.error(e.getMessage(), e);
			throw new BadRequestException(e.getMessage());
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			throw e;
		}
	}

	@PreAuthorize("hasRole('ROLE_AGENT') or hasPermission('#customerId','microservice_client')")
	@RequestMapping(value = "/count/customer/{customer-id}", method = RequestMethod.GET)
	public @ResponseBody Response<Long> get(@PathVariable("customer-id") long customerId,
			@RequestParam(name = "is-active", required = false) Boolean isActive,
			@RequestParam(name = "states", required = false) List<String> states) throws Exception {
		try {
			if (states != null && !states.isEmpty()) {
				return new Response<Long>(orderService.getOrderCountByCustomerId(customerId, states));
			}
			if (customerId > 0) {
				return new Response<Long>(orderService.getOrderCountByCustomerId(customerId, isActive));
			}
			throw new BadRequestException("Invalid parameters provided");
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * @author Manoj-Mac
	 * @param orderId
	 * @return
	 * @throws Exception
	 */
	@PreAuthorize("hasPermission('','microservice_client')")
	@RequestMapping(value = "/{order-id}/fareye-order-detail", method = RequestMethod.POST)
	public @ResponseBody FarEyeProcessData getFareyeOrderDetails(@RequestBody DispatchEvent dispatchEvent,
			@PathVariable("order-id") Long displayOrderId) throws Exception {
		try {
			if (displayOrderId > 0) {
				return orderService.getFareyeOrderDetails(displayOrderId, dispatchEvent.getReferenceId());
			}
			throw new BadRequestException("Invalid parameters provided");
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			throw e;
		}
	}
	@PreAuthorize("hasPermission('','microservice_client')")
	@RequestMapping(value = "/{order-id}/order-delivered", method = RequestMethod.POST)
	public @ResponseBody Boolean markDelivered(@PathVariable("order-id") String orderId,
			@RequestBody Object farEyeProcessData) throws Exception {
		try {
			orderId = orderId.replace("_", "/");
			orderId = URLDecoder.decode(orderId, "UTF-8");
			if (orderId != null) {
				return orderService.markOrderDelivered(orderId, farEyeProcessData);
			}
			throw new BadRequestException("Invalid parameters provided");
		} catch (Exception e) {
			throw e;
		}
	}

//	@RequestMapping(method = RequestMethod.PATCH, value = "/order-items")
//	public @ResponseBody Response<Order> updateOrderAndOrderItems(@RequestParam("order-number") String orderNumber,
//			@RequestBody OrderUpdateEvent orderUpdateEvent) throws Exception {
//		try {
//			orderNumber = URLDecoder.decode(orderNumber, "UTF-8");
//			if (orderNumber != null) {
//				return new Response<Order>(orderService.partialOrderItemUpdate(orderNumber, orderUpdateEvent));
//			}
//			throw new BadRequestException("Invalid parameters provided");
//		} catch (Exception e) {
//			LOGGER.error(e.getMessage(), e);
//			throw e;
//		}
//	}
	@PreAuthorize("hasPermission('','microservice_client')")
	@RequestMapping(value = "/{order-id}/update-status", method = RequestMethod.PATCH)
	public @ResponseBody Response<Boolean> updateStatus(@PathVariable("order-id") long orderId,
			@RequestBody OrderStateStatusRequest orderStateStatusRequest) throws Exception {

		if (orderId <= 0 || orderStateStatusRequest == null) {
			throw new BadRequestException("Invalid request params.");
		}
		return new Response<Boolean>(orderService.updateStateAndStatus(orderId, orderStateStatusRequest));
	}

	@PreAuthorize("hasPermission('','microservice_client')")
	@RequestMapping(value = "/{order-id}/auto-digitized", method = RequestMethod.PATCH)
	public @ResponseBody Response<Boolean> autoDigitized(@PathVariable("order-id") long orderId) throws Exception {

		if (orderId <= 0) {
			throw new BadRequestException("Invalid request params.");
		}
		return new Response<Boolean>(orderService.autoDigitized(orderId));
	}

	@PreAuthorize("hasPermission('','microservice_client')")
	@RequestMapping(value = "/{order-id}/auto-verified", method = RequestMethod.PATCH)
	public @ResponseBody Response<Boolean> autoVerified(@PathVariable("order-id") long orderId) throws Exception {

		if (orderId <= 0) {
			throw new BadRequestException("Invalid request params.");
		}
		return new Response<Boolean>(orderService.autoVerified(orderId));
	}

	//@PreAuthorize("hasPermission('','microservice_client')")
	@RequestMapping(method = RequestMethod.PATCH, value = "/{order-id}/split")
	public @ResponseBody Response<Boolean> splitOrder(OAuth2Authentication auth, @PathVariable("order-id") long orderId,
			@RequestBody List<UpdateOrderObject> updateOrderObjects) throws Exception {
		if (orderId <= 0 || updateOrderObjects == null || updateOrderObjects.size() < 2) {
			new BadRequestException("Invalid request params.");
		}
		User user = updateOrderObjects.get(0).getUser();
		if (user == null) {
			user = CommonUtil.getOauthUser(auth);
		}
		orderService.updateOrder(user, orderId, updateOrderObjects, OrderEvent.ORDER_SPLIT, false, false);
		return new Response<Boolean>(true);
	}
	
	@PreAuthorize("hasPermission('','microservice_client')")
	@RequestMapping(method = RequestMethod.PATCH, value = "/{order-id}/split-order")
	public @ResponseBody Response<Boolean> splitOrderBasedOnFacility(OAuth2Authentication auth, @PathVariable("order-id") long orderId) throws Exception {
		if (orderId <= 0) {
			new BadRequestException("Invalid request params.");
		}
		User user = CommonUtil.getOauthUser(auth);
		return new Response<Boolean>(orderService.splitOrderBasedOnFacility(user, orderId));
	}
	
	@PreAuthorize("hasRole('ROLE_AGENT') or hasPermission('','microservice_client')")
	@RequestMapping(method = RequestMethod.PATCH, value = "/{order-id}/merge-order")
	public @ResponseBody Response<Order> mergeOrderBasedOnFacility(OAuth2Authentication auth, @PathVariable("order-id") long orderId, @RequestBody List<Long> childOrderIds) throws Exception {
		if (orderId <= 0) {
			new BadRequestException("Invalid request params.");
		}
		User user = CommonUtil.getOauthUser(auth);
		return new Response<Order>(orderService.mergeOrderBasedOnFacility(user, orderId, childOrderIds));
	}

	// ChangeItem
	@PreAuthorize("hasPermission('','microservice_client')")
	@RequestMapping(method = RequestMethod.PATCH, value = "/{order-id}/change-items")
	public @ResponseBody Response<Order> updateOrder(OAuth2Authentication auth, @PathVariable("order-id") long orderId, @RequestParam(value="is-final-price",defaultValue = "false") boolean isFinalPrice,
			@RequestParam(value="is-invoiced",defaultValue = "false") boolean isInvoiced,
			@RequestBody UpdateOrderObject updateOrderObject) throws Exception {
		if (orderId <= 0 || updateOrderObject == null) {
			LOGGER.error("Invalid request params provided.");
			new BadRequestException("Invalid request params.");
		}
		User user = updateOrderObject.getUser();
		if (user == null) {
			user = CommonUtil.getOauthUser(auth);
		}
		
		List<UpdateOrderObject> updateOrderObjects = new ArrayList<UpdateOrderObject>();
		updateOrderObjects.add(updateOrderObject);
				
		
		return new Response<Order>(orderService.updateOrder(user, orderId, updateOrderObjects, OrderEvent.ORDER_UPDATE, isFinalPrice, isInvoiced));
	}

	@PreAuthorize("hasPermission('','microservice_client')")
	@RequestMapping(method = RequestMethod.PATCH, value = "/{order-id}/add-patient-medicines")
	public @ResponseBody Response<Boolean> addpatientMedicines(OAuth2Authentication auth,
			@PathVariable("order-id") long orderId) throws Exception {
		if (orderId <= 0) {
			new BadRequestException("Invalid request params.");
		}
		return new Response<Boolean>(orderService.addOrderItemsToPatientMedicines(orderId));
	}

	/**
	 * 
	 * @param auth
	 * @param orderId
	 * @param patientMedicines
	 * @return
	 * @throws Exception
	 * For Doctor App Agent
	 */
	@PreAuthorize("hasRole('ROLE_AGENT') or hasPermission('hasAccess','microservice_client')")
	@RequestMapping(method = RequestMethod.POST, value = "/{order-id}/patient-medicine/add-items")
	public @ResponseBody Response<List<OrderItem>> addOrderItemsFromPatientMedicines(OAuth2Authentication auth, @PathVariable("order-id") long orderId,
			@RequestBody Map<String, Object> leadDetails) throws Exception {
		if(leadDetails == null || leadDetails.isEmpty() || orderId <= 0) {
			new BadRequestException("Invalid request param.");
		}
		Agent agent = CommonUtil.getOauthAgent(auth);
		User user = new User();
		user.setFirstName(agent.getFirstName());
		user.setLastName(agent.getLastName());
		try {
			return new Response<List<OrderItem>>(orderService.addOrderItemsFromPatientMedicines(user, orderId, leadDetails));
		} catch(Exception e) {
			throw new BadRequestException(e.getMessage());
		}
	}
	
	@PreAuthorize("hasRole('ROLE_AGENT') or hasPermission('hasAccess','microservice_client')")
	@RequestMapping(method = RequestMethod.POST, value = "/{order-id}/item")
	public @ResponseBody Response<Order> addOrderItem(OAuth2Authentication auth,
			@PathVariable("order-id") long orderId, @RequestBody OrderItem orderItem,
			@RequestParam(value = "is-delivery-option-change-allowed", required = false) boolean isDeliveryOptionChangeAllowed,
			@RequestParam(value = "is-service-type-change-allowed", required = false) boolean isServiceTypeChangeAllowed, @RequestParam(value = "is-allowed", defaultValue = "false") boolean isAllowed) throws Exception {
		if (orderItem == null || orderId <= 0) {
			new BadRequestException("Invalid request param.");
		}
		User user = CommonUtil.getOauthUser(auth);
		return new Response<Order>(orderService.addOrderItem(user, orderId, orderItem, isDeliveryOptionChangeAllowed, isServiceTypeChangeAllowed, isAllowed));
	}

	@PreAuthorize("hasRole('ROLE_AGENT') or hasPermission('hasAccess','microservice_client')")
	@RequestMapping(method = RequestMethod.PUT, value = "/{order-id}/item/{order-item-id}")
	public @ResponseBody Response<OrderItem> updateOrderItem(OAuth2Authentication auth,
			@PathVariable("order-id") long orderId, @PathVariable("order-item-id") long orderItemId,
			@RequestBody OrderItem orderItem,
			@RequestParam(value = "is-delivery-option-change-allowed", required = false) boolean isDeliveryOptionChangeAllowed,
			@RequestParam(value = "is-service-type-change-allowed", required = false) boolean isServiceTypeChangeAllowed)
			throws Exception {
		if (orderItem == null || orderId <= 0) {
			new BadRequestException("Invalid request param.");
		}
		User user = CommonUtil.getOauthUser(auth);
		try {
			return new Response<OrderItem>(orderService.updateOrderItem(user, orderId, orderItem,
					isDeliveryOptionChangeAllowed, isServiceTypeChangeAllowed));
		} catch (PrescriptionExpiredException e) {
			throw e;
		} catch (Exception e) {
			throw new BadRequestException("Order item is not update due to :" + e.getMessage());
		}
		
	}

	// NEED to deprecate this API
	@PreAuthorize("hasRole('ROLE_AGENT') or hasPermission('','microservice_client')") //USER NOT ALLOWED
	@RequestMapping(method = RequestMethod.PATCH, value = "/{order-id}/item/{order-item-id}/verified")
	public @ResponseBody Response<OrderItem> verifiedOrderItem(OAuth2Authentication auth,
			@PathVariable("order-id") long orderId, @PathVariable("order-item-id") long orderItemId) throws Exception {
		if (orderItemId <= 0 || orderId <= 0) {
			new BadRequestException("Invalid request param.");
		}
		User user = CommonUtil.getOauthUser(auth);
		return new Response<OrderItem>(orderService.markItemVerified(user, orderId, orderItemId));
	}

	@PreAuthorize("hasPermission('','microservice_client')")
	@RequestMapping(method = RequestMethod.PATCH, value = "/{order-id}/items/auto-verified")
	public @ResponseBody Response<Boolean> verifiedAllOrderItem(@PathVariable("order-id") long orderId,
			@RequestBody(required = false) User user) throws Exception {
		if (orderId <= 0 && user == null) {
			new BadRequestException("Invalid request param.");
		}

		return new Response<Boolean>(orderService.markAllItemVerified(user, orderId));
	}

	@PreAuthorize("hasRole('ROLE_AGENT') or hasPermission('','microservice_client')")
	@RequestMapping(method = RequestMethod.PATCH, value = "/{order-id}/items/verified")
	public @ResponseBody Response<List<OrderItem>> verifiedOrderItems(OAuth2Authentication auth,
			@PathVariable("order-id") long orderId, @RequestBody List<Long> orderItemIds) throws Exception {
		if (orderItemIds == null || orderItemIds.isEmpty() || orderId <= 0) {
			new BadRequestException("Invalid request param.");
		}
		User user = CommonUtil.getOauthUser(auth);
		List<OrderItem> orderItems = new ArrayList<OrderItem>();
		for (Long orderItemId : orderItemIds) {
			orderItems.add(orderService.markItemVerified(user, orderId, orderItemId));
		}
		return new Response<List<OrderItem>>(orderItems);
	}
	//TODO Validate api, very sensitive
	@PreAuthorize("hasPermission('','microservice_client')")
	@RequestMapping(method = RequestMethod.PATCH, value = "/{order-id}")
	public @ResponseBody Response<Order> updateOrder(OAuth2Authentication auth, @PathVariable("order-id") long orderId,
			@RequestBody Map<String, Object> order) throws Exception {
		if (order == null || order.isEmpty() || orderId <= 0) {
			new BadRequestException("Invalid request param.");
		}
		User user = CommonUtil.getOauthUser(auth);
		return new Response<Order>(orderService.partialOrderUpdate(user, orderId, order));
	}

	@PreAuthorize("hasRole('ROLE_AGENT') or hasPermission('hasAccess','microservice_client')")
	@RequestMapping(method = RequestMethod.PATCH, value = "/{order-id}/item/{order-item-id}/delete")
	public @ResponseBody Response<OrderItem> deleteOrderItem(OAuth2Authentication auth,
			@PathVariable("order-id") long orderId, @PathVariable("order-item-id") long orderItemId) throws Exception {
		if (orderItemId <= 0 || orderId <= 0) {
			new BadRequestException("Invalid request param.");
		}
		User user = CommonUtil.getOauthUser(auth);
		return new Response<OrderItem>(orderService.markItemDeleted(user, orderId, orderItemId));
	}

	@RequestMapping(value = "cancel-reason", method = RequestMethod.GET)
	// @Cacheable("GetCancelReason")
	public @ResponseBody Response<List<Reason>> getCancelReason(@RequestParam (value = "group" , required = false, defaultValue = "cancel") String group) {
		return new Response<List<Reason>>(orderService.getCancellationReason(group));
	}
	

	@RequestMapping(value = "hold-reason", method = RequestMethod.GET)
	// @Cacheable("GetHoldReason")
	public @ResponseBody Response<List<Reason>> getHoldReason() {
		return new Response<List<Reason>>(orderService.getHoldReason());
	}

	@RequestMapping(value = "unhold-reason", method = RequestMethod.GET)
	// @Cacheable("GetUnHoldReason")
	public @ResponseBody Response<List<Reason>> getUnHoldReason() {
		return new Response<List<Reason>>(orderService.getUnHoldReason());
	}

	@PreAuthorize("hasPermission('','microservice_client')")
	@RequestMapping(method = RequestMethod.PATCH, value = "{order-id}/hold")
	public @ResponseBody Response<Boolean> holdOrder(OAuth2Authentication auth,
			@PathVariable("order-id") String orderId, @RequestBody StatusUpdateRequest statusUpdateRequest)
			throws Exception {
		try {
			User user = CommonUtil.getOauthUser(auth);
			if (statusUpdateRequest != null && orderId != null) {
				return new Response<Boolean>(orderService.holdOrder(user, orderId, statusUpdateRequest));
			}
			throw new BadRequestException("Invalid parameters provided");
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			throw e;
		}
	}

	@PreAuthorize("hasPermission('','microservice_client')")
	@RequestMapping(method = RequestMethod.PATCH, value = "{order-id}/unhold")
	public @ResponseBody Response<Boolean> unHoldOrder(OAuth2Authentication auth,
			@PathVariable("order-id") String orderId, @RequestBody StatusUpdateRequest statusUpdateRequest)
			throws Exception {
		try {
			User user = CommonUtil.getOauthUser(auth);
			if (statusUpdateRequest != null && orderId != null) {
				return new Response<Boolean>(orderService.unHoldOrder(user, orderId, statusUpdateRequest));
			}
			throw new BadRequestException("Invalid parameters provided");

		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			throw e;
		}
	}

	@RequestMapping(value = "/statuses", method = RequestMethod.GET)
	public @ResponseBody Response<Map<String, List<String>>> getStatuses(@RequestParam(name = "category", defaultValue = Order.CATEGORY.MEDICINE, required = false) String category) throws Exception {
		return new Response<Map<String, List<String>>>(orderService.getAllStatuses(category));
	}

	@RequestMapping(value = "/sources", method = RequestMethod.GET)
	public @ResponseBody Response<List<String>> getSources() throws Exception {
		return new Response<List<String>>(orderService.getAllSources());
	}

	@RequestMapping(value = "/{order-id}/synced", method = RequestMethod.GET)
	public @ResponseBody Response<Boolean> isOrderSynced(@PathVariable("order-id") long orderId) throws Exception {
		try {
			if (orderId > 0) {
				return new Response<Boolean>(orderService.isOrderSynced(orderId));
			}
			throw new BadRequestException("Invalid parameters provided");
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			throw e;
		}
	}

	@PreAuthorize("hasPermission('','microservice_client')")
	@RequestMapping(method = RequestMethod.PATCH, value = "/{order-id}/{facility-id}/sync-price")
	public @ResponseBody Response<Order> syncPrice(@PathVariable("order-id") long orderId,
			@PathVariable("facility-id") long facilityId, @RequestBody(required = false) User user) throws Exception {
		if (orderId <= 0 && user == null && facilityId <= 0) {
			throw new BadRequestException("Invalid request param.");
		}

		return new Response<Order>(orderService.syncPrice(user, orderId, facilityId));
	}
	
	@PreAuthorize("hasRole('ROLE_AGENT') or hasPermission('','microservice_client')")
	@RequestMapping(method = RequestMethod.PATCH, value = "/{order-id}/change-coupon")
	public @ResponseBody Response<Order> updateCoupon(OAuth2Authentication auth, @PathVariable("order-id") long orderId, @RequestBody String couponCode, HttpServletResponse response) throws Exception {
		if (orderId <= 0 || StringUtils.isBlank(couponCode)) {
			throw new BadRequestException("Invalid request param.");
		}
		User user = CommonUtil.getOauthUser(auth);
		Map<String, Object> responseHashmap = orderService.updateCoupon(user, orderId, couponCode);
		Order order = (Order) responseHashmap.getOrDefault("payload", null);
		Response<Order> responseEntity = new Response<Order>(order);
		in.lifcare.core.response.model.Error error = (Error) responseHashmap.getOrDefault("error", null);
		response.setStatus(HttpStatus.OK.value());
		if(error != null){
			responseEntity.setError(error);
			responseEntity.setHttpStatus(HttpStatus.MULTI_STATUS);
			response.setStatus(HttpStatus.MULTI_STATUS.value());		
		}
		return responseEntity;
	}

	@PreAuthorize("hasRole('ROLE_AGENT') or hasPermission('','microservice_client')")
	@RequestMapping(value = "/{order-id}/offset-score", method = RequestMethod.PATCH)
	public @ResponseBody Response<Float> updateOffsetScore(OAuth2Authentication auth,
			@PathVariable("order-id") long orderId, @RequestBody HashMap<String, Integer> offsetScoreMap) throws Exception {
		if (orderId > 0 && offsetScoreMap != null && !offsetScoreMap.isEmpty() && offsetScoreMap.containsKey("offset_score")) {
			User user = CommonUtil.getOauthUser(auth);
			if (orderId > 0) {
				try {
					// We only update offset score and not entire object.
					return new Response<Float>(orderService.updateOrderOffsetScore(user, orderId, offsetScoreMap.get("offset_score")));
				} catch (Exception e) {
					LOGGER.error("Failed to update offset-score with order-id " + orderId + ", due to " + e.getMessage(),
							e);
					throw e;
				}
			}
		}
		throw new BadRequestException("Invalid parameters provided");
	}

	/**
	 * 
	 * @param auth
	 * @param orderId
	 * @param partialOrderObject
	 * @return
	 * @throws Exception
	 */
	@PreAuthorize("hasRole('ROLE_AGENT') or hasPermission('','microservice_client')")
	@RequestMapping(value = "/{order-id}/promised-delivery-details", method = RequestMethod.PATCH)
	public @ResponseBody Response<Order> updatePromisedDeliveryDetails(OAuth2Authentication auth,
			@PathVariable("order-id") long orderId, @RequestBody Map<String, Object> partialOrderObject)
			throws Exception {
		if (orderId > 0 && partialOrderObject != null && !partialOrderObject.isEmpty()) {
			User user = CommonUtil.getOauthUser(auth);
			try {
				// We only update promised delivery date and not entire object.
				return new Response<Order>(
						orderService.updatePromisedDeliveryDetails(user, orderId, partialOrderObject));
			} catch (Exception e) {
				LOGGER.error("Failed to update promised delivery details with order-id " + orderId + " due to "
						+ e.getMessage(), e);
				throw e;
			}
		}
		throw new BadRequestException("Invalid parameters provided");
	}
	

	/**
	 * 
	 * @param orderId
	 * @return
	 * @throws Exception
	 */
	@Mask(maskParameters={"mobile"})
	@PreAuthorize("hasRole('ROLE_AGENT') or hasPermission('hasAccess','microservice_client')")
	@RequestMapping(value = "/{order-id}/shipping-address", method = RequestMethod.GET)
	public @ResponseBody Response<ShippingAddress> getShippingAddress(@PathVariable("order-id") long orderId) throws Exception {
		if (orderId > 0) {
			try{
				return new Response<ShippingAddress>(orderShippingAddressService.findByOrderId(orderId));
			}catch (Exception e) {
				LOGGER.error("Failed to get shipping address with order-id "+ orderId +" due to " + e.getMessage(), e);
				throw e;
			}
		}
		throw new BadRequestException("Invalid parameters provided");
	}
	
	@PostAuthorize("hasRole('ROLE_AGENT') or hasPermission(returnObject.payload != null ? returnObject.payload.customerId : 'hasAccess' ,'microservice_client')")
	@RequestMapping(value = "/patient/{patient-id}/active-order", method = RequestMethod.GET)
	public @ResponseBody Response<Order> getActiveOrderByPatientId(@PathVariable("patient-id") long patientId) throws Exception {
		if (patientId > 0) {
			try{
				return new Response<Order>(orderService.getActiveOrderByPatientId(patientId));
			}catch (Exception e) {
				LOGGER.error("Failed to get order with patient-id "+ patientId +" due to " + e.getMessage(), e);
				throw e;
			}
		}
		throw new BadRequestException("Invalid parameters provided");
	}

	@PreAuthorize("hasRole('ROLE_AGENT') or hasPermission('hasAccess','microservice_client')")
	@RequestMapping(value = "/customer/{customer-id}/applicable-shipping-charge", method = RequestMethod.GET)
	public @ResponseBody Response<Map<String,Object>> getApplicableShippingCharge(@PathVariable("customer-id") long customerId,
			@RequestParam("pincode") long pincode, @RequestParam("order-total") float totalMrp) throws Exception {
		if (customerId > 0) {
			try {
				return new Response<Map<String,Object>>(orderService.getApplicableShippingCharge(customerId, pincode, totalMrp));
			} catch (Exception e) {
				LOGGER.error("Failed to get order with patient-id " + customerId + " due to " + e.getMessage(), e);
				throw e;
			}
		}
		throw new BadRequestException("Invalid parameters provided");
	}
	
	@PreAuthorize("hasRole('ROLE_AGENT') or hasPermission('','microservice_client')")
	@RequestMapping(value = "/{order-id}/remove-shipping-charge", method = RequestMethod.DELETE)
	public @ResponseBody Response<Boolean> removeShippingCharge(@PathVariable("order-id") long orderId) throws Exception {
		if (orderId > 0) {
			try {
				return new Response<Boolean>(orderService.removeShippingCharge(orderId));
			} catch (Exception e) {
				LOGGER.error("Failed to get order with patient-id " + orderId + " due to " + e.getMessage(), e);
				throw e;
			}
		}
		throw new BadRequestException("Invalid parameters provided");
	}

	@PreAuthorize("hasRole('ROLE_AGENT') or hasPermission('hasAccess','microservice_client')")
	@RequestMapping(value = "/{order-id}/items/prescription/{prescription-id}", method = RequestMethod.PATCH)
	public @ResponseBody Response<List<OrderItem>> updateOrderItemPrecriptionId(@PathVariable("order-id") long orderId, @PathVariable("prescription-id") Long prescriptionId,
			@RequestBody List<String> itemSkuList) throws Exception {
		if (orderId > 0 && prescriptionId != null && prescriptionId > 0 && !itemSkuList.isEmpty() && itemSkuList != null) {
			return new Response<List<OrderItem>>(orderItemService.updateOrderItemPrecriptionId(orderId, prescriptionId, itemSkuList));
		}
		throw new BadRequestException("Invalid Parameters Provided");
	}
	
	@PreAuthorize("hasRole('ROLE_AGENT') or hasPermission('hasAccess','microservice_client')")
	@RequestMapping(value = "/{order-id}/items/prescription", method = RequestMethod.PATCH)
	public @ResponseBody Response<List<OrderItem>> updateOrderItemPrecriptionId(@PathVariable("order-id") long orderId, @RequestBody Map<String, Long> skuPrescriptionMap) throws Exception {
		if (orderId > 0 && skuPrescriptionMap != null && !skuPrescriptionMap.isEmpty()) {
			return new Response<List<OrderItem>>(orderItemService.updateOrderItemPrecriptionId(orderId, skuPrescriptionMap));
		}
		throw new BadRequestException("Invalid Parameters Provided");
	}
	
	@PreAuthorize("hasRole('ROLE_AGENT') or hasPermission('','microservice_client')")
	@RequestMapping(method = RequestMethod.PATCH, value = "/{order-id}/packaging-type")
	public @ResponseBody Response<Boolean> updatePackingType(OAuth2Authentication auth, @PathVariable("order-id") long orderId,
			@RequestParam("packaging-type") String packagingType) throws Exception {
		if (packagingType == null || packagingType.isEmpty() || orderId <= 0) {
			new BadRequestException("Invalid request param.");
		}
		CommonUtil.getOauthUser(auth);
		return new Response<Boolean>(orderService.updatePackagingType(orderId, packagingType));
	}
	
	@PreAuthorize("hasRole('ROLE_AGENT') or hasPermission('','microservice_client')")
	@RequestMapping(value = "/delivery-option-change-orders", method = RequestMethod.GET)
	public @ResponseBody Response<Page<Order>> getOrdersForDeliveryOptionChangeTracking(
			@RequestParam("delivery-option") String deliveryOption,
			@PageableDefault(page = 0, size = 30, sort = { "score" }, direction = Direction.DESC) Pageable pageable)
			throws Exception {
		try {
			if (deliveryOption != null) {
				return new Response<Page<Order>>(
						orderService.getOrdersForDeliveryOptionChangeTracking(deliveryOption, pageable));
			}
			throw new BadRequestException("Invalid parameters provided");
		} catch (Exception e) {
			throw e;
		}
	}
	
	@PreAuthorize("hasRole('ROLE_AGENT') or hasPermission('','microservice_client')")
	@RequestMapping(value = "/service-type-change-orders", method = RequestMethod.GET)
	public @ResponseBody Response<Page<Order>> getOrdersForDeliveryTracking(
			@RequestParam("service-type") String serviceType, 
			@PageableDefault(page = 0, size = 30, sort = { "score" }, direction = Direction.DESC) Pageable pageable)
			throws Exception {
		try {
			if (serviceType != null) {
				return new Response<Page<Order>>(
						orderService.getOrdersForServiceTypeChangeTracking(serviceType, pageable));
			}
			throw new BadRequestException("Invalid parameters provided");
		} catch (Exception e) {
			throw e;
		}
	}

	@PreAuthorize("hasRole('ROLE_AGENT') or hasPermission('','microservice_client')")
	@RequestMapping(value = "/{order-id}/additional-items", method = RequestMethod.GET)
	public @ResponseBody Response<List<OrderAdditionalItem>> getOrderAdditionalItemsByOrderId(@PathVariable("order-id") long orderId) throws Exception {
		if (orderId <= 0) {
			throw new BadRequestException("Invalid parameters provided");
		}
		try {
			return new Response<List<OrderAdditionalItem>>(orderAdditionalItemService.getOrderAdditionalItemsByOrderId(orderId));
		} catch (IllegalArgumentException ex) {
			throw new BadRequestException(ex.getMessage());
		} catch (Exception e) {
			throw e;
		}
	}
 	
	@PreAuthorize("hasRole('ROLE_AGENT') or hasPermission('','microservice_client')")
	@RequestMapping(method = RequestMethod.PATCH, value = "/{order-id}/update-delivery-option")
	public @ResponseBody Response<Boolean> updateDeliveryOption(OAuth2Authentication auth,
			@PathVariable("order-id") long orderId, @RequestBody(required = false) Map<String, Object> map) throws Exception {
		if (orderId <= 0) {
			new BadRequestException("Invalid request param.");
		}
		User user = CommonUtil.getOauthUser(auth);
		Boolean systemUpdated = true;
		String reason = OrderDeliveryObject.DELIVERY_OPTION_CHANGE_REASON.COMMITED_DELIVERY_TIME_LINE_EXCEEDED;
		return new Response<Boolean>(orderService.updateDeliveryOption(orderId, null, reason, systemUpdated, map, user));
	}

	@PreAuthorize("hasRole('ROLE_AGENT') or hasPermission('','microservice_client')")
	@RequestMapping(method = RequestMethod.PATCH, value = "/{order-id}/update-service-type")
	public @ResponseBody Response<Boolean> updateServiceType(OAuth2Authentication auth,
			@PathVariable("order-id") long orderId, @RequestBody(required = false) Map<String, Object> map) throws Exception {
		if (orderId <= 0) {
			new BadRequestException("Invalid request param.");
		}
		User user = CommonUtil.getOauthUser(auth);
		Boolean systemUpdated = true;
		return new Response<Boolean>(orderService.updateServiceType(orderId, null, systemUpdated,  null, map, user));
	}

	@PreAuthorize("hasRole('ROLE_AGENT') or hasPermission('','microservice_client')")
	@RequestMapping(value = "/{order-id}/additional-items", method = RequestMethod.POST)
	public @ResponseBody Response<List<OrderAdditionalItem>> saveOrderAdditionalItemsByOrderId(@PathVariable("order-id") long orderId, @RequestBody List<OrderAdditionalItem> orderAdditionalItems) throws Exception {
		if (orderId <= 0 || orderAdditionalItems == null || orderAdditionalItems.isEmpty()) {
			throw new BadRequestException("Invalid parameters provided");
		}
		try {
			return new Response<List<OrderAdditionalItem>>(orderAdditionalItemService.saveOrderAdditionalItems(orderAdditionalItems));
		} catch (IllegalArgumentException ex) {
			throw new BadRequestException(ex.getMessage());
		} catch (Exception e) {
			throw e;
		}
	}
	
	@RequestMapping(value = "/reason", method = RequestMethod.GET)
	public @ResponseBody Response<List<Reason>> getUnHoldReason(@RequestParam("group-type") String groupType) {
		return new Response<List<Reason>>(orderService.getReasonByGroup(groupType));
	}
	
	@PreAuthorize("hasRole('ROLE_AGENT') or hasPermission('hasAccess','microservice_client')")
	@RequestMapping(value = "/{order-id}/is-eligible-for-urgent-delivery", method = RequestMethod.GET)
	public @ResponseBody Response<Boolean> isEligibleForUrgentDelivery(@PathVariable("order-id") long orderId)
			throws Exception {
		if (orderId > 0) {
			try {
				return new Response<Boolean>(orderService.isEligibleForUrgentDelivery(orderId));
			} catch (Exception e) {
				LOGGER.error(
						"Failed to check urgent delivery option for order-id " + orderId + " due to " + e.getMessage(),
						e);
				throw e;
			}
		}
		throw new BadRequestException("Invalid parameters provided");
	}
	/**
	 * only visible to microservices not open customer and agent and not to do so
	 * @param shippingAddressId
	 * @return
	 * @throws Exception
	 */
	@PreAuthorize("hasPermission('','microservice_client')")
	@RequestMapping(value = "/shipping-address/{shipping-address-id}", method = RequestMethod.GET)
	public @ResponseBody Response<ShippingAddress> getShippingAddressByShippingAddressId(@PathVariable("shipping-address-id") long shippingAddressId)
			throws Exception {
		if (shippingAddressId > 0) {
			try {
				return new Response<ShippingAddress>(orderShippingAddressService.getShippingAddressByShippingAddressId(shippingAddressId));
			} catch (Exception e) {
				LOGGER.error(
						"Failed to get order-shipping-address by  order-shipping-address-id " + shippingAddressId + " due to " + e.getMessage(),
						e);
				throw e;
			}
		}
		throw new BadRequestException("Invalid parameters provided");
	}
	
	@PreAuthorize("hasPermission('','microservice_client')")
	@RequestMapping(value = "/{order-id}/auto-verified-failed/event", method = RequestMethod.POST)
	public @ResponseBody Response<Boolean> createAutoVerifiedFailedEvent(@PathVariable("order-id") long orderId) {
		return new Response<Boolean>(orderService.createAutoVerifiedFailedEvent(orderId));
	}
	
	@RequestMapping(value = "/{order-id}/prescriptions", method = RequestMethod.PATCH)
	public @ResponseBody Response<Order> updateOrderPrescription(OAuth2Authentication auth, @PathVariable("order-id") long orderId, @RequestBody Map<String, Object> updateMap) {

		if (orderId <= 0 || updateMap == null || updateMap.isEmpty()) {
			throw new BadRequestException("Invalid request param!");
		}
		Order order = null;
		User user = new User();
		try {
			user = CommonUtil.getOauthUser(auth);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (updateMap.get("prescription_ids") == null && updateMap.get("doctor_callback") == null) {
			throw new BadRequestException("Prescription-ids or Doctor-callback can not be null or empty!");
		}
		List<Long> prescriptionIds = null;
		boolean isDoctorCallBack = false;
		try {
			if (updateMap.get("prescription_ids") != null && StringUtils.isNotBlank(updateMap.get("prescription_ids").toString())) {
				prescriptionIds = mapper.readValue(updateMap.get("prescription_ids").toString(), mapper.getTypeFactory().constructCollectionType(List.class, Long.class));
			}
			if (updateMap.containsKey("doctor_callback") && StringUtils.isNotBlank(updateMap.get("doctor_callback").toString())) {
				isDoctorCallBack = (Boolean) updateMap.get("doctor_callback");
			}
			order = orderService.updateOrderPrescriptionOrDoctorCallBack(orderId, prescriptionIds, isDoctorCallBack, user);
			
		} catch (Exception e) {
			LOGGER.debug("Order {} not marked as prescription processed due {}", order.getId(), e);
			throw new BadRequestException(e.getMessage());
		}
		return new Response<Order>(order);
	}
	
	@PreAuthorize("hasRole('ROLE_AGENT') or hasPermission('','microservice_client')")
	@RequestMapping(method = RequestMethod.PATCH, value = "/{order-id}/appointment")
	public @ResponseBody Response<Order> updateAppointment(OAuth2Authentication auth,
			@PathVariable("order-id") long orderId, @RequestBody Map<String, Object> appointmentInfoMap) throws Exception {
		if (orderId <= 0) {
			new BadRequestException("Invalid request param.");
		}
		User user = CommonUtil.getOauthUser(auth);
		try {
			if (appointmentInfoMap == null || appointmentInfoMap.isEmpty()) {
				throw new BadRequestException("Invalid parameters provided");
			}
			return new Response<Order>(orderService.updateAppointmentDetails(orderId, appointmentInfoMap, user));
		} catch (Exception e) {
			throw e;
		}
	}
	
	@PreAuthorize("hasRole('ROLE_AGENT') or hasPermission('hasAccess','microservice_client')")
	@RequestMapping(value = "/{order-id}/lab-report-delivery-option", method = RequestMethod.PATCH)
	public @ResponseBody Response<Boolean> updateReportDeliveryOption(OAuth2Authentication auth,
			@PathVariable("order-id") long orderId, @RequestBody Map<String, Object> map) {
		try {
			if (map == null || map.isEmpty()) {
				throw new BadRequestException("Invalid parameters provided");
			}
			return new Response<Boolean>(orderService.updateReportDeliveryOption(orderId, null, map));
		} catch (IllegalArgumentException e) {
			throw new CartException(e.getMessage());
		} catch (Exception e) {
			throw e;
		}
	}
	
	@PreAuthorize("hasRole('ROLE_AGENT') or hasPermission('hasAccess','microservice_client')")
	@RequestMapping(value = "/{order-id}/prescriptions", method = RequestMethod.GET)
	public @ResponseBody Response<List<OrderPatientPrescription>> getOrderPrescriptions(OAuth2Authentication auth, @PathVariable("order-id") long orderId) {
		if (orderId <= 0) {
			throw new BadRequestException("Invalid order-id provided!");
		}
		try {
			return new Response<List<OrderPatientPrescription>>(orderService.getOrderPrescriptions(orderId));
		} catch (Exception e) {
			throw new BadRequestException("Order prescriptions are not found due to : " + e.getMessage());
		}
	}
	
	@PreAuthorize("hasRole('ROLE_AGENT') or hasPermission('hasAccess','microservice_client')")
	@RequestMapping(value = "/{order-id}/appointment/auto-assign", method = RequestMethod.PATCH)
	public @ResponseBody Response<Order> autoAssignAppointmentOrder(OAuth2Authentication auth, @PathVariable("order-id") long orderId) {
		if (orderId <= 0) {
			throw new BadRequestException("Invalid order-id provided!");
		}
		try {
			return new Response<Order>(orderService.autoAssignAppointmentOrder(orderId));
		} catch (Exception e) {
			throw new BadRequestException("Appointment not assigned due to : " + e.getMessage());
		}
	}
	
	@PreAuthorize("hasPermission('','microservice_client')")
	@RequestMapping(method = RequestMethod.PATCH, value = "/{order-id}/change-facility")
	public @ResponseBody Response<Boolean> moveJitForOrder(OAuth2Authentication auth, @PathVariable("order-id") long orderId,
			@RequestBody List<UpdateOrderObject> updateOrderObjects) throws Exception {
		if (orderId <= 0 || updateOrderObjects == null || updateOrderObjects.size() < 2) {
			new BadRequestException("Invalid request params.");
		}
		User user = updateOrderObjects.get(0).getUser();
		if (user == null) {
			user = CommonUtil.getOauthUser(auth);
		}
		return new Response<Boolean>(orderService.moveJitForOrder(user, orderId, updateOrderObjects));
	}
	
	@PreAuthorize("hasRole('ROLE_AGENT') or hasPermission('hasAccess','microservice_client')")
	@RequestMapping(value = "/eligible-for-status-check", method = RequestMethod.GET)
	public @ResponseBody Response<List<Long>> isEligibleForStatusChange(@RequestParam("courier-type") String courierType)
			throws Exception {
		if (StringUtils.isNotBlank(courierType)){
			try {
				return new Response<List<Long>>(orderService.isEligibleForStatusChange(courierType));
			} catch (Exception e) {
				throw e;
			}
		}
		throw new BadRequestException("Invalid parameters provided");
	}
	
	@PreAuthorize("hasRole('ROLE_AGENT') or hasPermission('hasAccess','microservice_client')")
	@RequestMapping(value = "/customer/{customer-id}/order-details", method = RequestMethod.GET)
	public @ResponseBody Response<Map<String, Object>> calculateOrders(@PathVariable("customer-id") long customerId) {
		try {
			if (customerId > 0) {
				return new Response<Map<String, Object>>(
						(Map<String, Object>) orderService.getOrdersDetailByCustomerId(customerId));
			}
			throw new BadRequestException("Invalid customer-id provided");
		} catch (Exception e) {
			LOGGER.debug("Invalid parameters provided ", e);
			throw e;
		}
	}
	
	@PreAuthorize("hasRole('ROLE_AGENT') or hasPermission('hasAccess','microservice_client')")
	@RequestMapping(value = "/{order-id}/move-to-facility", method = RequestMethod.PATCH)
	public @ResponseBody Response<Order> moveToFacility(OAuth2Authentication auth, @PathVariable("order-id") long orderId,@RequestParam("spoke-id") Long spokeId) {
		if (orderId <= 0) {
			throw new BadRequestException("Invalid order-id provided!");
		}
		try {
			return new Response<Order>(orderService.moveToFacility(orderId,spokeId));
		} catch (Exception e) {
			throw new BadRequestException("Cannot Chnage Facility due to : " + e.getMessage());
		}
	}
	
	@PreAuthorize("hasRole('ROLE_AGENT') or hasPermission('hasAccess','microservice_client')")
	@RequestMapping(value = "/customer/{customer-id}/merge", method = RequestMethod.PATCH)
	public @ResponseBody Response<Order> mergeOrder(OAuth2Authentication auth, @RequestBody HashMap<String, List<Long>> orderIdsMap, @PathVariable("customer-id") long customerId) {
		if (CollectionUtils.isEmpty(orderIdsMap) || CollectionUtils.isEmpty(orderIdsMap.get("order_ids"))
				|| customerId <= 0) {
			throw new BadRequestException("Invalid order-id provided!");
		}
		try {
				User user = CommonUtil.getOauthUser(auth);
			return new Response<Order>(orderService.mergePickedOrders(user, customerId, orderIdsMap.get("order_ids")));
		} catch (Exception e) {
			LOGGER.error("Cannot Chnage Merge Order due to : " + e.getMessage());
			throw new BadRequestException(e.getMessage());
		}
	}
	
	@RequestMapping(value = "/create/basic-order/{reference-id}/source/{source}", method = RequestMethod.POST)
	@PostAuthorize("hasRole('ROLE_AGENT') or hasPermission(returnObject.payload.customerId,'microservice_client')")
	public @ResponseBody Response<Order> addTempOrder(OAuth2Authentication auth,
			@PathVariable("reference-id") String referenceId, @PathVariable("source") String source,
			@RequestBody(required = false) User user) throws Exception {
		try {
			if (StringUtils.isNotBlank(referenceId) && StringUtils.isNotBlank(source)
					&& Order.SOURCE.POS.equalsIgnoreCase(source)) {

				// New order
				try {
					if (user == null) {
						user = CommonUtil.getOauthUser(auth);
					}
				} catch (Exception e) {
					user.setFirstName("Default User");
				}
				Order order = orderService.addBasicOrder(user, referenceId, source);
				return new Response<Order>(order);
			}
			throw new BadRequestException("Invalid parameters provided");

		} catch (Exception e) {
			throw e;
		}

	}
	
	private static final Logger LOGGER = LoggerFactory.getLogger(OrderController.class);

	private ObjectMapper mapper = new ObjectMapper();
	
	@Autowired
	private CartService cartService;
	
	@Autowired
	private OrderService orderService;

	@Autowired
	private OrderShippingAddressService orderShippingAddressService;
	
	@Autowired
	private OrderEventService orderEventService;
	
	@Autowired
	private OrderItemService orderItemService;

	@Autowired
	private OrderAdditionalItemService orderAdditionalItemService;
	
}
