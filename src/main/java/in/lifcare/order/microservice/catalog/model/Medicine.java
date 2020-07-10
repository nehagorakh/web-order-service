package in.lifcare.order.microservice.catalog.model;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import in.lifcare.order.cart.model.Cart;
import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Medicine {

	private String id;

	private String sku;

	private String name;

	private String brand;

	private String genericName;

	private long facilityId;
	
	private String facilityCode;

	private double sellingPrice;

	private double mrp;

	private double discount;

	private String locationCode;

	private String location;

	private long stock;

	private boolean isColdStorage;

	private String variantStatus;

	private String egSku;

	private String classificationCode;

	private String brandDistribution;

	private String classification;

	private String packType;

	private Map<String, Object> drugStrength;

	private String egProductName;

	private String reason;

	private String comment;

	private boolean isRetired;

	private boolean isDiscontinue;

	private String drugType;

	private double consumptionPerDay;

	private String variantReason;

	private String drugCategory;

	private String status;

	private boolean isAvailable;

	private String rackDetails;

	private String brandName;

	private String dosage;

	private String type;

	private String variantId;

	private Integer packOf;

	private String imagePath;

	private double perPackQty;

	private double noOfDaysStock;

	@JsonProperty("packaging_type")
	private String packagingType;

	private Map<String, Object> packSize;
	
	@JsonProperty("is_lc_assured_available")
	private boolean isLcAssuredAvailable;
	
	@JsonProperty("is_urgent_dl_available")
	private boolean isUrgentDlAvailable;
	
	private String stockAvailability;
	
	private String availableDeliveryOption;
	
	private String availableServiceType;
	
	public String getAvailableDeliveryOption(){
		if (isUrgentDlAvailable) {
			this.availableDeliveryOption = Cart.DELIVERY_OPTION.URGENT;
		} else {
			this.availableDeliveryOption = Cart.DELIVERY_OPTION.NORMAL;
		}
		return availableDeliveryOption;
	}
	
	public String getAvailableServiceType(){
		if (isLcAssuredAvailable) {
			this.availableServiceType = Cart.SERVICE_TYPE.LF_ASSURED;
		} else {
			this.availableServiceType = Cart.SERVICE_TYPE.NORMAL;
		}
		return availableServiceType;
	}
	
	public Map<String, Object> getPackSize() {
		return packSize;
	}

	public void setPackSize(Map<String, Object> packSize) {
		if (packSize != null) {
			this.packSize = packSize;
			Object packSizeValue = packSize.containsKey("value") ? packSize.get("value") : 0;
			if (packSizeValue instanceof Double) {
				this.perPackQty = (Double) packSizeValue;
			}
		}
	}
	
	public interface STATUS {
		static String ACTIVE = "Active";
		static String INACTIVE = "Inactive";
		static String PENDING = "Pending";
		static String TEMPORARILY_SUSPENDED = "Temporarily Suspended";
		static String RETIRED = "Retired";
		static String DISCONTINUED = "Permanently Discontinued";
		static List<String> INACTIVE_STATUS = Arrays.asList(INACTIVE, PENDING, TEMPORARILY_SUSPENDED, RETIRED, DISCONTINUED);
	}
	
	public interface PACK_TYPE_TAB {
		static String TABLET = "TABLET";
		static String CAPSULES = "CAPSULES";
		
		static List<String> ALL = Arrays.asList(TABLET, CAPSULES);
	}
	
	private Boolean isFullCourse;
	
	private boolean isTeleConsult;
	
	private Integer maxOrderQuantity;
	
	private Double refillIndex = 0.0;
	
	private boolean isVerified;
	
	private List<String> diseases;
	
	private List<String> moleculeTypes;
	
	private List<Salt> salts;
	
	private Integer bulkOrderQuantity;
	
	private Integer maxOrderUnitQuantity;
	
	public Integer getBulkOrderQuantity(){
		if (salts != null && !salts.isEmpty() && ( bulkOrderQuantity == null || bulkOrderQuantity != null && bulkOrderQuantity <= 0)) {
			List<Integer> bulkOrderQtyStream = salts.parallelStream()
					.filter(salt -> salt.getBulkOrderQuantity() != null)
					.map(Salt::getBulkOrderQuantity).collect(Collectors.toList());
			if (bulkOrderQtyStream != null && !bulkOrderQtyStream.isEmpty()) {
				this.bulkOrderQuantity = bulkOrderQtyStream.parallelStream().mapToInt(i -> i).min().getAsInt();
			}
		}
		return this.bulkOrderQuantity;
	}
	
	public static float DEFAULT_DISCOUNT = 15;
	
	private Integer validDays;

	private String slug;
	
	private boolean localAvailable;
}
