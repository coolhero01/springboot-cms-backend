package com.oneclicktech.spring.service.impl;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.oneclicktech.spring.domain.Constants;
import com.oneclicktech.spring.mapper.ShopOrderMapper;
import com.oneclicktech.spring.service.AcumaticaService;
import com.oneclicktech.spring.service.EmailService;
import com.oneclicktech.spring.service.QueryBuilderService;
import com.oneclicktech.spring.service.RestGenericService;
import com.oneclicktech.spring.util.PCDataUtil;
import com.oneclicktech.spring.util.ShopifyUtil;

@Service
public class AcumaticaServiceImpl implements AcumaticaService {

	private static final Logger logger = Logger.getLogger("AcumaticaServiceImpl");

	@Autowired
	RestGenericService restGenericService;

	@Autowired
	EmailService emailService;
	
	@Autowired
	QueryBuilderService queryBuilderService;

	
	@Value("${pc.acumatica.host}")
	String pcAcumHost;

	@Value("${pc.acumatica.username}")
	String pcAcumUser;

	@Value("${pc.acumatica.password}")
	String pcAcumPassword;

	@Value("${pc.acumatica.tenant}")
	String pcAcumTenant;

	@Value("${pc.acumatica.api.endpoint.name}")
	String pcAcumEndpointName;

	@Value("${pc.acumatica.api.endpoint.version}")
	String pcAcumEndpointVersion;

	@Value("${pc.acumatica.po.vendor-id}")
	String poDefaultVendorId;

	@Value("${pc.acumatica.po.owner-id}")
	String poDefaultOwnerId;

	@Value("${pc.acumatica.po.default.subacct}")
	String poDefaultSubAcct;

	@Autowired
	ShopOrderMapper shopOrderMapper;

	CloseableHttpClient httpclient;

	@PostConstruct
	public void init() {
		// this.logonToSite();
		logger.info("** AcumaticaServiceImpl >>  [INIT] ");
		httpclient = HttpClients.createDefault();

	}

	@Override
	public boolean logonToSite() {
		logger.info("** AcumaticaServiceImpl >> logonToSite >> [START]");
		CloseableHttpResponse response = null;
		try {
			String requestUrl = new StringBuilder("https://").append(pcAcumHost).append("/entity/auth/login")
					.toString();
			Map<String, Object> paramMap = new LinkedHashMap<String, Object>();
			paramMap.put("name", pcAcumUser);
			paramMap.put("password", pcAcumPassword);
			paramMap.put("company", pcAcumTenant);

			Gson gson = new Gson();
			String jsonStr = gson.toJson(paramMap);
			final StringEntity entity = new StringEntity(jsonStr);

			final HttpPost httpPost = new HttpPost(requestUrl);

			httpPost.setEntity(entity);
			httpPost.setHeader("Accept", "application/json");
			httpPost.setHeader("Content-type", "application/json");

			response = httpclient.execute(httpPost);
			logger.info("** AcumaticaServiceImpl >> logonToSite >> response : " + response);

		} catch (Throwable t) {
			logger.log(Level.SEVERE, t.getMessage(), t);
		} finally {
			this.closeHttp(response);
		}
		logger.info("** AcumaticaServiceImpl >> logonToSite >> [END]");
		return false;
	}

	@Override
	public List<Map<String, Object>> getStockItemList(boolean runLogin, HashMap<String, Object> paramMap,
			boolean isStockItem) {
		logger.info("** AcumaticaServiceImpl >> getStockItemList >> [START]");
		CloseableHttpResponse responseBody = null;
		List<Map<String, Object>> resultList = new ArrayList<>();
		try {
			if (runLogin)
				this.logonToSite();

			String limitCount = StringUtils.trimToEmpty((String) paramMap.get("limit"));
			if (StringUtils.isNotBlank(limitCount)) {
				limitCount = "?$top=" + limitCount;
			}
			String itemType = "StockItem";
			if (!isStockItem) {
				itemType = "NonStockItem";
			}

			String requestUrl = new StringBuilder("https://").append(pcAcumHost).append("/entity/")
					.append(pcAcumEndpointName).append("/").append(pcAcumEndpointVersion).append("/").append(itemType)
					.append(limitCount).toString();
			final HttpGet httpGet = new HttpGet(requestUrl);

			responseBody = (CloseableHttpResponse) httpclient.execute(httpGet);

			HttpEntity entity = responseBody.getEntity();
			String resultContent = EntityUtils.toString(entity);
			Gson gson = new Gson();
			resultList = gson.fromJson(resultContent, List.class);

			logger.info("** AcumaticaServiceImpl >> getStockItemList >> resultList: " + resultList.size());

		} catch (Throwable t) {
			logger.log(Level.SEVERE, t.getMessage(), t);
		} finally {
			this.closeHttp(responseBody);
		}
		logger.info("** AcumaticaServiceImpl >> getStockItemList >> [END]");
		return resultList;
	}

	@Override
	public List<Map<String, Object>> getWarehouseList(boolean runLogin, HashMap<String, Object> paramMap) {
		logger.info("** AcumaticaServiceImpl >> getWarehouseList >> [START]");
		CloseableHttpResponse responseBody = null;
		List<Map<String, Object>> resultList = new ArrayList<>();
		try {
			if (runLogin)
				this.logonToSite();

			String requestUrl = new StringBuilder("https://").append(pcAcumHost).append("/entity/")
					.append(pcAcumEndpointName).append("/").append(pcAcumEndpointVersion).append("/Warehouse")
					.toString();

			final HttpGet httpGet = new HttpGet(requestUrl);

			responseBody = (CloseableHttpResponse) httpclient.execute(httpGet);

			HttpEntity entity = responseBody.getEntity();
			String resultContent = EntityUtils.toString(entity);
			Gson gson = new Gson();
			resultList = gson.fromJson(resultContent, List.class);

			logger.info("** AcumaticaServiceImpl >> getWarehouseList >> resultList: " + resultList.size());

		} catch (Throwable t) {
			logger.log(Level.SEVERE, t.getMessage(), t);
		} finally {
			this.closeHttp(responseBody);
		}
		logger.info("** AcumaticaServiceImpl >> getWarehouseList >> [END]");
		return resultList;
	}

	@Override
	public Map<String, Object> getStockItemByID(boolean runLogin, String inventoryId, boolean isStockItem) {
		logger.info("** AcumaticaServiceImpl >> getStockItemByID >> [START]");
		CloseableHttpResponse responseBody = null;
		Map<String, Object> resultMap = new LinkedHashMap<>();
		try {
			if (runLogin)
				this.logonToSite();

			String itemType = "StockItem";
			if (!isStockItem) {
				itemType = "NonStockItem";
			}

			String requestUrl = new StringBuilder("https://").append(pcAcumHost).append("/entity/")
					.append(pcAcumEndpointName).append("/").append(pcAcumEndpointVersion).append("/").append(itemType)
					.append("/").append(inventoryId).toString();

			final HttpGet httpGet = new HttpGet(requestUrl);

			responseBody = (CloseableHttpResponse) httpclient.execute(httpGet);

			HttpEntity entity = responseBody.getEntity();
			String resultContent = EntityUtils.toString(entity);
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			resultMap = gson.fromJson(resultContent, Map.class);
			logger.info("** AcumaticaServiceImpl >> getStockItemByID >>resultMap: " + resultMap);

		} catch (Throwable t) {
			logger.log(Level.SEVERE, t.getMessage(), t);
		} finally {
			this.closeHttp(responseBody);
		}
		logger.info("** AcumaticaServiceImpl >> getStockItemByID >> [END]");
		return resultMap;
	}

	@Override
	public Map<String, Object> getPurchaseOrderByID(boolean runLogin, String poNumber) {
		logger.info("** AcumaticaServiceImpl >> getPurchaseOrderByID >> [START]");
		CloseableHttpResponse responseBody = null;
		Map<String, Object> resultMap = new LinkedHashMap<>();
		try {
			if (runLogin)
				this.logonToSite();

			String requestUrl = new StringBuilder("https://").append(pcAcumHost).append("/entity/")
					.append(pcAcumEndpointName).append("/").append(pcAcumEndpointVersion).append("/PurchaseOrder/RO/")
					.append(poNumber).append("?$expand=Details").toString();

			final HttpGet httpGet = new HttpGet(requestUrl);

			responseBody = (CloseableHttpResponse) httpclient.execute(httpGet);

			HttpEntity entity = responseBody.getEntity();
			String resultContent = EntityUtils.toString(entity);
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			resultMap = gson.fromJson(resultContent, Map.class);
			if (MapUtils.isNotEmpty(resultMap)) {
				logger.info("** AcumaticaServiceImpl >> getPurchaseOrderByID >>resultMap: " + resultMap);

			}

		} catch (Throwable t) {
			logger.log(Level.SEVERE, t.getMessage(), t);
		} finally {
			this.closeHttp(responseBody);
		}
		logger.info("** AcumaticaServiceImpl >> getPurchaseOrderByID >> [END]");
		return resultMap;
	}

	@Override
	public Map<String, Object> getWarehouseInfoByDesc(boolean runLogin, String storeName) {
		logger.info("** AcumaticaServiceImpl >> getWarehouseInfoByDesc >> [START]");
		CloseableHttpResponse responseBody = null;
		Map<String, Object> resultMap = new LinkedHashMap<>();
		try {
			if (runLogin)
				this.logonToSite();

			String requestUrl = new StringBuilder("https://").append(pcAcumHost).append("/entity/")
					.append(pcAcumEndpointName).append("/").append(pcAcumEndpointVersion)
					.append("/Warehouse?$filter=Description eq '").append(storeName).append("'").toString();

			final HttpGet httpGet = new HttpGet(requestUrl);

			responseBody = (CloseableHttpResponse) httpclient.execute(httpGet);

			HttpEntity entity = responseBody.getEntity();
			String resultContent = EntityUtils.toString(entity);
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			resultMap = gson.fromJson(resultContent, Map.class);
			if (MapUtils.isNotEmpty(resultMap)) {
				logger.info("** AcumaticaServiceImpl >> getWarehouseInfoByDesc >> resultMap: " + resultMap);

			}

		} catch (Throwable t) {
			logger.log(Level.SEVERE, t.getMessage(), t);
		} finally {
			this.closeHttp(responseBody);
		}
		logger.info("** AcumaticaServiceImpl >> getWarehouseInfoByDesc >> [END]");
		return resultMap;
	}

	@Override
	public Map<String, Object> getWarehouseInfoByID(boolean runLogin, String warehouseId) {
		logger.info("** AcumaticaServiceImpl >> getWarehouseInfoByID >> [START]");
		CloseableHttpResponse responseBody = null;
		Map<String, Object> resultMap = new LinkedHashMap<>();
		try {
			if (runLogin)
				this.logonToSite();

			String requestUrl = new StringBuilder("https://").append(pcAcumHost).append("/entity/")
					.append(pcAcumEndpointName).append("/").append(pcAcumEndpointVersion).append("/Warehouse/")
					.append(warehouseId).toString();

			final HttpGet httpGet = new HttpGet(requestUrl);

			responseBody = (CloseableHttpResponse) httpclient.execute(httpGet);

			HttpEntity entity = responseBody.getEntity();
			String resultContent = EntityUtils.toString(entity);
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			resultMap = gson.fromJson(resultContent, Map.class);
			if (MapUtils.isNotEmpty(resultMap)) {
				logger.info("** AcumaticaServiceImpl >> getWarehouseInfoByID >> resultMap: " + resultMap);

			}

		} catch (Throwable t) {
			logger.log(Level.SEVERE, t.getMessage(), t);
		} finally {
			this.closeHttp(responseBody);
		}
		logger.info("** AcumaticaServiceImpl >> getWarehouseInfoByDesc >> [END]");
		return resultMap;
	}

	public Map<String, Object> createPurchaseOrder(boolean runLogin, Map<String, Object> eOrderMap) {
		logger.info("** AcumaticaServiceImpl >> createPurchaseOrder >> [START]");
		String orderName = (String) eOrderMap.get("name");
		HashMap<String, Object> searchMap = new HashMap<>();
		searchMap.put("orderName", orderName);
		List<Map<String, Object>> dbOrderList = shopOrderMapper.getShopOrderList(searchMap);

		Map<String, Map<String, Object>> dbOrderLines = new HashMap<>();
		for (Map<String, Object> dbOrder : dbOrderList) {
			String soItemNo = (String) dbOrder.get("soItemNo");
			dbOrderLines.put(soItemNo, dbOrder);
		}

		CloseableHttpResponse responseBody = null;
		Map<String, Object> resultMap = new LinkedHashMap<>();
		try {
			if (runLogin)
				this.logonToSite();

			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			// Map<String, Object> poRequestMap = this.buildMainPORequest(eOrderMap,
			// dbOrderLines, dbOrderList);
			Map<String, Object> poRequestMap = null;
			String jsonTxt = gson.toJson(poRequestMap, Map.class);

			logger.info("***********************************************************");
			logger.info(jsonTxt);
			logger.info("***********************************************************");

			String requestUrl = new StringBuilder("https://").append(pcAcumHost).append("/entity/")
					.append(pcAcumEndpointName).append("/").append(pcAcumEndpointVersion).append("/PurchaseOrder")
					.toString();

			final HttpPut httpPut = new HttpPut(requestUrl);
			httpPut.setHeader("Accept", "application/json");
			httpPut.setHeader("Content-type", "application/json");
			StringEntity stringEntity = new StringEntity(jsonTxt);
			httpPut.setEntity(stringEntity);

			responseBody = (CloseableHttpResponse) httpclient.execute(httpPut);

			HttpEntity entity = responseBody.getEntity();
			String resultContent = EntityUtils.toString(entity);
			resultMap = gson.fromJson(resultContent, Map.class);
			if (MapUtils.isNotEmpty(resultMap)) {
				logger.info("** AcumaticaServiceImpl >> createPurchaseOrder >> resultMap: " + resultMap);
			}

		} catch (Throwable t) {
			logger.log(Level.SEVERE, t.getMessage(), t);
		} finally {
			this.closeHttp(responseBody);
		}
		logger.info("** AcumaticaServiceImpl >> createPurchaseOrder >> [END]");
		return resultMap;
	}

	private Map<String, Map<String, Object>> buildMainPOByOnline(Map<String, Object> eOrderMap,
			Map<String, Object> dbOrderMap, Map<String, Object> soWarehouseInfo) {

		Map<String, Map<String, Object>> mainMap = new LinkedHashMap<>();
		Map<String, Object> rootMap = new LinkedHashMap<>();

		String orderName = (String) eOrderMap.get("name");
		String poWarehouseName = (String) soWarehouseInfo.get("po_warehouse_name");
		String currencyID = "PHP";
		boolean Hold = true;
		String location = "MAIN";
		String promisedOn =  "2023-05-23"; // Delivery Date = "value": "2023-04-14T00:00:00+00:00"
//		String deliveryDate = (String) dbOrderMap.get("requestDeliveryDate");
//		if (deliveryDate != null) {
//			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
//			promisedOn = sdf.format(deliveryDate);
//		}

		String status = "On Hold";
		// String description = "from PC ONLINE ORDER " + orderName;
		StringBuilder description = new StringBuilder("PC ONLINE ORDER ").append(orderName);
		String type = "Normal";
		if (StringUtils.isNotBlank(poWarehouseName)) {
			description.append(" (").append(poWarehouseName).append(")");
		}

		rootMap.put("CurrencyID", PCDataUtil.getValueKey(currencyID));

		rootMap.put("Hold", PCDataUtil.getValueKey(Hold));

		rootMap.put("Location", PCDataUtil.getValueKey(location));
		rootMap.put("Owner", PCDataUtil.getValueKey(poDefaultOwnerId));
		rootMap.put("PromisedOn", PCDataUtil.getValueKey(promisedOn));
		rootMap.put("Status", PCDataUtil.getValueKey(status));
		rootMap.put("Type", PCDataUtil.getValueKey(type));
		rootMap.put("VendorID", PCDataUtil.getValueKey(poDefaultVendorId));

		// String warehouseId = "00010"; // "value": "Robinsons Galleria LG/F"
		// CustomerID "value": "00010"
		String warehouseId = (String) soWarehouseInfo.get("po_warehouse_id");

		String poAccrualSubAcct = poDefaultSubAcct;
		String poAccrualAcct = "0000000000";
		String subAccount = poDefaultSubAcct;
		String account = "0000000000"; 

//		Map<String, Object> poWHInfo = this.getWarehouseInfoByID(false, warehouseId);
//		String poAccrualSubAcct = null;
//		String poAccrualAcct = null;
//		String subAccount = null; 
//		String account = null;
//		if (MapUtils.isNotEmpty(poWHInfo)) {
//			poAccrualAcct = String.valueOf(PCDataUtil.getValue(poWHInfo, "POAccrualAccount"));
//			poAccrualSubAcct = String.valueOf(PCDataUtil.getValue(poWHInfo, "POAccrualSubaccount"));
//			subAccount = String.valueOf(PCDataUtil.getValue(poWHInfo, "SalesSubaccount"));
//			account = String.valueOf(PCDataUtil.getValue(poWHInfo, "SalesAccount"));
//		}

		List<Map<String, Object>> detailList = new ArrayList<Map<String, Object>>();

		List<Map<String, Object>> lineItems = (List<Map<String, Object>>) eOrderMap.get("line_items");
		Map<String, Object> errorMap = new HashMap<>();
		StringBuilder errorMsg = new StringBuilder();
		int lineCtr = 0;
		for (Map<String, Object> itemLine : lineItems) {

			logger.info("** AcumaticaServiceImpl >> itemLine: " + itemLine.toString());
			String soItemNo = "";

			try {

				Map<String, Object> detailMap = new LinkedHashMap<>();
				int qty = (int) Double.parseDouble(String.valueOf(itemLine.get("quantity")));
				soItemNo = ShopifyUtil.getD365ItemIdFromTitle((String) itemLine.get("title"));
				if (StringUtils.isEmpty(soItemNo)) {
					soItemNo = (String) itemLine.get("sku");
			 	}

				String lineType = null;
				Double unitPrice = Double.parseDouble((String) itemLine.get("price"));
				String lineDesc = (String) itemLine.get("title");
				  
				String itemUOM = null;

				Map<String, Object> acStockItem = this.getItemInfo(false, soItemNo);
				logger.info("** AcumaticaServiceImpl >> acStockItem: " + acStockItem);

				if (MapUtils.isNotEmpty(acStockItem)) {
					itemUOM = StringUtils
							.trimToEmpty((String) this.getValue(acStockItem, "PurchaseUnit", "PurchaseUOM"));
					lineType = StringUtils.trimToEmpty((String) acStockItem.get("lineType"));
				}

				detailMap.put("BranchID", PCDataUtil.getValueKey(Constants.PO_DEFAULT_BRANCH_ID));
				detailMap.put("Cancelled", PCDataUtil.getValueKey(false));
				detailMap.put("Completed", PCDataUtil.getValueKey(false));
				detailMap.put("InventoryID", PCDataUtil.getValueKey(soItemNo));
				detailMap.put("LineDescription", PCDataUtil.getValueKey(lineDesc));
				detailMap.put("LineType", PCDataUtil.getValueKey(lineType));
				detailMap.put("OrderQty", PCDataUtil.getValueKey(qty));
				detailMap.put("UnitCost", PCDataUtil.getValueKey(unitPrice));
				detailMap.put("UOM", PCDataUtil.getValueKey(itemUOM));
				detailMap.put("WarehouseID", PCDataUtil.getValueKey(warehouseId));
				detailMap.put("AccrualSubaccount", PCDataUtil.getValueKey(poAccrualSubAcct));
				detailMap.put("AccrualAccount", PCDataUtil.getValueKey(poAccrualAcct));

				if (StringUtils.isNotBlank(lineType)
						&& StringUtils.trimToEmpty(lineType).equalsIgnoreCase("Non-Stock")) {
					detailMap.put("Subaccount", PCDataUtil.getValueKey(subAccount));
					detailMap.put("Account", PCDataUtil.getValueKey(account));
				}  

				detailList.add(detailMap);
				lineCtr++;
			} catch (Exception e) {
				logger.info("** AcumaticaServiceImpl >> ERROR [" + orderName + "] itemLine: " + itemLine.toString());
				description.append("-ITEM ERROR");
				errorMsg.append("ERROR found in (").append(soItemNo).append(") ").append(" \r\n");
				logger.log(Level.SEVERE, e.getMessage(), e);
			}

		}

		errorMap.put("errorMsg", errorMsg.toString());

		rootMap.put("Description", PCDataUtil.getValueKey(description.toString()));
		rootMap.put("Details", detailList);

		mainMap.put("errorMap", errorMap);
		mainMap.put("poRootMap", rootMap);
		return mainMap;
	}

	private Map<String, Map<String, Object>> buildMainPOByWithSubAcct(Map<String, Object> eOrderMap,
			Map<String, Object> dbOrderMap, Map<String, Object> soWarehouseInfo) {

		Map<String, Map<String, Object>> mainMap = new LinkedHashMap<>();
		Map<String, Object> rootMap = new LinkedHashMap<>();

		String orderName = (String) eOrderMap.get("name");
		String poWarehouseName = (String) soWarehouseInfo.get("po_warehouse_name");
		String currencyID = "PHP";
		boolean Hold = true;
		String location = "MAIN";
		String promisedOn = null; // Delivery Date = "value": "2023-04-14T00:00:00+00:00"
		Date deliveryDate = (Date) dbOrderMap.get("requestDeliveryDate");
		if (deliveryDate != null) {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			promisedOn = sdf.format(deliveryDate);
		}

		String status = "On Hold";
		// String description = "from PC ONLINE ORDER " + orderName;
		StringBuilder description = new StringBuilder("PC ONLINE ORDER ").append(orderName);
		String type = "Normal";
		if (StringUtils.isNotBlank(poWarehouseName)) {
			description.append(" (").append(poWarehouseName).append(")");
		}

		if (Constants.TEST_ONLY) {
			description.append(" (TEST ONLY) ");
		}

		rootMap.put("CurrencyID", PCDataUtil.getValueKey(currencyID));

		rootMap.put("Hold", PCDataUtil.getValueKey(Hold));

		rootMap.put("Location", PCDataUtil.getValueKey(location));
		rootMap.put("Owner", PCDataUtil.getValueKey(poDefaultOwnerId));
		rootMap.put("PromisedOn", PCDataUtil.getValueKey(promisedOn));
		rootMap.put("Status", PCDataUtil.getValueKey(status));
		rootMap.put("Type", PCDataUtil.getValueKey(type));
		rootMap.put("VendorID", PCDataUtil.getValueKey(poDefaultVendorId));

		// String warehouseId = "00010"; // "value": "Robinsons Galleria LG/F"
		// CustomerID "value": "00010"
		String warehouseId = (String) soWarehouseInfo.get("po_warehouse_id");

		String poAccrualSubAcct = poDefaultSubAcct;
		String poAccrualAcct = "0000000000";
		String subAccount = poDefaultSubAcct;
		String account = "0000000000"; 

//	 	Map<String, Object> poWHInfo = this.getWarehouseInfoByID(false, warehouseId);
//		String poAccrualSubAcct = null;
//		String poAccrualAcct = null;
//		String inventorySubAcct = null;
//	 	if (MapUtils.isNotEmpty(poWHInfo)) {
//	 		poAccrualAcct = String.valueOf(PCDataUtil.getValue(poWHInfo, "POAccrualAccount"));
//	 		poAccrualSubAcct = String.valueOf(PCDataUtil.getValue(poWHInfo, "POAccrualSubaccount"));
//		    inventorySubAcct = String.valueOf(PCDataUtil.getValue(poWHInfo, "InventorySubaccount"));
//		}

		List<Map<String, Object>> detailList = new ArrayList<Map<String, Object>>();

		List<Map<String, Object>> lineItems = (List<Map<String, Object>>) eOrderMap.get("line_items");
		Map<String, Object> errorMap = new HashMap<>();
		StringBuilder errorMsg = new StringBuilder();
		int lineCtr = 0;
		for (Map<String, Object> itemLine : lineItems) {

			logger.info("** AcumaticaServiceImpl >> itemLine: " + itemLine.toString());
			String soItemNo = "";

			try {

				Map<String, Object> detailMap = new LinkedHashMap<>();
				int qty = (int) Double.parseDouble(String.valueOf(itemLine.get("quantity")));
				soItemNo = ShopifyUtil.getD365ItemIdFromTitle((String) itemLine.get("title"));
				if (StringUtils.isEmpty(soItemNo)) {
					soItemNo = (String) itemLine.get("sku");
				}

				if (Constants.TEST_ONLY) {
					if (lineCtr == 1) {
						throw new NoSuchFieldException("ERROR: NoSuchFieldException");
					}
				}

				String lineType = null;
				Double unitPrice = Double.parseDouble((String) itemLine.get("price"));
				String lineDesc = PCDataUtil.replaceInvalidChar((String) itemLine.get("title"), " ");
 				String itemUOM = null;

				Map<String, Object> acStockItem = this.getItemInfo(false, soItemNo);
				logger.info("** AcumaticaServiceImpl >> acStockItem: " + acStockItem);

				if (MapUtils.isNotEmpty(acStockItem)) {
					itemUOM = StringUtils
							.trimToEmpty((String) this.getValue(acStockItem, "PurchaseUnit", "PurchaseUOM"));
					lineType = StringUtils.trimToEmpty((String) acStockItem.get("lineType"));
				}

				detailMap.put("BranchID", PCDataUtil.getValueKey(Constants.PO_DEFAULT_BRANCH_ID));
				detailMap.put("Cancelled", PCDataUtil.getValueKey(false));
				detailMap.put("Completed", PCDataUtil.getValueKey(false));
				detailMap.put("InventoryID", PCDataUtil.getValueKey(soItemNo));
				detailMap.put("LineDescription", PCDataUtil.getValueKey(lineDesc));
				detailMap.put("LineType", PCDataUtil.getValueKey(lineType));
				detailMap.put("OrderQty", PCDataUtil.getValueKey(qty));
				detailMap.put("UnitCost", PCDataUtil.getValueKey(unitPrice));
				detailMap.put("UOM", PCDataUtil.getValueKey(itemUOM));
				detailMap.put("WarehouseID", PCDataUtil.getValueKey(warehouseId));
				detailMap.put("AccrualSubaccount", PCDataUtil.getValueKey(poAccrualSubAcct));
				detailMap.put("AccrualAccount", PCDataUtil.getValueKey(poAccrualAcct));

				if (StringUtils.isNotBlank(lineType)
						&& StringUtils.trimToEmpty(lineType).equalsIgnoreCase("Non-Stock")) {
					detailMap.put("Subaccount", PCDataUtil.getValueKey(subAccount));
					detailMap.put("Account", PCDataUtil.getValueKey(account));
				}

				detailList.add(detailMap);
				lineCtr++;
			} catch (Exception e) {
				logger.info("** AcumaticaServiceImpl >> ERROR [" + orderName + "] itemLine: " + itemLine.toString());
				description.append("-ITEM ERROR");
				errorMsg.append("ERROR found in (").append(soItemNo).append(") ").append(" \r\n");
				logger.log(Level.SEVERE, e.getMessage(), e);
			}

		}

		errorMap.put("errorMsg", errorMsg.toString());

		rootMap.put("Description", PCDataUtil.getValueKey(description.toString()));
		rootMap.put("Details", detailList);

		mainMap.put("errorMap", errorMap);
		mainMap.put("poRootMap", rootMap);
		return mainMap;
	}

	private Map<String, Map<String, Object>> buildMainPOByOrderNoSubAcct(Map<String, Object> eOrderMap,
			Map<String, Object> dbOrderMap, Map<String, Object> soWarehouseInfo) {

		Map<String, Map<String, Object>> mainMap = new LinkedHashMap<>();
		Map<String, Object> rootMap = new LinkedHashMap<>();

		String orderName = (String) eOrderMap.get("name");
		String poWarehouseName = (String) soWarehouseInfo.get("po_warehouse_name");
		String currencyID = "PHP";
		boolean Hold = true;
		String location = "MAIN";
		String promisedOn = null; // Delivery Date = "value": "2023-04-14T00:00:00+00:00"
		Date deliveryDate = (Date) dbOrderMap.get("requestDeliveryDate");
		if (deliveryDate != null) {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			promisedOn = sdf.format(deliveryDate);
		}

		String status = "On Hold";
		// String description = "from PC ONLINE ORDER " + orderName;
		StringBuilder description = new StringBuilder("PC ONLINE ORDER ").append(orderName);
		String type = "Normal";
		if (StringUtils.isNotBlank(poWarehouseName)) {
			description.append(" (").append(poWarehouseName).append(")");
		}

		if (Constants.TEST_ONLY) {
			description.append(" (TEST ONLY) ");
		}

		rootMap.put("CurrencyID", PCDataUtil.getValueKey(currencyID));

		rootMap.put("Hold", PCDataUtil.getValueKey(Hold));

		rootMap.put("Location", PCDataUtil.getValueKey(location));
		rootMap.put("Owner", PCDataUtil.getValueKey(poDefaultOwnerId));
		rootMap.put("PromisedOn", PCDataUtil.getValueKey(promisedOn));
		rootMap.put("Status", PCDataUtil.getValueKey(status));
		rootMap.put("Type", PCDataUtil.getValueKey(type));
		rootMap.put("VendorID", PCDataUtil.getValueKey(poDefaultVendorId));

		// String warehouseId = "00010"; // "value": "Robinsons Galleria LG/F"
		// CustomerID "value": "00010"
		String warehouseId = (String) soWarehouseInfo.get("po_warehouse_id");

		List<Map<String, Object>> detailList = new ArrayList<Map<String, Object>>();

		List<Map<String, Object>> lineItems = (List<Map<String, Object>>) eOrderMap.get("line_items");
		Map<String, Object> errorMap = new HashMap<>();
		StringBuilder errorMsg = new StringBuilder();
		int lineCtr = 0;
		for (Map<String, Object> itemLine : lineItems) {

			logger.info("** AcumaticaServiceImpl >> itemLine: " + itemLine.toString());
			String soItemNo = "";
			try {

				Map<String, Object> detailMap = new LinkedHashMap<>();
				int qty = (int) Double.parseDouble(String.valueOf(itemLine.get("quantity")));
				soItemNo = ShopifyUtil.getD365ItemIdFromTitle((String) itemLine.get("title"));
				if (StringUtils.isEmpty(soItemNo)) {
					soItemNo = (String) itemLine.get("sku");
				}

				if (Constants.TEST_ONLY) {
					if (lineCtr == 1) {
						throw new NoSuchFieldException("ERROR: NoSuchFieldException");
					}
				}

				String lineType = null;
				Double unitPrice = Double.parseDouble((String) itemLine.get("price"));
				String lineDesc = (String) itemLine.get("title");

				String itemUOM = null;

				Map<String, Object> acStockItem = this.getItemInfo(false, soItemNo);
				logger.info("** AcumaticaServiceImpl >> acStockItem: " + acStockItem);

				if (MapUtils.isNotEmpty(acStockItem)) {
					itemUOM = StringUtils
							.trimToEmpty((String) this.getValue(acStockItem, "PurchaseUnit", "PurchaseUOM"));
					lineType = StringUtils.trimToEmpty((String) acStockItem.get("lineType"));
				}

				detailMap.put("BranchID", PCDataUtil.getValueKey(Constants.PO_DEFAULT_BRANCH_ID));
				detailMap.put("Cancelled", PCDataUtil.getValueKey(false));
				detailMap.put("Completed", PCDataUtil.getValueKey(false));
				detailMap.put("InventoryID", PCDataUtil.getValueKey(soItemNo));
				detailMap.put("LineDescription", PCDataUtil.getValueKey(lineDesc));
				detailMap.put("LineType", PCDataUtil.getValueKey(lineType));
				detailMap.put("OrderQty", PCDataUtil.getValueKey(qty));
				detailMap.put("UnitCost", PCDataUtil.getValueKey(unitPrice));
				detailMap.put("UOM", PCDataUtil.getValueKey(itemUOM));
				detailMap.put("WarehouseID", PCDataUtil.getValueKey(warehouseId));
//				detailMap.put("AccrualSubaccount", PCDataUtil.getValueKey(poAccrualSubAcct));
//				detailMap.put("AccrualAccount", PCDataUtil.getValueKey(poAccrualAcct));
//				  
//				if (StringUtils.isNotBlank(lineType) 
//						&& StringUtils.trimToEmpty(lineType).equalsIgnoreCase("Non-Stock")) { 
//					detailMap.put("Subaccount", PCDataUtil.getValueKey(inventorySubAcct));
//				}

				detailList.add(detailMap);
				lineCtr++;
			} catch (Exception e) {
				logger.info("** AcumaticaServiceImpl >> ERROR [" + orderName + "] itemLine: " + itemLine.toString());
				description.append("-ITEM ERROR");
				errorMsg.append("ERROR found in (").append(soItemNo).append(") ").append(" \r\n");
				logger.log(Level.SEVERE, e.getMessage(), e);
			}

		}

		errorMap.put("errorMsg", errorMsg.toString());

		rootMap.put("Description", PCDataUtil.getValueKey(description.toString()));
		rootMap.put("Details", detailList);

		mainMap.put("errorMap", errorMap);
		mainMap.put("poRootMap", rootMap);
		return mainMap;
	}

//	private Map<String, Object> buildMainPORequest(Map<String, Object> eOrderMap,
//			Map<String, Map<String, Object>> dbOrderLines, List<Map<String, Object>> dbOrderList) {
//
//		Map<String, Object> rootMap = new LinkedHashMap<>();
//
//		String orderName = (String) eOrderMap.get("name");
//		String currencyID = "PHP";
//		boolean Hold = true;
//		String location = "MAIN";
//		String promisedOn = null; // TODO: Delivery Date = "value": "2023-04-14T00:00:00+00:00"
//		String status = "On Hold";
//		String description = "from PC ONLINE ORDER " + orderName;
//		String type = "Normal";
//
//		if (Constants.TEST_ONLY) {
//			description += " (TEST ONLY - Dont Process!)";
//		}
//
//		rootMap.put("CurrencyID", PCDataUtil.getValueKey(currencyID));
//		rootMap.put("Description", PCDataUtil.getValueKey(description));
//		rootMap.put("Hold", PCDataUtil.getValueKey(Hold));
//
//		rootMap.put("Location", PCDataUtil.getValueKey(location));
//		rootMap.put("Owner", PCDataUtil.getValueKey(poDefaultOwnerId));
//		rootMap.put("PromisedOn", promisedOn);
//		rootMap.put("Status", PCDataUtil.getValueKey(status));
//		rootMap.put("Type", PCDataUtil.getValueKey(type));
//		rootMap.put("VendorID", PCDataUtil.getValueKey(poDefaultVendorId));
//
//		List<Map<String, Object>> detailList = new ArrayList<Map<String, Object>>();
//
//		List<Map<String, Object>> lineItems = (List<Map<String, Object>>) eOrderMap.get("line_items");
//		for (Map<String, Object> itemLine : lineItems) {
//
//			Map<String, Object> detailMap = new LinkedHashMap<>();
//			int qty = (int) Double.parseDouble(String.valueOf(itemLine.get("quantity")));
//			String soItemNo = (String) itemLine.get("sku"); //
//
//			String lineType = "Goods for SO";
//			Double unitPrice = Double.parseDouble((String) itemLine.get("price"));
//			String lineDesc = (String) itemLine.get("title");
//			String warehouseId = "00010"; // TODO: "value": "Robinsons Galleria LG/F" CustomerID "value": "00010"
//
//			String itemUOM = null;
//			Map<String, Object> dbOrderLine = dbOrderLines.get(soItemNo);
//			if (MapUtils.isNotEmpty(dbOrderLine)) {
//				logger.info("** AcumaticaServiceImpl >> dbOrderLine: " + dbOrderLine);
//				itemUOM = StringUtils.upperCase((String) dbOrderLine.get("soUom"));
//			}
//
//			Map<String, Object> acStockItem = this.getItemInfo(false, soItemNo);
//			logger.info("** AcumaticaServiceImpl >> acStockItem: " + acStockItem);
//			if (MapUtils.isNotEmpty(acStockItem)) {
//				itemUOM = StringUtils.trimToEmpty((String) this.getValue(acStockItem, "PurchaseUnit", "PurchaseUOM"));
//			}
//
//			detailMap.put("BranchID", PCDataUtil.getValueKey(Constants.PO_DEFAULT_BRANCH_ID));
//			detailMap.put("Cancelled", PCDataUtil.getValueKey(false));
//			detailMap.put("Completed", PCDataUtil.getValueKey(false));
//			detailMap.put("InventoryID", PCDataUtil.getValueKey(soItemNo));
//			detailMap.put("LineDescription", PCDataUtil.getValueKey(lineDesc));
//			detailMap.put("LineType", PCDataUtil.getValueKey(lineType));
//			detailMap.put("OrderQty", PCDataUtil.getValueKey(qty));
//			detailMap.put("UnitCost", PCDataUtil.getValueKey(unitPrice));
//			detailMap.put("UOM", PCDataUtil.getValueKey(itemUOM));
//			detailMap.put("WarehouseID", PCDataUtil.getValueKey(warehouseId));
//
//			detailList.add(detailMap);
//		}
//
//		rootMap.put("Details", detailList);
//		return rootMap;
//	}
//	

	@Override
	public Map<String, Object> createPurchaseOrder(boolean runLogin, Map<String, Object> eOrderMap,
			Map<String, Object> dbOrderMap, Map<String, Object> soWarehouseInfo) {
		logger.info("** AcumaticaServiceImpl >> createPurchaseOrder >> [START]");
		String orderName = (String) eOrderMap.get("name");
		logger.info("** AcumaticaServiceImpl >> createPurchaseOrder >> orderName: " + orderName);
		CloseableHttpResponse responseBody = null;
		Map<String, Object> resultMap = new LinkedHashMap<>();

		try {
			if (runLogin)
				this.logonToSite();

			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			Map<String, Map<String, Object>> poRequestMap = null;
			if (Constants.TEST_ONLY) {
				poRequestMap = this.buildMainPOByOnline(eOrderMap, dbOrderMap, soWarehouseInfo);
			} else {
				poRequestMap = this.buildMainPOByWithSubAcct(eOrderMap, dbOrderMap, soWarehouseInfo);
			}

			Map<String, Object> poRootMap = (Map<String, Object>) poRequestMap.get("poRootMap");

			String jsonTxt = gson.toJson(poRootMap, Map.class);

			logger.info("***********************************************************");
			logger.info(jsonTxt);
			logger.info("***********************************************************");

			boolean mailSent = emailService.sendEmail(null, Constants.MAIL_RECIPIENTS,
					"PC - PO Creation for Order: " + orderName + "  [REQUEST] ", jsonTxt, null, null, null);

			String requestUrl = new StringBuilder("https://").append(pcAcumHost).append("/entity/")
					.append(pcAcumEndpointName).append("/").append(pcAcumEndpointVersion).append("/PurchaseOrder")
					.toString();

			final HttpPut httpPut = new HttpPut(requestUrl);
			httpPut.setHeader("Accept", "application/json");
			httpPut.setHeader("Content-type", "application/json");
			StringEntity stringEntity = new StringEntity(jsonTxt);
			httpPut.setEntity(stringEntity);

			responseBody = (CloseableHttpResponse) httpclient.execute(httpPut);

			HttpEntity entity = responseBody.getEntity();
			String resultContent = EntityUtils.toString(entity);
			resultMap = gson.fromJson(resultContent, Map.class);
			String poNumber = "";
			boolean WITH_PO_ERROR = false;
			if (MapUtils.isNotEmpty(resultMap)) {
				logger.info("** AcumaticaServiceImpl >> createPurchaseOrder >> resultMap: " + resultMap);
				poNumber = StringUtils.trimToEmpty((String) PCDataUtil.getValue(resultMap, "OrderNbr"));
			}

			if (MapUtils.isNotEmpty(poRequestMap)) {
				Map<String, Object> errorMap = (Map<String, Object>) poRequestMap.get("errorMap");
				String detailErrorMsg = (String) errorMap.get("errorMsg");
				String response = "SUCCESS";
				if (StringUtils.isNotBlank(detailErrorMsg) && detailErrorMsg.contains("ERROR")) {
					response = "WITH ERROR";
					WITH_PO_ERROR = true;
				}

				if (StringUtils.isBlank(poNumber)) {
					response = "WITH ERROR";
					WITH_PO_ERROR = true;
				} else {
					if (StringUtils.isNotBlank(poNumber) && !poNumber.startsWith("PO")) {
						response = "WITH ERROR";
						poNumber = "";
						WITH_PO_ERROR = true;
					}
				}

				String errorMsg = gson.toJson(errorMap);
				String responseMsg = gson.toJson(resultMap);
				String resultMsg = new StringBuilder("").append("ONLINE ORDER : ").append(orderName).append("\r\n")
						.append("PURCHASE ORDER : ").append(poNumber).append("\r\n").append(errorMsg).append("\r\n")
						.append("******************************************************\r\n")
						.append("******************************************************\r\n").append(responseMsg)
						.toString();

				emailService.sendEmail(null, Constants.MAIL_RECIPIENTS,
						"PC - PO Creation for Order: " + orderName + " [" + response + "] ", resultMsg, null, null,
						null);
				
			} else {
				emailService.sendEmail(null, Constants.MAIL_RECIPIENTS,
						"PC - PO Creation for Order: " + orderName + " [PO ERROR] ",
						"Error encountered during PO Generation", null, null, null);
				WITH_PO_ERROR = true;
			}
			
			if (WITH_PO_ERROR) {
				String queryTxt = "update cms_db.shop_order set po_number = 'PO_ERROR' where order_name  = '"+orderName+"'";
				queryBuilderService.execQuery(queryTxt);
			}
			
			
		} catch (Exception e) {

			emailService.sendEmail(null, Constants.MAIL_RECIPIENTS,
					"PC - PO Creation for Order: " + orderName + " [PO ERROR] ",
					"Error encountered during PO Generation", null, null, null);

			logger.log(Level.SEVERE, e.getMessage(), e);

		} catch (Throwable t) {
			logger.log(Level.SEVERE, t.getMessage(), t);
		} finally {
			this.closeHttp(responseBody);
		}

		logger.info("** AcumaticaServiceImpl >> createPurchaseOrder >> [END]");
		return resultMap;
	}

	private Map<String, Object> getItemInfo(boolean runLogin, String inventoryId) {
		Map<String, Object> resultMap = this.getStockItemByID(runLogin, inventoryId, true);
		resultMap.put("lineType", "Goods for IN");
		if (MapUtils.isNotEmpty(resultMap) && resultMap.containsKey("exceptionMessage")) {
			resultMap = this.getStockItemByID(runLogin, inventoryId, false);
			resultMap.put("lineType", "Non-Stock");
		}
		return resultMap;
	}

	private String getRealUOMValue(String uom) {
		String poUOM = uom;
		switch (uom) {
		case "PC":
			poUOM = "PACK";
			break;

		default:
			poUOM = uom;
			break;
		}

		return poUOM;
	}

	@SuppressWarnings("unchecked")
	private Object getValue(Map<String, Object> objMap, String key, String key2) {
		Map<String, Object> valMap = null;
		if (objMap.containsKey(key)) {
			valMap = (Map<String, Object>) objMap.get(key);
		} else {
			valMap = (Map<String, Object>) objMap.get(key2);
		}
		return valMap.get("value");
	}

	private void closeHttp(CloseableHttpResponse response) {
		try {
			if (response != null)
				response.close();
		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
		}
	}

}
