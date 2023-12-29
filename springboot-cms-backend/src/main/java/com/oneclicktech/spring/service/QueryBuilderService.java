package com.oneclicktech.spring.service;

import java.util.List;
import java.util.Map;

public interface QueryBuilderService {

	public boolean execQuery(String queryTxt);

	public List<Map<String, Object>> getExecQuery(String queryTxt);

	public boolean onlineExecQuery(String apiUrl, String queryTxt);

	public List<Map<String, Object>> getOnlineExecQuery(String apiUrl, String queryTxt);

}
