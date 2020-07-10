package in.lifcare.order.exception;

/**
 * 
 * @author Manoj-Mac
 * @since 31-05-17
 */
public class OrderItemNotFoundException extends RuntimeException {

	private static final long serialVersionUID = -5591838319589926743L;

	public OrderItemNotFoundException(String message) {
		super(message);
	}
}
