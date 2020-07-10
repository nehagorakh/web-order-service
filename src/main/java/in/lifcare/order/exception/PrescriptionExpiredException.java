package in.lifcare.order.exception;

public class PrescriptionExpiredException  extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public PrescriptionExpiredException(String message) {
		super(message);
	}

}
