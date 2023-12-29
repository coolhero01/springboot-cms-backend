package com.oneclicktech.spring.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.oneclicktech.spring.mapper.ProductDetailMapper;
import com.oneclicktech.spring.mapper.ProductInventoryMapper;
import com.oneclicktech.spring.mapper.ProductMasterMapper;
import com.oneclicktech.spring.mapper.TableUtilMapper;
import com.oneclicktech.spring.service.QueryBuilderService;
import com.oneclicktech.spring.service.RestGenericService;

@Service
public class QueryBuilderServiceImpl implements QueryBuilderService {

	private static final Logger logger = Logger.getLogger("QueryBuilderServiceImpl");

	@Autowired
	ProductMasterMapper productMasterMapper;

	@Autowired
	ProductDetailMapper productDetailMapper;

	@Autowired
	ProductInventoryMapper productInventoryMapper;

	@Autowired
	TableUtilMapper tableUtilMapper;

	@Autowired
	RestGenericService restGenericService;

	@Override
	public boolean execQuery(String queryTxt) {
		HashMap<String, Object> paramMap = new HashMap<>();
		
		queryTxt = StringUtils.trimToEmpty(queryTxt);
		String lowQueryTxt = StringUtils.lowerCase(queryTxt);
		
		int result = 0;
		if (lowQueryTxt.contains(";")) {
			String[] txtQueries = queryTxt.split(";");
			for (String txtQuery : txtQueries) {
				String lowTxtQuery = StringUtils.lowerCase(txtQuery);
				if (StringUtils.isNotBlank(txtQuery)) {
					paramMap.put("txtQuery", txtQuery);
					if (lowTxtQuery.contains("delete")) {
						result = tableUtilMapper.deleteQuery(paramMap);
					} else if (lowTxtQuery.contains("update")) {
						result = tableUtilMapper.updateQuery(paramMap);
					} else if (lowTxtQuery.contains("insert")) {
						result = tableUtilMapper.insertQuery(paramMap);
					} else {
						result = tableUtilMapper.updateQuery(paramMap);
					}
				}

			}

		} else {
			paramMap.put("txtQuery", queryTxt);
			if (lowQueryTxt.contains("delete")) {
				result = tableUtilMapper.deleteQuery(paramMap);
			} else if (lowQueryTxt.contains("update")) {
				result = tableUtilMapper.updateQuery(paramMap);

			} else if (lowQueryTxt.contains("insert")) {
				result = tableUtilMapper.insertQuery(paramMap);
 			} else {
				result = tableUtilMapper.updateQuery(paramMap);
			}
		}

		if (result != 0)
			return true;

		return false;
	}

	@Override
	public List<Map<String, Object>> getExecQuery(String queryTxt) {
		HashMap<String, Object> paramMap = new HashMap<>();
		paramMap.put("txtQuery", StringUtils.trimToEmpty(queryTxt));
		return tableUtilMapper.selectQuery(paramMap);
	}

	@Override
	public boolean onlineExecQuery(String apiUrl, String queryTxt) {
		try {
			Map<String, Object> paramMap = new HashMap<>();
			paramMap.put("queryTxt", StringUtils.trimToEmpty(queryTxt));
			Map<String, Object> resultMap = restGenericService.sendPostRequest(apiUrl, paramMap);
			if (MapUtils.isNotEmpty(resultMap)) {
				return true;
			}
		} catch (Throwable t) {
			logger.log(Level.SEVERE, t.getMessage(), t);
		}
		return false;
	}

	@Override
	public List<Map<String, Object>> getOnlineExecQuery(String apiUrl, String queryTxt) {
		try {
			Map<String, Object> paramMap = new HashMap<>();
			paramMap.put("queryTxt", StringUtils.trimToEmpty(queryTxt));
			Map<String, Object> resultMap = restGenericService.sendPostRequest(apiUrl, paramMap);
			if (MapUtils.isNotEmpty(resultMap) && resultMap.containsKey("result")) {
				List<Map<String, Object>> dataList = (List<Map<String, Object>>) resultMap.get("result");
				return dataList;
			}
		} catch (Throwable t) {
			logger.log(Level.SEVERE, t.getMessage(), t);
		}
		return null;
	}

}
