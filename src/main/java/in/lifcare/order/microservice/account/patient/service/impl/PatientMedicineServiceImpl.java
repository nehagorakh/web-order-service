package in.lifcare.order.microservice.account.patient.service.impl;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import in.lifcare.core.constant.APIEndPoint;
import in.lifcare.core.model.PatientMedicine;
import in.lifcare.core.response.model.Response;
import in.lifcare.core.util.MicroserviceClient;
import in.lifcare.order.microservice.account.patient.model.Patient;
import in.lifcare.order.microservice.account.patient.service.PatientMedicineService;
import in.lifcare.order.model.OrderItem;

@Service
public class PatientMedicineServiceImpl implements PatientMedicineService {

	@SuppressWarnings("unchecked")
	@Override
	public List<PatientMedicine> getAllPatientMedicines(long patientId) {

		ResponseEntity<Response> response = microserviceClient.exchange(
				APIEndPoint.ACCOUNT_SERVICE + "/patient/" + patientId + "/all-medicines?verified-only=false", HttpMethod.GET, null,
				Response.class);
		PatientMedicine[] patientMedicines = (PatientMedicine[]) response.getBody()
				.populatePayloadUsingJson(PatientMedicine[].class);
		return Arrays.asList(patientMedicines);

	}

	@Override
	public PatientMedicine add(long patientId, Timestamp dispatchDate, OrderItem orderItem, boolean isExcessiveOrdered) throws Exception {
		PatientMedicine patientMedicine = createPatientMedicineFromOrderItem(patientId, dispatchDate, orderItem, isExcessiveOrdered);
		Response<PatientMedicine> response = microserviceClient.postForObject(
				APIEndPoint.ACCOUNT_SERVICE + "/patient/" + patientId + "/medicine", patientMedicine, Response.class);
		return (PatientMedicine) response.populatePayloadUsingJson(PatientMedicine.class);
	}
	
	@Override
	public boolean addAll(long patientId, List<PatientMedicine> patientMedicines) throws Exception {
		Response<Boolean> response = microserviceClient.postForObject(
				APIEndPoint.ACCOUNT_SERVICE + "/patient/medicines", patientMedicines, Response.class);
		return (Boolean) response.getPayload();
	}

	@Override
	public PatientMedicine createPatientMedicineFromOrderItem(long patientId, Timestamp dispatchDate, OrderItem orderItem, boolean isExcessiveOrdered) {
		PatientMedicine patientMedicine = new PatientMedicine();
		if (orderItem.getPatientId() != null && orderItem.getPatientId() > 0) {
			patientMedicine.setPatientId(orderItem.getPatientId());
		} else {
			patientMedicine.setPatientId(patientId);
		}
		patientMedicine.setSku(orderItem.getSku());
		patientMedicine.setProductName(orderItem.getName());
		patientMedicine.setType(orderItem.getClassification());
		if (!PatientMedicine.TYPE.H1D.equalsIgnoreCase(orderItem.getClassification())) {
			patientMedicine.setVerified(true);
		}
		patientMedicine.setVerifiedBy(orderItem.getVerifiedBy());
		patientMedicine.setPackType(orderItem.getPackType());
		patientMedicine.setBrandName(orderItem.getBrandName());
		patientMedicine.setRecommendQty(orderItem.getOrderedQuantity());
		patientMedicine.setOrderedQty(orderItem.getQuantity());
		patientMedicine.setOrderedLooseQty(orderItem.getLooseQuantity());
		//patientMedicine.setDose(orderItem.getDosage());
		patientMedicine.setLastOrderDate(dispatchDate);//order created_at
		//patientMedicine.setDeliveryTat(1);
		//patientMedicine.setRefillLead(false);
		patientMedicine.setDosageValue(orderItem.getDosageValue());
		patientMedicine.setDosageSchedule(orderItem.getDosageSchedule());
		patientMedicine.setPrescriptionId(orderItem.getPrescriptionId());
		patientMedicine.setExcessiveOrderedQuantity(isExcessiveOrdered);
		patientMedicine.setProductCategory(orderItem.getProductCategory());
		return patientMedicine;
	}
	

	@Override
	public PatientMedicine getPatientMedicineWithIdAndSku(long patientId, String sku) {
		if (patientId > 0 && StringUtils.isNotBlank(sku)) {
			ResponseEntity<Response> response = microserviceClient.exchange(
					APIEndPoint.ACCOUNT_SERVICE + "/patient/" + patientId + "/medicine/sku/" +sku, HttpMethod.GET, null,
					Response.class);
			PatientMedicine patientMedicine = (PatientMedicine) response.getBody().populatePayloadUsingJson(PatientMedicine.class);
			return patientMedicine;
		}
		return null;
	}

	@SuppressWarnings("rawtypes")
	@Autowired
	private MicroserviceClient<Response> microserviceClient;

}
