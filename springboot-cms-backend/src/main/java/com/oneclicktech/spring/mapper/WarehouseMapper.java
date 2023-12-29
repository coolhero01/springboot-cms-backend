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
public interface WarehouseMapper {
	@Select({ "<script>SELECT tbl_id as 'tblId',\r\n" + 
			"warehouse as 'warehouse',\r\n" + 
			"name as 'name',\r\n" + 
			" concat(trim(warehouse), \"-\", trim(name)) as code_with_name ,\r\n" + 
			 "address as 'address',\r\n" + 
			"site as 'site',\r\n" + 
			"db_create_date as 'dbCreateDate',\r\n" + 
			"db_update_date as 'dbUpdateDate',\r\n" + 
			"db_update_by as 'dbUpdateBy' " +
		
			" FROM cms_db.warehouse ", " WHERE tbl_id is not null  ",
			"    <if test='name != null'> AND name=#{name}</if>", "</script>", 
			""})
	public List<Map<String, Object>> getWarehouseList(HashMap<String, Object> paramMap);

	@Insert({ "<script>",
 			"INSERT INTO cms_db.warehouse (tbl_id,\r\n" + 
 			"warehouse,\r\n" + 
 			"name,\r\n" + 
 			"address,\r\n" + 
 			"site, \r\n" + 
 			"db_create_date,\r\n" + 
 			"db_update_date,\r\n" + 
 			"db_update_by " + 
 		 	 " ) VALUES ( " + 
 			 "#{tblId},\r\n" + 
 			 "#{warehouse},\r\n" + 
 			 "#{name},\r\n" + 
 			 "#{address},\r\n" + 
 			 "#{site},\r\n" + 
 			 " now(),\r\n" + 
 			 " now(),\r\n" +  
 		 	 " #{dbUpdateBy} ) ",
			"</script>" })
	public int insertWarehouse(HashMap<String, Object> paramMap);

	
	@Insert({ "<script>",
			"INSERT INTO cms_db.warehouse ( warehouse,\r\n" + 
			"name,\r\n" + 
			"address,\r\n" + 
			"site, \r\n" + 
			" db_create_date,\r\n" + 
			" db_update_date,\r\n" + 
			" db_update_by " + 
		 	 " ) VALUES (  #{Warehouse},\r\n" + 
			 "#{Name},\r\n" + 
			 "#{Address},\r\n" + 
			 "#{Site},\r\n" + 
			 " now(),\r\n" + 
			 " now(),\r\n" +  
		 	 " 'ADMIN' ) ",
		"</script>" })
	public int insertWarehouseForSync(HashMap<String, Object> paramMap);

	
	@Update({ "<script>",
	 	"</script>" })
	public int updateWarehouse(HashMap<String, Object> paramMap);
   
	@Delete({ "<script>", " DELETE FROM cms_db.warehouse ", "</script>" })
	public int deleteWarehouse(HashMap<String, Object> paramMap);

}