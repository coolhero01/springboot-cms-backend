package com.oneclicktech.spring.controller;

import static java.util.stream.Collectors.joining;

import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.oneclicktech.spring.domain.Constants;
import com.oneclicktech.spring.mapper.EWTMapper;
import com.oneclicktech.spring.mapper.ShopOrderMapper;
import com.oneclicktech.spring.service.CronJobService;
import com.oneclicktech.spring.service.EmailService;
import com.oneclicktech.spring.service.IssuanceService;
import com.oneclicktech.spring.service.OnlineOrderService;
import com.oneclicktech.spring.service.RestGenericService;
import com.oneclicktech.spring.service.RestTemplateService;
import com.oneclicktech.spring.service.SyncD365Service;
import com.oneclicktech.spring.util.DateUtil;
import com.oneclicktech.spring.util.NumberUtil;
import com.oneclicktech.spring.util.PCDataUtil;
import com.oneclicktech.spring.util.PassEncryptUtil;
import com.oneclicktech.spring.util.ShopifyUtil;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/pc/upay")
public class OnlineUPayController {

	private static final Logger logger = Logger.getLogger("OnlineUPayController");

	@Autowired
	RestTemplateService restTemplateService;

	@Autowired
	RestGenericService restGenericService;

	@Autowired
	IssuanceService issuanceService;

	@Value("${pc.site.referrer1}")
	String pcSiteReferrer1;

	@Value("${pc.site.referrer2}")
	String pcSiteReferrer2;

	@Value("${ub.upay.site.referrer}")
	String upaySiteReferrer;

	@Value("${pc.upay.redirect-url}")
	String pcUPayRedirectUrl;

	@Value("${pc.upay.aes-key}")
	String pcUPayAESKey;

	@Value("${pc.upay.uuid}")
	String pcUPayBillerUUID;

	@Value("${pc.upay.post-api.url}")
	String pcUPayPostApiUrl;

	@Value("${cms.app.host-url}")
	String cmsAppHostUrl;

	@Autowired
	OnlineOrderService onlineOrderService;

	@Autowired
	EmailService emailService;

	@Autowired
	SyncD365Service syncD365Service;

	@Autowired
	CronJobService cronJobService;

	@Autowired
	ShopOrderMapper shopOrderMapper;

	@Autowired
	EWTMapper ewtMapper;

	@PostConstruct
	public void init() {
		logger.info("** OnlineUPayController >> OnlineUPayController >> [INIT]");
	}

	@GetMapping("/getAccessToken")
	public RedirectView getAccessToken(HttpServletRequest request) throws Throwable {
		logger.info("** OnlineUPayController >> getAccessToken >> [START]");
		logger.info("** OnlineUPayController >> request: " + request);

		String uri = request.getRequestURI();

		String queryString = request.getQueryString();
		String url = request.getRequestURL().toString();
		logger.info("** OnlineUPayController >> getAccessToken >> uri: " + uri);
		logger.info("** OnlineUPayController >> getAccessToken >> queryString: " + queryString);
		logger.info("** OnlineUPayController >> getAccessToken >> url: " + url);

		logger.info("** OnlineUPayController >> getAccessToken >> [END]");
		return new RedirectView("http://localhost:8080/pc-cms/pc-upay-post");
	}

	@GetMapping("/getPaymentDetailsORIG")
	public RedirectView getPaymentDetails_ORIG(@RequestParam("orderId") String orderIdStr, HttpServletRequest request) {
		logger.info("** OnlineUPayController >> getPaymentDetails >> [START]");
		// http://localhost:8080/pc/upay/getPaymentDetails?orderName=PC12345
		logger.info("** OnlineUPayController >> orderId: " + orderIdStr);
		String shopReferrer = request.getHeader("referer");
		if (Constants.TEST_ONLY) {
			shopReferrer = "test";
			pcSiteReferrer1 = "test";
		}

		String redirectUrl = "";
		if (StringUtils.isNotBlank(shopReferrer)
				&& (shopReferrer.startsWith(pcSiteReferrer1) || shopReferrer.startsWith(pcSiteReferrer2))) {
			// CORRECT Site Referrer
			try {
				Long orderId = Long.valueOf(orderIdStr);
				Map<String, Object> orderMap = onlineOrderService.getOneOrderByID(orderId);
				logger.info("** OnlineUPayController >> orderMap: " + orderMap);

				if (MapUtils.isNotEmpty(orderMap)) {

					String orderName = (String) orderMap.get("name");
					String soCustomerNo = ShopifyUtil.getSOCustomerNoByAddress(orderMap, null);

					Map<String, Object> customerMap = (Map<String, Object>) orderMap.get("customer");
					String customerId = String.valueOf(NumberUtil.getLongValue(customerMap, "id"));
					String customerName = new StringBuilder((String) customerMap.get("first_name")).append(" ")
							.append((String) customerMap.get("last_name")).toString();

					String email = (String) customerMap.get("email");
					String contactNo = (String) customerMap.get("phone");
					String totalOrderPrice = (String) orderMap.get("total_price");
					String currency = (String) orderMap.get("currency");

					String transactId = new StringBuilder(orderName).append(customerId).toString();

					Map<String, String> requestParams = new LinkedHashMap<>();
					requestParams.put("orderId", String.valueOf(orderId));
					requestParams.put("orderName", orderName);
					requestParams.put("customerId", customerId);
					requestParams.put("customerName", customerName);
					requestParams.put("email", email);
					requestParams.put("contactNo", contactNo);
					requestParams.put("totalOrderPrice", totalOrderPrice);
					requestParams.put("currency", currency);
					requestParams.put("transactDate", DateUtil.getDateNowInPattern("MM/dd/yyy HH:mm:ss"));
					requestParams.put("transactId", transactId);

					Gson gson = new Gson();
					String bodyRequest = gson.toJson(requestParams, Map.class);

					logger.info("** OnlineUPayController >> bodyRequest: " + bodyRequest);

					redirectUrl = requestParams.keySet().stream()
							.map(key -> key + "=" + ShopifyUtil.encodeValue(requestParams.get(key)))
							.collect(joining("&", "http://localhost:4200/pc-upay-post?", ""));

					logger.info("** OnlineUPayController >> redirectUrl: " + redirectUrl);

				}

			} catch (Exception e) {
				logger.log(Level.SEVERE, e.getMessage(), e);

			} catch (Throwable t) {
				logger.log(Level.SEVERE, t.getMessage(), t);
			}

		} else {
			// INVALID - Redirect to Invalid/Error PAGE

		}

		return new RedirectView(redirectUrl);
	}

	@GetMapping("/getPaymentDetails")
	public RedirectView getPaymentDetails(@RequestParam("orderId") String orderIdStr, HttpServletRequest request) {
		logger.info("** OnlineUPayController >> getPaymentDetails >> [START]");
		// http://localhost:8080/pc/upay/getPaymentDetails?orderName=PC12345
		logger.info("** OnlineUPayController >> orderId: " + orderIdStr);
		String shopReferrer = request.getHeader("referer");
		if (Constants.TEST_ONLY) {
			shopReferrer = "https://order.potatocorner.com";
		}
		logger.info("** OnlineUPayController >> shopReferrer: " + shopReferrer);

		String redirectUrl = "";

		try {
			Long orderId = Long.valueOf(orderIdStr);
			Map<String, Object> orderMap = onlineOrderService.getOneOrderByID(orderId);
			orderMap.put("orderId", orderId);
			logger.info("** OnlineUPayController >> orderMap: " + orderMap);

			if (MapUtils.isNotEmpty(orderMap)) {

				String orderName = (String) orderMap.get("name");
				String payStatus = StringUtils.trim((String) orderMap.get("financial_status"));
				String soCustomerNo = ShopifyUtil.getSOCustomerNoByAddress(orderMap, null);
				boolean isValidAcct = syncD365Service.isValidCustomerAcct(soCustomerNo);
				logger.info("** OnlineUPayController >> getPaymentDetails >> orderName: " + orderName);
				logger.info("** OnlineUPayController >> getPaymentDetails >> payStatus: " + payStatus);
				logger.info("** OnlineUPayController >> getPaymentDetails >> soCustomerNo: " + soCustomerNo);
				logger.info("** OnlineUPayController >> getPaymentDetails >> isValidAcct: " + isValidAcct);

				if (Constants.TEST_ONLY) {
					isValidAcct = true;
				}

				if (!isValidAcct) {
					return new RedirectView("https://order.potatocorner.com/pages/invalid-customer-account");
				}

				if (StringUtils.isNotBlank(payStatus) && (payStatus.equalsIgnoreCase(Constants.STATUS_PENDING)
						|| payStatus.equalsIgnoreCase(Constants.STATUS_PARTIALLY_PAID))) {
					
					// Check for STAGGERED Payment
					// ********************************************
					boolean hasStaggeredPayment = false;
					Map<String, Object> staggeredPayMap = issuanceService.getNextPaymentSched(orderName);
					if (MapUtils.isNotEmpty(staggeredPayMap)) {
						hasStaggeredPayment = true;
						orderMap.put("total_price", String.valueOf(staggeredPayMap.get("amountToPay")));
					}

					// Check for EWT Order
					// ********************************************
					boolean isEWTOrder = false;
					HashMap<String, Object> ewtSearchMap = new HashMap<String, Object>();
					ewtSearchMap.put("orderName", orderName);
					List<Map<String, Object>> ewtOrderList = ewtMapper.getOrderEWTList(ewtSearchMap);
					if (CollectionUtils.isNotEmpty(ewtOrderList)) {
						isEWTOrder = true;
						Map<String, Object> ewtOrderMap = ewtOrderList.get(0);
						double orderWithDiscount = NumberUtil.getDoubleValue(ewtOrderMap, "orderWithDiscount");
						double shipWithDiscount = NumberUtil.getDoubleValue(ewtOrderMap, "shipWithDiscount");
						double ewtTotalPrice = (orderWithDiscount + shipWithDiscount);
						orderMap.put("total_price", String.valueOf(ewtTotalPrice));
					}

					logger.info("** OnlineUPayController >> getPaymentDetails >> isEWTOrder: " + isEWTOrder);
					logger.info("** OnlineUPayController >> getPaymentDetails >> hasStaggeredPayment: " + hasStaggeredPayment);
			
					// PROCEED with UPAY Payment
					// *********************************************
					Gson gson = new GsonBuilder().setPrettyPrinting().create();

					Map<String, Object> uPayReqMap = null;
					if (payStatus.equalsIgnoreCase("pending")) {
						uPayReqMap = PCDataUtil.buildUPayLinkMap(orderMap, pcUPayRedirectUrl);
					} else {
						if (hasStaggeredPayment) {
							orderMap.put("total_price", String.valueOf(staggeredPayMap.get("amountToPay")));
						} else {
							// "partially_paid"
							List<Map<String, Object>> transactions = onlineOrderService.getOrderTransactions(orderId);
							String balanceAmt = PCDataUtil.getTotalBalance(transactions);
							orderMap.put("total_price", balanceAmt);
						}
						uPayReqMap = PCDataUtil.buildUPayLinkMap(orderMap, pcUPayRedirectUrl);
					}
					logger.info("** OnlineUPayController >> getPaymentDetails >> isEWTOrder: " + isEWTOrder);

					String jsonRequest = gson.toJson(uPayReqMap, Map.class);
					logger.info("** OnlineUPayController >> getPaymentDetails >> jsonRequest: " + jsonRequest);

					byte[] IV = PassEncryptUtil.getRandomNonceOrIV(16);
					byte[] keyBytes = PassEncryptUtil.hexStringToByteArray(pcUPayAESKey);

					SecretKey pcSecretKey = new SecretKeySpec(keyBytes, "AES");

					byte[] jsonCipherByte = PassEncryptUtil.encrypt(jsonRequest.getBytes(), pcSecretKey, IV);
					String jsonCipherRequest = Base64.getEncoder().encodeToString(jsonCipherByte);

					logger.info(
							"** OnlineUPayController >> getPaymentDetails >> jsonCipherRequest: " + jsonCipherRequest);

					String finalPostUrl = PCDataUtil.buildUPayPostLinkRequest(pcUPayPostApiUrl, pcUPayBillerUUID,
							jsonCipherRequest);

					logger.info("** OnlineUPayController >> getPaymentDetails >> finalPostUrl: " + finalPostUrl);
					if (StringUtils.isNotBlank(finalPostUrl)) {
						redirectUrl = finalPostUrl;
					}

				} else {
					// VALIDATE show order is already 'PAID/VOID/REFUND'
					redirectUrl = "https://order.potatocorner.com/pages/invalid-order-payment-status";
				}

			}

		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
		} catch (Throwable t) {
			logger.log(Level.SEVERE, t.getMessage(), t);
		}

		return new RedirectView(redirectUrl);
	}

	@SuppressWarnings("unchecked")
	@PostMapping("/processPayment")
	public ResponseEntity<Map<String, Object>> processPayment(@RequestBody Map<String, Object> paramBody,
			HttpServletRequest request) {

		HttpStatus httpStat = HttpStatus.OK;
		logger.info("** OnlineUPayController >> processPayment >> [START]");
		logger.info("** OnlineUPayController >> paramBody: " + paramBody);
		String siteReferrer = request.getHeader("referer");
		logger.info("** OnlineUPayController >> siteReferrer: " + siteReferrer);

		Map<String, Object> errorMap = new HashMap<String, Object>();
		ResponseEntity<Map<String, Object>> resEntity = null;
		String resStatus = (String) paramBody.get("status");
		Map<String, Object> payData = (Map<String, Object>) paramBody.get("paydata");
		String orderName = "";
		if (MapUtils.isNotEmpty(payData) && payData.containsKey("orderName")) {
			orderName = (String) payData.get("orderName");
		}

		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String reqJsonTxt = gson.toJson(paramBody);
		emailService.sendEmail(null, Constants.MAIL_RECIPIENTS, "UB - UPAY Autopost Order " + orderName + " [REQUEST] ",
				reqJsonTxt, null, null, null);

		if (resStatus.equals(Constants.HTTP_STATUS_OK) && MapUtils.isNotEmpty(payData)
				&& payData.containsKey("orderId")) {

			Map<String, Object> processPayMap = new LinkedHashMap<>();

			try {
				processPayMap.putAll(payData);
				Long orderId = Long.valueOf((String) payData.get("orderId"));
				Map<String, Object> orderMap = onlineOrderService.getOneOrderByID(orderId);
				if (MapUtils.isNotEmpty(orderMap)) {

					try {
						// For UPay RE-TRIGGER Support
						// *********************************************
						String requestUrl = new StringBuilder(cmsAppHostUrl)
								.append("/springboot-cms-backend/pc/webhook/resetObjectHook").toString();
						Map<String, Object> resetMap = new HashMap<>();
						resetMap.put("orderName", orderName);
						restGenericService.sendPostRequest(requestUrl, resetMap);
					} catch (Throwable t) {
						logger.log(Level.SEVERE, t.getMessage(), t);
					}

					String onlineOrderPrice = (String) orderMap.get("total_price");
					String uPayOrderPrice = ((String) processPayMap.get("totalOrderPrice")).replaceAll(",", "");

					// STAGGERED PAYMENT
					// ***********************************************************
					boolean hasStaggeredPayment = false;
					Map<String, Object> staggeredPayMap = issuanceService.getNextPaymentSched(orderName);
					if (MapUtils.isNotEmpty(staggeredPayMap)) {
						hasStaggeredPayment = true;
						uPayOrderPrice = String.valueOf(staggeredPayMap.get("amountToPay"));
					}

					// EWT PAYMENT
					// ***********************************************************
					boolean isEWTOrder = false;
					HashMap<String, Object> ewtSearchMap = new HashMap<String, Object>();
					ewtSearchMap.put("orderName", orderName);
					List<Map<String, Object>> ewtOrderList = ewtMapper.getOrderEWTList(ewtSearchMap);
					if (CollectionUtils.isNotEmpty(ewtOrderList)) {
						// EWT Should tag the amount same with the OOS Order Amount
						// to be able to mark it as PAID
						isEWTOrder = true;
						uPayOrderPrice = new String(onlineOrderPrice);
					}

					logger.info("** OnlineUPayController >> processPayment >> orderName: " + orderName);
					logger.info("** OnlineUPayController >> processPayment >> onlineOrderPrice: " + onlineOrderPrice);
					logger.info("** OnlineUPayController >> processPayment >> uPayOrderPrice: " + uPayOrderPrice);
					logger.info("** OnlineUPayController >> processPayment >> isEWTOrder: " + isEWTOrder);
					logger.info(
							"** OnlineUPayController >> processPayment >> hasStaggeredPayment: " + hasStaggeredPayment);

					Map<String, Object> retTransactMap = new LinkedHashMap<>();
					double dUPayPrice = Double.parseDouble(uPayOrderPrice);
					double dOnlinePrice = Double.parseDouble(onlineOrderPrice);
					if (dUPayPrice >= dOnlinePrice) {
						processPayMap.put("totalOrderPrice", onlineOrderPrice);
					} else {
						// Possible Partial Payment
						processPayMap.put("totalOrderPrice", uPayOrderPrice);
					}

					retTransactMap = onlineOrderService.processPayment(processPayMap);
					logger.info("** OnlineUPayController >> processPayment >> retTransactMap: " + retTransactMap);
					if (MapUtils.isNotEmpty(retTransactMap) && retTransactMap.containsKey("transaction")) {

						String paymentMode = StringUtils.trimToEmpty((String) processPayMap.get("paymentMode"));
						String resJsonTxt = gson.toJson(retTransactMap);

						Map<String, Object> transactMap = (Map<String, Object>) retTransactMap.get("transaction");
						Map<String, Object> newTransactMap = PCDataUtil.getTransactionWithLimit(transactMap);

						Map<String, Object> returnMap = new LinkedHashMap<>();
						returnMap.put("statusCode", Constants.HTTP_STATUS_OK);
						returnMap.put("transaction", newTransactMap);
						resEntity = new ResponseEntity<Map<String, Object>>(returnMap, HttpStatus.OK);

						try {
							StringBuilder newTags = new StringBuilder();
							if (StringUtils.isNotBlank(paymentMode)) {
								newTags.append(Constants.MODE_OF_PAYMENT_TAG).append(paymentMode);
								newTags.append(",").append(Constants.BANK_TAG_UB);
								onlineOrderService.addOrderTagToCurrent(orderMap, newTags.toString());
							}

							emailService.sendEmail(null, Constants.MAIL_RECIPIENTS,
									"UB - UPAY Autopost Order " + orderName + " [RESPONSE] ", resJsonTxt, null, null,
									null);

						} catch (Exception e) {
							logger.info("** OnlineUPayController >> processPayment >> ERROR: ");
							logger.log(Level.SEVERE, e.getMessage(), e);
						} catch (Throwable t) {
							logger.info("** OnlineUPayController >> processPayment >> ERROR: ");
							logger.log(Level.SEVERE, t.getMessage(), t);
						}

					}

				}

			} catch (Throwable t) {
				logger.info("** OnlineUPayController >> processPayment >> ERROR: ");
				logger.log(Level.SEVERE, t.getMessage(), t);
				errorMap.put("ERROR_MSG", t.getMessage());
				resEntity = new ResponseEntity<Map<String, Object>>(errorMap, HttpStatus.BAD_REQUEST);
			}

		} else {
			errorMap.put("ERROR_MSG", "Invalid request.");
			resEntity = new ResponseEntity<Map<String, Object>>(errorMap, HttpStatus.BAD_REQUEST);
		}

		logger.info("** OnlineUPayController >> resEntity: " + resEntity);
		logger.info("** OnlineUPayController >> processPayment >> [END]");
		return resEntity;
	}

	@SuppressWarnings("unchecked")
	@PostMapping("/validateBillsPayment")
	public ResponseEntity<Map<String, Object>> validateBillsPayment(@RequestBody Map<String, Object> paramBody,
			HttpServletRequest request) {
		HttpStatus httpStat = HttpStatus.OK;
		ResponseEntity<Map<String, Object>> resEntity = new ResponseEntity<Map<String, Object>>(httpStat);
		logger.info("** OnlineUPayController >> validateBillsPayment >> [START]");
		logger.info("** OnlineUPayController >> paramBody: " + paramBody);

		String resStatus = (String) paramBody.get("status");
		Map<String, Object> payData = (Map<String, Object>) paramBody.get("paydata");
		String orderName = "";
		if (MapUtils.isNotEmpty(payData) && payData.containsKey("orderName")) {
			orderName = (String) payData.get("orderName");
		}

		Map<String, Object> errorMap = new HashMap<String, Object>();
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String reqJsonTxt = gson.toJson(paramBody);
		emailService.sendEmail(null, Constants.MAIL_RECIPIENTS,
				"UB - ValidatePayment Order " + orderName + " [REQUEST] ", reqJsonTxt, null, null, null);

		if (resStatus.equals(Constants.HTTP_STATUS_OK) && MapUtils.isNotEmpty(payData)
				&& payData.containsKey("orderName")) {

			try {
				Map<String, Object> orderMap = onlineOrderService.getOneOrderByName(orderName);
				if (MapUtils.isNotEmpty(orderMap)) {
					// VALID - Order Exist
					String payStatus = StringUtils.trim((String) orderMap.get("financial_status"));
					if (payStatus.equals(Constants.STATUS_PENDING)
							|| payStatus.equals(Constants.STATUS_PARTIALLY_PAID)) {
						// VALID - Pending / Partially Paid
						Map<String, Object> processPayMap = new LinkedHashMap<>();
						processPayMap.putAll(payData);
						String onlineOrderPrice = (String) orderMap.get("total_price");
						String totalAmountPaid = ((String) processPayMap.get("totalAmountPaid")).replaceAll(",", "");
						logger.info("** OnlineUPayController >> validateBillsPayment >> orderName: " + orderName);
						logger.info("** OnlineUPayController >> validateBillsPayment >> onlineOrderPrice: "
								+ onlineOrderPrice);
						logger.info("** OnlineUPayController >> validateBillsPayment >> totalAmountPaid: "
								+ totalAmountPaid);

						double dBillsPayPrice = Double.parseDouble(totalAmountPaid);
						double dOnlinePrice = Double.parseDouble(onlineOrderPrice);
						if (dBillsPayPrice > dOnlinePrice) {
							// INVALID Over Payment ??
							errorMap.put("STATUS_MSG", "ERROR: Over payment is not allowed");
							resEntity = new ResponseEntity<Map<String, Object>>(errorMap, HttpStatus.BAD_REQUEST);
						} else {
							// VALID - Exact / Partial Payment
							errorMap.put("STATUS_MSG", "SUCCESS: Payment is valid");
							resEntity = new ResponseEntity<Map<String, Object>>(errorMap, HttpStatus.OK);
						}
					} else {
						// INVALID - PAID/VOIDED/CANCELED already
						errorMap.put("STATUS_MSG",
								"ERROR: Invalid - current order status: " + StringUtils.upperCase(payStatus));
						resEntity = new ResponseEntity<Map<String, Object>>(errorMap, HttpStatus.BAD_REQUEST);
					}

				} else {
					// INVALID - Order does not exist
					errorMap.put("STATUS_MSG", "ERROR: Order does not exist");
					resEntity = new ResponseEntity<Map<String, Object>>(errorMap, HttpStatus.NOT_FOUND);
				}
			} catch (Throwable t) {
				logger.info("** OnlineUPayController >> validateBillsPayment >> ERROR: ");
				logger.log(Level.SEVERE, t.getMessage(), t);
				errorMap.put("STATUS_MSG", t.getMessage());
				resEntity = new ResponseEntity<Map<String, Object>>(errorMap, HttpStatus.BAD_REQUEST);
			}
		} else {
			errorMap.put("STATUS_MSG", "ERROR: Invalid API request format");
			resEntity = new ResponseEntity<Map<String, Object>>(errorMap, HttpStatus.BAD_REQUEST);
		}

		emailService.sendEmail(null, Constants.MAIL_RECIPIENTS,
				"UB - ValidatePayment Order " + orderName + " [RESPONSE] ", resEntity.toString(), null, null, null);

		logger.info("** OnlineUPayController >> validateBillsPayment >> [END]");
		return resEntity;
	}

	@SuppressWarnings("unchecked")
	@PostMapping("/processBillsPayment")
	public ResponseEntity<Map<String, Object>> processBillsPayment(@RequestBody Map<String, Object> paramBody,
			HttpServletRequest request) {

		HttpStatus httpStat = HttpStatus.OK;
		logger.info("** OnlineUPayController >> processBillsPayment >> [START]");
		logger.info("** OnlineUPayController >> paramBody: " + paramBody);
		String siteReferrer = request.getHeader("referer");
		logger.info("** OnlineUPayController >> siteReferrer: " + siteReferrer);

		Map<String, Object> errorMap = new HashMap<String, Object>();
		ResponseEntity<Map<String, Object>> resEntity = new ResponseEntity<Map<String, Object>>(httpStat);
		String resStatus = (String) paramBody.get("status");
		Map<String, Object> payData = (Map<String, Object>) paramBody.get("paydata");
		String orderName = "";
		if (MapUtils.isNotEmpty(payData) && payData.containsKey("orderName")) {
			orderName = (String) payData.get("orderName");
		}

		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String reqJsonTxt = gson.toJson(paramBody);
		emailService.sendEmail(null, Constants.MAIL_RECIPIENTS,
				"UB - BillsPayment Autopost Order " + orderName + " [REQUEST] ", reqJsonTxt, null, null, null);

		if (resStatus.equals(Constants.HTTP_STATUS_OK) && MapUtils.isNotEmpty(payData)
				&& payData.containsKey("orderName")) {

			Map<String, Object> processPayMap = new LinkedHashMap<>();

			try {
				processPayMap.putAll(payData);
				Map<String, Object> orderMap = onlineOrderService.getOneOrderByName(orderName);
				if (MapUtils.isNotEmpty(orderMap)) {

					try {
						// For UPay RE-TRIGGER Support
						// *********************************************
						String requestUrl = new StringBuilder(cmsAppHostUrl)
								.append("/springboot-cms-backend/pc/webhook/resetObjectHook").toString();
						Map<String, Object> resetMap = new HashMap<>();
						resetMap.put("orderName", orderName);
						restGenericService.sendPostRequest(requestUrl, resetMap);
					} catch (Throwable t) {
						logger.log(Level.SEVERE, t.getMessage(), t);
					}

					Long orderId = ShopifyUtil.getOrderId(orderMap);
					processPayMap.put("orderId", String.valueOf(orderId));

					String onlineOrderPrice = (String) orderMap.get("total_price");
					String totalAmountPaid = ((String) processPayMap.get("totalAmountPaid")).replaceAll(",", "");

					// STAGGERED PAYMENT
					// ***********************************************************
					boolean hasStaggeredPayment = false;
					Map<String, Object> staggeredPayMap = issuanceService.getNextPaymentSched(orderName);
					if (MapUtils.isNotEmpty(staggeredPayMap)) {
						hasStaggeredPayment = true;
						totalAmountPaid = String.valueOf(staggeredPayMap.get("amountToPay"));
					}

					// EWT PAYMENT
					// ***********************************************************
					boolean isEWTOrder = false;
					HashMap<String, Object> ewtSearchMap = new HashMap<String, Object>();
					ewtSearchMap.put("orderName", orderName);
					List<Map<String, Object>> ewtOrderList = ewtMapper.getOrderEWTList(ewtSearchMap);
					if (CollectionUtils.isNotEmpty(ewtOrderList)) {
						// EWT Should tag the amount same with the OOS Order Amount
						// to be able to mark it as PAID
						isEWTOrder = true;
						totalAmountPaid = new String(onlineOrderPrice);
					}

					logger.info("** OnlineUPayController >> processBillsPayment >> orderName: " + orderName);
					logger.info("** OnlineUPayController >> processBillsPayment >> onlineOrderPrice: " + onlineOrderPrice);
					logger.info("** OnlineUPayController >> processBillsPayment >> totalAmountPaid: " + totalAmountPaid);
					logger.info("** OnlineUPayController >> processBillsPayment >> isEWTOrder: " + isEWTOrder);
					logger.info("** OnlineUPayController >> processBillsPayment >> hasStaggeredPayment: " + hasStaggeredPayment);

					Map<String, Object> retTransactMap = new LinkedHashMap<>();
					double dBillsPayPrice = Double.parseDouble(totalAmountPaid);
					double dOnlinePrice = Double.parseDouble(onlineOrderPrice);
					if (dBillsPayPrice >= dOnlinePrice) {
						processPayMap.put("totalOrderPrice", onlineOrderPrice);
					} else {
						// POSSIBLE Partial Payment
						processPayMap.put("totalOrderPrice", dBillsPayPrice);
					}

					retTransactMap = onlineOrderService.processBillsPayment(processPayMap);
					logger.info("** OnlineUPayController >> processBillsPayment >> retTransactMap: " + retTransactMap);
					if (MapUtils.isNotEmpty(retTransactMap) && retTransactMap.containsKey("transaction")) {

						String paymentMode = "BILLS-PAYMENT";
						String resJsonTxt = gson.toJson(retTransactMap);

						Map<String, Object> transactMap = (Map<String, Object>) retTransactMap.get("transaction");
						Map<String, Object> newTransactMap = PCDataUtil.getTransactionWithLimit(transactMap);

						String transactStatus = StringUtils.trimToEmpty((String) transactMap.get("status"));
						Map<String, Object> returnMap = new LinkedHashMap<>();

						if (transactStatus.equals(Constants.SUCCESS_TRANSACTION_STATUS)) {
							try {
								returnMap.put("statusCode", Constants.HTTP_STATUS_OK);
								StringBuilder newTags = new StringBuilder();
								if (StringUtils.isNotBlank(paymentMode)) {
									newTags.append(Constants.MODE_OF_PAYMENT_TAG).append(paymentMode);
									newTags.append(",").append(Constants.BANK_TAG_UB);
									onlineOrderService.addOrderTagToCurrent(orderMap, newTags.toString());
								}

							} catch (Exception e) {
								logger.info("** OnlineUPayController >> processBillsPayment >> ERROR: ");
								logger.log(Level.SEVERE, e.getMessage(), e);
							} catch (Throwable t) {
								logger.info("** OnlineUPayController >> processBillsPayment >> ERROR: ");
								logger.log(Level.SEVERE, t.getMessage(), t);
							}

						} else {
							returnMap.put("statusCode", HttpStatus.EXPECTATION_FAILED.value());
						}

						returnMap.put("transaction", newTransactMap);
						resEntity = new ResponseEntity<Map<String, Object>>(returnMap, HttpStatus.OK);
						emailService.sendEmail(null, Constants.MAIL_RECIPIENTS,
								"UB - BillsPayment Autopost Order " + orderName + " [RESPONSE] ", resJsonTxt, null,
								null, null);

					}

				} else {
					// Order DOES NOT Exist
					errorMap.put("ERROR_MSG", "Order does not exist");
					resEntity = new ResponseEntity<Map<String, Object>>(errorMap, HttpStatus.NOT_FOUND);
				}

			} catch (Throwable t) {
				logger.info("** OnlineUPayController >> processBillsPayment >> ERROR: ");
				logger.log(Level.SEVERE, t.getMessage(), t);
				errorMap.put("ERROR_MSG", t.getMessage());
				resEntity = new ResponseEntity<Map<String, Object>>(errorMap, HttpStatus.BAD_REQUEST);
			}

		} else {
			errorMap.put("ERROR_MSG", "Invalid request.");
			resEntity = new ResponseEntity<Map<String, Object>>(errorMap, HttpStatus.BAD_REQUEST);
		}

		logger.info("** OnlineUPayController >> processBillsPayment >> [END]");
		return resEntity;
	}

	@SuppressWarnings("unchecked")
	@GetMapping("/checkOrderStatus")
	public RedirectView checkOrderStatus(@RequestParam("orderId") String orderId, HttpServletRequest request) {

		HttpStatus httpStat = HttpStatus.OK;
		logger.info("** OnlineUPayController >> checkOrderStatus >> [START]");
		logger.info("** OnlineUPayController >> orderId: " + orderId);
		String siteReferrer = request.getHeader("referer");
		if (Constants.TEST_ONLY) {
			siteReferrer = "https://unionbank";
		}
		logger.info("** OnlineUPayController >> siteReferrer: " + siteReferrer);

		String redirectUrl = null;

		logger.info("** OnlineUPayController >> checkOrderStatus >> [END]");
		return new RedirectView(redirectUrl);
	}

	@SuppressWarnings("unchecked")
	@PostMapping("/processPayment_ORIG")
	public ResponseEntity<Map<String, Object>> processPayment_ORIG(@RequestBody Map<String, Object> paramBody,
			HttpServletRequest request) {

		HttpStatus httpStat = HttpStatus.OK;
		logger.info("** OnlineUPayController >> processPayment >> [START]");
		logger.info("** OnlineUPayController >> paramBody: " + paramBody);
		String siteReferrer = request.getHeader("referer");
		if (Constants.TEST_ONLY) {
			siteReferrer = "https://unionbank";
		}
		Map<String, Object> errorMap = new HashMap<String, Object>();
		ResponseEntity<Map<String, Object>> resEntity = null;
		if (StringUtils.isNotBlank(siteReferrer) && siteReferrer.startsWith(upaySiteReferrer)) {
			String resStatus = (String) paramBody.get("status");
			Map<String, Object> payData = (Map<String, Object>) paramBody.get("paydata");

			if (resStatus.equals(Constants.HTTP_STATUS_OK) && MapUtils.isNotEmpty(payData)) {
				Map<String, Object> processPayMap = new LinkedHashMap<>();

				try {
					processPayMap.putAll(payData);
					Map<String, Object> retTransactMap = onlineOrderService.processPayment(processPayMap);
					logger.info("** OnlineUPayController >> processPayment >> retTransactMap: " + retTransactMap);
					if (MapUtils.isNotEmpty(retTransactMap)
							&& ((String) retTransactMap.get("status")).equals("success")) {
						Map<String, Object> returnMap = new LinkedHashMap<>();
						returnMap.put("statusCode", Constants.HTTP_STATUS_OK);
						returnMap.put("transaction", retTransactMap.get("transaction"));
						resEntity = new ResponseEntity<Map<String, Object>>(returnMap, HttpStatus.OK);
					}

				} catch (Throwable t) {
					logger.info("** OnlineUPayController >> processPayment >>ERROR: ");
					logger.log(Level.SEVERE, t.getMessage(), t);

					errorMap.put("ERROR_MSG", t.getMessage());
					resEntity = new ResponseEntity<Map<String, Object>>(errorMap, HttpStatus.BAD_REQUEST);
				}

			}

		} else {
			errorMap.put("ERROR_MSG", "Unauthorized Access");
			resEntity = new ResponseEntity<Map<String, Object>>(errorMap, HttpStatus.UNAUTHORIZED);
		}

		logger.info("** OnlineUPayController >> resEntity: " + resEntity);
		logger.info("** OnlineUPayController >> processPayment >> [END]");
		return resEntity;
	}

}
