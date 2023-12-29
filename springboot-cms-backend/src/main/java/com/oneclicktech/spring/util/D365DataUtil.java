package com.oneclicktech.spring.util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.collections4.MapUtils; 

public class D365DataUtil {

	private static final Logger logger = Logger.getLogger("D365DataUtil");

	public static int GROUP_CUSTOMER_NO_MIN_SIZE = 5;
	public static int GROUP_CUSTOMER_NO_MAX_SIZE = 8;
	public static String DELIVERY_CHARGE_ITEM_ID = "SPC0000001";
	
	public static synchronized Map<String, Object> buildSalesOrderRequest(HashMap<String, Object> paramMap,
			List<Map<String, Object>> dbOrderLines, Map<String, Object> eOrderMap) {

		logger.info("** D365DataUtil >> buildSalesOrderRequest >> [START]");

		Map<String, Object> rootMap = new LinkedHashMap<>();
		Map<String, Object> bodyMap = new LinkedHashMap<>();

		Map<String, Object> orderMap = dbOrderLines.get(0);
		bodyMap.put("DataAreaId", paramMap.get("defaultDataAreaId"));
		bodyMap.put("Warehouse", orderMap.get("soWarehouseCode"));
		bodyMap.put("Site", orderMap.get("soWarehouseSite"));
		bodyMap.put("CustomerNumber", orderMap.get("soCustomerNo"));

		Date delivDate = DateUtil.getDateNowPlusTime(new Date(), Calendar.DAY_OF_YEAR, 3);
		if (orderMap.get("requestDeliveryDate") != null) {
			try {
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
				delivDate = sdf.parse(String.valueOf(orderMap.get("requestDeliveryDate")));
			} catch (Exception e) {
				logger.info("** D365DataUtil >> buildSalesOrderRequest >> ERROR: " + e.getMessage());
				delivDate = DateUtil.getDateNowPlusTime(new Date(), Calendar.DAY_OF_YEAR, 3);
			}
		}

		bodyMap.put("DeliveryDate", DateUtil.getDateWithPattern(delivDate, "MM/dd/yyyy"));
		bodyMap.put("ReferenceCode", orderMap.get("orderName"));

		List<Map<String, Object>> soLines = new ArrayList<Map<String, Object>>();
		for (Map<String, Object> orderLine : dbOrderLines) {

			Map<String, Object> soLine = new LinkedHashMap<String, Object>();
			soLine.put("ItemId", orderLine.get("soItemNo"));
			soLine.put("UOM", StringUtils.upperCase((String) orderLine.get("soUom")));
			soLine.put("Qty", String.valueOf(orderLine.get("quantity")));
			soLine.put("SalesPrice", Double.valueOf(String.valueOf(orderLine.get("price"))));
			soLines.add(soLine);
		}
		
		Map<String, Object> delivChargeMap = getDeliveryChargeSOLine(eOrderMap);
		if (MapUtils.isNotEmpty(delivChargeMap)) {
			soLines.add(delivChargeMap);
		}
		
		bodyMap.put("SOLines", soLines);
		rootMap.put("_dataContract", bodyMap);

		logger.info("** D365DataUtil >> buildSalesOrderRequest >> [END]");
		return rootMap;
	}

	public static synchronized List<Map<String, Object>>  buildSplitSORequest(HashMap<String, Object> paramMap,
			List<Map<String, Object>> dbOrderLines, Map<String, Object> eOrderMap) {

		logger.info("** D365DataUtil >> buildSplitSORequest >> [START]");

		List<Map<String, Object>> soList = new ArrayList<>();

		boolean with2ndSO = has2ndSO(dbOrderLines);
		logger.info("** D365DataUtil >> buildSplitSORequest >> with2ndSO: " + with2ndSO);

		if (with2ndSO) {
			// Generate TWO SO

			Map<String, String> whKeys = getWarehouseSOKeys(dbOrderLines);
			int whCtr = 1;
			for (Map.Entry<String, String> entry : whKeys.entrySet()) {

				String whCode = entry.getKey();
				String whSite = entry.getValue();

				logger.info("** D365DataUtil >> buildSplitSORequest >> whCode: " + whCode + " *** whSite: " + whSite);

				Map<String, Object> rootMap = new LinkedHashMap<>();
				Map<String, Object> bodyMap = new LinkedHashMap<>();

				Map<String, Object> orderMap = dbOrderLines.get(0);
				bodyMap.put("DataAreaId", paramMap.get("defaultDataAreaId"));
				bodyMap.put("Warehouse", whCode);
				bodyMap.put("Site", whSite);
				bodyMap.put("CustomerNumber", orderMap.get("soCustomerNo"));

				Date delivDate = DateUtil.getDateNowPlusTime(new Date(), Calendar.DAY_OF_YEAR, 3);
				if (orderMap.get("requestDeliveryDate") != null) {
					try {
						SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
						delivDate = sdf.parse(String.valueOf(orderMap.get("requestDeliveryDate")));
					} catch (Exception e) {
						logger.info("** D365DataUtil >> buildSplitSORequest >> ERROR: " + e.getMessage());
						delivDate = DateUtil.getDateNowPlusTime(new Date(), Calendar.DAY_OF_YEAR, 3);
					}
				}

				bodyMap.put("DeliveryDate", DateUtil.getDateWithPattern(delivDate, "MM/dd/yyyy"));
				bodyMap.put("ReferenceCode", orderMap.get("orderName"));

				List<Map<String, Object>> soLines = new ArrayList<Map<String, Object>>();
				for (Map<String, Object> orderLine : dbOrderLines) {

					String currWHCode = (String) orderLine.get("soWarehouseCode");
					if (whCode.equals(currWHCode)) {
						Map<String, Object> soLine = new LinkedHashMap<String, Object>();
						soLine.put("ItemId", orderLine.get("soItemNo"));
						soLine.put("UOM", StringUtils.upperCase((String) orderLine.get("soUom")));
						soLine.put("Qty", String.valueOf(orderLine.get("quantity")));
						soLine.put("SalesPrice", Double.valueOf(String.valueOf(orderLine.get("price"))));
						soLines.add(soLine);
					}
				}
				
				if (whCtr == 1) {
					Map<String, Object> delivChargeMap = getDeliveryChargeSOLine(eOrderMap);
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
			Map<String, Object> oneSOMap = buildSalesOrderRequest(paramMap, dbOrderLines, eOrderMap);
			soList.add(oneSOMap);
		}

		logger.info("** D365DataUtil >> buildSplitSORequest >> [END]");
		return soList;
	}

	public static boolean has2ndSO(List<Map<String, Object>> dbOrderLines) {
		boolean with2ndSO = false;
		Map<String, Object> whMap = new LinkedHashMap<>();
		int ctr = 0;
		for (Map<String, Object> orderLine : dbOrderLines) {
			String whCode = (String) orderLine.get("soWarehouseCode");
//			if (whCode == null)
//				continue;
			
			if (ctr == 0) {
				whMap.put(whCode, whCode);
			} else {
				if (whMap.containsKey(whCode)) {
					// SAME Warehouse - NO 2nd SO
					with2ndSO = false;
				} else {
					// DIFF Warehouse - With 2nd SO
					with2ndSO = true;
					break;
				}
			}

			ctr++;
		}

		return with2ndSO;
	}

	public static Map<String, Object> getDeliveryChargeSOLine(Map<String, Object> eOrderMap) {
		Map<String, Object> shipSet = (Map<String, Object>) eOrderMap.get("total_shipping_price_set");
		if (MapUtils.isNotEmpty(shipSet)) {

			Map<String, Object> shopMoney = (Map<String, Object>) shipSet.get("shop_money");
			String delivChargeAmt = (String) shopMoney.get("amount");
			double iDelivChargeAmt = Double.parseDouble(delivChargeAmt);
			if (iDelivChargeAmt > 0D) {
				Map<String, Object> soLine = new LinkedHashMap<String, Object>();
				soLine.put("ItemId", DELIVERY_CHARGE_ITEM_ID);
				soLine.put("UOM", "LOT");
				soLine.put("Qty", "1");
				soLine.put("SalesPrice", delivChargeAmt);

				return soLine;
			}

		}

		return null;
	}

	public static Map<String, String> getWarehouseSOKeys(List<Map<String, Object>> dbOrderLines) {
		Map<String, String> whMap = new LinkedHashMap<>();
		for (Map<String, Object> orderLine : dbOrderLines) {
			String whCode = (String) orderLine.get("soWarehouseCode");
	  		String whSite = (String) orderLine.get("soWarehouseSite");
			whMap.put(whCode, whSite);
		}

		return whMap;
	}

	public static HashMap<String, Object> mapProductToDBMap(Map<String, Object> dataMap) {

		HashMap<String, Object> dbMap = new HashMap<>();
		dbMap.put("name", StringUtils.cleanseData((String)dataMap.get("Name")));
		dbMap.put("description", StringUtils.cleanseData((String)dataMap.get("Description")));
		dbMap.put("sku", dataMap.get("SKU"));
		dbMap.put("category", dataMap.get("Category"));
		dbMap.put("subCategory", dataMap.get("SubCategory"));
		dbMap.put("purchUnitId", dataMap.get("PurchUnitId"));
		dbMap.put("salesUnitId", dataMap.get("SalesUnitId"));
		dbMap.put("costUnitId", dataMap.get("CostUnitId"));
		dbMap.put("itemGroup", dataMap.get("ItemGroup"));
		dbMap.put("purpose", dataMap.get("Purpose"));
		dbMap.put("status", dataMap.get("Status"));
		dbMap.put("oosStatus", dataMap.get("OOS_Status"));
		dbMap.put("oosMoq", dataMap.get("OOS_MOQ"));
		dbMap.put("oosBoq", dataMap.get("OOS_BOQ"));
		dbMap.put("itemId", dataMap.get("ItemId"));
		dbMap.put("sellPrice", dataMap.get("SellPrice"));

		return dbMap;
	}

	public static HashMap<String, Object> mapProductInventoryToDBMap(Map<String, Object> dataMap) {

		HashMap<String, Object> dbMap = new HashMap<>();
		dbMap.put("itemNumber", dataMap.get("ItemNumber"));
		dbMap.put("itemName", dataMap.get("ItemName"));
		dbMap.put("warehouse", dataMap.get("Warehouse"));
		dbMap.put("physicalInventory", dataMap.get("PhysicalInventory"));
		dbMap.put("physicalReserved", dataMap.get("PhysicalReserved"));
		dbMap.put("availablePhysical", dataMap.get("AvailablePhysical"));

		return dbMap;
	}

	public static HashMap<String, Object> mapCustomerAddressToDBMap(Map<String, Object> dataMap) {

		HashMap<String, Object> dbMap = new HashMap<>();
		dbMap.put("customerNumber", dataMap.get("CustomerNumber"));
		dbMap.put("description", dataMap.get("Description"));
		dbMap.put("storeCode", dataMap.get("StoreCode"));
		dbMap.put("storeName", dataMap.get("StoreName"));
		dbMap.put("address", dataMap.get("Address"));
		dbMap.put("townCity", dataMap.get("TownCity"));
		dbMap.put("street", dataMap.get("Street"));
		dbMap.put("province", dataMap.get("Province"));
		dbMap.put("zipCode", dataMap.get("ZipCode"));
		dbMap.put("country", dataMap.get("Country"));

		return dbMap;
	}

	public static HashMap<String, Object> mapCustomerToDBMap(Map<String, Object> dataMap) {

		HashMap<String, Object> dbMap = new HashMap<>();
		String customerNo = StringUtils.trimToEmpty((String) dataMap.get("CustomerNumber"));
		dbMap.put("customerNumber", customerNo);
		dbMap.put("firstName", dataMap.get("FirstName"));
		dbMap.put("middleName", dataMap.get("MiddleName"));
		dbMap.put("lastName", dataMap.get("LastName"));
		dbMap.put("fullName", dataMap.get("FullName"));
		dbMap.put("custGroup", dataMap.get("CustGroup"));
		dbMap.put("type", dataMap.get("Type"));
		dbMap.put("email",  StringUtils.trimToEmpty((String)dataMap.get("Email")));
		dbMap.put("phoneNumber", PCDataUtil.getParseMobileNo((String)dataMap.get("PhoneNumber")));
		Date dateTimeCreated = DateUtil.stringToDate(String.valueOf(dataMap.get("CreatedDateTime")),
				"MM/dd/yyyy hh:mm:ss a");
		Date dateTimeModified = DateUtil.stringToDate(String.valueOf(dataMap.get("ModifiedDateTime")),
				"MM/dd/yyyy hh:mm:ss a");
		dbMap.put("createdDateTime", dateTimeCreated);
		dbMap.put("modifiedDateTime", dateTimeModified);
		dbMap.put("oosWarehouse1", dataMap.get("OOSWarehouse1")); 
		dbMap.put("oosWarehouse2", dataMap.get("OOSWarehouse2"));
		dbMap.put("oosInclude", dataMap.get("OOSInclude"));
		 dbMap.put("groupCustomerNo", PCDataUtil.getGroupCustomerNo(customerNo));

		return dbMap;
	}

	public static void main(String[] args) {

	}
}
