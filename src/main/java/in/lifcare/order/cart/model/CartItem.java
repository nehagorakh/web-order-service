package in.lifcare.order.cart.model;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import in.lifcare.core.audit.AuditEntityListener;
import in.lifcare.core.audit.annotation.CreatedBy;
import in.lifcare.core.audit.annotation.UpdatedBy;
import in.lifcare.core.constant.PatientMedicineConstant;
import in.lifcare.core.constant.PatientMedicineConstant.DOSAGE;
import in.lifcare.core.model.ProductSalt;
import in.lifcare.core.util.CommonUtil;
import in.lifcare.order.microservice.catalog.model.Salt;
import in.lifcare.order.microservice.inventory.model.ProductStock;
import lombok.Data;

@Data
@Entity
@Table(name = "cart_item")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@EntityListeners(AuditEntityListener.class)
public class CartItem  implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private long id;

	@Column(unique = true)
	private String cartUid;

	private String sku;
	
	private Integer facilityCode;

	private String name;

	private Long patientId;

	private String productCategory = CartItem.PRODUCT_CATEGORY.MEDICINE;

	private String productSubCategory;

	private boolean isBundledProduct;
	
	private String imagePath;

	private String classification;

	private int perPackQty;

	private long quantity;

	private double mrp;

	private double sellingPrice;

    private double discount;
    
	private double tax;

	private String brand;

	private String type;

	private String drugType;

	private boolean isColdStorage;

	@Transient
	private boolean priceUpdateFlag;

	@JsonIgnore
	private int dosageValue;
	
	@Transient
	private List<String> dosageSchedule;

	private String status = CART_ITEM_STATUS.IN_STOCK;

	private String drugStrength;

	private boolean isVerified;
	
	@CreationTimestamp
	@Column(updatable = false)
	private Timestamp createdAt;

	@UpdateTimestamp
	private Timestamp updatedAt;
	
	@CreatedBy
	private String createdBy;

	@UpdatedBy
	private String updatedBy;

	@JsonIgnore
	private String log;
	
	private String stockAvailability = ProductStock.STOCK_AVAILABILITY_STATUS.NEW_ITEM;
	
	private String availableDeliveryOption = DELIVERY_OPTION.NORMAL;
	
	private String availableServiceType = SERVICE_TYPE.NORMAL;
	
	public interface CART_ITEM_STATUS {
		String OUT_OF_STOCK = "OUT_OF_STOCK";
		String IN_STOCK = "IN_STOCK";
		String NOT_AVAILABLE = "NOT_AVAILABLE";
 	}
	
	@Transient
	private int minDeliveryDay;
	
	@Transient
	private int maxDeliveryDay;

	public interface PRODUCT_CATEGORY {
		String LAB_TEST = "LAB";
		String MEDICINE = "MEDICINE";
		String MEMBERSHIP_CARD = "MEMBERSHIP_CARD";
	}
	
	public interface DELIVERY_OPTION {
		String URGENT = "URGENT";
		String NORMAL = "NORMAL";
	}

	public interface SERVICE_TYPE {
		String LF_ASSURED = "LF_ASSURED";
		String NORMAL = "NORMAL";
	}

	public interface ORDER_DELIVERY_TYPE {
		String IS_NORMAL_DELIVERY = "is_normal_delivery";
		String IS_LC_ASSURED_DELIVERY = "is_lc_assured_delivery";
		String IS_URGENT_DELIVERY = "is_urgent_delivery";
	}

	public void setStockAvailability(String stockAvailability) {
		this.stockAvailability = stockAvailability;
		if( ProductStock.STOCK_AVAILABILITY_STATUS.STOCK_NOT_AVAILABLE_LIST.contains(stockAvailability) ) {
			this.status = CartItem.CART_ITEM_STATUS.OUT_OF_STOCK;
		}
		if( ProductStock.STOCK_AVAILABILITY_STATUS.NEW_ITEM.equalsIgnoreCase(stockAvailability) ) {
			this.status = CartItem.CART_ITEM_STATUS.NOT_AVAILABLE;
		}
	}

	public String getStockAvailability() {
		if( ProductStock.STOCK_AVAILABILITY_STATUS.STOCK_NOT_AVAILABLE_LIST.contains(this.stockAvailability) ) {
			this.status = CartItem.CART_ITEM_STATUS.OUT_OF_STOCK;
		}
		if( ProductStock.STOCK_AVAILABILITY_STATUS.NEW_ITEM.equalsIgnoreCase(this.stockAvailability) ) {
			this.status = CartItem.CART_ITEM_STATUS.NOT_AVAILABLE;
		}
		return this.stockAvailability;
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
	
	@Transient
	private boolean isTeleConsult;
	
	@Transient
	private Double refillIndex;
	
	@Transient
	private Double consumptionPerDay;
	
	@Transient
	private Set<String> diseases;
	
	@Transient
	private Set<String> moleculeTypes;
	
	@Transient
	private boolean isProductVerified;
	
	@Transient
	private Integer maxOrderQuantity;
	
	@Transient
	private List<ProductSalt> salts;
	
	@Transient
	private boolean isExcessiveOrderedQuantity;
	
	private boolean isAutoSuggestQty;
	
	@Transient
	private int packOf;
	
	@Transient
	private int bulkOrderQuantity;
	
	@Transient
	private String packType;
	
	@Transient
	private String description;
	
	@Transient
	private Object additionalProperties = new HashMap<String, Object>();
	
	public Integer getMaxOrderQuantity() {
		if (this.maxOrderQuantity != null && this.maxOrderQuantity == 0) {
			this.maxOrderQuantity = null;
		}
		return this.maxOrderQuantity;
	}
	
	public double getMrp() {
		this.mrp = CommonUtil.round(mrp, CommonUtil.DEFAULT_PRECISION);
		return this.mrp > 0 ? this.mrp : 0;
	}

	public double getSellingPrice() {
		this.sellingPrice = CommonUtil.round(sellingPrice, CommonUtil.DEFAULT_PRECISION);
		return this.sellingPrice > 0 ? this.sellingPrice : 0;
	}

	public double getDiscount() {
		this.discount = CommonUtil.round(discount, CommonUtil.DEFAULT_PRECISION);
		return this.discount > 0 ? this.discount : 0;
	}

	private Long prescriptionId;
	
	@Transient
	private Integer validDays;
	
	@Transient
	private String comment;
	
	@Transient
	private boolean validToBuy = true;

	private String slug;
	
	private String patientFirstName;
	
	private String patientLastName;
	
	@Transient
	private String productStatus;
	
}