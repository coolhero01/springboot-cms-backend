package com.oneclicktech.spring.service.impl;

import static java.util.stream.Collectors.joining;

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

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.oneclicktech.spring.domain.Constants;
import com.oneclicktech.spring.mapper.ShopOrderMapper;
import com.oneclicktech.spring.service.OnlineCustomerService;
import com.oneclicktech.spring.service.OnlineOrderService;
import com.oneclicktech.spring.service.OnlineProductService;
import com.oneclicktech.spring.service.RestGenericService;
import com.oneclicktech.spring.service.RestTemplateService;
import com.oneclicktech.spring.util.DateUtil;
import com.oneclicktech.spring.util.HelperUtil;
import com.oneclicktech.spring.util.NumberUtil;
import com.oneclicktech.spring.util.PCDataUtil;
import com.oneclicktech.spring.util.ShopifyUtil;

import net.bytebuddy.agent.builder.AgentBuilder.FallbackStrategy.Simple;

@Service
public class OnlineOrderServiceImpl implements OnlineOrderService {

	private static final Logger logger = Logger.getLogger("OnlineOrderServiceImpl");

	@Autowired
	RestTemplateService restTemplateService;

	@Autowired
	OnlineProductService onlineProductService;

	@Autowired
	OnlineCustomerService onlineCustomerService;

	@Autowired
	RestGenericService restGenericService;

	@Autowired
	ShopOrderMapper shopOrderMapper;

	@Value("${pc.shopify.app.hosturl}")
	String hostUrl;

	@Value("${pc.shopify.app.webhook.version}")
	String apiVersion;

	@Value("${pc.shopify.app.rowlimit}")
	String rowLimit;

	@Value("${spavi.d365.default.data-area-id}")
	String defaultDataAreaId;

	@Value("${cms.app.host-url}")
	String cmsAppHostUrl;

	@SuppressWarnings("unchecked")
	@Override
	public List<Map<String, Object>> getOrderList(Map<String, Object> paramMap) throws Throwable {
		logger.info("** OnlineOrderServiceImpl >> getOrderList >> [START]");

		Map<String, Object> reqParamMap = new HashMap<String, Object>();
		reqParamMap.putAll(paramMap);

		String prodRequestUrl = new StringBuilder("/admin/api/").append(apiVersion).append("/orders.json").toString();

		prodRequestUrl = reqParamMap.keySet().stream()
				.map(key -> key + "=" + ShopifyUtil.encodeValue(String.valueOf(reqParamMap.get(key))))
				.collect(joining("&", prodRequestUrl.concat("?"), ""));

		Map<String, Object> resultMap = restTemplateService.sendGetRequest(prodRequestUrl,
				new HashMap<String, Object>());

		if (MapUtils.isNotEmpty(resultMap) && resultMap.containsKey(Constants.ORDERS)) {
			List<Map<String, Object>> dataList = (List<Map<String, Object>>) resultMap.get(Constants.ORDERS);
			logger.info("** OnlineOrderServiceImpl >> getOrderList >> dataList: " + dataList.size());
			for (Map<String, Object> rowMap : dataList) {

				String orderName = (String) rowMap.get("name");
				String orderTag = (String) rowMap.get("tags");
				rowMap.put("orderId", rowMap.get("id"));
				rowMap.put("tblId", rowMap.get("id"));

				String fullfillStatus = (String) rowMap.get("fulfillment_status");
				if (StringUtils.isNotBlank(fullfillStatus)) {
					rowMap.put("fullfillStatus", fullfillStatus);
				} else {
					rowMap.put("fullfillStatus", "Unfulfilled");
				}

				if (rowMap.get("cancelled_at") != null) {
					rowMap.put("fullfillStatus", "CANCELLED");
					rowMap.put("orderStatus", "CANCELLED");
				}

				rowMap.put("accessToken", null);
				Map<String, Object> customerMap = (Map<String, Object>) rowMap.get("customer");
				if (MapUtils.isNotEmpty(customerMap)) {
					rowMap.putAll(customerMap);
					rowMap.put("customerName", new StringBuilder((String) customerMap.get("first_name")).append(" ")
							.append((String) customerMap.get("last_name")).toString());
				}

				rowMap.put("soCustomerNo", ShopifyUtil.getSOCustomerNoByAddress(rowMap, customerMap));

				List<Map<String, Object>> lineItems = (List<Map<String, Object>>) rowMap.get("line_items");
				int totalItems = 0;
				for (Map<String, Object> itemLine : lineItems) {
					int qty = (int) Double.parseDouble(String.valueOf(itemLine.get("quantity")));
					totalItems += qty;
				}

				rowMap.put("totalItems", totalItems);
				rowMap.put("totalShippingCharge", 0.0);
				List<Map<String, Object>> shipLines = (List<Map<String, Object>>) rowMap.get("shipping_lines");
				if (CollectionUtils.isNotEmpty(shipLines)) {
					Double totalShipCharge = 0D;
					for (Map<String, Object> shipLine : shipLines) {
						Double shipCharge = Double.parseDouble(String.valueOf(shipLine.get("price")));
						totalShipCharge += shipCharge;
						logger.info("** OnlineOrderServiceImpl >> shipCharge:" + shipCharge);
					}
					rowMap.put("totalShippingCharge", totalShipCharge);
				}

				rowMap.put("salesOrderNo", null);
				List<Map<String, Object>> noteAttribs = (List<Map<String, Object>>) rowMap.get("note_attributes");
				if (CollectionUtils.isNotEmpty(noteAttribs)) {
					for (Map<String, Object> noteAttMap : noteAttribs) {
						String key = (String) noteAttMap.get("name");
						switch (key) {
						case "request-deliver-date":
							rowMap.put("requestDeliveryDate", noteAttMap.get("value"));
							break;
						}
					}
				}

				Map<String, Object> tagMap = ShopifyUtil.getOrderTagAsMap(orderTag);
				if (tagMap.containsKey("salesOrderNo")) {
					rowMap.put("salesOrderNo", tagMap.get("salesOrderNo"));
				}

				logger.info("** OnlineOrderServiceImpl >> ORDER NAME:" + orderName);

				rowMap.put("tags", orderTag);
			}
			return dataList;
		}
		logger.info("** OnlineOrderServiceImpl >> getOrderList >> [END]");
		return null;
	}

	private Map<String, Object> mapValuesToOnlineOrder(Map<String, Object> eOrderMap) {
		
		logger.info("** OnlineOrderServiceImpl >> mapValuesToOnlineOrder >> [START]");
		String orderName = (String) eOrderMap.get("name");
		String orderTag = (String) eOrderMap.get("tags");
		eOrderMap.put("orderId", eOrderMap.get("id"));
		eOrderMap.put("tblId", eOrderMap.get("id"));

		String fullfillStatus = (String) eOrderMap.get("fulfillment_status");
		if (StringUtils.isNotBlank(fullfillStatus)) {
			eOrderMap.put("fullfillStatus", fullfillStatus);
		} else {
			eOrderMap.put("fullfillStatus", "Unfulfilled");
		}

		if (eOrderMap.get("cancelled_at") != null) {
			eOrderMap.put("fullfillStatus", "CANCELLED");
			eOrderMap.put("orderStatus", "CANCELLED");
		}

		eOrderMap.put("accessToken", null);
		Map<String, Object> customerMap = (Map<String, Object>) eOrderMap.get("customer");
		if (MapUtils.isNotEmpty(customerMap)) {
			eOrderMap.putAll(customerMap);
			eOrderMap.put("customerName", new StringBuilder((String) customerMap.get("first_name")).append(" ")
					.append((String) customerMap.get("last_name")).toString());
		}

		eOrderMap.put("soCustomerNo", ShopifyUtil.getSOCustomerNoByAddress(eOrderMap, customerMap));

		List<Map<String, Object>> lineItems = (List<Map<String, Object>>) eOrderMap.get("line_items");
		int totalItems = 0;
		for (Map<String, Object> itemLine : lineItems) {
			int qty = (int) Double.parseDouble(String.valueOf(itemLine.get("quantity")));
			totalItems += qty;
		}

		eOrderMap.put("totalItems", totalItems);
		eOrderMap.put("totalShippingCharge", 0.0);
		List<Map<String, Object>> shipLines = (List<Map<String, Object>>) eOrderMap.get("shipping_lines");
		if (CollectionUtils.isNotEmpty(shipLines)) {
			Double totalShipCharge = 0D;
			for (Map<String, Object> shipLine : shipLines) {
				Double shipCharge = Double.parseDouble(String.valueOf(shipLine.get("price")));
				totalShipCharge += shipCharge;
				logger.info("** OnlineOrderServiceImpl >> shipCharge:" + shipCharge);
			}
			eOrderMap.put("totalShippingCharge", totalShipCharge);
		}

		eOrderMap.put("salesOrderNo", null);
		List<Map<String, Object>> noteAttribs = (List<Map<String, Object>>) eOrderMap.get("note_attributes");
		if (CollectionUtils.isNotEmpty(noteAttribs)) {
			for (Map<String, Object> noteAttMap : noteAttribs) {
				String key = (String) noteAttMap.get("name");
				switch (key) {
				case "request-deliver-date":
					eOrderMap.put("requestDeliveryDate", noteAttMap.get("value"));
					break;
				}
			}
		}

		Map<String, Object> tagMap = ShopifyUtil.getOrderTagAsMap(orderTag);
		if (tagMap.containsKey("salesOrderNo")) {
			eOrderMap.put("salesOrderNo", tagMap.get("salesOrderNo"));
		}

		logger.info("** OnlineOrderServiceImpl >> ORDER NAME:" + orderName);

		eOrderMap.put("tags", orderTag);
		logger.info("** OnlineOrderServiceImpl >> mapValuesToOnlineOrder >> [END]");
		
		return eOrderMap;

	}

	@Override
	public Map<String, Object> saveOnlineOrderByHookService(Map<String, Object> eOrderMap) throws Throwable {

		logger.info("** OnlineOrderServiceImpl >> saveOnlineOrderByHookService >> [START]");
		Map<String, Object> newOrderMap = this.mapValuesToOnlineOrder(eOrderMap);

		try {
			 
			Long orderId = ShopifyUtil.getOrderId(newOrderMap);
			String soCustomerNo = (String) newOrderMap.get("soCustomerNo");
			String orderName = (String) newOrderMap.get("name");
			String financialStatus = (String) newOrderMap.get("financial_status");
			String fulfillStatus = (String) newOrderMap.get("fulfillment_status");
			String orderTags = (String) newOrderMap.get("tags");
			
			HashMap<String, Object> searchMap = new HashMap<>();
			searchMap.put("orderName", orderName);
			List<Map<String, Object>> dbOrders = shopOrderMapper.getShopOrderWithNoLines(searchMap);
			Map<String, Object> dbOrder = null;
			if (CollectionUtils.isNotEmpty(dbOrders)) {
				dbOrder = dbOrders.get(0);
				financialStatus = StringUtils.trimToEmpty((String) dbOrder.get("financialStatus"));
				fulfillStatus = StringUtils.trimToEmpty((String) dbOrder.get("fulfillmentStatus"));
			}

			boolean skipOrder = PCDataUtil.skipFulfilledForBlankData(fulfillStatus, financialStatus, soCustomerNo, orderTags);
			if (skipOrder)
				return null;
			 

			logger.info("*** OnlineOrderServiceImpl >> saveOnlineOrderByHookService >> orderName: " + orderName);
			logger.info("*** OnlineOrderServiceImpl >> saveOnlineOrderByHookService >> orderId: " + orderId);
			logger.info("*** OnlineOrderServiceImpl >> saveOnlineOrderByHookService >> soCustomerNo: " + soCustomerNo);

			boolean existOnDB = false;
			if (MapUtils.isNotEmpty(dbOrder)) {
				Long dbOrderId = NumberUtil.getLongValue(dbOrder, "shopOrderId");
				logger.info("*** OnlineOrderServiceImpl >> saveOnlineOrderByHookService >> dbOrderId: " + dbOrderId);
				logger.info("*** OnlineOrderServiceImpl >> saveOnlineOrderByHookService >> orderId: " + orderId);
				if (String.valueOf(orderId).equals(String.valueOf(dbOrderId))) {
					existOnDB = true;
				}
			}

			logger.info("*** OnlineOrderServiceImpl >> saveOnlineOrderByHookService >> existOnDB: " + existOnDB);

			HashMap<String, Object> dbOrderMap = new HashMap<>();
			dbOrderMap.putAll(ShopifyUtil.buildOrderDBMap(newOrderMap, dbOrder));

			int result = 0;
			logger.info("*** OnlineOrderServiceImpl >> saveOnlineOrderByHookService >> dbOrderMap: " + dbOrderMap);

			if (existOnDB) {
				result = shopOrderMapper.updateShopOrder(dbOrderMap);
				logger.info("*** OnlineOrderServiceImpl >> saveOnlineOrderByHookService >> DETAIL UPDATE :" + result);
			} else {
				result = shopOrderMapper.insertShopOrder(dbOrderMap);
				logger.info("*** OnlineOrderServiceImpl >> saveOnlineOrderByHookService >> DETAIL INSERT :" + result);
			}

			if (result != 0) {
				// SUCCESS
				List<Map<String, Object>> lineItems = (List<Map<String, Object>>) newOrderMap.get("line_items");
				for (Map<String, Object> itemLine : lineItems) {

					try {
						Long productId = NumberUtil.getLongValue(itemLine, "product_id");
						Map<String, Object> productMap = onlineProductService.getOneProduct(productId);

						HashMap<String, Object> lineDbMap = new HashMap<>();
						lineDbMap.putAll(ShopifyUtil.buildOrderLineDBMap(newOrderMap, itemLine, productMap));
						int lineResult = 0;
						if (existOnDB) {
							lineResult = shopOrderMapper.updateShopOrderLine(lineDbMap);
							logger.info("*** OnlineOrderServiceImpl >> saveOnlineOrderByHookService >> LINE UPDATE :" + lineResult);
						} else {
							lineResult = shopOrderMapper.insertShopOrderLine(lineDbMap);
							logger.info("*** OnlineOrderServiceImpl >> saveOnlineOrderByHookService >> LINE INSERT :" + lineResult);
						}
					} catch (Exception e) {
						logger.log(Level.SEVERE, e.getMessage(), e);
					}

				}

			}

		} catch (Exception e) {
			logger.info("*** OnlineOrderServiceImpl >> saveOnlineOrderByHookService >> newOrderMap: " + newOrderMap);
			logger.info("*** OnlineOrderServiceImpl >> saveOnlineOrderByHookService >> ERROR:");
			logger.log(Level.SEVERE, e.getMessage(), e);
		}

		logger.info("** OnlineOrderServiceImpl >> saveOnlineOrderByHookService >> [END]");

		return null;
	}

	@Override
	public List<Map<String, Object>> getOrdersByCustomer(String customerId, String orderStatus) throws Throwable {

		String prodRequestUrl = new StringBuilder("/admin/api/").append(apiVersion).append("/customers/")
				.append(customerId).append("/orders.json?limit=").append(rowLimit).append("&status=")
				.append(orderStatus).toString();

		Map<String, Object> resultMap = restTemplateService.sendGetRequest(prodRequestUrl,
				new HashMap<String, Object>());
		if (MapUtils.isNotEmpty(resultMap) && resultMap.containsKey(Constants.ORDERS)) {
			List<Map<String, Object>> dataList = (List<Map<String, Object>>) resultMap.get(Constants.ORDERS);
			return dataList;
		}

		return null;
	}

	@Override
	public List<Map<String, Object>> getOrdersByAutoPayCustomer(String customerId, String orderStatus)
			throws Throwable {

		Date prevDate = DateUtil.getDateNowPlusTime(Calendar.DAY_OF_YEAR, -2);
		if (Constants.TEST_ONLY) {
			prevDate = DateUtil.getDateNowPlusTime(Calendar.DAY_OF_YEAR, -5);
		}
		String createdAtMin = DateUtil.getISODateFromTimeFormat(prevDate);

		String requestUrl = new StringBuilder("/admin/api/").append(apiVersion).append("/customers/").append(customerId)
				.append("/orders.json?created_at_min=").append(createdAtMin).append("&status=").append(orderStatus)
				.append("&financial_status=pending").append("&limit=").append(rowLimit).toString();

		Map<String, Object> resultMap = restTemplateService.sendGetRequest(requestUrl, new HashMap<String, Object>());
		if (MapUtils.isNotEmpty(resultMap) && resultMap.containsKey(Constants.ORDERS)) {
			List<Map<String, Object>> dataList = (List<Map<String, Object>>) resultMap.get(Constants.ORDERS);
			return dataList;
		}

		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Map<String, Object> getOneOrderByID(long orderId) throws Throwable {
		logger.info("** OnlineOrderServiceImpl >> getOneOrderByID >> [START]");

		Map<String, Object> reqParamMap = new HashMap<String, Object>();
		reqParamMap.put("id", orderId);

		String prodRequestUrl = new StringBuilder("/admin/api/").append(apiVersion).append("/orders/").append(orderId)
				.append(".json").toString();

		Map<String, Object> resultMap = restTemplateService.sendGetRequest(prodRequestUrl, reqParamMap);
		Map<String, Object> rowMap = new LinkedHashMap<>();
		if (MapUtils.isNotEmpty(resultMap) && resultMap.containsKey(Constants.ORDER)) {
			rowMap = new LinkedHashMap<>();
			rowMap = (Map<String, Object>) resultMap.get(Constants.ORDER);

			// logger.info("** OnlineOrderServiceImpl >> getOneOrderByID >> resultMap: " +
			// resultMap);

			String orderName = (String) rowMap.get("name");
			String orderTag = (String) rowMap.get("tags");
			logger.info("** OnlineOrderServiceImpl >> getOneOrderByID >> orderTag: " + orderTag);

			rowMap.put("tblId", rowMap.get("id"));
			rowMap.put("orderId", rowMap.get("id"));
			String fullfillStatus = (String) rowMap.get("fulfillment_status");
			if (StringUtils.isNotBlank(fullfillStatus)) {
				rowMap.put("fullfillStatus", fullfillStatus);
			} else {
				rowMap.put("fullfillStatus", "Unfulfilled");
			}

			if (rowMap.get("cancelled_at") != null) {
				rowMap.put("fullfillStatus", "CANCELLED");
				rowMap.put("orderStatus", "CANCELLED");
			}

			rowMap.put("accessToken", null);
			Map<String, Object> customerMap = (Map<String, Object>) rowMap.get("customer");
			rowMap.putAll(customerMap);

			rowMap.put("soCustomerNo", ShopifyUtil.getSOCustomerNoByAddress(rowMap, customerMap));
			rowMap.put("customerName", new StringBuilder((String) customerMap.get("first_name")).append(" ")
					.append((String) customerMap.get("last_name")).toString());

			List<Map<String, Object>> lineItems = (List<Map<String, Object>>) rowMap.get("line_items");
			int totalItems = 0;
			for (Map<String, Object> itemLine : lineItems) {
				int qty = (int) Double.parseDouble(String.valueOf(itemLine.get("quantity")));
				totalItems += qty;
			}

			rowMap.put("totalItems", totalItems);
			rowMap.put("totalShippingCharge", 0.0);
			List<Map<String, Object>> shipLines = (List<Map<String, Object>>) rowMap.get("shipping_lines");
			if (CollectionUtils.isNotEmpty(shipLines)) {
				Double totalShipCharge = 0D;
				for (Map<String, Object> shipLine : shipLines) {
					Double shipCharge = Double.parseDouble(String.valueOf(shipLine.get("price")));
					totalShipCharge += shipCharge;
					logger.info("** OnlineOrderServiceImpl >> shipCharge:" + shipCharge);
				}
				rowMap.put("totalShippingCharge", totalShipCharge);
			}

			rowMap.put("salesOrderNo", null);
			List<Map<String, Object>> noteAttribs = (List<Map<String, Object>>) rowMap.get("note_attributes");
			if (CollectionUtils.isNotEmpty(noteAttribs)) {
				for (Map<String, Object> noteAttMap : noteAttribs) {
					String key = (String) noteAttMap.get("name");
					switch (key) {
					case "request-deliver-date":
						rowMap.put("requestDeliveryDate", noteAttMap.get("value"));
						break;
					}
				}
			}

			logger.info("** OnlineOrderServiceImpl >> ORDER NAME:" + orderName);
			// logger.info("** OnlineOrderServiceImpl >> rowMap:" + rowMap );
			rowMap.put("tags", orderTag);
		}

		logger.info("** OnlineOrderServiceImpl >> getOneOrderByID >> [END]");
		return rowMap;
	}

	@Override
	public Map<String, Object> getOneOrderByName(String orderName) throws Throwable {
		logger.info("** OnlineOrderServiceImpl >> getOneOrderByName >> [START]");
		Map<String, Object> paramMap = new HashMap<>();
		paramMap.put("name", orderName);
		List<Map<String, Object>> eOrders = this.getOrderList(paramMap);
		if (CollectionUtils.isNotEmpty(eOrders)) {
			return eOrders.get(0);
		}

		logger.info("** OnlineOrderServiceImpl >> getOneOrderByName >> [END]");
		return null;
	}

	@Override
	public Map<String, Object> buildSalesOrderMapRequest(Map<String, Object> shopOrderMap) throws Throwable {

		String orderName = (String) shopOrderMap.get("name");
		Long orderId = NumberUtil.getLongValue(shopOrderMap, "id");

		HashMap<String, Object> rootMap = new LinkedHashMap<>();
		HashMap<String, Object> bodyMap = new LinkedHashMap<>();
		bodyMap.put("DataAreaId", defaultDataAreaId);

		List<Map<String, Object>> soLines = new ArrayList<Map<String, Object>>();
		List<Map<String, Object>> lineItems = (List<Map<String, Object>>) shopOrderMap.get("line_items");
		int inCtr = 0;
		for (Map<String, Object> itemLine : lineItems) {

			Long itemProdId = NumberUtil.getLongValue(itemLine, "product_id");
			Double salePrice = Double.parseDouble((String) itemLine.get("price"));
			int qty = NumberUtil.getIntValue(itemLine, "quantity");

			Map<String, Object> oProdMap = onlineProductService.getOneProduct(itemProdId);
			logger.info("** OnlineOrderServiceImpl >> buildSalesOrderMapRequest >> oProdMap: " + oProdMap);

			if (MapUtils.isNotEmpty(oProdMap)) {

				String itemTitle = (String) oProdMap.get("title");
				String itemNumber = ShopifyUtil.getD365ItemIdFromTitle(itemTitle);
				String uom = (String) oProdMap.get("uom");

				List<Map<String, Object>> variants = (List<Map<String, Object>>) oProdMap.get("variants");
				Map<String, Object> defVariant = new HashMap<>();
				if (CollectionUtils.isNotEmpty(variants)) {
					defVariant = (Map<String, Object>) variants.get(0);
				}

				if (inCtr == 0) {
					String warehouseCode = (String) oProdMap.get("warehouseCode");
					String warehouseSite = (String) oProdMap.get("warehouseSite");
					bodyMap.put("Site", warehouseSite);
					bodyMap.put("Warehouse", warehouseCode);
				}

				Map<String, Object> soLine = new LinkedHashMap<String, Object>();
				soLine.put("ItemId", itemNumber);
				soLine.put("UOM", uom);
				soLine.put("Qty", String.valueOf(qty));
				soLine.put("SalesPrice", salePrice);
				soLines.add(soLine);
			}

			inCtr++;
		}

		Map<String, Object> custDataMap = (Map<String, Object>) shopOrderMap.get(Constants.CUSTOMER);
		if (MapUtils.isNotEmpty(custDataMap)) {
			// Long customerId = NumberUtil.getLongValue(custDataMap, "id");
			Long customerId = ShopifyUtil.getCustomerId(custDataMap);
			Map<String, Object> oCustomerMap = onlineCustomerService.getOneCustomer(customerId);
			logger.info("** OnlineOrderServiceImpl >> buildSalesOrderMapRequest >> oCustomerMap: " + oCustomerMap);
			String customerNo = (String) oCustomerMap.get("customerNo");
			logger.info("** OnlineOrderServiceImpl >> buildSalesOrderMapRequest >> customerNo: " + customerNo);
			bodyMap.put("CustomerNumber", customerNo);
		}

		List<Map<String, Object>> noteAttribs = (List<Map<String, Object>>) shopOrderMap.get("note_attributes");
		if (CollectionUtils.isNotEmpty(noteAttribs)) {
			for (Map<String, Object> noteAttrMap : noteAttribs) {
				String key = (String) noteAttrMap.get("name");
				String value = (String) noteAttrMap.get("value");
				if (key.equals("request-deliver-date") && StringUtils.isNotBlank(value)) {
					bodyMap.put("DeliveryDate", value);
				}
			}

		} else {
			// Plus 3 days
			noteAttribs = new ArrayList<>();
			Date delivDate = DateUtil.getDateNowPlusTime(new Date(), Calendar.DAY_OF_YEAR, 3);
			bodyMap.put("DeliveryDate", DateUtil.getDateWithPattern(delivDate, "MM/dd/yyyy"));
		}

		bodyMap.put("ReferenceCode", orderName);
		bodyMap.put("SOLines", soLines);
		rootMap.put("_dataContract", bodyMap);

		return rootMap;
	}

	@Override
	public Map<String, Object> updateOrderNotes(Map<String, Object> paramMap) throws Throwable {

		logger.info("** OnlineOrderServiceImpl >> updateOrderNotes >> [START]");

		Map<String, Object> reqParamMap = new HashMap<String, Object>();
		reqParamMap.putAll(paramMap);

		Long orderId = (Long) reqParamMap.get("orderId");
		reqParamMap.remove("orderId");

		logger.info("** OnlineOrderServiceImpl >> orderId :" + orderId);
		String prodRequestUrl = new StringBuilder("/admin/api/").append(apiVersion).append("/orders/").append(orderId)
				.append(".json").toString();

		Map<String, Object> resultMap = restTemplateService.sendPutRequest(prodRequestUrl, reqParamMap);
		if (MapUtils.isNotEmpty(resultMap) && resultMap.containsKey(Constants.ORDER)) {
			Map<String, Object> orderMap = (Map<String, Object>) resultMap.get(Constants.ORDER);
			return orderMap;
		}

		logger.info("** OnlineOrderServiceImpl >> updateOrderNotes >> [END]");

		return null;
	}

	@Override
	public Map<String, Object> updateOrderTagForAutoPay(Map<String, Object> eOrderMap) throws Throwable {
		logger.info("** OnlineOrderServiceImpl >> updateOrderTagForAutoPay >> [START]");

		String orderId = String.valueOf(NumberUtil.getLongValue(eOrderMap, "id"));
		String payTag = StringUtils.trimToEmpty((String) eOrderMap.get("payTag"));
		String requestUrl = new StringBuilder("/admin/api/").append(apiVersion).append("/orders/").append(orderId)
				.append(".json").toString();
		StringBuilder newTags = new StringBuilder();
		String currentTags = StringUtils.trimToEmpty((String) eOrderMap.get("tags"));
		if (StringUtils.isBlank(payTag)) {
			payTag = "AUTOPAY_EWT";// DEFAULT Tag for AutoPay
		}

		if (StringUtils.isNotBlank(currentTags)) {
			newTags.append(currentTags).append(",").append(payTag);
		} else {
			newTags.append(payTag);
		}

		Map<String, Object> rootMap = new HashMap<>();
		Map<String, Object> reqParamMap = new HashMap<>();
		reqParamMap.put("tags", newTags.toString());
		rootMap.put(Constants.ORDER, reqParamMap);

		Map<String, Object> resultMap = restTemplateService.sendPutRequest(requestUrl, rootMap);
		if (MapUtils.isNotEmpty(resultMap) && resultMap.containsKey(Constants.ORDER)) {
			Map<String, Object> orderMap = (Map<String, Object>) resultMap.get(Constants.ORDER);
			return orderMap;
		}

		logger.info("** OnlineOrderServiceImpl >> updateOrderTagForAutoPay >> [END]");

		return null;
	}

	@Override
	public Map<String, Object> updateOnlineTagForSO(Map<String, Object> eOrderMap, Map<String, Object> dbOrderMap)
			throws Throwable {
		logger.info("** OnlineOrderServiceImpl >> updateOnlineTagForSO >> [START]");
		SimpleDateFormat sdfDelivFormat = new SimpleDateFormat("MM/dd/yyyy");
		String orderId = String.valueOf(NumberUtil.getLongValue(dbOrderMap, "shopOrderId"));
		String payTag = StringUtils.trimToEmpty((String) dbOrderMap.get("payTag"));
		String prevOnlineTag = StringUtils.trimToEmpty((String) eOrderMap.get("tags"));
		logger.info("** OnlineOrderServiceImpl >> updateOnlineTagForSO >> prevOnlineTag:" + prevOnlineTag);
		String requestUrl = new StringBuilder("/admin/api/").append(apiVersion).append("/orders/").append(orderId)
				.append(".json").toString();
		StringBuilder newTags = new StringBuilder();

		if (StringUtils.isNotBlank(prevOnlineTag)) {
			newTags.append(prevOnlineTag).append(",");
		}

		if (StringUtils.isNotBlank(payTag)) {
			newTags.append(payTag).append(",");
		}

		String dbSalesOrderNo = (String) dbOrderMap.get("salesOrderNo");
		if (StringUtils.isNotBlank(dbSalesOrderNo)) {
			String soNoWithTag = ShopifyUtil.getSONumberTagByOrderDB(dbSalesOrderNo);
			newTags.append(soNoWithTag).append(",");
		}

		String paymentMode = (String) dbOrderMap.get("paymentMode");
		if (StringUtils.isNotBlank(paymentMode)) {
			newTags.append(Constants.MODE_OF_PAYMENT_TAG).append(paymentMode).append(",");
		}

		Date reqDelivDate = (Date) dbOrderMap.get("requestDeliveryDate");
		if (reqDelivDate != null) {
			newTags.append(Constants.SO_DELIVERY_DATE_TAG).append(sdfDelivFormat.format(reqDelivDate)).append(",");
		}

		newTags.append(Constants.SALES_ORDER_STATUS_TAG).append("Open order").append(",");

		// this.addOrderTagToCurrent(eOrderMap, newTags.toString());
		Map<String, Object> rootMap = new HashMap<>();
		Map<String, Object> reqParamMap = new HashMap<>();
		reqParamMap.put("tags", newTags.toString());
		rootMap.put(Constants.ORDER, reqParamMap);

		Map<String, Object> resultMap = restTemplateService.sendPutRequest(requestUrl, rootMap);
		if (MapUtils.isNotEmpty(resultMap) && resultMap.containsKey(Constants.ORDER)) {
			Map<String, Object> orderMap = (Map<String, Object>) resultMap.get(Constants.ORDER);
			return orderMap;
		}

		logger.info("** OnlineOrderServiceImpl >> updateOnlineTagForSO >> [END]");

		return null;
	}

	@Override
	public Map<String, Object> addOrderTagToCurrent(Map<String, Object> eOrderMap, String addTag) throws Throwable {
		logger.info("** OnlineOrderServiceImpl >> addOrderTagToCurrent >> [START]");

		String orderId = String.valueOf(ShopifyUtil.getOrderId(eOrderMap));

		String requestUrl = new StringBuilder("/admin/api/").append(apiVersion).append("/orders/").append(orderId)
				.append(".json").toString();
		StringBuilder newTags = new StringBuilder();
		String currentTags = StringUtils.trimToEmpty((String) eOrderMap.get("tags"));

		if (StringUtils.isNotBlank(addTag)) {
			newTags.append(currentTags).append(",").append(addTag);
		}

		Map<String, Object> rootMap = new HashMap<>();
		Map<String, Object> reqParamMap = new HashMap<>();
		reqParamMap.put("tags", newTags.toString());
		rootMap.put(Constants.ORDER, reqParamMap);

		Map<String, Object> resultMap = restTemplateService.sendPutRequest(requestUrl, rootMap);
		if (MapUtils.isNotEmpty(resultMap) && resultMap.containsKey(Constants.ORDER)) {
			Map<String, Object> orderMap = (Map<String, Object>) resultMap.get(Constants.ORDER);
			return orderMap;
		}

		logger.info("** OnlineOrderServiceImpl >> addOrderTagToCurrent >> [END]");

		return null;
	}

	@Override
	public Map<String, Object> updateOnlineOrderTag(Map<String, Object> dbOrderMap, Map<String, Object> soD365Map)
			throws Throwable {

		logger.info("** OnlineOrderServiceImpl >> updateOnlineOrderTag >> [START]");
		Long eOrderId = NumberUtil.getLongValue(dbOrderMap, "shopOrderId");
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");

		String prodRequestUrl = new StringBuilder("/admin/api/").append(apiVersion).append("/orders/").append(eOrderId)
				.append(".json").toString();
		StringBuilder newTags = new StringBuilder();
		String dbSalesOrderNo = StringUtils.trimToEmpty((String) dbOrderMap.get("salesOrderNo"));

		Map<String, Object> eOrderMap = this.getOneOrderByID(eOrderId);
		String currentOrderTag = (String) eOrderMap.get("tags");
		boolean changeExist = false;
		if (StringUtils.isNotBlank(currentOrderTag)) {
			newTags = new StringBuilder();
			// Include the Current Tag
			newTags.append(currentOrderTag).append(",");
			// NO SalesOrder Tag
			if (dbSalesOrderNo.contains(",")) {
				if (!currentOrderTag.contains(Constants.SALES_ORDER_NO_2_TAG)
						|| !currentOrderTag.contains(Constants.SALES_ORDER_NO_TAG)) {
					String soNoWithTag = ShopifyUtil.getSONumberTagByOrderDB(dbSalesOrderNo);
					newTags.append(soNoWithTag).append(",");
					changeExist = true;
				}
			} else {
				if (!currentOrderTag.contains(Constants.SALES_ORDER_NO_TAG)) {
					String soNoWithTag = ShopifyUtil.getSONumberTagByOrderDB(dbSalesOrderNo);
					newTags.append(soNoWithTag).append(",");
					changeExist = true;
				}
			}

			// NO SO Delivery Date
			if (!currentOrderTag.contains(Constants.SO_DELIVERY_DATE_TAG)) {
				if (StringUtils.isNotBlank((String) soD365Map.get("DeliveryDate"))) {
					Date delivDate = DateUtil.stringToDate((String) soD365Map.get("DeliveryDate"),
							"yyyy-MM-dd'T'HH:mm:ss");
					newTags.append(Constants.SO_DELIVERY_DATE_TAG).append(sdf.format(delivDate)).append(",");
					changeExist = true;
				}
			}

			// NO SO Delivery Status
			if (!currentOrderTag.contains(Constants.SALES_ORDER_STATUS_TAG)) {

				if (StringUtils.isNotBlank((String) soD365Map.get("DeliveryStatus"))) {
					String spDelivStatus = (String) soD365Map.get("DeliveryStatus");
					newTags.append(Constants.SALES_ORDER_STATUS_TAG).append(spDelivStatus).append(",");
					changeExist = true;
				}
			}
		} else {
			changeExist = true;
			newTags = new StringBuilder();
			String soNoWithTag = ShopifyUtil.getSONumberTagByOrderDB(dbSalesOrderNo);
			newTags.append(soNoWithTag).append(",");

			if (StringUtils.isNotBlank((String) soD365Map.get("DeliveryDate"))) {
				Date delivDate = DateUtil.stringToDate((String) soD365Map.get("DeliveryDate"), "yyyy-MM-dd'T'HH:mm:ss");
				newTags.append(Constants.SO_DELIVERY_DATE_TAG).append(sdf.format(delivDate)).append(",");
			}

			if (StringUtils.isNotBlank((String) soD365Map.get("DeliveryStatus"))) {
				String spDelivStatus = (String) soD365Map.get("DeliveryStatus");
				newTags.append(Constants.SALES_ORDER_STATUS_TAG).append(spDelivStatus).append(",");
			}
		}

		Map<String, Object> rootMap = new HashMap<>();
		Map<String, Object> reqParamMap = new HashMap<>();
		reqParamMap.put("tags", newTags.toString());
		rootMap.put(Constants.ORDER, reqParamMap);

		if (changeExist) {
			Map<String, Object> resultMap = restTemplateService.sendPutRequest(prodRequestUrl, rootMap);
			if (MapUtils.isNotEmpty(resultMap) && resultMap.containsKey(Constants.ORDER)) {
				Map<String, Object> orderMap = (Map<String, Object>) resultMap.get(Constants.ORDER);
				return orderMap;
			}
		}

		logger.info("** OnlineOrderServiceImpl >> updateOnlineOrderTag >> [END]");

		return null;
	}

	public Map<String, Object> updateOrderTagWithSO_ORIG(Map<String, Object> paramMap) throws Throwable {

		logger.info("** OnlineOrderServiceImpl >> updateOrderTagWithSO >> [START]");

		Map<String, Object> reqParamMap = new HashMap<String, Object>();
		reqParamMap.putAll(paramMap);

		Long orderId = (Long) reqParamMap.get("orderId");
		reqParamMap.remove("orderId");

		logger.info("** OnlineOrderServiceImpl >> orderId :" + orderId);
		String prodRequestUrl = new StringBuilder("/admin/api/").append(apiVersion).append("/orders/").append(orderId)
				.append(".json").toString();

		Map<String, Object> resultMap = restTemplateService.sendPutRequest(prodRequestUrl, reqParamMap);
		if (MapUtils.isNotEmpty(resultMap) && resultMap.containsKey(Constants.ORDER)) {
			Map<String, Object> orderMap = (Map<String, Object>) resultMap.get(Constants.ORDER);
			return orderMap;
		}

		logger.info("** OnlineOrderServiceImpl >> updateOrderTagWithSO >> [END]");

		return null;
	}

	@Override
	public List<Map<String, Object>> getOrderTransactions(long orderId) throws Throwable {

		logger.info("** OnlineOrderServiceImpl >> getOrderTransactions >> [START]");

		Map<String, Object> reqParamMap = new HashMap<String, Object>();
		reqParamMap.put("id", orderId);

		String apiRequestUrl = new StringBuilder("/admin/api/").append(apiVersion).append("/orders/").append(orderId)
				.append("/transactions.json").toString();

		Map<String, Object> resultMap = restTemplateService.sendGetRequest(apiRequestUrl, reqParamMap);
		if (MapUtils.isNotEmpty(resultMap) && resultMap.containsKey(Constants.TRANSACTIONS)) {
			List<Map<String, Object>> dataList = (List<Map<String, Object>>) resultMap.get(Constants.TRANSACTIONS);
			return dataList;
		}

		logger.info("** OnlineOrderServiceImpl >> getOrderTransactions >> [END]");
		return null;
	}

	@Override
	public Map<String, Object> getOneSuccessTransaction(long orderId) throws Throwable {
		List<Map<String, Object>> transactList = this.getOrderTransactions(orderId);
		for (Map<String, Object> transMap : transactList) {
			String status = (String) transMap.get("status");
			if (status.equals("success")) {
				Map<String, Object> rootMap = new LinkedHashMap<>();
				rootMap.put("transaction", transMap);
				return rootMap;
			}
		}
		return null;
	}

	@Override
	public Map<String, Object> updateOrder(Map<String, Object> paramMap) throws Throwable {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, Object> createOrder(Map<String, Object> paramMap) throws Throwable {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, Object> cancelOrder(String orderId) throws Throwable {
		Map<String, Object> reqParamMap = new HashMap<String, Object>();

		String prodRequestUrl = new StringBuilder("/admin/api/").append(apiVersion).append("/orders/").append(orderId)
				.append("/cancel.json").toString();
		Map<String, Object> resultMap = restTemplateService.sendPostRequest(prodRequestUrl, new HashMap<>());
		if (MapUtils.isNotEmpty(resultMap) && resultMap.containsKey(Constants.ORDER)) {
			return resultMap;
		}
		return null;
	}

	@Override
	public Map<String, Object> deleteOrder(String orderId) throws Throwable {
		logger.info("** OnlineOrderServiceImpl >> deleteOrder >> [START]");
		String prodRequestUrl = new StringBuilder("/admin/api/").append(apiVersion).append("/orders/").append(orderId)
				.append(".json").toString();
		Map<String, Object> resultMap = restTemplateService.sendDeleteRequest(prodRequestUrl, new HashMap<>());
		if (MapUtils.isNotEmpty(resultMap)) {
			return resultMap;
		}
		return null;
	}

	@Override
	public Map<String, Object> processPayment(Map<String, Object> paramMap) throws Throwable {
		logger.info("** OnlineOrderServiceImpl >> processPayment >> [START]");
		Long orderId = Long.valueOf((String) paramMap.get("orderId"));

		String transRequestUrl = new StringBuilder("/admin/api/").append(apiVersion).append("/orders/").append(orderId)
				.append("/transactions.json").toString();
		
		logger.info("** OnlineOrderServiceImpl >> processPayment >> transRequestUrl: " + transRequestUrl);
		
		Map<String, Object> rootMap = new LinkedHashMap<>();
		Map<String, Object> requestMap = new LinkedHashMap<>();
		String currency = (String) paramMap.get("currency");
		if (StringUtils.isBlank(currency)) {
			currency = Constants.DEFAULT_CURRENCY;// PHP
		}
		
		requestMap.put("parent_id", null);
		requestMap.put("currency", currency);
		requestMap.put("amount", paramMap.get("totalOrderPrice"));
		requestMap.put("kind", "capture");

		rootMap.put("transaction", requestMap);

		logger.info("** OnlineOrderServiceImpl >> processPayment >> rootMap: " + rootMap);

		Map<String, Object> resultMap = restTemplateService.sendPostRequest(transRequestUrl, rootMap);

		logger.info("** OnlineOrderServiceImpl >> processPayment >> [START]");
		return resultMap;
	}

	@Override
	public Map<String, Object> processBillsPayment(Map<String, Object> paramMap) throws Throwable {
		logger.info("** OnlineOrderServiceImpl >> processBillsPayment >> [START]");
		Long orderId = Long.valueOf((String) paramMap.get("orderId"));

		String transRequestUrl = new StringBuilder("/admin/api/").append(apiVersion).append("/orders/").append(orderId)
				.append("/transactions.json").toString();
		String currency = (String) paramMap.get("currency");
		if (StringUtils.isBlank(currency)) {
			currency = Constants.DEFAULT_CURRENCY;// PHP
		}

		Map<String, Object> rootMap = new LinkedHashMap<>();
		Map<String, Object> requestMap = new LinkedHashMap<>();
		requestMap.put("parent_id", null);
		requestMap.put("currency", currency);
		requestMap.put("amount", paramMap.get("totalOrderPrice"));
		requestMap.put("kind", "capture");

		rootMap.put("transaction", requestMap);

		logger.info("** OnlineOrderServiceImpl >> processBillsPayment >> rootMap: " + rootMap);

		Map<String, Object> resultMap = restTemplateService.sendPostRequest(transRequestUrl, rootMap);

		logger.info("** OnlineOrderServiceImpl >> processBillsPayment >> [START]");
		return resultMap;
	}

	@Override
	public Map<String, Object> syncOrderDbSOToOnline(Map<String, Object> paramMap) {
		logger.info("** OnlineOrderServiceImpl >> syncOrderDbSOToOnline >> [START]");

		HashMap<String, Object> searchMap = new HashMap<>();
		searchMap.putAll(paramMap);
		searchMap.put("withSO", "true");
		searchMap.put("dbUpdateFrom", "true");
		searchMap.put("dbUpdateTo", "true");

		if (Constants.TEST_ONLY) {
			searchMap.remove("updateFrom");
			searchMap.remove("updateTo");
			searchMap.put("orderName", "POR1144");
		}

		logger.info("** OnlineOrderServiceImpl >> searchMap: " + searchMap.toString());
		List<Map<String, Object>> orderList = shopOrderMapper.getShopOrderWithNoLines(searchMap);

		for (Map<String, Object> dbOrderMap : orderList) {
			String orderName = (String) dbOrderMap.get("orderName");
			Long orderId = (Long) dbOrderMap.get("shopOrderId");
			String salesOrderNo = (String) dbOrderMap.get("salesOrderNo");
			logger.info("** OnlineOrderServiceImpl >> syncOrderDbSOToOnline >> orderName: " + orderName);
			logger.info("** OnlineOrderServiceImpl >> syncOrderDbSOToOnline >> salesOrderNo: " + salesOrderNo);

			try {
				Map<String, Object> eOrder = this.getOneOrderByID(orderId);
				if (MapUtils.isNotEmpty(eOrder)) {
					String orderTags = (String) eOrder.get("tags");
					logger.info("** OnlineOrderServiceImpl >> syncOrderDbSOToOnline >> orderTags: " + orderTags);
					if (StringUtils.isBlank(orderTags)) {
						// CREATE Tags
						Map<String, Object> updateMap = new LinkedHashMap<>();
						Map<String, Object> rootMap = new LinkedHashMap<>();

						String soNumberTags = ShopifyUtil.getSONumberTagByOrderDB(salesOrderNo);
						String soStatusTags = ShopifyUtil.getSOStatusTagByOrderDB(salesOrderNo);

						StringBuilder sbTags = new StringBuilder();
						sbTags.append(soNumberTags).append(",");
						sbTags.append(Constants.SALES_ORDER_STATUS_TAG).append("Pending");
						updateMap.put("tags", sbTags.toString());
						rootMap.put("orderId", orderId);
						rootMap.put(Constants.ORDER, updateMap);
						this.updateOrderTagWithSO_ORIG(rootMap);
					}
				}
			} catch (Throwable e) {
				logger.info("** OnlineOrderServiceImpl >> syncOrderDbSOToOnline >> ERROR : ");
				logger.log(Level.SEVERE, e.getMessage(), e);
			}

		}

		logger.info("** OnlineOrderServiceImpl >> syncOrderDbSOToOnline >> [END]");
		return null;
	}

	@Override
	public Map<String, Object> checkAllOrdersBySO(Map<String, Object> paramMap) throws Throwable {
		logger.info("** OnlineOrderServiceImpl >> checkAllOrdersBySO >> [START]");
		List<Map<String, Object>> orderList = this.getOrderList(paramMap);
		for (Map<String, Object> eOrderMap : orderList) {

			String orderName = (String) eOrderMap.get("name");
			String eFinanceStatus = (String) eOrderMap.get("financial_status");

			if (eFinanceStatus.equals(Constants.STATUS_PAID)) {
				HashMap<String, Object> dbParamMap = new HashMap<>();
				dbParamMap.put("orderName", orderName);
				List<Map<String, Object>> dbOrderList = shopOrderMapper.getShopOrderList(dbParamMap);
				boolean isStatusMatch = true;
				for (Map<String, Object> dbOrderMap : dbOrderList) {
					String dbFinanceStatus = (String) dbOrderMap.get("financialStatus");
					if (!eFinanceStatus.equals(dbFinanceStatus)) {
						isStatusMatch = false;
						break;
					}

				}

				logger.info("** OnlineOrderServiceImpl >> checkAllOrdersBySO >> orderName: " + orderName);
				logger.info("** OnlineOrderServiceImpl >> checkAllOrdersBySO >> isStatusMatch: " + isStatusMatch);

				if (!isStatusMatch) {
					// NOT MATCH - NEED to SYNC Online to DB
					Map<String, Object> newDBOrderMap = ShopifyUtil.buildOrderDBMap(eOrderMap, new HashMap<>());
					HashMap<String, Object> updateMap = new HashMap<>();
					updateMap.putAll(newDBOrderMap);
					int updateResult = shopOrderMapper.updateShopOrder(updateMap);
					logger.info("** OnlineOrderServiceImpl >> checkAllOrdersBySO >> updateResult: " + updateResult);
				}
			}

		}

		logger.info("** OnlineOrderServiceImpl >> checkAllOrdersBySO >> [END]");
		return null;
	}

	@Override
	public List<Map<String, Object>> getFulfillmentOrders(String eOrderId) throws Throwable {
		logger.info("** OnlineOrderServiceImpl >> getFulfillmentOrders >> [START] ");
		String requestUrl = new StringBuilder("/admin/api/").append(apiVersion).append("/orders/").append(eOrderId)
				.append("/fulfillment_orders.json").toString();

		Map<String, Object> resultMap = restTemplateService.sendGetRequest(requestUrl, new HashMap<String, Object>());
		if (MapUtils.isNotEmpty(resultMap)) {
			if (MapUtils.isNotEmpty(resultMap) && resultMap.containsKey("fulfillment_orders")) {
				List<Map<String, Object>> dataList = (List<Map<String, Object>>) resultMap.get("fulfillment_orders");
				logger.info("** OnlineOrderServiceImpl >> getFulfillmentOrders >> dataList: " + dataList.size());
				return dataList;
			}
		}
		logger.info("** OnlineOrderServiceImpl >> getFulfillmentOrders >> [END] ");
		return null;
	}

	public Map<String, Object> submitFulfillRequest(String fulfillOrderId, Map<String, Object> fulfillOrder)
			throws Throwable {
		logger.info("** OnlineOrderServiceImpl >> submitFulfillRequest >> [START] ");

		String requestUrl = new StringBuilder("/admin/api/").append(apiVersion).append("/fulfillment_orders/")
				.append(fulfillOrderId).append("/fulfillment_request.json").toString();

		Map<String, Object> rootMap = new LinkedHashMap<>();
		Map<String, Object> requestMap = new LinkedHashMap<>();
		requestMap.put("message", "SO already invoiced. Fulfill ASAP");

		List<Map<String, Object>> fulfillLinesReq = new ArrayList<>();
		List<Map<String, Object>> fulfillLines = (List<Map<String, Object>>) fulfillOrder.get("line_items");
		for (Map<String, Object> ffLine : fulfillLines) {
			Map<String, Object> newLineMap = new LinkedHashMap<>();
			Long ffLineId = NumberUtil.getLongValue(ffLine, "id");
			int ffLineQty = NumberUtil.getIntValue(ffLine, "quantity");

			newLineMap.put("id", ffLineId);
			newLineMap.put("quantity", ffLineQty);
			fulfillLinesReq.add(newLineMap);
		}

		requestMap.put("fulfillment_order_line_items", fulfillLinesReq);

		rootMap.put("fulfillment_request", requestMap);

		logger.info("** OnlineOrderServiceImpl >> processPayment >> rootMap: " + rootMap);
		HelperUtil.viewInJSON(rootMap);
		Map<String, Object> resultMap = restTemplateService.sendPostRequest(requestUrl, rootMap);

		logger.info("** OnlineOrderServiceImpl >> submitFulfillRequest >> [END] ");
		return resultMap;
	}

	public Map<String, Object> createFulfillRequest(String eOrderId, String fulfillOrderId,
			Map<String, Object> fulfillOrder) throws Throwable {

		logger.info("** OnlineOrderServiceImpl >> createFulfillRequest >> [START] ");

		String requestUrl = new StringBuilder("/admin/api/").append(apiVersion).append("/fulfillments.json").toString();

		Map<String, Object> rootMap = new LinkedHashMap<>();
		Map<String, Object> requestMap = new LinkedHashMap<>();
		requestMap.put("message", "Order delivery/pickup was successful.");
		requestMap.put("notify_customer", false);

		List<Map<String, Object>> fulfillLinesReq = new ArrayList<>();
		List<Map<String, Object>> fulfillLines = (List<Map<String, Object>>) fulfillOrder.get("line_items");
		for (Map<String, Object> ffLine : fulfillLines) {
			Map<String, Object> newLineMap = new LinkedHashMap<>();
			Long ffLineId = NumberUtil.getLongValue(ffLine, "id");
			int ffLineQty = NumberUtil.getIntValue(ffLine, "quantity");

			newLineMap.put("id", ffLineId);
			newLineMap.put("quantity", ffLineQty);
			fulfillLinesReq.add(newLineMap);
		}

		List<Map<String, Object>> linesFulfillOrder = new ArrayList<>();
		Map<String, Object> lineFulfillMap = new LinkedHashMap<>();
		lineFulfillMap.put("fulfillment_order_id", Long.valueOf(fulfillOrderId));
		lineFulfillMap.put("fulfillment_order_line_items", fulfillLinesReq);
		linesFulfillOrder.add(lineFulfillMap);

		requestMap.put("line_items_by_fulfillment_order", linesFulfillOrder);
		rootMap.put("fulfillment", requestMap);

		HelperUtil.viewInJSON(rootMap);

		logger.info("** OnlineOrderServiceImpl >> createFulfillRequest >> rootMap: " + rootMap);

		Map<String, Object> resultMap = restTemplateService.sendPostRequest(requestUrl, rootMap);

		logger.info("** OnlineOrderServiceImpl >> acceptFulfillRequest >> [END] ");
		return resultMap;
	}

	@Override
	public Map<String, Object> completeOrderFulfillment(String eOrderId) throws Throwable {
		logger.info("** OnlineOrderServiceImpl >> completeOrderFulfillment >> [START] ");
		List<Map<String, Object>> fulfillOrders = this.getFulfillmentOrders(eOrderId);
		Map<String, Object> resultMap = new HashMap<>();
		if (CollectionUtils.isNotEmpty(fulfillOrders)) {
			for (Map<String, Object> ffOrder : fulfillOrders) {
				logger.info("** OnlineOrderServiceImpl >> ffOrder: " + ffOrder);
				String fulfillOrderId = String.valueOf(NumberUtil.getLongValue(ffOrder, "id"));
				String requestStatus = (String) ffOrder.get("request_status");
				String ffStatus = (String) ffOrder.get("status");
				if (requestStatus.equals("unsubmitted") && ffStatus.equals("open")) {
					resultMap = this.createFulfillRequest(eOrderId, fulfillOrderId, ffOrder);
				}

			}
		}
		logger.info("** OnlineOrderServiceImpl >> completeOrderFulfillment >> [END] ");
		return resultMap;
	}

	@Override
	public Map<String, Object> resetObjectHook(String orderName) throws Throwable {
		logger.info("** OnlineOrderServiceImpl >> resetObjectHook >> [START] ");
		Map<String, Object> resultMap = new HashMap<>();
		try {
			String requestUrl = new StringBuilder(cmsAppHostUrl)
					.append("/springboot-cms-backend/pc/webhook/resetObjectHook").toString();

			Map<String, Object> paramMap = new HashMap<>();
			if (StringUtils.isNotBlank(orderName)) {
				paramMap.put("orderName", orderName);
			}
			restGenericService.sendPostRequest(requestUrl, paramMap);
			resultMap.put("SUCCESS", "SUCCESS");
		} catch (Throwable t) {
			logger.log(Level.SEVERE, t.getMessage(), t);
		}

		logger.info("** OnlineOrderServiceImpl >> resetObjectHook >> [END] ");
		return resultMap;
	}

	@Override
	public Map<String, Object> addDeliveryChargeToCart(String checkoutToken, Map<String, Object> paramMap)
			throws Throwable {
		logger.info("** OnlineOrderServiceImpl >> addDeliveryChargeToCart >> [START] ");
		String requestUrl = new StringBuilder("/admin/api/").append(apiVersion).append("/checkouts/")
				.append(checkoutToken).append(".json").toString();
		Map<String, Object> rootMap = new HashMap<>();
		rootMap.put("checkout", paramMap);

		logger.info("** OnlineOrderServiceImpl >> addDeliveryChargeToCart >> rootMap: " + rootMap);
		Map<String, Object> resultMap = restTemplateService.sendPutRequest(requestUrl, rootMap);
		logger.info("** OnlineOrderServiceImpl >> addDeliveryChargeToCart >> [END] ");
		return resultMap;
	}

	@Override
	public Map<String, Object> createEWTDiscountTag(Map<String, Object> eOrderMap, Map<String, Object> discountConfig)
			throws Throwable {

		logger.info("** OnlineOrderServiceImpl >> createEWTDiscountTag >> [START]");
		boolean isEWTOrder = PCDataUtil.isAnEWTOrder(eOrderMap);
		logger.info("** OnlineOrderServiceImpl >> isEWTOrder: " + isEWTOrder);
		if (isEWTOrder) {
			String discountTag = new StringBuilder(Constants.EWT_DISCOUNT_TAG).append("").toString();
			Map<String, Object> resultMap = this.addOrderTagToCurrent(eOrderMap, discountTag);
			return resultMap;
		}

		logger.info("** OnlineOrderServiceImpl >> createEWTDiscountTag >> [END]");
		return null;
	}

}
