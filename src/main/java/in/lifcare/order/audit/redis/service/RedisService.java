package in.lifcare.order.audit.redis.service;

import java.util.List;

public interface RedisService {

	List<String> get(List<String> keys);
	
	void add(String key, String value);
}
