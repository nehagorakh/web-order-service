package in.lifcare.order.microservice.inventory.model;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import javax.persistence.Transient;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductStock {
	
	private long id;
	
	private String sku;
	
	private String regionCode;
	
	private long facilityId;
	
	private float mrp;
	
	private String salePrice;
	
	private int stockQuantity;
	
	private int stockAllocated;
	
	private int packQuantity;
	
	@Transient
	private List<String> rackCode;
	
	private Timestamp createdAt;
	
	private Timestamp updatedAt;

	private Timestamp stockSyncedAt;

	private int reserveQuantity;
	
	private int lifetimeStockQuantity;
	
	private int jitMissedCount;
	
	private int jitProcuredCount;
	
	private Long jitProcurementDuration;
	
	private Timestamp jitUpdatedAt;
	
	private String manualAvailablity;
	
	private Boolean isDiscontinue;
	
	private Timestamp discontinuedAt;

	
	private int inFlightStock;

	private int expressInFlightStock;
	
	private int stockNotFound;

	private int stockFound;;

	public interface STOCK_AVAILABILITY_STATUS {
		String IN_STOCK = "IN_STOCK";
		String AVAILABLE = "Mostly Available";
		String TENTATIVE = "Tentative Availability";
		String LIMITED_SUPPLY = "Limited Availability";
		String NEW_ITEM = "New Item";
		List<String> STOCK_NOT_AVAILABLE_LIST = Arrays.asList(AVAILABLE, TENTATIVE, LIMITED_SUPPLY);
		String EXCEED_EXCESSIVE_ORDERED_QUANTITY = "EXCEED_EXCESSIVE_ORDERED_QUANTITY";
		String MAX_ORDERED_QUANTITY = "MAX_ORDERED_QUANTITY";
	}
	
	private int stockAvailabilityIndex;
	
	private int availableStock;
	
	private int failedExpressInFlightSkuCount;
	
	private String stockAvailability;
	
    private int additionalLooseQuantity;
	
	private boolean isSyncNeeded;
	
	private int jitInFlightStock;

	
	
}