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
public interface TableUtilMapper {
 

	@Delete({ "<script>", " CREATE TABLE cms_db.${newTable} like cms_db.${origTable} ", "</script>" })
	public int createTableCopy(HashMap<String, Object> paramMap);
	
	@Delete({ "<script>", " INSERT INTO cms_db.${newTable} SELECT * FROM cms_db.${origTable} ", "</script>" })
	public int insertTableCopy(HashMap<String, Object> paramMap);
	
	
	@Delete({ "<script>", " ${txtQuery}  ", "</script>" })
	public int deleteQuery(HashMap<String, Object> paramMap);
	 
	@Update({ "<script>", " ${txtQuery}  ", "</script>" })
	public int updateQuery(HashMap<String, Object> paramMap);
	
	
	@Insert({ "<script>", " ${txtQuery}  ", "</script>" })
	public int insertQuery(HashMap<String, Object> paramMap);
	
	@Select({ "<script>", " ${txtQuery}  ", "</script>" })
	public List<Map<String, Object>> selectQuery(HashMap<String, Object> paramMap);
}