package com.oneclicktech.spring.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface CronJobService {
	 
	public List<Map<String, Object>> getCronAuditLogs(HashMap<String, Object> paramMap);	
    
	public List<Map<String, Object>> getCronJobSchedList(HashMap<String, Object> paramMap);	
    
	public Map<String, Object> saveCronAuditLog(HashMap<String, Object> paramMap);
	 
	public Map<String, Object> deleteCronAuditLog(HashMap<String, Object> paramMap);
 	
	public Map<String, Object> saveOnlineOrderToDB(Map<String, Object> paramMap);
 	
	public Map<String, Object> syncD365StaticDataToDB(Map<String, Object> paramMap);
 
	public Map<String, Object> syncSalesOrderDataToDB(Map<String, Object> paramMap);
	
	public Map<String, Object> syncSalesOrderFulfillByOrderDB(Map<String, Object> paramMap);
	
	public Map<String, Object> syncOnlineOrderTagStatusByOrderDB(Map<String, Object> paramMap);
 	
	public Map<String, Object> processOrdersWithAutoPay(Map<String, Object> paramMap);
	
	public Map<String, Object> processPurchaseOrder(Map<String, Object> paramMap);
	 
	public Map<String, Object> runBDOPostTokenRequest(Map<String, Object> paramMap);
	
	public Map<String, Object> runBDOCheckPaymenStatusRequest(Map<String, Object> paramMap);
	
	public boolean getCronEnableStatus(String cronName);
	
}
