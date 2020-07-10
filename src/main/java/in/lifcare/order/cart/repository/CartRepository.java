package in.lifcare.order.cart.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import in.lifcare.order.cart.model.Cart;

@Repository
public interface CartRepository extends CrudRepository<Cart, Long>{

	Cart findOneByUid(String cartUid);

	Cart findTopByCustomerIdAndStatusAndTypeOrderByCreatedAtDesc(Long customerId, String status, String type);

	Cart findTopByCustomerIdAndPatientIdAndTypeAndStatusOrderByCreatedAtDesc(Long customerId, long patientId, String type, String status);

	Page<Cart> findAllByCustomerIdAndTypeAndStatusIn(long customerId, String type, List<String> statuses, Pageable pageable);

	List<Cart> findAllByUidIn(List<String> cartUids);

	List<Cart> findAllByUidInAndStatusIn(List<String> cartUids, List<String> statuses);

	//Cart findTopByCustomerIdAndStatusAndTypeOrderByCreatedAtAsc(Long customerId, String created, String normal);

	//Cart findTopByCustomerIdAndStatusAndTypeAndUserTypeOrderByCreatedAtDesc(Long customerId, String status, String cartType, String userType);

	Cart findTopByCustomerIdAndStatusAndTypeAndCategoryAndUserTypeOrderByCreatedAtDesc(Long customerId, String status, String cartType, String category, String userType);

}
