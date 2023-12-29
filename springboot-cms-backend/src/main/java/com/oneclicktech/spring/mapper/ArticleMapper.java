package com.oneclicktech.spring.mapper;

import java.util.HashMap;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.oneclicktech.spring.persistence.Article;

@Mapper
public interface ArticleMapper {
    @Select("SELECT * FROM ARTICLES WHERE id = #{id}")
    HashMap<String, Object> getArticle(@Param("id") Long id);
}