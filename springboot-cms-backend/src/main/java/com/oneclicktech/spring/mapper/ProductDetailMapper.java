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
public interface ProductDetailMapper {
	@Select({
			"<script>SELECT tbl_id as 'tblId', \r\n " + "name as 'name', \r\n " + "item_id as 'itemId', \r\n "
					+ "concat(item_id,'-',name) as 'idWithName', \r\n " + "shop_id as 'shopId', \r\n "
					+ "description as 'description', \r\n " + "sku as 'sku', \r\n " + "category as 'category', \r\n "
					+ "sub_category as 'subCategory', \r\n " + "purch_unit_id as 'purchUnitId', \r\n "
					+ "sales_unit_id as 'salesUnitId', \r\n " + "cost_unit_id as 'costUnitId', \r\n "
					+ "item_group as 'itemGroup', \r\n " + "purpose as 'purpose', \r\n " + "status as 'status', \r\n "
					+ "sell_price as 'sellPrice', \r\n " + "oos_status as 'oosStatus', \r\n "
					+ "oos_moq as 'oosMoq', \r\n " + "oos_boq as 'oosBoq', \r\n "
					+ "db_create_date as 'dbCreateDate', \r\n " + "db_update_date as 'dbUpdateDate', \r\n "
					+ "db_update_by as 'dbUpdateBy', \r\n " + "publish_date as 'publishDate' ",

			" FROM cms_db.product_detail ", " WHERE tbl_id is not null  ",
			"  <if test='name != null'> AND name=#{name}</if>",
			"  <if test='itemGroup != null'> AND item_group=#{itemGroup}</if>",

			"</script>", "" })
	public List<Map<String, Object>> getProductList(HashMap<String, Object> paramMap);

	@Select({
			"<script> SELECT DISTINCT pd.name as 'itemName'\r\n" + 
			"	, sai.item_id as 'itemId'\r\n" + 
			"    , sai.qty_issuance as 'promoQty'\r\n" + 
			"    , pi.available_physical as 'availInventory'\r\n" + 
			"FROM cms_db.store_auto_issuance sai\r\n" + 
			"JOIN cms_db.product_detail pd on sai.item_id = pd.item_id\r\n" + 
			"LEFT JOIN cms_db.product_inventory pi on sai.item_id = pi.item_number\r\n" + 
			"	 and sai.warehouse_code = pi.warehouse\r\n" + 
			"WHERE sai.item_id is not null \r\n" + 
			"  and promo_code = '#{promoCode}' \r\n" + 
			"  and warehouse_code = '#{warehouseCode}'\r\n" + 
			"  and customer_no = '#{customerNo}' ",

			"</script>", "" })
	public List<Map<String, Object>> getProductListByPromo(HashMap<String, Object> paramMap);

	@Select({ "<script> SELECT *  ", " FROM cms_db.product_detail ", " WHERE item_id = #{itemId} ",
			" LIMIT 1  </script>", "" })
	public Map<String, Object> getProductByItemId(HashMap<String, Object> paramMap);

	@Select({ "<script> SELECT *  ", " FROM cms_db.product_detail ", " WHERE shop_id = #{shopId} ",
			" LIMIT 1  </script>", "" })
	public Map<String, Object> getProductByShopId(HashMap<String, Object> paramMap);

	@Select({ "<script> SELECT DISTINCT pi.item_number as 'itemNumber',   \r\n "
			+ "	pi.shop_prod_id as 'shopProdId' ,  \r\n " + "	pd.name as 'name' ,  \r\n "
			+ "	lower(pd.sales_unit_id) as 'salesUnitId',  \r\n " + "	pd.sell_price as 'sellPrice', \r\n "
			+ "	pd.item_group as 'itemGroup', \r\n " + "	pd.status as 'status', \r\n "
			+ "	pd.oos_status as 'oosStatus', \r\n " + "	pd.oos_moq as 'oosMoq', \r\n "
			+ "	pd.oos_boq as 'oosBoq', \r\n " + "	FLOOR(pi.physical_inventory) as 'physicalInventory',  \r\n "
			+ " lower(replace(replace(replace(REGEXP_REPLACE(concat(trim(pd.name) , \"-\", trim(pi.item_number), \"-\" ,trim(wh.warehouse)),'[^0-9a-zA-Z ]','-'),' ','-'),'---','-'),'--','-')) as 'productHandle', \r\n "
			+ " pi.warehouse  as 'warehouseCode', \r\n " + "	wh.name as 'warehouseName', \r\n "
			+ "	wh.site as 'warehouseSite', \r\n " + "	wh.address as 'warehouseAddress' \r\n "
			+ " FROM cms_db.product_inventory pi  \r\n "
			+ " JOIN cms_db.product_detail pd on pd.item_id = pi.item_number \r\n "
			+ " LEFT JOIN cms_db.warehouse wh on wh.warehouse = pi.warehouse\r\n " + " WHERE pd.name is not null \r\n "
			+ "    <if test='itemNumber != null'> AND pi.item_number in (${itemNumber})</if>"
			+ "    <if test='oosStatus != null'> AND pd.oos_status = #{oosStatus}  </if>"
			+ " ORDER BY pi.item_number,  pi.warehouse  "

			, "</script>", "" })
	public List<Map<String, Object>> getProductsWithInventory(HashMap<String, Object> paramMap);

	@Insert({ "<script>", "INSERT INTO cms_db.product_detail (name, \r\n " + "item_id, \r\n " + "shop_id, \r\n "
			+ "description, \r\n " + "sku, \r\n " + "category, \r\n " + "sub_category, \r\n " + "purch_unit_id, \r\n "
			+ "sales_unit_id, \r\n " + "cost_unit_id, \r\n " + "item_group, \r\n " + "purpose, \r\n " + "status, \r\n "
			+ "sell_price, \r\n " + "oos_status, \r\n " + "oos_moq, \r\n " + "oos_boq, \r\n " + "db_create_date, \r\n "
			+ "db_update_date, \r\n " + "db_update_by, \r\n " + "publish_date ) VALUES ( #{name}, \r\n "
			+ "#{itemId}, \r\n " + "#{shopId}, \r\n " + "#{description}, \r\n " + "#{sku}, \r\n " + "#{category}, \r\n "
			+ "#{subCategory}, \r\n " + "#{purchUnitId}, \r\n " + "#{salesUnitId}, \r\n " + "#{costUnitId}, \r\n "
			+ "#{itemGroup}, \r\n " + "#{purpose}, \r\n " + "#{status}, \r\n " + "#{sellPrice}, \r\n "
			+ "#{oosStatus}, \r\n " + "#{oosMoq}, \r\n " + "#{oosBoq}, \r\n " + "now(), \r\n " + "now(), \r\n "
			+ "#{dbUpdateBy}, \r\n " + "#{publishDate} ) ", "</script>" })
	public int insertProductDetail(HashMap<String, Object> paramMap);

	@Insert({ "<script>", "INSERT INTO cms_db.product_detail (name, \r\n " + "item_id, \r\n " + "description, \r\n "
			+ "sku, \r\n " + "category, \r\n " + "sub_category, \r\n " + "purch_unit_id, \r\n " + "sales_unit_id, \r\n "
			+ "cost_unit_id, \r\n " + "item_group, \r\n " + "purpose, \r\n " + "status, \r\n " + "sell_price, \r\n "
			+ "oos_status, \r\n " + "oos_moq, \r\n " + "oos_boq, \r\n " + "db_create_date, \r\n "
			+ "db_update_date, \r\n " + "db_update_by \r\n " + ") VALUES ( #{name},\r\n" + "#{itemId},\r\n"
			+ "#{description},\r\n" + "#{sku},\r\n" + "#{category},\r\n" + "#{subCategory},\r\n" + "#{purchUnitId},\r\n"
			+ "#{salesUnitId},\r\n" + "#{costUnitId},\r\n" + "#{itemGroup},\r\n" + "#{purpose},\r\n" + "#{status},\r\n"
			+ "#{sellPrice},\r\n" + "#{oosStatus},\r\n" + "#{oosMoq},\r\n" + "#{oosBoq},  " + "now(), " + "now(), "
			+ "'ADMIN' ) ", "</script>" })
	public int insertProductDetailForSync(HashMap<String, Object> paramMap);

	@Update({
			"<script> UPDATE cms_db.product_detail set db_update_date=now() "
					+ "<if test='name != null'>, name = #{name} </if> \r\n "
					+ "<if test='shopId != null'>, shop_id = #{shopId} </if> \r\n "
					+ "<if test='description != null'>, description = #{description} </if> \r\n "
					+ "<if test='sku != null'>, sku = #{sku} </if> \r\n "
					+ "<if test='category != null'>, category = #{category} </if> \r\n "
					+ "<if test='subCategory != null'>, sub_category = #{subCategory} </if> \r\n "
					+ "<if test='purchUnitId != null'>, purch_unit_id = #{purchUnitId} </if> \r\n "
					+ "<if test='salesUnitId != null'>, sales_unit_id = #{salesUnitId} </if> \r\n "
					+ "<if test='costUnitId != null'>, cost_unit_id = #{costUnitId} </if> \r\n "
					+ "<if test='itemGroup != null'>, item_group = #{itemGroup} </if> \r\n "
					+ "<if test='purpose != null'>, purpose = #{purpose} </if> \r\n "
					+ "<if test='status != null'>, status = #{status} </if> \r\n "
					+ "<if test='sellPrice != null'>, sell_price = #{sellPrice} </if> \r\n "
					+ "<if test='oosStatus != null'>, oos_status = #{oosStatus} </if> \r\n "
					+ "<if test='oosMoq != null'>, oos_moq = #{oosMoq} </if> \r\n "
					+ "<if test='oosBoq != null'>, oos_boq = #{oosBoq} </if> \r\n "
					+ "<if test='publishDate != null'>, publish_date = #{publishDate} </if>  "

					+ " WHERE  item_id =  #{itemId}",

			"</script>" })
	public int updateProductDetail(HashMap<String, Object> paramMap);

	@Delete({ "<script>", " DELETE FROM cms_db.product_detail ", " WHERE item_id is not null  ",
			"    <if test='itemId != null'> AND item_id=#{itemId}</if>", "</script>" })
	public int deleteProductDetail(HashMap<String, Object> paramMap);

}