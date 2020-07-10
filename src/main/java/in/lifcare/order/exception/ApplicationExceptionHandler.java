package in.lifcare.order.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import in.lifcare.core.response.model.Error;
import in.lifcare.core.response.model.Response;

/**
 * 
 * @author Amit Kumar
 * @since 12/4/17
 * @version 0.1.0
 */

@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApplicationExceptionHandler {
	private static Logger logger = LoggerFactory.getLogger(ApplicationExceptionHandler.class);

	@ResponseStatus(value = HttpStatus.NOT_FOUND)
	@ExceptionHandler({ OrderNotFoundException.class, OrderItemNotFoundException.class,
			AddressNotFoundException.class, CartNotFoundException.class, CartItemNotFoundException.class, CartPrescriptionNotFoundException.class })
	public @ResponseBody Response<Error> handleNotFoundException(Exception e) {
		logger.error("ERROR", e);
		return new Response<Error>(new Error(e != null ? e.getClass().getSimpleName() : "NotFoundException", e.getMessage()));
	}
	
	@ResponseStatus(value = HttpStatus.CONFLICT)
	@ExceptionHandler({ InvalidWalletBalanceException.class})
	public @ResponseBody Response<Error> handleWalletBalanceException(Exception e) {
		logger.error("ERROR", e);
		return new Response<Error>(new Error(e != null ? e.getClass().getSimpleName() : "ConflictException", e.getMessage()));
	}

	@ResponseStatus(value = HttpStatus.PRECONDITION_FAILED)
	@ExceptionHandler({ InvalidCartStatusException.class })
	public @ResponseBody Response<Error> handlePreConditionFailedException(Exception e) {
		logger.error("ERROR", e);
		return new Response<Error>(new Error(e != null ? e.getClass().getSimpleName() : "PreConditionFailedException", e.getMessage()));
	}
	
	@ResponseStatus(value = HttpStatus.BAD_REQUEST)
	@ExceptionHandler({ OrderException.class, CartException.class, MaxOrderedQuantityExceeded.class, MaxPermissibleLimitReached.class, PrescriptionExpiredException.class, PrescriptionRxDateNotFound.class, UnserviceablePincodeException.class})
	public @ResponseBody Response<Error> handleOrderException(Exception e) {
		logger.error("ERROR", e);
		return new Response<Error>(new Error(e != null ? e.getClass().getSimpleName() : "BadRequestException", e.getMessage()));
	}
	
	@ResponseStatus(value = HttpStatus.NOT_ACCEPTABLE)
	@ExceptionHandler({ CancelOrderException.class,HoldOrderException.class})
	public @ResponseBody Response<Error> invalidStateException(Exception e) {
		logger.error("ERROR", e);
		return new Response<Error>(new Error(e != null ? e.getClass().getSimpleName() : "NotAcceptableException", e.getMessage()));
	}
	
	@ResponseStatus(value = HttpStatus.PRECONDITION_REQUIRED)
	@ExceptionHandler({ InvalidDeliveryOptionException.class, InvalidServiceTypeException.class })
	public @ResponseBody Response<Error> invalidDeliveryServiceOptionException(Exception e) {
		logger.error("ERROR", e);
		return new Response<Error>(
				new Error(e != null ? e.getClass().getSimpleName() : "PreConditionFailedException", e.getMessage()));
	}
}
