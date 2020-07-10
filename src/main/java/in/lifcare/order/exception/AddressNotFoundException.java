package in.lifcare.order.exception;

/**
 * 
 * @author Manoj-Mac
 * @since 31-05-17
 */
public class AddressNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 825243729971541969L;

	public AddressNotFoundException(String message) {
		super(message);
	}
}
