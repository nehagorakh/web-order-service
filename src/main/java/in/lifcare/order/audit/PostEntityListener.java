package in.lifcare.order.audit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.PostLoad;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import in.lifcare.core.model.ProductSalt;
import in.lifcare.order.audit.redis.service.RedisService;
import in.lifcare.order.microservice.catalog.service.CatalogService;
import in.lifcare.order.model.OrderItem;

@Component
public class PostEntityListener {

	private final ObjectMapper mapper = new ObjectMapper();
	private final static String ORDER = "Order";
	private final static String ORDER_ITEM = "OrderItem";
	
	@PostLoad
	public <T> void onPrePersist(T obj) {
		if (obj != null) {
			String type = obj.getClass().getSimpleName();
			switch (type) {
			case ORDER:
				break;
			case ORDER_ITEM:
				try {
					OrderItem orderItem = (OrderItem) obj;
					decodeDataFromCatalog(orderItem);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
			default:
				break;
			}
		}
	}
	
	private void decodeDataFromCatalog(OrderItem orderItem) {
		if (orderItem != null) {
				try {
					if (StringUtils.isNotBlank(orderItem.getSalt())) {
						List<String> saltIds = new java.util.ArrayList<>(Arrays.asList(orderItem.getSalt().split(",")));
						List<ProductSalt> salts = new ArrayList<>();
						List<String> objects = redisService.get(saltIds);
						if (objects != null && !objects.isEmpty()) {
							for (Object object : objects) {
								if (object != null) {
									ProductSalt salt = mapper.readValue(object.toString(), ProductSalt.class);
									salts.add(salt);
									saltIds.removeIf(s -> s.equalsIgnoreCase(salt.getId()));
								}
							}
						}
						
						if (saltIds != null && !saltIds.isEmpty()) {
							List<ProductSalt> nonCachedValues = getSaltsFromCatalog(saltIds);
							if (nonCachedValues != null && !nonCachedValues.isEmpty()) {
								updateRedis(nonCachedValues);
								salts.addAll(nonCachedValues);
							}
						}
						if (!salts.isEmpty()) {
							orderItem.updateInfoBySalts(salts);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
		}
	}
	
	private List<ProductSalt> getSaltsFromCatalog(List<String> nonCachedIds) {
		if (nonCachedIds != null && !nonCachedIds.isEmpty()) {
			try {
				return catalogService.findBySaltIds(nonCachedIds);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	@Async
	public void updateRedis(List<ProductSalt> nonCachedValues ) {
		nonCachedValues.parallelStream().forEach(salt -> {
			try {
				redisService.add(salt.getId(), mapper.writeValueAsString(salt));
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			}
		});
	}
	
	static private CatalogService catalogService;
	static private RedisService redisService;
	
	@Autowired
	public void init(CatalogService catalogService, RedisService redisService) 
	{
		PostEntityListener.catalogService = catalogService;
		PostEntityListener.redisService = redisService;
	}
}
