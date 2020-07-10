package in.lifcare.order.microservice.catalog.service.impl;


import static com.netflix.hystrix.contrib.javanica.conf.HystrixPropertiesManager.EXECUTION_ISOLATION_THREAD_INTERRUPT_ON_TIMEOUT;
import static com.netflix.hystrix.contrib.javanica.conf.HystrixPropertiesManager.EXECUTION_ISOLATION_THREAD_TIMEOUT_IN_MILLISECONDS;
import static com.netflix.hystrix.contrib.javanica.conf.HystrixPropertiesManager.EXECUTION_TIMEOUT_ENABLED;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.firewall.RequestRejectedException;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import com.amazonaws.services.glacier.model.RequestTimeoutException;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;

import in.lifcare.core.constant.APIEndPoint;
import in.lifcare.core.exception.NotFoundException;
import in.lifcare.core.model.ProductResponse;
import in.lifcare.core.model.ProductSalt;
import in.lifcare.core.response.model.Response;
import in.lifcare.core.util.CommonUtil;
import in.lifcare.core.util.MicroserviceClient;
import in.lifcare.order.microservice.catalog.model.Medicine;
import in.lifcare.order.microservice.catalog.model.Salt;
import in.lifcare.order.microservice.catalog.service.CatalogService;

/**
 * Created by dev on 17/11/17.
 */
@Service
public class CatalogServiceImpl implements CatalogService {

	@Override
	@SuppressWarnings("unchecked")

	@HystrixCommand(fallbackMethod = "getDataFallBack", commandProperties = { @HystrixProperty(name = EXECUTION_ISOLATION_THREAD_TIMEOUT_IN_MILLISECONDS, value = "1000"),
			@HystrixProperty(name = EXECUTION_ISOLATION_THREAD_INTERRUPT_ON_TIMEOUT, value = "true"), @HystrixProperty(name = EXECUTION_TIMEOUT_ENABLED, value = "true") })
	public List<Medicine> findByLocationAndSkuIn(String location, List<String> skus) {

		String targetUrl = UriComponentsBuilder.fromUriString(APIEndPoint.CATALOG_SERVICE).path("/location/" + location + "/medicines").queryParam("sku-ids", StringUtils.join(skus, ",")).build()
				.toUriString();
		@SuppressWarnings("rawtypes")
		//ResponseEntity<Response> response = restTemplate.exchange(targetUrl, HttpMethod.GET, null, Response.class);
		ResponseEntity<Response> response = microserviceClient.exchange(targetUrl, HttpMethod.GET, null, Response.class);
		if (response.getBody() != null && response.getBody().getPayload() != null) {
			Medicine[] BasicMedicines = (Medicine[]) response.getBody().populatePayloadUsingJson(Medicine[].class);
			return Arrays.asList(BasicMedicines);
		}
		throw new RequestRejectedException("No data found for given condition");
	}

	public List<Medicine> getDataFallBack(String pincode, List<String> skus) {
		throw new RequestTimeoutException("Fallback method handled exception while fetching medicine data for " + pincode + " and skus : " + skus);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	@HystrixCommand(fallbackMethod = "getDataFallBack2", commandProperties = { @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "1000")})
	public List<ProductSalt> findBySaltIds(List<String> saltIds) {
		UriComponentsBuilder targetUrl = UriComponentsBuilder.fromUriString(APIEndPoint.CATALOG_SERVICE).path("/salts").queryParam("salt-ids", String.join(",", saltIds));
		ResponseEntity<Response> response = microserviceClient.exchange(targetUrl.build().toString(), HttpMethod.GET, null, Response.class);
		if (response != null) {
			Page<ProductSalt> salts = (Page<ProductSalt>) response.getBody().populatePageableUsingJson(ProductSalt.class);
			return salts.getContent();
		}
		return null;
	}
	
	public List<ProductSalt> getDataFallBack2(List<String> saltIds) {
		throw new RequestTimeoutException("Fallback method handled exception while fetching medicine data and skus : " + saltIds);
	}
	
	public List<Salt> getDataFallBack(List<String> saltIds) {
		throw new RequestTimeoutException("Fallback method handled exception while fetching salts data for ids: " + saltIds);
	}
	
	@Override
	public Medicine getMedicineInformationBySkuAndFacilityCode(String sku, Integer facilityCode) throws Exception {
		if( StringUtils.isBlank(sku) || facilityCode == null ) {
			throw new IllegalArgumentException("Invalid parameters : sku or facility code is not specified");
		}
		Response<?> response = microserviceClient.getForObject(APIEndPoint.CATALOG_SERVICE + "/medicine/sku/" + sku + "?facility-code=" + facilityCode, Response.class);
		try {
			Medicine medicine = (Medicine) response.populatePayloadUsingJson(Medicine.class);
			if( medicine == null ) {
				throw new NotFoundException("No medicine stock found for sku : " + sku + " and facility code : " + facilityCode);
			}
			return medicine;
		} catch(Exception e) {
			throw new Exception("No medicine stock found for sku : " + sku + " and facility code : " + facilityCode + ", Exception : " + e.getMessage());
		}
	}
	

	@Override
	public List<Medicine> getMedicinesInformationBySkuAndFacilityCode(Integer facilityCode, List<String> skus) throws Exception {

		String targetUrl = UriComponentsBuilder.fromUriString(APIEndPoint.CATALOG_SERVICE).path("/location/" + facilityCode + "/medicines").queryParam("sku-ids", StringUtils.join(skus, ",")).build()
				.toUriString();
		ResponseEntity<Response> response = microserviceClient.exchange(targetUrl, HttpMethod.GET, null, Response.class);
		if (response.getBody() != null && response.getBody().getPayload() != null) {
			@SuppressWarnings("unchecked")
			Medicine[] BasicMedicines = (Medicine[]) response.getBody().populatePayloadUsingJson(Medicine[].class);
			return Arrays.asList(BasicMedicines);
		}
		throw new RequestRejectedException("No data found for given condition");
	}
	
	@Override
	@SuppressWarnings("unchecked")
	@HystrixCommand(fallbackMethod = "getDataFallBack1", commandProperties = { @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "1000")})
	public List<ProductResponse> getProductByPincodeAndSkus(String clientId, String pincode, String businessType, List<String> skus) {
		if(StringUtils.isBlank(clientId)) {
			clientId = CommonUtil.getClientFromSession();
		}
		if(StringUtils.isBlank(businessType)) {
			businessType = "b2c";
		}
		UriComponentsBuilder targetUrl = UriComponentsBuilder.fromUriString(APIEndPoint.CATALOG_SERVICE)
				.path("/pincode/" + pincode + "/products")
				.queryParam("client-id", clientId)
				.queryParam("business-type", businessType)
				.queryParam("sku-ids", String.join(",", skus));
		try {
			ResponseEntity<Response> response = microserviceClient.exchange(targetUrl.build().toString(), HttpMethod.GET, null, Response.class);
			ProductResponse[] products = (ProductResponse[]) response.getBody().populatePayloadUsingJson(ProductResponse[].class);
			return Arrays.asList(products);
		} catch (Exception e) {
			return new ArrayList<ProductResponse>();
		}
	}
	
	public List<ProductResponse> getDataFallBack1(String clientId, String pincode, String businessType, List<String> skus) {
		throw new RequestTimeoutException("Fallback method handled exception while fetching catalog data for skus: " + skus);
	}

	@Autowired
	private MicroserviceClient<Response> microserviceClient;
}
