package in.lifcare.order.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@Entity
@Table(name = "\"reason\"")
public class Reason{

	@Id
	@Column(updatable = false)
	@GeneratedValue
	@JsonIgnore
	private int id;

	private String reason;

	@JsonIgnore
	private String group;

	@JsonIgnore
	private String description;

	@JsonIgnore
	private int priority;

	public interface GROUP {
		String HOLD = "hold";
		String UNHOLD = "unhold";
		String CANCEL = "cancel";
		String RETURN = "return";
		String LAB_CANCEL = "lab-cancel";
	}

}
