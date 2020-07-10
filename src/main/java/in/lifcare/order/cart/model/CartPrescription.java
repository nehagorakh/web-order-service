package in.lifcare.order.cart.model;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Data;

@Data
@Entity
@Table(name = "cart_prescription")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CartPrescription  implements Serializable{

	@Id
	@GeneratedValue
	@Column(updatable = false)
    private long id;

	@Column(unique = true)
	private String cartUid;

    private Long prescriptionId;

    private Long patientId;
 
	@Transient
	private String location;
    
    private String bucket;

    private String relativeLocation;

    private String fileName;

    private String doctorName;

    private Timestamp expiryDate;

	private Timestamp rxDate;
	
	private String disease;

	private String type;

    @CreationTimestamp
	@Column(updatable = false)
	private Timestamp createdAt;

	@UpdateTimestamp
	private Timestamp updatedAt;
	
	private String createdBy;

	private String updatedBy;

	public interface PRESCRIPTION_TYPE {
		String IMAGE = "image";
		String PDF = "pdf";
		List<String> LIST = Arrays.asList(new String[] {IMAGE, PDF});
	}
	
}
