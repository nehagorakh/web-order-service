package in.lifcare.order.model;

import java.util.List;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * 
 * @author Manoj-Mac
 *
 */

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class OrderUpdateEvent {

	private List<OrderItemArray> orderItemArray;

	private long totalSalePrice;

	private long totalMrp;
	
	private String orderNumber;
	
	private double redeemedCarePoints;

	public double getRedeemedCarePoints() {
		return redeemedCarePoints;
	}

	public void setRedeemedCarePoints(double redeemedCarePoints) {
		this.redeemedCarePoints = redeemedCarePoints;
	}

	public String getOrderNumber() {
		return orderNumber;
	}

	public void setOrderNumber(String orderNumber) {
		this.orderNumber = orderNumber;
	}

	public List<OrderItemArray> getOrderItemArray() {
		return orderItemArray;
	}

	public void setOrderItemArray(List<OrderItemArray> orderItemArray) {
		this.orderItemArray = orderItemArray;
	}

	public long getTotalSalePrice() {
		return totalSalePrice;
	}

	public void setTotalSalePrice(long totalSalePrice) {
		this.totalSalePrice = totalSalePrice;
	}

	public long getTotalMrp() {
		return totalMrp;
	}

	public void setTotalMrp(long totalMrp) {
		this.totalMrp = totalMrp;
	}

}
