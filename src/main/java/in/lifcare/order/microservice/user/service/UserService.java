package in.lifcare.order.microservice.user.service;

import in.lifcare.core.model.Appointment;
import in.lifcare.order.microservice.user.model.RescheduleAppointment;

public interface UserService {

	Appointment createAppointment(Appointment appointment);
	
	Appointment cancelAppointment(long appointmentId);
	
	Appointment updateAppointment(long appointmentId, RescheduleAppointment rescheduleAppointment);
	
	Appointment getAppointment(long appointmentId);
	
	Appointment autoAssignAppointment(Long appointmentId);
}
