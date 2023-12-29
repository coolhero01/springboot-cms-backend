package com.oneclicktech.spring.service;

import java.util.Map;

public interface IssuanceService {
	 
	public Map<String, Object> getStaggeredPaymentDetails(String orderName);
	
	public boolean isStaggeredPaymentOrder(String orderName);
	
	public Map<String, Object> getNextPaymentSched(String orderName);
	
	public boolean checkStaggeredPaymentTransactionExist(long orderId, Map<String, Object> paymentSchedMap);
	
}
