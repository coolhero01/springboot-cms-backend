package com.oneclicktech.spring.service.impl;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.oneclicktech.spring.service.SupportService;
import com.oneclicktech.spring.util.DateUtil;

@Service
public class SupportServiceImpl implements SupportService {

	private static final Logger logger = Logger.getLogger("SupportServiceImpl");
	
	@Value("${spavi.d365.default.data-area-id}")
	String defaultDataAreaId;
	
	@Override
	public Map<String, Object> buildCustomerPaymentJournalRequest(Map<String, Object> eOrderMap,
			Map<String, Object> ewtOrderMap, Map<String, Object> paramMap) {
		
		logger.info("*** SupportServiceImpl >> buildCustomerPaymentJournalRequest >> [START]");
		Map<String, Object> rootMap = new LinkedHashMap<String, Object>();
		Map<String, Object> detailMap = new LinkedHashMap<String, Object>();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
		String soNumbers = (String) paramMap.get("soNumbers"); 
		String paymentRefId = (String) paramMap.get("paymentRefId"); 
			//Multiple SO's - CIPC_SO-000057221,  CIPC_SO-000057217
		
		try {
			String orderName = (String) eOrderMap.get("name");
			String customerNo = (String)paramMap.get("customerNo");
			Date phDate =  DateUtil.getDateInManilaPH();
		 	
			detailMap.put("DataAreaId", defaultDataAreaId);
			detailMap.put("AccountNum", customerNo); 
			detailMap.put("PaymentDate", sdf.format(phDate));
			detailMap.put("DeductionAmount", "");
			detailMap.put("SONumber", soNumbers);
			detailMap.put("ShopifyOrderNumber", orderName);
			detailMap.put("PaymentReference", paymentRefId);
			detailMap.put("PaymentMode", "ONLINEBANK");
			detailMap.put("Credit", "");
			detailMap.put("Debit", "");
			
			rootMap.put("_dataContract", detailMap);
		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
		}
		
		logger.info("*** SupportServiceImpl >> buildCustomerPaymentJournalRequest >> rootMap: " + rootMap);
		logger.info("*** SupportServiceImpl >> buildCustomerPaymentJournalRequest >> [END]");
 		return rootMap;
	}
	
	 
}
