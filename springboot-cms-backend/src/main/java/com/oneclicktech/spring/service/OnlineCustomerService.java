package com.oneclicktech.spring.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface OnlineCustomerService {
	
	public List<Map<String, Object>> getCustomerList(Map<String, Object> paramMap) 		
			throws Throwable  ;
	
	public List<Map<String, Object>> getAllOnlineCustomers(Map<String, Object> paramMap) 		
			throws Throwable  ;
	
	
	public List<Map<String, Object>> getAllOnlineAddressByID(String customerId) 		
			throws Throwable  ;
	
	
	public Map<String, Object> getOneCustomer(Long customerId) throws Throwable ;
	
	public Map<String, Object> getOneCustomerByEmail(String email) throws Throwable ;
	
	public Map<String, Object> getOneCustomerByTag(String email) throws Throwable ;
	
	public Map<String, Object> updateOnlineCustomer(Long eCustomerId, Map<String, Object> paramMap)
			throws Throwable ;
	
	public Map<String, Object> updateCustomerAddress(Long eCustomerId, Long eAddressId,
			Map<String, Object> paramMap)
			throws Throwable ;
	
	public Map<String, Object> updateDBCustomerByOnline(Map<String, Object> paramMap)
			throws Throwable ;
	
	
	public Map<String, Object> createCustomer(Map<String, Object> paramMap)
			throws Throwable ;
	
	public Map<String, Object> deleteCustomer(Map<String, Object> paramMap)
			throws Throwable ;
	
	public Map<String, Object> deleteCustomerById(String customerId)
			throws Throwable ;
	
	public Map<String, Object> deleteCustomerAddressById(String customerId, String customerAddrId)
			throws Throwable ;
	

 	public Map<String, Object> cleansOnlineCustomerAndDB(Map<String, Object> paramMap) throws Throwable ;
	
 	public Map<String, Object> backupCustomerDBTable(Map<String, Object> paramMap) throws Throwable ;
 	
	public Map<String, Object> cleansOnlineCustomerAddress(String customerId, String customerAddrId) throws Throwable ;
	
	public Map<String, Object> cleansOnlineCustomerAddress() throws Throwable ;
	
}
