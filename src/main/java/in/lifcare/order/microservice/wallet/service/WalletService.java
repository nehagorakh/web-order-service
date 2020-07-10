package in.lifcare.order.microservice.wallet.service;

import java.util.HashMap;

public interface WalletService {
	
	/**
	 * 
	 * @param customerId
	 * @param totalSalePrice
	 * @return
	 * @throws Exception 
	 */
	HashMap<String, Object> getRedeemableCarePoints(Long customerId, double totalSalePrice) throws Exception;

	/**
	 * 
	 * @param customerId
	 * @return
	 * @throws Exception 
	 */
	double getAvailableCarePoints(Long customerId) throws Exception;
	
}
