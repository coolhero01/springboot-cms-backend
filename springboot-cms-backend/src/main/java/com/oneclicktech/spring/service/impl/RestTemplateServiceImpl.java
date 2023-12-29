package com.oneclicktech.spring.service.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.oneclicktech.spring.domain.Constants;
import com.oneclicktech.spring.service.RestTemplateService;

@Service
public class RestTemplateServiceImpl implements RestTemplateService {

	private static final Logger logger = Logger.getLogger("RestTemplateServiceImpl");
	
	private static final int SLEEP_TIME = 1000; //1000 = 1sec
	
	RestTemplate restTemplate;

	@Value("${pc.shopify.app.hosturl}")
	String hostUrl; 

	@Value("${pc.shopify.app.port}")
	String hostPort;

	@Value("${pc.shopify.app.apikey}")
	String apiKey;

	@Value("${pc.shopify.app.password}")
	String apiPassword;

	@Value("${pc.shopify.app.secret}")
	String apiSecret;

	@Value("${pc.shopify.app.webhook.version}")
	String apiVersion;

	@Value("${pc.shopify.app.accesstoken}")
	String accessToken;

	boolean isShopifyRequest = true;
	
	
	@PostConstruct
	public void init() {
		restTemplate = new RestTemplate();
		isShopifyRequest = true;
	}

	@Override
	public void setRequest(boolean isShopifyReq) {
		isShopifyRequest = isShopifyReq;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Map<String, Object> sendGetRequest(String requestUrl, Map<String, Object> paramMap) throws Throwable {
		logger.info(" ** RestTemplateServiceImpl >> sendGetRequest >> [START]");
		pleaseWait(SLEEP_TIME);

		Map<String, Object> resultMap = new HashMap<>();
		StringBuilder finalRequestUrl = new StringBuilder(hostUrl).append(requestUrl);

		HttpHeaders headers = getHeaders();
		if (isShopifyRequest) {
			headers.set("X-Shopify-Access-Token", accessToken);
		} else {
			finalRequestUrl = new StringBuilder(requestUrl);
		}
		logger.info(" ** RestTemplateServiceImpl >> sendGetRequest >> finalRequestUrl: " + finalRequestUrl);
		HttpEntity<String> jwtEntity = new HttpEntity<String>(headers);
		// Use Token to get Response
		logger.info(" ** RestTemplateServiceImpl >> finalRequestUrl: " + finalRequestUrl);
		ResponseEntity<String> result = restTemplate.exchange(finalRequestUrl.toString(), HttpMethod.GET, jwtEntity,
				String.class, paramMap);

		resultMap.put(Constants.RESULT_HTTP_STATUS_CODE, result.getStatusCode());
		if (result != null && (result.getStatusCode().equals(HttpStatus.OK)
				|| result.getStatusCode().equals(HttpStatus.CREATED))) {
			Gson gson = new Gson();
			if (Constants.TEST_ONLY) {
				System.out.println("RESPONSE: " + result.getBody());
			}

			Map<String, Object> bodyMap = gson.fromJson(result.getBody(), Map.class);
			resultMap.putAll(bodyMap);
		}
		logger.info(" ** RestTemplateServiceImpl >> sendGetRequest >> [END]");
		return resultMap;
	}

	@Override
	public Map<String, Object> sendGetRequestWithHeader(String requestUrl, Map<String, Object> paramMap)
			throws Throwable {
		logger.info(" ** RestTemplateServiceImpl >> sendGetRequestWithHeader >> [START]");
		pleaseWait(SLEEP_TIME);
		Map<String, Object> resultMap = new HashMap<>();
		StringBuilder finalRequestUrl = new StringBuilder(hostUrl).append(requestUrl);
		logger.info(" ** RestTemplateServiceImpl >> sendGetRequestWithHeader >> finalRequestUrl: " + finalRequestUrl);
		HttpHeaders headers = getHeaders();
		if (isShopifyRequest)
			headers.set("X-Shopify-Access-Token", accessToken);
		HttpEntity<String> jwtEntity = new HttpEntity<String>(headers);
		// Use Token to get Response
		logger.info(" ** RestTemplateServiceImpl >> finalRequestUrl: " + finalRequestUrl);
		ResponseEntity<String> result = restTemplate.exchange(finalRequestUrl.toString(), HttpMethod.GET, jwtEntity,
				String.class, paramMap);

		resultMap.put(Constants.RESULT_HTTP_STATUS_CODE, result.getStatusCode());
		if (result != null && (result.getStatusCode().equals(HttpStatus.OK)
				|| result.getStatusCode().equals(HttpStatus.CREATED))) {
			Gson gson = new Gson();
			Map<String, Object> bodyMap = gson.fromJson(result.getBody(), Map.class);

			resultMap.put("body", bodyMap);
			resultMap.put("header", result.getHeaders());
		}
		logger.info(" ** RestTemplateServiceImpl >> sendGetRequestWithHeader >> [END]");
		return resultMap;
	}

	@Override
	public Map<String, Object> sendPostRequest(String requestUrl, Map<String, Object> paramMap) throws Throwable {
		logger.info(" ** RestTemplateServiceImpl >> sendPostRequest >> [START]");
		pleaseWait(SLEEP_TIME);
		Map<String, Object> resultMap = new HashMap<>();
		StringBuilder finalRequestUrl = new StringBuilder(hostUrl).append(requestUrl);
		logger.info(" ** RestTemplateServiceImpl >> sendPostRequest >> finalRequestUrl: " + finalRequestUrl);
		HttpHeaders headers = getHeaders();
		if (isShopifyRequest)
			headers.set("X-Shopify-Access-Token", accessToken);
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String bodyRequest = gson.toJson(paramMap);
		logger.info(" ** RestTemplateServiceImpl >> sendPostRequest >> bodyRequest: " + bodyRequest);
		HttpEntity<String> jwtEntity = new HttpEntity<String>(bodyRequest, headers);
		// Use Token to get Response
		Map<String, Object> newParamMap = new HashMap<>();
		ResponseEntity<String> result = restTemplate.exchange(finalRequestUrl.toString(), HttpMethod.POST, jwtEntity,
				String.class, newParamMap);

		resultMap.put(Constants.RESULT_HTTP_STATUS_CODE, result.getStatusCode());
		if (result != null && (result.getStatusCode().equals(HttpStatus.OK)
				|| result.getStatusCode().equals(HttpStatus.CREATED))) {

			if (Constants.TEST_ONLY) {
				System.out.println("RESPONSE: " + result.getBody());
			}

			Map<String, Object> bodyMap = gson.fromJson(result.getBody(), Map.class);
			resultMap.putAll(bodyMap);
		}
		logger.info(" ** RestTemplateServiceImpl >> sendPostRequest >> [END]");
		return resultMap;
	}

	@Override
	public Map<String, Object> sendDeleteRequest(String requestUrl, Map<String, Object> paramMap) throws Throwable {
		logger.info(" ** RestTemplateServiceImpl >> sendDeleteRequest >> [START]");
		pleaseWait(SLEEP_TIME);
		Map<String, Object> resultMap = new HashMap<>();
		StringBuilder finalRequestUrl = new StringBuilder(hostUrl).append(requestUrl);
		logger.info(" ** RestTemplateServiceImpl >> sendDeleteRequest >> finalRequestUrl: " + finalRequestUrl);
		HttpHeaders headers = getHeaders();
		if (isShopifyRequest)
			headers.set("X-Shopify-Access-Token", accessToken);
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
				System.out.println("RESPONSE: " + result.getBody());
			}

			Map<String, Object> bodyMap = gson.fromJson(result.getBody(), Map.class);
			resultMap.putAll(bodyMap);
		}
		logger.info(" ** RestTemplateServiceImpl >> sendDeleteRequest >> [END]");
		return resultMap;
	}

	@SuppressWarnings("unused")
	@Override
	public Map<String, Object> sendPutRequest(String requestUrl, Map<String, Object> paramMap) throws Throwable {
		logger.info(" ** RestTemplateServiceImpl >> sendPutRequest >> [START]");
		
		pleaseWait(SLEEP_TIME);

		Map<String, Object> resultMap = new HashMap<>();
		StringBuilder finalRequestUrl = new StringBuilder(hostUrl).append(requestUrl);
		logger.info(" ** RestTemplateServiceImpl >> sendPutRequest >> finalRequestUrl: " + finalRequestUrl);
		HttpHeaders headers = getHeaders();
		if (isShopifyRequest)
			headers.set("X-Shopify-Access-Token", accessToken);
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
				System.out.println("RESPONSE: " + result.getBody());
			}

			Map<String, Object> bodyMap = gson.fromJson(result.getBody(), Map.class);
			resultMap.putAll(bodyMap);
		}
		logger.info(" ** RestTemplateServiceImpl >> sendPutRequest >> [END]");
		return resultMap;
	}

	private HttpComponentsClientHttpRequestFactory getClientHttpRequestFactory() {
		HttpComponentsClientHttpRequestFactory clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory();
		clientHttpRequestFactory.setHttpClient(httpClient());
		return clientHttpRequestFactory;
	}

	private void pleaseWait(int ms) {
		try {
			logger.info("Sleeping..." + ms);
			Thread.sleep(ms);
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}

	private HttpClient httpClient() {
		CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
		credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(apiKey, apiPassword));
		HttpClient client = HttpClientBuilder.create().setDefaultCredentialsProvider(credentialsProvider).build();
		return client;
	}

	private HttpHeaders getHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.set("Content-Type", MediaType.APPLICATION_JSON_VALUE);
		headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
		return headers;
	}

	@PreDestroy
	public void destroy() {

	}
}
