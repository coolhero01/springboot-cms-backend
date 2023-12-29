package com.oneclicktech.spring.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface AuditLogService {
	 
	public List<Map<String, Object>> getOrderAuditLogs(HashMap<String, Object> paramMap);	
	
  	public int saveOrderAuditLog(String shopOrderNo, String salesOrderNo, String jsonRequest, String auditLogMsg);
 
	public String getLatestToken(String tokenType);
	
}
