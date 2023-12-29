package com.oneclicktech.spring.controller;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
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

import com.oneclicktech.spring.domain.Constants;
import com.oneclicktech.spring.mapper.BankAPIMapper;
import com.oneclicktech.spring.mapper.EWTMapper;
import com.oneclicktech.spring.mapper.ShopOrderMapper;
import com.oneclicktech.spring.service.AuditLogService;
import com.oneclicktech.spring.service.BankAPIService;
import com.oneclicktech.spring.service.CronJobService;
import com.oneclicktech.spring.service.EmailService;
import com.oneclicktech.spring.service.IssuanceService;
import com.oneclicktech.spring.service.OnlineOrderService;
import com.oneclicktech.spring.service.QueryBuilderService;
import com.oneclicktech.spring.service.RestBDOService;
import com.oneclicktech.spring.service.RestGenericService;
import com.oneclicktech.spring.service.RestTemplateService;
import com.oneclicktech.spring.service.SyncD365Service;
import com.oneclicktech.spring.util.BDOTransactUtil;
import com.oneclicktech.spring.util.DateUtil;
import com.oneclicktech.spring.util.NumberUtil;
import com.oneclicktech.spring.util.PCDataUtil;
import com.oneclicktech.spring.util.ShopifyUtil;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/pc/bdo")
public class OnlineBDOController {

	private static final Logger logger = Logger.getLogger("OnlineBDOController");

	@Autowired
	RestTemplateService restTemplateService;

	@Autowired
	RestGenericService restGenericService;
	
	@Autowired
	QueryBuilderService queryBuilderService;
	
	@Autowired
	RestBDOService restBDOService;

	@Autowired
	AuditLogService auditLogService;

	@Value("${cms.app.host-url}")
	String cmsAppHostUrl;

	@Autowired
	OnlineOrderService onlineOrderService;

	@Autowired
	EmailService emailService;

	@Autowired
	SyncD365Service syncD365Service;
	
	@Autowired
	IssuanceService issuanceService;
	
	@Autowired
	CronJobService cronJobService;

	@Autowired
	ShopOrderMapper shopOrderMapper;

	@Autowired
	BankAPIMapper bankAPIMapper;
	
	@Autowired
	BankAPIService bankAPIService;
	
	@Autowired
	EWTMapper ewtMapper;
	
	@Value("${pc.bdo.trust.store}")
	Resource pcTrustStore;

	@Value("${pc.bdo.trust.store.password")
	String pcTrustStorePassword; 
	
	@Value("${pc.cms.environment}")
	String pcCMSEnvironment; 

	private List<Map<String, Object>> authorizeConfigs;

	private List<Map<String, Object>> payStatusConfigs;

	@PostConstruct
	public void init() {
		logger.info("** OnlineBDOController >> OnlineBDOController >> [INIT]");
		try {
			HashMap<String, Object> configMap = new HashMap<>();
			configMap.put("bankName", Constants.PAYEEE_BANK_BDO);
			configMap.put("envType", pcCMSEnvironment); 
			configMap.put("apiUsage", "mandatory");
			configMap.put("apiType", "AUTHORIZE");
			authorizeConfigs = bankAPIMapper.getBankAPIConfigs(configMap);

			configMap.put("apiType", "BILLS_PAY_STATUS");
			payStatusConfigs = bankAPIMapper.getBankAPIConfigs(configMap);
		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
		}
 
	}

	@GetMapping("/transactioncomplete")
	public RedirectView transactionComplete(HttpServletRequest request) {
		String redirectUrl = "https://order.potatocorner.com/pages/bdo-pay-transact-complete";
		return new RedirectView(redirectUrl);
	}

	@GetMapping("/getPaymentDetails")
	public RedirectView getPaymentDetails(@RequestParam("orderId") String orderIdStr, HttpServletRequest request) {
		logger.info("** OnlineBDOController >> getPaymentDetails >> [START]");

		logger.info("** OnlineBDOController >> orderId: " + orderIdStr);
		String shopReferrer = request.getHeader("referer");

		logger.info("** OnlineBDOController >> shopReferrer: " + shopReferrer);
		String redirectUrl = "";
		String requestUrl = "";
	
		try {
			Long orderId = Long.valueOf(orderIdStr);
			Map<String, Object> orderMap = onlineOrderService.getOneOrderByID(orderId);
			orderMap.put("orderId", orderId);
			logger.info("** OnlineBDOController >> getPaymentDetails >> orderMap: " + orderMap);

			if (MapUtils.isNotEmpty(orderMap)) {

				String orderName = (String) orderMap.get("name");
				String payStatus = StringUtils.trim((String) orderMap.get("financial_status"));
				String soCustomerNo = ShopifyUtil.getSOCustomerNoByAddress(orderMap, null);
				boolean isValidAcct = syncD365Service.isValidCustomerAcct(soCustomerNo);
 
				logger.info("** OnlineBDOController >> getPaymentDetails >>  orderName: " + orderName);
				logger.info("** OnlineBDOController >> getPaymentDetails >>  payStatus: " + payStatus);
				logger.info("** OnlineBDOController >> getPaymentDetails >>  soCustomerNo: " + soCustomerNo);
				logger.info("** OnlineBDOController >> getPaymentDetails >>  isValidAcct: " + isValidAcct);
				
				if (Constants.TEST_ONLY) {
					isValidAcct = true;
				}
				
				if (!isValidAcct) {
					return new RedirectView("https://order.potatocorner.com/pages/invalid-customer-account");
				}

				if (StringUtils.isNotBlank(payStatus)
						&& (payStatus.equalsIgnoreCase(Constants.STATUS_PENDING) 
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

					logger.info("** OnlineBDOController >> getPaymentDetails >> orderMap: " + orderMap);
					logger.info("** OnlineBDOController >> getPaymentDetails >> hasStaggeredPayment: " + hasStaggeredPayment);
					logger.info("** OnlineBDOController >> getPaymentDetails >> isEWTOrder: " + isEWTOrder);
			 		
					// PROCEED with BDO Payment
					// ********************************************* 
 					if (payStatus.equalsIgnoreCase(Constants.STATUS_PENDING)) {
 						requestUrl = BDOTransactUtil.buildAuthorizeLink(orderMap, authorizeConfigs);
					} else {
						if (hasStaggeredPayment) {
							orderMap.put("total_price", String.valueOf(staggeredPayMap.get("amountToPay")));
						} else {
							// "partially_paid"
							List<Map<String, Object>> transactions = onlineOrderService.getOrderTransactions(orderId);
							String balanceAmt = PCDataUtil.getTotalBalance(transactions);
							orderMap.put("total_price", balanceAmt);
					    }
						requestUrl = BDOTransactUtil.buildAuthorizeLink(orderMap, authorizeConfigs);
			 		}
  
					logger.info("** OnlineBDOController >> getPaymentDetails >> requestUrl: " + requestUrl);
				 	Map<String, Object> resultMap = restBDOService.sendGetAuthorizeRequest(requestUrl,
							new HashMap<>());
				 	 
					if (MapUtils.isNotEmpty(resultMap) 
							&& StringUtils.isNotBlank((String) resultMap.get("redirectUrl"))) {
						logger.info("** OnlineBDOController >> getPaymentDetails >> resultMap: " + resultMap);
						redirectUrl = BDOTransactUtil.parseResponseURL((String) resultMap.get("redirectUrl"));
						HashMap<String, Object> saveMap = new HashMap<>();
						saveMap.put("orderName", orderName);
						saveMap.put("apiType", "AUTHORIZE");
						saveMap.put("orderId", orderId);
						saveMap.put("status", "pending");
						saveMap.put("requestUrl", requestUrl);
						saveMap.put("responseUrl", redirectUrl);
						saveMap.put("internalTransactRefNo", BDOTransactUtil.getInternalTransactRefNoByURL(requestUrl));
						saveMap.put("channelRefNo",  BDOTransactUtil.getChannelRefNoByURL(requestUrl));
						boolean success = bankAPIService.saveBDOTransactLog(saveMap); 
						
						logger.info("** OnlineBDOController >> getPaymentDetails >> saveBDOTransactLog: " + success);
		 			} else {
						redirectUrl = "https://order.potatocorner.com/pages/invalid-payment-transaction";
					}

				} else {
					// VALIDATE show order is already 'PAID/VOID/REFUND'
					redirectUrl = "https://order.potatocorner.com/pages/invalid-order-payment-status";
				}

			} else {
				redirectUrl = "https://order.potatocorner.com/pages/invalid-order";
			}

		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
		} catch (Throwable t) {
			logger.log(Level.SEVERE, t.getMessage(), t);
		}

		return new RedirectView(redirectUrl);
	}

	@PostMapping("/checkPaymentStatusByToken")
	public ResponseEntity<Map<String, Object>> checkPaymentStatusByToken(@RequestBody Map<String, Object> paramBody) {
		logger.info("** OnlineBDOController >> checkPaymentStatusByToken >> [START]");
		HttpStatus httpStat = HttpStatus.OK;
		Map<String, Object> resultMap = new HashMap<>();
		cronJobService.runBDOCheckPaymenStatusRequest(paramBody);
		logger.info("** OnlineBDOController >> checkPaymentStatusByToken >> [END]");
		return new ResponseEntity<Map<String, Object>>(resultMap, httpStat);
	}
	
	@PostMapping("/checkPaymentStatus")
	public ResponseEntity<Map<String, Object>> checkPaymentStatus(@RequestBody Map<String, Object> paramBody) {

		HttpStatus httpStat = HttpStatus.OK;
		logger.info("** OnlineBDOController >> checkPaymentStatus >> [START]");
		logger.info("** OnlineBDOController >> paramBody: " + paramBody);
		String orderNameParam = (String) paramBody.get("orderName");
		HashMap<String, Object> paramMap = new HashMap<>();
		Map<String, Object> resultMap = new HashMap<>();
		Date dateInPH = DateUtil.getDateInManilaPH();
		String dateStr = DateUtil.getDateWithPattern(dateInPH, "MM/dd/yyy HH:mm a");

		if (StringUtils.isNotBlank(orderNameParam)) {
			paramMap.put("orderName", orderNameParam);
		} else {
			paramMap.put("payeeBank", Constants.PAYEEE_BANK_BDO);
			paramMap.put("financialStatus", Constants.STATUS_PENDING);
			paramMap.put("emptySO", "true");
		}

		List<Map<String, Object>> bdoOrders = shopOrderMapper.getShopOrderWithNoLines(paramMap);

		if (CollectionUtils.isNotEmpty(bdoOrders)) {

			String accessToken = auditLogService.getLatestToken(Constants.PAYEEE_BANK_BDO);

			StringBuilder sbOrders = new StringBuilder("Checking payment status for BDO Orders (").append(dateStr)
					.append(") ").append(System.lineSeparator());
			int orderCtr = 1;
			for (Map<String, Object> bdoOrder : bdoOrders) {
				String orderName = (String) bdoOrder.get("orderName");
				Long dbOrderId = NumberUtil.getLongValue(bdoOrder, "shopOrderId");
				logger.info("** OnlineBDOController >> checkPaymentStatus >> bdoOrder: " + orderName);

				try {

					/*
					 * Check payment status if PAID, process SO and remove from list
					 */
					sbOrders.append(orderCtr).append(") ").append(orderName);
					Map<String, Object> orderMap = onlineOrderService.getOneOrderByID(dbOrderId);
					String paymentLink = BDOTransactUtil.buildPaymentStatusLink(orderMap, bdoOrder,
							authorizeConfigs, payStatusConfigs);
					logger.info("** OnlineBDOController >> checkPaymentStatus >> paymentLink: " + paymentLink);

					Map<String, Object> requestMap = new HashMap<>();
					resultMap = restGenericService.sendGetRequest(paymentLink, accessToken, requestMap);
					logger.info("** OnlineBDOController >> checkPaymentStatus >> " + orderName + " | resultMap: "
							+ resultMap);
					
					if (MapUtils.isNotEmpty(resultMap) && resultMap.containsKey("context")
							&& resultMap.containsKey("payload")) {
						// sbOrders.append(" >> STATUS: ").append(resultMap.toString());
						Map<String, Object> payloadMap = (Map<String, Object>) resultMap.get("payload");
						Map<String, Object> contextMap = (Map<String, Object>) resultMap.get("context");

						if (MapUtils.isNotEmpty(contextMap) && MapUtils.isNotEmpty(payloadMap)) {
							Map<String, Object> processPayMap = new LinkedHashMap<>();

							String onlineOrderPrice = (String) orderMap.get("total_price");
							String payStatusMsg = (String) contextMap.get("status");
							String bankPayStatus = (String) payloadMap.get("paymentStatus");
							// BS = Biller Credited. Payment confirmed and biller credited.
							// PC = Payment Confirmed. Payment confirmed and the bill amount is fully
							// recovered.
							//
							 
							boolean isSuccessPayStatus = BDOTransactUtil.successPaymentStatus(bankPayStatus);
							
							if (payStatusMsg.equals("SUCCESS") && isSuccessPayStatus) {
								String debitAmtStr = String.valueOf(payloadMap.get("debitAmount"));
								double dDebitAmt = Double.parseDouble(debitAmtStr);
								double dOnlinePrice = Double.parseDouble(onlineOrderPrice);
								boolean hasPayment = true;
								if (dDebitAmt >= dOnlinePrice) {
									// Fully PAID
									processPayMap.put("totalOrderPrice", onlineOrderPrice);
								} else if (dDebitAmt > 0D) {
									// Possible PARTIAL Payment
									processPayMap.put("totalOrderPrice", dDebitAmt);
								} else {
									// NO PAYMENT
									hasPayment = false;
									logger.info("** OnlineBDOController >> checkPaymentStatus >> NO/Zero PAYMENT");
								}

								if (hasPayment) {
									Map<String, Object> retTransactMap = onlineOrderService
											.processPayment(processPayMap);
									logger.info("** OnlineBDOController >> checkPaymentStatus >> retTransactMap: "
											+ retTransactMap);
									if (MapUtils.isNotEmpty(retTransactMap)
											&& retTransactMap.containsKey("transaction")) {
										String newTags = new StringBuilder(Constants.MODE_OF_PAYMENT_TAG)
												.append("BDO Payment").toString();
										onlineOrderService.addOrderTagToCurrent(orderMap, newTags.toString());
									}
								}

							}

						}
					}

				} catch (Exception e) {
					logger.log(Level.SEVERE, e.getMessage(), e);
				} catch (Throwable t) {
					logger.log(Level.SEVERE, t.getMessage(), t);
				}

				sbOrders.append(System.lineSeparator());
				orderCtr++;
			}

			String[] mailRecipients = Constants.MAIL_RECIPIENTS;
			if (Constants.TEST_ONLY) {
				mailRecipients = new String[] { "matthew.poako@gmail.com" };
			}

			StringBuilder mailSubj = new StringBuilder("PC - Checking Payment Status for BDO Orders (").append(dateStr)
					.append(") ");
			boolean mailSent = emailService.sendEmail(null, mailRecipients, mailSubj.toString(), sbOrders.toString(),
					null, null, null);
		}

		logger.info("** OnlineBDOController >> checkPaymentStatus >> [END]");
		return new ResponseEntity<Map<String, Object>>(resultMap, httpStat);
	}

	@SuppressWarnings("unchecked")
	@PostMapping("/reloadConfigs")
	public ResponseEntity<Map<String, Object>> reloadConfigs(@RequestBody Map<String, Object> paramBody) {

		HttpStatus httpStat = HttpStatus.OK;
		logger.info("** OnlineBDOController >> reloadConfigs >> [START]");
		this.init();
		logger.info("** OnlineBDOController >> reloadConfigs >> [END]");
		return new ResponseEntity<Map<String, Object>>(httpStat);
	}

	public List<Map<String, Object>> getAuthorizeConfigs() {
		return authorizeConfigs;
	}

	public void setAuthorizeConfigs(List<Map<String, Object>> authorizeConfigs) {
		this.authorizeConfigs = authorizeConfigs;
	}

	public List<Map<String, Object>> getPayStatusConfigs() {
		return payStatusConfigs;
	}

	public void setPayStatusConfigs(List<Map<String, Object>> payStatusConfigs) {
		this.payStatusConfigs = payStatusConfigs;
	}

}
