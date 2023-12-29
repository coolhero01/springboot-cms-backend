package com.oneclicktech.spring.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface ProcessCurlService {
	  
	public String runCurlCommand(List<String> curlCommands, Map<String, Object> curlParams, 
			Map<String, Object> replaceParams);
	
}
