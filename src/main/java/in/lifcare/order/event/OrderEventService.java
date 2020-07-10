package in.lifcare.order.event;

import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.dozer.DozerBeanMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import in.lifcare.core.constant.APIEndPoint;
import in.lifcare.core.constant.OrderEvent;
import in.lifcare.core.model.Customer;
import in.lifcare.core.model.EventData;
import in.lifcare.core.model.OrderInfo;
import in.lifcare.core.model.ShippingAddressInfo;
import in.lifcare.core.model.User;
import in.lifcare.core.response.model.Response;
import in.lifcare.core.util.MicroserviceClient;
import in.lifcare.order.model.Order;
import in.lifcare.order.model.ShippingAddress;
import in.lifcare.producer.exception.TopicNotFound;
import in.lifcare.producer.kafka.KafkaProducer;

/**
 * @author rahul
 *
 */
@Service
public class OrderEventService {

	@Async
	public void cancelOrderEvent(Order order, String source, User user) {
		if (order == null) {
			throw new IllegalArgumentException("order can't be empty");
		}
		String transactionId = null;
		if (StringUtils.isNotBlank(order.getDisplayOrderId())) {
			transactionId = source + "-" + order.getDisplayOrderId();
		}
		try {
			/*Response<Customer> response = restTemplate
					.getForObject(APIEndPoint.ACCOUNT_SERVICE + "/customer/" + order.getCustomerId(), Response.class);*/
			@SuppressWarnings({"rawtypes" })
			Response response = microServiceClient.getForObject(APIEndPoint.ACCOUNT_SERVICE + "/customer/" +order.getCustomerId(), Response.class);
			@SuppressWarnings({ "unchecked" })
			Customer customer = (Customer) response.populatePayloadUsingJson(Customer.class);
			if (null != customer) {
				cancelOrder(source, transactionId, customer, order, user);
			} else {
				LOGGER.info("Not Eligible for credit wllet");
			}
		} catch (Exception e) {
			LOGGER.error("Exception occurred in producing wallet creation hit");
		}
	}

	@Async
	public void orderEvent(Order order, String transactionId, String requestType, User user) {
		if (order == null) {
			throw new IllegalArgumentException("customerId can't be empty");
		}
		if (StringUtils.isNotBlank(transactionId)) {
			transactionId = requestType + "-" + transactionId;
		}
		try {
			OrderInfo orderInfo = dozerMapper.map(order, OrderInfo.class);
			orderInfo.setCustomerId(order.getCustomerId());
			orderInfo.setParentId(order.getParentId());
			orderInfo.setId(order.getId());
			orderInfo.setTotalMrp(order.getTotalMrp());
			orderInfo.setRedeemedCarePoint(order.getRedeemedCarePoints());
			orderInfo.setRedeemedCash(order.getRedeemedCash());
			orderInfo.setPaymentMethod(order.getPaymentMethod());
			orderInfo.setPaymentSubMethod(order.getPaymentSubMethod());
			orderInfo.setTotalPayableAmount(order.getTotalPayableAmount());
			orderInfo.setStatus(order.getStatus());
			orderInfo.setComment(order.getStatusComment());
			orderInfo.setManualHold(order.isManualHold());
			orderInfo.setSystemHold(order.isManualHold());
			orderInfo.setVersionId(null);
			orderInfo.setState(order.getState());
			orderInfo.setPaymentType(order.getOrderType());
			orderInfo.setOrderType(order.getOrderType());
			orderInfo.setUser(user);
			orderInfo.setPatientId(order.getPatientId());
			orderInfo.setDoctorCallback(order.isDoctorCallback());
			orderInfo.setTotalDiscount(order.getTotalDiscount());
			orderInfo.setTotalSalePrice(order.getTotalSalePrice());
			orderInfo.setCouponCode(order.getCouponCode());
			orderInfo.setTrackingNumber(order.getTrackingNumber());
			orderInfo.setCourierType(order.getCourierType());
			orderInfo.setDeliveryOption(order.getDeliveryOption());
			orderInfo.setServiceType(order.getServiceType());
			orderInfo.setDeliveryOptionChangeReason(order.getDeliveryOptionChangeReason());
			orderInfo.setServiceTypeChangeReason(order.getServiceTypeChangeReason());
			orderInfo.setPreferredDeliveryOption(order.getPreferredDeliveryOption());
			orderInfo.setPreferredServiceType(order.getPreferredServiceType());
			orderInfo.setUrgentDeliveryCharge(order.getUrgentDeliveryCharge());
			orderInfo.setRedeemedCash(order.getRedeemedCash());
			orderInfo.setFacilityName(order.getFacilityName());
			orderInfo.setBusinessChannel(order.getBusinessChannel());
			orderInfo.setBusinessType(order.getBusinessType());
			orderInfo.setCategory(order.getCategory());
			orderInfo.setAppointmentDate(order.getAppointmentDate());
			orderInfo.setAppointmentId(order.getAppointmentId());
			orderInfo.setAppointmentSlot(order.getAppointmentSlot());
			orderInfo.setProcurementType(order.getProcurementType());
			orderInfo.setFacilityName(order.getFacilityName());
			orderInfo.setNextRefillDate(order.getNextRefillDate());
			orderInfo.setNextRefillDay(order.getNextRefillDay());
			orderInfo.setMembershipAdded(order.isMembershipAdded());
			orderInfo.setReportHardCopyRequired(order.isReportHardCopyRequired());

			EventData eventData = new EventData();
			eventData.setData(orderInfo);
			eventData.setReferenceId(transactionId);
			eventData.setSource(requestType);
			eventData.setId(String.valueOf(order.getId()));
			eventData.setRequestedAt(new Date());
			eventData.setEventType(requestType);
			
			kafkaProducer.processMessage(eventData);
		} catch (Exception e) {
			LOGGER.error("Exception occurred in producing wallet creation hit {}", e);
		}
	}

	@Async
	private void cancelOrder(String source, String transactionId, Customer customer, Order order, User user)
			throws TopicNotFound, Exception {
		OrderInfo orderInfo = dozerMapper.map(order, OrderInfo.class);
		orderInfo.setCustomerId(order.getCustomerId());
		orderInfo.setParentId(order.getParentId());
		orderInfo.setFirstName(customer.getFirstName());
		orderInfo.setLastName(customer.getLastName());
		orderInfo.setId(order.getId());
		orderInfo.setTotalMrp(order.getTotalMrp());
		orderInfo.setRedeemedCarePoint(order.getRedeemedCarePoints());
		orderInfo.setRedeemedCash(order.getRedeemedCash());
		orderInfo.setRedeemedCouponCashback(order.getRedeemedCouponCashback());
		orderInfo.setPaymentMethod(order.getPaymentMethod());
		orderInfo.setPaymentSubMethod(order.getPaymentSubMethod());
		orderInfo.setTotalPayableAmount(order.getTotalPayableAmount());
		orderInfo.setStatus(order.getStatus());
		orderInfo.setState(order.getState());
		orderInfo.setPaymentType(order.getOrderType());
		orderInfo.setOrderType(order.getOrderType());
		orderInfo.setComment(order.getStatusComment());
		orderInfo.setUser(user);
		orderInfo.setPatientId(order.getPatientId());
		orderInfo.setDoctorCallback(order.isDoctorCallback());
		orderInfo.setTotalDiscount(order.getTotalDiscount());
		orderInfo.setTotalSalePrice(order.getTotalSalePrice());
		orderInfo.setCouponCode(order.getCouponCode());
		orderInfo.setDeliveryOption(order.getDeliveryOption());
		orderInfo.setServiceType(order.getServiceType());
		orderInfo.setDeliveryOptionChangeReason(order.getDeliveryOptionChangeReason());
		orderInfo.setServiceTypeChangeReason(order.getServiceTypeChangeReason());
		orderInfo.setPreferredDeliveryOption(order.getPreferredDeliveryOption());
		orderInfo.setPreferredServiceType(order.getPreferredServiceType());
		orderInfo.setUrgentDeliveryCharge(order.getUrgentDeliveryCharge());
		orderInfo.setRedeemedCash(order.getRedeemedCash());
		orderInfo.setFacilityName(order.getFacilityName());
		orderInfo.setBusinessChannel(order.getBusinessChannel());
		orderInfo.setBusinessType(order.getBusinessType());
		orderInfo.setCategory(order.getCategory());
		orderInfo.setAppointmentDate(order.getAppointmentDate());
		orderInfo.setAppointmentId(order.getAppointmentId());
		orderInfo.setAppointmentSlot(order.getAppointmentSlot());
		orderInfo.setProcurementType(order.getProcurementType());
		orderInfo.setFacilityName(order.getFacilityName());
		orderInfo.setNextRefillDate(order.getNextRefillDate());
		orderInfo.setNextRefillDay(order.getNextRefillDay());
		orderInfo.setMembershipAdded(order.isMembershipAdded());
		orderInfo.setReportHardCopyRequired(order.isReportHardCopyRequired());

		EventData eventData = new EventData();
		eventData.setData(orderInfo);
		eventData.setReferenceId(transactionId);
		eventData.setSource(source);
		eventData.setId(String.valueOf(order.getId()));
		eventData.setRequestedAt(new Date());
		eventData.setEventType(OrderEvent.ORDER_CANCELLED_CANCELLED);
		kafkaProducer.processMessage(eventData);
	}

	@Async
	public void markOrderVerified(Order order, String transactionId, String type, User user, int i) throws TopicNotFound, Exception {
		EventData eventData = new EventData();
		OrderInfo orderInfo = dozerMapper.map(order, OrderInfo.class);
		orderInfo.setCustomerId(order.getCustomerId());
		orderInfo.setId(order.getId());
		orderInfo.setParentId(order.getParentId());
		orderInfo.setTotalMrp(order.getTotalMrp());
		orderInfo.setRedeemedCarePoint(order.getRedeemedCarePoints());
		orderInfo.setRedeemedCash(order.getRedeemedCash());
		orderInfo.setPaymentMethod(order.getPaymentMethod());
		orderInfo.setPaymentSubMethod(order.getPaymentSubMethod());
		orderInfo.setTotalPayableAmount(order.getTotalPayableAmount());
		orderInfo.setStatus(order.getStatus());
		orderInfo.setState(order.getState());
		orderInfo.setPaymentType(order.getOrderType());
		orderInfo.setOrderType(order.getOrderType());
		orderInfo.setDoctorCallback(order.isDoctorCallback());
		orderInfo.setComment(order.getStatusComment());
		orderInfo.setScore(order.getScore());
		orderInfo.setUser(user);
		orderInfo.setCouponCode(order.getCouponCode());
		orderInfo.setDeliveryOption(order.getDeliveryOption());
		orderInfo.setServiceType(order.getServiceType());
		orderInfo.setDeliveryOptionChangeReason(order.getDeliveryOptionChangeReason());
		orderInfo.setServiceTypeChangeReason(order.getServiceTypeChangeReason());
		orderInfo.setPreferredDeliveryOption(order.getPreferredDeliveryOption());
		orderInfo.setPreferredServiceType(order.getPreferredServiceType());
		orderInfo.setUrgentDeliveryCharge(order.getUrgentDeliveryCharge());
		orderInfo.setRedeemedCash(order.getRedeemedCash());
		orderInfo.setFacilityName(order.getFacilityName());
		orderInfo.setBusinessChannel(order.getBusinessChannel());
		orderInfo.setBusinessType(order.getBusinessType());
		orderInfo.setCategory(order.getCategory());
		orderInfo.setAppointmentDate(order.getAppointmentDate());
		orderInfo.setAppointmentId(order.getAppointmentId());
		orderInfo.setAppointmentSlot(order.getAppointmentSlot());
		orderInfo.setProcurementType(order.getProcurementType());
		orderInfo.setFacilityName(order.getFacilityName());
		orderInfo.setNextRefillDate(order.getNextRefillDate());
		orderInfo.setNextRefillDay(order.getNextRefillDay());
		orderInfo.setMembershipAdded(order.isMembershipAdded());
		orderInfo.setReportHardCopyRequired(order.isReportHardCopyRequired());

		Long orderId = order.getId();
		eventData.setData(orderInfo);
		eventData.setReferenceId(transactionId);
		eventData.setId(orderId.toString());
		eventData.setRequestedAt(new Date());
		eventData.setEventType(type);
		eventData.setSource(order.getSource());
		kafkaProducer.processMessage(eventData);
	}

	@Async
	public void updateOrderEvent(Order order, String transactionId, String type, User user, long facilityId, String comment, Double urgentDeliveryCharge) throws TopicNotFound, Exception {
		EventData eventData = new EventData();
		OrderInfo orderInfo = dozerMapper.map(order, OrderInfo.class);
		orderInfo.setCustomerId(order.getCustomerId());
		orderInfo.setParentId(order.getParentId());
		orderInfo.setPatientId(order.getPatientId());
		orderInfo.setId(order.getId());
		orderInfo.setTotalMrp(order.getTotalMrp());
		orderInfo.setRedeemedCarePoint(order.getRedeemedCarePoints());
		orderInfo.setRedeemedCash(order.getRedeemedCash());
		orderInfo.setPaymentMethod(order.getPaymentMethod());
		orderInfo.setPaymentSubMethod(order.getPaymentSubMethod());
		orderInfo.setTotalPayableAmount(order.getTotalPayableAmount());
		orderInfo.setStatus(order.getStatus());
		orderInfo.setState(order.getState());
		orderInfo.setPaymentType(order.getOrderType());
		orderInfo.setOrderType(order.getOrderType());
		orderInfo.setScore(order.getScore());
		orderInfo.setUser(user);
		if (facilityId > 0) {
			orderInfo.setFacilityId(facilityId);
		} else {
			orderInfo.setFacilityId(order.getFacilityCode());
		}
		LOGGER.info("Facilty Id changed for {} , new facility id is {}", orderInfo.getId() ,orderInfo.getFacilityId());
		orderInfo.setPatientId(order.getPatientId());
		orderInfo.setTotalDiscount(order.getTotalDiscount());
		orderInfo.setDoctorCallback(order.isDoctorCallback());
		orderInfo.setTotalSalePrice(order.getTotalSalePrice());
		orderInfo.setCouponCode(order.getCouponCode());
		orderInfo.setOffsetScore(order.getOffsetScore());
		orderInfo.setComment(comment);
		orderInfo.setReason(order.getStatusComment());
		orderInfo.setRepeat(order.isRepeat());
		orderInfo.setTrackingNumber(order.getTrackingNumber());
		orderInfo.setCourierType(order.getCourierType());
		orderInfo.setDeliveryOption(order.getDeliveryOption());
		orderInfo.setServiceType(order.getServiceType());
		orderInfo.setDeliveryOptionChangeReason(order.getDeliveryOptionChangeReason());
		orderInfo.setServiceTypeChangeReason(order.getServiceTypeChangeReason());
		orderInfo.setPreferredDeliveryOption(order.getPreferredDeliveryOption());
		orderInfo.setPreferredServiceType(order.getPreferredServiceType());
		orderInfo.setUrgentDeliveryCharge(order.getUrgentDeliveryCharge());
		orderInfo.setManualDeliveryOptionChangeReason(order.getManualDeliveryOptionChangeReason());
		orderInfo.setManualServiceTypeChangeReason(order.getManualServiceTypeChangeReason());
		orderInfo.setRedeemedCash(order.getRedeemedCash());
		orderInfo.setManualCouponCode(order.getManualCouponCode());
		orderInfo.setFacilityName(order.getFacilityName());
		orderInfo.setBusinessChannel(order.getBusinessChannel());
		orderInfo.setBusinessType(order.getBusinessType());
		orderInfo.setCreatedAt(order.getCreatedAt());
		orderInfo.setSource(StringUtils.isNotBlank(order.getSource())?order.getSource():"");
		orderInfo.setDelayBy(order.getDelayBy());
		orderInfo.setNextRefillDate(order.getNextRefillDate());
		orderInfo.setAppointmentDate(order.getAppointmentDate());
		orderInfo.setAppointmentId(order.getAppointmentId());
		orderInfo.setAppointmentSlot(order.getAppointmentSlot());
		if(urgentDeliveryCharge != null){
			orderInfo.setUrgentDeliveryCharge(urgentDeliveryCharge);
		} else{
			orderInfo.setUrgentDeliveryCharge(order.getUrgentDeliveryCharge());
		}
		orderInfo.setCategory(order.getCategory());
		orderInfo.setProcurementType(order.getProcurementType());
		orderInfo.setFacilityName(order.getFacilityName());
		orderInfo.setNextRefillDate(order.getNextRefillDate());
		orderInfo.setNextRefillDay(order.getNextRefillDay());
		orderInfo.setMembershipAdded(order.isMembershipAdded());
		orderInfo.setReportHardCopyRequired(order.isReportHardCopyRequired());

		Long orderId = order.getId();
		eventData.setData(orderInfo);
		eventData.setReferenceId(transactionId);
		eventData.setId(orderId.toString());
		eventData.setRequestedAt(new Date());
		eventData.setEventType(type);
		eventData.setSource(StringUtils.isNotBlank(order.getSource())?order.getSource():"");
		kafkaProducer.processMessage(eventData);
	}
	

	@Async
	public void updateOrderShippingEvent(String transactionId, ShippingAddress shippingAddress, String type, User user) throws TopicNotFound, Exception {
		ShippingAddressInfo shippingAddressInfo = new ShippingAddressInfo();
		shippingAddressInfo.setId(shippingAddress.getId());
		shippingAddressInfo.setCity(shippingAddress.getCity());
		shippingAddressInfo.setCountry(shippingAddress.getCountry());
		shippingAddressInfo.setFirstName(shippingAddress.getFirstName());
		shippingAddressInfo.setLastName(shippingAddress.getLastName());
		shippingAddressInfo.setMobile(shippingAddress.getMobile());
		shippingAddressInfo.setOrderId(shippingAddress.getOrderId());
		shippingAddressInfo.setUser(user);
		EventData eventData = new EventData();
		Long orderId = shippingAddress.getOrderId();
		eventData.setData(shippingAddressInfo);
		eventData.setReferenceId(transactionId);
		eventData.setId(orderId.toString());
		eventData.setRequestedAt(new Date());
		eventData.setEventType(type);
		kafkaProducer.processMessage(eventData);
	}

	@Async
	public void createOrderEvent(Order order, String transactionId, String type, User user) throws TopicNotFound, Exception {
		EventData eventData = new EventData();
		OrderInfo orderInfo = dozerMapper.map(order, OrderInfo.class);
		orderInfo.setCustomerId(order.getCustomerId());
		orderInfo.setId(order.getId());
		orderInfo.setParentId(order.getParentId());
		orderInfo.setTotalMrp(order.getTotalMrp());
		orderInfo.setRedeemedCarePoint(order.getRedeemedCarePoints());
		orderInfo.setRedeemedCash(order.getRedeemedCash());
		orderInfo.setPaymentMethod(order.getPaymentMethod());
		orderInfo.setPaymentSubMethod(order.getPaymentSubMethod());
		orderInfo.setTotalPayableAmount(order.getTotalPayableAmount());
		orderInfo.setStatus(order.getStatus());
		orderInfo.setState(order.getState());
		orderInfo.setPaymentType(order.getOrderType());
		orderInfo.setOrderType(order.getOrderType());
		orderInfo.setPatientId(order.getPatientId());
		orderInfo.setTotalDiscount(order.getTotalDiscount());
		orderInfo.setTotalSalePrice(order.getTotalSalePrice());
		orderInfo.setUser(user);
		orderInfo.setCouponCode(order.getCouponCode());
		orderInfo.setTrackingNumber(order.getTrackingNumber());
		orderInfo.setCourierType(order.getCourierType());
		orderInfo.setDoctorCallback(order.isDoctorCallback());
		orderInfo.setDeliveryOption(order.getDeliveryOption());
		orderInfo.setServiceType(order.getServiceType());
		orderInfo.setDeliveryOptionChangeReason(order.getDeliveryOptionChangeReason());
		orderInfo.setServiceTypeChangeReason(order.getServiceTypeChangeReason());
		orderInfo.setPreferredDeliveryOption(order.getPreferredDeliveryOption());
		orderInfo.setPreferredServiceType(order.getPreferredServiceType());
		orderInfo.setUrgentDeliveryCharge(order.getUrgentDeliveryCharge());
		orderInfo.setRedeemedCash(order.getRedeemedCash());
		orderInfo.setUrgentDeliveryCharge(order.getUrgentDeliveryCharge());
		orderInfo.setFacilityId(order.getFacilityCode());
		orderInfo.setFacilityName(order.getFacilityName());
		orderInfo.setBusinessChannel(order.getBusinessChannel());
		orderInfo.setBusinessType(order.getBusinessType());
		orderInfo.setSource(StringUtils.isNotBlank(order.getSource())?order.getSource():"");
		orderInfo.setCreatedAt(order.getCreatedAt());
		orderInfo.setCategory(order.getCategory());
		orderInfo.setAppointmentDate(order.getAppointmentDate());
		orderInfo.setAppointmentId(order.getAppointmentId());
		orderInfo.setAppointmentSlot(order.getAppointmentSlot());
		orderInfo.setProcurementType(order.getProcurementType());
		orderInfo.setFacilityName(order.getFacilityName());
		orderInfo.setNextRefillDate(order.getNextRefillDate());
		orderInfo.setNextRefillDay(order.getNextRefillDay());
		orderInfo.setMembershipAdded(order.isMembershipAdded());
		orderInfo.setReportHardCopyRequired(order.isReportHardCopyRequired());
		
		//Set Fulfillable Flag for SPLIT
		orderInfo.setFulfillable(order.isFulfillable());
		
		Long orderId = order.getId();
		eventData.setData(orderInfo);
		eventData.setReferenceId(transactionId);
		eventData.setId(orderId.toString());
		eventData.setRequestedAt(new Date());
		eventData.setEventType(type);
		eventData.setSource(order.getSource());
		eventData.setSource(StringUtils.isNotBlank(order.getSource())?order.getSource():"");
		kafkaProducer.processMessage(eventData);
	}

	@Async
	public void repeatOrderEvent(Long customerId,Long id) throws TopicNotFound, Exception {
		EventData eventData = new EventData();
		OrderInfo orderInfo = new OrderInfo();
		orderInfo.setCustomerId(customerId);
		orderInfo.setId(id);
		eventData.setData(orderInfo);
		eventData.setId(id.toString());
		eventData.setRequestedAt(new Date());
		eventData.setEventType(OrderEvent.REPEAT_ORDER);
		kafkaProducer.processMessage(eventData);
	}

	@Async
	public void paymentFailedPushForCodOrderEvent(Order order) throws TopicNotFound, Exception {
		EventData eventData = new EventData();
		OrderInfo orderInfo = dozerMapper.map(order, OrderInfo.class);
		orderInfo.setCustomerId(order.getCustomerId());
		orderInfo.setId(order.getId());
		orderInfo.setOrderType(order.getOrderType());
		orderInfo.setTotalPayableAmount(order.getTotalPayableAmount());
		orderInfo.setPaymentType(order.getPaymentMethod());
		eventData.setData(orderInfo);
		eventData.setId(String.valueOf(order.getId()));
		eventData.setRequestedAt(new Date());
		eventData.setEventType(OrderEvent.ORDER_PAYMENT_FAILED_PUSH_FOR_COD);
		kafkaProducer.processMessage(eventData);
	}
	
	private static final Logger LOGGER = LoggerFactory.getLogger(WalletService.class);

	private DozerBeanMapper dozerMapper = new DozerBeanMapper();
	
	@Autowired
	private KafkaProducer kafkaProducer;
	
	@SuppressWarnings("rawtypes")
	@Autowired
	private MicroserviceClient<Response> microServiceClient;
}
