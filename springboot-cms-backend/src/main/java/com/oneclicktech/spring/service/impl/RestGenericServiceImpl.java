package com.oneclicktech.spring.service.impl;

import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.net.ssl.SSLContext;

import org.apache.commons.collections4.MapUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.springframework.core.io.Resource;
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
import com.oneclicktech.spring.domain.Constants;
import com.oneclicktech.spring.service.RestGenericService;

@Service
public class RestGenericServiceImpl implements RestGenericService {

	private static final Logger logger = Logger.getLogger("RestGenericServiceImpl");

	private static final int SLEEP_TIME = 400; // 1000 = 1sec

	RestTemplate restTemplate;

	@PostConstruct
	public void init() {
		restTemplate = new RestTemplate();
	}
 

	@SuppressWarnings("unchecked")
	@Override
	public Map<String, Object> sendGetRequest(String requestUrl, Map<String, Object> paramMap) throws Throwable {
		logger.info(" ** RestGenericServiceImpl >> sendGetRequest >> [START]");

		Map<String, Object> resultMap = new HashMap<>();
		StringBuilder finalRequestUrl = new StringBuilder(requestUrl);
		HttpHeaders headers = getHeaders();

		logger.info(" ** RestGenericServiceImpl >> sendGetRequest >> finalRequestUrl: " + finalRequestUrl);
		HttpEntity<String> jwtEntity = new HttpEntity<String>(headers);
		// Use Token to get Response
		logger.info(" ** RestGenericServiceImpl >> finalRequestUrl: " + finalRequestUrl);
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
		logger.info(" ** RestGenericServiceImpl >> sendGetRequest >> [END]");
		return resultMap;
	}

	@Override
	public Map<String, Object> sendGetRequestWithHeader(String requestUrl, Map<String, Object> paramMap)
			throws Throwable {
		logger.info(" ** RestGenericServiceImpl >> sendGetRequestWithHeader >> [START]");

		Map<String, Object> resultMap = new HashMap<>();
		StringBuilder finalRequestUrl = new StringBuilder(requestUrl);
		logger.info(" ** RestGenericServiceImpl >> sendGetRequestWithHeader >> finalRequestUrl: " + finalRequestUrl);
		HttpHeaders headers = getHeaders();
		HttpEntity<String> jwtEntity = new HttpEntity<String>(headers);
		// Use Token to get Response
		logger.info(" ** RestGenericServiceImpl >> finalRequestUrl: " + finalRequestUrl);
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
		logger.info(" ** RestGenericServiceImpl >> sendGetRequestWithHeader >> [END]");
		return resultMap;
	}

	@Override
	public Map<String, Object> sendPostRequest(String requestUrl, Map<String, Object> paramMap) throws Throwable {
		logger.info(" ** RestGenericServiceImpl >> sendPostRequest >> [START]");

		Map<String, Object> resultMap = new HashMap<>();
		StringBuilder finalRequestUrl = new StringBuilder(requestUrl);
		logger.info(" ** RestGenericServiceImpl >> sendPostRequest >> finalRequestUrl: " + finalRequestUrl);
		HttpHeaders headers = getHeaders();

		Gson gson = new Gson();
		String bodyRequest = gson.toJson(paramMap);

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
			if (MapUtils.isNotEmpty(bodyMap))
				resultMap.putAll(bodyMap);
		}
		logger.info(" ** RestGenericServiceImpl >> sendPostRequest >> [END]");
		return resultMap;
	}

	@Override
	public Map<String, Object> sendDeleteRequest(String requestUrl, Map<String, Object> paramMap) throws Throwable {
		logger.info(" ** RestGenericServiceImpl >> sendDeleteRequest >> [START]");

		Map<String, Object> resultMap = new HashMap<>();
		StringBuilder finalRequestUrl = new StringBuilder(requestUrl);
		logger.info(" ** RestGenericServiceImpl >> sendDeleteRequest >> finalRequestUrl: " + finalRequestUrl);
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
				System.out.println("RESPONSE: " + result.getBody());
			}

			Map<String, Object> bodyMap = gson.fromJson(result.getBody(), Map.class);
			if (MapUtils.isNotEmpty(bodyMap))
				resultMap.putAll(bodyMap);
		}
		logger.info(" ** RestGenericServiceImpl >> sendDeleteRequest >> [END]");
		return resultMap;
	}

	@SuppressWarnings("unused")
	@Override
	public Map<String, Object> sendPutRequest(String requestUrl, Map<String, Object> paramMap) throws Throwable {
		logger.info(" ** RestGenericServiceImpl >> sendPutRequest >> [START]");

		Map<String, Object> resultMap = new HashMap<>();
		StringBuilder finalRequestUrl = new StringBuilder(requestUrl);
		logger.info(" ** RestGenericServiceImpl >> sendPutRequest >> finalRequestUrl: " + finalRequestUrl);
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
				System.out.println("RESPONSE: " + result.getBody());
			}

			Map<String, Object> bodyMap = gson.fromJson(result.getBody(), Map.class);
			if (MapUtils.isNotEmpty(bodyMap))
				resultMap.putAll(bodyMap);
		}
		logger.info(" ** RestGenericServiceImpl >> sendPutRequest >> [END]");
		return resultMap;
	}

	@Override
	public Map<String, Object> sendGetRequest(String requestUrl, String accessToken, Map<String, Object> paramMap)
			throws Throwable {
		logger.info(" ** RestGenericServiceImpl >> sendGetRequest >> [START]");

		Map<String, Object> resultMap = new HashMap<>();
		StringBuilder finalRequestUrl = new StringBuilder(requestUrl);
		HttpHeaders headers = getHeaders();
		String authToken = "Bearer " + accessToken;
		headers.set("Authorization", authToken);

		logger.info(" ** RestGenericServiceImpl >> sendGetRequest >> finalRequestUrl: " + finalRequestUrl);
		HttpEntity<String> jwtEntity = new HttpEntity<String>(headers);
		// Use Token to get Response
		logger.info(" ** RestGenericServiceImpl >> finalRequestUrl: " + finalRequestUrl);
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
		logger.info(" ** RestGenericServiceImpl >> sendGetRequest >> [END]");
		return resultMap;
	}

	@Override
	public Map<String, Object> sendGetRequestWithHeader(String requestUrl, String accessToken,
			Map<String, Object> paramMap) throws Throwable {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, Object> sendPostRequest(String requestUrl, String accessToken, Map<String, Object> paramMap)
			throws Throwable {
		logger.info(" ** RestGenericServiceImpl >> sendPostRequest >> [START]");

		Map<String, Object> resultMap = new HashMap<>();
		StringBuilder finalRequestUrl = new StringBuilder(requestUrl);
		logger.info(" ** RestGenericServiceImpl >> sendPostRequest >> finalRequestUrl: " + finalRequestUrl);
		HttpHeaders headers = getHeaders();
		String authToken = "Bearer " + accessToken;
		headers.set("Authorization", authToken);

		Gson gson = new Gson();
		String bodyRequest = gson.toJson(paramMap);

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
			if (MapUtils.isNotEmpty(bodyMap))
				resultMap.putAll(bodyMap);
		}
		logger.info(" ** RestGenericServiceImpl >> sendPostRequest >> [END]");
		return resultMap;
	}

	@Override
	public Map<String, Object> sendDeleteRequest(String requestUrl, String accessToken, Map<String, Object> paramMap)
			throws Throwable {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, Object> sendPutRequest(String requestUrl, String accessToken, Map<String, Object> paramMap)
			throws Throwable {
		// TODO Auto-generated method stub
		return null;
	}

	private HttpHeaders getHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.add("user-agent", "Application");
		headers.set("Content-Type", MediaType.APPLICATION_JSON_VALUE);
		headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
		return headers;
	}

	@PreDestroy
	public void destroy() {

	}
}
