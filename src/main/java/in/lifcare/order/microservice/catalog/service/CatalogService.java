package in.lifcare.order.microservice.catalog.service;

import java.util.List;

import in.lifcare.core.model.ProductResponse;
import in.lifcare.core.model.ProductSalt;
import in.lifcare.order.microservice.catalog.model.Medicine;

/**
 * Created by dev on 17/11/17.
 */
public interface CatalogService {

    List<Medicine> findByLocationAndSkuIn(String location, List<String> skus);
    
    List<ProductSalt> findBySaltIds(List<String> saltIds);

	Medicine getMedicineInformationBySkuAndFacilityCode(String sku, Integer facilityCode) throws Exception;

	List<Medicine> getMedicinesInformationBySkuAndFacilityCode(Integer facilityCode, List<String> skus) throws Exception;

	List<ProductResponse> getProductByPincodeAndSkus(String clientId, String pincode, String businessType, List<String> skus);
}
