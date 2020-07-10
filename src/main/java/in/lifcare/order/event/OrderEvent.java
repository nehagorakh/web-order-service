package in.lifcare.order.event;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import in.lifcare.core.model.EventData;
import in.lifcare.core.model.OrderInfo;
import in.lifcare.order.model.Order;
import in.lifcare.producer.exception.TopicNotFound;
import in.lifcare.producer.kafka.KafkaProducer;

@Service
public class OrderEvent {
	
	public void updateOrderEvent(Order order, String transactionId, String type) throws TopicNotFound, Exception {
		EventData eventData = new EventData();
		OrderInfo orderInfo = new OrderInfo();
		orderInfo.setCustomerId(order.getCustomerId());
		orderInfo.setId(order.getId());
		orderInfo.setTotalMrp(order.getTotalMrp());
		orderInfo.setRedeemedCarePoint(order.getRedeemedCarePoints());
		orderInfo.setStatus(order.getStatus());
		orderInfo.setState(order.getState());
		orderInfo.setPaymentType(order.getOrderType());
		orderInfo.setDoctorCallback(order.isDoctorCallback());
		orderInfo.setTrackingNumber(order.getTrackingNumber());
		orderInfo.setCourierType(order.getCourierType());
		Long orderId = order.getId();
		eventData.setData(orderInfo);
		eventData.setReferenceId(transactionId);
		eventData.setId(orderId.toString());
		eventData.setRequestedAt(new Date());
		eventData.setEventType(type);
		kafkaProducer.processMessage(eventData);
	}
	
	public void createOrderEvent(Order order, String transactionId, String type) throws TopicNotFound, Exception {
		EventData eventData = new EventData();
		OrderInfo orderInfo = new OrderInfo();
		orderInfo.setCustomerId(order.getCustomerId());
		orderInfo.setId(order.getId());
		orderInfo.setTotalMrp(order.getTotalMrp());
		orderInfo.setRedeemedCarePoint(order.getRedeemedCarePoints());
		orderInfo.setStatus(order.getStatus());
		orderInfo.setState(order.getState());
		orderInfo.setDoctorCallback(order.isDoctorCallback());
		orderInfo.setPaymentType(order.getOrderType());
		orderInfo.setTrackingNumber(order.getTrackingNumber());
		orderInfo.setCourierType(order.getCourierType());
		Long orderId = order.getId();
		eventData.setData(orderInfo);
		eventData.setReferenceId(transactionId);
		eventData.setId(orderId.toString());
		eventData.setRequestedAt(new Date());
		eventData.setEventType(type);
		kafkaProducer.processMessage(eventData);
	}

	@Autowired
	private KafkaProducer kafkaProducer;

}
