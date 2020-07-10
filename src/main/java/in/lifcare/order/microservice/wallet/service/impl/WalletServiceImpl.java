package in.lifcare.order.microservice.wallet.service.impl;

import java.util.HashMap;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import in.lifcare.core.constant.APIEndPoint;
import in.lifcare.core.response.model.Response;
import in.lifcare.core.util.MicroserviceClient;
import in.lifcare.order.microservice.wallet.service.WalletService;

/**
 * 
 * @author karan
 *
 */
@SuppressWarnings({"rawtypes"})
@Service
public class WalletServiceImpl implements WalletService {

	@SuppressWarnings("unchecked")
	@Override
	public HashMap<String, Object> getRedeemableCarePoints(Long customerId, double totalSalePrice) throws Exception {
		if (customerId == null) {
			throw new IllegalArgumentException("Invalid customer id specified");
		}
		HashMap<String, Object> walletInfo = new HashMap<String, Object>();
		try {
			Response<?> response = microserviceClient.getForObject(
					APIEndPoint.WALLET_SERVICE + "/" + customerId + "/applicable-points/" + totalSalePrice,
					Response.class);
			if (response.getPayload() != null) {
				walletInfo = (HashMap<String, Object>) response.getPayload();
			}
		} catch (Exception e) {
			throw e;
		}
		return walletInfo;
	}

	@Override
	public double getAvailableCarePoints(Long customerId) throws Exception {
		if( customerId == null) {
			throw new IllegalArgumentException("Invalid customer id specified");
		}
		try {
			Response<?> response = microserviceClient.getForObject(APIEndPoint.WALLET_SERVICE + "/" + customerId, Response.class);
			if( response.getPayload() != null ) {
				JSONObject json = new JSONObject(mapper.writeValueAsString(response.getPayload()));
				return json.has("bonus") ? json.getDouble("bonus") : 0;
			}
		} catch( Exception e) {
			throw e;
		}
		return 0;
	}

	private ObjectMapper mapper = new ObjectMapper();
	
	@Autowired
	private MicroserviceClient<Response> microserviceClient;
	
}
