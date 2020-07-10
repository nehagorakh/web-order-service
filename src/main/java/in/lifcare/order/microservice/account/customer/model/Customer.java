package in.lifcare.order.microservice.account.customer.model;

import java.sql.Timestamp;
import java.util.List;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Transient;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import in.lifcare.order.microservice.account.patient.model.Patient;
import lombok.Data;


/**
 * 
 * @author Amit Kumar
 * @since 4/4/17
 * @version 0.1.0
 */
@Data
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Customer {

	private long id;

	private String prefix;

	@NotEmpty
	private String firstName;

	private String lastName = " ";

	@NotEmpty
	private String gender;
	
	@NotEmpty
	private String mobile;

	private String landline;

	private String email;

	private String displayCustomerId;
	
	private String advertisementId;
	
	private String fcmToken;

	private String userName;

	private boolean isActive = true;

	private String defaultLocation;

	private String languagePreference = "en";
	
	private Timestamp dateOfBirth;

	@CreationTimestamp
	@Column(updatable = false)
	private Timestamp createdAt;

	@UpdateTimestamp
	private Timestamp updatedAt;
	
	private String referenceBy;

	private String referenceCode;

	private Long referenceEntityId;

	private String referenceMobile;
	
	private String source;
	
	@Transient
	private Long defaultPatientId;
	
	private long age;
	
	private String membershipType;
	
	private String membershipCode;
	
	@Transient
	private List<Patient> patients;
	
	@Transient
	private Set<String> communications;
	
	private String referralCode;
	
	private String referralUrl;

	@Transient
	private Long profileImageId;

	@Transient
	private String profileImageUrl;

	private String relativeLocation;

	private String bucket;

	@Transient
	private String fullName;

	@Transient
	private String referenceByName;

	public String getFullName() {
		if (StringUtils.isEmpty(fullName)) {
			fullName = firstName + " ";
			if (StringUtils.isNotEmpty(lastName)) {
				fullName += lastName;
			}
		}
		return fullName;
	}

	public interface REFERENCE_BY {
		String DOCTOR = "DOCTOR";
		String CUSTOMER = "CUSTOMER";
	}

}
