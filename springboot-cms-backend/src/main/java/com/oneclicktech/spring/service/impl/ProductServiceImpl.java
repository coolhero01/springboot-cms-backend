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

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oneclicktech.spring.mapper.ProductDetailMapper;
import com.oneclicktech.spring.mapper.ProductInventoryMapper;
import com.oneclicktech.spring.mapper.ProductMasterMapper;
import com.oneclicktech.spring.mapper.WarehouseMapper;
import com.oneclicktech.spring.service.ProductService;

@Service
public class ProductServiceImpl implements ProductService {

	private static final Logger logger = Logger.getLogger("ProductServiceImpl");

	@Autowired
	ProductMasterMapper productMasterMapper;

	@Autowired
	ProductDetailMapper productDetailMapper;

	@Autowired
	ProductInventoryMapper productInventoryMapper;

	@Autowired
	WarehouseMapper warehouseMapper;

	@Override
	public boolean syncAzureProductToLocalDB() {

		logger.info("**** syncAzureProductToLocalDB >> [START] ");
		FileReader reader = null;
		ObjectMapper mapper = new ObjectMapper();
		try {
			File jsonFile = new File("C:\\Temp\\SPAVI\\TXT_JSON_COPY.json");
			List<Map<String, Object>> productMapList = mapper.readValue(jsonFile,
					new TypeReference<List<Map<String, Object>>>() {
					});
			logger.info("**** syncAzureProductToLocalDB >>productMapList: " + productMapList.size());
			for (Map<String, Object> prodMap : productMapList) {
				// System.out.println(prodMap.toString());
				HashMap<String, Object> paramMap = new HashMap<>();
				paramMap.putAll(prodMap);
				paramMap.put("prdId", prodMap.get("$id"));
				Date costModifiedDateTime = DateUtils.parseDate((String) prodMap.get("costModifiedDateTime"),
						"MM/dd/yyyy HH:mm:ss a");
				paramMap.put("costModifiedDateTime", costModifiedDateTime);

				Date purchModifiedDateTime = DateUtils.parseDate((String) prodMap.get("purchModifiedDateTime"),
						"MM/dd/yyyy HH:mm:ss a");
				paramMap.put("purchModifiedDateTime", purchModifiedDateTime);

				Date salesModifiedDateTime = DateUtils.parseDate((String) prodMap.get("salesModifiedDateTime"),
						"MM/dd/yyyy HH:mm:ss a");
				paramMap.put("salesModifiedDateTime", salesModifiedDateTime);

				Date modifiedDateTime = DateUtils.parseDate((String) prodMap.get("modifiedDateTime"),
						"MM/dd/yyyy HH:mm:ss a");
				paramMap.put("modifiedDateTime", modifiedDateTime);

				Date createdDateTime = DateUtils.parseDate((String) prodMap.get("createdDateTime"),
						"MM/dd/yyyy HH:mm:ss a");
				paramMap.put("createdDateTime", createdDateTime);

				System.out.println(paramMap.toString());
				productMasterMapper.insertProductMaster(paramMap);
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
		} finally {
			IOUtils.closeQuietly(reader);

		}
		logger.info("**** syncAzureProductToLocalDB >> [END] ");
		return false;
	}

	@Override
	public boolean syncAzureProductDetailToLocalDB() {
		logger.info("**** syncAzureProductDetailToLocalDB >> [START] ");
		FileReader reader = null;
		ObjectMapper mapper = new ObjectMapper();
		try {
			File jsonFile = new File("/temp/SPAVI/temp_data/PROD_DETAIL.json");
			List<Map<String, Object>> jsonMapList = mapper.readValue(jsonFile,
					new TypeReference<List<Map<String, Object>>>() {
					});
			logger.info("**** syncAzureProductToLocalDB >>jsonMapList: " + jsonMapList.size());

			for (Map<String, Object> rowMap : jsonMapList) {
				logger.info(rowMap.toString());
				HashMap<String, Object> paramMap = new HashMap<>();
				paramMap.putAll(rowMap);
				paramMap.put("tblId", rowMap.get("$id"));

				productDetailMapper.insertProductDetailForSync(paramMap);
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
		} finally {
			IOUtils.closeQuietly(reader);

		}
		logger.info("**** syncAzureProductDetailToLocalDB >> [END] ");
		return false;
	}

	@Override
	public List<Map<String, Object>> getProductList(HashMap<String, Object> paramMap) {
		logger.info("**** getProductList >> [START] ");
		List<Map<String, Object>> productList = productMasterMapper.getProductList(paramMap);

		logger.info("**** getProductList >> [END] ");
		return productList;
	}

	@Override
	public boolean syncAzureProductInventoryToLocalDB() {
		logger.info("**** syncAzureProductInventoryToLocalDB >> [START] ");
		FileReader reader = null;
		ObjectMapper mapper = new ObjectMapper();
		try {
			File jsonFile = new File("/temp/SPAVI/temp_data/PROD_INVENTORY_LIST.json");
			List<Map<String, Object>> jsonMapList = mapper.readValue(jsonFile,
					new TypeReference<List<Map<String, Object>>>() {
					});
			logger.info("**** syncAzureProductInventoryToLocalDB >>jsonMapList: " + jsonMapList.size());

			for (Map<String, Object> rowMap : jsonMapList) {
				logger.info(rowMap.toString());
				HashMap<String, Object> paramMap = new HashMap<>();
				paramMap.putAll(rowMap);
				paramMap.put("tblId", rowMap.get("$id"));

				productInventoryMapper.insertProductInventoryForSync(paramMap);
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
		} finally {
			IOUtils.closeQuietly(reader);

		}
		logger.info("**** syncAzureProductInventoryToLocalDB >> [END] ");
		return false;
	}

	@Override
	public boolean syncAzureWarehouseToLocalDB() {
		logger.info("**** syncAzureWarehouseToLocalDB >> [START] ");
		FileReader reader = null;
		ObjectMapper mapper = new ObjectMapper();
		try {
			File jsonFile = new File("/temp/SPAVI/temp_data/WAREHOUSE.json");
			List<Map<String, Object>> jsonMapList = mapper.readValue(jsonFile,
					new TypeReference<List<Map<String, Object>>>() {
					});
			logger.info("**** syncAzureWarehouseToLocalDB >>jsonMapList: " + jsonMapList.size());

			for (Map<String, Object> rowMap : jsonMapList) {
				logger.info(rowMap.toString());
				HashMap<String, Object> paramMap = new HashMap<>();
				paramMap.putAll(rowMap);
				paramMap.put("tblId", rowMap.get("$id"));

				warehouseMapper.insertWarehouseForSync(paramMap);
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
		} finally {
			IOUtils.closeQuietly(reader);

		}
		logger.info("**** syncAzureWarehouseToLocalDB >> [END] ");
		return false;
	}

	@Override
	public List<Map<String, Object>> getProductDetailList(HashMap<String, Object> paramMap) {
		logger.info("**** getProductDetailList >> [START] ");
		List<Map<String, Object>> productList = productDetailMapper.getProductList(paramMap);
		logger.info("**** getProductDetailList >> [END] ");
		return productList;
	}

	@Override
	public List<Map<String, Object>> getProductInventoryList(HashMap<String, Object> paramMap) {
		logger.info("**** getProductInventoryList >> [START] ");
		List<Map<String, Object>> dataList = productInventoryMapper.getProductInventoryList(paramMap);
		logger.info("**** getProductInventoryList >> [END] ");
		return dataList;
	}

	@Override
	public List<Map<String, Object>> getWarehouseList(HashMap<String, Object> paramMap) {
		logger.info("**** getWarehouseList >> [START] ");
		List<Map<String, Object>> dataList = warehouseMapper.getWarehouseList(paramMap);
		logger.info("**** getWarehouseList >> [END] ");
		return dataList;
	}

	@Override
	public List<Map<String, Object>> getShopProductList(HashMap<String, Object> paramMap) {
		// TODO Auto-generated method stub
		return null;
	}

}
