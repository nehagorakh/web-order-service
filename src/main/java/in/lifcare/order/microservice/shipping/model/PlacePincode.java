package in.lifcare.order.microservice.shipping.model;


import java.io.Serializable;
import java.sql.Timestamp;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Data;

/**
 * @author Manoj-Mac
 *
 */
@Data
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlacePincode implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private long id;

	private String pincode;

	private String city;

	private String state;

	private String country;

	private Double latitude;

	private Double longitude;

	private Integer deliveryDay;

	private Integer dispatchDay;

	private double freeShippingMinOrder;

	private double shippingFee;

	private Boolean isActive;

	private Timestamp activatedAt;

	private Timestamp deactivatedAt;

	private String updatedBy;

	private Timestamp createdAt;

	private Timestamp updatedAt;
	
	private Boolean isLcAssuredAvailable;
	
	private Boolean isUrgentDlAvailable;
	
	private double urgentDeliveryCharge;

	private double reportDeliveryCharge;
	
	private Boolean isReportHardCopyAvailable;
	
	private Boolean isLabOrderAvailable;
}
