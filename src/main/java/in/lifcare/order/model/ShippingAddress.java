package in.lifcare.order.model;

import java.io.Serializable;
import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.Min;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
@Table(name = "shipping_address")
public class ShippingAddress  implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Id
	@Column(updatable = false)
	@GeneratedValue
	private long id;

    @Min(value=1, message="not valid order_id")
	private long orderId;

    @Min(value=1, message="not valid customer_id")
	private long customerId;

	private String prefix;

	private String firstName;

	private String lastName;

	private String street1;

	private String street2;

	private String city;

	private String state;

	private long pincode;

	private String country;

	private double latitude;

	private double longitude;
	
	private double accuracy;

	private String type;

	private String mobile;

	@CreationTimestamp
	@Column(updatable =  false)
	private Timestamp createdAt;

	@UpdateTimestamp
	private Timestamp updatedAt;

	private String modifiedBy;

	@Transient
	private String fullName;


	public String getFullName() {
		if (StringUtils.isEmpty(fullName)) {
			fullName = firstName;
			if (StringUtils.isNotEmpty(lastName)) {
				fullName = fullName + " " + lastName;
			}
			;
		}
		return fullName;
	}

	public interface STATE {
		String HARYANA = "HARYANA";
		String UTTAR_PRADESH = "UTTAR PRADESH";
		String RAJASTHAN = "RAJASTHAN";
		String DELHI = "DELHI";
	}
	
	private Boolean isVerified;
	
	private String locality;
	
	private String email;
	
}
