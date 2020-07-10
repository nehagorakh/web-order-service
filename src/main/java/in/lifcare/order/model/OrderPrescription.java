package in.lifcare.order.model;

import java.io.Serializable;
import java.sql.Timestamp;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity
@Table(name = "order_prescription")
public class OrderPrescription  implements Serializable {

	@Id
	@Column(updatable = false)
	@GeneratedValue
	private long id;

	private long orderId;

	private long prescriptionId;

	private String bucket;

	private String relativeLocation;

	private String fileName;

	private String doctorName;

	@Transient
	private String location;

	@CreationTimestamp
	@Column(updatable = false)
	private Timestamp createdAt;

	@UpdateTimestamp
	private Timestamp updatedAt;
	
	private Timestamp expiryDate;

	private Timestamp rxDate;
	
	private String disease;

	private String type;
}
