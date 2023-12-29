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
public interface CronJobSchedMapper {
	@Select({
			"<script> SELECT  tbl_id as 'tblId'\r\n" + 
			", job_name as 'jobName'\r\n" + 
			", job_type as 'jobType'\r\n" + 
			", job_desc as 'jobDesc'\r\n" + 
			", job_run_time_interval as 'jobRunTimeInterval'\r\n" + 
			", job_run_time_unit as 'jobRunTimeUnit'\r\n" + 
			", job_run_days as 'jobRunDays'\r\n" + 
			", job_run_datetime as 'jobRunDateTime'\r\n" + 
			", update_date as 'updateDate'\r\n" + 
			", update_by as 'updateBy'\r\n" + 
			" FROM cms_db.cron_job_sched \r\n" + "WHERE update_date is not null  ",
			"  <if test='jobType != null'> and job_type = #{jobType}  </if>",
			 
			"</script>", "" })
	public List<Map<String, Object>> getCronJobSchedList(HashMap<String, Object> paramMap);

	
	
	 

}