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
public interface AuditLogMapper {
	@Select({
			"<script> SELECT shop_order_no as 'shopOrderNo',\r\n" + 
			" sales_order_no as 'salesOrderNo',\r\n" + 
			" so_json_request as 'soJsonRequest',\r\n" + 
			" audit_log_msg as 'auditLogMsg',\r\n" + 
			" update_date as 'updateDate' "
			+ " FROM cms_db.order_audit_log \r\n"
			+ " WHERE update_date is not null  ",
			"  <if test='shopOrderNo != null'> and shop_order_no = #{shopOrderNo}  </if>",
	 
			"</script>", "" })
	public List<Map<String, Object>> getOrderAuditLogs(HashMap<String, Object> paramMap);

  
	@Insert({ "<script>", "INSERT INTO cms_db.order_audit_log (shop_order_no,\r\n" + 
			"sales_order_no,\r\n" + 
			"so_json_request,\r\n" + 
			"audit_log_msg,\r\n" + 
			"update_date ) VALUES (#{shopOrderNo},\r\n" + 
			"#{salesOrderNo},\r\n" + 
			"#{soJsonRequest},\r\n" + 
			"#{auditLogMsg},\r\n" + 
			"now() )", "</script>" })
	public int insertOrderAuditLog(HashMap<String, Object> paramMap);

 

	@Update({ "<script> UPDATE cms_db.order_audit_log set update_date=now()  " +
		   " <if test='shopOrderNo != null'>, shop_order_no = #{shopOrderNo} </if> \r\n" + 
			"<if test='salesOrderNo != null'>, sales_order_no = #{salesOrderNo} </if> \r\n" + 
			"<if test='soJsonRequest != null'>, so_json_request = #{soJsonRequest} </if> \r\n" + 
			"<if test='auditLogMsg != null'>, audit_log_msg = #{auditLogMsg} </if> \r\n" +  
			" WHERE shop_order_no = #{shopOrderNo}   "
				+ " </script>" })
	public int updateOrderAuditLog(HashMap<String, Object> paramMap);

	@Delete({ "<script>", " DELETE FROM cms_db.CronAuditLog_DETAIL ", " WHERE prd_id is not null  ",
			"    <if test='itemId != null'> AND item_id=#{itemId}</if>", "</script>" })
	public int deleteOrderAuditLog(HashMap<String, Object> paramMap);
	
	
	
	

}