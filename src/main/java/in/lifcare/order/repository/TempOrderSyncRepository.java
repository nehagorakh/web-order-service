package in.lifcare.order.repository;

import in.lifcare.order.model.TempOrderSync;

import org.springframework.data.repository.CrudRepository;

public interface TempOrderSyncRepository extends CrudRepository<TempOrderSync, Long>{
	
	TempOrderSync findTopByOrderId(long orderId);

}