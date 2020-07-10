package in.lifcare.order.service.impl;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import in.lifcare.order.model.OrderAdditionalItem;
import in.lifcare.order.repository.OrderAdditionalItemRepository;
import in.lifcare.order.service.OrderAdditionalItemService; 

/**
 * 
 * @author karan
 *
 */
@Service
public class OrderAdditionalItemServiceImpl implements OrderAdditionalItemService {

	@Override
	public List<OrderAdditionalItem> saveOrderAdditionalItems(List<OrderAdditionalItem> orderAdditionalItems) {
		if( orderAdditionalItems == null || orderAdditionalItems.isEmpty() ) {
			throw new IllegalArgumentException("Invalid order additional items specified");
		}	
		return (List<OrderAdditionalItem>) orderAdditionalItemRepository.save(orderAdditionalItems);
	}

	@Override
	public List<OrderAdditionalItem> getOrderAdditionalItemsByOrderId(long orderId) {
		if( orderId <= 0 ) {
			throw new IllegalArgumentException("Invalid order id specified");
		}
		List<String> notAllowedStatus = Arrays.asList(new String[] {OrderAdditionalItem.STATUS.DELETED});
		List<String> notAllowedMedicineTypes = Arrays.asList(new String[] {OrderAdditionalItem.MEDICINE_TYPE.ORDER_ITEM});
		return (List<OrderAdditionalItem>) orderAdditionalItemRepository.findByOrderIdAndMedicineTypeNotInAndStatusNotIn(orderId, notAllowedMedicineTypes, notAllowedStatus);
	}
	
	@Autowired
	private OrderAdditionalItemRepository orderAdditionalItemRepository;
	
}
