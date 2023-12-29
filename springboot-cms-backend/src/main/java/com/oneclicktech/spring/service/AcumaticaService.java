package com.oneclicktech.spring.service;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface AcumaticaService {
	
	
	public boolean logonToSite();	
	
	public List<Map<String, Object>> getStockItemList(boolean runLogin, HashMap<String, Object> paramMap,  boolean isStockItem);	
 	
	public List<Map<String, Object>> getWarehouseList(boolean runLogin, HashMap<String, Object> paramMap);	
 	 
	public Map<String, Object> getStockItemByID(boolean runLogin, String inventoryId,  boolean isStockItem);	
	
	public Map<String, Object> getPurchaseOrderByID(boolean runLogin, String poNumber);	
	
	public Map<String, Object> getWarehouseInfoByDesc(boolean runLogin, String storeName);	
	
	public Map<String, Object> getWarehouseInfoByID(boolean runLogin, String warehouseId);	
 
	public Map<String, Object> createPurchaseOrder(boolean runLogin, Map<String, Object> eOrderMap, 
			Map<String, Object> dbOrderMap, Map<String, Object> soWarehouseInfo);
	
}
