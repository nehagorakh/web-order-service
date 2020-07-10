package in.lifcare.order.model;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.Min;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import in.lifcare.core.audit.AuditEntityListener;
import in.lifcare.core.constant.PatientMedicineConstant;
import in.lifcare.core.constant.PatientMedicineConstant.DOSAGE;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 
 * @author karan
 *
 */
@EqualsAndHashCode(callSuper = false)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity
@Data
@Table(name = "order_additional_item")
@EntityListeners(AuditEntityListener.class)
public class OrderAdditionalItem {

	@Id
	@Column(updatable = false)
	@GeneratedValue(strategy = GenerationType.AUTO)
	private long id;

	@Min(value = 1, message = "not valid order_id")
	private long orderId;

	private String sku;

	private String productName;

	private String status;

	private String medicineType;

	@JsonIgnore
	private int dosageValue;

	private int recommendQty;

	private String frequency;

	private String itemSource;

	private long prescriptionId;

	private int duration;
	
	@Transient
	private List<String> dosageSchedule;

	@CreationTimestamp
	@Column(updatable = false)
	private Timestamp createdAt;

	@UpdateTimestamp
	private Timestamp updatedAt;

	public interface STATUS {
		String NEW = "NEW";
		String MODIFIED = "MODIFIED";
		String DELETED = "DELETED";
	}
	
	public interface MEDICINE_TYPE {
		String ORDER_ITEM = "ORDER_ITEM";
		String PATIENT_TREATMENT_LINE = "PATIENT_TREATMENT_LINE";
	}

	@JsonIgnore
	public boolean isMorning() {
		return (dosageValue & PatientMedicineConstant.DOSAGE_FLAG.MORNING) == PatientMedicineConstant.DOSAGE_FLAG.MORNING;
	}

	@JsonIgnore
	public boolean isAfternoon() {
		return (dosageValue & PatientMedicineConstant.DOSAGE_FLAG.AFTERNOON) == PatientMedicineConstant.DOSAGE_FLAG.AFTERNOON;
	}

	@JsonIgnore
	public boolean isEvening() {
		return (dosageValue & PatientMedicineConstant.DOSAGE_FLAG.EVENING) == PatientMedicineConstant.DOSAGE_FLAG.EVENING;
	}

	@JsonIgnore
	public boolean isNight() {
		return (dosageValue & PatientMedicineConstant.DOSAGE_FLAG.NIGHT) == PatientMedicineConstant.DOSAGE_FLAG.NIGHT;
	}

	public void setMorning(boolean isMorning) {
		dosageValue = dosageValue | (isMorning ? PatientMedicineConstant.DOSAGE_FLAG.MORNING : 0);
	}

	public void setAfternoon(boolean isAfternoon) {
		dosageValue = dosageValue | (isAfternoon ? PatientMedicineConstant.DOSAGE_FLAG.AFTERNOON : 0);
	}

	public void setEvening(boolean isEvening) {
		dosageValue = dosageValue | (isEvening ? PatientMedicineConstant.DOSAGE_FLAG.EVENING : 0);
	}

	public void setNight(boolean isNight) {
		dosageValue = dosageValue | (isNight ? PatientMedicineConstant.DOSAGE_FLAG.NIGHT : 0);
	}

	public List<String> getDosageSchedule() {
		if (dosageSchedule == null || dosageSchedule.isEmpty()) {
			this.dosageSchedule = new ArrayList<String>();
		}
		setDosageParameters(dosageValue);
		return this.dosageSchedule;
	}

	public void setDosageSchedule(List<String> dosageSchedule) {
		if (dosageSchedule != null && !dosageSchedule.isEmpty()) {
			String morning = dosageSchedule.contains(DOSAGE.MORNING) ? "1" : "0";
			String afternoon = dosageSchedule.contains(DOSAGE.AFTERNOON) ? "1" : "0";
			String evening = dosageSchedule.contains(DOSAGE.EVENING) ? "1" : "0";
			String night = dosageSchedule.contains(DOSAGE.NIGHT) ? "1" : "0";
			String[] dosageArr = new String[] { night, evening, afternoon, morning };
			this.dosageValue = Integer.parseInt(String.join("", dosageArr), 2);
			setDosageParameters(dosageValue);
		} else {
			this.dosageSchedule = new ArrayList<String>();
		}
	}

	public void setDosageParameters(int dosageValue) {
		String dosageStr = Integer.toBinaryString(dosageValue);
		dosageStr = StringUtils.leftPad(dosageStr, 4, "0");
		String[] values = dosageStr.split("");
		if (values.length > 0) {
			for (int i = 0; i < values.length; i++) {
				switch (String.valueOf(i)) {
				case "0":
					if (dosageSchedule != null && !dosageSchedule.contains(DOSAGE.MORNING) && this.isMorning()) {
						dosageSchedule.add(DOSAGE.MORNING);
					}
					break;
				case "1":
					if (dosageSchedule != null && !dosageSchedule.contains(DOSAGE.AFTERNOON) && this.isAfternoon()) {
						dosageSchedule.add(DOSAGE.AFTERNOON);
					}
					break;
				case "2":
					if (dosageSchedule != null && !dosageSchedule.contains(DOSAGE.EVENING) && this.isEvening()) {
						dosageSchedule.add(DOSAGE.EVENING);
					}
					break;
				case "3":
					if (dosageSchedule != null && !dosageSchedule.contains(DOSAGE.NIGHT) && this.isNight()) {
						dosageSchedule.add(DOSAGE.NIGHT);
					}
					break;
				default:
					break;
				}
			}
		}
	}
}

