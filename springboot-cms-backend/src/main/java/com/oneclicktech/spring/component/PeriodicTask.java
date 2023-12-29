package com.oneclicktech.spring.component;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.oneclicktech.spring.domain.Constants;
import com.oneclicktech.spring.service.CronJobService;
import com.oneclicktech.spring.service.EmailService;
import com.oneclicktech.spring.service.OnlineOrderService;
import com.oneclicktech.spring.service.OnlineProductService;
import com.oneclicktech.spring.service.OnlineShopService;
import com.oneclicktech.spring.util.DateUtil;
 
@Component
public class PeriodicTask {

	private static final Logger logger = Logger.getLogger("PeriodicTask"); 
	
//	@Value("${process.cronjob.enable}")
//	Boolean enableCronJob;

	@Autowired
	OnlineProductService onlineProductService;

	@Autowired
	OnlineOrderService onlineOrderService;

	@Autowired
	OnlineShopService onlineShopService;

	@Autowired
	CronJobService cronJobService;

	@Autowired
	EmailService emailService;

	@Scheduled(cron = "${five.cron-string}")
	public void everyFiveSeconds() {
		// System.out.println("Periodic task [5]: " + new Date());
	}

	@Scheduled(cron = "${order.process.salesorder.cron-string}")
	public void processOnlineOrderToSalesOrder() {

		boolean runCron = cronJobService.getCronEnableStatus("processOnlineOrderToSalesOrder");
		if (runCron) {
			Date currentDate = new Date();
			logger.info("[PeriodicTask] >> processShopOrderToSalesOrder >> [START]: " + currentDate);
			try {
				Map<String, Object> paramMap = new HashMap<>();
				// cronJobService.saveOnlineOrderToDB(paramMap);
				// onlineOrderService.syncOrderDbSOToOnline(paramMap);

			} catch (Exception e) {
				logger.info("[PeriodicTask] >> processShopOrderToSalesOrder >> ERROR:");
				logger.log(Level.SEVERE, e.getMessage(), e);
			} catch (Throwable t) {
				logger.info("[PeriodicTask] >> processShopOrderToSalesOrder >> ERROR:");
				logger.log(Level.SEVERE, t.getMessage(), t);
			}

			logger.info("[PeriodicTask] >> processShopOrderToSalesOrder >> [END]: " + currentDate);
		}

	}

	
	@Scheduled(cron = "${sync.online.auto-pay.orders.cron-string}")
	public void processAutoPayOrders() {
		boolean runCron = cronJobService.getCronEnableStatus("processAutoPayOrders");
		if (runCron) {
			Date currentDate = new Date();
			logger.info("[PeriodicTask] >> processAutoPayOrders >> [START]: " + currentDate);
			cronJobService.processOrdersWithAutoPay(new HashMap<>());
			logger.info("[PeriodicTask] >> processAutoPayOrders >> [END]: " + currentDate);
		}
	}

//	@Scheduled(cron = "${sync.order-db.tags.to-online.cron-string}")
//	public void syncOrderDBTagsToOnline() {
//
//		if (enableCronJob) {
//			Date currentDate = new Date();
//			logger.info("[PeriodicTask] >> syncOrderDBTagsToOnline >> [START]: " + currentDate);
//			Map<String, Object> paramMap = new HashMap<>();
//			onlineOrderService.syncOrderDbSOToOnline(paramMap);
//			logger.info("[PeriodicTask] >> syncOrderDBTagsToOnline >> [END]: " + currentDate);
//		}
//	}

	@Scheduled(cron = "${sync.d365.create-so.cron-string}")
	public void syncSalesOrderDataToDB() {
		boolean runCron = cronJobService.getCronEnableStatus("syncSalesOrderDataToDB");
		if (runCron) {
			Date currentDate = new Date();
			logger.info("[PeriodicTask] >> syncSalesOrderDataToDB >> [START]: " + currentDate);
			Map<String, Object> paramMap = new HashMap<>();
			cronJobService.syncSalesOrderDataToDB(paramMap);
			logger.info("[PeriodicTask] >> syncSalesOrderDataToDB >> [END]: " + currentDate);
		}
	}

	@Scheduled(cron = "${sync.d365.so-fulfill.cron-string}")
	public void syncSalesOrderFulfillByOrderDB() {
		boolean runCron = cronJobService.getCronEnableStatus("syncSalesOrderFulfillByOrderDB");
		if (runCron) {
			Date currentDate = new Date();
			logger.info("[PeriodicTask] >> syncSalesOrderFulfillByOrderDB >> [START]: " + currentDate);
			Map<String, Object> paramMap = new HashMap<>();
			cronJobService.syncSalesOrderFulfillByOrderDB(paramMap);
			logger.info("[PeriodicTask] >> syncSalesOrderFulfillByOrderDB >> [END]: " + currentDate);
		}
	}
	
	
	@Scheduled(cron = "${hook.reset-object.cron-string}")
	public void resetObjectHook() {
		try {
			logger.info("[PeriodicTask] >> resetObjectHook >> [START]");
			onlineOrderService.resetObjectHook("");
			emailService.sendEmail(null, Constants.MAIL_RECIPIENTS, "PC - Reset Hook Object", "PC - Reset Hook Object",
					null, null, null);
			logger.info("[PeriodicTask] >> resetObjectHook >> [END]");
		} catch (Throwable t) {
			logger.log(Level.SEVERE, t.getMessage(), t);
		}
	}

	/*
	 * SAVE ONLINE ORDERS info everyday by UPDATED_AT_MIN to always GET the updated
	 * info
	 */
	@Scheduled(cron = "${sync.online.save-order.to-db.cron-string}")
	public void saveOnlineOrderToDB() {
		boolean runCron = cronJobService.getCronEnableStatus("saveOnlineOrderToDB");
		if (runCron) {
			Date currentDate = new Date();
			logger.info("[PeriodicTask] >> saveOnlineOrderToDB >> [START]: " + currentDate);
			Map<String, Object> paramMap = new HashMap<>();
			Date prevDate = DateUtil.getDateNowPlusTime(Calendar.DAY_OF_YEAR, -1);
			String updatedAtMin = DateUtil.getISODateFromTimeFormat(prevDate);
			paramMap.put("updated_at_min", updatedAtMin);
			paramMap.put("status", "open");// Show only open orders.
			paramMap.put("limit", "250");
			if (Constants.TEST_ONLY) {
				paramMap.remove("updated_at_min");
			}
			/*
			 * SAVE ONLINE ORDERS info everyday by UPDATED_AT_MIN to always GET the updated
			 * info
			 */
			try {
				Map<String, Object> resultMap = cronJobService.saveOnlineOrderToDB(paramMap);
			} catch (Exception e) {
				logger.log(Level.SEVERE, e.getMessage(), e);
			}
			

			logger.info("[PeriodicTask] >> saveOnlineOrderToDB >> [END]: " + currentDate);
		}

	}
	
	
	@Scheduled(cron = "${sync.pc.acumatica.po.cron-string}")
	public void syncProcessPurchaseOrder() {
		boolean runCron = cronJobService.getCronEnableStatus("syncProcessPurchaseOrder");
		if (runCron) {
	 		try {
				cronJobService.processPurchaseOrder(new HashMap<>());
			} catch (Exception e) {
				logger.log(Level.SEVERE, e.getMessage(), e);
			}
		}
	}

	@Scheduled(cron = "${sync.after-office.task.cron-string}")
	public void syncAllAfterOfficeTask() {
		boolean runCron = cronJobService.getCronEnableStatus("syncAllAfterOfficeTask");
		if (runCron) {
			Date currentDate = new Date();
			logger.info("[PeriodicTask] >> syncAllAfterOfficeProcess >> [START]: " + currentDate);

			StringBuilder msgDetails = new StringBuilder(
					"The After Office CRON Task/s is already RUNNING.... \r\n \r\n ");
			msgDetails.append("Jobs Running : [STARTED: ").append(currentDate).append(" ]");
			msgDetails.append("*** syncProductDataToDB - SYNC D365 Product data to Local DB to Online Store  \r\n")
					.append("*** syncCustomerDataToDB - SYNC D365 Customer data to Local DB to Online Store  \r\n")
					.append("*** syncWarehouseDataToDB - SYNC D365 Warehouse data to Local DB   \r\n")
					.append("*** syncProductInventory - SYNC All Product Inventory \r\n");

			boolean mailSent = emailService.sendEmail(null, Constants.MAIL_RECIPIENTS,
					"PC - After office hours CRON Task [STARTED]", msgDetails.toString(), null, null, null);
			// SYNC All Orders with PAID status
			// ********************************************
//			try {
//				Map<String, Object> paramMap = new HashMap<>();
//				paramMap.put("limit", 250);
//				onlineOrderService.checkAllOrdersBySO(paramMap);
//			} catch (Throwable t) {
//				logger.log(Level.SEVERE, t.getMessage(), t);
//			}

			// SYNC ALL D365 Static Data
			// ********************************************
			try {
				cronJobService.syncD365StaticDataToDB(null);
			} catch (Throwable t) {
				logger.log(Level.SEVERE, t.getMessage(), t);
			}

			// SYNC Product Inventory
			// ********************************************
			try {
				onlineProductService.syncProductInventory(new HashMap<>());
			} catch (Throwable t) {
				logger.log(Level.SEVERE, t.getMessage(), t);
			}

			msgDetails = new StringBuilder("The After Office CRON Task/s is already COMPLETED.... \r\n \r\n ");
			msgDetails.append("Jobs Running : [COMPLETED: ").append(currentDate).append(" ]");
			mailSent = emailService.sendEmail(null, Constants.MAIL_RECIPIENTS,
					"PC - After office hours CRON Task [COMPLETED]", msgDetails.toString(), null, null, null);

			logger.info("[PeriodicTask] >> syncAllAfterOfficeProcess >> [END]: " + currentDate);
		}
	}

//	
//	@Scheduled(cron = "${run.bdo.check-payment.cron-string}")
//	public void runBDOCheckPaymentStatus() {
//		boolean runCron = cronJobService.getCronEnableStatus("runBDOCheckPaymentStatus");
//		if (runCron) {
//			Date currentDate = new Date();
//			logger.info("[PeriodicTask] >> runBDOCheckPaymentStatus >> Async [START]: " + currentDate);
//			try {
//				cronJobService.runBDOCheckPaymenStatusRequest(new HashMap<>());
//			} catch (Throwable t) {
//				logger.log(Level.SEVERE, t.getMessage(), t);
//			}
//			logger.info("[PeriodicTask] >> runBDOCheckPaymentStatus >> Async [END]: " + currentDate);
//		}
//	}

	@Scheduled(cron = "${run.clean-backup.tables.cron-string}")
	public void runCleanBackupTables() {
		boolean runCron = cronJobService.getCronEnableStatus("runCleanBackupTables");
		if (runCron) {
			Date currentDate = new Date();
			logger.info("[PeriodicTask] >> runCleanBackupTables >> [START]: " + currentDate);
			try {
				onlineShopService.cleanBackupTables(new HashMap<>());
			} catch (Throwable t) {
				logger.log(Level.SEVERE, t.getMessage(), t);
			}
			logger.info("[PeriodicTask] >> runCleanBackupTables >> [END]: " + currentDate);
		}

	}

}