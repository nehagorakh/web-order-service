package in.lifcare.order.microservice.catalog.model;

import java.io.Serializable;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Salt implements Serializable {
	
	private String id;
	
	private String name;
	
	private Double consumptionPerDay;
	
	private List<String> diseases;
	
	private Double refillIndex;
	
	private List<String> moleculeTypes;
	
	private Boolean isFullCourse;
	
	private Boolean isTeleConsult;
	
	private Integer maxOrderQuantity;
	
	private Integer bulkOrderQuantity;
}