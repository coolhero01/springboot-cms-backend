package com.oneclicktech.spring.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface ProductService {
	
	public boolean syncAzureProductToLocalDB();	
	
	public boolean syncAzureProductDetailToLocalDB();	
	
	public boolean syncAzureProductInventoryToLocalDB();	
	
	public boolean syncAzureWarehouseToLocalDB();	
	
	public List<Map<String, Object>> getProductList(HashMap<String, Object> paramMap);	
 	
	public List<Map<String, Object>> getProductDetailList(HashMap<String, Object> paramMap);	
 	
	public List<Map<String, Object>> getProductInventoryList(HashMap<String, Object> paramMap);	
 	
	public List<Map<String, Object>> getWarehouseList(HashMap<String, Object> paramMap);	
 	
	public List<Map<String, Object>> getShopProductList(HashMap<String, Object> paramMap);	
	
	
}
