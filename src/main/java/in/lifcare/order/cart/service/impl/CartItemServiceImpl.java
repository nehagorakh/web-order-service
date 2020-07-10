package in.lifcare.order.cart.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import in.lifcare.client.inventory.model.ProductStock;
import in.lifcare.core.exception.NotFoundException;
import in.lifcare.core.model.PatientMedicine;
import in.lifcare.core.model.ProductResponse;
import in.lifcare.core.util.CommonUtil;
import in.lifcare.order.cart.model.Cart;
import in.lifcare.order.cart.model.CartItem;
import in.lifcare.order.cart.repository.CartItemRepository;
import in.lifcare.order.cart.service.CartItemService;
import in.lifcare.order.microservice.account.patient.service.PatientMedicineService;
import in.lifcare.order.microservice.catalog.model.Medicine;
import in.lifcare.order.microservice.catalog.service.CatalogService;
import in.lifcare.order.microservice.inventory.service.ProductStockService;
import in.lifcare.order.property.constant.OrderPropertyConstant;

@Service
public class CartItemServiceImpl implements CartItemService {

	@Override
	public CartItem saveCartItem(CartItem cartItem) {
		return cartItemRepository.save(cartItem);
	}

	@Override
	public List<CartItem> saveCartItems(List<CartItem> cartItems) {
		return (List<CartItem>) cartItemRepository.save(cartItems);
	}

	@Override
	public void deleteCartItem(Long cartItemId) {
		cartItemRepository.delete(cartItemId);
	}

	@Override
	public void deleteCartItems(List<CartItem> cartItems) {
		cartItemRepository.delete(cartItems);
	}
	
	@Override
	@Transactional(propagation=Propagation.REQUIRES_NEW)
	public void deleteCartItemsWithNewTransection(List<CartItem> cartItems) {
		cartItemRepository.delete(cartItems);
	}
	
	@Override
	public List<CartItem> getCartItemsByCartUid(String cartUid) {
		return cartItemRepository.findByCartUid(cartUid);
	}

	@Override
	public CartItem findByIdAndCartUid(Long cartItemId, String cartUid) {
		return cartItemRepository.findByIdAndCartUid(cartItemId, cartUid); 
	}

	Map<Integer, Integer> masterFacilityMap = new HashMap<Integer, Integer>(){
		{
			put(273, 101);
			put(259, 101);
			put(263, 101);
			put(231, 101);
			put(237, 101);
			put(227, 101);
			put(252, 101);
			put(255, 101);
			put(243, 101);
			put(208, 101);
			put(203, 101);
			put(245, 101);
			put(258, 101);
			put(257, 101);
			put(254, 101);
			put(264, 101);

		}

	};
	
	@Override
	// FIXME : Add hysterix fallback
	// FIXME : FIX FOR SKU SIZE NEW, NOT AVAILABLE SIZE
	public List<CartItem> fetchUpdatedCartItemInventoryStock(Integer facilityCode, Long patientId, List<CartItem> cartItems, int refillDays, boolean isAutoSeggestionQty, String pincode, String businessType) {
		if (cartItems == null || facilityCode == null) {
			throw new IllegalArgumentException("No cart item or Facility Code specified for stock information update");
		}
		String clientId = CommonUtil.getClientFromSession();
		List<String> skus = cartItems.parallelStream().filter(cartItem -> cartItem != null && StringUtils.isNotBlank(cartItem.getSku()))
				// TODO : ask about NEW sku's
				// .filter(cartItem -> cartItem.getSku().length() == 6)
				.map(CartItem::getSku).collect(Collectors.toList());
		try {
			if (skus == null || skus.isEmpty()) {
				throw new IllegalArgumentException("No skus found for cart item stock update");
			}

			CompletableFuture<List<ProductResponse>> medicineFuture = CompletableFuture.supplyAsync(() -> {
				try {
					return catalogService.getProductByPincodeAndSkus(clientId, pincode, businessType, skus);
				} catch (Exception e) {
					logger.error("Error occurred : {}",e);
					return null;
				}

			});

			CompletableFuture<List<ProductStock>> productStockFuture = CompletableFuture.supplyAsync(() -> productStockService.getProductStockByFacilityIdAndSkuIds(facilityCode, skus));

			CompletableFuture<List<PatientMedicine>> patientMedicinesFuture = CompletableFuture.supplyAsync(() -> {
				if (patientId == null || patientId <= 0) {
					return null;
				}
				try {
					return patientMedicineService.getAllPatientMedicines(patientId);
				} catch (Throwable e) {
					return null;
				}
			});

			CompletableFuture.allOf(medicineFuture, productStockFuture, patientMedicinesFuture).get();

			List<ProductResponse> medicines = medicineFuture.get();
			List<ProductStock> productStocks = productStockFuture.get();
			List<PatientMedicine> patientMedicines = patientMedicinesFuture.get();

			if (medicines == null) {
				throw new NotFoundException("medicine info not found for skus");
			}
			if (productStocks == null) {
				productStocks = new ArrayList<>();
			}
			if (patientMedicines == null) {
				patientMedicines = new ArrayList<>();
			}
			
			Map<String, ProductResponse> medicineSkuMap = medicines.stream()
	                .collect(Collectors.groupingBy(ProductResponse::getSku))
	                .entrySet()
	                .stream()
	                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get(0)));
			
//			Map<String, Medicine> medicineSkuMap = medicines.stream().collect(Collectors.toMap(medicine -> StringUtils.capitalize(medicine.getSku().toLowerCase()), Function.identity()));
			Map<String, ProductStock> productStockSkuMap = productStocks.parallelStream().collect(Collectors.toMap(productStock -> StringUtils.capitalize(productStock.getSku().toLowerCase()), Function.identity()));
			Map<String, PatientMedicine> patientMedicinesMap = patientMedicines.parallelStream().collect(Collectors.toMap(patientMedicine -> StringUtils.capitalize(patientMedicine.getSku().toLowerCase()), Function.identity()));

			return updateCartItem(cartItems, medicineSkuMap, productStockSkuMap, patientMedicinesMap, refillDays, isAutoSeggestionQty, businessType);

		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return cartItems;
	}
	
	
	@Override
	// FIXME : Add hysterix fallback
	// FIXME : FIX FOR SKU SIZE NEW, NOT AVAILABLE SIZE
	public List<CartItem> fetchUpdatedCartItemInventoryStock(Integer cartFacilityCode, String pincode, Long patientId, List<CartItem> cartItems, int refillDays, boolean isAutoSeggestionQty, String businessType) {
		if (cartItems == null || StringUtils.isBlank(pincode)) {
			throw new IllegalArgumentException("No cart item or Pincode Code specified for stock information update");
		}
		String clientId = CommonUtil.getClientFromSession();
		
		List<CartItem> cartItemsResult = new ArrayList<CartItem>();
		
		List<String> skus = cartItems.parallelStream().filter(cartItem -> cartItem != null && StringUtils.isNotBlank(cartItem.getSku()))
				// TODO : ask about NEW sku's
				// .filter(cartItem -> cartItem.getSku().length() == 6)
				.map(CartItem::getSku).collect(Collectors.toList());
		try {
			if (skus == null || skus.isEmpty()) {
				throw new IllegalArgumentException("No skus found for cart item stock update");
			}

			List<ProductResponse> medicines = catalogService.getProductByPincodeAndSkus(clientId, pincode, businessType, skus);
			
			Map<String, List<ProductResponse>> facilityMedicinesMap = new HashMap<String, List<ProductResponse>>();
			
			Map<String, List<String>> facilitySkuMap = new HashMap<String, List<String>>();
			
			for (ProductResponse medicine : medicines) {
				if (facilityMedicinesMap.containsKey(String.valueOf(medicine.getFacilityId()))) {
					facilityMedicinesMap.get(String.valueOf(medicine.getFacilityId())).add(medicine);
				} else {
					facilityMedicinesMap.put(String.valueOf(medicine.getFacilityId()), new ArrayList<ProductResponse>() {
						{
							add(medicine);
						}
					});
				}
			}
			
			Map<String, List<CartItem>> cartItemsMap = new HashMap<String, List<CartItem>>();
			
			for (CartItem cartItem : cartItems) {
				cartItem.setFacilityCode(cartFacilityCode);
				if (cartFacilityCode != null && StringUtils.isNotBlank(String.valueOf(cartFacilityCode))) {
					if (cartItemsMap.containsKey(String.valueOf(cartFacilityCode))) {
						cartItemsMap.get(String.valueOf(cartFacilityCode)).add(cartItem);
					} else {
						cartItemsMap.put(String.valueOf(cartFacilityCode), new ArrayList<CartItem>() {
							{
								add(cartItem);
							}
						});
					}
				} 
				
				
				if (facilitySkuMap.containsKey(String.valueOf(cartFacilityCode))) {
					facilitySkuMap.get(String.valueOf(cartFacilityCode)).add(cartItem.getSku());
				} else {
					facilitySkuMap.put(String.valueOf(cartFacilityCode), new ArrayList<String>() {
						{
							add(cartItem.getSku());
						}
					});
				}
			}
			
			Map<String, List<ProductStock>> facilityProductStockMap = new HashMap<String, List<ProductStock>>();
			for (String key : facilitySkuMap.keySet()) {
				facilityProductStockMap.put(key, productStockService.getProductStockByFacilityIdAndSkuIds(Integer.parseInt(key), facilitySkuMap.get(key)));
			}
			
			List<PatientMedicine> patientMedicines = new ArrayList<PatientMedicine>();
			if (patientId != null && patientId > 0) {

				patientMedicines = patientMedicineService.getAllPatientMedicines(patientId);

			}


			//List<Medicine> medicines = medicineFuture.get();
			//List<ProductStock> productStocks = productStockFuture.get();
			//List<PatientMedicine> patientMedicines = patientMedicinesFuture.get();

			if (patientMedicines == null) {
				patientMedicines = new ArrayList<>();
			}
			
			//Map<String, Map<String, ProductStock>> productStockFacilitySkuMap = new HashMap<String, Map<String, ProductStock>>();
			
			//Map<String, Map<String, Medicine>> medicineFacilitySkuMap = new HashMap<String, Map<String, Medicine>>();
			
			//Map<String, Map<String, PatientMedicine>> patientMedicineFacilitySkuMap = new HashMap<String, Map<String, PatientMedicine>>();
			
			for(String key : facilitySkuMap.keySet()) {
				
				List<CartItem> newCartItems = cartItems;
				if(cartItemsMap != null && !cartItemsMap.isEmpty()) {
					newCartItems = cartItemsMap.get(key);
				}
				
				String masterFacility = masterFacilityMap.containsKey(key) && masterFacilityMap.get(key) != null ? String.valueOf(masterFacilityMap.get(key))
						: key;
				
				Map<String, ProductStock> productStockSkuMap = facilityProductStockMap.get(key).parallelStream().collect(Collectors.toMap(productStock -> StringUtils.capitalize(productStock.getSku().toLowerCase()), Function.identity()));
				
				Map<String, ProductResponse> medicineSkuMap = facilityMedicinesMap.get(masterFacility).parallelStream().collect(Collectors.toMap(medicine -> StringUtils.capitalize(medicine.getSku().toLowerCase()), Function.identity()));
				
				Map<String, PatientMedicine> patientMedicinesMap = patientMedicines.parallelStream().collect(Collectors.toMap(patientMedicine -> StringUtils.capitalize(patientMedicine.getSku().toLowerCase()), Function.identity()));
				
				
				//productStockFacilitySkuMap.put(key, productStockSkuMap);
				//medicineFacilitySkuMap.put(key, medicineSkuMap);
				//patientMedicineFacilitySkuMap.put(key, patientMedicinesMap);
				
				cartItemsResult.addAll(updateCartItem(newCartItems, medicineSkuMap, productStockSkuMap, patientMedicinesMap, refillDays, isAutoSeggestionQty, businessType));
			}
			
			return cartItemsResult;


		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return cartItems;
	}
	

	@Override
	public CartItem findBySkuAndCartUidAndPatientId(String sku, Long patientId, String cartUid) {
		return cartItemRepository.findTopBySkuAndCartUidAndPatientId(sku, cartUid, patientId);
	}
	
	private List<CartItem> updateCartItem(List<CartItem> cartItems, Map<String, ProductResponse> medicineSkuMap, Map<String, ProductStock> productStockSkuMap,
			Map<String, PatientMedicine> patientMedicinesMap, int refillDays, boolean isAutoSeggestionQty, String businessType) {
		for (CartItem cartItem : cartItems) {
			try {
				double consumption = 0;
				if (medicineSkuMap.containsKey(cartItem.getSku())) {
					ProductResponse medicine = medicineSkuMap.get(cartItem.getSku());
					cartItem.setAvailableServiceType(Cart.SERVICE_TYPE.NORMAL);
					cartItem.setAvailableDeliveryOption(Cart.DELIVERY_OPTION.NORMAL);
					cartItem.setFacilityCode(medicine.getFacilityId() != null ? medicine.getFacilityId().intValue() : 0);
					cartItem.setName(medicine.getName());
					cartItem.setDiscount(medicine.getDiscount());
					cartItem.setBrand(medicine.getBrandName());
					cartItem.setSku(medicine.getSku());
					cartItem.setSellingPrice(medicine.getSellingPrice());
					cartItem.setMrp(medicine.getMrp());
					cartItem.setPackType(medicine.getPackType());
					cartItem.setType(medicine.getPackType());
					cartItem.setPerPackQty((int) medicine.getPerPackQty());
					cartItem.setImagePath(medicine.getImagePath());
					cartItem.setProductCategory(medicine.getCategory());
					cartItem.setDrugType(medicine.getDrugType());
					cartItem.setColdStorage(medicine.getIsColdStorage() != null ? medicine.getIsColdStorage() : false);
					cartItem.setQuantity(cartItem.getQuantity() <= 0 ? 1 : cartItem.getQuantity());
					cartItem.setConsumptionPerDay(medicine.getConsumptionPerDay() != null ? medicine.getConsumptionPerDay() : 1.0);
					cartItem.setRefillIndex(medicine.getRefillIndex());
					cartItem.setDiseases(medicine.getDiseases() != null ? new HashSet<>(medicine.getDiseases()) : null);
					cartItem.setMoleculeTypes(medicine.getMoleculeTypes() != null ? new HashSet<>(medicine.getMoleculeTypes()) : null);
					cartItem.setTeleConsult(medicine.getIsTeleConsult() != null ? medicine.getIsTeleConsult() : true);
					cartItem.setProductVerified(medicine.isVerified());
					cartItem.setClassification(medicine.getClassificationCode());
					cartItem.setProductStatus(medicine.getStatus());
					if(Cart.BUSINESS_TYPE.B2B.equalsIgnoreCase(businessType)) {
						cartItem.setMaxOrderQuantity(OrderPropertyConstant.MAX_BULK_ORDERED_QTY);
					} else {
						cartItem.setMaxOrderQuantity(OrderPropertyConstant.MAX_ORDERED_QTY);
					}
					if (medicine.getMaxOrderUnitQuantity() != null && medicine.getMaxOrderUnitQuantity() > 0 && !Cart.BUSINESS_TYPE.B2B.equalsIgnoreCase(businessType)) {	
						if (Medicine.PACK_TYPE_TAB.ALL.contains(medicine.getPackType()) && medicine.getPerPackQty() > 0) {
							int packOf = medicine.getPackOf() != null && medicine.getPackOf() > 0 ? medicine.getPackOf() : 1;
							int maxQty =  new Double(Math.ceil(medicine.getMaxOrderUnitQuantity() / (medicine.getPerPackQty() * packOf))).intValue();
							cartItem.setMaxOrderQuantity(maxQty);	
						}
					}
					cartItem.setSalts(medicine.getSalts());
					cartItem.setPackOf(medicine.getPackOf() != null && medicine.getPackOf() > 0 ? medicine.getPackOf() : 1);
					cartItem.setBulkOrderQuantity(medicine.getBulkOrderQuantity() != null ? medicine.getBulkOrderQuantity() : 0);
					// use cartItem status for medicine status and isExcessiveOrderedQuantity
					if (productStockSkuMap.containsKey(cartItem.getSku())) {
						ProductStock productStock = productStockSkuMap.get(cartItem.getSku());
						if (productStock.getAvailableStock() >= cartItem.getQuantity()) {
							cartItem.setStockAvailability(ProductStock.STOCK_AVAILABILITY_STATUS.IN_STOCK);
							cartItem.setAvailableDeliveryOption(Cart.DELIVERY_OPTION.URGENT);
							cartItem.setAvailableServiceType(Cart.SERVICE_TYPE.LF_ASSURED);
						} else {
							cartItem.setStockAvailability(productStock.getStockAvailability());
						}
					}
					consumption = medicine.getConsumptionPerDay() != null ? medicine.getConsumptionPerDay() : 1;
					cartItem.setSlug(medicine.getSlug());
					cartItem.setAdditionalProperties(medicine.getAdditionalProperties());
					cartItem.setDescription(medicine.getShortDescription());
				}
				long qty = cartItem.getQuantity();
				if (patientMedicinesMap.containsKey(cartItem.getSku())) {
					PatientMedicine patientMedicine = patientMedicinesMap.get(cartItem.getSku());
					if (patientMedicine.getConsumption() > 0) {
						cartItem.setConsumptionPerDay(new Double(patientMedicine.getConsumption()));
					}
					if (patientMedicine.getPrescribedConsumption() > 0) {
						consumption = patientMedicine.getPrescribedConsumption();
					}
					cartItem.setConsumptionPerDay(consumption);
					if(!Cart.BUSINESS_TYPE.B2B.equalsIgnoreCase(businessType)) {
						cartItem.setExcessiveOrderedQuantity(patientMedicine.isExcessiveOrderedQuantity());
					}
					cartItem.setVerified(patientMedicine.isVerified());
				}
				if ((isAutoSeggestionQty || cartItem.isAutoSuggestQty()) && StringUtils.isNotBlank(cartItem.getPackType()) && Medicine.PACK_TYPE_TAB.ALL.contains(cartItem.getPackType()) && refillDays > 0 && consumption > 0) {
					qty = getSuggestedQuantity(refillDays, cartItem.getPerPackQty() * cartItem.getPackOf(), consumption);
					if ( cartItem.getMaxOrderQuantity() != null && qty > cartItem.getMaxOrderQuantity()) {
						qty = cartItem.getMaxOrderQuantity();
					}
					cartItem.setAutoSuggestQty(Boolean.TRUE);
				}
				if (qty > 0) {
					cartItem.setQuantity(qty);
				}
			} catch (Exception e) {
				cartItem.setLog(e.getMessage());
				cartItem.setStockAvailability(ProductStock.STOCK_AVAILABILITY_STATUS.NEW_ITEM);
			}
		}
		return cartItems;
	}
	
	@Override
	public List<CartItem> getCartItemsByCartUidIn(List<String> cartUids) {
		return cartItemRepository.findByCartUidIn(cartUids);
	}
	
	private long getSuggestedQuantity(int refillDays, Integer perPackQty, double consumptionPerDay) {
		if ( refillDays > 0 && perPackQty != null &&  perPackQty > 0) {
			return new Double(Math.ceil((((consumptionPerDay * refillDays ) / perPackQty)))).intValue();
		}
		return 0;
	}
	
	private Logger logger = LoggerFactory.getLogger(CartItemServiceImpl.class);
	
	@Autowired
	private CatalogService catalogService;
	
	@Autowired
	private CartItemRepository cartItemRepository;
	
	@Autowired
	private ProductStockService productStockService;
	
	@Autowired
	private PatientMedicineService patientMedicineService;

}