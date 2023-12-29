package com.oneclicktech.spring.service.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.http.auth.InvalidCredentialsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oneclicktech.spring.domain.Constants;
import com.oneclicktech.spring.mapper.AuditLogMapper;
import com.oneclicktech.spring.mapper.ClientTokenMapper;
import com.oneclicktech.spring.mapper.CronAuditLogMapper;
import com.oneclicktech.spring.mapper.CronJobSchedMapper;
import com.oneclicktech.spring.mapper.CustomerAddressMapper;
import com.oneclicktech.spring.mapper.CustomerMapper;
import com.oneclicktech.spring.mapper.ProductMasterMapper;
import com.oneclicktech.spring.mapper.ShopOrderMapper;
import com.oneclicktech.spring.service.AuditLogService;
import com.oneclicktech.spring.service.CronJobService;
import com.oneclicktech.spring.service.CustomerService;
import com.oneclicktech.spring.service.OnlineOrderService;
import com.oneclicktech.spring.service.OnlineProductService;
import com.oneclicktech.spring.service.ProcessCurlService;
import com.oneclicktech.spring.service.ProductService;
import com.oneclicktech.spring.service.RestTemplateService;
import com.oneclicktech.spring.util.DateUtil;
import com.oneclicktech.spring.util.NumberUtil;
import com.oneclicktech.spring.util.ShopifyUtil;

@Service
public class ProcessCurlServiceImpl implements ProcessCurlService {

	private static final Logger logger = Logger.getLogger("ProcessCurlServiceImpl");

	@Override
	public String runCurlCommand(List<String> curlCommands, Map<String, Object> curlParams, 
			Map<String, Object> replaceParams) {
		logger.info("** ProcessCurlServiceImpl >> runCurlCommand >> [START]"); 
		logger.info("** ProcessCurlServiceImpl >> curlParams: " + curlParams);
		logger.info("** ProcessCurlServiceImpl >> replaceParams: " + replaceParams);
		String curlResult = null;
		InputStream inStream = null;
		BufferedReader buffRead = null;
		Process proc = null;
		
 		try {
			for (String curlTxt: curlCommands)	 {
	 	 		for (Map.Entry<String, Object> entry : replaceParams.entrySet()) { 
					String entryKey = entry.getKey();
					String entryValue = String.valueOf(entry.getKey());
					if (curlTxt.contains(entryKey)) {
					    curlTxt.replaceAll(entryKey, entryValue);
				  		break;
					}
		 		}
	 		}
			
			StringBuilder finalCmd = new StringBuilder();
			for (String curlTxt: curlCommands)	 {
				finalCmd.append(curlTxt).append(" ").append(System.lineSeparator());
			}
			
			logger.info("** ProcessCurlServiceImpl >> finalCmd: " + finalCmd);
			
//			"curl", "--silent", "--location", "--request", "POST", "<URL>",
//			"--header", "Content-Type:application/x-www-form-urlencoded", "--data-urlencode",
//			"inputParams=<Your Body>"
			
			ProcessBuilder pb = new ProcessBuilder(curlCommands);
			// errorstream of the process will be redirected to standard output
			pb.redirectErrorStream(true);
			// start the process
		    proc = pb.start();
			/*
			 * get the inputstream from the process which would get printed on the console /
			 * terminal
			 */
			inStream = proc.getInputStream();
			// creating a buffered reader
			buffRead = new BufferedReader(new InputStreamReader(inStream));
			StringBuilder sb = new StringBuilder();
			String responseTxt = buffRead.lines().collect(Collectors.joining("\n"));
			// close the buffered reader
			buffRead.close();
			inStream.close();
			/*
			 * wait until process completes, this should be always after the input_stream of
			 * processbuilder is read to avoid deadlock situations
			 */
			proc.waitFor();
			/*
			 * exit code can be obtained only after process completes, 0 indicates a
			 * successful completion
			 */
			int exitCode = proc.exitValue();
			// finally destroy the process
			curlResult = responseTxt;
			logger.info("** ProcessCurlServiceImpl >> exitCode: " + exitCode);
			logger.info("** ProcessCurlServiceImpl >> curlResult: " + curlResult);
			
			proc.destroy();
		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
		} finally {
			try {
				 if (buffRead!=null)
					 buffRead.close();
				 if (inStream!=null)
					 inStream.close();
				 if (proc!=null)
					 proc.destroy();
	 			 logger.info("** ProcessCurlServiceImpl >> CLOSE SUCCESS "); 
		 	} catch (Exception ee) {
				
			}
		}
		logger.info("** ProcessCurlServiceImpl >> runCurlCommand >> [END]");
		return null;
	}

}
