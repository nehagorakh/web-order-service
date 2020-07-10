package in.lifcare.order.cart.service.impl;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.PutObjectRequest;

import in.lifcare.core.exception.BadRequestException;
import in.lifcare.core.util.S3Util;
import in.lifcare.order.cart.model.CartPrescription;
import in.lifcare.order.cart.repository.CartPrescriptionRepository;
import in.lifcare.order.cart.service.CartPrescriptionService;
import in.lifcare.order.microservice.account.prescription.model.Prescription;
import in.lifcare.order.microservice.account.prescription.service.PrescriptionService;

@Service
public class CartPrescriptionServiceImpl implements CartPrescriptionService {

	@Value("${prescription.s3.access.key}")
	private String prescriptionS3AccessKey;

	@Value("${prescription.s3.secret.key}")
	private String prescriptionS3SecretKey;

	@Value("${prescription.s3.bucket}")
	private String prescriptionS3bucket;
	
	@Value("${prescription.s3.bucket.region:ap-south-1}")
	private String prescriptionS3bucketRegion;

	@Value("${prescription.temp.storage}")
	private String prescriptionTempStorage;
	
	@Override
	public CartPrescription savePrescription(CartPrescription cartPrescription) {
		return cartPrescriptionRepository.save(cartPrescription);
	}

	@Override
	public List<CartPrescription> savePrescriptions(List<CartPrescription> cartPrescriptions) {
		return (List<CartPrescription>) cartPrescriptionRepository.save(cartPrescriptions);
	}

	@Override
	public List<CartPrescription> getCartPrescriptionsByCartUid(String cartUid) {
		List<CartPrescription> cartPrescriptions = cartPrescriptionRepository.findByCartUid(cartUid);
		if( cartPrescriptions != null && !cartPrescriptions.isEmpty() ) {
			String relativeLocation = null;
			//FIXME stream
			for( int i = 0; i < cartPrescriptions.size(); i++ ) {
				relativeLocation = cartPrescriptions.get(i).getRelativeLocation();
				cartPrescriptions.get(i).setLocation(getPrescriptionLocation(relativeLocation));
			}
		}
		return cartPrescriptions;
	}

	@Override
	public String getPrescriptionLocation(String relativeLocation) {
		String location = null;
		try {
			if (!StringUtils.isBlank(relativeLocation)) {
				location = S3Util.generatePreSignedUrl(prescriptionS3AccessKey, prescriptionS3SecretKey, prescriptionS3bucket, relativeLocation);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return location;
	}
	
	@Override
	public CartPrescription addOrUploadCartPrescription(String cartUid, Long patientId, MultipartFile file, CartPrescription cartPrescription) throws Exception {
		if ( cartPrescription == null && (file == null || file.isEmpty()) ) {
			throw new IllegalArgumentException("File / Cart Prescription - atleast one is mandatory.");
		}
		if( cartPrescription == null ) {
			cartPrescription = new CartPrescription();
		}
		if( StringUtils.isBlank(cartPrescription.getType()) ) {
			cartPrescription.setType(CartPrescription.PRESCRIPTION_TYPE.IMAGE);
		}
		if( file != null && !file.isEmpty() ) {
			//Prescription Upload on AWS
			String prescriptionFileName = generatePrescriptionFileName(cartUid, file.getOriginalFilename());
			String fileLocation = prescriptionTempStorage + "/" + prescriptionFileName;
			Path path = Paths.get(fileLocation);
			byte[] bytes = file.getBytes();
			Files.write(path, bytes);
			
			BasicAWSCredentials creds = new BasicAWSCredentials(prescriptionS3AccessKey, prescriptionS3SecretKey);
			AmazonS3 s3Client = AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(creds)).withRegion(prescriptionS3bucketRegion).build();
			// Store prescription in s3 from local storage
			File awsfile = new File(fileLocation);
			String locationBasePath = "cart/" + cartUid;
			if( patientId != null && patientId > 0 ) {
				locationBasePath = "cart/" + cartUid + "/" + patientId;
				cartPrescription.setPatientId(patientId);
			}
			String relativeLocation = locationBasePath  + "/" + prescriptionFileName;
			s3Client.putObject(new PutObjectRequest(prescriptionS3bucket, relativeLocation, awsfile));
			awsfile.delete();
			cartPrescription.setRelativeLocation(relativeLocation);
			cartPrescription.setBucket(prescriptionS3bucket);
			cartPrescription.setFileName(file.getOriginalFilename());
			cartPrescription.setLocation(getPrescriptionLocation(relativeLocation));
		}
		cartPrescription.setCartUid(cartUid);
		return savePrescription(cartPrescription);
	}

	@Override
	public List<CartPrescription> addOrUploadCartPrescriptions(String cartUid, Long patientId, MultipartFile file, List<String> cartPrescriptionIds, Timestamp expiryDate, String doctorName, Timestamp rxDate) throws Exception {
		if ( (cartPrescriptionIds == null || cartPrescriptionIds.isEmpty()) && (file == null || file.isEmpty()) ) {
			throw new IllegalArgumentException("File / Cart Prescription - atleast one is mandatory.");
		}
		List<CartPrescription> cartPrescriptions = new ArrayList<CartPrescription>();
		CartPrescription cartPrescription = new CartPrescription();
		if( file != null && !file.isEmpty() ) {
			cartPrescription = new CartPrescription();
			cartPrescription.setCartUid(cartUid);
			cartPrescription.setExpiryDate(expiryDate);
			cartPrescription.setRxDate(rxDate);
			cartPrescription.setDoctorName(doctorName);
			//Prescription Upload on AWS
			String prescriptionFileName = generatePrescriptionFileName(cartUid, file.getOriginalFilename());
			BasicAWSCredentials creds = new BasicAWSCredentials(prescriptionS3AccessKey, prescriptionS3SecretKey);
			String locationBasePath = "cart/" + cartUid;
			if( patientId != null && patientId > 0 ) {
				locationBasePath = StringUtils.leftPad(String.valueOf(patientId), 14, '0');
				cartPrescription.setPatientId(patientId);
			}
			String relativeLocation = locationBasePath  + "/" + prescriptionFileName;
			AmazonS3 s3Client = AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(creds)).withRegion(prescriptionS3bucketRegion).build();
			s3Client.putObject(new PutObjectRequest(prescriptionS3bucket, relativeLocation,
					new ByteArrayInputStream(file.getBytes()), null));
			if( StringUtils.isBlank(cartPrescription.getType()) ) {
				cartPrescription.setType(CartPrescription.PRESCRIPTION_TYPE.IMAGE);
			}
			cartPrescription.setRelativeLocation(relativeLocation);
			cartPrescription.setBucket(prescriptionS3bucket);
			cartPrescription.setFileName(file.getOriginalFilename());
			cartPrescription.setLocation(getPrescriptionLocation(relativeLocation));
			if( cartPrescription.getPatientId() != null && cartPrescription.getPatientId() > 0 ) {
				Prescription prescription = new Prescription();
				prescription.setPatientId(patientId);
				prescription.setBucket(cartPrescription.getBucket());
				prescription.setRelativeLocation(cartPrescription.getRelativeLocation());
				prescription.setFileName(cartPrescription.getFileName());
				prescription.setDoctorName(cartPrescription.getDoctorName());
				prescription.setExpiryDate(expiryDate);
				prescription.setRxDate(rxDate);
				prescription = prescriptionService.addPrescription(prescription);
				cartPrescription.setPrescriptionId(prescription.getId());
			}
			cartPrescriptions.add(cartPrescription);
		}
		if( cartPrescriptionIds != null && !cartPrescriptionIds.isEmpty() && patientId != null ) {
			for( String cartPrescriptionId : cartPrescriptionIds ) {
				try {
					Prescription prescription = prescriptionService.getPrescriptionByPatientId(patientId, cartPrescriptionId);
					if( prescription == null ) {
						continue;
					}
					cartPrescription = new CartPrescription();
					cartPrescription.setExpiryDate(expiryDate);
					cartPrescription.setRxDate(rxDate);
					cartPrescription.setDoctorName(doctorName);
					cartPrescription.setPrescriptionId(prescription.getId());
					cartPrescription.setRelativeLocation(prescription.getRelativeLocation());
					cartPrescription.setBucket(prescription.getBucket());
					cartPrescription.setFileName(prescription.getFileName());
					cartPrescription.setLocation(prescription.getLocation());
					cartPrescription.setCartUid(cartUid);
					cartPrescription.setType(prescription.getType());
					cartPrescriptions.add(cartPrescription);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return savePrescriptions(cartPrescriptions);
	}
	
	@Override
	public String generatePrescriptionFileName(String cartUid, String fileName) {
		if (StringUtils.isNotBlank(fileName) && StringUtils.isNotBlank(cartUid)) {
			long timestamp = new Date().getTime();
			String[] extension = fileName.split("\\.");
			String prescriptionFileName = timestamp + "_" + cartUid + "_" + extension[0];
			if( extension.length > 1 ) {
				prescriptionFileName = prescriptionFileName + "." + extension[extension.length - 1];
			}
			return prescriptionFileName;
		}
		throw new BadRequestException("Invalid cart uid : [" + cartUid + "] or file name [" + fileName + "]");
	}
	
	@Override
	public void deleteCartPrescription(Long cartPrescriptionId) {
		cartPrescriptionRepository.delete(cartPrescriptionId);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void deleteCartPrescriptions(List<CartPrescription> cartPrescriptions) {
		cartPrescriptionRepository.delete(cartPrescriptions);
	}

	@Override
	public List<CartPrescription> getPrescriptionsByPrescriptionIds(List<Long> cartPrescriptionIds) {
		return cartPrescriptionRepository.findByIdIn(cartPrescriptionIds);
	}
	
	@Override
	public CartPrescription getPrescriptionByIdAndCartUid(Long cartPrescriptionId, String cartUid) {
		return cartPrescriptionRepository.findByIdAndCartUid(cartPrescriptionId, cartUid);
	}

	@Override
	public void updatePatientIdInPrescriptionsByCartUid(String cartUid, Long patientId) {
		List<CartPrescription> cartPrescriptions = cartPrescriptionRepository.findByCartUid(cartUid);
		if( cartPrescriptions != null && !cartPrescriptions.isEmpty() ) {
			for( int i = 0; i < cartPrescriptions.size(); i++ ) {
				cartPrescriptions.get(i).setPatientId(patientId);
			}
			cartPrescriptionRepository.save(cartPrescriptions);
		}
	}

	@Autowired
	private PrescriptionService prescriptionService;
	
	@Autowired
	private CartPrescriptionRepository cartPrescriptionRepository;

}
