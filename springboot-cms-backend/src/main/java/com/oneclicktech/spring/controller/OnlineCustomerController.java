package com.oneclicktech.spring.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;

import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.internal.LinkedTreeMap;
import com.oneclicktech.spring.datajpa.model.Tutorial;
import com.oneclicktech.spring.domain.Constants;
import com.oneclicktech.spring.service.OnlineCustomerService;
import com.oneclicktech.spring.service.OnlineProductService;
import com.oneclicktech.spring.service.OnlineShopService;
import com.oneclicktech.spring.service.ProductService;
import com.oneclicktech.spring.service.RestTemplateService;

@CrossOrigin(origins = "http://localhost:4200", maxAge = 3600, allowCredentials="true")
@RestController
@RequestMapping("/pc/online-customer")
public class OnlineCustomerController {

	private static final Logger logger = Logger.getLogger("OnlineCustomerController");

 
	@Autowired
	OnlineCustomerService onlineCustomerService;

	@Autowired
	OnlineShopService onlineShopService;

	@Value("${pc.shopify.app.webhook.version}")
	String apiVersion;

	@Value("${pc.shopify.app.rowlimit}")
	String rowLimit;

	@PostConstruct
	public void init() {

	}
	
	@PostMapping("/getShopCustomerList")
	public ResponseEntity<List<Map<String, Object>>> getCustomerList(@RequestBody Map<String, Object> paramBody)
			throws Throwable {
		logger.info("** OnlineCustomerController >> getCustomerList >> [START]");
		HttpStatus httpStat = HttpStatus.OK;
		HashMap<String, Object> paramMap = new HashMap<>();
		paramMap.putAll(paramBody);
		List<Map<String, Object>> dataList = onlineCustomerService.getCustomerList(paramMap);
		logger.info("** OnlineCustomerController >> getCustomerList >> [END]");
		return new ResponseEntity<List<Map<String, Object>>>(dataList,  httpStat);
	}
	
	@PostMapping("/deleteCustomer")
	public ResponseEntity<HttpStatus> deleteCustomer(@RequestBody List<Map<String, Object>> paramList)
			throws Throwable {
		logger.info("** OnlineCustomerController >> deleteCustomer >> [START]");
		HttpStatus httpStat = HttpStatus.OK;
		logger.info("** OnlineCustomerController >> deleteCustomer >> [END]");
		return new ResponseEntity<>(httpStat);
	}
	 
	@PostMapping("/createCustomer")
	public ResponseEntity<HttpStatus> createCustomer(@RequestBody Map<String, Object> paramBody)
			throws Throwable {
		logger.info("** OnlineCustomerController >> createCustomer >> [START]");
		HttpStatus httpStat = HttpStatus.OK;
		logger.info("** OnlineCustomerController >> createCustomer >> [END]");
		return new ResponseEntity<>(httpStat);
	}
	 
	@PostMapping("/updateCustomer")
	public ResponseEntity<HttpStatus> updateCustomer(@RequestBody Map<String, Object> paramBody)
			throws Throwable {
		logger.info("** OnlineCustomerController >> updateCustomer >> [START]");
		HttpStatus httpStat = HttpStatus.OK;
		logger.info("** OnlineCustomerController >> updateCustomer >> [END]");
		return new ResponseEntity<>(httpStat);
	}
	 

}
