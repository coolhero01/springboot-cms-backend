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
public interface CronAuditLogMapper {
	@Select({
			"<script> SELECT tbl_id as 'tblId'\r\n" + "	, process_type as 'processType'\r\n"
					+ "	, process_status as 'processStatus'\r\n" + "	, notes \r\n"
					+ "	, last_audit_log as 'lastAuditLog'\r\n" + "	, update_date as 'updateDate'\r\n"
					+ "FROM cms_db.cron_audit_log \r\n" + "WHERE update_date is not null  ",
			"  <if test='processType != null'> and process_type = #{processType}  </if>",
			"  <if test='minInterval != null'>  and ( update_date between  (now() - interval ${minInterval} MINUTE)\r\n"
					+ "	and now()) </if>",

			"</script>", "" })
	public List<Map<String, Object>> getCronAuditLogs(HashMap<String, Object> paramMap);

	 
	@Insert({ "<script>", "INSERT INTO cms_db.customer (tbl_id,\r\n" + " customer_number,\r\n" + " first_name,\r\n"
			+ " middle_name,\r\n" + " last_name,\r\n" + " full_name,\r\n" + " cust_group,\r\n" + " type,\r\n"
			+ " email,\r\n" + " phone_number,\r\n" + " db_create_date,\r\n" + " db_update_date,\r\n" + " db_update_by "
			+ " ) VALUES ( " + " #{tblId},\r\n" + " #{customerNumber},\r\n" + " #{firstName},\r\n"
			+ " #{middleName},\r\n" + " #{lastName},\r\n" + " #{fullName},\r\n" + " #{custGroup},\r\n" + " #{type},\r\n"
			+ " #{email},\r\n" + " #{phoneNumber},\r\n" + " now(),\r\n" + " now(),\r\n" + " 'ADMIN' ) ", "</script>" })
	public int insertCronAuditLog(HashMap<String, Object> paramMap);

	@Insert({ "<script>", "INSERT INTO cms_db.customer (tbl_id,\r\n" + " customer_number,\r\n" + " first_name,\r\n"
			+ " middle_name,\r\n" + " last_name,\r\n" + " full_name,\r\n" + " cust_group,\r\n" + " type,\r\n"
			+ " email,\r\n" + " phone_number,\r\n" + " db_create_date,\r\n" + " db_update_date,\r\n" + " db_update_by "
			+ " ) VALUES ( " + " #{tblId},\r\n" + " #{CronAuditLogNumber},\r\n" + " #{FirstName},\r\n"
			+ " #{MiddleName},\r\n" + " #{LastName},\r\n" + " #{FullName},\r\n" + " #{CustGroup},\r\n" + " #{Type},\r\n"
			+ " #{Email},\r\n" + " #{PhoneNumber},\r\n" + " now(),\r\n" + " now(),\r\n" + " 'ADMIN' ) ", "</script>" })
	public int insertCronAuditLogForSync(HashMap<String, Object> paramMap);

	@Update({ "<script>", "</script>" })
	public int updateCronAuditLog(HashMap<String, Object> paramMap);

	@Update({ "<script> UPDATE cms_db.cron_audit_log set update_date=now() " + ", last_audit_log=#{lastAuditLog} \r\n"
			+ ", notes=#{notes} \r\n" + " WHERE process_type=#{processType} ", "</script>" })
	public int updateCronAuditLogByProcess(HashMap<String, Object> paramMap);

	@Delete({ "<script>", " DELETE FROM cms_db.CronAuditLog_DETAIL ", " WHERE prd_id is not null  ",
			"    <if test='itemId != null'> AND item_id=#{itemId}</if>", "</script>" })
	public int deleteCronAuditLog(HashMap<String, Object> paramMap);
	
	
	
	

}