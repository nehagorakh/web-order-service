package in.lifcare.order.model;

import java.io.Serializable;
import java.sql.Timestamp;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@Entity
@Table(name = "payment")
public class Payment  implements Serializable {
	public interface MODE {
		String CREDIT_CARD = "CREDIT_CARD";
		String DEBIT_CARD = "DEBIT_CARD";
		String WALLET = "WALLET";
		String COD = "COD";
		String CASH = "CASH";
	}

	@Id
	@Column(updatable = false)
	@GeneratedValue
	private long id;

	private long orderId;

	private String paymentMode;

	private float amount;

	private String refTransactionId;

	private String bankName;

	@CreationTimestamp
	@Column(updatable = false)
	private Timestamp createdAt;

	@UpdateTimestamp
	private Timestamp updatedAt;

}
