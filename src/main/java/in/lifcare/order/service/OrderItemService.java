package in.lifcare.order.service;

import java.util.List;
import java.util.Map;

import in.lifcare.order.model.Order;
import in.lifcare.order.model.OrderItem;



/**
 * Created by dev on 17/11/17.
 */
public interface OrderItemService {

	List<OrderItem> save(String location, List<OrderItem> orderItems);
    
	List<OrderItem> save(long orderId, List<OrderItem> orderItems);
	
	List<OrderItem> save(List<OrderItem> orderItems);
	
	OrderItem save(long orderId, OrderItem orderItem);
	
	OrderItem save(OrderItem orderItem);
	
    List<OrderItem> findByOrderId(long orderId);
    
    void delete(List<OrderItem> orderItems);
    
    OrderItem findOne(long orderItemId);
    
    List<OrderItem> findByOrderIdAndIsActive(long orderId, boolean isActive);

    List<OrderItem>  updateOrderLineInfo(Order order, List<OrderItem> orderItems, String location, boolean isFinalPrice);

	public List<OrderItem> updateOrderItemPrecriptionId(long orderId, Long prescriptionId, List<String> itemSkuList) throws Exception;

	List<OrderItem> findByOrderIds(List<Long> orderIds, List<String> skus);
	
	Boolean updateSaltMapping(String sku, Map<String, Object> map);

	List<OrderItem> updateOrderItemPrecriptionId(long orderId, Map<String, Long> skuPrescriptionIdMap) throws Exception;
	
	OrderItem getOrderAndSkuAndPatientId(long orderId, String sku, long patinetId);

	void deleteById(List<Long> ids);

	List<OrderItem> findAllActiveByOrderId(long orderId);

	List<OrderItem> getByOrderIds(List<Long> orderIds);
}
