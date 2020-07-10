package in.lifcare.order.cart.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import in.lifcare.order.cart.model.CartPrescription;

@Repository
public interface CartPrescriptionRepository extends CrudRepository<CartPrescription, Long>{

	List<CartPrescription> findByCartUid(String cartUid);

	CartPrescription findByIdAndCartUid(Long id, String cartUid);

	List<CartPrescription> findByIdIn(List<Long> ids);
	
}
