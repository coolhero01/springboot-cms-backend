package com.oneclicktech.spring.controller;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
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
import com.oneclicktech.spring.service.CustomerService;
import com.oneclicktech.spring.service.OnlineProductService;
import com.oneclicktech.spring.service.OnlineShopService;
import com.oneclicktech.spring.service.ProductService;
import com.oneclicktech.spring.service.QueryBuilderService;
import com.oneclicktech.spring.util.NumberUtil;

@CrossOrigin(origins = "http://localhost:4200", maxAge = 3600, allowCredentials = "true")
@RestController
@RequestMapping("/pc/customer2")
public class CustomerController2 {

	private static final Logger logger = Logger.getLogger("CustomerController2");

	@Autowired
	CustomerService customerService;

	@Autowired
	ProductService productService;

	@Autowired
	QueryBuilderService queryBuilderService;

	@Autowired
	OnlineShopService onlineShopService;

	@Autowired
	OnlineProductService onlineProductService;

	@PostConstruct
	public void init() {

	}

	@GetMapping("/syncJSONDataToDB")
	public ResponseEntity<HttpStatus> syncAzureProductToDB(@RequestParam("syncType") Integer syncType) {
		logger.info("*** CustomerController2 >> syncJSONDataToDB >> [START]");
		boolean success = false;
		try {

//			switch (syncType) {
//			case 1:
//				success = customerService.syncAzureCustomerToLocalDB();
//				break;
//			case 2:
//				success = customerService.syncAzureCustomerAddressToLocalDB();
//				break;
// 			default:
//				break;
//			}

			return new ResponseEntity<>(HttpStatus.OK);
		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
			return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping("/retrieveCustomerList")
	public ResponseEntity<List<Map<String, Object>>> getCustomerList(@RequestBody Map<String, Object> paramBody) {
		HttpStatus httpStat = HttpStatus.OK;
		HashMap<String, Object> paramMap = new HashMap<>();
		paramMap.putAll(paramBody);
		List<Map<String, Object>> dataList = customerService.getCustomerList(paramMap);
		if (CollectionUtils.isNotEmpty(dataList)) {
			logger.info("retrieveCustomerList >> dataList: " + dataList.size());
		}
		return new ResponseEntity<List<Map<String, Object>>>(dataList, httpStat);
	}

	@PostMapping("/retrieveCustomerAddressList")
	public ResponseEntity<List<Map<String, Object>>> retrieveCustomerAddressList(
			@RequestBody Map<String, Object> paramBody) {
		HttpStatus httpStat = HttpStatus.OK;
		HashMap<String, Object> paramMap = new HashMap<>();
		paramMap.putAll(paramBody);
		List<Map<String, Object>> dataList = customerService.getCustomerAddressList(paramMap);

		return new ResponseEntity<List<Map<String, Object>>>(dataList, httpStat);
	}

	@PostMapping("/retrieveStoresByWarehouse")
	public ResponseEntity<List<Map<String, Object>>> retrieveStoresByWarehouse(
			@RequestBody Map<String, Object> paramBody) {
		HttpStatus httpStat = HttpStatus.OK;
		Map<String, Object> paramMap = (Map<String, Object>) paramBody.get("param1");
		HashMap<String, Object> searchMap = new HashMap<>();
		if (MapUtils.isNotEmpty(paramMap)) {
			searchMap.put("warehouse", paramMap.get("warehouse"));
		}

		List<Map<String, Object>> dataList = customerService.getStoresByWarehouse(searchMap);
		if (CollectionUtils.isNotEmpty(dataList)) {
			logger.info("retrieveStoresByWarehouse >> DATALIST: " + dataList.size());
		}
		return new ResponseEntity<List<Map<String, Object>>>(dataList, httpStat);

	}

	@PostMapping("/retrievePromoIssuanceLookup")
	public ResponseEntity<List<Map<String, Object>>> retrievePromoIssuanceLookup(
			@RequestBody Map<String, Object> paramBody) {
		logger.info("*** CustomerController2 >> retrievePromoIssuanceLookup >> [START]");
		HttpStatus httpStat = HttpStatus.OK;
		HashMap<String, Object> paramMap = new HashMap<>();
		String promoType = StringUtils.trimToEmpty((String) paramBody.get("promoType"));
		if (StringUtils.isNotBlank(promoType)) {
			promoType = " AND promo_type = '"+promoType+"' ";
		} 
		logger.info("*** CustomerController2 >>promoType: " + promoType);
		
		paramMap.putAll(paramBody);
		String quertTxt = "SELECT distinct  promo_code as 'promoCode'\r\n" + "  , promo_name as 'promoName'\r\n"
				+ "  , DATE_FORMAT(effect_start_date, '%m/%d/%Y') as 'effectStartDate'\r\n"
				+ "  , DATE_FORMAT(effect_end_date, '%m/%d/%Y') as 'effectEndDate'\r\n"
				+ "  , promo_qty as 'promoQty'\r\n"
				+ "  , DATE_FORMAT(update_date, '%m/%d/%Y %T:%i') as 'updateDate'\r\n"
				+ "FROM cms_db.promo_issuance_lkp " 
				+ " WHERE promo_code is not null " 
				+ promoType
				+ " ORDER BY updateDate desc ";

		List<Map<String, Object>> dataList = queryBuilderService.getExecQuery(quertTxt);
		logger.info("*** CustomerController2 >> retrievePromoIssuanceLookup >> [END]");
		return new ResponseEntity<List<Map<String, Object>>>(dataList, httpStat);
	}
	
	
	@PostMapping("/retrieveTempAutoIssuance")
	public ResponseEntity<List<Map<String, Object>>> retrieveTempAutoIssuance(
			@RequestBody Map<String, Object> paramBody) {
		logger.info("*** CustomerController2 >> retrieveTempAutoIssuance >> [START]");
		HttpStatus httpStat = HttpStatus.OK;
		HashMap<String, Object> paramMap = new HashMap<>();
		paramMap.putAll(paramBody);
		String quertTxt = " SELECT  concat(sai.promo_code, trim(sai.customer_no)) as 'dataId' \r\n" + 
				"      , sai.promo_code as 'promoCode'\r\n" + 
				"     , pil.promo_name as 'promoName'\r\n" + 
				"     , sai.warehouse_code as 'warehouseCode'\r\n" + 
				"     , sai.customer_no as 'customerNo'\r\n" + 
				"     , sai.order_name as 'orderName'\r\n" + 
				"     , ca.store_name as 'storeName'\r\n" + 
				"     , count(sai.item_id) as 'itemCount'\r\n" + 
				"	, DATE_FORMAT(pil.effect_start_date, '%m/%d/%Y')  as 'effectStartDate'\r\n" + 
				"	, DATE_FORMAT(pil.effect_end_date, '%m/%d/%Y') as 'effectEndDate'\r\n" + 
				"	, DATE_FORMAT(sai.issued_date, '%m/%d/%Y') as 'issuedDate'\r\n" + 
			    "    , sai.issued_flag as 'issuedFlag'\r\n" + 
			     "    , sai.oos_enabled as 'oosEnabled'\r\n" +  
				"	, max(sai.update_date) as 'updateDate'\r\n" + 
				"FROM cms_db.temp_auto_issuance sai   \r\n" + 
				"	JOIN cms_db.promo_issuance_lkp pil on pil.promo_code = sai.promo_code \r\n" + 
				"	JOIN cms_db.customer_address ca on ca.customer_number = sai.customer_no\r\n" + 
				"	JOIN cms_db.customer c  on c.customer_number = sai.customer_no \r\n" + 
				"	JOIN cms_db.product_detail pd on pd.item_id = sai.item_id\r\n" + 
				" WHERE sai.promo_code is not null \r\n" + 
				" GROUP BY dataId, promoCode, promoName, warehouseCode, customerNo, orderName, \r\n" + 
				" storeName, effectStartDate, effectEndDate, issuedDate, issuedFlag,  oosEnabled ";

		List<Map<String, Object>> dataList = queryBuilderService.getExecQuery(quertTxt);
	 
		logger.info("*** CustomerController2 >> retrieveTempAutoIssuance >> [END]");
		return new ResponseEntity<List<Map<String, Object>>>(dataList, httpStat);
	}

	
	@PostMapping("/retrieveAutoIssuanceCustomers")
	public ResponseEntity<List<Map<String, Object>>> retrieveAutoIssuanceCustomers(
			@RequestBody Map<String, Object> paramBody) {
		logger.info("*** CustomerController2 >> retrieveAutoIssuanceCustomers >> [START]");
		HttpStatus httpStat = HttpStatus.OK;
		HashMap<String, Object> paramMap = new HashMap<>();
		paramMap.putAll(paramBody);
		String quertTxt = " SELECT  concat(sai.promo_code, trim(sai.customer_no)) as 'dataId' \r\n" + 
				"      , sai.promo_code as 'promoCode'\r\n" + 
				"     , pil.promo_name as 'promoName'\r\n" + 
				"     , sai.warehouse_code as 'warehouseCode'\r\n" + 
				"     , sai.customer_no as 'customerNo'\r\n" + 
				"     , sai.order_name as 'orderName'\r\n" + 
				"     , ca.store_name as 'storeName'\r\n" + 
				"     , count(sai.item_id) as 'itemCount'\r\n" + 
				"	, DATE_FORMAT(pil.effect_start_date, '%m/%d/%Y')  as 'effectStartDate'\r\n" + 
				"	, DATE_FORMAT(pil.effect_end_date, '%m/%d/%Y') as 'effectEndDate'\r\n" + 
				"	, DATE_FORMAT(sai.issued_date, '%m/%d/%Y') as 'issuedDate'\r\n" + 
			    "    , sai.issued_flag as 'issuedFlag'\r\n" + 
			     "    , sai.oos_enabled as 'oosEnabled'\r\n" +  
				"	, max(sai.update_date) as 'updateDate'\r\n" + 
				"FROM cms_db.store_auto_issuance sai   \r\n" + 
				"	JOIN cms_db.promo_issuance_lkp pil on pil.promo_code = sai.promo_code \r\n" + 
				"	JOIN cms_db.customer_address ca on ca.customer_number = sai.customer_no\r\n" + 
				"	JOIN cms_db.customer c  on c.customer_number = sai.customer_no \r\n" + 
				"	JOIN cms_db.product_detail pd on pd.item_id = sai.item_id\r\n" + 
				" WHERE sai.promo_code is not null \r\n" + 
				" GROUP BY dataId, promoCode, promoName, warehouseCode, customerNo, orderName, \r\n" + 
				" storeName, effectStartDate, effectEndDate, issuedDate, issuedFlag,  oosEnabled ";

		List<Map<String, Object>> dataList = queryBuilderService.getExecQuery(quertTxt);
		for (Map<String, Object> dataMap : dataList) {
			String issuedFlagStr = (String) dataMap.get("issuedFlag");
			boolean issuedFlag = (StringUtils.isNotBlank(issuedFlagStr) && issuedFlagStr.equals("Y")) ? true : false;
			dataMap.put("issuedFlag", issuedFlag);
			dataMap.put("issuedFlagLabel", (issuedFlag) ? "TRUE (DONE)" : "FALSE");
			String oosEnabledStr = (String) dataMap.get("oosEnabled");
			boolean oosEnabled = (StringUtils.isNotBlank(oosEnabledStr) && oosEnabledStr.equals("Y")) ? true : false;
			dataMap.put("oosEnabled", oosEnabled);
			dataMap.put("oosEnabledLabel", (oosEnabled) ? "ON (ACTIVE)" : "OFF (INACTIVE)");
			String freeFlagStr = (String) dataMap.get("freeFlag");
			boolean freeFlag = (StringUtils.isNotBlank(freeFlagStr) && freeFlagStr.equals("Y")) ? true : false;
			dataMap.put("freeFlag", freeFlag);
		}
		logger.info("*** CustomerController2 >> retrieveAutoIssuanceCustomers >> [END]");
		return new ResponseEntity<List<Map<String, Object>>>(dataList, httpStat);
	}
	
	@SuppressWarnings("unchecked")
	@PostMapping("/addTempAutoIssuance")
	public ResponseEntity<Boolean> addTempAutoIssuance(@RequestBody Map<String, Object> paramBody) {
		logger.info("*** CustomerController2 >> addTempAutoIssuance >> [START]");
		HttpStatus httpStat = HttpStatus.OK;
		logger.info("*** CustomerController2 >> paramBody: " + paramBody);
		Map<String, Object> dataMap1 = (Map<String, Object>) paramBody.get("param1");// PROMO
		Map<String, Object> dataMap2 = (Map<String, Object>) paramBody.get("param2");// WAREHOUSE
		List<Map<String, Object>> dataMap3 = (List<Map<String, Object>>) paramBody.get("param3");// PRODUCTS
		List<Map<String, Object>> dataMap4 = (List<Map<String, Object>>) paramBody.get("param4");// STORES
		boolean save = false;
		if (MapUtils.isNotEmpty(dataMap1) && MapUtils.isNotEmpty(dataMap2) && CollectionUtils.isNotEmpty(dataMap3)
				&& CollectionUtils.isNotEmpty(dataMap4)) {

			String promoCode = String.valueOf(dataMap1.get("promoCode"));
			String warehouse = String.valueOf(dataMap2.get("warehouse"));

			for (Map<String, Object> storeMap : dataMap4) {
				String storeCustNo = (String) storeMap.get("customerNumber");

				for (Map<String, Object> productMap : dataMap3) {

					String itemId = String.valueOf(productMap.get("itemId"));
					//boolean freeItem = (Boolean)productMap.get("freeFlag");
					//String freeFlagTxt = (freeItem)?"Y":"N";
					String freeFlagTxt = "N";
							
					HashMap<String, Object> searchMap = new HashMap<>();
					searchMap.put("itemNumber", itemId);
					searchMap.put("warehouse", warehouse);
					List<Map<String, Object>> dbProducts = productService.getProductInventoryList(searchMap);
					if (CollectionUtils.isNotEmpty(dbProducts)) {
						Map<String, Object> dbProduct = dbProducts.get(0);
						
						try {
							Long shopProdId = (long) dbProduct.get("shopProdId");
							Map<String, Object> eProduct = onlineProductService.getOneProduct(shopProdId);
							if (MapUtils.isNotEmpty(eProduct)) {
								logger.info( "*** CustomerController2 >> addTempAutoIssuance >> eProduct: " + eProduct);
								List<Map<String, Object>> prodVariants = (List<Map<String, Object>>) eProduct.get("variants");
								Map<String, Object> variantMap = prodVariants.get(0);//FIRST Variant WITH PRICE
								
//								if (freeItem) {
//									try {
//										//Check FREE ITEM variant if exist 
//										Map<String, Object> freeVariantMap = new HashMap<>(); 
//										for (Map<String, Object> varntMap: prodVariants) {
//											String itemTitle = StringUtils.trimToEmpty((String)varntMap.get("title"));
//											if (itemTitle.equals("FREE ITEM")) {
//												freeVariantMap.putAll(varntMap);
//												break;
//											}
//										}	
//										
//										if (MapUtils.isEmpty(freeVariantMap)) {
//											freeVariantMap = onlineProductService.generateFreeVariantByPromo(eProduct);
//											if (MapUtils.isNotEmpty(freeVariantMap)) {
//												variantMap = (Map<String, Object>)freeVariantMap.get("variant");
//											}
//										}
//										
//									} catch (Throwable e) {
//										logger.log(Level.SEVERE, e.getMessage(), e);
//									}
//									
//								}  
								 
								Long variantId = NumberUtil.getLongValue(variantMap, "id");
								logger.info("*** CustomerController2 >> addTempAutoIssuance >> variantId: " + variantId);
								String itemQty = String.valueOf(productMap.get("itemQty"));
								String quertTxt = "insert into cms_db.temp_auto_issuance (promo_code, warehouse_code, customer_no, item_id, item_variant_id, item_prod_id, qty_issuance)\r\n"
										+ "values ('" + promoCode + "' "  
										+ ", '" + warehouse + "'" 
										+ ", '" + storeCustNo + "'"
										+ ", '" + itemId + "'"  
										+ ", '" + variantId + "'"
										+ ", '" + shopProdId + "'" 
									  + ", " + itemQty + ");";

							     save = queryBuilderService.execQuery(quertTxt);
							     logger.info("*** CustomerController2 >> addTempAutoIssuance >> save: " + save);
							}

						} catch (Throwable t) {
							logger.log(Level.SEVERE, t.getMessage(), t);
						}

					}

				}
			}

		}
		logger.info("*** CustomerController2 >> addTempAutoIssuance >> [END]");
		return new ResponseEntity<Boolean>(save, httpStat);
	}
	

	@SuppressWarnings("unchecked")
	@PostMapping("/addAutoIssuanceCustomer")
	public ResponseEntity<Boolean> addAutoIssuanceCustomer(@RequestBody Map<String, Object> paramBody) {
		logger.info("*** CustomerController2 >> addAutoIssuanceCustomer >> [START]");
		HttpStatus httpStat = HttpStatus.OK;
		logger.info("*** CustomerController2 >> paramBody: " + paramBody);
		Map<String, Object> dataMap1 = (Map<String, Object>) paramBody.get("param1");// PROMO
		Map<String, Object> dataMap2 = (Map<String, Object>) paramBody.get("param2");// WAREHOUSE
		List<Map<String, Object>> dataMap3 = (List<Map<String, Object>>) paramBody.get("param3");// PRODUCTS
		List<Map<String, Object>> dataMap4 = (List<Map<String, Object>>) paramBody.get("param4");// STORES
		boolean save = false;
		if (MapUtils.isNotEmpty(dataMap1) && MapUtils.isNotEmpty(dataMap2) && CollectionUtils.isNotEmpty(dataMap3)
				&& CollectionUtils.isNotEmpty(dataMap4)) {

			String promoCode = String.valueOf(dataMap1.get("promoCode"));
			String warehouse = String.valueOf(dataMap2.get("warehouse"));

			for (Map<String, Object> storeMap : dataMap4) {
				String storeCustNo = (String) storeMap.get("customerNumber");

				for (Map<String, Object> productMap : dataMap3) {

					String itemId = String.valueOf(productMap.get("itemId"));
					//boolean freeItem = (Boolean)productMap.get("freeFlag");
					//String freeFlagTxt = (freeItem)?"Y":"N";
					String freeFlagTxt = "N";
							
					HashMap<String, Object> searchMap = new HashMap<>();
					searchMap.put("itemNumber", itemId);
					searchMap.put("warehouse", warehouse);
					List<Map<String, Object>> dbProducts = productService.getProductInventoryList(searchMap);
					if (CollectionUtils.isNotEmpty(dbProducts)) {
						Map<String, Object> dbProduct = dbProducts.get(0);
						
						try {
							Long shopProdId = (long) dbProduct.get("shopProdId");
							Map<String, Object> eProduct = onlineProductService.getOneProduct(shopProdId);
							if (MapUtils.isNotEmpty(eProduct)) {
								logger.info( "*** CustomerController2 >> addAutoIssuanceCustomer >> eProduct: " + eProduct);
								List<Map<String, Object>> prodVariants = (List<Map<String, Object>>) eProduct.get("variants");
								Map<String, Object> variantMap = prodVariants.get(0);//FIRST Variant WITH PRICE
								
//								if (freeItem) {
//									try {
//										//Check FREE ITEM variant if exist 
//										Map<String, Object> freeVariantMap = new HashMap<>(); 
//										for (Map<String, Object> varntMap: prodVariants) {
//											String itemTitle = StringUtils.trimToEmpty((String)varntMap.get("title"));
//											if (itemTitle.equals("FREE ITEM")) {
//												freeVariantMap.putAll(varntMap);
//												break;
//											}
//										}	
//										
//										if (MapUtils.isEmpty(freeVariantMap)) {
//											freeVariantMap = onlineProductService.generateFreeVariantByPromo(eProduct);
//											if (MapUtils.isNotEmpty(freeVariantMap)) {
//												variantMap = (Map<String, Object>)freeVariantMap.get("variant");
//											}
//										}
//										
//									} catch (Throwable e) {
//										logger.log(Level.SEVERE, e.getMessage(), e);
//									}
//									
//								}  
								 
								Long variantId = NumberUtil.getLongValue(variantMap, "id");
								logger.info("*** CustomerController2 >> addAutoIssuanceCustomer >> variantId: " + variantId);
								String itemQty = String.valueOf(productMap.get("itemQty"));
								String quertTxt = "insert into cms_db.store_auto_issuance (promo_code, warehouse_code, customer_no, item_id, item_variant_id, item_prod_id, free_flag, qty_issuance)\r\n"
										+ "values ('" + promoCode + "' " 
										+ ", '" + warehouse + "'" 
										+ ", '" + storeCustNo + "'"
										+ ", '" + itemId + "'"  
										+ ", '" + variantId + "'"
										+ ", '" + shopProdId + "'"
										+ ", '" + freeFlagTxt + "'"
									  + ", " + itemQty + ");";

							     save = queryBuilderService.execQuery(quertTxt);
							     logger.info("*** CustomerController2 >> addAutoIssuanceCustomer >> save: " + save);
							}

						} catch (Throwable t) {
							logger.log(Level.SEVERE, t.getMessage(), t);
						}

					}

				}
			}

		}
		logger.info("*** CustomerController2 >> addAutoIssuanceCustomer >> [END]");
		return new ResponseEntity<Boolean>(save, httpStat);
	}
	
	
	@SuppressWarnings("unchecked")
	@PostMapping("/deletePromoIssuanceLkp")
	public ResponseEntity<Boolean> deletePromoIssuanceLkp(@RequestBody Map<String, Object> paramBody) {
		logger.info("*** CustomerController2 >> deletePromoIssuanceLkp >> [START]");
		HttpStatus httpStat = HttpStatus.OK;
		boolean deleted = false;
		logger.info("*** CustomerController2 >> paramBody: " + paramBody);
	 	Map<String, Object> dataMap = (Map<String, Object>) paramBody.get("param1");
 
		if (MapUtils.isNotEmpty(dataMap)) {
			try {
				String promoCode = String.valueOf(dataMap.get("promoCode"));
			 	String quertTxt = "delete from cms_db.promo_issuance_lkp " 
						+ " WHERE promo_code = '" + promoCode + "'" ;
				deleted = queryBuilderService.execQuery(quertTxt);
				logger.info("*** CustomerController2 >> deleted: " + deleted);
			}  catch (Throwable e) {
	    		logger.log(Level.SEVERE, e.getMessage(), e);	
	    	}
		}
		
		logger.info("*** CustomerController2 >> deletePromoIssuanceLkp >> [END]");
		return new ResponseEntity<Boolean>(deleted, httpStat);
	}
	
	
	@SuppressWarnings("unchecked")
	@PostMapping("/deleteAutoIssuanceCustomer")
	public ResponseEntity<Boolean> deleteAutoIssuanceCustomer(@RequestBody Map<String, Object> paramBody) {
		logger.info("*** CustomerController2 >> deleteAutoIssuanceCustomer >> [START]");
		HttpStatus httpStat = HttpStatus.OK;
		logger.info("*** CustomerController2 >> paramBody: " + paramBody);
		List<Map<String, Object>> dataMapList = (List<Map<String, Object>>) paramBody.get("param1");
		boolean deleted = false;
		if (CollectionUtils.isNotEmpty(dataMapList)) {
			for (Map<String, Object> dataMap : dataMapList) {
				String dataId = String.valueOf(dataMap.get("dataId"));
				String promoCode = String.valueOf(dataMap.get("promoCode"));
				String warehouseCode = String.valueOf(dataMap.get("warehouseCode"));
				String customerNo = String.valueOf(dataMap.get("customerNo"));
				String quertTxt = "delete from cms_db.store_auto_issuance " 
						+ " WHERE promo_code = '" + promoCode + "'"
						+ "   and customer_no = '" + customerNo + "'"
						+ "   and warehouse_code = '" + warehouseCode + "'"; 
					deleted = queryBuilderService.execQuery(quertTxt);
				logger.info("*** CustomerController2 >> dataId: " + dataId + " >> DELETED: " + deleted);
				if (deleted) {
					try {
			    		 
			    		Map<String, Object> rootMap = new LinkedHashMap<String, Object>();
						Map<String, Object> detailMap = new LinkedHashMap<String, Object>(); 
						
						detailMap.put("namespace", "product_auto_issuance"); 
						detailMap.put("key", customerNo);
						detailMap.put("value", "DELETED");
						detailMap.put("type", "single_line_text_field");
						rootMap.put("metafield", detailMap);
						rootMap.put("metaType", "SHOP"); 
						
						onlineShopService.updateShopMetafield(rootMap); 
			    		
			    	} catch (Throwable e) {
			    		logger.log(Level.SEVERE, e.getMessage(), e);	
			    	}
				}
			}
		}
		logger.info("*** CustomerController2 >> deleteAutoIssuanceCustomer >> [END]");
		return new ResponseEntity<Boolean>(deleted, httpStat);
	}

	@SuppressWarnings("unchecked")
	@PostMapping("/publishAutoIssuance")
	public ResponseEntity<Boolean> publishAutoIssuance(@RequestBody Map<String, Object> paramBody) {
		logger.info("*** CustomerController2 >> publishAutoIssuance >> [START]");
		HttpStatus httpStat = HttpStatus.OK;
		logger.info("*** CustomerController2 >> paramBody: " + paramBody);
		List<Map<String, Object>> dataMapList = (List<Map<String, Object>>) paramBody.get("param1");
		boolean success = false;
		
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		
		if (CollectionUtils.isNotEmpty(dataMapList)) {
		 
			for (Map<String, Object> dataMap : dataMapList) {
				
				try {
					logger.info("*** CustomerController2 >> dataMap: " + dataMap);
					String promoCode = String.valueOf(dataMap.get("promoCode"));
					String customerNo = String.valueOf(dataMap.get("customerNo"));
					 
					String quertTxt = "SELECT DISTINCT concat(sai.promo_code, trim(sai.customer_no)) as 'dataId' \r\n" + 
							"     , sai.promo_code as 'promoCode'\r\n" + 
							"     , pil.promo_name as 'promoName'\r\n" + 
							"     , sai.warehouse_code as 'warehouseCode'\r\n" + 
							"     , sai.customer_no as 'customerNo'\r\n" + 
							"     , ca.store_name as 'storeName'\r\n" + 
							"     , sai.item_id as 'itemId'\r\n" + 
							"     , pd.name as 'itemName'\r\n" + 
							"     , sai.item_variant_id as 'itemVariantId'\r\n" + 
							"     , sai.item_prod_id as 'itemProdId'\r\n" + 
						    "     , sai.qty_issuance as 'qtyIssuance'\r\n" + 
						    "	, DATE_FORMAT(pil.effect_start_date, '%Y-%m-%d')  as 'effectStartDate'\r\n" + 
							"	, DATE_FORMAT(pil.effect_end_date, '%Y-%m-%d') as 'effectEndDate'\r\n" + 
							"    , sai.issued_flag as 'issuedFlag'\r\n" +  
							"    , sai.free_flag as 'freeFlag'\r\n" + 
							"    , sai.oos_enabled as 'oosEnabled'\r\n" + 
							"	, sai.update_date as 'updateDate'\r\n" +  
							" FROM cms_db.store_auto_issuance sai   \r\n" + 
							"	JOIN cms_db.promo_issuance_lkp pil on pil.promo_code = sai.promo_code \r\n" + 
							"	JOIN cms_db.customer_address ca on ca.customer_number = sai.customer_no\r\n" + 
							"	JOIN cms_db.customer c  on c.customer_number = sai.customer_no \r\n" + 
							"	JOIN cms_db.product_detail pd on pd.item_id = sai.item_id\r\n" + 
							" WHERE sai.promo_code is not null \r\n" + 
							"	and sai.promo_code = '"+promoCode+"'\r\n" + 
							"	and sai.customer_no = '"+customerNo+"' \r\n" ;
					
					List<Map<String, Object>> itemDetailList = queryBuilderService.getExecQuery(quertTxt);
					if (CollectionUtils.isNotEmpty(itemDetailList)) {
						
						Map<String, Object> itemDetailMap = itemDetailList.get(0); 
						String promoName = String.valueOf(itemDetailMap.get("promoName"));
						String effectStartDate = String.valueOf(itemDetailMap.get("effectStartDate"));
						String effectEndDate = String.valueOf(itemDetailMap.get("effectEndDate"));
 				 		String issuedFlag = String.valueOf(itemDetailMap.get("issuedFlag"));
 				 		String oosEnabled = String.valueOf(itemDetailMap.get("oosEnabled"));
 				 
 					 		
				 		StringBuilder metaValue = new StringBuilder("PROMO_").append(promoName)
				 				.append("|").append("CNO_").append(customerNo)
				 				.append("|").append("EFFC_START_").append(effectStartDate)
				 				.append("|").append("EFFC_END_").append(effectEndDate)
				 				.append("|").append("ISSUE_FLG_").append(issuedFlag)
				 				.append("|").append("OOS_ENBL_").append(oosEnabled)
				 				.append("|").append("ITMLINE_");
					 	for (Map<String, Object> itemLineMap : itemDetailList) {
					 		
					 		String itemId = String.valueOf(itemLineMap.get("itemId"));
							String itemName = String.valueOf(itemLineMap.get("itemName")).replaceAll("'", "")
										.replaceAll("\"", "");
							String itemVariantId = String.valueOf(itemLineMap.get("itemVariantId"));
							String itemProdId = String.valueOf(itemLineMap.get("itemProdId"));
						    String qty = String.valueOf(itemLineMap.get("qtyIssuance"));
					 		metaValue.append(itemName).append(" (").append(itemId).append(")")
								.append("=").append(itemProdId) 
								.append("=").append(itemVariantId)
						 		.append("=").append(qty).append(":");
					 	   }
					 	 
					 	logger.info("metaValue: " + metaValue) ;
						Map<String, Object> rootMap = new LinkedHashMap<String, Object>();
						Map<String, Object> detailMap = new LinkedHashMap<String, Object>();
						
						detailMap.put("namespace", "product_auto_issuance");
						detailMap.put("key", customerNo);
						detailMap.put("value", metaValue);
						detailMap.put("type", "single_line_text_field");
						rootMap.put("metafield", detailMap);
						rootMap.put("metaType", "SHOP");

						logger.info(gson.toJson(rootMap));
						onlineShopService.updateShopMetafield(rootMap); 
						
						String updateTxt = "update cms_db.store_auto_issuance set oos_enabled = 'Y' "
								+ " WHERE promo_code = '"+promoCode+"'  \r\n" + 
								"	and  customer_no = '"+customerNo+"' \r\n" ;
					    queryBuilderService.execQuery(updateTxt);
						success = true; 
					} 
		 		
				} catch (Throwable t) {
					success = false;
					logger.log(Level.SEVERE, t.getMessage(), t);
				}

			}
		}
		logger.info("*** CustomerController2 >> publishAutoIssuance >> [END]");
		return new ResponseEntity<Boolean>(success, httpStat);
	}

	@PostMapping("/addPromoIssuance")
	public ResponseEntity<Boolean> addPromoIssuance(@RequestBody Map<String, Object> paramBody) {
		logger.info("*** CustomerController2 >> addPromoIssuance >> [START]");
		HttpStatus httpStat = HttpStatus.OK;
		logger.info("*** CustomerController2 >> paramBody: " + paramBody);
		Map<String, Object> dataMap = (Map<String, Object>) paramBody.get("param1");
		boolean save = false;
		if (MapUtils.isNotEmpty(dataMap)) {

			String promoName = (String) dataMap.get("promoName");
			String promoType = (String) dataMap.get("promoType");
		    String effectStartDate = (String) dataMap.get("effectStartDate");
			String effectEndDate = (String) dataMap.get("effectEndDate"); 

			String quertTxt = "insert into cms_db.promo_issuance_lkp (promo_name, effect_start_date, effect_end_date, promo_type) \r\n"
					+ " values ('" + promoName + "', " + " STR_TO_DATE('" + effectStartDate + "', '%m/%d/%Y'), "
					+ " STR_TO_DATE('" + effectEndDate + "', '%m/%d/%Y'), '"+promoType+"' );";

			save = queryBuilderService.execQuery(quertTxt);
			logger.info("*** CustomerController2 >> save: " + save);
		}
		logger.info("*** CustomerController2 >> addPromoIssuance >> [END]");
		return new ResponseEntity<Boolean>(save, httpStat);
	}

	@PostMapping("/retrieveAutoPayList")
	public ResponseEntity<List<Map<String, Object>>> retrieveAutoPayList(@RequestBody Map<String, Object> paramBody) {
		HttpStatus httpStat = HttpStatus.OK;
		HashMap<String, Object> paramMap = new HashMap<>();
		paramMap.putAll(paramBody);
		String quertTxt = "select distinct c.full_name , \r\n" + "   c.email ,\r\n" + "  cap.order_tag, \r\n"
				+ "   c.oos_include\r\n" + " from cms_db.customer_auto_pay cap\r\n"
				+ " join cms_db.customer c on c.email = cap.email\r\n" + " where  c.oos_include = 'Y'";

		List<Map<String, Object>> dataList = queryBuilderService.getExecQuery(quertTxt);

		return new ResponseEntity<List<Map<String, Object>>>(dataList, httpStat);
	}

	@PostMapping("/retrieveAutoPayTags")
	public ResponseEntity<List<Map<String, Object>>> retrieveAutoPayTags(@RequestBody Map<String, Object> paramBody) {
		HttpStatus httpStat = HttpStatus.OK;
		HashMap<String, Object> paramMap = new HashMap<>();
		paramMap.putAll(paramBody);
		String quertTxt = "select order_tag " + " from cms_db.customer_order_tags";

		List<Map<String, Object>> dataList = queryBuilderService.getExecQuery(quertTxt);

		return new ResponseEntity<List<Map<String, Object>>>(dataList, httpStat);
	}

	@PostMapping("/addAutoPay")
	public ResponseEntity<Boolean> addAutoPay(@RequestBody Map<String, Object> paramBody) {
		HttpStatus httpStat = HttpStatus.OK;
		HashMap<String, Object> paramMap = new HashMap<>();
		paramMap.putAll(paramBody);
		boolean success = false;
		String email = StringUtils.trimToEmpty((String) paramMap.get("email"));
		String tag = StringUtils.trimToEmpty((String) paramMap.get("order_tag"));
		if (StringUtils.isNotBlank(email)) {
			String quertTxt = "insert into cms_db.customer_auto_pay (email,order_tag,status) values ('" + email + "','"
					+ tag + "','ACTIVE');";
			success = queryBuilderService.execQuery(quertTxt);
		}

		return new ResponseEntity<Boolean>(success, httpStat);
	}

	@PostMapping("/deleteAutoPay")
	public ResponseEntity<Boolean> deleteAutoPay(@RequestBody List<Map<String, Object>> paramList) {
		HttpStatus httpStat = HttpStatus.OK;

		boolean success = false;
		for (Map<String, Object> delMap : paramList) {
			String email = StringUtils.trimToEmpty((String) delMap.get("email"));
			if (StringUtils.isNotBlank(email)) {
				String quertTxt = "delete from cms_db.customer_auto_pay where email ='" + email + "';";
				success = queryBuilderService.execQuery(quertTxt);
			}
		}

		return new ResponseEntity<Boolean>(success, httpStat);
	}

}
