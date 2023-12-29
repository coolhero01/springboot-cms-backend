package com.oneclicktech.spring.component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.oneclicktech.spring.service.CronJobService;
import com.oneclicktech.spring.service.EmailService;
import com.oneclicktech.spring.service.RestGenericService;

@EnableAsync
@Component
public class BDOScheduledTask {
	
	private static final Logger logger = Logger.getLogger("BDOScheduledTask"); 
	
	@Autowired
	CronJobService cronJobService;

	@Autowired
	EmailService emailService;
	 
	@Autowired
	RestGenericService restGenericService;
	
	@Async
	@Scheduled(cron = "${run.bdo.check-payment.cron-string}")
	public void runBDOCheckPaymentStatus() {
		boolean runCron = cronJobService.getCronEnableStatus("runBDOCheckPaymentStatus");
		if (runCron) {
			Date currentDate = new Date();
			logger.info("BDOScheduledTask >> runBDOCheckPaymentStatus >> Async [START]: " + currentDate);
			try {
				String requestUrl = new StringBuilder("https://cms.potatocorner.com")
						.append("/springboot-cms-backend/pc/bdo/checkPaymentStatusByToken").toString();
 
				restGenericService.sendPostRequest(requestUrl, new HashMap<>());
				
			} catch (Throwable t) {
				logger.log(Level.SEVERE, t.getMessage(), t);
			}
			logger.info("BDOScheduledTask >> runBDOCheckPaymentStatus >> Async [END]: " + currentDate);
		} 
	}
}
