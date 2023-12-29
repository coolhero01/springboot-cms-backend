package com.oneclicktech.spring.service.impl;

import java.io.File;
import java.io.FileReader;
import java.net.URI;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.InvalidCredentialsException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.oneclicktech.spring.domain.Constants;
import com.oneclicktech.spring.mapper.ClientTokenMapper;
import com.oneclicktech.spring.mapper.ProductMasterMapper;
import com.oneclicktech.spring.service.CustomerService;
import com.oneclicktech.spring.service.ProductService;
import com.oneclicktech.spring.service.RestD365Service;
import com.oneclicktech.spring.service.RestTemplateService;

@Service
public class RestD365ServiceImpl implements RestD365Service {

	private static final Logger logger = Logger.getLogger("RestD365ServiceImpl");

	RestTemplate restTemplate;

	@Value("${pc.shopify.app.hosturl}")
	String hostUrl;
	
	@Autowired
	ClientTokenMapper clientTokenMapper;
	
	@PostConstruct
	public void init() {
		restTemplate = new RestTemplate();
	}

	@Override
	public Map<String, Object> sendGetRequest(String requestUrl, String accessToken, Map<String, Object> paramMap)
			throws Throwable {
		Map<String, Object> resultMap = new HashMap<>();
		RestTemplate restTemplate = new RestTemplate();
		String authToken = "Bearer " + accessToken;
		HttpHeaders headers = new HttpHeaders();
		headers.set("Content-Type", MediaType.APPLICATION_JSON_VALUE);
		headers.set("Accept", MediaType.ALL_VALUE);
		headers.set("Authorization", authToken);
		Gson gson = new Gson();
		String bodyRequest = gson.toJson(paramMap, Map.class);
		HttpEntity<String> jwtEntity = new HttpEntity<String>(bodyRequest, headers);
		// Use Token to get Response
		ResponseEntity<String> result = restTemplate.exchange(requestUrl, HttpMethod.POST, jwtEntity,
				String.class);
		
		resultMap.put(Constants.RESULT_HTTP_STATUS_CODE, result.getStatusCode());
		if (result != null && (result.getStatusCode().equals(HttpStatus.OK)
				|| result.getStatusCode().equals(HttpStatus.CREATED))) { 
			Map<String, Object> bodyMap = gson.fromJson(result.getBody(), Map.class);
			resultMap.putAll(bodyMap);
		}
		
		return resultMap;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Map<String, Object> sendPostRequest(String requestUrl, String accessToken, Map<String, Object> paramMap)
			throws Throwable {
		Map<String, Object> resultMap = new HashMap<>();
		RestTemplate restTemplate = new RestTemplate();
		String authToken = "Bearer " + accessToken;
		HttpHeaders headers = new HttpHeaders();
		headers.set("Content-Type", MediaType.APPLICATION_JSON_VALUE);
		headers.set("Accept", MediaType.ALL_VALUE);
		headers.set("Authorization", authToken);
		Gson gson = new Gson();
		String bodyRequest = gson.toJson(paramMap, Map.class);
		HttpEntity<String> jwtEntity = new HttpEntity<String>(bodyRequest, headers);
		// Use Token to get Response
		ResponseEntity<String> result = restTemplate.exchange(requestUrl, HttpMethod.POST, jwtEntity,
				String.class);
		resultMap.put(Constants.RESULT_HTTP_STATUS_CODE, result.getStatusCode());
		if (result != null && (result.getStatusCode().equals(HttpStatus.OK)
				|| result.getStatusCode().equals(HttpStatus.CREATED))) {  

			List<Map<String, Object>> dataList = gson.fromJson(result.getBody(), List.class);
			resultMap.put(Constants.DATA_LIST, dataList);
		}
		
		return resultMap;
	}
	
	
		
	
	@Override
	public Map<String, Object> sendPostRequest(String requestUrl, String accessToken, Map<String, Object> paramMap,
			String objectType) throws Throwable {
		Map<String, Object> resultMap = new HashMap<>();
		RestTemplate restTemplate = new RestTemplate();
		String authToken = "Bearer " + accessToken;
		HttpHeaders headers = new HttpHeaders();
		headers.set("Content-Type", MediaType.APPLICATION_JSON_VALUE);
		headers.set("Accept", MediaType.ALL_VALUE);
		headers.set("Authorization", authToken);
		Gson gson = new Gson();
		String bodyRequest = gson.toJson(paramMap, Map.class);
		HttpEntity<String> jwtEntity = new HttpEntity<String>(bodyRequest, headers);
		// Use Token to get Response
		ResponseEntity<String> result = restTemplate.exchange(requestUrl, HttpMethod.POST, jwtEntity,
				String.class);
		resultMap.put(Constants.RESULT_HTTP_STATUS_CODE, result.getStatusCode());
		if (result != null && (result.getStatusCode().equals(HttpStatus.OK)
				|| result.getStatusCode().equals(HttpStatus.CREATED))) {  
			
			switch (objectType) {
			case "string":
				String value = gson.fromJson(result.getBody(), String.class);
				resultMap.put("result", value);
				break;
			case "map":
				Map<String, Object> mapValue = gson.fromJson(result.getBody(), Map.class);
				resultMap.put("result", mapValue);
				break;

			default:
				
				List<Map<String, Object>> dataList = gson.fromJson(result.getBody(), List.class);
				resultMap.put(Constants.DATA_LIST, dataList);
				break;
			}
			
			  
		}
		
		return resultMap;
	}

	@Override
	public Map<String, Object> sendDeleteRequest(String requestUrl, String accessToken, Map<String, Object> paramMap)
			throws Throwable {

		return null;
	}

	@Override
	public Map<String, Object> sendPutRequest(String requestUrl, String accessToken, Map<String, Object> paramMap)
			throws Throwable {

		return null;
	}
	
	
	
	@Override
	public String getLatestD365Token() throws Throwable {
		HashMap<String, Object> searchMap = new HashMap<>();
		searchMap.put("tokenType", "D365");

		List<Map<String, Object>> tokenMapList = clientTokenMapper.getTokenList(searchMap);
		Map<String, Object> tokenMap = tokenMapList.get(0);
		String accessToken = StringUtils.trim((String) tokenMap.get("access_token"));
		if (StringUtils.isBlank(accessToken)) {
			throw new InvalidCredentialsException("Invalid Access Token / Access Token is empty");
		}
		return accessToken;
	}

	@PreDestroy
	public void destroy() {

	}
}
