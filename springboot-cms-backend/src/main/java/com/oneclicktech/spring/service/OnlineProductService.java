package com.oneclicktech.spring.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface OnlineProductService {
	
	public List<Map<String, Object>> getProductList(Map<String, Object> paramMap) throws Throwable ;
	
	public List<Map<String, Object>> getAllOnlineProducts(Map<String, Object> paramMap) throws Throwable ;
	
	public Map<String, Object> getOneProduct(Long productId) throws Throwable ;
	  
	public List<Map<String, Object>>  getAllImagesByProduct(Long productId) throws Throwable ;
	
	public Map<String, Object> getOneOnlineProductByItemNo(String itemNo) throws Throwable ;
	
	public Map<String, Object> deleteProductImage(Long productId, Long imageId) throws Throwable ;
	
	public  List<Map<String, Object>> getOnlineProductByItemNo(String itemNo) throws Throwable ;
	
	public Map<String, Object> updateProduct(Map<String, Object> paramMap) throws Throwable ;
	
	public Map<String, Object> updateProductImage(Map<String, Object> paramMap) throws Throwable ;
	
	public Map<String, Object> createProduct(Map<String, Object> paramMap) throws Throwable ;
	
	public Map<String, Object> createPriceRule(Map<String, Object> paramMap) throws Throwable ;
	
	public Map<String, Object> createDiscountCode(Map<String, Object> paramMap) throws Throwable ;
	
	public Map<String, Object> deletePriceRule(Long priceRuleId) throws Throwable ;
	
	public Map<String, Object> deleteDiscountCode(Long priceRuleId, Long discountId) throws Throwable ;
	
	public Map<String, Object> deleteProduct(Map<String, Object> paramMap) throws Throwable ;
	
 	public Map<String, Object> createProductMetafield(Map<String, Object> paramMap) throws Throwable ;

 	public Map<String, Object> updateProductDBFromOnline(Map<String, Object> paramMap) throws Throwable ;
 	
 	public Map<String, Object> createProductImage(Map<String, Object> eProduct, 
			Map<String, Object> dbProduct) throws Throwable ;
 	
 	
 	public Map<String, Object> syncProductInventory(Map<String, Object> paramMap) throws Throwable ;
 	
 	public Map<String, Object> updateOnlineInventory(Long locationId, Long prodInventoryId, int newQty) 
 			throws Throwable ;
 	
 	public Map<String, Object> updateVariantInventory(Long eProductId, Long variantId, int newQty) 
 			throws Throwable ;
 	
 	public Map<String, Object> updateVariantData(Long eProductId, Long variantId, Map<String, Object> variantData) 
 			throws Throwable ;
 	
 	public Map<String, Object> cleansOnlineProductAndDB(Map<String, Object> paramMap) throws Throwable ;
 	
 	public Map<String, Object> backupProductDBTable(Map<String, Object> paramMap) throws Throwable ;
	
 	
	public Map<String, Object> cleanseOOSProductByDB(Map<String, Object> paramMap) throws Throwable ;
	
	public Map<String, Object> generateFreeVariantByPromo(Map<String, Object> paramMap) throws Throwable ;
	
 	public Map<String, Object> deleteVariant(Long eProductId, Long variantId) 
 			throws Throwable ;
 	
	
}
