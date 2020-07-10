package in.lifcare.order.microservice.account.patient.model;

import java.io.Serializable;
import java.sql.Timestamp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Patient implements Serializable {

	private static final long serialVersionUID = 1L;

	private long id;

	private String prefix;

	private String firstName;

	private String lastName;

	private String gender;

	private String mobile;

	private String email;

	private boolean isActive;

	private String displayPatientId;

	private String defaultLocation;

	private String languagePreference = "en";

	private Timestamp createdAt;

	private Timestamp updatedAt;

	private String fullName;

	private Integer birthYear;

	private Timestamp dateOfBirth;
	
	private Timestamp nextDueDate;

	private Long customerId;
	
	private int nextDueDays;
	
	private String primaryDoctor;
	
	private String primaryDisease;

	private Integer age;

	private String source;

	private String doctorName;

    private String alternateWhatsappNumber;

    private boolean isPrimaryProvider;
	
}