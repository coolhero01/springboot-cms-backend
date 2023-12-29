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
public interface ProductInventoryMapper {
	@Select({ "<script> SELECT tbl_id as 'tblId',\r\n" + 
				" item_number as 'itemNumber',\r\n" + 
				" item_name as 'itemName',\r\n" +  
				" shop_prod_id as 'shopProdId',\r\n" +  
				" warehouse as 'warehouse',\r\n" + 
				" physical_inventory as 'physicalInventory',\r\n" + 
				" physical_reserved as 'physicalReserved',\r\n" + 
				" available_physical as 'availablePhysical',\r\n" + 
				" db_create_date as 'dbCreateDate',\r\n" +  
				" db_update_date as 'dbUpdateDate',\r\n" + 
				" db_update_by as 'dbUpdateBy' ", 
	 		" FROM cms_db.product_inventory ",
		    " WHERE tbl_id is not null  ",
			"    <if test='itemNumber != null'> AND item_number=#{itemNumber}</if>", 
			"    <if test='warehouse != null'> AND warehouse=#{warehouse}</if>", 
		 " </script>", 
				""})
	public List<Map<String, Object>> getProductInventoryList(HashMap<String, Object> paramMap);
	
	
	@Select({ "<script> SELECT shop_prod_id as 'shopProdId' \r\n" + 
			" FROM cms_db.product_inventory\r\n" + 
			" WHERE db_update_date between DATE_SUB(now(), INTERVAL 1 WEEK) \r\n" + 
			"		and DATE_SUB(now(), INTERVAL 12 HOUR) \r\n" + 
			"  and (shop_prod_id is not null and trim(shop_prod_id) != '')  </script>", "" })
	public List<Map<String, Object>> getOnlineProductsForDeletion(HashMap<String, Object> paramMap);
	
	
	@Select({ "<script> SELECT tbl_id as 'tblId',\r\n" + 
			"item_number as 'itemNumber',\r\n" + 
			"item_name as 'itemName',\r\n" +  
			"shop_prod_id as 'shopProdId',\r\n" +  
			"warehouse as 'warehouse',\r\n" + 
			"physical_inventory as 'physicalInventory',\r\n" + 
			"physical_reserved as 'physicalReserved',\r\n" + 
			"available_physical as 'availablePhysical',\r\n" + 
			"db_create_date as 'dbCreateDate',\r\n" +    
			"db_update_date as 'dbUpdateDate',\r\n" + 
			"db_update_by as 'dbUpdateBy' ", 
		
			" FROM cms_db.product_inventory ", " WHERE tbl_id is not null  ",
			"    <if test='itemNumber != null'> AND item_number=#{itemNumber}</if>", 
			"    <if test='warehouse != null'> AND warehouse=#{warehouse}</if>", 
			"    <if test='shopProdId != null'> AND shop_prod_id=#{shopProdId}</if> "
			+ "  LIMIT 1 ", 
				
			"</script>", 
				""})
	public Map<String, Object> getProductInventoryById(HashMap<String, Object> paramMap);
	
	@Insert({ "<script>",
 			"INSERT INTO cms_db.product_inventory ( tbl_id,\r\n" + 
 			"item_number,\r\n" + 
 			"item_name,\r\n" + 
 			"warehouse,\r\n" + 
 			"physical_inventory,\r\n" + 
 			"physical_reserved,\r\n" + 
 			"available_physical,\r\n" + 
 			"db_create_date,\r\n" + 
 			"db_update_date,\r\n" + 
 			"db_update_by ", 
 		 	 " ) VALUES ( #{tblId},\r\n" + 
 		 	 "#{itemNumber},\r\n" + 
 		 	 "#{itemName},\r\n" + 
 		 	 "#{warehouse},\r\n" + 
 		 	 "#{physicalInventory},\r\n" + 
 		 	 "#{physicalReserved},\r\n" + 
 		 	 "#{availablePhysical},\r\n" + 
 		 	 "now(),\r\n" + 
 		 	 "now(),\r\n" + 
 		 	 "#{dbUpdateBy} ) ",
			"</script>" })
	public int insertProductInventory(HashMap<String, Object> paramMap);

	
	@Insert({ "<script>",
			"INSERT INTO cms_db.product_inventory (item_number,\r\n" + 
			"item_name,\r\n" + 
			"warehouse,\r\n" + 
			"physical_inventory,\r\n" + 
			"physical_reserved,\r\n" + 
			"available_physical, \r\n" + 
			"db_create_date,\r\n" + 
			"db_update_date,\r\n" + 
			"db_update_by  ", 
		 	 " ) VALUES ( #{itemNumber},\r\n" + 
		 	 "#{itemName},\r\n" + 
		 	 "#{warehouse},\r\n" + 
		 	 "#{physicalInventory},\r\n" + 
		 	 "#{physicalReserved},\r\n" + 
		 	 "#{availablePhysical}, " +
			 "now(), " +
			 "now(), " +
		  	 "'ADMIN' ) ",
		"</script>" })
	public int insertProductInventoryForSync(HashMap<String, Object> paramMap);

	
	@Update({ "<script> UPDATE cms_db.product_inventory set db_update_date = now() " + 
			"<if test='itemName != null'>, item_name = #{itemName} </if> \r\n" + 
			"<if test='shopProdId != null'>, shop_prod_id = #{shopProdId} </if> \r\n" +  
			"<if test='physicalInventory != null'>, physical_inventory = #{physicalInventory} </if> \r\n" + 
			"<if test='physicalReserved != null'>, physical_reserved = #{physicalReserved} </if> \r\n" + 
			"<if test='availablePhysical != null'>, available_physical = #{availablePhysical} </if> \r\n" +  
			"<if test='dbUpdateBy != null'>, db_update_by = #{dbUpdateBy} </if> ",
			" WHERE item_number is not null ",
			"<if test='itemNumber != null'> and item_number = #{itemNumber} </if> ",
			"<if test='warehouse != null'> and warehouse = #{warehouse} </if> ",
				    	
	 	"</script>" })
	public int updateProductInventory(HashMap<String, Object> paramMap);
   
	@Delete({ "<script>", " DELETE FROM cms_db.product_inventory ",
			" WHERE tbl_id is not null  ",
			"    <if test='itemNumber != null'> AND item_number = #{itemNumber} </if>", 
			"    <if test='shopProdId != null'> AND shop_prod_id = #{shopProdId} </if>", 
				" </script>" })
	public int deleteProductInventory(HashMap<String, Object> paramMap);
	

	@Delete({ "<script>", " delete \r\n" + 
			" from cms_db.product_detail \r\n" + 
			" where db_update_date between DATE_SUB(now(), INTERVAL 1 WEEK) \r\n" + 
			"		and DATE_SUB(now(), INTERVAL 12 HOUR)  </script>" })
	public int deleteOldProductDetailData();
	
	
	@Delete({ "<script>", " delete \r\n" + 
			" from cms_db.product_inventory\r\n" + 
			" where db_update_date between DATE_SUB(now(), INTERVAL 1 WEEK) \r\n" + 
			"		and DATE_SUB(now(), INTERVAL 12 HOUR)  </script>" })
	public int deleteOldProductInventoryData();

}