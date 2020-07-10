package in.lifcare.order.microservice.account.patient.service;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import in.lifcare.core.model.PatientMedicine;
import in.lifcare.order.microservice.account.patient.model.Patient;
import in.lifcare.order.model.OrderItem;

public interface PatientMedicineService {
	
	public List<PatientMedicine> getAllPatientMedicines(long patientId);

	public PatientMedicine add(long patientId, Timestamp dispatchDate, OrderItem orderItem, boolean isExcessiveOrdered) throws Exception;

	public PatientMedicine createPatientMedicineFromOrderItem(long patientId, Timestamp dispatchDate, OrderItem orderItem, boolean isExcessiveOrdered);

	public boolean addAll(long patientId, List<PatientMedicine> patientMedicines) throws Exception;
	
	PatientMedicine getPatientMedicineWithIdAndSku(long patientId, String sku);

}
