package com.oneclicktech.spring.mapper;

import java.util.HashMap;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.oneclicktech.spring.persistence.Article;

@Mapper
public interface RolesMapper {
    @Select({"<script>",
    	" SELECT * FROM cms_db.roles ",
    	 "    <if test='id != null'>where id=#{id}</if>",
     	"</script>"})
    List<HashMap<String, Object>> getRoles(@Param("id") Long id);
}