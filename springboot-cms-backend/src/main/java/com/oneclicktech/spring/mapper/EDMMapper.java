
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
public interface EDMMapper {

	@Select({ "<script> SELECT  eci.edm_id as 'edmId',\r\n" + 
			"		eci.edm_name as 'edmName',\r\n" + 
			"		eci.edm_type as 'edmType',\r\n" + 
			"		eci.image_content as 'imageContent',\r\n" + 
			"		eci.image_url as 'imageUrl',\r\n" + 
			"		eci.promo_link as 'promoLink',\r\n" + 
			"		eci.promote_flag as 'promoteFlag',\r\n" + 
			"		eci.promote_start_date as 'promoteStartDate',\r\n" + 
			"		eci.promote_end_date as 'promoteEndDate',\r\n" + 
		   "		eci.edm_form_id as 'edmFormId',\r\n" + 
			"		eci.update_date as 'updateDate', \r\n" + 
			"		ef.first_name as 'firstName',\r\n" + 
			"		ef.last_name as 'lastName',\r\n" + 
			"		ef.email as 'email',\r\n" + 
			"		ef.contact_no as 'contactNo',\r\n" + 
			"		ef.company as 'company',\r\n" + 
			"		ef.address_1 as 'address1',\r\n" + 
			"		ef.city as 'city',\r\n" + 
			"		ef.region as 'region',\r\n" + 
			"		ef.country as 'country',\r\n" + 
			"		ef.notes as 'notes' \r\n" + 
			" FROM cms_db.edm_config_info eci\r\n" + 
			" LEFT JOIN cms_db.edm_form ef on eci.edm_form_id  = ef.form_id\r\n" + 
			" WHERE eci.edm_id is not null",
			" <if test='edmId != null'> and eci.edm_id = #{edmId} </if> ",
			" <if test='edmName != null'> and eci.edm_name = #{edmName} </if> ",
			" <if test='edmFormId != null'> and eci.edm_form_id = #{edmFormId} </if> "
			+ " ORDER BY eci.update_date desc",
				
			"</script>", "" })
	public List<Map<String, Object>> getEDMConfigList(HashMap<String, Object> paramMap);

	@Select({ "<script> SELECT ori.order_name as 'orderName'  \r\n" + 
			"   , ori.customer_no as 'customerNo'  \r\n" + 
			"   , ca.store_name as 'storeName'  \r\n" + 
			"   , ori.goods_discount as 'goodsDiscount'  \r\n" + 
			"   , ori.services_discount as 'servicesDiscount'  \r\n" + 
			"   , ori.ewt_file_link as 'ewtFileLink'  \r\n" + 
			"   , ori.order_with_discount as 'orderWithDiscount'  \r\n" + 
			"   , ori.ship_with_discount as 'shipWithDiscount'  \r\n" + 
			"   , ori.disable_acct as 'disableAcct'  \r\n" + 
		    " FROM cms_db.ewt_order_info ori  \r\n" + 
			" JOIN cms_db.customer_address ca on ca.customer_number = ori.customer_no\r\n" + 
			" WHERE ori.order_name is not null " + 
			" <if test='orderName != null'> and ori.order_name  = #{orderName} </if> ",
			" <if test='customerNo != null'> and ori.customer_no  = #{customerNo} </if> ",

			"</script>", "" })
	public List<Map<String, Object>> getOrderEWTList(HashMap<String, Object> paramMap);

 
	@Select({ "<script> SELECT * FROM cms_db.config_discount " + " WHERE discount_type is not null ",
			"<if test='discountType != null'> and discount_type = #{discountType} </if> \r\n" + "</script>", "" })
	public List<Map<String, Object>> getConfigDiscountList(HashMap<String, Object> paramMap);

	@Insert({ "<script>",
			" INSERT INTO cms_db.edm_config_info\r\n" + 
			"( edm_name,\r\n" + 
			"edm_type,\r\n" + 
			"image_content,\r\n" + 
			"image_url,\r\n" + 
			"promo_link,\r\n" + 
			"promote_flag,\r\n" + 
			"promote_start_date,\r\n" + 
			"promote_end_date,\r\n" + 
		     "edm_form_id,\r\n" + 
			"update_date)\r\n" + 
			"VALUES\r\n" + 
			"(#{edmName},\r\n" + 
			"#{edmType},\r\n" + 
			"#{imageContent},\r\n" + 
			"#{imageUrl},\r\n" + 
			"#{promoLink},\r\n" + 
			"#{promoteFlag},\r\n" + 
			"#{promoteStartDate},\r\n" + 
			"#{promoteEndDate},\r\n" + 
	    	"#{edmFormId},\r\n" + 
			"now()) ",
			"</script>" })
	public int insertEDMConfig(HashMap<String, Object> paramMap);

	@Insert({ "<script>",
			" INSERT INTO cms_db.ewt_order_info \r\n" 
				+ " (order_name\r\n" 
				+ " ,customer_no\r\n" 
				+ " ,goods_discount\r\n"
				+ " ,services_discount\r\n" 
				+ " ,ewt_file_link\r\n"
				+ " ,order_with_discount\r\n"
				+ " ,ship_with_discount\r\n"
			    + " ,update_date) \r\n" 
					+ "VALUES\r\n"
				+ " (#{orderName},\r\n" 
				+ "#{customerNo},\r\n" 
				+ "#{goodsDiscount},\r\n" 
				+ "#{servicesDiscount},\r\n"
			  	+ "#{ewtFileLink},"
				+ "#{orderWithDiscount},"
				+ "#{shipWithDiscount},"
					+ "now()) ",
			"</script>" })
	public int insertEDMForm(HashMap<String, Object> paramMap);

	@Update({ "<script> UPDATE cms_db.edm_config_info \r\n" 
			+ " SET update_date = now() \r\n"
			+ "<if test='edmName != null'>, edm_name = #{edmName} </if> \r\n" + 
			" <if test='edmType != null'>, edm_type = #{edmType} </if> \r\n" + 
			" <if test='imageContent != null'>, image_content = #{imageContent} </if> \r\n" + 
			" <if test='imageUrl != null'>, image_url = #{imageUrl} </if> \r\n" + 
			" <if test='promoLink != null'>, promo_link = #{promoLink} </if> \r\n" + 
			" <if test='promoteFlag != null'>, promote_flag = #{promoteFlag} </if> \r\n" + 
			" <if test='promoteStartDate != null'>, promote_start_date = #{promoteStartDate} </if> \r\n" + 
			" <if test='promoteEndDate != null'>, promote_end_date = #{promoteEndDate} </if> \r\n" + 
			" <if test='edmFormId != null'>, edm_form_id = #{edmFormId} </if> "
		  	+ " WHERE edm_id is not null"

			, "</script>" })
	public int updateEDMConfig(HashMap<String, Object> paramMap);

 
	
	@Delete({ "<script>", " DELETE FROM cms_db.edm_config_info ",
		" WHERE edm_id = #{edmId}   ",
  	  	 "</script>" })
	public int deleteEDMConfig(HashMap<String, Object> paramMap);
	
}