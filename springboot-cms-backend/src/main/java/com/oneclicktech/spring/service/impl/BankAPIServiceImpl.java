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
import com.oneclicktech.spring.mapper.BankAPIMapper;
import com.oneclicktech.spring.mapper.ClientTokenMapper;
import com.oneclicktech.spring.mapper.CronAuditLogMapper;
import com.oneclicktech.spring.mapper.CronJobSchedMapper;
import com.oneclicktech.spring.mapper.CustomerAddressMapper;
import com.oneclicktech.spring.mapper.CustomerMapper;
import com.oneclicktech.spring.mapper.ProductMasterMapper;
import com.oneclicktech.spring.mapper.ShopOrderMapper;
import com.oneclicktech.spring.service.AuditLogService;
import com.oneclicktech.spring.service.BankAPIService;
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
public class BankAPIServiceImpl implements BankAPIService {

	private static final Logger logger = Logger.getLogger("BankAPIServiceImpl");
	
	
	@Autowired
	BankAPIMapper bankAPIMapper;
	 
	
	@Override
	public Map<String, Object> getBDOTransactLogByOrder(HashMap<String, Object> paramMap) {
		Map<String, Object> resultMap = bankAPIMapper.getOneBDOTransactLog(paramMap);
		return resultMap;
	}

	@Override
	public boolean saveBDOTransactLog(HashMap<String, Object> paramMap) {
		Map<String, Object> resultMap = bankAPIMapper.getOneBDOTransactLog(paramMap);
		int result = 0;
		if (MapUtils.isEmpty(resultMap)) {
			bankAPIMapper.insertBDOTransactLogs(paramMap);
		} else {
			bankAPIMapper.updateBDOTransactLogs(paramMap);
		}
		
		if (result!=0)
			return true;
		
		return false;
	}
	 
	

}
