package in.lifcare.order.audit.redis.service.impl;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import in.lifcare.order.audit.redis.service.RedisService;

@Service
public class RedisServiceImpl implements RedisService{

	@Override
	public List<String> get(List<String> keys) {
		return redisTemplate.opsForValue().multiGet(keys);
	}

	@Override
	public void add(String key, String value) {
		redisTemplate.opsForValue().set(key, value, 3, TimeUnit.DAYS);
	}

	@Autowired
	private RedisTemplate<String, String> redisTemplate;
}
