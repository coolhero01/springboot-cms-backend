
package com.oneclicktech.spring.mapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface EWTMapper {

	@Select({ "<script> select distinct ewt.customer_no as 'customerNo'  \r\n" 
			+ "	, ewt.ewt_flag as 'ewtFlag'\r\n"
			+ " , ca.store_name as 'storeName'      \r\n" 
			+ "	, c.email as 'customerEmail'\r\n"
			+ " , c.full_name as 'customerName'     \r\n"
			+ "	, ewt.goods_discount as 'goodsDiscount'\r\n"
			+ "	, ewt.services_discount as 'servicesDiscount'\r\n"
			+ "	, ewt.update_date as 'updateDate'\r\n"
			 + " FROM cms_db.ewt_customer_config ewt\r\n"
			+ " JOIN cms_db.customer_address ca on ca.customer_number = ewt.customer_no\r\n"
			+ " JOIN cms_db.customer c on c.customer_number = ewt.customer_no "
			+ " WHERE ewt.customer_no is not null  \r\n" 
			+ "    <if test='customerNo != null'> AND ewt.customer_no = #{customerNo}</if>" 
			+ "    <if test='ewtFlag != null'> AND ewt.ewt_flag = #{ewtFlag}</if>" 
		   + "order by ewt.update_date desc ",

			"</script>", "" })
	public List<Map<String, Object>> getCustomerEWTList(HashMap<String, Object> paramMap);

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

	@Select({
			"<script>select c.customer_number as 'customerNo'\r\n" + 
			" from cms_db.customer c\r\n" + 
			" where not exists (select ccw.customer_no \r\n" + 
			"	from cms_db.ewt_customer_config ccw where ccw.customer_no = c.customer_number)\r\n" + 
			" and c.oos_include = 'Y' ",

			"</script>", "" })
	public List<Map<String, Object>> getCustomerForEWTSave(HashMap<String, Object> paramMap);

	@Select({ "<script> SELECT * FROM cms_db.config_discount " + " WHERE discount_type is not null ",
			"<if test='discountType != null'> and discount_type = #{discountType} </if> \r\n" + "</script>", "" })
	public List<Map<String, Object>> getConfigDiscountList(HashMap<String, Object> paramMap);

	@Insert({ "<script>",
			" INSERT INTO cms_db.ewt_customer_config \r\n" + "(customer_no,\r\n" + "ewt_flag,\r\n" + "autopay_flag,\r\n"
					+ "goods_discount,\r\n" + "services_discount,\r\n" + "update_date)\r\n" + "VALUES\r\n"
					+ "(#{customerNo},\r\n" + "#{ewtFlag},\r\n" + "#{autopayFlag},\r\n" + "#{goodsDiscount},\r\n"
					+ "#{servicesDiscount},\r\n" + "now()) ",
			"</script>" })
	public int insertEWTCustomerConfig(HashMap<String, Object> paramMap);

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
	public int insertEWTOrderInfo(HashMap<String, Object> paramMap);

	@Update({ "<script> UPDATE cms_db.ewt_customer_config\r\n" 
			+ " SET update_date = now() \r\n"
			+ "<if test='ewtFlag != null'>, ewt_flag = #{ewtFlag}  </if> \r\n"
			+ "<if test='goodsDiscount != null'>, goods_discount = #{goodsDiscount}  </if> \r\n"
		  	+ " WHERE customer_no = #{customerNo} "

			, "</script>" })
	public int updateEWTCustomerConfig(HashMap<String, Object> paramMap);

	@Update({ "<script> UPDATE cms_db.ewt_order_info \r\n" 
			+ "SET update_date = now() \r\n"
			  + "<if test='customerNo != null'>, customer_no = #{customerNo} </if> \r\n" + 
				"<if test='goodsDiscount != null'>, goods_discount = #{goodsDiscount} </if> \r\n" + 
				"<if test='servicesDiscount != null'>, services_discount = #{servicesDiscount} </if> \r\n" + 
				"<if test='ewtFileLink != null'>, ewt_file_link = #{ewtFileLink} </if> \r\n" + 
				"<if test='orderWithDiscount != null'>, order_with_discount = #{orderWithDiscount} </if> \r\n" + 
				"<if test='shipWithDiscount != null'>, ship_with_discount = #{shipWithDiscount} </if> \r\n" + 
				"<if test='disableAcct != null'>, disable_acct = #{disableAcct} </if> "
			+ "WHERE order_name = #{orderName}  "

			, "</script>" })
	public int updateEWTOrderInfo(HashMap<String, Object> paramMap);
}