package in.lifcare.order.cart.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CartItemWebEngage implements Serializable {

	private static final long serialVersionUID = 1L;

	private String cartId;

	private Long customerId;

	private String category;

	private Set<String> diseases;

	private String location;
	
	private Integer quantity;
	
	private List<String> name = new ArrayList<>();
	
	private List<String> brand = new ArrayList<>();
	
	private List<String> skus = new ArrayList<>();

	public CartItemWebEngage

	(Builder builder) {

		this.cartId = builder.cartId;

		this.customerId = builder.customerId;

		this.category = builder.category;

		this.diseases = builder.diseases;

		this.location = builder.location;
		
		this.quantity = builder.quantity;
		
		this.name = builder.name;
		
		this.brand = builder.brand;

		this.skus = builder.skus;
	}

	public static class Builder {

		private String cartId;

		private Long customerId;

		private String category;

		private Set<String> diseases;

		private String location;
		
		private Integer quantity;
		
		private List<String> name;
		
		private List<String> brand;
		
		private List<String> skus;

		public Builder() {

		}

		public Builder cartId(String cartId) {

			this.cartId = cartId;

			return this;

		}
		
		public Builder quantity(Integer quantity) {

			this.quantity = quantity;

			return this;

		}

		public Builder customerId(Long customerId) {

			this.customerId = customerId;

			return this;

		}

		public Builder category(String category) {

			this.category = category;

			return this;

		}

		public Builder disease(Set<String> diseases) {

			this.diseases = diseases;

			return this;

		}

		public Builder location(String location) {

			this.location = location;

			return this;

		}
		
		public Builder name(List<String> name) {

			this.name = name;

			return this;

		}
		
		public Builder brand(List<String> brand) {

			this.brand = brand;

			return this;

		}
		
		public Builder skus(List<String> skus) {

			this.skus = skus;

			return this;

		}

		public CartItemWebEngage build() {

			return new CartItemWebEngage(this);

		}

	}

}