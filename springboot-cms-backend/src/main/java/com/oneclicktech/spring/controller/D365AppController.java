package com.oneclicktech.spring.controller;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.auth.InvalidCredentialsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.oneclicktech.spring.domain.Constants;
import com.oneclicktech.spring.mapper.ClientTokenMapper;
import com.oneclicktech.spring.mapper.CronAuditLogMapper;
import com.oneclicktech.spring.mapper.ProductDetailMapper;
import com.oneclicktech.spring.service.OnlineCustomerService;
import com.oneclicktech.spring.service.OnlineOrderService;
import com.oneclicktech.spring.service.OnlineProductService;
import com.oneclicktech.spring.service.OnlineShopService;
import com.oneclicktech.spring.service.RestD365Service;
import com.oneclicktech.spring.service.SyncD365Service;
import com.oneclicktech.spring.util.DateUtil;
import com.oneclicktech.spring.util.NumberUtil;

//DEV SETUP (origins = "<http://13.212.234.71:8080>", maxAge = 3600, allowCredentials = "true")
//DEV SETUP (origins = "<https://pc-cms.uat.shakeys.solutions>", maxAge = 3600, allowCredentials = "true")
//PROD SETUP (origins = "<https://cms.potatocorner.com>", maxAge = 3600, allowCredentials = "true")
//LOCAL SETUP (origins = "<http://localhost:4200>", maxAge = 3600, allowCredentials = "true")
//LOCAL TOMCAT SETUP (origins = "http://localhost:4200", maxAge = 3600, allowCredentials = "true")
//@CrossOrigin(origins = "*", maxAge = 3600) 
@CrossOrigin(origins = "http://localhost:4200", maxAge = 3600, allowCredentials = "true")
@RestController
@RequestMapping("/pc/d365")
public class D365AppController {

	private static final Logger logger = Logger.getLogger("D365AppController");

	@Autowired
	RestD365Service restD365Service;

	@Autowired
	OnlineProductService onlineProductService;

	@Autowired
	OnlineCustomerService onlineCustomerService;

	@Autowired
	OnlineOrderService onlineOrderService;

	@Autowired
	ProductDetailMapper productDetailMapper;

	@Autowired
	SyncD365Service syncD365Service;

	@Autowired
	CronAuditLogMapper cronAuditLogMapper;

	@Autowired
	ClientTokenMapper clientTokenMapper;

	@Autowired
	OnlineShopService onlineShopService;

	@Value("${spavi.d365.auth-url}")
	String authUrl;

	@Value("${spavi.d365.client-id}")
	String clientId;

	@Value("${spavi.d365.client-secret}")
	String clientSecret;

	@Value("${spavi.d365.default.data-area-id}")
	String defaultDataAreaId;

	@Value("${spavi.d365.api.host-url}")
	String apiHostUrl;

	@PostConstruct
	public void init() {

	}

	@PostMapping("/getProductDetails")
	public ResponseEntity<List<Map<String, Object>>> getProductDetails(@RequestBody Map<String, Object> paramBody)
			throws Throwable {
		logger.info("** D365AppController >> getProductDetails >> [START]");
		HttpStatus httpStat = HttpStatus.OK;
		String requestUrl = new StringBuilder(apiHostUrl)
				.append("/api/services/SPAVPCIntegrationServiceGroup/SPAVPCIntegrationService/getProductDetails")
				.toString();

		String accessToken = restD365Service.getLatestD365Token();

		HashMap<String, Object> rootMap = new HashMap<>();
		HashMap<String, Object> paramMap = new HashMap<>();
		paramMap.put("DataAreaId", defaultDataAreaId);
		rootMap.put("_dataContract", paramMap);
		Map<String, Object> resultMap = restD365Service.sendPostRequest(requestUrl, accessToken, rootMap);
		if (MapUtils.isNotEmpty(resultMap)) {
			HttpStatus resultStatus = (HttpStatus) resultMap.get(Constants.RESULT_HTTP_STATUS_CODE);
			if (resultStatus.equals(HttpStatus.OK)) {
				List<Map<String, Object>> dataList = (List<Map<String, Object>>) resultMap.get(Constants.DATA_LIST);
				return new ResponseEntity<List<Map<String, Object>>>(dataList, httpStat);
			}
		}

		logger.info("** D365AppController >> getProductDetails >> [END]");
		return null;
	}

	@PostMapping("/getProductInventory")
	public ResponseEntity<List<Map<String, Object>>> getProductInventory(@RequestBody Map<String, Object> paramBody)
			throws Throwable {
		logger.info("** D365AppController >> getProductInventory >> [START]");
		HttpStatus httpStat = HttpStatus.OK;
		String requestUrl = new StringBuilder(apiHostUrl)
				.append("/api/services/SPAVPCIntegrationServiceGroup/SPAVPCIntegrationService/getProductInventoryList")
				.toString();

		String accessToken = restD365Service.getLatestD365Token();
		logger.info("accessToken: " + accessToken);

		HashMap<String, Object> rootMap = new HashMap<>();
		HashMap<String, Object> paramMap = new HashMap<>();
		paramMap.put("DataAreaId", defaultDataAreaId);
		rootMap.put("_dataContract", paramMap);
		Map<String, Object> resultMap = restD365Service.sendPostRequest(requestUrl, accessToken, rootMap);
		if (MapUtils.isNotEmpty(resultMap)) {
			HttpStatus resultStatus = (HttpStatus) resultMap.get(Constants.RESULT_HTTP_STATUS_CODE);
			if (resultStatus.equals(HttpStatus.OK)) {
				List<Map<String, Object>> dataList = (List<Map<String, Object>>) resultMap.get(Constants.DATA_LIST);
				return new ResponseEntity<List<Map<String, Object>>>(dataList, httpStat);
			}
		}

		logger.info("** D365AppController >> getProductInventory >> [END]");
		return null;
	}

	@PostMapping("/cancelSalesOrder")
	public ResponseEntity<List<Map<String, Object>>> cancelSalesOrder(@RequestBody Map<String, Object> paramBody)
			throws Throwable {
		logger.info("** D365AppController >> cancelSalesOrder >> [START]");
		HttpStatus httpStat = HttpStatus.OK;
		if (Constants.TEST_ONLY) {
			apiHostUrl = "https://cms.potatocorner.com";
		}
		
		String requestUrl = new StringBuilder(apiHostUrl)
				.append("/api/services/SPAVPCIntegrationServiceGroup/SPAVPCIntegrationService/cancelSalesOrder")
				.toString();

		String accessToken = restD365Service.getLatestD365Token();
		logger.info("accessToken: " + accessToken);
		String soNumber = (String) paramBody.get("salesOrderNo");
		HashMap<String, Object> rootMap = new HashMap<>();
		HashMap<String, Object> paramMap = new HashMap<>();
		List<String> soList = new ArrayList<>();
		if (StringUtils.isNotBlank(soNumber)) {
			if (soNumber.contains(",")) {
				soList = Arrays.asList(soNumber.split(","));
			} else {
				soList.add(soNumber);
			}
		}

		for (String soNum : soList) {
 			if (StringUtils.isNotBlank(soNum)) {
				paramMap.put("SONumber", soNum);
				paramMap.put("DataAreaId", defaultDataAreaId);
				rootMap.put("_dataContract", paramMap);
				Map<String, Object> resultMap = restD365Service.sendPostRequest(requestUrl, accessToken, rootMap);
				if (MapUtils.isNotEmpty(resultMap)) {
					HttpStatus resultStatus = (HttpStatus) resultMap.get(Constants.RESULT_HTTP_STATUS_CODE);
					if (resultStatus.equals(HttpStatus.OK)) {
						logger.info("** D365AppController >> cancelSalesOrder >> CANCELED ");
					}
				}
			}

		}

		logger.info("** D365AppController >> cancelSalesOrder >> [END]");
		return null;
	}

	@PostMapping("/getCustomerList")
	public ResponseEntity<List<Map<String, Object>>> getCustomerList(@RequestBody Map<String, Object> paramBody)
			throws Throwable {
		logger.info("** D365AppController >> getCustomerList >> [START]");
		HttpStatus httpStat = HttpStatus.OK;
		String requestUrl = new StringBuilder(apiHostUrl)
				.append("/api/services/SPAVPCIntegrationServiceGroup/SPAVPCIntegrationService/getCustomers").toString();

		String accessToken = restD365Service.getLatestD365Token();
		logger.info("accessToken: " + accessToken);

		HashMap<String, Object> rootMap = new HashMap<>();
		HashMap<String, Object> paramMap = new HashMap<>();
		paramMap.put("DataAreaId", defaultDataAreaId);
		rootMap.put("_dataContract", paramMap);
		Map<String, Object> resultMap = restD365Service.sendPostRequest(requestUrl, accessToken, rootMap);
		if (MapUtils.isNotEmpty(resultMap)) {
			HttpStatus resultStatus = (HttpStatus) resultMap.get(Constants.RESULT_HTTP_STATUS_CODE);
			if (resultStatus.equals(HttpStatus.OK)) {
				List<Map<String, Object>> dataList = (List<Map<String, Object>>) resultMap.get(Constants.DATA_LIST);
				return new ResponseEntity<List<Map<String, Object>>>(dataList, httpStat);
			}
		}

		logger.info("** D365AppController >> getCustomerList >> [END]");
		return null;
	}

	@PostMapping("/getCustomerAddressList")
	public ResponseEntity<List<Map<String, Object>>> getCustomerAddressList(@RequestBody Map<String, Object> paramBody)
			throws Throwable {
		logger.info("** D365AppController >> getCustomerAddressList >> [START]");
		HttpStatus httpStat = HttpStatus.OK;
		String requestUrl = new StringBuilder(apiHostUrl)
				.append("/api/services/SPAVPCIntegrationServiceGroup/SPAVPCIntegrationService/getCustomerAddress")
				.toString();

		String accessToken = restD365Service.getLatestD365Token();

		HashMap<String, Object> rootMap = new HashMap<>();
		HashMap<String, Object> paramMap = new HashMap<>();
		paramMap.put("DataAreaId", defaultDataAreaId);
		rootMap.put("_dataContract", paramMap);
		Map<String, Object> resultMap = restD365Service.sendPostRequest(requestUrl, accessToken, rootMap);
		if (MapUtils.isNotEmpty(resultMap)) {
			HttpStatus resultStatus = (HttpStatus) resultMap.get(Constants.RESULT_HTTP_STATUS_CODE);
			if (resultStatus.equals(HttpStatus.OK)) {
				List<Map<String, Object>> dataList = (List<Map<String, Object>>) resultMap.get(Constants.DATA_LIST);
				return new ResponseEntity<List<Map<String, Object>>>(dataList, httpStat);
			}
		}

		logger.info("** D365AppController >> getCustomerAddressList >> [END]");
		return null;
	}

	@PostMapping("/getWarehouseList")
	public ResponseEntity<List<Map<String, Object>>> getWarehouseList(@RequestBody Map<String, Object> paramBody)
			throws Throwable {
		logger.info("** D365AppController >> getCustomerAddressList >> [START]");
		HttpStatus httpStat = HttpStatus.OK;
		String requestUrl = new StringBuilder(apiHostUrl)
				.append("/api/services/SPAVPCIntegrationServiceGroup/SPAVPCIntegrationService/getWarehouseList")
				.toString();

		String accessToken = restD365Service.getLatestD365Token();

		HashMap<String, Object> rootMap = new HashMap<>();
		HashMap<String, Object> paramMap = new HashMap<>();
		paramMap.put("DataAreaId", defaultDataAreaId);
		rootMap.put("_dataContract", paramMap);
		Map<String, Object> resultMap = restD365Service.sendPostRequest(requestUrl, accessToken, rootMap);
		if (MapUtils.isNotEmpty(resultMap)) {
			HttpStatus resultStatus = (HttpStatus) resultMap.get(Constants.RESULT_HTTP_STATUS_CODE);
			if (resultStatus.equals(HttpStatus.OK)) {
				List<Map<String, Object>> dataList = (List<Map<String, Object>>) resultMap.get(Constants.DATA_LIST);
				return new ResponseEntity<List<Map<String, Object>>>(dataList, httpStat);
			}
		}

		logger.info("** D365AppController >> getWarehouseList >> [END]");
		return null;
	}

	@PostMapping("/getSalesOrderDetails")
	public ResponseEntity<Map<String, Object>> getSalesOrderDetails(@RequestBody Map<String, Object> paramBody)
			throws Throwable {
		logger.info("** D365AppController >> getSalesOrderDetails >> [START]");
		HttpStatus httpStat = HttpStatus.OK;
		String requestUrl = new StringBuilder(apiHostUrl)
				.append("/api/services/SPAVPCIntegrationServiceGroup/SPAVPCIntegrationService/getSalesOrderDetails")
				.toString();

		String accessToken = restD365Service.getLatestD365Token();
		HashMap<String, Object> rootMap = new HashMap<>();
		HashMap<String, Object> paramMap = new HashMap<>();
		String salesOrderNo = (String) paramBody.get("salesOrderNo");
		paramMap.put("DataAreaId", defaultDataAreaId);
		paramMap.put("SONumber", salesOrderNo);

		rootMap.put("_dataContract", paramMap);
		Map<String, Object> resultMap = restD365Service.sendPostRequest(requestUrl, accessToken, rootMap);
		if (MapUtils.isNotEmpty(resultMap)) {
			HttpStatus resultStatus = (HttpStatus) resultMap.get(Constants.RESULT_HTTP_STATUS_CODE);
			if (resultStatus.equals(HttpStatus.OK)) {
				List<Map<String, Object>> dataList = (List<Map<String, Object>>) resultMap.get(Constants.DATA_LIST);
				if (CollectionUtils.isNotEmpty(dataList)) {
					return new ResponseEntity<Map<String, Object>>(dataList.get(0), httpStat);
				}

			}
		}

		logger.info("** D365AppController >> getProductDetails >> [END]");
		return null;
	}

	@SuppressWarnings("unused")
	@PostMapping("/createSalesOrder")
	public ResponseEntity<Map<String, Object>> createSalesOrder(@RequestBody Map<String, Object> paramBody)
			throws Throwable {
		logger.info("** D365AppController >> createSalesOrder >> [START]");
		logger.info("paramBody: " + paramBody);
		Map<String, Object> resultMap = syncD365Service.syncSalesOrderDataToDB(paramBody);
		logger.info("** D365AppController >> createSalesOrder >> [END]");
		return null;
	}

	@PostMapping("/getCustomers")
	public ResponseEntity<List<Map<String, Object>>> getCustomers(@RequestBody Map<String, Object> paramBody)
			throws Throwable {

		return null;
	}

	@PostMapping("/getSalesOrders")
	public ResponseEntity<List<Map<String, Object>>> getSalesOrders(@RequestBody Map<String, Object> paramBody)
			throws Throwable {

		logger.info("** D365AppController >> getSalesOrders >> [START]");
		HttpStatus httpStat = HttpStatus.OK;
		String requestUrl = new StringBuilder(apiHostUrl)
				.append("/api/services/SPAVPCIntegrationServiceGroup/SPAVPCIntegrationService/getSalesOrderDetailsList")
				.toString();

		String accessToken = restD365Service.getLatestD365Token();

		SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy");

		Date fromDate = new Date();
		Date toDate = DateUtil.getDateNowPlusTime(Calendar.DAY_OF_YEAR, 10);
		HashMap<String, Object> rootMap = new HashMap<>();
		HashMap<String, Object> paramMap = new HashMap<>();
		paramMap.put("DataAreaId", defaultDataAreaId);
		// DELIVERY DATE fromDate - toDate
		paramMap.put("DateFrom", sdf.format(fromDate));
		paramMap.put("DateTo", sdf.format(toDate));
		// Set the ROOT Map
		rootMap.put("_dataContract", paramMap);
		Map<String, Object> resultMap = restD365Service.sendPostRequest(requestUrl, accessToken, rootMap);
		if (MapUtils.isNotEmpty(resultMap)) {
			HttpStatus resultStatus = (HttpStatus) resultMap.get(Constants.RESULT_HTTP_STATUS_CODE);
			if (resultStatus.equals(HttpStatus.OK)) {
				List<Map<String, Object>> dataList = (List<Map<String, Object>>) resultMap.get(Constants.DATA_LIST);
				for (Map<String, Object> rowMap : dataList) {
					List<Map<String, Object>> soLines = (List<Map<String, Object>>) rowMap.get("SoLines");
					rowMap.put("soLineCount", soLines.size());
				}
				logger.info("** D365AppController >> getSalesOrders >> [SUCCESS]");
				return new ResponseEntity<List<Map<String, Object>>>(dataList, httpStat);
			}
		}

		logger.info("** D365AppController >> getSalesOrders >> [END]");
		return null;
	}

	@SuppressWarnings({ "unchecked" })
	@PostMapping("/syncSalesOrders")
	public ResponseEntity<Map<String, Object>> syncSalesOrders(@RequestBody Map<String, Object> paramBody)
			throws Throwable {
		HttpStatus httpStat = HttpStatus.OK;
		logger.info("** D365AppController >> syncSalesOrders >> [START]");
		String accessToken = (String) paramBody.get("accessToken");
		logger.info("** D365AppController >> accessToken: " + accessToken);
		Map<String, Object> retResultMap = new HashMap<>();
		HashMap<String, Object> auditMap = new HashMap<>();
		auditMap.put("processType", "CREATE_SO");

		Map<String, Object> paramMap = new HashMap<>();
		if (StringUtils.isBlank(accessToken)) {
			auditMap.put("lastAuditLog",
					"InvalidCredentialsException(\"Invalid Access Token / Access Token is empty\"");
			cronAuditLogMapper.updateCronAuditLogByProcess(auditMap);
			throw new InvalidCredentialsException("Invalid Access Token / Access Token is empty");
		}

		StringBuilder sbAuditLog = new StringBuilder();
		List<Map<String, Object>> shopOrderList = onlineOrderService.getOrderList(paramMap);
		for (Map<String, Object> orderMap : shopOrderList) {
			boolean hasSONumber = false;
			String orderName = (String) orderMap.get("name");
			Long orderId = NumberUtil.getLongValue(orderMap, "id");
			String soNum = "";
			List<Map<String, Object>> noteAttribs = (List<Map<String, Object>>) orderMap.get("note_attributes");
			if (CollectionUtils.isNotEmpty(noteAttribs)) {
				for (Map<String, Object> noteAttrMap : noteAttribs) {
					String key = (String) noteAttrMap.get("name");
					String value = (String) noteAttrMap.get("value");
					if (key.equals("SalesOrderNo") && StringUtils.isNotBlank(value)) {
						soNum = new String(value);
						hasSONumber = true;
						break;
					}
				}

			} else {
				noteAttribs = new ArrayList<>();
			}

			sbAuditLog.append("** D365AppController >> orderName: " + orderName + " -> hasSONumber: " + hasSONumber
					+ "-> SO:" + soNum).append("\r\n");
			logger.info("** D365AppController >> orderName: " + orderName + " -> hasSONumber: " + hasSONumber + "-> SO:"
					+ soNum);
			retResultMap.put("statusResult", "SUCCESS");
			retResultMap.put("lastAuditLog", sbAuditLog.toString());

			if (!hasSONumber) {
				// NO Sales Order YET -> SYNC Online Order to D365 Sales Order
				Map<String, Object> rootMap = onlineOrderService.buildSalesOrderMapRequest(orderMap);
				logger.info("** D365AppController >> syncSalesOrders >> rootMap: " + rootMap);
				String requestUrl = new StringBuilder(apiHostUrl)
						.append("/api/services/SPAVPCIntegrationServiceGroup/SPAVPCIntegrationService/createSalesOrder")
						.toString();

				Map<String, Object> resultMap = restD365Service.sendPostRequest(requestUrl, accessToken, rootMap);
				if (MapUtils.isNotEmpty(resultMap)) {
					HttpStatus resultStatus = (HttpStatus) resultMap.get(Constants.RESULT_HTTP_STATUS_CODE);
					if (resultStatus.equals(HttpStatus.OK)) {
						List<Map<String, Object>> dataList = (List<Map<String, Object>>) resultMap
								.get(Constants.DATA_LIST);
						Map<String, Object> outputMap = null;
						if (CollectionUtils.isNotEmpty(dataList)) {

							for (Map<String, Object> dataMap : dataList) {
								outputMap = dataMap;
								String resultMsg = (String) dataMap.get("Result");
								if (resultMsg.equalsIgnoreCase("SUCCESS")) {
									String soNo = (String) dataMap.get("SalesOrderNumber");

									logger.info("** D365AppController >> syncSalesOrders >> orderName: " + orderName);
									Map<String, Object> rootParam = new HashMap<>();
									Map<String, Object> nameValParam = new HashMap<>();
									nameValParam.put("name", "SalesOrderNo");
									nameValParam.put("value", soNo);
									noteAttribs.add(nameValParam);

									Map<String, Object> noteAttrParam = new HashMap<>();
									noteAttrParam.put("id", orderId);
									noteAttrParam.put("note_attributes", noteAttribs);
									rootParam.put("orderId", orderId);
									rootParam.put(Constants.ORDER, noteAttrParam);
									onlineOrderService.updateOrderNotes(rootParam);
								} else {
									// throw new ResponseStatusException(HttpStatus.BAD_REQUEST, resultMsg);
									// PRINT/EMAIL ERROR
									httpStat = HttpStatus.BAD_REQUEST;
									retResultMap.put("statusResult", "ERROR");
									retResultMap.put("lastAuditLog", resultMsg);
								}
							}
						}

					}
				}
			}

		}

		auditMap.put("lastAuditLog", sbAuditLog.toString());
		cronAuditLogMapper.updateCronAuditLogByProcess(auditMap);

		logger.info("** D365AppController >> syncSalesOrders >> [END]");
		return new ResponseEntity<Map<String, Object>>(retResultMap, httpStat);
	}

	@PostMapping("/syncAccessToken")
	public ResponseEntity<Map<String, Object>> syncAccessToken(@RequestBody Map<String, Object> paramBody)
			throws Throwable {
		logger.info("** D365AppController >> syncAccessToken >> [START]");
		HttpStatus httpStat = HttpStatus.OK;

		String accessToken = (String) paramBody.get("accessToken");
		logger.info("accessToken: " + accessToken);
		if (StringUtils.isBlank(accessToken)) {
			throw new InvalidCredentialsException("Invalid Access Token / Access Token is empty");
		}

		HashMap<String, Object> paramMap = new HashMap<>();
		paramMap.put("accessToken", accessToken);
		paramMap.put("tokenType", "D365");
		int result = clientTokenMapper.updateClientToken(paramMap);
		Map<String, Object> resultMap = new HashMap<>();
		if (result != 0) {
			resultMap.put("statusResult", "success");
			resultMap.put("lastAuditLog", "success");
		}
		logger.info("** D365AppController >> syncAccessToken >> result: " + result);
		logger.info("** D365AppController >> syncAccessToken >> resultMap: " + resultMap.toString());
		logger.info("** D365AppController >> syncAccessToken >> [END]");
		return new ResponseEntity<Map<String, Object>>(resultMap, httpStat);
	}

	@PostMapping("/syncProductDataToDB")
	public ResponseEntity<Map<String, Object>> syncProductDataToDB(@RequestBody Map<String, Object> paramBody)
			throws Throwable {
		logger.info("** D365AppController >> syncProductDataToDB >> [START]");
		HttpStatus httpStat = HttpStatus.OK;
		logger.info("** D365AppController >> syncProductDataToDB >> D365 Product to Local DB..... ");
		Map<String, Object> syncResult = syncD365Service.syncProductDataToDB(paramBody);
		logger.info("** D365AppController >> syncProductDataToDB >> BACKUP Tables..... ");
		onlineProductService.backupProductDBTable(new HashMap<>());
		logger.info("** D365AppController >> syncProductDataToDB >> CLEANSE Online & DB Products..... ");
		onlineProductService.cleansOnlineProductAndDB(new HashMap<>());
		logger.info("** D365AppController >> syncProductDataToDB >> SYNC = Local DB to Online..... ");
		onlineShopService.syncLocalProductToOnline(new HashMap<>());
		logger.info("** D365AppController >> syncProductDataToDB >> [END]");
		return new ResponseEntity<Map<String, Object>>(syncResult, httpStat);
	}

	@PostMapping("/syncCustomerDataToDB")
	public ResponseEntity<Map<String, Object>> syncCustomerDataToDB(@RequestBody Map<String, Object> paramBody)
			throws Throwable {
		logger.info("** D365AppController >> syncCustomerDataToDB >> [START]");
		HttpStatus httpStat = HttpStatus.OK;
		logger.info("** D365AppController >> syncProductDataToDB >> D365 Product to Local DB..... ");
		Map<String, Object> syncResult = syncD365Service.syncCustomerDataToDB(paramBody);
		logger.info("** D365AppController >> syncCustomerDataToDB >> BACKUP Tables ..... ");
		onlineCustomerService.backupCustomerDBTable(new HashMap<>());

		logger.info("** D365AppController >> syncCustomerDataToDB >> CLEANSE ..... ");
		onlineCustomerService.cleansOnlineCustomerAndDB(new HashMap<>());
		Map<String, Object> custBody = new HashMap<>();
		logger.info("** D365AppController >> syncCustomerDataToDB >> SYNC = Local DB to Online  ..... ");
		custBody.put("withEmail", "true");
		// paramBody.put("withPhone", "true");
		onlineShopService.syncLocalCustomerToOnlineByEmail(custBody);

		logger.info("** D365AppController >> syncCustomerDataToDB >> [END]");
		return new ResponseEntity<Map<String, Object>>(syncResult, httpStat);
	}

	@PostMapping("/syncWarehouseDataToDB")
	public ResponseEntity<Map<String, Object>> syncWarehouseDataToDB(@RequestBody Map<String, Object> paramBody)
			throws Throwable {
		logger.info("** D365AppController >> syncWarehouseDataToDB >> [START]");
		HttpStatus httpStat = HttpStatus.OK;
		Map<String, Object> paramMap = new HashMap<>();

		Map<String, Object> resultMap = syncD365Service.syncWarehouseDataToDB(paramMap);

		logger.info("** D365AppController >> syncWarehouseDataToDB >> [END]");
		return new ResponseEntity<Map<String, Object>>(resultMap, httpStat);
	}

	@PostMapping("/syncSalesOrderDataToDB")
	public ResponseEntity<Map<String, Object>> syncSalesOrderDataToDB(@RequestBody Map<String, Object> paramBody)
			throws Throwable {
		logger.info("** D365AppController >> syncSalesOrderDataToDB >> [START]");
		HttpStatus httpStat = HttpStatus.OK;
		Map<String, Object> syncResult = syncD365Service.syncSalesOrderDataToDB(paramBody);
		logger.info("** D365AppController >> syncSalesOrderDataToDB >> [END]");
		return new ResponseEntity<Map<String, Object>>(syncResult, httpStat);
	}

}
