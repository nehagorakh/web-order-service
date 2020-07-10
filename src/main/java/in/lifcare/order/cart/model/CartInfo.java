package in.lifcare.order.cart.model;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)				
public class CartInfo implements Serializable {
	
	private static final long serialVersionUID = 1L;
	private String cartUid;
	private List<CartItemWebEngage> cartItems;
	private long customerId;
	private Date orderDate;
	private String source;
	private String category;
	private String status;
	private String promoCode;
	private Float totalPayableAmt;
	private String paymentMode;
	private String currency;
	private String orderId;
	
	public CartInfo() {
		//Default Constructor
	}
	
	public CartInfo(Builder builder) {
		this.cartUid = builder.cartUid;
		this.cartItems = builder.cartItems;
		this.customerId = builder.customerId;
		this.orderDate = builder.orderDate;
		this.source = builder.source;
		this.category = builder.category;
		this.status = builder.status;
		this.promoCode = builder.promoCode;
		this.totalPayableAmt = builder.totalPayableAmt;
		this.paymentMode = builder.paymentMode;
		this.currency = builder.currency;
		this.orderId = builder.orderId;
	}

	@Data
	public static class Builder{
		private String cartUid;
		private List<CartItemWebEngage> cartItems;
		private long customerId;
		private Date orderDate;
		private String source;
		private String category;
		private String status;
		private String promoCode;
		private Float totalPayableAmt;
		private String paymentMode;
		private String currency;
		private String orderId;
		
		public Builder() {
			
		}
		
		public Builder cartUid(String cartUid ) {
			this.cartUid = cartUid;
			return this;
		}
		
		public Builder orderId(String orderId ) {
			this.orderId = orderId;
			return this;
		}
		public Builder category(String category ) {
			this.category = category;
			return this;
		}
		
		public Builder cartItems(List<CartItemWebEngage> cartItems) {
			this.cartItems = cartItems;
			return this;
		}
		
		public Builder customerId(long customerId) {
			this.customerId = customerId;
			return this;
		}
		public Builder orderDate(Date orderDate) {
			this.orderDate = orderDate;
			return this;
		}
		public Builder source(String source) {
			this.source = source;
			return this;
		}
		
		public Builder status(String status) {
			this.status = status;
			return this;
		}
		
		public Builder promoCode(String promoCode) {
			this.promoCode = promoCode;
			return this;
		}
		
		public Builder totalPayableAmt(Float totalPayableAmt) {
			this.totalPayableAmt = totalPayableAmt;
			return this;
		}
		
		public Builder paymentMode(String paymentMode) {
			this.paymentMode = paymentMode;
			return this;
		}
		
		public Builder currency(String currency) {
			this.currency = currency;
			return this;
		}
		
		public CartInfo build() {
			return new CartInfo(this);
		}
	}
}
