package in.lifcare.order.exception;

public class InvalidCartStatusException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public InvalidCartStatusException(String message) {
		super(message);
	}
}
