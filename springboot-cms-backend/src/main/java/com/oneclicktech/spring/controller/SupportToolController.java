package com.oneclicktech.spring.controller;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.IOUtils;
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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.oneclicktech.spring.domain.Constants;
import com.oneclicktech.spring.mapper.ClientTokenMapper;
import com.oneclicktech.spring.mapper.CustomerMapper;
import com.oneclicktech.spring.mapper.EWTMapper;
import com.oneclicktech.spring.mapper.ShopOrderMapper;
import com.oneclicktech.spring.mapper.TableUtilMapper;
import com.oneclicktech.spring.service.AcumaticaService;
import com.oneclicktech.spring.service.CronJobService;
import com.oneclicktech.spring.service.EmailService;
import com.oneclicktech.spring.service.OnlineCustomerService;
import com.oneclicktech.spring.service.OnlineOrderService;
import com.oneclicktech.spring.service.OnlineProductService;
import com.oneclicktech.spring.service.OnlineShopService;
import com.oneclicktech.spring.service.QueryBuilderService;
import com.oneclicktech.spring.service.RestD365Service;
import com.oneclicktech.spring.service.RestGenericService;
import com.oneclicktech.spring.service.RestTemplateService;
import com.oneclicktech.spring.service.SyncD365Service;
import com.oneclicktech.spring.util.ShopifyUtil;

//@CrossOrigin(origins = "*", maxAge = 3600)
@CrossOrigin(origins = "http://localhost:4200", maxAge = 3600, allowCredentials = "true")
@RestController
@RequestMapping("/pc/support")
public class SupportToolController {

	private static final Logger logger = Logger.getLogger("SupportToolController");

	@Autowired
	RestTemplateService restTemplateService;

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
	TableUtilMapper tableUtilMapper;

	@Autowired
	CustomerMapper customerMapper;

	@Autowired
	ShopOrderMapper shopOrderMapper;

	@Autowired
	SyncD365Service syncD365Service;

	@Autowired
	CronJobService cronJobService;

	@Autowired
	ClientTokenMapper clientTokenMapper;

	@Autowired
	EWTMapper ewtMapper;

	@Autowired
	RestD365Service restD365Service;

	@Value("${pc.shopify.app.webhook.version}")
	String apiVersion;

	@Value("${pc.shopify.app.rowlimit}")
	String rowLimit;

	@Autowired
	EmailService emailService;

	@PostConstruct
	public void init() {

	}



	@PostMapping("/reGenerateSalesOrder")
	public ResponseEntity<Map<String, Object>> reGenerateSalesOrder(@RequestBody Map<String, Object> paramBody)
			throws Throwable {
		logger.info("** SupportToolController >> reGenerateSalesOrder >> [START]");
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
			logger.info("** SupportToolController >> reGenerateSalesOrder >> orderName: " + orderName);
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
							logger.info("** SupportToolController >> reGenerateSalesOrder >> updateResult: "
									+ updateResult);
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
									logger.info("** SupportToolController >> reGenerateSalesOrder >> resultMap: "
											+ resultMap);
								}

							}
						}
					}

				}
			}

		}

		logger.info("** SupportToolController >> reGenerateSalesOrder >> [END]");
		return new ResponseEntity<Map<String, Object>>(resultMap, httpStat);
	}

	@GetMapping("/getWarehouseSwitchList")
	public ResponseEntity<List<Map<String, Object>>> getWarehouseSwitchList() throws Throwable {

		logger.info("** SupportToolController >> getWarehouseSwitchList >> [START]");
		HttpStatus httpStat = HttpStatus.OK;
		String sqlTxt = " select  b.warehouse as 'warehouseCode'   \r\n" + " , b.name as 'warehouseName' \r\n"
				+ ", a.fries_10_kg_flag as '10KgFlag'    \r\n" + ", a.fries_12_kg_flag as '12KgFlag'\r\n"
				+ ", a.shared_fries_flag as 'sharedFriesFlag'\r\n" + ", pd1.item_id as 'firstProductId'  \r\n"
				+ ", pd1.name as 'firstProductName'  \r\n" + ", a.fries_brand1_percent as 'firstProductPercent'  \r\n"
				+ ", pd2.item_id as 'secondProductId'  \r\n" + ", pd2.name as 'secondProductName'  \r\n"
				+ ", a.fries_brand2_percent as 'secondProductPercent'  \r\n" + ", a.update_date as 'lastUpdated'   \r\n"
				+ " from cms_db.config_fries_switch a \r\n"
				+ " join cms_db.warehouse b on b.warehouse = a.warehouse_code\r\n"
				+ " left join cms_db.product_detail pd1 on pd1.item_id = a.fries_brand1_id\r\n"
				+ " left join cms_db.product_detail pd2 on pd2.item_id = a.fries_brand2_id "

				+ " order by b.warehouse, a.fries_10_kg_flag";

		List<Map<String, Object>> dataList = queryBuilderService.getExecQuery(sqlTxt);

		for (Map<String, Object> dataMap : dataList) {
			String str10KgFlag = (String) dataMap.get("10KgFlag");
			boolean fries10KgFlag = (StringUtils.isNotBlank(str10KgFlag) && str10KgFlag.equals("Y")) ? true : false;

			String str12KgFlag = (String) dataMap.get("12KgFlag");
			boolean fries12KgFlag = (StringUtils.isNotBlank(str12KgFlag) && str12KgFlag.equals("Y")) ? true : false;

			String strSharedFriesFlag = (String) dataMap.get("sharedFriesFlag");
			boolean sharedFriesFlag = (StringUtils.isNotBlank(strSharedFriesFlag) && strSharedFriesFlag.equals("Y"))
					? true
					: false;

			dataMap.put("fries10KgFlag", fries10KgFlag);
			dataMap.put("fries10KgLabel", (fries10KgFlag) ? "ON" : "OFF");
			dataMap.put("fries12KgFlag", fries12KgFlag);
			dataMap.put("fries12KgLabel", (fries12KgFlag) ? "ON" : "OFF");
			dataMap.put("sharedFriesFlag", sharedFriesFlag);
			dataMap.put("sharedFriesLabel", (sharedFriesFlag) ? "ON" : "OFF");

			dataMap.put("percentStyleClass", "input-style1");
			int secondProdPercent = (Integer) dataMap.get("secondProductPercent");
			if (secondProdPercent > 1) {
				dataMap.put("percentStyleClass", "input-style2");
			}

		}

		logger.info("** SupportToolController >> getWarehouseSwitchList >> [END]");

		return new ResponseEntity<List<Map<String, Object>>>(dataList, httpStat);
	}
	
	
	
	@GetMapping("/getEWTOrderList")
	public ResponseEntity<List<Map<String, Object>>> getEWTOrderList(@RequestParam("customerNo") String customerNo,
			HttpServletRequest request) throws Throwable {
		Map<String, Object> resultMap = new HashMap<>();
		logger.info("** SupportToolController >> getEWTOrderList >> [START]");
		HttpStatus httpStat = HttpStatus.OK; 
		HashMap<String, Object> searchMap = new HashMap<String, Object>();
		 
		List<Map<String, Object>> ewtOrders = ewtMapper.getOrderEWTList(searchMap);
		for (Map<String, Object> ewtMap: ewtOrders) {
			String disableAcctStr = (String)ewtMap.get("disableAcct");
			boolean acctDisabled = (disableAcctStr!=null && disableAcctStr.equals("Y"))?true:false;
			ewtMap.put("disableAcctStr", disableAcctStr);
			ewtMap.put("acctDisabled", acctDisabled);
			ewtMap.put("acctDisabledLabel", acctDisabled?"ON":"OFF");
	 }
	 
		
		logger.info("** SupportToolController >> getEWTOrderList >> [END]");
		return new ResponseEntity<List<Map<String, Object>>>(ewtOrders, httpStat);
	}
	
	
	@GetMapping("/getConfigsForEWTCustomers")
	public ResponseEntity<Map<String, Object>> getConfigsForEWTCustomers() throws Throwable {
		Map<String, Object> resultMap = new HashMap<>();
		logger.info("** SupportToolController >> getConfigsForEWTCustomers >> [START]");
		HttpStatus httpStat = HttpStatus.OK;
	  
		List<Map<String, Object>> customerEWTList = ewtMapper.getCustomerEWTList(new HashMap<String, Object>());

		for (Map<String, Object> dataMap : customerEWTList) {
			String ewtFlagStr = (String) dataMap.get("ewtFlag");
			boolean ewtFlag = (StringUtils.isNotBlank(ewtFlagStr) && ewtFlagStr.equals("Y")) ? true : false;
			dataMap.put("customerEwtFlag", ewtFlag);
			dataMap.put("ewtFlagLabel", (ewtFlag) ? "ON" : "OFF");
	 	 }

		 
		String sqlDiscount = "SELECT * FROM cms_db.config_discount " + " where discount_type = 'EWT'";
		List<Map<String, Object>> discountEWTList = queryBuilderService.getExecQuery(sqlDiscount);
		for (Map<String, Object> discEwtMap : discountEWTList) {
			logger.info("** SupportToolController >> discEwtMap: " + discEwtMap);
			String key = ((String) discEwtMap.get("discount_name")).replaceAll(" ", "");
			double discountVal = (Double) discEwtMap.get("discount_value");
			resultMap.put(key, discountVal);
		}

		resultMap.put("customerEWTList", customerEWTList); 

		logger.info("** SupportToolController >> getConfigsForEWTCustomers >> [END]");

		return new ResponseEntity<Map<String, Object>>(resultMap, httpStat);
	}
	
	
	

	@PostMapping("/save2ndBrandProduct")
	public ResponseEntity<Map<String, Object>> save2ndBrandProduct(@RequestBody Map<String, Object> paramBody)
			throws Throwable {
		logger.info("** SupportToolController >> save2ndBrandProduct >> [START]");
		logger.info("** SupportToolController >> paramBody: " + paramBody);
		Map<String, Object> resultMap = new HashMap<>();
		HttpStatus httpStat = HttpStatus.OK;
		if (MapUtils.isNotEmpty(paramBody)) {
			Map<String, Object> selectedProduct = (Map<String, Object>) paramBody.get("param1");
			String productId = (String) selectedProduct.get("itemId");
			String updateTxt = "update  cms_db.config_fries_switch set fries_brand2_id = '" + productId + "', "
					+ " fries_brand1_percent=100, fries_brand2_percent=0, " + " update_date = now() ";
			queryBuilderService.execQuery(updateTxt);
		}
		logger.info("** SupportToolController >> save2ndBrandProduct >> [END]");
		return new ResponseEntity<Map<String, Object>>(resultMap, httpStat);
	}

	@PostMapping("/saveSharedPercentData")
	public ResponseEntity<Map<String, Object>> saveSharedPercentData(@RequestBody Map<String, Object> paramBody)
			throws Throwable {
		logger.info("** SupportToolController >> saveSharedPercentData >> [START]");
		logger.info("** SupportToolController >> paramBody: " + paramBody);
		Map<String, Object> resultMap = new HashMap<>();
		HttpStatus httpStat = HttpStatus.OK;
		if (MapUtils.isNotEmpty(paramBody)) {
			Gson gson = new GsonBuilder().setPrettyPrinting().create();

			Map<String, Object> selectedProduct = (Map<String, Object>) paramBody.get("param1");
			String firstProductId = (String) selectedProduct.get("firstProductId");
			String firstProductName = (String) selectedProduct.get("firstProductName");
			String secondProductId = (String) selectedProduct.get("secondProductId");
			String secondProductName = (String) selectedProduct.get("secondProductName");
			String warehouseCode = (String) selectedProduct.get("warehouseCode");
			int firstProdPercent = (Integer) selectedProduct.get("firstProductPercent");
			int secondProdPercent = (Integer) selectedProduct.get("secondProductPercent");

			String alertMsgInfo = new StringBuilder(String.valueOf(firstProdPercent)).append("% for ")
					.append(firstProductName).append(" (").append(firstProductId).append(") and ")
					.append(String.valueOf(secondProdPercent)).append("% for ").append(secondProductName).append(" (")
					.append(secondProductId).append(") ").toString();

			String updateTxt = "update  cms_db.config_fries_switch set shared_fries_flag = 'N', "
					+ " fries_brand1_percent = " + firstProdPercent + ", fries_brand2_percent = " + secondProdPercent
					+ ", " + " update_date = now() " + " where warehouse_code = '" + warehouseCode + "'";
			boolean updated = queryBuilderService.execQuery(updateTxt);
			if (updated) {
				Map<String, Object> rootMap = new LinkedHashMap<String, Object>();
				Map<String, Object> detailMap = new LinkedHashMap<String, Object>();
				String firstKey = new StringBuilder(firstProductId).append("|").append(warehouseCode).toString();

				detailMap.put("namespace", "fries_shared_percent_product1");
				detailMap.put("key", firstKey);
				detailMap.put("value", firstProdPercent);
				detailMap.put("type", "number_integer");
				rootMap.put("metafield", detailMap);
				rootMap.put("metaType", "SHOP");

				logger.info(gson.toJson(rootMap));
				onlineShopService.updateShopMetafield(rootMap);

				rootMap = new LinkedHashMap<String, Object>();
				detailMap = new LinkedHashMap<String, Object>();
				String secondKey = new StringBuilder(secondProductId).append("|").append(warehouseCode).toString();

				detailMap.put("namespace", "fries_shared_percent_product2");
				detailMap.put("key", secondKey);
				detailMap.put("value", secondProdPercent);
				detailMap.put("type", "number_integer");
				rootMap.put("metafield", detailMap);
				rootMap.put("metaType", "SHOP");

				logger.info(gson.toJson(rootMap));
				onlineShopService.updateShopMetafield(rootMap);

				rootMap = new LinkedHashMap<String, Object>();
				detailMap = new LinkedHashMap<String, Object>();

				detailMap.put("namespace", "fries_shared_brand_message_alert");
				detailMap.put("key", warehouseCode);
				detailMap.put("value", alertMsgInfo);
				detailMap.put("type", "single_line_text_field");
				rootMap.put("metafield", detailMap);
				rootMap.put("metaType", "SHOP");

				logger.info(gson.toJson(rootMap));
				onlineShopService.updateShopMetafield(rootMap);
			}
		}
		logger.info("** SupportToolController >> saveSharedPercentData >> [END]");
		return new ResponseEntity<Map<String, Object>>(resultMap, httpStat);
	}

	@PostMapping("/saveEWTDiscountConfig")
	public ResponseEntity<Map<String, Object>> saveEWTDiscountConfig(@RequestBody Map<String, Object> paramBody)
			throws Throwable {
		logger.info("** SupportToolController >> saveEWTDiscountConfig >> [START]");
		logger.info("** SupportToolController >> paramBody: " + paramBody);
		Map<String, Object> resultMap = new HashMap<>();
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		HttpStatus httpStat = HttpStatus.OK;
		if (MapUtils.isNotEmpty(paramBody)) {
			double goodsDiscount = (Double) paramBody.get("param1");
			double deliveryDiscount = (Double) paramBody.get("param2");
			logger.info("** SupportToolController >> goodsDiscount: " + goodsDiscount);
			logger.info("** SupportToolController >> deliveryDiscount: " + deliveryDiscount);
		}
		logger.info("** SupportToolController >> saveEWTDiscountConfig >> [END]");
		return new ResponseEntity<Map<String, Object>>(resultMap, httpStat);
	}

	@PostMapping("/saveEWTFlagConfig")
	public ResponseEntity<Map<String, Object>> saveEWTFlagConfig(@RequestBody Map<String, Object> paramBody)
			throws Throwable {
		logger.info("** SupportToolController >> saveEWTFlagConfig >> [START]");
		logger.info("** SupportToolController >> paramBody: " + paramBody);
		Map<String, Object> resultMap = new HashMap<>();
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		HttpStatus httpStat = HttpStatus.OK;
		if (MapUtils.isNotEmpty(paramBody) && paramBody.get("param1") != null) {
			Map<String, Object> paramMap = (Map<String, Object>) paramBody.get("param1");
			if (MapUtils.isNotEmpty(paramMap)) {
				String customerNo = (String) paramMap.get("customerNo");
				boolean ewtFlag = (Boolean) paramMap.get("customerEwtFlag");
				String ewtFlagStr = (ewtFlag) ? "Y" : "N";
				HashMap<String, Object> updateMap = new HashMap<String, Object>();
				updateMap.put("customerNo", customerNo);
				updateMap.put("ewtFlag", ewtFlagStr);
					
				int updateResult = ewtMapper.updateEWTCustomerConfig(updateMap);
				boolean saved = (updateResult!=0)? true:false;
				if (saved) {
					// SAVE to METAFIELD Shopify
			 		Map<String, Object> rootMap = new LinkedHashMap<String, Object>();
					Map<String, Object> detailMap = new LinkedHashMap<String, Object>();

					detailMap.put("namespace", "customer_ewt");
					detailMap.put("key", customerNo);
					if (ewtFlag) {
						detailMap.put("value", gson.toJson(paramMap, Map.class));
						detailMap.put("type", "json");
					} else {
						detailMap.put("value", "DELETED");
						detailMap.put("type", "single_line_text_field");
					}
					
					rootMap.put("metafield", detailMap);
					rootMap.put("metaType", "SHOP");

					logger.info(gson.toJson(rootMap));
					onlineShopService.updateShopMetafield(rootMap);
					resultMap.putAll(rootMap);

				}
				logger.info("** SupportToolController >> addCustomerToEWT >> saved: " + saved);
			}
		}

		logger.info("** SupportToolController >> saveEWTFlagConfig >> [END]");
		return new ResponseEntity<Map<String, Object>>(resultMap, httpStat);
	}

	@PostMapping("/addCustomerToEWT")
	public ResponseEntity<Map<String, Object>> addCustomerToEWT(@RequestBody Map<String, Object> paramBody)
			throws Throwable {
		logger.info("** SupportToolController >> addCustomerToEWT >> [START]");
		logger.info("** SupportToolController >> paramBody: " + paramBody);
		Map<String, Object> resultMap = new HashMap<>();
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		HttpStatus httpStat = HttpStatus.OK;
		if (MapUtils.isNotEmpty(paramBody) && paramBody.get("param1") != null) {
			Map<String, Object> paramMap = (Map<String, Object>) paramBody.get("param1");
			if (MapUtils.isNotEmpty(paramMap)) {
				String customerNo = (String) paramMap.get("customerNo");
				String queryTxt = "insert into cms_db.config_customer_ewt (customer_no) values ('" + customerNo + "')";
				boolean saved = queryBuilderService.execQuery(queryTxt);
				logger.info("** SupportToolController >> addCustomerToEWT >> saved: " + saved);
			}
		}

		logger.info("** SupportToolController >> addCustomerToEWT >> [END]");
		return new ResponseEntity<Map<String, Object>>(resultMap, httpStat);
	}

	@PostMapping("/saveFriesSwitchConfig")
	public ResponseEntity<Map<String, Object>> saveFriesSwitchConfig(@RequestBody Map<String, Object> paramBody)
			throws Throwable {
		logger.info("** SupportToolController >> saveFriesSwitchConfig >> [START]");
		logger.info("** SupportToolController >> paramBody: " + paramBody);
		Map<String, Object> resultMap = new HashMap<>();
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		HttpStatus httpStat = HttpStatus.OK;
		if (MapUtils.isNotEmpty(paramBody) && paramBody.get("param1") != null) {
			Map<String, Object> dataMap = (Map<String, Object>) paramBody.get("param1");
			if (MapUtils.isNotEmpty(dataMap)) {
				logger.info("** SupportToolController >> dataMap: " + dataMap);
				String activeFriesVariant = "10KG";
				String warehouseCode = (String) dataMap.get("warehouseCode");
				boolean fries10KgFlag = (Boolean) dataMap.get("fries10KgFlag");
				boolean fries12KgFlag = (Boolean) dataMap.get("fries12KgFlag");
				if (fries12KgFlag) {
					activeFriesVariant = "12KG";
				}

				String str10KgFlag = (fries10KgFlag) ? "Y" : "N";
				String str12KgFlag = (fries12KgFlag) ? "Y" : "N";
				String updateTxt = "update cms_db.config_fries_switch set fries_10_kg_flag = '" + str10KgFlag
						+ "',fries_12_kg_flag = '" + str12KgFlag + "', update_date=now() " + " where warehouse_code = '"
						+ warehouseCode + "'";
				boolean updated = queryBuilderService.execQuery(updateTxt);
				logger.info("** SupportToolController >> updated: " + updated);
				if (updated) {
					Map<String, Object> rootMap = new LinkedHashMap<String, Object>();
					Map<String, Object> detailMap = new LinkedHashMap<String, Object>();

					String whKey = StringUtils.trimToEmpty(warehouseCode.replaceAll("[^a-zA-Z0-9]", ""));

					detailMap.put("namespace", "cart_page");
					detailMap.put("key", whKey);
					detailMap.put("value", activeFriesVariant);
					detailMap.put("type", "single_line_text_field");
					rootMap.put("metafield", detailMap);
					rootMap.put("metaType", "SHOP");

					logger.info(gson.toJson(rootMap));
					onlineShopService.updateShopMetafield(rootMap);
					resultMap.putAll(rootMap);
				}
			}

		}

		logger.info("** SupportToolController >> saveFriesSwitchConfig >> [END]");
		return new ResponseEntity<Map<String, Object>>(resultMap, httpStat);
	}

	@PostMapping("/loadCartConfig")
	public ResponseEntity<Map<String, Object>> loadCartConfig(@RequestBody Map<String, Object> paramBody)
			throws Throwable {
		Map<String, Object> resultMap = new HashMap<>();
		HttpStatus httpStat = HttpStatus.OK;
		logger.info("** SupportToolController >> loadCartConfig >> [START]");
		logger.info("** SupportToolController >> loadCartConfig: " + paramBody);
		List<Map<String, Object>> delivConfigList = queryBuilderService
				.getExecQuery("SELECT * FROM cms_db.config_delivery_selection " + "ORDER BY order_seq ASC  ");

		for (Map<String, Object> delivMap : delivConfigList) {
			String delivFlag = (String) delivMap.get("deliv_select_flag");
			boolean deliveryFlag = (StringUtils.isNotBlank(delivFlag) && delivFlag.equals("Y")) ? true : false;
			delivMap.put("deliveryFlag", deliveryFlag);
		}

		List<Map<String, Object>> orderConfigList = queryBuilderService
				.getExecQuery("SELECT * FROM cms_db.config_online_order_selection " + "ORDER BY order_seq ASC  ");

		for (Map<String, Object> orderMap : orderConfigList) {
			String orderSelectFlag = (String) orderMap.get("order_select_flag");
			boolean orderFlag = (StringUtils.isNotBlank(orderSelectFlag) && orderSelectFlag.equals("Y")) ? true : false;
			orderMap.put("orderFlag", orderFlag);
		}

		List<Map<String, Object>> friesRatioList = queryBuilderService
				.getExecQuery("SELECT * FROM cms_db.config_fries_flavor_ratio  ");

		resultMap.put("orderConfigList", orderConfigList);
		resultMap.put("delivConfigList", delivConfigList);
		resultMap.put("friesRatioList", friesRatioList);

		logger.info("** SupportToolController >> loadCartConfig >> [END]");
		return new ResponseEntity<Map<String, Object>>(resultMap, httpStat);
	}

	@PostMapping("/autoAddWarehouseToSwitchList")
	public ResponseEntity<Map<String, Object>> autoAddWarehouseToSwitchList(@RequestBody Map<String, Object> paramBody)
			throws Throwable {
		logger.info("** SupportToolController >> autoAddWarehouseToSwitchList >> [START]");
		Map<String, Object> resultMap = new HashMap<>();
		HttpStatus httpStat = HttpStatus.OK;

		String queryTxt = " insert into cms_db.config_fries_switch (warehouse_code)\r\n"
				+ " select warehouse from cms_db.warehouse\r\n" + " where warehouse not in (\r\n"
				+ " select warehouse_code from  cms_db.config_fries_switch)";
		boolean inserted = queryBuilderService.execQuery(queryTxt);
		if (inserted) {
			resultMap.put("resultMessage", "SUCCESS");
		}

		logger.info("** SupportToolController >> autoAddWarehouseToSwitchList >> [END]");
		return new ResponseEntity<Map<String, Object>>(resultMap, httpStat);
	}

	@PostMapping("/updateDeliverySelection")
	public ResponseEntity<Map<String, Object>> updateDeliverySelection(@RequestBody Map<String, Object> paramBody)
			throws Throwable {
		Map<String, Object> resultMap = new HashMap<>();
		HttpStatus httpStat = HttpStatus.OK;
		logger.info("** SupportToolController >> updateDeliverySelection >> [START]");
		logger.info("** SupportToolController >> paramBody: " + paramBody);

		if (MapUtils.isNotEmpty(paramBody)) {
			Gson gson = new GsonBuilder().setPrettyPrinting().create();

			List<Map<String, Object>> deliveryList = (List<Map<String, Object>>) paramBody.get("param2");
			Map<String, Object> rootMap = new LinkedHashMap<String, Object>();
			Map<String, Object> detailMap = new LinkedHashMap<String, Object>();

			if (CollectionUtils.isNotEmpty(deliveryList)) {
				StringBuilder dbDays = new StringBuilder("(");
				int ctr = 1;
				for (Map<String, Object> delivMap : deliveryList) {

					boolean delivFlag = (Boolean) delivMap.get("deliveryFlag");
					int daySeq = (Integer) delivMap.get("order_seq");
					if (daySeq == 7) {
						daySeq = 0;
					}
					if (!delivFlag) {
						if (ctr == 1) {
							dbDays.append("day != ").append(daySeq);
						} else {
							dbDays.append(" && day != ").append(daySeq);
						}
						ctr++;
					}

					String delivFlagStr = (delivFlag) ? "Y" : "N";
					String delivDayStr = (String) delivMap.get("deliv_day");

					String queryTxt = new StringBuilder(
							"update cms_db.config_delivery_selection set deliv_select_flag='").append(delivFlagStr)
									.append("' , update_date = now() where deliv_day='").append(delivDayStr)
									.append("';").toString();
					queryBuilderService.execQuery(queryTxt);
				}
				dbDays.append(")");

				detailMap.put("namespace", "cart_page");
				detailMap.put("key", "delivery_day_selection");
				detailMap.put("value", dbDays.toString());
				detailMap.put("type", "single_line_text_field");
				// detailMap.put("value", gson.toJson(deliveryList, List.class));
				// detailMap.put("type", "json");

				rootMap.put("metafield", detailMap);
				rootMap.put("metaType", "SHOP");
				onlineShopService.updateShopMetafield(rootMap);
			}

		}

		logger.info("** SupportToolController >> updateDeliverySelection >> [END]");
		return new ResponseEntity<Map<String, Object>>(resultMap, httpStat);
	}

	@PostMapping("/updateOnlineOrderSchedule")
	public ResponseEntity<Map<String, Object>> updateOnlineOrderSchedule(@RequestBody Map<String, Object> paramBody)
			throws Throwable {
		Map<String, Object> resultMap = new HashMap<>();
		HttpStatus httpStat = HttpStatus.OK;
		logger.info("** SupportToolController >> updateOnlineOrderSchedule >> [START]");
		logger.info("** SupportToolController >> paramBody: " + paramBody);

		if (MapUtils.isNotEmpty(paramBody)) {
			Gson gson = new GsonBuilder().setPrettyPrinting().create();

			List<Map<String, Object>> orderSchedList = (List<Map<String, Object>>) paramBody.get("param2");
			Map<String, Object> rootMap = new LinkedHashMap<String, Object>();
			Map<String, Object> detailMap = new LinkedHashMap<String, Object>();

			if (CollectionUtils.isNotEmpty(orderSchedList)) {
				StringBuilder orderDays = new StringBuilder();
				int ctr = 1;
				for (Map<String, Object> orderMap : orderSchedList) {

					boolean orderFlag = (Boolean) orderMap.get("orderFlag");
					String orderFlagStr = (orderFlag) ? "Y" : "N";
					String orderDayStr = StringUtils.trimToEmpty((String) orderMap.get("order_day"));
					if (orderFlag) {
						orderDays.append(orderDayStr).append(" , ");
					}

					String queryTxt = new StringBuilder(
							"update cms_db.config_online_order_selection set order_select_flag='").append(orderFlagStr)
									.append("' , update_date = now() where order_day='").append(orderDayStr)
									.append("';").toString();
					queryBuilderService.execQuery(queryTxt);
				}

				detailMap.put("namespace", "cart_page");
				detailMap.put("key", "online_order_schedule");
				detailMap.put("value", orderDays.toString());
				detailMap.put("type", "single_line_text_field");

				rootMap.put("metafield", detailMap);
				rootMap.put("metaType", "SHOP");
				onlineShopService.updateShopMetafield(rootMap);
			}

		}

		logger.info("** SupportToolController >> updateOnlineOrderSchedule >> [END]");
		return new ResponseEntity<Map<String, Object>>(resultMap, httpStat);
	}

	@PostMapping("/updateFriesFlavorBundle")
	public ResponseEntity<Map<String, Object>> updateFriesFlavorBundle(@RequestBody Map<String, Object> paramBody)
			throws Throwable {
		Map<String, Object> resultMap = new HashMap<>();
		HttpStatus httpStat = HttpStatus.OK;
		logger.info("** SupportToolController >> updateFriesFlavorBundle >> [START]");
		logger.info("** SupportToolController >> updateFriesFlavorBundle: " + paramBody);
		if (MapUtils.isNotEmpty(paramBody)) {
			Gson gson = new GsonBuilder().setPrettyPrinting().create();

			Map<String, Object> bundleRatioMap = (Map<String, Object>) paramBody.get("param2");
			Map<String, Object> rootMap = new LinkedHashMap<String, Object>();
			Map<String, Object> detailMap = new LinkedHashMap<String, Object>();

			if (MapUtils.isNotEmpty(bundleRatioMap)) {

				int minQtyIn10Kg = Integer.parseInt(String.valueOf(bundleRatioMap.get("minQtyIn10Kg")));
				int addInQtyIn10Kg = Integer.parseInt(String.valueOf(bundleRatioMap.get("addInQtyIn10Kg")));
				int minQtyIn12Kg = Integer.parseInt(String.valueOf(bundleRatioMap.get("minQtyIn12Kg")));
				int addInQtyIn12Kg = Integer.parseInt(String.valueOf(bundleRatioMap.get("addInQtyIn12Kg")));

				String queryTxt1 = new StringBuilder("update cms_db.config_fries_flavor_ratio set flavor_min_qty='")
						.append(minQtyIn10Kg).append("' , flavor_addin_qty='").append(addInQtyIn10Kg)
						.append("' , update_date = now() where fries_type='10KG';").toString();
				queryBuilderService.execQuery(queryTxt1);

				String queryTxt2 = new StringBuilder("update cms_db.config_fries_flavor_ratio set flavor_min_qty='")
						.append(minQtyIn12Kg).append("' , flavor_addin_qty='").append(addInQtyIn12Kg)
						.append("' , update_date = now() where fries_type='12KG';").toString();
				queryBuilderService.execQuery(queryTxt2);

				detailMap.put("namespace", "cart_page");
				detailMap.put("key", "flavor_bundle_ratio");
				detailMap.put("value", gson.toJson(bundleRatioMap, Map.class));
				detailMap.put("type", "json");

				rootMap.put("metafield", detailMap);
				rootMap.put("metaType", "SHOP");
				onlineShopService.updateShopMetafield(rootMap);
			}

		}

		logger.info("** SupportToolController >> updateFriesFlavorBundle >> [END]");
		return new ResponseEntity<Map<String, Object>>(resultMap, httpStat);
	}

	@PostMapping("/checkAccountStatus")
	public ResponseEntity<Map<String, Object>> checkAccountStatus(@RequestBody Map<String, Object> paramBody)
			throws Throwable {
		Map<String, Object> resultMap = new HashMap<>();
		HttpStatus httpStat = HttpStatus.OK;
		logger.info("** SupportToolController >> checkAccountStatus >> [START]");
		logger.info("** SupportToolController >> paramBody: " + paramBody);

		logger.info("** SupportToolController >> checkAccountStatus >> [END]");
		return new ResponseEntity<Map<String, Object>>(resultMap, httpStat);
	}
	
	
	
	@PostMapping("/uploadEWTFile")
	public ResponseEntity<Map<String, Object>> uploadEWTFile(@RequestBody Map<String, Object> paramBody)
			throws Throwable {
		Map<String, Object> resultMap = new HashMap<>();
		HttpStatus httpStat = HttpStatus.OK;
		logger.info("** SupportToolController >> checkAccountStatus >> [START]");
		logger.info("** SupportToolController >> paramBody: " + paramBody);

		logger.info("** SupportToolController >> checkAccountStatus >> [END]");
		return new ResponseEntity<Map<String, Object>>(resultMap, httpStat);
	}
	
	
	@PostMapping("/saveCustomerEWTConfig")
	public ResponseEntity<Boolean> saveCustomerEWTConfig(@RequestBody Map<String, Object> paramBody) {
		logger.info("** SupportToolController >> saveCustomerEWTConfig>> [START]");
		HttpStatus httpStat = HttpStatus.OK;
		boolean success = false;
		HashMap<String, Object> paramMap = new HashMap<>();
		paramMap.putAll(paramBody);

		List<Map<String, Object>> customersForSave = ewtMapper.getCustomerForEWTSave(new HashMap<String, Object>());
		if (CollectionUtils.isNotEmpty(customersForSave)) {
			paramMap.put("discountType", "EWT");
			double goodsDiscount = 0D;
			double servicesDiscount = 0D;

			List<Map<String, Object>> ewtDiscountConfigs = ewtMapper.getConfigDiscountList(paramMap);
			for (Map<String, Object> discEwtMap : ewtDiscountConfigs) {
				logger.info("** SupportToolController >> saveCustomerEWTConfig>> discEwtMap: " + discEwtMap);
				String key = ((String) discEwtMap.get("discount_name")).replaceAll(" ", "");
				if (key.equals("GOODS_EWT_DISCOUNT")) {
					goodsDiscount = (Double) discEwtMap.get("discount_value");
				}
				if (key.equals("SERVICE_EWT_DISCOUNT")) {
					servicesDiscount = (Double) discEwtMap.get("discount_value");
				} 
			}

			for (Map<String, Object> customerMap : customersForSave) {
				String customerNo = (String) customerMap.get("customerNo");
				HashMap<String, Object> insertMap = new HashMap<String, Object>();
				insertMap.put("customerNo", customerNo);
				insertMap.put("ewtFlag", 'N');
				insertMap.put("autopayFlag", 'N');
			    insertMap.put("goodsDiscount", goodsDiscount);
				insertMap.put("servicesDiscount", servicesDiscount);
				
				try {
					int insResult = ewtMapper.insertEWTCustomerConfig(insertMap);
					if (insResult!=0) {
						logger.info("** SupportToolController >> saveCustomerEWTConfig>> insResult: " + insResult);
					} 
				} catch (Exception t) {
					logger.log(Level.SEVERE, t.getMessage(), t);
				}
				
			}
		}
		
		logger.info("** SupportToolController >> saveCustomerEWTConfig>> [END]");
		return new ResponseEntity<Boolean>(success, httpStat);
	}
	
	
	@PostMapping("/fileUpload")
	public ResponseEntity<Map<String, Object>> fileUpload(@RequestBody Map<String, Object> paramBody)
			throws Throwable {
		Map<String, Object> resultMap = new HashMap<>();
		HttpStatus httpStat = HttpStatus.OK;
		logger.info("** SupportToolController >> fileUpload >> [START]");
		logger.info("** SupportToolController >> fileUpload: " + paramBody);

		logger.info("** SupportToolController >> fileUpload >> [END]");
		return new ResponseEntity<Map<String, Object>>(resultMap, httpStat);
	}

	
	@PostMapping("/reSyncCustomerAccount")
	public ResponseEntity<Map<String, Object>> reSyncCustomerAccount(@RequestBody Map<String, Object> paramBody)
			throws Throwable {
		Map<String, Object> resultMap = new HashMap<>();
		HttpStatus httpStat = HttpStatus.OK;
		logger.info("** SupportToolController >> reSyncCustomerAccount >> [START]");
		logger.info("** SupportToolController >> paramBody: " + paramBody);

		logger.info("** SupportToolController >> reSyncCustomerAccount >> [END]");
		return new ResponseEntity<Map<String, Object>>(resultMap, httpStat);
	}

	@PostMapping("/combineImageTest")
	public ResponseEntity<Map<String, Object>> combineImageTest(@RequestBody Map<String, Object> paramBody)
			throws Throwable {

		logger.info("** SupportToolController >> combineImageTest >> [START]");
		logger.info("** SupportToolController >> paramBody: " + paramBody);

		HttpStatus httpStat = HttpStatus.OK;
		BufferedImage combineImage = null;
		BufferedImage itemImage = null;
		BufferedImage logoImage = null;
		InputStream itemFileIS = null;
		InputStream logoFileIS = null;
		Map<String, Object> resultMap = new HashMap<>();

		try {

			int iLocX = 150;
			int iLocY = 150;
			String locX = (String) paramBody.get("locX");
			String locY = (String) paramBody.get("locY");
			if (StringUtils.isNotBlank(locX)) {
				iLocX = (Integer.valueOf(locX) / 2);
			}
			if (StringUtils.isNotBlank(locY)) {
				iLocY = (Integer.valueOf(locY) / 2);
			}

			String itemImgUrl = (String) paramBody.get("itemImagePath");
			itemFileIS = new URL(itemImgUrl).openStream();
			itemImage = ImageIO.read(itemFileIS);
			int itemWidth = itemImage.getWidth();
			int itemHeight = itemImage.getHeight();

			String logoImgUrl = (String) paramBody.get("logoImagePath");
			logoFileIS = new URL(logoImgUrl).openStream();
			logoImage = ImageIO.read(logoFileIS);

			combineImage = new BufferedImage(itemWidth, itemHeight, BufferedImage.TYPE_INT_ARGB);
			Map<String, Integer> logoRatio = this.getLogoWidthHeightRatio(itemImage, logoImage, 10D);

			Graphics2D g2d = combineImage.createGraphics();
			g2d.drawImage(itemImage, 0, 0, null);
//			g2d.drawImage(logoImage, 150, 150, (logoImage.getWidth()-100) 
//					,(logoImage.getHeight()-100), null);
			g2d.drawImage(logoImage, iLocX, iLocY, logoRatio.get("logoWidth"), logoRatio.get("logoHeight"), null);

			g2d.dispose();
			try {
				logger.info("** SupportToolController >> combineImageTest >> GENERATING IMAGE.......");
				File imgFileGen = new File("C:\\webserver\\apache-tomcat-9.0.65\\webapps\\test-only\\combineImage.png");
				ImageIO.write(combineImage, "PNG", imgFileGen);
				resultMap.put("FILE_GENERATED", imgFileGen.getAbsoluteFile());
				logger.info("** SupportToolController >> combineImageTest >> SUCCESS..Generated!");
				Thread.sleep(1000);
			} catch (Exception ee) {
				ee.printStackTrace();
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			IOUtils.closeQuietly(itemFileIS);
			IOUtils.closeQuietly(logoFileIS);
		}

		logger.info("** SupportToolController >> combineImageTest >> [END]");
		return new ResponseEntity<Map<String, Object>>(resultMap, httpStat);
	}

	
	
	
	
	private Map<String, Integer> getLogoWidthHeightRatio(BufferedImage itemImage, BufferedImage logoImage,
			double percentage) {
		Map<String, Integer> logoRatio = new HashMap<>();
		int itemWidth = itemImage.getWidth();
		int itemHeight = itemImage.getHeight();
		int realLogoWidth = logoImage.getWidth();
		int realLogoHeight = logoImage.getHeight();

		double percentWidth = (itemWidth * (percentage / 100));
		int logoWidth = Integer.parseInt(String.valueOf(Math.round(percentWidth)));

		double percLogoWidth = ((Double.valueOf(logoWidth) / Double.valueOf(realLogoWidth)) * 100);
		double percentHeight = (realLogoHeight * (percLogoWidth / 100));
		int logoHeight = Integer.parseInt(String.valueOf(Math.round(percentHeight)));

		logoRatio.put("logoWidth", logoWidth);
		logoRatio.put("logoHeight", logoHeight);

		return logoRatio;
	}

	
	
	
	
}
