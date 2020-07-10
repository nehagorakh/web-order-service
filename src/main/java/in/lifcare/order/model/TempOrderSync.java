package in.lifcare.order.model;

import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@Entity
@Table(name = "temp_order_sync")
public class TempOrderSync {
	

	@Id
	@Column(updatable = false)
	@GeneratedValue
	private long id;
	
	private long orderId;
	
	private boolean newSync = false;
	
	private boolean oldSync = false;
	
	@CreationTimestamp
	@Column(updatable = false)
	private Timestamp createdAt;
	
	@UpdateTimestamp
	private Timestamp updatedAt;

}
