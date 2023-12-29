package com.oneclicktech.spring.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface OnlineShopService {
	
	public boolean syncLocalProductToOnline(Map<String, Object> paramMap)
			throws Throwable;	
	
	public boolean syncAlleProductsById(Map<String, Object> paramMap)
			throws Throwable;	
	
	
	public boolean syncLocalCustomerToOnline(Map<String, Object> paramMap)
			throws Throwable ;	
	
	public boolean syncLocalLocationToOnline(Map<String, Object> paramMap)
			throws Throwable ;	
	
	public boolean syncLocalCustomerToOnlineByEmail(Map<String, Object> paramMap)
			throws Throwable ;	
	
	
	
	public void cleanBackupTables(Map<String, Object> paramMap)
			throws Throwable ;	
	
	
	public void updateShopMetafield(Map<String, Object> paramMap)
			throws Throwable ;	
	
	
	public void deleteShopMetafield(String namespace, String key)
			throws Throwable ;	
	
}
