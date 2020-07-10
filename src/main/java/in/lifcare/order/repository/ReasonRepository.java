package in.lifcare.order.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import in.lifcare.order.model.Reason;


public interface ReasonRepository extends CrudRepository<Reason, Integer> {
	
	public List<Reason> findByGroupOrderByPriorityDesc(String reasonGroup);

	public Reason findOneById(Integer id);
	
	public List<Reason> findAll();
}
