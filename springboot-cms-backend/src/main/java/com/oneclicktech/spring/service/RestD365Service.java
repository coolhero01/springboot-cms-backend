package com.oneclicktech.spring.service;

import java.util.Map;

import aj.org.objectweb.asm.Type;

public interface RestD365Service {

	public Map<String, Object> sendGetRequest(String requestUrl, String accessToken, 
			Map<String, Object> paramMap) 
			throws Throwable;
	
	public Map<String, Object> sendPostRequest(String requestUrl, String accessToken,
			Map<String, Object> paramMap) 
			throws Throwable;
	
	public Map<String, Object> sendPostRequest(String requestUrl, String accessToken,
			Map<String, Object> paramMap, String objectType) 
			throws Throwable;
	
	public Map<String, Object> sendDeleteRequest(String requestUrl, String accessToken, 
			Map<String, Object> paramMap) 
			throws Throwable;
	
	public Map<String, Object> sendPutRequest(String requestUrl, String accessToken, 
			Map<String, Object> paramMap) 
			throws Throwable;
	
	public String getLatestD365Token() 		throws Throwable;;
			
}
