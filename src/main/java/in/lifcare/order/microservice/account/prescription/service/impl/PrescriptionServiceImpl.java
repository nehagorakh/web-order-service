package in.lifcare.order.microservice.account.prescription.service.impl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import in.lifcare.core.constant.APIEndPoint;
import in.lifcare.core.exception.NotFoundException;
import in.lifcare.core.response.model.Response;
import in.lifcare.core.util.MicroserviceClient;
import in.lifcare.order.microservice.account.prescription.model.Prescription;
import in.lifcare.order.microservice.account.prescription.service.PrescriptionService;

/**
 * 
 * @author karan
 *
 */
@SuppressWarnings({"rawtypes"})
@Service
public class PrescriptionServiceImpl implements PrescriptionService {

	@Override
	public Prescription addPrescription(Prescription prescription) throws Exception {
		if( prescription == null ) {
			throw new IllegalArgumentException("Invalid prescription ");
		}
		try {
			Response<?> response = microserviceClient.postForObject(APIEndPoint.ACCOUNT_SERVICE + "/prescription", prescription, Response.class);
			Prescription savedPrescription = (Prescription) response.populatePayloadUsingJson(Prescription.class);
			if( savedPrescription == null ) {
				throw new NotFoundException("No response receiving for saved prescription : " + savedPrescription);
			}
			return savedPrescription;
		} catch (Exception e) {
			throw new Exception("Error while adding prescription :: Exception : " + e.getMessage());
		}
	}

	@Override
	public Prescription getPrescriptionByPatientId(Long patientId, String prescriptionId) throws Exception {
		if( StringUtils.isBlank(prescriptionId)||  patientId == null ) {
			throw new IllegalArgumentException("Invalid prescription id / patient id");
		}
		try {
			Response<?> response = microserviceClient.getForObject(APIEndPoint.ACCOUNT_SERVICE + "/patient/" + patientId + "/prescription/" + prescriptionId, Response.class);
			Prescription prescription = (Prescription) response.populatePayloadUsingJson(Prescription.class);
			if( prescription == null ) {
				throw new NotFoundException("No response received for prescription id : " + prescriptionId);
			}
			return prescription;
		} catch (Exception e) {
			throw new Exception("Error while fetching prescription :: Exception : " + e.getMessage());
		}
	}
	
	@Autowired
	private MicroserviceClient<Response> microserviceClient;
	
}
