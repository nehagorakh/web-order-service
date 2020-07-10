package in.lifcare.order.model;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.Min;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import in.lifcare.core.audit.AuditEntityListener;
import in.lifcare.core.audit.annotation.CreatedByName;
import in.lifcare.core.audit.annotation.UpdatedByName;
import in.lifcare.core.constant.PatientMedicineConstant;
import in.lifcare.core.constant.PatientMedicineConstant.DOSAGE;
import in.lifcare.core.model.ProductSalt;
import in.lifcare.core.util.CommonUtil;
import in.lifcare.order.audit.PostEntityListener;
import in.lifcare.order.microservice.catalog.model.Medicine;
import in.lifcare.order.property.constant.OrderPropertyConstant;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = false)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity
@Data
@Table(name = "order_item")
@EntityListeners({AuditEntityListener.class, PostEntityListener.class})
public class OrderItem  implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8594602771331155326L;

	@Id
	@Column(updatable = false)
	@GeneratedValue
	private long id;

	@Min(value = 1, message = "not valid order_id")
	@Column(updatable = false)
	private long orderId;
	
	private long childOrderId;

	private String status;

	private String statusReason;

	private String state;

	private String sku;

	private String name;

	private String brandName;

	private String imageUrl;

	private Long prescriptionId;

	private String classification;

	private float salePrice;

	private String displayOrderLineId;

	private float mrp;

	private float tax;

	private float discount;

	private int quantity;

	private int looseQuantity;
	
	@Column(updatable = false)
	private int orderedQuantity;

	@JsonIgnore
	private int dosageValue;

	@Transient
	private List<String> dosageSchedule;

	private String packType;

	private int perPackQty;

	private boolean isVerified;

	private String verifiedBy;

	private boolean isActive = true;

	@CreatedByName
	private String createdByName;

	@UpdatedByName
	private String updatedByName;

	public String createdBy;

	@CreationTimestamp
	@Column(updatable = false)
	private Timestamp createdAt;

	@UpdateTimestamp
	private Timestamp updatedAt;

	public String updatedBy;

	@Transient
	private Float discountPercentage;

	@JsonProperty("is_cold_storage")
	private boolean isColdStorage;
	// dosage, frequecy, is_verified, sku genration,

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
	
	@Transient
	private String availableServiceType;
	
	@Transient
	private String availableDeliveryOption;
	
	@Transient
	private boolean isLcAssuredAvailable;
	
	@Transient
	private boolean isUrgentDlAvailable;
	
	@Transient
	private String stockAvailability;
	
	@Transient
	private String medicineStatus;
	
	private Integer packOf;
	
	private Integer facilityCode;
	
    private boolean isTeleConsult;
	
	private String autoVerificationFailedReason;
	
	private Double refillIndex;
	
	private boolean isProductVerified;
	
	private double consumptionPerDay;
	
	@Transient
	private Set<String> diseases;
	
	@Transient
	private Set<String> moleculeTypes;
	
	@Transient
	private List<ProductSalt> salts;
	
	@Transient
	private int bulkOrderQuantity;
	
	private String salt;
	
	private Integer maxOrderQuantity;
	
	public interface AUTO_VERIFICATION_FAILED_REASON {
		
		static String HABIT_FORMING_DRUG = "HABIT_FORMING_DRUG";
		static String UN_VERIFIED_MEDICINE = "UN_VERIFIED_MEDICINE";
		static String BULK_MEDICINE_QUANTITY_EXCEEDED = "BULK_MEDICINE_QUANTITY_EXCEEDED";
		static String INACTIVE_MEDICINE_STATUS = "INACTIVE_MEDICINE_STATUS";
		static String MEDICINE_NAME_MISMATCH = "MEDICINE_NAME_MISMATCH";
		static String INVALID_SKU = "INVALID_SKU";
		static String FIRST_PATIENT_ORDER = "FIRST_PATIENT_ORDER";
		static String MAX_ORDER_QUANTITY_EXCEEDED = "MAX_ORDER_QUANTITY_EXCEEDED";
		static String PRESCRIPTION_EXPIRED = "PRESCRIPTION_EXPIRED";
		static String PRESCRIPTION_RX_DATE_NOT_FOUND = "PRESCRIPTION_RX_DATE_NOT_FOUND";
		static String PRESCRIPTION_ID__NOT_MAPPED = "PRESCRIPTION_ID__NOT_MAPPED";
		static String UNKNOWN_EXCEPTION = "UNKNOWN_EXCEPTION";
	}
	
	public Integer getMaxOrderQuantity() {
		if (this.maxOrderQuantity != null && this.maxOrderQuantity == 0) {
			this.maxOrderQuantity = null;
		}
		return this.maxOrderQuantity;
	}
	
	public void updateInfoBySalts(List<ProductSalt> salts) {
		if (salts != null && !salts.isEmpty()) {
			boolean isTeleConsult = true;
			List<String> diseases = new ArrayList<>();
			double refillIndex = 0;
			double consumptionPerDay = 0;
			int bulkOrderQuantity = 0;
			int maxOrderQuantity = 0;
			List<String> moleculeType = new ArrayList<>();
			List<String> saltsIds = new ArrayList<>();
			for (ProductSalt salt : salts) {
				if (salt.getIsTeleConsult() != null && isTeleConsult) {
					isTeleConsult = salt.getIsTeleConsult();
				}
				if (salt.getConsumptionPerDay() != null && consumptionPerDay < salt.getConsumptionPerDay()) {
					consumptionPerDay = salt.getConsumptionPerDay();
				}
				if (salt.getRefillIndex() != null && refillIndex < salt.getRefillIndex()) {
					refillIndex = salt.getRefillIndex();
				}
				if (salt.getDiseases() != null && !salt.getDiseases().isEmpty()) {
					diseases.addAll(salt.getDiseases());
				}
				if (salt.getMoleculeTypes() != null && !salt.getMoleculeTypes().isEmpty()) {
					moleculeType.addAll(salt.getMoleculeTypes());
				}
				if (salt.getBulkOrderQuantity() != null && ( bulkOrderQuantity == 0 || bulkOrderQuantity > salt.getBulkOrderQuantity())) {
					bulkOrderQuantity = salt.getBulkOrderQuantity();
				}
				if (salt.getMaxOrderQuantity() != null && ( maxOrderQuantity == 0 || maxOrderQuantity > salt.getMaxOrderQuantity())) {
					maxOrderQuantity = salt.getMaxOrderQuantity();
				}
				saltsIds.add(salt.getId());
			}
			this.salt = !saltsIds.isEmpty() ? String.join(",", saltsIds) : null;
			this.maxOrderQuantity = null;	
			if (maxOrderQuantity > 0 ) {
				this.maxOrderQuantity = OrderPropertyConstant.MAX_ORDERED_QTY;	
				if (Medicine.PACK_TYPE_TAB.ALL.contains(this.packType) && this.perPackQty > 0) {
					int packOf = this.packOf != null && this.packOf > 0 ? this.packOf : 1;
					int maxQty =  new Double(Math.ceil(maxOrderQuantity / (this.perPackQty * packOf ))).intValue();
					this.maxOrderQuantity = maxQty;	
				}
			}
			this.isTeleConsult = isTeleConsult;
			this.consumptionPerDay = consumptionPerDay;
			this.bulkOrderQuantity = bulkOrderQuantity;
			
			this.refillIndex = refillIndex;
			this.salts = salts;
			if (!diseases.isEmpty()) {
				this.diseases = new HashSet<>(diseases);
			}
			if (!moleculeType.isEmpty()) {
				this.moleculeTypes = new HashSet<>(moleculeType);
			}
		}
	}
	
	public float getSalePrice() {
		this.salePrice = CommonUtil.round(salePrice, CommonUtil.DEFAULT_PRECISION);
		return this.salePrice > 0 ? this.salePrice : 0;
	}

	public float getMrp() {
		this.mrp = CommonUtil.round(mrp, CommonUtil.DEFAULT_PRECISION);
		return this.mrp > 0 ? this.mrp : 0;
	}

	public float getTax() {
		this.tax = CommonUtil.round(tax, CommonUtil.DEFAULT_PRECISION);
		return this.tax > 0 ? this.tax : 0;
	}

	public float getDiscount() {
		this.discount = CommonUtil.round(discount, CommonUtil.DEFAULT_PRECISION);
		return this.discount > 0 ? this.discount : 0;
	}
	
	@Transient
	private Integer validDays;
	
	private String slug;

	private Long patientId;

	private String productCategory = "MEDICINE";

	public interface PRODUCT_CATEGORY {
		String LAB = "LAB";
		String MEDICINE = "MEDICINE";
		String MEMBERSHIP_CARD = "MEMBERSHIP_CARD";
	}
	
	public interface STATE {
		String NEW = "NEW";
	}
	
	public interface STATUS {
		String NEW = "NEW";
	}

	private String patientFirstName;
	
	private String patientLastName;
	
	@Transient
	private Map<String, Object> additionalProperties = new HashMap<String, Object>();
	
	@Transient
	private String description;
	
	private boolean isSkuLocallyAvailable;
}


