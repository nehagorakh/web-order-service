package in.lifcare.order.microservice.inventory.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import in.lifcare.client.inventory.service.InventoryClientService;
import in.lifcare.core.response.model.Response;
import in.lifcare.order.exception.OrderException;
//import in.lifcare.order.microservice.inventory.model.ProductStock;
import in.lifcare.client.inventory.model.ProductStock;
import in.lifcare.order.microservice.inventory.service.ProductStockService;

@Service
public class ProductStockServiceImpl implements ProductStockService {

	@Override
	public List<ProductStock> getProductStockBySkuIdsAndRegionCode(List<String> skuIds, String regionCode) {
		try {
			//String targetUrl = UriComponentsBuilder.fromUriString(APIEndPoint.INVENTORY_SERVICE).path("/stock-list").queryParam("sku-ids", StringUtils.join(skuIds, ",")).queryParam("region-codes", regionCode).build().toUriString();
			//Response<List<ProductStock>> skuStockInfo = microserviceClient.getForObject(targetUrl, Response.class);
			List<String> regionCodes = new ArrayList<>();
			regionCodes.add(regionCode);
			Response<List<ProductStock>> skuStockInfo = inventoryClientService.getProductStockBySkusAndRegionCodes(skuIds, regionCodes);
			List<ProductStock> productStockList = mapper.readValue(mapper.writeValueAsString(skuStockInfo.getPayload()), new TypeReference<List<ProductStock>>(){});
			if( productStockList == null || productStockList.isEmpty() ) {
				throw new OrderException("No product stock list found for sku's");
			}
			return productStockList;
		} catch(Exception e) {
			throw new OrderException(e.getMessage());
		}
	}
	
	@Override
	public Boolean updateInFlightInventory(Long facilityId, Map<String, Integer> skuCountMap) {
		if (skuCountMap != null && !skuCountMap.isEmpty()) {
			try {
				/*
				 * microserviceClient.patchForObject( APIEndPoint.INVENTORY_SERVICE +
				 * "/facility/" + facilityId + "/update-inflight-inventory", skuCountMap,
				 * Response.class);
				 */
				inventoryClientService.updateInFlightInventory(facilityId, skuCountMap);
			} catch (Exception e) {
				if (skuCountMap != null) {
					for (Map.Entry<String, Integer> entry : skuCountMap.entrySet()) {
						String currentInflightCount = null;
						String currentExpressInflightCount = null;
						int skuInflightCount = 0;
						int skuExpressInflightCount = 0;
						if (entry.getValue() != null) {
							currentInflightCount = redisTemplate.opsForValue()
									.get("FailedInFlightSkuCount_" + facilityId + "_" + entry.getKey());
							if (currentInflightCount != null) {
								skuInflightCount = Integer.parseInt(currentInflightCount) + entry.getValue();
							}
							currentExpressInflightCount = redisTemplate.opsForValue()
									.get("FailedExpressInFlightSkuCount_" + facilityId + "_" + entry.getKey());
							if (currentExpressInflightCount != null) {
								skuExpressInflightCount = Integer.parseInt(currentExpressInflightCount) + entry.getValue();
							}
						}
						redisTemplate.opsForValue().set("FailedInFlightSkuCount_" + facilityId + "_" + entry.getKey(),
								String.valueOf(skuInflightCount));
						redisTemplate.opsForValue().set(
								"FailedExpressInFlightSkuCount_" + facilityId + "_" + entry.getKey(),
								String.valueOf(skuExpressInflightCount));
					}
					return false;
				}
			}
		}
		return false;
	}

	@Override
	public Boolean updateExpressInventory(Long facilityId, Map<String, Integer> skuCountMap) {
		if (skuCountMap != null) {
			try {
				/*
				 * microserviceClient.patchForObject( APIEndPoint.INVENTORY_SERVICE +
				 * "/facility/" + facilityId + "/update-express-inflight-inventory",
				 * skuCountMap, Response.class);
				 */
				inventoryClientService.updateExpressInFlightInventory(facilityId, skuCountMap);
			} catch (Exception e) {
				if (skuCountMap != null) {
					for (Map.Entry<String, Integer> entry : skuCountMap.entrySet()) {
						String currentCount = null;
						int skuCount = 0;
						if (entry.getValue() != null && entry.getValue() > 0) {
							currentCount = redisTemplate.opsForValue()
									.get("FailedExpressInFlightSkuCount_" + facilityId + "_" + entry.getKey());
							if (currentCount != null) {
								skuCount = Integer.parseInt(currentCount) + entry.getValue();
							}
						}
						redisTemplate.opsForValue().set(
								"FailedExpressInFlightSkuCount_" + facilityId + "_" + entry.getKey(),
								String.valueOf(skuCount));
					}
					return false;
				}
			}
		}
		return false;
	}
	
	@Override
	public ProductStock getProductStockBySkuIdAndRegionCode(String sku, long facilityId) {
		try {
			//String targetUrl = UriComponentsBuilder.fromUriString(APIEndPoint.INVENTORY_SERVICE).path("/facility/" + facilityId + "/stock/" + sku).build().toUriString();
			//Response<ProductStock> skuStockInfo = microserviceClient.getForObject(targetUrl, Response.class);
			Response<ProductStock> skuStockInfo = inventoryClientService.getProductStockByFacilityIdAndSku(facilityId, sku);
			ProductStock productStock = mapper.readValue(mapper.writeValueAsString(skuStockInfo.getPayload()), new TypeReference<ProductStock>(){});
			return productStock;
		} catch(Exception e) {
			return null;
		}
	}
	


	@Override
	public List<ProductStock> getProductStockByFacilityIdAndSkuIds(long facilityId, List<String> skus) {
		try {
			//String targetUrl = UriComponentsBuilder.fromUriString(APIEndPoint.INVENTORY_SERVICE).path("/facility/"+facilityId+"/stock-list").queryParam("sku-ids", StringUtils.join(skus, ",")).build().toUriString();
			//Response<List<ProductStock>> skuStockInfo = microserviceClient.getForObject(targetUrl, Response.class);
			Response<List<ProductStock>> skuStockInfo = inventoryClientService.getProductStockListByFacilityIdAndSkus(facilityId, skus);
			List<ProductStock> productStockList = mapper.readValue(mapper.writeValueAsString(skuStockInfo.getPayload()), new TypeReference<List<ProductStock>>(){});
			return productStockList;
		} catch(Exception e) {
			return null;
		}
	}

	private ObjectMapper mapper = new ObjectMapper();

	/*
	 * @Autowired private MicroserviceClient<Response> microserviceClient;
	 */
	
	@Autowired
	private RedisTemplate<String, String> redisTemplate;
	
	@Autowired
	private InventoryClientService inventoryClientService;

	
}
