package com.oneclicktech.spring.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;

import org.apache.commons.collections4.CollectionUtils;
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
import com.oneclicktech.spring.service.OnlineOrderService;
import com.oneclicktech.spring.service.OnlineProductService;
import com.oneclicktech.spring.service.OnlineShopService;
import com.oneclicktech.spring.service.ProductService;
import com.oneclicktech.spring.service.RestTemplateService;
import com.oneclicktech.spring.util.NumberUtil;

@CrossOrigin(origins = "http://localhost:4200", maxAge = 3600, allowCredentials="true")
@RestController
@RequestMapping("/pc/online-order")
public class OnlineOrderController {

	private static final Logger logger = Logger.getLogger("OnlineOrderController");

	@Autowired
	RestTemplateService restTemplateService;

	@Autowired
	OnlineOrderService onlineOrderService;

  
	@Value("${pc.shopify.app.webhook.version}")
	String apiVersion;

	@Value("${pc.shopify.app.rowlimit}")
	String rowLimit;

	@PostConstruct
	public void init() {

	}
	
	@PostMapping("/getShopOrderList")
	public ResponseEntity<List<Map<String, Object>>> getOrderList(@RequestBody Map<String, Object> paramBody)
			throws Throwable {
		logger.info("** OnlineOrderController >> getOrderList >> [START]");
		HttpStatus httpStat = HttpStatus.OK;
		HashMap<String, Object> paramMap = new HashMap<>();
		paramMap.putAll(paramBody);
		List<Map<String, Object>> dataList = onlineOrderService.getOrderList(paramMap);
		logger.info("** OnlineOrderController >> getOrderList >> [END]");
		return new ResponseEntity<List<Map<String, Object>>>(dataList,  httpStat);
	}
	
	@PostMapping("/cancelOrder")
	public ResponseEntity<HttpStatus> cancelOrder(@RequestBody Map<String, Object> paramBody)
			throws Throwable {
		logger.info("** OnlineOrderController >> cancelOrder >> [START]");
		HttpStatus httpStat = HttpStatus.OK;
//		Map<String, Object> resultMap = onlineOrderService.cancelOrder(null);
//		logger.info("** OnlineOrderController >> cancelOrder >> resultMap: " + resultMap);
		
		logger.info("** OnlineOrderController >> cancelOrder >> [END]");
		return new ResponseEntity<>(httpStat);
	}
	  
	
	@GetMapping("/cancelOrderById")
	public ResponseEntity<HttpStatus> cancelOrderById(@RequestParam("orderId") String orderId)
			throws Throwable {
		logger.info("** OnlineOrderController >> cancelOrderById >> [START]");
		HttpStatus httpStat = HttpStatus.OK;
		Map<String, Object> resultMap = onlineOrderService.deleteOrder(orderId);
		logger.info("** OnlineOrderController >> cancelOrder >> resultMap: " + resultMap);
 		logger.info("** OnlineOrderController >> cancelOrderById >> [END]");
		return new ResponseEntity<>(httpStat);
	}
	 
	
	@PostMapping("/deleteOrder")
	public ResponseEntity<HttpStatus> deleteOrder(@RequestBody List<Map<String, Object>> paramList)
			throws Throwable {
		logger.info("** OnlineOrderController >> cancelOrder >> [START]");
		HttpStatus httpStat = HttpStatus.OK;
		if (CollectionUtils.isNotEmpty(paramList)) {
			for (Map<String, Object> dataMap: paramList) {
				Map<String, Object> resultMap = onlineOrderService.cancelOrder(null);
				logger.info("** OnlineOrderController >> cancelOrder >> resultMap: " + resultMap);
	 		}
	 	}
		
		logger.info("** OnlineOrderController >> cancelOrder >> [END]");
		return new ResponseEntity<>(httpStat);
	}
	 
	
	@PostMapping("/deleteAllOrders")
	public ResponseEntity<HttpStatus> deleteAllOrders(@RequestBody  Map<String, Object>  paramMap)
			throws Throwable {
		logger.info("** OnlineOrderController >> deleteAllOrders >> [START]");
		HttpStatus httpStat = HttpStatus.OK;
		List<Map<String, Object>> orderList = onlineOrderService.getOrderList(paramMap);
		for (Map<String, Object> orderMap: orderList) {
			String orderName = (String)orderMap.get("name");
			String orderId = String.valueOf(NumberUtil.getLongValue(orderMap, "id"));
			
			logger.info("** OnlineOrderController >> deleteAllOrders >> DELETING..... " + orderName);
			try {
				Map<String, Object> delResult = onlineOrderService.deleteOrder(orderId);
				logger.info("** OnlineOrderController >> deleteAllOrders >> delResult: " + delResult);
			} catch (Exception e) {
				logger.log(Level.SEVERE, e.getMessage(), e);
			}
 			
	 	}
	 	logger.info("** OnlineOrderController >> deleteAllOrders >> [END]");
		return new ResponseEntity<>(httpStat);
	}
	 

}
