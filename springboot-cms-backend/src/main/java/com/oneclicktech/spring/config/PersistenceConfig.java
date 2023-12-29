package com.oneclicktech.spring.config;

import javax.sql.DataSource;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableAutoConfiguration(exclude = { DataSourceAutoConfiguration.class, MybatisAutoConfiguration.class })
public class PersistenceConfig {

	@Bean 
    @ConfigurationProperties("spring.datasource.mysql.pc")
    public DataSource dataSourceSpavi() {
        return  this.dataSourceProperties().initializeDataSourceBuilder()
          .build();
    }
    
	@Bean 
    @ConfigurationProperties("spring.datasource.mysql.pc")
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }
    @Bean
    public SqlSessionFactory sqlSessionFactory() throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSourceSpavi());
        return factoryBean.getObject();
    }

}
