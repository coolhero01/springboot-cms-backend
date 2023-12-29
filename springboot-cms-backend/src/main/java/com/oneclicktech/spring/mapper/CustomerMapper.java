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
public interface CustomerMapper {
	@Select({ "<script>SELECT tbl_id as 'tblId',\r\n" + 
			"customer_number as 'customerNumber',\r\n" + 
			"shop_id as 'shopId',\r\n" + 
			"group_customer_no as 'groupCustomerNo',\r\n" + 
			"first_name as 'firstName',\r\n" + 
			"middle_name as 'middleName',\r\n" + 
			"last_name as 'lastName',\r\n" + 
			"full_name as 'fullName',\r\n" + 
			"cust_group as 'custGroup',\r\n" + 
			"type as 'type',\r\n" + 
			"email as 'email',\r\n" + 
			"phone_number as 'phoneNumber',\r\n" + 
			" oos_warehouse_1 as 'oosWarehouse1',\r\n" + 
			" oos_warehouse_2 as 'oosWarehouse2',\r\n" + 
			" oos_include as 'oosInclude',\r\n" + 
					
			"created_date_time as 'createdDateTime',\r\n" + 
			"modified_date_time as 'modifiedDateTime',\r\n" + 
			"db_create_date as 'dbCreateDate',\r\n" + 
			"db_update_date as 'dbUpdateDate',\r\n" + 
			"db_update_by as 'dbUpdateBy',\r\n" + 
			"publish_date as 'publishDate' " +
		
			" FROM cms_db.customer ", " WHERE tbl_id is not null  ",
			"    <if test='name != null'> AND name=#{name}</if>", "</script>", 
			""})
	public List<Map<String, Object>> getCustomerList(HashMap<String, Object> paramMap);
	
	@Select({ "<script>SELECT tbl_id as 'tblId',\r\n" + 
			"customer_number as 'customerNumber',\r\n" + 
			"shop_id as 'shopId',\r\n" + 
			"group_customer_no as 'groupCustomerNo',\r\n" + 
			"first_name as 'firstName',\r\n" + 
			"middle_name as 'middleName',\r\n" + 
			"last_name as 'lastName',\r\n" + 
			"full_name as 'fullName',\r\n" + 
			"cust_group as 'custGroup',\r\n" + 
			"type as 'type',\r\n" + 
			"email as 'email',\r\n" + 
			"phone_number as 'phoneNumber',\r\n" + 
			"oos_warehouse_1 as 'oosWarehouse1',\r\n" + 
			"oos_warehouse_2 as 'oosWarehouse2',\r\n" + 
			"created_date_time as 'createdDateTime',\r\n" + 
			"modified_date_time as 'modifiedDateTime',\r\n" + 
			"db_create_date as 'dbCreateDate',\r\n" + 
			"db_update_date as 'dbUpdateDate',\r\n" + 
			"db_update_by as 'dbUpdateBy',\r\n" + 
			"publish_date as 'publishDate' " +
		
			" FROM cms_db.customer ", " WHERE tbl_id is not null  ",
			"    <if test='customerNumber != null'> AND customer_number=#{customerNumber}</if>", "</script>", 
			""})
	public Map<String, Object> getCustomerByCustNo(HashMap<String, Object> paramMap);
	
	
	@Select({ "<script> select c.shop_id as 'eCustId',\r\n" + 
			"  ca.shop_addr_id as 'eCustAddrId' \r\n" + 
			"  from cms_db.customer_address ca \r\n" + 
			" join  cms_db.customer c on c.customer_number = ca.customer_number \r\n" + 
			" where ca.db_update_date between DATE_SUB(now(), INTERVAL 1 WEEK) \r\n" + 
			"		and DATE_SUB(now(), INTERVAL 12 HOUR) \r\n" + 
			"  and (ca.shop_addr_id is not null and trim(ca.shop_addr_id) != '')    </script>", "" })
	public List<Map<String, Object>> getOnlineCustomerAddrForDeletion(HashMap<String, Object> paramMap);
	
	
	
	@Select({ "<script> SELECT distinct c.email, ca.customer_number as 'customerNumber'   \r\n" + 
			" FROM cms_db.customer c\r\n" + 
			" JOIN cms_db.customer_address ca on ca.customer_number = c.customer_number\r\n" + 
			" WHERE c.email = #{email} \r\n" + 
			" limit 1 "
			+ "</script>", 
			""})
	public Map<String, Object> getCustomerByEmail(HashMap<String, Object> paramMap);
	
	@Select({ "<script> SELECT DISTINCT c.* \r\n" + 
			"	, cu.description\r\n" + 
			"	, cu.store_code\r\n" + 
			"	, cu.store_name\r\n" + 
			"	, cu.address\r\n" + 
			"	, cu.town_city\r\n" + 
			"	, cu.street\r\n" + 
			"	, cu.province\r\n" + 
			"	, cu.zip_code\r\n" + 
			"	, cu.country\r\n" + 	
			"    \r\n" + 
			" FROM cms_db.customer c\r\n" + 
			" LEFT JOIN cms_db.customer_address cu on cu.customer_number = c.customer_number\r\n" + 
			" WHERE c.first_name is not null \r\n" + 
			" 	 and trim(c.first_name) != ''\r\n" + 
			"   ",
			"    </script>", 
			""})
	public List<Map<String, Object>> getCustomerWithFirstName(HashMap<String, Object> paramMap);
	
	
	@Select({ "<script> SELECT DISTINCT c.customer_number as 'customerNumber' \r\n" + 
			"	, c.shop_id as 'shopId'\r\n" + 
			"	, c.first_name as 'firstName'\r\n" + 
		    "	, c.middle_name as 'middleName'\r\n" + 
			"	, c.last_name as 'lastName'\r\n" + 
			"	, c.full_name as 'fullName'\r\n" + 
			"	, c.cust_group as 'custGroup'\r\n" + 
			"   , c.group_customer_no as 'groupCustomerNo' \r\n" + 
			"	, c.type as 'type'\r\n" + 
			"	, c.email as 'email'\r\n" + 
			"	, c.phone_number as 'phoneNumber'   \r\n" + 
			"	, c.oos_warehouse_1 as 'oosWarehouse1'   \r\n" + 
			"	, c.oos_warehouse_2 as 'oosWarehouse2'   \r\n" + 
			"	, c.oos_include as 'oosInclude'   \r\n" + 
		 	"	, c.created_date_time as 'createdDateTime'   \r\n" + 
			"	, c.modified_date_time as 'modifiedDateTime'   \r\n" + 
			"    , ca.shop_addr_id as 'shopAddrId'\r\n" + 
			"    , ca.store_code as 'storeCode'\r\n" + 
	    	"    , ca.store_name as 'storeName'\r\n" + 
			"    , ca.address as 'address'\r\n" + 
		    "    , ca.town_city as 'townCity'\r\n" + 
			"    , ca.street as 'street'\r\n" + 
			"    , ca.province as 'province'\r\n" + 
			"    , ca.zip_code as 'zipCode'\r\n" + 
			"    , ca.country as 'country'\r\n" + 
			" FROM cms_db.customer c \r\n" +   
			" JOIN cms_db.customer_address ca on ca.customer_number = c.customer_number\r\n" + 
			" WHERE c.customer_number is not null \r\n" + 
			"    <if test='customerNumber != null'> AND c.customer_number=#{customerNumber} </if> " +
			"    <if test='email != null'> AND c.email=#{email} </if> " +
			"    <if test='oosInclude != null'> AND c.oos_include=#{oosInclude} </if> " +
			"    <if test='emptyShopId != null'> AND (c.shop_id is null or trim(c.shop_id) = '') </if> " +
			"    <if test='withShopId != null'>  AND (c.shop_id is not null and trim(c.shop_id) != '') </if> " +
			"    <if test='withPhone != null'>   AND (c.phone_number is not null and trim(c.phone_number) != '') </if> " +
			"    <if test='withEmail != null'>   AND (c.email is not null and trim(c.email) != '') </if> " +
		    " ORDER BY c.email, c.customer_number \r\n" + 
			"   ",   
			"    </script>", 
			""})
	public List<Map<String, Object>> getCustomerWithAddressList(HashMap<String, Object> paramMap);

	
	@Select({ "<script> SELECT distinct c.shop_id as 'eCustomerNo' \r\n" + 
			" , c.email \r\n" + 
			" , cap.order_tag as 'orderTag'\r\n" + 
			" FROM cms_db.customer c\r\n" + 
			" JOIN cms_db.customer_auto_pay cap on cap.email=c.email\r\n" + 
			" where (c.shop_id is not null and trim(c.shop_id)!='')\r\n" + 
			"	and c.oos_include = 'Y' " + 
			" <if test='email != null'> and c.email = #{email} </if> " +
	 		"    </script>", 
			""})
	public List<Map<String, Object>> getAutoPayCustomers(HashMap<String, Object> paramMap);
	
	@Select({ "<script> SELECT distinct c.shop_id as 'eCustomerNo' \r\n" + 
			" , c.email \r\n" + 
			" , cap.order_tag as 'orderTag'\r\n" + 
			" FROM cms_db.customer c\r\n" + 
			" JOIN cms_db.customer_auto_pay cap on cap.email=c.email\r\n" + 
			" where (c.shop_id is not null and trim(c.shop_id)!='')\r\n" + 
			"	and c.oos_include = 'Y' " + 
			" <if test='email != null'> and c.email = #{email} </if> " +
	 		"    </script>", 
			""})
	public List<Map<String, Object>> getAutoIssuanceCustomers(HashMap<String, Object> paramMap);
	
	@Insert({ "<script>",
		"INSERT INTO cms_db.customer ( customer_number,\r\n" + 
		"shop_id,\r\n" + 
		"group_customer_no,\r\n" + 
		"first_name,\r\n" + 
		"middle_name,\r\n" + 
		"last_name,\r\n" + 
		"full_name,\r\n" + 
		"cust_group,\r\n" + 
		"type,\r\n" + 
		"email,\r\n" + 
		"phone_number,\r\n" + 
		"oos_warehouse_1,\r\n" + 
		"oos_warehouse_2,\r\n" + 
		"oos_include,\r\n" + 
			"created_date_time,\r\n" + 
		"modified_date_time,\r\n" + 
		"db_create_date,\r\n" + 
		"db_update_date,\r\n" + 
		"db_update_by,\r\n" + 
		"publish_date ) VALUES ( #{customerNumber},\r\n" + 
		"#{shopId},\r\n" + 
		"#{groupCustomerNo},\r\n" + 
		"#{firstName},\r\n" + 
		"#{middleName},\r\n" + 
		"#{lastName},\r\n" + 
		"#{fullName},\r\n" + 
		"#{custGroup},\r\n" + 
		"#{type},\r\n" + 
		"#{email},\r\n" + 
		"#{phoneNumber},\r\n" + 
		"#{oosWarehouse1},\r\n" + 
		"#{oosWarehouse2},\r\n" + 
		"#{oosInclude},\r\n" + 
			"#{createdDateTime},\r\n" + 
		"#{modifiedDateTime},\r\n" + 
		"now() ,\r\n" + 
		"now() ,\r\n" + 
		"#{dbUpdateBy},\r\n" + 
		"#{publishDate} ) ",
	"</script>" })
	public int insertCustomer(HashMap<String, Object> paramMap);

	
	@Insert({ "<script>",
			"INSERT INTO cms_db.customer ( customer_number,\r\n" + 
			"shop_id,\r\n" + 
			"group_customer_no,\r\n" + 
			"first_name,\r\n" + 
			"middle_name,\r\n" + 
			"last_name,\r\n" + 
			"full_name,\r\n" + 
			"cust_group,\r\n" + 
			"type,\r\n" + 
			"email,\r\n" + 
			"phone_number,\r\n" + 
			"oos_warehouse_1,\r\n" + 
			"oos_warehouse_2,\r\n" + 
			"oos_include,\r\n" + 
		    "created_date_time,\r\n" + 
			"modified_date_time,\r\n" + 
			"db_create_date,\r\n" + 
			"db_update_date,\r\n" + 
			"db_update_by,\r\n" + 
			"publish_date ) VALUES ( #{customerNumber},\r\n" + 
			"#{shopId},\r\n" + 
			"#{groupCustomerNo},\r\n" + 
			"#{firstName},\r\n" + 
			"#{middleName},\r\n" + 
			"#{lastName},\r\n" + 
			"#{fullName},\r\n" + 
			"#{custGroup},\r\n" + 
			"#{type},\r\n" + 
			"#{email},\r\n" + 
			"#{phoneNumber},\r\n" + 
			"#{oosWarehouse1},\r\n" + 
			"#{oosWarehouse2},\r\n" + 
			"#{oosInclude},\r\n" + 
				"#{createdDateTime},\r\n" + 
			"#{modifiedDateTime},\r\n" + 
			"now() ,\r\n" + 
			"now() ,\r\n" + 
			" 'ADMIN',\r\n" + 
			"#{publishDate} ) ",
		"</script>" })
	public int insertCustomerForSync(HashMap<String, Object> paramMap);

	
	@Update({ "<script>  UPDATE cms_db.customer  SET db_update_date = now() " +  
			"<if test='shopId != null'>, shop_id = #{shopId} </if> \r\n" + 
			"<if test='groupCustomerNo != null'>, group_customer_no = #{groupCustomerNo} </if> \r\n" + 
			"<if test='firstName != null'>, first_name = #{firstName} </if> \r\n" + 
			"<if test='middleName != null'>, middle_name = #{middleName} </if> \r\n" + 
			"<if test='lastName != null'>, last_name = #{lastName} </if> \r\n" + 
			"<if test='fullName != null'>, full_name = #{fullName} </if> \r\n" + 
			"<if test='custGroup != null'>, cust_group = #{custGroup} </if> \r\n" + 
			"<if test='type != null'>, type = #{type} </if> \r\n" + 
			"<if test='email != null'>, email = #{email} </if> \r\n" + 
			"<if test='phoneNumber != null'>, phone_number = #{phoneNumber} </if> \r\n" + 
			"<if test='oosWarehouse1 != null'>, oos_warehouse_1 = #{oosWarehouse1} </if> \r\n" + 
			"<if test='oosWarehouse2 != null'>, oos_warehouse_2 = #{oosWarehouse2} </if> \r\n" + 
			"<if test='oosInclude != null'>, oos_include = #{oosInclude} </if> \r\n" + 
	        "<if test='createdDateTime != null'>, created_date_time = #{createdDateTime} </if> \r\n" + 
			"<if test='modifiedDateTime != null'>, modified_date_time = #{modifiedDateTime} </if> \r\n" +  
			"<if test='dbUpdateBy != null'>, db_update_by = #{dbUpdateBy} </if> \r\n" + 
			"<if test='publishDate != null'>, publish_date = #{publishDate} </if> "
			
			+ " WHERE customer_number is not null "
			+ "<if test='customerNumber != null'> AND customer_number = #{customerNumber} </if> "
	  			 		
	 	  , "</script>" })
	public int updateCustomer(HashMap<String, Object> paramMap);
   
	
	@Update({ "<script> UPDATE cms_db.customer  SET db_update_date = now() " +  
			"<if test='shopId != null'>, shop_id = #{shopId} </if> \r\n" + 
			"<if test='groupCustomerNo != null'>, group_customer_no = #{groupCustomerNo} </if> \r\n" + 
			"<if test='firstName != null'>, first_name = #{firstName} </if> \r\n" + 
			"<if test='middleName != null'>, middle_name = #{middleName} </if> \r\n" + 
			"<if test='lastName != null'>, last_name = #{lastName} </if> \r\n" + 
			"<if test='fullName != null'>, full_name = #{fullName} </if> \r\n" + 
			"<if test='custGroup != null'>, cust_group = #{custGroup} </if> \r\n" + 
			"<if test='type != null'>, type = #{type} </if> \r\n" +  
			"<if test='phoneNumber != null'>, phone_number = #{phoneNumber} </if> \r\n" + 
			"<if test='oosWarehouse1 != null'>, oos_warehouse_1 = #{oosWarehouse1} </if> \r\n" + 
			"<if test='oosWarehouse2 != null'>, oos_include = #{oosWarehouse2} </if> \r\n" + 
			"<if test='oosInclude != null'>, oos_warehouse_2 = #{oosInclude} </if> \r\n" + 
			"<if test='createdDateTime != null'>, created_date_time = #{createdDateTime} </if> \r\n" + 
			"<if test='modifiedDateTime != null'>, modified_date_time = #{modifiedDateTime} </if> \r\n" +  
			"<if test='dbUpdateBy != null'>, db_update_by = #{dbUpdateBy} </if> \r\n" + 
			"<if test='publishDate != null'>, publish_date = #{publishDate} </if> "
			
			+ " WHERE customer_number is not null "
			+ "<if test='customerNumber != null'> AND customer_number = #{customerNumber} </if> "
			+ "<if test='email != null'> AND email = #{email} </if> "
	  			 		
	 	  , "</script>" })
	public int updateCustomerByEmail(HashMap<String, Object> paramMap);
	
	@Delete({ "<script>", " DELETE FROM cms_db.customer ",
				" WHERE customer_number is not null  ",
			"    <if test='customerNumber != null'> AND customer_number=#{customerNumber} </if> "
			, "</script>" })
	public int deleteCustomer(HashMap<String, Object> paramMap);
	
	
	@Delete({ "<script>", " delete \r\n" + 
			" from cms_db.customer\r\n" + 
			" where db_update_date between DATE_SUB(now(), INTERVAL 1 WEEK) \r\n" + 
			"		and DATE_SUB(now(), INTERVAL 12 HOUR)  </script>" })
	public int deleteOldCustomerDetailData();

	
	@Delete({ "<script>", " delete \r\n" + 
			" from cms_db.customer_address \r\n" + 
			" where db_update_date between DATE_SUB(now(), INTERVAL 1 WEEK) \r\n" + 
			"		and DATE_SUB(now(), INTERVAL 12 HOUR)  </script>" })
	public int deleteOldCustomerAddressData();

}