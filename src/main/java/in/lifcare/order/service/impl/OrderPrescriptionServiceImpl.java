package in.lifcare.order.service.impl;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.beanutils.BeanUtilsBean2;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.converters.SqlTimestampConverter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import in.lifcare.core.constant.APIEndPoint;
import in.lifcare.core.response.model.Response;
import in.lifcare.core.util.MicroserviceClient;
import in.lifcare.core.util.S3Util;
import in.lifcare.order.cart.model.CartPrescription;
import in.lifcare.order.microservice.account.prescription.model.Prescription;
import in.lifcare.order.microservice.account.prescription.service.PrescriptionService;
import in.lifcare.order.model.OrderPrescription;
import in.lifcare.order.repository.OrderPrescriptionRepository;
import in.lifcare.order.service.OrderPrescriptionService;

@Service
public class OrderPrescriptionServiceImpl implements OrderPrescriptionService {

	@Value("${prescription.s3.access.key}")
	private String prescriptionS3AccessKey;

	@Value("${prescription.s3.secret.key}")
	private String prescriptionS3SecretKey;

	@Value("${prescription.s3.bucket}")
	private String prescriptionS3bucket;
	
	@Value("${prescription.s3.bucket.region:ap-south-1}")
	private String prescriptionS3bucketRegion;

	@Override
	public List<OrderPrescription> saveOrderPrescriptions(long orderId, long patientId, List<Long> prescriptionIds) {
		if (prescriptionIds != null && !prescriptionIds.isEmpty()) {

			List<OrderPrescription> orderPrescriptions = new ArrayList<OrderPrescription>();
			for (Long prescriptionId : prescriptionIds) {

				/*HttpHeaders headers = new HttpHeaders();
				headers.setContentType(MediaType.APPLICATION_JSON);
				headers.set("Authorization", "Bearer 7959c30f-d4f6-4a1c-ae14-232e052b5341");

				//HttpEntity<String> entity = new HttpEntity<String>(headers);
				
				//@SuppressWarnings("rawtypes")
				//ResponseEntity<Response> responseEntity = restTemplate.exchange(APIEndPoint.ACCOUNT_SERVICE + "/patient/" + patientId + "/prescription/" + prescriptionId, HttpMethod.GET, entity, Response.class);

				//Response<?> response = responseEntity.getBody();
*/				Response<?> response = microServiceClient.getForObject(APIEndPoint.ACCOUNT_SERVICE + "/patient/" + patientId + "/prescription/" + prescriptionId, Response.class);
				OrderPrescription orderPrescription = (OrderPrescription) response.populatePayloadUsingJson(OrderPrescription.class);

				orderPrescription.setOrderId(orderId);
				orderPrescription.setPrescriptionId(prescriptionId);
				orderPrescription.setId(0);

				orderPrescriptions.add(orderPrescription);
			}
			return (List<OrderPrescription>) orderPrescriptionRepository.save(orderPrescriptions);
		} else {
			orderPrescriptionRepository.deleteByOrderId(orderId);
		}
		return null;
	}

	@Override
	public List<OrderPrescription> findByOrderId(long orderId) {
		List<OrderPrescription> orderPrescriptions = orderPrescriptionRepository.findByOrderId(orderId);
		try {
			for (OrderPrescription orderPrescription : orderPrescriptions) {
				orderPrescription.setLocation(S3Util.generatePreSignedUrl(prescriptionS3AccessKey, prescriptionS3SecretKey, prescriptionS3bucket, orderPrescription.getRelativeLocation()));
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		return orderPrescriptions;
	}

	@Override
	public List<OrderPrescription> saveCartOrderPrescriptions(long orderId, List<CartPrescription> cartPrescriptions) {
		if( cartPrescriptions == null || cartPrescriptions.isEmpty() || orderId <= 0 ) {
			throw new IllegalArgumentException("Invalid cart prescriptions or order id specified");
		}
		LOGGER.info("Testing logger : {}", orderId);
		List<OrderPrescription> orderPrescriptions = new ArrayList<OrderPrescription>();
		long prescriptionId = 0;
		for( CartPrescription cartPrescription : cartPrescriptions ) {
			prescriptionId = cartPrescription.getPrescriptionId() != null ? cartPrescription.getPrescriptionId(): 0;
			OrderPrescription orderPrescription =  new OrderPrescription();
			try {
				ConvertUtils.register(new SqlTimestampConverter(null), Timestamp.class);
				BeanUtilsBean2.getInstance().copyProperties(orderPrescription, cartPrescription);
			} catch (Exception e) {
				e.printStackTrace();
			}
			orderPrescription.setOrderId(orderId);
			try {
				if( prescriptionId <= 0 ) {
					String splitName[] = cartPrescription.getRelativeLocation().split("/");
					String fileName = splitName[splitName.length-1];
					String transferFileLocation = StringUtils.leftPad(String.valueOf(cartPrescription.getPatientId()), 14, '0') + "/" + fileName;
					S3Util.transferFileBetweenBuckets(prescriptionS3AccessKey, prescriptionS3SecretKey, prescriptionS3bucketRegion, prescriptionS3bucket, prescriptionS3bucket, cartPrescription.getRelativeLocation(), transferFileLocation);
					orderPrescription.setRelativeLocation(transferFileLocation);
					Prescription prescription = new Prescription();
					try {
						ConvertUtils.register(new SqlTimestampConverter(null), Timestamp.class);
						BeanUtilsBean2.getInstance().copyProperties(prescription, cartPrescription);
					} catch (Exception e) {
						e.printStackTrace();
					}
					prescription.setPatientId(cartPrescription.getPatientId());
					prescription.setRelativeLocation(transferFileLocation);
					prescription = prescriptionService.addPrescription(prescription);
					prescriptionId = prescription != null && prescription.getId() > 0 ? prescription.getId(): 0;
				}
			} catch(Exception e) {
				e.printStackTrace(); 
			}
			orderPrescription.setPrescriptionId(prescriptionId);
			orderPrescriptions.add(orderPrescription);
		}
		return (List<OrderPrescription>) orderPrescriptionRepository.save(orderPrescriptions);
	}
	
	@Override
	public List<OrderPrescription> updateOrderPrescriptions(long orderId, long patientId, List<Long> prescriptionIds) {
		if( prescriptionIds == null || prescriptionIds.isEmpty() || orderId <= 0 || patientId <= 0) {
			throw new IllegalArgumentException("Invalid prescriptions or order id specified!");
		}
		List<OrderPrescription> orderPrescriptions = new ArrayList<OrderPrescription>();
		List<Long> nonPatientPrescriptionIds = new ArrayList<>();
		for (Long prescriptionId : prescriptionIds) {
			try {
				Response<?> response = microServiceClient.getForObject(APIEndPoint.ACCOUNT_SERVICE + "/patient/" + patientId + "/prescription/" + prescriptionId, Response.class);
				OrderPrescription orderPrescription = (OrderPrescription) response.populatePayloadUsingJson(OrderPrescription.class);
				if (orderPrescriptionRepository.findTopByOrderIdAndPrescriptionId(orderId, prescriptionId) != null) {
					continue;
				}
				orderPrescription.setOrderId(orderId);
				orderPrescription.setPrescriptionId(prescriptionId);
				orderPrescription.setId(0);
				orderPrescriptions.add(orderPrescription);
			} catch (Exception e) {
				nonPatientPrescriptionIds.add(prescriptionId);
			}
		}
		LOGGER.info("Not mapped prescription-ids for order {} due to patient-id mismatch: {}", orderId, nonPatientPrescriptionIds);
		return (List<OrderPrescription>) orderPrescriptionRepository.save(orderPrescriptions);
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(OrderPrescriptionServiceImpl.class);
	
	@Autowired
	private OrderPrescriptionRepository orderPrescriptionRepository;

	@Autowired
	private PrescriptionService prescriptionService;
	
	/*@Autowired
	private RestTemplate restTemplate;*/
	
	@SuppressWarnings("rawtypes")
	@Autowired
	private MicroserviceClient<Response> microServiceClient;

}
