package in.lifcare.order.microservice.inventory.service;

import java.util.List;
import java.util.Map;

import in.lifcare.client.inventory.model.ProductStock;




public interface ProductStockService {

	List<ProductStock> getProductStockBySkuIdsAndRegionCode(List<String> skuIds, String regionCode);

	/**
	 * @param facilityId
	 * @param skuCountMap
	 * @return
	 */
	Boolean updateInFlightInventory(Long facilityId, Map<String, Integer> skuCountMap);

	/**
	 * 
	 * @param facilityId
	 * @param skuCountMap
	 * @return
	 */
	Boolean updateExpressInventory(Long facilityId, Map<String, Integer> skuCountMap);

	/**
	 * 
	 * @param skuId
	 * @param facilityId
	 * @return
	 */
	ProductStock getProductStockBySkuIdAndRegionCode(String skuId, long facilityId);
	
	/**
	 * 
	 * @param facilityId
	 * @param skus
	 * @return
	 */
	List<ProductStock> getProductStockByFacilityIdAndSkuIds(long facilityId, List<String> skus);

}
