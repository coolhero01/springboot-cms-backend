/**
 * 
 */
package com.oneclicktech.spring.util;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * @author kg255035
 *
 */
public class RestTesterUtil {

	private static final String REGISTRATION_URL = "http://localhost:4200/register";
	private static final String AUTHENTICATION_URL = "http://localhost:4200/authenticate";
	private static final String HELLO_URL = "http://localhost:4200/helloadmin";
	
	public void sendOAuth2RequestGetToken() {
		Map<String, Object> resultMap = new HashMap<>();

		String requestUrl = new StringBuilder("https://login.microsoftonline.com/organizations/oauth2/authorize?resource=https://bmi-sm-uat.sandbox.operations.dynamics.com&response_type=token&client_id=c0d41603-7da2-4d22-acdc-fdeffeb1f383&redirect_uri=http%3A%2F%2Flocalhost%3A4200%2Fhome").toString();

		RestTemplate restTemplate = new RestTemplate();  
		HttpHeaders headers = new HttpHeaders();
		headers.set("Accept", MediaType.ALL_VALUE); 
		HttpEntity<String> jwtEntity = new HttpEntity<String>(headers);
		// Use Token to get Response
		ResponseEntity<String> tokenResponse = restTemplate.exchange(requestUrl, HttpMethod.GET, 
				jwtEntity, String.class);
		System.out.println("sendOAuth2RequestGetToken : " + tokenResponse);
	}
	
	public void sendOAuth2Request() {
		
		Map<String, Object> resultMap = new HashMap<>();

		String requestUrl = new StringBuilder("https://bmi-sm-uat.sandbox.operations.dynamics.com/api/services/SPAVPCIntegrationServiceGroup/SPAVPCIntegrationService/getProductDetails")
				.toString();

		RestTemplate restTemplate = new RestTemplate();
		String accessToken = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsIng1dCI6Ii1LSTNROW5OUjdiUm9meG1lWm9YcWJIWkdldyIsImtpZCI6Ii1LSTNROW5OUjdiUm9meG1lWm9YcWJIWkdldyJ9.eyJhdWQiOiJodHRwczovL2JtaS1zbS11YXQuc2FuZGJveC5vcGVyYXRpb25zLmR5bmFtaWNzLmNvbSIsImlzcyI6Imh0dHBzOi8vc3RzLndpbmRvd3MubmV0LzkzZGZiZGM3LTE3OTEtNDRiNi1iYWFmLTUyZjZhZDg2ZjYxMy8iLCJpYXQiOjE2NzEwMzEyMDcsIm5iZiI6MTY3MTAzMTIwNywiZXhwIjoxNjcxMDM1NTY5LCJhY3IiOiIxIiwiYWlvIjoiRTJaZ1lHaEs1ZEdNTWZYVDdBczZ3OGFnSGwxL1hQRGRmYW1sS2MzcmhWNHZQNXh5djRMVmUxSDREdU43Q2dmQ3VVSzFZanJFQVE9PSIsImFtciI6WyJwd2QiXSwiYXBwaWQiOiJjMGQ0MTYwMy03ZGEyLTRkMjItYWNkYy1mZGVmZmViMWYzODMiLCJhcHBpZGFjciI6IjAiLCJpcGFkZHIiOiIxMzYuMTU4LjMzLjE2NSIsIm5hbWUiOiJEMzY1IFRFU1QgQUNDT1VOVCIsIm9pZCI6ImZmZjAxODQ2LWIxMjYtNDNiYi1hZDVmLTFmNDdjM2IwNmU2YSIsInB1aWQiOiIxMDAzMjAwMjFCNzQyQzBDIiwicmgiOiIwLkFXNEF4NzNmazVFWHRrUzZyMUwyclliMkV4VUFBQUFBQUFBQXdBQUFBQUFBQUFCVUFMMC4iLCJzY3AiOiJBWC5GdWxsQWNjZXNzIEN1c3RvbVNlcnZpY2UuRnVsbEFjY2VzcyBPZGF0YS5GdWxsQWNjZXNzIiwic3ViIjoiRUZFM1R2V3FBU2NDS0JXbnE2TFBGTXJfODYybXNxejZHcmdGVlRzeUh2WSIsInRpZCI6IjkzZGZiZGM3LTE3OTEtNDRiNi1iYWFmLTUyZjZhZDg2ZjYxMyIsInVuaXF1ZV9uYW1lIjoiZDM2NXRlc3RhY2NvdW50QHNoYWtleXMuYml6IiwidXBuIjoiZDM2NXRlc3RhY2NvdW50QHNoYWtleXMuYml6IiwidXRpIjoiV1B4ZGV1aWJXa3lQWlF2azNJTVJBQSIsInZlciI6IjEuMCJ9.oXZI1eLChyfX1KryfR0BrVvi8aRtDW_DHlSw7z3ub7scCRCj_yEHiNcjZkxOeUp8MyCQP40T8mgQnMjpO_ky-MjwOhYnsUkEPqRt1Mtt24i856hE0-a_uQDW-or3UQem3byd9NrfViKttiqjDAQeUF0UgFuA7zNMNtVmreo9B_HH1cjQmVUKhNCp63JvG_olpU7SwOwvGYdS4g5tVKw-5WtR5-ldfIywkDsDdzGzVn9FWoKw-q6Ve3248AI73WSSJI6-p9wkDvsoHXaXWQUpSrZIlqgX1x2TqySLUDmXQqbXlZOQxAnDGNKIvesXGdeBlCou54aERjgd7tUfb6ZoZg";
		String authToken = "Bearer " + accessToken;
		HttpHeaders headers = new HttpHeaders();
		headers.set("Content-Type", MediaType.APPLICATION_JSON_VALUE);
		headers.set("Accept", MediaType.ALL_VALUE);
		headers.set("Authorization", authToken);
		String bodyRequest = "{\r\n" + 
				"	\"_dataContract\": {\r\n" + 
				"		\"DataAreaId\":\"cipc\"\r\n" + 
				"	}\r\n" + 
				"}";
		HttpEntity<String> jwtEntity = new HttpEntity<String>(bodyRequest, headers);
		// Use Token to get Response
		ResponseEntity<String> tokenResponse = restTemplate.exchange(requestUrl, HttpMethod.POST, 
				jwtEntity, String.class);
		System.out.println("sendOAuth2Request >> tokenResponse: " + tokenResponse);
		
	}

	public void sendRequest() {
		Map<String, Object> resultMap = new HashMap<>();

		String requestUrl = new StringBuilder("https://potato-corner-uat.myshopify.com").append("/admin/api/")
				.append("2022-01").append("/products.json?limit=").append("250").toString();

		RestTemplate restTemplate = new RestTemplate();
		String accessToken = "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";
		String token = "Bearer " + accessToken;
		HttpHeaders headers = getHeaders();
		headers.set("X-Shopify-Access-Token", accessToken);
		HttpEntity<String> jwtEntity = new HttpEntity<String>(headers);
		// Use Token to get Response
		ResponseEntity<String> helloResponse = restTemplate.exchange(requestUrl, HttpMethod.GET, jwtEntity,
				String.class);
		System.out.println("helloResponse: " + helloResponse);
	}
 
	public HttpHeaders getHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.set("Content-Type", MediaType.APPLICATION_JSON_VALUE);
		headers.set("Accept", MediaType.ALL_VALUE);
		return headers;
	}

	private HttpComponentsClientHttpRequestFactory getClientHttpRequestFactory() {
		HttpComponentsClientHttpRequestFactory clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory();
		clientHttpRequestFactory.setHttpClient(httpClient());
		return clientHttpRequestFactory;
	}

	private HttpClient httpClient() {
		CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
		credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials("", ""));
		HttpClient client = HttpClientBuilder.create().setDefaultCredentialsProvider(credentialsProvider).build();
		return client;
	}
	
	

	public static void main(String[] args) {
		RestTesterUtil restUtil = new RestTesterUtil(); 
		
		
		Map<String, Object> resultMap = new HashMap<>();

//		String requestUrl = new StringBuilder("https://potato-corner-uat.myshopify.com").append("/admin/api/")
//				.append("2022-01").append("/products.json?limit=").append("250").toString();
		
		String requestUrl = "https://potatocornerspci.myshopify.com/admin/api/2022-01/orders/6719230411027/transactions.json";
		
		RestTemplate restTemplate = new RestTemplate();
		String accessToken = "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";
		String token = "Bearer " + accessToken;
		HttpHeaders headers = restUtil.getHeaders();
		headers.set("X-Shopify-Access-Token", accessToken);
		
		// Use Token to get Response
		
		Map<String, Object> rootMap = new LinkedHashMap<>();
		Map<String, Object> requestMap = new LinkedHashMap<>();
		requestMap.put("parent_id", null);
		requestMap.put("currency", "PHP");
		requestMap.put("amount", "10483.20");
		requestMap.put("kind", "capture");

		rootMap.put("transaction", requestMap);
		
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String bodyRequest = gson.toJson(rootMap);
		System.out.println(requestUrl);
		System.out.println(bodyRequest);
		
		HttpEntity<String> jwtEntity = new HttpEntity<String>(bodyRequest, headers);
		// Use Token to get Response 
		Map<String, Object> newParamMap = new HashMap<>();
		ResponseEntity<String> result = restTemplate.exchange(requestUrl, 
				HttpMethod.POST, jwtEntity, String.class);
		
		System.out.println("result: " + result);
	}
}
