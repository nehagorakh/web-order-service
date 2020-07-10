package in.lifcare.order.microservice.account.prescription.service;

import in.lifcare.order.microservice.account.prescription.model.Prescription;

public interface PrescriptionService {

	Prescription addPrescription(Prescription prescription) throws Exception;

	Prescription getPrescriptionByPatientId(Long patientId, String prescriptionId) throws Exception;

}
