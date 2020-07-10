package in.lifcare.order.repository;

import java.sql.Timestamp;
import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.data.jpa.domain.Specification;

import in.lifcare.order.model.Order;


public class OrderSpecification {
	public static Specification<Order> findAll() {
		return null;
	}
	
	public static Specification<Order> filterByParentIdNotNull() {
		return new Specification<Order>() {

			@Override
			public Predicate toPredicate(Root<Order> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
				return  cb.gt(root.get("parentId"), 0);
			}
		};
	}

	public static Specification<Order> filterByParentId(final Long parentId) {
		return new Specification<Order>() {

			@Override
			public Predicate toPredicate(Root<Order> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
				return cb.equal(root.get("parentId"), parentId);
			}
		};
	}

	public static Specification<Order> filterByChildOrderId(final Long childOrderId) {
		return new Specification<Order>() {

			@Override
			public Predicate toPredicate(Root<Order> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
				return cb.equal(root.get("id"), childOrderId);
			}
		};
	}
	
	public static Specification<Order> filterByFacilityCode(final Long facilityCode) {
		return new Specification<Order>() {

			@Override
			public Predicate toPredicate(Root<Order> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
				return cb.equal(root.get("facilityCode"), facilityCode);
			}
		};
	}
	
	public static Specification<Order> filterByChildFacilityCode(final Long childFacilityCode) {
		return new Specification<Order>() {

			@Override
			public Predicate toPredicate(Root<Order> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
				return cb.equal(root.get("facilityCode"), childFacilityCode);
			}
		};
	}
	
	public static Specification<Order> filterByStatus(final String status) {
		return new Specification<Order>() {

			@Override
			public Predicate toPredicate(Root<Order> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
				return cb.equal(root.get("status"), status);
			}
		};
	}
	
	public static Specification<Order> filterByChildStatuses(final List<String> childStatuses) {
		return new Specification<Order>() {

			@Override
			public Predicate toPredicate(Root<Order> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
				return cb.and(root.get("status").in(childStatuses));
			}
		};
	}

	public static Specification<Order> filterByDispatchDateBetween(final Timestamp satrtDate,
			final Timestamp endDate) {
		return new Specification<Order>() {

			@Override
			public Predicate toPredicate(Root<Order> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
				return cb.between(root.get("dispatchDate"), satrtDate, endDate);
			}
		};
	}
	
	public static Specification<Order> filterByPromisedDeliveryDateBetween(final Timestamp satrtDate,
			final Timestamp endDate) {
		return new Specification<Order>() {

			@Override
			public Predicate toPredicate(Root<Order> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
				return cb.between(root.get("promisedDeliveryDate"), satrtDate, endDate);
			}
		};
	}

}
