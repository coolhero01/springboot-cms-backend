package com.oneclicktech.spring.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.oneclicktech.spring.service.OnlineProductService;
import com.oneclicktech.spring.service.OnlineShopService;
import com.oneclicktech.spring.service.RestTemplateService;

@CrossOrigin(origins = "http://localhost:4200", maxAge = 3600, allowCredentials="true") 
@RestController
@RequestMapping("/pc/online-product")
public class OnlineProductController {

	private static final Logger logger = Logger.getLogger("OnlineProductController");

	@Autowired
	RestTemplateService restTemplateService;

	@Autowired
	OnlineProductService onlineProductService;
 

	@Value("${pc.shopify.app.webhook.version}")
	String apiVersion;

	@Value("${pc.shopify.app.rowlimit}")
	String rowLimit;

	@PostConstruct
	public void init() {
		
	}
	
	@PostMapping("/getShopProductList")
	public ResponseEntity<List<Map<String, Object>>> getProductList(@RequestBody Map<String, Object> paramBody)
			throws Throwable {
		logger.info("** OnlineProductController >> getProductList >> [START]");
		HttpStatus httpStat = HttpStatus.OK;
		HashMap<String, Object> paramMap = new HashMap<>();
		paramMap.putAll(paramBody);
		//List<Map<String, Object>> dataList = onlineProductService.getProductList(paramMap);
		List<Map<String, Object>> dataList = onlineProductService.getAllOnlineProducts(null);
		logger.info("** OnlineProductController >> getProductList >> [END]");
		return new ResponseEntity<List<Map<String, Object>>>(dataList,  httpStat);
	}
	
	@GetMapping("/getAllOnlineProducts")
	public ResponseEntity<List<Map<String, Object>>> getAllOnlineProducts()
			throws Throwable {
		logger.info("** OnlineProductController >> getAllOnlineProducts >> [START]");
		HttpStatus httpStat = HttpStatus.OK;
  		List<Map<String, Object>> dataList = onlineProductService.getAllOnlineProducts(null);
		//onlineProductService.updateProductDBFromOnline(null);
		logger.info("** OnlineProductController >> getAllOnlineProducts >> [END]");
	 	
		return new ResponseEntity<List<Map<String, Object>>>(dataList,  httpStat);
	}
	
	@PostMapping("/deleteProduct")
	public ResponseEntity<HttpStatus> deleteProduct(@RequestBody List<Map<String, Object>> paramList)
			throws Throwable {
		logger.info("** OnlineProductController >> deleteProduct >> [START]");
		HttpStatus httpStat = HttpStatus.OK;
		if (CollectionUtils.isNotEmpty(paramList)) {
			for (Map<String, Object> dataMap: paramList) {
				Map<String, Object> resultMap = onlineProductService.deleteProduct(dataMap);
				logger.info("** OnlineProductController >> deleteProduct >> resultMap: " + resultMap);
	 		}
	 	}
	  
		logger.info("** OnlineProductController >> deleteProduct >> [END]");
		return new ResponseEntity<>(httpStat);
	} 
	
	 
	@PostMapping("/updateProduct")
	public ResponseEntity<HttpStatus> updateProduct(@RequestBody Map<String, Object> paramBody)
			throws Throwable {
		logger.info("** OnlineProductController >> updateProduct >> [START]");
		HttpStatus httpStat = HttpStatus.OK;
		HashMap<String, Object> paramMap = new HashMap<>();
		paramMap.putAll(paramBody);
		onlineProductService.updateProduct(paramMap);
		logger.info("** OnlineProductController >> updateProduct >> [END]");
		return new ResponseEntity<>(httpStat);
	}
	
 	
	@PostMapping("/createProductMetafield")
	public ResponseEntity<List<Map<String, Object>>> createProductMetafield(@RequestBody List<Map<String, Object>> paramList) 	
			throws Throwable {
		logger.info("** OnlineProductController >> createProductMetafield >> [START]");
		HttpStatus httpStat = HttpStatus.OK; 
		for (Map<String, Object> prodMap: paramList)  {
			Map<String, Object> resultMap = onlineProductService.createProductMetafield(prodMap);
			if (MapUtils.isNotEmpty(resultMap)) {
				logger.info("** OnlineProductController >> resultMap: " + resultMap.toString());
			}
		}
 		
		logger.info("** OnlineProductController >> createProductMetafield >> [END]");
		
		return new ResponseEntity<List<Map<String, Object>>>(paramList,  httpStat);
	}
 
	
	@PostMapping("/deleteAllProduct")
	public ResponseEntity<HttpStatus> deleteAllProduct(@RequestBody Map<String, Object> paramBody)
			throws Throwable {
		logger.info("** OnlineProductController >> deleteAllProduct >> [START]");
		HttpStatus httpStat = HttpStatus.OK;
		List<Map<String, Object>> dataList = onlineProductService.getAllOnlineProducts(null);
		for (Map<String, Object> prodMap: dataList) {
			try {
				logger.info("** OnlineProductController >> deleteAllProduct >> DELETING.... " + prodMap.toString());
				Map<String, Object> resultMap = onlineProductService.deleteProduct(prodMap);
				if (MapUtils.isNotEmpty(resultMap)) {
					logger.info("** OnlineProductController >> deleteAllProduct >> DELETE Result:" + resultMap.toString());
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}
		
		logger.info("** OnlineProductController >> deleteAllProduct >> [END]");
		return new ResponseEntity<>(httpStat);
	} 
	
	
	
}
