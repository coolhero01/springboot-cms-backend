package com.oneclicktech.spring.controller;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.oneclicktech.spring.domain.Constants;
import com.oneclicktech.spring.mapper.ClientTokenMapper;
import com.oneclicktech.spring.mapper.ShopOrderMapper;
import com.oneclicktech.spring.mapper.TableUtilMapper;
import com.oneclicktech.spring.service.AcumaticaService;
import com.oneclicktech.spring.service.CronJobService;
import com.oneclicktech.spring.service.EmailService;
import com.oneclicktech.spring.service.OnlineCustomerService;
import com.oneclicktech.spring.service.OnlineOrderService;
import com.oneclicktech.spring.service.OnlineProductService;
import com.oneclicktech.spring.service.OnlineShopService;
import com.oneclicktech.spring.service.ProcessCurlService;
import com.oneclicktech.spring.service.QueryBuilderService;
import com.oneclicktech.spring.service.RestD365Service;
import com.oneclicktech.spring.service.RestGenericService;
import com.oneclicktech.spring.service.RestTemplateService;
import com.oneclicktech.spring.service.SyncD365Service;
import com.oneclicktech.spring.util.DateUtil;
import com.oneclicktech.spring.util.NumberUtil;
import com.oneclicktech.spring.util.PCDataUtil;
import com.oneclicktech.spring.util.ShopifyUtil;

//CrossOrigin(origins = "http://localhost:4200", maxAge = 3600, allowCredentials="true")
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/pc/online-shop")
public class OnlineShopController {

	private static final Logger logger = Logger.getLogger("OnlineShopController");

	@Autowired
	RestTemplateService restTemplateService;

	@Autowired
	ProcessCurlService processCurlService;

	@Autowired
	RestGenericService restGenericService;

	@Autowired
	AcumaticaService acumaticaService;

	@Autowired
	QueryBuilderService queryBuilderService;

	@Autowired
	OnlineProductService onlineProductService;

	@Autowired
	OnlineShopService onlineShopService;

	@Autowired
	OnlineOrderService onlineOrderService;

	@Autowired
	OnlineCustomerService onlineCustomerService;

	@Autowired
	EmailService emailService;

	@Autowired
	TableUtilMapper tableUtilMapper;

	@Autowired
	ShopOrderMapper shopOrderMapper;

	@Autowired
	RestD365Service restD365Service;

	@Value("${pc.shopify.app.webhook.version}")
	String apiVersion;

	@Value("${pc.shopify.app.rowlimit}")
	String rowLimit;

	@Value("${spavi.d365.default.data-area-id}")
	String defaultDataAreaId;

	@Value("${spavi.d365.api.host-url}")
	String apiSpaviHostUrl;

	@Autowired
	SyncD365Service syncD365Service;

	@Autowired
	CronJobService cronJobService;

	@Autowired
	ClientTokenMapper clientTokenMapper;

	@PostConstruct
	public void init() {

	}

	@GetMapping("/cancelOrderById")
	public RedirectView cancelOrderById(@RequestParam("orderId") String orderId, HttpServletRequest request)
			throws Throwable {

		String redirectUrl = "https://order.potatocorner.com/pages/cancel-order-failed";
		logger.info("** OnlineShopController >> cancelOrderById >> [START]");
		HttpStatus httpStat = HttpStatus.OK;
		logger.info("** OnlineShopController >> orderId: " + orderId);
		String siteReferrer = request.getHeader("referer");
		logger.info("** OnlineShopController >> siteReferrer: " + siteReferrer);

		Map<String, Object> resultMap = onlineOrderService.deleteOrder(orderId);
		logger.info("** OnlineShopController >> resultMap: " + resultMap);
		if (resultMap != null && resultMap.containsKey(Constants.RESULT_HTTP_STATUS_CODE)) {
			HttpStatus httpStats = (HttpStatus) resultMap.get(Constants.RESULT_HTTP_STATUS_CODE);
			if (httpStats.equals(HttpStatus.OK)) {
				redirectUrl = "https://order.potatocorner.com/pages/cancel-order-success-page";
				logger.info("** OnlineShopController >> redirectUrl: " + redirectUrl);
			}
		}

		logger.info("** OnlineShopController >> cancelOrderById >> [END]");
		return new RedirectView(redirectUrl);
	}

	@GetMapping("/viewSalesOrder")
	public RedirectView viewSalesOrder(@RequestParam("salesOrderNo") String salesOrderNo) throws Throwable {
		logger.info("** OnlineShopController >> viewSalesOrder >> [START]");
		String redirectUrl = "https://order.potatocorner.com/pages/cancel-order-failed";
		Map<String, Object> soMap = syncD365Service.viewSalesOrderDetail(salesOrderNo);
		logger.info("** OnlineShopController >> viewSalesOrder >> soMap: " + soMap);
		logger.info("** OnlineShopController >> viewSalesOrder >> [END]");
		return new RedirectView(redirectUrl);
	}

	@GetMapping("/processReOrder")
	public RedirectView processReOrder(@RequestParam("orderId") String orderId) throws Throwable {
		logger.info("** OnlineShopController >> processReOrder >> [START]");
		String redirectUrl = "https://order.potatocorner.com/pages/cancel-order-failed";

		logger.info("** OnlineShopController >> processReOrder >> [END]");
		return null;
	}

	@PostMapping("/getShopProductList")
	public ResponseEntity<List<Map<String, Object>>> getProductList(@RequestBody Map<String, Object> paramBody)
			throws Throwable {
		logger.info("** OnlineShopController >> getProductList >> [START]");
		HttpStatus httpStat = HttpStatus.OK;
		HashMap<String, Object> paramMap = new HashMap<>();
		paramMap.putAll(paramBody);
		List<Map<String, Object>> dataList = onlineProductService.getProductList(paramMap);
		logger.info("** OnlineShopController >> getProductList >> [END]");
		return new ResponseEntity<List<Map<String, Object>>>(dataList, httpStat);
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
			// boolean success = onlineShopService.syncLocalCustomerToOnline(paramBody);

			/*
			 * Process All Customers WITHOUT Customer ID WITH EMail WITH Phone Number
			 */
			// paramBody.put("emptyShopId", "true");
			paramBody.put("withEmail", "true");
			// paramBody.put("withPhone", "true");
			onlineShopService.syncLocalCustomerToOnlineByEmail(paramBody);

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
		logger.info("** OnlineOrderController >> deleteAllOrders >> [START]");
		HttpStatus httpStat = HttpStatus.OK;
		List<Map<String, Object>> orderList = onlineOrderService.getOrderList(paramMap);
		for (Map<String, Object> orderMap : orderList) {
			String orderName = (String) orderMap.get("name");
			String orderId = String.valueOf(NumberUtil.getLongValue(orderMap, "orderId"));

			logger.info("** OnlineOrderController >> deleteAllOrders >> DELETING..... " + orderName);

			try {
				Map<String, Object> cancelResult = onlineOrderService.cancelOrder(orderId);
				logger.info("** OnlineOrderController >> deleteAllOrders >> cancelResult: " + cancelResult);

			} catch (Throwable e) {
				logger.log(Level.SEVERE, e.getMessage(), e);
			}

			try {
				Map<String, Object> deleteResult = onlineOrderService.deleteOrder(orderId);
				if (MapUtils.isNotEmpty(deleteResult)) {

				}
				logger.info("** OnlineOrderController >> deleteAllOrders >> deleteResult: " + deleteResult);
			} catch (Throwable e) {
				logger.log(Level.SEVERE, e.getMessage(), e);
			}

		}
		logger.info("** OnlineOrderController >> deleteAllOrders >> [END]");
		return new ResponseEntity<>(httpStat);
	}

	@PostMapping("/deleteAllProduct")
	public ResponseEntity<HttpStatus> deleteAllProduct(@RequestBody Map<String, Object> paramBody) throws Throwable {
		logger.info("** OnlineShopController >> deleteAllProduct >> [START]");
		HttpStatus httpStat = HttpStatus.OK;
		List<Map<String, Object>> dataList = onlineProductService.getAllOnlineProducts(null);
		for (Map<String, Object> prodMap : dataList) {
			try {
				logger.info("** OnlineShopController >> deleteAllProduct >> DELETING.... " + prodMap.toString());
				Map<String, Object> resultMap = onlineProductService.deleteProduct(prodMap);
				if (MapUtils.isNotEmpty(resultMap)) {
					logger.info("** OnlineShopController >> deleteAllProduct >> DELETE Result:" + resultMap.toString());
				}
			} catch (Exception e) {
				logger.log(Level.SEVERE, e.getMessage(), e);
			}

		}

		logger.info("** OnlineShopController >> deleteAllProduct >> [END]");
		return new ResponseEntity<>(httpStat);
	}

	@PostMapping("/syncAlleProductsById")
	public ResponseEntity<HttpStatus> syncAlleProductsById(@RequestBody Map<String, Object> paramBody)
			throws Throwable {
		logger.info("** OnlineShopController >> syncAlleProductsById >> [START]");
		HttpStatus httpStat = HttpStatus.OK;
		onlineShopService.syncAlleProductsById(new HashMap<>());
		logger.info("** OnlineShopController >> syncAlleProductsById >> [END]");
		return new ResponseEntity<>(httpStat);
	}

	@PostMapping("/deleteAllCustomer")
	public ResponseEntity<HttpStatus> deleteAllCustomer(@RequestBody Map<String, Object> paramBody) throws Throwable {
		logger.info("** OnlineShopController >> deleteAllCustomer >> [START]");
		HttpStatus httpStat = HttpStatus.OK;
		List<Map<String, Object>> dataList = onlineCustomerService.getAllOnlineCustomers(null);
		for (Map<String, Object> custMap : dataList) {
			try {
				logger.info("** OnlineShopController >> deleteAllCustomer >> DELETING.... " + custMap.toString());
				String customerId = String.valueOf(custMap.get("customerId"));
				Map<String, Object> resultMap = onlineCustomerService.deleteCustomerById(customerId);
				if (MapUtils.isNotEmpty(resultMap)) {
					logger.info(
							"** OnlineShopController >> deleteAllCustomer >> DELETE Result:" + resultMap.toString());
				}
			} catch (Exception e) {
				logger.log(Level.SEVERE, e.getMessage(), e);
			}

		}

		logger.info("** OnlineShopController >> deleteAllCustomer >> [END]");
		return new ResponseEntity<>(httpStat);
	}

	@PostMapping("/syncD365CustomerToLocalDB")
	public ResponseEntity<HttpStatus> syncD365CustomerToLocalDB(@RequestBody Map<String, Object> paramBody)
			throws Throwable {
		HttpStatus httpStat = HttpStatus.OK;
		HashMap<String, Object> searchMap = new HashMap<>();
		searchMap.put("tokenType", "D365");

		logger.info("*** OnlineShopController >> syncD365CustomerToLocalDB >>  [START] ");
		try {
			String customerToken = restD365Service.getLatestD365Token();
			Map<String, Object> custMap = new HashMap<>();
			custMap.put("accessToken", customerToken);
			syncD365Service.syncCustomerDataToDB(custMap);
		} catch (Throwable t) {
			logger.info("*** OnlineShopController >> syncD365CustomerToLocalDB >>  [ERROR] ");

			logger.log(Level.SEVERE, t.getMessage(), t);
		}

		logger.info("*** OnlineShopController >> syncD365CustomerToLocalDB >>  [END] ");
		return new ResponseEntity<>(httpStat);
	}

	@PostMapping("/syncD365ProductDataToDB")
	public ResponseEntity<HttpStatus> syncD365ProductDataToDB(@RequestBody Map<String, Object> paramBody)
			throws Throwable {
		HttpStatus httpStat = HttpStatus.OK;
		HashMap<String, Object> searchMap = new HashMap<>();
		searchMap.put("tokenType", "D365");

		logger.info("*** OnlineShopController >> syncD365ProductDataToDB >>  [START] ");
		try {
			String customerToken = restD365Service.getLatestD365Token();
			Map<String, Object> custMap = new HashMap<>();
			custMap.put("accessToken", customerToken);
			syncD365Service.syncProductDataToDB(custMap);
		} catch (Throwable t) {
			logger.info("*** OnlineShopController >> syncD365ProductDataToDB >>  [ERROR] ");
			logger.log(Level.SEVERE, t.getMessage(), t);
		}

		logger.info("*** OnlineShopController >> syncD365ProductDataToDB >>  [END] ");
		return new ResponseEntity<>(httpStat);
	}

	@PostMapping("/syncSalesOrderDataToDB")
	public ResponseEntity<Map<String, Object>> syncSalesOrderDataToDB(@RequestBody Map<String, Object> paramBody)
			throws Throwable {

		HashMap<String, Object> searchMap = new HashMap<>();
		searchMap.put("tokenType", "D365");
		String customerToken = restD365Service.getLatestD365Token();
		Map<String, Object> custMap = new HashMap<>();
		custMap.put("accessToken", customerToken);
		custMap.putAll(paramBody);
		logger.info("** OnlineShopController >> syncSalesOrderDataToDB >> [START]");
		HttpStatus httpStat = HttpStatus.OK;
		Map<String, Object> syncResult = syncD365Service.syncSalesOrderDataToDB(custMap);
		logger.info("** OnlineShopController >> syncSalesOrderDataToDB >> [END]");
		return new ResponseEntity<Map<String, Object>>(syncResult, httpStat);
	}

	@PostMapping("/syncPurchaseOrder")
	public ResponseEntity<Map<String, Object>> syncPurchaseOrder(@RequestBody Map<String, Object> paramBody)
			throws Throwable {

		logger.info("** OnlineShopController >> syncPurchaseOrder >> [START]");
		HttpStatus httpStat = HttpStatus.OK;
		Map<String, Object> resultMap = syncD365Service.processPurchaseOrder(paramBody);
		logger.info("** OnlineShopController >> syncPurchaseOrder >> [END]");
		return new ResponseEntity<Map<String, Object>>(resultMap, httpStat);
	}

	@PostMapping("/executeDataQuery")
	public ResponseEntity<Map<String, Object>> executeDataQuery(@RequestBody Map<String, Object> paramBody)
			throws Throwable {

		logger.info("** OnlineShopController >> executeDataQuery >> [START]");
		HttpStatus httpStat = HttpStatus.OK;
		String queryTxt = StringUtils.trimToEmpty((String) paramBody.get("queryTxt"));
		logger.info("** OnlineShopController >> queryTxt: " + queryTxt);
		Map<String, Object> resultMap = new HashMap<>();
		if (StringUtils.isNotBlank(queryTxt)) {

			resultMap.put("txtQuery", queryTxt);
			String lowQueryTxt = StringUtils.lowerCase(queryTxt);
			if (lowQueryTxt.startsWith("select") || lowQueryTxt.startsWith("desc") || lowQueryTxt.startsWith("show")) {
				List<Map<String, Object>> dataList = queryBuilderService.getExecQuery(queryTxt);
				resultMap.put("result", dataList);
			} else {
				boolean success = queryBuilderService.execQuery(queryTxt);
				resultMap.put("result", success);
			}
		}

		logger.info("** OnlineShopController >> executeDataQuery >> [END]");
		return new ResponseEntity<Map<String, Object>>(resultMap, httpStat);
	}

	@PostMapping("/cleanSoDuplicates")
	public ResponseEntity<HttpStatus> cleanSoDuplicates(@RequestBody Map<String, Object> paramBody) throws Throwable {

		logger.info("** OnlineShopController >> cleanSoDuplicates >> [START]");
		HttpStatus httpStat = HttpStatus.OK;
		Date fromDate = DateUtil.getDateNowPlusTime(Calendar.DAY_OF_YEAR, -2);
		Date toDate = new Date();
		emailService.readGMail("PC - SO Creation for Order: POR", fromDate, toDate);

		logger.info("** OnlineShopController >> cleanSoDuplicates >> [END]");
		return new ResponseEntity<HttpStatus>(httpStat);
	}

	@GetMapping("/viewD365SalesOrder")
	public ModelAndView viewD365SalesOrder(@RequestParam("salesOrderNo") String salesOrderNo) {
		logger.info("** OnlineShopController >> viewD365SalesOrder >> [START]");
		logger.info("** OnlineShopController >> salesOrderNo: " + salesOrderNo);
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
		ModelAndView modelAndView = new ModelAndView();
		modelAndView.setViewName("view-so");
		try {
			Map<String, Object> resultMap = syncD365Service.viewSalesOrderDetail(salesOrderNo);
			if (MapUtils.isNotEmpty(resultMap)) {
				logger.info("** OnlineShopController >> resultMap: " + resultMap);
				List<Map<String, Object>> dataList = (List<Map<String, Object>>) resultMap.get(Constants.DATA_LIST);
				for (Map<String, Object> soMap : dataList) {
					Date delivDate = DateUtil.stringToDate((String) soMap.get("DeliveryDate"), "yyyy-MM-dd'T'HH:mm:ss");

					soMap.put("deliveryDate", sdf.format(delivDate));
					modelAndView.addObject("so", soMap);
				}

			}
		} catch (Throwable t) {
			logger.log(Level.SEVERE, t.getMessage(), t);
		}

		logger.info("** OnlineShopController >> viewD365SalesOrder >> [END]");
		return modelAndView;
	}

	@PostMapping("/saveOnlineOrderToDB")
	public void saveOnlineOrderToDB(Map<String, Object> paramMap) {
		try {
			cronJobService.saveOnlineOrderToDB(paramMap);
		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	@PostMapping("/execJobOnline")
	public void execJobOnline() {
		try {

//			String localQuery = "SELECT * FROM cms_db.config_fries_switch";
//			List<Map<String, Object>> localList = queryBuilderService.getExecQuery(localQuery);
//			for (Map<String, Object> localMap : localList) {
//				String whCode = StringUtils.upperCase(StringUtils.trimToEmpty((String) localMap.get("warehouse_code")));
//				String apiUrl = "https://pc-cms.uat.shakeys.solutions/springboot-cms-backend/pc/online-shop/executeDataQuery";
//				String eInsertTxt = "insert into cms_db.config_fries_switch (warehouse_code) values ('" + whCode
//						+ "') ";
//				queryBuilderService.onlineExecQuery(apiUrl, eInsertTxt);
//			}

			String apiUrl = "https://cms.potatocorner.com//springboot-cms-backend/pc/online-shop/executeDataQuery";
			String eQuery = "SELECT so.* from cms_db.shop_order so    WHERE  so.db_create_date BETWEEN DATE_SUB(now(), INTERVAL 1 DAY) and DATE_ADD(now(), INTERVAL 1 DAY) ";
			List<Map<String, Object>> eOrderList = queryBuilderService.getOnlineExecQuery(apiUrl, eQuery);
			for (Map<String, Object> eOrderMap : eOrderList) {
				String orderName = (String) eOrderMap.get("order_name");
				String dbCustomerNo = (String) eOrderMap.get("so_customer_no");
				String eItemQuery = " select item_id FROM cms_db.store_auto_issuance where customer_no = '"
						+ dbCustomerNo + "' " + "	and order_name is null";
				List<Map<String, Object>> eItemList = queryBuilderService.getOnlineExecQuery(apiUrl, eItemQuery);
//				String itemIds = PCDataUtil.getAllItemIdsByList(eItemList, "item_id");

				String issuanceQuery = "SELECT  sai.promo_code as 'promoCode'   "
						+ "	 , sai.customer_no as 'customerNo'   " + "	 , sai.warehouse_code as  'warehouse'    "
						+ "	 , max(so.db_create_date) as 'creationDate'   "
						+ "     , count(sai.item_id ) as 'itemCount'   " + " FROM cms_db.shop_order_line sol   "
						+ " JOIN cms_db.store_auto_issuance sai on sol.so_item_no = sai.item_id   "
						+ "  and sol.quantity = sai.qty_issuance   "
						+ " JOIN cms_db.shop_order so on sol.order_name = so.order_name   "
						+ "	and so.so_customer_no = sai.customer_no    " + " WHERE sol.order_name = '" + orderName
						+ "'	   " + "  and sai.oos_enabled = 'Y'    " + "  and sai.issued_flag = 'N'    "
						+ " GROUP BY  sai.promo_code , sai.customer_no , sai.warehouse_code ";

				List<Map<String, Object>> issuanceList = queryBuilderService.getOnlineExecQuery(apiUrl, issuanceQuery);
				if (CollectionUtils.isNotEmpty(issuanceList)) {
					Map<String, Object> issuanceMap = issuanceList.get(0);
					double promoCode = (Double) issuanceMap.get("promoCode");
					int iPromoCode = (int) promoCode;
					String customerNo = (String) issuanceMap.get("customerNo");

					logger.info("orderName: " + orderName);
					logger.info("issuanceList: " + issuanceList.size());
					double itemCount = (Double) issuanceMap.get("itemCount");
					logger.info("itemCount: " + itemCount);
					logger.info("eItemList: " + eItemList.size());
					if (eItemList.size() == itemCount) {
						logger.info("*** MATCH!!!! *** ");

						// LocalDateTime issuedDateTime = (LocalDateTime)
						// issuanceMap.get("creationDate");
						// Date issuedDate = DateUtil.convertToDateViaSqlTimestamp(issuedDateTime);
						String updateTxt = "UPDATE cms_db.store_auto_issuance set order_name = '" + orderName + "' "
								+ ", issued_date = now() " + ", issued_flag ='Y' " + " WHERE promo_code = '"
								+ iPromoCode + "' " + "   and customer_no = '" + customerNo + "'";

						boolean tagAsIssued = queryBuilderService.onlineExecQuery(apiUrl, updateTxt);
						logger.info("***  ecJobOnline >> tagAsIssued: " + tagAsIssued);
						// Initiate RE-Publishing of Promo and REMOVE Meta
						if (tagAsIssued) {
							try {
								Map<String, Object> rootMap = new LinkedHashMap<String, Object>();
								Map<String, Object> detailMap = new LinkedHashMap<String, Object>();

								detailMap.put("namespace", "product_auto_issuance");
								detailMap.put("key", customerNo);
								detailMap.put("value", "DELETED");
								detailMap.put("type", "single_line_text_field");
								rootMap.put("metafield", detailMap);
								rootMap.put("metaType", "SHOP");

								logger.info(
										"*** ShopWebhookController >> processAutoNBDIssuance >> rootMap: " + rootMap);

								onlineShopService.updateShopMetafield(rootMap);

							} catch (Throwable e) {
								logger.log(Level.SEVERE, e.getMessage(), e);
							}

//							try {
//								StringBuilder ewtTags = new StringBuilder("NPD_ORDER");
//								onlineOrderService.addOrderTagToCurrent(eOrderMap, ewtTags.toString());
//							} catch (Throwable e) {
//								logger.log(Level.SEVERE, e.getMessage(), e);
//							}
						}
					}
				}

			}

		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	@PostMapping("/rebuildStagData")
	public void rebuildStagData() {
		logger.info("**** rebuildStagData >> [START]");
		String apiUrl = "https://cms.potatocorner.com/springboot-cms-backend/pc/online-shop/executeDataQuery";
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");

//		try {
//
//			queryBuilderService.onlineExecQuery(apiUrl, "DELETE FROM cms_db.staggered_payment_mst; ");
//			queryBuilderService.onlineExecQuery(apiUrl, "DELETE FROM cms_db.staggered_payment_sched; ");
//
//			List<String> insertLines = FileUtils.readLines(new File("/temp/SPAVI/temp_insert_stagtxt2.txt"));
//			int ctr = 1;
//			for (String insLine : insertLines) {
//				try {
//					boolean inserted = queryBuilderService.onlineExecQuery(apiUrl, insLine);
//					if (inserted) {
//						logger.info("**** INSERTED [" + ctr + "] !!! *** " + insLine);
//					}
//
//					ctr++;
//				} catch (Throwable ee) {
//					logger.log(Level.SEVERE, ee.getMessage(), ee);
//				}
//				
//			}
//			 
//		} catch (Throwable e) {
//			logger.log(Level.SEVERE, e.getMessage(), e);
//		}

		try {
			// UPDATE item_variant_id, item_prod_id, item_qty,
			String eQuery = "select * from cms_db.staggered_payment_mst ";

			List<Map<String, Object>> staggerList = queryBuilderService.getOnlineExecQuery(apiUrl, eQuery);
			for (Map<String, Object> stagMap : staggerList) {

				String customerNo = (String) stagMap.get("customer_no");
				String itemNo = (String) stagMap.get("item_no");

				String wareQuery = "select customer_number, "
						+ "ifnull(oos_warehouse_1, oos_warehouse_2) as warehouse from cms_db.customer  "
						+ "where customer_number = '" + customerNo + "' limit 1;";
				List<Map<String, Object>> warehouseList = queryBuilderService.getOnlineExecQuery(apiUrl, wareQuery);
				if (CollectionUtils.isNotEmpty(warehouseList)) {
					Map<String, Object> wareMap = warehouseList.get(0);
					String warehouse = (String) wareMap.get("warehouse");
					if (StringUtils.isNotBlank(warehouse) && warehouse.equals("JTCRAYCLD")) {
						warehouse = "JTCONGCOM";
					}

					logger.info("*** rebuildStagData >> customerNo: " + customerNo);
					logger.info("*** rebuildStagData >> itemNo: " + itemNo);
					logger.info("*** rebuildStagData >> warehouse: " + warehouse);
					if (StringUtils.isNotBlank(warehouse)) {
						String prodQuery = "SELECT * FROM cms_db.product_inventory " + "where item_number = '"
								+ itemNo + "'  " + "and warehouse = '" + warehouse + "' limit 1";
						List<Map<String, Object>> prodList = queryBuilderService.getOnlineExecQuery(apiUrl, prodQuery);
						if (CollectionUtils.isNotEmpty(prodList)) {
							Map<String, Object> prodMap = prodList.get(0);
							long eProdId = NumberUtil.getLongValue(prodMap, "shop_prod_id");
							logger.info("*** rebuildStagData >> eProdId: " + eProdId);
							try {
								Map<String, Object> eProductMap = onlineProductService.getOneProduct(eProdId);
								if (MapUtils.isNotEmpty(eProductMap)) {
									logger.info("*** rebuildStagData >> eProductMap: " + eProductMap);
									List<Map<String, Object>> variants = (List<Map<String, Object>>) eProductMap
											.get("variants");
									Map<String, Object> varntMap = variants.get(0);
									long eVariantId = NumberUtil.getLongValue(varntMap, "id");

									String updateQuery = "update cms_db.staggered_payment_mst "
											+ " set item_variant_id = '" + eVariantId + "' , " + " item_prod_id = '"
											+ eProdId + "', " + " item_qty = 1 " + " WHERE customer_no = '" + customerNo
											+ "'";

									boolean updated = queryBuilderService.onlineExecQuery(apiUrl, updateQuery);
									if (updated) {
										logger.info("*** rebuildStagData >> UPDATED: " + updated);
									}

								}

							} catch (Throwable t) {
								logger.log(Level.SEVERE, t.getMessage(), t);
							}

						}

					}

				}

			}

		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
		}

		logger.info("**** rebuildStagData >> [END]");
	}

	@PostMapping("/execStaggeredStore")
	public void execStaggeredStore() {
		String apiUrl = "https://cms.potatocorner.com//springboot-cms-backend/pc/online-shop/executeDataQuery";
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");

		String eQuery = "  SELECT  max(spm.date_issued) as 'dateIssued', "
				+ "     spm.stgrd_sched_id as 'stgrdSchedId', " + "     spm.promo_code as 'promoCode', "
				+ "     spm.customer_no as 'customerNo', " + "	 	count(distinct sps.stgrd_pay_id) as 'schedCount', "
				+ "	 	spm.pay_terms_count as 'payTermsCount' " + "	 FROM cms_db.staggered_payment_mst spm     "
				+ "	 LEFT JOIN cms_db.staggered_payment_sched sps  on sps.stgrd_sched_id = spm.stgrd_sched_id  "
				+ "	 WHERE  spm.order_name is null  "
				+ "	 group by spm.customer_no,   spm.pay_terms_count,  spm.stgrd_sched_id, spm.promo_code " + " ";

		List<Map<String, Object>> dbOrderList = queryBuilderService.getOnlineExecQuery(apiUrl, eQuery);
		for (Map<String, Object> dbOrderMap : dbOrderList) {
			logger.info("*** execStaggeredNoSchedule >> dbOrderMap: " + dbOrderMap);

			String promoCode = String.valueOf(NumberUtil.getLongValue(dbOrderMap, "promoCode"));
			String customerNo = (String) dbOrderMap.get("customerNo");
			String dateIssued = (String) dbOrderMap.get("dateIssued");
			String stgrdSchedId = (String) dbOrderMap.get("stgrdSchedId");
			double payTermsCount = (Double) dbOrderMap.get("payTermsCount");
			int iPayTermsCount = (int) payTermsCount;

			// DELETE ALL SCHED
			String deleteTxt = "delete from cms_db.staggered_payment_sched " + " where stgrd_sched_id = '"
					+ stgrdSchedId + "' ";
			boolean deleted = queryBuilderService.onlineExecQuery(apiUrl, deleteTxt);
			int payTerms = 6;
			Date dateNow = DateUtil.getDateInManilaPH();
			for (int ii = 0; ii < payTerms; ii++) {
				dateNow = DateUtil.getDateNowPlusTime(dateNow, Calendar.MONTH, 1);
				String schedDateStr = sdf.format(dateNow);
				logger.info("*** execStaggeredNoSchedule >> SCHEDULE DELETED: " + deleted);

				String insertTxt = "insert into cms_db.staggered_payment_sched  " + "		( stgrd_sched_id,  "
						+ "		sched_pay_date,  " + "		actual_pay_date,  " + "		amount_to_pay,  "
						+ "		amount_paid,  " + "		pay_status,  " + "		update_date)  " + "		VALUES  "
						+ " ('" + stgrdSchedId + "',  " + "		STR_TO_DATE('" + schedDateStr + "', '%m/%d/%Y'),  "
						+ "		null, null,  " + "		null,  " + "		'pending',  " + "	  now()) ;";

				logger.info(insertTxt);
				boolean inserted = queryBuilderService.onlineExecQuery(apiUrl, insertTxt);
				logger.info("*** execStaggeredNoSchedule >> inserted: " + inserted);

			}

			String itemDetailQuery = " SELECT concat(spm.promo_code, spm.customer_no) as 'dataId', "
					+ "	spm.promo_code as 'promoCode', " + "	pil.promo_name as 'promoName',  "
					+ "	spm.customer_no as 'customerNo', " + "	ca.store_name as 'storeName', "
					+ "	spm.item_no  as 'itemNo',   " + "	spm.item_variant_id  as 'itemVariantId',   "
					+ "	spm.item_prod_id  as 'itemProdId',   " + "	spm.item_qty  as 'itemQty',   "
					+ "	pd.name as 'itemName',   " + "	spm.date_issued as 'dateIssued', "
					+ "	spm.oos_enabled as 'oosEnabled', " + "	spm.issued_flag as 'issuedFlag', "
					+ "	spm.order_name as 'orderName', " + "	spm.stgrd_sched_id as 'stgrdSchedId', "
					+ "	spm.pay_terms_count as 'payTermsCount', " + "	spm.interest_rate as 'interestRate', "
					+ "	DATE_FORMAT(pil.effect_start_date, '%m/%d/%Y')  as 'effectStartDate', "
					+ "	DATE_FORMAT(pil.effect_end_date, '%m/%d/%Y') as 'effectEndDate'  "
					+ " FROM cms_db.staggered_payment_mst spm "
					+ " JOIN cms_db.promo_issuance_lkp pil on pil.promo_code = spm.promo_code  "
					+ " JOIN cms_db.customer_address ca on ca.customer_number = spm.customer_no "
					+ " JOIN cms_db.customer c  on c.customer_number = spm.customer_no  "
					+ " JOIN cms_db.product_detail pd on pd.item_id = spm.item_no " + " WHERE spm.promo_code = '"
					+ promoCode + "' and   spm.customer_no  = '" + customerNo + "'";

			List<Map<String, Object>> issuanceItemDetails = queryBuilderService.getOnlineExecQuery(apiUrl,
					itemDetailQuery);
			
			if (CollectionUtils.isNotEmpty(issuanceItemDetails)) {
				String jsonMeta = gson.toJson(issuanceItemDetails, List.class);
				try {
					Map<String, Object> rootMap = new LinkedHashMap<String, Object>();
					Map<String, Object> detailMap = new LinkedHashMap<String, Object>();

					detailMap.put("namespace", "staggered_pay_issuance");
					detailMap.put("key", customerNo);
					detailMap.put("value", jsonMeta);
					detailMap.put("type", "json");
					rootMap.put("metafield", detailMap);
					rootMap.put("metaType", "SHOP");

					onlineShopService.updateShopMetafield(rootMap);
					logger.info("*** ProductController >> publishStaggeredPayIssuance >> rootMap: " + rootMap);
					HashMap<String, Object> updateMap = new HashMap<>();
					updateMap.put("promoCode", promoCode);
					updateMap.put("customerNo", customerNo);
					updateMap.put("oosEnabled", "Y");
					String updateTxt = " UPDATE cms_db.staggered_payment_mst SET update_date = now() , "
							+ " oos_enabled = 'Y',  issued_flag = 'N' "
							+ " WHERE promo_code = '"+promoCode +"' and customer_no = '"+customerNo+"' ";
					
					boolean updated = queryBuilderService.onlineExecQuery(apiUrl, updateTxt);
					  
					if (updated) {
						// DELETE First the Next Sched Meta(IF Exist)
						Map<String, Object> rootSchedMap = new LinkedHashMap<String, Object>();
						Map<String, Object> detailSchedMap = new LinkedHashMap<String, Object>();

						detailSchedMap.put("namespace", "staggered_pay_next_sched");
						detailSchedMap.put("key", customerNo);
						detailSchedMap.put("value", "DELETED");
						detailSchedMap.put("type", "single_line_text_field");
						rootSchedMap.put("metafield", detailSchedMap);
						rootSchedMap.put("metaType", "SHOP");
						
						logger.info("*** ProductController >> publishStaggeredPayIssuance >> rootSchedMap: " + rootSchedMap);
						
						onlineShopService.updateShopMetafield(rootSchedMap);
					}

				} catch (Throwable t) {
					logger.log(Level.SEVERE, t.getMessage(), t);
				}
				
			}
		}
	}

	@PostMapping("/execStaggeredNoSchedule")
	public void execStaggeredNoSchedule() {

		// ;String TEST_ORDER = "POR38576";
//		String[] STAGGERED_ORDERS = new String[] {"POR38413","POR38414","POR38420","POR38444","POR38464"};

		// UPDATE ALL Schedule Id
		String apiUrl = "https://cms.potatocorner.com/springboot-cms-backend/pc/online-shop/executeDataQuery";
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
//		String storeQuery = "select order_name as 'orderName', "
//				+ "  promo_code as 'promoCode', "
//				+ "  customer_no as 'customerNo' "
//				+ " from cms_db.staggered_payment_mst ";
//		List<Map<String, Object>> STAGGERED_STORES = queryBuilderService.getOnlineExecQuery(apiUrl, storeQuery);
//		for (Map<String, Object> stagStore : STAGGERED_STORES) {
//			
//			double dPromoCode = (Double)stagStore.get("promoCode"); 
//			int iPromoCode = (int)dPromoCode;
//			String promoCode = String.valueOf(iPromoCode);
//			String customerNo = String.valueOf(stagStore.get("customerNo")); 
//				
//			String genStgSchedId = new StringBuilder("S").append(promoCode)
//					.append(customerNo).toString();
//			
//			String deleteTxt = "update cms_db.staggered_payment_mst set stgrd_sched_id = '"+genStgSchedId+"'" 
//					+ " where customer_no = '" 	+ customerNo + "'";
//			boolean updated = queryBuilderService.onlineExecQuery(apiUrl, deleteTxt);
//			if (updated) {
//				logger.info("*** execStaggeredNoSchedule >> updated: " + updated);
//			}
//		}

//				String eQuery = " select * from (  " + "	 select spm.order_name as 'orderName', "
//						+ "    spm.customer_no as 'customerNo', "
//						+ "     max(spm.date_issued) as 'dateIssued', "
//							+ "     spm.stgrd_sched_id as 'stgrdSchedId', " + "	 so.shop_order_id as 'shopOrderId', "
//						+ "	 count(distinct sps.stgrd_pay_id) as 'schedCount', "
//						+ "	 spm.pay_terms_count as 'payTermsCount' "
//						+ "	 FROM cms_db.staggered_payment_mst spm     "
//						+ "     JOIN cms_db.shop_order so on so.order_name = spm.order_name "
//						+ "	 LEFT JOIN cms_db.staggered_payment_sched sps  on sps.stgrd_sched_id = spm.stgrd_sched_id  "
//						+ "	 WHERE  spm.order_name = '"+TEST_ORDER+"'  "
//						+ "	 group by spm.order_name, spm.pay_terms_count, spm.customer_no, so.shop_order_id, spm.stgrd_sched_id "
//						+ "   ) a where a.schedCount != a.payTermsCount " + " ; ";

		String eQuery = "  select spm.order_name as 'orderName', " + "     max(spm.date_issued) as 'dateIssued', "
				+ "     spm.stgrd_sched_id as 'stgrdSchedId', " + "     spm.customer_no as 'customerNo', "
				+ "	 so.shop_order_id as 'shopOrderId', " + "	 count(distinct sps.stgrd_pay_id) as 'schedCount', "
				+ "	 spm.pay_terms_count as 'payTermsCount' " + "	 FROM cms_db.staggered_payment_mst spm     "
				+ "  JOIN cms_db.shop_order so on so.order_name = spm.order_name "
				+ "	 LEFT JOIN cms_db.staggered_payment_sched sps  on sps.stgrd_sched_id = spm.stgrd_sched_id  "
				+ "	 WHERE  spm.order_name is not null  "
				+ "	 group by spm.customer_no, spm.order_name, spm.pay_terms_count, so.shop_order_id, spm.stgrd_sched_id "
				+ " ";

		List<Map<String, Object>> dbOrderList = queryBuilderService.getOnlineExecQuery(apiUrl, eQuery);
		for (Map<String, Object> dbOrderMap : dbOrderList) {
			logger.info("*** execStaggeredNoSchedule >> dbOrderMap: " + dbOrderMap);
			String orderName = (String) dbOrderMap.get("orderName");

			try {

				String customerNo = (String) dbOrderMap.get("customerNo");
				String dateIssued = (String) dbOrderMap.get("dateIssued");
				String stgrdSchedId = (String) dbOrderMap.get("stgrdSchedId");
				double payTermsCount = (Double) dbOrderMap.get("payTermsCount");
				int iPayTermsCount = (int) payTermsCount;

				double dShopOrderId = (Double) dbOrderMap.get("shopOrderId");
				String shopOrderId = String.valueOf((long) dShopOrderId);
				// INSERT into STAGGERED_PAYMENT_SCHED

				int payTerms = 6;
				double interestRate = 7.1186;
				logger.info("*** execStaggeredNoSchedule >> orderName: " + orderName);
				logger.info("*** execStaggeredNoSchedule >> shopOrderId: " + shopOrderId);
				Map<String, Object> eOrderMap = onlineOrderService.getOneOrderByID(Long.valueOf(shopOrderId));

				if (MapUtils.isNotEmpty(eOrderMap)) {
					boolean isFullPayment = PCDataUtil.isStagIssuanceFullPayment(eOrderMap);
					String payStatus = (String) eOrderMap.get("financial_status");

					String updateOrderTxt = "update cms_db.shop_order set financial_status = '" + payStatus + "'"
							+ " where order_name = '" + orderName + "' ";
					boolean updatedOrder = queryBuilderService.onlineExecQuery(apiUrl, updateOrderTxt);

					if (isFullPayment) {
						String updateTxt = "update cms_db.staggered_payment_mst set pay_terms_count = 0 "
								+ " where order_name = '" + orderName + "' ";
						boolean updated = queryBuilderService.onlineExecQuery(apiUrl, updateTxt);
						logger.info("*** execStaggeredNoSchedule >> updated: " + updated);
						String deleteTxt = "delete from cms_db.staggered_payment_sched" + " where stgrd_sched_id = '"
								+ stgrdSchedId + "' ";
						boolean deleted = queryBuilderService.onlineExecQuery(apiUrl, deleteTxt);
						logger.info("*** execStaggeredNoSchedule >> SCHEDULE DELETED: " + deleted);

						Map<String, Object> rootSchedMap = new LinkedHashMap<String, Object>();
						Map<String, Object> detailSchedMap = new LinkedHashMap<String, Object>();

						detailSchedMap.put("namespace", "staggered_pay_next_sched");
						detailSchedMap.put("key", customerNo);
						detailSchedMap.put("value", "DELETED");
						detailSchedMap.put("type", "single_line_text_field");
						rootSchedMap.put("metafield", detailSchedMap);
						rootSchedMap.put("metaType", "SHOP");
						onlineShopService.updateShopMetafield(rootSchedMap);
						continue;
					}

					logger.info("*** execStaggeredNoSchedule >> eOrderMap: " + eOrderMap);

					double totalOrderAmt = Double.parseDouble(String.valueOf(eOrderMap.get("subtotal_price")));
					double orderInterest = PCDataUtil.computeInterestValue(totalOrderAmt, interestRate);
					double shippingAmt = ShopifyUtil.getTotalShippingAmount(eOrderMap);
					logger.info("*** execStaggeredNoSchedule >> totalOrderAmt: " + totalOrderAmt);
					logger.info("*** execStaggeredNoSchedule >> shippingAmt: " + shippingAmt);

					totalOrderAmt = (totalOrderAmt + shippingAmt + orderInterest);
					double totalOrderPlusInterest = NumberUtil.roundTwoDec(totalOrderAmt);

					double splitPayAmt = NumberUtil.roundTwoDec((totalOrderAmt / payTerms));
					boolean paySchedUpdated = false;
					double splitPaymentTotal = 0;

					boolean inserted = false;

					// DELETE ALL SCHED
					String deleteTxt = "delete from cms_db.staggered_payment_sched " + " where stgrd_sched_id = '"
							+ stgrdSchedId + "' ";
					boolean deleted = queryBuilderService.onlineExecQuery(apiUrl, deleteTxt);
					logger.info("*** execStaggeredNoSchedule >> SCHEDULE DELETED: " + deleted);

					Date issuedDate = DateUtil.stringToDate(dateIssued, "yyyy-MM-dd'T'HH:mm:ss");
					Date schedDate = new Date(issuedDate.getTime());
					for (int ii = 1; ii <= payTerms; ii++) {
						logger.info("*** execStaggeredNoSchedule >> orderName: " + orderName);
						schedDate = DateUtil.getDateNowPlusTime(schedDate, Calendar.MONTH, 1);

						String schedDateStr = sdf.format(schedDate);
						if (ii == payTerms) {
							// LAST Payment for staggered
							double lastPayment = (totalOrderPlusInterest - splitPaymentTotal);
							logger.info(
									"*** ShopWebhookController >> orderCreationHook >> lastPayment: " + lastPayment);
							splitPayAmt = NumberUtil.roundTwoDec(lastPayment);
						}

						String insertTxt = "insert into cms_db.staggered_payment_sched  " + "		( stgrd_sched_id,  "
								+ "		sched_pay_date,  " + "		actual_pay_date,  " + "		amount_to_pay,  "
								+ "		amount_paid,  " + "		pay_status,  " + "		update_date)  "
								+ "		VALUES  " + " ('" + stgrdSchedId + "',  " + "		STR_TO_DATE('"
								+ schedDateStr + "', '%m/%d/%Y'),  " + "		null,  " + "		'" + splitPayAmt
								+ "',  " + "		null,  " + "		'pending',  " + "	  now()) ;";
						inserted = false;
						logger.info(insertTxt);
						inserted = queryBuilderService.onlineExecQuery(apiUrl, insertTxt);
						if (inserted) {

							// List<Map<String, Object>> nextPayDetails =
							// issuanceMapper.getNextPaymentDetails(nxtMap);
							String nxtPayQuery = "SELECT min(sps.stgrd_pay_id) as 'rowSchedId'    "
									+ "			 , sps.stgrd_sched_id as  'stgrdSchedId'    "
									+ "			 , min(sps.amount_to_pay) as 'amountToPay'    "
									+ "			 , min(sps.sched_pay_date) as 'nextPayDate'    "
									+ "			 , sps.pay_status as 'payStatus'    "
									+ "			 ,  spm.order_name as 'orderName'    "
									+ "			 , sol.so_item_no as 'itemId'   "
									+ "			 , sol.quantity as 'itemQty'       "
									+ "			 , pd.name as 'itemName'     "
									+ "			 , (select sum(amount_to_pay) from cms_db.staggered_payment_sched   "
									+ "						where stgrd_sched_id =  sps.stgrd_sched_id) as 'totalAmountToPay'   "
									+ "		 FROM cms_db.staggered_payment_sched sps    "
									+ "			 join cms_db.staggered_payment_mst spm on sps.stgrd_sched_id = spm.stgrd_sched_id    "
									+ "			 join cms_db.shop_order_line sol on sol.order_name = spm.order_name    "
									+ "			 join cms_db.product_detail pd on pd.item_id = sol.so_item_no       "
									+ "		 WHERE spm.order_name = '" + orderName + "'     "
									+ "		   and sps.pay_status = 'pending' "
									+ "	   group by sps.stgrd_sched_id,   sps.pay_status,   "
									+ "		 sol.so_item_no  , pd.name, sol.quantity,  spm.order_name ;";
							List<Map<String, Object>> nextPayDetails = queryBuilderService.getOnlineExecQuery(apiUrl,
									nxtPayQuery);

							if (CollectionUtils.isNotEmpty(nextPayDetails)) {

								try {

									Map<String, Object> nextPayMap = nextPayDetails.get(0);
									List<HashMap<String, String>> itemDetails = new ArrayList<>();
									for (Map<String, Object> nxtPayDtl : nextPayDetails) {
										HashMap<String, String> itemDtl = new HashMap<>();
										itemDtl.put("itemId", (String) nxtPayDtl.get("itemId"));
										itemDtl.put("itemName", (String) nxtPayDtl.get("itemName"));
										int itemQty = NumberUtil.getIntValue(nxtPayDtl, "itemQty");
										itemDtl.put("itemQty", String.valueOf(itemQty));
										itemDetails.add(itemDtl);
									}
									nextPayMap.put("itemDetails", itemDetails);

									if (nextPayMap.get("nextPayDate") != null) {
										String nextPayDateTime = (String) nextPayMap.get("nextPayDate");
										// Date nextPayDate =
										// DateUtil.convertToDateViaSqlTimestamp(nextPayDateTime);
//												String nextPayDateStr = DateUtil.getDateWithPattern(nextPayDate,
//														"MM/dd/yyyy");

										Date nextPayDate = DateUtil.stringToDate(nextPayDateTime,
												"yyyy-MM-dd'T'HH:mm:ss");
										String nextPayDateStr = DateUtil.getDateWithPattern(nextPayDate, "MM/dd/yyyy");
										nextPayMap.put("nextPayDateStr", nextPayDateStr);
									}

									String jsonMeta = gson.toJson(nextPayMap, Map.class);

									Map<String, Object> rootSchedMap = new LinkedHashMap<String, Object>();
									Map<String, Object> detailSchedMap = new LinkedHashMap<String, Object>();

									detailSchedMap.put("namespace", "staggered_pay_next_sched");
									detailSchedMap.put("key", customerNo);
									detailSchedMap.put("value", jsonMeta);
									detailSchedMap.put("type", "json");
									rootSchedMap.put("metafield", detailSchedMap);
									rootSchedMap.put("metaType", "SHOP");
									onlineShopService.updateShopMetafield(rootSchedMap);

								} catch (Throwable t) {
									logger.log(Level.SEVERE, t.getMessage(), t);
								}
							}

						}

						splitPaymentTotal += splitPayAmt;

					}
				}

			} catch (Throwable e) {
				logger.info("*** execStaggeredNoSchedule >> orderName: " + orderName);
				logger.info("*** execStaggeredNoSchedule >> orderName: " + orderName);
				logger.log(Level.SEVERE, e.getMessage(), e);
			}

		}

	}

	@PostMapping("/execJobOnline2")
	public void execJobOnline2() {
		try {

//			String localQuery = "SELECT * FROM cms_db.config_fries_switch";
//			List<Map<String, Object>> localList = queryBuilderService.getExecQuery(localQuery);
//			for (Map<String, Object> localMap : localList) {
//				String whCode = StringUtils.upperCase(StringUtils.trimToEmpty((String) localMap.get("warehouse_code")));
//				String apiUrl = "https://pc-cms.uat.shakeys.solutions/springboot-cms-backend/pc/online-shop/executeDataQuery";
//				String eInsertTxt = "insert into cms_db.config_fries_switch (warehouse_code) values ('" + whCode
//						+ "') ";
//				queryBuilderService.onlineExecQuery(apiUrl, eInsertTxt);
//			}

			String apiUrl = "https://cms.potatocorner.com//springboot-cms-backend/pc/online-shop/executeDataQuery";
			String eQuery = "SELECT so.* from cms_db.shop_order so    WHERE  so.db_create_date BETWEEN DATE_SUB(now(), INTERVAL 1 DAY) and DATE_ADD(now(), INTERVAL 1 DAY) ";
			List<Map<String, Object>> eOrderList = queryBuilderService.getOnlineExecQuery(apiUrl, eQuery);
			for (Map<String, Object> eOrderMap : eOrderList) {
				String orderName = (String) eOrderMap.get("order_name");
				String dbCustomerNo = (String) eOrderMap.get("so_customer_no");
				String eItemQuery = " select item_id FROM cms_db.store_auto_issuance where customer_no = '"
						+ dbCustomerNo + "' " + "	and order_name is null";
				List<Map<String, Object>> eItemList = queryBuilderService.getOnlineExecQuery(apiUrl, eItemQuery);
//				String itemIds = PCDataUtil.getAllItemIdsByList(eItemList, "item_id");

				String issuanceQuery = "SELECT  sai.promo_code as 'promoCode'   "
						+ "	 , sai.customer_no as 'customerNo'   " + "	 , sai.warehouse_code as  'warehouse'    "
						+ "	 , max(so.db_create_date) as 'creationDate'   "
						+ "     , count(sai.item_id ) as 'itemCount'   " + " FROM cms_db.shop_order_line sol   "
						+ " JOIN cms_db.store_auto_issuance sai on sol.so_item_no = sai.item_id   "
						+ "  and sol.quantity = sai.qty_issuance   "
						+ " JOIN cms_db.shop_order so on sol.order_name = so.order_name   "
						+ "	and so.so_customer_no = sai.customer_no    " + " WHERE sol.order_name = '" + orderName
						+ "'	   " + "  and sai.oos_enabled = 'Y'    " + "  and sai.issued_flag = 'N'    "
						+ " GROUP BY  sai.promo_code , sai.customer_no , sai.warehouse_code ";

				List<Map<String, Object>> issuanceList = queryBuilderService.getOnlineExecQuery(apiUrl, issuanceQuery);
				if (CollectionUtils.isNotEmpty(issuanceList)) {
					Map<String, Object> issuanceMap = issuanceList.get(0);
					double promoCode = (Double) issuanceMap.get("promoCode");
					int iPromoCode = (int) promoCode;
					String customerNo = (String) issuanceMap.get("customerNo");

					logger.info("orderName: " + orderName);
					logger.info("issuanceList: " + issuanceList.size());
					double itemCount = (Double) issuanceMap.get("itemCount");
					logger.info("itemCount: " + itemCount);
					logger.info("eItemList: " + eItemList.size());
					if (eItemList.size() == itemCount) {
						logger.info("*** MATCH!!!! *** ");

						// LocalDateTime issuedDateTime = (LocalDateTime)
						// issuanceMap.get("creationDate");
						// Date issuedDate = DateUtil.convertToDateViaSqlTimestamp(issuedDateTime);
						String updateTxt = "UPDATE cms_db.store_auto_issuance set order_name = '" + orderName + "' "
								+ ", issued_date =now() " + ", issued_flag ='Y' " + " WHERE promo_code = '" + iPromoCode
								+ "' " + "   and customer_no = '" + customerNo + "'";

						boolean tagAsIssued = queryBuilderService.onlineExecQuery(apiUrl, updateTxt);
						logger.info("***  ecJobOnline >> tagAsIssued: " + tagAsIssued);
						// Initiate RE-Publishing of Promo and REMOVE Meta
						if (tagAsIssued) {
							try {
								Map<String, Object> rootMap = new LinkedHashMap<String, Object>();
								Map<String, Object> detailMap = new LinkedHashMap<String, Object>();

								detailMap.put("namespace", "product_auto_issuance");
								detailMap.put("key", customerNo);
								detailMap.put("value", "DELETED");
								detailMap.put("type", "single_line_text_field");
								rootMap.put("metafield", detailMap);
								rootMap.put("metaType", "SHOP");

								logger.info(
										"*** ShopWebhookController >> processAutoNBDIssuance >> rootMap: " + rootMap);

								onlineShopService.updateShopMetafield(rootMap);

							} catch (Throwable e) {
								logger.log(Level.SEVERE, e.getMessage(), e);
							}

//							try {
//								StringBuilder ewtTags = new StringBuilder("NPD_ORDER");
//								onlineOrderService.addOrderTagToCurrent(eOrderMap, ewtTags.toString());
//							} catch (Throwable e) {
//								logger.log(Level.SEVERE, e.getMessage(), e);
//							}
						}
					}
				}

			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@PostMapping("/cancelSalesOrder")
	public ResponseEntity<List<Map<String, Object>>> cancelSalesOrder(@RequestBody Map<String, Object> paramBody)
			throws Throwable {
		logger.info("** D365AppController >> cancelSalesOrder >> [START]");
		HttpStatus httpStat = HttpStatus.OK;
		if (Constants.TEST_ONLY) {
			apiSpaviHostUrl = "https://spavi-d365.operations.dynamics.com";
		}

		String requestUrl = new StringBuilder(apiSpaviHostUrl)
				.append("/api/services/SPAVPCIntegrationServiceGroup/SPAVPCIntegrationService/cancelSalesOrder")
				.toString();

		String accessToken = restD365Service.getLatestD365Token();
		logger.info("accessToken: " + accessToken);
		String soNumber = (String) paramBody.get("salesOrderNo");
		HashMap<String, Object> rootMap = new HashMap<>();
		HashMap<String, Object> paramMap = new HashMap<>();
		List<String> soList = new ArrayList<>();
		if (StringUtils.isNotBlank(soNumber)) {
			if (soNumber.contains(",")) {
				soList = Arrays.asList(soNumber.split(","));
			} else {
				soList.add(soNumber);
			}
		}

		for (String soNum : soList) {
			if (StringUtils.isNotBlank(StringUtils.trimToEmpty(soNum))) {
				paramMap.put("SONumber", StringUtils.trimToEmpty(soNum));
				paramMap.put("DataAreaId", defaultDataAreaId);
				rootMap.put("_dataContract", paramMap);
				Map<String, Object> resultMap = restD365Service.sendPostRequest(requestUrl, accessToken, rootMap,
						"string");
				if (MapUtils.isNotEmpty(resultMap)) {
					HttpStatus resultStatus = (HttpStatus) resultMap.get(Constants.RESULT_HTTP_STATUS_CODE);
					if (resultStatus.equals(HttpStatus.OK) && resultMap.get("result") != null) {
						logger.info("** D365AppController >> cancelSalesOrder >> RESULT: " + soNum + "***"
								+ String.valueOf(resultMap.get("result")));
					}
				}
			}

		}

		logger.info("** D365AppController >> cancelSalesOrder >> [END]");
		return null;
	}

	@PostMapping("/reGenerateSalesOrder")
	public ResponseEntity<Map<String, Object>> reGenerateSalesOrder(@RequestBody Map<String, Object> paramBody)
			throws Throwable {
		logger.info("** OnlineShopController >> reGenerateSalesOrder >> [START]");
		HttpStatus httpStat = HttpStatus.OK;
		Map<String, Object> resultMap = new HashMap<>();
		List<String> orderList = new ArrayList<String>();
		String orderNameStr = (String) paramBody.get("orderName");
		if (orderNameStr.contains(",")) {
			orderList = Arrays.asList(orderNameStr.split(","));
		} else {
			orderList.add(orderNameStr);
		}

		for (String orderName : orderList) {
			logger.info("** OnlineShopController >> reGenerateSalesOrder >> orderName: " + orderName);
			// Remove existing the SO in Tags and DB
			// ***************************************
			if (StringUtils.isNotBlank(orderName)) {
				Map<String, Object> eOrder = onlineOrderService.getOneOrderByName(orderName);
				if (MapUtils.isNotEmpty(eOrder)) {
					String financialStatus = (String) eOrder.get("financial_status");
					if (financialStatus.equals(Constants.STATUS_PAID)) {
						String soCustomerNo = ShopifyUtil.getSOCustomerNoByAddress(eOrder, new HashMap<>());
						if (StringUtils.isNotBlank(soCustomerNo)) {
							HashMap<String, Object> updateMap = new HashMap<>();
							updateMap.put("soCustomerNo", soCustomerNo);
							updateMap.put("salesOrderNo", "");
							updateMap.put("orderName", orderName);
							int updateResult = shopOrderMapper.updateShopOrder(updateMap);
							logger.info(
									"** OnlineShopController >> reGenerateSalesOrder >> updateResult: " + updateResult);
							if (updateResult != 0) {
								Map<String, Object> syncMap = new HashMap<>();
								syncMap.put("processPerOrder", true);
								syncMap.put("orderName", orderName);
								resultMap = syncD365Service.syncSalesOrderDataToDB(syncMap);
								logger.info(resultMap.toString());
								if (MapUtils.isNotEmpty(resultMap) && resultMap.containsKey("statusResult")
										&& ((String) resultMap.get("statusResult")).equals("SUCCESS")) {
									resultMap.put("soCustomerNo", soCustomerNo);
									resultMap.put("orderName", orderName);
									resultMap.put("financialStatus", "paid");
									logger.info("** OnlineShopController >> reGenerateSalesOrder >> resultMap: "
											+ resultMap);
								}

							}
						}
					}

				}
			}

		}

		logger.info("** OnlineShopController >> reGenerateSalesOrder >> [END]");
		return new ResponseEntity<Map<String, Object>>(resultMap, httpStat);
	}

	@PostMapping("/runTestJob")
	public void runTestJob() {
		// cronJobService.processOrdersWithAutoPay(null);
		System.out.println("********** OnlineShopController >> runTestJob >> [START] ");
		try {

			Map<String, Object> paramMap = new HashMap<>();

			String getTablesQuery = "SELECT  " + "    * " + " FROM " + "    serverposdb.INFORMATION_SCHEMA.tables;";

			List<Map<String, Object>> tableList = queryBuilderService.getExecQuery(getTablesQuery);
			for (Map<String, Object> tableMap : tableList) {

			}

//			String[] mailRecipients = Constants.MAIL_RECIPIENTS;
//			if (Constants.TEST_ONLY) {
//				mailRecipients = new String[] { "kgundran@gmail.com" };
//			}
//			
//			boolean mailSent = emailService.sendEmail(null, mailRecipients, "RUN Job Test", "RUN Job Test",
//					null, null, null);	
//			onlineProductService.updateProductDBFromOnline(null);
//			onlineShopService.syncLocalProductToOnline(paramMap);

//			Map<String, Object> rootMap = new LinkedHashMap<String, Object>();
//			Map<String, Object> detailMap = new LinkedHashMap<String, Object>();
// 
//			detailMap.put("namespace", "staggered_pay_next_sched");
//			detailMap.put("key", "FR066PCF00145");
//			detailMap.put("value", "DELETED");
//			detailMap.put("type", "single_line_text_field");
//			rootMap.put("metafield", detailMap);
//			rootMap.put("metaType", "SHOP");
//
//			onlineShopService.updateShopMetafield(rootMap);

//			paramMap.put("local_path", "C:\\Temp\\SPAVI\\prod_images\\new upload-20231004T135017Z-001\\new upload\\");
//			paramMap.put("img_extensions", "jpg,PNG");
//				    
//			onlineProductService.updateProductImage(paramMap);

			// cronJobService.runBDOPostTokenRequest(paramMap);
//			
//			paramMap.put("orderName", "UAT1047");
//			cronJobService.runBDOCheckPaymenStatusRequest(paramMap);

//			String queryTxt = " SELECT  sai.promo_code as 'promoCode' " + 
//					"      , sai.customer_no as 'customerNo' " + 
//					"      , sai.warehouse_code as  'warehouse'  " + 
//					"      , so.db_create_date as 'creationDate' " + 
//					" FROM cms_db.shop_order_line sol " + 
//					" JOIN  cms_db.store_auto_issuance sai on sol.so_item_no = sai.item_id " + 
//					"	and sol.so_warehouse_code = sai.warehouse_code " + 
//					"   and sol.quantity = sai.qty_issuance " + 
//					" JOIN cms_db.shop_order so on sol.order_name = so.order_name     " + 
//					" WHERE sol.order_name = 'UAT1038' " + 
//					"	and sai.oos_enabled = 'Y' " + 
//					"	and sai.issued_flag = 'N' " + 
//					" LIMIT 1    ";
//			List<Map<String, Object>> issuanceDataList = queryBuilderService.getExecQuery(queryTxt);
//			for (Map<String, Object> issueData: issuanceDataList) {
//				String promoCode = String.valueOf(issueData.get("promoCode"));
//				String customerNo = String.valueOf(issueData.get("customerNo"));
//				String warehouse = String.valueOf(issueData.get("warehouse"));
//				LocalDateTime issuedDateTime = (LocalDateTime)issueData.get("creationDate");
//				Date issuedDate = DateUtil.convertToDateViaSqlTimestamp(issuedDateTime);
//				 String updateTxt = "UPDATE cms_db.store_auto_issuance set order_name = 'UAT1038' "
//				    		+ ", issued_date ='"+issuedDate+"' "
//				    		+ ", issued_flag ='Y' "
//							+ " WHERE promo_code = '"+promoCode+"' "
//				        	+ "   and customer_no = '"+customerNo+"'" 
//		        			+ "   and warehouse_code = '"+warehouse+"'";
//			    boolean issuanceTag =	queryBuilderService.execQuery(updateTxt);
//			}
//			onlineShopService.syncLocalCustomerToOnlineByEmail(paramMap);
//			String customerId = "6827958206739";
//			List<Map<String, Object>> eAddressList = onlineCustomerService.getAllOnlineAddressByID(customerId);	
//			for (Map<String, Object> eAddrMap: eAddressList) {
//				logger.info("eAddrMap: " + eAddrMap);
//				Long eAddressId = NumberUtil.getLongValue(eAddrMap, "id");
//		 		String address1 = StringUtils.trimToEmpty((String) eAddrMap.get("address1")) + ".";
//			  	paramMap.put("address1", address1);
//				onlineCustomerService.updateCustomerAddress(Long.valueOf(customerId), eAddressId, paramMap); 
//				
//			}
//	 
//			Date prevDate = DateUtil.getDateNowPlusTime(Calendar.DAY_OF_YEAR, -1);
//			String updatedAtMin = DateUtil.getISODateFromTimeFormat(prevDate);
//			//paramMap.put("updated_at_min", updatedAtMin);
//			paramMap.put("status", "open");// Show only open orders.
//			paramMap.put("limit", "250");
//			paramMap.put("name", "UAT1151");
//			Map<String, Object> resultMap = cronJobService.saveOnlineOrderToDB(paramMap);

//			Map<String, Object> replaceParams = new LinkedHashMap<>();
//			List<String> curlCommands = new ArrayList<>();
//			curlCommands.addAll(Arrays.asList("cmd", "/c")); 
//			curlCommands.add("curl -X POST https://postman-echo.com/post --data foo1=bar1&foo2=bar2");
//			curlCommands.add("curl --location --request POST 'https://pc-cms.uat.shakeys.solutions/springboot-cms-backend/pc/online-shop/executeDataQuery' \\");
//			curlCommands.add("--header 'Content-Type: application/json' \\");
//			curlCommands.add("--data ' { " + 
//					" \"queryTxt\":\"select * from  cms_db.shop_order where order_name = '\\''UAT1031'\\''\"   " + 
//					"} '"); 

//			processCurlService.runCurlCommand(curlCommands, null, replaceParams);

//			cronJobService.runBDOCheckPaymenStatusRequest(paramMap);

//			List<String> orderLines = FileUtils.readLines(new File("/temp/SPAVI/missing_so.txt"));
//			for (String orderLine: orderLines) {
//				String[] orderArry = orderLine.split("=");
//				System.out.println(orderArry[0]);
//				System.out.println(orderArry[1]);
//				
//				String paramOrder = orderArry[0];
//				String paramSalesOrderNo = orderArry[1]; 
//				  
//				String apiUrl = "https://cms.potatocorner.com/springboot-cms-backend/pc/online-shop/executeDataQuery";
//				String queryTxt = " select sales_order_no, order_name, shop_order_id  " + "from cms_db.shop_order "
//						+ "where  order_name = '"+paramOrder+"'  ";
//				List<Map<String, Object>> orderList = queryBuilderService.getOnlineExecQuery(apiUrl, queryTxt);
//				StringBuilder sbOrders = new StringBuilder();
//				for (Map<String, Object> orderMap : orderList) {
//					String orderName = (String) orderMap.get("order_name"); 
//					Long orderId = NumberUtil.getLongValue(orderMap, "shop_order_id");
//					try {
//						Map<String, Object> eOrderMap = onlineOrderService.getOneOrderByID(orderId);
//						if (MapUtils.isNotEmpty(eOrderMap)) {
//							String orderTag = (String) eOrderMap.get("tags");
//							String financialStatus = (String) eOrderMap.get("financial_status");
//							if (!orderTag.contains(Constants.SALES_ORDER_NO_TAG)
//									&& financialStatus.equals(Constants.STATUS_PAID)) {
//								// NO SO Tag Yet
//								// PUT SO Tag in ORDER
//								String finalSOTag = ShopifyUtil.getSONumberTagByOrderDB(paramSalesOrderNo);
//								System.out.println(orderName + "==" + finalSOTag);
//								  
//								StringBuilder addTag = new StringBuilder();
//								addTag.append("SOSTAT_Open order").append(",")
//								.append(finalSOTag);
//								onlineOrderService.addOrderTagToCurrent(eOrderMap, addTag.toString());
//							}
//
//						}
//					} catch (Throwable t) {
//
//					}
//
//				}
//				
//			}
//			
//			String paramOrder = "POR31130";
//			String paramSalesOrderNo = "CIPC_SO-000112060,CIPC_SO-000112062"; 
//			  
//			String apiUrl = "https://cms.potatocorner.com/springboot-cms-backend/pc/online-shop/executeDataQuery";
//			String queryTxt = " select sales_order_no, order_name, shop_order_id  " + "from cms_db.shop_order "
//					+ "where  order_name = '"+paramOrder+"'  ";
//			List<Map<String, Object>> orderList = queryBuilderService.getOnlineExecQuery(apiUrl, queryTxt);
//			StringBuilder sbOrders = new StringBuilder();
//			for (Map<String, Object> orderMap : orderList) {
//				String orderName = (String) orderMap.get("order_name"); 
//				Long orderId = NumberUtil.getLongValue(orderMap, "shop_order_id");
//				try {
//					Map<String, Object> eOrderMap = onlineOrderService.getOneOrderByID(orderId);
//					if (MapUtils.isNotEmpty(eOrderMap)) {
//						String orderTag = (String) eOrderMap.get("tags");
//						String financialStatus = (String) eOrderMap.get("financial_status");
//						if (!orderTag.contains(Constants.SALES_ORDER_NO_TAG)
//								&& financialStatus.equals(Constants.STATUS_PAID)) {
//							// NO SO Tag Yet
//							// PUT SO Tag in ORDER
//							String finalSOTag = ShopifyUtil.getSONumberTagByOrderDB(paramSalesOrderNo);
//							System.out.println(orderName + "==" + finalSOTag);
//							 
//							StringBuilder addTag = new StringBuilder();
//							addTag.append("SOSTAT_Open order").append(",")
//							.append(finalSOTag);
//							onlineOrderService.addOrderTagToCurrent(eOrderMap, addTag.toString());
//						}
//
//					}
//				} catch (Throwable t) {
//
//				}
//
//			}
//			
//			System.out.println("sbOrders: " + sbOrders);

			// cronJobService.syncSalesOrderDataToDB(new HashMap<>());
//		 
// 			onlineOrderService.syncOrderDbSOToOnline(paramMap);
//			syncD365Service.syncProductDataToDB(new HashMap<>()); 

//			List<Map<String, Object>> eProducts = onlineProductService.getAllOnlineProducts(new HashMap<>());
//			for (Map<String, Object> prodMap: eProducts) {
//				 String prodTitle = (String)prodMap.get("title");
//				 String itemNo = ShopifyUtil.getD365ItemIdFromTitle(prodTitle);
//			 	 Long productId = NumberUtil.getLongValue(prodMap, "id");
//			 			 
//				try {  
//					Map<String, Object> tempDBProdMap = new HashMap<>();
//					tempDBProdMap.put("itemNumber", itemNo);
//					Map<String, Object> imgRequestMap = ShopifyUtil.buildProductImageRequest(null, tempDBProdMap, prodMap);
//					String imgRequestUrl = new StringBuilder("/admin/api/").append(apiVersion).append("/products/")
//							.append(productId).append("/images.json").toString();
//					
//					if (MapUtils.isNotEmpty(imgRequestMap)) {
//						Map<String, Object> createdImgMap = restTemplateService.sendPostRequest(imgRequestUrl, 	imgRequestMap);
//						logger.info("*** syncLocalProductToOnline >> createdImgMap: " + createdImgMap);
//					}
//				} catch (Throwable t) {
//					logger.log(Level.SEVERE, t.getMessage(), t);
//				}
//					 
//		 	
//			}

//			syncD365Service.syncCustomerDataToDB(new HashMap<>());
//			paramMap.put("email", "");
//			onlineShopService.syncLocalCustomerToOnlineByEmail(paramMap);
			// syncD365Service.syncSalesOrderFulfillByOrderDB(new HashMap<>());
			// syncD365Service.syncCustomerDataToDB(new HashMap<>());
//			HashMap<String, Object> paramMap = new HashMap<>();
//			paramMap.put("limit", "50");
			// acumaticaService.getStockItemList(paramMap);

			// acumaticaService.getStockItemByID("ART0100001");
//			Long orderId = Long.valueOf("4677924028512");
//			//acumaticaService.getStockItemByID(true, "PC00000001", false);
//			Map<String, Object> eOrderMap = onlineOrderService.getOneOrderByID(orderId);
			// acumaticaService.createPurchaseOrder(true, eOrderMap);

//			Date prevDate = DateUtil.getDateNowPlusTime(Calendar.DAY_OF_YEAR, -1); 
//			String updatedAtMin = DateUtil.getISODateFromTimeFormat(prevDate);
//  			paramMap.put("updated_at_min", updatedAtMin);
//			paramMap.put("status", "open");// Show only open orders.
//			paramMap.put("limit", "250");
//			 
//			cronJobService.saveOnlineOrderToDB(paramMap);
//			List<Map<String, Object>> warehouseList = acumaticaService.getWarehouseList(true, paramMap);
//			for (Map<String, Object> whMap: warehouseList) {
//				String poWHId = (String)PCDataUtil.getValue(whMap, "WarehouseID");
//				String poWHName = (String)PCDataUtil.getValue(whMap, "Description");
//		       // logger.info(poWHId + "|" + poWHName);
//				 (  poWHName);
//			}

//			List<String> soEmails = new ArrayList<>();
//			soEmails.add("matthew.poako@gmail.com");
//			soEmails.add("jacquelinetytan@yahoo.com");
//			soEmails.add("eighthofjuly@outlook.com");
//			soEmails.add("baligod_stephanie@yahoo.com");
//			soEmails.add("wchernandez29@gmail.com");
//			soEmails.add("mary.dulin@potatocorner.com"); 
//			soEmails.add("neziel.glinogo@potatocorner.com");
//			soEmails.add("karen.resuello@potatocorner.com");
//			soEmails.add("mary.dulin@potatocorner.com"); 
//			soEmails.add("");  
//			cronJobService.saveOnlineOrderToDB(new HashMap<>());
//			paramMap.put("processPerOrder", true); 
//			paramMap.put("orderName", "UAT1047"); 
//			syncD365Service.syncSalesOrderDataToDB(paramMap);

//			for (String email: soEmails) { 
//				paramMap.put("email", email);  
//				cronJobService.processOrdersWithAutoPay(paramMap);
// 			}

			// syncD365Service.processPurchaseOrder(new HashMap<>());

//			syncD365Service.syncSalesOrderDataToDB(new HashMap<>());

//			List<Map<String, Object>> eProducts = onlineProductService.getAllOnlineProducts(new HashMap<>());
//			for (Map<String, Object> prodMap: eProducts) {
//				List<Map<String, Object>> variantMaps = (List<Map<String, Object>> )prodMap.get("variants");
//				Map<String, Object> variantMap = variantMaps.get(0);
//				Long prodId = NumberUtil.getLongValue(variantMap,"product_id");
//				Long variantId = NumberUtil.getLongValue(variantMap,"id");
//					String sku = (String)variantMap.get("sku");
//				if (StringUtils.isBlank(sku)) {
//					Map<String, Object> updateMap = new HashMap<>();
//					String newSKU = ShopifyUtil.getD365ItemIdFromTitle((String)prodMap.get("title"));
//					updateMap.put("sku", newSKU);
//					try {
//						Map<String, Object> resultMap = onlineProductService.updateVariantData(prodId, variantId, updateMap);
//						logger.info("resultMap: " + resultMap);
//					} catch (Throwable t) {
//						logger.log(Level.SEVERE, t.getMessage(), t);
//					}
//				
//				}
//		 	}
//			

			// onlineProductService.syncProductInventory(new HashMap<>());
//			List<Map<String,Object>> dbPOs = queryBuilderService.getExecQuery("select * from cms_db.bank_api_config");
//			for (Map<String,Object> poMap: dbPOs) {
//				
//				String bank_name = StringUtils.trimToEmpty((String)poMap.get("bank_name"));
//				String env_type = StringUtils.trimToEmpty((String)poMap.get("env_type"));
//				String api_request_url = StringUtils.trimToEmpty((String)poMap.get("api_request_url"));
//				String api_column = StringUtils.trimToEmpty((String)poMap.get("api_column"));
//				String api_value = StringUtils.trimToEmpty((String)poMap.get("api_value"));
//				String api_data_type = StringUtils.trimToEmpty((String)poMap.get("api_data_type"));
//				String api_usage = StringUtils.trimToEmpty((String)poMap.get("api_usage"));
//				String api_type = StringUtils.trimToEmpty((String)poMap.get("api_type"));
//				String api_description =  StringUtils.trimToEmpty((String)poMap.get("api_description"));
//				Integer column_seq = (Integer)poMap.get("column_seq");
//			 
//					
//				String requestUrl = "https://cms.potatocorner.com/springboot-cms-backend/pc/online-shop/executeDataQuery";
//				
//				String queryTxt = "insert into cms_db.bank_api_config (bank_name " + 
//						",env_type " + 
//						",api_request_url " + 
//						",api_column " + 
//						",api_value " + 
//						",api_data_type " + 
//						",api_usage " + 
//						",api_type " + 
//						",api_description " + 
//						",column_seq " + 
//						",update_date)  "
//						+ " values ('"+bank_name+"' , '"+env_type+"','"+api_request_url+"','"
//						+api_column+"','"+api_value+"','"+api_data_type+"','"+api_usage+"','"
//						+api_type+"','"+api_description+"','"+column_seq+"', now());";
//				Map<String, Object> paramMap = new HashMap<>();
//				paramMap.put("queryTxt", queryTxt);
//				
//				try {
//					restGenericService.sendPostRequest(requestUrl, paramMap);
//				} catch (Throwable t) {
//					logger.log(Level.SEVERE, t.getMessage(), t);
//				}
//		 		 
//			} 

//			String requestUrl = "https://cms.potatocorner.com/springboot-cms-backend/pc/online-shop/executeDataQuery";
//
//			String queryTxt = "update cms_db.shop_order set contact_email = concat(contact_email, '_X') where contact_email not like '%_X%';";
//			paramMap = new HashMap<>();
//			paramMap.put("queryTxt", queryTxt);
//			//1000 = 1sec
//			//60000 = 1min
//			int sleepInMins = (60000 * 15);//15mins 
//			 
//			for (int ii=0;ii<=100;ii++) {
//				try {
//					Date startDate = new Date();
//					System.out.println("**** update cms_db.shop_order [START] " + startDate);
//					restGenericService.sendPostRequest(requestUrl, paramMap);
//					Thread.sleep(sleepInMins); 
//					Date endDate = new Date();
//					System.out.println("**** update cms_db.shop_order [END] " + endDate);
//				} catch (Throwable t) {
//					logger.log(Level.SEVERE, t.getMessage(), t);
//				}
//			}

//			syncD365Service.syncSalesOrderDataToDB(new HashMap<>());
//			

		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
		} catch (Throwable t) {
			logger.log(Level.SEVERE, t.getMessage(), t);
		}

		System.out.println("********** OnlineShopController >> runTestJob >> [END] ");
	}

}
