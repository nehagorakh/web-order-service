package in.lifcare.order.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.lifcare.core.constant.APIEndPoint;
import in.lifcare.core.constant.WalletEvent;
import in.lifcare.core.exception.BadRequestException;
import in.lifcare.core.model.Customer;
import in.lifcare.core.model.EventData;
import in.lifcare.core.model.OrderInfo;
import in.lifcare.core.model.WalletInfo;
import in.lifcare.core.model.WalletTransactionInfo;
import in.lifcare.core.response.model.Response;
import in.lifcare.core.util.MicroserviceClient;
import in.lifcare.order.model.Order;
import in.lifcare.producer.exception.TopicNotFound;
import in.lifcare.producer.kafka.KafkaProducer;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * @author rahul
 *
 */
@Service
public class WalletService {

	public void creditRefereeWalletEvent(Order order, String transactionId, String source) {
		if (order == null) {
			throw new IllegalArgumentException("customerId can't be empty");
		}
		if (StringUtils.isNotBlank(transactionId)) {
			transactionId = source + "-" + transactionId;
		}
		try {
			@SuppressWarnings({ "unchecked" })
			/*Response<Customer> response = restTemplate
					.getForObject(APIEndPoint.ACCOUNT_SERVICE + "/customer/" + order.getCustomerId(), Response.class);*/
			Response<Customer> response = microServiceClient.getForObject(APIEndPoint.ACCOUNT_SERVICE + "/customer/" + order.getCustomerId(), Response.class);
			Customer customer = (Customer) response.populatePayloadUsingJson(Customer.class);
			if (null != customer && null != customer.getReferenceCode() && null != customer.getReferenceEntityId()
					&& Customer.REFERENCE_BY.CUSTOMER.equalsIgnoreCase(customer.getReferenceBy())) {
				int carePoint = (int) Math.ceil((order.getTotalSalePrice()+order.getRedeemedCarePoints()) * 0.02);
				creditRefereeWallet(carePoint, 0, source, transactionId, customer, order);
			} else {
				LOGGER.info("Not Eligible for credit wllet");
			}
		} catch (Exception e) {
			LOGGER.error("Exception occurred in producing wallet creation hit");
		}
	}

	private void creditRefereeWallet(int carePoint, float money, String source, String transactionId, Customer customer,
			Order order) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		WalletInfo walletInfo = new WalletInfo();
		walletInfo.setCarePoint(carePoint);
		walletInfo.setCustomerId(customer.getReferenceEntityId());
		walletInfo.setMobile(customer.getReferenceMobile());
		walletInfo.setTransactionId(transactionId);
		walletInfo.setSource(source);
		walletInfo.setMoney(money);
		OrderInfo orderInfo = new OrderInfo();
		orderInfo.setCustomerId(order.getCustomerId());
		orderInfo.setId(order.getId());
		orderInfo.setTotalMrp(order.getTotalMrp());
		orderInfo.setRedeemedCarePoint(order.getRedeemedCarePoints());
		orderInfo.setDoctorCallback(order.isDoctorCallback());
		orderInfo.setStatus(order.getStatus());
		orderInfo.setState(order.getState());
		orderInfo.setPaymentType(order.getOrderType());
		walletInfo.setReferenceData(mapper.writeValueAsString(orderInfo));
		EventData eventData = new EventData();
		Long customerId = customer.getId();
		eventData.setData(walletInfo);
		eventData.setReferenceId(transactionId);
		eventData.setSource(source);
		eventData.setId(customerId.toString());
		eventData.setRequestedAt(new Date());
		eventData.setEventType(WalletEvent.CREDIT);
		kafkaProducer.processMessage(eventData);
	}

	public void creditCustomerWalletReturnCancellationEvent(Order order, String transactionId, String source) {
		if (order == null) {
			throw new IllegalArgumentException("customerId can't be empty");
		}
		if (StringUtils.isNotBlank(transactionId)) {
			transactionId = source + "-" + transactionId;
		}
		try {
			int carePoint = order.getRedeemedCarePoints();
			if (carePoint > 0) {
				@SuppressWarnings({ "unchecked" })
				Response<Customer> response = microServiceClient.getForObject(
						APIEndPoint.ACCOUNT_SERVICE + "/customer/" + order.getCustomerId(), Response.class);
				Customer customer = (Customer) response.populatePayloadUsingJson(Customer.class);
				creditWallet(carePoint, 0, source, transactionId, customer, order);
			} else {
				LOGGER.info("Not Eligible for credit wllet");
			}
		} catch (Exception e) {
			LOGGER.error("Exception occurred in producing wallet creation hit");
		}
	}

	private void creditWallet(int carePoint, float money, String source, String transactionId, Customer customer,
			Order order) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		WalletInfo walletInfo = new WalletInfo();
		walletInfo.setCarePoint(carePoint);
		walletInfo.setCustomerId(customer.getId());
		walletInfo.setEmail(customer.getEmail());
		walletInfo.setFirstName(customer.getFirstName());
		walletInfo.setLastName(customer.getLastName());
		walletInfo.setMobile(customer.getMobile());
		walletInfo.setTransactionId(transactionId);
		walletInfo.setSource(source);
		walletInfo.setMoney(0);
		OrderInfo orderInfo = new OrderInfo();
		orderInfo.setCustomerId(order.getCustomerId());
		orderInfo.setId(order.getId());
		orderInfo.setTotalMrp(order.getTotalMrp());
		orderInfo.setDoctorCallback(order.isDoctorCallback());
		orderInfo.setRedeemedCarePoint(order.getRedeemedCarePoints());
		orderInfo.setStatus(order.getStatus());
		orderInfo.setState(order.getState());
		orderInfo.setPaymentType(order.getOrderType());
		walletInfo.setReferenceData(mapper.writeValueAsString(orderInfo));
		EventData eventData = new EventData();
		Long customerId = customer.getId();
		eventData.setData(walletInfo);
		eventData.setReferenceId(transactionId);
		eventData.setSource(source);
		eventData.setId(customerId.toString());
		eventData.setRequestedAt(new Date());
		eventData.setEventType(WalletEvent.CREDIT);
		kafkaProducer.processMessage(eventData);
	}

	public void debitWallet(Order order, String transactionId, String source, int redeemedCarePoints, float redeemedCash) {
		if (null == order) {
			throw new BadRequestException("Order not found");
		}
		if (StringUtils.isNotBlank(transactionId)) {
			transactionId = source + "-" + transactionId;
		}
		try {
			ObjectMapper mapper = new ObjectMapper();
			WalletTransactionInfo walletTransactionInfo = new WalletTransactionInfo();
			walletTransactionInfo.setCarePoint(redeemedCarePoints);
			walletTransactionInfo.setMoney(redeemedCash);
			walletTransactionInfo.setCashType(WalletTransactionInfo.CASH_TYPE.BONUS);
			walletTransactionInfo.setTransactionType(WalletTransactionInfo.TRANSACTION_TYPE.DEBIT);
			walletTransactionInfo.setCustomerId(order.getCustomerId());
			walletTransactionInfo.setSource(source);
			walletTransactionInfo.setComment(null);
			Long orderId = order.getId();
			walletTransactionInfo.setDisplayComment("New Order Id: "+orderId.toString());
			OrderInfo orderInfo = new OrderInfo();
			orderInfo.setCustomerId(order.getCustomerId());
			orderInfo.setId(order.getId());
			orderInfo.setTotalMrp(order.getTotalMrp());
			orderInfo.setDoctorCallback(order.isDoctorCallback());
			orderInfo.setRedeemedCarePoint(order.getRedeemedCarePoints());
			orderInfo.setRedeemedCash(order.getRedeemedCash());
			orderInfo.setStatus(order.getStatus());
			orderInfo.setState(order.getState());
			orderInfo.setPaymentType(order.getOrderType());
			walletTransactionInfo.setReferenceData(mapper.writeValueAsString(orderInfo));
			walletTransactionInfo.setReferenceId(transactionId);
			/*walletTransactionInfo = microserviceClient.postForObject(
					APIEndPoint.WALLET_SERVICE + "/" + order.getCustomerId() + "/transaction/debit",
					walletTransactionInfo, WalletTransactionInfo.class);*/
			microServiceClient.postForObject(
					APIEndPoint.WALLET_SERVICE + "/" + order.getCustomerId() + "/transaction/debit",
					walletTransactionInfo, Response.class);
		} catch (Exception e) {
			LOGGER.error("Exception occurred in producing wallet creation hit");
		}
	}
	
	public boolean creditWallet(Order order, String transactionId, String source, String cashType,
			int redeemedCarePoints, float cash,float couponCashback) {
		if (null == order) {
			throw new BadRequestException("Order not found");
		}
		if (StringUtils.isNotBlank(transactionId)) {
			transactionId = source + "-" + transactionId;
		}
		try {
			ObjectMapper mapper = new ObjectMapper();
			WalletTransactionInfo walletTransactionInfo = new WalletTransactionInfo();
			if (WalletTransactionInfo.CASH_TYPE.BONUS.equalsIgnoreCase(cashType)) {
				walletTransactionInfo.setCarePoint(redeemedCarePoints);
			} else if (WalletTransactionInfo.CASH_TYPE.CASH.equalsIgnoreCase(cashType)) {
				walletTransactionInfo.setMoney(cash);
			} else if (WalletTransactionInfo.CASH_TYPE.COUPON_CASHBACK.equalsIgnoreCase(cashType)) {
				walletTransactionInfo.setCouponCashback(couponCashback);
			}
			walletTransactionInfo.setCashType(cashType);
			walletTransactionInfo.setTransactionType(WalletTransactionInfo.TRANSACTION_TYPE.CREDIT);
			walletTransactionInfo.setCustomerId(order.getCustomerId());
			walletTransactionInfo.setSource(source);
			walletTransactionInfo.setComment(null);
			Long orderId = order.getId();
			walletTransactionInfo.setDisplayComment("New Order Id: " + orderId.toString());
			OrderInfo orderInfo = new OrderInfo();
			orderInfo.setCustomerId(order.getCustomerId());
			orderInfo.setId(order.getId());
			orderInfo.setTotalMrp(order.getTotalMrp());
			orderInfo.setDoctorCallback(order.isDoctorCallback());
			orderInfo.setRedeemedCarePoint(redeemedCarePoints);
			orderInfo.setRedeemedCouponCashback(couponCashback);
			orderInfo.setStatus(order.getStatus());
			orderInfo.setState(order.getState());
			orderInfo.setPaymentType(order.getOrderType());
			walletTransactionInfo.setReferenceData(mapper.writeValueAsString(orderInfo));
			walletTransactionInfo.setReferenceId(transactionId);
			microServiceClient.postForObject(
					APIEndPoint.WALLET_SERVICE + "/" + order.getCustomerId() + "/transaction/credit",
					walletTransactionInfo, Response.class);
			return true;
		} catch (Exception e) {
			LOGGER.error("Exception occurred in producing wallet credit");
			return false;
		}
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(WalletService.class);

	@Autowired
	private KafkaProducer kafkaProducer;
	
	@SuppressWarnings("rawtypes")
	@Autowired 
	private MicroserviceClient<Response> microServiceClient;

}
