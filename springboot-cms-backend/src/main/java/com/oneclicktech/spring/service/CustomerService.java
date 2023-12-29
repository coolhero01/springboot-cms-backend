package com.oneclicktech.spring.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface CustomerService {
	
	public boolean syncAzureCustomerToLocalDB();	
	
	public boolean syncAzureCustomerAddressToLocalDB();	
	
	public List<Map<String, Object>> getCustomerList(HashMap<String, Object> paramMap);	

	public List<Map<String, Object>> getCustomerAddressList(HashMap<String, Object> paramMap);	
 	 
	 
	public List<Map<String, Object>> getStoresByWarehouse(HashMap<String, Object> paramMap);	
	
	
	public boolean checkIfPromoDataExist(String tableName, 
			String promoCode, String customerNo);
	
}
