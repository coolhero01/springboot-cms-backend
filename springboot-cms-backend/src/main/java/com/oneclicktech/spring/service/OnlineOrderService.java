package com.oneclicktech.spring.service;

import java.util.List;
import java.util.Map;

public interface OnlineOrderService {
	
	public List<Map<String, Object>> getOrderList(Map<String, Object> paramMap)  throws Throwable ;
	
	public List<Map<String, Object>> getOrderTransactions(long orderId)  throws Throwable ;
	
	public List<Map<String, Object>> getOrdersByCustomer(String customerId, String orderStatus)  throws Throwable ;
	
	public List<Map<String, Object>> getOrdersByAutoPayCustomer(String customerId, String orderStatus)  throws Throwable ;
	
	public  Map<String, Object>  getOneSuccessTransaction(long orderId)  throws Throwable ;
	
	public  Map<String, Object>  getOneOrderByID(long orderId)  throws Throwable ;
	 
	public  Map<String, Object>  getOneOrderByName(String orderName)  throws Throwable ;
	
	public  Map<String, Object> buildSalesOrderMapRequest(Map<String, Object> shopOrderMap)  throws Throwable ;
	
	public List<Map<String, Object>> getFulfillmentOrders(String eOrderId) throws Throwable ;
	 
	public Map<String, Object>  resetObjectHook(String orderName) throws Throwable ;
	
	public Map<String, Object>  addDeliveryChargeToCart(String checkoutToken, Map<String, Object> paramMap) throws Throwable ;
	
	public Map<String, Object>  completeOrderFulfillment(String eOrderId) throws Throwable ;
	
	public Map<String, Object> updateOrder(Map<String, Object> paramMap)  throws Throwable ;
	
	public Map<String, Object> updateOnlineOrderTag(Map<String, Object> dbOrderMap, Map<String, Object> soD365Map)  throws Throwable ;
	
	public Map<String, Object> updateOnlineTagForSO(Map<String, Object> eOrderMap, Map<String, Object> dbOrderMap) 
			throws Throwable ;
	
	public Map<String, Object> addOrderTagToCurrent(Map<String, Object> eOrderMap, String addTag)  throws Throwable ;
	
	public Map<String, Object> updateOrderTagForAutoPay(Map<String, Object> eOrderMap)  throws Throwable ;
	
	public Map<String, Object> updateOrderNotes(Map<String, Object> paramMap)  throws Throwable ;
	
	public Map<String, Object> createOrder(Map<String, Object> paramMap)  throws Throwable ;
	
	public Map<String, Object> processPayment(Map<String, Object> paramMap)  throws Throwable ;
	
	public Map<String, Object> saveOnlineOrderByHookService(Map<String, Object> paramMap)  throws Throwable ;
	
	public Map<String, Object> processBillsPayment(Map<String, Object> paramMap)  throws Throwable ;
	
	public Map<String, Object> cancelOrder(String orderId) throws Throwable ;
	
	public Map<String, Object> deleteOrder(String orderId) throws Throwable ;
 
	public Map<String, Object> syncOrderDbSOToOnline(Map<String, Object> paramMap); 
	
	public Map<String, Object> checkAllOrdersBySO(Map<String, Object> paramMap)  throws Throwable ; 
	 
	public Map<String, Object> createEWTDiscountTag(Map<String, Object> eOrderMap, 
			Map<String, Object> discountConfig) throws Throwable ;
	
	
	
}
