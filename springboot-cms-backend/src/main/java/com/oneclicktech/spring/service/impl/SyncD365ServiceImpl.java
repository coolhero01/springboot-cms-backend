package com.oneclicktech.spring.service.impl;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.oneclicktech.spring.domain.Constants;
import com.oneclicktech.spring.mapper.ClientTokenMapper;
import com.oneclicktech.spring.mapper.CustomerAddressMapper;
import com.oneclicktech.spring.mapper.CustomerMapper;
import com.oneclicktech.spring.mapper.IssuanceMapper;
import com.oneclicktech.spring.mapper.ProductDetailMapper;
import com.oneclicktech.spring.mapper.ProductInventoryMapper;
import com.oneclicktech.spring.mapper.ShopOrderMapper;
import com.oneclicktech.spring.mapper.WarehouseMapper;
import com.oneclicktech.spring.service.AcumaticaService;
import com.oneclicktech.spring.service.AuditLogService;
import com.oneclicktech.spring.service.EmailService;
import com.oneclicktech.spring.service.IssuanceService;
import com.oneclicktech.spring.service.OnlineOrderService;
import com.oneclicktech.spring.service.QueryBuilderService;
import com.oneclicktech.spring.service.RestD365Service;
import com.oneclicktech.spring.service.SyncD365Service;
import com.oneclicktech.spring.util.D365DataUtil;
import com.oneclicktech.spring.util.DateUtil;
import com.oneclicktech.spring.util.NumberUtil;
import com.oneclicktech.spring.util.PCDataUtil;
import com.oneclicktech.spring.util.ShopifyUtil;

@Service
public class SyncD365ServiceImpl implements SyncD365Service {

	private static final Logger logger = Logger.getLogger("SyncD365ServiceImpl");

	RestTemplate restTemplate;

	@Value("${pc.shopify.app.hosturl}")
	String hostUrl;

	@Value("${spavi.d365.default.data-area-id}")
	String defaultDataAreaId;

	@Value("${spavi.d365.api.host-url}")
	String apiHostUrl;

	@Autowired
	ProductDetailMapper productDetailMapper;

	@Autowired
	ProductInventoryMapper productInventoryMapper;

	@Autowired
	CustomerMapper customerMapper;

	@Autowired
	CustomerAddressMapper addressMapper;

	@Autowired
	WarehouseMapper warehouseMapper;

	@Autowired
	ClientTokenMapper clientTokenMapper;

	@Autowired
	ShopOrderMapper shopOrderMapper;

	@Autowired
	OnlineOrderService onlineOrderService;

	@Autowired
	RestD365Service restD365Service;

	@Autowired
	AuditLogService auditLogService;
	
	@Autowired
	IssuanceService issuanceService;
	
	@Autowired
	EmailService emailService;

	@Autowired
	AcumaticaService acumaticaService;

	@Autowired
	QueryBuilderService queryBuilderService;
	
	@Autowired
	IssuanceMapper issuanceMapper;
	
	@PostConstruct
	public void init() {
		restTemplate = new RestTemplate();
	}

	@Override
	public Map<String, Object> viewSalesOrderDetail(String salesOrderNo) throws Throwable {

		logger.info("** SyncD365ServiceImpl >> viewSalesOrderDetail >> [START]");
		String accessToken = restD365Service.getLatestD365Token();

		String requestUrl = new StringBuilder(apiHostUrl)
				.append("/api/services/SPAVPCIntegrationServiceGroup/SPAVPCIntegrationService/getSalesOrderDetails")
				.toString();

		HashMap<String, Object> rootMap = new HashMap<>();
		HashMap<String, Object> detailparamMap = new HashMap<>();
		detailparamMap.put("DataAreaId", defaultDataAreaId);
		detailparamMap.put("SONumber", salesOrderNo);
		rootMap.put("_dataContract", detailparamMap);
		logger.info("** SyncD365ServiceImpl >> viewSalesOrderDetail >> rootMap: " + rootMap);
		Map<String, Object> resultMap = restD365Service.sendPostRequest(requestUrl, accessToken, rootMap);

		logger.info("** SyncD365ServiceImpl >> viewSalesOrderDetail >> resultMap: " + resultMap);
		logger.info("** SyncD365ServiceImpl >> viewSalesOrderDetail >> [END]");

		return resultMap;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Map<String, Object> syncProductDataToDB(Map<String, Object> paramMap) throws Throwable {
		logger.info("** SyncD365ServiceImpl >> syncProductDataToDB >> [START]");
		String accessToken = (String) paramMap.get("accessToken");
		logger.info("** SyncD365ServiceImpl >> accessToken: " + accessToken);
		if (StringUtils.isBlank(accessToken)) {
			accessToken = restD365Service.getLatestD365Token();
		}

		/*
		 * SYNC D365 Product Details To DB
		 * 
		 */
		String productDetailUrl = new StringBuilder(apiHostUrl)
				.append("/api/services/SPAVPCIntegrationServiceGroup/SPAVPCIntegrationService/getProductDetails")
				.toString();

		HashMap<String, Object> prodRootMap = new HashMap<>();
		HashMap<String, Object> prodParamMap = new HashMap<>();
		prodParamMap.put("DataAreaId", defaultDataAreaId);
		prodRootMap.put("_dataContract", prodParamMap);
		Map<String, Object> prodResultMap = restD365Service.sendPostRequest(productDetailUrl, accessToken, prodRootMap);
		HashMap<String, Object> searchMap = new HashMap<>();
		int prodDtlCtr = 0;
		if (MapUtils.isNotEmpty(prodResultMap)) {
			HttpStatus prodResultStatus = (HttpStatus) prodResultMap.get(Constants.RESULT_HTTP_STATUS_CODE);
			if (prodResultStatus.equals(HttpStatus.OK)) {
				List<Map<String, Object>> prod365List = (List<Map<String, Object>>) prodResultMap
						.get(Constants.DATA_LIST);

				int result = 0;
				for (Map<String, Object> prod365Map : prod365List) {

					try {
						if (StringUtils.isNotBlank((String) prod365Map.get("ItemId"))) {
							searchMap = new HashMap<>();
							searchMap.put("itemId", String.valueOf(prod365Map.get("ItemId")).trim());
							Map<String, Object> resultProdMap = productDetailMapper.getProductByItemId(searchMap);
							if (MapUtils.isNotEmpty(resultProdMap)) {
								/*
								 * Product already EXIST in DB UPDATE the Product
								 ****************/
								logger.info("** SyncD365ServiceImpl >>[" + prodDtlCtr
										+ "] PRODUCT DETAILS >> *** UPDATE ****");
								HashMap<String, Object> updateMap = D365DataUtil.mapProductToDBMap(prod365Map);
								result = productDetailMapper.updateProductDetail(updateMap);

							} else {
								/*
								 * Product NOT Exist in DB INSERT the Product
								 ****************/
								logger.info("** SyncD365ServiceImpl >>[" + prodDtlCtr
										+ "] PRODUCT DETAILS >> *** INSERT ****");
								HashMap<String, Object> insertMap = D365DataUtil.mapProductToDBMap(prod365Map);
								result = productDetailMapper.insertProductDetailForSync(insertMap);
							}

						}

						logger.info(
								"** SyncD365ServiceImpl >>[" + prodDtlCtr + "] PRODUCT DETAILS >> result: " + result);

					} catch (Exception e) {
						logger.log(Level.SEVERE, e.getMessage(), e);
					}
					prodDtlCtr++;
				}
			}

		}

		/*
		 * SYNC D365 Product Inventory To DB
		 * 
		 */
		String productInvUrl = new StringBuilder(apiHostUrl)
				.append("/api/services/SPAVPCIntegrationServiceGroup/SPAVPCIntegrationService/getProductInventoryList")
				.toString();

		int prodInvCtr = 0;
		Map<String, Object> invResultMap = restD365Service.sendPostRequest(productInvUrl, accessToken, prodRootMap);
		if (MapUtils.isNotEmpty(invResultMap)) {
			HttpStatus invResultStatus = (HttpStatus) invResultMap.get(Constants.RESULT_HTTP_STATUS_CODE);
			if (invResultStatus.equals(HttpStatus.OK)) {
				List<Map<String, Object>> inv365List = (List<Map<String, Object>>) invResultMap
						.get(Constants.DATA_LIST);
				for (Map<String, Object> inv365Map : inv365List) {
					try {
						String itemNo = (String) inv365Map.get("ItemNumber");
						String warehouse = (String) inv365Map.get("Warehouse");

						if (StringUtils.isNotBlank(itemNo)) {
							searchMap = new HashMap<>();
							searchMap.put("itemNumber", itemNo);
							searchMap.put("warehouse", warehouse);

							Map<String, Object> resultInvMap = productInventoryMapper
									.getProductInventoryById(searchMap);
							if (MapUtils.isNotEmpty(resultInvMap)) {
								/*
								 * Product already EXIST in DB UPDATE the Product
								 ****************/
								logger.info("** SyncD365ServiceImpl >> [" + prodInvCtr
										+ "] PRODUCT INVENTORY >> *** UPDATE ****");
								HashMap<String, Object> updateMap = D365DataUtil.mapProductInventoryToDBMap(inv365Map);
								productInventoryMapper.updateProductInventory(updateMap);
							} else {
								/*
								 * Product NOT Exist in DB INSERT the Product
								 ****************/
								logger.info("** SyncD365ServiceImpl >> [" + prodInvCtr
										+ "] PRODUCT INVENTORY >> *** INSERT ****");
								HashMap<String, Object> insertMap = D365DataUtil.mapProductInventoryToDBMap(inv365Map);
								productInventoryMapper.insertProductInventoryForSync(insertMap);
							}
						}

					} catch (Exception e) {
						logger.log(Level.SEVERE, e.getMessage(), e);
					}
					prodInvCtr++;
				}
			}

		}
		logger.info("** SyncD365ServiceImpl >> syncProductDataToDB >> [END]");
		return invResultMap;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Map<String, Object> syncCustomerDataToDB(Map<String, Object> paramMap) throws Throwable {
		logger.info("** SyncD365ServiceImpl >> syncCustomerDataToDB >> [START]");
		String accessToken = (String) paramMap.get("accessToken");
		logger.info("** SyncD365ServiceImpl >> accessToken: " + accessToken);
		if (StringUtils.isBlank(accessToken)) {
			accessToken = restD365Service.getLatestD365Token();
		}

		String customerUrl = new StringBuilder(apiHostUrl)
				.append("/api/services/SPAVPCIntegrationServiceGroup/SPAVPCIntegrationService/getCustomers").toString();

		HashMap<String, Object> custRootMap = new HashMap<>();
		HashMap<String, Object> custParamMap = new HashMap<>();
		custParamMap.put("DataAreaId", defaultDataAreaId);
		custRootMap.put("_dataContract", custParamMap);

		Map<String, Object> custResultMap = restD365Service.sendPostRequest(customerUrl, accessToken, custRootMap);
		HashMap<String, Object> searchMap = new HashMap<>();
		int custDtlCtr = 0;
		if (MapUtils.isNotEmpty(custResultMap)) {
			HttpStatus custResultStatus = (HttpStatus) custResultMap.get(Constants.RESULT_HTTP_STATUS_CODE);
			if (custResultStatus.equals(HttpStatus.OK)) {
				List<Map<String, Object>> cust365List = (List<Map<String, Object>>) custResultMap
						.get(Constants.DATA_LIST);

				int result = 0;
				for (Map<String, Object> cust365Map : cust365List) {
					String customerNo = null;
					try {
						customerNo = (String) cust365Map.get("CustomerNumber");
						if (StringUtils.isNotBlank(customerNo)) {
							searchMap = new HashMap<>();
							searchMap.put("customerNumber", customerNo);
							Map<String, Object> resultProdMap = customerMapper.getCustomerByCustNo(searchMap);
							if (MapUtils.isNotEmpty(resultProdMap)) {
								/*
								 * Product already EXIST in DB UPDATE the Product
								 ****************/
								logger.info("** SyncD365ServiceImpl >>[" + custDtlCtr
										+ "] CUSTOMER DETAILS >> *** UPDATE ****");
								HashMap<String, Object> updateMap = D365DataUtil.mapCustomerToDBMap(cust365Map);
								result = customerMapper.updateCustomer(updateMap);

							} else {
								/*
								 * Product NOT Exist in DB INSERT the Product
								 ****************/
								logger.info("** SyncD365ServiceImpl >>[" + custDtlCtr
										+ "] CUSTOMER DETAILS >> *** INSERT ****");
								HashMap<String, Object> insertMap = D365DataUtil.mapCustomerToDBMap(cust365Map);
								result = customerMapper.insertCustomer(insertMap);

							}

						}

						logger.info(
								"** SyncD365ServiceImpl >>[" + custDtlCtr + "] CUSTOMER DETAILS >> result: " + result);

					} catch (Exception e) {
						logger.log(Level.SEVERE, e.getMessage(), e);
					}
					custDtlCtr++;
				}
			}

		}

		/*
		 * *****************************************************************************
		 * ********************* CUSTOMER ADDRESS
		 ******************************************************************************* 
		 */

		String custAddressUrl = new StringBuilder(apiHostUrl)
				.append("/api/services/SPAVPCIntegrationServiceGroup/SPAVPCIntegrationService/getCustomerAddress")
				.toString();

		HashMap<String, Object> addrRootMap = new HashMap<>();
		HashMap<String, Object> addrParamMap = new HashMap<>();
		addrParamMap.put("DataAreaId", defaultDataAreaId);
		addrRootMap.put("_dataContract", addrParamMap);

		Map<String, Object> addrResultMap = restD365Service.sendPostRequest(custAddressUrl, accessToken, addrRootMap);
		HashMap<String, Object> searchAddrMap = new HashMap<>();
		int addrCtr = 0;
		if (MapUtils.isNotEmpty(addrResultMap)) {
			HttpStatus addrResultStatus = (HttpStatus) addrResultMap.get(Constants.RESULT_HTTP_STATUS_CODE);
			if (addrResultStatus.equals(HttpStatus.OK)) {
				List<Map<String, Object>> address365List = (List<Map<String, Object>>) addrResultMap
						.get(Constants.DATA_LIST);

				int result = 0;
				for (Map<String, Object> addr365Map : address365List) {
					try {
						String customerNo = (String) addr365Map.get("CustomerNumber");
						if (StringUtils.isNotBlank(customerNo)) {

							searchAddrMap = new HashMap<>();
							searchAddrMap.put("customerNumber", customerNo);
							List<Map<String, Object>> addrList = addressMapper.getCustomerAddressList(searchAddrMap);
							if (CollectionUtils.isNotEmpty(addrList)) {
								/*
								 * Product already EXIST in DB UPDATE the Product
								 ****************/
								Map<String, Object> currentDbMap = addrList.get(0);
								logger.info("** SyncD365ServiceImpl >>[" + addrCtr
										+ "] CUSTOMER ADDRESS >> *** UPDATE ****");
								HashMap<String, Object> updateMap = D365DataUtil.mapCustomerAddressToDBMap(addr365Map);
								currentDbMap.putAll(updateMap);
								result = addressMapper.updateCustomerAddress(updateMap);

							} else {
								/*
								 * Product NOT Exist in DB INSERT the Product
								 ****************/
								logger.info("** SyncD365ServiceImpl >>[" + addrCtr
										+ "] CUSTOMER ADDRESS >> *** INSERT ****");
								HashMap<String, Object> insertMap = D365DataUtil.mapCustomerAddressToDBMap(addr365Map);
								result = addressMapper.insertCustomerAddress(insertMap);

							}

						}

						logger.info("** SyncD365ServiceImpl >>[" + addrCtr + "] CUSTOMER ADDRESS >> result: " + result);

					} catch (Exception e) {
						logger.log(Level.SEVERE, e.getMessage(), e);
					}
					addrCtr++;
				}
			}

		}

		logger.info("** SyncD365ServiceImpl >> syncCustomerDataToDB >> [END]");
		return addrResultMap;
	}

	@Override
	public Map<String, Object> syncWarehouseDataToDB(Map<String, Object> paramMap) throws Throwable {
		logger.info("** SyncD365ServiceImpl >> syncWarehouseDataToDB >> [START]");
		String accessToken = (String) paramMap.get("accessToken");

		if (StringUtils.isBlank(accessToken)) {
			accessToken = restD365Service.getLatestD365Token();
		}
		logger.info("** SyncD365ServiceImpl >> accessToken: " + accessToken);

		String requestUrl = new StringBuilder(apiHostUrl)
				.append("/api/services/SPAVPCIntegrationServiceGroup/SPAVPCIntegrationService/getWarehouseList")
				.toString();

		HashMap<String, Object> rootMap = new HashMap<>();
		paramMap.put("DataAreaId", defaultDataAreaId);
		rootMap.put("_dataContract", paramMap);

		Map<String, Object> resultMap = restD365Service.sendPostRequest(requestUrl, accessToken, rootMap);
		if (MapUtils.isNotEmpty(resultMap)) {
			HttpStatus resultStatus = (HttpStatus) resultMap.get(Constants.RESULT_HTTP_STATUS_CODE);
			if (resultStatus.equals(HttpStatus.OK)) {
				List<Map<String, Object>> dataList = (List<Map<String, Object>>) resultMap.get(Constants.DATA_LIST);
				logger.info("** SyncD365ServiceImpl >> dataList: " + dataList);
				if (CollectionUtils.isNotEmpty(dataList)) {
					warehouseMapper.deleteWarehouse(null);
					for (Map<String, Object> whMap : dataList) {
						HashMap<String, Object> insMap = new HashMap<>();
						insMap.putAll(whMap);
						int result = warehouseMapper.insertWarehouseForSync(insMap);
						logger.info("** SyncD365ServiceImpl >> result: " + result);
					}
				}
			}
		}
		logger.info("** SyncD365ServiceImpl >> syncWarehouseDataToDB >> [END]");
		return resultMap;
	}

	/*
	 * this will CREATE D365 Sales Order (SO)
	 */
	@SuppressWarnings({ "unchecked", "unused" })
	@Override
	public Map<String, Object> syncSalesOrderDataToDB(Map<String, Object> paramMap) throws Throwable {
		logger.info("** SyncD365ServiceImpl >> syncSalesOrderDataToDB >> [START]");

		String accessToken = (String) paramMap.get("accessToken");
		boolean processPerOrder = paramMap.get("processPerOrder") != null ? ((boolean) paramMap.get("processPerOrder"))
				: false;

		if (StringUtils.isBlank(accessToken)) {
			accessToken = restD365Service.getLatestD365Token();
		}

		logger.info("** SyncD365ServiceImpl >> accessToken: " + accessToken);
		String payTag = StringUtils.trimToEmpty((String) paramMap.get("payTag"));
		String paymentMode = StringUtils.trimToEmpty((String) paramMap.get("paymentMode"));

		Map<String, Object> syncResultMap = new HashMap<>();

		HashMap<String, Object> soParamMap = new HashMap<>();
		if (processPerOrder) {
			soParamMap.put("orderName", paramMap.get("orderName"));
		} else {
			soParamMap.put("emptySO", "true");
			soParamMap.put("financialStatus", "paid");
			soParamMap.put("dbUpdateFrom", "true");
			soParamMap.put("dbUpdateTo", "true");
			// Include this because AutoPay will process SO gen immediately
			soParamMap.put("noneAutoPay", "true");
		}

		/*
		 * LIST all Order DB that is PAID
		 */
		List<Map<String, Object>> dbOrderList = shopOrderMapper.getShopOrderList(soParamMap);
		Map<String, List<Map<String, Object>>> mainMap = new LinkedHashMap<String, List<Map<String, Object>>>();
		List<Map<String, Object>> orderLines = new ArrayList<Map<String, Object>>();
		int rowCtr = 0;
		for (Map<String, Object> orderMap : dbOrderList) {
			String orderName = (String) orderMap.get("orderName");
			if (rowCtr == 0) {
				orderLines = new ArrayList<Map<String, Object>>();
				orderLines.add(orderMap);
				mainMap.put(orderName, orderLines);
			} else {
				if (mainMap.containsKey(orderName)) {
					orderLines.add(orderMap);
				} else {
					orderLines = new ArrayList<Map<String, Object>>();
					orderLines.add(orderMap);
					mainMap.put(orderName, orderLines);
				}
			}

			rowCtr++;
		}

		if (Constants.TEST_ONLY) {
			int grpCtr = 1;
			for (Map.Entry<String, List<Map<String, Object>>> entry : mainMap.entrySet()) {
				logger.info(grpCtr + "syncSalesOrderDataToDB KEY:" + entry.getKey());
				logger.info(grpCtr + "syncSalesOrderDataToDB VALUES:" + entry.getValue().size());
				grpCtr++;
			}
		}

		for (Map.Entry<String, List<Map<String, Object>>> entry : mainMap.entrySet()) {

			String orderName = entry.getKey();
			// CHECK IF There's already assigned SO to Order
			// SKIP SO generation if record EXIST
			logger.info("** SyncD365ServiceImpl >> syncSalesOrderDataToDB >> [" + orderName + "] ");

			List<Map<String, Object>> orderList = entry.getValue();

			Map<String, Object> dbOrderMap = orderList.get(0);
			Long orderId = (Long) dbOrderMap.get("shopOrderId");

			List<Map<String, Object>> dbOrder = queryBuilderService
					.getExecQuery("select sales_order_no from cms_db.shop_order where order_name = '" + orderName
							+ "' and (sales_order_no is not null and trim(sales_order_no) != '') limit 1 ");

			if (CollectionUtils.isNotEmpty(dbOrder))
				continue;

			try {

				Map<String, Object> eOrderMap = onlineOrderService.getOneOrderByID(orderId);
				boolean soDataIssueOrTag = PCDataUtil.hasSODataIssueOrSOTag(eOrderMap);
				if (soDataIssueOrTag)
					continue;

				HashMap<String, Object> otherParam = new HashMap<>();
				otherParam.put("defaultDataAreaId", defaultDataAreaId);
				otherParam.putAll(paramMap);
				String orderTags = (String) eOrderMap.get("tags");
				boolean isStaggeredOrder = PCDataUtil.checkOrderIfStaggeredIssuance(orderTags, paramMap);
				boolean isFullPayment = PCDataUtil.isStagIssuanceFullPayment(eOrderMap);
					double interestRate = 0D;
				if (isStaggeredOrder && !isFullPayment) {
					Map<String, Object>  stagInterestMap =  issuanceMapper.getStaggeredInterestRate(orderName);
					if (MapUtils.isNotEmpty(stagInterestMap)) {
					     interestRate = NumberUtil.getDoubleValue(stagInterestMap, "interestRate");
					}
				}
				
				List<Map<String, Object>> soCreateResults = new ArrayList<>();
				// List<Map<String, Object>> soRequestList =
				// D365DataUtil.buildSplitSOServiceRequest(otherParam, orderList, eOrderMap);
				List<Map<String, Object>> soRequestList = this.buildSplitSOServiceRequest(otherParam, orderList,
						eOrderMap);

				if (CollectionUtils.isNotEmpty(soRequestList)) {
 
					for (Map<String, Object> soRequestMap : soRequestList) {

						logger.info("** SyncD365ServiceImpl >> syncSalesOrderDataToDB >> [" + orderName
								+ "] >> soRequestMap:" + soRequestMap);

						String requestUrl = new StringBuilder(apiHostUrl).append(
								"/api/services/SPAVPCIntegrationServiceGroup/SPAVPCIntegrationService/createSalesOrder")
								.toString();

						String genSalesOrderNo = null;
						String resultMsg = null;

						try {
							/*
							 * This request will CREATE Sales Order to D365 and UPDATE the Order DB
							 */
							Gson gson = new GsonBuilder().setPrettyPrinting().create();
							String bodyRequest = gson.toJson(soRequestMap, Map.class);
							logger.info("*** syncSalesOrderDataToDB >> bodyRequest: " + bodyRequest);
							logger.info("*** syncSalesOrderDataToDB >> requestUrl: " + requestUrl);

							Map<String, Object> resultMap = restD365Service.sendPostRequest(requestUrl, accessToken,
									soRequestMap);

							HttpStatus resultStatus = (HttpStatus) resultMap.get(Constants.RESULT_HTTP_STATUS_CODE);
							if (resultStatus.equals(HttpStatus.OK)) {
								List<Map<String, Object>> dataList = (List<Map<String, Object>>) resultMap
										.get(Constants.DATA_LIST);
								Map<String, Object> outputMap = null;
								if (CollectionUtils.isNotEmpty(dataList)) {
									for (Map<String, Object> dataMap : dataList) {
										resultMsg = (String) dataMap.get("Result");
										String soNumber = (String) dataMap.get("SalesOrderNumber");
										String soGenResult = "SUCCESS";
										if (StringUtils.isBlank(soNumber)) {
											soGenResult = "ERROR";
										}

										logger.info("** SyncD365ServiceImpl >> syncSalesOrderDataToDB >> resultMsg: "
												+ resultMsg);
										String mailBodyMsg = new StringBuilder("REQUEST: \r\n").append(bodyRequest)
												.append("\r\n\r\n").append("RESPONSE: \r\n").append(dataMap.toString())
												.toString();

										boolean mailSent = emailService.sendEmail(null, Constants.MAIL_RECIPIENTS,
												"PC - SO Creation for Order: " + orderName + "  [" + soGenResult + "] ",
												mailBodyMsg, null, null, null);

										soCreateResults.add(dataMap);
									}
								}
							}

							syncResultMap.put("statusResult", "SUCCESS");
							syncResultMap.put("lastAuditLog", resultMsg);

						} catch (Exception e) {
							logger.info("** SyncD365ServiceImpl >> syncSalesOrderDataToDB >>ERROR: " + orderName);
							resultMsg = e.getMessage();
							auditLogService.saveOrderAuditLog(orderName, genSalesOrderNo, soRequestMap.toString(),
									resultMsg);
							logger.log(Level.SEVERE, e.getMessage(), e);
						}

					}

					if (CollectionUtils.isNotEmpty(soCreateResults)) {
						String generatedSONo = null;
						StringBuilder soNumbers = new StringBuilder();
						for (Map<String, Object> soResult : soCreateResults) {
							String createResult = (String) soResult.get("Result");
							logger.info("** SyncD365ServiceImpl >> syncSalesOrderDataToDB >> soResult: " + soResult);
							if (createResult.equals("success")) {
								generatedSONo = (String) soResult.get("SalesOrderNumber");
								soNumbers.append(generatedSONo).append(",");
							} else {
								logger.info("** SyncD365ServiceImpl >> syncSalesOrderDataToDB >> ERROR: ");
								logger.log(Level.SEVERE, "ERROR [" + orderName + "]", "ERROR");
								// soNumbers.append("SO_Data_Issue").append(",");
							}
						}

						if (soNumbers.length() > 1) {
							
							//boolean isStaggeredOrder = issuanceService.isStaggeredPaymentOrder(orderName);
					 		String finalSO = soNumbers.substring(0, soNumbers.length() - 1);
							HashMap<String, Object> updateMap = new HashMap<>();
							updateMap.put("orderName", orderName);
							if (isStaggeredOrder && !isFullPayment) {
								updateMap.put("financialStatus", Constants.STATUS_PENDING);
							} else {
								updateMap.put("financialStatus", Constants.STATUS_PAID);
							}
							
					  		updateMap.put("salesOrderNo", finalSO);
							updateMap.put("soCreateDate", new Date());
							int updateResult = shopOrderMapper.updateShopOrder(updateMap);
							syncResultMap.put("salesOrderNo", finalSO);
							logger.info("** SyncD365ServiceImpl >> syncSalesOrderDataToDB >> updateResult: "
									+ updateResult);

							if (updateResult != 0) {
								// UPDATE ORDER TAG
								HashMap<String, Object> searchMap = new HashMap<>();
								searchMap.put("orderName", orderName);
								List<Map<String, Object>> newDBOrders = shopOrderMapper
										.getShopOrderWithNoLines(searchMap);

								if (CollectionUtils.isNotEmpty(newDBOrders)) {
									Map<String, Object> newDBOrder = newDBOrders.get(0);
									if (CollectionUtils.isNotEmpty(newDBOrders)) {
										if (StringUtils.isNotBlank(payTag)) {
											newDBOrder.put("payTag", payTag);
										}
										if (StringUtils.isNotBlank(paymentMode)) {
											newDBOrder.put("paymentMode", paymentMode);
										}
										onlineOrderService.updateOnlineTagForSO(eOrderMap, newDBOrder);
									}
								}
								auditLogService.saveOrderAuditLog(orderName, finalSO, updateMap.toString(), finalSO);
							}

						} else {
							// ERROR tag as SO_Data_Issue
							// ****************************************
							syncResultMap.put("statusResult", "FAILED");
							syncResultMap.put("lastAuditLog", "ERROR: SO Data Issue");
							onlineOrderService.addOrderTagToCurrent(eOrderMap, Constants.SO_DATA_ISSUE);
						}
					}

				}

			} catch (Throwable t) {
				logger.log(Level.SEVERE, t.getMessage(), t);
			}

		}

		logger.info("** SyncD365ServiceImpl >> syncSalesOrderDataToDB >> [END]");
		return syncResultMap;
	}

	private boolean checkItemCountIfMatch(List<Map<String, Object>> soRequestList,
			List<Map<String, Object>> dbOrderLines) {

		boolean itemCountIsMatch = false;
		int dbItemLineCount = 0;
		if (CollectionUtils.isNotEmpty(dbOrderLines)) {
			dbItemLineCount = dbOrderLines.size();
		}

		int soItemLineCount = 0;
		for (Map<String, Object> soMap : soRequestList) {
			Map<String, Object> rootMap = (Map<String, Object>) soMap.get("_dataContract");
			if (MapUtils.isNotEmpty(rootMap)) {
				List<Map<String, Object>> soLines = (List<Map<String, Object>>) rootMap.get("SOLines");
				if (CollectionUtils.isNotEmpty(soLines)) {
					soItemLineCount = soItemLineCount + soLines.size();
				}
			}

		}
		soItemLineCount = (soItemLineCount - 1); // Remove Delivery charge count
		if (dbItemLineCount == soItemLineCount) {
			itemCountIsMatch = true;
		}

		return itemCountIsMatch;
	}

//	public Map<String, Object> syncSalesOrderDataToDB(Map<String, Object> paramMap) throws Throwable {
//		logger.info("** SyncD365ServiceImpl >> syncSalesOrderDataToDB >> [START]");
//		String[] mailReceipients = new String[] { "matthew.poako@gmail.com", "alex.deleon@oneclicktech.ph" };
//		
//		String accessToken = (String) paramMap.get("accessToken"); 
//		boolean processPerOrder = paramMap.get("processPerOrder")!=null? ((boolean)paramMap.get("processPerOrder")):false; 
//			
//		if (StringUtils.isBlank(accessToken)) {
//			accessToken = restD365Service.getLatestD365Token();
//		}
//
//		logger.info("** SyncD365ServiceImpl >> accessToken: " + accessToken);
//
//		Map<String, Object> syncResultMap = new HashMap<>();
//		 
//		HashMap<String, Object> soParamMap = new HashMap<>();
//		if (processPerOrder) {
//	 		soParamMap.put("orderName", paramMap.get("orderName"));
//	  	} else {
//			soParamMap.put("emptySO", "true");
//			soParamMap.put("financialStatus", "paid");
//			soParamMap.put("dbUpdateFrom", "true");
//			soParamMap.put("dbUpdateTo", "true");
//			//Include this because AutoPay will process SO gen immediately
//			soParamMap.put("noneAutoPay", "true");
// 		}
//		 
//		if (Constants.TEST_ONLY) {
//			soParamMap.put("emptySO", null);
//			soParamMap.put("dbUpdateFrom", null);
//			soParamMap.put("dbUpdateTo", null);
//			soParamMap.put("financialStatus", null);
//			//soParamMap.put("orderName", "PC1017");
//		}
//		/*
//		 * LIST all Order DB that is PAID
//		 */
//		List<Map<String, Object>> dbOrderList = shopOrderMapper.getShopOrderList(soParamMap);
//		Map<String, List<Map<String, Object>>> mainMap = new LinkedHashMap<String, List<Map<String, Object>>>();
//		List<Map<String, Object>> orderLines = new ArrayList<Map<String, Object>>();
//		int rowCtr = 0;
//		for (Map<String, Object> orderMap : dbOrderList) {
//			String orderName = (String) orderMap.get("orderName");
//			if (rowCtr == 0) {
//				orderLines = new ArrayList<Map<String, Object>>();
//				orderLines.add(orderMap);
//				mainMap.put(orderName, orderLines);
//			} else {
//				if (mainMap.containsKey(orderName)) {
//					orderLines.add(orderMap);
//				} else {
//					orderLines = new ArrayList<Map<String, Object>>();
//					orderLines.add(orderMap);
//					mainMap.put(orderName, orderLines);
//				}
//			}
//
//			rowCtr++;
//		}
//
//		if (Constants.TEST_ONLY) {
//			int grpCtr = 1;
//			for (Map.Entry<String, List<Map<String, Object>>> entry : mainMap.entrySet()) {
//				logger.info(grpCtr + "syncSalesOrderDataToDB KEY:" + entry.getKey());
//				logger.info(grpCtr + "syncSalesOrderDataToDB VALUES:" + entry.getValue().size());
//				grpCtr++;
//			}
//		}
//		
//	 	
//		for (Map.Entry<String, List<Map<String, Object>>> entry : mainMap.entrySet()) {
//	 
//			String orderName = entry.getKey();
//	 		//CHECK IF There's already assigned SO to Order
//			//SKIP SO generation if record EXIST
//			logger.info("** SyncD365ServiceImpl >> syncSalesOrderDataToDB >> [" + orderName + "] ");
//			
//			List<Map<String, Object>> orderList = entry.getValue();
//
//			Map<String, Object> dbOrderMap = orderList.get(0);
//			Long orderId = (Long) dbOrderMap.get("shopOrderId");
// 			
//			List<Map<String,Object>> dbOrder = queryBuilderService.getExecQuery("select sales_order_no from cms_db.shop_order where order_name = '"+orderName+"' and (sales_order_no is not null and trim(sales_order_no) != '') limit 1 ");
//			if (CollectionUtils.isNotEmpty(dbOrder))
//				continue;
//			
//		 	try {
//		 		
//		 		Map<String, Object> eOrderMap = onlineOrderService.getOneOrderByID(orderId);
//		  		HashMap<String, Object> otherParam = new HashMap<>();
//				otherParam.put("defaultDataAreaId", defaultDataAreaId);
//				
//			 	List<Map<String, Object>> soCreateResults = new ArrayList<>();
//				
//				List<Map<String, Object>> soRequestList = D365DataUtil.buildSplitSOServiceRequest(otherParam, orderList,
//						eOrderMap);
//
//				for (Map<String, Object> soRequestMap : soRequestList) {
//
//					logger.info("** SyncD365ServiceImpl >> syncSalesOrderDataToDB >> [" + orderName + "] >> soRequestMap:"
//							+ soRequestMap);
//
//					String requestUrl = new StringBuilder(apiHostUrl)
//							.append("/api/services/SPAVPCIntegrationServiceGroup/SPAVPCIntegrationService/createSalesOrder")
//							.toString();
//					
//					String genSalesOrderNo = null;
//					String resultMsg = null;
//					try {
//						/*
//						 * This request will CREATE Sales Order to D365 and UPDATE the Order DB
//						 */
//						Gson gson = new GsonBuilder().setPrettyPrinting().create();
//						String bodyRequest = gson.toJson(soRequestMap, Map.class);
//						System.out.println("*** syncSalesOrderDataToDB >> bodyRequest: " + bodyRequest);
//
//						Map<String, Object> resultMap = restD365Service.sendPostRequest(requestUrl, accessToken,
//								soRequestMap);
//
//						HttpStatus resultStatus = (HttpStatus) resultMap.get(Constants.RESULT_HTTP_STATUS_CODE);
//						if (resultStatus.equals(HttpStatus.OK)) {
//							List<Map<String, Object>> dataList = (List<Map<String, Object>>) resultMap
//									.get(Constants.DATA_LIST);
//							Map<String, Object> outputMap = null;
//							if (CollectionUtils.isNotEmpty(dataList)) {
//								for (Map<String, Object> dataMap : dataList) {
//									resultMsg = (String) dataMap.get("Result");
//									logger.info("** SyncD365ServiceImpl >> syncSalesOrderDataToDB >> resultMsg: " + resultMsg);
//									String mailBodyMsg = new StringBuilder("REQUEST: \r\n").append(bodyRequest)
//											.append("\r\n\r\n").append("RESPONSE: \r\n").append(dataMap.toString())
//											.toString();
//
//									boolean mailSent = emailService.sendEmail(null, mailReceipients,
//											"PC - SO Creation for Order: " + orderName + "  [STARTED] ", mailBodyMsg, null,
//											null, null);
//
//									soCreateResults.add(dataMap);
//								}
//							}
//						}
//
//						syncResultMap.put("statusResult", "SUCCESS");
//						syncResultMap.put("lastAuditLog", resultMsg);
//
//					} catch (Exception e) {
//						logger.info("** SyncD365ServiceImpl >> syncSalesOrderDataToDB >>ERROR: " + orderName);
//						resultMsg = e.getMessage();
//						auditLogService.saveOrderAuditLog(orderName, genSalesOrderNo, soRequestMap.toString(), resultMsg);
//						logger.log(Level.SEVERE, e.getMessage(), e);
//					}
//
//				}
//
//				if (CollectionUtils.isNotEmpty(soCreateResults)) {
//					String generatedSONo = null;
//					StringBuilder soNumbers = new StringBuilder();
//					for (Map<String, Object> soResult : soCreateResults) {
//						String createResult = (String) soResult.get("Result");
//						logger.info("** SyncD365ServiceImpl >> syncSalesOrderDataToDB >> soResult: " + soResult);
//						if (createResult.equals("success")) {
//							generatedSONo = (String) soResult.get("SalesOrderNumber");
//							soNumbers.append(generatedSONo).append(",");
//						} else {
//							logger.info("** SyncD365ServiceImpl >> syncSalesOrderDataToDB >> ERROR: ");
//							logger.log(Level.SEVERE, "ERROR [" + orderName + "]", "ERROR");
//					 		soNumbers.append("SO_Data_Issue").append(",");
//						}
//					}
//
//					if (soNumbers.length() > 1) {
//						String finalSO = soNumbers.substring(0, soNumbers.length() - 1);
//						HashMap<String, Object> updateMap = new HashMap<>();
//						updateMap.put("orderName", orderName);
//						updateMap.put("financialStatus", "paid");
//						updateMap.put("salesOrderNo", finalSO);
//					    updateMap.put("soCreateDate", new Date());
//						int updateResult = shopOrderMapper.updateShopOrder(updateMap);
//						logger.info("** SyncD365ServiceImpl >> syncSalesOrderDataToDB >> updateResult: " + updateResult);
//
//						if (updateResult != 0) {
//							// UPDATE ORDER TAG
//							HashMap<String, Object> searchMap = new HashMap<>();
//							searchMap.put("orderName", orderName);
//							List<Map<String, Object>> newDBOrders = shopOrderMapper.getShopOrderWithNoLines(searchMap);
//							
//							if (CollectionUtils.isNotEmpty(newDBOrders)) {
//								Map<String, Object> newDBOrder = newDBOrders.get(0);
//								Map<String, Object> soDetailMap = this.viewSalesOrderDetail(generatedSONo);
//								List<Map<String, Object>> dataList = (List<Map<String, Object>>) soDetailMap
//										.get(Constants.DATA_LIST);
//								if (CollectionUtils.isNotEmpty(dataList)) {
//									Map<String, Object> oneSODetail = dataList.get(0);
//									onlineOrderService.updateOnlineOrderTag(newDBOrder, oneSODetail);
//								}
//							}
//
//							auditLogService.saveOrderAuditLog(orderName, finalSO, updateMap.toString(), finalSO);
//						}
//
//					}
//				}
//
//		 	}  catch (Throwable t) {
//		 		logger.log(Level.SEVERE, t.getMessage(), t);
//		 	}
//		 	
//		
//		}
//
//		logger.info("** SyncD365ServiceImpl >> syncSalesOrderDataToDB >> [END]");
//		return syncResultMap;
//	}
// 

	@SuppressWarnings("unchecked")
	@Override
	public Map<String, Object> syncSalesOrderFulfillByOrderDB(Map<String, Object> paramMap) throws Throwable {
		logger.info("** SyncD365ServiceImpl >> syncSalesOrderFulfillByOrderDB >> [START]");
		String accessToken = restD365Service.getLatestD365Token();
		logger.info("** SyncD365ServiceImpl >> accessToken: " + accessToken);

		HashMap<String, Object> prodParamMap = new HashMap<>();
		prodParamMap.put("financialStatus", "paid");
		prodParamMap.put("withSO", "true");
		prodParamMap.put("dbUpdateFrom", "true");
		prodParamMap.put("dbUpdateTo", "true");
		prodParamMap.put("notInvoiced", "true");

		if (Constants.TEST_ONLY) {
			prodParamMap.put("dbUpdateFrom", null);
			prodParamMap.put("dbUpdateTo", null);
			prodParamMap.put("orderName", "PC1014");
			prodParamMap.put("notInvoiced", null);
		}
		/*
		 * LIST all Order DB that is PAID
		 */
		StringBuilder mailBodyMsg = new StringBuilder();

		List<Map<String, Object>> dbOrderList = shopOrderMapper.getShopOrderWithNoLines(prodParamMap);
		if (CollectionUtils.isNotEmpty(dbOrderList)) {
			mailBodyMsg.append("*** Orders to be check for fulfillment [START]***** \r\n");
			for (Map<String, Object> orderMap : dbOrderList) {
				String orderName = (String) orderMap.get("orderName");
				String salesOrderNos = (String) orderMap.get("salesOrderNo");

				mailBodyMsg.append("Order Name: ").append(orderName).append("\r\n");
				mailBodyMsg.append("Sales Order #: ").append(salesOrderNos).append("\r\n");

				List<String> soNumbers = PCDataUtil.getSONumbersByDB(salesOrderNos);
				for (String soNum : soNumbers) {
					logger.info("** SyncD365ServiceImpl >> syncSalesOrderFulfillByOrderDB >> orderName: " + orderName);
					logger.info("** SyncD365ServiceImpl >> syncSalesOrderFulfillByOrderDB >> soNum: " + soNum);

					try {
						String requestUrl = new StringBuilder(apiHostUrl).append(
								"/api/services/SPAVPCIntegrationServiceGroup/SPAVPCIntegrationService/getSalesOrderDetails")
								.toString();
						Map<String, Object> rootMap = new LinkedHashMap<>();
						Map<String, Object> requestMap = new LinkedHashMap<>();
						requestMap.put("DataAreaId", defaultDataAreaId);
						requestMap.put("SONumber", soNum);
						rootMap.put("_dataContract", requestMap);

						Map<String, Object> resultMap = restD365Service.sendPostRequest(requestUrl, accessToken,
								rootMap);
						logger.info("** SyncD365ServiceImpl >> syncSalesOrderFulfillByOrderDB >> resultMap: "
								+ resultMap.toString());
						if (MapUtils.isNotEmpty(resultMap)) {
							HttpStatus resultStatus = (HttpStatus) resultMap.get(Constants.RESULT_HTTP_STATUS_CODE);
							if (resultStatus.equals(HttpStatus.OK)) {
								List<Map<String, Object>> dataList = (List<Map<String, Object>>) resultMap
										.get(Constants.DATA_LIST);
								if (CollectionUtils.isNotEmpty(dataList)) {
									Map<String, Object> soOrderMap = dataList.get(0);
									String soFulfillStatus = StringUtils
											.trimToEmpty((String) soOrderMap.get("DeliveryStatus"));

									if (StringUtils.isNotBlank(soFulfillStatus)) {
										HashMap<String, Object> updateMap = new HashMap<>();
										updateMap.put("orderName", orderName);
										updateMap.put("soFulfillmentStatus", soFulfillStatus);
										int updateResult = shopOrderMapper.updateShopOrder(updateMap);
										logger.info(
												"** SyncD365ServiceImpl >> syncSalesOrderFulfillByOrderDB >> updateResult: "
														+ updateResult);
									}

								}
							}
						}

					} catch (Exception e) {
						logger.log(Level.SEVERE, e.getMessage(), e);
					} catch (Throwable t) {
						logger.log(Level.SEVERE, t.getMessage(), t);
					}
				}

			}

			mailBodyMsg.append("*** Orders to be check for fulfillment [END]***** \r\n");
		}

		/*
		 * LIST all Order DB that is PAID with SO and INVOICED PROCESS ONLINE COMPLETE
		 * ORDER Fulfillment
		 */
		prodParamMap = new HashMap<>();
		prodParamMap.put("withSO", "true");
		prodParamMap.put("soFulfillmentStatus", Constants.SO_FULFILLMENT_STATUS_INVOICED);
		prodParamMap.put("notFulfilled", "true");
		prodParamMap.put("dbUpdateFrom", "true");
		prodParamMap.put("dbUpdateTo", "true");

		if (Constants.TEST_ONLY) {
			prodParamMap.put("withSO", null);
			prodParamMap.put("dbUpdateFrom", null);
			prodParamMap.put("dbUpdateTo", null);
			prodParamMap.put("soFulfillmentStatus", null);
			prodParamMap.put("notFulfilled", null);
			prodParamMap.put("orderName", "PC1014");
		}

		List<Map<String, Object>> dbOrderFulfillList = shopOrderMapper.getShopOrderWithNoLines(prodParamMap);
		if (CollectionUtils.isNotEmpty(dbOrderFulfillList)) {
			mailBodyMsg.append("*** INVOICED Orders for fulfillment [START]***** \r\n");
			for (Map<String, Object> dbOrder : dbOrderFulfillList) {
				String eOrderId = String.valueOf((Long) dbOrder.get("shopOrderId"));
				String orderName = (String) dbOrder.get("orderName");
				mailBodyMsg.append("Order ID: ").append(eOrderId).append("\r\n");
				mailBodyMsg.append("Order Name: ").append(orderName).append("\r\n");

				try {
					Map<String, Object> resultMap = onlineOrderService.completeOrderFulfillment(eOrderId);
					if (MapUtils.isNotEmpty(resultMap) && resultMap.containsKey("fulfillment")) {
						Map<String, Object> fulFillMap = (Map<String, Object>) resultMap.get("fulfillment");
						if (MapUtils.isNotEmpty(fulFillMap) && ((String) fulFillMap.get("status")).equals("success")) {
							HashMap<String, Object> updateMap = new HashMap<>();
							updateMap.put("orderName", orderName);
							updateMap.put("fulfillmentStatus", "fulfilled");
							int updateResult = shopOrderMapper.updateShopOrder(updateMap);
						}

					}
				} catch (Throwable t) {
					logger.log(Level.SEVERE, t.getMessage(), t);
				}

			}
			mailBodyMsg.append("*** INVOICED Orders for fulfillment [END]***** \r\n");
		}
		SimpleDateFormat sdf = new SimpleDateFormat("MMddyyyy");
		String dateNow = sdf.format(new Date());
		boolean mailSent = emailService.sendEmail(null, Constants.MAIL_RECIPIENTS,
				"PC - Order Fulfillment Report " + dateNow, mailBodyMsg.toString(), null, null, null);

		logger.info("** SyncD365ServiceImpl >> syncSalesOrderFulfillByOrderDB >> [END]");
		return null;
	}

	@Override
	public boolean isValidCustomerAcct(String soCustomerNo) throws Throwable {
		boolean isValidAcct = false;

		try {
			String accessToken = restD365Service.getLatestD365Token();

			String requestUrl = new StringBuilder(apiHostUrl)
					.append("/api/services/SPAVPCIntegrationServiceGroup/SPAVPCIntegrationService/getCustomers")
					.toString();

			Map<String, Object> rootMap = new LinkedHashMap();
			Map<String, Object> detailParamMap = new LinkedHashMap<>();
			detailParamMap.put("DataAreaId", defaultDataAreaId);
			detailParamMap.put("CustomerNumber", soCustomerNo);
			rootMap.put("_dataContract", detailParamMap);
			logger.info("** SyncD365ServiceImpl >> isValidCustomerAcct >> rootMap: " + rootMap);

			Map<String, Object> resultMap = restD365Service.sendPostRequest(requestUrl, accessToken, rootMap);
			if (MapUtils.isNotEmpty(resultMap)) {
				List<Map<String, Object>> dataList = (List<Map<String, Object>>) resultMap.get(Constants.DATA_LIST);
				if (CollectionUtils.isNotEmpty(dataList)) {
					for (Map<String, Object> dataMap : dataList) {
						logger.info("** SyncD365ServiceImpl >> isValidCustomerAcct >> dataMap: " + dataMap); 
						
						boolean oosInclude = (dataMap.get("OOSInclude") != null
								&& String.valueOf(dataMap.get("OOSInclude")).equals("Y")) ? true : false;
						
						boolean isAcctOnHold = (dataMap.get("IsCustomerOnHold") != null
								&& String.valueOf(dataMap.get("IsCustomerOnHold")).equals("Y")) ? true : false;
						
						if (apiHostUrl.contains("bmi-sm-uat")) {
							isAcctOnHold = false;
							oosInclude = true;
						}
						
						if (oosInclude && !isAcctOnHold)  {
							//VALID ACCOUNT
							//OOS_INCLUDE = Y and IsCustomerOnHold = N
							isValidAcct = true;
							break;
						}
					}
				}

			}

		} catch (Throwable t) {
			logger.log(Level.SEVERE, t.getMessage(), t);
			isValidAcct = false;
		}

		return isValidAcct;
	}

	@Override
	public Map<String, Object> processSalesAgreement(Map<String, Object> paramMap) throws Throwable {
		logger.info("*** SyncD365ServiceImpl >> processSalesAgreement >> [START]");

		HttpStatus httpStat = HttpStatus.OK;
		String requestUrl = new StringBuilder(apiHostUrl)
				.append("/api/services/SPAVPCIntegrationServiceGroup/SPAVPCIntegrationService/getProductInventoryList")
				.toString();

		String accessToken = restD365Service.getLatestD365Token();
		logger.info("accessToken: " + accessToken);

		HashMap<String, Object> rootMap = new HashMap<>();
		HashMap<String, Object> detailParamMap = new HashMap<>();
		detailParamMap.put("DataAreaId", defaultDataAreaId);
		rootMap.put("_dataContract", detailParamMap);
		Map<String, Object> resultMap = restD365Service.sendPostRequest(requestUrl, accessToken, rootMap);
		if (MapUtils.isNotEmpty(resultMap)) {
			HttpStatus resultStatus = (HttpStatus) resultMap.get(Constants.RESULT_HTTP_STATUS_CODE);
			List<Map<String, Object>> dataList = (List<Map<String, Object>>) resultMap.get(Constants.DATA_LIST);
			for (Map<String, Object> dataMap : dataList) {
				String customerNo = (String) dataMap.get("CustomerAccount");
				String status = (String) dataMap.get("Status");
				Date expirationDate = DateUtils.parseDate((String) dataMap.get("Status"), "MM/dd/yyyy HH:mm:ss a");
				logger.info("*** SyncD365ServiceImpl >> processSalesAgreement >> customerNo: " + customerNo);
			}
		}

		logger.info("*** SyncD365ServiceImpl >> processSalesAgreement >> [END]");
		return null;
	}

	@Override
	public Map<String, Object> processPurchaseOrder(Map<String, Object> paramMap) throws Throwable {
		logger.info("*** SyncD365ServiceImpl >> processPurchaseOrder >> [START]");

		HashMap<String, Object> searchMap = new HashMap<>();
		searchMap.putAll(paramMap);
		searchMap.put("withSO", "true");
		searchMap.put("emptyPO", "true");
		searchMap.put("notInvoiced", "true");
		searchMap.put("soCustomerNoLike", "WBHI%");
		searchMap.put("financialStatus", "paid");

		if (Constants.TEST_ONLY) {
			searchMap = new HashMap<>();
		}

		if (MapUtils.isNotEmpty(paramMap) && paramMap.containsKey("orderName")) {
			searchMap.put("orderName", paramMap.get("orderName"));
		}

		if (MapUtils.isNotEmpty(paramMap) && paramMap.containsKey("orderIn")) {
			searchMap.put("orderIn", paramMap.get("orderIn"));
		}

//		if (Constants.TEST_ONLY) {
//			searchMap = new HashMap<>();
//			searchMap.put("orderName", "PC1017");
//		}

		List<Map<String, Object>> dbOrderList = shopOrderMapper.getShopOrderWithNoLines(searchMap);

		if (CollectionUtils.isNotEmpty(dbOrderList)) {

			Map<String, Map<String, Object>> whMaps = new HashMap<>();
			String queryTxt = "select * from cms_db.po_warehouse_mapping";
			List<Map<String, Object>> whMappingList = queryBuilderService.getExecQuery(queryTxt);
			for (Map<String, Object> whMap : whMappingList) {
				String soWarehouseId = StringUtils.trimToEmpty((String) whMap.get("so_warehouse_id"));
				whMaps.put(soWarehouseId, whMap);
			}

			for (Map<String, Object> dbOrder : dbOrderList) {

				long dbOrderId = (Long) dbOrder.get("shopOrderId");
				String orderName = (String) dbOrder.get("orderName");
				String soCustomerNo = (String) dbOrder.get("soCustomerNo");
				logger.info("*** SyncD365ServiceImpl >> processPurchaseOrder >> soCustomerNo: " + soCustomerNo);

				try {
					if (whMaps.get(soCustomerNo) != null) {
						// Has PO Warehouse Mapping
						Map<String, Object> soWarehouseInfo = whMaps.get(soCustomerNo);
						Map<String, Object> eOrderMap = onlineOrderService.getOneOrderByID(dbOrderId);

						if (MapUtils.isNotEmpty(eOrderMap)) {
							Map<String, Object> poResultMap = acumaticaService.createPurchaseOrder(true, eOrderMap,
									dbOrder, soWarehouseInfo);

							if (MapUtils.isNotEmpty(poResultMap) && !poResultMap.containsKey("error")
									&& poResultMap.containsKey("OrderNbr")) {
								// SUCCESS - PO Creation
								String poNumber = StringUtils
										.trimToEmpty((String) PCDataUtil.getValue(poResultMap, "OrderNbr"));

								if (StringUtils.isNotBlank(poNumber) && poNumber.startsWith("PO")) {
									logger.info("*** SyncD365ServiceImpl >> processPurchaseOrder >> PO NUMBER: "
											+ poNumber);

									HashMap<String, Object> updateMap = new HashMap<>();
									updateMap.put("orderName", orderName);
									updateMap.put("poNumber", poNumber);
									int result = shopOrderMapper.updateShopOrder(updateMap);
									if (result != 0 && StringUtils.isNotBlank(poNumber)) {
										String poTag = new StringBuilder(Constants.PURCHASE_ORDER_NO_TAG)
												.append(poNumber).toString();
										onlineOrderService.addOrderTagToCurrent(eOrderMap, poTag);
									}
								} else {
									logger.info(
											"*** SyncD365ServiceImpl >> processPurchaseOrder >> ERROR: NO PO Generated ");
								}

							} else {
								logger.info(
										"*** SyncD365ServiceImpl >> processPurchaseOrder >> ERROR: NO PO Generated ");
							}
						}
					}
				} catch (Exception e) {
					logger.log(Level.SEVERE, e.getMessage(), e);
				} catch (Throwable t) {
					logger.log(Level.SEVERE, t.getMessage(), t);
				}

			}
		}

		logger.info("*** SyncD365ServiceImpl >> processPurchaseOrder >> [END]");
		return null;
	}

	@Override
	public Map<String, Object> cancelSalesOrder(String salesOrderNo) {
		logger.info("*** SyncD365ServiceImpl >> cancelSalesOrder >> [START]");

		HttpStatus httpStat = HttpStatus.OK;
		String requestUrl = new StringBuilder(apiHostUrl)
				.append("/api/services/SPAVPCIntegrationServiceGroup/SPAVPCIntegrationService/XXXXXXXXXXXXXXXX")
				.toString();
		Map<String, Object> resultMap = null;

		try {
			String accessToken = restD365Service.getLatestD365Token();
			logger.info("accessToken: " + accessToken);

			HashMap<String, Object> rootMap = new HashMap<>();
			HashMap<String, Object> detailParamMap = new HashMap<>();
			detailParamMap.put("DataAreaId", defaultDataAreaId);
			rootMap.put("_dataContract", detailParamMap);
			resultMap = restD365Service.sendPostRequest(requestUrl, accessToken, rootMap);
			if (MapUtils.isNotEmpty(resultMap)) {
				logger.info("*** SyncD365ServiceImpl >> cancelSalesOrder >> resultMap: " + resultMap);
			}

		} catch (Throwable t) {
			logger.log(Level.SEVERE, t.getMessage(), t);
		}

		logger.info("*** SyncD365ServiceImpl >> cancelSalesOrder >> [END]");
		return resultMap;
	}

	@Override
	public Map<String, Object> processPurchaseOrderOnline(Map<String, Object> paramMap) throws Throwable {
		logger.info("*** SyncD365ServiceImpl >> processPurchaseOrder >> [START]");

		String orderNameParam = (String) paramMap.get("orderName");
		String apiUrl = "https://cms.potatocorner.com/springboot-cms-backend/pc/online-shop/executeDataQuery";
		String eQueryTxt = "SELECT DISTINCT so.shop_order_id as 'shopOrderId', \r\n"
				+ "		 so.order_name as 'orderName', \r\n" + "		 so.sales_order_no as 'salesOrderNo', \r\n"
				+ "		 so.so_customer_no as 'soCustomerNo', \r\n" + "		 so.app_id as 'appId', \r\n"
				+ "		 so.cancel_reason as 'cancelReason', \r\n" + "		 so.cancelled_at as 'cancelledAt', \r\n"
				+ "		 so.contact_email as 'contactEmail', \r\n" + "		 so.created_at as 'createdAt', \r\n"
				+ "		 so.current_subtotal_price as 'currentSubtotalPrice', \r\n"
				+ "		 so.current_total_price as 'currentTotalPrice', \r\n"
				+ "		 so.current_total_tax as 'currentTotalTax', \r\n"
				+ "		 so.order_number as 'orderNumber', \r\n" + "		 so.processed_at as 'processedAt', \r\n"
				+ "		 so.order_status_url as 'orderStatusUrl', \r\n"
				+ "		 so.subtotal_price as 'subtotalPrice', \r\n" + "		 so.tags as 'tags', \r\n"
				+ "		 so.total_discounts as 'totalDiscounts', \r\n"
				+ "		 so.total_line_items_price as 'totalLineItemsPrice', \r\n"
				+ "		 so.total_tax as 'totalTax', \r\n" + "		 so.currency as 'currency', \r\n"
				+ "		 so.financial_status as 'financialStatus', \r\n"
				+ "		 so.fulfillment_status as 'fulfillmentStatus', \r\n"
				+ "		 so.customer_id as 'customerId', \r\n"
				+ "		 so.default_address_id as 'defaultAddressId', \r\n" + "		 so.updated_at as 'updatedAt', \r\n"
				+ "		 so.request_delivery_date as 'requestDeliveryDate', \r\n"
				+ "		 so.so_fulfillment_status as 'soFulfillmentStatus', \r\n"
				+ "		 so.so_fulfillment_process_date as 'soFulfillmentProcessDate', \r\n"
				+ "		 so.so_sync_to_online as 'soSyncToOnline', \r\n"
				+ "		 so.so_create_date as 'soCreateDate', \r\n" + "		 so.payment_date as 'paymentDate', \r\n"
				+ "		 so.po_number as 'poNumber', \r\n" + "		 so.db_create_date as 'dbCreateDate', \r\n"
				+ "		 so.db_update_date as 'dbUpdateDate'    \r\n" + " FROM cms_db.shop_order so  \r\n"
				+ " WHERE so.order_name = '" + orderNameParam + "';";

		// List<Map<String, Object>> dbOrderList =
		// shopOrderMapper.getShopOrderWithNoLines(searchMap);
		List<Map<String, Object>> dbOrderList = queryBuilderService.getOnlineExecQuery(apiUrl, eQueryTxt);

		if (CollectionUtils.isNotEmpty(dbOrderList)) {

			Map<String, Map<String, Object>> whMaps = new HashMap<>();
			String whMapTxt = "select * from cms_db.po_warehouse_mapping";
//			List<Map<String, Object>> whMappingList = queryBuilderService.getExecQuery(queryTxt);
			List<Map<String, Object>> whMappingList = queryBuilderService.getOnlineExecQuery(apiUrl, whMapTxt);
			for (Map<String, Object> whMap : whMappingList) {
				String soWarehouseId = StringUtils.trimToEmpty((String) whMap.get("so_warehouse_id"));
				whMaps.put(soWarehouseId, whMap);
			}

			for (Map<String, Object> dbOrder : dbOrderList) {

				// long dbOrderId = (Long) dbOrder.get("shopOrderId");
				long dbOrderId = NumberUtil.getLongValue(dbOrder, "shopOrderId");
				String orderName = (String) dbOrder.get("orderName");
				String soCustomerNo = (String) dbOrder.get("soCustomerNo");
				logger.info("*** SyncD365ServiceImpl >> processPurchaseOrder >> soCustomerNo: " + soCustomerNo);

				try {
					if (whMaps.get(soCustomerNo) != null) {
						// Has PO Warehouse Mapping
						Map<String, Object> soWarehouseInfo = whMaps.get(soCustomerNo);
						Map<String, Object> eOrderMap = onlineOrderService.getOneOrderByID(dbOrderId);

						if (MapUtils.isNotEmpty(eOrderMap)) {
							Map<String, Object> poResultMap = acumaticaService.createPurchaseOrder(true, eOrderMap,
									dbOrder, soWarehouseInfo);

							if (MapUtils.isNotEmpty(poResultMap) && !poResultMap.containsKey("error")
									&& poResultMap.containsKey("OrderNbr")) {
								// SUCCESS - PO Creation
								String poNumber = StringUtils
										.trimToEmpty((String) PCDataUtil.getValue(poResultMap, "OrderNbr"));

								if (StringUtils.isNotBlank(poNumber) && poNumber.startsWith("PO")) {
									logger.info("*** SyncD365ServiceImpl >> processPurchaseOrder >> PO NUMBER: "
											+ poNumber);

									HashMap<String, Object> updateMap = new HashMap<>();
									// updateMap.put("orderName", orderName);
									// updateMap.put("poNumber", poNumber);
									// int result = shopOrderMapper.updateShopOrder(updateMap);
									String updateTxt = "update cms_db.shop_order set po_number = '" + poNumber + "' "
											+ " where order_name = '" + orderName + "'";
									boolean success = queryBuilderService.onlineExecQuery(apiUrl, updateTxt);
									if (success && StringUtils.isNotBlank(poNumber)) {
										String poTag = new StringBuilder(Constants.PURCHASE_ORDER_NO_TAG)
												.append(poNumber).toString();
										onlineOrderService.addOrderTagToCurrent(eOrderMap, poTag);
									}
								} else {
									logger.info(
											"*** SyncD365ServiceImpl >> processPurchaseOrder >> ERROR: NO PO Generated ");
								}

							} else {
								logger.info(
										"*** SyncD365ServiceImpl >> processPurchaseOrder >> ERROR: NO PO Generated ");
							}
						}
					}
				} catch (Exception e) {
					logger.log(Level.SEVERE, e.getMessage(), e);
				} catch (Throwable t) {
					logger.log(Level.SEVERE, t.getMessage(), t);
				}

			}
		}

		logger.info("*** SyncD365ServiceImpl >> processPurchaseOrder >> [END]");
		return null;
	}

	@Override
	public List<Map<String, Object>> buildSplitSOServiceRequest(HashMap<String, Object> paramMap,
			List<Map<String, Object>> dbOrderLines, Map<String, Object> eOrderMap) throws Throwable {

		logger.info("** SyncD365ServiceImpl >> buildSplitSOServiceRequest >> [START]");

		List<Map<String, Object>> soList = new ArrayList<>();

		boolean with2ndSO = D365DataUtil.has2ndSO(dbOrderLines);
		logger.info("** SyncD365ServiceImpl >> buildSplitSOServiceRequest >> with2ndSO: " + with2ndSO);
		
		String orderTags = (String)eOrderMap.get("tags");
		String orderName = (String)eOrderMap.get("name");
		boolean isStaggeredOrder = PCDataUtil.checkOrderIfStaggeredIssuance(orderTags, paramMap);
		boolean isFullPayment = PCDataUtil.isStagIssuanceFullPayment(eOrderMap);
		
		double interestRate = 0D;
		if (isStaggeredOrder && !isFullPayment) {
			Map<String, Object>  stagInterestMap =  issuanceMapper.getStaggeredInterestRate(orderName);
			if (MapUtils.isNotEmpty(stagInterestMap)) {
				interestRate = NumberUtil.getDoubleValue(stagInterestMap, "interestRate");
			}
		}
		
		if (with2ndSO) {
			// Generate TWO SO

			Map<String, String> whKeys = D365DataUtil.getWarehouseSOKeys(dbOrderLines);
			int whCtr = 1;
			for (Map.Entry<String, String> entry : whKeys.entrySet()) {

				String whCode = entry.getKey();
				String whSite = entry.getValue();
		 
				
				logger.info("** SyncD365ServiceImpl >> buildSplitSOServiceRequest >> whCode: " + whCode
						+ " *** whSite: " + whSite);

				Map<String, Object> rootMap = new LinkedHashMap<>();
				Map<String, Object> bodyMap = new LinkedHashMap<>();

				Map<String, Object> orderMap = dbOrderLines.get(0);
				String soCustomerNo = (String) orderMap.get("soCustomerNo");
				if (StringUtils.isBlank(soCustomerNo)) {
					soCustomerNo = ShopifyUtil.getSOCustomerNoByAddress(eOrderMap);
				}

				bodyMap.put("DataAreaId", paramMap.get("defaultDataAreaId"));
				bodyMap.put("Warehouse", whCode);
				bodyMap.put("Site", whSite);
				bodyMap.put("CustomerNumber", soCustomerNo);

				Date delivDate = DateUtil.getDateNowPlusTime(new Date(), Calendar.DAY_OF_YEAR, 3);
				if (orderMap.get("requestDeliveryDate") != null) {
					try {
						SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
						delivDate = sdf.parse(String.valueOf(orderMap.get("requestDeliveryDate")));
					} catch (Exception e) {
						logger.info("** SyncD365ServiceImpl >> buildSplitSOServiceRequest >> ERROR: " + e.getMessage());
						delivDate = DateUtil.getDateNowPlusTime(new Date(), Calendar.DAY_OF_YEAR, 3);
					}
				}

				bodyMap.put("DeliveryDate", DateUtil.getDateWithPattern(delivDate, "MM/dd/yyyy"));
				bodyMap.put("ReferenceCode", orderMap.get("orderName"));

				List<Map<String, Object>> soLines = new ArrayList<Map<String, Object>>();
				for (Map<String, Object> orderLine : dbOrderLines) {

					String currWHCode = (String) orderLine.get("soWarehouseCode");
					Double itemPrice = Double.valueOf(String.valueOf(orderLine.get("price")));
					String itemTitle = (String)orderLine.get("name");
					
					
					if (whCode == null || currWHCode == null)
						continue;
						
					boolean isItemEWT = PCDataUtil.isEWTItemByName(itemTitle);
					if (isItemEWT)
						continue; //SKIP
					
				
					
					if (whCode.equals(currWHCode)) {
						Map<String, Object> soLine = new LinkedHashMap<String, Object>();
						soLine.put("ItemId", orderLine.get("soItemNo"));
						soLine.put("UOM", StringUtils.upperCase((String) orderLine.get("soUom")));
						soLine.put("Qty", String.valueOf(orderLine.get("quantity")));
						logger.info("** SyncD365ServiceImpl >> buildOneSalesOrderRequest >> ORIG itemPrice: " +itemPrice);
						if (isStaggeredOrder && !isFullPayment) {
							itemPrice = NumberUtil.roundTwoDec((itemPrice + PCDataUtil.computeInterestValue(itemPrice, interestRate)));
							logger.info("** SyncD365ServiceImpl >> buildOneSalesOrderRequest >> INTEREST itemPrice: " +itemPrice);
						}
						
						soLine.put("SalesPrice", itemPrice);
						soLines.add(soLine);
					}
				}

				if (whCtr == 1) {
					Map<String, Object> delivChargeMap = D365DataUtil.getDeliveryChargeSOLine(eOrderMap);
					if (MapUtils.isNotEmpty(delivChargeMap)) {
						soLines.add(delivChargeMap);
					}
				}

				bodyMap.put("SOLines", soLines);
				rootMap.put("_dataContract", bodyMap);

				soList.add(rootMap);
				whCtr++;
			}

		} else {
			// ONLY 1 SO
			Map<String, Object> oneSOMap = this.buildOneSalesOrderRequest(paramMap, dbOrderLines, eOrderMap);
			soList.add(oneSOMap);
		}

		logger.info("** SyncD365ServiceImpl >> buildSplitSOServiceRequest >> [END]");
		return soList;
	}

	@Override
	public Map<String, Object> buildOneSalesOrderRequest(HashMap<String, Object> paramMap,
			List<Map<String, Object>> dbOrderLines, Map<String, Object> eOrderMap) throws Throwable {

		logger.info("** SyncD365ServiceImpl >> buildOneSalesOrderRequest >> [START]");

		Map<String, Object> rootMap = new LinkedHashMap<>();
		Map<String, Object> bodyMap = new LinkedHashMap<>();
		String orderTags = (String) eOrderMap.get("tags");
		String orderName = (String) eOrderMap.get("name");
		boolean isStaggeredOrder = PCDataUtil.checkOrderIfStaggeredIssuance(orderTags, paramMap);
		boolean isFullPayment = PCDataUtil.isStagIssuanceFullPayment(eOrderMap);
		
		double interestRate = 0D;
		if (isStaggeredOrder && !isFullPayment) {
			Map<String, Object>  stagInterestMap =  issuanceMapper.getStaggeredInterestRate(orderName);
			if (MapUtils.isNotEmpty(stagInterestMap)) {
				interestRate = NumberUtil.getDoubleValue(stagInterestMap, "interestRate");
				
			}
		}
		
		Map<String, Object> orderMap = dbOrderLines.get(0);
		String soCustomerNo = (String) orderMap.get("soCustomerNo");
		if (StringUtils.isBlank(soCustomerNo)) {
			soCustomerNo = ShopifyUtil.getSOCustomerNoByAddress(eOrderMap);
		}

		bodyMap.put("DataAreaId", paramMap.get("defaultDataAreaId"));
		bodyMap.put("Warehouse", orderMap.get("soWarehouseCode"));
		bodyMap.put("Site", orderMap.get("soWarehouseSite"));
		bodyMap.put("CustomerNumber", soCustomerNo);

		Date delivDate = DateUtil.getDateNowPlusTime(new Date(), Calendar.DAY_OF_YEAR, 3);
		if (orderMap.get("requestDeliveryDate") != null) {
			try {
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
				delivDate = sdf.parse(String.valueOf(orderMap.get("requestDeliveryDate")));
			} catch (Exception e) {
				logger.info("** SyncD365ServiceImpl >> buildOneSalesOrderRequest >> ERROR: " + e.getMessage());
				delivDate = DateUtil.getDateNowPlusTime(new Date(), Calendar.DAY_OF_YEAR, 3);
			}
		}

		bodyMap.put("DeliveryDate", DateUtil.getDateWithPattern(delivDate, "MM/dd/yyyy"));
		bodyMap.put("ReferenceCode", orderMap.get("orderName"));

		List<Map<String, Object>> soLines = new ArrayList<Map<String, Object>>();
		for (Map<String, Object> orderLine : dbOrderLines) {
			String itemTitle = (String)orderLine.get("name");
			Double itemPrice = Double.valueOf(String.valueOf(orderLine.get("price")));
			String currWHCode = (String) orderLine.get("soWarehouseCode");
			
			if (currWHCode == null)
				continue;
			
			boolean isItemEWT = PCDataUtil.isEWTItemByName(itemTitle);
			if (isItemEWT)
				continue; //SKIP
			
 			Map<String, Object> soLine = new LinkedHashMap<String, Object>();
			soLine.put("ItemId", orderLine.get("soItemNo"));
			soLine.put("UOM", StringUtils.upperCase((String) orderLine.get("soUom")));
			soLine.put("Qty", String.valueOf(orderLine.get("quantity")));
			logger.info("** SyncD365ServiceImpl >> buildOneSalesOrderRequest >> ORIG itemPrice: " +itemPrice);
			if (isStaggeredOrder && !isFullPayment) {
				itemPrice = NumberUtil.roundTwoDec((itemPrice + PCDataUtil.computeInterestValue(itemPrice, interestRate)));
				logger.info("** SyncD365ServiceImpl >> buildOneSalesOrderRequest >> INTEREST itemPrice: " +itemPrice);
			}
			
			soLine.put("SalesPrice", itemPrice);
			soLines.add(soLine);
 		}

		Map<String, Object> delivChargeMap = D365DataUtil.getDeliveryChargeSOLine(eOrderMap);
		if (MapUtils.isNotEmpty(delivChargeMap)) {
			soLines.add(delivChargeMap);
		}

		bodyMap.put("SOLines", soLines);
		rootMap.put("_dataContract", bodyMap);

		logger.info("** SyncD365ServiceImpl >> buildOneSalesOrderRequest >> [END]");
		return rootMap;
	}

	@Override
	public Map<String, Object> buildCustomerPaymentJournalRequest(Map<String, Object> eOrderMap,
			Map<String, Object> ewtOrderMap, Map<String, Object> paramMap) throws Throwable {
		
		logger.info("*** SyncD365ServiceImpl >> buildCustomerPaymentJournalRequest >> [START]");
		Map<String, Object> rootMap = new LinkedHashMap<String, Object>();
		Map<String, Object> detailMap = new LinkedHashMap<String, Object>();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
		String soNumbers = (String) paramMap.get("salesOrderNo"); 
		String paymentRefId = (String) paramMap.get("paymentRefId"); 
			//Multiple SO's - CIPC_SO-000057221,  CIPC_SO-000057217
		 
		try {
			
			double eTotalOrderAmt = NumberUtil.getDoubleValue(eOrderMap, "total_price");
			double ewtOrderDiscount = NumberUtil.getDoubleValue(ewtOrderMap, "orderWithDiscount");
			double ewtShipDiscount = NumberUtil.getDoubleValue(ewtOrderMap, "shipWithDiscount");
		    double ewtTotalOrderWithDiscount = NumberUtil.roundTwoDec((ewtOrderDiscount + ewtShipDiscount)); 
			double deductAmount = NumberUtil.roundTwoDec((eTotalOrderAmt - ewtTotalOrderWithDiscount));
			 
			String orderName = (String) eOrderMap.get("name");
			String customerNo = (String)paramMap.get("customerNo");
			Date phDate =  DateUtil.getDateInManilaPH();
		 	
			detailMap.put("DataAreaId", defaultDataAreaId);
			detailMap.put("AccountNum", customerNo);
			detailMap.put("PaymentDate", sdf.format(phDate));
			detailMap.put("DeductionAmount", String.valueOf(deductAmount));
			detailMap.put("SONumber", soNumbers);
			detailMap.put("ShopifyOrderNumber", orderName);
			detailMap.put("PaymentReference", paymentRefId);
			detailMap.put("PaymentMode", "ONLINEBANK");
			detailMap.put("Credit", String.valueOf(eTotalOrderAmt));
			detailMap.put("Debit", String.valueOf(ewtTotalOrderWithDiscount));
			
			rootMap.put("_dataContract", detailMap);
		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
		}
		
		logger.info("*** SyncD365ServiceImpl >> buildCustomerPaymentJournalRequest >> rootMap: " + rootMap);
		logger.info("*** SyncD365ServiceImpl >> buildCustomerPaymentJournalRequest >> [END]");
 		return rootMap;
	}

	@Override
	public Map<String, Object> processPaymentJournal(Map<String, Object> eOrderMap,
			Map<String, Object> ewtOrderMap, Map<String, Object> paramMap)
			throws Throwable {
		logger.info("*** SyncD365ServiceImpl >> processPaymentJournal >> [START]");
		try {
			
			Map<String, Object> ewtRequestMap = this.buildCustomerPaymentJournalRequest(eOrderMap, ewtOrderMap, paramMap);
			if (MapUtils.isNotEmpty(ewtRequestMap)) {
				
				String accessToken = (String) paramMap.get("accessToken");
				String orderName = (String) eOrderMap.get("name");
				if (StringUtils.isBlank(accessToken)) {
					accessToken = restD365Service.getLatestD365Token();
				}
			 	
				String requestURL = new StringBuilder(apiHostUrl)
						.append("/api/services/SPAVPCIntegrationServiceGroup/SPAVPCIntegrationService/createCustomerPaymentJournal")
						.toString();
 		 
				Map<String, Object> resultMap = restD365Service.sendPostRequest(requestURL, accessToken, ewtRequestMap, "string");
		  		if (MapUtils.isNotEmpty(resultMap)) {
		  			logger.info("*** SyncD365ServiceImpl >> processPaymentJournal >> resultMap: " + resultMap);
		  			String resultMsg = (String)resultMap.get("result");
		  			logger.info("*** SyncD365ServiceImpl >> processPaymentJournal >> resultMsg: " + resultMsg);
		  			
		  			if (StringUtils.isNotEmpty(resultMsg) 
		  					&& StringUtils.upperCase(resultMsg).contains("SUCCESS")) {
		  				String payJournalNo = PCDataUtil.getPaymentJournalNo(resultMsg);
		  				HashMap<String, Object> updateMap = new HashMap<String, Object>();
		  				updateMap.put("orderName", orderName);
		  				updateMap.put("payJournalNo", payJournalNo);
			  		 	int updateResult = shopOrderMapper.updateShopOrder(updateMap);
			  		 	if (updateResult!=0) {
			  		 		Long orderId = ShopifyUtil.getOrderId(eOrderMap);
			  		 		Map<String, Object> newOrderMap = onlineOrderService.getOneOrderByID(orderId);
			  		 		onlineOrderService.addOrderTagToCurrent(newOrderMap, "PJNO_" + payJournalNo);
		  				}
		  			}
		  			
				}
				
			}
			
		} catch (Throwable e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
		} 
		logger.info("*** SyncD365ServiceImpl >> processPaymentJournal >> [END]");
		return null;
	}

}
