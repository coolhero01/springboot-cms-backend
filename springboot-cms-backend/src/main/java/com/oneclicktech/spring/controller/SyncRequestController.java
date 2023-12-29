package com.oneclicktech.spring.controller;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.InvalidCredentialsException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.oneclicktech.spring.domain.Constants;
import com.oneclicktech.spring.mapper.ClientTokenMapper;
import com.oneclicktech.spring.mapper.ShopOrderMapper;
import com.oneclicktech.spring.service.EmailService;
import com.oneclicktech.spring.service.OnlineCustomerService;
import com.oneclicktech.spring.service.OnlineOrderService;
import com.oneclicktech.spring.service.OnlineProductService;
import com.oneclicktech.spring.service.OnlineShopService;
import com.oneclicktech.spring.service.RestD365Service;
import com.oneclicktech.spring.service.RestTemplateService;
import com.oneclicktech.spring.service.SyncD365Service;
import com.oneclicktech.spring.util.NumberUtil;

@CrossOrigin(origins = "http://localhost:4200", maxAge = 3600, allowCredentials = "true")
@RestController
@RequestMapping("/pc/sync")
public class SyncRequestController {

	private static final Logger logger = Logger.getLogger("SyncRequestController");

	@Autowired
	RestTemplateService restTemplateService;

	@Autowired
	OnlineOrderService onlineOrderService;
	
	@Autowired
	OnlineCustomerService onlineCustomerService;
	
	@Autowired
	OnlineShopService onlineShopService;
 	
	@Autowired
	OnlineProductService onlineProductService;

	@Autowired
	EmailService emailService;

	@Autowired
	SyncD365Service syncD365Service;

	@Autowired
	ShopOrderMapper shopOrderMapper;
  
	 
	@Autowired
	RestD365Service restD365Service; 
	
	@PostConstruct
	public void init() {

	}

	 
	@PostMapping("/syncD365ProductToLocalDB")
	public ResponseEntity<HttpStatus> syncD365ProductToLocalDB(@RequestBody Map<String, Object> paramBody)
			throws Throwable {
		HttpStatus httpStat = HttpStatus.OK;
		HashMap<String, Object> searchMap = new HashMap<>();
		searchMap.put("tokenType", "D365");
		logger.info("*** SyncRequestController >> syncD365ProductToLocalDB >>  [START] ");
		try { 
			String productToken =  restD365Service.getLatestD365Token();
			Map<String, Object> prodMap = new HashMap<>();
			prodMap.put("accessToken", productToken);
			syncD365Service.syncProductDataToDB(prodMap);
		} catch (Throwable t) { 
			logger.info("*** SyncRequestController >> syncD365ProductToLocalDB >>  [ERROR] ");
			logger.log(Level.SEVERE, t.getMessage(), t);
		}
		logger.info("*** SyncRequestController >> syncD365ProductToLocalDB >>  [END] ");
		return new ResponseEntity<>(httpStat);
	}

	public ResponseEntity<HttpStatus> syncD365CustomerToLocalDB(@RequestBody Map<String, Object> paramBody)
			throws Throwable {
		HttpStatus httpStat = HttpStatus.OK;
		HashMap<String, Object> searchMap = new HashMap<>();
		searchMap.put("tokenType", "D365");

		logger.info("*** SyncRequestController >> syncD365CustomerToLocalDB >>  [START] ");
		try {
			String customerToken = restD365Service.getLatestD365Token();
			Map<String, Object> custMap = new HashMap<>();
			custMap.put("accessToken", customerToken);
			syncD365Service.syncCustomerDataToDB(custMap);
		} catch (Throwable t) {
			logger.info("*** SyncRequestController >> syncD365CustomerToLocalDB >>  [ERROR] "); ;
			logger.log(Level.SEVERE, t.getMessage(), t);
		}

		logger.info("*** SyncRequestController >> syncD365CustomerToLocalDB >>  [END] ");
		return new ResponseEntity<>(httpStat);
	}

	public ResponseEntity<HttpStatus> syncD365WarehouseToLocalDB(@RequestBody Map<String, Object> paramBody)
			throws Throwable {
		HttpStatus httpStat = HttpStatus.OK;
 
		logger.info("*** SyncRequestController >> syncD365WarehouseToLocalDB >>  [START] ");
		try {
			String warehouseToken = restD365Service.getLatestD365Token();
			Map<String, Object> wareMap = new HashMap<>();
			wareMap.put("accessToken", warehouseToken);
			syncD365Service.syncWarehouseDataToDB(wareMap);
		} catch (Throwable t) {
			logger.info("*** SyncRequestController >>syncD365WarehouseToLocalDB >> ERROR: ");
			logger.log(Level.SEVERE, t.getMessage(), t);
		}

		logger.info("*** SyncRequestController >> syncD365WarehouseToLocalDB >>  [END] ");
		return new ResponseEntity<>(httpStat);
	}
	
	
	@SuppressWarnings("unused")
	@PostMapping("/syncLocalProductToOnline")
	public ResponseEntity<HttpStatus> syncLocalProductToOnline(@RequestBody Map<String, Object> paramBody)
			throws Throwable {

		try {
			boolean success = onlineShopService.syncLocalProductToOnline(paramBody);
			return new ResponseEntity<>(HttpStatus.OK);
		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
			return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping("/syncLocalCustomerToOnline")
	public ResponseEntity<HttpStatus> syncLocalCustomerToOnline(@RequestBody Map<String, Object> paramBody)
			throws Throwable {

		try {
			boolean success = onlineShopService.syncLocalCustomerToOnline(paramBody);

			return new ResponseEntity<>(HttpStatus.OK);
		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
			return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping("/syncLocalWarehouseToOnline")
	public ResponseEntity<HttpStatus> syncLocalWarehouseToOnline(@RequestBody Map<String, Object> paramBody)
			throws Throwable {

		try {
			boolean success = onlineShopService.syncLocalProductToOnline(paramBody);

			return new ResponseEntity<>(HttpStatus.OK);
		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
			return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping("/deleteAllOrders")
	public ResponseEntity<HttpStatus> deleteAllOrders(@RequestBody Map<String, Object> paramMap) throws Throwable {
		logger.info("** SyncRequestController >> deleteAllOrders >> [START]");
		HttpStatus httpStat = HttpStatus.OK;
		List<Map<String, Object>> orderList = onlineOrderService.getOrderList(paramMap);
		for (Map<String, Object> orderMap : orderList) {
			String orderName = (String) orderMap.get("name");
			String orderId = String.valueOf(NumberUtil.getLongValue(orderMap, "orderId"));

			logger.info("** SyncRequestController >> deleteAllOrders >> DELETING..... " + orderName);

			try {
				Map<String, Object> delResult = onlineOrderService.deleteOrder(orderId);
				logger.info("** SyncRequestController >> deleteAllOrders >> delResult: " + delResult);
			} catch (Exception e) {
				logger.log(Level.SEVERE, e.getMessage(), e);
			}

		}
		logger.info("** SyncRequestController >> deleteAllOrders >> [END]");
		return new ResponseEntity<>(httpStat);
	}

	@PostMapping("/deleteAllProduct")
	public ResponseEntity<HttpStatus> deleteAllProduct(@RequestBody Map<String, Object> paramBody) throws Throwable {
		logger.info("** SyncRequestController >> deleteAllProduct >> [START]");
		HttpStatus httpStat = HttpStatus.OK;
		List<Map<String, Object>> dataList = onlineProductService.getAllOnlineProducts(null);
		for (Map<String, Object> prodMap : dataList) {
			try {
				logger.info("** SyncRequestController >> deleteAllProduct >> DELETING.... " + prodMap.toString());
				Map<String, Object> resultMap = onlineProductService.deleteProduct(prodMap);
				if (MapUtils.isNotEmpty(resultMap)) {
					logger.info("** SyncRequestController >> deleteAllProduct >> DELETE Result:" + resultMap.toString());
				}
			} catch (Exception e) {
				logger.log(Level.SEVERE, e.getMessage(), e);
			}

		}

		logger.info("** SyncRequestController >> deleteAllProduct >> [END]");
		return new ResponseEntity<>(httpStat);
	}
	
	
	@PostMapping("/deleteAllCustomer")
	public ResponseEntity<HttpStatus> deleteAllCustomer(@RequestBody Map<String, Object> paramMap) throws Throwable {
		logger.info("** SyncRequestController >> deleteAllCustomer >> [START]");
		HttpStatus httpStat = HttpStatus.OK;
		List<Map<String, Object>> orderList = onlineOrderService.getOrderList(paramMap);
		for (Map<String, Object> orderMap : orderList) { 
			String customerId = String.valueOf(NumberUtil.getLongValue(orderMap, "id"));
	 		logger.info("** SyncRequestController >> deleteAllCustomer >> DELETING.....customerId: " + customerId);

			try {
				Map<String, Object> delResult = onlineCustomerService.deleteCustomerById(customerId);
				logger.info("** SyncRequestController >> deleteAllCustomer >> delResult: " + delResult);
			} catch (Exception e) {
				logger.log(Level.SEVERE, e.getMessage(), e);
			}

		}
		logger.info("** SyncRequestController >> deleteAllCustomer >> [END]");
		return new ResponseEntity<>(httpStat);
	}
	
	 

}
