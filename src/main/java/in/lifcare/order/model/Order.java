package in.lifcare.order.model;

import java.io.Serializable;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.Min;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.util.Precision;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.validator.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import in.lifcare.core.audit.AuditEntityListener;
import in.lifcare.core.audit.annotation.CreatedByName;
import in.lifcare.core.audit.annotation.UpdatedByName;
import in.lifcare.core.constant.OrderStatus;
import in.lifcare.core.model.Appointment;
import in.lifcare.core.model.User;
import in.lifcare.core.model.Order.PROCUREMENT_TYPE;
import in.lifcare.core.util.CommonUtil;
import in.lifcare.order.microservice.account.patient.model.Patient;
import in.lifcare.order.microservice.catalog.model.Medicine;
import in.lifcare.order.property.constant.OrderPropertyConstant;
import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@Entity
@Table(name = "\"order\"")
@JsonIgnoreProperties(ignoreUnknown = true)
@EntityListeners({AuditEntityListener.class})
public class Order implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1554386768566183081L;

	@Id
	@Column(updatable = false)
	@GeneratedValue
	private long id;

	private long parentId = 0;

	private String displayOrderId;

	@Column(name = "\"status\"")
	private String status;

	private String state;

	@Min(value = 1, message = "not valid customer_id")
	private long customerId;

	private String customerFirstName;

	private String customerLastName;

	@Min(value = 1, message = "not valid patient_id")
	private long patientId;

	private String patientFirstName;

	private String patientLastName;

	private String orderType;

	@Column(name = "\"source\"")
	@NotBlank
	private String source;

	private String packagingType;

	private float totalSalePrice;

	@Transient
	private float totalPayableAmount;

	@Transient
	private float remainingPayableAmount;
	
	private float totalMrp;

	private float totalTaxAmount;

	private float couponDiscount;

	private float couponCashback;

	@Transient
	private float cashbackPercentage;

	private String couponCode;
	
	private String manualCouponCode;

	private String orderNumber;

	@CreationTimestamp
	@Column(updatable = false)
	private Timestamp createdAt;

	@UpdateTimestamp
	private Timestamp updatedAt;

	private String createdBy;

	private String updatedBy;
	
	private String mergedBy;
	
	private String mergedByName;

	private Timestamp promisedDeliveryDate;
	
	@Transient
	private int estimateMinDeliveryDay;
	
	private String procurementType = PROCUREMENT_TYPE.NORMAL;

	public int getEstimateMinDeliveryDay() {
		if (this.getPromisedDeliveryDate() != null && this.estimateMinDeliveryDay == 0) {
			LocalDate deliveryDate = this.getPromisedDeliveryDate().toLocalDateTime().toLocalDate();
			LocalDate currentDate = LocalDate.now();
			this.estimateMinDeliveryDay = new Long(ChronoUnit.DAYS.between(deliveryDate, currentDate)).intValue();
		}
		this.estimateMinDeliveryDay = this.estimateMinDeliveryDay <= 0 ? 2 : this.estimateMinDeliveryDay;
		return estimateMinDeliveryDay;
	}

	@Transient
	private int estimateMaxDeliveryDay;

	public int getEstimateMaxDeliveryDay() {
		if (this.getPromisedDeliveryDate() != null && this.estimateMinDeliveryDay == 0) {
			LocalDate deliveryDate = this.getPromisedDeliveryDate().toLocalDateTime().toLocalDate();
			LocalDate currentDate = LocalDate.now();
			this.estimateMinDeliveryDay = new Long(ChronoUnit.DAYS.between(deliveryDate, currentDate)).intValue();
		}
		this.estimateMinDeliveryDay = this.estimateMinDeliveryDay <= 0 ? 2 : this.estimateMinDeliveryDay;
		return this.estimateMinDeliveryDay + 1;
	}

	private int repeatDay;

	@Transient
	private int displayRepeatDay;

	private boolean isRepeat = false;

	private boolean isPrescriptionProvided = false;

	private String comment;

	private Time promisedDeliveryBeginTime;

	private Time promisedDeliveryEndTime;

	@Transient
	private long shippingAddressId;

	@Transient
	private Timestamp repeatDate;

	@Transient
	private List<Payment> payments;

	@Transient
	private List<Long> prescriptionIds;

	@Transient
	private Patient patient;

	@Transient
	private ShippingAddress shippingAddress;

	@Transient
	private List<OrderItem> orderItems;

	@Transient
	private List<OrderPrescription> orderPrescriptions;

	@Transient
	private String promisedDeliveryTime;

	public String getDisplayOrderId() {
		if (StringUtils.isEmpty(displayOrderId)) {
			displayOrderId = String.valueOf(id);
		}
		return displayOrderId;
	}

	public int getDisplayRepeatDay() {

		if (nextRefillDay > 10) {
			displayRepeatDay = nextRefillDay - 3;
		} else if (nextRefillDay <= 10 && nextRefillDay > 0) {
			displayRepeatDay = nextRefillDay - 2;
		}
		return displayRepeatDay;
	}

	public String getPromisedDeliveryTime() {
		if (StringUtils.isEmpty(promisedDeliveryTime) && promisedDeliveryBeginTime != null
				&& promisedDeliveryEndTime != null) {
			promisedDeliveryTime = promisedDeliveryBeginTime + " - "
					+ promisedDeliveryEndTime;
		}
		return promisedDeliveryTime;
	}

	private int redeemedCarePoints;

	private float redeemedCouponCashback;

	private float totalDiscount;

	private float discount;
	
	private boolean isManualHold = false;

	private String manualHoldReason;

	private float offsetScore = 0;

	private String channel;

	private float score;

	private String statusComment;

	private boolean isDoctorCallback = false;

	private int facilityCode;

	private boolean isShippingChargeExempted;

	@Transient
	private float couponDiscountPercentage;
	
	private String couponDescription;
	
	private String shortCouponDescription;

	private Timestamp dispatchDate;

	private float shippingCharge;

	private String courierType;

	private String trackingNumber;

	public interface PACKAGING_TYPE {
		String BOX = "Box";
		String BAG = "Bag";
		String TEMPER_PROOF = "Temper Proof";
		String DEFAULT = "Box";
	}
	
	public interface PROCUREMENT_TYPE {
		String BULK = "BULK";
		String NORMAL = "NORMAL";
	}

	public static List<String> PACKAGING_PRIORITY = Stream
			.of(PACKAGING_TYPE.TEMPER_PROOF, PACKAGING_TYPE.BAG, PACKAGING_TYPE.BOX).collect(Collectors.toList());

	private String preferredDeliveryOption;

	private String deliveryOption;

	private String deliveryOptionChangeReason;

	private String preferredServiceType;

	private String serviceType;

	private String serviceTypeChangeReason;

	private double urgentDeliveryCharge;
	
	private double reportDeliveryCharge;
	
	private float redeemedCash;

	public interface DELIVERY_OPTION {
		String URGENT = "URGENT";
		String NORMAL = "NORMAL";
	}

	public interface SERVICE_TYPE {
		String LF_URGENT_ASSURED = "LF_ASSURED";
		String LF_ASSURED = "LF_ASSURED";
		String NORMAL = "NORMAL";
	}

	public interface ORDER_DELIVERY_TYPE {
		String IS_NORMAL_DELIVERY = "is_normal_delivery";
		String IS_LC_ASSURED_DELIVERY = "is_lc_assured_delivery";
		String IS_URGENT_DELIVERY = "is_urgent_delivery";
	}
	
	private String manualDeliveryOptionChangeReason;
	
	private String manualServiceTypeChangeReason;
	
	@Transient
	private float lineTotalAmount;
	
	private Double externalInvoiceAmount;
	
	@Transient
	private boolean isBestCoupon;

	public boolean isBestCoupon() {
		if (StringUtils.isNotBlank(this.couponCode)) {
			return !this.couponCode.equalsIgnoreCase(this.manualCouponCode);
		} else {
			return true;
		}
	}
	
	private boolean isTeleConsult;
	
	private boolean isAutoVerificationFailed;
	
	private double refillIndex;
	
	@Transient
	private Set<String> diseases;
	
	public Set<String> getDiseases() {
		if (this.orderItems != null && !this.orderItems.isEmpty()) {
			this.diseases = this.orderItems.parallelStream()
					.filter(orderItem1 -> orderItem1.getDiseases() != null && !orderItem1.getDiseases().isEmpty() && orderItem1.isActive())
					.map(OrderItem::getDiseases)
			        .flatMap(Set::stream)
			        .collect(Collectors.toSet());
		}
		return this.diseases;
	}
//	@SuppressWarnings("unchecked")
//	public Set<String> getDiseases() {
//		Set<String> dis = new HashSet<>();
//		if (this.orderItems != null && !this.orderItems.isEmpty()) {
//			dis = new HashSet<>(this.orderItems.parallelStream()
//					.filter(orderItem1 -> orderItem1.getAdditionalProperties().containsKey("diseases") && orderItem1.getAdditionalProperties().get("diseases") != null
//							&& ((Map<String, Object>) orderItem1.getAdditionalProperties().get("diseases")).containsKey("value") && !((List<String>) ((Map<String, Object>) orderItem1.getAdditionalProperties().get("diseases")).get("value")).isEmpty()
//							&& orderItem1.isActive())
//					.map(oi -> (List<String>) ((Map<String, Object>) oi.getAdditionalProperties().get("diseases")).get("value"))
//					.flatMap(List::stream).collect(Collectors.toList()));
//		}
//		this.diseases = dis;
//		return this.diseases;
//	}
	
	public boolean isTeleConsult(){
		if (this.orderItems != null && !this.orderItems.isEmpty()) {
			this.isTeleConsult = this.orderItems.parallelStream()
					.filter(OrderItem::isActive).allMatch(OrderItem::isTeleConsult);
		}
		return this.isTeleConsult;
	}
	
	public Double getRefillIndex(){
		if (this.orderItems != null && !this.orderItems.isEmpty()) {
			List<OrderItem> orderItemStream = this.orderItems.parallelStream()
					.filter(orderItem1 -> orderItem1.getRefillIndex() != null && orderItem1.isActive()).collect(Collectors.toList());
			if (orderItemStream != null && !orderItemStream.isEmpty()) {
				this.refillIndex = orderItemStream.parallelStream().mapToDouble(OrderItem::getRefillIndex).average().getAsDouble();
				if (this.refillIndex  > 0) {
					this.refillIndex = Precision.round(this.refillIndex, 2);
				}
			}
		}
		return this.refillIndex;
	}
	
	public void setOrderItems(List<OrderItem> orderItems) {
		this.orderItems = orderItems;
		if (this.orderItems != null && !this.orderItems.isEmpty()) {
			this.repeatDay = calculateRepeatDay();
			System.out.println("order set repeat day after calculate from item consumption : "+  this.repeatDay );
		}
	}
	
	private int calculateRepeatDay() {
		Double consumptionDay = 0.0;
		List<OrderItem> orderItemStream = this.orderItems.parallelStream().filter(OrderItem::isActive).collect(Collectors.toList());
		for (OrderItem orderItem : orderItemStream) {
			if (StringUtils.isNotBlank(orderItem.getPackType())) {
				if (Medicine.PACK_TYPE_TAB.ALL.contains(orderItem.getPackType()) && orderItem.getConsumptionPerDay() > 0) {
					double consumptionPerDay = Math.ceil((orderItem.getQuantity() * orderItem.getPerPackQty() ) / orderItem.getConsumptionPerDay() );
					if (consumptionDay < consumptionPerDay) {
						consumptionDay = consumptionPerDay;
					}
				} else {
					if (consumptionDay < (OrderPropertyConstant.DEFAULT_CONSUMPTION != null ? OrderPropertyConstant.DEFAULT_CONSUMPTION : 10.0)) {
						consumptionDay = OrderPropertyConstant.DEFAULT_CONSUMPTION;
					}
				}
			}
			System.out.println("order_item set : "+orderItem.getSku() + ", " +  consumptionDay );
		}
		return consumptionDay.intValue();
	}

	public void calculateCouponCashback() {
		couponCashback = Float.parseFloat(String.valueOf(CommonUtil.round(calculateTotalPrice() * cashbackPercentage * 0.01, CommonUtil.DEFAULT_PRECISION)));
	}

	public void setTotalSalePrice(float totalSalePrice) {
		//this.totalSalePrice = Math.round(totalSalePrice * 100f) / 100f;
		this.totalSalePrice = CommonUtil.round(totalSalePrice, 2);
	}

	public float getTotalSalePrice() {
		//this.totalSalePrice = Math.round(totalSalePrice * 100f) / 100f;
		this.totalSalePrice = CommonUtil.round(totalSalePrice, 2);
		return this.totalSalePrice > 0 ? this.totalSalePrice: 0;
	}
	/**
	 * Do not change this method (Used Many Places to calculate total sale price)
	 * @return
	 */
	public float calculateTotalPrice() {
		float totalPrice = (float) (this.getTotalMrp() + this.getShippingCharge() + this.getReportDeliveryCharge() - this.getCouponDiscount() - this.getDiscount() - this.getRedeemedCouponCashback()
				+ this.getUrgentDeliveryCharge() - this.getRedeemedCarePoints() - this.getRedeemedCash());
		return totalPrice > 0 ? totalPrice: 0;
	}

	public float getCustomerAmountToPay() {
		float amountToPay = new Float(this.getTotalMrp() + this.getShippingCharge() - this.getCouponDiscount() - this.getDiscount()
				+ this.getUrgentDeliveryCharge());
		//amountToPay = Math.round(amountToPay * 100f) / 100f;
		amountToPay = CommonUtil.round(amountToPay, 2);
		return amountToPay > 0 ? amountToPay: 0;
	}
	
	public float getTotalPayableAmount() {
		//this.totalPayableAmount = Math.round(this.calculateTotalPrice() * 100f) / 100f;
		this.totalPayableAmount = CommonUtil.round(this.calculateTotalPrice(), 2);
		return this.totalPayableAmount > 0 ? this.totalPayableAmount: 0;
	}

	public float getRemainingPayableAmount() {
		float remainingAmt = (float) (this.getTotalPayableAmount() - this.getGatewayAmount());
		//remainingAmt = Math.round(remainingAmt * 100f) / 100f;
		remainingAmt = CommonUtil.round(remainingAmt, 2);
		return remainingAmt > 0 ? remainingAmt: 0;
	}

	public double getGatewayAmount() {
		if( OrderStatus.STATE_NEW.PAYMENT_PENDING.equals(this.status) || OrderStatus.STATE_NEW.PAYMENT_FAILED.equals(this.status) ) {
			this.gatewayAmount = 0;
		}
		//this.gatewayAmount = Math.round(gatewayAmount * 100D) / 100D;
		this.gatewayAmount = CommonUtil.round(gatewayAmount, CommonUtil.DEFAULT_PRECISION);
		return gatewayAmount > 0 ? gatewayAmount: 0;
	}

	public void setPaymentMethod(String paymentMethod) {
		this.paymentMethod = paymentMethod;
		this.orderType = !ORDER_TYPE.COD.equalsIgnoreCase(paymentMethod) ? ORDER_TYPE.PREPAID: ORDER_TYPE.COD;
	}

	public String getPaymentMethod() {
		return StringUtils.isNotBlank(this.paymentMethod) ? this.paymentMethod: ORDER_TYPE.COD;
	}
	
	private long sellerId;

	private String sellerName;

	private String paymentMethod;

	private String paymentSubMethod;

	private double podAmount;

	private double gatewayAmount;

	@Transient
	private int paymentConfirmationTime;

	@Transient
	private int paymentCancellationTime;	

	@Transient
	private String customerCareNumber;

	private String category = Order.CATEGORY.MEDICINE;

    private Timestamp appointmentDate;

	private String appointmentSlot;
	
	private Long appointmentSlotId;

	private Long appointmentId;

	@Transient
	private long hubId;

	@Transient
	private String hubName;

	@Transient
	private long spokeId;

	@Transient
	private String spokeName;
	
	@Transient
	private String courierProvider;
	
	@Transient
	private String consigneePincode;

	@Transient
	private String consigneeState;

	@Transient
	private String consigneeCity;
	
	public int getPaymentConfirmationTime() {
		return OrderPropertyConstant.PAYMENT_CONFIRMATION_TIME;
	}

	public int getPaymentCancellationTime() {
		return OrderPropertyConstant.PAYMENT_CANCELLATION_TIME;
	}	
	
	public String getCustomerCareNumber() {
		if (this.facilityCode == 100) {
			return OrderPropertyConstant.CUSTOMER_CARE_NUMBER_FACILITY_100;
		} else {
			return OrderPropertyConstant.CUSTOMER_CARE_NUMBER_FACILITY_101;
		}
	}

	public interface CATEGORY {
		String LAB = "LAB";
		String MEDICINE = "MEDICINE";
	}
	
	public interface ORDER_TYPE {
		String COD = "COD";
		String PREPAID = "PREPAID";
		List<String> VALID_ORDER_TYPE_LIST = new ArrayList<String>() {
			{
				add(COD);
				add(PREPAID);
			}
		};
	}
	
	private int revisitDay;
	
	@Transient
	private Timestamp revisitDate;
	
	private String facilityName;
	
	private String deliveryType;
	
	private String businessType;
	
	private String businessChannel;
	
	private long mergeWithId;
	
	public interface DELIVERY_TYPE {
		String SELF_PICKUP = "SELF_PICKUP";
		String HOME_DELIVERY = "HOME_DELIVERY";
	}
		
	public interface BUSINESS_CHANNEL {
		String ONLINE = "ONLINE";
		String OFFLINE = "OFFLINE";
	}
	
	public interface BUSINESS_TYPE {
		String B2B = "B2B";
		String B2C = "B2C";
	}
	
	public interface SOURCE {

		String POS = "POS";
		
	}
	
	public float getTotalTaxAmount() {
		this.totalTaxAmount = CommonUtil.round(totalTaxAmount, CommonUtil.DEFAULT_PRECISION);
		return this.totalTaxAmount > 0 ? this.totalTaxAmount: 0;
	}
	
	public float getCouponDiscount() {
		this.couponDiscount = CommonUtil.round(couponDiscount, CommonUtil.DEFAULT_PRECISION);
		return this.couponDiscount > 0 ? this.couponDiscount: 0;
	}
	
	public float getTotalDiscount() {
		this.totalDiscount = CommonUtil.round(totalDiscount, CommonUtil.DEFAULT_PRECISION);
		return this.totalDiscount > 0 ? this.totalDiscount: 0;
	}
	
	public float getDiscount() {
		this.discount = CommonUtil.round(discount, CommonUtil.DEFAULT_PRECISION);
		return this.discount > 0 ? this.discount: 0;
	}
	
	public float getLineTotalAmount() {
		this.lineTotalAmount = CommonUtil.round(lineTotalAmount, CommonUtil.DEFAULT_PRECISION);
		return this.lineTotalAmount > 0 ? this.lineTotalAmount: 0;
	}
	
	public Double getExternalInvoiceAmount() {
		this.externalInvoiceAmount = CommonUtil.round(externalInvoiceAmount, CommonUtil.DEFAULT_PRECISION);
		return ( this.externalInvoiceAmount != null && this.externalInvoiceAmount > 0) ? this.externalInvoiceAmount: 0;
	}
	
	public double getPodAmount() {
		this.podAmount = CommonUtil.round(podAmount, CommonUtil.DEFAULT_PRECISION);
		return this.podAmount > 0 ? this.podAmount: 0;
}

	private int nextRefillDay;
	
	@Transient
	private Timestamp nextRefillDate;
	
	@Transient
	private List<Order> associateOrders;
	
	@Transient
	private List<Order> mergedOrders;
	
	public Timestamp getNextRefillDate() {
		if( nextRefillDay > 0 && nextRefillDate == null ) {
//			java.util.Date date = new java.util.Date();
//			Calendar calendar = Calendar.getInstance();
//			calendar.setTime(date);
//			calendar.add(Calendar.DAY_OF_YEAR, this.nextRefillDay);
//			this.nextRefillDate = new Timestamp(calendar.getTimeInMillis());
			LocalDateTime localdate = LocalDateTime.now();
			LocalDateTime nextdate = localdate.plusDays(this.nextRefillDay);
			this.nextRefillDate = Timestamp.valueOf(nextdate);
		}
		return this.nextRefillDate;
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

	public float getTotalMrp() {
		this.totalMrp = CommonUtil.round(totalMrp, CommonUtil.DEFAULT_PRECISION);
		return this.totalMrp > 0 ? this.totalMrp : 0;
	}
	
	@Transient
	private Appointment appointment;
	
	private boolean isReportHardCopyRequired;
	
	@CreatedByName
	private String createdByName;

	@UpdatedByName
	private String updatedByName;
	
	@Transient
	private String email;
	
	@Transient
	private String mobile;
	
	@Transient
	private User user;

	@Transient
	private long delayBy;
	
	private boolean isMembershipAdded;

	@Transient
	private String facilityCallbackMobile;
	
	@Transient
	private boolean isFulfillable;

	public long getDelayBy() {
		Timestamp promisedDeliveryDate = getPromisedDeliveryDate();
		long promisedDeliveryMilliseconds = promisedDeliveryDate != null && promisedDeliveryDate.getTime() > 0
				? promisedDeliveryDate.getTime()
				: 0;
		this.delayBy = getDateDiff(promisedDeliveryMilliseconds, System.currentTimeMillis(), TimeUnit.DAYS);
		return delayBy;
	}

	public long getDateDiff(long timeUpdate, long timeNow, TimeUnit timeUnit) {
		if (timeNow > timeUpdate) {
			long diffInMillies = Math.abs(timeNow - timeUpdate);
			return timeUnit.convert(diffInMillies, TimeUnit.MILLISECONDS);
		}
		return 0l;
	}
	
	public interface DEFAULT_VALUES{
		long DEFAULT_CSTOMER_ID = 10000000L;
		long DEFAULT_PATIENT_ID = 19999999L;
	}
}
