package com.oneclicktech.spring.service.impl;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.net.ssl.SSLContext;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections4.MapUtils;
import org.apache.http.ProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.google.gson.Gson;
import com.oneclicktech.spring.config.CleanUrlRedirectStrategy;
import com.oneclicktech.spring.domain.Constants;
import com.oneclicktech.spring.service.RestBDOService;

@Service
public class RestBDOServiceImpl implements RestBDOService {

	private static final Logger logger = Logger.getLogger("RestBDOServiceImpl");

	private static final int SLEEP_TIME = 400; // 1000 = 1sec

	RestTemplate restTemplate;

	@Value("${pc.bdo.trust.store}")
	Resource pcTrustStore;

	@Value("${pc.bdo.trust.store.password}")
	String pcTrustStorePassword;

	@Value("${pc.bdo.cms.oauth.client-id}")
	String bdoAuthClientId;
	@Value("${pc.bdo.cms.oauth.client-secret}")
	String bdoAuthClientSecret;

	@PostConstruct
	public void init() {
		logger.info(" ** RestBDOServiceImpl >> INIT >> [START]");
		try {
			TrustStrategy acceptingTrustStrategy = (X509Certificate[] chain, String authType) -> true;

			SSLContext sslContext = SSLContextBuilder.create().loadKeyMaterial(pcTrustStore.getURL(),
					pcTrustStorePassword.toCharArray(), pcTrustStorePassword.toCharArray())
					.loadTrustMaterial(null, acceptingTrustStrategy).build();

			int timeout = 5;
			RequestConfig config = RequestConfig.custom().setConnectTimeout(timeout * 1000)
					.setConnectionRequestTimeout(timeout * 1000).setSocketTimeout(timeout * 1000).build();

			CloseableHttpClient client = HttpClientBuilder.create().setDefaultRequestConfig(config)
					.setSSLContext(sslContext).setRedirectStrategy(new CleanUrlRedirectStrategy())
					.disableRedirectHandling().build();

			HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();

			requestFactory.setHttpClient(client);

			restTemplate = new RestTemplate(requestFactory);

			logger.info(" ** RestBDOServiceImpl >> INIT >> BINDED ");
		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
		}
		logger.info(" ** RestBDOServiceImpl >> INIT >> [END] ");
	}

	@Override
	public void loadKeystore(Resource keyStorePath, String storePassword) {
		logger.info(" ** RestBDOServiceImpl >> loadKeystore >> [START]");
		try {
			TrustStrategy acceptingTrustStrategy = (X509Certificate[] chain, String authType) -> true;

			SSLContext sslContext = SSLContextBuilder.create()
					.loadKeyMaterial(keyStorePath.getURL(), storePassword.toCharArray(), storePassword.toCharArray())
					.loadTrustMaterial(null, acceptingTrustStrategy).build();

			int timeout = 5;
			RequestConfig config = RequestConfig.custom().setConnectTimeout(timeout * 1000)
					.setConnectionRequestTimeout(timeout * 1000).setSocketTimeout(timeout * 1000).build();

			CloseableHttpClient client = HttpClientBuilder.create().setDefaultRequestConfig(config)
					.setSSLContext(sslContext).setRedirectStrategy(new CleanUrlRedirectStrategy())
					.disableRedirectHandling().build();

			HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();

			requestFactory.setHttpClient(client);

			restTemplate = new RestTemplate(requestFactory);

			logger.info(" ** RestBDOServiceImpl >> loadKeystore >> BINDED ");
		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
		}
		logger.info(" ** RestBDOServiceImpl >> loadKeystore >> [END] ");
	}

	@Override
	public Map<String, Object> sendGetAuthorizeRequest(String requestUrl, Map<String, Object> paramMap)
			throws Throwable {
		logger.info(" ** RestBDOServiceImpl >> sendGetRequest >> [START]");

		Map<String, Object> resultMap = new HashMap<>();
		StringBuilder finalRequestUrl = new StringBuilder(requestUrl);
		HttpHeaders headers = new HttpHeaders();
		headers.add("user-agent", "Application");
		headers.set("Accept", "*/*");

		logger.info(" ** RestBDOServiceImpl >> sendGetRequest >> finalRequestUrl: " + finalRequestUrl);
		HttpEntity<String> jwtEntity = new HttpEntity<String>(headers);
		// Use Token to get Response
		logger.info(" ** RestBDOServiceImpl >> finalRequestUrl: " + finalRequestUrl);
		ResponseEntity<String> result = null;
		try {
			result = restTemplate.exchange(finalRequestUrl.toString(), HttpMethod.GET, jwtEntity, String.class);

			resultMap.put(Constants.RESULT_HTTP_STATUS_CODE, result.getStatusCode());
			if (result != null && (result.getStatusCode().equals(HttpStatus.OK)
					|| result.getStatusCode().equals(HttpStatus.CREATED)
					|| result.getStatusCode().equals(HttpStatus.FOUND))) {
				Gson gson = new Gson();
				if (Constants.TEST_ONLY) {
					logger.info("RESPONSE getHeaders: " + result.getHeaders());
					logger.info("RESPONSE getBody: " + result.getBody());
				}
				HttpHeaders headerResult = result.getHeaders();
				if (headerResult != null) {
					List<String> headerValues = headerResult.getValuesAsList("location");
					for (String hdrValue : headerValues) {
						logger.info("hdrValue: " + hdrValue);
						resultMap.put("redirectUrl", hdrValue);
					}
				}

			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
		}

		logger.info(" ** RestBDOServiceImpl >> sendGetRequest >> [END]");
		return resultMap;
	}
	
	
	
	
	@Override
	public Map<String, Object> sendGetBillsPaymentRequest(String requestUrl, Map<String, Object> paramMap)
			throws Throwable {
		logger.info(" ** RestBDOServiceImpl >> sendGetBillsPaymentRequest >> [START]");

		
		Map<String, Object> resultMap = new HashMap<>();
		
		String authHeader = "Bearer " + String.valueOf(paramMap.get("accessToken"));

		StringBuilder finalRequestUrl = new StringBuilder(requestUrl);
		HttpHeaders headers = new HttpHeaders();
		headers.add("user-agent", "Application");
		headers.set("Accept", MediaType.ALL_VALUE);
		
		headers.set("Authorization", authHeader);

		logger.info(" ** RestBDOServiceImpl >> sendGetBillsPaymentRequest >> finalRequestUrl: " + finalRequestUrl);
		HttpEntity<String> jwtEntity = new HttpEntity<String>(headers);
		// Use Token to get Response
		logger.info(" ** RestBDOServiceImpl >> finalRequestUrl: " + finalRequestUrl);
		ResponseEntity<String> result = null;
		try {
			result = restTemplate.exchange(finalRequestUrl.toString(), HttpMethod.GET, jwtEntity, String.class);

			resultMap.put(Constants.RESULT_HTTP_STATUS_CODE, result.getStatusCode());
			if (result != null && (result.getStatusCode().equals(HttpStatus.OK)
					|| result.getStatusCode().equals(HttpStatus.CREATED)
					|| result.getStatusCode().equals(HttpStatus.FOUND))) {
				Gson gson = new Gson();
				if (Constants.TEST_ONLY) {
					logger.info("RESPONSE getHeaders: " + result.getHeaders());
					logger.info("RESPONSE getBody: " + result.getBody());
				}
				Map<String, Object> bodyMap = gson.fromJson(result.getBody(), Map.class);
				if (MapUtils.isNotEmpty(bodyMap))
					resultMap.putAll(bodyMap);

			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
		}

		logger.info(" ** RestBDOServiceImpl >> sendGetRequest >> [END]");
		return resultMap;
	}

	@Override
	public Map<String, Object> sendPostTokenRequest(String requestUrl, Map<String, Object> paramMap) throws Throwable {

		logger.info(" ** RestBDOServiceImpl >> sendPostTokenRequest >> [START]");

		Map<String, Object> resultMap = new HashMap<>();
		StringBuilder finalRequestUrl = new StringBuilder(requestUrl);
		logger.info(" ** RestBDOServiceImpl >> sendPostTokenRequest >> finalRequestUrl: " + finalRequestUrl);

		String auth = bdoAuthClientId + ":" + bdoAuthClientSecret;
		byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(Charset.forName("US-ASCII")));
		String authHeader = "Basic " + new String(encodedAuth);

		HttpHeaders headers = new HttpHeaders();
		//headers.add("user-agent", "Application");
		headers.set("Accept", MediaType.ALL_VALUE);
		headers.set("Content-Type", MediaType.APPLICATION_FORM_URLENCODED_VALUE);
		headers.set("Authorization", authHeader);
		Gson gson = new Gson();
		//String bodyRequest = gson.toJson(paramMap);
		MultiValueMap<String, String> bodyRequest = new LinkedMultiValueMap();
		bodyRequest.add("grant_type","client_credentials"); 
		
		HttpEntity<MultiValueMap<String, String>> jwtEntity = 
				new HttpEntity<MultiValueMap<String, String>>(bodyRequest, headers);
	  
		ResponseEntity<String> result = restTemplate.exchange(finalRequestUrl.toString(), HttpMethod.POST, jwtEntity,
				String.class);

		resultMap.put(Constants.RESULT_HTTP_STATUS_CODE, result.getStatusCode());
		if (result != null && (result.getStatusCode().equals(HttpStatus.OK)
				|| result.getStatusCode().equals(HttpStatus.CREATED))) {

			if (Constants.TEST_ONLY) {
				logger.info("RESPONSE: " + result.getBody());
			}

			Map<String, Object> bodyMap = gson.fromJson(result.getBody(), Map.class);
			if (MapUtils.isNotEmpty(bodyMap))
				resultMap.putAll(bodyMap);
		}
		logger.info(" ** RestBDOServiceImpl >> sendPostTokenRequest >> [END]");
		return resultMap;

	}

	@Override
	public Map<String, Object> sendDeleteRequest(String requestUrl, Map<String, Object> paramMap) throws Throwable {
		logger.info(" ** RestBDOServiceImpl >> sendDeleteRequest >> [START]");

		Map<String, Object> resultMap = new HashMap<>();
		StringBuilder finalRequestUrl = new StringBuilder(requestUrl);
		logger.info(" ** RestBDOServiceImpl >> sendDeleteRequest >> finalRequestUrl: " + finalRequestUrl);
		HttpHeaders headers = getHeaders();

		Gson gson = new Gson();
		String bodyRequest = gson.toJson(paramMap);

		HttpEntity<String> jwtEntity = new HttpEntity<String>(bodyRequest, headers);
		// Use Token to get Response
		Map<String, Object> newParamMap = new HashMap<>();
		ResponseEntity<String> result = restTemplate.exchange(finalRequestUrl.toString(), HttpMethod.DELETE, jwtEntity,
				String.class, newParamMap);

		resultMap.put(Constants.RESULT_HTTP_STATUS_CODE, result.getStatusCode());
		if (result != null && (result.getStatusCode().equals(HttpStatus.OK)
				|| result.getStatusCode().equals(HttpStatus.CREATED))) {
			if (Constants.TEST_ONLY) {
				logger.info("RESPONSE: " + result.getBody());
			}

			Map<String, Object> bodyMap = gson.fromJson(result.getBody(), Map.class);
			if (MapUtils.isNotEmpty(bodyMap))
				resultMap.putAll(bodyMap);
		}
		logger.info(" ** RestBDOServiceImpl >> sendDeleteRequest >> [END]");
		return resultMap;
	}

	@SuppressWarnings("unused")
	@Override
	public Map<String, Object> sendPutRequest(String requestUrl, Map<String, Object> paramMap) throws Throwable {
		logger.info(" ** RestBDOServiceImpl >> sendPutRequest >> [START]");

		Map<String, Object> resultMap = new HashMap<>();
		StringBuilder finalRequestUrl = new StringBuilder(requestUrl);
		logger.info(" ** RestBDOServiceImpl >> sendPutRequest >> finalRequestUrl: " + finalRequestUrl);
		HttpHeaders headers = getHeaders();

		Gson gson = new Gson();

		String bodyRequest = gson.toJson(paramMap);

		HttpEntity<String> jwtEntity = new HttpEntity<String>(bodyRequest, headers);
		// Use Token to get Response
		Map<String, Object> newParamMap = new HashMap<>();
		ResponseEntity<String> result = restTemplate.exchange(finalRequestUrl.toString(), HttpMethod.PUT, jwtEntity,
				String.class);

		resultMap.put(Constants.RESULT_HTTP_STATUS_CODE, result.getStatusCode());
		if (result != null && (result.getStatusCode().equals(HttpStatus.OK)
				|| result.getStatusCode().equals(HttpStatus.CREATED))) {
			if (Constants.TEST_ONLY) {
				logger.info("RESPONSE: " + result.getBody());
			}

			Map<String, Object> bodyMap = gson.fromJson(result.getBody(), Map.class);
			if (MapUtils.isNotEmpty(bodyMap))
				resultMap.putAll(bodyMap);
		}
		logger.info(" ** RestBDOServiceImpl >> sendPutRequest >> [END]");
		return resultMap;
	}

	private HttpHeaders getHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.add("user-agent", "Application");
		// headers.set("Content-Type", MediaType.APPLICATION_JSON_VALUE);
		headers.set("Accept", "*/*");
		return headers;
	}

	@PreDestroy
	public void destroy() {

	}
}
