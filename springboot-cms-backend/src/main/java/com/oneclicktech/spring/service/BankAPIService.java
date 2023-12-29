package com.oneclicktech.spring.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface BankAPIService {
	 
	public Map<String, Object>  getBDOTransactLogByOrder(HashMap<String, Object> paramMap);	
	
  	public boolean saveBDOTransactLog(HashMap<String, Object> paramMap);
 
  
}
