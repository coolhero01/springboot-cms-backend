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
public interface ClientTokenMapper {
	@Select({
			"<script> SELECT * FROM cms_db.client_token ",
			"   WHERE update_date is not null ",
			"  <if test='tokenType != null'> and token_type = #{tokenType}  </if>",
		 			"</script>", "" })
	public List<Map<String, Object>> getTokenList(HashMap<String, Object> paramMap);

	   
	@Update({ "<script> UPDATE cms_db.client_token set update_date=now() "  
			+ ", access_token=#{accessToken} \r\n"
			+ ", old_token = access_token \r\n"
	        + " WHERE token_type = #{tokenType} " ,		
	"</script>" })
	public int updateClientToken(HashMap<String, Object> paramMap);
  
	  

}
