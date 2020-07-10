package in.lifcare.order.microservice.payment.model;

import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class OrderWalletPaymentRequest {

	private long orderId;
	private long customerId;
	private String source;
	private double amount;
	private String walletMethod;
	private String transactionType;

	public interface TRANSACTION_TYPE {
		String DEBIT = "debit";
		String CREDIT = "credit";
		List<String> VALID_TRANSACTION_TYPE_LIST = Arrays.asList(new String[] {DEBIT, CREDIT});
	}
	
	public interface WALLET_METHOD {
		String CARE_POINT = "CARE_POINT";
		String CARE_POINT_PLUS = "CARE_POINT_PLUS";
		List<String> VALID_WALLET_METHOD_LIST = Arrays.asList(new String[] {CARE_POINT, CARE_POINT_PLUS});
	}

}