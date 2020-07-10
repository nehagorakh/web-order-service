package in.lifcare.order.service;

import java.util.List;

import in.lifcare.order.model.OrderAdditionalItem;

/**
 * 
 * @author karan
 *
 */
public interface OrderAdditionalItemService {

	/**
	 * 
	 * @param orderAdditionalItems
	 * @return
	 */
	List<OrderAdditionalItem> saveOrderAdditionalItems(List<OrderAdditionalItem> orderAdditionalItems);

	/**
	 * 
	 * @param orderId
	 * @return
	 */
	List<OrderAdditionalItem> getOrderAdditionalItemsByOrderId(long orderId);
	
}
