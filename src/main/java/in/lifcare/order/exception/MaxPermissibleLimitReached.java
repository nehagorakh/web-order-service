package in.lifcare.order.exception;

public class MaxPermissibleLimitReached  extends RuntimeException {
	
	private static final long serialVersionUID = 7990502869349739421L;
	public MaxPermissibleLimitReached(String message) {
		super(message);
	}
}