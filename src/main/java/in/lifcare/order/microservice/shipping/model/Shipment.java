package in.lifcare.order.microservice.shipping.model;

import java.sql.Timestamp;
import javax.persistence.Column;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Data;


@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class Shipment {

	private long id;

	private String awbNumber;

	private String referenceNumber;

	private String manifestId;

	private String courierProvider;

	private String routeCode;

	private String reverseAwbNumber;

	private String reverseCourierProvider;

	private String shipmentType;

	private String itemDescription;

	private int itemCount;

	private double collectableAmount;

	private double totalAmount;

	private String status;

	private int facilityId;

	private long hubId;

	private String hubName;

	private long spokeId;

	private String spokeName;

	private String originCity;

	private String currentCity;

	private String currentState;

	private String destinationCity;

	private int retryCount;

	private String allocatedAgentName;

	private String allocatedAgentMobile;

	@NotNull(message = "Consignee first name must be specified in shipment")
	private String consigneeFirstName;

	private String consigneeLastName;

	@NotNull(message = "Consignee Address must be specified in shipment")
	private String consigneeAddress1;

	private String consigneeAddress2;

	private String consigneeAddress3;

	@NotNull(message = "Consignee pincode must be specified in shipment")
	private String consigneePincode;

	private String consigneeState;

	private String consigneeCity;

	private String consigneeCountry;

	@NotNull(message = "Consignee mobile number must be specified in shipment")
	private String consigneeMobile;

	private String consigneeTelephone;

	private String jobType;

	@CreationTimestamp
	@Column(updatable = false)
	private Timestamp createdAt;

	@UpdateTimestamp
	private Timestamp updatedAt;

	private Timestamp handOverToCourierTime;

	private Timestamp promisedDeliveryTime;

	private String actualDeliveryTime;

	private String expectedDeliveryTime;

	private String retryReason;

	private String returnReason;

	private double weight;

	private int length;

	private int breadth;

	private int height;

	private int deliveryAttemptCount;

	private String deliveryOption;

	private String serviceType;

	private Timestamp firstAttemptRetryTime;

	private String firstAttemptRetryReason;

	private Timestamp secondAttemptRetryTime;

	private String secondAttemptRetryReason;

	private Timestamp thirdAttemptRetryTime;

	private String thirdAttemptRetryReason;

	private String packagingType;

	private String shipmentFailureReason;

	private boolean isMiddleMile = false;

	private String pickupName;

	private String pickupAddressLine1;

	private String pickupAddressLine2;

	private String pickupPincode;

	private String pickupPhone;

	private String pickupMobile;

	private String returnName;

	private String returnAddressLine1;

	private String returnAddressLine2;

	private String returnPincode;

	private String returnPhone;

	private String returnMobile;

	private String shipmentCreatedByName;

	private String shipmentManifestedBy;

	private String shipmentDispatchedBy;

	private String shipmentDeliveredBy;

}
