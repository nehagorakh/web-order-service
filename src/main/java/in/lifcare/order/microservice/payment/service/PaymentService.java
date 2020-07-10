package in.lifcare.order.microservice.payment.service;

import java.util.HashMap;
import java.util.List;

import in.lifcare.core.model.WalletTransactionInfo;
import in.lifcare.order.microservice.payment.model.OrderPaymentGatewayVerifyRequest;
import in.lifcare.order.microservice.payment.model.OrderPaymentInitiateRequest;
import in.lifcare.order.microservice.payment.model.OrderWalletPaymentRequest;
import in.lifcare.order.microservice.payment.model.PaymentChannelData;
import in.lifcare.order.microservice.payment.model.PaymentGatewayData;

public interface PaymentService {

	HashMap<String, Object> getWalletDetails(Long customerId, double totalSalePrice, double couponCashbackEligibleAmount) throws Exception;

	PaymentGatewayData initiateOrderPayment(OrderPaymentInitiateRequest orderPaymentInitiateRequest);

	List<PaymentChannelData> getPaymentChannels(boolean isCod);

	PaymentGatewayData verifyOrderPayment(long orderId, OrderPaymentGatewayVerifyRequest orderPaymentGatewayVerifyRequest) throws Exception;

	WalletTransactionInfo processOrderWalletTransaction(OrderWalletPaymentRequest orderWalletPaymentRequest) throws Exception;

	List<PaymentGatewayData> confirmOrderPayment(long orderId, boolean verifyCod) throws Exception;

}
