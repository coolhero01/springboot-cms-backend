package com.oneclicktech.spring.mapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CustomerAddressMapper {
	@Select({ "<script> "
			+ " SELECT tbl_id as 'tblId',\r\n" + 
			" customer_number as 'customerNumber',\r\n" + 
			" shop_addr_id as 'shopAddrId',\r\n" + 
		    " description as 'description',\r\n" + 
			" store_code as 'storeCode',\r\n" + 
			" store_name as 'storeName',\r\n" + 
			" concat(customer_number,'-',store_name) as 'storeCustomerNo',\r\n" + 
			 " address as 'address',\r\n" + 
			" town_city as 'townCity',\r\n" + 
			" street as 'street',\r\n" + 
			" province as 'province',\r\n" + 
			" zip_code as 'zipCode',\r\n" + 
			" country as 'country',\r\n" + 
			" db_create_date as 'dbCreateDate',\r\n" + 
			" db_update_date as 'dbUpdateDate',\r\n" + 
			" db_update_by as 'dbUpdateBy' ", 
		
			" FROM cms_db.customer_address ", 
			" WHERE tbl_id is not null  ",
			"    <if test='customerNumber != null'> AND customer_number = #{customerNumber}</if>", 
			"    <if test='shopAddrId != null'> AND shop_addr_id = #{shopAddrId}</if>", 
			 	"</script>", 
			""})
	public List<Map<String, Object>> getCustomerAddressList(HashMap<String, Object> paramMap);
	
	
	@Select({ "<script> " + 
			" SELECT DISTINCT ca.customer_number as 'customerNumber'\r\n" + 
			"      , ca.store_name as 'storeName'\r\n" + 
			"      , ca.store_code as 'storeCode'\r\n" + 
			"      , CONCAT(ca.store_code, ' ', ca.store_name) as 'storeNameCode'\r\n" + 
		    "      , ca.shop_addr_id as 'shopAddrId'\r\n" + 
			"      , c.oos_warehouse_1 as 'warehouse1'\r\n" + 
			"      , c.oos_warehouse_2 as 'warehouse2'\r\n" + 
			" FROM cms_db.customer_address ca\r\n" + 
			" JOIN cms_db.customer c on ca.customer_number = c.customer_number\r\n" + 
			" WHERE (ca.store_name is not null and trim(ca.store_name) != '') \r\n" + 
			"  and (c.oos_warehouse_1 is not null and trim(c.oos_warehouse_1) != '')",
			"    <if test='warehouse != null'> AND (c.oos_warehouse_1 = #{warehouse} or c.oos_warehouse_2 = #{warehouse})  </if>", 
			"    <if test='customerNumber != null'> AND ca.customer_number = #{customerNumber}</if>", 
			 "   <if test='shopAddrId != null'> AND ca.shop_addr_id = #{shopAddrId}</if>", 
		   "</script>", 
			""})
	public List<Map<String, Object>> getStoresByWarehouse(HashMap<String, Object> paramMap);
	
	
	@Insert({ "<script>",
 			"INSERT INTO cms_db.customer_address (customer_number,\r\n" + 
			"shop_addr_id,\r\n" + 
			"description,\r\n" + 
	 			"store_code,\r\n" + 
 			"store_name,\r\n" + 
 			"address,\r\n" + 
 			"town_city,\r\n" + 
 			"street,\r\n" + 
 			"province,\r\n" + 
 			"zip_code,\r\n" + 
 			"country,\r\n" + 
 			"db_create_date,\r\n" + 
 			"db_update_date,\r\n" + 
 			"db_update_by ", 
 		 	 " ) VALUES (  #{customerNumber},\r\n" + 
 			"#{shopAddrId},\r\n" + 
 			"#{description},\r\n" + 
 			 "#{storeCode},\r\n" + 
 		 	 "#{storeName},\r\n" + 
 		 	 "#{address},\r\n" + 
 		 	 "#{townCity},\r\n" + 
 		 	 "#{street},\r\n" + 
 		 	 "#{province},\r\n" + 
 		 	 "#{zipCode},\r\n" + 
 		 	 "#{country},\r\n" + 
 		 	 "now(),\r\n" + 
 		 	 "now(),\r\n" + 
 		 	 "#{dbUpdateBy} ) ",
			"</script>" })
	public int insertCustomerAddress(HashMap<String, Object> paramMap);

	
	@Insert({ "<script>",
			"INSERT INTO cms_db.customer_address ( customer_number,\r\n" + 
			"description,\r\n" + 
			"store_code,\r\n" + 
			"store_name,\r\n" + 
			"address,\r\n" + 
			"town_city,\r\n" + 
			"street,\r\n" + 
			"province,\r\n" + 
			"zip_code,\r\n" + 
			"country,\r\n" + 
			"db_create_date,\r\n" + 
			"db_update_date,\r\n" + 
			"db_update_by ", 
		 	 " ) VALUES ( #{CustomerNumber},\r\n" + 
		 	 "#{Description},\r\n" + 
		 	 "#{StoreCode},\r\n" + 
		 	 "#{StoreName},\r\n" + 
		 	 "#{Address},\r\n" + 
		 	 "#{TownCity},\r\n" + 
		 	 "#{Street},\r\n" + 
		 	 "#{Province},\r\n" + 
		 	 "#{ZipCode},\r\n" + 
		 	 "#{Country},\r\n" + 
		 	 "now(),\r\n" + 
		 	 "now(),\r\n" + 
		 	 "'ADMIN') ",
		"</script>" })
	public int insertCustomerAddressForSync(HashMap<String, Object> paramMap);

	
	@Update({ "<script> UPDATE cms_db.customer_address set db_update_date = now() " + 
			 	"<if test='shopAddrId != null'>, shop_addr_id = #{shopAddrId} </if> " +   
			 	"<if test='description != null'>, description = #{description} </if> " + 
			 	"<if test='storeCode != null'>, store_code = #{storeCode} </if> " + 
			 	"<if test='storeName != null'>, store_name = #{storeName} </if> " + 
			 	"<if test='address != null'>, address = #{address} </if> " + 
			 	"<if test='townCity != null'>, town_city = #{townCity} </if> " + 
			 	"<if test='street != null'>, street = #{street} </if> " + 
			 	"<if test='province != null'>, province = #{province} </if> " + 
			 	"<if test='zipCode != null'>, zip_code = #{zipCode} </if> " + 
			 	"<if test='country != null'>, country = #{country} </if> " +  
			 	"<if test='dbUpdateBy != null'>, db_update_by = #{dbUpdateBy} </if> "
			+ " WHERE customer_number is not null "
			+ "  <if test='customerNumber != null'> and customer_number = #{customerNumber} </if>  "  
	 	   ,"</script>" })
	public int updateCustomerAddress(HashMap<String, Object> paramMap);
	
	@Delete({ "<script>", " DELETE FROM cms_db.customer_address ", " WHERE customer_number is not null  ",
			"    <if test='customerNumber != null'> AND customer_number=#{customerNumber}</if>", "</script>" })
	public int deleteCustomerAddress(HashMap<String, Object> paramMap);

}