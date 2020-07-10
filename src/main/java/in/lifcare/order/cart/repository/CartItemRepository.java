package in.lifcare.order.cart.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import in.lifcare.order.cart.model.CartItem;

@Repository
public interface CartItemRepository extends CrudRepository<CartItem, Long>{

	List<CartItem> findByCartUid(String cartUid);

	CartItem findByIdAndCartUid(Long id, String cartUid);

	CartItem findTopBySkuAndCartUidAndPatientId(String sku, String cartUid, Long patientId);
	
	List<CartItem> findByCartUidIn(List<String> cartUids);
	
}
