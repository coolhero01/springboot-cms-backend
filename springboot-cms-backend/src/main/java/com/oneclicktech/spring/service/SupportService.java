package com.oneclicktech.spring.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface SupportService {
	 
	public Map<String, Object>  buildCustomerPaymentJournalRequest(Map<String, Object> eOrderMap,
			Map<String, Object> ewtOrderMap, Map<String, Object> paramMap);	
	
  
}
