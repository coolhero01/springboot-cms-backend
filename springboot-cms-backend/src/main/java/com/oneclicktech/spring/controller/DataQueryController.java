package com.oneclicktech.spring.controller;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.oneclicktech.spring.service.CronJobService;

//@CrossOrigin(origins = "*", maxAge = 3600)
@CrossOrigin(origins = "http://localhost:4200", maxAge = 3600, allowCredentials="true")
@RestController
@RequestMapping("/pc/dataquery")
public class DataQueryController {
	
	private static final Logger logger = Logger.getLogger("DataQueryController");
	
	@Autowired
	CronJobService cronJobService;  
	
	
	
	@PostConstruct
	public void init() {
		
	}
	 
	@PostMapping("/executeQuery")
	public ResponseEntity<Map<String, Object>> checkCronJobStatus(@RequestBody Map<String, Object> paramBody) {
		logger.info("** DataQueryController >> checkCronJobStatus >> [START]");
		
		HttpStatus httpStat = HttpStatus.OK;
		HashMap<String, Object> paramMap = new HashMap<>();
		paramMap.putAll(paramBody);
		Map<String, Object> resultMap = null;
		List<Map<String, Object>> dataList = cronJobService.getCronAuditLogs(paramMap);
		if (CollectionUtils.isNotEmpty(dataList)) {
			resultMap = new LinkedHashMap<String, Object>();
			resultMap.putAll(dataList.get(0));
  		}
		logger.info("** DataQueryController >> checkCronJobStatus >> [END]");
		return new ResponseEntity<Map<String, Object>>(resultMap,  httpStat);
	}
 
 
	
	@PostMapping("/getCronJobSchedList")
	public ResponseEntity<List<Map<String, Object>>> getCronJobSchedList(@RequestBody Map<String, Object> paramBody) {
		logger.info("** DataQueryController >> getCronJobSchedList >> [START]");
		
		HttpStatus httpStat = HttpStatus.OK;
		HashMap<String, Object> paramMap = new HashMap<>();
		paramMap.putAll(paramBody);
		Map<String, Object> resultMap = null;
		List<Map<String, Object>> dataList = cronJobService.getCronJobSchedList(paramMap);
		 
		logger.info("** DataQueryController >> getCronJobSchedList >> [END]");
		return new ResponseEntity<List<Map<String, Object>>>(dataList,  httpStat);
	}
 
 
	@PostMapping("/saveOnlineOrderToDB")
	public ResponseEntity<Map<String, Object>> saveOnlineOrderToDB(@RequestBody Map<String, Object> paramBody) {
		logger.info("** DataQueryController >> saveOnlineOrderToDB >> [START]");
		
		HttpStatus httpStat = HttpStatus.OK;
		
		cronJobService.saveOnlineOrderToDB(paramBody);
		
		logger.info("** DataQueryController >> saveOnlineOrderToDB >> [END]");
		return new ResponseEntity<Map<String, Object>>(paramBody,  httpStat);
	}
 
}
