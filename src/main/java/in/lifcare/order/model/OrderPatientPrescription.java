package in.lifcare.order.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import in.lifcare.order.microservice.account.prescription.model.Prescription;
import lombok.Data;

/**
 * @author dev
 *
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class OrderPatientPrescription {

	private long patientId;

	private String patientFirstName;

	private String patientLastName = " ";

	private String patientFullName;

	List<Prescription> prescription;
}
