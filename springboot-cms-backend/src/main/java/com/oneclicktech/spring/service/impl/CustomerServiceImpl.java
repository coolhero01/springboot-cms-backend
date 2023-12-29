package com.oneclicktech.spring.service.impl;

import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oneclicktech.spring.mapper.CustomerAddressMapper;
import com.oneclicktech.spring.mapper.CustomerMapper;
import com.oneclicktech.spring.mapper.IssuanceMapper;
import com.oneclicktech.spring.mapper.ProductMasterMapper;
import com.oneclicktech.spring.service.CustomerService;
import com.oneclicktech.spring.service.ProductService;

@Service
public class CustomerServiceImpl implements CustomerService {

	private static final Logger logger = Logger.getLogger("CustomerServiceImpl");

	@Autowired
	CustomerMapper customerMapper;

	@Autowired
	CustomerAddressMapper customerAddressMapper;

	@Autowired
	IssuanceMapper issuanceMapper;

	@Override
	public boolean syncAzureCustomerToLocalDB() {
		logger.info("**** syncAzureCustomerToLocalDB >> [START] ");
		FileReader reader = null;
		ObjectMapper mapper = new ObjectMapper();
		try {
			File jsonFile = new File("/temp/SPAVI/temp_data/CUSTOMER.json");
			List<Map<String, Object>> jsonMapList = mapper.readValue(jsonFile,
					new TypeReference<List<Map<String, Object>>>() {
					});

			logger.info("**** syncAzureCustomerToLocalDB >>jsonMapList: " + jsonMapList.size());

			for (Map<String, Object> rowMap : jsonMapList) {
				logger.info(rowMap.toString());
				HashMap<String, Object> paramMap = new HashMap<>();
				paramMap.putAll(rowMap);
				paramMap.put("tblId", rowMap.get("$id"));

				customerMapper.insertCustomerForSync(paramMap);
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
		} finally {
			IOUtils.closeQuietly(reader);

		}
		logger.info("**** syncAzureCustomerToLocalDB >> [END] ");
		return false;
	}

	@Override
	public boolean syncAzureCustomerAddressToLocalDB() {
		logger.info("**** syncAzureCustomerAddressToLocalDB >> [START] ");
		FileReader reader = null;
		ObjectMapper mapper = new ObjectMapper();
		try {
			File jsonFile = new File("/temp/SPAVI/temp_data/CUSTOMER_ADDRESS.json");
			List<Map<String, Object>> jsonMapList = mapper.readValue(jsonFile,
					new TypeReference<List<Map<String, Object>>>() {
					});
			logger.info("**** syncAzureCustomerAddressToLocalDB >>jsonMapList: " + jsonMapList.size());

			for (Map<String, Object> rowMap : jsonMapList) {
				logger.info(rowMap.toString());
				HashMap<String, Object> paramMap = new HashMap<>();
				paramMap.putAll(rowMap);
				paramMap.put("tblId", rowMap.get("$id"));

				customerAddressMapper.insertCustomerAddressForSync(paramMap);
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
		} finally {
			IOUtils.closeQuietly(reader);

		}
		logger.info("**** syncAzureCustomerAddressToLocalDB >> [END] ");
		return false;
	}

	@Override
	public List<Map<String, Object>> getCustomerList(HashMap<String, Object> paramMap) {
		logger.info("**** getCustomerList >> [START] ");
		List<Map<String, Object>> customerList = customerMapper.getCustomerList(paramMap);

		logger.info("**** getCustomerList >> [END] ");
		return customerList;
	}

	@Override
	public List<Map<String, Object>> getCustomerAddressList(HashMap<String, Object> paramMap) {
		logger.info("**** getCustomerAddressList >> [START] ");
		List<Map<String, Object>> customerList = customerAddressMapper.getCustomerAddressList(paramMap);

		logger.info("**** getCustomerAddressList >> [END] ");
		return customerList;
	}

	@Override
	public List<Map<String, Object>> getStoresByWarehouse(HashMap<String, Object> paramMap) {
		logger.info("**** getStoresByWarehouse >> [START] ");
		List<Map<String, Object>> storeList = customerAddressMapper.getStoresByWarehouse(paramMap);
		logger.info("**** getStoresByWarehouse >> [END] ");
		return storeList;
	}

	@Override
	public boolean checkIfPromoDataExist(String tableName, 
			String promoCode, String customerNo) {
		Map<String, Object> dataMap = issuanceMapper.getOnePromoDataByCustomer( tableName, 
				  promoCode,   customerNo);
		if (MapUtils.isNotEmpty(dataMap)) {
			return true;
		}
		return false;
	}

//	@Override
//	public void updateCustomerDBIdByEmail(HashMap<String, Object> paramMap) {
//		HashMap<String, Object> searchMap = new HashMap<>();
//		searchMap.put("withPhone", "true");
//		searchMap.put("withEmail", "true");
//		List<Map<String, Object>> dbCustomers = customerMapper.getCustomerWithAddressList(paramMap);
//		for (Map<String, Object> custMap: dbCustomers) {
//			String email = (String)custMap.get("email");
//			
//			store.myshopify.com/admin/customers/search.json?query=email:name@domain.com;
//			
//			
//		}
//		 
//	}

}
