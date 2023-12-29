package com.oneclicktech.spring.service.impl;

import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.http.auth.InvalidCredentialsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oneclicktech.spring.domain.Constants;
import com.oneclicktech.spring.mapper.AuditLogMapper;
import com.oneclicktech.spring.mapper.ClientTokenMapper;
import com.oneclicktech.spring.mapper.CronAuditLogMapper;
import com.oneclicktech.spring.mapper.CronJobSchedMapper;
import com.oneclicktech.spring.mapper.CustomerAddressMapper;
import com.oneclicktech.spring.mapper.CustomerMapper;
import com.oneclicktech.spring.mapper.ProductMasterMapper;
import com.oneclicktech.spring.mapper.ShopOrderMapper;
import com.oneclicktech.spring.service.AuditLogService;
import com.oneclicktech.spring.service.CronJobService;
import com.oneclicktech.spring.service.CustomerService;
import com.oneclicktech.spring.service.OnlineOrderService;
import com.oneclicktech.spring.service.OnlineProductService;
import com.oneclicktech.spring.service.ProductService;
import com.oneclicktech.spring.service.RestTemplateService;
import com.oneclicktech.spring.util.DateUtil;
import com.oneclicktech.spring.util.NumberUtil;
import com.oneclicktech.spring.util.ShopifyUtil;

@Service
public class AuditLogServiceImpl implements AuditLogService {

	private static final Logger logger = Logger.getLogger("AuditLogServiceImpl");
	
	private static final int AUDIT_MSG_CHAR_LIMIT = 3999;
	
	@Autowired
	CronAuditLogMapper cronAuditLogMapper;

	@Autowired
	AuditLogMapper auditLogMapper;

	@Autowired
	ClientTokenMapper clientTokenMapper;
	
	@Override
	public List<Map<String, Object>> getOrderAuditLogs(HashMap<String, Object> paramMap) {
		// TODO Auto-generated method stub
		return auditLogMapper.getOrderAuditLogs(paramMap);
	}

	@Override
	public int saveOrderAuditLog(String shopOrderNo, String salesOrderNo, String jsonRequest, String auditLogMsg) {
		 
		HashMap<String, Object> logMap = new HashMap<>();
		
		if (StringUtils.isNotBlank(jsonRequest) 
				&& jsonRequest.length() >= AUDIT_MSG_CHAR_LIMIT) {
			jsonRequest = jsonRequest.substring(0, AUDIT_MSG_CHAR_LIMIT-1);
		}
		
		if (StringUtils.isNotBlank(auditLogMsg) 
				&& auditLogMsg.length() >= AUDIT_MSG_CHAR_LIMIT) {
			auditLogMsg = auditLogMsg.substring(0, AUDIT_MSG_CHAR_LIMIT-1);
		}
		
		logMap.put("shopOrderNo", shopOrderNo);
		logMap.put("salesOrderNo", salesOrderNo);
		logMap.put("soJsonRequest", jsonRequest);
		logMap.put("auditLogMsg", auditLogMsg);  
		
		int result = 0;
		List<Map<String, Object>> auditLogs = auditLogMapper.getOrderAuditLogs(logMap);
		if (CollectionUtils.isEmpty(auditLogs)) {
			//INSERT
			result = auditLogMapper.insertOrderAuditLog(logMap);
		} else {
			//UPDATE
			result = auditLogMapper.updateOrderAuditLog(logMap);
		}
		
		return result;
 	}

	@Override
	public String getLatestToken(String tokenType) {

		HashMap<String, Object> searchMap = new HashMap<>();
		searchMap.put("tokenType", tokenType);

		List<Map<String, Object>> tokenMapList = clientTokenMapper.getTokenList(searchMap);
		if (CollectionUtils.isNotEmpty(tokenMapList)) {
			Map<String, Object> tokenMap = tokenMapList.get(0);
			String accessToken = StringUtils.trim((String) tokenMap.get("access_token"));
			logger.info("** AuditLogServiceImpl >> getLatestToken >> accessToken: " + accessToken);
			return accessToken; 
		}
		
		return null;
	}

	 

}
