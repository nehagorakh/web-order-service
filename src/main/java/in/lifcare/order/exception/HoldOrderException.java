package in.lifcare.order.exception;

/**
 * 
 * @author Amit Kumar
 * @since 12/4/17
 * @version 0.1.0
 */

public class HoldOrderException extends RuntimeException {

	private static final long serialVersionUID = 7990502869349739421L;

	public HoldOrderException(String message) {
		super(message);
	}

}
