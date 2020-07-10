package in.lifcare.order.microservice.coupon.model;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Data;

/**
 * 
 * @author karan
 *
 */
@Data
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Coupon implements java.io.Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String id;

	private List<String> label;// multiple, [promotion, membership]

	private String couponCode;

	private String description;

	private String shortDescription;

	private Date startDate;

	private Date endDate;

	private long usageCount;

	private int priority = 0;

	private String type;

	private List<String> segments;

	private boolean isClubable = false;

	private boolean isFreeShipping;

	private boolean isFreeCOD;

	private boolean isActive;

	private String prefix;

	private String postfix;

	private long totalCoupon;

	private int couponPerCustomer;

	private boolean autoApply;

	private float discountAmount;

	private float discountPercentage;

	private float itemDiscountPercentage;

	private boolean isAutoGenerate;

	private int couponLength;

	private int dashSeparator;

	private float maxDiscount;

	private float minBuy;

	private String termsLink;

	private Date createdAt;

	private Date updatedAt;

	public interface TYPE {
		String CUSTOMER = "CUSTOMER";
		String GLOBAL = "GLOBAL";
		String PRODUCT = "PRODUCT";
		String VOUCHER = "VOUCHER";
		String INTERNAL = "INTERNAL";
	}

}