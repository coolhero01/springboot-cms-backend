package com.oneclicktech.spring.service;

import java.util.Map;

public interface RestTemplateService {
	
	public void setRequest(boolean isShopifyRequest);
	
	public Map<String, Object> sendGetRequest(String requestUrl, Map<String, Object> paramMap) 
			throws Throwable;
	
	public Map<String, Object> sendGetRequestWithHeader(String requestUrl, Map<String, Object> paramMap) 
			throws Throwable;
	
	public Map<String, Object> sendPostRequest(String requestUrl, Map<String, Object> paramMap) 
			throws Throwable;
	
	public Map<String, Object> sendDeleteRequest(String requestUrl, Map<String, Object> paramMap) 
			throws Throwable;
	
	public Map<String, Object> sendPutRequest(String requestUrl, Map<String, Object> paramMap) 
			throws Throwable;
	
 
}
