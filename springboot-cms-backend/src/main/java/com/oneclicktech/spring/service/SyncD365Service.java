package com.oneclicktech.spring.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface SyncD365Service {

	public Map<String, Object> syncProductDataToDB(Map<String, Object> paramMap)  throws Throwable ;

	public Map<String, Object> syncCustomerDataToDB(Map<String, Object> paramMap)  throws Throwable ;
	
	public Map<String, Object> syncWarehouseDataToDB(Map<String, Object> paramMap)  throws Throwable ;
	
	public Map<String, Object> syncSalesOrderDataToDB(Map<String, Object> paramMap)  throws Throwable ;
	
	public Map<String, Object> syncSalesOrderFulfillByOrderDB(Map<String, Object> paramMap)  throws Throwable ;
	
	public Map<String, Object> cancelSalesOrder(String salesOrderNo) ;
	
	public Map<String, Object> viewSalesOrderDetail(String salesOrderNo)  throws Throwable;
	
	public boolean isValidCustomerAcct(String soCustomerNo)  throws Throwable;
	
	public Map<String, Object> processSalesAgreement(Map<String, Object> paramMap)  throws Throwable;
	
	public Map<String, Object> processPurchaseOrder(Map<String, Object> paramMap)  throws Throwable;
	
	public Map<String, Object> processPurchaseOrderOnline(Map<String, Object> paramMap)  throws Throwable;
	
	public List<Map<String, Object>> buildSplitSOServiceRequest(HashMap<String, Object> paramMap,
			List<Map<String, Object>> dbOrderLines, Map<String, Object> eOrderMap) throws Throwable;
 	
	public Map<String, Object> buildOneSalesOrderRequest(HashMap<String, Object> paramMap,
			List<Map<String, Object>> dbOrderLines, Map<String, Object> eOrderMap) throws Throwable;
	
	public Map<String, Object> buildCustomerPaymentJournalRequest(Map<String, Object> eOrderMap,
			Map<String, Object> ewtOrderMap, Map<String, Object> paramMap) throws Throwable;
	
	
	public Map<String, Object> processPaymentJournal(Map<String, Object> eOrderMap,
			Map<String, Object> ewtOrderMap, Map<String, Object> paramMap) throws Throwable;
	
}
