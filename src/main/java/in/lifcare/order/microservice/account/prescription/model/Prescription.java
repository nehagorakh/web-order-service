package in.lifcare.order.microservice.account.prescription.model;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Transient;
import javax.validation.constraints.Min;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Prescription {

	@Id
	@GeneratedValue
	@Column(updatable = false)
	private long id;

	@Min(value=1, message="not valid patient_id")
	private long patientId;

	private String relativeLocation;

	private String fileName;

	private String bucket;

	@Transient
	private String location;

	private long doctorId;

	private Timestamp expiryDate;

	@CreationTimestamp
	@Column(updatable = false)
	private Timestamp createdAt;
	
	@UpdateTimestamp
	private Timestamp updatedAt;
	
	private String doctorName;
	
	private Timestamp rxDate;
	
	private String disease;

	private String type;

	public interface PRESCRIPTION_TYPE {
		String IMAGE = "image";
		String PDF = "pdf";
		List<String> LIST = Arrays.asList(new String[] {IMAGE, PDF});
	}

}
