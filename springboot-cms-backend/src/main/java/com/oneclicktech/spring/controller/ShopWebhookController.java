package com.oneclicktech.spring.controller;

import java.time.LocalDateTime;
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
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.oneclicktech.spring.domain.Constants;
import com.oneclicktech.spring.mapper.CreditMemoMapper;
import com.oneclicktech.spring.mapper.EWTMapper;
import com.oneclicktech.spring.mapper.IssuanceMapper;
import com.oneclicktech.spring.mapper.ShopOrderMapper;
import com.oneclicktech.spring.service.CronJobService;
import com.oneclicktech.spring.service.IssuanceService;
import com.oneclicktech.spring.service.OnlineCustomerService;
import com.oneclicktech.spring.service.OnlineOrderService;
import com.oneclicktech.spring.service.OnlineProductService;
import com.oneclicktech.spring.service.OnlineShopService;
import com.oneclicktech.spring.service.QueryBuilderService;
import com.oneclicktech.spring.service.RestGenericService;
import com.oneclicktech.spring.service.RestTemplateService;
import com.oneclicktech.spring.service.SupportService;
import com.oneclicktech.spring.service.SyncD365Service;
import com.oneclicktech.spring.util.DateUtil;
import com.oneclicktech.spring.util.NumberUtil;
import com.oneclicktech.spring.util.PCDataUtil;
import com.oneclicktech.spring.util.ShopifyUtil;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/pc/webhook")
public class ShopWebhookController {

	private static final Logger logger = Logger.getLogger("ShopWebhookController");

	private static final int SLEEP_TIME = 3000; // 1000 = 1 sec
	private static final int SLEEP_TIME_ORDER = 10000; // 1000 = 1 sec

	@Autowired
	RestTemplateService restTemplateService;

	@Autowired
	QueryBuilderService queryBuilderService;

	@Autowired
	OnlineProductService onlineProductService;

	@Autowired
	OnlineOrderService onlineOrderService;

	@Autowired
	IssuanceService issuanceService;

	@Autowired
	SupportService supportService;

	@Autowired
	OnlineCustomerService onlineCustomerService;

	@Autowired
	EWTMapper ewtMapper;

	@Autowired
	RestGenericService restGenericService;

	@Autowired
	OnlineShopService onlineShopService;

	@Autowired
	ShopOrderMapper shopOrderMapper;

	@Autowired
	IssuanceMapper issuanceMapper;
	
	@Autowired
	CreditMemoMapper creditMemoMapper;

	@Value("${pc.shopify.app.webhook.version}")
	String apiVersion;

	@Value("${pc.shopify.app.rowlimit}")
	String rowLimit;

	@Value("${pc.value.added.tax}")
	String valueAddedTax;

	@Autowired
	SyncD365Service syncD365Service;

	@Autowired
	CronJobService cronJobService;

	@Value("${cms.app.host-url}")
	String cmsAppHostUrl;

	private Map<String, String> processOrders;
	private Map<String, String> createdOrders;
	private Map<String, Double> ewtConfig;

	private static String TEST_ORDER_ID = "5714648203574"; // UAT1130

	@PostConstruct
	public void init() {
		logger.info("*** ShopWebhookController >>  [INIT]");
		processOrders = new HashMap<>(); 
		createdOrders = new HashMap<>();

		try {
			ewtConfig = new HashMap<String, Double>();
			String queryTxt = "SELECT * from cms_db.config_discount " + " WHERE discount_type = 'EWT'";
			List<Map<String, Object>> ewtDiscList = queryBuilderService.getExecQuery(queryTxt);
			for (Map<String, Object> discntMap : ewtDiscList) {
				String disctName = (String) discntMap.get("discount_name");
				double disctValue = (Double) discntMap.get("discount_value");
				ewtConfig.put(disctName, disctValue);
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	@PostMapping("/orderCreationHook")
	public ResponseEntity orderCreationHook(@RequestBody Map<String, Object> paramBody) {
		logger.info("*** ShopWebhookController >> orderCreationHook >> [START]");

		if (Constants.TEST_ONLY) {
			try {
				createdOrders = new HashMap<>();
				Long orderId = Long.valueOf(TEST_ORDER_ID);
				paramBody = onlineOrderService.getOneOrderByID(orderId);
				paramBody.put("id", orderId);
			} catch (Throwable t) {
				logger.info("** OnlineUPayController >> orderCreationHook >> ERROR: ");
				logger.log(Level.SEVERE, t.getMessage(), t);
			}

		}

		String orderName = StringUtils.trimToEmpty((String) paramBody.get("name"));
		Long orderId = NumberUtil.getLongValue(paramBody, "id");
		logger.info("*** ShopWebhookController >> orderCreationHook >> orderName: " + orderName);
		logger.info("*** ShopWebhookController >> orderCreationHook >> orderId: " + orderId);
		logger.info("*** ShopWebhookController >> orderCreationHook >> createdOrders: " + createdOrders);
		logger.info("*** ShopWebhookController >> **********************************************************");
		logger.info("*** ShopWebhookController >> **********************************************************");
		logger.info("*** ShopWebhookController >> paramBody: " + paramBody);

		if (MapUtils.isNotEmpty(paramBody) && StringUtils.isNotBlank(orderName)
				&& !createdOrders.containsKey(orderName)) {

			createdOrders.put(orderName, orderName);

			try {
				HashMap<String, Object> orderParam = new HashMap<>();
				orderParam.put("orderName", orderName);
				Map<String, Object> oneOrder = shopOrderMapper.getOneOrderData(orderParam);
				logger.info("*** ShopWebhookController >> orderCreationHook >> oneOrder: " + oneOrder);
				if (MapUtils.isEmpty(oneOrder)) {

					onlineOrderService.saveOnlineOrderByHookService(paramBody);
				}
				/*
				 * ********************************************************************** GET
				 * Product Auto Issuance Data to PROCESS
				 * **********************************************************************
				 */
				this.processAutoNBDIssuance(paramBody);

				/*
				 * ********************************************************************** GET
				 * Staggered Pay Issuance to PROCESS
				 * **********************************************************************
				 */
				this.processStaggeredPayAfterOrderCreation(paramBody);

				/*
				 * ********************************************************************** Check
				 * if the Order is EWT
				 * **********************************************************************
				 */
				this.processCreditMemoAfterOrderCreation(paramBody);
				/*
				 * ********************************************************************** Check
				 * if the Order is EWT
				 * **********************************************************************
				 */
				this.processEWTOrderAfterOrderCreation(paramBody);

			} catch (Exception e) {
				logger.info("** OnlineUPayController >> orderCreationHook >> ERROR: ");
				logger.log(Level.SEVERE, e.getMessage(), e);
			} catch (Throwable t) {
				logger.info("** OnlineUPayController >> orderCreationHook >> ERROR: ");
				logger.log(Level.SEVERE, t.getMessage(), t);
			}
		}

		logger.info("*** ShopWebhookController >> orderCreationHook >> [END]");
		return ResponseEntity.ok().build();
	}

	@PostMapping("/orderUpdateHook")
	public ResponseEntity orderUpdateHook(@RequestBody Map<String, Object> paramBody) {
		logger.info("*** ShopWebhookController >> orderUpdateHook >> [START]");
		String orderName = StringUtils.trimToEmpty((String) paramBody.get("name"));
		Long orderId = NumberUtil.getLongValue(paramBody, "id");
		logger.info("*** ShopWebhookController >> orderUpdateHook >> orderName: " + orderName);
		logger.info("*** ShopWebhookController >> orderUpdateHook >> orderId: " + orderId);

		if (MapUtils.isNotEmpty(paramBody) && StringUtils.isNotBlank(orderName)) {

//			try {
//				HashMap<String, Object> orderParam = new HashMap<>(); 
//				orderParam.put("name", orderName);
//				cronJobService.saveOnlineOrderToDB(orderParam);
//	 		} catch (Exception e) {
//				logger.info("** OnlineUPayController >> orderUpdateHook >> ERROR: ");
//				logger.log(Level.SEVERE, e.getMessage(), e);
//			} catch (Throwable t) {
//				logger.info("** OnlineUPayController >> orderUpdateHook >> ERROR: ");
//				logger.log(Level.SEVERE, t.getMessage(), t);
//			}
		}

		logger.info("*** ShopWebhookController >> orderUpdateHook >> [END]");
		return ResponseEntity.ok().build();
	}

	@PostMapping("/orderPaymentHook")
	public ResponseEntity orderPaymentHook(@RequestBody Map<String, Object> paramBody) {
		logger.info("*** ShopWebhookController >> orderPaymentHook >> [START]");

		if (Constants.TEST_ONLY) {
			try {
				processOrders = new HashMap<>();
				Long orderId = Long.valueOf(String.valueOf(TEST_ORDER_ID));
				paramBody = onlineOrderService.getOneOrderByID(orderId);
				paramBody.put("id", orderId);
			} catch (Throwable t) {
				logger.info("** OnlineUPayController >> orderPaymentHook >> ERROR: ");
				logger.log(Level.SEVERE, t.getMessage(), t);
			}

		}

		String orderName = StringUtils.trimToEmpty((String) paramBody.get("name"));
		Long orderId = NumberUtil.getLongValue(paramBody, "id");

		logger.info("*** ShopWebhookController >> orderPaymentHook >> orderName: " + orderName);
		logger.info("*** ShopWebhookController >> orderPaymentHook >> orderId: " + orderId);
		logger.info("*** ShopWebhookController >> orderPaymentHook >> processOrders: " + processOrders);
		logger.info("*** ShopWebhookController >> **********************************************************");
		logger.info("*** ShopWebhookController >> **********************************************************");
		logger.info("*** ShopWebhookController >> paramBody: " + paramBody);
		if (MapUtils.isNotEmpty(paramBody) && StringUtils.isNotBlank(orderName)
				&& !processOrders.containsKey(orderName)) {

			processOrders.put(orderName, orderName);

			try {
				// SAVE The order first to GET/SAVE the latest status
				try {
					onlineOrderService.saveOnlineOrderByHookService(paramBody);
				} catch (Throwable t) {
					logger.log(Level.SEVERE, t.getMessage(), t);
				}

				HashMap<String, Object> orderParam = new HashMap<>();
				orderParam.put("orderName", orderName);
				Map<String, Object> oneOrder = shopOrderMapper.getOneOrderData(orderParam);

				logger.info("*** ShopWebhookController >> orderPaymentHook >> oneOrder: " + oneOrder);

				if (MapUtils.isNotEmpty(oneOrder)) {
					String soNo = StringUtils.trimToEmpty((String) oneOrder.get("salesOrderNo"));
					if (StringUtils.isBlank(soNo)) {
						Map<String, Object> eOrderMap = onlineOrderService.getOneOrderByID(orderId);
						String financialStatus = StringUtils.trimToEmpty((String) eOrderMap.get("financial_status"));
						logger.info("*** ShopWebhookController >> orderPaymentHook >> soNo: " + soNo);
						logger.info(
								"*** ShopWebhookController >> orderPaymentHook >> financialStatus: " + financialStatus);

						if (financialStatus.equals(Constants.STATUS_PAID)) {
							// NO SO Number / PAID / NO SO# Tags
							// Process SO Generation
							// *******************************************************
							Map<String, Object> soMap = new HashMap<>();
							soMap.put("processPerOrder", true);
							soMap.put("orderName", orderName);
							Map<String, Object> syncResultMap = syncD365Service.syncSalesOrderDataToDB(soMap);

							// **********************************************************
							// EWT ORDER
							// You can ONLY process EWT after SO generation
							// **********************************************************
							this.processEWTOrderAfterSOGeneration(paramBody, syncResultMap);
						}
					}

				}

				// STAGGERED PAYMENT SCHEDULE
				// Process payment & schedule
				// **********************************************************
				this.processStaggeredAfterPayment(paramBody);

				// **********************************************************
			} catch (Exception e) {
				logger.info("** OnlineUPayController >> orderPaymentHook >> ERROR: ");
				logger.log(Level.SEVERE, e.getMessage(), e);
			} catch (Throwable t) {
				logger.info("** OnlineUPayController >> orderPaymentHook >> ERROR: ");
				logger.log(Level.SEVERE, t.getMessage(), t);
			}
		}

		logger.info("*** ShopWebhookController >> orderPaymentHook >> [END]");
		return ResponseEntity.ok().build();
	}

	@PostMapping("/cartUpdateHook")
	public ResponseEntity cartUpdateHook(@RequestBody Map<String, Object> paramBody) {
		logger.info("*** ShopWebhookController >> cartUpdateHook >> [START]");
		logger.info("*** ShopWebhookController >> cartUpdateHook >> paramBody: " + paramBody);
		logger.info("*** ShopWebhookController >> cartUpdateHook >> [END]");
		return ResponseEntity.ok().build();
	}

	@PostMapping("/cartCreationHook")
	public ResponseEntity cartCreateHook(@RequestBody Map<String, Object> paramBody) {
		logger.info("*** ShopWebhookController >> cartCreationHook >> [START]");
		logger.info("*** ShopWebhookController >> cartCreationHook >> paramBody: " + paramBody);
		logger.info("*** ShopWebhookController >> cartCreationHook >> [END]");
		return ResponseEntity.ok().build();
	}

	@PostMapping("/checkoutCreationHook")
	public ResponseEntity checkoutCreationHook(@RequestBody Map<String, Object> paramBody, HttpServletRequest request) {
		logger.info("*** ShopWebhookController >> checkoutCreationHook >> [START]");
		logger.info("*** ShopWebhookController >> checkoutCreationHook >> paramBody: " + paramBody);
		logger.info("*** ShopWebhookController >> checkoutCreationHook >> request: " + request);
		String siteReferrer = request.getHeader("referer");
		logger.info("*** ShopWebhookController >> checkoutCreationHook >> siteReferrer: " + siteReferrer);
		logger.info("*** ShopWebhookController >> checkoutCreationHook >> [END]");
		return ResponseEntity.ok().build();
	}

	@PostMapping("/checkoutUpdateHook")
	public ResponseEntity checkoutUpdateHook(@RequestBody Map<String, Object> paramBody, HttpServletRequest request) {
		logger.info("*** ShopWebhookController >> checkoutUpdateHook >> [START]");
		logger.info("*** ShopWebhookController >> checkoutUpdateHook >> paramBody: " + paramBody);
		logger.info("*** ShopWebhookController >> checkoutUpdateHook >> request: " + request);
		String siteReferrer = request.getHeader("referer");
		logger.info("*** ShopWebhookController >> checkoutCreationHook >> siteReferrer: " + siteReferrer);
		logger.info("*** ShopWebhookController >> checkoutCreationHook >> getPathInfo: " + request.getPathInfo());
		logger.info("*** ShopWebhookController >> checkoutCreationHook >> getContextPath: " + request.getContextPath());
		logger.info("*** ShopWebhookController >> checkoutCreationHook >> getRequestURI: " + request.getRequestURI());
		String checkoutToken = (String) paramBody.get("token");
		// String checkoutToken = "0e2089f918f181fa16084bce3c55c2d8";
		List<Map<String, Object>> shippingLines = (List<Map<String, Object>>) paramBody.get("shipping_lines");
		for (Map<String, Object> shipLine : shippingLines) {
			Double shipAmt = Double.valueOf(String.valueOf(shipLine.get("price")));
			if (shipAmt < 1D) {
				// NO Shipping Amount
				// Add Delivery Charge

				try {
					Map<String, Object> shipChargeMap = new HashMap<>();
					shipChargeMap.put("title", "Delivery Charge");
					shipChargeMap.put("code", "Standard");
					shipChargeMap.put("price", "45.50");
					onlineOrderService.addDeliveryChargeToCart(checkoutToken, shipChargeMap);
				} catch (Throwable t) {
					logger.log(Level.SEVERE, t.getMessage(), t);
				}

			}
		}

		logger.info("*** ShopWebhookController >> checkoutUpdateHook >> [END]");
		return ResponseEntity.ok().build();
	}

	@PostMapping("/customerUpdateHook")
	public ResponseEntity customerUpdateHook(@RequestBody Map<String, Object> paramBody) {
		logger.info("*** ShopWebhookController >> customerUpdateHook >> [START]");
		logger.info("*** ShopWebhookController >> customerUpdateHook >> paramBody: " + paramBody);

		List<Map<String, Object>> addressList = (List<Map<String, Object>>) paramBody.get("addresses");
		if (CollectionUtils.isNotEmpty(addressList)) {
			for (Map<String, Object> addrMap : addressList) {
				Boolean isDefault = (Boolean) addrMap.get("default");
				if (isDefault != null && isDefault) {
					logger.info("*** ShopWebhookController >> customerUpdateHook >> addrMap: " + addrMap);
					Long eCustomerId = NumberUtil.getLongValue(addrMap, "customer_id");
					Long eAddressId = NumberUtil.getLongValue(addrMap, "id");
					String address1 = StringUtils.trimToEmpty((String) addrMap.get("address1"));
					try {
						Map<String, Object> updateMap = new HashMap<>();
						updateMap.put("address1", address1);
						updateMap.put("default", true);
						onlineCustomerService.updateCustomerAddress(eCustomerId, eAddressId, updateMap);
					} catch (Throwable t) {
						logger.log(Level.SEVERE, t.getMessage(), t);
					}

				}
			}
		}

		logger.info("*** ShopWebhookController >> customerUpdateHook >> [END]");
		return ResponseEntity.ok().build();
	}

	private void processAutoNBDIssuance(Map<String, Object> eOrderMap) {

		logger.info("*** ShopWebhookController >> processAutoNBDIssuance >> [START] ");
		String orderName = (String) eOrderMap.get("name");
		HashMap<String, Object> searchMap = new HashMap<>();
		searchMap.put("orderName", orderName);
		List<Map<String, Object>> issuanceDataList = issuanceMapper.getAutoIssuanceListByOrder(searchMap);

		if (CollectionUtils.isNotEmpty(issuanceDataList)) {

			Map<String, Object> issuanceMap = issuanceDataList.get(0);

			String dbCustomerNo = (String) issuanceMap.get("customerNo");
			String eItemQuery = " select item_id FROM cms_db.store_auto_issuance where customer_no = '" + dbCustomerNo
					+ "' " + "	and order_name is null";
			List<Map<String, Object>> eItemList = queryBuilderService.getExecQuery(eItemQuery);
			int dbItemCount = 0;
			if (CollectionUtils.isNotEmpty(eItemList)) {
				dbItemCount = eItemList.size();
			}

			logger.info("*** ShopWebhookController >> processAutoNBDIssuance >> orderName: " + orderName);
			logger.info("*** ShopWebhookController >> processAutoNBDIssuance >> dbItemCount: " + dbItemCount);

			for (Map<String, Object> issueData : issuanceDataList) {
				boolean matchItemCount = false;
				String promoCode = String.valueOf(issueData.get("promoCode"));
				String customerNo = String.valueOf(issueData.get("customerNo"));
				int eItemCount = Integer.valueOf(String.valueOf(issueData.get("itemCount")));
				logger.info("*** ShopWebhookController >> processAutoNBDIssuance >> eItemCount: " + eItemCount);
				LocalDateTime issuedDateTime = (LocalDateTime) issueData.get("creationDate");
				Date issuedDate = DateUtil.convertToDateViaSqlTimestamp(issuedDateTime);
				String updateTxt = "UPDATE cms_db.store_auto_issuance set order_name = '" + orderName + "' "
						+ ", issued_date ='" + issuedDate + "' " + ", issued_flag ='Y' " + " WHERE promo_code = '"
						+ promoCode + "' " + "   and customer_no = '" + customerNo + "'";

				if (eItemCount >= dbItemCount) {
					matchItemCount = true;
				}

				logger.info("*** ShopWebhookController >> processAutoNBDIssuance >> matchItemCount: " + matchItemCount);
				if (matchItemCount) {
					boolean tagAsIssued = queryBuilderService.execQuery(updateTxt);
					logger.info("*** ShopWebhookController >> processAutoNBDIssuance >> tagAsIssued: " + tagAsIssued);
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

							logger.info("*** ShopWebhookController >> processAutoNBDIssuance >> rootMap: " + rootMap);

							onlineShopService.updateShopMetafield(rootMap);

						} catch (Throwable e) {
							logger.log(Level.SEVERE, e.getMessage(), e);
						}

						try {
							StringBuilder ewtTags = new StringBuilder("NPD_ORDER");
							onlineOrderService.addOrderTagToCurrent(eOrderMap, ewtTags.toString());
						} catch (Throwable e) {
							logger.log(Level.SEVERE, e.getMessage(), e);
						}
					}
				}

			}

		}
		logger.info("*** ShopWebhookController >> processAutoNBDIssuance >> [END] ");

	}

	@SuppressWarnings("unused")
	private void processEWTOrderAfterOrderCreation(Map<String, Object> eOrderMap) {
		logger.info("*** ShopWebhookController >> processEWTOrderAfterOrderCreation >> [START] ");
		Map<String, String> ewtInfo = PCDataUtil.getEWTItemInfo(eOrderMap);
		String customerNo = ShopifyUtil.getSOCustomerNoByAddress(eOrderMap);
		String orderName = (String) eOrderMap.get("name");

		logger.info("*** ShopWebhookController >> processEWTOrderAfterOrderCreation >> ewtInfo: " + ewtInfo);
		logger.info("*** ShopWebhookController >> processEWTOrderAfterOrderCreation >> orderName: " + orderName);
		// boolean isEWTOrder = PCDataUtil.isAnEWTOrder(eOrderMap);
		// logger.info("*** ShopWebhookController >> orderCreationHook >> isEWTOrder: "
		// + isEWTOrder);
		if (MapUtils.isNotEmpty(ewtInfo)) {

			Gson gson = new GsonBuilder().setPrettyPrinting().create();

			try {
				HashMap<String, Object> searchMap = new HashMap<String, Object>();
				searchMap.put("customerNo", customerNo);
				searchMap.put("ewtFlag", "Y");
				List<Map<String, Object>> ewtCustomers = ewtMapper.getCustomerEWTList(searchMap);
				if (CollectionUtils.isNotEmpty(ewtCustomers)) {
					Map<String, Object> ewtCustomer = ewtCustomers.get(0);
					logger.info("*** ShopWebhookController >> processEWTOrderAfterOrderCreation >> ewtCustomer: "
							+ ewtCustomer);

					double goodsDiscount = Double.parseDouble(String.valueOf(ewtCustomer.get("goodsDiscount")));
					double servicesDiscount = Double.parseDouble(String.valueOf(ewtCustomer.get("servicesDiscount")));

					// double subTotal = Double.parseDouble((String)
					// eOrderMap.get("total_line_items_price"));
					double subTotal = Double.parseDouble((String) eOrderMap.get("subtotal_price"));
					double vatTax = Double.parseDouble(valueAddedTax);

					double orderWithDiscount = PCDataUtil.computeEWTDiscount(subTotal, vatTax, goodsDiscount);
					double shipWithDiscount = 0D;
					double shipTotal = 0D;
					Map<String, Object> shippingSet = (Map<String, Object>) eOrderMap.get("total_shipping_price_set");
					if (MapUtils.isNotEmpty(shippingSet)) {
						Map<String, Object> shipMoneyMap = (Map<String, Object>) shippingSet.get("shop_money");
						if (MapUtils.isNotEmpty(shipMoneyMap)) {
							shipTotal = Double.parseDouble((String) shipMoneyMap.get("amount"));
							if (shipTotal > 0) {
								shipWithDiscount = PCDataUtil.computeEWTDiscount(shipTotal, vatTax, servicesDiscount);
								logger.info(
										"*** ShopWebhookController >> processEWTOrderAfterOrderCreation >> shipWithDiscount: "
												+ shipWithDiscount);
							}
						}
					}
					logger.info("*** ShopWebhookController >> processEWTOrderAfterOrderCreation >> orderWithDiscount: "
							+ orderWithDiscount);
					logger.info("*** ShopWebhookController >> processEWTOrderAfterOrderCreation >> shipWithDiscount: "
							+ shipWithDiscount);
					// PUT Order Tag
					try {

						double orderEWTAmount = NumberUtil.roundTwoDec((subTotal - orderWithDiscount));
						double deliveryEWTAmount = NumberUtil.roundTwoDec((shipTotal - shipWithDiscount));

						HashMap<String, Object> insertMap = new HashMap<String, Object>();
						String ewtFileLink = (String) ewtInfo.get("ewtFileLink");
						insertMap.put("orderName", orderName);
						insertMap.put("customerNo", customerNo);
						insertMap.put("goodsDiscount", goodsDiscount);
						insertMap.put("servicesDiscount", servicesDiscount);
						insertMap.put("ewtFileLink", ewtFileLink);
						insertMap.put("orderWithDiscount", orderWithDiscount);
						insertMap.put("shipWithDiscount", shipWithDiscount);
						insertMap.put("orderEWTAmount", orderEWTAmount);
						insertMap.put("deliveryEWTAmount", deliveryEWTAmount);

						logger.info("*** ShopWebhookController >> processEWTOrderAfterOrderCreation >> ewtFileLink: "
								+ ewtFileLink);
						logger.info("*** ShopWebhookController >> processEWTOrderAfterOrderCreation >> insertMap: "
								+ insertMap);

						int result = ewtMapper.insertEWTOrderInfo(insertMap);
						logger.info(
								"*** ShopWebhookController >> processEWTOrderAfterOrderCreation >> result: " + result);
						if (result != 0) {
							Map<String, Object> rootMap = new LinkedHashMap<String, Object>();
							Map<String, Object> detailMap = new LinkedHashMap<String, Object>();
							Map<String, Object> ewtMap = new LinkedHashMap<String, Object>();

							ewtMap.putAll(insertMap);

							String jsonStr = gson.toJson(ewtMap, Map.class);
							logger.info("*** ShopWebhookController >> processEWTOrderAfterOrderCreation >> jsonStr: "
									+ jsonStr);
							detailMap.put("namespace", "ewt_order_map");
							detailMap.put("key", orderName);
							detailMap.put("value", jsonStr);
							detailMap.put("type", "json");
							rootMap.put("metafield", detailMap);
							rootMap.put("metaType", "SHOP");
							logger.info("*** ShopWebhookController >> processEWTOrderAfterOrderCreation >> rootMap: "
									+ rootMap);

							onlineShopService.updateShopMetafield(rootMap);

							onlineOrderService.addOrderTagToCurrent(eOrderMap, "EWT_ORDER");

						}

					} catch (Throwable e) {
						logger.log(Level.SEVERE, e.getMessage(), e);
					}
				}

			} catch (Throwable t) {
				logger.log(Level.SEVERE, t.getMessage(), t);
			}

		}

		logger.info("*** ShopWebhookController >> processEWTOrderAfterOrderCreation >> [END] ");
	}

	private void processEWTOrderAfterSOGeneration(Map<String, Object> eOrderMap, Map<String, Object> soSyncMap) {
		logger.info("*** ShopWebhookController >> processEWTOrderAfterSOGeneration >> [START] ");
		Map<String, String> ewtItemInfo = PCDataUtil.getEWTItemInfo(eOrderMap);
		String customerNo = ShopifyUtil.getSOCustomerNoByAddress(eOrderMap);
		long orderId = ShopifyUtil.getOrderId(eOrderMap);
		String orderName = (String) eOrderMap.get("name");
		logger.info("*** ShopWebhookController >> processEWTOrderAfterSOGeneration >> orderName: " + orderName);
		logger.info("*** ShopWebhookController >> processEWTOrderAfterSOGeneration >> customerNo: " + customerNo);
		logger.info("*** ShopWebhookController >> processEWTOrderAfterSOGeneration >> ewtItemInfo: " + ewtItemInfo);
		logger.info("*** ShopWebhookController >> processEWTOrderAfterSOGeneration >> soSyncMap: " + soSyncMap);

		if (MapUtils.isNotEmpty(ewtItemInfo) && MapUtils.isNotEmpty(soSyncMap)
				&& ((String) soSyncMap.get("statusResult")).equals("SUCCESS")) {

			try {

				Map<String, Object> paramMap = new LinkedHashMap<String, Object>();
				paramMap.put("customerNo", customerNo);
				paramMap.putAll(ewtItemInfo);

				HashMap<String, Object> searchMap = new HashMap<String, Object>();
				searchMap.put("orderName", orderName);
				List<Map<String, Object>> ewtOrderList = ewtMapper.getOrderEWTList(searchMap);
				if (CollectionUtils.isNotEmpty(ewtOrderList)) {
					Map<String, Object> ewtOrderMap = (Map<String, Object>) ewtOrderList.get(0);
					Map<String, Object> orderTrasactMap = onlineOrderService.getOneSuccessTransaction(orderId);
					Map<String, Object> oneTransactMap = (Map<String, Object>) orderTrasactMap.get("transaction");
					String paymentRefId = String.valueOf(NumberUtil.getLongValue(oneTransactMap, "id"));
					paramMap.put("paymentRefId", paymentRefId);
					paramMap.putAll(soSyncMap);

					Map<String, Object> ewtResultMap = syncD365Service.processPaymentJournal(eOrderMap, ewtOrderMap,
							paramMap);
					if (MapUtils.isNotEmpty(ewtResultMap)) {
						logger.info("*** ShopWebhookController >> processEWTOrderAfterSOGeneration >> ewtResultMap: "
								+ ewtResultMap);
					}

				}
			} catch (Throwable t) {
				logger.log(Level.SEVERE, t.getMessage(), t);
			}

		}

		logger.info("*** ShopWebhookController >> processEWTOrderAfterSOGeneration >> [END] ");

	}

//	private void processEWTOrder_ORIG(Map<String, Object> eOrderMap) {
//		logger.info("*** ShopWebhookController >> processEWTOrderAfterOrderCreation >> [START] ");
//		Map<String, String> ewtInfo = PCDataUtil.getEWTItemInfo(eOrderMap);
//		logger.info("*** ShopWebhookController >> processEWTOrderAfterOrderCreation >> ewtInfo: " + ewtInfo);
//		//boolean isEWTOrder = PCDataUtil.isAnEWTOrder(eOrderMap);
//		//logger.info("*** ShopWebhookController >> orderCreationHook >> isEWTOrder: " + isEWTOrder);
//		if (MapUtils.isNotEmpty(ewtInfo)) {
//			
//			try {
//				StringBuilder ewtTags = new StringBuilder("EWT_ORDER").append(",");
//				double subTotal = Double.parseDouble((String) eOrderMap.get("total_line_items_price"));
//				double vatTax = Double.parseDouble(valueAddedTax);
//
//				double orderWithDiscount = PCDataUtil.computeEWTDiscount(subTotal, vatTax, 1.11);
//				ewtTags.append("EWT_DISCOUNT_ORDER_").append(orderWithDiscount);
//
//				Map<String, Object> shippingSet = (Map<String, Object>) eOrderMap.get("total_shipping_price_set");
//				if (MapUtils.isNotEmpty(shippingSet)) {
//					Map<String, Object> shipMoneyMap = (Map<String, Object>) eOrderMap.get("shop_money");
//					if (MapUtils.isNotEmpty(shipMoneyMap)) {
//						double shipTotal = Double.parseDouble((String) eOrderMap.get("amount"));
//						if (shipTotal > 0) {
//							double shipWithDiscount = PCDataUtil.computeEWTDiscount(subTotal, vatTax, 1.1);
//							ewtTags.append(",").append("EWT_DISCOUNT_SHIP_").append(shipWithDiscount);
//						}
//					}
//				}
//				
//				
//				//PUT Order Tag
//				try {
//					onlineOrderService.addOrderTagToCurrent(eOrderMap, ewtTags.toString());
//				} catch (Throwable e) {
//					logger.log(Level.SEVERE, e.getMessage(), e);
//				}
//				
//			} catch (Throwable t) {
//				logger.log(Level.SEVERE, t.getMessage(), t);
//			}
//			 
//		}
//		
//		logger.info("*** ShopWebhookController >> processEWTOrderAfterOrderCreation >> [END] ");
//	}
//

	private void processStaggeredAfterPayment(Map<String, Object> eOrderMap) {
		logger.info("*** ShopWebhookController >> processStaggeredAfterPayment >> [START] ");
		try {
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			Date phDate = DateUtil.getDateInManilaPH();
			String phDateStr = DateUtil.getDateWithPattern(phDate, "MM/dd/yyyy");
			HashMap<String, Object> paramMap = new HashMap<>();
			String orderName = (String) eOrderMap.get("name");
			// Long orderId = NumberUtil.getLongValue(eOrderMap, "id");
			logger.info("*** ShopWebhookController >> processStaggeredAfterPayment >> orderName: " + orderName);
			Long orderId = ShopifyUtil.getOrderId(eOrderMap);
			paramMap.put("orderName", orderName);
			paramMap.put("payStatus", "pending");
			paramMap.put("paymentDate", phDateStr);
			Map<String, Object> paySchedMap = issuanceMapper.getNextPaymentSched(paramMap);

			if (MapUtils.isNotEmpty(paySchedMap)) {
				logger.info("*** ShopWebhookController >> processStaggeredAfterPayment >> paySchedMap: " + paySchedMap);
				// Check OOS Payment Transaction if listed and SUCCESS
				// *****************************************
				boolean paymentSuccess = issuanceService.checkStaggeredPaymentTransactionExist(orderId, paySchedMap);
				logger.info("*** ShopWebhookController >> processStaggeredAfterPayment >> paymentSuccess: "
						+ paymentSuccess);
				if (paymentSuccess) {
					String customerNo = (String) paySchedMap.get("customerNo");

					HashMap<String, Object> updateMap = new HashMap<>();
					updateMap.put("stgrdPayId", paySchedMap.get("rowSchedId"));
					updateMap.put("stgrdSchedId", paySchedMap.get("stgrdSchedId"));
					updateMap.put("amountPaid", paySchedMap.get("amountToPay"));
					updateMap.put("actualPayDate", DateUtil.getDateInManilaPH());
					updateMap.put("payStatus", "paid");
					int updateResult = issuanceMapper.updateStaggeredIssuanceSched(updateMap);
					logger.info("*** ShopWebhookController >> processStaggeredAfterPayment >> updateResult: "
							+ updateResult);
					if (updateResult != 0) {
						// 1. SUCCESS - Next Payment has been tagged 'PAID' in Staggered Schedule
						// *************************************************************************
						Map<String, Object> nextSchedMap = issuanceMapper.getNextPaymentSched(paramMap);
						logger.info("*** ShopWebhookController >> processStaggeredAfterPayment >> nextSchedMap: "
								+ nextSchedMap);
						if (MapUtils.isNotEmpty(nextSchedMap)) {
							// 2. NEXT PROCESS - CREATE new META for the next payment schedule
							// ***************************************************************
							String jsonMeta = gson.toJson(nextSchedMap, Map.class);

							Map<String, Object> rootSchedMap = new LinkedHashMap<String, Object>();
							Map<String, Object> detailSchedMap = new LinkedHashMap<String, Object>();

							detailSchedMap.put("namespace", "staggered_pay_next_sched");
							detailSchedMap.put("key", customerNo);
							detailSchedMap.put("value", jsonMeta);
							detailSchedMap.put("type", "json");
							rootSchedMap.put("metafield", detailSchedMap);
							rootSchedMap.put("metaType", "SHOP");
							onlineShopService.updateShopMetafield(rootSchedMap);
						}
					}
				}

			}

		} catch (Throwable t) {
			logger.log(Level.SEVERE, t.getMessage(), t);
		}

		logger.info("*** ShopWebhookController >> processStaggeredAfterPayment >> [END] ");
	}

	private void processCreditMemoAfterOrderCreation(Map<String, Object> eOrderMap) {
		logger.info("*** ShopWebhookController >> processCreditMemoAfterOrderCreation >> [START] ");
		Gson gson = new GsonBuilder().setPrettyPrinting().create();

		try {
 
			String orderName = StringUtils.trimToEmpty((String) eOrderMap.get("name"));
			HashMap<String, Object> searchMap = new HashMap<>();
			searchMap.put("orderName", orderName);
			logger.info("*** ShopWebhookController >> processCreditMemoAfterOrderCreation >> orderName: " + orderName);
			Double totalDiscount = Double.valueOf((String) eOrderMap.get("total_discounts"));
			List<Map<String, Object>> discountCodes = (List<Map<String, Object>>) eOrderMap.get("discount_codes");
			logger.info("*** ShopWebhookController >> processCreditMemoAfterOrderCreation >> totalDiscount: "
					+ totalDiscount);
			logger.info("*** ShopWebhookController >> processCreditMemoAfterOrderCreation >> discountCodes: "
					+ discountCodes);
			
			
			if (totalDiscount > 1D && CollectionUtils.isNotEmpty(discountCodes)) {
				String customerNo = ShopifyUtil.getSOCustomerNoByAddress(eOrderMap);
				
				StringBuilder sbCodes = new StringBuilder();
				for (Map<String, Object> discntMap: discountCodes) {
					String discntCode = (String)discntMap.get("code"); 
					sbCodes.append("'").append(discntCode).append("',");
				}
				  
				String finalCodes = sbCodes.substring(0, sbCodes.length()-1);
				logger.info("*** ShopWebhookController >> processCreditMemoAfterOrderCreation >> finalCodes: "
						+ finalCodes);
				
				HashMap<String, Object> updateMap = new HashMap<String, Object>();
				updateMap.put("usageFlag", "Y");
				updateMap.put("orderName", orderName);
				updateMap.put("customerNo", customerNo);
			    updateMap.put("usageDate", new Date());
			    updateMap.put("discountCodeIn", finalCodes);
			    int updateResult = creditMemoMapper.updateCreditMemo(updateMap);
				logger.info("*** ShopWebhookController >> processCreditMemoAfterOrderCreation >> updateResult: " 
				+ updateResult);
				if (updateResult!=0) {
					
					HashMap<String, Object> searchCodeMap = new HashMap<String, Object>();
					searchCodeMap.put("customerNo", customerNo);
					searchCodeMap.put("orderNameIsNull", "true");
				    List<Map<String, Object>> discntCodes =  creditMemoMapper.getDiscountCodes(searchCodeMap);
					if (CollectionUtils.isNotEmpty(discntCodes)) {
					
						String jsonMeta = gson.toJson(discntCodes, List.class);
						Map<String, Object> rootMap = new LinkedHashMap<String, Object>();
						Map<String, Object> detailMap = new LinkedHashMap<String, Object>();

						detailMap.put("namespace", "credit_memo_discount");
						detailMap.put("key", customerNo);
						detailMap.put("value", jsonMeta);
						detailMap.put("type", "json");   
						rootMap.put("metafield", detailMap);
						rootMap.put("metaType", "SHOP");

						onlineShopService.updateShopMetafield(rootMap);
						
					}
					
				}
		 	}
			
			
			
		} catch (Throwable t) {
			logger.log(Level.SEVERE, t.getMessage(), t);
		}

		logger.info("*** ShopWebhookController >> processCreditMemoAfterOrderCreation >> [END] ");
	}

	private void processStaggeredPayAfterOrderCreation(Map<String, Object> eOrderMap) {

		logger.info("*** ShopWebhookController >> processStaggeredPayAfterOrderCreation >> [START] ");
		try {

			String orderName = StringUtils.trimToEmpty((String) eOrderMap.get("name"));
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			HashMap<String, Object> searchMap = new HashMap<>();
			searchMap.put("orderName", orderName);
			List<Map<String, Object>> staggeredDataList = issuanceMapper.getStaggeredIssuanceListByOrder(searchMap);
			if (CollectionUtils.isNotEmpty(staggeredDataList)) {
				boolean matchItemCount = false;

				Map<String, Object> issuanceData = staggeredDataList.get(0);
				String dbCustomerNo = (String) issuanceData.get("customerNo");
				int eItemCount = Integer.valueOf(String.valueOf(issuanceData.get("itemCount")));
				String eItemQuery = " select item_no FROM cms_db.staggered_payment_mst where customer_no = '"
						+ dbCustomerNo + "' " + "	and order_name is null";
				List<Map<String, Object>> eItemList = queryBuilderService.getExecQuery(eItemQuery);
				int dbItemCount = 0;
				if (CollectionUtils.isNotEmpty(eItemList)) {
					dbItemCount = eItemList.size();
				}

				if (eItemCount == dbItemCount) {
					matchItemCount = true;
				}
				logger.info("*** ShopWebhookController >> processStaggeredPayAfterOrderCreation >> orderName: "
						+ orderName);
				logger.info("*** ShopWebhookController >> processStaggeredPayAfterOrderCreation >> matchItemCount: "
						+ matchItemCount);

				if (matchItemCount) {
					boolean isFullPayment = PCDataUtil.isStagIssuanceFullPayment(eOrderMap);

					logger.info("*** ShopWebhookController >> processStaggeredPayAfterOrderCreation >> isFullPayment: "
							+ isFullPayment);

					if (isFullPayment) {
						// SCHEME - FULL PAYMENT
						// ***************************************************
						String promoCode = String.valueOf(issuanceData.get("promoCode"));
						String customerNo = String.valueOf(issuanceData.get("customerNo"));
						LocalDateTime issuedDateTime = (LocalDateTime) issuanceData.get("creationDate");
						Date issuedDate = DateUtil.convertToDateViaSqlTimestamp(issuedDateTime);
						String staggeredSchedId = String.valueOf(issuanceData.get("stgrdSchedId"));

						HashMap<String, Object> updateMap = new HashMap<>();
						updateMap.put("promoCode", promoCode);
						updateMap.put("customerNo", customerNo);
						updateMap.put("stgrdSchedId", staggeredSchedId);
						// FULL PAYMENT
						updateMap.put("orderName", orderName);
						updateMap.put("issuedFlag", "Y");
						updateMap.put("dateIssued", issuedDate);
						updateMap.put("payTermsCount", 0);

						int updResult = issuanceMapper.updateStaggeredIssuanceMaster(updateMap);
						boolean tagAsIssued = false;
						if (updResult != 0) {
							tagAsIssued = true;
						}

						if (tagAsIssued) {

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
							} catch (Throwable e) {
								logger.log(Level.SEVERE, e.getMessage(), e);
							}

							try {

								// DELETE - Payment Schedule DB bcoz of FULL payment
								// *************************************************************
								HashMap<String, Object> delMap = new HashMap<String, Object>();
								delMap.put("stgrdSchedId", staggeredSchedId);
								issuanceMapper.deleteStaggeredIssuanceSched(delMap);

								// DELETE - Payment Schedule META
								// *************************************************************
								Map<String, Object> rootSchedMap = new LinkedHashMap<String, Object>();
								Map<String, Object> detailSchedMap = new LinkedHashMap<String, Object>();

								detailSchedMap.put("namespace", "staggered_pay_next_sched");
								detailSchedMap.put("key", customerNo);
								detailSchedMap.put("value", "DELETED");
								detailSchedMap.put("type", "single_line_text_field");
								rootSchedMap.put("metafield", detailSchedMap);
								rootSchedMap.put("metaType", "SHOP");
								onlineShopService.updateShopMetafield(rootSchedMap);

								onlineOrderService.addOrderTagToCurrent(eOrderMap, "STAGGERED_PAY_ORDER");
							} catch (Throwable t) {
								logger.log(Level.SEVERE, t.getMessage(), t);
							}
						}
					} else {
						// SCHEME - STAGGERED PAYMENT 6 MONTHS
						// ***************************************************************
						String staggeredSchedId = null;
						for (Map<String, Object> issueData : staggeredDataList) {
							String promoCode = String.valueOf(issueData.get("promoCode"));
							String customerNo = String.valueOf(issueData.get("customerNo"));
							LocalDateTime issuedDateTime = (LocalDateTime) issueData.get("creationDate");
							Date issuedDate = DateUtil.convertToDateViaSqlTimestamp(issuedDateTime);
							double interestRate = NumberUtil.getDoubleValue(issueData, "interestRate");

							staggeredSchedId = String.valueOf(issueData.get("stgrdSchedId"));

							HashMap<String, Object> updateMap = new HashMap<>();
							updateMap.put("promoCode", promoCode);
							updateMap.put("customerNo", customerNo);
							updateMap.put("orderName", orderName);
							updateMap.put("issuedFlag", "Y");
							updateMap.put("dateIssued", issuedDate);

							int updResult = issuanceMapper.updateStaggeredIssuanceMaster(updateMap);
							boolean tagAsIssued = false;
							if (updResult != 0) {
								tagAsIssued = true;
							}

							logger.info(
									"*** ShopWebhookController >> processStaggeredPayAfterOrderCreation >> tagAsIssued: "
											+ tagAsIssued);
							// Initiate RE-Publishing of Issuance and REMOVE Meta
							if (tagAsIssued) {
								try {

									// DELETE - Issuance Data bcoz it's already issued
									Map<String, Object> rootMap = new LinkedHashMap<String, Object>();
									Map<String, Object> detailMap = new LinkedHashMap<String, Object>();

									detailMap.put("namespace", "staggered_pay_issuance");
									detailMap.put("key", customerNo);
									detailMap.put("value", "DELETED");
									detailMap.put("type", "single_line_text_field");
									rootMap.put("metafield", detailMap);
									rootMap.put("metaType", "SHOP");

									onlineShopService.updateShopMetafield(rootMap);
									logger.info(
											"*** ShopWebhookController >> processStaggeredPayAfterOrderCreation >> staggered_pay_issuance DELETED ");

									HashMap<String, Object> searchSchedMap = new HashMap<>();
									searchSchedMap.put("stgrdSchedId", staggeredSchedId);
									List<Map<String, Object>> schedList = issuanceMapper
											.getStaggeredSchedList(searchSchedMap);

									if (CollectionUtils.isNotEmpty(schedList)) {

										int payTerms = schedList.size();
										logger.info(
												"*** ShopWebhookController >> processStaggeredPayAfterOrderCreation >> payTerms: "
														+ payTerms);
										// Only get SUBTOTAL_PRICE because interest rate is applicable to ITEMS ONLY
										double totalOrderAmt = Double
												.parseDouble(String.valueOf(eOrderMap.get("subtotal_price")));
										double orderInterest = PCDataUtil.computeInterestValue(totalOrderAmt,
												interestRate);
										double shippingAmt = ShopifyUtil.getTotalShippingAmount(eOrderMap);

										totalOrderAmt = (totalOrderAmt + shippingAmt + orderInterest);
										double totalOrderPlusInterest = NumberUtil.roundTwoDec(totalOrderAmt);

										double splitPayAmt = NumberUtil.roundTwoDec((totalOrderAmt / payTerms));
										boolean paySchedUpdated = false;
										Date schedDate = new Date(issuedDate.getTime());
										int schedRow = 1;
										double splitPaymentTotal = 0;

										for (Map<String, Object> schedMap : schedList) {
											int schedPayId = NumberUtil.getIntValue(schedMap, "stgrdPayId");
											schedDate = DateUtil.getDateNowPlusTime(schedDate, Calendar.MONTH, 1);

											// This will enable the Exact payment VS total order amount

											if (schedRow == payTerms) {
												// LAST Payment for staggered
												double lastPayment = (totalOrderPlusInterest - splitPaymentTotal);
												logger.info(
														"*** ShopWebhookController >> processStaggeredPayAfterOrderCreation >> lastPayment: "
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
												logger.info(
														"*** ShopWebhookController >> processStaggeredPayAfterOrderCreation >> schedResult: "
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
										logger.info(
												"*** ShopWebhookController >> processStaggeredPayAfterOrderCreation >> paySchedUpdated: "
														+ paySchedUpdated);
										if (paySchedUpdated) {
											HashMap<String, Object> nxtMap = new HashMap<>();
											nxtMap.put("orderName", orderName);
											nxtMap.put("payStatus", "pending");
											List<Map<String, Object>> nextPayDetails = issuanceMapper
													.getNextPaymentDetails(nxtMap);
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
													Date nextPayDate = DateUtil
															.convertToDateViaSqlTimestamp(nextPayDateTime);
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

									}

									try {
										// TAG Order as STAGGERED_PAY_ORDER
										onlineOrderService.addOrderTagToCurrent(eOrderMap, "STAGGERED_PAY_ORDER");

										// Generate SO
										Map<String, Object> soMap = new HashMap<>();
										soMap.put("processPerOrder", true);
										soMap.put("orderName", orderName);
										soMap.put("staggeredPayOrder", true);
										syncD365Service.syncSalesOrderDataToDB(soMap);
									} catch (Throwable tt) {
										logger.log(Level.SEVERE, tt.getMessage(), tt);
									}

								} catch (Throwable e) {
									logger.log(Level.SEVERE, e.getMessage(), e);
								}

							}
						}
					}
				}

			}

		} catch (Throwable t) {
			logger.log(Level.SEVERE, t.getMessage(), t);
		}

		logger.info("*** ShopWebhookController >> processStaggeredPayAfterOrderCreation >> [END] ");

	}

	@PostMapping("/resetObjectHook")
	public ResponseEntity resetObjectHook(@RequestBody Map<String, Object> paramBody) {
		logger.info("*** ShopWebhookController >> resetObjectHook >> [START]");
		try {
			if (MapUtils.isNotEmpty(paramBody) && paramBody.containsKey("orderName")) {
				processOrders.remove(paramBody.get("orderName"));
				createdOrders.remove(paramBody.get("orderName"));
			} else {
				processOrders = new HashMap<>();
				createdOrders = new HashMap<>();
			}

		} catch (Exception e) {
			logger.info("** OnlineUPayController >> resetObjectHook >> ERROR: ");
			logger.log(Level.SEVERE, e.getMessage(), e);
		}

		logger.info("*** ShopWebhookController >> resetObjectHook >> processOrders: " + processOrders);
		logger.info("*** ShopWebhookController >> resetObjectHook >> [END]");
		return ResponseEntity.ok().build();
	}

	public Map<String, String> getProcessOrders() {
		return processOrders;
	}

	public void setProcessOrders(Map<String, String> processOrders) {
		this.processOrders = processOrders;
	}

	public Map<String, String> getCreatedOrders() {
		return createdOrders;
	}

	public void setCreatedOrders(Map<String, String> createdOrders) {
		this.createdOrders = createdOrders;
	}

	public Map<String, Double> getEwtConfig() {
		return ewtConfig;
	}

	public void setEwtConfig(Map<String, Double> ewtConfig) {
		this.ewtConfig = ewtConfig;
	}

}
