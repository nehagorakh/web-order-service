
package in.lifcare.order.service.impl;

import java.lang.reflect.InvocationTargetException;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.BeanUtilsBean2;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.converters.SqlDateConverter;
import org.apache.commons.beanutils.converters.SqlTimeConverter;
import org.apache.commons.beanutils.converters.SqlTimestampConverter;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Days;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.domain.Specifications;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import com.amazonaws.services.codecommit.model.InvalidOrderException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.CaseFormat;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;

import in.lifcare.account.client.facility.service.AccountClientFacilityService;
import in.lifcare.account.client.model.Facility;
import in.lifcare.client.inventory.model.ProductStock;
import in.lifcare.core.constant.APIEndPoint;
import in.lifcare.core.constant.LabOrderState;
import in.lifcare.core.constant.LabOrderStatus;
import in.lifcare.core.constant.OrderEvent;
import in.lifcare.core.constant.OrderSource;
import in.lifcare.core.constant.OrderState;
import in.lifcare.core.constant.OrderStatus;
import in.lifcare.core.constant.OrderType;
import in.lifcare.core.exception.AccessNotAllowedException;
import in.lifcare.core.exception.BadRequestException;
import in.lifcare.core.exception.DuplicateEntryException;
import in.lifcare.core.exception.NotFoundException;
import in.lifcare.core.fareye.model.FarEyeProcessData;
import in.lifcare.core.fareye.model.ProcessData;
import in.lifcare.core.model.Appointment;
import in.lifcare.core.model.AppointmentAddress;
import in.lifcare.core.model.FarEyeDetails;
import in.lifcare.core.model.HubSpokePincodeMapping;
import in.lifcare.core.model.ItemInfo;
import in.lifcare.core.model.Name;
import in.lifcare.core.model.OrderStateStatusRequest;
import in.lifcare.core.model.PatientMedicine;
import in.lifcare.core.model.ProductSalt;
import in.lifcare.core.model.UpdateOrderObject;
import in.lifcare.core.model.User;
import in.lifcare.core.model.WalletTransactionInfo;
import in.lifcare.core.response.model.Error;
import in.lifcare.core.response.model.Response;
import in.lifcare.core.util.CommonUtil;
import in.lifcare.core.util.MicroserviceClient;
import in.lifcare.order.cart.model.Cart;
import in.lifcare.order.cart.model.CartItem;
import in.lifcare.order.cart.model.CartPrescription;
import in.lifcare.order.cart.service.CartService;
import in.lifcare.order.event.OrderEventService;
import in.lifcare.order.event.WalletService;
import in.lifcare.order.exception.AddressNotFoundException;
import in.lifcare.order.exception.CancelOrderException;
import in.lifcare.order.exception.CartNotFoundException;
import in.lifcare.order.exception.HoldOrderException;
import in.lifcare.order.exception.InvalidDeliveryOptionException;
import in.lifcare.order.exception.InvalidServiceTypeException;
import in.lifcare.order.exception.MaxOrderedQuantityExceeded;
import in.lifcare.order.exception.MaxPermissibleLimitReached;
import in.lifcare.order.exception.OrderException;
import in.lifcare.order.exception.OrderItemNotFoundException;
import in.lifcare.order.exception.OrderNotFoundException;
import in.lifcare.order.exception.PaymentGatewayException;
import in.lifcare.order.exception.PrescriptionExpiredException;
import in.lifcare.order.exception.PrescriptionRxDateNotFound;
import in.lifcare.order.microservice.account.customer.model.Customer;
import in.lifcare.order.microservice.account.customer.service.CustomerService;
import in.lifcare.order.microservice.account.patient.model.Patient;
import in.lifcare.order.microservice.account.patient.service.PatientMedicineService;
import in.lifcare.order.microservice.account.prescription.model.Prescription;
import in.lifcare.order.microservice.account.prescription.service.PrescriptionService;
import in.lifcare.order.microservice.catalog.model.Medicine;
import in.lifcare.order.microservice.catalog.service.CatalogService;
import in.lifcare.order.microservice.inventory.service.ProductStockService;
import in.lifcare.order.microservice.payment.model.OrderPaymentGatewayVerifyRequest;
import in.lifcare.order.microservice.payment.model.OrderPaymentInitiateRequest;
import in.lifcare.order.microservice.payment.model.OrderWalletPaymentRequest;
import in.lifcare.order.microservice.payment.model.PaymentChannelData;
import in.lifcare.order.microservice.payment.model.PaymentGatewayData;
import in.lifcare.order.microservice.payment.service.PaymentService;
import in.lifcare.order.microservice.salus.service.SalusService;
import in.lifcare.order.microservice.shipping.model.PlacePincode;
import in.lifcare.order.microservice.shipping.model.Shipment;
import in.lifcare.order.microservice.shipping.service.ShippingService;
import in.lifcare.order.microservice.user.model.RescheduleAppointment;
import in.lifcare.order.microservice.user.service.UserService;
import in.lifcare.order.model.Order;
import in.lifcare.order.model.OrderCreationCartRequest;
import in.lifcare.order.model.OrderDeliveryObject;
import in.lifcare.order.model.OrderItem;
import in.lifcare.order.model.OrderItem.PRODUCT_CATEGORY;
import in.lifcare.order.model.OrderItemSummary;
import in.lifcare.order.model.OrderPatientPrescription;
import in.lifcare.order.model.OrderPaymentGatewayObject;
import in.lifcare.order.model.OrderPrescription;
import in.lifcare.order.model.OrderPrescriptionSummary;
import in.lifcare.order.model.OrderPrice;
import in.lifcare.order.model.OrderSearchResponse;
import in.lifcare.order.model.OrderSummary;
import in.lifcare.order.model.Reason;
import in.lifcare.order.model.RegionCode;
import in.lifcare.order.model.ShippingAddress;
import in.lifcare.order.model.StatusUpdateRequest;
import in.lifcare.order.model.TempOrderSync;
import in.lifcare.order.property.constant.OrderPropertyConstant;
import in.lifcare.order.repository.OrderRepository;
import in.lifcare.order.repository.OrderSpecification;
import in.lifcare.order.repository.ReasonRepository;
import in.lifcare.order.repository.ShippingAddressRepository;
import in.lifcare.order.repository.TempOrderSyncRepository;
import in.lifcare.order.service.OrderItemService;
import in.lifcare.order.service.OrderPrescriptionService;
import in.lifcare.order.service.OrderService;
import in.lifcare.order.service.OrderShippingAddressService;

@Service
public class OrderServiceImpl implements OrderService {

	List<String> HABIT_FORMING_DRUGS = Arrays.asList(PatientMedicine.TYPE.ND, PatientMedicine.TYPE.H1D_NRX, PatientMedicine.TYPE.HD_NRX, PatientMedicine.TYPE.SGD, PatientMedicine.TYPE.SXD);
	List<String> UNVERIFIED_DRUG_TYPE = Arrays.asList(PatientMedicine.TYPE.H1D);
	List<String> AutoVerifiedDrugType = new ArrayList<String>(Arrays.asList(PatientMedicine.TYPE.NSD, PatientMedicine.TYPE.OTC));
	List<Integer> validDiscounts = new ArrayList<Integer>(Arrays.asList(20, 25));
	public static final String LEAD_MEDICINE_LINE_ITEMS = "medicine_line_items";
	public static final String LEAD_KEY = "lead";
		
	@Value("${expressDelivery.startTime}")
	private String expressDeliveryStartTime;
		
	@Value("${expressDelivery.endTime}")
	private String expressDeliveryEndTime;
	
	@Value("${expressDelivery.maxCashback}")
	private Float expressDeliveryMaxCashback;
	
	@Value("${lfAssured.maxCashback}")
	private Float lfAssuredMaxCashback;
	
	@Value("${lfAssured.lfAssuredCashbackPercentage}")
	private float lfAssuredCashbackPercentage;
	
	@Value("${expressDelivery.expressDeliveryCashbackPercentage}")
	private float expressDeliveryCashbackPercentage;
	
	public Order get(long orderId) throws Exception {
		Order order = orderRepository.findOne(orderId);
		if (order != null) {
			// Get order line items

			if (order.getOrderNumber() != null) {
				order.setDisplayOrderId(order.getOrderNumber());
			}
			List<OrderItem> orderItems = orderItemService.findByOrderId(orderId);
			if (order.getTotalSalePrice() < 0) {
				order.setTotalSalePrice(0);
			}
			order.setLineTotalAmount(order.getTotalMrp() - order.getDiscount());
			// Get shipping address
			ShippingAddress shippingAddress = orderShippingAddressService.findByOrderId(orderId);

			if (shippingAddress != null) {
				order.setShippingAddress(shippingAddress);
				order.setShippingAddressId(shippingAddress.getId());
			}
			
			if (orderItems != null) {
				orderItems = orderItems.parallelStream()
				.map(orderItem -> {
					orderItem.setAvailableDeliveryOption(order.getDeliveryOption());
					orderItem.setAvailableServiceType(order.getServiceType());
					return orderItem;
				}).collect(Collectors.toList());
				
				order.setOrderItems(orderItems);
			}

			

			CompletableFuture<List<OrderPrescription>> orderPrescriptionResponceFuture = CompletableFuture.supplyAsync(() -> {
				try {
					return orderPrescriptionService.findByOrderId(orderId);
				} catch (Exception e) {
					return null;
				}

			});
			CompletableFuture<List<OrderItem>> orderItemsFuture = CompletableFuture.supplyAsync(() -> {
				try {
					return orderItemService.updateOrderLineInfo(order, order.getOrderItems(), String.valueOf(shippingAddress.getPincode()), true);
				} catch (Exception e) {
					return null;
				}

			});
			
			CompletableFuture<Appointment> appointmentFuture = CompletableFuture.supplyAsync(() -> {
				try {
					if (Order.CATEGORY.MEDICINE.equalsIgnoreCase(order.getCategory())){
						return null;
					}
					return userService.getAppointment(order.getAppointmentId());
				} catch (Exception e) {
					return null;
				}

			});
			CompletableFuture.allOf(orderPrescriptionResponceFuture, orderItemsFuture, appointmentFuture).get();
			List<OrderPrescription> orderPrescriptions = orderPrescriptionResponceFuture.get();
			if (orderPrescriptions != null && !orderPrescriptions.isEmpty()) {
				order.setOrderPrescriptions(orderPrescriptions);

				List<Long> prescriptionIds = new ArrayList<Long>();
				for (OrderPrescription orderPrescription : orderPrescriptions) {
					prescriptionIds.add(orderPrescription.getId());
				}

				order.setPrescriptionIds(prescriptionIds);
			} else {
				order.setOrderPrescriptions(new ArrayList<OrderPrescription>());
				order.setPrescriptionIds(new ArrayList<Long>());
			}
			if (appointmentFuture.get() != null) {
				order.setAppointment(appointmentFuture.get());
			}
			order.setOrderItems(orderItemsFuture.get());

			try {
				Response<?> response = microserviceClient.getForObject(APIEndPoint.ACCOUNT_SERVICE + "/customer/" + order.getCustomerId() + "/patient/" + order.getPatientId(), Response.class);
				order.setPatient((Patient) response.populatePayloadUsingJson(Patient.class));
			} catch(Exception e) {
				LOGGER.error("Error in fetching patient : {}", e.getMessage());
			}
			
			order.setAssociateOrders(orderRepository.findAllByParentId(orderId));
			order.setMergedOrders(orderRepository.findAllByMergeWithId(orderId));
			return order;
		} else {
			throw new OrderNotFoundException("Order id " + orderId + " not present in the system");
		}

	}

	@Override
	public Order getOrder(long orderId) throws Exception {
		if (orderId > 0) {
			Order order = orderRepository.findOne(orderId);
			if (order != null) {
				return order;
			} else {
				throw new OrderNotFoundException("Order id " + orderId + " not present in the system");
			}
		} else {
			throw new IllegalArgumentException("Invalid order Id::" + orderId);
		}
	}

	@Override
	public Boolean autoDigitized(long orderId) throws Exception {
		if (orderId > 0) {
			Order order = orderRepository.findOne(orderId);
			if (order != null) {
				List<OrderItem> orderItems = orderItemService.findByOrderId(orderId);
				if (StringUtils.isBlank(order.getComment()) && orderItems != null && !orderItems.isEmpty() && !order.isPrescriptionProvided()) {
					return !orderItems.parallelStream()
							.filter(oi -> StringUtils.isNotBlank(oi.getSku()))
							.map(OrderItem::getSku)
							.anyMatch(s -> s.contains("NEW"));
				} else {
					return false;
				}
			} else {
				throw new OrderNotFoundException("Order id " + orderId + " not present in the system");
			}
		} else {
			throw new IllegalArgumentException("Invalid order Id::" + orderId);
		}
	}

	@Override
	public Boolean autoVerified(long orderId) throws Exception {
		if (orderId > 0) {
			Order order = orderRepository.findOne(orderId);
			if (order != null) {
				if (Order.CATEGORY.LAB.equalsIgnoreCase(order.getCategory())) {
					return true;
				}
				List<OrderItem> orderItems = orderItemService.findByOrderId(orderId);
				if( orderItems != null && orderItems.size() == 1 && orderItems.parallelStream().allMatch(oi -> oi.getProductCategory().equalsIgnoreCase(OrderItem.PRODUCT_CATEGORY.MEMBERSHIP_CARD))) {
					return true;
				}
				if (orderItems != null && !orderItems.isEmpty()) {
					// update order items info
					ShippingAddress shippingAddress = shippingAddressRepository.findTopByOrderId(order.getId());
					if (shippingAddress != null) {
						orderItems = orderItemService.updateOrderLineInfo(order, orderItems, String.valueOf(shippingAddress.getPincode() > 0 ? shippingAddress.getPincode() : ""), false);
					}
					boolean isAutoVerifiable = false;
					Map<String, PatientMedicine> filteredMedicineMap = new HashMap<>();
					List<PatientMedicine> patientMedicines = patientMedicineService.getAllPatientMedicines(order.getPatientId());
					List<String> bulkMedicineSkus = getExcessiveOrderedItemSku(order.getPatientId(), orderItems);
					if (patientMedicines != null && !patientMedicines.isEmpty()) {
						filteredMedicineMap = patientMedicines.stream().collect(Collectors.groupingBy(PatientMedicine::getSku)).entrySet().stream()
								.collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get(0)));
					}
					for (OrderItem orderItem : orderItems) {
						try {
							if (orderItem.getMaxOrderQuantity() != null && orderItem.getMaxOrderQuantity() > 0 && orderItem.getMaxOrderQuantity() < orderItem.getQuantity()) {
								orderItem.setAutoVerificationFailedReason(OrderItem.AUTO_VERIFICATION_FAILED_REASON.MAX_ORDER_QUANTITY_EXCEEDED + " (" + orderItem.getMaxOrderQuantity() + ")");
								orderItem.setVerified(false);
							} else if (StringUtils.isNotBlank(orderItem.getProductCategory()) && orderItem.getProductCategory().equalsIgnoreCase(PRODUCT_CATEGORY.MEMBERSHIP_CARD) 
									|| (orderItem.getClassification() != null && AutoVerifiedDrugType.contains(orderItem.getClassification()))) {
								orderItem.setAutoVerificationFailedReason(StringUtils.EMPTY);
								orderItem.setVerified(true);
							} else if (orderItem.getClassification() != null && filteredMedicineMap.containsKey(orderItem.getSku())
									&& filteredMedicineMap.get(orderItem.getSku()).isVerified() && !HABIT_FORMING_DRUGS.contains(orderItem.getClassification()) && !UNVERIFIED_DRUG_TYPE.contains(orderItem.getClassification())) {
								orderItem.setAutoVerificationFailedReason(StringUtils.EMPTY);
								orderItem.setVerified(true);
								//checkPrescriptionVelidityForSku(order.getPatientId(), orderItem.getPrescriptionId(), orderItem.getValidDays());
							} else {
								if (filteredMedicineMap.containsKey(orderItem.getSku())) {
									PatientMedicine patientMedicine = filteredMedicineMap.get(orderItem.getSku());
									if (!patientMedicine.getProductName().equalsIgnoreCase(orderItem.getName())) {
										orderItem.setAutoVerificationFailedReason(OrderItem.AUTO_VERIFICATION_FAILED_REASON.MEDICINE_NAME_MISMATCH);
									} else if (!patientMedicine.isVerified()) {
										orderItem.setAutoVerificationFailedReason(OrderItem.AUTO_VERIFICATION_FAILED_REASON.UN_VERIFIED_MEDICINE);
									} else if (patientMedicine.isExcessiveOrderedQuantity() || (bulkMedicineSkus != null && bulkMedicineSkus.contains(orderItem.getSku()))) {
										orderItem.setAutoVerificationFailedReason(OrderItem.AUTO_VERIFICATION_FAILED_REASON.BULK_MEDICINE_QUANTITY_EXCEEDED + " (" + orderItem.getBulkOrderQuantity() + ")");
									} else {
										orderItem.setVerified(patientMedicine.isVerified());
									}
								} else {
									if (Medicine.STATUS.INACTIVE_STATUS.contains(orderItem.getMedicineStatus())) {
										orderItem.setAutoVerificationFailedReason(orderItem.getMedicineStatus());
									} else if (HABIT_FORMING_DRUGS.contains(orderItem.getClassification())) {
										orderItem.setAutoVerificationFailedReason(OrderItem.AUTO_VERIFICATION_FAILED_REASON.HABIT_FORMING_DRUG);
									} else {
										orderItem.setAutoVerificationFailedReason(OrderItem.AUTO_VERIFICATION_FAILED_REASON.UN_VERIFIED_MEDICINE);
									}
								}
							}
						} catch (PrescriptionExpiredException e2) {
							orderItem.setVerified(false);
							orderItem.setAutoVerificationFailedReason(OrderItem.AUTO_VERIFICATION_FAILED_REASON.PRESCRIPTION_EXPIRED);
						} catch (PrescriptionRxDateNotFound e2) {
							orderItem.setVerified(false);
							orderItem.setAutoVerificationFailedReason(OrderItem.AUTO_VERIFICATION_FAILED_REASON.PRESCRIPTION_RX_DATE_NOT_FOUND);
						} catch (NotFoundException e2) {
							orderItem.setVerified(false);
							orderItem.setAutoVerificationFailedReason(OrderItem.AUTO_VERIFICATION_FAILED_REASON.PRESCRIPTION_RX_DATE_NOT_FOUND);
						} catch (Exception e2) {
							orderItem.setVerified(false);
							orderItem.setAutoVerificationFailedReason(OrderItem.AUTO_VERIFICATION_FAILED_REASON.UNKNOWN_EXCEPTION);
						}
					}
					isAutoVerifiable = orderItems.parallelStream().allMatch(OrderItem::isVerified);
					if (order.isPrescriptionProvided()) {
						isAutoVerifiable = false;
					} else if (!order.isRepeat()) {
						orderItems.parallelStream().forEach(orderItem -> orderItem.setAutoVerificationFailedReason(OrderItem.AUTO_VERIFICATION_FAILED_REASON.FIRST_PATIENT_ORDER));
						isAutoVerifiable = false;
					} 
					order.setOrderItems(orderItems);
					String couponCode = order.getCouponCode();
					if(StringUtils.isNotBlank(order.getManualCouponCode())) {
						couponCode = order.getManualCouponCode();
					}
					applyCoupon(order, couponCode, "update");
					order.setAutoVerificationFailed(!isAutoVerifiable);
					orderRepository.save(order);
					orderItemService.save(order.getOrderItems());
					return isAutoVerifiable;
				} else {
					throw new OrderItemNotFoundException("Order-items not present in system for order-id " + orderId);
				}
			} else {
				throw new OrderNotFoundException("Order id " + orderId + " not present in the system");
			}
		} else {
			throw new IllegalArgumentException("Invalid order Id::" + orderId);
		}
	}

	@Override
	public Page<Order> getOrders(long customerId, String category, Pageable pageable) throws Exception {
		if (customerId <= 0) {
			throw new IllegalArgumentException("Invalid customer-id");
		}
		if (category != null) {
			return orderRepository.findAllByCustomerIdAndCategory(customerId, category, pageable);
		}
		return orderRepository.findAllByCustomerId(customerId, pageable);
	}
	

	@Override
	public Page<OrderSummary> getOrdersSummary(long customerId, String category, Pageable pageable) {
		Page<Order> orders = orderRepository.findAllByCustomerIdAndCategory(customerId, category, pageable);
		List<OrderSummary> orderSummaryList = new ArrayList<OrderSummary>();
		if( orders.hasContent() ) {
			orders.getContent().stream().forEach(order -> {
				OrderSummary orderSummary = new OrderSummary();
				orderSummary.setId(order.getId());
				orderSummary.setState(order.getState());
				orderSummary.setStatus(order.getStatus());
				orderSummary.setTotalPayableAmount(order.getTotalPayableAmount());
				orderSummary.setCreatedAt(order.getCreatedAt());
				orderSummary.setDeliveryDate(order.getPromisedDeliveryDate());
				orderSummary.setPaymentMethod(order.getPaymentMethod());
				orderSummary.setCustomerFirstName(order.getCustomerFirstName());
				orderSummary.setCustomerLastName(order.getCustomerLastName());
				orderSummary.setDeliveryOption(order.getDeliveryOption());
				orderSummary.setServiceType(order.getServiceType());
				List<OrderItemSummary> orderItemSummaryList = new ArrayList<OrderItemSummary>();
				List<OrderPrescriptionSummary> orderPrescriptionSummaryList = new ArrayList<OrderPrescriptionSummary>();
				try {
					Optional<List<OrderItem>> orderItemOptional = Optional.ofNullable(orderItemService.findByOrderId(order.getId()));
					orderItemOptional.ifPresent(orderItems -> {
						orderItems.parallelStream().forEach(orderItem -> {
							OrderItemSummary orderItemSummary = new OrderItemSummary();
							orderItemSummary.setName(orderItem.getName());
							orderItemSummary.setBrand(orderItem.getBrandName());
							orderItemSummary.setSku(orderItem.getSku());
							orderItemSummary.setQuantity(orderItem.getQuantity());
							orderItemSummary.setMrp(orderItem.getMrp());
							orderItemSummary.setSalePrice(orderItem.getSalePrice());
							orderItemSummaryList.add(orderItemSummary);
						});
					});
				} catch(Exception e) {
					LOGGER.error("Error in getting order items : {}, Exception : {}", order.getId(), e.getMessage());
				}
				try {
					Optional<List<OrderPrescription>> orderPrescriptionOptional = Optional.ofNullable(orderPrescriptionService.findByOrderId(order.getId()));
					orderPrescriptionOptional.ifPresent(orderPrescriptions -> {
						orderPrescriptions.parallelStream().forEach(orderPrescription -> {
							OrderPrescriptionSummary orderPrescriptionSummary = new OrderPrescriptionSummary();
							orderPrescriptionSummary.setPrescriptionId(orderPrescription.getPrescriptionId());
							orderPrescriptionSummary.setLocation(orderPrescription.getLocation());
							orderPrescriptionSummary.setType(orderPrescription.getType());
							orderPrescriptionSummary.setFileName(orderPrescription.getFileName());
							orderPrescriptionSummaryList.add(orderPrescriptionSummary);
						});
					});
				} catch(Exception e) {
					LOGGER.error("Error in getting order prescriptions : {}, Exception : {}", order.getId(), e.getMessage());
				}
				orderSummary.setItems(orderItemSummaryList);
				orderSummary.setPrescriptions(orderPrescriptionSummaryList);
				orderSummaryList.add(orderSummary);
			});
		}
		return new PageImpl<>(orderSummaryList, pageable, orderSummaryList.size());
	}

	@Override
	public Page<Order> getOrdersByPatient(long patientId, String category, Pageable pageable) {
		return orderRepository.findAllByPatientIdAndCategory(patientId, category, pageable);
	}
	
	@Override
	public Page<Order> getOrdersCreatedAtBetween(Timestamp createdAtBegins, Timestamp createdAtEnds, Pageable pageable) {
		if( createdAtBegins.getTime() <= 0 || createdAtEnds.getTime() <= 0 ) {
			throw new IllegalArgumentException("Invalid parameters createdAtBegins : " + createdAtBegins + ", createdAtEnds : " + createdAtEnds);
		}
		return orderRepository.findByCreatedAtBetween(createdAtBegins, createdAtEnds, pageable);
	}

	@Override
	public Page<Order> recentOrders(long customerId, Pageable pageable) throws Exception {
		DateTime timeline = DateTime.now(DateTimeZone.UTC).minusDays(40);
		Page<Order> orders = orderRepository.findByCustomerIdAndCreatedAtGreaterThan(customerId, timeline.toDate(), pageable);
		if (orders == null || !orders.hasContent() ) {
			throw new OrderNotFoundException("No recent orders found for customer-id : - "  + customerId);
		}
		return orders;
	}
	
	@Override
	public Page<Order> activeOrders(long customerId, Pageable pageable) throws Exception {
		List<String> statuses = OrderStatus.ACTIVE_ORDER_STATUS;
		Page<Order> orders = orderRepository.findByCustomerIdAndStatusIn(customerId, statuses, pageable);
		if (orders == null || !orders.hasContent() ) {
			throw new OrderNotFoundException("No active orders found for customer-id : - "  + customerId);
		}
		return orders;
	}

	@Override
	public Order updateOrderNextRefillDay(long orderId, Timestamp nextRefillDate, int nextRefillDays) throws Exception {
		if (orderId < 0) {
			throw new IllegalArgumentException("Invalid order uid specified");
		}
		if (nextRefillDays <= 0 && nextRefillDate == null) {
			throw new IllegalArgumentException("Invalid next refill day or next refill date provided");
		}
		if (nextRefillDate != null && nextRefillDays == 0) {
			Date currDate = new Date();
			nextRefillDays = (int) Math.ceil(((nextRefillDate.getTime() - currDate.getTime()) / (1000 * 60 * 60 * 24)));
			nextRefillDays = nextRefillDays > 0 ? nextRefillDays : 0;
		}
		Order order = orderRepository.findOne(orderId);
		order.setNextRefillDay(nextRefillDays);
		return orderRepository.save(order);
	}

	@Override
	@Transactional(noRollbackFor = InvalidDeliveryOptionException.class)
	public Order createCartOrder(OrderCreationCartRequest orderCreationCartRequest, User user) throws Exception {
		try {
			String cartUid = orderCreationCartRequest.getCartUid();
			if( StringUtils.isBlank(cartUid) ) {
				throw new IllegalArgumentException("Invalid cart uid specified");
			}
			Long loginCustomerId = user != null ? user.getId() : null;
			if( loginCustomerId == null ) {
				throw new AccessNotAllowedException("User not authorized to create cart order");
			}
			Cart cart = cartService.getCartSummary(cartUid, loginCustomerId, false);
			if( cart == null ) {
				throw new CartNotFoundException("No cart found for cart uid : " + cartUid + " for creating order");
			}
			validateCartForOrder(cart);
			cart.setPaymentMethod(orderCreationCartRequest.getPaymentMethod());
			cart.setPaymentSubMethod(orderCreationCartRequest.getPaymentSubMethod());
			Order order = new Order();
			ConvertUtils.register(new SqlTimeConverter(null), Time.class);
			ConvertUtils.register(new SqlTimestampConverter(null), Timestamp.class);
			ConvertUtils.register(new SqlTimeConverter(null), Time.class);
			ConvertUtils.register(new SqlDateConverter(null), java.sql.Date.class);
			BeanUtilsBean2.getInstance().copyProperties(order, cart);
			order.setId(0);
			order.setCreatedAt(null);
			order.setUpdatedAt(null);
			order.setDisplayOrderId(null);
			order.setSource(cart.getSource());
			order.setCategory(cart.getCategory());
			order.setOrderType(cart.getPaymentType());
			order.setPaymentMethod(orderCreationCartRequest.getPaymentMethod());
			order.setPaymentSubMethod(orderCreationCartRequest.getPaymentSubMethod());
			if( cart.getPromisedDeliveryDate() == null ) {
				order.setPromisedDeliveryDate(cart.getEstimateDeliveryDate());
			}
			if (cart.getSellerDetail() != null) {
				order.setSellerId(cart.getSellerDetail().getRetailFacilityId());
				order.setSellerName(cart.getSellerDetail().getRetailFaciltyName());
			}
			order.setRedeemedCarePoints(cart.getRedeemedCarePoints());
			order.setRedeemedCouponCashback(cart.getRedeemedCouponCashback());
			order.setDoctorCallback(cart.isDoctorCallback());
			order.setShippingCharge((float)cart.getShippingFee());
			// Check Is order Eligible for Express Delivery or Lf Assured
			checkIsEligibleForUrgentAssured(cart);
			order.setDeliveryOption(cart.getDeliveryOption());
			order.setPreferredDeliveryOption(cart.getPreferredDeliveryOption() != null
					? cart.getPreferredDeliveryOption() : order.getDeliveryOption());
			order.setServiceType(cart.getServiceType());
			order.setPreferredServiceType(
					cart.getPreferredServiceType() != null ? cart.getPreferredServiceType() : order.getServiceType());
			order.setDeliveryOptionChangeReason(cart.getDeliveryOptionChangeReason());
			order.setUrgentDeliveryCharge((float)cart.getUrgentDeliveryCharge());
			List<CartItem> cartItems = cart.getCartItems();
			if( cartItems != null && !cartItems.isEmpty() ) {
				List<OrderItem> orderItems = new ArrayList<OrderItem>();
				OrderItem orderItem = null;
				for( CartItem cartItem : cartItems ) {
					List<String> salt = cartItem.getSalts() != null && !cartItem.getSalts().isEmpty() ? 
							cartItem.getSalts().parallelStream().map(ProductSalt::getId).collect(Collectors.toList()) : null;
					orderItem = new OrderItem();
					ConvertUtils.register(new SqlTimestampConverter(null), Timestamp.class);
					ConvertUtils.register(new SqlDateConverter(null), java.sql.Date.class);
					BeanUtilsBean2.getInstance().copyProperties(orderItem, cartItem);
					orderItem.setId(0);
					orderItem.setCreatedAt(null);
					orderItem.setUpdatedAt(null);
					orderItem.setBrandName(cartItem.getBrand());
					orderItem.setPackType(cartItem.getType());
					orderItem.setClassification(cartItem.getDrugType());
					orderItem.setSalePrice((float)cartItem.getSellingPrice());
					orderItem.setSalt(salt != null && !salt.isEmpty() ? String.join(",", salt) : null);
					orderItem.setMaxOrderQuantity(cartItem.getMaxOrderQuantity());
					orderItems.add(orderItem);
				}
				order.setOrderItems(orderItems);
			}

			List<CartPrescription> cartPrescriptions = cart.getCartPrescriptions();
			List<OrderPrescription> orderPrescriptions = new ArrayList<OrderPrescription>();
			Long prescriptionId = null;
			boolean isPrescriptionProvided = false;
			if( cartPrescriptions != null && !cartPrescriptions.isEmpty() ) {
				isPrescriptionProvided = true;
			}
			order.setPrescriptionProvided(isPrescriptionProvided);
			order.setCouponCode(StringUtils.EMPTY);
			order.setManualCouponCode(cart.getCouponCode());
			order.setBusinessType(cart.getBusinessType());
			order.setBusinessChannel(cart.getBusinessChannel());
			order.setFacilityName(cart.getFacilityName());
			order.setFacilityCode(cart.getFacilityCode());
			
			if (order.getCategory().equalsIgnoreCase(Order.CATEGORY.LAB)) {
				try {
					order.setShippingChargeExempted(cart.isShippingChargeExempted());
					AppointmentAddress appointmentAddress = new AppointmentAddress();
					ConvertUtils.register(new SqlTimeConverter(null), Time.class);
					ConvertUtils.register(new SqlTimestampConverter(null), Timestamp.class);
					ConvertUtils.register(new SqlTimeConverter(null), Time.class);
					ConvertUtils.register(new SqlDateConverter(null), java.sql.Date.class);
					BeanUtilsBean2.getInstance().copyProperties(appointmentAddress, cart.getShippingAddress());
					Appointment appointment = new Appointment();
					appointment.setAppointmentAddress(appointmentAddress);
					appointment.setSlotId(order.getAppointmentSlotId());
					appointment.setDate(order.getAppointmentDate());
					order.setAppointment(appointment);
				} catch (Exception e) {

				}
			}
			Order cloneOrderObject = SerializationUtils.clone(order);
			cloneOrderObject.setOrderItems(order.getOrderItems());
			if(!Order.PROCUREMENT_TYPE.BULK.equalsIgnoreCase(order.getProcurementType())) {
				applyCoupon(cloneOrderObject, StringUtils.EMPTY, "apply-default");
				if (cloneOrderObject != null && StringUtils.isNotBlank(cloneOrderObject.getCouponCode()) && cloneOrderObject.getCouponCode().equalsIgnoreCase(cart.getCouponCode())) {
					order.setManualCouponCode(StringUtils.EMPTY);
		      		order.setCouponCode(cloneOrderObject.getCouponCode());
				} 
			}
			
			order = add(order, user);

			if( isPrescriptionProvided && order.getId() > 0 ) {
				try {
					orderPrescriptions = orderPrescriptionService.saveCartOrderPrescriptions(order.getId(), cartPrescriptions);
					if(orderPrescriptions != null && !orderPrescriptions.isEmpty()) {
						prescriptionId = orderPrescriptions.get(0).getPrescriptionId();
						if( prescriptionId != null && prescriptionId > 0 && order.getOrderItems() != null && !order.getOrderItems().isEmpty() ) {
							for( int i = 0; i < order.getOrderItems().size(); i++ ) {
								order.getOrderItems().get(i).setPrescriptionId(prescriptionId);
							}
							orderItemService.save(order.getOrderItems());
						}
					}
				} catch(Exception e) {
					e.printStackTrace();
					LOGGER.error("Error in saving cart prescription : " + e.getMessage());
				}
			}
			
			try {
				cart.setOrderId(order.getId());
				cart.setStatus(Cart.STATUS.COMPLETED);
				cartService.saveCart(cart);
			} catch(Exception e) {
				LOGGER.error("Error in updating cart status and order id : " + e.getMessage());
			}
			return order;
		} catch (Exception e) {
			throw e;
		}
	}

	private void validateCartForOrder(Cart cart) {
		cartService.validateCart(cart);
		cartService.validateActiveCart(cart);
		cartService.validateCartServiceability(cart);
	}

	private void checkIsEligibleForUrgentAssured(Cart cart) {
		if (cart.isNotAvailableUrgent()) {
			cart.setDeliveryOption(Cart.DELIVERY_OPTION.NORMAL);
			cart.setPreferredDeliveryOption(Cart.DELIVERY_OPTION.NORMAL);
			cart.setDeliveryOptionChangeReason(cart.getDeliveryOptionChangeReason());
			cart.setUrgentDeliveryCharge(0.00);
			cartService.saveCart(cart);
			throw new InvalidDeliveryOptionException("Invalid Delivery option :: " + cart.getDeliveryOptionChangeReason());
		} else if (cart.isNotAvailableLfAssured()) {
			cart.setServiceTypeChangeReason(cart.getServiceTypeChangeReason());
			cart.setServiceType(Cart.SERVICE_TYPE.NORMAL);
			cart.setPreferredServiceType(Cart.SERVICE_TYPE.NORMAL);
			cartService.saveCart(cart);
			throw new InvalidServiceTypeException("Invalid Service Type :: " + cart.getDeliveryOptionChangeReason());
		}
	}
	
	@Transactional
	public Order addPosOrder(Order order, User user) throws Exception {
		try{
			if (order != null) {
				if (order.getSource().equals(Order.SOURCE.POS)) {
					if (StringUtils.isBlank(order.getOrderType())) {
						order.setOrderType(Order.ORDER_TYPE.COD);
					}
					if (order.getDeliveryType() == null
							|| order.getDeliveryType().equals(Order.DELIVERY_TYPE.SELF_PICKUP)) {
						order.setDeliveryType(Order.DELIVERY_TYPE.SELF_PICKUP);
					}
					order.setState(OrderState.PROCESSING);
					order.setStatus(OrderStatus.STATE_PROCESSING.PICKED);
					if (StringUtils.isBlank(order.getBusinessChannel())
							|| StringUtils.isBlank(order.getBusinessType())) {
						order.setBusinessType(Order.BUSINESS_CHANNEL.OFFLINE);
						order.setBusinessChannel(Order.BUSINESS_TYPE.B2C);
					}
					order.setDeliveryOption(Order.DELIVERY_OPTION.NORMAL);
					order.setServiceType(Order.SERVICE_TYPE.NORMAL);
					order.setPrescriptionProvided((order.isPrescriptionProvided()
							|| (order.getPrescriptionIds() != null && !order.getPrescriptionIds().isEmpty())));

					if (user.getId() > 0) {
						order.setCreatedBy(((Long) user.getId()).toString());
					}
					if (order.getFacilityCode() <= 0) {
						throw new IllegalArgumentException("Invalid facility code : " + order.getFacilityCode());
					}
					Customer customer = customerService.getCustomerDetail(order.getCustomerId());
					if (customer == null) {
						throw new IllegalArgumentException("Invalid customer object");
					}
					order.setCustomerFirstName(customer.getFirstName());
					order.setCustomerLastName(customer.getLastName());
					ShippingAddress webOrderShippingAddress = new ShippingAddress();

					Response<Facility> facilityResponse = accountClientFacilityService
							.getFacility((long) order.getFacilityCode());
					if (facilityResponse == null || facilityResponse.getPayload() == null) {
						throw new IllegalArgumentException(
								"Facility response not received for id : " + order.getFacilityCode());
					}
					Facility facility = (Facility) facilityResponse.populatePayloadUsingJson(Facility.class);
					if (!Order.DELIVERY_TYPE.SELF_PICKUP.equalsIgnoreCase(order.getDeliveryType())) {
						if (order.getShippingAddress() == null || order.getShippingAddressId() <= 0) {
							throw new IllegalArgumentException(
									"Shipping address mandatory for non self pickup options");
						}
						BeanUtilsBean2.getInstance().copyProperties(webOrderShippingAddress,
								order.getShippingAddress());
						order.setShippingAddress(webOrderShippingAddress);
					} else {
						BeanUtilsBean2.getInstance().copyProperties(webOrderShippingAddress, facility);
						webOrderShippingAddress.setId(0);
						webOrderShippingAddress.setFirstName(facility.getName());
					}
					if (StringUtils.isBlank(webOrderShippingAddress.getMobile())) {
						webOrderShippingAddress.setMobile(customer.getMobile());
					}
					webOrderShippingAddress.setCustomerId(order.getCustomerId());
					orderRepository.save(order);
					webOrderShippingAddress.setOrderId(order.getId());

					ShippingAddress shippingAddress = orderShippingAddressService.save(webOrderShippingAddress);

					order.setShippingAddress(shippingAddress);

					List<OrderItem> orderItemList = new ArrayList<>();
					Map<String, OrderItem> skuWiseItemMap = new HashMap<>();
					Map<String, Integer> skuWiseItemQuantityMap = new HashMap<>();
					Map<String, Integer> skuWiseItemLooseQuantityMap = new HashMap<>();
					order.getOrderItems().stream().forEach(item -> {
						skuWiseItemMap.put(item.getSku(), item);
						skuWiseItemLooseQuantityMap.put(item.getSku(),
								skuWiseItemLooseQuantityMap.get(item.getSku()) != null
										? skuWiseItemLooseQuantityMap.get(item.getSku()) + item.getLooseQuantity()
										: item.getLooseQuantity());
						skuWiseItemQuantityMap.put(item.getSku(),
								skuWiseItemQuantityMap.get(item.getSku()) != null
										? skuWiseItemQuantityMap.get(item.getSku()) + item.getQuantity()
										: item.getQuantity());
					});
					skuWiseItemMap.entrySet().parallelStream().forEach(s -> {
						OrderItem item = s.getValue();
						item.setPerPackQty(item.getPerPackQty() <= 0 ? 1 : item.getPerPackQty());
						item.setQuantity(skuWiseItemQuantityMap.get(item.getSku()));
						item.setLooseQuantity(skuWiseItemLooseQuantityMap.get(item.getSku()));
						item.setMrp(item.getMrp());
						item.setSalePrice(item.getSalePrice());
						orderItemList.add(item);
					});
					order.setOrderItems(orderItemList);
					boolean applyCoupon = false;
					String couponCode = order.getCouponCode();
					if (StringUtils.isNotBlank(order.getManualCouponCode())) {
						couponCode = order.getManualCouponCode();
					}
					if (StringUtils.isNotBlank(couponCode)) {
						applyCoupon = true;
					}
					updateOrderItemWithCatalogAndApplyCoupon(order,
							shippingAddress != null ? String.valueOf(shippingAddress.getPincode()) : null, false,
							couponCode, "redeem", applyCoupon);
					order.setOrderItems(orderItemService.save(order.getId(), order.getOrderItems()));
					order.setOrderPrescriptions(orderPrescriptionService.saveOrderPrescriptions(order.getId(),
							order.getPatientId(), order.getPrescriptionIds()));
					order = orderRepository.save(order);
					order.setDisplayOrderId(String.valueOf(order.getId()));
					order.setOrderNumber(String.valueOf(order.getId()));
					order.setFacilityName(StringUtils.isNotBlank(facility.getDisplayName()) ? facility.getDisplayName()
							: facility.getName());
					order.setFacilityCallbackMobile(facility.getCallbackNumber());
					if (order.getNextRefillDay() > 0) {
						order.setNextRefillDate(
								Timestamp.valueOf(LocalDateTime.now().plusDays(order.getNextRefillDay())));
					}
					order = orderRepository.save(order);
				}
				return order;
			} else {
				throw new IllegalArgumentException("Order can't be empty");
			}
			
		}catch(Exception e) {
			throw e;
		}
	}

	@Transactional
	public Order add(Order order, User user) throws Exception {
		try {
			if (order != null) {
				if( StringUtils.isBlank(order.getOrderType()) ) {
					order.setOrderType(Order.ORDER_TYPE.COD);
				}

				if (order.getId() == 0) {
					order.setState(OrderState.NEW);
					order.setStatus(OrderStatus.STATE_NEW.PAYMENT_PENDING);
					if( Order.ORDER_TYPE.COD.equalsIgnoreCase(order.getOrderType()) ) {
						order.setStatus(OrderStatus.STATE_NEW.NEW);
					}	
				}

				if (order.getRepeatDate() != null && order.getRepeatDay() == 0) {
					order.setRepeatDay((Days.daysBetween(DateTime.now().toLocalDate(), (new DateTime(order.getRepeatDate().getTime())).toLocalDate()).getDays()));
				}

				if( StringUtils.isBlank(order.getOrderType()) ) {
					order.setOrderType(OrderType.COD);
				}
				
				if (StringUtils.isBlank(order.getBusinessChannel()) || StringUtils.isBlank(order.getBusinessType())) {
					order.setBusinessType(Order.BUSINESS_CHANNEL.ONLINE);
					order.setBusinessChannel(Order.BUSINESS_TYPE.B2C);
				}
				
				if (Order.BUSINESS_CHANNEL.OFFLINE.equalsIgnoreCase(order.getBusinessChannel()) && (Order.BUSINESS_TYPE.B2C.equalsIgnoreCase(order.getBusinessType())
								|| Order.BUSINESS_TYPE.B2B.equalsIgnoreCase(order.getBusinessType()))) {
					order.setState(OrderState.NEW);
					order.setStatus(OrderStatus.STATE_NEW.VERIFIED);
				}

				if ((order.getPromisedDeliveryBeginTime() == null || order.getPromisedDeliveryEndTime() == null) && StringUtils.isNotBlank(order.getPromisedDeliveryTime())) {
					SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
					long beginMilliSecs = sdf.parse((order.getPromisedDeliveryTime().split("-")[0]).trim()).getTime();
					long endMilliSecs = sdf.parse((order.getPromisedDeliveryTime().split("-")[1]).trim()).getTime();
					order.setPromisedDeliveryBeginTime(new Time(beginMilliSecs));
					order.setPromisedDeliveryEndTime(new Time(endMilliSecs));
				}
				// TODO need to change logic after wards of repeat order
				//Commented for v5 api - since this was done specifically for android source - if order items were there it was treated as repeat
//				order.setRepeat((order.getSource().equalsIgnoreCase(OrderSource.ANDROID)
//						&& order.getOrderItems() != null && !order.getOrderItems().isEmpty()));
				order.setPrescriptionProvided(
						( order.isPrescriptionProvided() || (order.getPrescriptionIds() != null && !order.getPrescriptionIds().isEmpty())) );

				if (user.getId() > 0) {
					order.setCreatedBy(((Long) user.getId()).toString());
				}
				
				Appointment appointment = order.getAppointment();
				// save order object
				order = orderRepository.save(order);
				
				
				if (StringUtils.isEmpty(order.getDisplayOrderId())) {
					order.setDisplayOrderId(String.valueOf(order.getId()));
				}
				/*
				 * if (order.getOrderItems() != null && !order.getOrderItems().isEmpty()) {
				 * for(OrderItem orderItem : order.getOrderItems()) { orderItem.setDiscount(0);
				 * orderItem.setDiscountPercentage(0F); } }
				 */
				boolean isCheckForShippingCharge = true;

				ShippingAddress shippingAddress = orderShippingAddressService.saveOrderShippingAddress(order.getId(),
						order.getCustomerId(), order.getShippingAddressId(), order.getMobile(), order.getEmail());
				if (shippingAddress != null) {
					// CALL SHIPPING TAT API BASED ON PINCODE
					String pincode = String.valueOf(shippingAddress.getPincode());
					PlacePincode placePincode = null;
					if (pincode != null) {
						try {
							placePincode = shippingService.getPlaceInformationByPincode(pincode);
						} catch (Exception e) {
							LOGGER.error("Error in getting place pincode with pincode {}", pincode);
						}
						int deliveryTatDays = calculateDeliveryTat(placePincode);
						if (isCheckForShippingCharge && !order.isShippingChargeExempted()) {
							order.setShippingCharge(
									getApplicableShippingChargeForSavedOrder(order.getCustomerId(), order.getTotalMrp(),
											(order.getShippingCharge() != 0 ? order.getShippingCharge() : null),
											order.isShippingChargeExempted(), placePincode));
						} else {
							order.setShippingCharge(0);
						}
						if (order.getPromisedDeliveryDate() == null) {
							DateTime promisedDeliveryDate = DateTime.now(DateTimeZone.UTC).plusDays(deliveryTatDays);
							order.setPromisedDeliveryDate(new Timestamp(promisedDeliveryDate.getMillis()));
						}
						DateTime dispatchDate = new DateTime(order.getPromisedDeliveryDate());
						int dispatchTatDays = deliveryTatDays - 1;
						order.setDispatchDate(new Timestamp(dispatchDate.minusDays(dispatchTatDays).getMillis()));
						if (Order.DELIVERY_OPTION.URGENT.equalsIgnoreCase(order.getDeliveryOption())) {
							order.setDispatchDate(order.getPromisedDeliveryDate());
						}
						if (order.getPromisedDeliveryDate().before(new Timestamp(DateTime.now(DateTimeZone.UTC).plusDays(deliveryTatDays).getMillis()))) {
							DateTime promisedDeliveryDate = DateTime.now(DateTimeZone.UTC).plusDays(deliveryTatDays);
							order.setPromisedDeliveryDate(new Timestamp(promisedDeliveryDate.getMillis()));
							dispatchDate = new DateTime(order.getPromisedDeliveryDate());
							order.setDispatchDate(new Timestamp(dispatchDate.minusDays(dispatchTatDays).getMillis()));
							if (Order.DELIVERY_OPTION.URGENT.equalsIgnoreCase(order.getDeliveryOption())) {
								promisedDeliveryDate = DateTime.now(DateTimeZone.UTC).plusDays(1);
								Calendar cal = Calendar.getInstance();
								cal.setTime(promisedDeliveryDate.toDate());
								cal.set(Calendar.HOUR_OF_DAY, 23);
								cal.set(Calendar.MINUTE, 59);
								cal.set(Calendar.SECOND, 58);
								cal.set(Calendar.MILLISECOND, 00);
								cal.setTimeZone(TimeZone.getTimeZone("Asia/Calcutta"));
								order.setPromisedDeliveryDate(new Timestamp(cal.getTimeInMillis()));
								order.setDispatchDate(order.getPromisedDeliveryDate());
							}
						}
					}
				}
				
				String couponCode = order.getCouponCode();
				if(StringUtils.isNotBlank(order.getManualCouponCode())) {
					couponCode = order.getManualCouponCode();
				}
				if(!Order.PROCUREMENT_TYPE.BULK.equalsIgnoreCase(order.getProcurementType())) {
					updateOrderItemWithCatalogAndApplyCoupon(order,
						shippingAddress != null ? String.valueOf(shippingAddress.getPincode()) : null, false, couponCode,
						"redeem", true);
				}

				// Save order items
				HashMap<String, Integer> skuCountMap = new HashMap<String, Integer>();
				if (order.getOrderItems() != null && !order.getOrderItems().isEmpty()) {
					StringBuilder comment = new StringBuilder();
					if (StringUtils.isNotBlank(order.getComment())) {
						comment.append(order.getComment());
					}
					float totalMrp = 0;
					for (Iterator<OrderItem> it = order.getOrderItems().iterator(); it.hasNext();) {
						OrderItem orderItem = it.next();
						totalMrp += orderItem.getMrp() * orderItem.getQuantity();
						orderItem.setOrderId(order.getId());
						orderItem.setState(OrderState.NEW);
						orderItem.setOrderedQuantity(orderItem.getQuantity());
						orderItem.setStatus(OrderStatus.STATE_NEW.NEW);
						orderItem.setActive(true);
						orderItem.setStatusReason(null);
						skuCountMap.put(orderItem.getSku(), orderItem.getQuantity());
						if (StringUtils.isBlank(orderItem.getSku())) {

							if (StringUtils.isNotBlank(orderItem.getName())) {
								comment.append(orderItem.getName()).append(" -- ").append(orderItem.getQuantity())
										.append(" -- ").append(String.join(",", orderItem.getDosageSchedule()))
										.append(";<\\br>");
							}
							it.remove();
						}
						if (orderItem.getMrp() == 0) {
							isCheckForShippingCharge = false;
						}
					}

					order.setComment(comment.toString());
					if (order.getTotalSalePrice() <= 0) {
						order.setTotalSalePrice(0);
					}
					order.setTotalMrp(totalMrp);
					order.setOrderNumber(String.valueOf(order.getId()));
					order.setTotalDiscount(order.getDiscount() + order.getCouponDiscount());
				}
				try {
					order = addLocalProcurementStatusInOrderItems(order);
				} catch (Exception e) {
					LOGGER.error("Error in Saving LocalProcuremnet Flag due to {}",e);
					//throw e;
				}

				orderItemService.save(order.getOrderItems());
				
				// Add shipping Charge logic & urgent delivery charge
				order.setTotalSalePrice(order.calculateTotalPrice());
				if (order.getTotalSalePrice() <= 0) {
					order.setTotalSalePrice(0);
				}

				// Save order prescription details if any
				orderPrescriptionService.saveOrderPrescriptions(order.getId(), order.getPatientId(),
						order.getPrescriptionIds());

				// Save payment details; For now ignore payment details from web
				// paymentService.addCodPaymentForOrderWithUnknownPrice(order.getId());

				if (Order.DELIVERY_OPTION.URGENT.equalsIgnoreCase(order.getDeliveryOption())) {
					// 1. Update order count in redis
					setUrgentOrderCountInRedis();
				}
				if (Order.CATEGORY.MEDICINE.equalsIgnoreCase(order.getCategory())) {
					updateProductStock(order, skuCountMap);
				} else if (Order.CATEGORY.LAB.equalsIgnoreCase(order.getCategory()) && appointment != null) {
					appointment.setOrderId(order.getId());
					appointment = userService.createAppointment(appointment);
					order.setAppointmentId(appointment.getId());
					order.setAppointmentDate(appointment.getDate());
					order.setAppointmentSlot(appointment.getDisplaySlot());
					order.setAppointment(appointment);
				}
				if( StringUtils.isBlank(order.getPatientFirstName()) ) {
					order.setPatientFirstName("");
					order.setPatientLastName("");
				}
				if (order.getOrderItems() != null && !order.getOrderItems().isEmpty()) {
//					order.setPatientFirstName(order.getOrderItems().stream().map(ot -> ot.getPatientFirstName()).filter(Objects::nonNull) // elide all null elements
//							.collect(()->new StringJoiner(",", "", "").setEmptyValue(""), // use "" when empty
//							         StringJoiner::add, StringJoiner::merge).toString());
					List<OrderItem> items = order.getOrderItems().stream().filter(x -> StringUtils.isNotBlank(x.getPatientFirstName())).collect(Collectors.toList());
					order.setPatientFirstName("");
					order.setPatientLastName("");
					if(items != null && !items.isEmpty()) {
						order.setPatientFirstName(items.get(0).getPatientFirstName());
						order.setPatientLastName(items.get(0).getPatientLastName());
					}
				}
				return order;

			} else {
				throw new IllegalArgumentException("Order can't be empty");
			}
		} catch (Exception e) {
			roolbackCoupon(order.getId(), order.getCouponCode());
			order.setCouponCode(null);
			order.setTotalDiscount(order.getTotalDiscount() - order.getCouponDiscount());
			order.setCouponDiscount(0);
			order.setRedeemedCarePoints(0);
			orderRepository.save(order);
			// e.printStackTrace();
			throw e;
		}
	}

	private Order addLocalProcurementStatusInOrderItems(Order order) throws Exception {
		
		List<OrderItem> orderItems = order.getOrderItems();
		
		List<String> skus = orderItems.stream().map(OrderItem::getSku).collect(Collectors.toList());
		
		LOGGER.info("Hub and spoke for order");
		HubSpokePincodeMapping hubSpoke = getHubAndSpoke(order);
		
		if(hubSpoke!=null && StringUtils.isNotEmpty(hubSpoke.getSpokeName())) {
			List<Medicine> medicine =catalogService.getMedicinesInformationBySkuAndFacilityCode((int)hubSpoke.getSpokeId(), skus);
			LOGGER.info("Map of Sku and Procurement Available in Spoke");
			Map<String,Boolean> isLocalProcurementAvailable = medicine.stream().map(Function.identity()).collect(Collectors.toMap(Medicine::getSku,t->t.isLocalAvailable()));
			
			LOGGER.info("Adding local procuremnet flag in Order Items For Distinct Skus");
			List<OrderItem> orderItemsList = orderItems.stream().map(e->{
				e.setSkuLocallyAvailable(isLocalProcurementAvailable.get(e.getSku()));
				return e;
			}).collect(Collectors.toList());
			order.setOrderItems(orderItemsList);
		}
		return order;
	}
	@SuppressWarnings("unchecked")
	private HubSpokePincodeMapping getHubAndSpoke(Order order) {
			
		Response response = microserviceClient.getForObject(APIEndPoint.SHIPPING_SERVICE + "/location/" + order.getShippingAddress().getPincode() ,  Response.class);
		
		HubSpokePincodeMapping hubSpoke = (HubSpokePincodeMapping) response.populatePayloadUsingJson(HubSpokePincodeMapping.class);
		
		return hubSpoke;

	}

	private int calculateDeliveryTat(PlacePincode placePincode) {
		if (placePincode != null && placePincode.getDeliveryDay() != null && placePincode.getDeliveryDay() > 0) {
			return placePincode.getDeliveryDay();
		}
		return 2;
	}

	private void roolbackCoupon(long orderId, String couponCode) {
		try {
			if (StringUtils.isNotBlank(couponCode) && orderId > 0) {
				microserviceClient.patchForObject(APIEndPoint.COUPON + "/order/" + orderId + "/roll-back", couponCode,
						Response.class);
			}
		} catch (Exception e) {
			LOGGER.info("Error while rvrting coupon for oderId {} and Coupon Code {}", orderId, couponCode);
		}
	}

	@Override
	public ShippingAddress update(Order orderModel, ShippingAddress shippingAddress) throws Exception {
		try {
			shippingAddress.setId(orderModel.getId());
			shippingAddress.setCustomerId(orderModel.getCustomerId());
			shippingAddress = shippingAddressRepository.save(shippingAddress);

			microserviceClient.postForObject(
					APIEndPoint.ACCOUNT_SERVICE + "/customer/" + orderModel.getCustomerId() + "/shipping-address/",
					shippingAddress, Response.class);
			// Response<ShippingAddress> response =
			// restTemplate.getForObject(APIEndPoint.ACCOUNT_SERVICE +
			// "/customer/" + orderModel.getCustomerId() + "/shipping-address/"
			// + shippingAddress.getId(), Response.class);
			return shippingAddress;

		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			throw e;
		}
	}

	@Override
	public ShippingAddress partialOrderShippingAddress(long orderId, HashMap<String, Object> shippingAddress)
			throws Exception {

		if (shippingAddress.containsKey("shipping_address_id") && shippingAddress.containsKey("customer_id")) {
			try {
				long shippingAddressId = (Long) shippingAddress.get("shipping_address_id");
				long customerId = (Long) shippingAddress.get("customer_id");
				return orderShippingAddressService.saveOrderShippingAddress(orderId, customerId, shippingAddressId, null, null);

			} catch (Exception e) {
				LOGGER.error(e.getMessage(), e);
				throw e;
			}
		}
		throw new BadRequestException("Invalid parameters provided");
	}

	// M
	@Override
	public Long getOrderCountByCustomerId(long customerId, Boolean isActive) throws Exception {
		long orderAvailable = 0;
		Long orderCount = 0L;
		List<String> activeState = OrderState.ACTIVE_ORDER_STATUS;
		if (isActive == null) {
			orderCount = orderRepository.countByCustomerIdAndStateIn(customerId, OrderState.ALL_ORDER_STATES);
		} else {
			if (isActive) {
				orderCount = orderRepository.countByCustomerIdAndStateIn(customerId, activeState);
			} else {
				orderCount = orderRepository.countByCustomerIdAndStateNotIn(customerId, activeState);
			}
		}
		if (orderCount != null) {
			orderAvailable = orderCount;
		}
		return orderAvailable;
	}

	@Override
	public List<OrderItem> getItems(long orderId) {
		return orderItemService.findByOrderId(orderId);
	}

	@Override
	public Boolean updateItemsPrescription(long orderId) {
		Order order = orderRepository.findById(orderId);
		Map<String, OrderItem> skuOrderItems = new HashMap<String, OrderItem>();
		List<OrderItem> orderItems = orderItemService.findByOrderId(orderId);
		List<OrderItem> newOrderItems = new ArrayList<OrderItem>();
		for (OrderItem orderItem : orderItems) {
			skuOrderItems.put(orderItem.getSku(), orderItem);
		}
		List<PatientMedicine> patientMedicines = patientMedicineService.getAllPatientMedicines(order.getPatientId());
		for (PatientMedicine patientMedicine : patientMedicines) {
			if (skuOrderItems.containsKey(patientMedicine.getSku()) && patientMedicine.getPrescriptionId() != null
					&& patientMedicine.getPrescriptionId() > 0) {
				OrderItem orderItem = skuOrderItems.get(patientMedicine.getSku());
				if (orderItem.getPrescriptionId() == null || orderItem.getPrescriptionId() == 0) {
					orderItem.setPrescriptionId(patientMedicine.getPrescriptionId());
					newOrderItems.add(orderItem);
				}
			}
		}
		orderItemService.save(newOrderItems);
		return true;
	}

	@Override
	public FarEyeProcessData getFareyeOrderDetails(Long displayOrderId, String orderNumber) throws Exception {
		Order order = null;
		try {
			order = orderRepository.findByOrderNumber(orderNumber);
			if (order == null) {
				order = orderRepository.findByDisplayOrderIdOrId(orderNumber, displayOrderId);
				if (order == null) {
					throw new OrderNotFoundException("order number " + orderNumber + " not found");
				}
			}
		} catch (Exception e) {
			throw e;
		}
		FarEyeProcessData farEyeProcessData = new FarEyeProcessData();
		ProcessData processData = new ProcessData();
		List<FarEyeDetails> farEyeDetails = new ArrayList<FarEyeDetails>();
		processData.setRxOnDelivery(String.valueOf(order.isPrescriptionProvided()));
		processData.setOrderAmount(String.valueOf(order.getTotalSalePrice()));
		DateFormat userDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:SS");
		DateFormat dateFormatNeeded = new SimpleDateFormat("yyyy-MM-dd");
		Date date = userDateFormat.parse(order.getPromisedDeliveryDate().toString());
		String finaldate = dateFormatNeeded.format(date);
		processData.setDeliveryDate(finaldate);
		// Get shipping address
		ShippingAddress shippingAddress = null;
		try {
			shippingAddress = orderShippingAddressService.findByOrderId(order.getId());
		} catch (Exception e) {
			LOGGER.error("Order id " + orderNumber + " shipping address not found");
			throw new AddressNotFoundException("AddressNotInSyetem");
		}
		if (null == shippingAddress) {
			throw new AddressNotFoundException("AddressNotInSystem");
		}
		processData.setCustomerName(shippingAddress.getFullName());
		processData.setCustomerAddress(shippingAddress.getStreet1() + " " + shippingAddress.getStreet2() + ","
				+ shippingAddress.getCity() + "," + shippingAddress.getState());
		processData.setPincode(String.valueOf(shippingAddress.getPincode()));
		processData.setMobileNumber(shippingAddress.getMobile());
		processData.setLocation(shippingAddress.getStreet2() + "," + shippingAddress.getCity());
		// Get order line items
		List<OrderItem> orderItems = null;
		try {
			orderItems = orderItemService.findByOrderId(order.getId());
		} catch (Exception e) {
			LOGGER.error("Order id " + orderNumber + " items not found");
			throw new OrderItemNotFoundException("OrderItemNotFoundInSystem");
		}
		if (null == orderItems || orderItems.isEmpty()) {
			throw new OrderItemNotFoundException("OrderItemNotFoundInSystem");
		}
		Iterator<OrderItem> iterator = orderItems.iterator();
		while (iterator.hasNext()) {
			FarEyeDetails fareyedetail = new FarEyeDetails();
			OrderItem orderItem = iterator.next();
			fareyedetail.setProduct_description(orderItem.getClassification());
			String maskedName = orderItem.getName();
			if (StringUtils.isNotBlank(maskedName)) {
				maskedName = StringUtils.overlay(maskedName, StringUtils.repeat("*", maskedName.length() - 3), 3,
						maskedName.length() - 3);
			}
			fareyedetail.setProduct_name(maskedName);
			fareyedetail.setQuantity(Integer.toString(orderItem.getQuantity()));
			fareyedetail.setSku_code(orderItem.getSku());
			fareyedetail.setUnit_price(String.valueOf(orderItem.getSalePrice()));
			farEyeDetails.add(fareyedetail);
		}
		processData.setProductDetailsArray(farEyeDetails);
		farEyeProcessData.setProcessData(processData);
		return farEyeProcessData;
	}

	@Override
	public Boolean markOrderDelivered(String orderId, Object farEyeProcessData) throws Exception {
		Order order = null;
		try {
			order = orderRepository.findByOrderNumber(orderId);
			if (order == null) {
				order = orderRepository.findByDisplayOrderId(String.valueOf(orderId));
				if (null == order) {
					throw new BadRequestException("Order " + orderId + "not found");
				}
			}
			if (OrderState.COMPLETED.equalsIgnoreCase(order.getState())
					&& (OrderStatus.STATE_COMPLETE.DELIVERED.equalsIgnoreCase(order.getStatus())
							|| OrderStatus.STATE_COMPLETE.RETURNED.equalsIgnoreCase(order.getStatus()))) {
				return true;
			}
			ObjectMapper mapper = new ObjectMapper();
			String farEyeOutResponseString = mapper.writeValueAsString(farEyeProcessData);
			JSONObject json = new JSONObject(farEyeOutResponseString);
			if (OrderState.SHIPPED.equalsIgnoreCase(order.getState())
					&& OrderStatus.STATE_SHIPPED.DISPATCHED.equalsIgnoreCase(order.getStatus()) && json.has("status")
					&& json.getString("status").equalsIgnoreCase("UNSEEN")) {
				return true;
			}
			if (json.has("source") && json.getString("source").equalsIgnoreCase("CRM") && json.has("status")
					&& json.getString("status").equalsIgnoreCase(OrderStatus.STATE_COMPLETE.DELIVERED)) {
				order.setState(OrderState.COMPLETED);
				order.setStatus(OrderStatus.STATE_COMPLETE.DELIVERED);
				orderRepository.save(order);
				walletService.creditRefereeWalletEvent(order, String.valueOf(order.getId()),
						WalletTransactionInfo.TRANSACTION_SOURCE.CRM_REFERRED_ORDER_DELIVERED);
				return true;
			}
			String crmStatusCode = "";
			Boolean isMarkDeliveredCrm = false;
			if (json.has("status") && (json.getString("status").equalsIgnoreCase("delivered")
					|| json.getString("status").equalsIgnoreCase("full_delivered")
					|| json.getString("status").equalsIgnoreCase("full_delivered_with_return"))) {
				order.setState(OrderState.COMPLETED);
				order.setStatus(OrderStatus.STATE_COMPLETE.DELIVERED);
				crmStatusCode = "110";
				isMarkDeliveredCrm = true;
				walletService.creditRefereeWalletEvent(order, String.valueOf(order.getId()),
						WalletTransactionInfo.TRANSACTION_SOURCE.REFERRED_ORDER_DELIVERED);
			} else if (json.has("status") && json.getString("status").equalsIgnoreCase("return")) {
				order.setState(OrderState.COMPLETED);
				order.setStatus(OrderStatus.STATE_COMPLETE.RETURNED);
				crmStatusCode = "120";
				isMarkDeliveredCrm = true;
				if (order.getRedeemedCarePoints() > 0) {
					walletService.creditCustomerWalletReturnCancellationEvent(order, String.valueOf(order.getId()),
							WalletTransactionInfo.TRANSACTION_SOURCE.ORDER_RETURNED);
				}
			} else if (json.has("status") && json.getString("status").equalsIgnoreCase("UNSEEN")) {
				order.setState(OrderState.SHIPPED);
				order.setStatus(OrderStatus.STATE_SHIPPED.DISPATCHED);
				crmStatusCode = "90";
				isMarkDeliveredCrm = true;
			}
			if (isMarkDeliveredCrm) {
				orderRepository.save(order);
				try {
					restTemplate
							.getForObject(APIEndPoint.LIFCARE_SERVICE + "?method=update_order_state&order_status_code="
									+ crmStatusCode + "&order_number=" + orderId, String.class);
				} catch (Exception e) {
					LOGGER.error("Unable to update CRM Application order status");
				}
			}
		} catch (Exception e) {
			throw e;
		}
		return true;
	}

	// @Override
	// public Order partialOrderItemUpdate(String orderNumber, OrderUpdateEvent
	// orderUpdateEvent) {
	// Order order = orderRepository.findByOrderNumber(orderNumber);
	// if (order == null) {
	// throw new OrderNotFoundException("Order " + orderNumber + "not found");
	// }
	// order.setRedeemedCarePoints((int)
	// (orderUpdateEvent.getRedeemedCarePoints()));
	// order.setTotalMrp(orderUpdateEvent.getTotalMrp());
	// order.setTotalSalePrice(orderUpdateEvent.getTotalSalePrice());
	// List<OrderItem> orderItemList =
	// orderItemService.findByOrderId(order.getId());
	// if (orderItemList == null || orderItemList.isEmpty()) {
	// throw new OrderItemNotFoundException("order items Not Found with order
	// number=" + orderNumber);
	// }
	// List<OrderItemArray> partilOrderItemList =
	// orderUpdateEvent.getOrderItemArray();
	//
	// ListIterator<OrderItem> listIter = orderItemList.listIterator();
	// // Itreator for order itemlist
	// Map<String, OrderItemArray> orderItemArraySkus = new HashMap<String,
	// OrderItemArray>();
	// Map<String, OrderItemArray> orderItemArrayNames = new HashMap<String,
	// OrderItemArray>();
	// for (OrderItemArray orderItemArray : partilOrderItemList) {
	// orderItemArraySkus.put(orderItemArray.getSku(), orderItemArray);
	// orderItemArrayNames.put(orderItemArray.getName(), orderItemArray);
	// }
	// List<OrderItem> deleteOrderItems = new ArrayList<OrderItem>();
	// List<OrderItem> updateOrderItems = new ArrayList<OrderItem>();
	// while (listIter.hasNext()) {
	// OrderItem orderItem = listIter.next();
	// Boolean isDelete = true;
	// OrderItemArray orderItemArray = null;
	// if (orderItemArraySkus.containsKey(orderItem.getSku())) {
	// orderItemArray = orderItemArraySkus.get(orderItem.getSku());
	// } else if (orderItemArrayNames.containsKey(orderItem.getName())) {
	// orderItemArray = orderItemArrayNames.get(orderItem.getName());
	// }
	// if (orderItemArray != null) {
	// orderItem.setSalePrice(Float.parseFloat(orderItemArray.getSalePrice()));
	// orderItem.setMrp(Float.parseFloat(orderItemArray.getMrp()));
	// orderItem.setDiscount(Float.parseFloat(orderItemArray.getDiscount()));
	// orderItem.setQuantity(Integer.parseInt(orderItemArray.getQuantity()));
	// isDelete = false;
	// updateOrderItems.add(orderItem);
	// }
	// if (isDelete) {
	// // orderItemList.remove(orderItem);
	// deleteOrderItems.add(orderItem);
	// }
	// }
	// orderItemService.save(order.getId(), updateOrderItems);
	// orderItemService.delete(deleteOrderItems);
	// return orderRepository.save(order);
	// }

	@Override
	public List<Reason> getCancellationReason(String group) {
		return reasonRepository.findByGroupOrderByPriorityDesc(group);
	}

//	@Override
//	public List<Reason> getLabCancellationReason() {
//		return reasonRepository.findByGroupOrderByPriorityDesc(Reason.GROUP.LAB_CANCEL);
//	}

	
	@Override
	public List<Reason> getHoldReason() {
		return reasonRepository.findByGroupOrderByPriorityDesc(Reason.GROUP.HOLD);
	}

	@Override
	public List<Reason> getUnHoldReason() {
		return reasonRepository.findByGroupOrderByPriorityDesc(Reason.GROUP.UNHOLD);
	}

	@Override
	public Boolean cancelOrder(User user, String orderId, StatusUpdateRequest statusUpdateRequest) {
		if (statusUpdateRequest == null || orderId == null) {
			throw new BadRequestException("Invalid parameters provided");
		}
		try {
			Order order = orderRepository.findByDisplayOrderId(orderId);
			if (order == null) {
				throw new OrderNotFoundException("Order " + orderId + "not found");
			}

			// update inventory on urgent order
			if (Order.CATEGORY.MEDICINE.equalsIgnoreCase(order.getCategory())) {
				if (OrderStatus.INVENTORY_REDUCTION_ORDER_STATUS.contains(order.getStatus()) && !order.getStatus().equalsIgnoreCase(OrderStatus.STATE_PROCESSING.PART_FULFILLABLE)) {
					updateProductStock(order, getUpdatedInventoryMap(order.getId()));
				}
			} else if (Order.CATEGORY.LAB.equalsIgnoreCase(order.getCategory())) {
				userService.cancelAppointment(order.getAppointmentId());
			}

			HashMap<String, String> result = new HashMap<String, String>();
			validateCancelOrderRequest(order, statusUpdateRequest);
			order.setState(OrderState.CANCELED);
			order.setStatus(OrderStatus.STATE_CANCELLED.CANCELLED);
			order.setStatusComment(statusUpdateRequest.getReason());
			orderRepository.save(order);
			result.put("status", "success");
			orderEventService.cancelOrderEvent(order, OrderEvent.ORDER_CANCELLED_CANCELLED, user);
			return true;
		} catch (Exception e) {
			throw e;
		}

	}

	private void validateCancelOrderRequest(Order order, StatusUpdateRequest statusUpdateRequest) {
		if (statusUpdateRequest == null || order == null) {
			throw new BadRequestException("Invalid parameters provided");
		}
		if (statusUpdateRequest.getReason() == null) {
			throw new CancelOrderException("Reason not found");
		}
		if (OrderState.CANCELED.equalsIgnoreCase(order.getState())) {
			throw new CancelOrderException("Order already cancelled");
		}
		if (OrderState.SHIPPED.equalsIgnoreCase(order.getState())
				|| OrderState.COMPLETED.equalsIgnoreCase(order.getState())
				|| OrderState.CLOSED.equalsIgnoreCase(order.getState())
				|| OrderState.RETURNED.equalsIgnoreCase(order.getState())) {
			throw new CancelOrderException("Order not in cancellable state");
		}
		return;
	}

	@Override
	public Boolean holdOrder(User user, String orderId, StatusUpdateRequest statusUpdateRequest) {
		if (statusUpdateRequest == null || orderId == null) {
			throw new BadRequestException("Invalid parameters provided");
		}
		try {
			Order order = orderRepository.findByDisplayOrderId(orderId);
			if (order == null) {
				throw new OrderNotFoundException("Order " + orderId + " not found");
			}
			validateHoldOrderRequest(order, statusUpdateRequest);
			HashMap<String, String> result = new HashMap<String, String>();
			order.setManualHold(true);
			order.setManualHoldReason(statusUpdateRequest.getReason());
			orderRepository.save(order);
			result.put("status", "success");
			orderEventService.orderEvent(order, orderId, OrderEvent.ORDER_HOLD, user);
			return true;
		} catch (Exception e) {
			throw e;
		}

	}

	private void validateHoldOrderRequest(Order order, StatusUpdateRequest statusUpdateRequest) {
		if (statusUpdateRequest == null || order == null) {
			throw new BadRequestException("Invalid parameters provided");
		}
		if (statusUpdateRequest.getReason() == null) {
			throw new HoldOrderException("Reason not found");
		}
		if (OrderState.CANCELED.equalsIgnoreCase(order.getState())) {
			throw new HoldOrderException("Order already cancelled");
		}
		if (OrderState.SHIPPED.equalsIgnoreCase(order.getState())
				|| OrderState.COMPLETED.equalsIgnoreCase(order.getState())
				|| OrderState.CLOSED.equalsIgnoreCase(order.getState())
				|| OrderState.RETURNED.equalsIgnoreCase(order.getState())) {
			throw new HoldOrderException("Order not in hold state");
		}
		return;
	}

	@Override
	public Boolean unHoldOrder(User user, String orderId, StatusUpdateRequest statusUpdateRequest) {
		if (orderId == null) {
			throw new BadRequestException("Invalid parameters provided");
		}
		try {
			Order order = orderRepository.findByDisplayOrderId(orderId);
			if (order == null) {
				throw new OrderNotFoundException("Order " + orderId + " not found");
			}
			validateUnHoldOrderRequest(order, statusUpdateRequest);
			HashMap<String, String> result = new HashMap<String, String>();
			order.setManualHold(false);
			orderRepository.save(order);
			result.put("status", "success");
			orderEventService.orderEvent(order, orderId, OrderEvent.ORDER_UNHOLD, user);
			return true;
		} catch (Exception e) {
			throw e;
		}

	}

	private void validateUnHoldOrderRequest(Order order, StatusUpdateRequest statusUpdateRequest) {
		if (order == null) {
			throw new BadRequestException("Invalid parameters provided");
		}
		if (OrderState.CANCELED.equalsIgnoreCase(order.getState())) {
			throw new HoldOrderException("Order already cancelled");
		}
		if (OrderState.SHIPPED.equalsIgnoreCase(order.getState())
				|| OrderState.COMPLETED.equalsIgnoreCase(order.getState())
				|| OrderState.CLOSED.equalsIgnoreCase(order.getState())
				|| OrderState.RETURNED.equalsIgnoreCase(order.getState())) {
			throw new HoldOrderException("Order not in unhold state");
		}
		return;
	}

	@Transactional
	@Override
	public Order updateOrder(User user, UpdateOrderObject updateOrderObject, Order order, boolean isFinalPrice, OrderPrice orderPrice, Boolean applyCoupon, Boolean isChildCreating, boolean isInvoiced)
			throws Exception {
		
		order = mapSkuWithOrderItems(user, updateOrderObject, order, isFinalPrice, orderPrice, applyCoupon,
				isChildCreating, isInvoiced);
		 
		if (updateOrderObject == null) {
			return order;
		}
		if (!isFinalPrice) {
			LOGGER.info("Mac3 parent order:: " + order);
			updateOrderItemWithCatalogAndApplyCoupon(order, StringUtils.EMPTY, isFinalPrice, order.getCouponCode(),
					"update", applyCoupon);
			LOGGER.info("Mac4 parent order:: " + order);
		}
		LOGGER.info("Mac4 parent order:: " + order);
		List<Order> associatedOrders = orderRepository.findAllByParentId(order.getParentId());

		
		if (!isInvoiced && associatedOrders.size() == 0 && order.getParentId() == 0) {
			customerPriceAdjustment(order, order.getCustomerAmountToPay(), isFinalPrice);
		}
		
		try {
			LOGGER.info("ICE ITEM FOR CHANGE PRICE");
			if (associatedOrders.size() > 0 || order.getParentId() != 0) {
				changeOrderPrice(order, updateOrderObject, associatedOrders);
			}
		} catch (Exception e) {
			LOGGER.error("CANNOT CHANGE ORDER PRICE DUE TO ",e.getMessage());
			throw e;
		}
	
		return orderRepository.save(order);
	}

	private Order mapSkuWithOrderItems(User user, UpdateOrderObject updateOrderObject, Order order, boolean isFinalPrice,
			OrderPrice orderPrice, Boolean applyCoupon, Boolean isChildCreating, boolean isInvoiced) {
		List<OrderItem> newOrderItems = new ArrayList<>();
		List<ItemInfo> newItemInfos = new ArrayList<>();
		float discount = 15;

		Map<String, ItemInfo> skuItemsMap = new HashMap<String, ItemInfo>();
		for (ItemInfo itemInfo : updateOrderObject.getItemsInfos()) {
			if (null != itemInfo.getSku()) {
				skuItemsMap.put(itemInfo.getSku(), itemInfo);
			}
		}

		int previousQty = 0;
		int currentQty = 0;
		for (OrderItem orderItem : order.getOrderItems()) {
			previousQty = previousQty + orderItem.getOrderedQuantity();
			if (!skuItemsMap.containsKey(orderItem.getSku())) {
				orderItem.setQuantity(0);
				orderItem.setLooseQuantity(0);
			} else {
				ItemInfo itemInfo = skuItemsMap.get(orderItem.getSku());
				orderItem.setName(itemInfo.getName() != null ? itemInfo.getName() : orderItem.getName());
				orderItem.setActive(itemInfo.getIsActive() != null ? itemInfo.getIsActive() : orderItem.isActive());
				orderItem
						.setImageUrl(itemInfo.getImageUrl() != null ? itemInfo.getImageUrl() : orderItem.getImageUrl());
				orderItem.setSalePrice(
						itemInfo.getSalePrice() != null ? itemInfo.getSalePrice() : orderItem.getSalePrice());
				orderItem.setMrp(itemInfo.getMrp() != null ? itemInfo.getMrp() : orderItem.getMrp());
				orderItem.setTax(itemInfo.getTax() != null ? itemInfo.getTax() : orderItem.getTax());
				orderItem
						.setDiscount(itemInfo.getDiscount() != null ? itemInfo.getDiscount() : orderItem.getDiscount());
				orderItem
						.setQuantity(itemInfo.getQuantity() != null ? itemInfo.getQuantity() : orderItem.getQuantity());
				orderItem
				.setLooseQuantity(itemInfo.getLooseQuantity() != null ? itemInfo.getLooseQuantity() : orderItem.getLooseQuantity());
				orderItem.setPerPackQty(itemInfo.getPerPackQty() != null && itemInfo.getPerPackQty() > 0 ? itemInfo.getPerPackQty() : orderItem.getPerPackQty());
				orderItem.setPerPackQty(orderItem.getPerPackQty() > 0 ? orderItem.getPerPackQty() : 1);
				orderItem.setBrandName(
						itemInfo.getBrandName() != null ? itemInfo.getBrandName() : orderItem.getBrandName());
				orderItem.setStatus(itemInfo.getStatus() != null ? itemInfo.getStatus() : orderItem.getStatus());
				orderItem.setStatusReason(
						itemInfo.getReason() != null ? itemInfo.getReason() : orderItem.getStatusReason());
				orderItem.setUpdatedBy(
						itemInfo.getUpdatedBy() != null ? itemInfo.getUpdatedBy() : orderItem.getUpdatedBy());
				orderItem.setUpdatedByName(itemInfo.getUpdatedByName() != null ? itemInfo.getUpdatedByName()
						: orderItem.getUpdatedByName());
				skuItemsMap.remove(orderItem.getSku());
			}
			discount = orderItem.getDiscount();
			newOrderItems.add(orderItem);
		}

		long orderId = order.getId();
		if (!skuItemsMap.isEmpty()) {
			skuItemsMap.forEach((sku, itemInfo) -> {
				if (itemInfo.getSku() != null) {
					OrderItem orderItem = new OrderItem();
					orderItem.setSku(itemInfo.getSku());
					orderItem.setName(itemInfo.getName() != null ? itemInfo.getName() : itemInfo.getSku());
					orderItem.setOrderedQuantity(itemInfo.getQuantity() != null ? itemInfo.getQuantity() : 0);
					orderItem.setQuantity(itemInfo.getQuantity() != null ? itemInfo.getQuantity() : 0);
					orderItem.setLooseQuantity(itemInfo.getLooseQuantity() != null ? itemInfo.getLooseQuantity() : 0);
					orderItem.setPerPackQty(itemInfo.getPerPackQty() != null && itemInfo.getPerPackQty() > 0 ? itemInfo.getPerPackQty() : 1);
					orderItem.setActive(itemInfo.getIsActive() != null ? itemInfo.getIsActive() : true);
					orderItem.setMrp(itemInfo.getMrp() != null ? itemInfo.getMrp() : 0);
					orderItem.setSalePrice(itemInfo.getSalePrice() != null ? itemInfo.getSalePrice() : 0);
					orderItem.setTax(itemInfo.getTax() != null ? itemInfo.getTax() : 0);
					orderItem.setDiscount(itemInfo.getDiscount() != null ? itemInfo.getDiscount() : 0);
					orderItem.setBrandName(itemInfo.getBrandName());
					orderItem.setState("NEW");
					orderItem.setStatus("VERIFIED");
					orderItem.setStatusReason(itemInfo.getReason());
					orderItem.setOrderId(orderId);
					orderItem.setUpdatedByName(itemInfo.getUpdatedByName() != null ? itemInfo.getUpdatedByName()
							: orderItem.getUpdatedByName());
					newOrderItems.add(orderItem);
				}

			});
		}

		float orderSalePrice = 0;
		float orderMrp = 0;
		
		String userId = null;
		try {
			userId = ((Long) user.getId()).toString();
		} catch (Exception e) {

		}
		for (OrderItem orderItem : newOrderItems) {
			if (orderItem.getQuantity() <= 0 && orderItem.getLooseQuantity() <= 0) {
				orderItem.setActive(false);
			}

			orderItem.setUpdatedBy(userId);

			if (orderItem.isActive()) {
				if(orderItem.getPerPackQty() <= 0) {
					orderItem.setPerPackQty(1);
				}
				// orderSalePrice += orderItem.getMrp() * orderItem.getQuantity() * (100 -
				// orderItem.getDiscount()) / 100;
				//orderMrp += orderItem.getMrp() * orderItem.getQuantity();
				orderMrp += orderItem.getMrp() * (orderItem.getQuantity() + ((float) orderItem.getLooseQuantity() / (float) orderItem.getPerPackQty()));
				orderSalePrice += orderItem.getMrp() * (orderItem.getQuantity() + ((float) orderItem.getLooseQuantity() / (float) orderItem.getPerPackQty())) * (100 - orderItem.getDiscount()) / 100;
				currentQty = currentQty + orderItem.getOrderedQuantity();
			}
		}
		if (isChildCreating) {
			double ratio = 1;
			if (orderPrice.getTotalMrp() > 0) {
				ratio = orderMrp / orderPrice.getTotalMrp();
			} else if (previousQty > 0) {
				ratio = currentQty / previousQty;
			}
			LOGGER.info("Mac parent oldorder2:: " + order);
			OrderPrice orderNewPrice = getChildOrderPrice(orderPrice, ratio, true);
			LOGGER.info("Mac parent orderNewPrice:: " + orderNewPrice);
			order = setOrderPriceFromOrderPrice(order, orderNewPrice);
		}
		LOGGER.info("Mac parent order:: " +  order);
		order.setDiscount(orderMrp - orderSalePrice);
		
		float couponDiscount = 0;
		if (StringUtils.isNotBlank(order.getCouponCode())) {
			couponDiscount = order.getCouponDiscount();
		}
		float usedCarePoint = order.getRedeemedCarePoints();
		float usedCash = order.getRedeemedCash();
		
		if (null != updateOrderObject.getCoupanDiscount()) {
			couponDiscount = (new Float(updateOrderObject.getCoupanDiscount()));
		}
		if (null != updateOrderObject.getUsedCarePoint()) {
			usedCarePoint = (new Float(updateOrderObject.getUsedCarePoint()));
		}
		if (updateOrderObject.getRedeemedCash() > 0) {
			usedCash = (new Float(updateOrderObject.getRedeemedCash()));
		}
		// orderSalePrice = orderSalePrice - (usedCarePoint + couponDiscount + usedCash)
		// + order.getShippingCharge() + (float) order.getUrgentDeliveryCharge();
		order.setTotalMrp(orderMrp);
		order.setRedeemedCarePoints((int) usedCarePoint);
		order.setRedeemedCash(usedCash);
		order.setCouponDiscount(couponDiscount);

		LOGGER.info("Mac5 orderItems:: " +  order.getOrderItems());
		//orderItemService.delete(order.getOrderItems());

		// order.setOrderItems(orderItemService.updateOrderLineInfo(order,
		// newOrderItems, null, isFinalPrice));
		// LOGGER.debug("Updated Order-Items : "+order.getOrderItems());
		// applyCoupon(order, order.getCouponCode(), "update");
		order.setOrderItems(newOrderItems);
		LOGGER.info("Mac2 parent order:: " +  order);
		if (!isFinalPrice) {
			LOGGER.info("Mac3 parent order:: " +  order);
			String couponCode = order.getCouponCode();
			if(StringUtils.isNotBlank(order.getManualCouponCode())) {
				couponCode = order.getManualCouponCode();
			}
			updateOrderItemWithCatalogAndApplyCoupon(order, StringUtils.EMPTY, isFinalPrice, couponCode,
					"update", applyCoupon);
			LOGGER.info("Mac4 parent order:: " +  order);
		}
		if (!isInvoiced) {
			customerPriceAdjustment(order, order.getCustomerAmountToPay(), isFinalPrice);
		}
		LOGGER.info("Mac5 parent order:: " +  order);
		order.setTotalDiscount(order.getDiscount() + order.getCouponDiscount());
		if(updateOrderObject.getShippingCharge() > 0) {
			order.setShippingCharge(updateOrderObject.getShippingCharge());
		} 
		LOGGER.info("Mac6 parent order:: " +  order);
		order.setTotalSalePrice(order.getTotalMrp() + order.getShippingCharge() + new Double(order.getReportDeliveryCharge()).floatValue() - order.getTotalDiscount()
				+ (float) order.getUrgentDeliveryCharge() - order.getRedeemedCarePoints() - order.getRedeemedCash());
		order.setTotalSalePrice(order.getTotalSalePrice() > 0 ? order.getTotalSalePrice() : 0);
		if (isFinalPrice && updateOrderObject.getExternalInvoiceAmount() != null
				&& updateOrderObject.getExternalInvoiceAmount() > 0) {
			order.setExternalInvoiceAmount(updateOrderObject.getExternalInvoiceAmount());
		}
		LOGGER.info("Mac7 parent order:: " +  order);
		orderItemService.save(order.getOrderItems());
		 
		return order;
	}

	private void customerPriceAdjustment(Order order, float amountToPay, boolean isFinalPrice) {
		float amountPaid = ((float) order.getGatewayAmount() + order.getRedeemedCash() + order.getRedeemedCarePoints() + order.getRedeemedCouponCashback());
		double podAmount = order.getPodAmount();
		float extraAmount = amountToPay - amountPaid - (float) podAmount;
		if (extraAmount != 0 && isFinalPrice) {
			if (extraAmount > 0) {
				try {
					Map<String, Object> walletDetails = paymentService.getWalletDetails(order.getCustomerId(),
							extraAmount, 0);
					if (walletDetails != null && !walletDetails.isEmpty()) {
						int redeemedPoints = 0;
						float redeemedCash = 0;
						if (walletDetails.containsKey("applicable_cash")
								&& walletDetails.get("applicable_cash") != null) {
							redeemedCash = Float.parseFloat(String.valueOf(walletDetails.get("applicable_cash")));
						}
						if (walletDetails.containsKey("applicable_point")
								&& walletDetails.get("applicable_point") != null) {
							redeemedPoints = Integer.parseInt(String.valueOf(walletDetails.get("applicable_point")));
						}
						OrderWalletPaymentRequest orderWalletPaymentRequest = new OrderWalletPaymentRequest();
						orderWalletPaymentRequest.setOrderId(order.getId());
						orderWalletPaymentRequest.setCustomerId(order.getCustomerId());
						orderWalletPaymentRequest.setTransactionType(OrderWalletPaymentRequest.TRANSACTION_TYPE.DEBIT);
						orderWalletPaymentRequest
								.setSource(WalletTransactionInfo.TRANSACTION_SOURCE.ORDER_PAYMENT_EXTRA_AMOUNT);

						WalletTransactionInfo walletTransactionInfo = null;
						if (redeemedPoints > 0) {
							orderWalletPaymentRequest.setAmount(redeemedPoints);
							orderWalletPaymentRequest
									.setWalletMethod(OrderWalletPaymentRequest.WALLET_METHOD.CARE_POINT);
							try {
								walletTransactionInfo = paymentService
										.processOrderWalletTransaction(orderWalletPaymentRequest);
								if (walletTransactionInfo != null
										&& StringUtils.isNotBlank(walletTransactionInfo.getReferenceId())) {
									extraAmount = extraAmount - redeemedPoints;
									order.setRedeemedCarePoints(order.getRedeemedCarePoints() + redeemedPoints);
								}
							} catch (Exception e) {
								LOGGER.error("Exception while debiting wallet care point : {}", e.getMessage());
							}
						}

						if (redeemedCash > 0) {
							orderWalletPaymentRequest.setAmount(redeemedCash);
							orderWalletPaymentRequest
									.setWalletMethod(OrderWalletPaymentRequest.WALLET_METHOD.CARE_POINT_PLUS);
							try {
								walletTransactionInfo = paymentService
										.processOrderWalletTransaction(orderWalletPaymentRequest);
								if (walletTransactionInfo != null
										&& StringUtils.isNotBlank(walletTransactionInfo.getReferenceId())) {
									extraAmount = extraAmount - redeemedCash;
									order.setRedeemedCash(order.getRedeemedCash() + redeemedCash);
								}
							} catch (Exception e) {
								LOGGER.error("Exception while debiting wallet care point plus : {}", e.getMessage());
							}
						}
					}
				} catch (Exception e) {
					LOGGER.error("Exception in Getting wallet info {}", order.getCustomerId());
				}
			} else {
				try {
					float extraAmountToRefund = (amountToPay - amountPaid) < 0 ? (-1) * (amountToPay - amountPaid) : 0;
					refundCustomerOrderExtraAmount(order, extraAmountToRefund);
				} catch (Exception e) {
					LOGGER.error("Exception in crediting wallet : {}", e.getMessage());
				}
			}
		}
		double finalAmountPaid = ((float) order.getGatewayAmount() + order.getRedeemedCash()
				+ order.getRedeemedCarePoints());
		order.setPodAmount(amountToPay - finalAmountPaid);
		order.setPodAmount(order.getPodAmount() >= 1 ? order.getPodAmount() : 0);
		order.setOrderType(Order.ORDER_TYPE.COD);
		if (order.getPodAmount() <= 0) {
			order.setOrderType(Order.ORDER_TYPE.PREPAID);
		}
	}

	private void refundCustomerOrderExtraAmount(Order order, float extraAmount) {
		if (extraAmount > 0) {
			double refundCash = order.getGatewayAmount() >= extraAmount ? extraAmount : order.getGatewayAmount();
			order.setGatewayAmount(order.getGatewayAmount() - refundCash);
			extraAmount -= refundCash;
			extraAmount = extraAmount > 0 ? extraAmount : 0;
			double cashRefundAmt = order.getRedeemedCash() >= extraAmount ? extraAmount : order.getRedeemedCash();
			refundCash += cashRefundAmt;
			extraAmount -= cashRefundAmt;
			extraAmount = extraAmount > 0 ? extraAmount : 0;
			double refundCarePoint = order.getRedeemedCarePoints() >= extraAmount ? extraAmount
					: order.getRedeemedCarePoints();
			extraAmount -= refundCarePoint;
			extraAmount = extraAmount > 0 ? extraAmount : 0;
			refundCash += extraAmount;
			OrderWalletPaymentRequest orderWalletPaymentRequest = new OrderWalletPaymentRequest();
			orderWalletPaymentRequest.setOrderId(order.getId());
			orderWalletPaymentRequest.setCustomerId(order.getCustomerId());
			orderWalletPaymentRequest.setTransactionType(OrderWalletPaymentRequest.TRANSACTION_TYPE.CREDIT);
			orderWalletPaymentRequest
					.setSource(WalletTransactionInfo.TRANSACTION_SOURCE.ORDER_PAYMENT_GATEWAY_CASH_EXTRA_AMOUNT);
			if (refundCash > 0) {
				orderWalletPaymentRequest.setAmount(refundCash);
				orderWalletPaymentRequest.setWalletMethod(OrderWalletPaymentRequest.WALLET_METHOD.CARE_POINT_PLUS);
				try {
					paymentService.processOrderWalletTransaction(orderWalletPaymentRequest);
					order.setRedeemedCash((float) (order.getRedeemedCash() - refundCash));
					order.setRedeemedCash(order.getRedeemedCash() > 0 ? order.getRedeemedCash() : 0);
				} catch (Exception e) {
					LOGGER.error("Exception while crediting wallet care point plus : {}", e.getMessage());
				}
			}
			if (refundCarePoint > 0) {
				orderWalletPaymentRequest.setAmount(refundCarePoint);
				orderWalletPaymentRequest.setWalletMethod(OrderWalletPaymentRequest.WALLET_METHOD.CARE_POINT);
				try {
					paymentService.processOrderWalletTransaction(orderWalletPaymentRequest);
					order.setRedeemedCarePoints((int) Math.round(order.getRedeemedCarePoints() - refundCarePoint));
					order.setRedeemedCarePoints(order.getRedeemedCarePoints() > 0 ? order.getRedeemedCarePoints() : 0);
				} catch (Exception e) {
					LOGGER.error("Exception while crediting wallet care point : {}", e.getMessage());
				}
			}
		}
	}

	@Override
	public Map<String, Object> updateCoupon(User user, long orderId, String couponCode) throws Exception {
		if(orderId <= 0 || StringUtils.isBlank(couponCode)) {
			throw new IllegalArgumentException("Invalid order id / coupon code specified");
		}
		Map<String, Object> responseHashmap = new HashMap<String, Object>();
		Order order = orderRepository.findOne(orderId);
		if (order == null) {
			throw new OrderNotFoundException("No order found for order id : " + orderId);
		}
		LOGGER.debug("User id :- {} name :-  {} has requested to change coupon form {} -to- {} for order id:- {}",user.getId(), user.getFirstName(), order.getCouponCode(),couponCode, orderId );
		responseHashmap.put("payload", order);

		List<OrderItem> orderItems = orderItemService.findByOrderId(orderId);
		// order.setOrderItems(orderItemService.updateOrderLineInfo(order, orderItems,
		// null, false));
		//
		// responseHashmap = applyCoupon(order, couponCode, "update");
		order.setOrderItems(orderItems);
		String oldCouponCode = order.getCouponCode();
		boolean isCouponUpdated = false;

		responseHashmap = updateOrderItemWithCatalogAndApplyCoupon(order, null, false, couponCode, "update", true);
		in.lifcare.core.response.model.Error error = (Error) responseHashmap.getOrDefault("error", null);
		
		if( error == null ) {
			if(StringUtils.isNotBlank(oldCouponCode) && !oldCouponCode.equalsIgnoreCase(order.getCouponCode())) {
				order.setManualCouponCode(oldCouponCode);
				isCouponUpdated = true;
			}
			if( StringUtils.isBlank(oldCouponCode) && StringUtils.isNotBlank(order.getCouponCode()) ) {
				order.setManualCouponCode(StringUtils.EMPTY);
				isCouponUpdated = true;
			}
		}

		if (isCouponUpdated) {
			try {
				orderEventService.updateOrderEvent(order, order.getId() + order.getUpdatedAt().toString(),
						OrderEvent.ORDER_COUPON_CODE_UPDATED, user, order.getFacilityCode(), null, null);
				LOGGER.debug("Genrate update coupon code Event for order id: " + orderId);
			} catch (Exception e) {
				LOGGER.error("Order Coupon-code updation Event genration with order-id " + orderId + " failed due to : "
						+ e.getMessage(), e);
			}
			order.setManualCouponCode(order.getCouponCode());	
		}
		if ("DEFAULT".equalsIgnoreCase(couponCode)) {
			order.setManualCouponCode(StringUtils.EMPTY);
		}
		orderRepository.save(order);
		orderItemService.save(order.getOrderItems());

		responseHashmap.put("payload", order);
		return responseHashmap;
	}

	@SuppressWarnings({ "unchecked" })
	@HystrixCommand(fallbackMethod = "couponServiceNotAvailable")
	private Map<String, Object> applyCoupon(Order order, String couponCode, String action) {
		Map<String, Object> responseHashmap = new HashMap<String, Object>();
		if(Order.PROCUREMENT_TYPE.BULK.equalsIgnoreCase(order.getProcurementType())) {
			return responseHashmap;
		}
		in.lifcare.core.response.model.Error error = null;
		if (null != order) {
			if (null == couponCode || StringUtils.isBlank(couponCode)) {
				couponCode = "DEFAULT";
				order.setBestCoupon(true);
			} else {
				order.setBestCoupon(false);
			}
			
			if(order.getShippingAddress() == null) {
				long shippingAddressId = order.getShippingAddressId(); 
				if(shippingAddressId > 0 && order.getCustomerId() > 0 ) {
					ShippingAddress shippingAddress = customerService.getShippingAddressByCustomerIdAndShippingAddressId(order.getCustomerId(), shippingAddressId);
					if(shippingAddress != null) {
						order.setShippingAddress(shippingAddress);
					}
				}
				
			}
			
			/*
			 * if(order.getOrderItems() != null && !order.getOrderItems().isEmpty()) { for
			 * (OrderItem orderItem : order.getOrderItems()) { int percentage = (int)
			 * (((orderItem.getMrp() - orderItem.getSalePrice())/orderItem.getMrp()) * 100);
			 * if(!validDiscounts.contains(percentage)) {
			 * orderItem.setDiscountPercentage((float) 15); orderItem.setDiscount((float)
			 * 15); orderItem.setSalePrice((float) (orderItem.getMrp() * 0.85)); } } }
			 */
			String applyCouponUrl = APIEndPoint.COUPON + "/" + couponCode + "/" + action;

			try {

				Response<Response> response = microserviceClient.postForObject(applyCouponUrl, order, Response.class);

				error = (in.lifcare.core.response.model.Error) response
						.populateErrorUsingJson(in.lifcare.core.response.model.Error.class);
				if (error != null) {
					order.setBestCoupon(true);
					responseHashmap.put("error", error);
				} else {
					order.setBestCoupon(false);
				}
				Order appliedCouponOrder = (Order) response.populatePayloadUsingJson(Order.class);
				if (null != appliedCouponOrder) {
					order.setCouponCode(appliedCouponOrder.getCouponCode());
					order.setCouponDiscount(appliedCouponOrder.getCouponDiscount());
					order.setDiscount(appliedCouponOrder.getDiscount());
					order.setCouponDescription(appliedCouponOrder.getCouponDescription());
					order.setShortCouponDescription(appliedCouponOrder.getShortCouponDescription());
					order.setCouponCashback(appliedCouponOrder.getCouponCashback());
					order.setCashbackPercentage(appliedCouponOrder.getCashbackPercentage());
					// order.setTotalSalePrice(appliedCouponOrder.getTotalSalePrice() +
					// order.getShippingCharge());
					if (order.getOrderItems() != null && !order.getOrderItems().isEmpty()) {
						Map<String, OrderItem> skuItemsMap = new HashMap<String, OrderItem>();

						for (OrderItem appliedCpnOItem : appliedCouponOrder.getOrderItems()) {
							skuItemsMap.put(appliedCpnOItem.getSku(), appliedCpnOItem);
						}
						for (OrderItem orderItem : order.getOrderItems()) {
							if (skuItemsMap.containsKey(orderItem.getSku())) {
								orderItem.setDiscountPercentage(
										skuItemsMap.get(orderItem.getSku()).getDiscountPercentage());
								orderItem.setSalePrice(skuItemsMap.get(orderItem.getSku()).getSalePrice());
								orderItem.setDiscount(skuItemsMap.get(orderItem.getSku()).getDiscount());
							}
						}
					}
					order = updateShippingChargeInorder(order);
					order.setTotalDiscount(order.getDiscount() + order.getCouponDiscount());
					order.setTotalSalePrice(order.getTotalMrp() + order.getShippingCharge() + new Double(order.getReportDeliveryCharge()).floatValue()
							+ (float) order.getUrgentDeliveryCharge() - order.getTotalDiscount()
							- order.getRedeemedCarePoints() - order.getRedeemedCash() - order.getRedeemedCouponCashback());
					order.calculateCouponCashback();
					if (order.getTotalSalePrice() <= 0) {
						order.setTotalSalePrice(0);
					}
				}

			} catch (Exception e) {
				couponServiceNotAvailable(order, couponCode, action);
			}
		}
		return responseHashmap;
	}

	List<String> sources = Arrays.asList("ANDROID", "IOS", "MWEB", "MSITE");

	public Map<String, Object> couponServiceNotAvailable(Order order, String couponCode, String action) {
		Map<String, Object> responseHashmap = new HashMap<String, Object>();
		if (order != null && order.getOrderItems() != null) {
			float additionalDiscountPercentage = 0;
			float totalSalePrice = 0;
			float totalMrp = 0;
			if (order.getSource() != null && sources.contains(order.getSource())) {
				additionalDiscountPercentage = 2;
			}
			for (OrderItem orderItem : order.getOrderItems()) {
				float lineItemDiscountInPercentage = orderItem.getDiscount() + additionalDiscountPercentage;
				float salePrice = 0;
				if (validDiscounts.contains(orderItem.getDiscount())) {
					lineItemDiscountInPercentage = orderItem.getDiscount();
				}
				// if( lineItemDiscountInPercentage == 0 ) {
				// salePrice = (float) (orderItem.getSalePrice() * (100 -
				// additionalDiscountPercentage)/100.0);
				// if( orderItem.getMrp() > 0 ) {
				// lineItemDiscountInPercentage = (float) ((orderItem.getMrp() - salePrice) *
				// (100.0 / orderItem.getMrp()));
				// lineItemDiscountInPercentage = (float)
				// Math.round(lineItemDiscountInPercentage);
				// }
				// }
				salePrice = (float) (orderItem.getMrp() * (100 - lineItemDiscountInPercentage) / 100.0);
				totalMrp += orderItem.getMrp() * orderItem.getQuantity();
				orderItem.setDiscount(lineItemDiscountInPercentage);
				orderItem.setDiscountPercentage(lineItemDiscountInPercentage);
				orderItem.setSalePrice(salePrice);
				totalSalePrice += orderItem.getSalePrice() * orderItem.getQuantity();
			}

			// order.setTotalSalePrice(totalSalePrice);
			order.setTotalMrp(totalMrp);
			float $discount = totalMrp - totalSalePrice;
			order.setDiscount($discount > 0 ? $discount : 0);
			order.setTotalDiscount(order.getDiscount() + order.getCouponDiscount());
			order.setTotalSalePrice(order.getTotalMrp() + order.getShippingCharge() + new Double(order.getReportDeliveryCharge()).floatValue() - order.getTotalDiscount()
					- order.getRedeemedCarePoints() - order.getRedeemedCash());
			if (order.getTotalSalePrice() <= 0) {
				order.setTotalSalePrice(0);
			}
		}
		return responseHashmap;
	}

	@Override
	public Order createChild(User user, UpdateOrderObject updateOrderObject, Order oldOrder) throws Exception {
		// TODO Auto-generated method stub... add facilityId if not null on newOrder from updateOrderObject
		LOGGER.info("Old Order:: {}",  oldOrder);
		Map<String, ItemInfo> skuItemsMap = new HashMap<String, ItemInfo>();
		Order order = new Order();
		ConvertUtils.register(new SqlTimestampConverter(null), Timestamp.class);
		ConvertUtils.register(new SqlTimeConverter(null), Time.class);
		ConvertUtils.register(new SqlDateConverter(null), java.sql.Date.class);
		BeanUtilsBean.getInstance().copyProperties(order, oldOrder);
		order.setId(0);
		order.setOrderNumber(null);
		order.setStatus(OrderStatus.STATE_NEW.VERIFIED);
		order.setState(OrderState.NEW);
		order.setParentId(updateOrderObject.getParentId());
		order.setCouponDiscount(0);
		order.setDiscount(0);
		order.setExternalInvoiceAmount(0D);
		order.setGatewayAmount(0);
		order.setPodAmount(0);
		order.setRedeemedCarePoints(0);
		order.setRedeemedCash(0);
		order.setShippingCharge(0);
		order.setTotalDiscount(0);
		order.setTotalMrp(0);
		order.setTotalSalePrice(0);
		order.setTotalTaxAmount(0);
		order.setUrgentDeliveryCharge(0);
		order.setCreatedAt(null);
		order.setUpdatedAt(null);
		
		//Set Fulfillable Flag for SPLIT
		order.setFulfillable(updateOrderObject.isFulfillable());
		
		if(updateOrderObject.getFacilityId() > 0 ) {
			order.setFacilityCode((int)updateOrderObject.getFacilityId());
			
		}
		for (ItemInfo itemInfo : updateOrderObject.getItemsInfos()) {
			if(itemInfo.getQuantity() != null && itemInfo.getQuantity() > 0) {
				skuItemsMap.put(itemInfo.getSku(), itemInfo);
			}
			
		}
		order = orderRepository.save(order);
		
		
		
		double totalMrp = 0.0, totalSalePrice = 0.0, discount = 0.0;
		
		List<OrderItem> orderItems = new ArrayList<>();
		for (OrderItem oldOrderItem : order.getOrderItems()) {
			if (skuItemsMap.containsKey(oldOrderItem.getSku())) {
				OrderItem orderItem = new OrderItem();
				ConvertUtils.register(new SqlDateConverter(null), java.sql.Date.class);
				BeanUtilsBean2.getInstance().copyProperties(orderItem, oldOrderItem);
				ItemInfo itemInfo = skuItemsMap.get(orderItem.getSku());
				orderItem
						.setQuantity(itemInfo.getQuantity() != null ? itemInfo.getQuantity() : orderItem.getQuantity());
				orderItem.setId(0);
				orderItem.setCreatedAt(null);
				orderItem.setUpdatedAt(null);
				orderItem.setActive(true);
				orderItem.setProductVerified(itemInfo.isProductVerified());
				//setOrderItem(user, order, orderItem);
				orderItem.setOrderId(order.getId());
				orderItems.add(orderItem);
			} 
		}
		
		
		order.setCouponCode(StringUtils.EMPTY);
		order.setManualCouponCode(StringUtils.EMPTY);
		order.setOrderItems(orderItems);		

		ShippingAddress shippingAddress = new ShippingAddress();
		ShippingAddress oldShippingAddress = oldOrder.getShippingAddress();
		BeanUtilsBean2.getInstance().copyProperties(shippingAddress, oldShippingAddress);
		shippingAddress.setId(0);
		shippingAddress.setOrderId(order.getId());
		shippingAddress.setCreatedAt(null);
		shippingAddress.setUpdatedAt(null);
		shippingAddressRepository.save(shippingAddress);
		orderItemService.save(order.getOrderItems());
		
		return orderRepository.save(order);
	}

	@Override
	public Order updateOrderStatus(Order order, String status) {
		// TODO Auto-generated method stub
		order.setStatus(status);
		return orderRepository.save(order);
	}

	@Override
	// Digitized Reedem point Discussion
	public List<OrderItem> setOrderItems(User user, Order order, List<OrderItem> orderItems) {
		for (OrderItem orderItem : orderItems) {
			setOrderItem(user, order, orderItem);
		}
		return orderItems;
	}

	public OrderItem setOrderItem(User user, Order order, OrderItem orderItem) {
		long discountInPercentage = 15;
		if (order.getSource().equalsIgnoreCase(OrderSource.ANDROID)
				|| order.getSource().equalsIgnoreCase(OrderSource.MWEB)) {
			discountInPercentage = 17;
		}
		float salePrice = orderItem.getMrp() * (100 - discountInPercentage) / 100;
		if (orderItem.getDiscountPercentage() != null
				&& validDiscounts.contains(new Integer(orderItem.getDiscountPercentage().intValue()))) {
			discountInPercentage = orderItem.getDiscountPercentage().longValue();
			salePrice = orderItem.getMrp() * (100 - orderItem.getDiscountPercentage().longValue()) / 100;
		}
		orderItem.setCreatedBy(((Long) user.getId()).toString());
		orderItem.setOrderId(order.getId());
		orderItem.setState(order.getState());
		orderItem.setStatus(order.getStatus());
		orderItem.setOrderedQuantity(orderItem.getQuantity());
		orderItem.setActive(true);
		if (StringUtils.isBlank(orderItem.getSku())) {
			Random random = new Random();
			// generate a random integer from 0 to 899, then add 100
			int x = random.nextInt(900) + 100;
			orderItem.setSku("NEW" + x);
		}
		// orderItem.setDiscount(orderItem.getMrp() - salePrice);
		orderItem.setDiscount(discountInPercentage);
		orderItem.setSalePrice(salePrice);
		return orderItem;
	}
	
public Order setOrderPriceOldOrder(Order order, UpdateOrderObject updateOrderObject) {
		

		
		float totalSalePrice = 0;
		float totalMrp = 0;
		float discount = 0;
		List<OrderItem> orderItems = orderItemService.findByOrderId(order.getId());
		for (OrderItem orderItem : orderItems) {
			if (orderItem.isActive()) {
				totalSalePrice += orderItem.getSalePrice() * orderItem.getQuantity();
				totalMrp += orderItem.getMrp() * orderItem.getQuantity();
				discount += ((orderItem.getMrp() - orderItem.getSalePrice()) * orderItem.getQuantity());
			}
		}
		if (updateOrderObject == null) {
			
			if (totalSalePrice > 0) {
				order.setTotalSalePrice(totalSalePrice - order.getRedeemedCarePoints() - order.getRedeemedCash());
			} else {
				order.setTotalSalePrice(0);
			}
			order.setDiscount(discount);
			order.setTotalMrp(totalMrp);
			order.setTotalDiscount(discount);
			return order;
		} else {

			float invoiceDiscount = 0;
			float invoiceCarepoint = 0;
			if (updateOrderObject.getCoupanDiscount() != null) {
				invoiceDiscount = new Float(updateOrderObject.getCoupanDiscount());
			}
			if (updateOrderObject.getUsedCarePoint() != null) {
				invoiceCarepoint = new Float(updateOrderObject.getUsedCarePoint());
			}

			order.setTotalSalePrice(totalSalePrice - (invoiceCarepoint + invoiceDiscount));
			order.setDiscount(invoiceDiscount);
			order.setTotalMrp(totalMrp);
			order.setRedeemedCarePoints((new Float(invoiceCarepoint)).intValue());
			order.setTotalDiscount(invoiceDiscount);
			return order;
		}
	}

	public Order setOrderPrice(Order order, UpdateOrderObject updateOrderObject, OrderPrice orderPrice) {
		
		
		LOGGER.info("Mac1 parentOrderPrice:: " +  orderPrice);
		
		float totalSalePrice = 0;
		float totalMrp = 0;
		float discount = 0;
		List<OrderItem> orderItems = orderItemService.findByOrderId(order.getId());
		for (OrderItem orderItem : orderItems) {
			if (orderItem.isActive()) {
				totalSalePrice += orderItem.getSalePrice() * orderItem.getQuantity();
				totalMrp += orderItem.getMrp() * orderItem.getQuantity();
				discount += ((orderItem.getMrp() - orderItem.getSalePrice()) * orderItem.getQuantity());
			}
		}
		if (updateOrderObject == null) {
			double ratio = 1;
			if(totalMrp > 0 && orderPrice.getTotalMrp() > 0 && totalMrp < orderPrice.getTotalMrp()) {
				ratio = totalMrp/orderPrice.getTotalMrp();
			} 
			
			
			OrderPrice childOrderPrice = getChildOrderPrice(orderPrice, ratio, false);
			LOGGER.info("Mac1 childOrderPrice:: " +  childOrderPrice);
			order = setOrderPriceFromOrderPrice(order, childOrderPrice);
			
			LOGGER.info("Mac1 childOrder:: " +  order);
			return order;
		} else {

			float invoiceDiscount = 0;
			float invoiceCarepoint = 0;
			if (updateOrderObject.getCoupanDiscount() != null) {
				invoiceDiscount = new Float(updateOrderObject.getCoupanDiscount());
			}
			if (updateOrderObject.getUsedCarePoint() != null) {
				invoiceCarepoint = new Float(updateOrderObject.getUsedCarePoint());
			}

			order.setTotalSalePrice(totalSalePrice - (invoiceCarepoint + invoiceDiscount));
			order.setDiscount(invoiceDiscount);
			order.setTotalMrp(totalMrp);
			order.setRedeemedCarePoints((new Float(invoiceCarepoint)).intValue());
			order.setTotalDiscount(invoiceDiscount);
			return order;
		}
	}
	
	private OrderPrice getOrderPrice(Order order) {
		OrderPrice orderPrice = new OrderPrice();
		orderPrice.setCouponDiscount(order.getCouponDiscount());
		orderPrice.setDiscount(order.getDiscount());
		orderPrice.setExternalInvoiceAmount(order.getExternalInvoiceAmount());
		orderPrice.setGatewayAmount(order.getGatewayAmount());
		orderPrice.setPodAmount(order.getPodAmount());
		orderPrice.setRedeemedCarePoints(order.getRedeemedCarePoints());
		orderPrice.setRedeemedCash(order.getRedeemedCash());
		orderPrice.setShippingCharge(order.getShippingCharge());
		orderPrice.setTotalDiscount(order.getTotalDiscount());
		orderPrice.setTotalMrp(order.getTotalMrp());
		orderPrice.setTotalSalePrice(order.getTotalSalePrice());
		orderPrice.setTotalTaxAmount(order.getTotalTaxAmount());
		orderPrice.setUrgentDeliveryCharge(order.getUrgentDeliveryCharge());
		return orderPrice;
	}
	
	private Order setOrderPriceFromOrderPrice(Order order, OrderPrice orderPrice) {
		
		
		order.setCouponDiscount(orderPrice.getCouponDiscount());
		order.setDiscount(orderPrice.getDiscount());
		order.setExternalInvoiceAmount(orderPrice.getExternalInvoiceAmount());
		order.setGatewayAmount(orderPrice.getGatewayAmount());
		order.setPodAmount(orderPrice.getPodAmount());
		order.setRedeemedCarePoints(orderPrice.getRedeemedCarePoints());
		order.setRedeemedCash(orderPrice.getRedeemedCash());
		order.setShippingCharge(orderPrice.getShippingCharge());
		order.setTotalDiscount(orderPrice.getTotalDiscount());
		order.setTotalMrp(orderPrice.getTotalMrp());
		order.setTotalSalePrice(orderPrice.getTotalSalePrice());
		order.setTotalTaxAmount(orderPrice.getTotalTaxAmount());
		order.setUrgentDeliveryCharge(orderPrice.getUrgentDeliveryCharge());
		
		
		return order;
	}

	private OrderItem setPatientNameInOrderItem(Long customerId, OrderItem orderItem) {
		if (customerId != null && customerId >= 0 && orderItem != null ) {
			if (orderItem.getPatientId() != null && orderItem.getPatientId() > 0) {
				Map<Long, Patient> map = customerService.getPatients(customerId, Arrays.asList(orderItem.getPatientId()));
				if (map != null) {
					Patient patient = map.get(orderItem.getPatientId());
					orderItem.setPatientFirstName(patient.getFirstName());
					orderItem.setPatientLastName(patient.getLastName());
				}
			}
		}
		return orderItem;
	}

	@Override
	// Digitized Reedem point Discussion
	public Order addOrderItem(User user, long orderId, OrderItem orderItem, boolean isDeliveryOptionChangeAllowed,
			boolean isServiceTypeChangeAllowed, boolean isAllowed) throws Exception {

		Order order = orderRepository.findOne(orderId);
		if (order == null) {
			throw new OrderNotFoundException("Order not found for id: " + orderId);
		}
		if (StringUtils.isBlank(order.getCategory()) 
				|| (!OrderItem.PRODUCT_CATEGORY.MEMBERSHIP_CARD.equalsIgnoreCase(String.valueOf(orderItem.getProductCategory()))
						&& !order.getCategory().equalsIgnoreCase(String.valueOf(orderItem.getProductCategory())))) {
			throw new IllegalArgumentException("Order category and item product category mismatch!");
		}
		
		if(orderItem.getProductCategory().equalsIgnoreCase(OrderItem.PRODUCT_CATEGORY.MEMBERSHIP_CARD)) {
			List<OrderItem> orderItems = orderItemService.findByOrderId(orderId);
			boolean isMembershipCardExists = orderItems.stream().anyMatch(oi -> OrderItem.PRODUCT_CATEGORY.MEMBERSHIP_CARD.equalsIgnoreCase(oi.getProductCategory()) && oi.isActive() == true);
			if(isMembershipCardExists) {
				throw new IllegalArgumentException("Already a membership added. Please remove first to add");
			}
			orderItem.setQuantity(1);
			order.setMembershipAdded(true);
		}
		
		if(orderItem.getPatientId()== null || orderItem.getPatientId() <= 0) {
			if(order.getPatientId() <= 0) {
				throw new IllegalArgumentException("Invalid patient-id in order");
			}
			orderItem.setPatientId(order.getPatientId());
		}
		if (StringUtils.isNotBlank(orderItem.getSku()) && orderItem.getPatientId() != null && orderItem.getPatientId() > 0) {
			OrderItem oldOrderItem = orderItemService.getOrderAndSkuAndPatientId(orderId, orderItem.getSku(), orderItem.getPatientId());
			if (oldOrderItem != null) {
				throw new DuplicateEntryException("Sku " + orderItem.getSku() + " already added for patient-id : "+orderItem.getPatientId());
			}
		}
		
		
		
		// orderItem.setDiscount(0);

		if ((order.getDeliveryOption().equalsIgnoreCase(Order.DELIVERY_OPTION.URGENT)
				|| order.getServiceType().equalsIgnoreCase(Order.SERVICE_TYPE.LF_ASSURED))
				&& !isDeliveryOptionChangeAllowed && !isServiceTypeChangeAllowed) {
			OrderDeliveryObject deliveryObject = getOrderDeliveryObject(order);
			if (!deliveryObject.getDeliveryOption().equalsIgnoreCase(Order.DELIVERY_OPTION.URGENT)
					&& order.getDeliveryOption().equalsIgnoreCase(Order.DELIVERY_OPTION.URGENT)) {
				throw new InvalidDeliveryOptionException("Newly added item not applicable for URGENT delivery!");
			} else if (!deliveryObject.getServiceType().equalsIgnoreCase(Order.SERVICE_TYPE.LF_ASSURED)
					&& order.getServiceType().equalsIgnoreCase(Order.SERVICE_TYPE.LF_ASSURED)) {
				throw new InvalidServiceTypeException("Newly added item not applicable for LF_ASSURED!");
			}
		}

		orderItem = setOrderItem(user, order, orderItem);
		PatientMedicine patientMedicine = patientMedicineService.getPatientMedicineWithIdAndSku(orderItem.getPatientId(),
				orderItem.getSku());
		if (orderItem.getMaxOrderQuantity() != null && orderItem.getMaxOrderQuantity() > 0
				&& orderItem.getMaxOrderQuantity() < orderItem.getQuantity()) {
			throw new MaxPermissibleLimitReached(
					"Max permissible quantity limit reached for medicine : " + orderItem.getName());
		} else if (patientMedicine != null && patientMedicine.isExcessiveOrderedQuantity() && !isAllowed) {
			throw new MaxOrderedQuantityExceeded("Max ordered quantity for customer " + order.getCustomerFirstName()
					+ " exceeded for medicine : " + orderItem.getName());
		}
		
		setPatientNameInOrderItem(order.getCustomerId(), orderItem);
		orderItem.setState(order.getState());
		orderItem.setStatus(order.getStatus());
		
		orderItem = orderItemService.save(orderItem);
		// orderRepository.save(setOrderPrice(order, null));
		order = setOrderPriceOldOrder(order, null);

		if (isDeliveryOptionChangeAllowed) {
			updateDeliveryOption(orderId, order,
					OrderDeliveryObject.DELIVERY_OPTION_CHANGE_REASON.NEWLY_ADDED_ITEMS_NOT_APPLICABLE_FOR_URGENT_DELIVERY,
					false, null, user);
		}
		if (isServiceTypeChangeAllowed) {
			updateServiceType(orderId, order, false,
					OrderDeliveryObject.SERVICE_TYPE_CHANGE_REASON.NEWLY_ADDED_ITEMS_NOT_APPLICABLE_FOR_LF_ASSURED,
					null, user);
		}

		HashMap<String, Integer> map = new HashMap<>();
		map.put(orderItem.getSku(), orderItem.getQuantity());
		if (Order.CATEGORY.MEDICINE.equalsIgnoreCase(order.getCategory())) {
			updateProductStock(order, map);
		}
//		if (Order.DELIVERY_OPTION.URGENT.equalsIgnoreCase(order.getDeliveryOption())
//				|| Order.SERVICE_TYPE.LF_ASSURED.equalsIgnoreCase(order.getServiceType())) {
//			productStockService.updateExpressInventory(new Long(order.getFacilityCode()), map);
//		} else {
//			productStockService.updateInFlightInventory(new Long(order.getFacilityCode()), map);
//		}

		List<OrderItem> orderItems = orderItemService.findByOrderId(orderId);
		if (orderItems == null) {
			orderItems = new ArrayList<>();
		}
		order.setOrderItems(orderItems);
		String couponCode = order.getCouponCode();
		if(StringUtils.isNotBlank(order.getManualCouponCode())) {
			couponCode = order.getManualCouponCode();
		}
		updateOrderItemWithCatalogAndApplyCoupon(order, StringUtils.EMPTY, false, couponCode,
				"update", true);
		// applyCoupon(order, order.getCouponCode(), "update");
		// order = updateShippingChargeInorder(order);
		// order.setTotalSalePrice(order.getTotalSalePrice() +
		// order.getShippingCharge());
		orderRepository.save(order);
		orderItemService.save(order.getOrderItems());
		List<OrderItem> finalOrderItems = orderItemService.findByOrderId(orderId);
		order.setOrderItems(finalOrderItems);
		return order;
	}

	@Override
	public ShippingAddress updateShippingAddress(User user, long orderId, long shippingAddressId,
			ShippingAddress shippingAddress, boolean isDeliveryOptionChangeAllowed, boolean isServiceTypeChangeAllowed)
			throws Exception {

		if (orderId <= 0 || shippingAddress == null || shippingAddressId <= 0 || shippingAddress.getOrderId() != orderId) {
			throw new IllegalArgumentException("Invalid input param provided.");
		}
		if (!CommonUtil.isValidAddress(shippingAddress.getStreet1()) || !CommonUtil.isValidAddress(shippingAddress.getStreet2())) {
			throw new IllegalArgumentException("Invalid street1 / street2 specified in shipping address");
		}
		if (shippingAddress.getMobile() != null && !(CommonUtil.isValidMobile(shippingAddress.getMobile()))) {
			ShippingAddress oldShippingAddress = shippingAddressRepository.findOne(shippingAddress.getId());
			if (oldShippingAddress != null) {
				shippingAddress.setMobile(oldShippingAddress.getMobile());
			}
		}
		Order order = orderRepository.findOne(orderId);
		if (order != null) {
			if (Order.DELIVERY_OPTION.URGENT.equalsIgnoreCase(order.getDeliveryOption())
					|| Order.SERVICE_TYPE.LF_ASSURED.equalsIgnoreCase(order.getServiceType())) {
				// CALL SHIPPING API BASED ON PINCODE
				String pincode = String.valueOf(shippingAddress.getPincode());
				PlacePincode placePincode = null;
				if (pincode != null) {
					try {
						placePincode = shippingService.getPlaceInformationByPincode(pincode);
					} catch (Exception e) {
						LOGGER.error("Error in getting place pincode with pincode {}", pincode);
					}
					if ((order.getDeliveryOption().equalsIgnoreCase(Order.DELIVERY_OPTION.URGENT)
							|| order.getServiceType().equalsIgnoreCase(Order.SERVICE_TYPE.LF_ASSURED))
							&& !isDeliveryOptionChangeAllowed && !isServiceTypeChangeAllowed) {
						if (!placePincode.getIsUrgentDlAvailable()
								&& order.getDeliveryOption().equalsIgnoreCase(Order.DELIVERY_OPTION.URGENT)) {
							throw new InvalidDeliveryOptionException(
									"Updated address not applicable for URGENT delivery!");
						} else if (!placePincode.getIsLcAssuredAvailable()
								&& order.getServiceType().equalsIgnoreCase(Order.SERVICE_TYPE.LF_ASSURED)) {
							throw new InvalidServiceTypeException("Updated address not applicable for LF_ASSURED!");
						}
					}
					if (isDeliveryOptionChangeAllowed) {
						updateDeliveryOption(orderId, order,
								OrderDeliveryObject.DELIVERY_OPTION_CHANGE_REASON.PINCODE_IS_NOT_APPLICABLE_FOR_URGENT_DELIVERY,
								false, null, user);
					}
					if (isServiceTypeChangeAllowed) {
						updateServiceType(orderId, order, false,
								OrderDeliveryObject.SERVICE_TYPE_CHANGE_REASON.PINCODE_IS_NOT_APPLICABLE_FOR_LF_ASSURED,
								null, user);
					}
				}
			}
		}
		if (StringUtils.isBlank(shippingAddress.getFirstName()) && StringUtils.isBlank(shippingAddress.getLastName())
				&& StringUtils.isNotBlank(shippingAddress.getFullName())) {
			Name name = new Name(shippingAddress.getFullName());
			shippingAddress.setFirstName(name.getFirstName());
			shippingAddress.setLastName(name.getLastName());
		}
		shippingAddress.setId(shippingAddressId);
		shippingAddress = shippingAddressRepository.save(shippingAddress);
		try {
			orderEventService.updateOrderShippingEvent(
					shippingAddress.getOrderId() + (DateTime.now(DateTimeZone.UTC)).toString(), shippingAddress,
					OrderEvent.ORDER_SHIPPING_ADDRESS_UPDATE, user);
		} catch (Exception e) {
			LOGGER.error("Order shipping address update Event genration failed due to : " + e.getMessage(), e);
			throw e;
		}
		return shippingAddress;
	}

	/*
	 * private boolean updateShippingAddressOnOps(ShippingAddress shippingAddress) {
	 * if(shippingAddress == null){ throw new
	 * IllegalArgumentException("Invalid shipping address"); }
	 * restTemplate.put(APIEndPoint.ORDER_PROCESSING_SERVICE + "/" +
	 * shippingAddress.getOrderId() + "/shipping-address", shippingAddress,
	 * Boolean.class); return true; }
	 */

	@Override
	public OrderItem updateOrderItem(User user, long orderId, OrderItem orderItem,
			boolean isDeliveryOptionChangeAllowed, boolean isServiceTypeChangeAllowed) throws Exception {
		Order order = orderRepository.findOne(orderId);
		if (order != null && orderItem != null && orderItem.getOrderId() == orderId) {
			OrderItem pastOrderItem = orderItemService.findOne(orderItem.getId());
			if (pastOrderItem != null) {
				int pastItemQty = pastOrderItem.getQuantity();
				if (pastItemQty != orderItem.getQuantity()) {
					List<OrderItem> orderItems = new ArrayList<>();
					orderItems.add(orderItem);
					order.setOrderItems(orderItems);
					if ((order.getDeliveryOption().equalsIgnoreCase(Order.DELIVERY_OPTION.URGENT)
							|| order.getServiceType().equalsIgnoreCase(Order.SERVICE_TYPE.LF_ASSURED))
							&& !isDeliveryOptionChangeAllowed && !isServiceTypeChangeAllowed) {
						OrderDeliveryObject deliveryObject = getOrderDeliveryObject(order);
						if (!deliveryObject.getDeliveryOption().equalsIgnoreCase(Order.DELIVERY_OPTION.URGENT)
								&& order.getDeliveryOption().equalsIgnoreCase(Order.DELIVERY_OPTION.URGENT)) {
							throw new InvalidDeliveryOptionException(
									"Increased Quantity item not applicable for URGENT delivery!");
						} else if (!deliveryObject.getServiceType().equalsIgnoreCase(Order.SERVICE_TYPE.LF_ASSURED)
								&& order.getServiceType().equalsIgnoreCase(Order.SERVICE_TYPE.LF_ASSURED)) {
							throw new InvalidServiceTypeException(
									"Increased Quantity item not applicable for LF_ASSURED!");
						}
					}
					if (isDeliveryOptionChangeAllowed) {
						updateDeliveryOption(orderId, order,
								OrderDeliveryObject.DELIVERY_OPTION_CHANGE_REASON.NEWLY_ADDED_ITEMS_NOT_APPLICABLE_FOR_URGENT_DELIVERY,
								false, null, user);
					}
					if (isServiceTypeChangeAllowed) {
						updateServiceType(orderId, order, false,
								OrderDeliveryObject.SERVICE_TYPE_CHANGE_REASON.NEWLY_ADDED_ITEMS_NOT_APPLICABLE_FOR_LF_ASSURED,
								null, user);
					}

					if (orderItem.getMaxOrderQuantity() != null && orderItem.getMaxOrderQuantity() > 0
							&& orderItem.getMaxOrderQuantity() < orderItem.getQuantity()) {
						throw new MaxPermissibleLimitReached(
								"Max permissible quantity limit reached for medicine : " + orderItem.getName());
					}
					setPatientNameInOrderItem(order.getCustomerId(), orderItem);
					int diff = orderItem.getQuantity() - pastItemQty;
					HashMap<String, Integer> map = new HashMap<>();
					map.put(orderItem.getSku(), diff);
					if (Order.CATEGORY.MEDICINE.equalsIgnoreCase(order.getCategory())) {
						updateProductStock(order, map);
					}
				}
			}
			orderItem = orderItemService.updateOrderLineInfo(order, Arrays.asList(orderItem), null, false).get(0);
			// if (orderItem.getPrescriptionId() != null && orderItem.getValidDays() !=
			// null) {
			// checkPrescriptionVelidityForSku(order.getPatientId(),
			// orderItem.getPrescriptionId(), orderItem.getValidDays());
			// }
			orderItem.setUpdatedBy(((Long) user.getId()).toString());
			orderItem = orderItemService.save(orderItem);

			List<OrderItem> orderItems = orderItemService.findByOrderId(orderId);
			order.setOrderItems(orderItems);

			order = setOrderPriceOldOrder(order, null);
			String couponCode = order.getCouponCode();
			if(StringUtils.isNotBlank(order.getManualCouponCode())) {
				couponCode = order.getManualCouponCode();
			}
			updateOrderItemWithCatalogAndApplyCoupon(order, StringUtils.EMPTY, false, couponCode,
					"update", true);
			// applyCoupon(order, order.getCouponCode(), "update");
			// order = updateShippingChargeInorder(order);
			// order.setTotalSalePrice(order.getTotalSalePrice() +
			// order.getShippingCharge());

			orderRepository.save(order);
			orderItemService.save(order.getOrderItems());
			return orderItemService.findOne(orderItem.getId());
		}
		throw new InvalidOrderException("Invalid Order-Id :: " + orderId);
	}

	private void checkPrescriptionVelidityForSku(long patientId, Long prescriptionId, Integer validDays)
			throws Exception {
		if (prescriptionId == null) {
			throw new NotFoundException("Prescription-id can not be null or empty!");
		}
		Prescription prescription = null;
		try {
			prescription = prescriptionService.getPrescriptionByPatientId(patientId, String.valueOf(prescriptionId));
		} catch (Exception e) {
			LOGGER.error("Prescription not found for given patientId {} and prescriptionId {} due to {}", patientId,
					prescriptionId, e.getMessage());
			throw e;
		}
		if (prescription.getRxDate() == null) {
			throw new PrescriptionRxDateNotFound("Prescription rx-date can not be null or empty!");
		}
		if (prescription != null && validDays != null) {
			if (!isPrescriptionValid(prescription.getRxDate(), validDays)) {
				throw new PrescriptionExpiredException("Prescription has expired!");
			}
		}
	}

	private static boolean isPrescriptionValid(Date prescriptionDate, Integer validateDays) {
		if (prescriptionDate != null && validateDays != null) {
			DateTime a = new DateTime(prescriptionDate.getTime()).plusDays(validateDays);
			return new DateTime().isBefore(a.getMillis());
		}
		return false;
	}

	@Override
	@Transactional
	public OrderItem markItemVerified(User user, long orderId, long orderItemId) throws Exception {
		Order order = orderRepository.findOne(orderId);
		OrderItem orderItem = orderItemService.findOne(orderItemId);
		if (order != null && orderItem != null && orderItem.getOrderId() == orderId) {
			List<String> excessiveOrderedItemSku = getExcessiveOrderedItemSku(order.getPatientId(),
					Arrays.asList(orderItem));
			orderItem.setVerified(true);
			orderItem.setVerifiedBy(String.valueOf(user.getId()));
			orderItem.setUpdatedBy(String.valueOf(user.getId()));
			orderItem = orderItemService.save(orderItem);
			if (orderItem.getClassification() != null && !HABIT_FORMING_DRUGS.contains(orderItem.getClassification())
					&& !orderItem.getSku().contains("NEW")) {
				boolean isExcessiveOrdered = excessiveOrderedItemSku.contains(orderItem.getSku());
				patientMedicineService.add(order.getPatientId(), order.getDispatchDate(), orderItem,
						isExcessiveOrdered);
			}
			List<OrderItem> orderItems = orderItemService.findByOrderId(orderId);
			boolean markOrderVerified = true;
			for (OrderItem orderItem2 : orderItems) {
				if (orderItem2.isActive() && !orderItem2.isVerified()) {
					markOrderVerified = false;
				}
			}
			if (markOrderVerified) {
				orderEventService.markOrderVerified(order, order.getId() + order.getUpdatedAt().toString(),
						OrderEvent.MARK_ORDER_VERIFIED, user, 0);
			}
			return orderItem;
		}
		throw new InvalidOrderException("Invalid Order-Id :: " + orderId);
	}

	@Override
	@Transactional
	public Boolean markAllItemVerified(User user, long orderId) throws Exception {
		Order order = orderRepository.findOne(orderId);
		if (order == null) {
			throw new InvalidOrderException("Invalid Order-Id :: " + orderId);
		}
		List<OrderItem> orderItems = orderItemService.findByOrderId(orderId);
		if (orderItems != null && !orderItems.isEmpty()) {
			List<String> excessiveOrderedItemSku = getExcessiveOrderedItemSku(order.getPatientId(), orderItems);
			List<PatientMedicine> patientMedicines = new ArrayList<PatientMedicine>();
			for (OrderItem orderItem : orderItems) {
				orderItem.setVerified(true);
				if (user.getId() <= 0) {
					orderItem.setVerifiedBy(user.getFirstName());
					orderItem.setUpdatedBy(user.getFirstName());
				} else {
					orderItem.setVerifiedBy(String.valueOf(user.getId()));
					orderItem.setUpdatedBy(String.valueOf(user.getId()));
				}
				if (Order.CATEGORY.LAB.equalsIgnoreCase(order.getCategory())) {
					PatientMedicine patientMedicine = patientMedicineService.createPatientMedicineFromOrderItem(orderItem.getPatientId(),
							order.getDispatchDate(), orderItem, false);
					patientMedicine.setVerified(true);
					patientMedicines.add(patientMedicine);
				} else if (orderItem.getClassification() != null
						&& !HABIT_FORMING_DRUGS.contains(orderItem.getClassification())
						&& !orderItem.getSku().contains("NEW")) {
					boolean isExcessiveOrdered = excessiveOrderedItemSku.contains(orderItem.getSku());
					patientMedicines.add(patientMedicineService.createPatientMedicineFromOrderItem(order.getPatientId(),
							order.getDispatchDate(), orderItem, isExcessiveOrdered));
				}
			}
			patientMedicineService.addAll(order.getPatientId(), patientMedicines);
			orderItemService.save(orderItems);
			return true;
		}
		throw new OrderItemNotFoundException("Order items not found. Order-id :: " + orderId);
	}

	@Override
	public boolean addOrderItemsToPatientMedicines(long orderId) throws Exception {
		Order order = orderRepository.findOne(orderId);
		if (order == null) {
			throw new InvalidOrderException("Invalid Order-Id :: " + orderId);
		}
		List<OrderItem> orderItems = orderItemService.findByOrderId(orderId);
		if (orderItems != null && !orderItems.isEmpty()) {
			List<String> excessiveOrderedItemSku = getExcessiveOrderedItemSku(order.getPatientId(), orderItems);
			List<PatientMedicine> patientMedicines = new ArrayList<PatientMedicine>();
			for (OrderItem orderItem : orderItems) {
				if (orderItem.isActive() && !HABIT_FORMING_DRUGS.contains(orderItem.getClassification())
						&& !orderItem.getSku().contains("NEW")) {
					boolean isExcessiveOrdered = excessiveOrderedItemSku.contains(orderItem.getSku());
					patientMedicines.add(patientMedicineService.createPatientMedicineFromOrderItem(order.getPatientId(),
							order.getDispatchDate(), orderItem, isExcessiveOrdered));
				}
			}
			patientMedicineService.addAll(order.getPatientId(), patientMedicines);
			// orderItemService.save(orderItems);
			return true;
		}
		throw new OrderItemNotFoundException("Order items not found. Order-id :: " + orderId);
	}

	@SuppressWarnings("unchecked")
	@Transactional(rollbackFor = Exception.class)
	@Override
	public List<OrderItem> addOrderItemsFromPatientMedicines(User user, long orderId, Map<String, Object> leadDetails)
			throws Exception {
		if (orderId <= 0) {
			throw new IllegalArgumentException("Invalid order id");
		}
		if (leadDetails == null || leadDetails.isEmpty() || orderId <= 0) {
			new IllegalArgumentException("Invalid request param.");
		}
		if (!leadDetails.containsKey("lead")) {
			new IllegalArgumentException("No lead information present in lead details");
		}
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> lead = (Map<String, Object>) leadDetails.get(LEAD_KEY);
		if (!lead.containsKey(LEAD_MEDICINE_LINE_ITEMS)) {
			new IllegalArgumentException(LEAD_MEDICINE_LINE_ITEMS + " not present in lead data");
		}
		List<PatientMedicine> patientMedicines = mapper.readValue(
				mapper.writeValueAsString(lead.get(LEAD_MEDICINE_LINE_ITEMS)),
				new TypeReference<List<PatientMedicine>>() {
				});
		if (patientMedicines == null || patientMedicines.isEmpty()) {
			throw new IllegalArgumentException("Patient medicines cannot be empty : order id : " + orderId);
		}
		Order order = orderRepository.findOne(orderId);
		if (order == null) {
			throw new InvalidOrderException("No order id found in system :: " + orderId);
		}
		if (!OrderState.NEW.equalsIgnoreCase(order.getState())
				|| !OrderStatus.STATE_NEW.PRESCRIPTION_PENDING.equalsIgnoreCase(order.getStatus())) {
			throw new InvalidOrderException(
					"Invalid order state : " + order.getState() + " and status : " + order.getStatus());
		}
		List<OrderItem> orderItemsExist = orderItemService.findByOrderId(orderId);
		boolean orderItemAlreadyExist = false;
		if (orderItemsExist != null && !orderItemsExist.isEmpty()) {
			orderItemAlreadyExist = true;
		}
		List<OrderItem> orderItems = new ArrayList<OrderItem>();
		OrderItem orderItem = null;
		List<String> skuIds = new ArrayList<String>();
		for (PatientMedicine patientMedicine : patientMedicines) {
			if (!StringUtils.isBlank(patientMedicine.getSku())) {
				skuIds.add(patientMedicine.getSku());
			}
		}
		try {
			ShippingAddress shippingAddress = orderShippingAddressService.findByOrderId(orderId);
			if (shippingAddress == null) {
				throw new OrderException("No shipping address found for order id : " + orderId);
			}
			String pincode = shippingAddress.getPincode() > 0 ? String.valueOf(shippingAddress.getPincode()) : "";
			String state = shippingAddress.getState();
			String regionCode = getRegionCodeByOrderId(state);
			List<ProductStock> productStockList = productStockService.getProductStockBySkuIdsAndRegionCode(skuIds,
					regionCode);
			if (productStockList == null || productStockList.isEmpty()) {
				throw new OrderException("No product stock list found for sku's of patient medicines");
			}
			Map<String, ProductStock> skuProductStockMap = new HashMap<String, ProductStock>();
			for (ProductStock productStock : productStockList) {
				skuProductStockMap.put(productStock.getSku(), productStock);
			}
			for (PatientMedicine patientMedicine : patientMedicines) {
				orderItem = new OrderItem();
				orderItem.setSku(patientMedicine.getSku());
				orderItem.setName(patientMedicine.getProductName());
				orderItem.setClassification(patientMedicine.getType());
				orderItem.setVerifiedBy(patientMedicine.getVerifiedBy());
				orderItem.setPackType(patientMedicine.getPackType());
				orderItem.setBrandName(patientMedicine.getBrandName());
				orderItem.setOrderedQuantity((int) patientMedicine.getRecommendQty());
				orderItem.setDosageParameters(patientMedicine.getDosageValue());
				ProductStock productStock = skuProductStockMap.containsKey(patientMedicine.getSku())
						? skuProductStockMap.get(patientMedicine.getSku())
						: null;
				if (productStock != null) {
					orderItem.setMrp(productStock.getMrp());
					orderItem.setSalePrice(productStock.getSalePrice());
				}
				orderItem = setOrderItem(user, order, orderItem);
				orderItem.setVerified(true);
				orderItem.setState(order.getState());
				orderItem.setStatus(order.getStatus());
				orderItems.add(orderItem);
			}
			order.setOrderItems(orderItemService.updateOrderLineInfo(order, orderItems, pincode, false));
			setPatientNameInOrderItem(order.getCustomerId(), orderItem);
			orderItemService.save(orderItems);
			if (orderItemAlreadyExist) {
				for (int i = 0; i < orderItemsExist.size(); i++) {
					orderItemsExist.get(i).setActive(false);
					orderItemsExist.get(i).setOrderedQuantity(0);
				}
				orderItemService.save(orderId, orderItemsExist);
			}
			orderRepository.save(setOrderPriceOldOrder(order, null));

			// Create Event For Verification of Items
			try {
				orderEventService.markOrderVerified(order, order.getId() + order.getUpdatedAt().toString(),
						OrderEvent.MARK_ORDER_VERIFIED, user, 0);
			} catch (Exception e) {
				LOGGER.error("Error in verified event creation : " + e.getMessage());
			}
		} catch (Exception e) {
			throw new Exception("Exception while processing order items and order price for order id : " + orderId
					+ " :: " + e.getMessage());
		}
		return orderItems;
	}

	@Override
	public String getRegionCodeByOrderId(String state) {
		try {
			if (state == null) {
				throw new OrderException("Invalid order id : " + state);
			}
			switch (state.toUpperCase()) {
			case ShippingAddress.STATE.DELHI:
				return RegionCode.DELHI;
			case ShippingAddress.STATE.UTTAR_PRADESH:
				return RegionCode.DELHI;
			case ShippingAddress.STATE.HARYANA:
				return RegionCode.DELHI;
			case ShippingAddress.STATE.RAJASTHAN:
				return RegionCode.JAIPUR;
			default:
				return RegionCode.DELHI;
			}
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
		}
		return null;
	}

	@Override
	public OrderItem markItemDeleted(User user, long orderId, long orderItemId) throws Exception {
		Order order = orderRepository.findOne(orderId);
		OrderItem orderItem = orderItemService.findOne(orderItemId);
		if (order != null && orderItem != null && orderItem.getOrderId() == orderId) {

			HashMap<String, Integer> map = new HashMap<>();
			map.put(orderItem.getSku(), Math.negateExact(orderItem.getQuantity()));
			if (Order.CATEGORY.MEDICINE.equalsIgnoreCase(order.getCategory())) {
				updateProductStock(order, map);
			}
			orderItem.setActive(false);
			orderItem.setQuantity(0);
			orderItem.setUpdatedBy(String.valueOf(user.getId()));
			orderItemService.save(orderId, orderItem);
			// orderRepository.save(setOrderPrice(order, null));
			order = setOrderPriceOldOrder(order, null);
			List<OrderItem> orderItems = orderItemService.findByOrderId(orderId);
			order.setOrderItems(orderItems);
			// applyCoupon(order, order.getManualCouponCode(), "update");
			String couponCode = order.getCouponCode();
			if(StringUtils.isNotBlank(order.getManualCouponCode())) {
				couponCode = order.getManualCouponCode();
			}
			if(order.isMembershipAdded() && orderItem.getProductCategory().equalsIgnoreCase(PRODUCT_CATEGORY.MEMBERSHIP_CARD)) {
				order.setMembershipAdded(false);		
			}
			updateOrderItemWithCatalogAndApplyCoupon(order, null, false, couponCode, "update", true);
			orderRepository.save(order);
			orderItemService.save(order.getOrderItems());
			return orderItemService.findOne(orderItem.getId());
		}
		throw new InvalidOrderException("Invalid Order-Id :: " + orderId);
	}

	@Override
	public Order partialOrderUpdate(User user, long orderId, Map<String, Object> order) throws Exception {
		try {

			if (orderId > 0 && order != null && !order.isEmpty()) {
				Order oldOrder = orderRepository.findOne(orderId);
				Map<String, Object> customerCameCase = new HashMap<>();
				order.forEach(
						(k, v) -> customerCameCase.put(CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, k), v));
				ConvertUtils.register(new SqlTimestampConverter(null), Timestamp.class);
				BeanUtilsBean2.getInstance().copyProperties(oldOrder, customerCameCase);

				return orderRepository.save(oldOrder);

			} else {
				throw new IllegalArgumentException("Order can't be empty");
			}
		} catch (Exception e) {
			throw e;
		}
	}

	@Override
	public Order markOrderDigitized(User user, long orderId, List<OrderItem> orderItems) throws Exception {
		Order order = orderRepository.findOne(orderId);
		if (order == null) {
			throw new OrderNotFoundException("Order not found for id: " + orderId);
		}
		orderItemService.save(setOrderItems(user, order, orderItems));
		order = setOrderPriceOldOrder(order, null);
		order = updateOrderStatus(order, "DIGITIZED");
		orderEventService.updateOrderEvent(order, order.getId() + order.getUpdatedAt().toString(),
				OrderEvent.ORDER_NEW_DIGITIZED, user, 0, null, null);
		return order;
	}

	@Override
	public Order markOrderVerified(User user, long orderId) throws Exception {
		Order order = orderRepository.findOne(orderId);
		if (order == null) {
			throw new OrderNotFoundException("Order not found for id: " + orderId);
		}
		order = updateOrderStatus(order, "VERIFIED");
		orderEventService.updateOrderEvent(order, order.getId() + order.getUpdatedAt().toString(),
				OrderEvent.ORDER_NEW_VERIFIED, user, 0, null, null);
		return order;
	}

	@Override
	public Order shortOrder(User user, long orderId, UpdateOrderObject updateOrderObject) throws Exception {
		Order order = orderRepository.findOne(orderId);
		if (order == null) {
			throw new OrderNotFoundException("Order not found for id: " + orderId);
		}
		OrderPrice orderPrice = getOrderPrice(order);

		order = updateOrder(user, updateOrderObject, order, false, orderPrice, true, false, false);
		orderEventService.updateOrderEvent(order, order.getId() + order.getUpdatedAt().toString(),
				OrderEvent.ORDER_SHORT, user, 0, null, null);
		return order;
	}

	@Override
	public Boolean splitOrder(User user, long orderId, List<UpdateOrderObject> updateOrderObjects) throws Exception {
		Order order = orderRepository.findOne(orderId);
		if (order == null) {
			throw new OrderNotFoundException("Order not found for id: " + orderId);
		}
		List<Order> result = new ArrayList<Order>();
		order.setOrderItems(orderItemService.findByOrderId(orderId));
		OrderPrice orderPrice = getOrderPrice(order);
		for (UpdateOrderObject updateOrderObject : updateOrderObjects) {
			if (updateOrderObject.getParentId() > 0) {
				//order = createChild(user, updateOrderObject, order, orderPrice);
				result.add(order);
				orderEventService.createOrderEvent(order, order.getId() + order.getUpdatedAt().toString(),
						OrderEvent.ORDER_NEW_VERIFIED, user);
			} else {

				result.add(updateOrder(user, updateOrderObject, order, false, orderPrice, false, true, false));
				orderEventService.updateOrderEvent(order, order.getId() + order.getUpdatedAt().toString(),
						OrderEvent.ORDER_SPLIT, user, 0, null, null);
			}
		}
		return true;
	}
	
	@Override
	public Order updateOrder(User user, long orderId, List<UpdateOrderObject> updateOrderObjects, String task, boolean isFinalPrice, boolean isInvoiced) throws Exception {
				
		long parentId = orderId;
		Order order = orderRepository.findOne(orderId);
		if (order == null) {
			throw new OrderNotFoundException("Order not found for id: " + orderId);
		}
		order.setOrderItems(orderItemService.findByOrderId(orderId));
		order.setShippingAddress(shippingAddressRepository.findTopByOrderId(orderId));
				
		Map<Long,Order> orderMap = new HashMap<Long,Order>();
		Order childOrder = new Order();
		List<Order> childOrders = new ArrayList<Order>();
		boolean isChildCreated = false;
		for (UpdateOrderObject updateOrderObject : updateOrderObjects) {
			if (updateOrderObject.getParentId() > 0 && OrderEvent.ORDER_SPLIT.equalsIgnoreCase(task)) {
				childOrder = createChild(user, updateOrderObject, order);
				childOrders.add(childOrder);
				if(childOrder != null && childOrder.getId() > 0) {
					isChildCreated = true;
					childOrders.add(childOrder);
				}
				
			} else {
				order = changeOrder(user, updateOrderObject, order);
				/*orderEventService.updateOrderEvent(order, order.getId() + order.getUpdatedAt().toString(),
						OrderEvent.ORDER_SPLIT, user, 0, null, null);*/
			}                                      
		}
		order = orderRepository.save(order);
		
		if(order.getParentId() > 0) {
			parentId = order.getParentId();
		}
		if (!isInvoiced) {
			Order masterOrder = getMergeOrders(parentId, orderMap);

			if (Order.BUSINESS_TYPE.B2C.equalsIgnoreCase(masterOrder.getBusinessType())) {
				if (masterOrder.getExternalInvoiceAmount() <= 0 && !isFinalPrice) {
					LOGGER.info("Master order before :: updateOrderItemWithCatalogAndApplyCoupon {} ", masterOrder);
					updateOrderItemWithCatalogAndApplyCoupon(masterOrder,
							String.valueOf(masterOrder.getShippingAddress().getPincode()), false,
							masterOrder.getCouponCode(), "update", true);
					LOGGER.info("Master order after :: updateOrderItemWithCatalogAndApplyCoupon {} ", masterOrder);
				}
				customerPriceAdjustment(masterOrder, masterOrder.getCustomerAmountToPay(), isFinalPrice);
			}
			// Sync price to child
			syncPriceOrder(masterOrder, orderMap);
			// OrderPrice masterOrderPrice = getOrderPrice(masterOrder);
		}
		for (Order splittedOrder : childOrders) {
			if (isChildCreated && splittedOrder != null && splittedOrder.getId() > 0) {
				orderEventService.createOrderEvent(splittedOrder, splittedOrder.getId() + splittedOrder.getUpdatedAt().toString(),
						OrderEvent.ORDER_NEW_VERIFIED, user);
			}
		}
		orderEventService.updateOrderEvent(order, order.getId() + order.getUpdatedAt().toString(),
				task, user, 0, null, null);
		return order;
	}
	
	
	private boolean syncPriceOrder(Order masterOrder, Map<Long, Order> orderMap) {
		
		Map<String, OrderItem> skuMasterItemMap = new HashMap<String, OrderItem>();
		
		for(OrderItem  orderItem : masterOrder.getOrderItems()) {
			skuMasterItemMap.put(orderItem.getSku(), orderItem);
		}
		
		ConvertUtils.register(new SqlTimestampConverter(null), Timestamp.class);
		ConvertUtils.register(new SqlTimeConverter(null), Time.class);
		ConvertUtils.register(new SqlDateConverter(null), java.sql.Date.class);
		OrderPrice masterOrderPrice = getOrderPrice(masterOrder);
		float masterTotalPrice = masterOrder.getTotalMrp();
		for (Order order : orderMap.values()) {
			if (order.getExternalInvoiceAmount() <= 0) {
				double totalMrp = 0;
				List<OrderItem> newOrderItems = new ArrayList<OrderItem>();

				for (OrderItem orderItem : order.getOrderItems()) {

					if (skuMasterItemMap.containsKey(orderItem.getSku())) {
						try {
							long id = orderItem.getId();
							long orderId = orderItem.getOrderId();
							int oldOty = orderItem.getQuantity();
							BeanUtilsBean.getInstance().copyProperties(orderItem, skuMasterItemMap.get(orderItem.getSku()));
							orderItem.setOrderId(orderId);
							orderItem.setId(id);
							orderItem.setQuantity(oldOty);
							newOrderItems.add(orderItem);
							totalMrp += orderItem.getMrp() * orderItem.getQuantity();
						} catch (Exception e) {

						}
					}
				}
				order.setTotalMrp((float) totalMrp);
				if(masterTotalPrice > 0) {
					float ratio = order.getTotalMrp() / masterTotalPrice;
					boolean isParent = order.getId() == masterOrder.getId();
					OrderPrice childOrderPrice = getChildOrderPrice(masterOrderPrice, ratio, isParent);
					order = setOrderPriceFromOrderPrice(order, childOrderPrice);
				}
				orderItemService.save(newOrderItems);
				orderRepository.save(order);
			}
		}

		return true;
	}

	public Order changeOrder(User user, UpdateOrderObject updateOrderObject, Order order) {
		

		List<OrderItem> newOrderItems = new ArrayList<>();
		/*List<ItemInfo> newItemInfos = new ArrayList<>();
		float discount = 15;*/

		Map<String, ItemInfo> skuItemsMap = new HashMap<String, ItemInfo>();
		for (ItemInfo itemInfo : updateOrderObject.getItemsInfos()) {
			if (null != itemInfo.getSku()) {
				skuItemsMap.put(itemInfo.getSku(), itemInfo);
			}
		}

		int previousQty = 0;
		//int currentQty = 0;
		for (OrderItem orderItem : order.getOrderItems()) {
			previousQty = previousQty + orderItem.getOrderedQuantity();
			if (!skuItemsMap.containsKey(orderItem.getSku())) {
				orderItem.setQuantity(0);
				orderItem.setLooseQuantity(0);
			} else {
				ItemInfo itemInfo = skuItemsMap.get(orderItem.getSku());
				orderItem.setName(itemInfo.getName() != null ? itemInfo.getName() : orderItem.getName());
				orderItem.setActive(itemInfo.getIsActive() != null ? itemInfo.getIsActive() : orderItem.isActive());
				orderItem
						.setImageUrl(itemInfo.getImageUrl() != null ? itemInfo.getImageUrl() : orderItem.getImageUrl());
				orderItem.setSalePrice(
						itemInfo.getSalePrice() != null ? itemInfo.getSalePrice() : orderItem.getSalePrice());
				orderItem.setMrp(itemInfo.getMrp() != null ? itemInfo.getMrp() : orderItem.getMrp());
				orderItem.setTax(itemInfo.getTax() != null ? itemInfo.getTax() : orderItem.getTax());
				orderItem
						.setDiscount(itemInfo.getDiscount() != null ? itemInfo.getDiscount() : orderItem.getDiscount());
				orderItem
						.setQuantity(itemInfo.getQuantity() != null ? itemInfo.getQuantity() : orderItem.getQuantity());
				orderItem
				.setLooseQuantity(itemInfo.getLooseQuantity() != null ? itemInfo.getLooseQuantity() : orderItem.getLooseQuantity());
				orderItem.setPerPackQty(itemInfo.getPerPackQty() != null && itemInfo.getPerPackQty() > 0 ? itemInfo.getPerPackQty() : orderItem.getPerPackQty());
				orderItem.setPerPackQty(orderItem.getPerPackQty() > 0 ? orderItem.getPerPackQty() : 1);
				orderItem.setBrandName(
						itemInfo.getBrandName() != null ? itemInfo.getBrandName() : orderItem.getBrandName());
				orderItem.setStatus(itemInfo.getStatus() != null ? itemInfo.getStatus() : orderItem.getStatus());
				orderItem.setStatusReason(
						itemInfo.getReason() != null ? itemInfo.getReason() : orderItem.getStatusReason());
				orderItem.setUpdatedBy(
						itemInfo.getUpdatedBy() != null ? itemInfo.getUpdatedBy() : orderItem.getUpdatedBy());
				orderItem.setUpdatedByName(itemInfo.getUpdatedByName() != null ? itemInfo.getUpdatedByName()
						: orderItem.getUpdatedByName());
				skuItemsMap.remove(orderItem.getSku());
			}
			//discount = orderItem.getDiscount();
			newOrderItems.add(orderItem);
		}

		long orderId = order.getId();
		if (!skuItemsMap.isEmpty()) {
			skuItemsMap.forEach((sku, itemInfo) -> {
				if (itemInfo.getSku() != null) {
					OrderItem orderItem = new OrderItem();
					orderItem.setSku(itemInfo.getSku());
					orderItem.setName(itemInfo.getName() != null ? itemInfo.getName() : itemInfo.getSku());
					orderItem.setOrderedQuantity(itemInfo.getQuantity() != null ? itemInfo.getQuantity() : 0);
					orderItem.setQuantity(itemInfo.getQuantity() != null ? itemInfo.getQuantity() : 0);
					orderItem.setLooseQuantity(itemInfo.getLooseQuantity() != null ? itemInfo.getLooseQuantity() : 0);
					orderItem.setPerPackQty(itemInfo.getPerPackQty() != null && itemInfo.getPerPackQty() > 0 ? itemInfo.getPerPackQty() : 1);
					orderItem.setActive(itemInfo.getIsActive() != null ? itemInfo.getIsActive() : true);
					orderItem.setMrp(itemInfo.getMrp() != null ? itemInfo.getMrp() : 0);
					orderItem.setSalePrice(itemInfo.getSalePrice() != null ? itemInfo.getSalePrice() : 0);
					orderItem.setTax(itemInfo.getTax() != null ? itemInfo.getTax() : 0);
					orderItem.setDiscount(itemInfo.getDiscount() != null ? itemInfo.getDiscount() : 0);
					orderItem.setBrandName(itemInfo.getBrandName());
					orderItem.setState("NEW");
					orderItem.setStatus("VERIFIED");
					orderItem.setStatusReason(itemInfo.getReason());
					orderItem.setOrderId(orderId);
					orderItem.setUpdatedByName(itemInfo.getUpdatedByName() != null ? itemInfo.getUpdatedByName()
							: orderItem.getUpdatedByName());
					newOrderItems.add(orderItem);
				}

			});
		}

		float orderSalePrice = 0;
		float orderMrp = 0;
		
		String userId = null;
		try {
			userId = ((Long) user.getId()).toString();
		} catch (Exception e) {

		}
		
		for (OrderItem orderItem : newOrderItems) {
			if (orderItem.getQuantity() <= 0 && orderItem.getLooseQuantity() <= 0) {
				
				orderItem.setActive(false);
			}

			orderItem.setUpdatedBy(userId);

			if (orderItem.isActive()) {
				if(orderItem.getPerPackQty() <= 0) {
					orderItem.setPerPackQty(1);
				}
				
				orderMrp += orderItem.getMrp() * (orderItem.getQuantity() + ((float) orderItem.getLooseQuantity() / (float) orderItem.getPerPackQty()));
				orderSalePrice += orderItem.getMrp() * (orderItem.getQuantity() + ((float) orderItem.getLooseQuantity() / (float) orderItem.getPerPackQty())) * (100 - orderItem.getDiscount()) / 100;
				//currentQty = currentQty + orderItem.getOrderedQuantity();
			}
		}
		order.setMembershipAdded(newOrderItems.stream().anyMatch(oi -> oi.getProductCategory().equalsIgnoreCase(OrderItem.PRODUCT_CATEGORY.MEMBERSHIP_CARD) && oi.isActive()));		
		
		order.setDiscount(orderMrp - orderSalePrice);
		order.setTotalMrp(orderMrp);
		
		if (null != updateOrderObject.getCoupanDiscount()) {
			float couponDiscount = (new Float(updateOrderObject.getCoupanDiscount()));
			order.setCouponDiscount(couponDiscount);
		}
		if (null != updateOrderObject.getUsedCarePoint()) {
			float usedCarePoint = (new Float(updateOrderObject.getUsedCarePoint()));
			order.setRedeemedCarePoints((int) usedCarePoint);
		}
		if (updateOrderObject.getRedeemedCash() > 0) {
			float usedCash = (new Float(updateOrderObject.getRedeemedCash()));
			order.setRedeemedCash(usedCash);
		}
		if (updateOrderObject.getExternalInvoiceAmount() != null
				&& updateOrderObject.getExternalInvoiceAmount() > 0) {
			order.setExternalInvoiceAmount(updateOrderObject.getExternalInvoiceAmount());
		}
		
		if (updateOrderObject.getShippingCharge() > 0) {
			order.setShippingCharge(updateOrderObject.getShippingCharge());
		}
		order.setTotalDiscount(order.getDiscount() + order.getCouponDiscount());
		order.setTotalSalePrice(order.calculateTotalPrice());
		order.setPodAmount(order.getTotalSalePrice() - (float) order.getGatewayAmount());
		order.setOrderItems(newOrderItems);
		
		LOGGER.info("Mac5 parent order:: " +  order);
		orderItemService.save(order.getOrderItems());
		 
		return order;
	
		
	}

	public Order getMergeOrders(long parentId, Map<Long,Order> orderMap) throws Exception {

		Order masterOrder = new Order();
		Order parentOrder = new Order();

		List<Order> associatedOrders = new ArrayList<>();
		
		
		associatedOrders = orderRepository.findAllByParentId(parentId);
		parentOrder = orderRepository.findOne(parentId);
		parentOrder.setOrderItems(orderItemService.findAllActiveByOrderId(parentId));
		parentOrder.setShippingAddress(shippingAddressRepository.findTopByOrderId(parentId));
		
		orderMap.put(parentOrder.getId(), parentOrder);
		
		ConvertUtils.register(new SqlTimeConverter(null), Time.class);
		ConvertUtils.register(new SqlTimestampConverter(null), Timestamp.class);
		ConvertUtils.register(new SqlTimeConverter(null), Time.class);
		ConvertUtils.register(new SqlDateConverter(null), java.sql.Date.class);
		
		if(!CollectionUtils.isEmpty(associatedOrders)) {
			List<OrderItem> allOrderItems = new ArrayList<OrderItem>();
			
			for(Order childOrder : associatedOrders) {
				childOrder.setOrderItems(orderItemService.findAllActiveByOrderId(childOrder.getId()));
				orderMap.put(childOrder.getId(), childOrder);
				allOrderItems.addAll(childOrder.getOrderItems());
			}
			allOrderItems.addAll(parentOrder.getOrderItems());
			BeanUtils.copyProperties(masterOrder, parentOrder);
			masterOrder = setPriceForParent(masterOrder, allOrderItems, associatedOrders, allOrderItems);
			masterOrder.setOrderItems(allOrderItems);
		} else {
			BeanUtils.copyProperties(masterOrder, parentOrder);
		}
		return masterOrder;
	}
	
	@Override
	public Boolean splitOrderBasedOnFacility(User user, long orderId) throws IllegalAccessException, InvocationTargetException {
		Order order = orderRepository.findOne(orderId);
		if (order == null) {
			throw new OrderNotFoundException("Order not found for id: " + orderId);
		}
		
		ShippingAddress shippingAddress = orderShippingAddressService.findByOrderId(orderId);
		
		List<OrderItem> orderItems = orderItemService.findByOrderId(orderId);
		
		Map<Integer, List<OrderItem>> subOrderItems = new HashMap<Integer, List<OrderItem>>();
		for (OrderItem orderItem : orderItems) {
			if (!subOrderItems.containsKey(orderItem.getFacilityCode())) {
				List<OrderItem> facilityOrderItems = new ArrayList<OrderItem>();
				facilityOrderItems.add(orderItem);
				subOrderItems.put(orderItem.getFacilityCode(), facilityOrderItems);
			} else {
				List<OrderItem> facilityOrderItems = subOrderItems.get(orderItem.getFacilityCode());
				if (facilityOrderItems == null || facilityOrderItems.isEmpty()) {
					facilityOrderItems = new ArrayList<OrderItem>();
				}
				facilityOrderItems.add(orderItem);
				subOrderItems.put(orderItem.getFacilityCode(), facilityOrderItems);
			}
		}
		
		if (subOrderItems == null || subOrderItems.isEmpty() || subOrderItems.keySet() == null || subOrderItems.keySet().size() < 2) {
			throw new NotFoundException("Items with different facility not found.");
		}
		
		OrderPrice orderPrice = getOrderPrice(order);
		
		//Map<Integer, Order> facilityChildOrders = new HashMap<Integer, Order>();
		
		orderPrice.setSubOrderItems(subOrderItems);
		//Need to add orderPrice
		Map<Integer, OrderPrice> childOrderPriceAndOrderItems = getChildOrderPrice(orderPrice);
		for (Integer key : childOrderPriceAndOrderItems.keySet()) {
			OrderPrice childOrderPrice = childOrderPriceAndOrderItems.get(key);
			
			Order childOrder = new Order();
			
			ConvertUtils.register(new SqlTimestampConverter(null), Timestamp.class);
			ConvertUtils.register(new SqlTimeConverter(null), Time.class);
			BeanUtilsBean2.getInstance().copyProperties(childOrder, order);
			
			
			childOrder.setParentId(orderId);
			childOrder.setFacilityCode(key);
			childOrder = setOrderPriceFromOrderPrice(childOrder, childOrderPrice);

			childOrder.setId(0);
			childOrder.setOrderNumber(null);
			
			childOrder = orderRepository.save(childOrder);
			
			ShippingAddress childShippingAddress = new ShippingAddress();
			
			ConvertUtils.register(new SqlTimestampConverter(null), Timestamp.class);
			ConvertUtils.register(new SqlTimeConverter(null), Time.class);
			BeanUtilsBean2.getInstance().copyProperties(childShippingAddress, shippingAddress);
			childShippingAddress.setId(0);
			childShippingAddress.setOrderId(childOrder.getId());
			
			shippingAddressRepository.save(childShippingAddress);
			
			List<OrderItem> parentOrderItems = subOrderItems.get(key);
			List<OrderItem> childOrderItems = new ArrayList<OrderItem>();
			for(OrderItem orderItem : parentOrderItems) {
				OrderItem childOrderItem = new OrderItem();
				
				ConvertUtils.register(new SqlTimestampConverter(null), Timestamp.class);
				ConvertUtils.register(new SqlTimeConverter(null), Time.class);
				BeanUtilsBean2.getInstance().copyProperties(childOrderItem, orderItem);
				childOrderItem.setId(0);
				childOrderItem.setOrderId(childOrder.getId());
				
				childOrderItems.add(childOrderItem);
				
			}
			
			//save order item from price module
			orderItemService.save(childOrderItems);
			
			try {
				orderEventService.updateOrderEvent(childOrder, childOrder.getId() + childOrder.getUpdatedAt().toString(), OrderEvent.ORDER_FACILITY_SPLIT, user, 0, null, null);
			} catch (Exception e) {
				LOGGER.debug("SPLIT ORDER EVENT Cannot Be Created {} {}", childOrder.getId(), e);
			}
			
		}
		try {
			orderEventService.updateOrderEvent(order, order.getId() + order.getUpdatedAt().toString(), OrderEvent.ORDER_FACILITY_SPLIT, user, 0, null, null);
		} catch (Exception e) {
			LOGGER.debug("SPLIT ORDER EVENT Cannot Be Created {} {}", order.getId(), e);
		}
		return true;
	}
	
	private Map<Integer, OrderPrice> getChildOrderPrice(OrderPrice orderPrice) {
		Map<Integer, Double> salePriceWiseRatio = new HashMap<Integer, Double>();
		
		Map<Integer, OrderPrice> childOrderPriceAndOrderItems = new HashMap<Integer, OrderPrice>();
		
		for (Integer key : orderPrice.getSubOrderItems().keySet()) {
			List<OrderItem> childOrderItems = orderPrice.getSubOrderItems().get(key);
			double totalMrp = 0;
			for(OrderItem orderItem : childOrderItems) {
				totalMrp = totalMrp + (orderItem.getMrp() * orderItem.getQuantity());
			}
			salePriceWiseRatio.put(key, totalMrp);
			double ratio = (totalMrp/orderPrice.getTotalMrp());
			OrderPrice childOrderPrice = getChildOrderPrice(orderPrice, ratio, false);
			
			childOrderPriceAndOrderItems.put(key, childOrderPrice);
		}
		
		
		
		return childOrderPriceAndOrderItems;
		
	}
	
	@Override
	public OrderPrice getChildOrderPrice(OrderPrice orderPrice, double ratio, Boolean isParentOrder) {
		OrderPrice childOrderPrice = new OrderPrice();
		childOrderPrice.setCouponDiscount((float) (orderPrice.getCouponDiscount() * ratio));
		childOrderPrice.setDiscount((float) (orderPrice.getDiscount() * ratio));
		childOrderPrice.setExternalInvoiceAmount(orderPrice.getExternalInvoiceAmount() * ratio);
		childOrderPrice.setGatewayAmount(orderPrice.getGatewayAmount() * ratio);
		childOrderPrice.setRedeemedCarePoints((int) Math.round(orderPrice.getRedeemedCarePoints() * ratio));
		childOrderPrice.setRedeemedCash((float) (orderPrice.getRedeemedCash() * ratio));
		if(isParentOrder) {
			childOrderPrice.setShippingCharge(orderPrice.getShippingCharge());//Parent
			childOrderPrice.setUrgentDeliveryCharge(orderPrice.getUrgentDeliveryCharge()); //PARENT > 
		} else {
			childOrderPrice.setShippingCharge(0);
			childOrderPrice.setUrgentDeliveryCharge(0); //PARENT > 
		}
		
		childOrderPrice.setTotalDiscount((float) (orderPrice.getTotalDiscount() * ratio));
		childOrderPrice.setTotalMrp((float) (orderPrice.getTotalMrp() * ratio));//MRP is of child
		double actualSalePrice = orderPrice.getTotalSalePrice() - orderPrice.getShippingCharge()
				- orderPrice.getUrgentDeliveryCharge();
		childOrderPrice.setTotalSalePrice((float) ((actualSalePrice * ratio) + childOrderPrice.getShippingCharge()
				+ childOrderPrice.getUrgentDeliveryCharge()));
		childOrderPrice.setPodAmount(childOrderPrice.getTotalSalePrice() - childOrderPrice.getGatewayAmount());
		childOrderPrice.setTotalTaxAmount((float) (orderPrice.getTotalTaxAmount() * ratio));
		
		return childOrderPrice;
	}

	@Override
	public Order mergeOrderBasedOnFacility(User user, long orderId, List<Long> childOrderIds) throws IllegalAccessException, InvocationTargetException {

		Order order = orderRepository.findOne(orderId);
		if (order == null) {
			throw new OrderNotFoundException("Order not found for id: " + orderId);
		}

		if (!OrderStatus.STATE_PACKED.READY_TO_MERGED.equals(order.getStatus())) {
			throw new OrderNotFoundException("Order status is not ready for merge. Id : " + orderId);
		}

		//List<OrderStateStatusRequest> orderStateStatusRequests = new ArrayList<OrderStateStatusRequest>();
		Map<Long, OrderStateStatusRequest> orderOrderStateStatusRequests = new HashMap<Long, OrderStateStatusRequest>();
		List<Order> childOrders = orderRepository.findAllByParentId(orderId);
		List<OrderItem> orderItems = orderItemService.findByOrderId(orderId);
		List<OrderItem> orderItemsWithNewPrice = new ArrayList<OrderItem>();
		for (Order childOrder : childOrders) {
			OrderStateStatusRequest childOrderStateStatusRequest = new OrderStateStatusRequest();
			childOrderStateStatusRequest.setFacilityId(childOrder.getFacilityCode());
			if (childOrder.getState().equals(OrderState.SHIPPED) && childOrder.getStatus().equals(OrderStatus.STATE_SHIPPED.READY_TO_MERGED)) {
				childOrderStateStatusRequest.setState(OrderState.SHIPPED);
				childOrderStateStatusRequest.setStatus(OrderStatus.STATE_SHIPPED.MERGED);
			} else if (childOrder.getState().equals(OrderState.PACKED) && childOrder.getStatus().equals(OrderStatus.STATE_PACKED.READY_TO_MERGED)) {
				childOrderStateStatusRequest.setState(OrderState.PACKED);
				childOrderStateStatusRequest.setStatus(OrderStatus.STATE_PACKED.MERGED);
			} else {
				throw new InvalidOrderException("Order status for child order id" + childOrder.getId() + " is in invalid status : " + childOrder.getStatus());
			}
			orderOrderStateStatusRequests.put(childOrder.getId(), childOrderStateStatusRequest);
			orderItemsWithNewPrice.addAll(orderItemService.findByOrderId(childOrder.getId()));
		}

		// set price of items for order
		order = setPriceForParent(order, orderItems, childOrders, orderItemsWithNewPrice);

		OrderStateStatusRequest orderStateStatusRequest = new OrderStateStatusRequest();
		
		orderStateStatusRequest.setFacilityId(order.getFacilityCode());
		orderStateStatusRequest.setState(OrderState.PACKED);
		orderStateStatusRequest.setStatus(OrderStatus.STATE_PACKED.MERGED);
		orderStateStatusRequest.setUser(user);
		
		orderOrderStateStatusRequests.put(order.getId(), orderStateStatusRequest);
		
		orderRepository.save(order);
		orderItemService.save(order.getOrderItems());
		
		salusService.orderStatusChange(orderOrderStateStatusRequests);

		try {
			orderEventService.updateOrderEvent(order, order.getId() + order.getUpdatedAt().toString(), OrderEvent.ORDER_FACILITY_MERGED, user, 0, null, null);
		} catch (Exception e) {
			LOGGER.debug("EVENT Cannot Be Created {} {}", order.getId(), e);
		}
		return order;

	}

	private Order setPriceForParent(Order order, List<OrderItem> orderItems, List<Order> childOrders, List<OrderItem> orderItemsWithNewPrice) throws IllegalAccessException, InvocationTargetException {
		
		float couponDiscount = 0;
		float discount = 0;
		double externalInvoiceAmount = 0;
		double gatewayAmount = 0;
		double podAmount = 0;
		int redeemedCarePoints = 0;
		float redeemedCash = 0;
		float shippingCharge = 0;
		float totalDiscount = 0;
		float totalMrp = 0;
		float totalSalePrice = 0;
		float totalTaxAmount = 0;
		double urgentDeliveryCharge = 0;
		
		for(Order childOrder : childOrders) {
			couponDiscount = couponDiscount + childOrder.getCouponDiscount();
			discount = discount + childOrder.getDiscount();
			externalInvoiceAmount = externalInvoiceAmount + childOrder.getExternalInvoiceAmount();
			gatewayAmount = gatewayAmount + childOrder.getGatewayAmount();
			podAmount = podAmount + childOrder.getPodAmount();
			redeemedCarePoints = redeemedCarePoints + childOrder.getRedeemedCarePoints();
			redeemedCash = redeemedCash + childOrder.getRedeemedCash();
			shippingCharge = shippingCharge + childOrder.getShippingCharge();
			totalDiscount = totalDiscount + childOrder.getTotalDiscount();
			totalMrp = totalMrp + childOrder.getTotalMrp();
			totalSalePrice = totalSalePrice + childOrder.getTotalSalePrice();
			totalTaxAmount = totalTaxAmount + childOrder.getTotalTaxAmount();
			urgentDeliveryCharge = urgentDeliveryCharge + childOrder.getUrgentDeliveryCharge();
		}
		
		Map<String, OrderItem> skuOrderItem = new HashMap<String, OrderItem>();
		
		for(OrderItem orderItem : orderItems) {
			skuOrderItem.put(orderItem.getSku(), orderItem);
		}
		
		List<OrderItem> updatedOrderItems=new ArrayList<OrderItem>();
		for(OrderItem orderItem : orderItemsWithNewPrice) {
			OrderItem newOrderItem = new OrderItem();
			BeanUtils.copyProperties(newOrderItem, orderItem);
			if(skuOrderItem.containsKey(orderItem.getSku())) {
				newOrderItem.setId(skuOrderItem.get(orderItem.getSku()).getId());
			} else{
				newOrderItem.setId(0);
			}
			newOrderItem.setOrderId(order.getId());
			updatedOrderItems.add(newOrderItem);
		}
		
		order.setCouponDiscount(order.getCouponDiscount() + couponDiscount);
		order.setDiscount(order.getDiscount() + discount);
		order.setExternalInvoiceAmount(order.getExternalInvoiceAmount() + externalInvoiceAmount);
		order.setGatewayAmount(order.getGatewayAmount() + gatewayAmount);
		order.setPodAmount(order.getPodAmount() + podAmount);
		order.setRedeemedCarePoints(order.getRedeemedCarePoints() + redeemedCarePoints);
		order.setRedeemedCash(order.getRedeemedCash() + redeemedCash);
		order.setShippingCharge(order.getShippingCharge() + shippingCharge);
		order.setTotalDiscount(order.getTotalDiscount() + totalDiscount);
		order.setTotalMrp(order.getTotalMrp() + totalMrp);
		order.setTotalSalePrice(order.getTotalSalePrice() + totalSalePrice);
		order.setTotalTaxAmount(order.getTotalTaxAmount() + totalTaxAmount);
		order.setUrgentDeliveryCharge(order.getUrgentDeliveryCharge() + urgentDeliveryCharge);
		order.setOrderItems(updatedOrderItems);
		
		return order;
	}

	@Override
	public Order updateOrder(User user, long orderId, UpdateOrderObject updateOrderObject, boolean isFinalPrice, boolean isInvoiced)
			throws Exception {
		Order order = orderRepository.findOne(orderId);
		if (order == null) {
			LOGGER.error("Order not found for id : " + orderId);
			throw new OrderNotFoundException("Order not found for id: " + orderId);
		}
		order.setOrderItems(orderItemService.findByOrderId(orderId));

		OrderPrice orderPrice = getOrderPrice(order);

		order = updateOrder(user, updateOrderObject, order, isFinalPrice, orderPrice, true, false, isInvoiced);
		orderEventService.updateOrderEvent(order, order.getId() + order.getUpdatedAt().toString(),
				updateOrderObject.getType(), user, 0, null, null);
		return order;
	}

	@Override
	@Transactional
	 public Boolean updateStateAndStatus(long orderId, OrderStateStatusRequest orderStateStatusRequest) throws Exception { 
		if (!StringUtils.isBlank(orderStateStatusRequest.getState())
				&& !StringUtils.isBlank(orderStateStatusRequest.getStatus())) {
			Order order = orderRepository.findOne(orderId);
			if (order == null) {
				throw new OrderNotFoundException("Order not found for id: " + orderId);
			}
			String previousState = order.getState();
			String previousStatus = order.getStatus();
			if (Order.CATEGORY.MEDICINE.equalsIgnoreCase(order.getCategory()) && orderStateStatusRequest.getState().equals(OrderState.NEW)
					&& orderStateStatusRequest.getStatus().equals(OrderStatus.STATE_NEW.VERIFIED) && !Order.BUSINESS_TYPE.B2B.equalsIgnoreCase(order.getBusinessType())) {
				ShippingAddress shippingAddress = shippingAddressRepository.findTopByOrderId(orderId);
				if (shippingAddress == null || shippingAddress.getPincode() >= 999999) {
					throw new BadRequestException("Shipping address is invalid.");
				}
				
				
				List<OrderItem> orderItems = orderItemService.findByOrderIdAndIsActive(orderId, true);
				Assert.notEmpty(orderItems, "Order-items can not be null or empty");

				Map<String, String> skuItemNameMap = orderItems.parallelStream()
						.collect(Collectors.toMap(OrderItem::getSku, orderItem -> orderItem.getName()));
				List<String> skusDeliverOnPincode = shippingService.getSkusDeliverOnPincode(
						new ArrayList<>(skuItemNameMap.keySet()), shippingAddress.getState(),
						String.valueOf(shippingAddress.getPincode()));
				List<String> nonDeliverItemName = skuItemNameMap.entrySet().stream()
						.filter(entry -> !skusDeliverOnPincode.contains(entry.getKey())).map(entry -> entry.getValue())
						.distinct().collect(Collectors.toList());
				if (nonDeliverItemName != null && !nonDeliverItemName.isEmpty()) {
					throw new BadRequestException(
							"This product(s) is not serviceable on pincode " + shippingAddress.getPincode()
									+ ". Please remove this product(s) to proceed ahead! : " + nonDeliverItemName);
				}

				order.setOrderItems(updateOrderItemWithCatalog(shippingAddress.getPincode() > 0 ? String.valueOf(shippingAddress.getPincode()) : "", orderItems));
				
				String couponCode = order.getCouponCode();
				if(StringUtils.isNotBlank(order.getManualCouponCode())) {
					couponCode = order.getManualCouponCode();
				}
				applyCoupon(order, couponCode, "update");
				orderItemService.save(order.getOrderItems());

				LOGGER.info("Delivered-Item Sku : " + skusDeliverOnPincode);
				LOGGER.info("Non-Delivered-Item : " + nonDeliverItemName);

			}
			if (!order.getState().equals(orderStateStatusRequest.getState())
					|| !order.getStatus().equals(orderStateStatusRequest.getStatus())) {
				if ((orderStateStatusRequest.getState().equals(OrderState.SHIPPED)
						&& orderStateStatusRequest.getStatus().equals(OrderStatus.STATE_SHIPPED.DELIVERED))
						|| (orderStateStatusRequest.getState().equals(OrderState.RETURNED)
								&& orderStateStatusRequest.getStatus().equals(OrderStatus.STATE_RETURNED.RECEIVED))) {
					if (isCurrentDateAfterPromisedDeliveryDate(order)) {
						if (order.getServiceType() != null
								&& Order.SERVICE_TYPE.NORMAL.equalsIgnoreCase(order.getServiceType())
								&& Order.SERVICE_TYPE.LF_ASSURED.equalsIgnoreCase(order.getPreferredServiceType())) {
							// Credit cash wallet
							try {
								float creditAmount = lfAssuredCashbackPercentage * order.getTotalSalePrice();
								if (creditAmount > lfAssuredMaxCashback) {
									creditAmount = lfAssuredMaxCashback;
								}
								if (orderStateStatusRequest.getState().equals(OrderState.RETURNED)
										&& orderStateStatusRequest.getStatus()
												.equals(OrderStatus.STATE_RETURNED.RECEIVED)) {
									walletService.creditWallet(order, String.valueOf(order.getId()),
											WalletTransactionInfo.TRANSACTION_SOURCE.ORDER_SERVICE_TYPE_CHANGE_CREDIT_POST_RETURNED,
											WalletTransactionInfo.CASH_TYPE.CASH, 0, creditAmount,0);
								} else if (orderStateStatusRequest.getState().equals(OrderState.SHIPPED)
										&& orderStateStatusRequest.getStatus()
												.equals(OrderStatus.STATE_SHIPPED.DELIVERED)) {
									walletService.creditWallet(order, String.valueOf(order.getId()),
											WalletTransactionInfo.TRANSACTION_SOURCE.ORDER_SERVICE_TYPE_CHANGE_CREDIT_POST_DELIVERED,
											WalletTransactionInfo.CASH_TYPE.CASH, 0, creditAmount,0);
								}
							} catch (Exception e) {
								LOGGER.debug("Exception in crediting wallet");
							}
							float creditAmount = lfAssuredCashbackPercentage * order.getTotalSalePrice();
						}
						if (order.getDeliveryOption() != null
								&& Order.DELIVERY_OPTION.NORMAL.equalsIgnoreCase(order.getDeliveryOption())
								&& Order.DELIVERY_OPTION.URGENT.equalsIgnoreCase(order.getPreferredDeliveryOption())) {
							// Credit cash wallet
							try {
								float creditAmount = expressDeliveryCashbackPercentage
										* order.getTotalSalePrice();
								if (creditAmount > expressDeliveryMaxCashback) {
									creditAmount = expressDeliveryMaxCashback;
								}
								if (orderStateStatusRequest.getState().equals(OrderState.RETURNED)
										&& orderStateStatusRequest.getStatus()
												.equals(OrderStatus.STATE_RETURNED.RECEIVED)) {
									walletService.creditWallet(order, String.valueOf(order.getId()),
											WalletTransactionInfo.TRANSACTION_SOURCE.ORDER_DELIVERY_OPTION_CHANGE_CREDIT_POST_RETURNED,
											WalletTransactionInfo.CASH_TYPE.CASH, 0, creditAmount,0);
								} else if (orderStateStatusRequest.getState().equals(OrderState.SHIPPED)
										&& orderStateStatusRequest.getStatus()
												.equals(OrderStatus.STATE_SHIPPED.DELIVERED)) {
									walletService.creditWallet(order, String.valueOf(order.getId()),
											WalletTransactionInfo.TRANSACTION_SOURCE.ORDER_DELIVERY_OPTION_CHANGE_CREDIT_POST_DELIVERED,
											WalletTransactionInfo.CASH_TYPE.CASH, 0, creditAmount,0);
								}
							} catch (Exception e) {
								LOGGER.debug("Exception in crediting wallet");
							}
						}
					}
				}
				if (orderStateStatusRequest.getStatus().equalsIgnoreCase(OrderStatus.STATE_CANCELLED.CANCELLED)) {
					if (Order.CATEGORY.LAB.equalsIgnoreCase(order.getCategory())) {
						Appointment appointment = userService.cancelAppointment(order.getAppointmentId());
						order.setAppointment(appointment);
					} else if (Order.CATEGORY.MEDICINE.equalsIgnoreCase(order.getCategory()) && OrderStatus.INVENTORY_REDUCTION_ORDER_STATUS.contains(order.getStatus())
							&& !order.getStatus().equalsIgnoreCase(OrderStatus.STATE_PROCESSING.PART_FULFILLABLE)) {
						List<OrderItem> orderItems = orderItemService.findByOrderIdAndIsActive(orderId, true);
						updateProductStock(order, getUpdatedInventoryMap(orderItems));
					}
				} else if (orderStateStatusRequest.getState().equalsIgnoreCase(OrderState.NEW)
						&& orderStateStatusRequest.getStatus()
								.equalsIgnoreCase(OrderStatus.STATE_NEW.PRESCRIPTION_PENDING)) {
					orderEventService.updateOrderEvent(order, order.getId() + order.getUpdatedAt().toString(),
							OrderEvent.ORDER_AUTO_VERIFIED_FAILED, orderStateStatusRequest.getUser(),
							order.getFacilityCode(), orderStateStatusRequest.getComments(),
							order.getUrgentDeliveryCharge());
				} 
				/*
				 * if
				 * (OrderState.PROCESSING.equalsIgnoreCase(orderStateStatusRequest.getState())
				 * && (OrderStatus.STATE_PROCESSING.PART_FULFILLABLE
				 * .equalsIgnoreCase(orderStateStatusRequest.getStatus()) ||
				 * OrderStatus.STATE_PROCESSING.PART_PICKED
				 * .equalsIgnoreCase(orderStateStatusRequest.getStatus()))) { if
				 * (Order.DELIVERY_OPTION.URGENT.equalsIgnoreCase(order.getDeliveryOption())) {
				 * updateDeliveryOption(orderId, order,
				 * OrderDeliveryObject.DELIVERY_OPTION_CHANGE_REASON.
				 * COMMITED_DELIVERY_TIME_LINE_EXCEEDED, true, null,
				 * orderStateStatusRequest.getUser()); } else if
				 * (Order.SERVICE_TYPE.LF_ASSURED.equalsIgnoreCase(order.getServiceType())) {
				 * updateServiceType(orderId, order, true,
				 * OrderDeliveryObject.SERVICE_TYPE_CHANGE_REASON.
				 * COMMITED_DELIVERY_TIME_LINE_EXCEEDED, null,
				 * orderStateStatusRequest.getUser()); } }
				 */
				order.setState(orderStateStatusRequest.getState());
				order.setScore(orderStateStatusRequest.getScore());
				order.setStatus(orderStateStatusRequest.getStatus());
				order.setStatusComment(orderStateStatusRequest.getReason());
				order.setUpdatedBy(String.valueOf(orderStateStatusRequest.getUser().getId()));
				orderRepository.save(order);
				try {
					orderEventService.updateOrderEvent(order, order.getId() + order.getUpdatedAt().toString(),
							"ORDER_" + StringUtils.upperCase(orderStateStatusRequest.getState()) + "_"
									+ StringUtils.upperCase(orderStateStatusRequest.getStatus()),
							orderStateStatusRequest.getUser(), orderStateStatusRequest.getFacilityId(),
							orderStateStatusRequest.getComments(), null);
				} catch (Exception e) {
			        e.printStackTrace();
					order.setState(previousState);
					order.setScore(orderStateStatusRequest.getScore());
					order.setStatus(previousStatus);
					order.setStatusComment(orderStateStatusRequest.getReason());
					order.setUpdatedBy(String.valueOf(orderStateStatusRequest.getUser().getId()));
					orderRepository.save(order);
				}
				if ((order.getCategory().equals(Order.CATEGORY.MEDICINE)
						&& order.getState().equals(OrderState.SHIPPED)
						&& order.getStatus().equals(OrderStatus.STATE_SHIPPED.DELIVERED)) ||
						(order.getCategory().equals(Order.CATEGORY.LAB)
								&& order.getState().equals(LabOrderState.DELIVERED) && !order.getState().equals(previousState))) {
					if (order.getCouponCashback() > 0) {
						walletService.creditWallet(order, String.valueOf(order.getId()),
								WalletTransactionInfo.TRANSACTION_SOURCE.ORDER_DELIVERY_COUPON_CASHBACK_CREDIT_POST_DELIVERED,
								WalletTransactionInfo.CASH_TYPE.COUPON_CASHBACK, 0, 0, order.getCouponCashback());
					}
				}
				return true;
			} else {
				throw new BadRequestException("Order already in same status.");
			}
		} else {
			throw new IllegalArgumentException("Input param is invalid for state and status update.");
		}
	}

	private boolean isCurrentDateAfterPromisedDeliveryDate(Order order) {
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);

		Calendar calOfStartDate = Calendar.getInstance();
		calOfStartDate.setTime(order.getPromisedDeliveryDate());
		calOfStartDate.set(Calendar.MINUTE, 0);
		calOfStartDate.set(Calendar.HOUR_OF_DAY, 0);
		calOfStartDate.set(Calendar.SECOND, 0);
		calOfStartDate.set(Calendar.MILLISECOND, 0);

		return cal.after(calOfStartDate);
	}

	@Override
	@SuppressWarnings("serial")
	public Map<String, List<String>> getAllStatuses(String category) {
		if(!category.equalsIgnoreCase(Order.CATEGORY.LAB) ) {
		Map<String, List<String>> statuses = new HashMap<String, List<String>>() 
		
		{
			{
				put(OrderState.NEW, new ArrayList<String>() {
					{
						add(OrderStatus.STATE_NEW.PAYMENT_PENDING);
						add(OrderStatus.STATE_NEW.PAYMENT_FAILED);
						add(OrderStatus.STATE_NEW.PAYMENT_CONFIRMED);
						add(OrderStatus.STATE_NEW.NEW);
						add(OrderStatus.STATE_NEW.DIGITIZED);
						add(OrderStatus.STATE_NEW.PRESCRIPTION_PENDING);
						add(OrderStatus.STATE_NEW.PRESCRIPTION_PROCESSED);
						add(OrderStatus.STATE_NEW.VERIFIED);
						add(OrderStatus.STATE_NEW.CUSTOMER_PRESCRIPTION_PENDING);
					}
				});
				put(OrderState.PROCESSING, new ArrayList<String>() {
					{
						add(OrderStatus.STATE_PROCESSING.NEW);
						add(OrderStatus.STATE_PROCESSING.FULFILLABLE);
						add(OrderStatus.STATE_PROCESSING.PART_FULFILLABLE);
						add(OrderStatus.STATE_PROCESSING.PART_PICKED);
						add(OrderStatus.STATE_PROCESSING.PICKED);
						add(OrderStatus.STATE_PROCESSING.INVOICED);
						add(OrderStatus.STATE_PROCESSING.READY_FOR_PACKED);
					}
				});
				put(OrderState.PACKED, new ArrayList<String>() {
					{
						add(OrderStatus.STATE_PACKED.NEW);
						add(OrderStatus.STATE_PACKED.INVOICED);
						add(OrderStatus.STATE_PACKED.MANIFESTED);
						add(OrderStatus.STATE_PACKED.MERGED);
						add(OrderStatus.STATE_PACKED.READY_TO_MERGED);
					}
				});
				put(OrderState.SHIPPED, new ArrayList<String>() {
					{
						add(OrderStatus.STATE_SHIPPED.REACHED_AT_HUB);
						add(OrderStatus.STATE_SHIPPED.IN_SCAN_AT_HUB);
						add(OrderStatus.STATE_SHIPPED.HANDOVER_TO_COURIER);
						add(OrderStatus.STATE_SHIPPED.READY_TO_MERGED);
						add(OrderStatus.STATE_SHIPPED.MERGED);
						add(OrderStatus.STATE_SHIPPED.DISPATCHED);
						add(OrderStatus.STATE_SHIPPED.IN_TRANSIT);
						add(OrderStatus.STATE_SHIPPED.IN_SCAN_AT_HUB);
						add(OrderStatus.STATE_SHIPPED.OUT_FOR_DELIVERY);
						add(OrderStatus.STATE_SHIPPED.RETRY);
						add(OrderStatus.STATE_SHIPPED.DELIVERED);
						add(OrderStatus.STATE_SHIPPED.CLINIC_DELIVERED);
					}
				});
				put(OrderState.COMPLETED, new ArrayList<String>() {
					{
						add(OrderStatus.STATE_COMPLETE.DELIVERED);
						add(OrderStatus.STATE_COMPLETE.CANCELLED);
						add(OrderStatus.STATE_COMPLETE.RETURNED);
						add(OrderStatus.STATE_COMPLETE.MERGED);
					}
				});
				put(OrderState.CANCELED, new ArrayList<String>() {
					{
						add(OrderStatus.STATE_CANCELLED.CANCELLED);
					}
				});
				put(OrderState.RETURNED, new ArrayList<String>() {
					{
						add(OrderStatus.STATE_RETURNED.RECEIVED);
						add(OrderStatus.STATE_RETURNED.REQUESTED);
					}
				});
			}
		};
		return statuses;
		}
		if(category.equalsIgnoreCase(Order.CATEGORY.LAB) ) {
			Map<String, List<String>> statuses = new HashMap<String, List<String>>() {
				{
					put(LabOrderState.NEW, new ArrayList<String>() {
						{
							add(LabOrderStatus.STATE_NEW.PAYMENT_PENDING);
							add(LabOrderStatus.STATE_NEW.PAYMENT_FAILED);
							add(LabOrderStatus.STATE_NEW.PAYMENT_CONFIRMED);
							add(LabOrderStatus.STATE_NEW.NEW);
							add(LabOrderStatus.STATE_NEW.DIGITIZED);
							add(LabOrderStatus.STATE_NEW.VERIFIED);
						}
					});
					put(LabOrderState.PROCESSING, new ArrayList<String>() {
						{
							add(LabOrderStatus.STATE_PROCESSING.NEW);
							add(LabOrderStatus.STATE_PROCESSING.ASSIGNED);
							add(LabOrderStatus.STATE_PROCESSING.OUT_FOR_SAMPLE_COLLECTION);
							add(LabOrderStatus.STATE_PROCESSING.REACHED_TO_CUSTOMER_LOCATION);
							add(LabOrderStatus.STATE_PROCESSING.SAMPLE_COLLECTED);
						}
					});
					put(LabOrderState.SAMPLE_IN_TRANSIT, new ArrayList<String>() {
						{
							add(LabOrderStatus.STATE_SAMPLE_IN_TRANSIT.OUT_FOR_COLLECTION_CENTRE);
							add(LabOrderStatus.STATE_SAMPLE_IN_TRANSIT.IN_SCAN_COLLECTION_CENTRE);
							add(LabOrderStatus.STATE_SAMPLE_IN_TRANSIT.OUT_FOR_LAB);
							add(LabOrderStatus.STATE_SAMPLE_IN_TRANSIT.IN_SCAN_LAB);
						}
					});
					put(LabOrderState.TESTING_IN_PROGRESS, new ArrayList<String>() {
						{
							add(LabOrderStatus.STATE_TESTING_IN_PROGRESS.NEW);
							add(LabOrderStatus.STATE_TESTING_IN_PROGRESS.SAMPLE_DISMISSED);
							add(LabOrderStatus.STATE_TESTING_IN_PROGRESS.SAMPLE_REJECTED);
							add(LabOrderStatus.STATE_TESTING_IN_PROGRESS.SAMPLE_TESTED);
						}
					});
					put(LabOrderState.COMPLETED, new ArrayList<String>() {
						{
							add(LabOrderStatus.STATE_COMPLETED.NEW);
							add(LabOrderStatus.STATE_COMPLETED.REPORT_GENERATED);
						}
					});
					put(LabOrderState.CANCELLED, new ArrayList<String>() {
						{
							add(LabOrderStatus.STATE_CANCELLED.CANCELLED);
						}
					});
					put(LabOrderState.DELIVERED, new ArrayList<String>() {
						{
							add(LabOrderStatus.STATE_DELIVERED.DIGITAL_REPORT_SENDED);
							add(LabOrderStatus.STATE_DELIVERED.REPORT_IN_TRANSIT);
							add(LabOrderStatus.STATE_DELIVERED.REPORT_DELIVERED);
						}
					});
					put(LabOrderState.CONSULTATION, new ArrayList<String>() {
						{
							add(LabOrderStatus.STATE_CONSULTATION.PENDING);
							add(LabOrderStatus.STATE_CONSULTATION.COMPLETED);
						}
					});
				}
			};
			return statuses;
			}
		return null;
	}

	@Override
	@SuppressWarnings("serial")
	public List<String> getAllSources(){
		List<String> sources = new ArrayList<String>() {
			{
				add(OrderSource.ANDROID);
				add(OrderSource.CALL_CRM);
				add(OrderSource.CRM);
				add(OrderSource.IOS);
				add(OrderSource.MWEB);
				add(OrderSource.UNKNOWN);
				add(OrderSource.VISIT);
				add(OrderSource.PHARMACIST);
				add(OrderSource.JIVA);
				add(OrderSource.POS);

			}
		};
		return sources;
	}

	@Override
	public Boolean isOrderSynced(long orderId) throws Exception {
		TempOrderSync tempOrderSync = tempOrderSyncRepository.findTopByOrderId(orderId);
		if (tempOrderSync == null) {
			ShippingAddress shippingAddress = shippingAddressRepository.findTopByOrderId(orderId);
			if (shippingAddress != null && StringUtils.isNotBlank(shippingAddress.getState())
					&& (shippingAddress.getState().equalsIgnoreCase("Delhi")
							|| shippingAddress.getState().equalsIgnoreCase("Uttar Pradesh")
							|| shippingAddress.getState().equalsIgnoreCase("Haryana"))) {
				TempOrderSync delhiTempOrderSync = tempOrderSyncRepository.findTopByOrderId(101);
				if (delhiTempOrderSync != null && delhiTempOrderSync.isNewSync()) {
					TempOrderSync newTempOrderSync = new TempOrderSync();
					newTempOrderSync.setOrderId(orderId);
					newTempOrderSync.setNewSync(true);
					tempOrderSyncRepository.save(newTempOrderSync);
					return true;
				}
			} else if (shippingAddress != null && StringUtils.isNotBlank(shippingAddress.getState())
					&& (shippingAddress.getState().equalsIgnoreCase("Rajasthan"))) {
				TempOrderSync jaipurTempOrderSync = tempOrderSyncRepository.findTopByOrderId(100);
				if (jaipurTempOrderSync != null && jaipurTempOrderSync.isNewSync()) {
					TempOrderSync newTempOrderSync = new TempOrderSync();
					newTempOrderSync.setOrderId(orderId);
					newTempOrderSync.setNewSync(true);
					tempOrderSyncRepository.save(newTempOrderSync);
					return true;
				}
			}
			return false;
		} else {
			return true;
		}
	}

	@Override
	@Transactional
	public Order syncPrice(User user, long orderId, long facilityId) throws Exception {
		Order order = orderRepository.findOne(orderId);
		if (order == null) {
			throw new InvalidOrderException("Invalid Order-Id :: " + orderId);
		}
		List<OrderItem> orderItems = orderItemService.findByOrderId(orderId);
		if (orderItems != null && !orderItems.isEmpty()) {
			List<String> skuList = orderItems.parallelStream()
					.filter(orderItem -> orderItem.getQuantity() > 0 && orderItem.isActive()).map(OrderItem::getSku)
					.distinct().collect(Collectors.toList());

			List<OrderItem> updatedOrderItems = orderItems;
			try {
				CompletableFuture<List<OrderItem>> orderItemsFuture = CompletableFuture
						.supplyAsync(() -> orderItemService.updateOrderLineInfo(order, orderItems, StringUtils.EMPTY,
								Boolean.FALSE));

				CompletableFuture<List<ProductStock>> productStockFuture = CompletableFuture.supplyAsync(
						() -> productStockService.getProductStockByFacilityIdAndSkuIds(facilityId, skuList));

				CompletableFuture<List<OrderItem>> future = orderItemsFuture.thenCombine(productStockFuture,
						(orderItemsFutureResponse, productStockFutureResponse) -> {
							Map<String, ProductStock> productStockMap = new HashMap<String, ProductStock>();
							if (productStockFutureResponse != null && !productStockFutureResponse.isEmpty()) {
								productStockMap = productStockFutureResponse.parallelStream().collect(Collectors.toMap(
										ProductStock::getSku, Function.identity(), (productStock1, productStock2) -> {
											return productStock2;
										}));
							}

							for (OrderItem orderItem : orderItemsFutureResponse) {
								ProductStock productStock = productStockMap.get(orderItem.getSku());
								if (null != productStock) {
									if (productStock.getMrp() > 0) {
										orderItem.setMrp(productStock.getMrp());
										float salePrice = orderItem.getMrp() * (100 - orderItem.getDiscount()) / 100;
										orderItem.setSalePrice(salePrice);
									}
								}
							}
							return orderItemsFutureResponse;
						});

				updatedOrderItems = future.get();
			} catch (Exception e) {
				LOGGER.error(e.getMessage(), e);
			}

			order.setOrderItems(updatedOrderItems);
			String couponCode = order.getCouponCode();
			if(StringUtils.isNotBlank(order.getManualCouponCode())) {
				couponCode = order.getManualCouponCode();
			}
			applyCoupon(order, couponCode, "update");
			// Save order items
			float totalSalePrice = 0;
			float totalMrp = 0;
			float discount = 0;
			for (Iterator<OrderItem> it = order.getOrderItems().iterator(); it.hasNext();) {
				OrderItem orderItem = it.next();
				if (orderItem.getQuantity() > 0 && orderItem.isActive()) {
					float salePrice = orderItem.getMrp() * (100 - orderItem.getDiscount()) / 100;
					totalSalePrice += salePrice * orderItem.getQuantity();
					totalMrp += orderItem.getMrp() * orderItem.getQuantity();
					discount += ((orderItem.getMrp() - salePrice) * orderItem.getQuantity());
					orderItem.setSalePrice(salePrice);
				}
			}
			// order = updateShippingChargeInorder(order);
			totalSalePrice = totalSalePrice - order.getCouponDiscount() + order.getShippingCharge()
					+ (float) order.getUrgentDeliveryCharge() - order.getRedeemedCarePoints() - order.getRedeemedCash();
			// order.setRedeemedCarePoints(0);
			// order.setRedeemedCash(0);

			if (totalSalePrice > 0) {
				order.setTotalSalePrice(totalSalePrice);
			} else {
				order.setTotalSalePrice(0);
			}
			// order = updateShippingChargeInorder(order);
			order.setTotalSalePrice(totalSalePrice);
			order.setDiscount(discount);
			order.setTotalMrp(totalMrp);
			order.setTotalDiscount(discount + order.getCouponDiscount());
			orderItemService.save(order.getOrderItems());
			return orderRepository.save(order);
			// return true;
		}
		throw new OrderItemNotFoundException("Order items not found. Order-id :: " + orderId);
	}

	@Override
	@Transactional
	public float updateOrderOffsetScore(User user, long orderId, int offsetScore) {
		if (orderId <= 0 || offsetScore == 0) {
			throw new IllegalArgumentException("Invalid order-id provided");
		}
		Order order = orderRepository.findOne(orderId);
		if (order == null) {
			throw new OrderNotFoundException("order not found with order-id " + orderId);
		}
		order.setOffsetScore(order.getOffsetScore() + offsetScore);
		order.setScore(order.getScore() + offsetScore);
		orderRepository.save(order);
		try {
			orderEventService.updateOrderEvent(order, order.getId() + order.getUpdatedAt().toString(),
					OrderEvent.ORDER_SCORE_UPDATE, user, 0, null, null);
		} catch (Exception e) {
			LOGGER.error("Order score update Event genration with order-id " + orderId + " failed due to : "
					+ e.getMessage(), e);
		}
		return order.getScore();
	}

	@Override
	public Order updatePromisedDeliveryDetails(User user, long orderId, Map<String, Object> partialOrderObject) {
		if (orderId <= 0 || partialOrderObject == null || partialOrderObject.isEmpty()) {
			throw new IllegalArgumentException("Invalid order-id provided");
		}
		Order order = orderRepository.findOne(orderId);
		if (order == null) {
			throw new OrderNotFoundException("order not found with order-id " + orderId);
		}
		String promisedDeliveryDate = "promised_delivery_date";
		String promisedDeliveryBeginDate = "promised_delivery_begin_time";
		String promisedDeliveryEndDate = "promised_delivery_end_time";
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		if (partialOrderObject.containsKey(promisedDeliveryDate)
				&& partialOrderObject.get(promisedDeliveryDate) != null) {
			order.setPromisedDeliveryDate(new Timestamp((long) partialOrderObject.get(promisedDeliveryDate)));
			ShippingAddress shippingAddress = orderShippingAddressService.findByOrderId(orderId);
			if (shippingAddress != null) {
				// CALL SHIPPING TAT API BASED ON PINCODE
				String pincode = String.valueOf(shippingAddress.getPincode());
				if (pincode != null) {
					PlacePincode placePincode = null;
					try {
						placePincode = shippingService.getPlaceInformationByPincode(pincode);
					} catch (Exception e) {
						LOGGER.error("Error in getting place pincode with pincode {}", pincode);
					}
					int deliveryTatDays = calculateDeliveryTat(placePincode);
					DateTime dispatchDate = new DateTime(order.getPromisedDeliveryDate());
					int dispatchTatDays = deliveryTatDays - 1;
					order.setDispatchDate(new Timestamp(dispatchDate.minusDays(dispatchTatDays).getMillis()));
				}
			}
		}
		try {
			if (partialOrderObject.containsKey(promisedDeliveryBeginDate)
					&& partialOrderObject.get(promisedDeliveryBeginDate) != null) {
				order.setPromisedDeliveryBeginTime(
						new Time(sdf.parse((String) partialOrderObject.get(promisedDeliveryBeginDate)).getTime()));
			}
			if (partialOrderObject.containsKey(promisedDeliveryEndDate)
					&& partialOrderObject.get(promisedDeliveryEndDate) != null) {
				order.setPromisedDeliveryEndTime(
						new Time(sdf.parse((String) partialOrderObject.get(promisedDeliveryEndDate)).getTime()));
			}
		} catch (Exception e) {
		}
		if (user.getId() > 0) {
			order.setUpdatedBy(String.valueOf(user.getId()));
		}
		order = orderRepository.save(order);
		try {
			orderEventService.updateOrderEvent(order, order.getId() + order.getUpdatedAt().toString(),
					OrderEvent.ORDER_PROMISED_DELIVERY_DETAIL_UPDATE, user, 0, null, null);
		} catch (Exception e) {
			LOGGER.error("Order promised delivery detail update Event genration with order-id " + orderId
					+ " failed due to : " + e.getMessage(), e);
		}
		return order;
	}

	@Override
	public Order getActiveOrderByPatientId(long patientId) {
		if (patientId <= 0) {
			throw new BadRequestException("patient-id can not be empty");
		}
		return orderRepository.findTopByPatientIdAndStatusNotInOrderByIdAsc(patientId,
				Arrays.asList(OrderStatus.STATE_CANCELLED.CANCELLED));
	}

	@Override
	public Map<String, Object> getApplicableShippingCharge(long customerId, long pincode, float totalMrp)
			throws Exception {
		float shippingCharge = 0;
		Map<String, Object> map = new HashMap<>();
		if (customerId <= 0 || pincode <= 0) {
			return map;
		}
		long previousOrderCount = 0;
		try {
			List<String> shippingChargeApplicableState = new ArrayList<>();
			shippingChargeApplicableState.add(OrderState.SHIPPED);
			List<String> shippingChargeApplicableStatus = new ArrayList<>();
			shippingChargeApplicableStatus.add(OrderStatus.STATE_SHIPPED.DELIVERED);
			previousOrderCount = orderRepository.countByCustomerIdAndStateInAndStatusIn(customerId,
					shippingChargeApplicableState, shippingChargeApplicableStatus);

			// List<String> cancelledState = new ArrayList<>();
			// cancelledState.add(OrderState.CANCELED);
			// previousOrderCount =
			// orderRepository.countByCustomerIdAndStateNotIn(customerId, cancelledState);
		} catch (Exception e) {
			e.printStackTrace();
		}
		PlacePincode placePincode = null;
		try {
			placePincode = shippingService.getPlaceInformationByPincode(String.valueOf(pincode));
			if (placePincode == null) {
				throw new NotFoundException("placePincode not found with " + pincode);
			}
			map.put("minimum_mrp", placePincode.getFreeShippingMinOrder());
		} catch (Exception e) {
			LOGGER.error("Error in getting place pincode with pincode {}, Exception : {}", pincode, e.getMessage());
		}
		if (totalMrp > 0) {
			shippingCharge = getApplicableShippingCharge(previousOrderCount, totalMrp, null, false, placePincode);
		}
		map.put("shipping_charge", shippingCharge);
		return map;
	}

	private float getApplicableShippingChargeForSavedOrder(long customerId, float totalMrp,
			Float previousShippingCharge, boolean isShippingChargeExempted, PlacePincode placePincode) {
		float shippingCharge = 0;
		if (customerId <= 0 || placePincode == null || isShippingChargeExempted) {
			return shippingCharge;
		}
		long previousOrderCount = 0;
		try {
			List<String> shippingChargeApplicableState = new ArrayList<>();
			shippingChargeApplicableState.add(OrderState.SHIPPED);
			List<String> shippingChargeApplicableStatus = new ArrayList<>();
			shippingChargeApplicableStatus.add(OrderStatus.STATE_SHIPPED.DELIVERED);
			previousOrderCount = orderRepository.countByCustomerIdAndStateInAndStatusIn(customerId,
					shippingChargeApplicableState, shippingChargeApplicableStatus);
		} catch (Exception e) {
			LOGGER.error("Error in getting order count with pincode {}", e.getMessage());
		}
		if (totalMrp > 0) {
			shippingCharge = getApplicableShippingCharge(previousOrderCount, totalMrp, previousShippingCharge,
					isShippingChargeExempted, placePincode);
		}
		return shippingCharge;
	}

	private float getApplicableShippingCharge(long previousOrderCount, float totalMrp, Float previousShippingCharge,
			boolean isShippingChargeExempted, PlacePincode placePincode) {
		float shippingCharge = 0;
		if (placePincode == null || isShippingChargeExempted) {
			return shippingCharge;
		}
		if (previousOrderCount <= 0) {
			return shippingCharge;
		}
		if (totalMrp < placePincode.getFreeShippingMinOrder()) {
			shippingCharge = (float) placePincode.getShippingFee();
		}
		// previousShippingCharge is nullable
		if (previousShippingCharge != null && shippingCharge > previousShippingCharge) {
			shippingCharge = previousShippingCharge;
		}
		return shippingCharge;
	}

	@Override
	public Boolean removeShippingCharge(long orderId) {
		if (orderId > 0) {
			Order order = orderRepository.findById(orderId);
			if (order == null) {
				throw new OrderNotFoundException("order not found with order-id " + orderId);
			}
			order.setTotalSalePrice(order.getTotalSalePrice() - order.getShippingCharge());
			order.setShippingCharge(0);
			order.setShippingChargeExempted(true);
			orderRepository.save(order);
			return true;
		}
		throw new IllegalArgumentException("Invailed order-id has provided");
	}

	@Override
	public Long getOrderCountByCustomerId(long customerId, List<String> orderStates) throws Exception {
		if (customerId > 0) {
			return orderRepository.countByCustomerIdAndStateIn(customerId, orderStates);
		}
		throw new IllegalArgumentException("Invailed customer-id ");
	}

	private Order updateShippingChargeInorder(Order order) {
		if (order != null) {
			ShippingAddress shippingAddress = shippingAddressRepository.findTopByOrderId(order.getId());
			PlacePincode placePincode = null;
			if (shippingAddress != null && shippingAddress.getPincode() > 0) {
				try {
					placePincode = shippingService
							.getPlaceInformationByPincode(String.valueOf(shippingAddress.getPincode()));
				} catch (Exception e) {
					LOGGER.error("Error in getting place pincode with pincode {}", shippingAddress.getPincode());
				}
			}
			order.setShippingCharge(getApplicableShippingChargeForSavedOrder(order.getCustomerId(), order.getTotalMrp(),
					order.getShippingCharge(), order.isShippingChargeExempted(), placePincode));
		}
		return order;
	}

	@Override
	public boolean updatePackagingType(long orderId, String packingType) {
		if (orderId > 0 && Strings.isNotBlank(packingType)) {
			Order order = orderRepository.findById(orderId);
			if (order == null) {
				throw new BadRequestException("Invalid order id!");
			}
			if (order.getPackagingType() != null && Order.PACKAGING_PRIORITY.contains(order.getPackagingType())) {
				int existingPriority = Order.PACKAGING_PRIORITY.indexOf(order.getPackagingType());
				int nextPriority = Order.PACKAGING_PRIORITY.indexOf(packingType);
				if (existingPriority < nextPriority) {
					order.setPackagingType(packingType);
				} else {
					throw new BadRequestException("Invalid packaging type provided, you can not change from "
							+ order.getPackagingType() + " to " + packingType);
				}
			} else {
				order.setPackagingType(packingType);
			}
			orderRepository.save(order);
			return true;
		}
		throw new BadRequestException("Order-id or packing-type can not be null or empty!");
	}

	private void setUrgentOrderCountInRedis() {
		DateTime todayDatePlusOneDay = DateTime.now(DateTimeZone.UTC).plusDays(1);
		Calendar cal = Calendar.getInstance();
		cal.setTime(todayDatePlusOneDay.toDate());
		cal.set(Calendar.HOUR, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		int orderCount = 1;
		String currentCount = null;
		try {
			currentCount = redisTemplate.opsForValue().get("expressOrderCount");
			if (currentCount != null) {
				orderCount = Integer.parseInt(currentCount) + 1;
			}
		} catch (Exception e) {

		}
		redisTemplate.opsForValue().set("expressOrderCount", String.valueOf(orderCount));
		redisTemplate.expireAt("expressOrderCount", cal.getTime());
	}

	@Override
	public Page<Order> getOrdersForDeliveryOptionChangeTracking(String deliveryOption, Pageable pageable)
			throws Exception {
		if (deliveryOption == null) {
			throw new BadRequestException("Invalid serviceType or deliveryOption!");
		}
		List<String> statuses = OrderStatus.REFUNDABLE_ORDER_STATUS;
		Timestamp currentDateTime = new Timestamp(System.currentTimeMillis());
		Page<Order> orders = orderRepository.findByDeliveryOptionAndPromisedDeliveryDateBeforeAndStatusIn(
				deliveryOption, currentDateTime, statuses, pageable);
		if (orders == null || !orders.hasContent()) {
			throw new OrderNotFoundException("No active orders found");
		}
		return orders;
	}

	@Override
	public Page<Order> getOrdersForServiceTypeChangeTracking(String serviceType, Pageable pageable) throws Exception {
		if (serviceType == null) {
			throw new BadRequestException("Invalid serviceType or deliveryOption!");
		}
		List<String> statuses = OrderStatus.REFUNDABLE_ORDER_STATUS;
		Timestamp currentDateTime = new Timestamp(System.currentTimeMillis());
		Page<Order> orders = orderRepository.findByServiceTypeAndPromisedDeliveryDateBeforeAndStatusIn(serviceType,
				currentDateTime, statuses, pageable);
		if (orders == null || !orders.hasContent()) {
			throw new OrderNotFoundException("No active orders found");
		}
		return orders;
	}

	@Override
	public Boolean updateDeliveryOption(long orderId, Order order, String reason, boolean systemUpdated,
			Map<String, Object> updatedMap, User user) {
		if (order == null && orderId > 0) {
			order = orderRepository.findById(orderId);
		}
		if (order == null) {
			throw new BadRequestException("Invalid order id!");
		}
		if (order.getDeliveryOption() != null) {
			String deliveryOption = Order.DELIVERY_OPTION.NORMAL;

			if (updatedMap != null && !updatedMap.isEmpty()) {
				return updateDeliveryOption(order, updatedMap, user);
			}

			if (Order.DELIVERY_OPTION.URGENT.equalsIgnoreCase(order.getDeliveryOption())
					&& OrderState.NEW.equalsIgnoreCase(order.getState())
					&& OrderStatus.STATE_NEW.ALL_STATUSES.contains(order.getStatus()) && systemUpdated) {
				DateTime newPromisedDeliveryDate = new DateTime(order.getPromisedDeliveryDate());
				order.setPromisedDeliveryDate(new Timestamp(newPromisedDeliveryDate.plusDays(1).getMillis()));
				order.setDispatchDate(order.getPromisedDeliveryDate());
				order = orderRepository.save(order);
				return true;
			}

			double urgentDeliveryCharge = order.getUrgentDeliveryCharge();
			if (systemUpdated) {
				order.setManualDeliveryOptionChangeReason(null);
				order.setDeliveryOptionChangeReason(reason);
			} else {
				order.setDeliveryOptionChangeReason(null);
				order.setManualDeliveryOptionChangeReason(reason);
				order.setPreferredDeliveryOption(deliveryOption);
				order.setTotalSalePrice(order.getTotalSalePrice() - (float) order.getUrgentDeliveryCharge());
				order.setUrgentDeliveryCharge(0);
			}
			order.setDeliveryOption(deliveryOption);
			order = orderRepository.save(order);
			if (OrderStatus.INVENTORY_REDUCTION_ORDER_STATUS.contains(order.getStatus())) {
				productStockService.updateExpressInventory(new Long(order.getFacilityCode()),
						getUpdatedInventoryMap(orderId));
			}
			try {
				orderEventService.updateOrderEvent(order, order.getId() + order.getUpdatedAt().toString(),
						OrderEvent.ORDER_DELIVERY_OPTION_UPDATED, user, order.getFacilityCode(), null,
						urgentDeliveryCharge);
			} catch (Exception e) {
				LOGGER.error("Order Delivery change update Event genration with order-id " + order.getId()
						+ " failed due to : " + e.getMessage(), e);
			}
			return true;
		}
		return false;
	}

	private Boolean updateDeliveryOption(Order order, Map<String, Object> updatedMap, User user) {
		if (updatedMap != null && !updatedMap.isEmpty()) {
			String manualReason = updatedMap.containsKey("delivery_option_change_reason")
					? (String) updatedMap.get("delivery_option_change_reason")
					: null;
			String comment = updatedMap.containsKey("delivery_option_change_comment")
					? (String) updatedMap.get("delivery_option_change_comment")
					: null;
			String deliveryOption = updatedMap.containsKey("delivery_option")
					? (String) updatedMap.get("delivery_option")
					: null;

			if (deliveryOption != null && deliveryOption.equalsIgnoreCase(order.getDeliveryOption())) {
				return true;
			}

			if (manualReason != null) {
				manualReason += comment != null ? " (" + comment + ")" : "";
			} else {
				manualReason = comment;
			}
			order.setManualDeliveryOptionChangeReason(manualReason);
			order.setDeliveryOptionChangeReason(null);
			order.setDeliveryOption(deliveryOption);
			order.setPreferredDeliveryOption(deliveryOption);
			if (Order.DELIVERY_OPTION.URGENT.equalsIgnoreCase(order.getDeliveryOption())) {
				OrderDeliveryObject checkForUrgentDelivery = getOrderDeliveryObject(order);
				if (checkForUrgentDelivery != null) {
					if (Order.DELIVERY_OPTION.URGENT.equalsIgnoreCase(order.getDeliveryOption())
							&& !Order.DELIVERY_OPTION.URGENT
									.equalsIgnoreCase(checkForUrgentDelivery.getDeliveryOption())
							&& Order.DELIVERY_OPTION.URGENT.equalsIgnoreCase(order.getPreferredDeliveryOption())) {
						throw new InvalidDeliveryOptionException("Order  Delivery Option can not marked as URGENT : "
								+ checkForUrgentDelivery.getDeliveryOptionChangeReason());
					}
					Map<String, Integer> skusQtyMap = new HashMap<String, Integer>();
					if (order != null && !order.getOrderItems().isEmpty()) {
						skusQtyMap = order.getOrderItems().parallelStream().filter(orderItem -> orderItem.isActive())
								.collect(Collectors.toMap(OrderItem::getSku, OrderItem::getQuantity));
					}
					// 1. Update order count in redis
					setUrgentOrderCountInRedis();
					// 2. Update express_in_flight_stock in
					// inventory.product_stock
					if (skusQtyMap != null && !skusQtyMap.isEmpty()) {
						productStockService.updateExpressInventory((long) order.getFacilityCode(), skusQtyMap);
					}
					order.setUrgentDeliveryCharge(checkForUrgentDelivery.getUrgentDeliveryCharge());
					order.setTotalSalePrice(order.getTotalSalePrice() + (float) order.getUrgentDeliveryCharge());
					order.setPromisedDeliveryDate(cartService.getPromisedDeliveryDateForUrgentOrder());
					order.setDispatchDate(cartService.getPromisedDeliveryDateForUrgentOrder());
					order.setServiceType(Order.SERVICE_TYPE.NORMAL);
					order.setPreferredServiceType(Order.SERVICE_TYPE.NORMAL);
				}
			} else if (Order.DELIVERY_OPTION.NORMAL.equalsIgnoreCase(order.getDeliveryOption())) {
				order.setTotalSalePrice(order.getTotalSalePrice() - (float) order.getUrgentDeliveryCharge());
				order.setUrgentDeliveryCharge(0);
				if (OrderStatus.INVENTORY_REDUCTION_ORDER_STATUS.contains(order.getStatus())) {
					productStockService.updateExpressInventory(new Long(order.getFacilityCode()),
							getUpdatedInventoryMap(order.getId()));
				}
			}
			order = orderRepository.save(order);
			try {
				orderEventService.updateOrderEvent(order, order.getId() + order.getUpdatedAt().toString(),
						OrderEvent.ORDER_DELIVERY_OPTION_UPDATED, user, order.getFacilityCode(), null, null);
			} catch (Exception e) {
				LOGGER.error("Order score update Event genration with order-id " + order.getId() + " failed due to : "
						+ e.getMessage(), e);
			}
			return true;
		}
		return false;
	}

	@Override
	public Boolean updateServiceType(long orderId, Order order, boolean systemUpdated, String reason,
			Map<String, Object> updateMap, User user) {
		if (order == null && orderId > 0) {
			order = orderRepository.findById(orderId);
		}
		if (order == null) {
			throw new BadRequestException("Invalid order id!");
		}
		String serviceType = null;
		if (updateMap != null && !updateMap.isEmpty()) {

			reason = updateMap.containsKey("service_type_change_reason")
					? (String) updateMap.get("service_type_change_reason")
					: null;
			String comment = updateMap.containsKey("service_type_change_comment")
					? (String) updateMap.get("service_type_change_comment")
					: null;
			serviceType = updateMap.containsKey("service_type") ? (String) updateMap.get("service_type") : null;
			if (reason != null) {
				reason += comment != null ? " (" + comment + ")" : "";
			} else {
				reason = comment != null ? comment : "";
			}
			if (serviceType != null && serviceType.equalsIgnoreCase(order.getServiceType())) {
				return true;
			}
			order.setManualServiceTypeChangeReason(reason);
			order.setPreferredServiceType(serviceType);
			order.setServiceType(serviceType);
			// order.setDeliveryOption(Order.DELIVERY_OPTION.NORMAL);
		} else if (order.getServiceType() != null
				&& Order.SERVICE_TYPE.LF_ASSURED.equalsIgnoreCase(order.getServiceType())) {
			if (OrderState.NEW.equalsIgnoreCase(order.getState())
					&& OrderStatus.STATE_NEW.ALL_STATUSES.contains(order.getStatus()) && systemUpdated) {
				DateTime newPromisedDeliveryDate = new DateTime(order.getPromisedDeliveryDate());
				order.setPromisedDeliveryDate(new Timestamp(newPromisedDeliveryDate.plusDays(1).getMillis()));
				order.setDispatchDate(new Timestamp(newPromisedDeliveryDate.minusDays(1).getMillis()));
				order = orderRepository.save(order);
				return true;
			}
			if (systemUpdated) {
				reason = OrderDeliveryObject.SERVICE_TYPE_CHANGE_REASON.COMMITED_DELIVERY_TIME_LINE_EXCEEDED;
				order.setManualServiceTypeChangeReason(null);
				order.setServiceTypeChangeReason(reason);
			} else {
				order.setManualServiceTypeChangeReason(reason);
				order.setServiceTypeChangeReason(null);
				order.setPreferredServiceType(Order.SERVICE_TYPE.NORMAL);
			}
			order.setDeliveryOption(Order.DELIVERY_OPTION.NORMAL);
			order.setServiceType(Order.SERVICE_TYPE.NORMAL);
		}
		orderRepository.save(order);
		try {
			orderEventService.updateOrderEvent(order, order.getId() + order.getUpdatedAt().toString(),
					OrderEvent.ORDER_SERVICE_TYPE_UPDATED, user, order.getFacilityCode(), null, null);
		} catch (Exception e) {
			LOGGER.error("Order service type updation Event genration with order-id " + orderId + " failed due to : "
					+ e.getMessage(), e);
		}
		return true;
	}

	private HashMap<String, Integer> getUpdatedInventoryMap(long orderId) {
		if (orderId > 0) {
			List<OrderItem> orderItems = orderItemService.findByOrderId(orderId);
			HashMap<String, Integer> map = new HashMap<>();
			if (orderItems != null && !orderItems.isEmpty()) {
				orderItems.parallelStream().map(orderItem -> {
					if (orderItem.getQuantity() > 0) {
						map.put(orderItem.getSku(), Math.negateExact(orderItem.getQuantity()));
					}
					return map;
				});
			}
			return map;
		}
		return null;
	}

	private HashMap<String, Integer> getUpdatedInventoryMap(List<OrderItem> orderItems) {
		if (orderItems != null && !orderItems.isEmpty()) {
			HashMap<String, Integer> map = new HashMap<>();
			if (orderItems != null && !orderItems.isEmpty()) {
				for (OrderItem orderItem : orderItems) {
					if (orderItem.getQuantity() > 0) {
						map.put(orderItem.getSku(), Math.negateExact(orderItem.getQuantity()));
					}
				}
			}
			return map;
		}
		return null;
	}

	public OrderDeliveryObject getOrderDeliveryObject(Order order) {
		OrderDeliveryObject orderDeliveryObject = new OrderDeliveryObject();
		orderDeliveryObject.setDeliveryOption(Cart.DELIVERY_OPTION.NORMAL);
		orderDeliveryObject.setServiceType(Cart.SERVICE_TYPE.NORMAL);
		if (order != null) {
			if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
				List<OrderItem> orderItems = orderItemService.findByOrderId(order.getId());
				order.setOrderItems(orderItems);
			}
			if (order.getShippingAddress() == null) {
				ShippingAddress shippingAddress = shippingAddressRepository.findTopByOrderId(order.getId());
				order.setShippingAddress(shippingAddress);
			}
			try {
				Map<String, Object> deliveryTypeHashMap = null;
				Map<String, Integer> skusQtyList = new HashMap<String, Integer>();
				if (order != null && !order.getOrderItems().isEmpty()) {
					skusQtyList = order.getOrderItems().parallelStream().filter(orderItem -> orderItem.isActive())
							.collect(Collectors.toMap(OrderItem::getSku, OrderItem::getQuantity));
				}
				if (order.getShippingAddress() != null) {
					deliveryTypeHashMap = shippingService.getDeliveryType(
							String.valueOf(order.getShippingAddress().getPincode()), order.getFacilityCode(),
							skusQtyList);
				}
				if (deliveryTypeHashMap != null) {
					if (deliveryTypeHashMap.containsKey(Cart.ORDER_DELIVERY_TYPE.IS_LC_ASSURED_DELIVERY) && Boolean.TRUE
							.equals(deliveryTypeHashMap.get(Cart.ORDER_DELIVERY_TYPE.IS_LC_ASSURED_DELIVERY))) {
						orderDeliveryObject.setServiceType(Cart.SERVICE_TYPE.LF_ASSURED);
					}
					if (deliveryTypeHashMap.containsKey("urgent_delivery_charge")) {
						orderDeliveryObject.setUrgentDeliveryCharge(
								new Double(deliveryTypeHashMap.get("urgent_delivery_charge").toString()).floatValue());
					}

					if (deliveryTypeHashMap.containsKey(Cart.ORDER_DELIVERY_TYPE.IS_URGENT_DELIVERY) && Boolean.TRUE
							.equals(deliveryTypeHashMap.get(Cart.ORDER_DELIVERY_TYPE.IS_URGENT_DELIVERY))) {
						orderDeliveryObject.setDeliveryOption(Cart.DELIVERY_OPTION.URGENT);
						// check for time> 12 :00 i.e 18: 30 and < 4: 00 pm i.e 10:30:00
						Boolean resultForUrgentdelivery = isUrgentOrdersAllowed(expressDeliveryStartTime,
								expressDeliveryEndTime);
						LOGGER.info("FINAL RESULT FOR IS ELIGIBILITY IS " + resultForUrgentdelivery);
						LOGGER.debug("FINAL RESULT FOR IS ELIGIBILITY IS " + resultForUrgentdelivery);
						if (Boolean.FALSE.equals(resultForUrgentdelivery)) {
							LOGGER.info(
									"ORDER NOT ELIGIBLE FOR EXPRESS TIME LIMIT EXCEEDED  " + resultForUrgentdelivery);
							LOGGER.debug(
									"ORDER NOT ELIGIBLE FOR EXPRESS TIME LIMIT EXCEEDED  " + resultForUrgentdelivery);
							orderDeliveryObject.setServiceType(orderDeliveryObject.getServiceType());
							orderDeliveryObject.setDeliveryOption(Cart.DELIVERY_OPTION.NORMAL);
							orderDeliveryObject.setDeliveryOptionChangeReason(
									OrderDeliveryObject.DELIVERY_OPTION_CHANGE_REASON.TIME_LIMIT_EXCEEDED);
						}
						// check for order count < 100
						try {
							String urgentOrderCount = redisTemplate.opsForValue().get("expressOrderCount");
							LOGGER.info("URGENT ORDER STRING COUNT IS  " + urgentOrderCount);
							LOGGER.debug("URGENT ORDER STRING COUNT IS  " + urgentOrderCount);
							if (urgentOrderCount != null) {
								int count = Integer.parseInt(urgentOrderCount);
								LOGGER.info("URGENT ORDER INTEGER COUNT IS  " + count);
								LOGGER.debug("URGENT ORDER INTEGER COUNT IS  " + count);
								if (count >= 100) {
									LOGGER.info("URGENT ORDER INTEGER COUNT IS GREATER THAN 100 SO NORMAL  " + count);
									LOGGER.debug("URGENT ORDER INTEGER COUNT IS GREATER THAN 100 SO NORMAL  " + count);
									orderDeliveryObject.setDeliveryOption(Cart.DELIVERY_OPTION.NORMAL);
									orderDeliveryObject.setDeliveryOptionChangeReason(
											OrderDeliveryObject.DELIVERY_OPTION_CHANGE_REASON.ORDER_COUNT_EXCEEDED);
								}
							}
						} catch (Exception e) {
							LOGGER.error("UNABLE TO FIND REDIS ORDER COUNT");
						}
					}
				}
			} catch (Exception e) {
				LOGGER.error("Error in fetching delivery type for customer : cart uid : {} : Exception : {}",
						order.getId(), e.getMessage());
			}
		}

		return orderDeliveryObject;
	}

	// check for time> 00:00 i.e 18: 30 and < 4: 00 pm i.e 10:30:00
	private boolean isUrgentOrdersAllowed(String minS, String maxS) {
		SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
		String currS = formatter.format(new Date());
		LocalTime minT = LocalTime.parse(minS);
		LocalTime maxT = LocalTime.parse(maxS);
		LocalTime currT = LocalTime.parse(currS);
		LOGGER.info("OK CHECKING MINS__" + minS + "__MAXS__" + maxS + "__MINT __" + minT + "__CURRT__" + currT
				+ "__MAXT__" + maxT);
		LOGGER.debug("OK CHECKING MINS__" + minS + "__MAXS__" + maxS + "__MINT __" + minT + "__CURRT__" + currT
				+ "__MAXT__" + maxT);
		System.out.println("OK CHECKING MINS__" + minS + "__MAXS__" + maxS + "__MINT __" + minT + "__CURRT__" + currT
				+ "__MAXT__" + maxT);
		boolean result = minT.isBefore(maxT) ? (currT.isAfter(minT) && currT.isBefore(maxT))
				: !(currT.isAfter(maxT) && currT.isBefore(minT));
		System.out.println("RESULT FOR IS ELIGIBILITY IS " + result);
		LOGGER.info("RESULT FOR IS ELIGIBILITY IS " + result);
		LOGGER.debug("RESULT FOR IS ELIGIBILITY IS " + result);
		return result;
	}

	@Override
	public List<Reason> getReasonByGroup(String groupType) {
		if (StringUtils.isBlank(groupType)) {
			return reasonRepository.findAll();
		} else {
			return reasonRepository.findByGroupOrderByPriorityDesc(groupType);
		}
	}

	@Override
	public Boolean isEligibleForUrgentDelivery(long orderId) {
		if (orderId > 0) {
			Order order = orderRepository.findById(orderId);
			if (order == null) {
				throw new BadRequestException("Invalid order id!");
			}
			try {
				if (OrderState.NEW.equalsIgnoreCase(order.getState())
						&& OrderStatus.STATE_NEW.ALL_STATUSES.contains(order.getStatus())) {
					OrderDeliveryObject orderDeliveryObject = getOrderDeliveryObject(order);
					if (null != orderDeliveryObject) {
						return Order.DELIVERY_OPTION.URGENT.equalsIgnoreCase(orderDeliveryObject.getDeliveryOption());
					}
				}
				return false;
			} catch (Exception e) {
				return false;
			}
		}
		throw new BadRequestException("Invalid parameters provided");
	}

	@Override
	public boolean createAutoVerifiedFailedEvent(long orderId) {
		Order order = orderRepository.findOne(orderId);
		if (order != null) {
			try {
				orderEventService.updateOrderEvent(order, order.getId() + order.getUpdatedAt().toString(),
						OrderEvent.ORDER_AUTO_VERIFIED_FAILED, null, order.getFacilityCode(), null,
						order.getUrgentDeliveryCharge());
				return true;
			} catch (Exception e) {
				LOGGER.error("Error in creating auto verified failed event for : order id : {} : Exception : {}",
						order.getId(), e.getMessage());
			}
		}
		return false;
	}

	@Override
	public List<Long> getCustomerIdsByCreatedAtAfter(Timestamp createdAtAfter) {
		return orderRepository.findDistinctCustomerIdByCreatedAtAfter(createdAtAfter);
	}

	private List<OrderItem> updateOrderItemWithCatalog(String location, List<OrderItem> orderItems) {
		if (orderItems != null && !orderItems.isEmpty()) {
			orderItems = orderItemService.updateOrderLineInfo(null, orderItems, location, false);
			List<OrderItem> inactiveStatusMedicines = orderItems.stream()
					.filter(orderItem -> Medicine.STATUS.INACTIVE_STATUS.contains(orderItem.getMedicineStatus()))
					.collect(Collectors.toList());
			if (inactiveStatusMedicines != null && !inactiveStatusMedicines.isEmpty()) {
				String nameSkuStatus = "";
				for (OrderItem medicine : inactiveStatusMedicines) {
					nameSkuStatus += "< " + medicine.getSku() + ":" + medicine.getName() + "is a <"
							+ medicine.getMedicineStatus() + "> medicines";
					break;
				}
				throw new OrderException("This order cannot be Verified as " + nameSkuStatus);
			}
		}
		return orderItems;
	}

	private Map<String, Object> updateOrderItemWithCatalogAndApplyCoupon(Order order, String location,
			boolean isFinalPrice, String couponCode, String action, Boolean applyCoupon) {
		if (order != null && !CollectionUtils.isEmpty(order.getOrderItems())) {
			order.setOrderItems(orderItemService.updateOrderLineInfo(order, order.getOrderItems(), location, isFinalPrice));
			if(applyCoupon) {
				return applyCoupon(order, couponCode, action);
			}
		}
		return new HashMap<String, Object>();
	}

	private List<String> getExcessiveOrderedItemSku(long patientId, List<OrderItem> orderItems) {
		if (patientId <= 0)
			return new ArrayList<>();
		Date createdAt = new Date(DateTime.now().minusDays(OrderPropertyConstant.BULK_ORDER_DAYS).getMillis());
		List<Order> orders = orderRepository.findByPatientIdAndCreatedAtGreaterThan(patientId, createdAt);
		if (orders != null && !orders.isEmpty()) {
			List<Long> orderIds = orders.parallelStream().map(Order::getId).collect(Collectors.toList());
			List<String> skus = orderItems.parallelStream().map(OrderItem::getSku).distinct()
					.collect(Collectors.toList());
			List<OrderItem> items = orderItemService.findByOrderIds(orderIds, skus);
			if (items != null && !items.isEmpty()) {
				Set<String> excessiveOrderedSku = new HashSet<>();
				Map<String, Integer> map = new HashMap<>();
				for (OrderItem orderItem : items) {
					if (StringUtils.isNotBlank(orderItem.getPackType())) {
						int count = 0;
						if (map.containsKey(orderItem.getSku())) {
							count = map.get(orderItem.getSku());
							if (Medicine.PACK_TYPE_TAB.ALL.contains(orderItem.getPackType())
									&& orderItem.getPerPackQty() > 0) {
								count += orderItem.getQuantity() * orderItem.getPerPackQty();
							} else {
								count += orderItem.getQuantity();
							}
						} else {
							if (Medicine.PACK_TYPE_TAB.ALL.contains(orderItem.getPackType())
									&& orderItem.getPerPackQty() > 0) {
								count = orderItem.getQuantity() * orderItem.getPerPackQty();
							} else {
								count = orderItem.getQuantity();
							}
						}
						if (orderItem.getBulkOrderQuantity() > 0 && orderItem.getBulkOrderQuantity() <= count) {
							excessiveOrderedSku.add(orderItem.getSku());
						}
						map.put(orderItem.getSku(), count);
					}
				}
				return new ArrayList<>(excessiveOrderedSku);
			}

		}
		return new ArrayList<>();
	}

	@Override
	public Order updateOrderPrescriptionOrDoctorCallBack(long orderId, List<Long> prescriptionIds,
			boolean isDoctorCallBack, User user) throws Exception {
		if (orderId <= 0) {
			throw new BadRequestException("Order-id can not be null or empty!");
		}
		Order order = getOrder(orderId);
		if (order == null) {
			throw new BadRequestException("Order not found for order-id : " + orderId);
		}
		boolean isOrderAligibleForPrescriptionProcessed = false;
		if (prescriptionIds != null && !prescriptionIds.isEmpty()) {
			List<OrderPrescription> orderPrescriptions = orderPrescriptionService
					.updateOrderPrescriptions(order.getId(), order.getPatientId(), prescriptionIds);
			if (orderPrescriptions != null && !orderPrescriptions.isEmpty()) {
				order.setPrescriptionIds(orderPrescriptions != null && !orderPrescriptions.isEmpty()
						? orderPrescriptions.parallelStream().map(OrderPrescription::getId).collect(Collectors.toList())
						: null);
				isOrderAligibleForPrescriptionProcessed = true;
			}
		}
		if (isDoctorCallBack) {
			order.setDoctorCallback(isDoctorCallBack);
			isOrderAligibleForPrescriptionProcessed = true;
		}
		if (isOrderAligibleForPrescriptionProcessed) {
			order.setUpdatedBy(user.getFullName());
			saveOrder(order);
			boolean flag = salusService.markOrderPrescriptionProcessed(orderId);
			if (!flag) {
				throw new IllegalArgumentException("Order can not mark as precription-processed!");
			}
		}
		return order;
	}

	public Order updateOrderRevisitDay(long orderId, int days) throws Exception {
		Order order = orderRepository.findOne(orderId);
		order.setRevisitDay(days);
		return orderRepository.save(order);
	}

	@Override
	public Order saveOrder(Order order) {
		if (order == null) {
			throw new IllegalArgumentException("Invalid Order Object specified");
		}
		return orderRepository.save(order);
	}

	@Override
	public List<PaymentChannelData> fetchOrderPaymentChannels(long orderId) {
		if (orderId <= 0) {
			throw new IllegalArgumentException("Invalid order id specified");
		}
		Order order = orderRepository.findOne(orderId);
		if (order == null) {
			throw new OrderNotFoundException("No order found for order id : " + orderId);
		}
		// TODO: Check If COD Available
		// try {
		// long shippingAddressId = order.getShippingAddressId();
		// ShippingAddress shippingAddress =
		// shippingAddressRepository.findOne(shippingAddressId);
		// PlacePincode placePincode =
		// shippingService.getPlaceInformationByPincode(String.valueOf(shippingAddress.getPincode()));
		// } catch(Exception e) {
		// LOGGER.error("Error in getting shipping address : {}", e.getMessage());
		// }
		return paymentService.getPaymentChannels(true);
	}

	@Override
	public OrderPaymentGatewayObject verifyOrderPayment(long orderId,
			OrderPaymentGatewayVerifyRequest orderPaymentGatewayVerifyRequest, User user) throws Exception {
		if (orderId <= 0) {
			throw new IllegalArgumentException("Invalid order id specified");
		}
		Order order = getOrder(orderId);
		if (order == null) {
			throw new OrderNotFoundException("No order found for order id : " + orderId);
		}
		OrderPaymentGatewayObject orderPaymentGatewayObject = new OrderPaymentGatewayObject();
		try {
			PaymentGatewayData paymentGatewayData = paymentService.verifyOrderPayment(order.getId(),
					orderPaymentGatewayVerifyRequest);
			if (paymentGatewayData == null) {
				throw new IllegalArgumentException("Error occured while verifying payment gateway");
			}
			orderPaymentGatewayObject.setPaymentGateway(paymentGatewayData);
			salusService.markOrderPaymentConfirmed(orderId);
			order = getOrder(orderId);
		} catch (Exception e) {
			LOGGER.error("Payment Verifying Error : {}", e.getMessage());
			throw new PaymentGatewayException(e.getMessage());
		}
		orderPaymentGatewayObject.setOrder(order);
		return orderPaymentGatewayObject;
	}

	@SuppressWarnings("unchecked")
	@Override
	public OrderPaymentGatewayObject initiatePayment(Order order, String paymentMethod, String paymentSubMethod,
			boolean orderPlaced) throws Exception {
		if (order == null) {
			throw new IllegalArgumentException("Invalid order object specified");
		}
		if (orderPlaced) {
			List<PaymentGatewayData> paymentGatewayDataList = paymentService.confirmOrderPayment(order.getId(), false);
			float amountPaid = 0;
			if (paymentGatewayDataList != null && !paymentGatewayDataList.isEmpty()) {
				for (PaymentGatewayData paymentGatewayData : paymentGatewayDataList) {
					amountPaid += paymentGatewayData.getAmount();
				}
				float paidDiff = amountPaid - order.getCustomerAmountToPay();
				paidDiff = paidDiff >= 0 ? paidDiff : -paidDiff;
				if (paidDiff < 1) {
					salusService.markOrderPaymentConfirmed(order.getId());
					throw new IllegalArgumentException("Order payment cannot be initiated as payment already done");
				}
			}
		}
		OrderPaymentGatewayObject orderPaymentGatewayObject = new OrderPaymentGatewayObject();
		Error error = null;
		try {
			OrderPaymentInitiateRequest orderPaymentInitiateRequest = new OrderPaymentInitiateRequest();
			orderPaymentInitiateRequest.setOrderId(order.getId());
			orderPaymentInitiateRequest.setPaymentMethod(paymentMethod);
			orderPaymentInitiateRequest.setPaymentSubMethod(paymentSubMethod);
			orderPaymentInitiateRequest.setCustomerId(order.getCustomerId());
			orderPaymentInitiateRequest.setAmount(order.getCustomerAmountToPay());
			if(order.getCategory().equals(Order.CATEGORY.LAB)) {
				orderPaymentInitiateRequest.setCouponCashbackEligibleAmount(order.getCustomerAmountToPay());
			}
			PaymentGatewayData paymentGatewayData = paymentService.initiateOrderPayment(orderPaymentInitiateRequest);
			if (paymentGatewayData != null && paymentGatewayData.getOrderId() == order.getId()) {
				orderPaymentGatewayObject.setPaymentGateway(paymentGatewayData);
				Map<String, Object> additionPaymentGatewayData = paymentGatewayData.getAdditionalData();
				if (additionPaymentGatewayData != null && !additionPaymentGatewayData.isEmpty()) {
					order.setRedeemedCarePoints(additionPaymentGatewayData.containsKey("redeemed_care_points")
							&& additionPaymentGatewayData.get("redeemed_care_points") != null
									? Integer.parseInt(
											String.valueOf(additionPaymentGatewayData.get("redeemed_care_points")))
									: 0);
					order.setRedeemedCash(additionPaymentGatewayData.containsKey("redeemed_cash")
							&& additionPaymentGatewayData.get("redeemed_cash") != null
									? Float.parseFloat(String.valueOf(additionPaymentGatewayData.get("redeemed_cash")))
									: 0);
					order.setRedeemedCouponCashback(additionPaymentGatewayData.containsKey("redeemed_coupon_cashback")
							&& additionPaymentGatewayData.get("redeemed_coupon_cashback") != null
							? Float.parseFloat(String.valueOf(additionPaymentGatewayData.get("redeemed_coupon_cashback")))
							: 0);
					order.calculateCouponCashback();
					order.setTotalSalePrice(order.calculateTotalPrice());
				}
				order.setPaymentMethod(paymentGatewayData.getMethod());
				order.setPaymentSubMethod(paymentGatewayData.getSubMethod());
				if (Order.ORDER_TYPE.COD.equalsIgnoreCase(order.getOrderType())) {
					order.setPodAmount(paymentGatewayData.getAmount());
					order.setGatewayAmount(0);
					if (OrderStatus.STATE_NEW.PAYMENT_PENDING.equalsIgnoreCase(order.getStatus())
							|| OrderStatus.STATE_NEW.PAYMENT_FAILED.equalsIgnoreCase(order.getStatus())) {
						order.setState(OrderState.NEW);
						order.setStatus(OrderStatus.STATE_NEW.NEW);
					}
				} else {
					order.setGatewayAmount(paymentGatewayData.getAmount());
				}
				order = saveOrder(order);
			}
		} catch (Exception e) {
			LOGGER.debug("Error while initiating payment : {} {}", order.getId(), e);
			error = new Error();
			error.setMessage(e.getMessage());
			try {
				Response<Error> errRes = new ObjectMapper().readValue(e.getMessage(), Response.class);
				error = (Error) errRes.populateErrorUsingJson(Error.class);
			} catch (Exception ex) {
				e.printStackTrace();
			}
		}
		orderPaymentGatewayObject.setOrder(order);
		orderPaymentGatewayObject.setError(error);
		return orderPaymentGatewayObject;
	}
	
	public void updateProductStock(Order order, Map<String, Integer> map) {

		if (Order.DELIVERY_OPTION.URGENT.equalsIgnoreCase(order.getDeliveryOption()) || Order.SERVICE_TYPE.LF_ASSURED.equalsIgnoreCase(order.getServiceType())) {
			// 1. Update express_in_flight_stock in inventory.product_stock
			if (map != null && map.isEmpty()) {
				productStockService.updateExpressInventory((long) order.getFacilityCode(), map);
			}
		} else {
			/*
			 * We are stop increasing in flight stock. 1. Update in_flight_stock &
			 * express_in_flight_stock in inventory.product_stock
			 * if (map != null && !map.isEmpty()) {
			 * //productStockService.updateInFlightInventory((long) order.getFacilityCode(),
			 * map); }
			 */
		}
	}
	

	@Override
	public Order updateAppointmentDetails(long orderId, Map<String, Object> updateMap, User user) throws Exception {
		if (orderId <= 0 || updateMap == null || updateMap.isEmpty() || user == null) {
			throw new IllegalArgumentException("Invalid order id specified");
		}
		Timestamp appointmentDate =  updateMap.get("appointment_date") != null ?
				new Timestamp(Long.parseLong(String.valueOf(updateMap.get("appointment_date")))) : null;
		String appointmentSlot = updateMap.get("appointment_slot") != null ?
				String.valueOf(updateMap.get("appointment_slot")) : null;
		long appointmentSlotId = updateMap.get("appointment_slot_id") != null ?
				Long.parseLong(String.valueOf(updateMap.get("appointment_slot_id"))) : 0;
		long userId = updateMap.get("user_id") != null ?
				Long.parseLong(String.valueOf(updateMap.get("user_id"))) : 0;
		Order order = getOrder(orderId);
		if (order == null) {
			throw new OrderNotFoundException("No order found for order id : " + orderId);
		}
		if (!LabOrderState.VALID_RESCHEDULING_STATES.contains(order.getState())
				&& !LabOrderStatus.VALID_RESCEDULING_STATUS.contains(order.getState())) {
			throw new BadRequestException("Appointment can not be rescheduled due to order state: " + order.getState() + " and status: " + order.getState());
		}

		Appointment appointment = userService.updateAppointment(order.getAppointmentId(),
				new RescheduleAppointment(appointmentSlotId, userId, user));
		order.setAppointmentDate(appointmentDate);
		order.setAppointmentSlot(appointmentSlot);
		order.setAppointmentSlotId(appointmentSlotId);
		order.setAppointmentId(appointment.getId());
		order.setAppointment(appointment);
		if (appointment.getStatus().equalsIgnoreCase(LabOrderStatus.STATE_PROCESSING.ASSIGNED) 
				&& !LabOrderStatus.STATE_PROCESSING.ASSIGNED.equalsIgnoreCase(order.getStatus())) {
			OrderStateStatusRequest orderStateStatusRequest = new OrderStateStatusRequest();
			orderStateStatusRequest.setState(LabOrderState.PROCESSING);
			orderStateStatusRequest.setUser(user);
			orderStateStatusRequest.setStatus(LabOrderStatus.STATE_PROCESSING.ASSIGNED);
			boolean flag = salusService.updateLabOrderStateStatus(orderId, orderStateStatusRequest);
			if (!flag) {
				//Info: revert appointment status form assign to in_process
				userService.updateAppointment(order.getAppointmentId(),
						new RescheduleAppointment(appointmentSlotId, 0, user));
			}
		}
		return orderRepository.save(order);
	}

	@Override
	public Boolean updateReportDeliveryOption(long orderId, Order order, Map<String, Object> map) {
		if (order == null && orderId > 0) {
			order = orderRepository.findById(orderId);
		}
		if (order == null) {
			throw new BadRequestException("Invalid order id!");
		}
		if (!Order.CATEGORY.LAB.equalsIgnoreCase(order.getCategory())) {
			throw new BadRequestException("Order category is not Lab");
		}
		if (map == null || map.isEmpty()) {
			throw new BadRequestException("Invalid param provided");
		}
		
		Boolean isReportHardCopyRequired = map.get("is_report_hard_copy_required") != null ? (Boolean) map.get("is_report_hard_copy_required") : null;
		if (isReportHardCopyRequired == null){
			throw new IllegalArgumentException("Invalid key provided for ReportDeliveryOption");
		}
		if (!order.isReportHardCopyRequired() && isReportHardCopyRequired.booleanValue()) {
			try {
				String pincode = null;
				if (order.getShippingAddress() == null) {
					ShippingAddress shippingAddress = shippingAddressRepository.findTopByOrderId(order.getId());
					if(map.get("email") != null || map.get("mobile") != null) {
						if (map.get("email") != null && StringUtils.isNotBlank(String.valueOf(map.get("email")))) {
							shippingAddress.setEmail(String.valueOf(map.get("email")));	
						}
						if ( map.get("mobile") != null && StringUtils.isNotBlank(String.valueOf(map.get("mobile")))) {
							shippingAddress.setEmail(String.valueOf(map.get("mobile")));	
						}
						orderShippingAddressService.save(shippingAddress);	
					}
					pincode = String.valueOf(shippingAddress.getPincode());
				} else {
					pincode = String.valueOf(order.getShippingAddress().getPincode());
				}
				if (StringUtils.isBlank(pincode)) {
					throw new BadRequestException("Pincode not found for order-id : " + order.getId());
				}
				PlacePincode placePincode = shippingService.getPlaceInformationByPincode(pincode);
				if (placePincode != null) {
					if (placePincode.getIsReportHardCopyAvailable() == null || !placePincode.getIsReportHardCopyAvailable()) {
						throw new BadRequestException("We do not serve hard copy of the report at this pincode : " + pincode);
					}
					order.setReportHardCopyRequired(isReportHardCopyRequired);
					order.setReportDeliveryCharge(placePincode.getReportDeliveryCharge());
					order.setTotalSalePrice(order.getTotalSalePrice() + (float) order.getReportDeliveryCharge());
				}
			} catch (Exception e) {
				LOGGER.error("Report Delivery option change update with order-id " + order.getId()
						+ " failed due to : " + e.getMessage(), e);
				throw e;
			}
		} else if (!isReportHardCopyRequired){
			order.setReportHardCopyRequired(isReportHardCopyRequired);
			order.setTotalSalePrice(order.getTotalSalePrice() - (float) order.getReportDeliveryCharge());
			order.setReportDeliveryCharge(0.0);
		}
		order = orderRepository.save(order);
		return true;
	}

	@Override
	public List<OrderPatientPrescription> getOrderPrescriptions(long orderId) {
		if (orderId <= 0) {
			throw new BadRequestException("Order-id is not valid");
		}
		Order order = orderRepository.findById(orderId);
		if (order == null) {
			throw new OrderNotFoundException("Order not found for order-id : " + orderId);
		}
		List<OrderItem> orderItems = orderItemService.findByOrderId(orderId);
		if (orderItems == null || orderItems.isEmpty()) {
			return new ArrayList<>();
		}
		List<Long> patientIds = orderItems.parallelStream().filter(oi -> oi.getPatientId() != null && oi.getPatientId() > 0).map(OrderItem::getPatientId).distinct().collect(Collectors.toList());
		return customerService.getPrescriptions(order.getCustomerId(), patientIds);
	}



	@SuppressWarnings("serial")
	@Override
	public Page<OrderSearchResponse> searchOrder(Long parentId, Long childOrderId, Long childFacilityCode, List<String> childOrderStatuses, Long deliveryDateFrom, Long deliveryDateTo,
			Long dispatchDateFrom, Long dispatchDateTo, Pageable pageable) {

		try {
			Specification<Order> specification = OrderSpecification.findAll();
			specification = Specifications.where(specification)
					.and(OrderSpecification.filterByParentIdNotNull());
			if (null != parentId) {
				specification = Specifications.where(specification)
						.and(OrderSpecification.filterByParentId(parentId));
			}
			if (null != childOrderId) {
				specification = Specifications.where(specification)
						.and(OrderSpecification.filterByChildOrderId(childOrderId));
			}
			if (null != childFacilityCode) {
				specification = Specifications.where(specification)
						.and(OrderSpecification.filterByChildFacilityCode(childFacilityCode));
			}
			if (childOrderStatuses != null && !childOrderStatuses.isEmpty()) {
				specification = Specifications.where(specification)
						.and(OrderSpecification.filterByChildStatuses(childOrderStatuses));
			}
			if (null != dispatchDateFrom && null != dispatchDateTo) {
				Timestamp ddf = new Timestamp(dispatchDateFrom);
				Timestamp ddt = new Timestamp(dispatchDateTo);
				specification = Specifications.where(specification)
						.and(OrderSpecification.filterByDispatchDateBetween(ddf, ddt));
			}
			if (null != deliveryDateFrom && null != deliveryDateTo) {
				Timestamp ddf = new Timestamp(deliveryDateFrom);
				Timestamp ddt = new Timestamp(deliveryDateTo);
				specification = Specifications.where(specification)
						.and(OrderSpecification.filterByDispatchDateBetween(ddf, ddt));
			}
			
			Page<Order> childOrderPage = orderRepository.findAll(specification, pageable);
			List<Order> childOrders = childOrderPage.getContent();
			List<Long> childOrderIds = new ArrayList<Long>();
			List<Long> parentOrderIds = new ArrayList<Long>();
			Map<Long, List<Order>> parentIdOrderMap = new HashMap<Long, List<Order>>();
			List<OrderSearchResponse> resultArray = new ArrayList<OrderSearchResponse>();
			for (Order order : childOrders) {
				childOrderIds.add(order.getId());
				parentOrderIds.add(order.getParentId());
				if (parentIdOrderMap.containsKey(order.getParentId())) {
					parentIdOrderMap.get(order.getParentId()).add(order);
				} else {
					parentIdOrderMap.put(order.getParentId(), new ArrayList<Order>() {
						{
							add(order);
						}
					});
				}
			}
			
			//List<ShippingAddress> shippingAddressChilds = shippingAddressRepository.findAllByOrderIdIn(childOrderIds);
			List<Shipment> childOrderShipments = shippingService.getAllShipmentsByOrderIdIn(childOrderIds);
			
			Map<Long, Shipment> childOrderShipmentsMap = new HashMap<Long, Shipment>();
			
			if (childOrderShipments != null && !childOrderShipments.isEmpty()) {
				for (Shipment shipment : childOrderShipments) {
					childOrderShipmentsMap.put(Long.parseLong(shipment.getReferenceNumber()), shipment);
				}
			}
			
			List<Order> parentsOrders = orderRepository.findAllByIdIn(parentOrderIds);
			
			Map<Long, Order> parentsOrdersMap = new HashMap<Long, Order>();
			
			for(Order order : parentsOrders) {
				parentsOrdersMap.put(order.getId(), order);
			}
			
			for(Long orderId : parentIdOrderMap.keySet()) {
				OrderSearchResponse orderSearchResponse = new OrderSearchResponse();
				orderSearchResponse.setParentId(orderId);
				for(Order childOrder : parentIdOrderMap.get(orderId)) {
					if(childOrderShipmentsMap.containsKey(childOrder.getId())) {
						Shipment childShipment = childOrderShipmentsMap.get(childOrder.getId());
						childOrder.setHubId(childShipment.getHubId());
						childOrder.setSpokeId(childShipment.getSpokeId());
						childOrder.setHubName(childShipment.getHubName());
						childOrder.setSpokeName(childShipment.getSpokeName());
						childOrder.setCourierProvider(childShipment.getCourierProvider());
						childOrder.setConsigneeCity(childShipment.getConsigneeCity());
						childOrder.setConsigneeState(childShipment.getConsigneeState());
						childOrder.setConsigneePincode(childShipment.getConsigneePincode());
					}
				}
				orderSearchResponse.setAssociateOrders(parentIdOrderMap.get(orderId));
				orderSearchResponse.setStatus(parentsOrdersMap.get(orderId).getStatus());
				resultArray.add(orderSearchResponse);
			}
			
			Page<OrderSearchResponse> result = new PageImpl<OrderSearchResponse>(resultArray, pageable, childOrderPage.getTotalElements());
			return result;
		} catch (Exception e) {
			throw e;
		}
	}
	
	public Order getChildOrder(long parentId, long childOrderId) {
		if(parentId > 0 && childOrderId > 0) {
			Order order = orderRepository.findByParentIdAndId(parentId, childOrderId);
			if(order == null) {
				throw new OrderNotFoundException("Child order for id - " + childOrderId + " and parent-id - " + parentId + " not found.");
			}
			return order;
		}
		throw new IllegalArgumentException("Not a valid input param");
	}
	
	@Override
	public Order autoAssignAppointmentOrder(long orderId) {
		if (orderId <= 0)
			throw new IllegalArgumentException("Invalid params provided");
		Order order = orderRepository.findById(orderId);
		if (order == null) {
			throw new NotFoundException("Order not found Exception");
		}
		if (order.getCategory().equalsIgnoreCase(Order.CATEGORY.MEDICINE)) {
			throw new BadRequestException("Order category Medicine");
		}
		Long appointmentId = order.getAppointmentId();
		if (appointmentId == null || appointmentId <= 0)
			throw new IllegalArgumentException("Invalid appointment Id");

		Appointment appointment = null;
		try {
			appointment = userService.autoAssignAppointment(appointmentId);
		} catch (Exception e) {
			throw e;
		}
		if (appointment == null) {
			throw new BadRequestException("Error in auto assignment to appointment");
		}
		OrderStateStatusRequest orderStateStatusRequest = new OrderStateStatusRequest();
		orderStateStatusRequest.setState(LabOrderState.PROCESSING);
		orderStateStatusRequest.setStatus(LabOrderStatus.STATE_PROCESSING.ASSIGNED);
		boolean flag = salusService.updateLabOrderStateStatus(orderId, orderStateStatusRequest);
		if (!flag) {
			User user =  new User();
			user.setFirstName("Revert");
			user.setLastName("System");
			user.setId(10001);
			//Info: revert appointment status form assign to in_process
			appointment = userService.updateAppointment(order.getAppointmentId(),
					new RescheduleAppointment(order.getAppointmentSlotId(), 0, user));
		}
		order.setAppointment(appointment);
		orderRepository.save(order);
		return order;
	}
	
	@Override
	public Boolean moveJitForOrder(User user, long orderId, List<UpdateOrderObject> updateOrderObjects) throws Exception {
		Order order = orderRepository.findOne(orderId);
		if (order == null) {
			throw new OrderNotFoundException("Order not found for id: " + orderId);
		}
		List<Order> result = new ArrayList<Order>();
		order.setOrderItems(orderItemService.findByOrderId(orderId));
		OrderPrice orderPrice = getOrderPrice(order);

		for (UpdateOrderObject updateOrderObject : updateOrderObjects) {
			if (updateOrderObject.getParentId() > 0) {
				//order = createChild(user, updateOrderObject, order, orderPrice);
				result.add(order);
				orderEventService.createOrderEvent(order, order.getId() + order.getUpdatedAt().toString(),
						OrderEvent.ORDER_NEW_VERIFIED, user);
			} else {
				result.add(updateOrder(user, updateOrderObject, order, false, orderPrice, false, true, false));
				orderEventService.updateOrderEvent(order, order.getId() + order.getUpdatedAt().toString(),
						OrderEvent.ORDER_SPLIT, user, 0, null, null);
			}
		}
		return true;
	}
	
	@SuppressWarnings("serial")
	@Override
	public List<Long> isEligibleForStatusChange(String courierType) {
		if (courierType == null) {
			throw new BadRequestException("Invalid courierType!");
		}
		List<String> statuses = new ArrayList<String>() {
			{
				add(OrderStatus.STATE_SHIPPED.DISPATCHED);
				add(OrderStatus.STATE_SHIPPED.RETRY);
				add(OrderStatus.STATE_SHIPPED.IN_TRANSIT);
				add(OrderStatus.STATE_SHIPPED.HANDOVER_TO_COURIER);
				add(OrderStatus.STATE_SHIPPED.OUT_FOR_DELIVERY);
				add(OrderStatus.STATE_SHIPPED.DISPATCHED_FROM_HUB);
				add(OrderStatus.STATE_SHIPPED.IN_SCAN_AT_HUB);
				add(OrderStatus.STATE_SHIPPED.REACHED_AT_HUB);
				add(OrderStatus.STATE_PACKED.INVOICED);
				add(OrderStatus.STATE_PACKED.NEW);
				add(OrderStatus.STATE_PACKED.MANIFESTED);
			}
		};
		List<String> state = new ArrayList<String>() {
			{
				add(OrderState.PACKED);
				add(OrderState.SHIPPED);
			}
		};
		List<Long> orderIds = new ArrayList<Long>();
		Timestamp currentDateTime = new Timestamp(System.currentTimeMillis());
		List<Order> orders = orderRepository.findByCourierTypeAndPromisedDeliveryDateBeforeAndStatusInAndStateIn(courierType,
				currentDateTime, statuses, state);
		if (orders != null && !orders.isEmpty()) {
			orders.forEach(order -> {
				orderIds.add(order.getId());
			});
		} else {
			throw new OrderNotFoundException("No active orders found");
		}
		return orderIds;
	}
	
	@Override
	public Map<String, Object> getOrdersDetailByCustomerId(long customerId) {
		Map<String, Object> orderDetails = new HashMap<>();
		List<Order> orders = orderRepository.findAllByCustomerId(customerId);
		Long totalOrderDelivered = 0L, totalOrderByApp = 0L;
		if (orders != null && !orders.isEmpty()) {
			List<String> sources = Arrays.asList("ANDROID", "IOS", "MWEB");
			totalOrderDelivered = orders.parallelStream().filter(o -> ("COMPLETE".equalsIgnoreCase(o.getState())
					&& "DELIVERED".equalsIgnoreCase(o.getStatus()))
					|| ("SHIPPED".equalsIgnoreCase(o.getState()) && "DELIVERED".equalsIgnoreCase(o.getStatus())))
					.count();
			totalOrderByApp = orders.parallelStream().filter(o -> sources.contains(o.getSource())).count();
		}
		orderDetails.put("total_order_by_app", totalOrderByApp);
		orderDetails.put("total_order_delivered", totalOrderDelivered);
		return orderDetails;
	}
	

	private void changeOrderPrice(Order order, UpdateOrderObject updateOrderObject, List<Order> associatedOrders) {
		if (updateOrderObject != null && order != null && updateOrderObject.getParentId()>0) {

			try {
				Order parentOrder = orderRepository.findById(updateOrderObject.getParentId());
				List<Order> orders = new ArrayList<>();
				orders.addAll(associatedOrders);
				orders.add(parentOrder);
				Order orderWithTotalPrice = getTotalPriceForAllOrders(orders);

				setPriceForOrder(order, orderWithTotalPrice, updateOrderObject);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				LOGGER.warn(e.getMessage());
			}
		}

	}
	
	private List<Order> setPriceForOrder(Order order, Order orderWithTotalPrice,
			UpdateOrderObject updateOrderObject) {

		List<Order> orders = orderRepository.findAllByParentId(updateOrderObject.getParentId());

		List<Order> finalPrice = new ArrayList<Order>();

		Order currentOrder = order;
		for (Order orderForPrice : orders) {
			if (orderForPrice.getExternalInvoiceAmount() <= 0) {
				if(orderWithTotalPrice.getTotalMrp()>0) {
					float ratio = currentOrder.getTotalMrp() / orderWithTotalPrice.getTotalMrp();
					order.setTotalDiscount(orderWithTotalPrice.getTotalDiscount() * ratio);
					order.setTotalMrp(orderWithTotalPrice.getTotalMrp() * ratio);
					order.setTotalSalePrice(orderWithTotalPrice.getTotalSalePrice() * ratio);
					order.setTotalPayableAmount(orderWithTotalPrice.getTotalPayableAmount() * ratio);
					order.setTotalTaxAmount(orderWithTotalPrice.getTotalTaxAmount() * ratio);
					order.setCouponDiscount(orderWithTotalPrice.getCouponDiscount() * ratio);
					order.setGatewayAmount(orderWithTotalPrice.getGatewayAmount() * ratio);
					order.setPodAmount(orderWithTotalPrice.getPodAmount() * ratio);
					order.setExternalInvoiceAmount(orderWithTotalPrice.getExternalInvoiceAmount() * ratio);
					order.setLineTotalAmount(orderWithTotalPrice.getLineTotalAmount() * ratio);
					order.setRedeemedCash(orderWithTotalPrice.getRedeemedCash() * ratio);
					order.setRedeemedCarePoints(Math.round(orderWithTotalPrice.getRedeemedCarePoints() * ratio));
					order.setShippingCharge(orderWithTotalPrice.getShippingCharge() * ratio);
					order.setDiscount(orderWithTotalPrice.getDiscount() * ratio);
					finalPrice.add(order);
				}
			
			}
		}
		return finalPrice;
	}

	public Order getTotalPriceForAllOrders(List<Order> orders) {
		if (orders != null) {
			float totalDiscount = (float) orders.parallelStream().mapToDouble(Order::getTotalDiscount).sum();
			float totalMrp = (float) orders.parallelStream().mapToDouble(Order::getTotalMrp).sum();
			float totalSalePrice = (float) orders.parallelStream().mapToDouble(Order::getTotalSalePrice).sum();
			float totalPayableAmount = (float) orders.parallelStream().mapToDouble(Order::getTotalPayableAmount).sum();
			float totalTaxAmount = (float) orders.parallelStream().mapToDouble(Order::getTotalTaxAmount).sum();
			float couponDiscount = (float) orders.parallelStream().mapToDouble(Order::getCouponDiscount).sum();
			double gatewayAmount = (float) orders.parallelStream().mapToDouble(Order::getGatewayAmount).sum();
			double podAmount = (float) orders.parallelStream().mapToDouble(Order::getPodAmount).sum();
			double externalInvoiceAmount = (float) orders.parallelStream().mapToDouble(Order::getExternalInvoiceAmount)
					.sum();
			float lineTotalAmount = (float) orders.parallelStream().mapToDouble(Order::getLineTotalAmount).sum();
			float redeemedCash = (float) orders.parallelStream().mapToDouble(Order::getRedeemedCash).sum();
			int redeemedCarePoints = orders.parallelStream().mapToInt(Order::getRedeemedCarePoints).sum();
			float shippingCharge = (float) orders.parallelStream().mapToDouble(Order::getShippingCharge).sum();
			float discount = (float) orders.parallelStream().mapToDouble(Order::getDiscount).sum();

			Order totalPriceForAllOrders = new Order();
			totalPriceForAllOrders.setTotalDiscount(totalDiscount);
			totalPriceForAllOrders.setTotalMrp(totalMrp);
			totalPriceForAllOrders.setTotalSalePrice(totalSalePrice);
			totalPriceForAllOrders.setTotalPayableAmount(totalPayableAmount);
			totalPriceForAllOrders.setTotalTaxAmount(totalTaxAmount);
			totalPriceForAllOrders.setCouponDiscount(couponDiscount);
			totalPriceForAllOrders.setGatewayAmount(gatewayAmount);
			totalPriceForAllOrders.setPodAmount(podAmount);
			totalPriceForAllOrders.setExternalInvoiceAmount(externalInvoiceAmount);
			totalPriceForAllOrders.setLineTotalAmount(lineTotalAmount);
			totalPriceForAllOrders.setRedeemedCash(redeemedCash);
			totalPriceForAllOrders.setRedeemedCarePoints(redeemedCarePoints);
			totalPriceForAllOrders.setShippingCharge(shippingCharge);
			totalPriceForAllOrders.setDiscount(discount);

			return totalPriceForAllOrders;
		}
		return null;
	}

	@Override
	public Order moveToFacility(long orderId, Long spokeId) {
		try {
			Order order = orderRepository.findById(orderId);
			order.setFacilityCode(Math.toIntExact(spokeId));
			orderRepository.save(order);
			return order;
		} catch (Exception e) {
			LOGGER.error("Unable to change facility" + e.getMessage());
			throw e;
		}
	}
	
	
	@Override
	public Order mergePickedOrders(User user, long customerId, List<Long> orderIds) {
		if (user == null || customerId <= 0 || CollectionUtils.isEmpty(orderIds)) {
			throw new IllegalArgumentException("Invalid user or customer id or order ids provided");
		}
		List<Order> orders = orderRepository.findAllByIdIn(orderIds);
		// Merge orders
		Order newOrder = mergePickedOrders(user, customerId, orderIds, orders);

		if (orders.size() == 1) {
			return orders.get(0);
		}
		//State Update for merged orders
		Map<Long, OrderStateStatusRequest> orderOrderStateStatusRequests = new HashMap<Long, OrderStateStatusRequest>();
		orders.forEach(o -> {
			OrderStateStatusRequest orderStateStatusRequest = new OrderStateStatusRequest();
			orderStateStatusRequest.setFacilityId(o.getFacilityCode());
			orderStateStatusRequest.setState(OrderState.COMPLETED);
			orderStateStatusRequest.setStatus(OrderStatus.STATE_COMPLETE.MERGED);
			orderStateStatusRequest.setMergeWithId(o.getMergeWithId());
			orderOrderStateStatusRequests.put(o.getId(), orderStateStatusRequest);
		});
		salusService.orderStatusChange(orderOrderStateStatusRequests);
		newOrder.setMergedOrders(orderRepository.findAllByIdIn(orderIds));
		return newOrder;
	}

	@Transactional
	public Order mergePickedOrders(User user, long customerId, List<Long> orderIds, List<Order> orders) {
		if (CollectionUtils.isEmpty(orders)) {
			throw new IllegalArgumentException("No order found with requested order ids for merge ");
		}
		// Step 1: Perform Validation
		validateOrdersForMerging(customerId, orders);
		if (orders.size() == 1) {
			OrderStateStatusRequest orderStateStatusRequest = new OrderStateStatusRequest();
			orderStateStatusRequest.setState(OrderState.PROCESSING);
			orderStateStatusRequest.setStatus(OrderStatus.STATE_PROCESSING.READY_FOR_PACKED);
			salusService.orderStatusChange(orders.get(0).getId(), orderStateStatusRequest);
			return orders.get(0);
		}
		// Step 2: Create new order
		Order newOrder = new Order();
		try {
			org.springframework.beans.BeanUtils.copyProperties(orders.get(0), newOrder);
		} catch (Exception e1) {
			LOGGER.error("Error in copy order properties ");
		}
		//Set order 0, So new order id can be genrated
		newOrder.setId(0);
		newOrder.setOrderNumber(null);
		newOrder.setDisplayOrderId(null);
		// Save New Order
		newOrder = orderRepository.save(newOrder);
		newOrder.setOrderNumber(String.valueOf(newOrder.getId()));
		newOrder.setDisplayOrderId(String.valueOf(newOrder.getId()));
		newOrder.setState(OrderState.PROCESSING);
		newOrder.setStatus(OrderStatus.STATE_PROCESSING.READY_FOR_PACKED);
		long orderId = newOrder.getId();

		// Step 3: Merge order items
		List<OrderItem> orderItems = orderItemService.getByOrderIds(orderIds);
		List<OrderItem> neworderItems = orderItems.stream().filter(orderItem -> orderItem.isActive())
				.collect(Collectors.groupingBy(OrderItem::getSku)).entrySet().stream()
				.map(e -> e.getValue().stream().reduce((a, b) -> {
					OrderItem orderItem = getCopiedNewOrderItem(orderId, a);
					orderItem.setQuantity(a.getQuantity() + b.getQuantity());
					orderItem.setOrderedQuantity(a.getQuantity() + b.getQuantity());
					return orderItem;
				})).map(e -> {
					return getCopiedNewOrderItem(orderId, e.get());
				}).collect(Collectors.toList());
		neworderItems = orderItemService.save(neworderItems);
		newOrder.setOrderItems(neworderItems);
		float totalMrp = 0;
		float totalSalePrice = 0;
		for (OrderItem orderItem : neworderItems) {
			float quantity = orderItem.getQuantity() + ((float) orderItem.getLooseQuantity()
					/ (float) (orderItem.getPerPackQty() != 0 ? orderItem.getPerPackQty() : 1));
			totalMrp += orderItem.getMrp() * quantity;
			totalSalePrice += orderItem.getMrp() * quantity * (100 - orderItem.getDiscount()) / 100;
		}
		newOrder.setTotalMrp(totalMrp);
		newOrder.setTotalSalePrice(totalSalePrice);
		newOrder.setDiscount(newOrder.getTotalMrp() - newOrder.getTotalSalePrice());
		//TODO Assumeing that No Gateway Amount Will be there for B2B ORDER and zero coupon discount
		newOrder.setCouponDiscount(0);
		//TODO Total Discount considered to be discount + coupon-discount but here considering zero coupon discount  
		newOrder.setTotalDiscount(newOrder.getDiscount());
		newOrder.setGatewayAmount(0);
		newOrder.setPodAmount(newOrder.getTotalSalePrice() - ((float) newOrder.getGatewayAmount()
				+ newOrder.getRedeemedCash() + newOrder.getRedeemedCarePoints()));
		ShippingAddress shippingAddress = orderShippingAddressService.findByOrderId(orders.get(0).getId());
		if (shippingAddress != null) {
			ShippingAddress newShippingAddress = new ShippingAddress();
			try {
				org.springframework.beans.BeanUtils.copyProperties(shippingAddress, newShippingAddress);
			} catch (Exception e1) {
				LOGGER.error("Error in copy order properties ");
			}
			newShippingAddress.setOrderId(orderId);
			newShippingAddress.setId(0);
			newOrder.setShippingAddress(shippingAddressRepository.save(newShippingAddress));
		}
		orders.forEach(order -> {
			order.setMergeWithId(orderId);
		});
		orderRepository.save(orders);
		orderRepository.save(newOrder);
		// Step 4: Create Event of ORDER_PROCESSING_READY_FOR_PACKED
		orderEventService.orderEvent(newOrder, String.valueOf(orderId), OrderEvent.ORDER_PROCESSING_READY_FOR_PACKED,
				user);
		return newOrder;
	}

	private void validateOrdersForMerging(long customerId, List<Order> orders) {
		orders.forEach(order -> {
			StringBuilder errorMessage = new StringBuilder();
			if (order.getCustomerId() != customerId) {
				errorMessage.append("Can't merge order with different customer. Here Requested Customer id: ")
						.append(customerId).append(" and order customer id: ").append(order.getCustomerId());
				throw new IllegalArgumentException(errorMessage.toString());
			}
			if (!OrderState.PROCESSING.equalsIgnoreCase(order.getState())
					|| !OrderStatus.STATE_PROCESSING.PICKED.equalsIgnoreCase(order.getStatus())) {
				errorMessage.append("Can't merge order due to order : ").append(order.getId())
						.append("  in not mergable state. Current state & status are : ").append(order.getState())
						.append(" & ").append(order.getStatus());
				throw new IllegalArgumentException(errorMessage.toString());
			}
			if (order.getMergeWithId() > 0) {
				errorMessage.append("You Can't merge order due to order : ").append(order.getId())
						.append(" is already merged with order id : ").append(order.getMergeWithId())
						.append(", Current state & status are : ").append(order.getState()).append(" & ")
						.append(order.getStatus());
				throw new IllegalArgumentException(errorMessage.toString());
			}
		});
	}

	private OrderItem getCopiedNewOrderItem(long orderId, OrderItem e) {
		OrderItem orderItem = new OrderItem();
		try {
			org.springframework.beans.BeanUtils.copyProperties(e, orderItem);
		} catch (Exception e1) {
			LOGGER.error("Error in copy order item properties ");
		}
		orderItem.setId(0);
		orderItem.setOrderId(orderId);
		return orderItem;
	}

	
	@Override
	public Order addBasicOrder(User user, String referenceId, String source) {
		Order order = new Order();
		order.setComment(referenceId);
		order.setSource(source);
		order.setPatientId(Order.DEFAULT_VALUES.DEFAULT_PATIENT_ID);
		order.setCustomerId(Order.DEFAULT_VALUES.DEFAULT_CSTOMER_ID);
		order.setState(OrderState.NEW);
		order.setStatus(OrderStatus.STATE_NEW.NEW);
		order.setOrderType(Order.ORDER_TYPE.COD);
		order.setSource(Order.SOURCE.POS);
		return orderRepository.save(order);
	}
	
	@Autowired
	private RedisTemplate<String, String> redisTemplate;

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private OrderShippingAddressService orderShippingAddressService;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private OrderPrescriptionService orderPrescriptionService;

	@Autowired
	private PaymentService paymentService;

	@Autowired
	private SalusService salusService;

	@Autowired
	private OrderItemService orderItemService;

	@Autowired
	private ShippingAddressRepository shippingAddressRepository;

	@Autowired
	private WalletService walletService;

	@Autowired
	private ReasonRepository reasonRepository;

	private static final Logger LOGGER = LoggerFactory.getLogger(OrderServiceImpl.class);

	@Autowired
	private OrderEventService orderEventService;

	@Autowired
	private TempOrderSyncRepository tempOrderSyncRepository;

	@Autowired
	private PatientMedicineService patientMedicineService;

	@SuppressWarnings("rawtypes")
	@Autowired
	private MicroserviceClient<Response> microserviceClient;

	@Autowired
	private ProductStockService productStockService;

	@Autowired
	private ShippingService shippingService;

	@Autowired
	private CartService cartService;

	@Autowired
	private PrescriptionService prescriptionService;
	
	@Autowired
	private UserService userService;
	
	@Autowired
	private CustomerService customerService;

	@Autowired
	private AccountClientFacilityService accountClientFacilityService;

	@Autowired 
	private CatalogService catalogService;
}
