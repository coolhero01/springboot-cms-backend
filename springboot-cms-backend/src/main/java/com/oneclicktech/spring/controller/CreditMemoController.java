package com.oneclicktech.spring.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
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
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
import org.springframework.web.multipart.MultipartFile;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.oneclicktech.spring.mapper.CreditMemoMapper;
import com.oneclicktech.spring.message.ResponseMessage;
import com.oneclicktech.spring.service.AuditLogService;
import com.oneclicktech.spring.service.EmailService;
import com.oneclicktech.spring.service.OnlineProductService;
import com.oneclicktech.spring.service.OnlineShopService;
import com.oneclicktech.spring.service.RestGenericService;
import com.oneclicktech.spring.service.RestTemplateService;
import com.oneclicktech.spring.util.DateUtil;
import com.oneclicktech.spring.util.NumberUtil;
import com.oneclicktech.spring.util.PCDataUtil;
import com.oneclicktech.spring.util.StringUtils;

@CrossOrigin(origins = "http://localhost:4200", maxAge = 3600, allowCredentials = "true")
@RestController
@RequestMapping("/pc/creditmemo")
public class CreditMemoController {

	private static final Logger logger = Logger.getLogger("CreditMemoController");

	@Autowired
	RestTemplateService restTemplateService;

	@Autowired
	RestGenericService restGenericService;

	@Autowired
	AuditLogService auditLogService;

	@Autowired
	OnlineProductService onlineProductService;

	@Value("${cms.app.host-url}")
	String cmsAppHostUrl;

	@Autowired
	EmailService emailService;

	@Autowired
	CreditMemoMapper creditMemoMapper;
	
	@Autowired
	OnlineShopService onlineShopService;
	
	@PostConstruct
	public void init() {
		logger.info("** CreditMemoController >> [INIT]");
	}

	@GetMapping("/getCreditMemoList")
	public ResponseEntity<List<Map<String, Object>>> getCreditMemoList(@RequestParam("customerNo") String customerNo,
			@RequestParam("discountCode") String discountCode) {
		logger.info("** CreditMemoController >> getCreditMemoList >> [START]");
		HttpStatus httpStat = HttpStatus.OK;
		logger.info("** CreditMemoController >> customerNo: " + customerNo);
		logger.info("** CreditMemoController >> discountCode: " + discountCode);

		HashMap<String, Object> paramMap = new HashMap<String, Object>();
		if (StringUtils.isNotBlank(customerNo)) {
			paramMap.put("customerNo", customerNo);
		}
		if (StringUtils.isNotBlank(discountCode)) {
			paramMap.put("discountCode", discountCode);
		}

		List<Map<String, Object>> cmList = new ArrayList<Map<String, Object>>();

		cmList = creditMemoMapper.getCreditMemoList(paramMap);
		if (CollectionUtils.isNotEmpty(cmList)) {
			for (Map<String, Object> cmMap : cmList) {
				boolean usageFlag = (cmMap.get("usageFlag") != null && ((String) cmMap.get("usageFlag")).equals("Y"))
						? true
						: false;
				cmMap.put("usageFlag", usageFlag);
			}
		}

		logger.info("** CreditMemoController >> getCreditMemoList >> [END]");
		return new ResponseEntity<List<Map<String, Object>>>(cmList, httpStat);
	}

	@PostMapping("/uploadCreditMemoFile")
	public ResponseEntity<ResponseMessage> uploadCreditMemoFile(@RequestParam("myfile") MultipartFile multiFile) {
		String message = "";
		logger.info("** CreditMemoController >> uploadCreditMemoFile >> [START]");
		Workbook workbook = null;
		try {
			logger.info(
					"** CreditMemoController >> uploadCreditMemoFile >> multiFile: " + multiFile.getOriginalFilename());
			File tempFile = File.createTempFile("creditMemoTmp", ".xlsx");
			logger.info("** CreditMemoController >> uploadCreditMemoFile >> tempFile: " + tempFile.getAbsolutePath());

			try (OutputStream os = new FileOutputStream(tempFile)) {
				os.write(multiFile.getBytes());
			}

			FileInputStream file = new FileInputStream(tempFile);
			workbook = new XSSFWorkbook(file);

			Sheet sheet = workbook.getSheetAt(0);

			Map<Integer, List<Map<String, Object>>> data = new HashMap<>();

			int columnCnt = 3;
			List<Map<Integer, Object>> dataList = new ArrayList<Map<Integer, Object>>();
			int rowCtr = 0;

			for (Row row : sheet) {
				Map<Integer, Object> dataMap = new LinkedHashMap<Integer, Object>();
				if (rowCtr != 0) {
					for (int ii = 0; ii < columnCnt; ii++) {
						Cell myCell = row.getCell(ii);
						String cellValue = PCDataUtil.getStringCellValue(myCell);
						dataMap.put(ii, cellValue);
					}
					dataList.add(dataMap);
				}

				rowCtr++;
			}

			String dateNow = DateUtil.getDateNowInPattern("MM/dd/YYYY");
			if (CollectionUtils.isNotEmpty(dataList)) {
				for (Map<Integer, Object> dtaMap : dataList) {
					logger.info(dtaMap.toString());
					try {
						String customerNo = (String) dtaMap.get(0);
						String discountCode = (String) dtaMap.get(1);
						String discountAmt = (String) dtaMap.get(2);

						HashMap<String, Object> paramMap = new HashMap<String, Object>();
						paramMap.put("customerNo", customerNo);
						paramMap.put("discountCode", discountCode);
						paramMap.put("discountAmt", discountAmt);
						paramMap.put("usageFlag", "N");
						paramMap.put("startActiveDate", dateNow);
						creditMemoMapper.insertCreditMemo(paramMap);
					} catch (Exception e) {
						logger.log(Level.SEVERE, e.getMessage(), e);
					}

				}
			}

			// CLOSE Workbook resources
			workbook.close();

			// DELETE Temp file
			FileUtils.deleteQuietly(tempFile);

		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
			message = "Could not upload the file: " + multiFile.getOriginalFilename() + ". Error: " + e.getMessage();
			return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(new ResponseMessage(message));
		} finally {
			IOUtils.closeQuietly(workbook);
		}

		logger.info("** CreditMemoController >> uploadCreditMemoFile >> [END]");
		return ResponseEntity.status(HttpStatus.OK).body(new ResponseMessage(message));

	}

	@SuppressWarnings("unchecked")
	@PostMapping("/publishCreditMemo")
	public ResponseEntity<Boolean> publishCreditMemo(@RequestBody Map<String, Object> paramBody) {
		HttpStatus httpStat = HttpStatus.OK;
		boolean published = false;
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		logger.info("** CreditMemoController >> publishCreditMemo >> [START]");
		try {

			if (MapUtils.isNotEmpty(paramBody) && paramBody.containsKey("param1")) {
				List<Map<String, Object>> dataMapList = (List<Map<String, Object>>) paramBody.get("param1");
				if (CollectionUtils.isNotEmpty(dataMapList)) {

					Map<String, String> custNoMap = new HashMap<String, String>();
					Date publishDate = new Date();
					int discntCtr = 1;

					for (Map<String, Object> dtaMap : dataMapList) {

						try {
							
							if (dtaMap.get("publishDate")!=null)
								continue;
							 
							String customerNo = (String) dtaMap.get("customerNo");
							String discountCode = (String) dtaMap.get("discountCode");

							Double discountAmt = NumberUtil.getDoubleValue(dtaMap, "discountAmt");
							String discountAmtStr = "-".concat(String.valueOf(discountAmt));
							String startActiveDateStr = (String) dtaMap.get("startActiveDate");
							Map<String, Object> rootMap = new HashMap<>();
							Map<String, Object> detailMap = new HashMap<>();
							String ruleTitle = new StringBuilder(customerNo).append("-").append(discountCode)
									.toString();

							detailMap.put("title", ruleTitle);
							detailMap.put("value_type", "fixed_amount");
							detailMap.put("value", discountAmtStr);
							detailMap.put("customer_selection", "all");
							detailMap.put("target_type", "line_item");
							detailMap.put("target_selection", "all");
							detailMap.put("allocation_method", "across");
							detailMap.put("target_selection", "all");
							detailMap.put("once_per_customer", false);
							detailMap.put("usage_limit", 1);
							detailMap.put("starts_at", startActiveDateStr);

							rootMap.put("price_rule", detailMap);

							logger.info("** CreditMemoController >> publishCreditMemo >> price_rule: " + rootMap);

							Map<String, Object> ruleMap = onlineProductService.createPriceRule(rootMap);
							if (MapUtils.isNotEmpty(ruleMap) && ruleMap.containsKey("price_rule")) {
								// SUCCESS
								Map<String, Object> priceRuleMap = (Map<String, Object>) ruleMap.get("price_rule");
								long ruleId = NumberUtil.getLongValue(priceRuleMap, "id");
								String priceRuleId = String.valueOf(ruleId);

								rootMap = new HashMap<>();
								detailMap = new HashMap<>();
								detailMap.put("code", discountCode);

								rootMap.put("priceRuleId", priceRuleId);
								rootMap.put("discount_code", detailMap);

								custNoMap.put(customerNo, customerNo);

								logger.info(
										"** CreditMemoController >> publishCreditMemo >> discount_code: " + rootMap);
								Map<String, Object> discntMap = onlineProductService.createDiscountCode(rootMap);
								if (MapUtils.isNotEmpty(discntMap) && discntMap.containsKey("discount_code")) {
									// SUCCESS
									HashMap<String, Object> updateMap = new HashMap<String, Object>();
									updateMap.put("customerNo", customerNo);
									updateMap.put("discountCode", discountCode);
									updateMap.put("publishDate", publishDate);
									updateMap.put("priceRuleId", priceRuleId);
									int updateResult = creditMemoMapper.updateCreditMemo(updateMap);
									if (updateResult != 0) {
										logger.info("** CreditMemoController >> publishCreditMemo >> updateResult: "
												+ updateResult);
									}

									discntCtr++;
								}
							}
							published = true;
						} catch (Exception e) {
							logger.log(Level.SEVERE, e.getMessage(), e);
						}

					}
					
					if (MapUtils.isNotEmpty(custNoMap)) {
						for (Map.Entry<String, String> entry : custNoMap.entrySet()) {
							String customerNo = entry.getKey();
							HashMap<String, Object> searchMap = new HashMap<String, Object>();
							searchMap.put("customerNo", customerNo);
							searchMap.put("orderNameIsNull", "true");
						    List<Map<String, Object>> discntCodes =  creditMemoMapper.getDiscountCodes(searchMap);
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
					 
				}

			}

		} catch (Throwable e) {
			published = false;
			logger.log(Level.SEVERE, e.getMessage(), e);
		}

		logger.info("** CreditMemoController >> publishCreditMemo >> [END]");
		return new ResponseEntity<Boolean>(published, httpStat);

	}

	@PostMapping("/saveCreditMemo")
	public ResponseEntity<Boolean> saveCreditMemo(@RequestBody Map<String, Object> paramBody) {
		HttpStatus httpStat = HttpStatus.OK;
		boolean save = false;
		logger.info("** CreditMemoController >> saveCreditMemo >> [START]");
		try {
			if (MapUtils.isNotEmpty(paramBody)) {
				HashMap<String, Object> paramMap = new HashMap<String, Object>();
				paramMap.putAll(paramBody);
				int result = creditMemoMapper.insertCreditMemo(paramMap);
				if (result != 0) {
					save = true;
				}
			}

		} catch (Throwable e) {
			save = false;
			logger.log(Level.SEVERE, e.getMessage(), e);
		}

		logger.info("** CreditMemoController >> saveCreditMemo >> [END]");
		return new ResponseEntity<Boolean>(save, httpStat);

	}

	@PostMapping("/deleteCreditMemo")
	public ResponseEntity<Boolean> deleteCreditMemo(@RequestBody Map<String, Object> paramBody) {
		HttpStatus httpStat = HttpStatus.OK;
		boolean deleted = false;
		logger.info("** CreditMemoController >> deleteCreditMemo >> [START]");
		try {
			if (MapUtils.isNotEmpty(paramBody) && paramBody.containsKey("param1")) {
				List<Map<String, Object>> dataMapList = (List<Map<String, Object>>) paramBody.get("param1");
				for (Map<String, Object> dtaMap : dataMapList) {
					String priceRuleId = (String) dtaMap.get("priceRuleId");
					String customerNo = (String) dtaMap.get("customerNo");
					logger.info("** CreditMemoController >> deleteCreditMemo >> priceRuleId: " + priceRuleId);
					
					//Already has PRICE RULE data
					//DELETE Price Rule 
					if (StringUtils.isNotBlank(priceRuleId)) {
						onlineProductService.deletePriceRule(Long.valueOf(priceRuleId));
					}
					
					//Already PUBLISH
					//DELETE Meta data
					if (dtaMap.get("publishDate")!=null) {
						Map<String, Object> rootMap = new LinkedHashMap<String, Object>();
						Map<String, Object> detailMap = new LinkedHashMap<String, Object>();

						detailMap.put("namespace", "credit_memo_discount");
						detailMap.put("key", customerNo);
						detailMap.put("value", "DELETED");
						detailMap.put("type", "single_line_text_field"); 
						rootMap.put("metafield", detailMap);
						rootMap.put("metaType", "SHOP");

						onlineShopService.updateShopMetafield(rootMap);
					}
						  
					 
					//DELETE Physical Data
					HashMap<String, Object> paramMap = new HashMap<String, Object>();
					paramMap.putAll(dtaMap);
					int result = creditMemoMapper.deleteCreditMemo(paramMap);
					if (result != 0) {
						deleted = true;
					}

					logger.info("** CreditMemoController >> deleteCreditMemo >> deleted: " + deleted);
				}

			}

		} catch (Throwable e) {
			deleted = false;
			logger.log(Level.SEVERE, e.getMessage(), e);
		}

		logger.info("** CreditMemoController >> deleteCreditMemo >> [END]");
		return new ResponseEntity<Boolean>(deleted, httpStat);

	}

}
