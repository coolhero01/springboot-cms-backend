package com.oneclicktech.spring.service;

import java.util.Map;
import org.springframework.core.io.Resource;

public interface RestGenericService {
	 
 
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
	 
	
	public Map<String, Object> sendGetRequest(String requestUrl, String accessToken, Map<String, Object> paramMap) 
			throws Throwable;
	
	public Map<String, Object> sendGetRequestWithHeader(String requestUrl, String accessToken,  Map<String, Object> paramMap) 
			throws Throwable;
	
	public Map<String, Object> sendPostRequest(String requestUrl, String accessToken,  Map<String, Object> paramMap) 
			throws Throwable;
	
	public Map<String, Object> sendDeleteRequest(String requestUrl,String accessToken,  Map<String, Object> paramMap) 
			throws Throwable;
	
	public Map<String, Object> sendPutRequest(String requestUrl, String accessToken,  Map<String, Object> paramMap) 
			throws Throwable;
	
 
}
