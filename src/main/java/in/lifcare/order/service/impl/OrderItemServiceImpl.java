package in.lifcare.order.service.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.netflix.hystrix.exception.HystrixRuntimeException;

import in.lifcare.core.constant.AuthConstant;
import in.lifcare.core.constant.OrderSource;
import in.lifcare.core.constant.OrderStatus;
import in.lifcare.core.model.ProductResponse;
import in.lifcare.core.model.User;
import in.lifcare.core.util.CommonUtil;
import in.lifcare.order.exception.OrderItemNotFoundException;
import in.lifcare.order.microservice.catalog.service.CatalogService;
import in.lifcare.order.model.Order;
import in.lifcare.order.model.OrderItem;
import in.lifcare.order.model.ShippingAddress;
import in.lifcare.order.repository.OrderItemRepository;
import in.lifcare.order.repository.ShippingAddressRepository;
import in.lifcare.order.service.OrderItemService; 

/**
 * Created by dev on 17/11/17.
 */
@Service
public class OrderItemServiceImpl implements OrderItemService {


    private final OrderItemRepository orderItemRepository;
    private final CatalogService catalogService;
    private final ShippingAddressRepository shippingAddressRepository;
    private final static Logger log = LoggerFactory.getLogger(OrderItemServiceImpl.class);

    @Autowired
    public OrderItemServiceImpl(OrderItemRepository orderItemRepository, CatalogService catalogService, ShippingAddressRepository shippingAddressRepository, AuditorAware<User> auditorAware) {
        this.orderItemRepository = orderItemRepository;
        this.catalogService = catalogService;
        this.shippingAddressRepository = shippingAddressRepository;
    }

    @Override
    public List<OrderItem> save(String location, List<OrderItem> orderItems) {
    	if(Strings.isNotBlank(location)){
	        if (orderItems == null || orderItems.isEmpty()) {
	            throw new IllegalArgumentException("Order-Item object can not be null or empty");
	        }
	        try {
	            return (List<OrderItem>) orderItemRepository.save(updateOrderLineInfo(null, orderItems, location, false));
	        }catch (Exception e){
	        	log.error(e.getMessage());
			}
    	}
        return (List<OrderItem>) orderItemRepository.save(orderItems);
    }

	private List<OrderItem> updateOrderItems(List<OrderItem> orderItems, List<ProductResponse> basicProductDetails, Order order, boolean isFinalPrice) {
		if( orderItems == null || basicProductDetails == null ) {
			throw new IllegalArgumentException("Invalid parameters specified");
		}
		System.out.println("Request for updating sku : " +basicProductDetails);
        boolean isPackingTypeBox = false;
        boolean isTeleConsult = true;
		float totalMrp = 0;
		float totalSalePrice = 0;
		boolean isPosOrder = order != null && OrderSource.POS.equalsIgnoreCase(order.getSource()) ? true : false;

     /* collect only data that have unique sku in list */
		List<String> orderItemNames = orderItems.parallelStream()
				.map(orderItem -> orderItem.getName().toLowerCase())
				.collect(Collectors.toList());
        final Map<String, ProductResponse> filteredProductMap = basicProductDetails.stream()
                .collect(Collectors.groupingBy(ProductResponse::getSku))
                .entrySet()
                .stream()
                .filter(pair -> pair.getValue().stream()
                .anyMatch(product -> orderItemNames.contains(product.getName().toLowerCase())) || isPosOrder)
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get(0)));
        final Collection<String> boxPackagingSkus = basicProductDetails
        		 .parallelStream()
        		 .filter(basicProduct -> basicProduct.getPackagingType() != null)
			     .filter(basicProduct -> basicProduct.getPackagingType().equalsIgnoreCase(Order.PACKAGING_TYPE.BOX))
			     .map(ProductResponse::getSku)
			     .collect(Collectors.toCollection(HashSet::new));

        log.debug("filteredMedicineMap : {}",filteredProductMap.values());
     /* update order items with basic medicines data */
		for (OrderItem orderItem : orderItems) {
			ProductResponse product = filteredProductMap.get(orderItem.getSku());
			float mrp = orderItem.getMrp();
			float salePrice = orderItem.getSalePrice();
			float discount = orderItem.getDiscount();
			if (product != null) {
				if (!isFinalPrice && !isPosOrder) {
					mrp = product.getMrp();
					salePrice = (float) product.getSellingPrice();
					discount = (float) product.getDiscount();
				}
				orderItem.setColdStorage(product.getIsColdStorage() != null ? product.getIsColdStorage() : false);
				orderItem.setPackType(product.getType());
				orderItem.setClassification(product.getClassificationCode());
				orderItem.setBrandName(product.getBrandName());
				orderItem.setLcAssuredAvailable(product.isLcAssuredAvailable());
				orderItem.setUrgentDlAvailable(product.isUrgentDlAvailable());
				orderItem.setStockAvailability(product.getStockAvailability());
				orderItem.setMedicineStatus(product.getStatus());
				orderItem.setMoleculeTypes(product.getMoleculeTypes() != null ? new HashSet<>(product.getMoleculeTypes()) : new HashSet<>());
				orderItem.setPackOf(product.getPackOf() != null && product.getPackOf() > 0 ? product.getPackOf() : 1);
				orderItem.setMaxOrderQuantity(product.getMaxOrderQuantity() != null && product.getMaxOrderQuantity() != 0 ? product.getMaxOrderQuantity() : 10);
				orderItem.setPerPackQty(product.getPerPackQty() > 0 ? new Double(product.getPerPackQty()).intValue()
						: product.getPackSizeUnit() != null && product.getPackSizeUnit() > 0 ? product.getPackSizeUnit() : 1);
			//	orderItem.setMaxOrderQuantity(null);
				orderItem.setTeleConsult(product.getIsTeleConsult() != null ? product.getIsTeleConsult() : false);
				orderItem.updateInfoBySalts(product.getSalts());
				orderItem.setValidDays(product.getValidDays());
				orderItem.setProductVerified(product.isVerified());
				orderItem.setRefillIndex(product.getRefillIndex() != null ? product.getRefillIndex() : 0);
				orderItem.setConsumptionPerDay(product.getConsumptionPerDay() != null ? product.getConsumptionPerDay() : 1.6);
				orderItem.setSlug(product.getSlug());
				orderItem.setAdditionalProperties(product.getAdditionalProperties());
				orderItem.setDescription(product.getShortDescription());
			} else if (!isFinalPrice) {
				if (orderItem.getSku().toUpperCase().contains("NEW") || (orderItem.getDiscount() <= 0 && !isPosOrder)) {
					discount = ProductResponse.DEFAULT_DISCOUNT;
				}
				salePrice = orderItem.getMrp() * (1 - (orderItem.getDiscount() / 100));
			}
			if(boxPackagingSkus.contains(orderItem.getSku()) && !isPackingTypeBox){
				isPackingTypeBox = true;
			}
			if (isTeleConsult) {
				isTeleConsult = orderItem.isTeleConsult();
			}
			if( orderItem.getPerPackQty() <= 0 ) {
				orderItem.setPerPackQty(1);
			}
			orderItem.setMrp(mrp);
			orderItem.setSalePrice(salePrice);
			orderItem.setDiscount(discount);
			totalMrp += (float) (orderItem.getMrp() * (orderItem.getQuantity() + ((float) orderItem.getLooseQuantity() / (float) orderItem.getPerPackQty())));
			totalSalePrice += (float) (orderItem.getMrp() * (orderItem.getQuantity() + ((float) orderItem.getLooseQuantity() / (float) orderItem.getPerPackQty()))) * (100 - orderItem.getDiscount()) / 100;
		}
		if(order != null) {
			order.setTotalMrp(totalMrp);
			order.setTotalSalePrice(totalSalePrice);
			if (isPackingTypeBox) {
				order.setPackagingType(Order.PACKAGING_TYPE.BOX);
			} else {
				order.setPackagingType(Order.PACKAGING_TYPE.TEMPER_PROOF);
			}
			order.setTeleConsult(isTeleConsult);
		}
        log.debug("update order items from catalog : {}",orderItems);
        return orderItems;
    }
    
    @Override
    public List<OrderItem> findByOrderId(long orderId){
    	return orderItemRepository.findByOrderIdOrderByIsActiveDescCreatedAtDesc(orderId);
    }
    
    @Override
    public List<OrderItem> findAllActiveByOrderId(long orderId){
    	return orderItemRepository.findByOrderIdAndIsActive(orderId, true);
    }
    
    @Override
	public List<OrderItem> save(long orderId, List<OrderItem> orderItems) {
    	if(orderId <= 0){
    		throw new IllegalArgumentException("Order-id not valid");
    	}
    	for(OrderItem orderItem :  orderItems) {
    		orderItem.setOrderId(orderId);
    		if(StringUtils.isBlank(orderItem.getState())) {
    			orderItem.setState(OrderItem.STATE.NEW);
    		}
    		if(StringUtils.isBlank(orderItem.getStatus())) {
    			orderItem.setStatus(OrderItem.STATUS.NEW);
    		}
    	}
		return (List<OrderItem>) orderItemRepository.save(orderItems);
	}

	@Override
	public void delete(List<OrderItem> orderItems) {
		log.info("Mac6 orderItems:: " +  orderItems);

		orderItemRepository.delete(orderItems);
	}
	
	@Override
	@Transactional
	public void deleteById(List<Long> ids) {
		orderItemRepository.deleteByIdIn(ids);
	}

	@Override
	public OrderItem save(long orderId, OrderItem orderItem) {
		if(orderId <= 0){
    		throw new IllegalArgumentException("Order-id not valid");
    	}
    	ShippingAddress shippingAddress = shippingAddressRepository.findTopByOrderId(orderId);
    	if(shippingAddress != null){
    		List<OrderItem> orderItems = save(shippingAddress.getPincode(), Arrays.asList(orderItem));
    		if (orderItems != null && !orderItems.isEmpty()) {
				return orderItems.get(0);
			}
    	}
		return orderItemRepository.save(orderItem);
	}

	@Override
	public OrderItem findOne(long orderItemId) {
		return orderItemRepository.findOne(orderItemId);
	}

	@Override
	public List<OrderItem> save(List<OrderItem> orderItems) {
		return (List<OrderItem>) orderItemRepository.save(orderItems);
	}

	@Override
	public List<OrderItem> updateOrderLineInfo(Order order, List<OrderItem> orderItems, String location, boolean isFinalPrice) {
		if(Strings.isEmpty(location) && order != null){
			ShippingAddress shippingAddress = shippingAddressRepository.findTopByOrderId(order.getId());
	    	if(shippingAddress != null){
	    		location = String.valueOf(shippingAddress.getPincode() > 0 ? shippingAddress.getPincode() : "");
	    	}
		}
		String clientId = CommonUtil.getClientFromSession();
		if(StringUtils.isNotBlank(clientId) && StringUtils.isBlank(AuthConstant.CLIENT_SOURCE_MAP.get(clientId)) && order != null ) {
			clientId = AuthConstant.SOURCE_CLIENT_MAP.get(order.getSource());
		}
		String businessType = "";
		if(order != null) {
			businessType = order.getBusinessType();
		}
		if (orderItems != null && !orderItems.isEmpty() && Strings.isNotBlank(location)) {
			// find distinct sku list
			try {
				List<String> skus = orderItems.stream().map(OrderItem::getSku).distinct()
						.collect(Collectors.toList());

				List<ProductResponse> medicines = catalogService.getProductByPincodeAndSkus(clientId, location, businessType, skus);
				log.info("Medicine found for skus : {} is {}",skus, medicines);
				return updateOrderItems(orderItems, medicines, order, isFinalPrice);
			} catch (HystrixRuntimeException e) {
				log.error(e.getFallbackException().getMessage());
			} catch (Exception e) {
				log.error(e.getMessage());
			}
		}
		return orderItems;
	}

	@Override
	public OrderItem save(OrderItem orderItem) {
		return orderItemRepository.save(orderItem);
	}
	
	@Override
	public List<OrderItem> updateOrderItemPrecriptionId(long orderId, Long prescriptionId, List<String> itemSkuList) throws Exception {
		if (orderId <= 0 || prescriptionId == null || prescriptionId <= 0 || itemSkuList == null || itemSkuList.isEmpty()) {
			throw new IllegalArgumentException("Invalid input arguments specified");
		}
		List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
		if (orderItems == null || orderItems.isEmpty()) {
			throw new OrderItemNotFoundException("Order Items not found");
		}
		for (int i = 0; i < orderItems.size(); i++) {
			if (itemSkuList.contains(orderItems.get(i).getSku())) {
				orderItems.get(i).setPrescriptionId(prescriptionId);
			}
		}
    	
		return (List<OrderItem>) orderItemRepository.save(orderItems);
	}
	
	@Override
	public List<OrderItem> updateOrderItemPrecriptionId(long orderId, Map<String, Long> skuPrescriptionIdMap) throws Exception {
		if (orderId <= 0 || skuPrescriptionIdMap == null  || skuPrescriptionIdMap.isEmpty()) {
			throw new IllegalArgumentException("Invalid input arguments specified");
		}
		List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
		if (orderItems == null || orderItems.isEmpty()) {
			throw new OrderItemNotFoundException("Order Items not found");
		}
		for (OrderItem orderItem : orderItems) {
			if (skuPrescriptionIdMap.containsKey(orderItem.getSku())) {
				orderItem.setPrescriptionId(skuPrescriptionIdMap.get(orderItem.getSku()));
			}
		}
		return (List<OrderItem>) orderItemRepository.save(orderItems);
	}

	@Override
	public List<OrderItem> findByOrderIdAndIsActive(long orderId, boolean isActive) {
		return orderItemRepository.findByOrderIdAndIsActive(orderId, isActive);
	}
	
	@Override
	public List<OrderItem> findByOrderIds(List<Long> orderIds, List<String> skus) {
		if (orderIds != null && !orderIds.isEmpty() && skus != null && !skus.isEmpty()) {
			return orderItemRepository.findByOrderIdInAndSkuIn(orderIds, skus);
		}
		throw new IllegalArgumentException("Invalid input arguments specified");
	}

	@Override
	public Boolean updateSaltMapping(String sku, Map<String, Object> map) {
		if (StringUtils.isNotBlank(sku) && map != null && !map.isEmpty()) {
			boolean isProductVerified = map.get("is_verified") != null ? (Boolean) map.get("is_verified") : false;
			List<String> statuses = OrderStatus.STATE_NEW.ALL_STATUSES;
			statuses.addAll(OrderStatus.STATE_PROCESSING.ALL_STATUSES);
			Optional<List<OrderItem>> orderItemsOptional = Optional.ofNullable(orderItemRepository.findAllBySkuAndIsProductVerifiedAndStatusIn(sku, !isProductVerified, statuses));
			orderItemsOptional.ifPresent( orderItems -> orderItems.parallelStream().forEach(orderItem -> {
				orderItem.setProductVerified(isProductVerified);
			}));
			orderItemRepository.save(orderItemsOptional.get());
			return true;
		}
		return false;
	}

	@Override
	public OrderItem getOrderAndSkuAndPatientId(long orderId, String sku, long patinetId) {
		if (orderId <= 0 || StringUtils.isBlank(sku) || patinetId<= 0) {
			throw new IllegalArgumentException("Invalid param provided");
		}
		return orderItemRepository.findTopByOrderIdAndSkuAndPatientIdAndIsActive(orderId, sku, patinetId, true);
	}

	@Override
	public List<OrderItem> getByOrderIds(List<Long> orderIds) {
		if (CollectionUtils.isEmpty(orderIds)) {
			throw new IllegalArgumentException("Invalid param provided");
		}
		return orderItemRepository.findByOrderIdIn(orderIds);
	}
}
