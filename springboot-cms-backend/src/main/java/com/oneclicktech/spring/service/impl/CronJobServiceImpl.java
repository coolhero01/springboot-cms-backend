package com.oneclicktech.spring.service.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.persistence.NoResultException;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.oneclicktech.spring.domain.Constants;
import com.oneclicktech.spring.mapper.BankAPIMapper;
import com.oneclicktech.spring.mapper.ClientTokenMapper;
import com.oneclicktech.spring.mapper.CronAuditLogMapper;
import com.oneclicktech.spring.mapper.CronJobSchedMapper;
import com.oneclicktech.spring.mapper.CustomerMapper;
import com.oneclicktech.spring.mapper.EWTMapper;
import com.oneclicktech.spring.mapper.ShopOrderMapper;
import com.oneclicktech.spring.service.AuditLogService;
import com.oneclicktech.spring.service.CronJobService;
import com.oneclicktech.spring.service.EmailService;
import com.oneclicktech.spring.service.IssuanceService;
import com.oneclicktech.spring.service.OnlineCustomerService;
import com.oneclicktech.spring.service.OnlineOrderService;
import com.oneclicktech.spring.service.OnlineProductService;
import com.oneclicktech.spring.service.OnlineShopService;
import com.oneclicktech.spring.service.QueryBuilderService;
import com.oneclicktech.spring.service.RestBDOService;
import com.oneclicktech.spring.service.RestD365Service;
import com.oneclicktech.spring.service.RestGenericService;
import com.oneclicktech.spring.service.SyncD365Service;
import com.oneclicktech.spring.util.BDOTransactUtil;
import com.oneclicktech.spring.util.DateUtil;
import com.oneclicktech.spring.util.NumberUtil;
import com.oneclicktech.spring.util.ShopifyUtil;

@Service
public class CronJobServiceImpl implements CronJobService {

	private static final Logger logger = Logger.getLogger("CronJobServiceImpl");

	@Autowired
	CronAuditLogMapper cronAuditLogMapper;

	@Autowired
	CronJobSchedMapper cronJobSchedMapper;

	@Autowired
	OnlineOrderService onlineOrderService;

	@Autowired
	OnlineProductService onlineProductService;

	@Autowired
	OnlineCustomerService onlineCustomerService;

	@Autowired
	EmailService emailService;

	@Autowired
	AuditLogService auditLogService;

	@Autowired
	OnlineShopService onlineShopService;

	@Autowired
	SyncD365Service syncD365Service;

	@Autowired
	ShopOrderMapper shopOrderMapper;

	@Autowired
	CustomerMapper customerMapper;

	@Autowired
	ClientTokenMapper clientTokenMapper;

	@Autowired
	QueryBuilderService queryBuilderService;

	@Autowired
	RestD365Service restD365Service;

	@Autowired
	BankAPIMapper bankAPIMapper;

	@Autowired
	EWTMapper ewtMapper;

	@Autowired
	RestGenericService restGenericService;

	@Autowired
	IssuanceService issuanceService;

	@Value("${cms.app.host-url}")
	String cmsAppHostUrl;

	@Value("${pc.bdo.cms.oauth.token.url}")
	String bdoAuthTokenUrl;

	@Value("${pc.bdo.cms.oauth.grant-type}")
	String bdoAuthGrantType;

	@Value("${pc.bdo.cms.oauth.client-id}")
	String bdoAuthClientId;

	@Value("${pc.bdo.cms.oauth.client-secret}")
	String bdoAuthClientSecret;

	@Autowired
	RestBDOService restBDOService;

	List<Map<String, Object>> authorizeConfigs;

	List<Map<String, Object>> payStatusConfigs;

	@Value("${pc.cms.environment}")
	String pcCMSEnvironment;

	@PostConstruct
	public void init() {
		HashMap<String, Object> configMap = new HashMap<>();
		configMap.put("bankName", Constants.PAYEEE_BANK_BDO);
		configMap.put("envType", pcCMSEnvironment);
		configMap.put("apiUsage", "mandatory");
		configMap.put("apiType", "AUTHORIZE");
		authorizeConfigs = bankAPIMapper.getBankAPIConfigs(configMap);

		configMap.put("apiType", "BILLS_PAY_STATUS");
		payStatusConfigs = bankAPIMapper.getBankAPIConfigs(configMap);
	}

	@Override
	public List<Map<String, Object>> getCronAuditLogs(HashMap<String, Object> paramMap) {
		// TODO Auto-generated method stub
		return cronAuditLogMapper.getCronAuditLogs(paramMap);
	}

	@Override
	public Map<String, Object> processOrdersWithAutoPay(Map<String, Object> paramMap) {
		logger.info("*** CronJobServiceImpl >> processOrdersWithAutoPay >> [START]");
		HashMap<String, Object> searchMap = new HashMap<>();
		searchMap.putAll(paramMap);
		Date startDate = new Date();

		List<Map<String, Object>> custList = customerMapper.getAutoPayCustomers(searchMap);
		for (Map<String, Object> custMap : custList) {
			String eCustId = String.valueOf(custMap.get("eCustomerNo"));
			String custEmail = (String) custMap.get("email");
			String autoPaytag = (String) custMap.get("orderTag");
			logger.info("*** CronJobServiceImpl >> processOrdersWithAutoPay >> CustomerId: " + eCustId);
			logger.info("*** CronJobServiceImpl >> processOrdersWithAutoPay >> Email: " + custEmail);

			try {

				List<Map<String, Object>> orders = onlineOrderService.getOrdersByAutoPayCustomer(eCustId, "open");
				for (Map<String, Object> orderMap : orders) {

					String financeStatus = (String) orderMap.get("financial_status");

					if (financeStatus.equals(Constants.STATUS_PENDING)) {
						Map<String, Object> payMap = new HashMap<>();
						String orderName = (String) orderMap.get("name");
						String orderId = String.valueOf(NumberUtil.getLongValue(orderMap, "id"));
						payMap.put("orderId", orderId);
						payMap.put("currency", orderMap.get("currency"));
						payMap.put("totalOrderPrice", orderMap.get("current_total_price"));
						logger.info("*** CronJobServiceImpl >> processOrdersWithAutoPay >> orderName: " + orderName);
						logger.info(
								"*** CronJobServiceImpl >> processOrdersWithAutoPay >> payMap: " + payMap.toString());
						try {
							Thread.sleep(10000); // 5secs Sleep
							Map<String, Object> resultMap = onlineOrderService.processPayment(payMap);
							logger.info(
									"*** CronJobServiceImpl >> processOrdersWithAutoPay >> resultMap: " + resultMap);
							if (MapUtils.isNotEmpty(resultMap)) {
								// **********************************************
								// SUCCESS = Status is PAID
								// **********************************************
								if (StringUtils.isNotBlank(autoPaytag)) {
									onlineOrderService.addOrderTagToCurrent(orderMap, autoPaytag);
								}
							}
						} catch (Throwable t) {
							logger.log(Level.SEVERE, t.getMessage(), t);
						}

					}

				}
			} catch (Throwable t) {
				logger.log(Level.SEVERE, t.getMessage(), t);
			}

		}

		logger.info("*** CronJobServiceImpl >> processOrdersWithAutoPay >> [END]");
		return null;
	}

	@Override
	public List<Map<String, Object>> getCronJobSchedList(HashMap<String, Object> paramMap) {
		// TODO Auto-generated method stub
		List<Map<String, Object>> cronList = cronJobSchedMapper.getCronJobSchedList(paramMap);
		for (Map<String, Object> cronMap : cronList) {
			cronMap.put("startBtn", "START");
			cronMap.put("stopBtn", "STOP");
			cronMap.put("jobStatus", "STOPPED");
			cronMap.put("jobStatusColor", "text-red-600");
			cronMap.put("runTimeOnly", "text-red-600");
			cronMap.put("jobIsRunning", false);
		}

		return cronList;
	}

	@Override
	public Map<String, Object> saveOnlineOrderToDB(Map<String, Object> paramMap) {
		logger.info("*** CronJobServiceImpl >> saveOnlineOrderToDB >> [START]");
		try {

			logger.info("*** CronJobServiceImpl >> saveOnlineOrderToDB >> paramMap: " + paramMap);
			List<Map<String, Object>> orderList = onlineOrderService.getOrderList(paramMap);

			for (Map<String, Object> orderMap : orderList) {

				Long orderId = ShopifyUtil.getOrderId(orderMap);
				String soCustomerNo = (String) orderMap.get("soCustomerNo");
				String orderName = (String) orderMap.get("name");
				String financialStatus = (String) orderMap.get("financial_status");
				String fulfillStatus = (String) orderMap.get("fulfillment_status");
				String orderTags = (String) orderMap.get("tags");

				try {
					HashMap<String, Object> searchMap = new HashMap<>();
					searchMap.put("orderName", orderName);
					List<Map<String, Object>> dbOrders = shopOrderMapper.getShopOrderWithNoLines(searchMap);
					Map<String, Object> dbOrder = null;
					if (CollectionUtils.isNotEmpty(dbOrders)) {
						dbOrder = dbOrders.get(0);
						financialStatus = StringUtils.trimToEmpty((String) dbOrder.get("financialStatus"));
						fulfillStatus = StringUtils.trimToEmpty((String) dbOrder.get("fulfillmentStatus"));
					}

					boolean skipOrder = this.skipFulfilledOrders(fulfillStatus, financialStatus, soCustomerNo,
							orderTags);
					if (skipOrder)
						continue;

//				if (Constants.TEST_ONLY && !orderName.equals("POR1144"))  
//					continue;

					logger.info("*** CronJobServiceImpl >> saveOnlineOrderToDB >> orderName: " + orderName);
					logger.info("*** CronJobServiceImpl >> saveOnlineOrderToDB >> orderId: " + orderId);
					logger.info("*** CronJobServiceImpl >> saveOnlineOrderToDB >> soCustomerNo: " + soCustomerNo);

					boolean existOnDB = false;
					if (MapUtils.isNotEmpty(dbOrder)) {
						Long dbOrderId = NumberUtil.getLongValue(dbOrder, "shopOrderId");
						logger.info("*** CronJobServiceImpl >> saveOnlineOrderToDB >> dbOrderId: " + dbOrderId);
						logger.info("*** CronJobServiceImpl >> saveOnlineOrderToDB >> orderId: " + orderId);
						if (String.valueOf(orderId).equals(String.valueOf(dbOrderId))) {
							existOnDB = true;
						}
					}

					logger.info("*** CronJobServiceImpl >> saveOnlineOrderToDB >> existOnDB: " + existOnDB);

					HashMap<String, Object> dbOrderMap = new HashMap<>();
					dbOrderMap.putAll(ShopifyUtil.buildOrderDBMap(orderMap, dbOrder));

					int result = 0;
					logger.info("*** CronJobServiceImpl >> saveOnlineOrderToDB >> dbOrderMap: " + dbOrderMap);

					if (existOnDB) {
						result = shopOrderMapper.updateShopOrder(dbOrderMap);
						logger.info("*** CronJobServiceImpl >> saveOnlineOrderToDB >> DETAIL UPDATE :" + result);
					} else {
						result = shopOrderMapper.insertShopOrder(dbOrderMap);
						logger.info("*** CronJobServiceImpl >> saveOnlineOrderToDB >> DETAIL INSERT :" + result);
					}

					if (result != 0) {
						// SUCCESS
						List<Map<String, Object>> lineItems = (List<Map<String, Object>>) orderMap.get("line_items");
						for (Map<String, Object> itemLine : lineItems) {

							try {
								Long productId = NumberUtil.getLongValue(itemLine, "product_id");
								Map<String, Object> productMap = onlineProductService.getOneProduct(productId);

								HashMap<String, Object> lineDbMap = new HashMap<>();
								lineDbMap.putAll(ShopifyUtil.buildOrderLineDBMap(orderMap, itemLine, productMap));
								int lineResult = 0;
								if (existOnDB) {
									lineResult = shopOrderMapper.updateShopOrderLine(lineDbMap);
									logger.info("*** CronJobServiceImpl >> saveOnlineOrderToDB >> LINE UPDATE :"
											+ lineResult);
								} else {
									lineResult = shopOrderMapper.insertShopOrderLine(lineDbMap);
									logger.info("*** CronJobServiceImpl >> saveOnlineOrderToDB >> LINE INSERT :"
											+ lineResult);
								}
							} catch (Exception e) {
								logger.log(Level.SEVERE, e.getMessage(), e);
							}

						}

					}

				} catch (Exception e) {
					logger.info("*** CronJobServiceImpl >> saveOnlineOrderToDB >> orderMap: " + orderMap);
					logger.info("*** CronJobServiceImpl >> saveOnlineOrderToDB >> ERROR:");
					logger.log(Level.SEVERE, e.getMessage(), e);
				}

			}

		} catch (Exception e) {
			logger.info("*** CronJobServiceImpl >> saveOnlineOrderToDB >> ERROR:");
			logger.log(Level.SEVERE, e.getMessage(), e);
		} catch (Throwable t) {
			logger.info("*** CronJobServiceImpl >> saveOnlineOrderToDB >> ERROR:");
			logger.log(Level.SEVERE, t.getMessage(), t);
		}
		logger.info("*** CronJobServiceImpl >> saveOnlineOrderToDB >> [END]");
		return null;
	}

	private boolean skipFulfilledOrders(String fulfillStatus, String financialStatus, String soCustomerNo,
			String orderTags) {
		if (StringUtils.isNotBlank(fulfillStatus) && fulfillStatus.equals("fulfilled")
				&& StringUtils.isNotBlank(financialStatus) && financialStatus.equals("paid")) {
			return true;
		}

		if (StringUtils.isBlank(soCustomerNo))
			return true;

		if (StringUtils.isNotBlank(orderTags) && orderTags.contains("TEST_ONLY"))
			return true;

		return false;
	}

	@Override
	public Map<String, Object> syncOnlineOrderTagStatusByOrderDB(Map<String, Object> paramMap) {

		logger.info("** CronJobServiceImpl >> syncOnlineOrderTagStatusByOrderDB >> [START]");

		logger.info("** CronJobServiceImpl >> syncOnlineOrderTagStatusByOrderDB >> [END]");

		return null;
	}

	@Override
	public Map<String, Object> saveCronAuditLog(HashMap<String, Object> paramMap) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, Object> deleteCronAuditLog(HashMap<String, Object> paramMap) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, Object> syncD365StaticDataToDB(Map<String, Object> paramMap) {
		HashMap<String, Object> searchMap = new HashMap<>();
		searchMap.put("tokenType", "D365");
		logger.info("*** CronJobServiceImpl >> syncD365DataToDB >> [START]");
		try {
			logger.info("*** CronJobServiceImpl >> syncD365DataToDB >> PRODUCT SYNC >> [START] ");
			String productToken = restD365Service.getLatestD365Token();
			Map<String, Object> prodMap = new HashMap<>();
			prodMap.put("accessToken", productToken);

			logger.info("** CronJobServiceImpl >> syncProductDataToDB >> D365 Product to Local DB..... ");
			syncD365Service.syncProductDataToDB(new HashMap<>());
			logger.info("** CronJobServiceImpl >> syncProductDataToDB >> BACKUP Tables..... ");
			onlineProductService.backupProductDBTable(new HashMap<>());
			logger.info("** CronJobServiceImpl >> syncProductDataToDB >> CLEANSE Online & DB Products..... ");
			onlineProductService.cleansOnlineProductAndDB(new HashMap<>());
			logger.info("** CronJobServiceImpl >> syncProductDataToDB >> SYNC = Local DB to Online..... ");
			onlineShopService.syncLocalProductToOnline(new HashMap<>());

		} catch (Throwable t) {
			logger.info("*** CronJobServiceImpl >> syncD365DataToDB >> PRODUCT SYNC >> [ERROR] ");
			logger.info("*** CronJobServiceImpl >> syncD365DataToDB >> syncProductDataToDB >> ERROR: ");
			logger.log(Level.SEVERE, t.getMessage(), t);
		}

		logger.info("*** CronJobServiceImpl >> syncD365DataToDB >> PRODUCT SYNC >> [END] ");
		try {
			logger.info("*** CronJobServiceImpl >> syncD365DataToDB >> CUSTOMER SYNC >> [START] ");
			String customerToken = restD365Service.getLatestD365Token();
			Map<String, Object> custMap = new HashMap<>();
			custMap.put("accessToken", customerToken);
			logger.info("** CronJobServiceImpl >> syncCustomerDataToDB >> D365 Product to Local DB..... ");
			syncD365Service.syncCustomerDataToDB(new HashMap<>());

			logger.info("** CronJobServiceImpl >> syncCustomerDataToDB >> BACKUP Tables ..... ");
			onlineCustomerService.backupCustomerDBTable(new HashMap<>());

			logger.info("** CronJobServiceImpl >> syncCustomerDataToDB >> CLEANSE ..... ");
			onlineCustomerService.cleansOnlineCustomerAndDB(new HashMap<>());
			// DELETE Online Customer Address with OOS_INCLUDE=N
			onlineCustomerService.cleansOnlineCustomerAddress();

			Map<String, Object> custBody = new HashMap<>();
			logger.info("** CronJobServiceImpl >> syncCustomerDataToDB >> SYNC = Local DB to Online  ..... ");
			custBody.put("withEmail", "true");
			// paramBody.put("withPhone", "true");
			onlineShopService.syncLocalCustomerToOnlineByEmail(custBody);

		} catch (Throwable t) {
			logger.info("*** CronJobServiceImpl >> syncD365DataToDB >> CUSTOMER SYNC >> [ERROR] ");
			logger.info("*** CronJobServiceImpl >> syncD365DataToDB >> syncCustomerDataToDB >> ERROR: ");
			logger.log(Level.SEVERE, t.getMessage(), t);
		}
		logger.info("*** CronJobServiceImpl >> syncD365DataToDB >> CUSTOMER SYNC >> [END] ");
//

		try {
			String warehouseToken = restD365Service.getLatestD365Token();
			Map<String, Object> wareMap = new HashMap<>();
			wareMap.put("accessToken", warehouseToken);
			syncD365Service.syncWarehouseDataToDB(wareMap);
		} catch (Throwable t) {
			logger.info("*** CronJobServiceImpl >> syncD365DataToDB >> syncWarehouseDataToDB >> ERROR: ");
			logger.log(Level.SEVERE, t.getMessage(), t);
		}

		logger.info("*** CronJobServiceImpl >> syncD365DataToDB >> [END]");
		return null;
	}

	@Override
	public Map<String, Object> syncSalesOrderDataToDB(Map<String, Object> paramMap) {
		HashMap<String, Object> searchMap = new HashMap<>();
		searchMap.put("tokenType", "D365");
		logger.info("*** CronJobServiceImpl >> syncSalesOrderDataToDB >> [START]");

		try {
			String accessToken = restD365Service.getLatestD365Token();
			Map<String, Object> prodMap = new HashMap<>();
			prodMap.put("accessToken", accessToken);
			syncD365Service.syncSalesOrderDataToDB(prodMap);
		} catch (Throwable t) {
			logger.info("*** CronJobServiceImpl >> syncSalesOrderDataToDB >> syncSalesOrderDataToDB >> ERROR: ");
			logger.log(Level.SEVERE, t.getMessage(), t);
		}

		logger.info("*** CronJobServiceImpl >> syncSalesOrderDataToDB >> [END]");
		return null;
	}

	@Override
	public Map<String, Object> syncSalesOrderFulfillByOrderDB(Map<String, Object> paramMap) {
		HashMap<String, Object> searchMap = new HashMap<>();
		searchMap.put("tokenType", "D365");
		logger.info("*** CronJobServiceImpl >> syncSalesOrderFulfillByOrderDB >> [START]");
		try {
			String accessToken = restD365Service.getLatestD365Token();
			Map<String, Object> prodMap = new HashMap<>();
			prodMap.put("accessToken", accessToken);
			syncD365Service.syncSalesOrderFulfillByOrderDB(prodMap);
		} catch (Throwable t) {
			logger.info("*** CronJobServiceImpl >> syncSalesOrderFulfillByOrderDB >> ERROR: ");
			logger.log(Level.SEVERE, t.getMessage(), t);
		}

		logger.info("*** CronJobServiceImpl >> syncSalesOrderFulfillByOrderDB >> [END]");
		return null;
	}

	@Override
	public Map<String, Object> processPurchaseOrder(Map<String, Object> paramMap) {
		try {
			syncD365Service.processPurchaseOrder(paramMap);
		} catch (Throwable t) {
			logger.info("*** CronJobServiceImpl >> processPurchaseOrder >> ERROR: ");
			logger.log(Level.SEVERE, t.getMessage(), t);
		}
		return null;
	}

	@Override
	public boolean getCronEnableStatus(String cronName) {

		String quertTxt = "SELECT * from cms_db.cron_job_enable " + " WHERE job_name = '" + cronName + "' limit 1";
		List<Map<String, Object>> dataList = queryBuilderService.getExecQuery(quertTxt);
		if (CollectionUtils.isNotEmpty(dataList)) {
			for (Map<String, Object> dataMap : dataList) {
				boolean enableJob = (dataMap.get("enable_flag") != null
						&& ((String) dataMap.get("enable_flag")).equals("Y") ? true : false);
				return enableJob;
			}
		}

		return false;
	}

	@Override
	public Map<String, Object> runBDOPostTokenRequest(Map<String, Object> paramMap) {
		logger.info("*** CronJobServiceImpl >> runBDOPostTokenRequest >> [START] ");
		Map<String, Object> tokenMap = null;
		try {
			tokenMap = restBDOService.sendPostTokenRequest(bdoAuthTokenUrl, paramMap);
			if (MapUtils.isNotEmpty(tokenMap) && tokenMap.containsKey("access_token")) {
				HashMap<String, Object> paramToken = new HashMap<>();
				paramToken.put("tokenType", Constants.PAYEEE_BANK_BDO);
				paramToken.put("accessToken", String.valueOf(tokenMap.get("access_token")));
				int result = clientTokenMapper.updateClientToken(paramToken);
				logger.info("*** CronJobServiceImpl >> runBDOPostTokenRequest >> result: " + result);
			} else {
				throw new NoResultException("TOKEN NOT FOUND");
			}
		} catch (Throwable t) {
			logger.info("*** CronJobServiceImpl >> runBDOPostTokenRequest >> ERROR: ");
			logger.log(Level.SEVERE, t.getMessage(), t);
		}

		logger.info("*** CronJobServiceImpl >> runBDOPostTokenRequest >> [END] ");
		return tokenMap;
	}

	@Override
	public Map<String, Object> runBDOCheckPaymenStatusRequest(Map<String, Object> paramMap) {
		logger.info("*** CronJobServiceImpl >> runBDOCheckPaymenStatusRequest >> [START] ");

		String orderNameParam = (String) paramMap.get("orderName");
		Map<String, Object> resultMap = new HashMap<>();
		Date dateInPH = DateUtil.getDateInManilaPH();
		String dateStr = DateUtil.getDateWithPattern(dateInPH, "MM/dd/yyy HH:mm a");

		if (StringUtils.isNotBlank(orderNameParam)) {
			paramMap.put("orderName", orderNameParam);
		} else {
			// paramMap.put("updateDate", "true");
			paramMap.put("nonPaidStatus", "true");
			paramMap.put("apiType", "AUTHORIZE");
		}

		HashMap<String, Object> searchMap = new HashMap<>();
		searchMap.putAll(paramMap);
		// List<Map<String, Object>> bdoOrders =
		// shopOrderMapper.getShopOrderWithNoLines(searchMap);
		List<Map<String, Object>> bdoOrders = bankAPIMapper.getBDOTransactLogs(searchMap);

		if (CollectionUtils.isNotEmpty(bdoOrders)) {
			logger.info("*** CronJobServiceImpl >> bdoOrders: " + bdoOrders.size());
			StringBuilder sbOrders = new StringBuilder("Checking payment status for BDO Orders (").append(dateStr)
					.append(") ").append(System.lineSeparator());

			int rowCtr = 1;
			for (Map<String, Object> bdoOrder : bdoOrders) {
				String orderName = (String) bdoOrder.get("orderName");
				Long dbOrderId = NumberUtil.getLongValue(bdoOrder, "orderId");
				logger.info("** CronJobServiceImpl >> runBDOCheckPaymenStatusRequest >> orderName: " + orderName);
				try {
					Map<String, Object> eOrderMap = onlineOrderService.getOneOrderByID(dbOrderId);
					if (MapUtils.isNotEmpty(eOrderMap)) {
						String ePayStatus = (String) eOrderMap.get("financial_status");

						sbOrders.append(rowCtr++).append(") ORDER: ").append(orderName).append(" ** STATUS: ")
								.append(ePayStatus).append(System.lineSeparator());

						logger.info(
								"** CronJobServiceImpl >> runBDOCheckPaymenStatusRequest >> ePayStatus: " + ePayStatus);
						if (StringUtils.isNotBlank(ePayStatus) && (ePayStatus.equals(Constants.STATUS_PENDING)
								|| ePayStatus.equals(Constants.STATUS_PARTIALLY_PAID))) {
							// VALID ORDER
							// Proceed in checking the order status in BDO
							// ***************************************************************************
							Map<String, Object> tokenMap = restBDOService.sendPostTokenRequest(bdoAuthTokenUrl,
									paramMap);
							logger.info(
									"** CronJobServiceImpl >> runBDOCheckPaymenStatusRequest >> tokenMap: " + tokenMap);
							if (MapUtils.isNotEmpty(tokenMap) && tokenMap.containsKey("access_token")) {
								/*
								 * Check payment status if PAID, process SO and remove from list
								 */
								String accessToken = StringUtils.trimToEmpty((String) tokenMap.get("access_token"));

								String paymentLink = BDOTransactUtil.buildPaymentStatusLink(eOrderMap, bdoOrder,
										authorizeConfigs, payStatusConfigs);

								logger.info("** CronJobServiceImpl >> runBDOCheckPaymenStatusRequest >> paymentLink: "
										+ paymentLink);
								sbOrders.append(orderName).append(": paymentLink >> ").append(paymentLink)
										.append(System.lineSeparator()).append(System.lineSeparator());

								Map<String, Object> reqMap = new HashMap<>();
								reqMap.put("accessToken", accessToken);
								resultMap = restBDOService.sendGetBillsPaymentRequest(paymentLink, reqMap);

								logger.info("** CronJobServiceImpl >> runBDOCheckPaymenStatusRequest >> resultMap: "
										+ resultMap);
								if (MapUtils.isNotEmpty(resultMap) && resultMap.containsKey("context")
										&& resultMap.containsKey("payload")) {
									Map<String, Object> payloadMap = (Map<String, Object>) resultMap.get("payload");
									Map<String, Object> contextMap = (Map<String, Object>) resultMap.get("context");

									sbOrders.append(orderName).append(": payload >> ").append(payloadMap)
											.append(System.lineSeparator());
									sbOrders.append(": context >> ").append(contextMap).append(System.lineSeparator());

									if (MapUtils.isNotEmpty(contextMap) && MapUtils.isNotEmpty(payloadMap)) {
  
										Map<String, Object> processPayMap = new LinkedHashMap<>();
										processPayMap.put("orderId", String.valueOf(dbOrderId));

										String onlineOrderPrice = (String) eOrderMap.get("total_price");
										String payStatusMsg = (String) contextMap.get("status");
										String bankPayStatus = (String) payloadMap.get("paymentStatus");
										boolean isSuccess = BDOTransactUtil.successPaymentStatus(bankPayStatus);
										logger.info("** CronJobServiceImpl >> runBDOCheckPaymenStatusRequest >> isSuccess: " + isSuccess);
										sbOrders.append(">> payStatusMsg: ").append(payStatusMsg)
												.append(System.lineSeparator());
										sbOrders.append(">> bankPayStatus: ").append(bankPayStatus)
												.append(System.lineSeparator());
										sbOrders.append(">> isSuccess: ").append(isSuccess)
												.append(System.lineSeparator());
										
										if (payStatusMsg.equals("SUCCESS") && isSuccess) {
											String debitAmtStr = String.valueOf(payloadMap.get("debitAmount"));
											double dDebitAmt = Double.parseDouble(debitAmtStr);
											double dOnlinePrice = Double.parseDouble(onlineOrderPrice);
											boolean hasPayment = true;
											
											// Check for STAGGERED Payment
											// ********************************************
											boolean hasStaggeredPayment = false;
											Map<String, Object> staggeredPayMap = issuanceService.getNextPaymentSched(orderName);
											if (MapUtils.isNotEmpty(staggeredPayMap)) {
												hasStaggeredPayment = true;
											}

											// Check for EWT Order
											// ********************************************
											boolean isEWTOrder = false;
											HashMap<String, Object> ewtSearchMap = new HashMap<String, Object>();
											ewtSearchMap.put("orderName", orderName);
											List<Map<String, Object>> ewtOrderList = ewtMapper.getOrderEWTList(ewtSearchMap);
											if (CollectionUtils.isNotEmpty(ewtOrderList)) {
												isEWTOrder = true;
												dDebitAmt = Double.parseDouble(onlineOrderPrice);
											}
 	  
											logger.info("** CronJobServiceImpl >> runBDOCheckPaymenStatusRequest >> hasStaggeredPayment: "
															+ hasStaggeredPayment);
											logger.info("** CronJobServiceImpl >> runBDOCheckPaymenStatusRequest >> isEWTOrder: "
													+ isEWTOrder);
											logger.info("** CronJobServiceImpl >> runBDOCheckPaymenStatusRequest >> dDebitAmt: "
													+ dDebitAmt);
									
											String payStatus = Constants.STATUS_PAID;
											if (dDebitAmt >= dOnlinePrice) {
												// Fully PAID
												processPayMap.put("totalOrderPrice", onlineOrderPrice);
												payStatus = Constants.STATUS_PAID;
											} else if (dDebitAmt > 0D && dDebitAmt < dOnlinePrice) {
												// Possible PARTIAL Payment
												processPayMap.put("totalOrderPrice", dDebitAmt);
												payStatus = Constants.STATUS_PARTIALLY_PAID;
											} else {
												// NO PAYMENT
												payStatus = Constants.STATUS_PENDING;
												hasPayment = false;
												logger.info("** CronJobServiceImpl >> runBDOCheckPaymenStatusRequest >> NO/Zero PAYMENT");
											}

											if (hasPayment) {
												Map<String, Object> retTransactMap = onlineOrderService.processPayment(processPayMap);
												logger.info(
														"** CronJobServiceImpl >> runBDOCheckPaymenStatusRequest >> retTransactMap: "
																+ retTransactMap);
												if (MapUtils.isNotEmpty(retTransactMap)
														&& retTransactMap.containsKey("transaction")) {
													String newTags = new StringBuilder(Constants.MODE_OF_PAYMENT_TAG)
															.append("BDO-Online-Payment").toString();
													onlineOrderService.addOrderTagToCurrent(eOrderMap,
															newTags.toString());
													HashMap<String, Object> updateMap = new HashMap<>();
													updateMap.put("orderName", orderName);
													updateMap.put("apiType", "AUTHORIZE");
													updateMap.put("status", payStatus);
													int updResult = bankAPIMapper.updateBDOTransactLogs(updateMap);
													logger.info("** CronJobServiceImpl >> runBDOCheckPaymenStatusRequest >> updResult: "
																	+ updResult);

												}
											}

										}

									}
								}

							}

							sbOrders.append("================================================")
									.append(System.lineSeparator());

						} else {
							// OTHER STATUS that is NOT Pending or Partially Paid
							if (StringUtils.isNotBlank(ePayStatus)) {
								HashMap<String, Object> updateMap = new HashMap<>();
								updateMap.put("orderName", orderName);
								updateMap.put("apiType", "AUTHORIZE");
								updateMap.put("status", ePayStatus);
								int updResult = bankAPIMapper.updateBDOTransactLogs(updateMap);
								logger.info("** CronJobServiceImpl >> runBDOCheckPaymenStatusRequest >> updResult: "
										+ updResult);

							}
						}
					}

				} catch (Exception e) {
					logger.log(Level.SEVERE, e.getMessage(), e);
				} catch (Throwable t) {
					logger.log(Level.SEVERE, t.getMessage(), t);
				}

			}

			emailService.sendEmail(null, Constants.MAIL_RECIPIENTS, "BDO - Check Bills Payment [" + dateStr + "] ",
					sbOrders.toString(), null, null, null);

		}
		logger.info("*** CronJobServiceImpl >> runBDOCheckPaymenStatusRequest >> [END] ");
		return null;
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
