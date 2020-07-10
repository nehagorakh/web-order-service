package in.lifcare.order.microservice.payment.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;

import in.lifcare.core.constant.APIEndPoint;
import in.lifcare.core.model.WalletTransactionInfo;
import in.lifcare.core.response.model.Response;
import in.lifcare.core.util.MicroserviceClient;
import in.lifcare.order.microservice.payment.model.OrderPaymentGatewayVerifyRequest;
import in.lifcare.order.microservice.payment.model.OrderPaymentInitiateRequest;
import in.lifcare.order.microservice.payment.model.OrderWalletPaymentRequest;
import in.lifcare.order.microservice.payment.model.PaymentChannelData;
import in.lifcare.order.microservice.payment.model.PaymentChannelDataOption;
import in.lifcare.order.microservice.payment.model.PaymentGatewayData;
import in.lifcare.order.microservice.payment.service.PaymentService;
import in.lifcare.order.model.Order;

import static com.netflix.hystrix.contrib.javanica.conf.HystrixPropertiesManager.EXECUTION_ISOLATION_THREAD_TIMEOUT_IN_MILLISECONDS;
//import static com.netflix.hystrix.contrib.javanica.conf.HystrixPropertiesManager.EXECUTION_ISOLATION_THREAD_INTERRUPT_ON_TIMEOUT;
//import static com.netflix.hystrix.contrib.javanica.conf.HystrixPropertiesManager.EXECUTION_TIMEOUT_ENABLED;

@SuppressWarnings("unchecked")
@Service
public class PaymentServiceImpl implements PaymentService {

	@Override
	@HystrixCommand(commandProperties = { @HystrixProperty(name = EXECUTION_ISOLATION_THREAD_TIMEOUT_IN_MILLISECONDS, value = "5000") })
	public HashMap<String, Object> getWalletDetails(Long customerId, double totalSalePrice, double couponCashbackEligibleAmount) throws Exception {
		if (customerId == null) {
			throw new IllegalArgumentException("Invalid customer id specified");
		}
		HashMap<String, Object> walletInfo = new HashMap<String, Object>();
		try {
			Response<?> response = microserviceClient.getForObject(APIEndPoint.PAYMENT_SERVICE + "/wallet-detail/customer/"+ customerId +
					"?amount=" + totalSalePrice + "&cashback_applicable_amount=" + couponCashbackEligibleAmount, Response.class);
			if ( response != null && response.getPayload() != null) {
				walletInfo = (HashMap<String, Object>) response.getPayload();
			}
		} catch (Exception e) {
			throw e;
		}
		return walletInfo;
	}
	
	@Override
	@HystrixCommand(fallbackMethod = "intiateCODOrderPayment", commandProperties = { @HystrixProperty(name = EXECUTION_ISOLATION_THREAD_TIMEOUT_IN_MILLISECONDS, value = "5000") })
	public PaymentGatewayData initiateOrderPayment(OrderPaymentInitiateRequest orderPaymentInitiateRequest) {
		if (orderPaymentInitiateRequest == null) {
			throw new IllegalArgumentException("Invalid order payment request specified");
		}
		PaymentGatewayData paymentGatewayData = null;
		try {
			Response<?> response = microserviceClient.postForObject(APIEndPoint.PAYMENT_SERVICE + "/order", orderPaymentInitiateRequest, Response.class);
			if ( response != null && response.getPayload() != null) {
				paymentGatewayData = (PaymentGatewayData) response.populatePayloadUsingJson(PaymentGatewayData.class);
			}
		} catch (Exception e) {
			LOGGER.error("Error while initiating payment : {}", e);
			if( Order.ORDER_TYPE.COD.equalsIgnoreCase(orderPaymentInitiateRequest.getPaymentMethod()) ) {
				return intiateCODOrderPayment(orderPaymentInitiateRequest);
			}
			throw e;
		}
		return paymentGatewayData;
	}

	public PaymentGatewayData intiateCODOrderPayment(OrderPaymentInitiateRequest orderPaymentInitiateRequest) {
		PaymentGatewayData paymentGatewayData = null;
		if( Order.ORDER_TYPE.COD.equalsIgnoreCase(orderPaymentInitiateRequest.getPaymentMethod()) ) {
			paymentGatewayData = new PaymentGatewayData();
			paymentGatewayData.setName("COD");
			paymentGatewayData.setMethod("COD");
			paymentGatewayData.setOrderId(orderPaymentInitiateRequest.getOrderId());
			paymentGatewayData.setAmount(0);
		}
		return paymentGatewayData;
	}
	
	@Override
	@HystrixCommand(fallbackMethod = "getDefaultPaymentChannelList", commandProperties = { @HystrixProperty(name = EXECUTION_ISOLATION_THREAD_TIMEOUT_IN_MILLISECONDS, value = "5000") })
	public List<PaymentChannelData> getPaymentChannels(boolean isCod) {
		List<PaymentChannelData> paymentChannelDataList = new ArrayList<PaymentChannelData>();
		try {
			Response<?> response = microserviceClient.getForObject(APIEndPoint.PAYMENT_SERVICE + "/channels?is-cod=" + isCod, Response.class);
			if ( response != null && response.getPayload() != null) {
				paymentChannelDataList = mapper.readValue(mapper.writeValueAsString(response.getPayload()), mapper.getTypeFactory().constructCollectionType(List.class, PaymentChannelData.class));
			}
		} catch (Exception e) {
			LOGGER.error("Error in getting payment channels : {}", e.getMessage());
		}
		if( paymentChannelDataList == null || paymentChannelDataList.isEmpty() ) {
			paymentChannelDataList = getDefaultPaymentChannelList(isCod);
		}
		return paymentChannelDataList;
	}

	public List<PaymentChannelData> getDefaultPaymentChannelList(boolean isCod) {
		List<PaymentChannelData> paymentChannelList = new ArrayList<PaymentChannelData>();
		if( isCod  ) {
			PaymentChannelData paymentChannelData = new PaymentChannelData();
			paymentChannelData.setName("Cash On Delivery");
			paymentChannelData.setMethod("COD");
			paymentChannelData.setDescription("");
			paymentChannelData.setOptions(new ArrayList<PaymentChannelDataOption>());
			paymentChannelList.add(paymentChannelData);
		}
		return paymentChannelList;
	}
	
	@Override
	@HystrixCommand(commandProperties = { @HystrixProperty(name = EXECUTION_ISOLATION_THREAD_TIMEOUT_IN_MILLISECONDS, value = "5000") })
	public PaymentGatewayData verifyOrderPayment(long orderId, OrderPaymentGatewayVerifyRequest orderPaymentGatewayVerifyRequest) throws Exception  {
		if( orderId <= 0 ) {
			throw new IllegalArgumentException("Invalid order id specified");
		}
		if( orderPaymentGatewayVerifyRequest == null ) {
			throw new IllegalArgumentException("Invalid orderPaymentGatewayVerifyRequest specified");
		}
 		if (StringUtils.isBlank(orderPaymentGatewayVerifyRequest.getGatewayName())) {
			throw new IllegalArgumentException("Invalid payment gateway specified in orderPaymentGatewayVerifyRequest");
		}
 		if (orderPaymentGatewayVerifyRequest.getGatewayData() == null || orderPaymentGatewayVerifyRequest.getGatewayData().isEmpty()) {
			throw new IllegalArgumentException("No payment gateway data specified for verification");
		}
		PaymentGatewayData paymentGatewayData = null;
		try {
			Response<?> response = microserviceClient.postForObject(APIEndPoint.PAYMENT_SERVICE + "/order/" + orderId + "/verify", orderPaymentGatewayVerifyRequest, Response.class);
			if ( response != null && response.getPayload() != null) {
				paymentGatewayData = (PaymentGatewayData) response.populatePayloadUsingJson(PaymentGatewayData.class);
			}
		} catch (Exception e) {
			throw e;
		}
		return paymentGatewayData;
	}

	@Override
	@HystrixCommand(commandProperties = { @HystrixProperty(name = EXECUTION_ISOLATION_THREAD_TIMEOUT_IN_MILLISECONDS, value = "5000") })
	public WalletTransactionInfo processOrderWalletTransaction(OrderWalletPaymentRequest orderWalletPaymentRequest) throws Exception {
		if( orderWalletPaymentRequest == null ) {
			throw new IllegalArgumentException("Invalid order wallet payment request specified");
		}
		WalletTransactionInfo walletTransactionInfo = null;
		try {
			Response<?> response = microserviceClient.postForObject(APIEndPoint.PAYMENT_SERVICE + "/order/wallet-transaction", orderWalletPaymentRequest, Response.class);
			if ( response != null && response.getPayload() != null) {
				walletTransactionInfo = (WalletTransactionInfo) response.populatePayloadUsingJson(WalletTransactionInfo.class);
			}
		} catch (Exception e) {
			throw e;
		}
		return walletTransactionInfo;
	}
	
	@Override
	@HystrixCommand(commandProperties = { @HystrixProperty(name = EXECUTION_ISOLATION_THREAD_TIMEOUT_IN_MILLISECONDS, value = "5000") })
	public List<PaymentGatewayData> confirmOrderPayment(long orderId, boolean verifyCod) throws Exception {
		List<PaymentGatewayData> paymentGatewayDataList = null;
		try {
			Response<?> response = microserviceClient.postForObject(APIEndPoint.PAYMENT_SERVICE + "/order/" + orderId + "/confirm?verify-cod=" + verifyCod, null, Response.class);
			if ( response != null && response.getPayload() != null) {
				paymentGatewayDataList = mapper.readValue(mapper.writeValueAsString(response.getPayload()), mapper.getTypeFactory().constructCollectionType(List.class, PaymentGatewayData.class));
			}
		} catch (Exception e) {
			LOGGER.error("Error in payment confirmation for order id : " + orderId + " : Exception : " + e.getMessage());
		}
		return paymentGatewayDataList;
	}
	
	private static final Logger LOGGER = LoggerFactory.getLogger(PaymentServiceImpl.class);
	
	private ObjectMapper mapper = new ObjectMapper();
	
	@Autowired
	private MicroserviceClient<Response> microserviceClient;

}
