package in.lifcare.order.model;

import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import in.lifcare.core.model.Name;
import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderSummary {

	private long id;

	private String state;

	private String status;

	private String customerFirstName;

	private String customerLastName;
	
	private String  customerFullName;

	private List<OrderItemSummary> items;

	private List<OrderPrescriptionSummary> prescriptions;

	private Date createdAt;

	private Date deliveryDate;

	private String paymentMethod;

	private float totalPayableAmount;

	private String deliveryOption;

	private String serviceType;

	public String getCustomerFullName() {
		if(StringUtils.isBlank(this.customerFullName)) {
			Name name = new Name(customerFirstName, customerLastName);
			this.customerFullName = name.getFullName();
		}
		return this.customerFullName;
	}
	
}
