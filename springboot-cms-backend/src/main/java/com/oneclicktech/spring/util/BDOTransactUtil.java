package com.oneclicktech.spring.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;

import com.oneclicktech.spring.domain.Constants;

public class BDOTransactUtil {
	
	private static final Logger logger = Logger.getLogger("BDOTransactUtil");
	
	public static String parseResponseURL(String responseURL) {
		 String newURL = StringUtils.trimToEmpty(responseURL)
				 .replaceAll(" ", "%20");
		 return newURL;
	}
	
	public static String getInternalTransactRefNoByURL(String reqUrl) {
		if (StringUtils.isNotBlank(reqUrl) 
				&& reqUrl.contains("internalTransactionRefNo")) {
			String firstTranRefNo = reqUrl.substring(reqUrl.indexOf("internalTransactionRefNo=")+25,reqUrl.length());
			String inTranRefNo = firstTranRefNo.substring(0, firstTranRefNo.indexOf("&"));
			logger.info("BDOTransactUtil >> getInternalTransactRefNoByURL >> inTranRefNo:" + inTranRefNo);
			return inTranRefNo;
		}
		return null;
	}
	
	public static String getChannelRefNoByURL(String reqUrl) {
		if (StringUtils.isNotBlank(reqUrl) 
				&& reqUrl.contains("channelRefNumber")) {
			String firstTranRefNo = reqUrl.substring(reqUrl.indexOf("channelRefNumber=")+17,reqUrl.length());
			String chRefNo = firstTranRefNo.substring(0, firstTranRefNo.indexOf("&"));
			logger.info("BDOTransactUtil >> getChannelRefNoByURL >> chRefNo:" + chRefNo);
			return chRefNo;
		}
		return null;
	}
	
	public static  String buildAuthorizeLink(Map<String, Object> orderMap, List<Map<String, Object>> bankAPIConfigs) {

		StringBuilder sbLink = new StringBuilder();
		Map<String, Object> rootMap = new LinkedHashMap<String, Object>();
		String totalBillCollectionAmt = (String) orderMap.get("total_price");
		double doubTotalOrderPrice = NumberUtil.roundTwoDec(Double.parseDouble(totalBillCollectionAmt));
		if (doubTotalOrderPrice > 0) {
			totalBillCollectionAmt = String.valueOf(doubTotalOrderPrice);
		}
		
		String orderName = (String) orderMap.get("name");
		String subscriberNo = null;
		Map<String, Object> customerMap = (Map<String, Object>) orderMap.get("customer");
		subscriberNo = StringUtils.trimToEmpty((String) customerMap.get("phone")).replaceAll("\\+", "");
		if (Constants.TEST_ONLY 
				&& StringUtils.isBlank(subscriberNo)) {
			subscriberNo = "09178741234";// TESTING KG Mobile
		}
		
		Date dateNow = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("MMddYYYHHmmss");
		String uniqTranNo = "-"+sdf.format(dateNow);
		
		String billerId = null;
		String apiRequestUrl = null;
		for (Map<String, Object> apiMap : bankAPIConfigs) {
			String apiColumn = (String) apiMap.get("apiColumn");
			String apiValue = (String) apiMap.get("apiValue");
			apiRequestUrl = (String) apiMap.get("apiRequestUrl");

			switch (apiColumn) {
			case "totalBillCollectionAmount":
				apiValue = totalBillCollectionAmt;
				break;
			case "internalTransactionRefNo":
				apiValue = orderName.concat(uniqTranNo);
				break;
			case "billerId":
				billerId = apiValue;
				break;
			case "channelRefNumber":
				apiValue = billerId.concat(orderName).concat(uniqTranNo);
				break;
			case "subscriberNumber":
				apiValue = subscriberNo;
				break;
			}
			rootMap.put(apiColumn, apiValue);
		}

		sbLink.append(apiRequestUrl).append("?");
		for (Map.Entry<String, Object> entry : rootMap.entrySet()) {
			String key = entry.getKey();
			String apiValue = String.valueOf(entry.getValue());
			sbLink.append(key).append("=").append(apiValue).append("&");
		}

		String finalRequestUrl = sbLink.substring(0, sbLink.length() - 1);
		logger.info("BDOTransactUtil >> buildAuthorizeLink >> rootMap:" + rootMap);
		
		finalRequestUrl = finalRequestUrl.replaceAll(" ", "-");
		
		logger.info("BDOTransactUtil >> buildAuthorizeLink >> finalRequestUrl:" + finalRequestUrl);
		
		return finalRequestUrl;
	}
	
	
	public static String buildPaymentStatusLink(Map<String, Object> orderMap, Map<String, Object> bankAPIMap, 
			List<Map<String, Object>> authorizeConfigs, List<Map<String, Object>> paymentStatusConfigs) {

		StringBuilder sbLink = new StringBuilder();
		Map<String, Object> rootMap = new LinkedHashMap<String, Object>();
		Date dateInPH = DateUtil.getDateInManilaPH();
		String requestDate = DateUtil.getDateWithPattern(dateInPH, "dd-MM-yyyy");
		String requestTime = DateUtil.getDateWithPattern(dateInPH, "HH:mm");
		String orderName = (String) orderMap.get("name");
		 
		SimpleDateFormat sdf = new SimpleDateFormat("MMddYYYHHmmss");
		String uniqTranNo = sdf.format(dateInPH);
		
		String billerId = null;
		for (Map<String, Object> apiMap : authorizeConfigs) {
			String apiColumn = (String) apiMap.get("apiColumn");
			String apiValue = (String) apiMap.get("apiValue");
			if (apiColumn.equals("billerId")) {
				billerId = apiValue;
			}
		}

		String apiRequestUrl = null;
		for (Map<String, Object> apiMap : paymentStatusConfigs) {
			String apiColumn = (String) apiMap.get("apiColumn");
			String apiValue = (String) apiMap.get("apiValue");
			apiRequestUrl = (String) apiMap.get("apiRequestUrl");

			switch (apiColumn) {
			case "requestDate":
				apiValue = requestDate;
				break;
			case "requestTime":
				apiValue = requestTime;
				break;
			case "internalTransactionRefNo":
				apiValue = (String)bankAPIMap.get("internalTransactRefNo");
				break;
			case "channelRefNumber":
				apiValue = orderName.concat(uniqTranNo);
				break;
			}

			rootMap.put(apiColumn, apiValue);
		}

		sbLink.append(apiRequestUrl).append("?");
		for (Map.Entry<String, Object> entry : rootMap.entrySet()) {
			String key = entry.getKey();
			String apiValue = String.valueOf(entry.getValue());
			sbLink.append(key).append("=").append(apiValue).append("&");
		}

		String finalRequestUrl = sbLink.substring(0, sbLink.length() - 1);
		logger.info("PCDataUtil >> buildPaymentStatusLink >> rootMap:" + rootMap);
		logger.info("PCDataUtil >> buildPaymentStatusLink >> finalRequestUrl:" + finalRequestUrl);

		return finalRequestUrl;
	}

	public static boolean successPaymentStatus(String bankStatus) {
		boolean isSuccess = false;
		bankStatus = StringUtils.trimToEmpty(bankStatus);
		switch (bankStatus) {
		case "PC":
			isSuccess = true;
			break;
		case "BS":
			isSuccess = true;
			break;

		default:
			isSuccess = false;
			break;
		}
		
		return isSuccess;
	}
	
	
	public static void main(String[] args) {
		String reqURL= "https://api17.apigateway.uat.bdo.com.ph/v1/authorize?client_id=rxSFCA3mhkQ1NddmadgX27ok0HEetS39yP3b88wHRz9dYfAm&exitUrl=https://pc-cms.uat.shakeys.solutions/springboot-cms-backend/pc/bdo/transactioncomplete&merchantName=SHAKEYS-PIZZA-COMMERCE-INC&totalBillCollectionAmount=2038.40&totalBillCollectionCurrency=PHP&internalTransactionRefNo=UAT1071-09152023043227&billerId=E01526&channelRefNumber=E01526UAT1071-09152023043227&serviceCategory=EPAYMENT&serviceType=EPAYMENT&subscriberNumber=639178742983";	
		BDOTransactUtil.getInternalTransactRefNoByURL(reqURL);
		BDOTransactUtil.getChannelRefNoByURL(reqURL);
			
	}
	
}
