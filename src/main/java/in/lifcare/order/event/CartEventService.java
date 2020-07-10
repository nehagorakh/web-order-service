package in.lifcare.order.event;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.stereotype.Service;

import in.lifcare.core.constant.CartEvent;
import in.lifcare.core.model.CartInfoEventData;
import in.lifcare.core.model.User;
import in.lifcare.core.util.CommonUtil;
import in.lifcare.order.cart.model.CartInfo;
import in.lifcare.order.cart.model.CartItem;
import in.lifcare.order.cart.model.CartItemWebEngage;
import in.lifcare.producer.kafka.KafkaProducer;

@Service
public class CartEventService {

	@Async
	public void addCartItems(Long customerId, String cartUid, List<CartItem> cartItems, String defaultLocation, List<String> name, List<String> brand, List<String> skus, int quantity) {
		List<CartItemWebEngage> cartItemList = new ArrayList<>();
		for (CartItem cartItem : cartItems) {
			CartItemWebEngage cartInfoCartItem = new CartItemWebEngage.Builder().cartId(cartItem.getCartUid())
					.category(cartItem.getClassification()).customerId(customerId).disease(cartItem.getDiseases()).location(defaultLocation).brand(brand).name(name).skus(skus).quantity(quantity).build();
			cartItemList.add(cartInfoCartItem);
		}
		CartInfo cartInfo = new CartInfo.Builder().customerId(customerId).cartUid(cartUid).cartItems(cartItemList).build();
		pushMessage(cartUid, CartEvent.ADD_TO_CART, cartInfo);

	}

	@Async
	public void addPrescription(OAuth2Authentication auth,String cartUid) {
		User user = CommonUtil.getOauthUser(auth);
		if(user!=null) {
			CartInfo cartInfo = new CartInfo.Builder().cartUid(cartUid).customerId(user.getId()).build();
			pushMessage(cartUid, CartEvent.ADD_PRESCRIPTION, cartInfo);	
		}
		
	}

	@Async
	public void assignPatient(OAuth2Authentication auth, String cartUid) {
		User user = CommonUtil.getOauthUser(auth);
		if (user != null) {
			CartInfo cartInfo = new CartInfo.Builder().cartUid(cartUid).customerId(user.getId()).build();
			pushMessage(cartUid, CartEvent.ASSIGN_PATIENT, cartInfo);
		}
	}

	@Async
	public void addShippingAddress(OAuth2Authentication auth, String cartUid) {
		User user = CommonUtil.getOauthUser(auth);
		if (user != null) {
			CartInfo cartInfo = new CartInfo.Builder().cartUid(cartUid).customerId(user.getId()).build();
			pushMessage(cartUid, CartEvent.SELECT_SHIPPING_ADDRESS, cartInfo);
		}
	}

	@Async
	public void applyPromo(String promoCode, String cartUid, Long customerId, String category, String status) {
		CartInfo cartInfo = new CartInfo.Builder().promoCode(promoCode).cartUid(cartUid).customerId(customerId)
				.category(category).status(status).build();
		pushMessage(cartUid, CartEvent.OFFER_AVAILED, cartInfo);
	}

	@Async
	public void refillMedicineCartEvent(Long customerId,String url,String cartUid) {
		CartInfo cartInfo = new CartInfo.Builder().customerId(customerId).cartUid(cartUid).build();
		pushMessage(cartUid, CartEvent.REFILL_MEDICINE_CART_EVENT, cartInfo);
	}
	
	@Async
	public void pushMessage(String id, String eventName, CartInfo cartInfo) {
		CartInfoEventData eventData = new CartInfoEventData();
		eventData.setData(cartInfo);
		eventData.setEventType(eventName);
		eventData.setId(id);
		eventData.setRequestedAt(new Date());
		try {
			kafkaProducer.processMessage(eventData);
		} catch (Exception e) {
			LOGGER.debug("Error occured for cartId : {} for Event {} with Exception ", id, eventName, e);
		}
	}

	@Autowired
	private KafkaProducer kafkaProducer;
	private static final Logger LOGGER = LoggerFactory.getLogger(CartEventService.class);

}
