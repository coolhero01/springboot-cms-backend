package com.oneclicktech.spring.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;

import com.google.gson.Gson;
import com.oneclicktech.spring.domain.Constants;

public class PCDataUtil {

	private static final Logger logger = Logger.getLogger("PCDataUtil");

	public static int GROUP_CUSTOMER_NO_MIN_SIZE = 5;
	public static int GROUP_CUSTOMER_NO_MAX_SIZE = 8;
	public static int UPAY_MOBILE_CHAR_LIMIT = 10;

	public static synchronized String getUniqueId(String prefix) {
		SimpleDateFormat sdf = new SimpleDateFormat("MMddYYHHmmss");
		StringBuilder sb = new StringBuilder(prefix).append(sdf.format(new Date()));
		return sb.toString();
	}

	public static List<Map<String, String>> getSalesOrderDuplicates(Map<String, Object> eOrderMap) {

		if (MapUtils.isNotEmpty(eOrderMap)) {
			String orderTags = (String) eOrderMap.get("tags");
			List<String> soNos = ShopifyUtil.getSONumberByOrderTag(orderTags);

		}

		return null;
	}

	public static String getStringCellValue(Cell cell) {
		String newValue = null;
		if (cell != null) {
			switch (cell.getCellType()) {
			case STRING:
				newValue = cell.getStringCellValue();
				break;
			case NUMERIC:
				newValue = String.valueOf(cell.getNumericCellValue());
				break;
			case BOOLEAN:
				break;
			case FORMULA:
				break;
			default: 
				newValue = cell.getStringCellValue();
		  	}
		}
		
		return newValue;
	}

	public static double computeInterestValue(Double totalAmt, Double interestRate) {

		double intrstPercent = (new Double(interestRate) / 100);
		double interestVal = (totalAmt * intrstPercent);

		return interestVal;
	}

	public static boolean skipFulfilledOrders(String fulfillStatus, String financialStatus, String soCustomerNo,
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

	public static boolean skipFulfilledForBlankData(String fulfillStatus, String financialStatus, String soCustomerNo,
			String orderTags) {

		if (StringUtils.isBlank(soCustomerNo))
			return true;

		if (StringUtils.isNotBlank(orderTags) && orderTags.contains("TEST_ONLY"))
			return true;

		return false;
	}

	public static boolean checkOrderIfStaggeredIssuance(String orderTag, Map<String, Object> paramMap) {
		if (StringUtils.isNotBlank(orderTag) && orderTag.contains("STAGGERED_PAY_ORDER")) {
			return true;
		}

		if (MapUtils.isNotEmpty(paramMap) && paramMap.get("staggeredPayOrder") != null
				&& (Boolean) paramMap.get("staggeredPayOrder")) {
			return true;
		}

		return false;
	}

	public static synchronized Double computeEWTDiscount(Double subTotal, Double vatTax, Double ewtDiscount) {
		DecimalFormat decFormat = new DecimalFormat("####0.00");
		double totalNoVat = (subTotal / vatTax);
		double totalWithDiscount = NumberUtil.roundTwoDec(totalNoVat * ewtDiscount);

		return Double.valueOf(decFormat.format(totalWithDiscount));
	}

	public static boolean isAnEWTOrder(Map<String, Object> eOrderMap) {
		if (eOrderMap.get("line_items") != null) {
			List<Map<String, Object>> lineItems = (List<Map<String, Object>>) eOrderMap.get("line_items");
			for (Map<String, Object> lineItem : lineItems) {
				String itemTitle = StringUtils.upperCase((String) lineItem.get("title"));
				if (itemTitle.contains("EWT PROOF")) {
					return true;
				}

			}
		}

		return false;
	}

	public static boolean isEWTItemByName(String itemTitle) {
		if (StringUtils.isNotBlank(itemTitle) && StringUtils.upperCase(itemTitle).contains("EWT PROOF")) {
			return true;
		}

		return false;
	}

	public static String getPaymentJournalNo(String resultMsg) {
		if (StringUtils.isNotBlank(resultMsg)) {
			String payJournalNo = resultMsg.substring(resultMsg.indexOf("CIPC_"), resultMsg.lastIndexOf("created") - 1);
			return StringUtils.trimToEmpty(payJournalNo);
		}

		return null;
	}

	public static Map<String, String> getEWTItemInfo(Map<String, Object> eOrderMap) {
		Map<String, String> ewtInfo = new HashMap<String, String>();
		if (eOrderMap.get("line_items") != null) {
			List<Map<String, Object>> lineItems = (List<Map<String, Object>>) eOrderMap.get("line_items");
			for (Map<String, Object> lineItem : lineItems) {
				String itemTitle = StringUtils.upperCase((String) lineItem.get("title"));
				List<Map<String, Object>> itemProps = (List<Map<String, Object>>) lineItem.get("properties");
				if (itemTitle.contains("EWT PROOF") && CollectionUtils.isNotEmpty(itemProps)) {
					Map<String, Object> itemProp = itemProps.get(0);
					ewtInfo.put("ewtTitle", itemTitle);
					ewtInfo.put("ewtFileLink", (String) itemProp.get("value"));
					break;
				}

			}
		}

		return ewtInfo;
	}

	public static boolean isStagIssuanceFullPayment(Map<String, Object> eOrderMap) {
		if (eOrderMap.get("note_attributes") != null) {
			List<Map<String, Object>> noteAttribs = (List<Map<String, Object>>) eOrderMap.get("note_attributes");
			for (Map<String, Object> noteMap : noteAttribs) {
				String nameAttr = (String) noteMap.get("name");
				String valueAttr = (String) noteMap.get("value");
				if (nameAttr.equals("PAYMENT SCHEME") && valueAttr.equals("FULL PAYMENT")) {
					return true;
				}
			}
		}

		return false;
	}

	public static String getAllItemIdsByList(List<Map<String, Object>> itemList, String itemKey) {
		StringBuilder sb = new StringBuilder();
		if (CollectionUtils.isNotEmpty(itemList)) {
			for (Map<String, Object> iteMap : itemList) {
				String itemNo = (String) iteMap.get(itemKey);
				if (StringUtils.isNotBlank(itemNo))
					sb.append("'").append(itemNo).append("',");
			}

			if (sb.length() > 1) {
				return sb.substring(0, sb.length() - 1);
			}

		}

		return null;
	}

	public static String replaceInvalidChar(String txtMsg, String replaceValue) {
		if (StringUtils.isNotBlank(txtMsg)) {
			String regex = "[^A-Za-z0-9.,;{}()-\\/#&]+";
			String result = txtMsg.replaceAll(regex, replaceValue);
			return result;
		}
		return "";
	}

	public static String buildSalesOrderTags(Map<String, Object> dbOrderMap, Map<String, Object> eOrderMap,
			Map<String, Object> soOrderMap) {

		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
		StringBuilder soTags = new StringBuilder();

		if (StringUtils.isNotBlank((String) soOrderMap.get("DeliveryDate"))) {
			Date delivDate = DateUtil.stringToDate((String) soOrderMap.get("DeliveryDate"), "yyyy-MM-dd'T'HH:mm:ss");
			soTags.append(Constants.SO_DELIVERY_DATE_TAG).append(sdf.format(delivDate)).append(",");
		}

		if (StringUtils.isNotBlank((String) soOrderMap.get("DeliveryStatus"))) {
			String spDelivStatus = (String) soOrderMap.get("DeliveryStatus");
			soTags.append(Constants.SALES_ORDER_STATUS_TAG).append(spDelivStatus).append(",");

		}

		return null;
	}

	public static List<String> getSONumbersByDB(String dbSalesOrderNo) {
		List<String> soNos = new ArrayList<>();
		if (dbSalesOrderNo.contains(",")) {
			String[] strSOs = dbSalesOrderNo.split(",");
			for (String strSO : strSOs) {
				soNos.add(StringUtils.trimToEmpty(strSO));
			}
		} else {
			soNos.add(dbSalesOrderNo);
		}

		return soNos;
	}

	public static String getTotalBalance(List<Map<String, Object>> transactions) {

		double totalPending = 0D;
		double totalAmtPaid = 0D;
		for (Map<String, Object> tranMap : transactions) {
			String status = (String) tranMap.get("status");
			String kind = (String) tranMap.get("kind");
			String amountStr = (String) tranMap.get("amount");
			double dAmount = Double.parseDouble(amountStr);
			if (status.equals("pending") && kind.equals("sale")) {
				totalPending += dAmount;
			}
			if (status.equals("success") && kind.equals("sale")) {
				totalAmtPaid += dAmount;
			}
		}
		double totalBalance = (totalPending - totalAmtPaid);
		if (totalBalance > 0D) {
			return String.valueOf(totalBalance);
		}

		return "0";
	}

	public static Map<String, Object> getTransactionWithLimit(Map<String, Object> transactMap) {

		Map<String, Object> newTransact = new LinkedHashMap<>();
		newTransact.putAll(transactMap);
		if (newTransact.containsKey("location_id")) {
			newTransact.remove("location_id");
		}
		if (newTransact.containsKey("admin_graphql_api_id")) {
			newTransact.remove("admin_graphql_api_id");
		}
		if (newTransact.containsKey("source_name")) {
			newTransact.remove("source_name");
		}
		if (newTransact.containsKey("processed_at")) {
			newTransact.remove("processed_at");
		}
		if (newTransact.containsKey("parent_id")) {
			newTransact.remove("parent_id");
		}
		if (newTransact.containsKey("device_id")) {
			newTransact.remove("device_id");
		}

		if (newTransact.containsKey("authorization")) {
			newTransact.remove("authorization");
		}

		if (newTransact.containsKey("kind")) {
			newTransact.remove("kind");
		}

		if (newTransact.containsKey("test")) {
			newTransact.remove("test");
		}

		if (newTransact.containsKey("message")) {
			newTransact.remove("message");
		}

		if (newTransact.containsKey("receipt")) {
			newTransact.remove("receipt");
		}

		if (newTransact.containsKey("currency")) {
			newTransact.remove("currency");
		}

		if (newTransact.containsKey("created_at")) {
			newTransact.remove("created_at");
		}

		return newTransact;
	}

	public static boolean isValidURL(String url) {
		// Regex to check valid URL
		String regex = "((http|https)://)(www.)?" + "[a-zA-Z0-9@:%._\\+~#?&//=]" + "{2,256}\\.[a-z]"
				+ "{2,6}\\b([-a-zA-Z0-9@:%" + "._\\+~#?&//=]*)";

		// Compile the ReGex
		Pattern p = Pattern.compile(regex);

		// If the string is empty
		// return false
		if (url == null) {
			return false;
		}

		// Find match between given string
		// and regular expression
		// using Pattern.matcher()
		Matcher m = p.matcher(url);

		// Return if the string
		// matched the ReGex
		return m.matches();
	}

	public static Map<String, Object> buildUPayLinkMap(Map<String, Object> orderMap, String redirectURL) {

		Map<String, Object> rootMap = new LinkedHashMap<String, Object>();
		List<Map<String, String>> referenceList = new ArrayList<Map<String, String>>();

		String orderName = (String) orderMap.get("name");
		String orderId = String.valueOf(orderMap.get("orderId"));

		Map<String, Object> customerMap = (Map<String, Object>) orderMap.get("customer");
		String customerNo = (String) orderMap.get("soCustomerNo");
		String customerName = new StringBuilder((String) customerMap.get("first_name")).append(" ")
				.append((String) customerMap.get("last_name")).toString();

		String email = (String) customerMap.get("email");
		String contactNoOrig = (String) customerMap.get("phone");
		String contactNo = (String) customerMap.get("phone");
		String totalOrderPrice = (String) orderMap.get("total_price");
		double doubTotalOrderPrice = NumberUtil.roundTwoDec(Double.parseDouble(totalOrderPrice));
		if (doubTotalOrderPrice > 0) {
			totalOrderPrice = String.valueOf(doubTotalOrderPrice);
		}

		String currency = (String) orderMap.get("currency");

		if (StringUtils.isNotBlank(contactNo)) {
			if (StringUtils.length(contactNo) > UPAY_MOBILE_CHAR_LIMIT) {
				contactNo = contactNo.substring((contactNo.length() - UPAY_MOBILE_CHAR_LIMIT), contactNo.length());
			}
		}

		rootMap.put("Amt", totalOrderPrice);
		rootMap.put("Email", email);
		rootMap.put("Mobile", contactNo);
		rootMap.put("Amt", totalOrderPrice);
		rootMap.put("Redir", redirectURL);

		Map<String, String> orderNameRef = new LinkedHashMap<String, String>();
		orderNameRef.put("Id", "1");
		orderNameRef.put("Name", "orderName");
		orderNameRef.put("Val", orderName);
		referenceList.add(orderNameRef);

		Map<String, String> orderIdRef = new LinkedHashMap<String, String>();
		orderIdRef.put("Id", "2");
		orderIdRef.put("Name", "orderId");
		orderIdRef.put("Val", orderId);
		referenceList.add(orderIdRef);

		Map<String, String> customerIdRef = new LinkedHashMap<String, String>();
		customerIdRef.put("Id", "3");
		customerIdRef.put("Name", "customerNo");
		customerIdRef.put("Val", StringUtils.trimToEmpty(customerNo));
		referenceList.add(customerIdRef);

		Map<String, String> customerNameRef = new LinkedHashMap<String, String>();
		customerNameRef.put("Id", "4");
		customerNameRef.put("Name", "customerName");
		customerNameRef.put("Val", StringUtils.trimToEmpty(customerName));
		referenceList.add(customerNameRef);

		Map<String, String> emailRef = new LinkedHashMap<String, String>();
		emailRef.put("Id", "5");
		emailRef.put("Name", "email");
		emailRef.put("Val", StringUtils.trimToEmpty(email));
		referenceList.add(emailRef);

		Map<String, String> phoneRef = new LinkedHashMap<String, String>();
		phoneRef.put("Id", "6");
		phoneRef.put("Name", "phone");
		phoneRef.put("Val", StringUtils.trimToEmpty(contactNoOrig));
		referenceList.add(phoneRef);

		Map<String, String> orderPriceRef = new LinkedHashMap<String, String>();
		orderPriceRef.put("Id", "7");
		orderPriceRef.put("Name", "totalOrderPrice");
		orderPriceRef.put("Val", StringUtils.trimToEmpty(totalOrderPrice));
		referenceList.add(orderPriceRef);

		rootMap.put("References", referenceList);
		logger.info("*** PCDataUtil >> buildUPayLinkMap : " + rootMap.toString());
		return rootMap;
	}

	public static String getParseMobileNo(String contactNo) {
		if (StringUtils.isNotBlank(contactNo)) {
			String newContactNo = contactNo.replaceAll("[^0-9]", "");
			if (newContactNo.startsWith("63")) {
				newContactNo = newContactNo.substring(2, newContactNo.length());
			}
			if (newContactNo.startsWith("0")) {
				newContactNo = newContactNo.substring(1, newContactNo.length());
			}
			if (newContactNo.length() > 10) {
				newContactNo = newContactNo.substring(0, 10);
			}
			return newContactNo;
		}
		return null;
	}

	public static Object getValue(Map<String, Object> objMap, String key) {
		Map<String, Object> valMap = (Map<String, Object>) objMap.get(key);
		if (valMap != null)
			return valMap.get("value");
		else
			return null;
	}

	public static Map<String, Object> getValueKey(Object obj) {
		Map<String, Object> valMap = new HashMap<>();
		valMap.put("value", obj);
		return valMap;
	}

	public static String getParseEmail(String email) {
		if (StringUtils.isNotBlank(email)) {
			String EMAIl_PATTERN = "[^a-zA-Z0-9!#$%&@'*+-/=?^_`{|}~.]+";
			String modifiedEmail = email.replaceAll(EMAIl_PATTERN, "");
			return modifiedEmail;
		}
		return null;
	}

	public static boolean isWithinAmountRange(double payAmount, double compareAmount, double variant) {
		double fromAmt = (compareAmount - variant);
		double toAmt = (compareAmount + variant);
		if (payAmount >= fromAmt && payAmount <= toAmt) {
			return true;
		}

		return false;
	}

	public static boolean isWithinPaymentSchedule(Date datePaid, Date scheduleDate, int dayVariant) {
		Date fromSchedDate = DateUtil.getDateNowPlusTime(scheduleDate, Calendar.DAY_OF_YEAR, -1);
		Date toSchedDate = DateUtil.getDateNowPlusTime(scheduleDate, Calendar.DAY_OF_YEAR, dayVariant);
		if (datePaid.getTime() >= fromSchedDate.getTime() && datePaid.getTime() <= toSchedDate.getTime()) {
			return true;
		}

		return false;
	}

	public static Map<String, Object> buildUPayLinkMapWithValidation(Map<String, Object> orderMap, String redirectURL) {

		Map<String, Object> rootMap = new LinkedHashMap<String, Object>();
		List<Map<String, String>> referenceList = new ArrayList<Map<String, String>>();

		String orderName = (String) orderMap.get("name");
		String orderId = String.valueOf(orderMap.get("orderId"));

		Map<String, Object> customerMap = (Map<String, Object>) orderMap.get("customer");
		String customerNo = (String) orderMap.get("soCustomerNo");
		String customerName = new StringBuilder((String) customerMap.get("first_name")).append(" ")
				.append((String) customerMap.get("last_name")).toString();

		String email = (String) customerMap.get("email");
		String contactNoOrig = (String) customerMap.get("phone");
		String contactNo = (String) customerMap.get("phone");
		String totalOrderPrice = (String) orderMap.get("total_price");
		String currency = (String) orderMap.get("currency");

		if (StringUtils.isNotBlank(contactNo)) {
			if (StringUtils.length(contactNo) > UPAY_MOBILE_CHAR_LIMIT) {
				contactNo = contactNo.substring((contactNo.length() - UPAY_MOBILE_CHAR_LIMIT), contactNo.length());
			}
		}

		rootMap.put("Amt", totalOrderPrice);
		rootMap.put("Email", email);
		rootMap.put("Mobile", contactNo);
		rootMap.put("Amt", totalOrderPrice);
		rootMap.put("Redir", redirectURL);

		Map<String, String> orderNameRef = new LinkedHashMap<String, String>();
		orderNameRef.put("Id", "1");
		orderNameRef.put("Name", "orderName");
		orderNameRef.put("Val", orderName);
		referenceList.add(orderNameRef);

		Map<String, String> orderIdRef = new LinkedHashMap<String, String>();
		orderIdRef.put("Id", "2");
		orderIdRef.put("Name", "orderId");
		orderIdRef.put("Val", orderId);
		referenceList.add(orderIdRef);

		Map<String, String> customerIdRef = new LinkedHashMap<String, String>();
		customerIdRef.put("Id", "3");
		customerIdRef.put("Name", "customerNo");
		customerIdRef.put("Val", StringUtils.trimToEmpty(customerNo));
		referenceList.add(customerIdRef);

		Map<String, String> customerNameRef = new LinkedHashMap<String, String>();
		customerNameRef.put("Id", "4");
		customerNameRef.put("Name", "customerName");
		customerNameRef.put("Val", StringUtils.trimToEmpty(customerName));
		referenceList.add(customerNameRef);

		Map<String, String> emailRef = new LinkedHashMap<String, String>();
		emailRef.put("Id", "5");
		emailRef.put("Name", "email");
		emailRef.put("Val", StringUtils.trimToEmpty(email));
		referenceList.add(emailRef);

		Map<String, String> phoneRef = new LinkedHashMap<String, String>();
		phoneRef.put("Id", "6");
		phoneRef.put("Name", "phone");
		phoneRef.put("Val", StringUtils.trimToEmpty(contactNoOrig));
		referenceList.add(phoneRef);

		Map<String, String> orderPriceRef = new LinkedHashMap<String, String>();
		orderPriceRef.put("Id", "7");
		orderPriceRef.put("Name", "totalOrderPrice");
		orderPriceRef.put("Val", StringUtils.trimToEmpty(totalOrderPrice));
		referenceList.add(orderPriceRef);

		rootMap.put("References", referenceList);
		logger.info("*** PCDataUtil >> buildUPayLinkMap : " + rootMap.toString());
		return rootMap;
	}

	public static String getGroupCustomerNo(String customerNo) {

		if (StringUtils.length(customerNo) <= GROUP_CUSTOMER_NO_MAX_SIZE) {
			return customerNo;
		} else {
			// HFR013HCF00024 / CLT007CLT007 / FR014PCF00035 / WBHI001PCO270
			String groupCustNo = "";
			for (int ii = (GROUP_CUSTOMER_NO_MIN_SIZE - 1); ii <= (GROUP_CUSTOMER_NO_MAX_SIZE - 1); ii++) {
				String cIndex = customerNo.substring(ii, ii + 1);
				if (NumberUtil.isDigits(cIndex)) {
					groupCustNo = customerNo.substring(0, ii + 1);
				}
			}

			return groupCustNo;
		}

	}

	public static Map<String, Object> getOrderTagValuesAsMap(Map<String, Object> eOrderMap) {
		Map<String, Object> tagMap = new HashMap<>();
		String orderTags = StringUtils.trimToEmpty((String) eOrderMap.get("tags"));
		if (StringUtils.isNotBlank(orderTags)) {
			if (orderTags.contains(Constants.COMMA)) {
				String[] tagArray = orderTags.split(Constants.COMMA);
				for (String tag : tagArray) {
					if (tag.startsWith(Constants.MODE_OF_PAYMENT_TAG)) {
						tagMap.put("paymentMode", tag.replace(Constants.MODE_OF_PAYMENT_TAG, ""));
					}
					if (tag.startsWith(Constants.SALES_ORDER_NO_TAG)) {
						tagMap.put("salesOrderNo1", tag.replace(Constants.SALES_ORDER_NO_TAG, ""));
					}
					if (tag.startsWith(Constants.SALES_ORDER_NO_TAG)) {
						tagMap.put("salesOrderNo2", tag.replace(Constants.SALES_ORDER_NO_2_TAG, ""));
					}
					if (tag.startsWith(Constants.SO_DELIVERY_DATE_TAG)) {
						tagMap.put("deliveryDate", tag.replace(Constants.SO_DELIVERY_DATE_TAG, ""));
					}
				}
			} else {
				// 1 tag only
				if (orderTags.startsWith(Constants.MODE_OF_PAYMENT_TAG)) {
					tagMap.put("paymentMode", orderTags.replace(Constants.MODE_OF_PAYMENT_TAG, ""));
				}
				if (orderTags.startsWith(Constants.SALES_ORDER_NO_TAG)) {
					tagMap.put("salesOrderNo1", orderTags.replace(Constants.SALES_ORDER_NO_TAG, ""));
				}
				if (orderTags.startsWith(Constants.SALES_ORDER_NO_TAG)) {
					tagMap.put("salesOrderNo2", orderTags.replace(Constants.SALES_ORDER_NO_2_TAG, ""));
				}
				if (orderTags.startsWith(Constants.SO_DELIVERY_DATE_TAG)) {
					tagMap.put("deliveryDate", orderTags.replace(Constants.SO_DELIVERY_DATE_TAG, ""));
				}
			}
		}

		return tagMap;
	}

	public static String getValidName(String name) {
		if (StringUtils.isNotBlank(name)) {
			return name.replaceAll("[\\W]+", "");
		}
		return name;
	}

	public static HashMap<String, Object> getFirstLastNameFromFullName(String fullName) {
		HashMap<String, Object> nameMap = new HashMap<>();

		String tFullName = StringUtils.trimToEmpty(fullName);
		if (tFullName.contains(" ")) {
			nameMap.put("firstName", tFullName.substring(0, tFullName.indexOf(' ')).trim().replaceAll("[\\W]+", ""));
			nameMap.put("lastName",
					tFullName.substring(tFullName.indexOf(' '), tFullName.length()).trim().replaceAll("[\\W]+", ""));

		} else {
			nameMap.put("firstName", "PC");
			nameMap.put("lastName", tFullName.replaceAll("[\\W]+", ""));
		}

		return nameMap;
	}

	public static String buildUPayPostLinkRequest(String upayIndexLink, String billerUID, String jsonCypherData) {
		StringBuilder sbRequest = new StringBuilder();
		sbRequest.append(upayIndexLink).append("/WhiteLabel/").append(billerUID).append("?s=")
				.append(ShopifyUtil.encodeValue(jsonCypherData));

		return sbRequest.toString();
	}

	public static boolean hasSODataIssueOrSOTag(Map<String, Object> eOrderMap) {
		if (MapUtils.isNotEmpty(eOrderMap)) {
			String orderTags = StringUtils.trimToEmpty((String) eOrderMap.get("tags"));
			if (orderTags.contains(Constants.SO_DATA_ISSUE) || orderTags.contains(Constants.SALES_ORDER_NO_TAG))
				return true;
		}
		return false;
	}

	public static void testRun() {
		logger.info("** PCDataUtil >> [START]");
		Process process = null;
		InputStream inStream = null;
		try {
			Gson gson = new Gson();
//			String command = "curl --location 'https://pc-cms.uat.shakeys.solutions/springboot-cms-backend/pc/online-shop/executeDataQuery' \\\r\n" + 
//					" --header 'Content-Type: application/json' \\" + 
//					" --data ' {" + 
//					" \"queryTxt\":\"select * from  cms_db.shop_order where order_name = 'UAT1031'\"" + 
//					"} '";
			String command = "curl -X POST https://postman-echo.com/post --data foo1=bar1&foo2=bar2";
			logger.info("command: " + command);

			process = Runtime.getRuntime().exec(command);
			inStream = process.getInputStream();
			String responseTxt = new BufferedReader(new InputStreamReader(inStream)).lines()
					.collect(Collectors.joining("\n"));
			logger.info("responseTxt: " + responseTxt);
			Map<String, Object> responseMap = gson.fromJson(responseTxt, Map.class);
			logger.info("responseMap: " + responseMap);
			inStream.close();
		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
		} finally {
			try {
				if (inStream != null)
					inStream.close();

				if (process != null)
					process.destroy();
			} catch (Exception ee) {
			}
		}

		logger.info("** PCDataUtil >> [END]");

	}

	public static void main(String[] args) {

		try {

			System.out.println(PCDataUtil.getPaymentJournalNo("Success, CIPC_JB-000031360 created."));

		} catch (Exception e) {

		}

//		logger.info(PCDataUtil.replaceInvalidChar("THERMOMETER DEEP FRY (50°-550°F) (PC00000055)&", " "));
//		logger.info(
//				PCDataUtil.replaceInvalidChar("PAPER BOWL MEGA (520 CC) 50PC/BDL 1000PC/BOX (PC00000460)", " "));
//		logger.info(
//				PCDataUtil.replaceInvalidChar("TABLE NAPKIN (5.75\" x 12.5\") 400 SHEETS x #25 (PC00000030)", " "));

		// boolean isValid =
		// PCDataUtil.isValidURL("https://www.epayments.uat.bdo.com.ph/epayments?client_id=rxSFCA3mhkQ1NddmadgX27ok0HEetS39yP3b88wHRz9dYfAm&merchantName=SHAKEYS
		// PIZZA COMMERCE
		// INC&totalBillCollectionAmount=10353.20&totalBillCollectionCurrency=PHP&internalTransactionRefNo=UAT1066&billerId=E01526&channelRefNumber=E01526UAT1066&serviceCategory=epayment&serviceType=epayment&subscriberName=&subscriberNumber=639178742983&exitUrl=https%3A%2F%2Fpc-cms.uat.shakeys.solutions%2Fspringboot-cms-backend%2Fpc%2Fbdo%2Ftransactioncomplete");
		// System.out.println("isValid: " + isValid);

//		Date dateNow = new Date();
//		SimpleDateFormat sdf = new SimpleDateFormat("MMddYYYHHmmss");
//		String uniqTranNo = sdf.format(dateNow);
//		System.out.println(uniqTranNo);

//		double orderWithDiscount = PCDataUtil.computeEWTDiscount(1000D, 1.12, 1.11);
//		System.out.println("orderWithDiscount: " + orderWithDiscount);
//		double shippingWithDiscount = PCDataUtil.computeEWTDiscount(1000D, 1.12, 1.1);
//		System.out.println("shippingWithDiscount: " + shippingWithDiscount);
		System.out.println(PCDataUtil.getUniqueId("STG"));

//		Set<String> zoneIds = ZoneId.getAvailableZoneIds();
//
//		for (String zone : zoneIds) {
//			System.out.println(zone);
//		}

//		PCDataUtil.getSalesOrderDuplicates(null);

//		String grpCustNo = PCDataUtil.getGroupCustomerNo("HFR013HCF00024");
//		logger.info("grpCustNo: " + grpCustNo);
//		String grpCustNo1 = PCDataUtil.getGroupCustomerNo("CLT007CLT007");
//		logger.info("grpCustNo1: " + grpCustNo1);
//		String grpCustNo2 = PCDataUtil.getGroupCustomerNo("FR014PCF00035");
//		logger.info("grpCustNo2: " + grpCustNo2);
//		String grpCustNo3 = PCDataUtil.getGroupCustomerNo("WBHI001PCO270");
//		logger.info("grpCustNo3: " + grpCustNo3);
//		String grpCustNo4 = PCDataUtil.getGroupCustomerNo("INT04KHM001");
//		logger.info("grpCustNo4: " + grpCustNo4);
//		String grpCustNo5 = PCDataUtil.getGroupCustomerNo("ESALE001");
//		logger.info("grpCustNo5: " + grpCustNo5);

//		String strVal = "1.0";
//		logger.info((int)Double.parseDouble(strVal));
//		Map<String, Object> paramMap = new HashMap<>();
//		paramMap.put("processPerOrder", true);
//		boolean processPerOrder = paramMap.get("processPerOrder")!=null? ((boolean)paramMap.get("processPerOrder")):false ; 
//		logger.info(processPerOrder);
//		logger.info(PCDataUtil.getParseMobileNo("639171111111222"));
//		logger.info(PCDataUtil.getParseMobileNo("09171111111222"));
//		logger.info(PCDataUtil.getParseMobileNo("63917-111-111-1222"));
//		logger.info(PCDataUtil.getParseMobileNo("+63-9=1=7=1=111111222"));
//		logger.info(PCDataUtil.getParseMobileNo("639171111111222/2343243242"));
	}
}
