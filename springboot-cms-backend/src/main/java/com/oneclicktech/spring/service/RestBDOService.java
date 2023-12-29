package com.oneclicktech.spring.service;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Map;

import org.apache.http.ProtocolException;
import org.springframework.core.io.Resource;

public interface RestBDOService {
	 
	public void loadKeystore(Resource keyStorePath, String storePassword);
	
	public Map<String, Object> sendGetAuthorizeRequest(String requestUrl, Map<String, Object> paramMap)
			throws Throwable;
 
	public Map<String, Object> sendGetBillsPaymentRequest(String requestUrl, Map<String, Object> paramMap)
			throws Throwable;
 
	
	public Map<String, Object> sendPostTokenRequest(String requestUrl, Map<String, Object> paramMap) 
			throws Throwable;
	
	public Map<String, Object> sendDeleteRequest(String requestUrl, Map<String, Object> paramMap) 
			throws Throwable;
	
	public Map<String, Object> sendPutRequest(String requestUrl, Map<String, Object> paramMap) 
			throws Throwable;
	 
	 
 
}
