package in.lifcare.order.exception;

public class MaxOrderedQuantityExceeded extends RuntimeException{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public MaxOrderedQuantityExceeded(String msg) {
		super(msg);
	}
}