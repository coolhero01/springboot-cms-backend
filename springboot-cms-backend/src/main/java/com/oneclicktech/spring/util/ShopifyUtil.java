/**
 * 
 */
package com.oneclicktech.spring.util;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import com.oneclicktech.spring.domain.Constants;

/**
 * @author kg255035
 *
 */
public class ShopifyUtil {

	private static final Logger logger = Logger.getLogger("ShopifyUtil");

	private static final String PC_PROD_IMAGES_TEMP_DIR = "/Temp/SPAVI/prod_images/";

	public static String[] provinceList = new String[] { "Abra", "Agusan del Norte", "Agusan del Sur", "Aklan", "Albay",
			"Antique", "Apayao", "Aurora", "Basilan", "Bataan", "Batanes", "Batangas", "Benguet", "Biliran", "Bohol",
			"Bukidnon", "Bulacan", "Cagayan", "Camarines Norte", "Camarines Sur", "Camiguin", "Capiz", "Catanduanes",
			"Cavite", "Cebu", "Cotabato", "Davao de Oro", "Davao del Norte", "Davao del Sur", "Davao Occidental",
			"Davao Oriental", "Dinagat Islands", "Eastern Samar", "Guimaras", "Ifugao", "Ilocos Norte", "Ilocos Sur",
			"Iloilo", "Isabela", "Kalinga", "La Union", "Laguna", "Lanao del Norte", "Lanao del Sur", "Leyte",
			"Maguindanao", "Marinduque", "Masbate", "Metro Manila", "Misamis Occidental", "Misamis Oriental",
			"Mountain Province", "Negros Occidental", "Negros Oriental", "Northern Samar", "Nueva Ecija",
			"Nueva Vizcaya", "Occidental Mindoro", "Oriental Mindoro", "Palawan", "Pampanga", "Pangasinan", "Quezon",
			"Quirino", "Rizal", "Romblon", "Samar", "Sarangani", "Siquijor", "Sorsogon", "South Cotabato",
			"Southern Leyte", "Sultan Kudarat", "Sulu", "Surigao del Norte", "Surigao del Sur", "Tarlac", "Tawi-Tawi",
			"Zambales", "Zamboanga del Norte", "Zamboanga del Sur", "Zamboanga Sibugay" };

	public static String[] metroManilaList = new String[] { "Manila", "Caloocan", "Las Piñas", "Las Pinas", "Makati",
			"Malabon", "Mandaluyong", "Marikina", "Muntinlupa", "Navotas", "Parañaque", "Paranaque", "Pasay", "Pasig",
			"San Juan", "Taguig", "Valenzuela", "Pateros", "Quezon City" };

	/**
	 * @param args
	 */
	public static Map<String, Object> buildProductImageRequest(String imagePath, Map<String, Object> dbProdMap,
			Map<String, Object> shopProdMap) {
		Map<String, Object> rootMap = new HashMap<String, Object>();
		if (StringUtils.isBlank(imagePath)) {
			imagePath = PC_PROD_IMAGES_TEMP_DIR;
		}
		Map<String, Object> imageMap = new LinkedHashMap<String, Object>();
		String itemId = StringUtils.trimToEmpty((String) dbProdMap.get("itemNumber"));
		String imgFullPath = new StringBuilder(imagePath).append(itemId).append(".png").toString();
		File imgFile = new File(imgFullPath);
		if (!imgFile.exists()) {
			imgFullPath = new StringBuilder(imagePath).append("pc_img_default.png").toString();
			imgFile = new File(imgFullPath);
		}

		try {
			byte[] fileContent = FileUtils.readFileToByteArray(imgFile);
			String imgBase64 = Base64.encodeBase64String(fileContent);
			imageMap.put("position", 1);
			imageMap.put("attachment", imgBase64);
			rootMap.put("image", imageMap);
		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
		}

		return rootMap;
	}

	public static Map<String, Object> buildProductImageByImgURL(String imgUrlPath, Map<String, Object> eProduct,
			Map<String, Object> dbProduct) {
		Map<String, Object> rootMap = new HashMap<String, Object>();

		Map<String, Object> imageMap = new LinkedHashMap<String, Object>();
		String itemId = StringUtils.trimToEmpty((String) dbProduct.get("itemNumber"));
		String fileExt = StringUtils.trimToEmpty((String) dbProduct.get("fileExt"));
		if (StringUtils.isEmpty(fileExt)) {
			fileExt = "png";
		}
		String imgFullPath = new StringBuilder(imgUrlPath).append(itemId).append(".")
					.append(fileExt).toString();
		

		try {
			boolean urlFileExist = PCFileUtil.urlExists(imgFullPath);
			if (!urlFileExist) {
				imgFullPath = new StringBuilder(imgUrlPath).append("pc_img_default.png").toString();
			}

			imageMap.put("position", 1);
			imageMap.put("src", imgFullPath);
			rootMap.put("image", imageMap);
		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
		}

		return rootMap;
	}

	public static Long getOrderId(Map<String, Object> eOrderMap) {

		Long orderId = NumberUtil.getLongValue(eOrderMap, "orderId");
		if (orderId == null || orderId == 0L) {
			orderId = NumberUtil.getLongValue(eOrderMap, "id");
		}
		if (orderId == null || orderId == 0L) {
			orderId = NumberUtil.getLongValue(eOrderMap, "tblId");
		}

		return orderId;
	}

	public static Long getProductId(Map<String, Object> eProductMap) {

		Long productId = NumberUtil.getLongValue(eProductMap, "productId");
		if (productId == null || productId == 0L) {
			productId = NumberUtil.getLongValue(eProductMap, "id");
		}
		if (productId == null || productId == 0L) {
			productId = NumberUtil.getLongValue(eProductMap, "tblId");
		}

		return productId;
	}

	public static Long getCustomerId(Map<String, Object> customerMap) {
		Map<String, Object> addrMap = (Map<String, Object>) customerMap.get("default_address");
		if (MapUtils.isNotEmpty(addrMap)) {
			Long customerId = NumberUtil.getLongValue(addrMap, "customer_id");
			if (customerId != null && customerId != 0L) {
				return customerId;
			} else {
				customerId = NumberUtil.getLongValue(customerMap, "id");
				return customerId;
			}
		} else {
			// Single Address Map
			if (MapUtils.isNotEmpty(customerMap)) {
				Long customerId = NumberUtil.getLongValue(customerMap, "customer_id");
				if (customerId != null && customerId != 0L) {
					return customerId;
				} else {
					customerId = NumberUtil.getLongValue(customerMap, "id");
					return customerId;
				}
			}
		}

		return null;
	}

	public static String getSOCustomerNoByTag(Map<String, Object> customerMap) {
		String soCustomerNo = null;
		String custTags = (String) customerMap.get("tags");
		if (custTags.contains(Constants.COMMA)) {
			String[] tagArry = custTags.split(Constants.COMMA);
			for (String tag : tagArry) {
				if (tag.startsWith(Constants.CUSTOMER_NO_TAG_CODE)) {
					soCustomerNo = tag.substring(tag.indexOf('_') + 1);
					break;
				}
			}
		} else {
			if (custTags.startsWith(Constants.CUSTOMER_NO_TAG_CODE)) {
				soCustomerNo = custTags.substring(custTags.indexOf('_') + 1);
			}
		}

		return soCustomerNo;
	}

	public static String getSOCustomerNoByAddress(Map<String, Object> eOrderMap, Map<String, Object> customerMap) {
		String soCustomerNo = null;
		Map<String, Object> shipAddrMap = (Map<String, Object>) eOrderMap.get("shipping_address");
		if (MapUtils.isNotEmpty(shipAddrMap)) {
			soCustomerNo = StringUtils.trimToEmpty((String) shipAddrMap.get("first_name"));
		} else {
			if (MapUtils.isNotEmpty(customerMap)) {
				Map<String, Object> addrMap = (Map<String, Object>) customerMap.get("default_address");
				if (MapUtils.isNotEmpty(addrMap)) {
					soCustomerNo = StringUtils.trimToEmpty((String) customerMap.get("first_name"));
				}
			}
			
		}

		if (StringUtils.isBlank(soCustomerNo)) {
			soCustomerNo = "ERROR_CustomerNo";
		}

		return soCustomerNo;
	}
	
	
	public static String getSOCustomerNoByAddress(Map<String, Object> eOrderMap) {
		String soCustomerNo = null;
		Map<String, Object> shipAddrMap = (Map<String, Object>) eOrderMap.get("shipping_address");
		if (MapUtils.isNotEmpty(shipAddrMap)) {
			soCustomerNo = StringUtils.trimToEmpty((String) shipAddrMap.get("first_name"));
		}  

		if (StringUtils.isBlank(soCustomerNo)) {
			soCustomerNo = "ERROR_CustomerNo";
		}

		return soCustomerNo;
	}

	public static Map<String, Object> getOrderTagAsMap(String orderTags) {
		Map<String, Object> tagMap = new LinkedHashMap<>();
		if (StringUtils.isNotBlank(orderTags)) {
			if (orderTags.contains(Constants.COMMA)) {
				String[] tagArry = orderTags.split(Constants.COMMA);
				String soNo = null;
				String soNo2 = null;
				for (String tag : tagArry) {
					if (tag.startsWith(Constants.SALES_ORDER_NO_TAG)) {
						soNo = tag.substring(tag.indexOf('_') + 1);
					}
					if (tag.startsWith(Constants.SALES_ORDER_NO_2_TAG)) {
						soNo2 = tag.substring(tag.indexOf('_') + 1);
					}
					if (tag.startsWith(Constants.SALES_ORDER_STATUS_TAG)) {
						String soStatus = tag.substring(tag.indexOf('_') + 1);
						tagMap.put("salesOrderStatus", soStatus);
					}
				}
				StringBuilder finalSOTag = new StringBuilder();
				if (StringUtils.isNotBlank(soNo)) {
					finalSOTag.append(soNo);
				}
				if (StringUtils.isNotBlank(soNo2)) {
					finalSOTag.append(",").append(soNo2);
				}
				tagMap.put("salesOrderNo", finalSOTag.toString());
			} else {
				String soNo = null;
				String soNo2 = null;
				if (orderTags.startsWith(Constants.SALES_ORDER_NO_TAG)) {
					soNo = orderTags.substring(orderTags.indexOf('_') + 1);
				}
				if (orderTags.startsWith(Constants.SALES_ORDER_NO_2_TAG)) {
					soNo2 = orderTags.substring(orderTags.indexOf('_') + 1);
				}
				if (orderTags.startsWith(Constants.SALES_ORDER_STATUS_TAG)) {
					String soStatus = orderTags.substring(orderTags.indexOf('_') + 1);
					tagMap.put("salesOrderStatus", soStatus);
				}
				if (StringUtils.isNotBlank(soNo)) {
					tagMap.put("salesOrderNo", soNo);
				}
				if (StringUtils.isNotBlank(soNo2)) {
					String finalSOTag = new StringBuilder(soNo).append(",").append(soNo2).toString();
					tagMap.put("salesOrderNo", finalSOTag);
				}

			}
		}

		return tagMap;
	}

	public static String encodeValue(String value) {
		try {
			return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
		}
		return null;
	}

	public static String getD365ItemIdFromTitle(String prodTitle) {
		if (StringUtils.isNotBlank(prodTitle) 
				&& prodTitle.contains("(") && prodTitle.contains(")")) {
			String itemId = prodTitle.substring(prodTitle.lastIndexOf('(') + 1, prodTitle.lastIndexOf(')'));
			return itemId;
		}
		return prodTitle;
	}

	public static Map<String, Object> buildCustomerMapRequest_ORIG(Map<String, Object> dbCustomerMap, int rowCtr) {
		Map<String, Object> rootMap = new HashMap<String, Object>();
		Map<String, Object> custMap = new LinkedHashMap<String, Object>();
		StringBuilder custTags = new StringBuilder();

		for (Map.Entry<String, Object> entry : dbCustomerMap.entrySet()) {

			switch (entry.getKey()) {
			case "first_name":
				custMap.put("first_name", entry.getValue());
				break;
			case "last_name":
				custMap.put("last_name", entry.getValue());
				break;
			case "email":
				String custEmail = (String) entry.getValue();
				logger.info(" buildCustomerMapRequest >>  custEmail: " + custEmail);
				if (StringUtils.isNotBlank(custEmail)) {
					custMap.put("email", entry.getValue());
				} else {
					// TODO: Replace this
					String tempEmail = new StringBuilder("pctest_").append(rowCtr).append("@potatocorner.com")
							.toString();

					logger.info(" buildCustomerMapRequest >>  tempEmail: " + tempEmail);
					custMap.put("email", tempEmail);
				}
				custMap.put("verified_email", true);
				custMap.put("password", Constants.TEMP_CUSTOMER_PASSWORD);
				custMap.put("password_confirmation", Constants.TEMP_CUSTOMER_PASSWORD);

				break;
			case "phone_number":
				if (StringUtils.isNotBlank((String) entry.getValue()))
					custMap.put("phone", entry.getValue());
				break;
			case "customer_number":
				if (StringUtils.isNotBlank((String) entry.getValue())) {
					String custNoTag = Constants.CUSTOMER_NO_TAG_CODE
							+ StringUtils.trimToEmpty((String) entry.getValue());
					custTags.append(custNoTag).append(Constants.COMMA);
				}
				break;
			case "type":
				if (StringUtils.isNotBlank((String) entry.getValue())) {
					String typeTag = "TYPE_" + StringUtils.trimToEmpty((String) entry.getValue());
					custTags.append(typeTag).append(Constants.COMMA);
				}
				break;
			case "cust_group":
				if (StringUtils.isNotBlank((String) entry.getValue())) {
					String custGroupTag = "CGRP_" + StringUtils.trimToEmpty((String) entry.getValue());
					custTags.append(custGroupTag).append(Constants.COMMA);
				}
				break;
			}

			if (custTags.length() > 0) {
				custMap.put("tags", custTags.toString());
			}

		}

		rootMap.put("customer", custMap);

		return rootMap;
	}

	public static Map<String, Object> buildOrderLineDBMap(Map<String, Object> shopOrderMap,
			Map<String, Object> itemLine, Map<String, Object> productMap) {

		String orderName = (String) shopOrderMap.get("name");

		Map<String, Object> lineDBMap = new LinkedHashMap<String, Object>();

		String itemName = (String) itemLine.get("name");
		String itemNumber = ShopifyUtil.getD365ItemIdFromTitle(itemName);

		Long productId = NumberUtil.getLongValue(itemLine, "product_id");

		if (MapUtils.isNotEmpty(productMap)) {
			String[] itemTags = ((String) productMap.get("tags")).split(Constants.COMMA);
			for (String itemTag : itemTags) {
				itemTag = StringUtils.trimToEmpty(itemTag);
				// logger.info("*** buildOrderLineDBMap >> buildOrderLineDBMap >> itemTag :" +
				// itemTag);

				if (itemTag.startsWith(Constants.WARESHOUSE_SITE_TAG_CODE)) {
					lineDBMap.put("soWarehouseSite", itemTag.substring(itemTag.indexOf('_') + 1));
				}
				if (itemTag.startsWith(Constants.WARESHOUSE_CODE_TAG_CODE)) {
					lineDBMap.put("soWarehouseCode", itemTag.substring(itemTag.indexOf('_') + 1));
				}
				if (itemTag.startsWith(Constants.UNIT_OF_MEASURE_TAG_CODE)) {
					lineDBMap.put("soUom", itemTag.substring(itemTag.indexOf('_') + 1));
				}
				if (itemTag.startsWith(Constants.ITEM_GROUP_TAG_CODE)) {
					lineDBMap.put("soItemGroup", itemTag.substring(itemTag.indexOf('_') + 1));
				}
			}

		}

		lineDBMap.put("shopLineId", itemLine.get("id"));
		lineDBMap.put("orderName", orderName);
		lineDBMap.put("soItemNo", itemNumber);

		lineDBMap.put("variantId", itemLine.get("variant_id"));
		lineDBMap.put("sku", itemLine.get("sku"));
		lineDBMap.put("fulfillableQuantity", itemLine.get("fulfillable_quantity"));
		lineDBMap.put("fulfillmentService", itemLine.get("fulfillment_service"));
		lineDBMap.put("fulfillmentStatus", itemLine.get("fulfillment_status"));
		lineDBMap.put("name", itemLine.get("name"));
		lineDBMap.put("price", itemLine.get("price"));
		lineDBMap.put("productId", productId);
		lineDBMap.put("quantity", itemLine.get("quantity"));
		lineDBMap.put("title", itemLine.get("title"));
		lineDBMap.put("vendor", itemLine.get("vendor"));
		lineDBMap.put("requiresShipping", itemLine.get("requires_shipping"));

		return lineDBMap;
	}
	
	public static double getTotalShippingAmount(Map<String, Object> eOrderMap) {
		Map<String, Object> shippingSet = (Map<String, Object>) eOrderMap.get("total_shipping_price_set");
		if (MapUtils.isNotEmpty(shippingSet)) {
			Map<String, Object> shipMoneyMap = (Map<String, Object>) shippingSet.get("shop_money");
			if (MapUtils.isNotEmpty(shipMoneyMap)) {
				double shipTotal = Double.parseDouble((String) shipMoneyMap.get("amount"));
				return shipTotal;
			}
		}
		return 0D;
	}
	
	public static boolean hasSalesOrderTags(Map<String, Object> eOrderMap) {
		String orderTags = (String) eOrderMap.get("tags");
		if (StringUtils.isNotBlank(orderTags) && orderTags.contains(Constants.SALES_ORDER_NO_TAG)) {
			return true;
		}

		return false;
	}
	
	public static String getPayeeBank(String eOrderTag) {
		if (StringUtils.isNotBlank(eOrderTag) 
				&& eOrderTag.contains("BANK_")) {
			if (eOrderTag.contains(",")) {
				String[] tags = eOrderTag.split(",");
				for (String tag: tags) {
					if (tag.contains("BANK_")) {
						return StringUtils.trimToEmpty(tag.replace("BANK_", "")); 
					}
				}
			} else {
				return StringUtils.trimToEmpty(eOrderTag.replace("BANK_", ""));
			}
		}
		return null;
	}
	
	public static Map<String, Object> buildOrderDBMap(Map<String, Object> eOrderMap, Map<String, Object> dbOrderMap) {

		Long orderId = ShopifyUtil.getOrderId(eOrderMap);

		String orderName = (String) eOrderMap.get("name");
		logger.info("*** ShopifyUtil >> buildOrderDBMap >> orderName: " + orderName);
		logger.info("*** ShopifyUtil >> buildOrderDBMap >> orderId: " + orderId);

		Map<String, Object> orderDbMap = new LinkedHashMap<String, Object>();
		String eOrderTags = (String)eOrderMap.get("tags");
		orderDbMap.put("shopOrderId", orderId);
		orderDbMap.put("orderName", orderName);
		orderDbMap.put("appId", eOrderMap.get("app_id"));
		orderDbMap.put("cancelReason", eOrderMap.get("cancel_reason"));
		orderDbMap.put("cancelledAt", eOrderMap.get("cancelled_at"));
		orderDbMap.put("contactEmail", eOrderMap.get("contact_email"));
		orderDbMap.put("createdAt", eOrderMap.get("created_at"));
		orderDbMap.put("currentSubtotalPrice", eOrderMap.get("current_subtotal_price"));
		orderDbMap.put("currentTotalPrice", eOrderMap.get("current_total_price"));
		orderDbMap.put("currentTotalTax", eOrderMap.get("current_total_tax"));
		orderDbMap.put("orderNumber", eOrderMap.get("order_number"));
		orderDbMap.put("processedAt", eOrderMap.get("processed_at"));
		orderDbMap.put("orderStatusUrl", eOrderMap.get("order_status_url"));
		orderDbMap.put("subtotalPrice", eOrderMap.get("subtotal_price"));
		orderDbMap.put("tags", eOrderTags);
		orderDbMap.put("totalDiscounts", eOrderMap.get("total_discounts"));
		orderDbMap.put("totalLineItemsPrice", eOrderMap.get("total_line_items_price"));
		orderDbMap.put("totalTax", eOrderMap.get("total_tax"));
		orderDbMap.put("currency", eOrderMap.get("currency"));
		orderDbMap.put("financialStatus", eOrderMap.get("financial_status"));
		orderDbMap.put("fulfillmentStatus", eOrderMap.get("fulfillment_status"));

		orderDbMap.put("defaultAddressId", eOrderMap.get("default_address_id"));
		orderDbMap.put("updatedAt", eOrderMap.get("updated_at"));
		orderDbMap.put("payeeBank", getPayeeBank(eOrderTags));

		if (MapUtils.isNotEmpty(dbOrderMap) && dbOrderMap.containsKey("salesOrderNo")) {
			orderDbMap.put("salesOrderNo", dbOrderMap.get("salesOrderNo"));
		}

		List<Map<String, Object>> noteAttribs = (List<Map<String, Object>>) eOrderMap.get("note_attributes");
		if (CollectionUtils.isNotEmpty(noteAttribs)) {
			for (Map<String, Object> noteAttMap : noteAttribs) {
				String key = (String) noteAttMap.get("name");
				switch (key) {
				case "request-deliver-date":
					Date reqDate = DateUtil.getStringToDate(String.valueOf(noteAttMap.get("value")), "MM/dd/yyyy");
					orderDbMap.put("requestDeliveryDate", reqDate);
					break;
				}
			}
		}

		Map<String, Object> customerMap = (Map<String, Object>) eOrderMap.get("customer");
		orderDbMap.put("customerId", customerMap.get("id"));
		String customerTag = (String) customerMap.get("tags");
		String customerNo = ShopifyUtil.getSOCustomerNoByAddress(eOrderMap, customerMap);
		if (StringUtils.isNotBlank(customerNo)) {
			orderDbMap.put("soCustomerNo", customerNo);
		}
		

		return orderDbMap;
	}

	public static Map<String, Object> buildOrderDBMap(Map<String, Object> eOrderMap,
			List<Map<String, Object>> dbOrderList) {

		Map<String, Object> dbOrderMap = new HashMap<>();
		if (CollectionUtils.isNotEmpty(dbOrderList)) {
			dbOrderMap = dbOrderList.get(0);
		}

		Long orderId = ShopifyUtil.getOrderId(eOrderMap);

		String orderName = (String) eOrderMap.get("name");
		logger.info("*** ShopifyUtil >> buildOrderDBMap >> orderName: " + orderName);
		logger.info("*** ShopifyUtil >> buildOrderDBMap >> orderId: " + orderId);

		Map<String, Object> orderDbMap = new LinkedHashMap<String, Object>();

		orderDbMap.put("shopOrderId", orderId);
		orderDbMap.put("orderName", orderName);
		orderDbMap.put("appId", eOrderMap.get("app_id"));
		orderDbMap.put("cancelReason", eOrderMap.get("cancel_reason"));
		orderDbMap.put("cancelledAt", eOrderMap.get("cancelled_at"));
		orderDbMap.put("contactEmail", eOrderMap.get("contact_email"));
		orderDbMap.put("createdAt", eOrderMap.get("created_at"));
		orderDbMap.put("currentSubtotalPrice", eOrderMap.get("current_subtotal_price"));
		orderDbMap.put("currentTotalPrice", eOrderMap.get("current_total_price"));
		orderDbMap.put("currentTotalTax", eOrderMap.get("current_total_tax"));
		orderDbMap.put("orderNumber", eOrderMap.get("order_number"));
		orderDbMap.put("processedAt", eOrderMap.get("processed_at"));
		orderDbMap.put("orderStatusUrl", eOrderMap.get("order_status_url"));
		orderDbMap.put("subtotalPrice", eOrderMap.get("subtotal_price"));
		orderDbMap.put("tags", eOrderMap.get("tags"));
		orderDbMap.put("totalDiscounts", eOrderMap.get("total_discounts"));
		orderDbMap.put("totalLineItemsPrice", eOrderMap.get("total_line_items_price"));
		orderDbMap.put("totalTax", eOrderMap.get("total_tax"));
		orderDbMap.put("currency", eOrderMap.get("currency"));
		orderDbMap.put("financialStatus", eOrderMap.get("financial_status"));
		orderDbMap.put("fulfillmentStatus", eOrderMap.get("fulfillment_status"));

		orderDbMap.put("defaultAddressId", eOrderMap.get("default_address_id"));
		orderDbMap.put("updatedAt", eOrderMap.get("updated_at"));

		List<Map<String, Object>> noteAttribs = (List<Map<String, Object>>) eOrderMap.get("note_attributes");
		if (CollectionUtils.isNotEmpty(noteAttribs)) {
			for (Map<String, Object> noteAttMap : noteAttribs) {
				String key = (String) noteAttMap.get("name");
				switch (key) {
				case "request-deliver-date":
					Date reqDate = DateUtil.getStringToDate(String.valueOf(noteAttMap.get("value")), "MM/dd/yyyy");
					orderDbMap.put("requestDeliveryDate", reqDate);
					break;
				}
			}
		}

		Map<String, Object> customerMap = (Map<String, Object>) eOrderMap.get("customer");
		orderDbMap.put("customerId", customerMap.get("id"));
		String customerTag = (String) customerMap.get("tags");
		if (StringUtils.isNotBlank(customerTag)) {
			orderDbMap.put("soCustomerNo", ShopifyUtil.getSOCustomerNoByAddress(eOrderMap, customerMap));
		}

		return orderDbMap;
	}

	public static String getSONumberTagByOrderDB(String dbSalesOrderNo) {
		if (StringUtils.isNotBlank(dbSalesOrderNo)) {
			StringBuilder sbSOs = new StringBuilder();
			if (dbSalesOrderNo.contains(",")) {
				String[] soNumbers = dbSalesOrderNo.split(",");
				sbSOs.append(Constants.SALES_ORDER_NO_TAG).append(soNumbers[0]).append(",")
						.append(Constants.SALES_ORDER_NO_2_TAG).append(soNumbers[1]);
			} else {
				sbSOs.append(Constants.SALES_ORDER_NO_TAG).append(dbSalesOrderNo);
			}

			return sbSOs.toString();
		}
		return dbSalesOrderNo;
	}

	public static List<String> getSONumberByOrderTag(String tags) {
		List<String> soNos = new ArrayList<>();
		if (StringUtils.isNotBlank(tags)) {
			if (tags.contains(",")) {
				String[] tagArry = tags.split(",");
				for (String tag: tagArry) {
					if (tag.startsWith(Constants.SALES_ORDER_NO_TAG)) {
						String soNo = StringUtils.trimToEmpty(tag.replace(Constants.SALES_ORDER_NO_TAG, ""));
						soNos.add(soNo);
					}
					if (tag.startsWith(Constants.SALES_ORDER_NO_2_TAG)) {
						String soNo = StringUtils.trimToEmpty(tag.replace(Constants.SALES_ORDER_NO_2_TAG, ""));
						soNos.add(soNo);
					}
				}
				
				
			} else {
				if (tags.startsWith(Constants.SALES_ORDER_NO_TAG)) {
					String soNo = StringUtils.trimToEmpty(tags.replace(Constants.SALES_ORDER_NO_TAG, ""));
					soNos.add(soNo);
				}
			}
		}

		return soNos;
	}

	public static String getPONumberTagByOrderDB(String dbPurchseOrderNo) {
		if (StringUtils.isNotBlank(dbPurchseOrderNo)) {
			StringBuilder sbPOs = new StringBuilder();
			if (dbPurchseOrderNo.contains(",")) {
				String[] poNumbers = dbPurchseOrderNo.split(",");
				sbPOs.append(Constants.PURCHASE_ORDER_NO_TAG).append(poNumbers[0]).append(",")
						.append(Constants.PURCHASE_ORDER_NO_2_TAG).append(poNumbers[1]);
			} else {
				sbPOs.append(Constants.PURCHASE_ORDER_NO_TAG).append(dbPurchseOrderNo);
			}

			return sbPOs.toString();
		}
		return dbPurchseOrderNo;
	}

	public static String getSOStatusTagByOrderDB(String deliveryStatus) {
		if (StringUtils.isNotBlank(deliveryStatus)) {
			StringBuilder sbTags = new StringBuilder();
			sbTags.append(Constants.SALES_ORDER_STATUS_TAG).append(deliveryStatus);
			return sbTags.toString();
		}
		return "";
	}

	public static Map<String, Object> buildCustomerMapRequest(Map<String, Object> dbCustomerMap, int rowCtr) {
		Map<String, Object> rootMap = new HashMap<String, Object>();
		Map<String, Object> custMap = new LinkedHashMap<String, Object>();
		StringBuilder custTags = new StringBuilder();
		String fullName = (String) dbCustomerMap.get("fullName");
		HashMap<String, Object> nameMap = PCDataUtil.getFirstLastNameFromFullName(fullName);

		for (Map.Entry<String, Object> entry : dbCustomerMap.entrySet()) {

			switch (entry.getKey()) {
			case "firstName":
				String firstName = (String) entry.getValue();
				if (StringUtils.isBlank(firstName)) {
					firstName = (String) nameMap.get("firstName");
				}
				custMap.put("first_name", firstName);
				break;
			case "lastName":
				String lastName = (String) entry.getValue();
				if (StringUtils.isBlank(lastName)) {
					lastName = (String) nameMap.get("lastName");
				}
				custMap.put("last_name", lastName);
				break;
			case "email":
				String custEmail = StringUtils.trimToEmpty((String) entry.getValue());
				logger.info(" buildCustomerMapRequest >>  custEmail: " + custEmail);
				if (StringUtils.isNotBlank(custEmail)) {
					custMap.put("email", custEmail);
				}
				custMap.put("verified_email", true);
				custMap.put("password", Constants.TEMP_CUSTOMER_PASSWORD);
				custMap.put("password_confirmation", Constants.TEMP_CUSTOMER_PASSWORD);

				break;
			case "phoneNumber":
				if (StringUtils.isNotBlank((String) entry.getValue()))
					custMap.put("phone", entry.getValue());
				break;
			case "customerNumber":
				if (StringUtils.isNotBlank((String) entry.getValue())) {
					String custNoTag = Constants.CUSTOMER_NO_TAG_CODE
							+ StringUtils.trimToEmpty((String) entry.getValue());
					custTags.append(custNoTag).append(Constants.COMMA);
				}
				break;
			case "custType":
				if (StringUtils.isNotBlank((String) entry.getValue())) {
					String typeTag = "TYPE_" + StringUtils.trimToEmpty((String) entry.getValue());
					custTags.append(typeTag).append(Constants.COMMA);
				}
				break;
			case "custGroup":
				if (StringUtils.isNotBlank((String) entry.getValue())) {
					String custGroupTag = "CGRP_" + StringUtils.trimToEmpty((String) entry.getValue());
					custTags.append(custGroupTag).append(Constants.COMMA);
				}
				break;
			case "oosWarehouse1":
				if (StringUtils.isNotBlank((String) entry.getValue())) {
					String whCode1 = "WHC1_" + StringUtils.trimToEmpty((String) entry.getValue());
					custTags.append(whCode1).append(Constants.COMMA);
				}
				break;
			case "oosWarehouse2":
				if (StringUtils.isNotBlank((String) entry.getValue())) {
					String whCode2 = "WHC2_" + StringUtils.trimToEmpty((String) entry.getValue());
					custTags.append(whCode2).append(Constants.COMMA);
				}
				break;
			case "storeName":
				if (StringUtils.isNotBlank((String) entry.getValue())) {
					String storeTag = "STORE_" + StringUtils.trimToEmpty((String) entry.getValue());
					custTags.append(storeTag).append(Constants.COMMA);
				}
				break;
			}

			if (custTags.length() > 0) {
				custMap.put("tags", custTags.toString());
			}

		}

		rootMap.put("customer", custMap);

		return rootMap;
	}

	public static Map<String, Object> buildCustAddressRequestForUpdate(Map<String, Object> dbCustomerMap,
			Map<String, Object> eAddrMap) {
		Map<String, Object> rootMap = new HashMap<String, Object>();
		Map<String, Object> addrMap = new LinkedHashMap<String, Object>();

		rootMap.put("customer_address", addrMap);
		return rootMap;
	}

	public static Map<String, Object> buildCustomerMapRequestForUpdate(Map<String, Object> dbCustomerMap) {
		Map<String, Object> rootMap = new HashMap<String, Object>();
		Map<String, Object> custMap = new LinkedHashMap<String, Object>();
		StringBuilder custTags = new StringBuilder();
		String fullName = (String) dbCustomerMap.get("fullName");
		HashMap<String, Object> nameMap = PCDataUtil.getFirstLastNameFromFullName(fullName);

		for (Map.Entry<String, Object> entry : dbCustomerMap.entrySet()) {

			switch (entry.getKey()) {
			case "firstName":
				String firstName = (String) entry.getValue();
				if (StringUtils.isBlank(firstName)) {
					firstName = (String) nameMap.get("firstName");
				}
				custMap.put("first_name", firstName);
				break;
			case "lastName":
				String lastName = (String) entry.getValue();
				if (StringUtils.isBlank(lastName)) {
					lastName = (String) nameMap.get("lastName");
				}
				custMap.put("last_name", lastName);
				break;

			case "phoneNumber":
				if (StringUtils.isNotBlank((String) entry.getValue()))
					custMap.put("phone", entry.getValue());
				break;
			case "customerNumber":
				if (StringUtils.isNotBlank((String) entry.getValue())) {
					String custNoTag = Constants.CUSTOMER_NO_TAG_CODE
							+ StringUtils.trimToEmpty((String) entry.getValue());
					custTags.append(custNoTag).append(Constants.COMMA);
				}
				break;
			case "custType":
				if (StringUtils.isNotBlank((String) entry.getValue())) {
					String typeTag = "TYPE_" + StringUtils.trimToEmpty((String) entry.getValue());
					custTags.append(typeTag).append(Constants.COMMA);
				}
				break;
			case "custGroup":
				if (StringUtils.isNotBlank((String) entry.getValue())) {
					String custGroupTag = "CGRP_" + StringUtils.trimToEmpty((String) entry.getValue());
					custTags.append(custGroupTag).append(Constants.COMMA);
				}
				break;
			case "oosWarehouse1":
				if (StringUtils.isNotBlank((String) entry.getValue())) {
					String whCode1 = "WHC1_" + StringUtils.trimToEmpty((String) entry.getValue());
					custTags.append(whCode1).append(Constants.COMMA);
				}
				break;
			case "oosWarehouse2":
				if (StringUtils.isNotBlank((String) entry.getValue())) {
					String whCode2 = "WHC2_" + StringUtils.trimToEmpty((String) entry.getValue());
					custTags.append(whCode2).append(Constants.COMMA);
				}
				break;
			case "storeName":
				if (StringUtils.isNotBlank((String) entry.getValue())) {
					String storeTag = "STORE_" + StringUtils.trimToEmpty((String) entry.getValue());
					custTags.append(storeTag).append(Constants.COMMA);
				}
				break;
			}

			if (custTags.length() > 0) {
				custMap.put("tags", custTags.toString());
			}

		}

		rootMap.put("customer", custMap);

		return rootMap;
	}

	public static Map<String, Object> buildCustomerAddressMapRequest_ORIG(Map<String, Object> dbCustomerMap) {
		Map<String, Object> rootMap = new HashMap<String, Object>();
		Map<String, Object> custMap = new LinkedHashMap<String, Object>();
		if (StringUtils.isNotBlank((String) dbCustomerMap.get("address"))) {

			for (Map.Entry<String, Object> entry : dbCustomerMap.entrySet()) {

				switch (entry.getKey()) {
				case "first_name":
					if (StringUtils.isNotBlank((String) entry.getValue()))
						custMap.put("first_name", entry.getValue());
					break;
				case "last_name":
					if (StringUtils.isNotBlank((String) entry.getValue()))
						custMap.put("last_name", entry.getValue());
					break;
				case "address":
					if (StringUtils.isNotBlank((String) entry.getValue()))
						custMap.put("address1", entry.getValue());
					break;
				case "street":
					if (StringUtils.isNotBlank((String) entry.getValue()))
						custMap.put("address2", entry.getValue());
					break;
				case "town_city":
					if (StringUtils.isNotBlank((String) entry.getValue()))
						custMap.put("city", entry.getValue());
					break;

				}

				custMap.put("country", "Philippines");
				custMap.put("country_code", "PH");
			}
		}

		rootMap.put("address", custMap);
		return rootMap;
	}

	public static Map<String, Object> buildCustomerAddressMapRequest(Map<String, Object> dbCustomerMap) {
		Map<String, Object> rootMap = new HashMap<String, Object>();
		Map<String, Object> custMap = new LinkedHashMap<String, Object>();
		if (StringUtils.isNotBlank((String) dbCustomerMap.get("address"))) {

			for (Map.Entry<String, Object> entry : dbCustomerMap.entrySet()) {

				switch (entry.getKey()) {
				case "firstName":
					if (StringUtils.isNotBlank((String) entry.getValue()))
						custMap.put("first_name", entry.getValue());
					break;
				case "lastName":
					if (StringUtils.isNotBlank((String) entry.getValue()))
						custMap.put("last_name", entry.getValue());
					break;
				case "fullName":
					if (StringUtils.isNotBlank((String) entry.getValue()))
						custMap.put("company", entry.getValue());
					break;
				case "address":
					if (StringUtils.isNotBlank((String) entry.getValue()))
						custMap.put("address1", entry.getValue());
					break;
				case "street":
					if (StringUtils.isNotBlank((String) entry.getValue()))
						custMap.put("address2", entry.getValue());
					break;
				case "townCity":
					if (StringUtils.isNotBlank((String) entry.getValue()))
						custMap.put("city", entry.getValue());
					break;

				case "zipCode":
					if (StringUtils.isNotBlank((String) entry.getValue()))
						custMap.put("zip", entry.getValue());
					break;

				}

				String shopProvince = ShopifyUtil.getProvinceFromAddress((String) dbCustomerMap.get("address"));
				if (StringUtils.isNotBlank(shopProvince)) {
					custMap.put("province", shopProvince);
				}
				custMap.put("country", "Philippines");
				custMap.put("country_code", "PH");
			}
		}

		rootMap.put("address", custMap);
		return rootMap;
	}

	public static Map<String, Object> buildCustomerAddressForUpdate(Map<String, Object> dbAddressMap,
			Map<String, Object> eAddrMap) {
		Map<String, Object> rootMap = new HashMap<String, Object>();
		Map<String, Object> custMap = new LinkedHashMap<String, Object>();

		String custAddress = StringUtils.trimToEmpty((String) dbAddressMap.get("address"));

		if (StringUtils.isNotBlank(custAddress)) {
			String customerNo = (String) dbAddressMap.get("customerNumber");
			String fullName = StringUtils.trimToEmpty((String) dbAddressMap.get("fullName"));

			String warehouseTag = new StringBuilder((String) dbAddressMap.get("oosWarehouse1")).append(",")
					.append((String) dbAddressMap.get("oosWarehouse2")).toString();

			custMap.put("first_name", customerNo);
			custMap.put("last_name", fullName);
			custMap.put("company", fullName);

			custMap.put("address1", custAddress);
			custMap.put("address2", warehouseTag);
			custMap.put("city", StringUtils.trimToEmpty((String) dbAddressMap.get("townCity")));
			custMap.put("zip", StringUtils.trimToEmpty((String) dbAddressMap.get("zipCode")));
			String shopProvince = ShopifyUtil.getProvinceFromAddress(custAddress);
			if (StringUtils.isNotBlank(shopProvince)) {
				custMap.put("province", shopProvince);
			}
			custMap.put("country", "Philippines");
			custMap.put("country_code", "PH");
		}

		rootMap.put("address", custMap);
		return rootMap;

	}

	public static Map<String, Object> buildCustomerAddrMapRequestByEmail(Map<String, Object> dbCustomerMap) {
		Map<String, Object> rootMap = new HashMap<String, Object>();
		Map<String, Object> custMap = new LinkedHashMap<String, Object>();

		String custAddress = StringUtils.trimToEmpty((String) dbCustomerMap.get("address"));
		if (StringUtils.isNotBlank(custAddress)) {
			String customerNo = (String) dbCustomerMap.get("customerNumber");
			String fullName = StringUtils.trimToEmpty((String) dbCustomerMap.get("fullName"));

			String warehouseTag = new StringBuilder((String) dbCustomerMap.get("oosWarehouse1")).append(",")
					.append((String) dbCustomerMap.get("oosWarehouse2")).toString();

			custMap.put("first_name", customerNo);
			custMap.put("last_name", fullName);
			custMap.put("company", fullName);
			custMap.put("address1", custAddress);
			custMap.put("address2", warehouseTag);
			custMap.put("city", StringUtils.trimToEmpty((String) dbCustomerMap.get("townCity")));
			custMap.put("zip", StringUtils.trimToEmpty((String) dbCustomerMap.get("zipCode")));

			String shopProvince = ShopifyUtil.getProvinceFromAddress(custAddress);
			if (StringUtils.isNotBlank(shopProvince)) {
				custMap.put("province", shopProvince);
			}
			custMap.put("country", "Philippines");
			custMap.put("country_code", "PH");
		}

		rootMap.put("address", custMap);
		return rootMap;
	}

	public static String getProvinceFromAddress(String address) {
		if (StringUtils.isNotBlank(address)) {
			String uAddress = StringUtils.trimToEmpty(address).toUpperCase();
			for (String shopProv : provinceList) {
				if (uAddress.contains(StringUtils.upperCase(shopProv))) {
					return shopProv;
				}
			}
		}
		return "Metro Manila";
	}

	public static String getMetroManilaFromAddress(String address) {
		if (StringUtils.isNotBlank(address)) {
			String uAddress = StringUtils.trimToEmpty(address).toUpperCase();
			for (String shopMetro : metroManilaList) {
				if (uAddress.contains(StringUtils.upperCase(shopMetro))) {
					return "Metro Manila";
				}
			}
		}
		return null;
	}
	
	
	public static String getShopProductHandle(String productName, String prodItemNo) {
		if (isNotEmpty(productName)) {
			 
			prodItemNo = StringUtils.trimToEmpty(prodItemNo);
	 		String eProdTitle = new StringBuilder(StringUtils.trimToEmpty(productName)).append(prodItemNo).toString();
			String cleanTitle = eProdTitle.replaceAll("[^a-zA-Z0-9\\s]", "");
			String handle = cleanTitle.replaceAll("\\s", "-").toLowerCase()
					.replaceAll("---", "-").replaceAll("--", "-");
		 
			return handle;
		}
		
		return null;
	}
	
	public static Map<String, Object> buildProductMapRequest(Map<String, Object> dbProdMap) {

		Map<String, Object> rootMap = new HashMap<String, Object>();
		Map<String, Object> productMap = new LinkedHashMap<String, Object>();
		productMap.put("vendor", "PotatoCorner");
		productMap.put("published_scope", "global");

		Map<String, Object> variantMap = new LinkedHashMap<>();
		variantMap.put("fulfillment_service", "manual");
		variantMap.put("taxable", false);

		List<Map<String, Object>> variantList = new ArrayList<>();
		String itemId = (String) dbProdMap.get("itemNumber");
		ArrayList<String> tags = new ArrayList<>();

		for (Map.Entry<String, Object> entry : dbProdMap.entrySet()) {
			switch (entry.getKey()) {
			case "name":
				String nameWithId = new StringBuilder((String) entry.getValue()).append(" (").append(itemId).append(")")
						.toString();
				productMap.put("title", nameWithId);
				productMap.put("body_html", nameWithId);
				variantMap.put("title", nameWithId);
				// variantMap.put("sku", itemId);
				break;
			case "sku":
				if (StringUtils.isNotBlank((String) entry.getValue())) {
					variantMap.put("sku", (String) entry.getValue());
				} else {
					String whCode = (String) dbProdMap.get("warehouseCode");
					StringBuilder sbSku = new StringBuilder(itemId);
					if (StringUtils.isNotBlank(whCode)) {
						sbSku.append("-").append(whCode);
					}
					variantMap.put("sku", sbSku.toString());
				}

				break;
			case "salesUnitId":
				variantMap.put("weight_unit", "");
				String uom = Constants.UNIT_OF_MEASURE_TAG_CODE.concat((String) entry.getValue());
				tags.add(uom);
				break;
			case "itemNumber":
				tags.add((String) entry.getValue());
				break;
			case "itemGroup":
				String itemGrp = Constants.ITEM_GROUP_TAG_CODE.concat((String) entry.getValue());
				tags.add(itemGrp);
				productMap.put("product_type", entry.getValue());
				break;
			case "sellPrice":
				variantMap.put("price", String.valueOf(entry.getValue()));
				break;
			case "warehouseCode":
				String whCode = Constants.WARESHOUSE_CODE_TAG_CODE.concat((String) entry.getValue());
				tags.add(whCode);
				break;
			case "warehouseSite":
				String whSite = Constants.WARESHOUSE_SITE_TAG_CODE.concat((String) entry.getValue());
				tags.add(whSite);
				break;
			case "oosMoq":
				String moqValue = Constants.MOQ_TAG_CODE.concat(String.valueOf((int) entry.getValue()));
				tags.add(moqValue);
				break;
			case "oosBoq":
				String boqValue = Constants.BOQ_TAG_CODE.concat(String.valueOf((int) entry.getValue()));
				tags.add(boqValue);
				break;
			case "physicalInventory":

				variantMap.put("inventory_policy", "deny");
				variantMap.put("inventory_management", "shopify");
				variantMap.put("inventory_quantity", (int) Double.parseDouble(String.valueOf(entry.getValue())));
				break;
			}
		}

		productMap.put("tags", tagListToString(tags));

		// Default values
		productMap.put("variants", variantList);
		variantList.add(variantMap);

		// IMAGES
		productMap.put("variants", variantList);

		rootMap.put("product", productMap);
		logger.info("*** ShopifyUtil >> buildProductMapRequest >> rootMap:" + rootMap.toString());

		return rootMap;
	}

	public static Map<String, Object> buildProductRequestForUpdate(Map<String, Object> dbProdMap,
			Map<String, Object> eProdMap) {

		String itemId = (String) dbProdMap.get("itemNumber");
		String name = (String) dbProdMap.get("name");
		Map<String, Object> rootMap = new HashMap<String, Object>();
		Map<String, Object> newProdMap = new LinkedHashMap<>();
		ArrayList<String> tags = new ArrayList<>();

		if (MapUtils.isNotEmpty(eProdMap)) {
			String nameWithId = new StringBuilder(name).append(" (").append(itemId).append(")").toString();

			newProdMap.put("id", NumberUtil.getLongValue(eProdMap, "id"));
			newProdMap.put("title", nameWithId);
			newProdMap.put("body_html", nameWithId);
			tags.add(itemId);

			if (dbProdMap.get("salesUnitId") != null) {
				String uom = Constants.UNIT_OF_MEASURE_TAG_CODE.concat((String) dbProdMap.get("salesUnitId"));
				tags.add(uom);
			}

			if (dbProdMap.get("itemGroup") != null) {
				String itemGrp = Constants.ITEM_GROUP_TAG_CODE.concat((String) dbProdMap.get("itemGroup"));
				tags.add(itemGrp);
			}

			if (dbProdMap.get("warehouseCode") != null) {
				String whCode = Constants.WARESHOUSE_CODE_TAG_CODE.concat((String) dbProdMap.get("warehouseCode"));
				tags.add(whCode);
			}

			if (dbProdMap.get("warehouseSite") != null) {
				String whSite = Constants.WARESHOUSE_SITE_TAG_CODE.concat((String) dbProdMap.get("warehouseSite"));
				tags.add(whSite);
			}

			if (dbProdMap.get("oosMoq") != null) {
				String moqValue = Constants.MOQ_TAG_CODE.concat(String.valueOf((int) dbProdMap.get("oosMoq")));
				tags.add(moqValue);
			}

			if (dbProdMap.get("oosBoq") != null) {
				String boqValue = Constants.BOQ_TAG_CODE.concat(String.valueOf((int) dbProdMap.get("oosBoq")));
				tags.add(boqValue);
			}

			newProdMap.put("tags", tagListToString(tags));

			List<Map<String, Object>> newVariants = new ArrayList<>();
			List<Map<String, Object>> currVariants = (List<Map<String, Object>>) eProdMap.get("variants");
			for (Map<String, Object> currVarMap : currVariants) {
				Map<String, Object> newVariant = new LinkedHashMap<>();
				newVariant.put("id", NumberUtil.getLongValue(currVarMap, "id"));
				newVariant.put("price", Double.parseDouble(String.valueOf(dbProdMap.get("sellPrice"))));
				newVariant.put("inventory_item_id", NumberUtil.getLongValue(currVarMap, "inventory_item_id"));
				newVariant.put("inventory_quantity",
						Integer.parseInt(String.valueOf(dbProdMap.get("physicalInventory"))));

				newVariants.add(newVariant);
			}

			newProdMap.put("variants", newVariants);
		}

		rootMap.put("product", newProdMap);
		logger.info("*** ShopifyUtil >> buildProductMapRequest >> rootMap:" + rootMap.toString());

		return rootMap;
	}

	public static Map<String, Object> buildProductRequestForUpdate_V2(Map<String, Object> dbProdMap,
			Map<String, Object> eProdMap) {

		String itemId = (String) dbProdMap.get("itemNumber");
		String name = (String) dbProdMap.get("name");
		Map<String, Object> rootMap = new HashMap<String, Object>();

		ArrayList<String> tags = new ArrayList<>();

		if (MapUtils.isNotEmpty(eProdMap)) {
			String nameWithId = new StringBuilder(name).append(" (").append(itemId).append(")").toString();

			eProdMap.put("title", nameWithId);
			eProdMap.put("body_html", nameWithId);
			tags.add(itemId);

			if (dbProdMap.get("salesUnitId") != null) {
				String uom = Constants.UNIT_OF_MEASURE_TAG_CODE.concat((String) dbProdMap.get("salesUnitId"));
				tags.add(uom);
			}

			if (dbProdMap.get("itemGroup") != null) {
				String itemGrp = Constants.ITEM_GROUP_TAG_CODE.concat((String) dbProdMap.get("itemGroup"));
				tags.add(itemGrp);
			}

			if (dbProdMap.get("warehouseCode") != null) {
				String whCode = Constants.WARESHOUSE_CODE_TAG_CODE.concat((String) dbProdMap.get("warehouseCode"));
				tags.add(whCode);
			}

			if (dbProdMap.get("warehouseSite") != null) {
				String whSite = Constants.WARESHOUSE_SITE_TAG_CODE.concat((String) dbProdMap.get("warehouseSite"));
				tags.add(whSite);
			}

			if (dbProdMap.get("oosMoq") != null) {
				String moqValue = Constants.MOQ_TAG_CODE.concat(String.valueOf((int) dbProdMap.get("oosMoq")));
				tags.add(moqValue);
			}

			if (dbProdMap.get("oosBoq") != null) {
				String boqValue = Constants.BOQ_TAG_CODE.concat(String.valueOf((int) dbProdMap.get("oosBoq")));
				tags.add(boqValue);
			}

			List<Map<String, Object>> newVariants = new ArrayList<>();
			List<Map<String, Object>> currVariants = (List<Map<String, Object>>) eProdMap.get("variants");
			for (Map<String, Object> currVarMap : currVariants) {
				int iPosVar = (int) Double.parseDouble(String.valueOf(currVarMap.get("position")));
				int iPosInv = (int) Double.parseDouble(String.valueOf(currVarMap.get("inventory_quantity")));
				int iPosOldInv = (int) Double.parseDouble(String.valueOf(currVarMap.get("old_inventory_quantity")));
				currVarMap.put("position", iPosVar);
				currVarMap.put("inventory_quantity", iPosInv);
				currVarMap.put("old_inventory_quantity", iPosOldInv);
				newVariants.add(currVarMap);
			}

			List<Map<String, Object>> currOptions = (List<Map<String, Object>>) eProdMap.get("options");
			for (Map<String, Object> currOptMap : currOptions) {
				int iPosImg = (int) Double.parseDouble(String.valueOf(currOptMap.get("position")));
				currOptMap.put("position", iPosImg);
			}

			List<Map<String, Object>> currImgs = (List<Map<String, Object>>) eProdMap.get("images");
			for (Map<String, Object> currImgMap : currImgs) {
				int iPosImg = (int) Double.parseDouble(String.valueOf(currImgMap.get("position")));
				currImgMap.put("position", iPosImg);
			}

			Map<String, Object> currImg = (Map<String, Object>) eProdMap.get("image");
			int iPosImge = (int) Double.parseDouble(String.valueOf(currImg.get("position")));
			currImg.put("position", iPosImge);

			eProdMap.put("image", currImg);
			eProdMap.put("images", currImgs);
			eProdMap.put("options", currOptions);
			eProdMap.put("variants", newVariants);
		}

		// *********************************************************
//		Map<String, Object> rootMap = new HashMap<String, Object>();
//		Map<String, Object> productMap = new LinkedHashMap<String, Object>();
//		productMap.put("vendor", "PotatoCorner");
//		productMap.put("published_scope", "global");
//
//		Map<String, Object> variantMap = new LinkedHashMap<>();
//		variantMap.put("fulfillment_service", "manual");
//		variantMap.put("taxable", false);
//
//		List<Map<String, Object>> variantList = new ArrayList<>();
//		String itemId = (String) dbProdMap.get("itemNumber");
//		ArrayList<String> tags = new ArrayList<>();
//		
//		for (Map.Entry<String, Object> entry : dbProdMap.entrySet()) {
//			switch (entry.getKey()) {
//			case "name":
//				String nameWithId = new StringBuilder((String) entry.getValue()).append(" (").append(itemId).append(")")
//						.toString();
//				productMap.put("title", nameWithId);
//				productMap.put("body_html", nameWithId);
//				variantMap.put("title", nameWithId);
//				// variantMap.put("sku", itemId);
//				break;
//			case "sku":
//				if (StringUtils.isNotBlank((String) entry.getValue())) {
//					variantMap.put("sku", (String) entry.getValue());
//				} else {
//					String whCode = (String) dbProdMap.get("warehouseCode");
//					StringBuilder sbSku = new StringBuilder(itemId);
//					if (StringUtils.isNotBlank(whCode)) {
//						sbSku.append("-").append(whCode);
//					}
//					variantMap.put("sku", sbSku.toString());
//				}
//
//				break;
//			case "salesUnitId":
//				variantMap.put("weight_unit", "");
//				String uom = Constants.UNIT_OF_MEASURE_TAG_CODE.concat((String) entry.getValue());
//				tags.add(uom);
//				break;
//			case "itemNumber":
//				tags.add((String) entry.getValue());
//				break;
//			case "itemGroup":
//				String itemGrp = Constants.ITEM_GROUP_TAG_CODE.concat((String) entry.getValue());
//				tags.add(itemGrp);
//				productMap.put("product_type", entry.getValue());
//				break;
//			case "sellPrice":
//				variantMap.put("price", String.valueOf(entry.getValue()));
//				break;
//			case "warehouseCode":
//				String whCode = Constants.WARESHOUSE_CODE_TAG_CODE.concat((String) entry.getValue());
//				tags.add(whCode);
//				break;
//			case "warehouseSite":
//				String whSite = Constants.WARESHOUSE_SITE_TAG_CODE.concat((String) entry.getValue());
//				tags.add(whSite);
//				break;
//			case "oosMoq":
//				String moqValue = Constants.MOQ_TAG_CODE.concat(String.valueOf((int) entry.getValue()));
//				tags.add(moqValue);
//				break;
//			case "oosBoq":
//				String boqValue = Constants.BOQ_TAG_CODE.concat(String.valueOf((int) entry.getValue()));
//				tags.add(boqValue);
//				break;
//			case "physicalInventory":
//
//				variantMap.put("inventory_policy", "deny");
//				variantMap.put("inventory_management", "shopify");
//				variantMap.put("inventory_quantity", (int) Double.parseDouble(String.valueOf(entry.getValue())));
//				break;
//			}
//		}
//
//		productMap.put("tags", tagListToString(tags));
//
//		// Default values
//		productMap.put("variants", variantList);
//		variantList.add(variantMap);
//
//		// IMAGES
//		productMap.put("variants", variantList);
//
		rootMap.put("product", eProdMap);
		logger.info("*** ShopifyUtil >> buildProductMapRequest >> rootMap:" + rootMap.toString());

		return rootMap;
	}

	public static Map<String, Object> buildNewProductMapFroEdit(Map<String, Object> shopProductMap) {

		Map<String, Object> rootMap = new HashMap<String, Object>();
		Map<String, Object> productMap = new LinkedHashMap<String, Object>();

		Map<String, Object> variantMap = new LinkedHashMap<>();

		List<Map<String, Object>> variantList = new ArrayList<>();

		productMap.put("id", shopProductMap.get("id"));
//		productMap.put("title", shopProductMap);
//		productMap.put("body_html", nameWithId);
//		variantMap.put("title", nameWithId);
//		 
//		variantMap.put("price", String.valueOf(entry.getValue()));
//		variantMap.put("inventory_quantity", (int) Double.parseDouble(String.valueOf(entry.getValue())));

		// Default values
		productMap.put("variants", variantList);
		variantList.add(variantMap);

		// IMAGES
		productMap.put("variants", variantList);

		rootMap.put("product", productMap);
		logger.info("*** ShopifyUtil >> buildNewProductMapFroEdit >> rootMap:" + rootMap.toString());

		return rootMap;
	}

	public static String jsonGetParamBuilder(HashMap<String, Object> paramMap, boolean isFirstParamAnd) {

		if (MapUtils.isNotEmpty(paramMap)) {
			String andSign = "";
			if (isFirstParamAnd) {
				andSign = "&";
			}

			StringBuilder sb = new StringBuilder(andSign);
			Map<String, Object> params = paramMap;
			for (Map.Entry<String, Object> entry : params.entrySet()) {
				logger.info(" jsonGetRequestBuilder >> Key : " + entry.getKey() + " Value : " + entry.getValue());
				sb.append(entry.getKey()).append("=").append(String.valueOf(entry.getValue())).append("&");
			}

			return sb.substring(0, sb.length() - 1);
		}

		return "";
	}

	public static Map<String, Object> getMatchAddressByCustomerNo(String customerNo,
			List<Map<String, Object>> dbAddrList) {
		if (StringUtils.isNotBlank(customerNo)) {
			for (Map<String, Object> dbAddr : dbAddrList) {
				String dbCustomerNo = StringUtils.trimToEmpty((String) dbAddr.get("customerNumber"));
				if (customerNo.equals(dbCustomerNo))
					return dbAddr;
			}
		}
		return null;
	}

	public static String tagListToString(List<String> tags) {
		StringBuilder sb = new StringBuilder();
		for (String tag : tags) {
			sb.append(tag).append(Constants.COMMA);
		}
		return sb.substring(0, sb.length() - 1);
	}

	public static void main(String[] args) {
//		String itemId = ShopifyUtil.getD365ItemIdFromTitle("ALCOHOL SPRAY CARD 75% ALCOHOL 20ml (PC00000278)");

		String province = ShopifyUtil.getProvinceFromAddress(
				"Gaisano Grand Citimall  - Satll D-17 Gaisano Mall Citimall , Ilustre St, Brgy 3-A Poblacion District, Davao City, 8000 Davao del Sur\\nDavao City MIN 8000\\nPHL");

		System.out.println(province);
	}

}
