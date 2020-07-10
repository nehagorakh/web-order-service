package in.lifcare.order.cart.service;

import java.util.List;

import in.lifcare.order.cart.model.CartItem;

/**
 * 
 * @author karan
 *
 */
public interface CartItemService {

	/**
	 * 
	 * @param cartItem
	 * @return
	 */
	CartItem saveCartItem(CartItem cartItem);

	/**
	 * 
	 * @param cartItems
	 * @return
	 */
	List<CartItem> saveCartItems(List<CartItem> cartItems);

	/**
	 * 
	 * @param cartItemId
	 */
	void deleteCartItem(Long cartItemId);

	/**
	 * 
	 * @param cartItems
	 */
	void deleteCartItems(List<CartItem> cartItems);

	/**
	 * 
	 * @param cartUid
	 * @return
	 */
	List<CartItem> getCartItemsByCartUid(String cartUid);

	/**
	 * 
	 * @param cartItemId
	 * @param cartUid
	 * @return
	 */
	CartItem findByIdAndCartUid(Long cartItemId, String cartUid);

	/**
	 * 
	 * @param patientId TODO
	 * @param cartItems
	 * @param pincode TODO
	 * @param procurementType TODO
	 * @param i
	 * @return
	 */
	List<CartItem> fetchUpdatedCartItemInventoryStock(Integer facilityCode, Long patientId, List<CartItem> cartItems, int refillDays, boolean isAutoSeggestionQty, String pincode, String businessType);

	/**
	 * 
	 * @param sku
	 * @param patientId TODO
	 * @param cartUid
	 * @return
	 */
	CartItem findBySkuAndCartUidAndPatientId(String sku, Long patientId, String cartUid);
	
	List<CartItem> getCartItemsByCartUidIn(List<String> cartUids);

	void deleteCartItemsWithNewTransection(List<CartItem> cartItems);

	List<CartItem> fetchUpdatedCartItemInventoryStock(Integer cartFacilityCode, String pincode, Long patientId, List<CartItem> cartItems, int refillDays, boolean isAutoSeggestionQty, String businessType);
	
}
