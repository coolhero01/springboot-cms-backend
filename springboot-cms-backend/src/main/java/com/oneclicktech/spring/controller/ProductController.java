package com.oneclicktech.spring.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
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
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.http.impl.cookie.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
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
import com.oneclicktech.spring.mapper.IssuanceMapper;
import com.oneclicktech.spring.service.CustomerService;
import com.oneclicktech.spring.service.OnlineOrderService;
import com.oneclicktech.spring.service.OnlineProductService;
import com.oneclicktech.spring.service.OnlineShopService;
import com.oneclicktech.spring.service.ProductService;
import com.oneclicktech.spring.service.QueryBuilderService;
import com.oneclicktech.spring.service.SyncD365Service;
import com.oneclicktech.spring.util.DateUtil;
import com.oneclicktech.spring.util.NumberUtil;
import com.oneclicktech.spring.util.PCDataUtil;
import com.oneclicktech.spring.util.ShopifyUtil;

@CrossOrigin(origins = "http://localhost:4200", maxAge = 3600, allowCredentials = "true")
@RestController
@RequestMapping("/pc/product")
public class ProductController {

	private static final Logger logger = Logger.getLogger("ProductController");

	@Autowired
	ProductService productService;

	@Autowired
	QueryBuilderService queryBuilderService;

	@Autowired
	IssuanceMapper issuanceMapper;

	@Autowired
	OnlineProductService onlineProductService;

	@Autowired
	OnlineShopService onlineShopService;

	@Autowired
	OnlineOrderService onlineOrderService;

	@Autowired
	CustomerService customerService;

	@Autowired
	SyncD365Service syncD365Service;

	@PostConstruct
	public void init() {

	}

	@GetMapping("/syncJSONDataToDB")
	public ResponseEntity<HttpStatus> syncAzureProductToDB(@RequestParam("syncType") Integer syncType) {

		boolean success = false;
		try {

			switch (syncType) {
			case 1:
				success = productService.syncAzureProductToLocalDB();
				break;
			case 2:
				success = productService.syncAzureProductDetailToLocalDB();
				break;
			case 3:
				success = productService.syncAzureProductInventoryToLocalDB();
				break;
			case 4:
				success = productService.syncAzureWarehouseToLocalDB();
				break;

			default:
				break;
			}

			return new ResponseEntity<>(HttpStatus.OK);
		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
			return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping("/retrieveProductList")
	public ResponseEntity<List<Map<String, Object>>> getProductList(@RequestBody Map<String, Object> paramBody) {
		HttpStatus httpStat = HttpStatus.OK;
		HashMap<String, Object> paramMap = new HashMap<>();
		paramMap.putAll(paramBody);
		List<Map<String, Object>> dataList = productService.getProductList(paramMap);

		return new ResponseEntity<List<Map<String, Object>>>(dataList, httpStat);
	}

	@PostMapping("/retrieveProductsByPromo")
	public ResponseEntity<List<Map<String, Object>>> retrieveProductsByPromo(
			@RequestBody Map<String, Object> paramBody) {
		logger.info("*** ProductController >> retrieveProductsByPromo >> [START]");
		HttpStatus httpStat = HttpStatus.OK;
		Map<String, Object> paramMap = (Map<String, Object>) paramBody.get("param1");
		HashMap<String, Object> searchMap = new HashMap<>();
		List<Map<String, Object>> dataList = null;
		if (MapUtils.isNotEmpty(paramMap)) {
			String promoCode = String.valueOf(paramMap.get("promoCode"));
			String warehouseCode = String.valueOf(paramMap.get("warehouseCode"));
			String customerNo = String.valueOf(paramMap.get("customerNo"));

			String quertTxt = "SELECT DISTINCT pd.name as 'itemName'\r\n" + "	, sai.item_id as 'itemId'\r\n"
					+ "    , sai.qty_issuance as 'promoQty'\r\n" + "    , sai.free_flag as 'freeFlag'\r\n"
					+ "    , pi.available_physical as 'availInventory'\r\n" + " FROM cms_db.store_auto_issuance sai\r\n"
					+ " JOIN cms_db.product_detail pd on sai.item_id = pd.item_id\r\n"
					+ " LEFT JOIN cms_db.product_inventory pi on sai.item_id = pi.item_number\r\n"
					+ "	 and sai.warehouse_code = pi.warehouse\r\n" + " WHERE sai.item_id is not null \r\n"
					+ "   and promo_code = '" + promoCode + "' \r\n" + "   and customer_no = '" + customerNo + "' ";

			dataList = queryBuilderService.getExecQuery(quertTxt);
			for (Map<String, Object> dataMap : dataList) {
				boolean freeFlag = (dataMap.get("freeFlag") != null
						&& String.valueOf(dataMap.get("freeFlag")).equals("Y")) ? true : false;
				dataMap.put("freeItem", freeFlag);
			}
		}

		logger.info("*** ProductController >> retrieveProductsByPromo >> [END]");
		return new ResponseEntity<List<Map<String, Object>>>(dataList, httpStat);
	}

	@PostMapping("/retrieveProductsByStaggeredIssuance")
	public ResponseEntity<List<Map<String, Object>>> retrieveProductsByStaggeredIssuance(
			@RequestBody Map<String, Object> paramBody) {
		logger.info("*** ProductController >> retrieveProductsByStaggeredIssuance >> [START]");
		HttpStatus httpStat = HttpStatus.OK;
		Map<String, Object> paramMap = (Map<String, Object>) paramBody.get("param1");
		HashMap<String, Object> searchMap = new HashMap<>();
		List<Map<String, Object>> dataList = null;
		if (MapUtils.isNotEmpty(paramMap)) {
			String promoCode = String.valueOf(paramMap.get("promoCode"));
			String customerNo = String.valueOf(paramMap.get("customerNo"));
			searchMap.put("promoCode", promoCode);
			searchMap.put("customerNo", customerNo);
			dataList = issuanceMapper.getProductsForStaggeredIssuance(searchMap);
		}

		logger.info("*** ProductController >> retrieveProductsByStaggeredIssuance >> [END]");
		return new ResponseEntity<List<Map<String, Object>>>(dataList, httpStat);
	}

	@PostMapping("/retrieveProductDetailList")
	public ResponseEntity<List<Map<String, Object>>> retrieveProductDetailList(
			@RequestBody Map<String, Object> paramBody) {
		HttpStatus httpStat = HttpStatus.OK;
		HashMap<String, Object> paramMap = new HashMap<>();
		paramMap.putAll(paramBody);
		List<Map<String, Object>> dataList = productService.getProductDetailList(paramMap);

		return new ResponseEntity<List<Map<String, Object>>>(dataList, httpStat);
	}

	@PostMapping("/retrieveProductListWithQty")
	public ResponseEntity<List<Map<String, Object>>> retrieveProductListWithQty(
			@RequestBody Map<String, Object> paramBody) {
		HttpStatus httpStat = HttpStatus.OK;
		HashMap<String, Object> paramMap = new HashMap<>();
		paramMap.putAll(paramBody);

		String quertTxt = "SELECT DISTINCT pd.item_id as 'itemId'\r\n" + "	, pd.name as 'itemName'\r\n"
				+ "    , concat(pd.item_id,'-',pd.name) as  'idWithName'\r\n" + "    , 0 as  'itemQty'\r\n"
				+ " FROM cms_db.product_detail pd\r\n"
				+ " JOIN cms_db.product_inventory pi on pd.item_id = pi.item_number\r\n"
				+ " WHERE  pi.shop_prod_id is not null ";

		List<Map<String, Object>> dataList = queryBuilderService.getExecQuery(quertTxt);
		for (Map<String, Object> prodMap : dataList) {
			prodMap.put("freeFlag", false);
		}

		return new ResponseEntity<List<Map<String, Object>>>(dataList, httpStat);
	}

	@PostMapping("/retrieveProductInventory")
	public ResponseEntity<List<Map<String, Object>>> retrieveProductInventory(
			@RequestBody Map<String, Object> paramBody) {
		HttpStatus httpStat = HttpStatus.OK;
		HashMap<String, Object> paramMap = new HashMap<>();
		paramMap.putAll(paramBody);
		List<Map<String, Object>> dataList = productService.getProductInventoryList(paramMap);

		return new ResponseEntity<List<Map<String, Object>>>(dataList, httpStat);
	}

	@PostMapping("/retrieveProductByWarehouse")
	public ResponseEntity<List<Map<String, Object>>> retrieveProductByWarehouse(
			@RequestBody Map<String, Object> paramBody) {
		HttpStatus httpStat = HttpStatus.OK;
		HashMap<String, Object> paramMap = new HashMap<>();
		paramMap.putAll(paramBody);
		List<Map<String, Object>> dataList = issuanceMapper.getProductsByWarehouse(paramMap);

		return new ResponseEntity<List<Map<String, Object>>>(dataList, httpStat);
	}

	@PostMapping("/retrieveWarehouseList")
	public ResponseEntity<List<Map<String, Object>>> retrieveWarehouseList(@RequestBody Map<String, Object> paramBody) {
		HttpStatus httpStat = HttpStatus.OK;
		HashMap<String, Object> paramMap = new HashMap<>();
		paramMap.putAll(paramBody);
		List<Map<String, Object>> dataList = productService.getWarehouseList(paramMap);

		return new ResponseEntity<List<Map<String, Object>>>(dataList, httpStat);
	}

	@PostMapping("/updateStaggeredPayIssuance")
	public ResponseEntity<Boolean> updateStaggeredPayIssuance(@RequestBody Map<String, Object> paramBody) {
		logger.info("*** ProductController >> updateStaggeredPayIssuance >> [START]");
		HttpStatus httpStat = HttpStatus.OK;
		logger.info("*** ProductController >> paramBody: " + paramBody);
		boolean save = false;
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		Map<String, Object> dataMap = (Map<String, Object>) paramBody.get("param1");
		if (MapUtils.isNotEmpty(dataMap)) {
			logger.info("*** ProductController >> dataMap: " + dataMap);
			HashMap<String, Object> paramMap = new HashMap<>();
			int promoCode = (Integer)dataMap.get("promoCode");
			String customerNo = (String) dataMap.get("customerNo");
			String staggeredSchedId = (String) dataMap.get("stgrdSchedId");
			String orderName = (String) dataMap.get("orderName");
			double interestRate = NumberUtil.getDoubleValue(dataMap, "interestRate");
			
			paramMap.put("promoCode", promoCode);
			paramMap.put("customerNo", customerNo);
			paramMap.put("stgrdSchedId", staggeredSchedId);
			try {
				// Date issueDate = DateUtil.stringToDate((String) dataMap.get("dateIssued"),
				// "MM/dd/yyyy");
				// String dateIssuedStr = DateUtils.parseDate((String)
				// dataMap.get("dateIssuedStr"));

				// Date issueDate = DateUtil.stringToDate((String) dataMap.get("dateIssuedStr"),
				// "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
				Date issueDate = DateUtil.stringToDate((String) dataMap.get("dateIssuedStr"), "MM/dd/yyyy");
				String issuedFlag = (dataMap.get("issuedFlagTxt") != null
						&& ((String) dataMap.get("issuedFlagTxt")).equals("true")) ? "Y" : "N";
				String oosEnabled = (dataMap.get("oosEnabledTxt") != null
						&& ((String) dataMap.get("oosEnabledTxt")).equals("true")) ? "Y" : "N";
				paramMap.put("orderName", dataMap.get("orderName"));
				paramMap.put("dateIssued", issueDate);
				paramMap.put("issuedFlag", issuedFlag);
				paramMap.put("oosEnabled", oosEnabled);
				paramMap.put("finalPayStatus", dataMap.get("finalPayStatus"));

				int updateResult = issuanceMapper.updateStaggeredIssuanceMaster(paramMap);
				if (updateResult != 0) {
					save = true;
				}

				if (save) {
					try {
						Map<String, Object> rootMap = new LinkedHashMap<String, Object>();
						Map<String, Object> detailMap = new LinkedHashMap<String, Object>();

						detailMap.put("namespace", "staggered_pay_issuance");
						detailMap.put("key", customerNo);
						detailMap.put("value", "DELETED");
						detailMap.put("type", "single_line_text_field");
						rootMap.put("metafield", detailMap);
						rootMap.put("metaType", "SHOP");

						onlineShopService.updateShopMetafield(rootMap);

						StringBuilder staggeredTag = new StringBuilder();
						staggeredTag.append("STAGGERED_PAY_ORDER").append(",");
						HashMap<String, Object> searchSchedMap = new HashMap<>();
						searchSchedMap.put("stgrdSchedId", staggeredSchedId);
						List<Map<String, Object>> schedList = issuanceMapper.getStaggeredSchedList(searchSchedMap);
						
						
						if (CollectionUtils.isNotEmpty(schedList)) {
							logger.info( "*** ProductController >> updateStaggeredPayIssuance >> orderName: " + orderName);
							Map<String, Object> eOrderMap = onlineOrderService.getOneOrderByName(orderName);
							
							if (MapUtils.isNotEmpty(eOrderMap)) {

								int payTerms = schedList.size();
								// Only get SUBTOTAL_PRICE because interest rate is applicable to ITEMS ONLY
								double totalOrderAmt = Double
										.parseDouble(String.valueOf(eOrderMap.get("subtotal_price")));
								double orderInterest = PCDataUtil.computeInterestValue(totalOrderAmt, interestRate);
								double shippingAmt = ShopifyUtil.getTotalShippingAmount(eOrderMap);

								totalOrderAmt = (totalOrderAmt + shippingAmt + orderInterest);
								double totalOrderPlusInterest = NumberUtil.roundTwoDec(totalOrderAmt);

								double splitPayAmt = NumberUtil.roundTwoDec((totalOrderAmt / payTerms));
								boolean paySchedUpdated = false;
								Date schedDate = new Date(issueDate.getTime());
								int schedRow = 1;
								double splitPaymentTotal = 0;

								for (Map<String, Object> schedMap : schedList) {
									int schedPayId = NumberUtil.getIntValue(schedMap, "stgrdPayId");
									schedDate = DateUtil.getDateNowPlusTime(schedDate, Calendar.MONTH, 1);

									// This will enable the Exact payment VS total order amount

									if (schedRow == payTerms) {
										// LAST Payment for staggered
										double lastPayment = (totalOrderPlusInterest - splitPaymentTotal);
										logger.info("*** ShopWebhookController >> orderCreationHook >> lastPayment: "
												+ lastPayment);
										splitPayAmt = NumberUtil.roundTwoDec(lastPayment);
									}

									HashMap<String, Object> updSchedMap = new HashMap<>();
									updSchedMap.put("stgrdPayId", schedPayId);
									updSchedMap.put("stgrdSchedId", staggeredSchedId);
									updSchedMap.put("schedPayDate", schedDate);
									updSchedMap.put("amountToPay", splitPayAmt);
									updSchedMap.put("payStatus", "pending");

									int schedResult = issuanceMapper.updateStaggeredIssuanceSched(updSchedMap);
									if (schedResult != 0) {
										logger.info("*** ShopWebhookController >> orderCreationHook >> schedResult: "
												+ schedResult);
										paySchedUpdated = true;
									}

									splitPaymentTotal += splitPayAmt;
									schedRow++;
								}

								/*
								 * FIRST PAYMENT META This meta will pop the payment schedule for Staggered and
								 * prevent users from ordering unless settled
								 */
								if (paySchedUpdated) {
									HashMap<String, Object> nxtMap = new HashMap<>();
									nxtMap.put("orderName", orderName);
									nxtMap.put("payStatus", "pending");
									List<Map<String, Object>> nextPayDetails = issuanceMapper.getNextPaymentDetails(nxtMap);
									if (CollectionUtils.isNotEmpty(nextPayDetails)) {
										Map<String, Object> nextPayMap = nextPayDetails.get(0);
										List<HashMap<String, String>> itemDetails = new ArrayList<>();
										for (Map<String, Object> nxtPayDtl : nextPayDetails) {
											HashMap<String, String> itemDtl = new HashMap<>();
											itemDtl.put("itemId", (String) nxtPayDtl.get("itemId"));
											itemDtl.put("itemName", (String) nxtPayDtl.get("itemName"));
											itemDtl.put("itemQty", String.valueOf(nxtPayDtl.get("itemQty")));
											itemDetails.add(itemDtl);
										}
										nextPayMap.put("itemDetails", itemDetails);

										if (nextPayMap.get("nextPayDate") != null) {
											LocalDateTime nextPayDateTime = (LocalDateTime) nextPayMap
													.get("nextPayDate");
											Date nextPayDate = DateUtil.convertToDateViaSqlTimestamp(nextPayDateTime);
											String nextPayDateStr = DateUtil.getDateWithPattern(nextPayDate,
													"MM/dd/yyyy");
											nextPayMap.put("nextPayDateStr", nextPayDateStr);
										}

										try {
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
								
								
								try {
									//TAG Order as STAGGERED_PAY_ORDER
									onlineOrderService.addOrderTagToCurrent(eOrderMap, staggeredTag.toString());
									
									//Generate SO
									Map<String, Object> soMap = new HashMap<>();
									soMap.put("processPerOrder", true);
									soMap.put("orderName", orderName);
									soMap.put("staggeredPayOrder", true);
									syncD365Service.syncSalesOrderDataToDB(soMap);
								} catch (Throwable tt) {
									logger.log(Level.SEVERE, tt.getMessage(), tt);
								}
							}
						}
						
						

					} catch (Throwable e) {
						logger.log(Level.SEVERE, e.getMessage(), e);
					}

				}

			} catch (Exception e) {
				logger.log(Level.SEVERE, e.getMessage(), e);
			}

		}
		logger.info("*** ProductController >> updateStaggeredPayIssuance >> [END]");
		return new ResponseEntity<Boolean>(save, httpStat);
	}

	@PostMapping("/updateStaggeredPaySched")
	public ResponseEntity<Boolean> updateStaggeredPaySched(@RequestBody Map<String, Object> paramBody) {
		logger.info("*** ProductController >> updateStaggeredPaySched >> [START]");
		HttpStatus httpStat = HttpStatus.OK;
		logger.info("*** ProductController >> paramBody: " + paramBody);
		boolean save = false;
		Map<String, Object> dataMap1 = (Map<String, Object>) paramBody.get("param1");
		List<Map<String, Object>> dataMap2 = (List<Map<String, Object>>) paramBody.get("param2");
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		if (MapUtils.isNotEmpty(dataMap1) && CollectionUtils.isNotEmpty(dataMap2)) {
			for (Map<String, Object> schedMap : dataMap2) {
				HashMap<String, Object> updateMap = new HashMap<>();
				updateMap.putAll(schedMap);
				if (schedMap.get("schedPayDateStr") != null) {
					Date schedPayDate = DateUtil.stringToDate(String.valueOf(schedMap.get("schedPayDateStr")),
							"MM/dd/yyyy");
					updateMap.put("schedPayDate", schedPayDate);
				}
				if (schedMap.get("actualPayDateStr") != null) {
					Date actualPayDate = DateUtil.stringToDate(String.valueOf(schedMap.get("actualPayDateStr")),
							"MM/dd/yyyy");
					updateMap.put("actualPayDate", actualPayDate);
				}

				int updResult = issuanceMapper.updateStaggeredIssuanceSched(updateMap);
				if (updResult != 0) {
					save = true;
				}

				logger.info("*** ProductController >> updateStaggeredPaySched: " + updResult);
			}

			if (save) {
				String orderName = (String) dataMap1.get("orderName");
				String customerNo = (String) dataMap1.get("customerNo");
				HashMap<String, Object> nxtMap = new HashMap<>();
				nxtMap.put("orderName", orderName);
				nxtMap.put("payStatus", "pending");
				List<Map<String, Object>> nextPayDetails = issuanceMapper.getNextPaymentDetails(nxtMap);
				if (CollectionUtils.isNotEmpty(nextPayDetails)) {
					Map<String, Object> nextPayMap = nextPayDetails.get(0);
					List<HashMap<String, String>> itemDetails = new ArrayList<>();
					for (Map<String, Object> nxtPayDtl : nextPayDetails) {
						HashMap<String, String> itemDtl = new HashMap<>();
						itemDtl.put("itemId", (String) nxtPayDtl.get("itemId"));
						itemDtl.put("itemName", (String) nxtPayDtl.get("itemName"));
						itemDtl.put("itemQty", String.valueOf(nxtPayDtl.get("itemQty")));
						itemDetails.add(itemDtl);
					}
					nextPayMap.put("itemDetails", itemDetails);

					if (nextPayMap.get("nextPayDate") != null) {
						LocalDateTime nextPayDateTime = (LocalDateTime) nextPayMap.get("nextPayDate");
						Date nextPayDate = DateUtil.convertToDateViaSqlTimestamp(nextPayDateTime);
						String nextPayDateStr = DateUtil.getDateWithPattern(nextPayDate, "MM/dd/yyyy");
						nextPayMap.put("nextPayDateStr", nextPayDateStr);
					}

					try {
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

		}
		logger.info("*** ProductController >> updateStaggeredPaySched >> [END]");
		return new ResponseEntity<Boolean>(save, httpStat);
	}

	@SuppressWarnings("unchecked")
	@PostMapping("/addStaggeredPayIssuance")
	public ResponseEntity<Boolean> addStaggeredPayIssuance(@RequestBody Map<String, Object> paramBody) {
		logger.info("*** ProductController >> addStaggeredPayIssuance >> [START]");
		HttpStatus httpStat = HttpStatus.OK;
		HashMap<String, Object> paramMap = new HashMap<>();
		paramMap.putAll(paramBody);
		logger.info("*** ProductController >> paramBody: " + paramBody);
		boolean save = false;
		Map<String, Object> dataMap1 = (Map<String, Object>) paramBody.get("param1");// PROMO
		// String dataMap2 = String.valueOf(paramBody.get("param2"));// PAYTERMS
		Map<String, Object> dataMap2 = (Map<String, Object>) paramBody.get("param2");// PROMO

		List<Map<String, Object>> dataMap3 = (List<Map<String, Object>>) paramBody.get("param3");// PRODUCTS
		List<Map<String, Object>> dataMap4 = (List<Map<String, Object>>) paramBody.get("param4");// STORES
		if (MapUtils.isNotEmpty(dataMap1) && MapUtils.isNotEmpty(dataMap2) && CollectionUtils.isNotEmpty(dataMap3)
				&& CollectionUtils.isNotEmpty(dataMap4)) {

			String promoCode = String.valueOf(dataMap1.get("promoCode"));
			//String stgSchedId = PCDataUtil.getUniqueId("SP");
			int payTerms = Integer.valueOf(String.valueOf(dataMap2.get("payTerms"))); 
			double interestRate = NumberUtil.getDoubleValue(dataMap2, "interestRate");
			logger.info("*** ProductController >> payTerms: " + payTerms);
			logger.info("*** ProductController >> interestRate: " + interestRate);

			// INSERT in STAGGERED_PAYMENT_MST
			// ***********************************************
			for (Map<String, Object> storeMap : dataMap4) {
				String storeCustNo = StringUtils.trim((String) storeMap.get("customerNumber"));
				String warehouse1 = StringUtils.trim((String) storeMap.get("warehouse1"));
				String warehouse2 = StringUtils.trim((String) storeMap.get("warehouse2"));
				
				String stgSchedId = new StringBuilder("S").append(promoCode)
						.append(storeCustNo).toString();
				
				boolean recordExist = customerService.checkIfPromoDataExist("staggered_payment_mst", promoCode,
						storeCustNo);
				if (recordExist) {
					throw new DuplicateKeyException("Record already exist");
				}

				for (Map<String, Object> productMap : dataMap3) {

					String itemId = String.valueOf(productMap.get("itemId"));
					HashMap<String, Object> searchMap = new HashMap<>();
					searchMap.put("itemNumber", itemId);
					searchMap.put("warehouse", warehouse1);

					List<Map<String, Object>> dbProducts = productService.getProductInventoryList(searchMap);
					if (CollectionUtils.isEmpty(dbProducts) && StringUtils.isNotBlank(warehouse2)) {
						searchMap.put("warehouse", warehouse2);
						dbProducts = productService.getProductInventoryList(searchMap);
					}

					if (CollectionUtils.isNotEmpty(dbProducts)) {
						Map<String, Object> dbProduct = dbProducts.get(0);
						try {
							logger.info("*** ProductController >>  shopProdId: " + dbProduct.get("shopProdId"));
							if (dbProduct.get("shopProdId") != null) {

								Long shopProdId = (long) dbProduct.get("shopProdId");
								Map<String, Object> eProduct = onlineProductService.getOneProduct(shopProdId);
								if (MapUtils.isNotEmpty(eProduct)) {
									logger.info("*** ProductController >> addAutoIssuanceCustomer >> eProduct: "
											+ eProduct);
									List<Map<String, Object>> prodVariants = (List<Map<String, Object>>) eProduct
											.get("variants");
									Map<String, Object> variantMap = prodVariants.get(0);// FIRST Variant WITH PRICE
									Long variantId = NumberUtil.getLongValue(variantMap, "id");
									logger.info("*** ProductController >> addAutoIssuanceCustomer >> variantId: "
											+ variantId);
									String itemQty = String.valueOf(productMap.get("itemQty"));
									logger.info("*** ProductController >> addAutoIssuanceCustomer >> save: " + save);

									HashMap<String, Object> insertMap = new HashMap<>();
									insertMap.put("promoCode", promoCode);
									insertMap.put("customerNo", storeCustNo);
									insertMap.put("itemNo", itemId);
									insertMap.put("itemVariantId", variantId);
									insertMap.put("itemProdId", shopProdId);
									insertMap.put("itemQty", itemQty);
									insertMap.put("stgrdSchedId", stgSchedId);
									insertMap.put("payTermsCount", payTerms);
									insertMap.put("interestRate", interestRate);
									insertMap.put("issuedFlag", "N");

									int inserted = issuanceMapper.insertStaggeredIssuanceMaster(insertMap);
									logger.info("*** ProductController >> inserted: " + inserted);
									if (inserted != 0)
										save = true;
								}

							}
						} catch (Throwable t) {
							logger.log(Level.SEVERE, t.getMessage(), t);
						}

					}
				}
				
				// INSERT in STAGGERED_PAYMENT_SCHED
				// ***********************************************
				Date dateNow = DateUtil.getDateInManilaPH();
				for (int ii = 0; ii < payTerms; ii++) {
					dateNow = DateUtil.getDateNowPlusTime(dateNow, Calendar.MONTH, 1);

					HashMap<String, Object> insertMap = new HashMap<>();
					insertMap.put("stgrdSchedId", stgSchedId);
					insertMap.put("schedPayDate", dateNow);
					int inserted = issuanceMapper.insertStaggeredIssuanceSched(insertMap);
					logger.info("*** ProductController >> inserted: " + inserted);

				}
			}
 

		}
		logger.info("*** ProductController >> addStaggeredPayIssuance >> [END]");
		return new ResponseEntity<Boolean>(save, httpStat);
	}

	@PostMapping("/updatePromoIssuance")
	public ResponseEntity<Boolean> updatePromoIssuance(@RequestBody Map<String, Object> paramBody) {
		logger.info("*** ProductController >> updatePromoIssuance >> [START]");
		HttpStatus httpStat = HttpStatus.OK;
		HashMap<String, Object> paramMap = new HashMap<>();
		Map<String, Object> param1 = (Map<String, Object>) paramBody.get("param1");
		paramMap.putAll(param1);
		logger.info("*** ProductController >> paramMap: " + paramMap);
		boolean save = false;
		if (paramMap.get("effectStartDate") != null) {
			Date effStartDate = DateUtil.stringToDate(String.valueOf(paramMap.get("effectStartDate")), "MM/dd/yyyy");
			paramMap.put("effectStartDate", effStartDate);
		}
		if (paramMap.get("effectEndDate") != null) {
			Date effEndDate = DateUtil.stringToDate(String.valueOf(paramMap.get("effectEndDate")), "MM/dd/yyyy");
			paramMap.put("effectEndDate", effEndDate);
		}
		int updateResult = issuanceMapper.updatePromoIssuance(paramMap);
		if (updateResult != 0) {
			save = true;
		}
		logger.info("*** ProductController >> updatePromoIssuance >> [END]");
		return new ResponseEntity<Boolean>(save, httpStat);
	}

	@PostMapping("/addStaggeredPaySchedule")
	public ResponseEntity<Boolean> addStaggeredPaySchedule(@RequestBody Map<String, Object> paramBody) {
		logger.info("*** ProductController >> addStaggeredPaySchedule >> [START]");
		HttpStatus httpStat = HttpStatus.OK;
		HashMap<String, Object> paramMap = new HashMap<>();
		paramMap.putAll(paramBody);
		logger.info("*** ProductController >> paramBody: " + paramBody);
		boolean save = false;

		logger.info("*** ProductController >> addStaggeredPaySchedule >> [END]");
		return new ResponseEntity<Boolean>(save, httpStat);
	}

	@SuppressWarnings("unchecked")
	@PostMapping("/deleteStaggeredPayIssuance")
	public ResponseEntity<Boolean> deleteStaggeredPayIssuance(@RequestBody Map<String, Object> paramBody) {
		logger.info("*** ProductController >> deleteStaggeredPayIssuance >> [START]");
		HttpStatus httpStat = HttpStatus.OK;
		logger.info("*** ProductController >> paramBody: " + paramBody);
		List<Map<String, Object>> dataMapList = (List<Map<String, Object>>) paramBody.get("param1");
		boolean deleted = false;
		if (CollectionUtils.isNotEmpty(dataMapList)) {
			for (Map<String, Object> dataMap : dataMapList) {
				
				String dataId = String.valueOf(dataMap.get("dataId"));
				String promoCode = String.valueOf(dataMap.get("promoCode"));
				String customerNo = String.valueOf(dataMap.get("customerNo"));
				String stgrdSchedId = String.valueOf(dataMap.get("stgrdSchedId"));

				HashMap<String, Object> mstMap = new HashMap<>();
				mstMap.put("promoCode", promoCode);
				mstMap.put("customerNo", customerNo);
				int resultMst = issuanceMapper.deleteStaggeredIssuanceMaster(mstMap);

				HashMap<String, Object> schedMap = new HashMap<>();
				schedMap.put("stgrdSchedId", stgrdSchedId);
				int resultSched = issuanceMapper.deleteStaggeredIssuanceSched(schedMap);
				logger.info("*** ProductController >> deleteStaggeredPayIssuance >> resultMst: " + resultMst + " >> resultSched: " + resultSched);
				if (resultMst != 0) {
					deleted = true;
				}

				if (deleted) {
					try {

						Map<String, Object> rootMap = new LinkedHashMap<String, Object>();
						Map<String, Object> detailMap = new LinkedHashMap<String, Object>();

						detailMap.put("namespace", "staggered_pay_issuance");
						detailMap.put("key", customerNo);
						detailMap.put("value", "DELETED");
						detailMap.put("type", "single_line_text_field");
						rootMap.put("metafield", detailMap);
						rootMap.put("metaType", "SHOP");

						onlineShopService.updateShopMetafield(rootMap);

						detailMap.put("namespace", "staggered_pay_next_sched");
						detailMap.put("key", customerNo);
						detailMap.put("value", "DELETED");
						detailMap.put("type", "single_line_text_field");
						rootMap.put("metafield", detailMap);
						rootMap.put("metaType", "SHOP");

						onlineShopService.updateShopMetafield(rootMap);

//						onlineShopService.deleteShopMetafield("staggered_pay_issuance", customerNo);
//						onlineShopService.deleteShopMetafield("staggered_pay_next_sched", customerNo);

					} catch (Throwable e) {
						logger.log(Level.SEVERE, e.getMessage(), e);
					}
				}
			}
		}
		logger.info("*** ProductController >> deleteStaggeredPayIssuance >> [END]");
		return new ResponseEntity<Boolean>(deleted, httpStat);
	}

	@PostMapping("/retrieveStaggeredMasterList")
	public ResponseEntity<List<Map<String, Object>>> retrieveStaggeredMasterList(
			@RequestBody Map<String, Object> paramBody) {
		HttpStatus httpStat = HttpStatus.OK;
		Map<String, Object> paramMap = (Map<String, Object>) paramBody.get("param1");
		HashMap<String, Object> searchMap = new HashMap<>();
		if (MapUtils.isNotEmpty(paramMap)) {
			searchMap.putAll(paramMap);
		}
		List<Map<String, Object>> dataList = issuanceMapper.getStaggeredPayDisplay(searchMap);
		for (Map<String, Object> dataMap : dataList) {
			String issuedFlagStr = (String) dataMap.get("issuedFlag");
			boolean issuedFlag = (StringUtils.isNotBlank(issuedFlagStr) && issuedFlagStr.equals("Y")) ? true : false;
			dataMap.put("issuedFlag", issuedFlag);
			dataMap.put("issuedFlagTxt", String.valueOf(issuedFlag));
			dataMap.put("issuedFlagLabel", (issuedFlag) ? "TRUE (DONE)" : "FALSE");
			String oosEnabledStr = (String) dataMap.get("oosEnabled");
			boolean oosEnabled = (StringUtils.isNotBlank(oosEnabledStr) && oosEnabledStr.equals("Y")) ? true : false;
			dataMap.put("oosEnabled", oosEnabled);
			dataMap.put("oosEnabledTxt", String.valueOf(oosEnabled));
			dataMap.put("oosEnabledLabel", (oosEnabled) ? "ON (ACTIVE)" : "OFF (INACTIVE)");
			
			
			dataMap.put("financialStatus", StringUtils.upperCase((String)dataMap.get("financialStatus")));
			
			long schedCount = (Long) dataMap.get("schedCount");
			dataMap.put("paymentScheme", (schedCount != 0)?"Staggered Payment":"Full Payment");

		}
		return new ResponseEntity<List<Map<String, Object>>>(dataList, httpStat);
	}

	@PostMapping("/retrieveStaggeredSchedList")
	public ResponseEntity<List<Map<String, Object>>> retrieveStaggeredSchedList(
			@RequestBody Map<String, Object> paramBody) {
		HttpStatus httpStat = HttpStatus.OK;
		Map<String, Object> paramMap = (Map<String, Object>) paramBody.get("param1");
		HashMap<String, Object> searchMap = new HashMap<>();
		searchMap.putAll(paramMap);

		List<Map<String, Object>> dataList = issuanceMapper.getStaggeredSchedList(searchMap);

		return new ResponseEntity<List<Map<String, Object>>>(dataList, httpStat);
	}

	@PostMapping("/publishStaggeredPayIssuance")
	public ResponseEntity<Boolean> publishStaggeredPayIssuance(@RequestBody Map<String, Object> paramBody) {
		logger.info("*** ProductController >> publishStaggeredPayIssuance >> [START]");
		HttpStatus httpStat = HttpStatus.OK;
		logger.info("*** ProductController >> publishStaggeredPayIssuance >> paramBody: " + paramBody);
		List<Map<String, Object>> dataMapList = (List<Map<String, Object>>) paramBody.get("param1");
		boolean success = false;

		Gson gson = new GsonBuilder().setPrettyPrinting().create();

		if (CollectionUtils.isNotEmpty(dataMapList)) {

			for (Map<String, Object> dataMap : dataMapList) {

				try {
					logger.info("*** ProductController >> publishStaggeredPayIssuance >> dataMap: " + dataMap);
					String promoCode = String.valueOf(dataMap.get("promoCode"));
					String customerNo = String.valueOf(dataMap.get("customerNo"));
					HashMap<String, Object> searchMap = new HashMap<>();
					searchMap.put("promoCode", promoCode);
					searchMap.put("customerNo", customerNo);
					List<Map<String, Object>> issuanceItemDetails = issuanceMapper.getStaggeredPayDetailList(searchMap);

					if (CollectionUtils.isNotEmpty(issuanceItemDetails)) {
						String jsonMeta = gson.toJson(issuanceItemDetails, List.class);

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
						int updated = issuanceMapper.updateStaggeredIssuanceMaster(updateMap);
						if (updated != 0) {
							success = true;
						}

						if (success) {
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

					}

				} catch (Throwable t) {
					success = false;
					logger.log(Level.SEVERE, t.getMessage(), t);
				}

			}
		}

		logger.info("*** ProductController >> publishStaggeredPayIssuance >> [END]");
		return new ResponseEntity<Boolean>(success, httpStat);
	}

}
