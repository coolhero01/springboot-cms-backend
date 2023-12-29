package com.oneclicktech.spring.service.impl;

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
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import com.oneclicktech.spring.domain.Constants;
import com.oneclicktech.spring.mapper.CustomerMapper;
import com.oneclicktech.spring.mapper.TableUtilMapper;
import com.oneclicktech.spring.service.OnlineCustomerService;
import com.oneclicktech.spring.service.RestTemplateService;
import com.oneclicktech.spring.util.DateUtil;
import com.oneclicktech.spring.util.NumberUtil;
import com.oneclicktech.spring.util.ShopifyUtil;

@Service
public class OnlineCustomerServiceImpl implements OnlineCustomerService {

	private static final Logger logger = Logger.getLogger("OnlineCustomerServiceImpl");

	@Autowired
	RestTemplateService restTemplateService;

	@Autowired
	CustomerMapper customerMapper;

	@Value("${pc.shopify.app.hosturl}")
	String hostUrl;

	@Value("${pc.shopify.app.webhook.version}")
	String apiVersion;

	@Value("${pc.shopify.app.rowlimit}")
	String rowLimit;

	@Autowired
	TableUtilMapper tableUtilMapper;

	@Override
	public List<Map<String, Object>> getCustomerList(Map<String, Object> paramMap) throws Throwable {
		logger.info("** OnlineCustomerServiceImpl >> getCustomerList >> [START]");

		Map<String, Object> reqParamMap = new HashMap<String, Object>();
		reqParamMap.putAll(paramMap);

		String prodRequestUrl = new StringBuilder("/admin/api/").append(apiVersion).append("/customers.json?limit=")
				.append(rowLimit).toString();

		Map<String, Object> resultMap = restTemplateService.sendGetRequest(prodRequestUrl, reqParamMap);
		if (MapUtils.isNotEmpty(resultMap) && resultMap.containsKey(Constants.CUSTOMERS)) {
			List<Map<String, Object>> dataList = (List<Map<String, Object>>) resultMap.get(Constants.CUSTOMERS);
			logger.info("** OnlineCustomerServiceImpl >> getCustomerList >> dataList: " + dataList.size());
			for (Map<String, Object> rowMap : dataList) {
				rowMap.put("tblId", rowMap.get("id"));

				List<Map<String, Object>> addressList = (List<Map<String, Object>>) rowMap.get("addresses");
				for (Map<String, Object> addrMap : addressList) {
					rowMap.putAll(addrMap);
					break;
				}

				String fullAddress = new StringBuilder(StringUtils.trimToEmpty((String) rowMap.get("address1")))
						.append(StringUtils.trimToEmpty((String) rowMap.get("address2"))).append(" ")
						.append(StringUtils.trimToEmpty((String) rowMap.get("city"))).append(" ")
						.append(StringUtils.trimToEmpty((String) rowMap.get("province"))).append(" ")
						.append(StringUtils.trimToEmpty((String) rowMap.get("zip"))).append(" ")
						.append(StringUtils.trimToEmpty((String) rowMap.get("country_code"))).toString();

				rowMap.put("fullAddress", fullAddress);

				if (StringUtils.isNotBlank((String) rowMap.get("first_name"))) {
					rowMap.put("fullName", new StringBuilder((String) rowMap.get("first_name")).append(" ")
							.append((String) rowMap.get("last_name")).toString());
				}

				logger.info("** OnlineCustomerServiceImpl >> getCustomerList >> rowMap: " + rowMap.toString());
			}
			return dataList;
		}

		logger.info("** OnlineCustomerServiceImpl >> getCustomerList >> [END]");
		return null;
	}

	@Override
	public List<Map<String, Object>> getAllOnlineCustomers(Map<String, Object> paramMap) throws Throwable {
		logger.info(" ** OnlineCustomerServiceImpl >> getAllOnlineCustomers >> [START]");

		String countRequestUrl = new StringBuilder("/admin/api/").append(apiVersion).append("/customers/count.json")
				.toString();
		Map<String, Object> countMap = restTemplateService.sendGetRequest(countRequestUrl, new HashMap<>());
		List<Map<String, Object>> allOnlineCust = new ArrayList<Map<String, Object>>();

		int totalCount = ((Double) countMap.get("count")).intValue();
		double maxPage = (Math.ceil((totalCount / Double.valueOf(rowLimit))));

		StringBuilder custRequestUrl = new StringBuilder("/admin/api/").append(apiVersion)
				.append("/customers.json?limit=").append(rowLimit);

		if (totalCount > Integer.valueOf(rowLimit)) {
			String custInfoParam = null;
			for (int ii = 0; ii < maxPage; ii++) {

				if (custInfoParam != null) {
					custRequestUrl.append("&page_info=").append(custInfoParam);
				}
				Map<String, Object> custMap = restTemplateService.sendGetRequestWithHeader(custRequestUrl.toString(),
						new HashMap<>());

				if (custMap.get("header") != null) {
					HttpHeaders headers = (HttpHeaders) custMap.get("header");
					if (headers != null) {
						List<String> links = headers.get("Link");
						if (links != null) {

							String link = links.get(0);
							custInfoParam = link.substring(link.indexOf("&page_info=") + 11, link.lastIndexOf('>'));
							System.out.println(custInfoParam);

							Map<String, Object> bodyMap = (Map<String, Object>) custMap.get("body");
							List<Map<String, Object>> pageCustomers = (List<Map<String, Object>>) bodyMap
									.get(Constants.CUSTOMERS);
							if (CollectionUtils.isNotEmpty(pageCustomers)) {
								logger.info(" ** OnlineCustomerServiceImpl >> pageCustomers: " + pageCustomers.size());
								allOnlineCust.addAll(pageCustomers);
							}

						}
					}
				}

			}

		} else {
			allOnlineCust.addAll(this.getCustomerList(new HashMap<>()));
		}

		List<Map<String, Object>> finalCustomerList = new ArrayList<Map<String, Object>>();
		for (Map<String, Object> rowMap : allOnlineCust) {
			String customerId = String.valueOf(NumberUtil.getLongValue(rowMap, "id"));
			String cTags = StringUtils.upperCase((String) rowMap.get("tags"));
			if (cTags.contains("TEST_USER")) {
				// DONT ADD - Its a TEST USER
				logger.info("rowMap: " + rowMap);
			} else {
				rowMap.put("customerId", customerId);
				finalCustomerList.add(rowMap);
			}
		}

		logger.info(" ** OnlineCustomerServiceImpl >> getAllOnlineCustomers >> [END]");
		return finalCustomerList;
	}

	@Override
	public Map<String, Object> getOneCustomerByEmail(String email) throws Throwable {
		logger.info("** OnlineCustomerServiceImpl >> getOneCustomerByEmail >> [START]");

		Map<String, Object> reqParamMap = new HashMap<String, Object>();

		String requestUrl = new StringBuilder("/admin/api/").append(apiVersion)
				.append("/customers/search.json?query=email:").append(email).toString();

		Map<String, Object> resultMap = restTemplateService.sendGetRequest(requestUrl, reqParamMap);
		if (MapUtils.isNotEmpty(resultMap) && resultMap.containsKey(Constants.CUSTOMERS)) {
			List<Map<String, Object>> customerList = (List<Map<String, Object>>) resultMap.get(Constants.CUSTOMERS);
			if (CollectionUtils.isNotEmpty(customerList)) {
				logger.info("** OnlineCustomerServiceImpl >> customerList: " + customerList.size());
				return customerList.get(0);
			}

		}
		logger.info("** OnlineCustomerServiceImpl >> getOneCustomerByEmail >> [END]");

		return null;
	}

	@Override
	public Map<String, Object> getOneCustomerByTag(String tag) throws Throwable {
		logger.info("** OnlineCustomerServiceImpl >> getOneCustomerByEmail >> [START]");

		Map<String, Object> reqParamMap = new HashMap<String, Object>();

		String requestUrl = new StringBuilder("/admin/api/").append(apiVersion)
				.append("/customers/search.json?query=tags:").append(tag).toString();

		Map<String, Object> resultMap = restTemplateService.sendGetRequest(requestUrl, reqParamMap);
		if (MapUtils.isNotEmpty(resultMap) && resultMap.containsKey(Constants.CUSTOMERS)) {
			List<Map<String, Object>> customerList = (List<Map<String, Object>>) resultMap.get(Constants.CUSTOMERS);
			if (CollectionUtils.isNotEmpty(customerList)) {
				logger.info("** OnlineCustomerServiceImpl >> customerList: " + customerList.size());
				return customerList.get(0);
			}

		}
		logger.info("** OnlineCustomerServiceImpl >> getOneCustomerByEmail >> [END]");

		return null;
	}

	@Override
	public List<Map<String, Object>> getAllOnlineAddressByID(String customerId) throws Throwable {
		logger.info("** OnlineCustomerServiceImpl >> getAllOnlineAddressByID >> [START]");

		Map<String, Object> reqParamMap = new HashMap<String, Object>();

		String requestUrl = new StringBuilder("/admin/api/").append(apiVersion).append("/customers/").append(customerId)
				.append("/addresses.json?limit=250").toString();

		Map<String, Object> resultMap = restTemplateService.sendGetRequest(requestUrl, reqParamMap);
		if (MapUtils.isNotEmpty(resultMap) && resultMap.containsKey(Constants.ADDRESSES)) {
			List<Map<String, Object>> addressList = (List<Map<String, Object>>) resultMap.get(Constants.ADDRESSES);
			if (CollectionUtils.isNotEmpty(addressList)) {
				logger.info("** OnlineCustomerServiceImpl >> addressList: " + addressList.size());
				return addressList;
			}

		}
		logger.info("** OnlineCustomerServiceImpl >> getAllOnlineAddressByID >> [END]");

		return null;
	}

	@Override
	public Map<String, Object> getOneCustomer(Long customerId) throws Throwable {
		logger.info("** OnlineCustomerServiceImpl >> getOneCustomer >> [START]");

		Map<String, Object> reqParamMap = new HashMap<String, Object>();

		String requestUrl = new StringBuilder("/admin/api/").append(apiVersion).append("/customers/").append(customerId)
				.append(".json").toString();

		Map<String, Object> resultMap = restTemplateService.sendGetRequest(requestUrl, reqParamMap);
		if (MapUtils.isNotEmpty(resultMap) && resultMap.containsKey(Constants.CUSTOMER)) {
			Map<String, Object> customerMap = (Map<String, Object>) resultMap.get(Constants.CUSTOMER);
			// tags
			if (StringUtils.isNotBlank((String) customerMap.get("tags"))) {
				String[] tags = ((String) customerMap.get("tags")).split(Constants.COMMA);
				for (String tag : tags) {
					if (tag.contains(Constants.CUSTOMER_NO_TAG_CODE)) {
						customerMap.put("customerNo", tag.substring(tag.indexOf('_') + 1));
					}
				}
			}

			logger.info("customerMap: " + customerMap);
			return customerMap;

		}
		logger.info("** OnlineCustomerServiceImpl >> getOneCustomer >> [END]");

		return null;
	}

	@Override
	public Map<String, Object> updateOnlineCustomer(Long eCustomerId, Map<String, Object> paramMap) throws Throwable {
		logger.info("** OnlineCustomerServiceImpl >> updateOnlineCustomer >> [START]");

		Map<String, Object> reqParamMap = new HashMap<String, Object>(); 
		reqParamMap.putAll(paramMap);
		String requestUrl = new StringBuilder("/admin/api/").append(apiVersion).append("/customers/").append(eCustomerId)
				.append(".json").toString();

		Map<String, Object> resultMap = restTemplateService.sendPutRequest(requestUrl, reqParamMap);
		if (MapUtils.isNotEmpty(resultMap) && resultMap.containsKey(Constants.CUSTOMER)) {
			Map<String, Object> customerMap = (Map<String, Object>) resultMap.get(Constants.CUSTOMER);
			return customerMap;
 		}
		logger.info("** OnlineCustomerServiceImpl >> updateOnlineCustomer >> [END]");

		return null;
	}
	
	
	
	
	@Override
	public Map<String, Object> updateCustomerAddress(Long eCustomerId, Long eAddressId, Map<String, Object> paramMap)
			throws Throwable {
		logger.info("** OnlineCustomerServiceImpl >> updateCustomerAddress >> [START]");
		Map<String, Object> rootMap = new HashMap<String, Object>(); 
		Map<String, Object> reqParamMap = new HashMap<String, Object>(); 
		reqParamMap.putAll(paramMap);
		rootMap.put("address", reqParamMap);
		
		String requestUrl = new StringBuilder("/admin/api/").append(apiVersion).append("/customers/").append(eCustomerId)
				.append("/addresses/").append(eAddressId).append(".json").toString();

		Map<String, Object> resultMap = restTemplateService.sendPutRequest(requestUrl, rootMap);
		if (MapUtils.isNotEmpty(resultMap) && resultMap.containsKey("customer_address")) {
			Map<String, Object> custAddressMap = (Map<String, Object>) resultMap.get("customer_address");
			return custAddressMap;
 		}
		logger.info("** OnlineCustomerServiceImpl >> updateCustomerAddress >> [END]");

		return null;
	}

	@Override
	public Map<String, Object> updateDBCustomerByOnline(Map<String, Object> paramMap) throws Throwable {
		logger.info("*** OnlineCustomerServiceImpl >> updateDBCustomerByOnline >> [START] ");
		List<Map<String, Object>> eCustomers = this.getAllOnlineCustomers(paramMap);
		for (Map<String, Object> eCustMap : eCustomers) {
			try {
				Long customerId = ShopifyUtil.getCustomerId(eCustMap);
				String email = (String) eCustMap.get("email");
				HashMap<String, Object> updateMap = new HashMap<>();
				updateMap.put("shopId", customerId);
				updateMap.put("email", email);
				int updResult = customerMapper.updateCustomer(updateMap);
				logger.info("*** OnlineCustomerServiceImpl >> updateDBCustomerByOnline >> email: " + email);
				logger.info("*** OnlineCustomerServiceImpl >> updateDBCustomerByOnline >> updResult: " + updResult);
			} catch (Exception e) {
				logger.log(Level.SEVERE, e.getMessage(), e);
			}
		}
		logger.info("*** OnlineCustomerServiceImpl >> updateDBCustomerByOnline >> [END] ");
		return null;
	}

	@Override
	public Map<String, Object> createCustomer(Map<String, Object> paramMap) throws Throwable {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, Object> deleteCustomer(Map<String, Object> paramMap) throws Throwable {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, Object> deleteCustomerById(String customerId) throws Throwable {
		logger.info("** OnlineCustomerServiceImpl >> deleteCustomerById >> [START]");

		Map<String, Object> reqParamMap = new HashMap<String, Object>();

		String requestUrl = new StringBuilder("/admin/api/").append(apiVersion).append("/customers/").append(customerId)
				.append(".json").toString();

		Map<String, Object> resultMap = restTemplateService.sendDeleteRequest(requestUrl, reqParamMap);
		if (MapUtils.isNotEmpty(resultMap) && resultMap.containsKey(Constants.CUSTOMER)) {
			return resultMap;
		}
		logger.info("** OnlineCustomerServiceImpl >> deleteCustomerById >> [END]");

		return null;
	}

	@Override
	public Map<String, Object> deleteCustomerAddressById(String customerId, String customerAddrId) throws Throwable {
		logger.info("** OnlineCustomerServiceImpl >> deleteCustomerAddressById >> [START]");

		Map<String, Object> reqParamMap = new HashMap<String, Object>();

		String requestUrl = new StringBuilder("/admin/api/").append(apiVersion).append("/customers/").append(customerId)
				.append("/addresses/").append(customerAddrId).append(".json").toString();

		Map<String, Object> resultMap = restTemplateService.sendDeleteRequest(requestUrl, reqParamMap);
		if (MapUtils.isNotEmpty(resultMap)) {
			return resultMap;
		}
		logger.info("** OnlineCustomerServiceImpl >> deleteCustomerAddressById >> [END]");

		return null;
	}

	@Override
	public Map<String, Object> cleansOnlineCustomerAndDB(Map<String, Object> paramMap) throws Throwable {

		try {
			logger.info("*************************************************  ");
			logger.info(" DELETE ONLINE Customers with Old ID in DB ");
			logger.info("*************************************************  ");
			List<Map<String, Object>> delAddrList = customerMapper.getOnlineCustomerAddrForDeletion(null);
			for (Map<String, Object> addrMap : delAddrList) {
				String eCustId = String.valueOf((Long) addrMap.get("eCustId"));
				String eCustAddrId = String.valueOf((Long) addrMap.get("eCustAddrId"));

				try {
					logger.info("** OnlineProductServiceImpl >> cleansOnlineProductAndDB >> DELETING..eCustId: "
							+ eCustId + " *** eCustAddrId: " + eCustAddrId);
					this.deleteCustomerAddressById(eCustId, eCustAddrId);
				} catch (Throwable t) {
					logger.log(Level.WARNING, t.getMessage(), t);
				}
			}

			logger.info("*************************************************  ");
			logger.info("DELETE ALL Old Product Data in DB ");
			logger.info("*************************************************  ");
			try {
				customerMapper.deleteOldCustomerDetailData();
				customerMapper.deleteOldCustomerAddressData();
			} catch (Throwable t) {
				logger.log(Level.WARNING, t.getMessage(), t);
			}

			logger.info("*************************************************  ");
			logger.info("DELETE ALL Online Customer NOT SYNC with DB");
			logger.info("*************************************************  ");
			List<Map<String, Object>> eCustomers = this.getAllOnlineCustomers(new HashMap<>());
			for (Map<String, Object> eCustMap : eCustomers) {
				String eCustEmail = StringUtils.trimToEmpty((String) eCustMap.get("email"));
				Long eCustomerId = ShopifyUtil.getCustomerId(eCustMap);

				HashMap<String, Object> searchMap = new HashMap<>();
				searchMap.put("email", eCustEmail);
				Map<String, Object> dbCustMap = customerMapper.getCustomerByEmail(searchMap);
				if (MapUtils.isEmpty(dbCustMap)) {
					// Online Customer NOT in DB
					logger.info("** OnlineProductServiceImpl >> cleansOnlineProductAndDB >> DELETING..eCustEmail: "
							+ eCustEmail);
					logger.info("** OnlineProductServiceImpl >> cleansOnlineProductAndDB >> DELETING..eCustomerId: "
							+ eCustomerId);
			 		try {
						this.deleteCustomerById(String.valueOf(eCustomerId));
					} catch (Throwable t) {
						logger.log(Level.WARNING, t.getMessage(), t);
					}
				}

			}

		} catch (Throwable t) { 
			logger.log(Level.SEVERE, t.getMessage(), t);
		}

		return null;
	}

	@Override
	public Map<String, Object> cleansOnlineCustomerAddress(String customerId, String customerAddrId) throws Throwable {

		return null;
	}

	@Override
	public Map<String, Object> cleansOnlineCustomerAddress() throws Throwable {

		HashMap<String, Object> mainMap = new HashMap<>();
		mainMap.put("oosInclude", "N");
		mainMap.put("withEmail", "true");
		StringBuilder sb = new StringBuilder();
		List<Map<String, Object>> allDBCustomers = customerMapper.getCustomerWithAddressList(mainMap);
		for (Map<String, Object> dbCustomer : allDBCustomers) {
			String dbCustNo = (String) dbCustomer.get("customerNumber");
			String dbEmail = (String) dbCustomer.get("email");
			Map<String, Object> eCustomer = this.getOneCustomerByEmail(dbEmail);
			logger.info("** OnlineProductController >> cleansOnlineCustomerAddress >> dbEmail: " + dbEmail);
			if (MapUtils.isNotEmpty(eCustomer)) {
				logger.info("** OnlineProductController >> cleansOnlineCustomerAddress >> eCustomer: " + eCustomer);
				String customerId = String.valueOf(ShopifyUtil.getCustomerId(eCustomer));
				List<Map<String, Object>> addressList = this.getAllOnlineAddressByID(customerId);
				if (addressList.size() > 1) {
					for (Map<String, Object> eAddrMap : addressList) {
						String addrCustNo = (String) eAddrMap.get("first_name");
						Long eCustAddrId = NumberUtil.getLongValue(eAddrMap, "id");
						if (addrCustNo.equals(dbCustNo)) {
							try {
								Map<String, Object> updMap = new LinkedHashMap<String, Object>();
								updMap.put("default", false); 
							    this.updateCustomerAddress(Long.valueOf(customerId), eCustAddrId, updMap);
								this.deleteCustomerAddressById(customerId, String.valueOf(eCustAddrId));
							} catch (Throwable t) {
								logger.log(Level.SEVERE, t.getMessage(), t);
							}
						}
					}
				} else {
					try {
						this.deleteCustomerById(customerId);
					} catch (Throwable t) {
						logger.log(Level.SEVERE, t.getMessage(), t);
					}
				}

			} else {
				logger.info("** OnlineProductController >> cleansOnlineCustomerAddress >> dbEmail: " + dbEmail);
				sb.append(dbEmail).append("\r\n");
			}
		}
		logger.info("** OnlineProductController >> cleansOnlineCustomerAddress >> sb: " + sb.toString());

		return null;
	}

	private boolean isInvalidAddress(Map<String, Object> currentAddrMap, List<Map<String, Object>> addressList) {
		boolean isInvalid = false;
		if (CollectionUtils.isNotEmpty(addressList)) {
			String currAddrCustNo = (String) currentAddrMap.get("first_name");
			String zipCode = StringUtils.trimToEmpty((String) currentAddrMap.get("zip"));
			if (StringUtils.isBlank(zipCode)) {
				return true;
			}

		}
		return isInvalid;
	}

	@Override
	public Map<String, Object> backupCustomerDBTable(Map<String, Object> paramMap) throws Throwable {
		logger.info("** OnlineProductServiceImpl >> backupCustomerDBTable >> [START] ");
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("MMddYYYY_HHmm");
			String dateTxt = sdf.format(new Date());

			if (Constants.TEST_ONLY) {
				Date prevDate = DateUtil.getDateNowPlusTime(Calendar.DAY_OF_YEAR, -7);
				dateTxt = sdf.format(prevDate);
			}

			HashMap<String, Object> inputMap = new HashMap<>();
			// PRODUCT DETAIL
			inputMap.put("newTable", "customer_".concat(dateTxt));
			inputMap.put("origTable", "customer");
			logger.info("** OnlineProductServiceImpl >> backupCustomerDBTable >> inputMap: " + inputMap.toString());
			tableUtilMapper.createTableCopy(inputMap);
			tableUtilMapper.insertTableCopy(inputMap);

			inputMap.put("newTable", "customer_address_".concat(dateTxt));
			inputMap.put("origTable", "customer_address");
			logger.info("** OnlineProductServiceImpl >> backupCustomerDBTable >> inputMap: " + inputMap.toString());
			tableUtilMapper.createTableCopy(inputMap);
			tableUtilMapper.insertTableCopy(inputMap);
		} catch (Throwable t) {
			 
			logger.log(Level.SEVERE, t.getMessage(), t);
		}
		
		logger.info("** OnlineProductServiceImpl >> backupCustomerDBTable >> [END] ");
		return null;
	}

}
