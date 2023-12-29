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
public interface ProductMasterMapper {
	@Select({ "<script>", " SELECT prd_id as 'prdId',\r\n" + 
			"item_id as 'itemId',\r\n" + 
			"search_name as 'searchName',\r\n" + 
			"product_name as 'productName',\r\n" + 
			"category as 'category',\r\n" + 
			"item_group as 'itemGroup',\r\n" + 
			"sub_item_group as 'subItemGroup',\r\n" + 
			"cost_price as 'costPrice',\r\n" + 
			"purchase_price as 'purchasePrice',\r\n" + 
			"sales_price as 'salesPrice',\r\n" + 
			"cost_active_date as 'costActiveDate',\r\n" + 
			"purch_active_date as 'purchActiveDate',\r\n" + 
			"sales_active_date as 'salesActiveDate',\r\n" + 
			"cost_tax_item_group as 'costTaxItemGroup',\r\n" + 
			"purch_tax_item_group as 'purchTaxItemGroup',\r\n" + 
			"sales_tax_item_group as 'salesTaxItemGroup',\r\n" + 
			"cost_over_delivery as 'costOverDelivery',\r\n" + 
			"purch_over_delivery as 'purchOverDelivery',\r\n" + 
			"sales_over_delivery as 'salesOverDelivery',\r\n" + 
			"sos_pool_id as 'sosPoolId',\r\n" + 
			"sos_category_id as 'sosCategoryId',\r\n" + 
			"available_ordered as 'availableOrdered',\r\n" + 
			"available_physical as 'availablePhysical',\r\n" + 
			"on_order as 'onOrder',\r\n" + 
			"cost_unit_id as 'costUnitId',\r\n" + 
			"purch_unit_id as 'purchUnitId',\r\n" + 
			"sales_unit_id as 'salesUnitId',\r\n" + 
			"cost_modified_date_time as 'costModifiedDateTime',\r\n" + 
			"purch_modified_date_time as 'purchModifiedDateTime',\r\n" + 
			"sales_modified_date_time as 'salesModifiedDateTime',\r\n" + 
			"data_area_id as 'dataAreaId',\r\n" + 
			"business_unit as 'businessUnit',\r\n" + 
			"department as 'department',\r\n" + 
			"purpose as 'purpose',\r\n" + 
			"sales_segment as 'salesSegment',\r\n" + 
			"modified_date_time as 'modifiedDateTime',\r\n" + 
			"created_date_time as 'createdDateTime',\r\n" + 
			"model_group as 'modelGroup' FROM cms_db.PRODUCT_MASTER ", " WHERE prd_id is not null  ",
			"    <if test='itemId != null'> AND item_id=#{itemId}</if>", "</script>" })
	public List<Map<String, Object>> getProductList(HashMap<String, Object> paramMap);

	@Insert({ "<script>",
 			"INSERT INTO cms_db.product_master", "(prd_id,", "item_id,", "search_name,", "product_name,", "category,",
			"item_group,", "sub_item_group,", "cost_price,", "purchase_price,", "sales_price,", "cost_active_date,",
			"purch_active_date,", "sales_active_date,", "cost_tax_item_group,", "purch_tax_item_group,",
			"sales_tax_item_group,", "cost_over_delivery,", "purch_over_delivery,", "sales_over_delivery,",
			"sos_pool_id,", "sos_category_id,", "available_ordered,", "available_physical,", "on_order,",
			"cost_unit_id,", "purch_unit_id,", "sales_unit_id,", "cost_modified_date_time,",
			"purch_modified_date_time,", "sales_modified_date_time,", "data_area_id,", "business_unit,", "department,",
			"purpose,", "sales_segment,", "modified_date_time,", "created_date_time,", "model_group, db_create_date, db_update_date)"
			, "VALUES",
			"(#{prdId},", "#{itemId},", "#{searchName},", "#{productName},", "#{category},", "#{itemGroup},",
			"#{SubItemGroup},", "#{costPrice},", "#{purchasePrice},", "#{salesPrice},", "#{costActiveDate},",
			"#{purchActiveDate},", "#{salesActiveDate},", "#{costTaxItemGroup},", "#{purchTaxItemGroup},",
			"#{salesTaxItemGroup},", "#{costOverDelivery},", "#{purchOverDelivery},", "#{salesOverDelivery},",
			"#{sosPoolId},", "#{sosCategoryId},", "#{availableOrdered},", "#{availablePhysical},", "#{onOrder},",
			"#{costUnitId},", "#{purchUnitId},", "#{salesUnitId},", "#{costModifiedDateTime},",
			"#{purchModifiedDateTime},", "#{salesModifiedDateTime},", "#{dataAreaId},", "#{businessUnit},",
			"#{department},", "#{purpose},", "#{salesSegment},", "#{modifiedDateTime},", "#{createdDateTime},", "#{modelGroup}, "
			, "now(),", "now()" + " );",
			"</script>" })
	public int insertProductMaster(HashMap<String, Object> paramMap);

	@Update({ "<script>", "INSERT INTO cms_db.product_master", "(prd_id,", "item_id,", "search_name,",
			 
			"#{department},", "#{purpose},", "#{salesSegment},", "#{modifiedDateTime},", "now(),", "#{modelGroup})",
			"</script>" })
	public int updateProductMaster(HashMap<String, Object> paramMap);
   
	@Delete({ "<script>", " SELECT * FROM cms_db.PRODUCT_MASTER ", " WHERE prd_id is not null  ",
			"    <if test='itemId != null'> AND item_id=#{itemId}</if>", "</script>" })
	public int deleteProductMaster(HashMap<String, Object> paramMap);

}