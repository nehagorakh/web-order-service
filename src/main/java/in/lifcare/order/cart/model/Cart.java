package in.lifcare.order.cart.model;

import java.io.Serializable;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.util.Precision;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import in.lifcare.core.audit.AuditEntityListener;
import in.lifcare.core.audit.annotation.CreatedBy;
import in.lifcare.core.audit.annotation.UpdatedBy;
import in.lifcare.core.model.Name;
import in.lifcare.core.model.SellerDetail;
import in.lifcare.core.model.OrderInfo.PROCUREMENT_TYPE;
import in.lifcare.core.util.CommonUtil;
import in.lifcare.order.microservice.account.patient.model.Patient;
import in.lifcare.order.microservice.payment.model.PaymentChannelData;
import in.lifcare.order.model.ShippingAddress;
import lombok.Data;

//FIXME : isCouponValid (only in cart model) and isItemUpdated (in both cart and cartItem model)
@Data
@Entity
@Table(name = "cart")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@SuppressWarnings("serial")
@EntityListeners(AuditEntityListener.class)
public class Cart implements Serializable{

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private long id;

	@Column(unique = true)
	private String uid = UUID.randomUUID().toString();

	private Long customerId;

	private String customerFirstName;

	private String customerLastName;

	@Transient
	private String customerFullName;

	private Long patientId;

	private Integer facilityCode = FACILITY_CODE.FACILITY_101;
	
	private String pincode = PINCODE.DELHI;

	private String patientFirstName;

	private String patientLastName;

	@Transient
	private String patientFullName;

	private Long orderId;

	private Long shippingAddressId;
	
	private String packagingType;

	@Transient
	private ShippingAddress shippingAddress;

	private String type = TYPE.NORMAL;

	private boolean isDoctorCallback = false;

	private String sourceType;

	private String category = Cart.CATEGORY.MEDICINE;

	private Timestamp appointmentDate;

	private Long appointmentSlotId;
	
	private String appointmentSlot;

	private String userType;
	
	private String comment;

	private String paymentMethod = PAYMENT_METHOD.COD;

	private String paymentSubMethod;

	@Transient
	private String paymentType;

	private double totalMrp;

	private double totalSalePrice;

	private double totalTaxAmount;

	@Transient
	private double totalPayableAmount;
	
	private long itemCount;

	private long prescriptionCount;

	private String status;

	private String source;

	private int repeatDay;

	@Transient
	private Timestamp repeatDate;
	
	@Transient
	private boolean isPriceUpdateFlag;

	@Transient
	private boolean isCouponValidFlag;

	@Transient
	private List<CartItem> cartItems;

	@Transient
	private List<CartItem> refillItems;

	@Transient
	private List<CartPrescription> cartPrescriptions;

	private String couponCode;

	private double couponDiscount;

	//@Transient
	private float couponCashback;

	@Transient
	private float cashbackPercentage;

	private double discount;

	@Transient
	private int availableCarePoints;

	@Transient
	private int redeemableCarePoints;
	
	@Transient
	private float availableCouponCashback;

	@Transient
	private float redeemableCouponCashback;

	
	private int redeemedCarePoints;

	@Transient
	private int redeemedCouponCashback;

	@Transient
	private Timestamp estimateDeliveryDate;

	@Transient
	private int estimateMinDeliveryDay;

	public int getEstimateMinDeliveryDay() {
		if (this.getEstimateDeliveryDate() != null && this.estimateMinDeliveryDay == 0) {
			LocalDate deliveryDate = this.getEstimateDeliveryDate().toLocalDateTime().toLocalDate();
			LocalDate currentDate = LocalDate.now();
			this.estimateMinDeliveryDay = new Long(ChronoUnit.DAYS.between(deliveryDate, currentDate)).intValue();
		}
		this.estimateMinDeliveryDay = this.estimateMinDeliveryDay <= 0 ? 2 : this.estimateMinDeliveryDay;
		return estimateMinDeliveryDay;
	}

	@Transient
	private int estimateMaxDeliveryDay;

	public int getEstimateMaxDeliveryDay() {
		if (this.getEstimateDeliveryDate() != null && this.estimateMinDeliveryDay == 0) {
			LocalDate deliveryDate = this.getEstimateDeliveryDate().toLocalDateTime().toLocalDate();
			LocalDate currentDate = LocalDate.now();
			this.estimateMinDeliveryDay = new Long(ChronoUnit.DAYS.between(deliveryDate, currentDate)).intValue();
		}
		this.estimateMinDeliveryDay = this.estimateMinDeliveryDay <= 0 ? 2 : this.estimateMinDeliveryDay;
		return this.estimateMinDeliveryDay + 1;
	}

	@Transient
	private double shippingFee;

	@Transient
	private double minMrpShippingFee;

	private boolean isShippingChargeExempted;

	@Transient
	private Timestamp refillDate;

	@Transient
	private long refillDays;

	@Transient
	private String couponDescription;

	@Transient
	private String shortCouponDescription;

	@Transient
	private final boolean isCouponAutoApplied = false;

	@CreationTimestamp
	@Column(updatable = false)
	private Timestamp createdAt;

	@UpdateTimestamp
	private Timestamp updatedAt;

	@CreatedBy
	private String createdBy;

	@UpdatedBy
	private String updatedBy;

	@Transient
	private Long priority;

	private Time promisedDeliveryBeginTime;
	
	private Time promisedDeliveryEndTime;

	private Timestamp promisedDeliveryDate;

	@Transient
	private String promisedDeliveryTime;
	
	private String procurementType = PROCUREMENT_TYPE.NORMAL;

	public void calculateCouponCashback() {
		couponCashback = Float.parseFloat(String.valueOf(CommonUtil.round(this.getTotalPayableAmount() * cashbackPercentage * 0.01, CommonUtil.DEFAULT_PRECISION)));
	}

	public interface PROCUREMENT_TYPE {
		String BULK = "BULK";
		String NORMAL = "NORMAL";
	}

	public void setPromisedDeliveryTime(String promisedDeliveryTime) {
		this.promisedDeliveryTime = promisedDeliveryTime;
		if (!StringUtils.isBlank(this.promisedDeliveryTime)) {
			try {
				SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
				long beginMilliSecs = sdf.parse((this.promisedDeliveryTime.split("-")[0]).trim()).getTime();
				long endMilliSecs = sdf.parse((this.promisedDeliveryTime.split("-")[1]).trim()).getTime();
				this.promisedDeliveryBeginTime = new Time(beginMilliSecs);
				this.promisedDeliveryEndTime = new Time(endMilliSecs);
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public String getPromisedDeliveryTime() {
		if (StringUtils.isEmpty(promisedDeliveryTime) && promisedDeliveryBeginTime != null && promisedDeliveryEndTime != null) {
			promisedDeliveryTime = promisedDeliveryBeginTime + " - " + promisedDeliveryEndTime;
		}	
		return promisedDeliveryTime;
	}
	
	public void setShippingFee(double shippingFee) {
		//this.shippingFee = Math.round(shippingFee * 100D) / 100D;
		this.shippingFee = CommonUtil.round(shippingFee, CommonUtil.DEFAULT_PRECISION);
	}

	public void setMinMrpShippingFee(double minMrpShippingFee) {
		//this.minMrpShippingFee = Math.round(minMrpShippingFee * 100D) / 100D;
		this.minMrpShippingFee = CommonUtil.round(minMrpShippingFee, CommonUtil.DEFAULT_PRECISION);
	}
	
	public void setTotalMrp(double totalMrp) {
		//this.totalMrp = Math.round(totalMrp * 100D) / 100D;
		this.totalMrp = CommonUtil.round(totalMrp, CommonUtil.DEFAULT_PRECISION);
	}

	public void setTotalSalePrice(double totalSalePrice) {
		//this.totalSalePrice = Math.round(totalSalePrice * 100D) / 100D;
		this.totalSalePrice = CommonUtil.round(totalSalePrice, CommonUtil.DEFAULT_PRECISION);
	}

	public double getTotalSalePrice() {
    		//this.totalSalePrice = Math.round(totalSalePrice * 100D) / 100D;
    		this.totalSalePrice = CommonUtil.round(totalSalePrice, CommonUtil.DEFAULT_PRECISION);
    		return this.totalSalePrice;
	}
	
	public double getTotalPayableAmount() {
		this.totalPayableAmount = this.getTotalSalePrice() - this.redeemableCarePoints - this.redeemableCash - this.redeemableCouponCashback;
		//this.totalPayableAmount = Math.round(this.totalPayableAmount * 100D) / 100D;
		this.totalPayableAmount = CommonUtil.round(totalPayableAmount, CommonUtil.DEFAULT_PRECISION);
		if (this.totalPayableAmount < 0) {
			this.totalPayableAmount = 0;
		}
		return this.totalPayableAmount;
	}	
	
	public void setTotalTaxAmount(double totalTaxAmount) {
		//this.totalTaxAmount = Math.round(totalTaxAmount * 100D) / 100D;
		this.totalTaxAmount = CommonUtil.round(totalTaxAmount, CommonUtil.DEFAULT_PRECISION);
	}

	public void setCouponDiscount(double couponDiscount) {
		//this.couponDiscount = Math.round(couponDiscount * 100D) / 100D;
		this.couponDiscount = CommonUtil.round(couponDiscount, CommonUtil.DEFAULT_PRECISION);
	}

	public void setDiscount(double discount) {
		//this.discount = Math.round(discount * 100D) / 100D;
		this.discount = CommonUtil.round(discount, CommonUtil.DEFAULT_PRECISION);
	}
	
	public String getCustomerFullName() {
		if (StringUtils.isBlank(this.customerFullName)) {
			Name name = new Name(customerFirstName, customerLastName);
			this.customerFullName = name.getFullName();
		}
		return this.customerFullName;
	}

	public String getPatientFullName() {
		if (StringUtils.isBlank(this.patientFullName)) {
			Name name = new Name(patientFirstName, patientLastName);
			this.patientFullName = name.getFullName();
		}
		return this.patientFullName;
	}

	public int getRepeatDay() {
		if( repeatDay > 0 && repeatDate == null ) {
			Date date = new Date();
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(date);
			calendar.add(Calendar.DAY_OF_YEAR, this.repeatDay);
			this.repeatDate = new Timestamp(calendar.getTimeInMillis());
		}
		return this.repeatDay;
	}

	public void setRepeatDay(int repeatDay) {
		this.repeatDay = repeatDay;
		if( repeatDay > 0 && repeatDate == null ) {
			Date date = new Date();
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(date);
			calendar.add(Calendar.DAY_OF_YEAR, this.repeatDay);
			this.repeatDate = new Timestamp(calendar.getTimeInMillis());
		}
	}
	
	public int getNextRefillDay() {
		if( nextRefillDay > 0 && nextRefillDate == null ) {
//			Date date = new Date();
//			Calendar calendar = Calendar.getInstance();
//			calendar.setTime(date);
//			calendar.add(Calendar.DAY_OF_YEAR, this.nextRefillDay);
//			this.nextRefillDate = new Timestamp(calendar.getTimeInMillis());
			LocalDateTime localdate = LocalDateTime.now();
			LocalDateTime nextdate = localdate.plusDays(this.nextRefillDay);
			this.nextRefillDate = Timestamp.valueOf(nextdate);
		}
		return this.nextRefillDay;
	}

	public void setNextRefillDay(int nextRefillDay) {
		this.nextRefillDay = nextRefillDay;
		if( nextRefillDay > 0 && nextRefillDate == null) {
//			Date date = new Date();
//			Calendar calendar = Calendar.getInstance();
//			calendar.setTime(date);
//			calendar.add(Calendar.DAY_OF_YEAR, this.nextRefillDay);
//			this.nextRefillDate = new Timestamp(calendar.getTimeInMillis());
			LocalDateTime localdate = LocalDateTime.now();
			LocalDateTime nextdate = localdate.plusDays(this.nextRefillDay);
			this.nextRefillDate = Timestamp.valueOf(nextdate);
		}
	}

	public interface CATEGORY {
		String LAB = "LAB";
		String MEDICINE = "MEDICINE";
		List<String> VALID_CATEGORY_LIST = new ArrayList<String>() {
			{
				add(LAB);
				add(MEDICINE);
			}
		};
	}

	public interface USER_TYPE { 
		String EXTERNAL = "EXTERNAL";
		String INTERNAL = "INTERNAL";
		List<String> VALID_USER_TYPE_LIST = new ArrayList<String>() {
			{
				add(EXTERNAL);
				add(INTERNAL);
			}
		};
	}
	
	public interface TYPE {
		String REFILL = "REFILL";
		String NORMAL = "NORMAL";
		String JIVA = "JIVA";
		String B2B = "B2B";
		List<String> VALID_CART_TYPE_LIST = new ArrayList<String>() {
			{
				add(REFILL);
				add(NORMAL);
				add(JIVA);
				add(B2B);
			}
		};
	}

	public interface SOURCE_TYPE {
		String REFILL = "REFILL";
		String REFILL_CRM = "REFILL_CRM";
		List<String> VALID_CART_SOURCE_TYPE_LIST = new ArrayList<String>() {
			{
				add(REFILL);
				add(REFILL_CRM);
			}
		};
	}

	public interface STATUS {
		String CREATED = "CREATED";
		String COMPLETED = "COMPLETED";
		String EXPIRED = "EXPIRED";
		String FAILED = "FAILED";
		String DISCARDED = "DISCARDED";
		List<String> VALID_CART_STATUS_LIST = new ArrayList<String>() {
			{
				add(CREATED);
				add(COMPLETED);
				add(EXPIRED);
				add(FAILED);
				add(DISCARDED);
			}
		};
	}

	public interface SOURCE {
		String IOS = "IOS";
		String ANDROID = "ANDROID";
		String JIVA = "JIVA";
		String POS = "POS";
		String MWEB = "MWEB";
		String MSITE = "MSITE";
		String DESKTOP = "DESKTOP";
		String REFILL_CRON = "REFILL_CRON";
		List<String> VALID_CART_SOURCE_LIST = new ArrayList<String>() {
			{
				add(IOS);
				add(ANDROID);
				add(JIVA);
				add(MWEB);
				add(POS);
				add(MSITE);
				add(REFILL_CRON);
				add(DESKTOP);
			}
		};
	}

	public interface PAYMENT_TYPE {
		String COD = "COD";
		String PREPAID = "PREPAID";
		List<String> VALID_PAYMENT_METHOD_LIST = new ArrayList<String>() {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			{
				add(COD);
				add(PREPAID);
			}
		};
	}
	
	public interface PAYMENT_METHOD {
		String COD = "COD";
		String DEBIT_CARD = "DEBIT_CARD";
		String CREDIT_CARD = "CREDIT_CARD";
		String NET_BANKING = "NET_BANKING";
		String WALLET = "WALLET";
		String UPI = "UPI";
		String EMI = "EMI";
		List<String> VALID_PAYMENT_METHOD_LIST = new ArrayList<String>() {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			{
				add(COD);
				add(DEBIT_CARD);
				add(CREDIT_CARD);
				add(NET_BANKING);
				add(WALLET);
				add(UPI);
				add(EMI);
			}
		};
	}
	
	// FIXME : Validate from account api using redis - remove hardcoded list
	// from here - guava cache
	public interface FACILITY_CODE {
		int FACILITY_101 = 101;
		int FACILITY_100 = 100;
		int FACILITY_0 = 0;
		int FACILITY_121 = 121;
		List<Integer> VALID_CART_FACILITY_CODE_LIST = new ArrayList<Integer>() {
			{
				add(FACILITY_101);
				add(FACILITY_100);
				add(FACILITY_0);
				add(FACILITY_121);
			}
		};
	}
	
	public interface PINCODE {
		String DELHI = "110001";
		String JAIPUR = "302001";
		
	}

	@Transient
	private List<String> availableDeliveryOption;

	private String preferredDeliveryOption;

	private String deliveryOption;

	private String deliveryOptionChangeReason;

	private String preferredServiceType;

	private String serviceType;

	private String serviceTypeChangeReason;

	@Transient
	private double availableCash;

	@Transient
	private double redeemableCash;

	private double redeemedCash;

	private double urgentDeliveryCharge;
	
	private double reportDeliveryCharge;

	public interface DELIVERY_OPTION {
		String URGENT = "URGENT";
		String NORMAL = "NORMAL";
	}

	public interface SERVICE_TYPE {
		String LF_ASSURED = "LF_ASSURED";
		String NORMAL = "NORMAL";
		String URGENT_ASSURED = "URGENT_ASSURED";
	}

	public interface ORDER_DELIVERY_TYPE {
		String IS_NORMAL_DELIVERY = "is_normal_delivery";
		String IS_LC_ASSURED_DELIVERY = "is_lc_assured_delivery";
		String IS_URGENT_DELIVERY = "is_urgent_delivery";
	}
	
	public interface DELIVERY_OPTION_CHANGE_REASON {
		String TIME_LIMIT_EXCEEDED = "TIME_LIMIT_EXCEEDED";
		String ORDER_COUNT_EXCEEDED = "ORDER_COUNT_EXCEEDED";
		String UPDATED_PINCODE_IS_NOT_APPLICABLE_FOR_URGENT_DELIVERY = "UPDATED_PINCODE_IS_NOT_APPLICABLE_FOR_URGENT_DELIVERY";
	}

	public interface SERVICE_TYPE_CHANGE_REASON {
		String TIME_LIMIT_EXCEEDED = "TIME_LIMIT_EXCEEDED";
		String ORDER_COUNT_EXCEEDED = "ORDER_COUNT_EXCEEDED";
		String UPDATED_PINCODE_IS_NOT_APPLICABLE_FOR_LC_ASSURED_SERVICE = "UPDATED_PINCODE_IS_NOT_APPLICABLE_FOR_LC_ASSURED_SERVICE";
	}
	
	private String manualDeliveryOptionChangeReason;

	private String manualServiceTypeChangeReason;

	@Transient
	private boolean isNotAvailableUrgent;
	
	@Transient
	private boolean isNotAvailableLfAssured;
	
	@Transient
	private boolean isTeleConsult;
	
	@Transient
	private Set<String> diseases;
	
	@Transient
	private Double refillIndex;
	
	@Transient
	private Double consumptionDays;
	
	@Transient
	private boolean isExcessiveOrderedQuantity;
	
	public boolean isTeleConsult(){
		if (this.cartItems != null && !this.cartItems.isEmpty()) {
			this.isTeleConsult = this.cartItems.parallelStream().allMatch(CartItem::isTeleConsult);
		}
		return this.isTeleConsult;
	}
	
	public Double getRefillIndex(){
		if (this.cartItems != null && !this.cartItems.isEmpty()) {
			List<CartItem> cartItemStream = this.cartItems.parallelStream()
					.filter(cartItem -> cartItem.getRefillIndex() != null).collect(Collectors.toList());
			if (cartItemStream != null && !cartItemStream.isEmpty()) {
				this.refillIndex = cartItemStream.parallelStream().mapToDouble(CartItem::getRefillIndex).average().getAsDouble();
				if (this.refillIndex > 0) {
					this.refillIndex = Precision.round(this.refillIndex, 2);
				}
			}
		}
		return this.refillIndex;
	}
	
	public Set<String> getDiseases() {
		if (this.cartItems != null && !this.cartItems.isEmpty()) {
			this.diseases = this.cartItems.parallelStream()
					.filter(cartItem -> cartItem.getDiseases() != null && !cartItem.getDiseases().isEmpty())
					.map(CartItem::getDiseases)
			        .flatMap(Set::stream)
			        .collect(Collectors.toSet());
		}
		return this.diseases;
	}
	
	public Double getConsumptionDays() {
		if (this.repeatDay == 0 && this.cartItems != null && !this.cartItems.isEmpty()) {
			List<CartItem> cartItemStream  = this.cartItems.parallelStream()
					.filter(cartItem -> cartItem.getConsumptionPerDay() != null).collect(Collectors.toList());
			if (cartItemStream != null && !cartItemStream.isEmpty()) {
				this.consumptionDays = cartItemStream.parallelStream().mapToDouble(CartItem::getConsumptionPerDay).average().getAsDouble();
			}
		} else {
			this.consumptionDays = new Double(this.repeatDay);
		}
		
		return this.consumptionDays;
	}
	
	public boolean isExcessiveOrderedQuantity() {
		
		if (this.cartItems != null && !this.cartItems.isEmpty()) {
			this.isExcessiveOrderedQuantity = this.cartItems.parallelStream().anyMatch(CartItem::isExcessiveOrderedQuantity);
		}
		
		return this.isExcessiveOrderedQuantity;
	}

	public String getPaymentType() {
		return !PAYMENT_METHOD.COD.equalsIgnoreCase(this.paymentMethod) ? PAYMENT_TYPE.PREPAID: PAYMENT_TYPE.COD;
	}

	@Transient
	private List<PaymentChannelData> paymentChannels = new ArrayList<PaymentChannelData>();

	@Transient
	private SellerDetail sellerDetail;
	
	private String businessType;
	
	private String facilityName;
	
	private String businessChannel;
	
	public interface BUSINESS_CHANNEL {
		String ONLINE = "ONLINE";
		String OFFLINE = "OFFLINE";
	}
	
	public interface BUSINESS_TYPE {
		String B2B = "B2B";
		String B2C = "B2C";
	}
	
	private int revisitDay;

	@Transient
	private Timestamp revisitDate;

	public int getRevisitDay() {
		if (revisitDay > 0 && revisitDate == null) {
			Date date = new Date();
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(date);
			calendar.add(Calendar.DAY_OF_YEAR, this.revisitDay);
			this.revisitDate = new Timestamp(calendar.getTimeInMillis());
		}
		return this.revisitDay;
	}

	public void setRevisitDay(int revisitDay) {
		this.revisitDay = revisitDay;
		if (revisitDay > 0 && revisitDate == null) {
			Date date = new Date();
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(date);
			calendar.add(Calendar.DAY_OF_YEAR, this.revisitDay);
			this.revisitDate = new Timestamp(calendar.getTimeInMillis());
		}
	}

	public double getTotalMrp() {
		this.totalMrp = CommonUtil.round(totalMrp, CommonUtil.DEFAULT_PRECISION);
		return this.totalMrp > 0 ? this.totalMrp : 0;

	}

	public double getTotalTaxAmount() {
		this.totalTaxAmount = CommonUtil.round(totalTaxAmount, CommonUtil.DEFAULT_PRECISION);
		return this.totalTaxAmount > 0 ? this.totalTaxAmount : 0;
	}
	
	public String getCategory() {
		if( StringUtils.isBlank(category) ) {
			category = CATEGORY.MEDICINE;
		}
		return category;
	}

	/*public String getPincode() {
		if(this.facilityCode == FACILITY_CODE.FACILITY_101) {
			return PINCODE.DELHI;
		} else {
			return PINCODE.JAIPUR;
		}
	}*/
	
	private int nextRefillDay;
	
	private Timestamp nextRefillDate;

	@Transient
	private Patient patient;
	
	private boolean isReportHardCopyRequired;
	
	private String mobile;
	
	private String email;
	
	private boolean isMembershipAdded;
	
	@Transient
	private double lineTotalAmount;
	
	public double getLineTotalAmount() {
		this.lineTotalAmount = CommonUtil.round(this.getTotalMrp()- this.getDiscount(), CommonUtil.DEFAULT_PRECISION);
		return this.lineTotalAmount > 0 ? this.lineTotalAmount: 0;
	}
}
