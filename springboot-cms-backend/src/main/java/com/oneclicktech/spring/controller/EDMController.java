package com.oneclicktech.spring.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections4.CollectionUtils;
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

import com.oneclicktech.spring.mapper.EDMMapper;
import com.oneclicktech.spring.mapper.ShopOrderMapper;
import com.oneclicktech.spring.message.ResponseMessage;
import com.oneclicktech.spring.service.AuditLogService;
import com.oneclicktech.spring.service.CronJobService;
import com.oneclicktech.spring.service.EmailService;
import com.oneclicktech.spring.service.OnlineOrderService;
import com.oneclicktech.spring.service.RestGenericService;
import com.oneclicktech.spring.service.RestTemplateService;
import com.oneclicktech.spring.util.StringUtils;

@CrossOrigin(origins = "http://localhost:4200", maxAge = 3600, allowCredentials="true")
@RestController
@RequestMapping("/pc/edm")
public class EDMController {

	private static final Logger logger = Logger.getLogger("EDMController");

	@Autowired
	RestTemplateService restTemplateService;

	@Autowired
	RestGenericService restGenericService;

	@Autowired
	AuditLogService auditLogService;

	@Value("${cms.app.host-url}")
	String cmsAppHostUrl;

	@Autowired
	OnlineOrderService onlineOrderService;

	@Autowired
	EmailService emailService;

	@Autowired
	CronJobService cronJobService;

	@Autowired
	ShopOrderMapper shopOrderMapper;

	@Autowired
	EDMMapper edmMapper;

	@PostConstruct
	public void init() {
		logger.info("** EDMController >> [INIT]");

	}

	@GetMapping("/getEDMConfigList")
	public ResponseEntity<List<Map<String, Object>>> getEDMConfigList(@RequestParam("edmId") String edmId) {
		logger.info("** EDMController >> getSiteRegistrationForm >> [START]");
		HttpStatus httpStat = HttpStatus.OK;
		logger.info("** EDMController >> edmId: " + edmId);

		HashMap<String, Object> paramMap = new HashMap<String, Object>();
		if (StringUtils.isNotBlank(edmId)) {
			paramMap.put("edmId", edmId);
		}
		
		List<Map<String, Object>> configList = new ArrayList<Map<String, Object>>();

		configList = edmMapper.getEDMConfigList(paramMap);
		if (CollectionUtils.isNotEmpty(configList)) {
			for (Map<String, Object> edmMap: configList) {
				boolean promoteFlag =  (edmMap.get("promoteFlag")!=null 
						&& ((String)edmMap.get("promoteFlag")).equals("Y")) ? true:false;
				
				edmMap.put("promoteFlag", promoteFlag);
				
			}	
		}

		logger.info("** EDMController >> getSiteRegistrationForm >> [END]");
		return new ResponseEntity<List<Map<String, Object>>>(configList, httpStat);
	}

	@PostMapping("/uploadEDMFile")
	public ResponseEntity<ResponseMessage> uploadEDMFile(@RequestParam("file") MultipartFile file) {
		String message = "";
		logger.info("** EDMController >> uploadFile >> [START]");
		try {

			logger.info("** EDMController >> uploadFile: " + file.getOriginalFilename());
		} catch (Exception e) {
			message = "Could not upload the file: " + file.getOriginalFilename() + ". Error: " + e.getMessage();
			return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(new ResponseMessage(message));
		}

		logger.info("** EDMController >> uploadFile >> [END]");
		return ResponseEntity.status(HttpStatus.OK).body(new ResponseMessage(message));

	}

	@PostMapping("/saveEDMConfig")
	public ResponseEntity<Boolean> saveEDMConfig(@RequestBody Map<String, Object> paramBody) {
		HttpStatus httpStat = HttpStatus.OK;
		boolean save = false;
		logger.info("** EDMController >> saveEDMConfig >> [START]");
		try {

		} catch (Throwable e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
		}

		logger.info("** EDMController >> saveEDMConfig >> [END]");
		return new ResponseEntity<Boolean>(save, httpStat);

	}

	@GetMapping("/getSiteRegistrationForm")
	public ResponseEntity<Map<String, Object>> getSiteRegistrationForm(@RequestBody Map<String, Object> paramBody,
			HttpServletRequest request) {
		logger.info("** EDMController >> getSiteRegistrationForm >> [START]");
		HttpStatus httpStat = HttpStatus.OK;
		logger.info("** EDMController >> paramBody: " + paramBody);
		Map<String, Object> resultMap = new LinkedHashMap<String, Object>();

		logger.info("** EDMController >> getSiteRegistrationForm >> [END]");
		return new ResponseEntity<Map<String, Object>>(resultMap, httpStat);
	}

}
