package com.oneclicktech.spring.service.impl;

import java.io.File;
import java.io.FileReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
import com.oneclicktech.spring.mapper.IssuanceMapper;
import com.oneclicktech.spring.mapper.ProductMasterMapper;
import com.oneclicktech.spring.mapper.ShopOrderMapper;
import com.oneclicktech.spring.service.AuditLogService;
import com.oneclicktech.spring.service.BankAPIService;
import com.oneclicktech.spring.service.CronJobService;
import com.oneclicktech.spring.service.CustomerService;
import com.oneclicktech.spring.service.IssuanceService;
import com.oneclicktech.spring.service.OnlineOrderService;
import com.oneclicktech.spring.service.OnlineProductService;
import com.oneclicktech.spring.service.ProductService;
import com.oneclicktech.spring.service.RestTemplateService;
import com.oneclicktech.spring.util.DateUtil;
import com.oneclicktech.spring.util.NumberUtil;
import com.oneclicktech.spring.util.PCDataUtil;
import com.oneclicktech.spring.util.ShopifyUtil;

@Service
public class IssuanceServiceImpl implements IssuanceService {

	private static final Logger logger = Logger.getLogger("IssuanceServiceImpl");

	@Autowired
	IssuanceMapper issuanceMapper;

	@Autowired
	OnlineOrderService onlineOrderService;

	@Override
	public Map<String, Object> getStaggeredPaymentDetails(String orderName) {
		HashMap<String, Object> paramMap = new HashMap<>();
		paramMap.put("orderName", orderName);
		Map<String, Object> schedMap = issuanceMapper.getNextPaymentSched(paramMap);
		return schedMap;
	}

	@Override
	public boolean isStaggeredPaymentOrder(String orderName) {
		HashMap<String, Object> paramMap = new HashMap<>();
		paramMap.put("orderName", orderName);
		List<Map<String, Object>> issuanceList = issuanceMapper.getPaymentScheduleList(paramMap);
		if (CollectionUtils.isNotEmpty(issuanceList)) {
			return true;
		}
		return false;
	} 

	@Override
	public Map<String, Object> getNextPaymentSched(String orderName) {
		HashMap<String, Object> paramMap = new HashMap<>();
		paramMap.put("orderName", orderName);
		paramMap.put("payStatus", "pending");
		Map<String, Object> schedMap = issuanceMapper.getNextPaymentSched(paramMap);
		return schedMap;
	}

	@Override
	public boolean checkStaggeredPaymentTransactionExist(long orderId, Map<String, Object> paymentSchedMap) {
		try {
		 	LocalDateTime nextPayDateTime = (LocalDateTime) paymentSchedMap.get("nextPayDate");
			Date schedDate = DateUtil.convertToDateViaSqlTimestamp(nextPayDateTime);
			double compareAmount = Double.parseDouble(String.valueOf(paymentSchedMap.get("amountToPay")));
			List<Map<String, Object>> payTrxList = onlineOrderService.getOrderTransactions(orderId);
			for (Map<String, Object> payTrx : payTrxList) {
				String payKind = (String) payTrx.get("kind");
				String payStatus = (String) payTrx.get("status");
			  	Date datePaid = DateUtil.getDateInISO_OFFSET(String.valueOf(payTrx.get("created_at")));
				double payAmount = Double.parseDouble((String) payTrx.get("amount"));
				if (payKind.equals("sale") && payStatus.equals("success") ) {
					
					boolean isWithinPayAmount = PCDataUtil.isWithinAmountRange(payAmount, compareAmount, 1.0);
					boolean isWithinPayDate = PCDataUtil.isWithinPaymentSchedule(datePaid, schedDate, 27);
					logger.info("*** IssuanceServiceImpl >> checkStaggeredPaymentTransactionExist >> isWithinPayAmount: "
							+ isWithinPayAmount);
					logger.info("*** IssuanceServiceImpl >> checkStaggeredPaymentTransactionExist >> isWithinPayDate: "
							+ isWithinPayDate);
				 	//NOW Check the Date to Accurately match the payment schedule
					if (isWithinPayAmount && isWithinPayDate) {
						return true;
					}
			 	}
			}

		} catch (Throwable t) {
			logger.log(Level.SEVERE, t.getMessage(), t);
		}
		return false;
	}

}
