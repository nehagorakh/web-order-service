package in.lifcare.order.cart.service;

import java.sql.Timestamp;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;
import in.lifcare.order.cart.model.CartPrescription;

/**
 * 
 * @author karan
 *
 */
public interface CartPrescriptionService {

	/**
	 * 
	 * @param cartPrescription
	 * @return
	 */
	CartPrescription savePrescription(CartPrescription cartPrescription);

	/**
	 * 
	 * @param cartPrescriptions
	 * @return
	 */
	List<CartPrescription> savePrescriptions(List<CartPrescription> cartPrescriptions);

	/**
	 * 
	 * @param cartUid
	 * @return
	 */
	List<CartPrescription> getCartPrescriptionsByCartUid(String cartUid);

	/**
	 * 
	 * @param cartUid
	 * @param patientId
	 * @param file
	 * @param cartPrescription
	 * @return
	 * @throws Exception
	 */
	CartPrescription addOrUploadCartPrescription(String cartUid, Long patientId, MultipartFile file, CartPrescription cartPrescription) throws Exception;

	/**
	 * 
	 * @param cartPrescriptionId
	 */
	void deleteCartPrescription(Long cartPrescriptionId);

	/**
	 * 
	 * @param cartPrescriptions
	 */
	void deleteCartPrescriptions(List<CartPrescription> cartPrescriptions);
	
	/**
	 * 
	 * @param cartPrescriptionId
	 * @param cartUid
	 * @return
	 */
	CartPrescription getPrescriptionByIdAndCartUid(Long cartPrescriptionId, String cartUid);

	/**
	 * 
	 * @param cartPrescriptionIds
	 * @return
	 */
	List<CartPrescription> getPrescriptionsByPrescriptionIds(List<Long> cartPrescriptionIds);
	
	/**
	 * 
	 * @param cartUid
	 * @param fileName
	 * @return
	 */
	String generatePrescriptionFileName(String cartUid, String fileName);

	/**
	 * 
	 * @param relativeLocation
	 * @return
	 */
	String getPrescriptionLocation(String relativeLocation);

	/**
	 * 
	 * @param cartUid
	 * @param patientId
	 */
	public void updatePatientIdInPrescriptionsByCartUid(String cartUid, Long patientId);

	/**
	 * 
	 * @param cartUid
	 * @param patientId
	 * @param file
	 * @param cartPrescriptionIds
	 * @param expiryDate 
	 * @param doctorName 
	 * @param rxDate TODO
	 * @return
	 * @throws Exception 
	 */
	List<CartPrescription> addOrUploadCartPrescriptions(String cartUid, Long patientId, MultipartFile file, List<String> cartPrescriptionIds, Timestamp expiryDate, String doctorName, Timestamp rxDate) throws Exception;
	
}
