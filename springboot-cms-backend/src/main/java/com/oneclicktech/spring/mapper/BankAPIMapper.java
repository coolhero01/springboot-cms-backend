
package com.oneclicktech.spring.mapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.oneclicktech.spring.persistence.Article;

@Mapper
public interface BankAPIMapper {
	
	@Select({
			"<script> SELECT tbl_id as 'tblId',\r\n" + 
			" bank_name as 'bankName',\r\n" + 
			" env_type as 'envType',\r\n" + 
			" api_request_url as 'apiRequestUrl',\r\n" + 
			" api_column as 'apiColumn',\r\n" + 
			" api_value as 'apiValue',\r\n" + 
			" api_data_type as 'apiDataType',\r\n" + 
			" api_usage as 'apiUsage',\r\n" + 
			" api_type as 'apiType',\r\n" + 
			" column_seq as 'columnSeq',\r\n" + 
			" api_description as 'apiDescription',\r\n" + 
			" update_date as 'updateDate'"
			+ " FROM cms_db.bank_api_config \r\n"
			+ " WHERE tbl_id is not null  ",
			"  <if test='bankName != null'> and bank_name = #{bankName}  </if>",
			"  <if test='envType != null'> and env_type = #{envType}  </if>",
			"  <if test='apiType != null'> and api_type = #{apiType}  </if>",
			"  <if test='apiColumn != null'> and api_column = #{apiColumn}  </if>",
		    "  <if test='apiUsage != null'> and api_usage = #{apiUsage}  </if>"
				+ "  order by column_seq asc " ,
					 
			"</script>", "" })
	public List<Map<String, Object>> getBankAPIConfigs(HashMap<String, Object> paramMap);
	
	
	@Select({
		"<script>    SELECT order_name as 'orderName' ,\r\n" + 
			"    api_type as 'apiType',\r\n" + 
			"    order_id as 'orderId',\r\n" + 
		    "    internal_transact_ref_no as 'internalTransactRefNo',\r\n" + 
			"    channel_ref_no as 'channelRefNo',\r\n" + 
			"    status as 'status',\r\n" + 
			"    request_url as 'requestUrl',\r\n" + 
			"    response_url as 'responseUrl',\r\n" + 
			"    update_date as 'responseUrl' \r\n" + 
			" FROM cms_db.bdo_transact_log\r\n" + 
			" WHERE order_name is not null " ,
			"<if test='orderName != null'> and order_name = #{orderName} </if> \r\n" + 		 
			"<if test='orderId != null'> and order_id = #{orderId} </if> \r\n" + 		 
		    "<if test='apiType != null'> and api_type = #{apiType} </if> \r\n "
		    + " LIMIT 1 " + 		 
	 	"</script>", "" })
	public  Map<String, Object>  getOneBDOTransactLog(HashMap<String, Object> paramMap);
	
	@Select({
		"<script> SELECT order_name as 'orderName' ,\r\n" + 
				"    api_type as 'apiType',\r\n" + 
				"    order_id as 'orderId',\r\n" + 
			    "    internal_transact_ref_no as 'internalTransactRefNo',\r\n" + 
				"    channel_ref_no as 'channelRefNo',\r\n" + 
				"    status as 'status',\r\n" + 
				"    request_url as 'requestUrl',\r\n" + 
				"    response_url as 'responseUrl',\r\n" + 
				"    update_date as 'updateDate' \r\n" + 
			" FROM cms_db.bdo_transact_log\r\n" + 
			" WHERE order_name is not null " ,
			"<if test='orderName != null'> and order_name = #{orderName} </if> \r\n" + 		 
			"<if test='orderId != null'> and order_id = #{orderId} </if> \r\n" + 		 
		    "<if test='apiType != null'> and api_type = #{apiType} </if> \r\n " +   
		    "<if test='status != null'> and status = #{status} </if> \r\n " +   
		    "<if test='nonPaidStatus != null'> and (status = 'pending' or status = 'partially_paid') </if> \r\n " +   
			"<if test='updateDate != null'> and update_date BETWEEN "
		    + "  DATE_SUB(now(), INTERVAL 1 DAY) and DATE_ADD(now(), INTERVAL 1 DAY)  </if> \r\n " +   
	 	"</script>", "" })
	public  List<Map<String, Object>> getBDOTransactLogs(HashMap<String, Object> paramMap);
		
	@Insert({ "<script>",
		" INSERT INTO cms_db.bdo_transact_log\r\n" + 
		"(order_name,\r\n" + 
		"api_type,\r\n" + 
		"order_id,\r\n" + 
	    "internal_transact_ref_no,\r\n" + 
		"channel_ref_no,\r\n" + 
		"status,\r\n" + 
		"request_url,\r\n" + 
		"response_url,\r\n" + 
		"update_date)\r\n" + 
		"VALUES\r\n" + 
		"(#{orderName},\r\n" + 
		"#{apiType},\r\n" + 
		"#{orderId},\r\n" + 
	    "#{internalTransactRefNo},\r\n" + 
		"#{channelRefNo},\r\n" + 
		"#{status},\r\n" + 
		"#{requestUrl},\r\n" + 
		"#{responseUrl},\r\n" + 
		"now()) ",
	"</script>" })
    public int insertBDOTransactLogs(HashMap<String, Object> paramMap);
	
	
	@Update({ "<script> UPDATE cms_db.bdo_transact_log\r\n" + 
			"SET update_date = now() \r\n" + 
			"<if test='orderId != null'>, order_id = #{orderId}  </if> \r\n" + 
			"<if test='internalTransactRefNo != null'>, internal_transact_ref_no = #{internalTransactRefNo}  </if> \r\n" + 
		    "<if test='channelRefNo != null'>, channel_ref_no = #{channelRefNo} </if> \r\n" + 
			"<if test='status != null'>, status = #{status} </if> \r\n" + 
			"<if test='requestUrl != null'>, request_url = #{requestUrl} </if> \r\n" + 
			"<if test='responseUrl != null'>, response_url = #{responseUrl} </if> \r\n" + 
			"WHERE order_name = #{orderName} AND api_type =  #{apiType} "
	  			 		
	 	  , "</script>" })
    public int updateBDOTransactLogs(HashMap<String, Object> paramMap);
}