package in.lifcare.order.service;

import java.lang.reflect.InvocationTargetException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import in.lifcare.core.fareye.model.FarEyeProcessData;
import in.lifcare.core.model.OrderStateStatusRequest;
import in.lifcare.core.model.UpdateOrderObject;
import in.lifcare.core.model.User;
import in.lifcare.order.microservice.payment.model.OrderPaymentGatewayVerifyRequest;
import in.lifcare.order.microservice.payment.model.PaymentChannelData;
import in.lifcare.order.model.Order;
import in.lifcare.order.model.OrderCreationCartRequest;
import in.lifcare.order.model.OrderItem;
import in.lifcare.order.model.OrderPatientPrescription;
import in.lifcare.order.model.OrderPaymentGatewayObject;
import in.lifcare.order.model.OrderPrice;
import in.lifcare.order.model.OrderSearchResponse;
import in.lifcare.order.model.OrderSummary;
import in.lifcare.order.model.Reason;
import in.lifcare.order.model.ShippingAddress;
import in.lifcare.order.model.StatusUpdateRequest;
import lombok.NonNull;

public interface OrderService {

	Order saveOrder(Order order);
	
	Order add(Order order, User user) throws Exception;

	Order get(long orderId) throws Exception;

	Page<Order> recentOrders(long customerId, Pageable pageable) throws Exception;

	Order updateOrderNextRefillDay(long orderId, Timestamp nextRepeatDate, int days) throws Exception;

	Page<Order> getOrders(long customerId, String category, Pageable pageable) throws Exception;

	// M
	Long getOrderCountByCustomerId(long customerId, Boolean isActive) throws Exception;

	ShippingAddress update(Order orderModel, ShippingAddress shippingAddressBody) throws Exception;

	ShippingAddress partialOrderShippingAddress(long orderId, HashMap<String, Object> shippingAddress) throws Exception;

	/**
	 * 
	 * @param orderId
	 * @param orderNumber
	 * @return
	 * @throws Exception
	 */
	FarEyeProcessData getFareyeOrderDetails(Long orderId, String orderNumber) throws Exception;

	Boolean markOrderDelivered(String orderId, Object farEyeProcessData) throws Exception;

	Page<Order> getOrdersByPatient(long patientId, String category, Pageable pageable);

	List<OrderItem> getItems(long orderId);

	//Order partialOrderItemUpdate(String orderNumber, OrderUpdateEvent orderUpdateEvent);

	Order updateOrder(User user, UpdateOrderObject updateOrderObject, Order order, boolean isResetFinalPrice, OrderPrice orderPrice, Boolean applyCoupon, Boolean isChildCreating, boolean isInvoiced) throws Exception;

	List<Reason> getCancellationReason(String group);

	List<Reason> getHoldReason();

	List<Reason> getUnHoldReason();

	Boolean cancelOrder(User user, String orderId, StatusUpdateRequest statusUpdateRequest);

	Boolean holdOrder(User user, String orderId, StatusUpdateRequest statusUpdateRequest);

	Boolean unHoldOrder(User user, String orderId, StatusUpdateRequest statusUpdateRequest);

	Order createChild(User user, UpdateOrderObject updateOrderObject, Order order) throws Exception;

	Order updateOrderStatus(Order order, String string);

	List<OrderItem> setOrderItems(User user, Order order, List<OrderItem> orderItems);

	Order addOrderItem(User user, long orderId, OrderItem orderItem, boolean isDeliveryOptionChangeAllowed, boolean isServiceTypeChangeAllowed, boolean isAllowed) throws Exception;

	ShippingAddress updateShippingAddress(User user, long orderId, long shippingAddressId,
			ShippingAddress shippingAddress, boolean isDeliveryOptionChangeAllowed, boolean isServiceTypeChangeAllowed)
			throws Exception;

	/**
	 * 
	 * @param user
	 * @param orderId
	 * @param orderItem
	 * @param isDeliveryOptionChangeAllowed
	 * @param isServiceTypeChangeAllowed
	 * @return
	 * @throws Exception
	 */
	OrderItem updateOrderItem(User user, long orderId, OrderItem orderItem, boolean isDeliveryOptionChangeAllowed,
			boolean isServiceTypeChangeAllowed) throws Exception;

	OrderItem markItemVerified(User user, long orderId, long orderItemId) throws Exception;

	OrderItem markItemDeleted(User user, long orderId, long orderItemId) throws Exception;

	Order partialOrderUpdate(User user, long orderId, Map<String, Object> order) throws Exception;

	Order markOrderDigitized(User user, long orderId, List<OrderItem> orderItems) throws Exception;

	Order markOrderVerified(User user, long orderId) throws Exception;

	Order shortOrder(User user, long orderId, UpdateOrderObject updateOrderObject) throws Exception;

	Boolean splitOrder(User user, long orderId, List<UpdateOrderObject> updateOrderObjects) throws Exception;

	Order updateOrder(User user, long orderId, UpdateOrderObject updateOrderObject, boolean isFinalPrice, boolean isInvoiced) throws Exception;

	Boolean updateStateAndStatus(long orderId, OrderStateStatusRequest orderStateStatusRequest) throws Exception;

	Map<String, List<String>> getAllStatuses(String category);

	List<String> getAllSources();

	Boolean isOrderSynced(long orderId) throws Exception;

	Order getOrder(long orderId) throws Exception;

	Boolean autoVerified(long orderId) throws Exception;

	Boolean autoDigitized(long orderId) throws Exception;

	Boolean markAllItemVerified(User user, long orderId) throws Exception;

	boolean addOrderItemsToPatientMedicines(long orderId) throws Exception;

	List<OrderItem> addOrderItemsFromPatientMedicines(User user, long orderId, Map<String, Object> leadDetails) throws Exception;

	String getRegionCodeByOrderId(String state);

	Order syncPrice(User user, long orderId, long facilityId) throws Exception;

	float updateOrderOffsetScore(User user, long orderId, int offsetScore);

	Order updatePromisedDeliveryDetails(User user, long orderId, Map<String, Object> partialOrderObject);

	Map<String, Object> updateCoupon(User user, long orderId, String couponCode) throws Exception;
	
	Order getActiveOrderByPatientId(long patientId);

	Map<String,Object> getApplicableShippingCharge(long customerId, long pincode, float totalMrp) throws Exception;

	Boolean removeShippingCharge(long orderId);

	Long getOrderCountByCustomerId(long customerId, List<String> states) throws Exception;

	Order createCartOrder(OrderCreationCartRequest orderCreationCartRequest, User user) throws Exception;

	Page<Order> activeOrders(long customerId, Pageable pageable) throws Exception;
	
	boolean updatePackagingType(long orderId, String packagingType);

	Boolean updateDeliveryOption(long orderId, Order order, String reason, boolean systemUpdated, Map<String, Object> updatedMap, User user);

	Boolean updateServiceType(long orderId, Order order, boolean systemUpdated, String reason, Map<String, Object> updateMap, User user);
	
	/**
	 * 
	 * @param groupType
	 * @return
	 */
	List<Reason> getReasonByGroup(String groupType);

	/**
	 * 
	 * @param orderId
	 * @return
	 */
	Boolean isEligibleForUrgentDelivery(long orderId);

	/**
	 * 
	 * @param serviceType
	 * @param pageable
	 * @return
	 * @throws Exception
	 */
	Page<Order> getOrdersForServiceTypeChangeTracking(String serviceType, Pageable pageable) throws Exception;

	/**
	 * 
	 * @param deliveryOption
	 * @param pageable
	 * @return
	 * @throws Exception
	 */
	Page<Order> getOrdersForDeliveryOptionChangeTracking(String deliveryOption, Pageable pageable) throws Exception;
	
	/**
	 * 
	 * @param orderId
	 * @return
	 */
	boolean createAutoVerifiedFailedEvent(@NonNull long orderId);
	
	/**
	 * 
	 * @param orderId
	 * @param integer
	 * @return
	 * @throws Exception
	 */
	Order updateOrderRevisitDay(long orderId, int integer) throws Exception;


	/**
	 * 
	 * @param orderId
	 * @return
	 */
	List<PaymentChannelData> fetchOrderPaymentChannels(long orderId);

	/**
	 * 
	 * @param orderId
	 * @param orderPaymentGatewayVerifyRequest
	 * @param user 
	 * @return
	 * @throws Exception 
	 */
	OrderPaymentGatewayObject verifyOrderPayment(long orderId, OrderPaymentGatewayVerifyRequest orderPaymentGatewayVerifyRequest, User user) throws Exception;

	List<Long> getCustomerIdsByCreatedAtAfter(Timestamp createdAtAfter);
	
	Order updateOrderPrescriptionOrDoctorCallBack(long orderId, List<Long> prescriptionIds, boolean isDoctorCallBack, User user) throws Exception;

	/**
	 * 
	 * @param order
	 * @param paymentMethod
	 * @param paymentSubMethod
	 * @param orderPlaced
	 * @return
	 * @throws Exception 
	 */
	OrderPaymentGatewayObject initiatePayment(Order order, String paymentMethod, String paymentSubMethod, boolean orderPlaced) throws Exception;

	/**
	 * 
	 * @param customerId
	 * @param category TODO
	 * @param pageable
	 * @return
	 */
	Page<OrderSummary> getOrdersSummary(long customerId, String category, Pageable pageable);

	/**
	 * 
	 * @param orderId
	 * @return
	 */
	Boolean updateItemsPrescription(long orderId);
	 
	 /**
	 * @param createdAtBegins
	 * @param createdAtEnds
	 * @param pageable
	 * @return
	 */
	Page<Order> getOrdersCreatedAtBetween(Timestamp createdAtBegins, Timestamp createdAtEnds, Pageable pageable);

	Order updateAppointmentDetails(long orderId, Map<String, Object> updateMap, User user) throws Exception;

	Boolean updateReportDeliveryOption(long orderId, Order order, Map<String, Object> map);
	
	List<OrderPatientPrescription> getOrderPrescriptions(long orderId);
	/**
	 * 
	 * @param user
	 * @param orderId
	 * @return
	 * @throws InvocationTargetException 
	 * @throws IllegalAccessException 
	 */
	Boolean splitOrderBasedOnFacility(User user, long orderId) throws IllegalAccessException, InvocationTargetException;

	/**
	 * 
	 * @param user
	 * @param orderId
	 * @param childOrderIds
	 * @return
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	Order mergeOrderBasedOnFacility(User user, long orderId, List<Long> childOrderIds) throws IllegalAccessException, InvocationTargetException;

	/**
	 * 
	 * @param parentId
	 * @param childOrderId
	 * @param facilityId
	 * @param childFacilityId
	 * @param status
	 * @param childOrderStatus
	 * @param deliveryDateFrom
	 * @param deliveryDateTo
	 * @param dispatchDateFrom
	 * @param dispatchDateTo
	 * @param pageable
	 * @return
	 */
	Page<OrderSearchResponse> searchOrder(Long parentId, Long childOrderId, Long childFacilityId, List<String> status, Long deliveryDateFrom, Long deliveryDateTo,
			Long dispatchDateFrom, Long dispatchDateTo, Pageable pageable);

	/**
	 * 
	 * @param parentId
	 * @param childOrderId
	 * @return
	 */
	Order getChildOrder(long parentId, long childOrderId);
	
	Order autoAssignAppointmentOrder(long orderId);
	
	Boolean moveJitForOrder(User user, long orderId, List<UpdateOrderObject> updateOrderObjects) throws Exception;

	OrderPrice getChildOrderPrice(OrderPrice orderPrice, double ratio, Boolean isParentOrder);
	
	List<Long> isEligibleForStatusChange(String courierType);

	/**
	 * 
	 * @param order
	 * @param user
	 * @return
	 * @throws Exception
	 */
	Order addPosOrder(Order order, User user) throws Exception;

	Map<String, Object> getOrdersDetailByCustomerId(long customerId);

	Order moveToFacility(long orderId, Long spokeId);

	Order updateOrder(User user, long orderId, List<UpdateOrderObject> updateOrderObjects, String task, boolean isFinalPrice, boolean isInvoiced) throws Exception;

	Order mergePickedOrders(User user, long customerId, List<Long> orderIds);

	Order addBasicOrder(User user, String referenceId, String source);


}
