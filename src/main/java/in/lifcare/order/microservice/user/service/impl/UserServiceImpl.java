package in.lifcare.order.microservice.user.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import in.lifcare.core.constant.APIEndPoint;
import in.lifcare.core.exception.BadRequestException;
import in.lifcare.core.model.Appointment;
import in.lifcare.core.model.User;
import in.lifcare.core.response.model.Response;
import in.lifcare.core.util.MicroserviceClient;
import in.lifcare.order.microservice.user.model.RescheduleAppointment;
import in.lifcare.order.microservice.user.service.UserService;

@Service
public class UserServiceImpl implements UserService {

	@Autowired
	private MicroserviceClient<Response>  microserviceClient;
	
	@Override
	public Appointment createAppointment(Appointment appointment) {
		if (appointment == null) {
			throw new IllegalArgumentException("Invalid param provided!");
		}
		try {
			Response response = microserviceClient.postForObject(APIEndPoint.USER_SERVICE + "/appointment", appointment, Response.class);
			if (response != null) {
				return (Appointment) response.populatePayloadUsingJson(Appointment.class);
			}
			throw new BadRequestException("Appointment not created for order-id : " + appointment.getOrderId());
		} catch (Exception e) {
			throw e;
		}
	}

	@Override
	public Appointment cancelAppointment(long appointmentId) {
		if (appointmentId <= 0) {
			throw new IllegalArgumentException("Invalid param provided!");
		}
		try {
			Response response = microserviceClient.patchForObject(APIEndPoint.USER_SERVICE + "/appointment/" + appointmentId + "/cancel", null, Response.class);
			if (response != null) {
				return (Appointment) response.populatePayloadUsingJson(Appointment.class);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		throw new BadRequestException("Appointment not cancelled for appointment-id : " + appointmentId);
	}

	@Override
	public Appointment updateAppointment(long appointmentId, RescheduleAppointment rescheduleAppointment) {
		if (appointmentId <= 0 || rescheduleAppointment == null) {
			throw new IllegalArgumentException("Invalid param provided!");
		}
		Response response = microserviceClient.patchForObject(APIEndPoint.USER_SERVICE + "/appointment/" + appointmentId + "/reschedule", rescheduleAppointment, Response.class);
		if (response != null) {
			return (Appointment) response.populatePayloadUsingJson(Appointment.class);
		}
		throw new BadRequestException("Appointment not rescheduled for appointment-id : " + appointmentId);
	}

	@Override
	public Appointment getAppointment(long appointmentId) {
		if (appointmentId > 0) {
			Response response = microserviceClient.getForObject(APIEndPoint.USER_SERVICE + "/appointment/" + appointmentId, Response.class);
			if (response != null) {
				return (Appointment) response.populatePayloadUsingJson(Appointment.class);
			}
		}
		return null;
	}
	
	public Appointment autoAssignAppointment(Long appointmentId) {
		if (appointmentId == null || appointmentId <= 0) {
			throw new IllegalArgumentException("Invalid param provided!");
		}
		Response response = microserviceClient.patchForObject(APIEndPoint.USER_SERVICE + "/appointment/" + appointmentId + "/auto-assign", null , Response.class);
		if (response != null) {
			return (Appointment) response.populatePayloadUsingJson(Appointment.class);
		}
		throw new BadRequestException("Appointment not assigned for appointment-id : " + appointmentId);
	}
}
