package com.oneclicktech.spring.service.impl;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.oneclicktech.spring.domain.Constants;
import com.oneclicktech.spring.mapper.CustomerAddressMapper;
import com.oneclicktech.spring.mapper.CustomerMapper;
import com.oneclicktech.spring.mapper.ProductDetailMapper;
import com.oneclicktech.spring.mapper.ProductInventoryMapper;
import com.oneclicktech.spring.mapper.TableUtilMapper;
import com.oneclicktech.spring.service.OnlineCustomerService;
import com.oneclicktech.spring.service.OnlineProductService;
import com.oneclicktech.spring.service.OnlineShopService;
import com.oneclicktech.spring.service.RestTemplateService;
import com.oneclicktech.spring.util.DateUtil;
import com.oneclicktech.spring.util.HelperUtil;
import com.oneclicktech.spring.util.NumberUtil;
import com.oneclicktech.spring.util.PCDataUtil;
import com.oneclicktech.spring.util.ShopifyUtil;

@Service
public class OnlineShopServiceImpl implements OnlineShopService {

	private static final Logger logger = Logger.getLogger("OnlineShopServiceImpl");

	@Value("${pc.shopify.app.hosturl}")
	String hostUrl;

	@Value("${pc.shopify.app.webhook.version}")
	String apiVersion;

	@Value("${pc.shopify.app.rowlimit}")
	String rowLimit;

	@Value("${pc.online.local.image-path}")
	String pcImagePath;

	@Autowired
	ProductDetailMapper productDetailMapper;

	@Autowired
	ProductInventoryMapper productInventoryMapper;

	@Autowired
	CustomerMapper customerMapper;

	@Autowired
	CustomerAddressMapper customerAddressMapper;

	@Autowired
	RestTemplateService restTemplateService;

	@Autowired
	OnlineProductService onlineProductService;

	@Autowired
	OnlineCustomerService onlineCustomerService;

	@Autowired
	TableUtilMapper tableUtilMapper;

	@Override
	public boolean syncLocalProductToOnline(Map<String, Object> paramMap) throws Throwable {
		logger.info("*** syncLocalProductToOnline >> [START] ");
		HashMap<String, Object> searchMap = new HashMap<>();
		searchMap.put("oosStatus", "Y");
		searchMap.putAll(paramMap);

		List<Map<String, Object>> dbProdList = productDetailMapper.getProductsWithInventory(searchMap);
		int rowCtr = 0;

		for (Map<String, Object> prodMap : dbProdList) {

			String itemNo = (String) prodMap.get("itemNumber");
			String warehouseCode = (String) prodMap.get("warehouseCode");
			Long shopProdId = (Long) prodMap.get("shopProdId");
			String oosStatus = (String) prodMap.get("oosStatus");

			logger.info("*** syncLocalProductToOnline >> prodMap: " + prodMap);
			logger.info("*** syncLocalProductToOnline >> oosStatus: " + oosStatus);
			logger.info("*** syncLocalProductToOnline >> shopProdId: " + shopProdId);

			// BUILD Request Product Map

			if (shopProdId != null && shopProdId > 0L) {
				// UPDATE PRODUCT
				// ***********************************
				try {
					Map<String, Object> eProdMap = onlineProductService.getOneProduct(Long.valueOf(shopProdId));
					if (MapUtils.isNotEmpty(eProdMap)) {
						String prodRequestUrl = new StringBuilder("/admin/api/").append(apiVersion).append("/products/")
								.append(shopProdId).append(".json").toString();
						/*
						 * UPDATE the following fields: Tags: Inventory Images
						 */
						Map<String, Object> requestProdMap = ShopifyUtil.buildProductRequestForUpdate(prodMap,
								eProdMap);
						logger.info("*** syncLocalProductToOnline >> eProdMap: " + eProdMap);
						Map<String, Object> updatedProdMap = restTemplateService.sendPutRequest(prodRequestUrl,
								requestProdMap);
						logger.info("*** syncLocalProductToOnline >> updatedProdMap: " + updatedProdMap);
						Map<String, Object> imgMap = (Map<String, Object>) eProdMap.get("image");
						if (MapUtils.isEmpty(imgMap)) {
							// Product has NO IMAGE
							onlineProductService.createProductImage(eProdMap, prodMap);
						}

					} else {
						try {
							HashMap<String, Object> delMap = new HashMap<>();
							delMap.put("shopProdId", shopProdId);
							int delResult = productInventoryMapper.deleteProductInventory(delMap);
							logger.info("*** syncLocalProductToOnline >> delResult: " + delResult);
							this.createNewProduct(prodMap, itemNo, warehouseCode);
						} catch (Throwable t) {
							logger.log(Level.SEVERE, t.getMessage(), t);
						}
					}
				} catch (Exception e) {
					logger.log(Level.SEVERE, e.getMessage(), e);
				}

			} else {
				// CREATE NEW PRODUCT
				// ***********************************
				try {
					this.createNewProduct(prodMap, itemNo, warehouseCode);
				} catch (Throwable t) {
					logger.log(Level.SEVERE, t.getMessage(), t);
				}
			}

			logger.info("*** syncLocalProductToOnline >> PRODUCT ROW: " + rowCtr);
			rowCtr++;
		}

		logger.info("*** syncLocalProductToOnline >> [END] ");
		return false;
	}

	@Override
	public boolean syncAlleProductsById(Map<String, Object> paramMap) throws Throwable {
		logger.info("*** OnlineShopServiceImpl >> syncAlleProductsById >> [START]");
		List<Map<String, Object>> eProducts = onlineProductService.getAllOnlineProducts(paramMap);
		for (Map<String, Object> eProdMap : eProducts) {
			try {
				Long productId = NumberUtil.getLongValue(eProdMap, "id");
				HashMap<String, Object> searchMap = new HashMap<>();
				searchMap.put("shopProdId", productId);
				Map<String, Object> resultMap = productInventoryMapper.getProductInventoryById(searchMap);
				if (MapUtils.isEmpty(resultMap)) {
					// DELETE ONLINE Product IF Id does NOT EXIST in DB
					Map<String, Object> deleteMap = onlineProductService.deleteProduct(eProdMap);
					if (MapUtils.isNotEmpty(deleteMap)) {
						logger.info("*** OnlineShopServiceImpl >> syncAlleProductsById >> DELETED: " + deleteMap);
					}
				}
			} catch (Exception e) {
				logger.log(Level.SEVERE, e.getMessage(), e);
			}

		}

		logger.info("*** OnlineShopServiceImpl >> syncAlleProductsById >> [END]");
		return false;
	}

	private void createNewProduct(Map<String, Object> dbProdMap, String itemNo, String warehouseCode) throws Throwable {

		// CREATE NEW PRODUCT
		// ***********************************
		logger.info("*** syncLocalProductToOnline >> createNewProduct >> [START]");
		Map<String, Object> requestProdMap = ShopifyUtil.buildProductMapRequest(dbProdMap);
		String prodRequestUrl = new StringBuilder("/admin/api/").append(apiVersion).append("/products.json").toString();
		try {
			Map<String, Object> createdProdMap = restTemplateService.sendPostRequest(prodRequestUrl, requestProdMap);
			logger.info("*** syncLocalProductToOnline >> createNewProduct >> createdProdMap: " + createdProdMap);
			if (MapUtils.isNotEmpty(createdProdMap)) {
				Map<String, Object> newProdMap = (Map<String, Object>) createdProdMap.get("product");
				Long productId = NumberUtil.getLongValue(newProdMap, "id");
				/*
				 * UPDATE the Product Inventory with SHOP_PROD_ID value
				 */
				HashMap<String, Object> invMap = new HashMap<>();
				invMap.put("itemNumber", itemNo);
				invMap.put("warehouse", warehouseCode);
				invMap.put("shopProdId", productId);
				int updateResult = productInventoryMapper.updateProductInventory(invMap);
				logger.info("*** syncLocalProductToOnline >> updateResult: " + updateResult);
				if (MapUtils.isNotEmpty(newProdMap)) {
					onlineProductService.createProductImage(newProdMap, dbProdMap);
				}

			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
		}
		logger.info("*** syncLocalProductToOnline >> createNewProduct >> [END]");
	}

	@Override
	public boolean syncLocalCustomerToOnline(Map<String, Object> paramMap) throws Throwable {
		logger.info("*** syncLocalCustomerToOnline >> [START] ");
		HashMap<String, Object> cParamMap = new HashMap<>();
		cParamMap.put("emptyShopId", "true");
		List<Map<String, Object>> customerList = customerMapper.getCustomerWithAddressList(cParamMap);
		int rowCtr = 0;
		for (Map<String, Object> dataMap : customerList) {
			Map<String, Object> requestDataMap = ShopifyUtil.buildCustomerMapRequest(dataMap, rowCtr);
			String customerNo = (String) dataMap.get("customerNumber");

			logger.info("*** syncLocalCustomerToOnline >> customerNo: " + customerNo);

			String apiRequestUrl = new StringBuilder("/admin/api/").append(apiVersion).append("/customers.json")
					.toString();
			try {
				Map<String, Object> createdDataMap = restTemplateService.sendPostRequest(apiRequestUrl, requestDataMap);
				logger.info("*** syncLocalCustomerToOnline >> createdDataMap: " + createdDataMap);
				String address = (String) dataMap.get("address");
				if (MapUtils.isNotEmpty(createdDataMap) && StringUtils.isNotBlank(address)) {
					Map<String, Object> newDataMap = (Map<String, Object>) createdDataMap.get("customer");
					BigDecimal customerId = new BigDecimal((Double) newDataMap.get("id"));

					HashMap<String, Object> updateMap = new HashMap<>();
					updateMap.put("customerNumber", customerNo);
					updateMap.put("shopId", String.valueOf(customerId.longValue()));
					int updateResult = customerMapper.updateCustomer(updateMap);
					logger.info("*** syncLocalCustomerToOnline >> updateResult: " + updateResult);

					String addrRequestUrl = new StringBuilder("/admin/api/").append(apiVersion).append("/customers/")
							.append(customerId.longValue()).append("/addresses.json").toString();

					Map<String, Object> requestAddrMap = ShopifyUtil.buildCustomerAddressMapRequest(dataMap);
					Map<String, Object> createdAddrMap = restTemplateService.sendPostRequest(addrRequestUrl,
							requestAddrMap);
					logger.info("*** syncLocalProductToOnline >> createdAddrMap: " + createdAddrMap);
				}
			} catch (Exception e) {
				logger.log(Level.SEVERE, e.getMessage(), e);
			}

			if (Constants.TEST_ONLY) {
				if (rowCtr >= 5) {
					break;
				}
			}

			rowCtr++;
		}
		logger.info("*** syncLocalCustomerToOnline >> [END] ");
		return false;
	}

	public boolean syncLocalCustomerToOnline_WithGroupTag(Map<String, Object> paramMap) throws Throwable {
		logger.info("*** syncLocalCustomerToOnline >> [START] ");
		List<Map<String, Object>> customerList = customerMapper.getCustomerWithAddressList(null);
		Map<String, List<Map<String, Object>>> mainMap = new LinkedHashMap<String, List<Map<String, Object>>>();
		List<Map<String, Object>> addressList = new ArrayList<Map<String, Object>>();
		int rowCtr = 0;
		for (Map<String, Object> custMap : customerList) {
			String customerNo = (String) custMap.get("customerNumber");
			String groupCustNo = (String) custMap.get("groupCustomerNo");
			if (StringUtils.isBlank(groupCustNo)) {
				groupCustNo = PCDataUtil.getGroupCustomerNo(customerNo);
			}

			String firstName = (String) custMap.get("firstName");
			String fullName = (String) custMap.get("fullName");
			if (StringUtils.isBlank(firstName) && StringUtils.isNotBlank(fullName)) {
				HashMap<String, Object> nameMap = PCDataUtil.getFirstLastNameFromFullName(fullName);
				custMap.putAll(nameMap);
			}

			custMap.put("groupCustNo", groupCustNo);

			if (rowCtr == 0) {
				addressList = new ArrayList<Map<String, Object>>();
				addressList.add(custMap);
				mainMap.put(groupCustNo, addressList);
			} else {
				// 1 mainMap = abc123 | groupCustNo = abc111
				// 2 mainMap = abc123 | groupCustNo = abc123
				if (mainMap.containsKey(groupCustNo)) {
					logger.info(rowCtr + " SAME GROUP: " + groupCustNo);
					addressList.add(custMap);
				} else {
					logger.info(rowCtr + " NEW:  " + groupCustNo);
					addressList = new ArrayList<Map<String, Object>>();
					addressList.add(custMap);
					mainMap.put(groupCustNo, addressList);
				}

			}

			rowCtr++;
		}

		int grpCtr = 1;
		if (Constants.TEST_ONLY) {
			for (Map.Entry<String, List<Map<String, Object>>> entry : mainMap.entrySet()) {
				logger.info(grpCtr + " KEY:" + entry.getKey());
				logger.info(grpCtr + " VALUES:" + entry.getValue());
				grpCtr++;
			}
		}

		int dataCtr = 0;
		for (Map.Entry<String, List<Map<String, Object>>> entry : mainMap.entrySet()) {

			String groupCustNo = entry.getKey();
			List<Map<String, Object>> custAddrList = entry.getValue();
			Map<String, Object> custAddrMap = custAddrList.get(0);

			String apiRequestUrl = new StringBuilder("/admin/api/").append(apiVersion).append("/customers.json")
					.toString();

			Map<String, Object> requestDataMap = ShopifyUtil.buildCustomerMapRequest(custAddrMap, dataCtr);
			Map<String, Object> createdDataMap = restTemplateService.sendPostRequest(apiRequestUrl, requestDataMap);
			logger.info("*** syncLocalCustomerToOnline >> createdDataMap: " + createdDataMap);

			Map<String, Object> newDataMap = (Map<String, Object>) createdDataMap.get("customer");
			BigDecimal customerId = new BigDecimal((Double) newDataMap.get("id"));
			String addrRequestUrl = new StringBuilder("/admin/api/").append(apiVersion).append("/customers/")
					.append(customerId.longValue()).append("/addresses.json").toString();

			// Create/Request ADDRESSES for Customer
			if (custAddrList.size() > 1) {
				// Customer with MULTIPLE Address
				for (Map<String, Object> addressMap : custAddrList) {
					Map<String, Object> requestAddrMap = ShopifyUtil.buildCustomerAddressMapRequest(addressMap);
					Map<String, Object> createdAddrMap = restTemplateService.sendPostRequest(addrRequestUrl,
							requestAddrMap);
					logger.info("*** syncLocalProductToOnline >> MULTIPLE: createdAddrMap: " + createdAddrMap);
				}
			} else {
				// Single Address
				Map<String, Object> requestAddrMap = ShopifyUtil.buildCustomerAddressMapRequest(custAddrMap);
				Map<String, Object> createdAddrMap = restTemplateService.sendPostRequest(addrRequestUrl,
						requestAddrMap);
				logger.info("*** syncLocalProductToOnline >> SINGLE: createdAddrMap: " + createdAddrMap);
			}

			dataCtr++;
		}

		logger.info("*** syncLocalCustomerToOnline >> [END] ");
		return false;
	}

	@Override
	public boolean syncLocalCustomerToOnlineByEmail(Map<String, Object> paramMap) throws Throwable {

		logger.info("*** syncLocalCustomerToOnlineByEmail >> [START] ");
		HashMap<String, Object> searchMap = new HashMap<>();
		searchMap.putAll(paramMap);
		if (Constants.TEST_ONLY) {
			searchMap.put("email", "kerwin.gundran@gmail.com");
			// searchMap.put("email", "kgundran@gmail.com");
		}
		List<Map<String, Object>> customerList = customerMapper.getCustomerWithAddressList(searchMap);
		Map<String, List<Map<String, Object>>> mainMap = new LinkedHashMap<String, List<Map<String, Object>>>();
		List<Map<String, Object>> addressList = new ArrayList<Map<String, Object>>();
		int rowCtr = 0;
		for (Map<String, Object> custMap : customerList) {
			String customerNo = (String) custMap.get("customerNumber");
			String email = StringUtils.trimToEmpty((String) custMap.get("email"));

			String firstName = (String) custMap.get("firstName");
			String fullName = (String) custMap.get("fullName");
			if (StringUtils.isBlank(firstName) && StringUtils.isNotBlank(fullName)) {
				HashMap<String, Object> nameMap = PCDataUtil.getFirstLastNameFromFullName(fullName);
				custMap.putAll(nameMap);
			}

			if (rowCtr == 0) {
				addressList = new ArrayList<Map<String, Object>>();
				addressList.add(custMap);
				mainMap.put(email, addressList);
			} else {
				// 1 mainMap = abc123 | email = abc111@gmail.com
				// 2 mainMap = abc123 | email = abc123@gmail.com
				if (mainMap.containsKey(email)) {
					logger.info(rowCtr + " SAME GROUP EMAIL: " + email);
					addressList.add(custMap);
				} else {
					logger.info(rowCtr + " NEW:  " + email);
					addressList = new ArrayList<Map<String, Object>>();
					addressList.add(custMap);
					mainMap.put(email, addressList);
				}

			}

			rowCtr++;
		}

		int dataCtr = 0;
		int testCtr = 0;
		for (Map.Entry<String, List<Map<String, Object>>> entry : mainMap.entrySet()) {

			String emailKey = entry.getKey();

			List<Map<String, Object>> dbCustAddrList = entry.getValue();
			Map<String, Object> custAddrMap = dbCustAddrList.get(0);
			String d365CustomerNo = (String) custAddrMap.get("customerNumber");

			try {
				logger.info("*** syncLocalCustomerToOnlineByEmail >> emailKey: " + emailKey);
				Map<String, Object> eCustomer = onlineCustomerService.getOneCustomerByEmail(emailKey);
				logger.info("*** syncLocalCustomerToOnlineByEmail >> eCustomer: " + eCustomer);
 				
				HelperUtil.viewInJSON(eCustomer);
 
 				if (MapUtils.isNotEmpty(eCustomer)) {
					// UPDATE CUSTOMER
					logger.info("*** syncLocalCustomerToOnlineByEmail >> UPDATE CUSTOMER ");

					Long eCustomerId = ShopifyUtil.getCustomerId(eCustomer);

					HashMap<String, Object> updateMap = new HashMap<>();
					updateMap.put("email", emailKey);
					updateMap.put("shopId", String.valueOf(eCustomerId));
					int updateResult = customerMapper.updateCustomerByEmail(updateMap);
					logger.info("*** syncLocalCustomerToOnlineByEmail >> updateResult: " + updateResult);
					if (updateResult != 0) {
						try {
							// UPDATE Online Customer
							Map<String, Object> reqCustomerMap = ShopifyUtil.buildCustomerMapRequestForUpdate(custAddrMap);
							Map<String, Object> updateResultMap = onlineCustomerService.updateOnlineCustomer(eCustomerId,
									reqCustomerMap);
							logger.info("*** syncLocalCustomerToOnlineByEmail >> updateResultMap: " + updateResultMap);
						} catch (Throwable t) {
							logger.log(Level.SEVERE, t.getMessage(), t);
						}
						 
					}

					// List<Map<String, Object>> eCustAddrList = (List<Map<String, Object>>)
					// eCustomer.get("addresses");
					List<Map<String, Object>> eCustAddrList = onlineCustomerService
							.getAllOnlineAddressByID(String.valueOf(eCustomerId));

					if (CollectionUtils.isNotEmpty(eCustAddrList)) {

						if (eCustAddrList.size() != dbCustAddrList.size()) {
							// Address NOT in DB ....CREATE the Address
							logger.info( "*** syncLocalCustomerToOnlineByEmail >>**********************************************************");
							logger.info( "*** syncLocalCustomerToOnlineByEmail >> ADDRESS Count NOT in SYNC , possible ERROR....CREATE NEW ACCT ");
							logger.info( "*** syncLocalCustomerToOnlineByEmail >>**********************************************************");
							this.fixAddressAcctNotInSync(String.valueOf(eCustomerId), dbCustAddrList, eCustAddrList);

						} else {

							for (Map<String, Object> eAddrMap : eCustAddrList) {
								Long eCustAddrId = NumberUtil.getLongValue(eAddrMap, "id");
								String addrCustNo = (String) eAddrMap.get("first_name");
								HashMap<String, Object> updAddrMap = new HashMap<>();
								updAddrMap.put("customerNumber", addrCustNo);
								updAddrMap.put("shopAddrId", String.valueOf(eCustAddrId));

								int addrResult = customerAddressMapper.updateCustomerAddress(updAddrMap);
								logger.info("*** syncLocalCustomerToOnlineByEmail >> addrResult: " + addrResult);

								try {
									// POST Address Update
									String apiRequestUrl = new StringBuilder("/admin/api/").append(apiVersion)
											.append("/customers/").append(eCustomerId).append("/addresses/")
											.append(eCustAddrId).append(".json").toString();

									logger.info(
											"*** syncLocalCustomerToOnlineByEmail >> apiRequestUrl: " + apiRequestUrl);
									Map<String, Object> dbAddrMap = ShopifyUtil.getMatchAddressByCustomerNo(addrCustNo,
											dbCustAddrList);
									logger.info("*** syncLocalCustomerToOnlineByEmail >> addrCustNo: " + addrCustNo);
									logger.info("*** syncLocalCustomerToOnlineByEmail >> dbAddrMap: " + dbAddrMap);
									if (MapUtils.isNotEmpty(dbAddrMap)) {
										// UPDATE Address
										Map<String, Object> requestUpdateMap = ShopifyUtil
												.buildCustomerAddressForUpdate(dbAddrMap, eAddrMap);
										Map<String, Object> updatedDataMap = restTemplateService
												.sendPutRequest(apiRequestUrl, requestUpdateMap);
										logger.info("*** syncLocalCustomerToOnlineByEmail >> updatedDataMap: "
												+ updatedDataMap);
									} else {
										// Address NOT in DB ....CREATE the Address
										logger.info(
												"*** syncLocalCustomerToOnlineByEmail >>**********************************************************");
										logger.info(
												"*** syncLocalCustomerToOnlineByEmail >> Address NOT in DB, possible ERROR....CREATE NEW ACCT ");
										logger.info(
												"*** syncLocalCustomerToOnlineByEmail >>**********************************************************");
										// DELETE Existing Online User with Invalid data
//										Map<String, Object> deleteResult = onlineCustomerService.deleteCustomerById(String.valueOf(eCustomerId));
//										// CREATE New Acct for the User
//										this.createNewCustomerAccount(emailKey, d365CustomerNo, custAddrMap,  
//												custAddrList, dataCtr);
//										break;
									}
								} catch (Exception e) {
									logger.log(Level.SEVERE, e.getMessage(), e);
								}

							}
						}

					} else {
						this.fixEmptyOnlineAddress(String.valueOf(eCustomerId), dbCustAddrList);
					}

				} else {
					logger.info("*** syncLocalCustomerToOnlineByEmail >> CREATE CUSTOMER .......");
					this.createNewCustomerAccount(emailKey, d365CustomerNo, custAddrMap, dbCustAddrList, dataCtr);
				}

			} catch (Exception e) {
				logger.log(Level.SEVERE, e.getMessage(), e);
			}

			dataCtr++;
		}

		logger.info("*** syncLocalCustomerToOnlineByEmail >> [END] ");
		return false;
	}

	private void fixAddressAcctNotInSync(String eCustomerId, List<Map<String, Object>> dbCustAddrList,
			List<Map<String, Object>> eCustAddrList) throws Throwable {
		logger.info("*** syncLocalCustomerToOnlineByEmail >> fixAddressAcctNotInSync >> [START] ");
		int dbSize = dbCustAddrList.size();
		int eSize = eCustAddrList.size();

		String addrRequestUrl = new StringBuilder("/admin/api/").append(apiVersion).append("/customers/")
				.append(eCustomerId).append("/addresses.json").toString();

		logger.info("*** syncLocalCustomerToOnlineByEmail >> dbSize: " + dbSize);
		logger.info("*** syncLocalCustomerToOnlineByEmail >> eSize: " + eSize);

		if (dbSize > eSize) {
			// CREATE The Online Address from DB
			for (Map<String, Object> dbAddrMap : dbCustAddrList) {
				String dbAddrCustNo = StringUtils.trimToEmpty((String) dbAddrMap.get("customerNumber"));
				boolean matchCustomerNo = false;
				for (Map<String, Object> eAddrMap : eCustAddrList) {
					String eAddrCustNo = StringUtils.trimToEmpty((String) eAddrMap.get("first_name"));
					if (dbAddrCustNo.equals(eAddrCustNo)) {
						matchCustomerNo = true;
						break;
					}
				}

				if (!matchCustomerNo) {
					// NO MATCH - Create Online Address
					Map<String, Object> requestAddrMap = ShopifyUtil.buildCustomerAddrMapRequestByEmail(dbAddrMap);
					Map<String, Object> createdAddrMap = restTemplateService.sendPostRequest(addrRequestUrl,
							requestAddrMap);

					Map<String, Object> eGenAddrMap = (Map<String, Object>) createdAddrMap
							.get(Constants.CUSTOMER_ADDRESS);

					Long custAddrId = NumberUtil.getLongValue(eGenAddrMap, "id");

					HashMap<String, Object> updAddrMap = new HashMap<>();
					updAddrMap.put("customerNumber", dbAddrCustNo);
					updAddrMap.put("shopAddrId", String.valueOf(custAddrId));

					int addrResult = customerAddressMapper.updateCustomerAddress(updAddrMap);
					logger.info("*** syncLocalCustomerToOnlineByEmail >> fixAddressAcctNotInSync: addrResult: "
							+ addrResult);
				}

			}
		}

		if (eSize > dbSize) {

			for (Map<String, Object> eAddrMap : eCustAddrList) {
				String eAddrCustNo = StringUtils.trimToEmpty((String) eAddrMap.get("first_name"));
				String eCustAddrId = String.valueOf(NumberUtil.getLongValue(eAddrMap, "id"));
				boolean matchCustomerNo = false;
				for (Map<String, Object> dbAddrMap : dbCustAddrList) {
					String dbAddrCustNo = StringUtils.trimToEmpty((String) dbAddrMap.get("customerNumber"));
					if (dbAddrCustNo.equals(eAddrCustNo)) {
						matchCustomerNo = true;
						break;
					}
				}

				if (!matchCustomerNo) {
					// Address NOT EXIST in DB so DELETE it
					boolean isDefault = (Boolean) eAddrMap.get("default");
					if (isDefault) {
						// SET Others as DEFAULT
						this.setOtherAddressAsDefault(eCustAddrList, eCustAddrId, eCustomerId);
						onlineCustomerService.deleteCustomerAddressById(eCustomerId, eCustAddrId);
					} else {
						onlineCustomerService.deleteCustomerAddressById(eCustomerId, eCustAddrId);
					}

				}

			}
		}

		logger.info("*** syncLocalCustomerToOnlineByEmail >> fixAddressAcctNotInSync >> [END] ");
	}

	private Map<String, Object> setOtherAddressAsDefault(List<Map<String, Object>> eCustAddrList, String eCustAddrId,
			String eCustomerId) throws Throwable {
		logger.info("*** syncLocalCustomerToOnlineByEmail >> setOtherAddressAsDefault >> [START] ");

		for (Map<String, Object> eAddrMap : eCustAddrList) {
			String currentAddrId = String.valueOf(NumberUtil.getLongValue(eAddrMap, "id"));
			boolean isDefault = (Boolean) eAddrMap.get("default");
			if (!currentAddrId.equals(eCustAddrId) && !isDefault) {
				// SET DEFAULT Address
//				/admin/api/2023-01/customers/207119551/addresses/1053317287/default.json
				String apiRequestUrl = new StringBuilder("/admin/api/").append(apiVersion).append("/customers/")
						.append(eCustomerId).append("/addresses/").append(currentAddrId).append("/default.json")
						.toString();

				Map<String, Object> updatedAddrap = restTemplateService.sendPutRequest(apiRequestUrl, new HashMap<>());
				logger.info("*** syncLocalCustomerToOnlineByEmail >> setOtherAddressAsDefault:  " + updatedAddrap);
				return eAddrMap;
			}

		}

		logger.info("*** syncLocalCustomerToOnlineByEmail >> setOtherAddressAsDefault >> [END] ");
		return null;
	}

	private void fixEmptyOnlineAddress(String eCustomerId, List<Map<String, Object>> dbCustAddrList) throws Throwable {
		logger.info("*** syncLocalCustomerToOnlineByEmail >> fixEmptyOnlineAddress >> [START] ");
		String addrRequestUrl = new StringBuilder("/admin/api/").append(apiVersion).append("/customers/")
				.append(eCustomerId).append("/addresses.json").toString();

		// Customer with MULTIPLE Address
		for (Map<String, Object> addressMap : dbCustAddrList) {

			Map<String, Object> requestAddrMap = ShopifyUtil.buildCustomerAddrMapRequestByEmail(addressMap);
			Map<String, Object> createdAddrMap = restTemplateService.sendPostRequest(addrRequestUrl, requestAddrMap);

			Map<String, Object> eAddrMap = (Map<String, Object>) createdAddrMap.get(Constants.CUSTOMER_ADDRESS);
			String addrCustomerNo = (String) addressMap.get("customerNumber");
			Long custAddrId = NumberUtil.getLongValue(eAddrMap, "id");

			HashMap<String, Object> updAddrMap = new HashMap<>();
			updAddrMap.put("customerNumber", addrCustomerNo);
			updAddrMap.put("shopAddrId", String.valueOf(custAddrId));

			int addrResult = customerAddressMapper.updateCustomerAddress(updAddrMap);

			logger.info("*** syncLocalCustomerToOnlineByEmail >> fixEmptyOnlineAddress: addrResult: " + addrResult);
		}

		logger.info("*** syncLocalCustomerToOnlineByEmail >> fixEmptyOnlineAddress >> [END] ");

	}

	private void createNewCustomerAccount(String emailKey, String d365CustomerNo, Map<String, Object> custAddrMap,
			List<Map<String, Object>> custAddrList, int dataCtr) throws Throwable {
		logger.info("*** syncLocalCustomerToOnlineByEmail >> createNewCustomerAccount >> [START] ");
		// CREATE NEW CUSTOMER
		if (Constants.TEST_ONLY) {
			// continue;
		}
		String apiRequestUrl = new StringBuilder("/admin/api/").append(apiVersion).append("/customers.json").toString();

		Map<String, Object> requestDataMap = ShopifyUtil.buildCustomerMapRequest(custAddrMap, dataCtr);
		Map<String, Object> createdDataMap = restTemplateService.sendPostRequest(apiRequestUrl, requestDataMap);
		logger.info("*** syncLocalCustomerToOnlineByEmail >> createdDataMap: " + createdDataMap);

		Map<String, Object> newDataMap = (Map<String, Object>) createdDataMap.get("customer");
		// BigDecimal customerId = new BigDecimal((Double) newDataMap.get("id"));
		Long customerId = NumberUtil.getLongValue(newDataMap, "id");
		HashMap<String, Object> updateMap = new HashMap<>();
		// UPDATED to EMAIL bcoz 1 email = 1 account
		// updateMap.put("customerNumber", customerNo);
		updateMap.put("email", emailKey);
		updateMap.put("shopId", String.valueOf(customerId));
		int updateResult = customerMapper.updateCustomerByEmail(updateMap);
		logger.info("*** syncLocalCustomerToOnlineByEmail >> updateResult: " + updateResult);

		String addrRequestUrl = new StringBuilder("/admin/api/").append(apiVersion).append("/customers/")
				.append(customerId).append("/addresses.json").toString();

		if (Constants.TEST_ONLY) {

		}
		// CREATE/Request ADDRESSES for Customer
		if (custAddrList.size() > 1) {
			// Customer with MULTIPLE Address
			for (Map<String, Object> addressMap : custAddrList) {

				Map<String, Object> requestAddrMap = ShopifyUtil.buildCustomerAddrMapRequestByEmail(addressMap);
				Map<String, Object> createdAddrMap = restTemplateService.sendPostRequest(addrRequestUrl,
						requestAddrMap);

				Map<String, Object> eAddrMap = (Map<String, Object>) createdAddrMap.get(Constants.CUSTOMER_ADDRESS);
				String addrCustomerNo = (String) addressMap.get("customerNumber");
				Long custAddrId = NumberUtil.getLongValue(eAddrMap, "id");

				HashMap<String, Object> updAddrMap = new HashMap<>();
				updAddrMap.put("customerNumber", addrCustomerNo);
				updAddrMap.put("shopAddrId", String.valueOf(custAddrId));

				int addrResult = customerAddressMapper.updateCustomerAddress(updAddrMap);

				logger.info("*** syncLocalCustomerToOnlineByEmail >> MULTIPLE: addrResult: " + addrResult);
				logger.info("*** syncLocalCustomerToOnlineByEmail >> MULTIPLE: createdAddrMap: " + createdAddrMap);
			}

		} else {
			// Single Address
			Map<String, Object> requestAddrMap = ShopifyUtil.buildCustomerAddrMapRequestByEmail(custAddrMap);
			Map<String, Object> createdAddrMap = restTemplateService.sendPostRequest(addrRequestUrl, requestAddrMap);
			Map<String, Object> eAddrMap = (Map<String, Object>) createdAddrMap.get(Constants.CUSTOMER_ADDRESS);
			Long custAddrId = NumberUtil.getLongValue(eAddrMap, "id");

			HashMap<String, Object> updAddrMap = new HashMap<>();
			updAddrMap.put("customerNumber", d365CustomerNo);
			updAddrMap.put("shopAddrId", String.valueOf(custAddrId));

			int addrResult = customerAddressMapper.updateCustomerAddress(updAddrMap);

			logger.info("*** syncLocalCustomerToOnlineByEmail >> SINGLE: createdAddrMap: " + createdAddrMap);
			logger.info("*** syncLocalCustomerToOnlineByEmail >> SINGLE: addrResult: " + addrResult);
		}
		logger.info("*** syncLocalCustomerToOnlineByEmail >> createNewCustomerAccount >> [END] ");
	}

	public boolean syncLocalCustomerToOnline_ORIG(Map<String, Object> paramMap) throws Throwable {
		logger.info("*** syncLocalCustomerToOnline_ORIG >> [START] ");
		List<Map<String, Object>> customerList = customerMapper.getCustomerWithAddressList(null);
		int rowCtr = 0;
		for (Map<String, Object> dataMap : customerList) {
			Map<String, Object> requestDataMap = ShopifyUtil.buildCustomerMapRequest(dataMap, rowCtr);

			String apiRequestUrl = new StringBuilder("/admin/api/").append(apiVersion).append("/customers.json")
					.toString();
			try {
				Map<String, Object> createdDataMap = restTemplateService.sendPostRequest(apiRequestUrl, requestDataMap);
				logger.info("*** syncLocalCustomerToOnline_ORIG >> createdDataMap: " + createdDataMap);
				String address = (String) dataMap.get("address");
				if (MapUtils.isNotEmpty(createdDataMap) && StringUtils.isNotBlank(address)) {
					Map<String, Object> newDataMap = (Map<String, Object>) createdDataMap.get("customer");
					BigDecimal customerId = new BigDecimal((Double) newDataMap.get("id"));

					String addrRequestUrl = new StringBuilder("/admin/api/").append(apiVersion).append("/customers/")
							.append(customerId.longValue()).append("/addresses.json").toString();

					Map<String, Object> requestAddrMap = ShopifyUtil.buildCustomerAddressMapRequest(dataMap);
					Map<String, Object> createdAddrMap = restTemplateService.sendPostRequest(addrRequestUrl,
							requestAddrMap);
					logger.info("*** syncLocalCustomerToOnline_ORIG >> createdAddrMap: " + createdAddrMap);
				}
			} catch (Exception e) {
				logger.log(Level.SEVERE, e.getMessage(), e);
			}
			rowCtr++;
		}
		logger.info("*** syncLocalCustomerToOnline >> [END] ");
		return false;
	}

	@Override
	public boolean syncLocalLocationToOnline(Map<String, Object> paramMap) throws Throwable {

		List<Map<String, Object>> localProdList = productDetailMapper.getProductsWithInventory(null);
		for (Map<String, Object> prodMap : localProdList) {
			Map<String, Object> requestProdMap = ShopifyUtil.buildProductMapRequest(prodMap);

			String prodRequestUrl = new StringBuilder("/admin/api/").append(apiVersion).append("/products.json")
					.toString();
			try {
				Map<String, Object> createdProdMap = restTemplateService.sendPostRequest(prodRequestUrl,
						requestProdMap);
				logger.info("*** syncLocalLocationToOnline >> createdProdMap: " + createdProdMap);
				if (MapUtils.isNotEmpty(createdProdMap)) {
					Map<String, Object> newProdMap = (Map<String, Object>) createdProdMap.get("product");
					BigDecimal productId = new BigDecimal((Double) newProdMap.get("id"));

					String imgRequestUrl = new StringBuilder("/admin/api/").append(apiVersion).append("/products/")
							.append(productId.longValue()).append("/images.json").toString();

					Map<String, Object> requestImgMap = ShopifyUtil.buildProductImageRequest(pcImagePath, prodMap,
							newProdMap);
					Map<String, Object> createdImgMap = restTemplateService.sendPostRequest(imgRequestUrl,
							requestImgMap);
					logger.info("*** syncLocalLocationToOnline >> createdImgMap: " + createdImgMap);
				}
			} catch (Exception e) {
				logger.log(Level.SEVERE, e.getMessage(), e);
			}

		}
		return false;
	}

	@Override
	public void cleanBackupTables(Map<String, Object> paramMap) throws Throwable {

		try {
			String dbSchema = "cms_db";

			SimpleDateFormat sdf = new SimpleDateFormat("_MMddYYYY_");
			List<String> deleteDateKeys = new ArrayList<String>();
			for (int ii = 15; ii >= 4; ii--) {
				Date prevDate = DateUtil.getDateNowPlusTime(Calendar.DAY_OF_YEAR, -ii);
				String dateKey = sdf.format(prevDate);
				deleteDateKeys.add(dateKey);
			}

			List<String> dropBackupTables = new ArrayList<String>();
			HashMap<String, Object> execMap = new HashMap<>();

			execMap.put("txtQuery", "SHOW TABLES in " + dbSchema + ";");
			List<Map<String, Object>> tableList = tableUtilMapper.selectQuery(execMap);
			for (Map<String, Object> tblMap : tableList) {
				String tableName = (String) tblMap.get("Tables_in_".concat(dbSchema));
				for (String delDateKey : deleteDateKeys) {
					if (tableName.contains(delDateKey)) {
						logger.info("** OnlineShopServiceImpl >> tableName:" + tableName);
						dropBackupTables.add(tableName);
					}
				}
			}

			for (String dropBkpTable : dropBackupTables) {
				HashMap<String, Object> dropMap = new HashMap<>();

				String dropSQL = new StringBuilder(" DROP TABLE IF EXISTS ").append(dbSchema).append(".")
						.append(dropBkpTable).append(";").toString();

				dropMap.put("txtQuery", dropSQL);
				logger.info("** OnlineShopServiceImpl >> DROPPING Tables...: " + dropMap.toString());
				tableUtilMapper.deleteQuery(dropMap);
			}

			logger.info("** OnlineShopServiceImpl >> runTestJob >> [END]");
		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
		} catch (Throwable t) {
			logger.log(Level.SEVERE, t.getMessage(), t);
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.oneclicktech.spring.service.OnlineShopService#deleteShopMetafield(java.
	 * util.Map)
	 */
	@Override
	public void deleteShopMetafield(String namespace, String key) throws Throwable {
		logger.info("** OnlineShopServiceImpl >> deleteShopMetafield >> [START]");
		try {
			String requestUrl = new StringBuilder("/admin/api/").append(apiVersion).append("/metafields.json")
					.toString();

			Map<String, Object> reqParamMap = new LinkedHashMap<>();

			/*
			 * GET ALL SHOP METAFIELDS
			 */
			Map<String, Object> resultMap = restTemplateService.sendGetRequest(requestUrl, reqParamMap);
			if (MapUtils.isNotEmpty(resultMap)) {

				List<Map<String, Object>> shopMetafields = (List<Map<String, Object>>) resultMap.get("metafields");
				for (Map<String, Object> shopMeta : shopMetafields) {
					String metaNamespace = (String) shopMeta.get("namespace");
					String metaKey = (String) shopMeta.get("key");
					long metaId = NumberUtil.getLongValue(shopMeta, "id");
					logger.info("** OnlineShopServiceImpl >>  META: " + metaNamespace + " *** " + metaKey);
					if (namespace.equals(metaNamespace) && key.equals(metaKey)) {
						logger.info("** OnlineShopServiceImpl >> DELETING....: " + namespace + " *** " + key);
						// MATCH META - DELETE
						String deleteUrl = new StringBuilder("/admin/api/").append(apiVersion).append("/metafields/")
								.append(metaId).append(".json").toString();
						Map<String, Object> delMap = restTemplateService.sendDeleteRequest(deleteUrl, reqParamMap);
						logger.info("** OnlineShopServiceImpl >> delMap: " + delMap);

					}

				}
			}

		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
		} catch (Throwable t) {
			logger.log(Level.SEVERE, t.getMessage(), t);
		}
		logger.info("** OnlineShopServiceImpl >> deleteShopMetafield >> [END]");
	}

	@Override
	public void updateShopMetafield(Map<String, Object> paramMap) throws Throwable {
		logger.info("** OnlineShopServiceImpl >> updateShopMetafield >> [START]");
		try {
			String requestUrl = null;
			Map<String, Object> resultMap = new LinkedHashMap<>();
			Map<String, Object> reqParamMap = new LinkedHashMap<>();

			String metaType = (String) paramMap.get("metaType");
			switch (metaType) {
			case "SHOP":
				requestUrl = new StringBuilder("/admin/api/").append(apiVersion).append("/metafields.json").toString();
				reqParamMap.putAll(paramMap);
				reqParamMap.remove("metaType");

				resultMap = restTemplateService.sendPostRequest(requestUrl, reqParamMap);
				break;

			default:
				break;
			}

			logger.info("** OnlineShopServiceImpl >> resultMap: " + resultMap);
		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
		} catch (Throwable t) {
			logger.log(Level.SEVERE, t.getMessage(), t);
		}
		logger.info("** OnlineShopServiceImpl >> updateShopMetafield >> [END]");
	}

}
