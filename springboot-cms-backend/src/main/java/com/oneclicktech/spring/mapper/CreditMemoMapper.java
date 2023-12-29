
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
public interface CreditMemoMapper {

	@Select({ "<script> SELECT  a.customer_no as 'customerNo',\r\n" + 
			"		a.discount_code as 'discountCode',\r\n" + 
			"		a.discount_amt as 'discountAmt',\r\n" + 
			"		a.publish_date as 'publishDate',\r\n" + 
			"		a.usage_flag as 'usageFlag',\r\n" + 
			"		a.usage_date as 'usageDate',\r\n" + 
			"		a.start_active_date as 'startActiveDate',\r\n" + 
			"		a.order_name as 'orderName',\r\n" + 
			"		a.price_rule_id as 'priceRuleId',\r\n" + 
			"		a.discount_id as 'discountId',\r\n" + 
			"		a.update_date as 'updateDate',\r\n" + 
			"        ca.store_name as 'storeName'\r\n" + 
			" FROM cms_db.credit_memo a\r\n" + 
			" JOIN cms_db.customer_address ca on a.customer_no = ca.customer_number " + 
			" WHERE a.customer_no is not null",
			" <if test='customerNo != null'> and a.customer_no = #{customerNo} </if> ",
			" <if test='discountCode != null'> and a.discount_code = #{discountCode} </if> ", 
			" <if test='publishDateNotNull != null'> and a.publish_date is not null </if> ", 
			" <if test='orderNotNull != null'> and a.order_name is not null </if> ", 
	        " ORDER BY a.update_date desc",
				
			"</script>", "" })
	public List<Map<String, Object>> getCreditMemoList(HashMap<String, Object> paramMap);


	@Select({ "<script> SELECT  a.customer_no as 'customerNo',\r\n" + 
			"		a.discount_code as 'discountCode',\r\n" + 
			"    	a.discount_amt as 'discountAmt' \r\n" + 
			" FROM cms_db.credit_memo a \r\n" + 
			" WHERE a.publish_date is not null ",
			" <if test='customerNo != null'> and a.customer_no = #{customerNo} </if> ",
			" <if test='orderNameIsNull != null'> and a.order_name is null </if> ",
			" <if test='orderNameIsNotNull != null'> and a.order_name is not null </if> ",
					 			
			"</script>", "" })
	public List<Map<String, Object>> getDiscountCodes(HashMap<String, Object> paramMap);

	
	@Insert({ "<script>",
			" INSERT INTO cms_db.credit_memo \r\n" 
				+ " (customer_no,\r\n" + 
				"discount_code,\r\n" + 
				"discount_amt,\r\n" + 
				"publish_date,\r\n" + 
				"usage_flag,\r\n" + 
				"usage_date,\r\n" + 
				"start_active_date,\r\n" + 
				"order_name,\r\n" + 
				"price_rule_id,\r\n" + 
				"discount_id,\r\n" + 
				"update_date) \r\n" 
				+ "VALUES\r\n" +
			  " (#{customerNo},\r\n" + 
			  "#{discountCode},\r\n" + 
			  "#{discountAmt},\r\n" + 
			  "#{publishDate},\r\n" + 
			  "#{usageFlag},\r\n" + 
			  "#{usageDate},\r\n" + 
			  "STR_TO_DATE(#{startActiveDate}, '%m/%d/%Y'),\r\n" + 
			  "#{orderName},\r\n" + 
			  "#{priceRuleId},\r\n" + 
			  "#{discountId}," + 
				"now()) ",
			"</script>" })
	public int insertCreditMemo(HashMap<String, Object> paramMap);

	@Update({ "<script> UPDATE cms_db.credit_memo \r\n" 
			+ " SET update_date = now() \r\n" + 
			" <if test='discountAmt != null'>, discount_amt = #{discountAmt} </if> \r\n" + 
			" <if test='publishDate != null'>, publish_date = #{publishDate} </if> \r\n" + 
			" <if test='usageFlag != null'>, usage_flag = #{usageFlag} </if> \r\n" + 
			" <if test='usageDate != null'>, usage_date = #{usageDate} </if> \r\n" + 
			" <if test='startActiveDate != null'>, start_active_date = #{startActiveDate} </if> \r\n" + 
			" <if test='orderName != null'>, order_name = #{orderName} </if> \r\n" + 
			" <if test='priceRuleId != null'>, price_rule_id = #{priceRuleId} </if> \r\n" + 
			" <if test='discountId != null'>, discount_id = #{discountId} </if>  "
		  	+ " WHERE  customer_no = #{customerNo} "
		  	+ " <if test='discountCode != null'> and discount_code = #{discountCode} </if> "
			+ " <if test='discountCodeIn != null'> and discount_code in (${discountCodeIn}) </if> "

			, "</script>" })
	public int updateCreditMemo(HashMap<String, Object> paramMap);

 
	
	@Delete({ "<script>", " DELETE FROM cms_db.credit_memo ",
		" WHERE customer_no  = #{customerNo}  "
		+ " and discount_code = #{discountCode}  ",
  	  	 "</script>" })
	public int deleteCreditMemo(HashMap<String, Object> paramMap);
	
}