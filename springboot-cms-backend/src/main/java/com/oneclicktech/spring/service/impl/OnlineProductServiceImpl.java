package com.oneclicktech.spring.service.impl;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import com.oneclicktech.spring.domain.Constants;
import com.oneclicktech.spring.mapper.ProductDetailMapper;
import com.oneclicktech.spring.mapper.ProductInventoryMapper;
import com.oneclicktech.spring.mapper.TableUtilMapper;
import com.oneclicktech.spring.service.OnlineProductService;
import com.oneclicktech.spring.service.QueryBuilderService;
import com.oneclicktech.spring.service.RestTemplateService;
import com.oneclicktech.spring.util.HelperUtil;
import com.oneclicktech.spring.util.NumberUtil;
import com.oneclicktech.spring.util.PCFileUtil;
import com.oneclicktech.spring.util.ShopifyUtil;

@Service
public class OnlineProductServiceImpl implements OnlineProductService {

	private static final Logger logger = Logger.getLogger("OnlineProductServiceImpl");

	@Autowired
	RestTemplateService restTemplateService;

	@Autowired
	ProductInventoryMapper productInventoryMapper;

	@Autowired
	ProductDetailMapper productDetailMapper;
	
	@Autowired
	QueryBuilderService queryBuilderService;
	 
	@Autowired
	TableUtilMapper tableUtilMapper;

	@Value("${pc.shopify.app.hosturl}")
	String hostUrl;

	@Value("${pc.shopify.app.webhook.version}")
	String apiVersion;

	@Value("${pc.shopify.app.rowlimit}")
	String rowLimit;

	@Value("${pc.shopify.app.location-id}")
	String pcShopifyLocationId;

	@Value("${pc.shopify.app.image.url-path}")
	String imageUrlPath;

	@Override
	public List<Map<String, Object>> getProductList(Map<String, Object> paramMap) throws Throwable {
		logger.info("** OnlineProductServiceImpl >> getProductList >> [START]");

		Map<String, Object> reqParamMap = new HashMap<String, Object>();
		reqParamMap.putAll(paramMap);

		String prodRequestUrl = new StringBuilder("/admin/api/").append(apiVersion).append("/products.json?limit=")
				.append(rowLimit).toString();

		Map<String, Object> resultMap = restTemplateService.sendGetRequest(prodRequestUrl, reqParamMap);
		if (MapUtils.isNotEmpty(resultMap) && resultMap.containsKey(Constants.PRODUCTS)) {
			List<Map<String, Object>> productList = (List<Map<String, Object>>) resultMap.get(Constants.PRODUCTS);
			logger.info("** OnlineProductServiceImpl >> getProductList >> productList: " + productList.size());
			for (Map<String, Object> prodMap : productList) {
				List<Map<String, Object>> variants = (List<Map<String, Object>>) prodMap.get("variants");
				Map<String, Object> prodVariant = variants.get(0);
				prodMap.put("tblId", prodMap.get("id"));
				prodMap.put("productId", prodMap.get("id"));
				prodMap.put("itemId", prodMap.get("itemId"));
				prodMap.put("variantPrice", prodVariant.get("price"));
				prodMap.put("variantSku", prodVariant.get("sku"));
				prodMap.put("variantInventoryQty", prodVariant.get("inventory_quantity"));

				// https://potato-corner-uat.myshopify.com/products/flavor-chili-barbeque-220g
				String productUrl = new StringBuilder(hostUrl).append("/products/")
						.append((String) prodMap.get("handle")).toString();

				prodMap.put("productUrl", productUrl);

				Map<String, Object> imgMap = (Map<String, Object>) prodMap.get("image");
				if (MapUtils.isNotEmpty(imgMap)) {
					prodMap.put("imgSrc", imgMap.get("src"));
				}
				// tags=PC00000002, POTATOES, WHC_JTCDAVCOM
				if (StringUtils.isNotBlank((String) prodMap.get("tags"))) {
					String[] tags = ((String) prodMap.get("tags")).split(Constants.COMMA);
					for (String tag : tags) {
						if (tag.contains(Constants.WARESHOUSE_CODE_TAG_CODE)) {
							prodMap.put("warehouseCode", tag.substring(tag.indexOf('_') + 1));
						}
						if (tag.contains(Constants.WARESHOUSE_SITE_TAG_CODE)) {
							prodMap.put("warehouseSite", tag.substring(tag.indexOf('_') + 1));
						}
						if (tag.contains(Constants.UNIT_OF_MEASURE_TAG_CODE)) {
							prodMap.put("uom", tag.substring(tag.indexOf('_') + 1));
						}
					}
				}

				logger.info("prodMap: " + prodMap);
			}
			return productList;
		}
		logger.info("** OnlineProductServiceImpl >> getProductList >> [END]");
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Map<String, Object>> getAllOnlineProducts(Map<String, Object> paramMap) throws Throwable {
		logger.info(" ** OnlineProductServiceImpl >> getAllProductOnAllPages >> [START]");

		String countRequestUrl = new StringBuilder("/admin/api/").append(apiVersion).append("/products/count.json")
				.toString();
		Map<String, Object> countMap = restTemplateService.sendGetRequest(countRequestUrl, new HashMap<>());
		List<Map<String, Object>> allOnlineProds = new ArrayList<Map<String, Object>>();

		int totalCount = ((Double) countMap.get("count")).intValue();
		double maxPage = (Math.ceil((totalCount / Double.valueOf(rowLimit))));

		StringBuilder prodRequestUrl = new StringBuilder("/admin/api/").append(apiVersion)
				.append("/products.json?limit=").append(rowLimit);

		if (totalCount > Integer.valueOf(rowLimit)) {
			String prodInfoParam = null;
			for (int ii = 0; ii < maxPage; ii++) {

				if (prodInfoParam != null) {
					prodRequestUrl.append("&page_info=").append(prodInfoParam);
				}

				Map<String, Object> prodMap = restTemplateService.sendGetRequestWithHeader(prodRequestUrl.toString(),
						new HashMap<>());
				if (prodMap.get("header") != null) {
					HttpHeaders headers = (HttpHeaders) prodMap.get("header");
					if (headers != null) {
						List<String> links = headers.get("Link");
						if (links != null) {

							String link = links.get(0);
							prodInfoParam = link.substring(link.indexOf("&page_info=") + 11, link.lastIndexOf('>'));
							System.out.println(prodInfoParam);

							Map<String, Object> bodyMap = (Map<String, Object>) prodMap.get("body");
							List<Map<String, Object>> pageProducts = (List<Map<String, Object>>) bodyMap
									.get(Constants.PRODUCTS);
							if (CollectionUtils.isNotEmpty(pageProducts)) {
								logger.info(" ** OnlineProductServiceImpl >> pageProducts: " + pageProducts.size());
								allOnlineProds.addAll(pageProducts);
							}

						}
					}
				}

			}

			for (Map<String, Object> prodMap : allOnlineProds) {
				List<Map<String, Object>> variants = (List<Map<String, Object>>) prodMap.get("variants");
				Map<String, Object> prodVariant = variants.get(0);
				prodMap.put("tblId", prodMap.get("id"));
				prodMap.put("productId", prodMap.get("id"));
				prodMap.put("itemId", prodMap.get("itemId"));
				prodMap.put("variantPrice", prodVariant.get("price"));
				prodMap.put("variantSku", prodVariant.get("sku"));
				prodMap.put("variantInventoryQty", prodVariant.get("inventory_quantity"));

				// https://potato-corner-uat.myshopify.com/products/flavor-chili-barbeque-220g
				String productUrl = new StringBuilder(hostUrl).append("/products/")
						.append((String) prodMap.get("handle")).toString();

				prodMap.put("productUrl", productUrl);

				Map<String, Object> imgMap = (Map<String, Object>) prodMap.get("image");
				if (MapUtils.isNotEmpty(imgMap)) {
					prodMap.put("imgSrc", imgMap.get("src"));
				}
				// tags=PC00000002, POTATOES, WHC_JTCDAVCOM
				if (StringUtils.isNotBlank((String) prodMap.get("tags"))) {
					String[] tags = ((String) prodMap.get("tags")).split(Constants.COMMA);
					for (String tag : tags) {
						if (tag.contains(Constants.WARESHOUSE_CODE_TAG_CODE)) {
							prodMap.put("warehouseCode", tag.substring(tag.indexOf('_') + 1));
						}
						if (tag.contains(Constants.WARESHOUSE_SITE_TAG_CODE)) {
							prodMap.put("warehouseSite", tag.substring(tag.indexOf('_') + 1));
						}
						if (tag.contains(Constants.UNIT_OF_MEASURE_TAG_CODE)) {
							prodMap.put("uom", tag.substring(tag.indexOf('_') + 1));
						}
					}
				}

				logger.info("prodMap: " + prodMap);
			}

		} else {
			allOnlineProds.addAll(this.getProductList(new HashMap<>()));
		}

		logger.info(" ** OnlineProductServiceImpl >> getAllProductOnAllPages >> [END]");
		return allOnlineProds;
	}

	@Override
	public Map<String, Object> getOneProduct(Long productId) throws Throwable {
		logger.info("** OnlineProductServiceImpl >> getOneProduct >> [START]");

		Map<String, Object> reqParamMap = new HashMap<String, Object>();
		try {
			String prodRequestUrl = new StringBuilder("/admin/api/").append(apiVersion).append("/products/")
					.append(productId).append(".json").toString();

			Map<String, Object> resultMap = restTemplateService.sendGetRequest(prodRequestUrl, reqParamMap);
			if (MapUtils.isNotEmpty(resultMap) && resultMap.containsKey(Constants.PRODUCT)) {
				Map<String, Object> productMap = (Map<String, Object>) resultMap.get(Constants.PRODUCT);

				List<Map<String, Object>> variants = (List<Map<String, Object>>) productMap.get("variants");
				Map<String, Object> prodVariant = variants.get(0);
				productMap.put("tblId", productMap.get("id"));
				productMap.put("productId", productMap.get("id"));
				productMap.put("itemId", productMap.get("itemId"));
				productMap.put("variantPrice", prodVariant.get("price"));
				productMap.put("variantSku", prodVariant.get("sku"));
				productMap.put("variantInventoryQty", prodVariant.get("inventory_quantity"));

				// https://potato-corner-uat.myshopify.com/products/flavor-chili-barbeque-220g
				String productUrl = new StringBuilder(hostUrl).append("/products/")
						.append((String) productMap.get("handle")).toString();

				productMap.put("productUrl", productUrl);

				Map<String, Object> imgMap = (Map<String, Object>) productMap.get("image");
				if (MapUtils.isNotEmpty(imgMap)) {
					productMap.put("imgSrc", imgMap.get("src"));
				}
				// tags=PC00000002, POTATOES, WH_JTCDAVCOM
				if (StringUtils.isNotBlank((String) productMap.get("tags"))) {
					String[] tags = ((String) productMap.get("tags")).split(Constants.COMMA);
					for (String tag : tags) {
						if (tag.contains(Constants.WARESHOUSE_CODE_TAG_CODE)) {
							productMap.put("warehouseCode", tag.substring(tag.indexOf('_') + 1));
						}
						if (tag.contains(Constants.WARESHOUSE_SITE_TAG_CODE)) {
							productMap.put("warehouseSite", tag.substring(tag.indexOf('_') + 1));
						}
						if (tag.contains(Constants.UNIT_OF_MEASURE_TAG_CODE)) {
							productMap.put("uom", tag.substring(tag.indexOf('_') + 1));
						}
					}
				}

				logger.info("productMap: " + productMap);
				return productMap;
			}

		} catch (Throwable t) {
			logger.log(Level.SEVERE, t.getMessage(), t);
		}

		logger.info("** OnlineProductServiceImpl >> getOneProduct >> [END]");

		return null;
	}

	@Override
	public Map<String, Object> updateProduct(Map<String, Object> paramMap) throws Throwable {
		logger.info("** OnlineProductServiceImpl >> getProductList >> [START]");

		Map<String, Object> reqParamMap = new HashMap<String, Object>();
		reqParamMap.putAll(paramMap);

		String prodRequestUrl = new StringBuilder("/admin/api/").append(apiVersion).append("/products.json?limit=")
				.append(rowLimit).toString();

		Map<String, Object> resultMap = restTemplateService.sendPutRequest(prodRequestUrl, reqParamMap);
		if (MapUtils.isNotEmpty(resultMap)) {
			logger.info("** OnlineProductServiceImpl >> resultMap: " + resultMap.toString());
		}
		logger.info("** OnlineProductServiceImpl >> getProductList >> [END]");
		return null;
	}

	@Override
	public Map<String, Object> createProduct(Map<String, Object> paramMap) throws Throwable {
		logger.info("** OnlineProductServiceImpl >> getProductList >> [START]");

		Map<String, Object> reqParamMap = new HashMap<String, Object>();
		reqParamMap.putAll(paramMap);
		String productId = String.valueOf(reqParamMap.get("id"));
		String prodRequestUrl = new StringBuilder("/admin/api/").append(apiVersion).append("/products/")
				.append(productId).append(".json").toString();

		Map<String, Object> resultMap = restTemplateService.sendPutRequest(prodRequestUrl, reqParamMap);
		if (MapUtils.isNotEmpty(resultMap)) {
			logger.info("** OnlineProductServiceImpl >> resultMap: " + resultMap.toString());
		}
		logger.info("** OnlineProductServiceImpl >> getProductList >> [END]");
		return null;
	}
	
	
	
	
	@Override
	public Map<String, Object> createPriceRule(Map<String, Object> paramMap) throws Throwable {
		logger.info("** OnlineProductServiceImpl >> createPriceRule >> [START]");

		Map<String, Object> reqParamMap = new HashMap<String, Object>();
		reqParamMap.putAll(paramMap); 
		String requestUrl = new StringBuilder("/admin/api/").append(apiVersion) 
				.append("/price_rules.json").toString();
		
		logger.info("** OnlineProductServiceImpl >> createPriceRule >> requestUrl: " + requestUrl);
		logger.info("** OnlineProductServiceImpl >> createPriceRule >> reqParamMap: " + reqParamMap);
			
		Map<String, Object> resultMap = restTemplateService.sendPostRequest(requestUrl, reqParamMap);
		if (MapUtils.isNotEmpty(resultMap)) {
			logger.info("** OnlineProductServiceImpl >> resultMap: " + resultMap.toString());
			return resultMap;
		}
		logger.info("** OnlineProductServiceImpl >> createPriceRule >> [END]");
		return null;
	}

	@Override
	public Map<String, Object> createDiscountCode(Map<String, Object> paramMap) throws Throwable {
		logger.info("** OnlineProductServiceImpl >> createDiscountCode >> [START]");

		Map<String, Object> reqParamMap = new HashMap<String, Object>();
		String priceRuleId = (String)paramMap.get("priceRuleId");
		reqParamMap.putAll(paramMap); 
		String requestUrl = new StringBuilder("/admin/api/").append(apiVersion)
				.append("/price_rules/") .append(priceRuleId)
				.append("/discount_codes.json").toString();
		
		if (reqParamMap.containsKey("priceRuleId"))
		reqParamMap.remove("priceRuleId");
		
		logger.info("** OnlineProductServiceImpl >> createDiscountCode >> requestUrl: " + requestUrl);
		logger.info("** OnlineProductServiceImpl >> createDiscountCode >> reqParamMap: " + reqParamMap);
			
		Map<String, Object> resultMap = restTemplateService.sendPostRequest(requestUrl, reqParamMap);
		if (MapUtils.isNotEmpty(resultMap)) {
			logger.info("** OnlineProductServiceImpl >> resultMap: " + resultMap.toString());
			return resultMap;
		}
		logger.info("** OnlineProductServiceImpl >> createDiscountCode >> [END]");
		return null;
	}

	@Override
	public Map<String, Object> deletePriceRule(Long priceRuleId) throws Throwable {

		logger.info("** OnlineProductServiceImpl >> deletePriceRule >> [START]");

		Map<String, Object> reqParamMap = new HashMap<String, Object>();
		try {
			String prodRequestUrl = new StringBuilder("/admin/api/").append(apiVersion)
					.append("/price_rules/").append(priceRuleId).append(".json").toString();

			Map<String, Object> resultMap = restTemplateService.sendDeleteRequest(prodRequestUrl, reqParamMap);
			if (MapUtils.isNotEmpty(resultMap) ) {
			  	return resultMap;
			}

		} catch (Throwable t) {
			logger.log(Level.SEVERE, t.getMessage(), t);
		}

		logger.info("** OnlineProductServiceImpl >> deletePriceRule >> [END]");
 
		return null;
	}

	@Override
	public Map<String, Object> deleteDiscountCode(Long priceRuleId, Long discountId) throws Throwable {

		logger.info("** OnlineProductServiceImpl >> deleteDiscountCode >> [START]");

		Map<String, Object> reqParamMap = new HashMap<String, Object>();
		try {
			String prodRequestUrl = new StringBuilder("/admin/api/").append(apiVersion)
					.append("/price_rules/").append(priceRuleId).append("/discount_codes/")
					.append(discountId).append(".json").toString();

			Map<String, Object> resultMap = restTemplateService.sendDeleteRequest(prodRequestUrl, reqParamMap);
			if (MapUtils.isNotEmpty(resultMap) ) {
			  	return resultMap;
			}

		} catch (Throwable t) {
			logger.log(Level.SEVERE, t.getMessage(), t);
		}

		logger.info("** OnlineProductServiceImpl >> deleteDiscountCode >> [END]");
 
		return null;
	}

	@Override
	public Map<String, Object> deleteProduct(Map<String, Object> paramMap) throws Throwable {
		logger.info("** OnlineProductServiceImpl >> getProductList >> [START]");

		Map<String, Object> reqParamMap = new HashMap<String, Object>();
		reqParamMap.putAll(paramMap);
		// /admin/api/2022-04/products/632910392.json
		String productId = String.valueOf(NumberUtil.getLongValue(reqParamMap, "id"));
		String prodRequestUrl = new StringBuilder("/admin/api/").append(apiVersion).append("/products/")
				.append(productId).append(".json").toString();

		Map<String, Object> resultMap = restTemplateService.sendDeleteRequest(prodRequestUrl, reqParamMap);
		if (MapUtils.isNotEmpty(resultMap)) {
			logger.info("** OnlineProductServiceImpl >> resultMap: " + resultMap.toString());
			return resultMap;
		}
		logger.info("** OnlineProductServiceImpl >> getProductList >> [END]");
		return null;
	}

	@Override
	public Map<String, Object> getOneOnlineProductByItemNo(String itemNo) throws Throwable {
		// TODO Auto-generated method stub
		return null;
	}
	
	
	
	
	@Override
	public List<Map<String, Object>> getOnlineProductByItemNo(String itemNo) throws Throwable {
		
	
		
		return null;
	}

	@Override
	public Map<String, Object> createProductMetafield(Map<String, Object> paramMap) throws Throwable {

		logger.info("** OnlineProductServiceImpl >> createProductMetafield >> [START]");

		Map<String, Object> reqParamMap = new HashMap<String, Object>();
		reqParamMap.putAll(paramMap);
		// /admin/api/2022-04/products/632910392/metafields.json
		String productId = String.valueOf(reqParamMap.get("productId"));
		// PC00000149
		String itemId = String.valueOf(reqParamMap.get("itemId"));
		int boqValue = Integer.valueOf(String.valueOf(reqParamMap.get("boqValue")));
		int moqValue = Integer.valueOf(String.valueOf(reqParamMap.get("moqValue")));

		String requestUrl = new StringBuilder("/admin/api/").append(apiVersion).append("/products/").append(productId)
				.append("/metafields.json").toString();

		Map<String, Object> rootMoqMap = new LinkedHashMap<String, Object>();
		Map<String, Object> metaMoqMap = new LinkedHashMap<String, Object>();
		metaMoqMap.put("namespace", "moq_value");
		metaMoqMap.put("key", itemId);
		metaMoqMap.put("value", moqValue);
		metaMoqMap.put("value_type", "number_integer");

		Map<String, Object> rootBoqMap = new LinkedHashMap<String, Object>();
		Map<String, Object> metaBoqMap = new LinkedHashMap<String, Object>();
		metaBoqMap.put("namespace", "boq_value");
		metaBoqMap.put("key", itemId);
		metaBoqMap.put("value", boqValue);
		metaBoqMap.put("value_type", "number_integer");

		rootMoqMap.put(Constants.METAFIELD, metaMoqMap);
		rootBoqMap.put(Constants.METAFIELD, metaBoqMap);

		logger.info("** OnlineProductServiceImpl >> createProductMetafield >>rootMoqMap: " + rootMoqMap);
		logger.info("** OnlineProductServiceImpl >> createProductMetafield >>rootBoqMap: " + rootBoqMap);

		Map<String, Object> resultMoqMap = restTemplateService.sendPostRequest(requestUrl, rootMoqMap);
		Map<String, Object> resultBoqMap = restTemplateService.sendPostRequest(requestUrl, rootBoqMap);
		resultMoqMap.putAll(resultBoqMap);

//		Map<String, Object> resultMap = restTemplateService.sendPostRequest(requestUrl, rootMap);
//		if (MapUtils.isNotEmpty(resultMap)
//				&& resultMap.containsKey(Constants.METAFIELD)) {
//			logger.info("** OnlineProductServiceImpl >> createProductMetafield >> resultMap: " + resultMap);
//			return resultMap;
//		}

		logger.info("** OnlineProductServiceImpl >> createProductMetafield >> [END]");

		return resultMoqMap;
	}

	@Override
	public Map<String, Object> updateProductDBFromOnline(Map<String, Object> paramMap) throws Throwable {

		List<Map<String, Object>> allProducts = this.getAllOnlineProducts(null);
		for (Map<String, Object> prodMap : allProducts) {
			Long productId = NumberUtil.getLongValue(prodMap, "id");
			String prodTitle = (String) prodMap.get("title");

			String itemNo = ShopifyUtil.getD365ItemIdFromTitle(prodTitle);
			String warehouseCode = null;
			if (StringUtils.isNotBlank((String) prodMap.get("tags"))) {
				String[] tags = ((String) prodMap.get("tags")).split(Constants.COMMA);
				for (String tag : tags) {
					if (tag.contains(Constants.WARESHOUSE_CODE_TAG_CODE)) {
						warehouseCode = tag.substring(tag.indexOf('_') + 1);
					}

				}
			}

			logger.info("** OnlineProductServiceImpl >> updateProductDBFromOnline >>productId: " + productId);
			logger.info("** OnlineProductServiceImpl >> updateProductDBFromOnline >>itemNo: " + itemNo);
			logger.info("** OnlineProductServiceImpl >> updateProductDBFromOnline >>warehouseCode: " + warehouseCode);
			HashMap<String, Object> updateMap = new HashMap<>();
			updateMap.put("itemNumber", itemNo);
			updateMap.put("warehouse", warehouseCode);
			updateMap.put("shopProdId", productId);
			int updResult = productInventoryMapper.updateProductInventory(updateMap);

			logger.info("** OnlineProductServiceImpl >> updateProductDBFromOnline >>UPDATE: " + updResult);

		}

		return null;
	}

	@Override
	public Map<String, Object> cleansOnlineProductAndDB(Map<String, Object> paramMap) throws Throwable {
		logger.info("** OnlineProductServiceImpl >> cleansOnlineProductAndDB >> [START] ");

		logger.info("*************************************************  ");
		logger.info(" DELETE ONLINE Products with Old ID in DB ");
		logger.info("*************************************************  ");
		List<Map<String, Object>> delProdList = productInventoryMapper.getOnlineProductsForDeletion(null);
		for (Map<String, Object> prodMap : delProdList) {
			Long shopProdId = (Long) prodMap.get("shopProdId");
			Map<String, Object> delParam = new HashMap<>();
			delParam.put("id", shopProdId);
			try {
				logger.info("** OnlineProductServiceImpl >> cleansOnlineProductAndDB >> DELETING..shopProdId: "
						+ shopProdId);
				this.deleteProduct(delParam);
			} catch (Throwable t) {
				logger.log(Level.WARNING, t.getMessage(), t);
			}
		}

		logger.info("*************************************************  ");
		logger.info("DELETE ALL Old Product Data in DB ");
		logger.info("*************************************************  ");
		try {
			productInventoryMapper.deleteOldProductDetailData();
			productInventoryMapper.deleteOldProductInventoryData();
		} catch (Throwable t) {
			logger.log(Level.WARNING, t.getMessage(), t);
		}

		logger.info("*************************************************  ");
		logger.info("DELETE ALL Online Products that does NOT EXIST in DB ");
		logger.info("*************************************************  ");

		List<Map<String, Object>> eProducts = this.getAllOnlineProducts(new HashMap<>());
		for (Map<String, Object> eProdMap : eProducts) {

			try {
				Long eProdId = NumberUtil.getLongValue(eProdMap, "id");
				String itemTitle = (String) eProdMap.get("title");
				String itemNumber = ShopifyUtil.getD365ItemIdFromTitle(itemTitle);
				String whCode = (String) eProdMap.get("warehouseCode");
				HashMap<String, Object> searchMap = new HashMap<>();
				searchMap.put("itemNumber", itemNumber);
				searchMap.put("warehouse", whCode);
				List<Map<String, Object>> dbProds = productInventoryMapper.getProductInventoryList(searchMap);
				if (CollectionUtils.isEmpty(dbProds)) {
					// NOT FOUND - Delete eProduct
					Map<String, Object> delParam = new HashMap<>();
					delParam.put("id", eProdId);
					try {
						logger.info(
								"** OnlineProductServiceImpl >> cleansOnlineProductAndDB >> searchMap: " + searchMap);
						logger.info("** OnlineProductServiceImpl >> cleansOnlineProductAndDB >> DELETING..delParam: "
								+ delParam);
						this.deleteProduct(delParam);
					} catch (Throwable t) {
						logger.log(Level.WARNING, t.getMessage(), t);
					}
				}

			} catch (Throwable t) {
				logger.log(Level.WARNING, t.getMessage(), t);
			}

		}

		logger.info("** OnlineProductServiceImpl >> cleansOnlineProductAndDB >> [END] ");
		return null;
	}
	
	
	
	
	@Override
	public Map<String, Object> cleanseOOSProductByDB(Map<String, Object> paramMap) throws Throwable {
		
		
		List<Map<String, Object>> eProducts = this.getAllOnlineProducts(new HashMap<>());
		for (Map<String, Object> eProduct: eProducts) {
			
			
		}
 		
		return null;
	}

	@Override
	public Map<String, Object> backupProductDBTable(Map<String, Object> paramMap) throws Throwable {
		logger.info("** OnlineProductServiceImpl >> backupProductDBTable >> [START] ");
		SimpleDateFormat sdf = new SimpleDateFormat("MMddYYYY_HHmm");
		String dateTxt = sdf.format(new Date());
		HashMap<String, Object> inputMap = new HashMap<>();
		// PRODUCT DETAIL
		inputMap.put("newTable", "product_detail_".concat(dateTxt));
		inputMap.put("origTable", "product_detail");
		logger.info("** OnlineProductServiceImpl >> backupProductDBTable >> inputMap: " + inputMap.toString());
		tableUtilMapper.createTableCopy(inputMap);
		tableUtilMapper.insertTableCopy(inputMap);

		inputMap.put("newTable", "product_inventory_".concat(dateTxt));
		inputMap.put("origTable", "product_inventory");
		logger.info("** OnlineProductServiceImpl >> backupProductDBTable >> inputMap: " + inputMap.toString());
		tableUtilMapper.createTableCopy(inputMap);
		tableUtilMapper.insertTableCopy(inputMap);
		logger.info("** OnlineProductServiceImpl >> backupProductDBTable >> [END] ");
		return null;
	}

	@Override
	public Map<String, Object> syncProductInventory(Map<String, Object> paramMap) throws Throwable {

		logger.info("** OnlineProductServiceImpl >> syncProductInventory >> [START] ");
		HashMap<String, Object> searchMap = new HashMap<>();
		searchMap.put("oosStatus", "Y");
		if (Constants.TEST_ONLY) {
			searchMap.put("itemNumber", "'PC00000001'");
		}

		Long locationId = Long.valueOf(StringUtils.trimToEmpty(pcShopifyLocationId));

		List<Map<String, Object>> dbProducts = productDetailMapper.getProductsWithInventory(searchMap);
		for (Map<String, Object> prodMap : dbProducts) {

			try {
				
				long eProductId = (Long) prodMap.get("shopProdId");
				double doubValue = Double.parseDouble(String.valueOf(prodMap.get("physicalInventory")));
				int invQty = (int) doubValue;
				Map<String, Object> eProduct = this.getOneProduct(eProductId);
				if (MapUtils.isNotEmpty(eProduct)) {
					// PROCESS ONLINE Products
					List<Map<String, Object>> variants = (List<Map<String, Object>>) eProduct.get("variants");
					for (Map<String, Object> vartMap : variants) {
						Long inventItemId = NumberUtil.getLongValue(vartMap, "inventory_item_id");
						// this.updateVariantInventory(eProductId, variantId, invQty);
						this.updateOnlineInventory(locationId, inventItemId, invQty);
					}
 				}
			} catch (Throwable t) {
				logger.log(Level.SEVERE, t.getMessage(), t);
			}

		}

		logger.info("** OnlineProductServiceImpl >> syncProductInventory >> [END] ");

		return null;
	}

	@Override
	public Map<String, Object> updateOnlineInventory(Long locationId, Long inventItemId, int newQty) throws Throwable {

		String requestUrl = new StringBuilder("/admin/api/").append(apiVersion).append("/inventory_levels/set.json")
				.toString();
		// String requestUrl = new
		// StringBuilder("/admin/api/").append(apiVersion).append("/inventory_levels/adjust.json").toString();

		Map<String, Object> rootMap = new LinkedHashMap<String, Object>();
		Map<String, Object> requestMap = new LinkedHashMap<String, Object>();
		requestMap.put("location_id", locationId);
		requestMap.put("inventory_item_id", inventItemId);
		requestMap.put("available", newQty);

		// rootMap.put("inventory_level", requestMap);
		HelperUtil.viewInJSON(rootMap);
		Map<String, Object> resultMap = restTemplateService.sendPostRequest(requestUrl, requestMap);

		return resultMap;
	}

	@Override
	public Map<String, Object> updateVariantInventory(Long eProductId, Long variantId, int newQty) throws Throwable {

		String requestUrl = new StringBuilder("/admin/api/").append(apiVersion).append("/variants/").append(variantId)
				.append(".json").toString();

		Map<String, Object> rootMap = new LinkedHashMap<String, Object>();
		Map<String, Object> requestMap = new LinkedHashMap<String, Object>();
		requestMap.put("inventory_quantity", newQty);

		rootMap.put("variant", requestMap);
		HelperUtil.viewInJSON(rootMap);
		Map<String, Object> resultMap = restTemplateService.sendPutRequest(requestUrl, rootMap);

		return resultMap;
	}

	@Override
	public Map<String, Object> updateVariantData(Long eProductId, Long variantId, Map<String, Object> variantData)
			throws Throwable {

		String requestUrl = new StringBuilder("/admin/api/").append(apiVersion).append("/variants/").append(variantId)
				.append(".json").toString();
		Map<String, Object> rootMap = new LinkedHashMap<String, Object>();
		rootMap.put("variant", variantData);
		HelperUtil.viewInJSON(rootMap);
		Map<String, Object> resultMap = restTemplateService.sendPutRequest(requestUrl, rootMap);

		return resultMap;
	}
	
	 
	
	@Override
	public Map<String, Object> deleteProductImage(Long productId, Long imageId) throws Throwable {

		logger.info("** OnlineProductServiceImpl >> deleteProductImage >> [START]");

		Map<String, Object> reqParamMap = new HashMap<String, Object>();
		try {
			String prodRequestUrl = new StringBuilder("/admin/api/").append(apiVersion).append("/products/")
					.append(productId).append("/images/").append(imageId).append(".json").toString();

			Map<String, Object> resultMap = restTemplateService.sendDeleteRequest(prodRequestUrl, reqParamMap);
			if (MapUtils.isNotEmpty(resultMap) ) {
			  	return resultMap;
			}

		} catch (Throwable t) {
			logger.log(Level.SEVERE, t.getMessage(), t);
		}

		logger.info("** OnlineProductServiceImpl >> deleteProductImage >> [END]");
 
		return null;
	}

	@Override
	public List<Map<String, Object>> getAllImagesByProduct(Long productId) throws Throwable {

		logger.info("** OnlineProductServiceImpl >> getAllImagesByProduct >> [START]");

		Map<String, Object> reqParamMap = new HashMap<String, Object>();
		try {
			String prodRequestUrl = new StringBuilder("/admin/api/").append(apiVersion).append("/products/")
					.append(productId).append("/images.json").toString();

			Map<String, Object> resultMap = restTemplateService.sendGetRequest(prodRequestUrl, reqParamMap);
			if (MapUtils.isNotEmpty(resultMap) && resultMap.containsKey("images")) {
				List<Map<String, Object>> prodImages = (List<Map<String, Object>>) resultMap.get("images");
				return prodImages;
			}

		} catch (Throwable t) {
			logger.log(Level.SEVERE, t.getMessage(), t);
		}

		logger.info("** OnlineProductServiceImpl >> getAllImagesByProduct >> [END]");
 
		return null;
	}

	@Override
	public Map<String, Object> updateProductImage(Map<String, Object> paramMap) throws Throwable {
		logger.info("** OnlineProductServiceImpl >> updateProductImage >> [START] ");
		String localPathDir = (String)paramMap.get("local_path");
		String imgExtensions  = (String)paramMap.get("img_extensions");
		File dirPath = new File(localPathDir);
		Collection<File> imgFiles = PCFileUtil.listFiles(dirPath, new String[] {"PNG"}, false);
		
		for (File imgFile: imgFiles) {
			String itemNo = PCFileUtil.getItemNoFromImage(imgFile.getName());
			String fileExt = FilenameUtils.getExtension(imgFile.getName());
			logger.info(" updateProductImage >> itemNo: " + itemNo);
			String queryTxt = "select * from cms_db.product_inventory where item_number = '"+itemNo+"'";
			 
	 		List<Map<String, Object>> eProductDBList = queryBuilderService.getOnlineExecQuery("https://cms.potatocorner.com/springboot-cms-backend/pc/online-shop/executeDataQuery", 
					queryTxt);
	 		for (Map<String, Object> eProdDB: eProductDBList) { 
	 			logger.info(" updateProductImage >> eProdDB: " + eProdDB);
	 			long eProductId = NumberUtil.getLongValue(eProdDB, "shop_prod_id");
	 			Map<String, Object> dbProduct = new HashMap<>();
	 		 	dbProduct.put("itemNumber", itemNo);
	 		 	dbProduct.put("fileExt", StringUtils.lowerCase(fileExt));
	 			Map<String, Object> eProduct = new HashMap<>();
	 			eProduct.put("productId", eProductId);
	 			
	 			//Get All Product Images
	 			List<Map<String, Object>> prodImages = this.getAllImagesByProduct(eProductId);
	 			for (Map<String, Object> imgMap: prodImages) {
	 				long imageId = NumberUtil.getLongValue(imgMap, "id");
	 				//DELETE Current Image/s
	 				this.deleteProductImage(eProductId, imageId);
	 				//CREATE New Image
	 				this.createProductImage(eProduct, dbProduct);
	 			}
	 			
	 		}
		}
		logger.info("** OnlineProductServiceImpl >> updateProductImage >> [END] ");
		
		return null;
	}

	@Override
	public Map<String, Object> createProductImage(Map<String, Object> eProduct, Map<String, Object> dbProduct)
			throws Throwable {

		Long productId = ShopifyUtil.getProductId(eProduct);

		String imgRequestUrl = new StringBuilder("/admin/api/").append(apiVersion).append("/products/")
				.append(productId).append("/images.json").toString();

		Map<String, Object> requestImgMap = ShopifyUtil.buildProductImageByImgURL(imageUrlPath, eProduct, dbProduct);

		logger.info("*** OnlineProductServiceImpl >> requestImgMap: " + requestImgMap);
		if (MapUtils.isNotEmpty(requestImgMap)) {
			Map<String, Object> createdImgMap = restTemplateService.sendPostRequest(imgRequestUrl, requestImgMap);
			logger.info("*** OnlineProductServiceImpl >> createdImgMap: " + createdImgMap);
			return createdImgMap;
		}

		return null;
	}

	@Override
	public Map<String, Object> generateFreeVariantByPromo(Map<String, Object> eProduct) throws Throwable {
	 
		if (MapUtils.isNotEmpty(eProduct)) {
			Long productId = NumberUtil.getLongValue(eProduct, "id");
			String requestUrl = new StringBuilder("/admin/api/").append(apiVersion).append("/products/")
					.append(productId).append("/variants.json").toString();
			Map<String, Object> rootMap = new HashMap<>();
			Map<String, Object> detailMap = new HashMap<>();
			
			List<Map<String, Object>> variants = (List<Map<String, Object>>) eProduct.get("variants");
		 	int inventoryQty = 0;
			for (Map<String, Object> varntMap: variants) {
				double itemPrice = Double.parseDouble(String.valueOf(varntMap.get("price")));
				if (itemPrice > 0) {
					inventoryQty= (int)Double.parseDouble(String.valueOf(varntMap.get("inventory_quantity")));
					break;
				}
			}
			
			detailMap.put("option1", "FREE ITEM");
			detailMap.put("price", "0.0"); 	 
						
			rootMap.put("variant", detailMap);
			Map<String, Object> resultMap = restTemplateService.sendPostRequest(requestUrl, rootMap);
			if (MapUtils.isNotEmpty(resultMap)) {
				Long locationId = Long.valueOf(StringUtils.trimToEmpty(pcShopifyLocationId));
				logger.info(resultMap.toString());
				Map<String, Object> freeItemVar  = (Map<String, Object>)resultMap.get("variant");
				long inventItemId = NumberUtil.getLongValue(freeItemVar, "inventory_item_id");
				Map<String, Object> inventResultMap = this.updateOnlineInventory(locationId, inventItemId, inventoryQty);
				logger.info(inventResultMap.toString());
				return resultMap;
			}
			
			
		}
		return null;
	}

	@Override
	public Map<String, Object> deleteVariant(Long eProductId, Long variantId) throws Throwable {
		// TODO Auto-generated method stub
		return null;
	}
	
	
	
	
	
}
