package in.lifcare.order.cart.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CartSummary {

    private String uid;

    private Long customerId;

    private String patientName;
    
    private String customerName;

    private double totalMrp;
    
    private double totalSalePrice;

    private long itemCount;

    private long prescriptionCount;

}
