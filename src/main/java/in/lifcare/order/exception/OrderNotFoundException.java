package in.lifcare.order.exception;

/**
 * 
 * @author Amit Kumar
 * @since 12/4/17
 * @version 0.1.0
 */

public class OrderNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 7990502869349739421L;

	public OrderNotFoundException(String message) {
		super(message);
	}

}
