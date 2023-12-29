
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
public interface IssuanceMapper {
	
	@Select({
			"<script> SELECT  strgrd_id as 'strgrdId',\r\n" + 
			"promo_code as 'promoCode',\r\n" + 
			"customer_no as 'customerNo',\r\n" + 
			"item_no as 'itemNo',\r\n" + 
			"item_variant_id as 'itemVariantId',\r\n" + 
			"item_prod_id as 'itemProdId',\r\n" + 
			"item_qty as 'itemQty',\r\n" + 
			"date_issued as 'dateIssued',\r\n" + 
			"oos_enabled as 'oosEnabled',\r\n" + 
			"issued_flag as 'issuedFlag',\r\n" + 
			"order_name as 'orderName',\r\n" + 
			"stgrd_sched_id as 'stgrdSchedId',\r\n" + 
			"total_amt_to_pay as 'totalAmtToPay',\r\n" + 
			"total_amt_paid as 'totalAmtPaid',\r\n" + 
			"pay_terms_count as 'payTermsCount',\r\n" + 
			"interest_rate as 'interestRate',\r\n" + 
		    "update_date as 'updateDate' "
			+ " FROM cms_db.staggered_payment_mst \r\n"
			+ " WHERE strgrd_id is not null  ",
			"  <if test='promoCode != null'> and promo_code = #{promoCode}  </if>",
			"  <if test='customerNo != null'> and customer_no = #{customerNo}  </if>",
			"  <if test='orderName != null'> and order_name = #{orderName}  </if>", 
			"  ORDER BY strgrd_id asc " ,
					 
			"</script>", "" })
	public List<Map<String, Object>> getStaggeredMasterList(HashMap<String, Object> paramMap);
	
	
	@Select({
		"<script> SELECT DISTINCT pd.item_id as 'itemId'\r\n" + 
		"  , pd.name as 'itemName' \r\n" + 
		"  , pi.warehouse as 'warehouse' \r\n" + 
		"  , w.name as 'warehouseName' \r\n" + 
		"  , concat(pd.item_id,'-',pd.name) as  'idWithName'  \r\n" + 
		"  , 0 as  'itemQty' \r\n" + 
		"FROM cms_db.product_detail pd \r\n" + 
		"JOIN cms_db.product_inventory pi on pd.item_id = pi.item_number  \r\n" + 
		"LEFT JOIN cms_db.warehouse w on w.warehouse = pi.warehouse\r\n" + 
		"WHERE  pi.shop_prod_id is not null  " +
		"  <if test='warehouse != null'> and pi.warehouse = #{warehouse}  </if>", 
		"  <if test='warehouseIn != null'> and pi.warehouse in (${warehouseIn}) </if>", 
					 
		"</script>", "" })
	public List<Map<String, Object>> getProductsByWarehouse(HashMap<String, Object> paramMap);
	
	
	@Select({
		"<script> SELECT  concat(sai.promo_code, trim(sai.customer_no)) as 'dataId'     \r\n" + 
		"		 	  , sai.promo_code as 'promoCode'    \r\n" + 
		"		 		 , pil.promo_name as 'promoName'       \r\n" + 
		"		 		 , sai.customer_no as 'customerNo'    \r\n" + 
		"		 		 , sai.order_name as 'orderName'    \r\n" + 
		"				 , c.oos_warehouse_1 as 'warehouseCode'   \r\n" + 
		"		 		 , wh1.name as 'warehouse1'   \r\n" + 
		"		 		 , wh2.name as 'warehouse2'   \r\n" + 
		"		 		 , ca.store_name as 'storeName'  \r\n" + 
		"                , count(sai.item_id) as 'itemCount'    \r\n" + 
		"		 		, DATE_FORMAT(pil.effect_start_date, '%m/%d/%Y')  as 'effectStartDate'    \r\n" + 
		"		 		, DATE_FORMAT(pil.effect_end_date, '%m/%d/%Y') as 'effectEndDate'    \r\n" + 
		"		 		, DATE_FORMAT(sai.issued_date, '%m/%d/%Y') as 'issuedDate'    \r\n" + 
		"		 		, sai.issued_flag as 'issuedFlag'    \r\n" + 
		"		 	    , sai.oos_enabled as 'oosEnabled'     \r\n" + 
		"		 		, max(sai.update_date) as 'updateDate'    \r\n" + 
		"	    FROM cms_db.store_auto_issuance sai        \r\n" + 
		"		JOIN cms_db.promo_issuance_lkp pil on pil.promo_code = sai.promo_code  \r\n" + 
		"		JOIN cms_db.customer_address ca on ca.customer_number = sai.customer_no \r\n" + 
		"		JOIN cms_db.customer c  on c.customer_number = sai.customer_no  \r\n" + 
		"		JOIN cms_db.product_detail pd on pd.item_id = sai.item_id " + 
		  "     LEFT JOIN cms_db.warehouse wh1 on wh1.warehouse = c.oos_warehouse_1\r\n" + 
		"	    LEFT JOIN cms_db.warehouse wh2 on wh2.warehouse = c.oos_warehouse_2\r\n" + 
		"	 WHERE sai.promo_code is not null  ",
		"  <if test='promoCode != null'> and sai.promo_code = #{promoCode}  </if>",
		"  <if test='customerNo != null'> and sai.promo_code = #{customerNo}  </if>",
		"  <if test='orderName != null'> and sai.order_name = #{orderName}  </if>", 
		" GROUP BY dataId, promoCode, promoName, warehouseCode, customerNo, orderName,     \r\n" + 
		"		 	 storeName, effectStartDate, effectEndDate, issuedDate, issuedFlag,  oosEnabled,\r\n" + 
		"             warehouse1, warehouse2 " ,
				 
		"</script>", "" })
	public List<Map<String, Object>> getNPDIssuanceList(HashMap<String, Object> paramMap);

 
	@Select({
		"<script> SELECT concat(spm.promo_code, spm.customer_no) as 'dataId',   \r\n" + 
		"		spm.promo_code as 'promoCode',   \r\n" + 
		"		pil.promo_name as 'promoName',    \r\n" + 
		"		spm.customer_no as 'customerNo',   \r\n" + 
		"		ca.store_name as 'storeName',   \r\n" + 
		"		count(distinct spm.item_no) as 'itemCount',   \r\n" + 
		"		 wh1.name as 'warehouse1' ,     \r\n" + 
		"		 wh2.name as 'warehouse2' ,      \r\n" + 
		"		spm.date_issued as 'dateIssued',   \r\n" + 
		"		DATE_FORMAT(spm.date_issued, '%m/%d/%Y')as 'dateIssuedStr',   \r\n" + 
		"	    spm.oos_enabled as 'oosEnabled',   \r\n" + 
		"		spm.issued_flag as 'issuedFlag',   \r\n" + 
		"		spm.order_name as 'orderName',   \r\n" + 
		"		so.financial_status as  'financialStatus',   \r\n" + 
		"		spm.pay_terms_count as 'payTermsCount',   \r\n" + 
		"		spm.stgrd_sched_id as 'stgrdSchedId',   \r\n" + 
		"		spm.final_pay_status as 'finalPayStatus',   \r\n" + 
		"		count(distinct sps.stgrd_pay_id) as 'schedCount',\r\n" + 
		"		spm.interest_rate as 'interestRate',   \r\n" + 
		"	    DATE_FORMAT(pil.effect_start_date, '%m/%d/%Y')  as 'effectStartDate',   \r\n" + 
		"		DATE_FORMAT(pil.effect_end_date, '%m/%d/%Y') as 'effectEndDate',   \r\n" + 
		"		max(spm.update_date) as 'updateDate'    \r\n" + 
		" 		FROM cms_db.staggered_payment_mst spm   \r\n" + 
		"			JOIN cms_db.promo_issuance_lkp pil on pil.promo_code = spm.promo_code    \r\n" + 
		"			JOIN cms_db.customer_address ca on ca.customer_number = spm.customer_no   \r\n" + 
		"			JOIN cms_db.customer c  on c.customer_number = spm.customer_no    \r\n" + 
		"			JOIN cms_db.product_detail pd on pd.item_id = spm.item_no   \r\n" + 
		"		    LEFT JOIN cms_db.warehouse wh1 on wh1.warehouse = c.oos_warehouse_1    \r\n" + 
		"			LEFT JOIN cms_db.warehouse wh2 on wh2.warehouse = c.oos_warehouse_2    \r\n" + 
		"           LEFT JOIN cms_db.shop_order so on so.order_name = spm.order_name\r\n" + 
		"           LEFT JOIN cms_db.staggered_payment_sched sps  on sps.stgrd_sched_id = spm.stgrd_sched_id \r\n" + 
		"		WHERE spm.promo_code is not null " + 
		" <if test='promoCode != null'> and spm.promo_code = #{promoCode}  </if> " + 
		" <if test='customerNo != null'> and spm.customer_no  = #{customerNo}  </if> " + 
		" <if test='orderName != null'> and spm.order_name = #{orderName}  </if>" + 
			
		" GROUP BY promoCode, promoName, customerNo, stgrdSchedId,\r\n" + 
			" dateIssued, oosEnabled , issuedFlag, orderName,\r\n" + 
			" effectStartDate, effectEndDate, payTermsCount, dateIssuedStr,  " +
			" warehouse1, warehouse2 , finalPayStatus, interestRate , financialStatus \r\n" +  
				 
		"</script>", "" })      
	public List<Map<String, Object>> getStaggeredPayDisplay(HashMap<String, Object> paramMap);
	
	
	@Select({
		"<script> SELECT concat(spm.promo_code, spm.customer_no) as 'dataId',\r\n" + 
		"	spm.promo_code as 'promoCode',\r\n" + 
		"	pil.promo_name as 'promoName', \r\n" + 
		"	spm.customer_no as 'customerNo',\r\n" + 
		"	ca.store_name as 'storeName',\r\n" + 
		"	spm.item_no  as 'itemNo',  \r\n" + 
		"	spm.item_variant_id  as 'itemVariantId',  \r\n" + 
		"	spm.item_prod_id  as 'itemProdId',  \r\n" + 
		"	spm.item_qty  as 'itemQty',  \r\n" + 
		"	pd.name as 'itemName',  \r\n" + 
	    "	spm.date_issued as 'dateIssued',\r\n" + 
		"	spm.oos_enabled as 'oosEnabled',\r\n" + 
		"	spm.issued_flag as 'issuedFlag',\r\n" + 
		"	spm.order_name as 'orderName',\r\n" + 
		"	spm.stgrd_sched_id as 'stgrdSchedId',\r\n" + 
		"	spm.pay_terms_count as 'payTermsCount',\r\n" + 
		"	spm.interest_rate as 'interestRate',\r\n" + 
  	    "	DATE_FORMAT(pil.effect_start_date, '%m/%d/%Y')  as 'effectStartDate',\r\n" + 
		"	DATE_FORMAT(pil.effect_end_date, '%m/%d/%Y') as 'effectEndDate' \r\n" + 
		" FROM cms_db.staggered_payment_mst spm\r\n" + 
		" JOIN cms_db.promo_issuance_lkp pil on pil.promo_code = spm.promo_code \r\n" + 
		" JOIN cms_db.customer_address ca on ca.customer_number = spm.customer_no\r\n" + 
		" JOIN cms_db.customer c  on c.customer_number = spm.customer_no \r\n" + 
		" JOIN cms_db.product_detail pd on pd.item_id = spm.item_no " + 
		" WHERE spm.promo_code is not null\r\n" + 
		" <if test='promoCode != null'> and spm.promo_code = #{promoCode}  </if> " + 
		" <if test='customerNo != null'> and spm.customer_no  = #{customerNo}  </if> " + 
		" <if test='orderName != null'> and spm.order_name = #{orderName}  </if>" + 
  				 
		"</script>", "" })      
	public List<Map<String, Object>> getStaggeredPayDetailList(HashMap<String, Object> paramMap);
	
	
	@Select({
		"<script> SELECT DISTINCT spc.stgrd_pay_id as 'stgrdPayId',\r\n" + 
		"		spc.stgrd_sched_id as 'stgrdSchedId',\r\n" + 
		"	    DATE(spc.sched_pay_date)  as 'schedPayDate',\r\n" + 
		"	    date_format(DATE(spc.sched_pay_date),'%m/%d/%Y')  as 'schedPayDateStr',\r\n" + 
		"		DATE(spc.actual_pay_date) as 'actualPayDate',\r\n" + 
		"		date_format(DATE(spc.actual_pay_date) ,'%m/%d/%Y')as 'actualPayDateStr',\r\n" + 
		"		spc.amount_to_pay as 'amountToPay',\r\n" + 
		"		spc.amount_paid as 'amountPaid',\r\n" + 
	 	"       spm.order_name as 'orderName', \r\n" + 
		"       spc.pay_status as 'payStatus', \r\n" + 
	    "		DATE(spc.update_date) as 'updateDate'  \r\n" + 
		" FROM cms_db.staggered_payment_sched spc\r\n" + 
		" JOIN cms_db.staggered_payment_mst spm on spc.stgrd_sched_id = spm.stgrd_sched_id\r\n" + 
  
	    " WHERE spc.stgrd_pay_id is not null " ,
	    "<if test='stgrdSchedId != null'> and spc.stgrd_sched_id = #{stgrdSchedId} </if> \r\n" + 		 
	    "<if test='orderName != null'> and spm.order_name = #{orderName} </if> \r\n" + 		 
	    " ORDER by spc.stgrd_pay_id, spc.stgrd_sched_id  "	+			 
	 	"</script>", "" })
	public List<Map<String, Object>>  getStaggeredSchedList(HashMap<String, Object> paramMap);
	
	
	@Select({
		"<script> SELECT DISTINCT spc.stgrd_pay_id as 'stgrdPayId',\r\n" + 
		"		spc.stgrd_sched_id as 'stgrdSchedId',\r\n" + 
		"	    DATE(spc.sched_pay_date)  as 'schedPayDate',\r\n" + 
		"	    date_format(DATE(spc.sched_pay_date),'%m/%d/%Y')  as 'schedPayDateStr',\r\n" + 
		"		DATE(spc.actual_pay_date) as 'actualPayDate',\r\n" + 
		"		date_format(DATE(spc.actual_pay_date) ,'%m/%d/%Y')as 'actualPayDateStr',\r\n" + 
		"		spc.amount_to_pay as 'amountToPay',\r\n" + 
		"		spc.amount_paid as 'amountPaid',\r\n" + 
	 	"       spm.order_name as 'orderName', \r\n" + 
		"       spc.pay_status as 'payStatus', \r\n" + 
	    "		DATE(spc.update_date) as 'updateDate'  \r\n" + 
		" FROM cms_db.staggered_payment_sched spc\r\n" + 
		" JOIN cms_db.staggered_payment_mst spm on spc.stgrd_sched_id = spm.stgrd_sched_id\r\n" + 
  
	    " WHERE spc.stgrd_pay_id is not null " ,
	    "<if test='stgrdSchedId != null'> and spc.stgrd_sched_id = #{stgrdSchedId} </if> \r\n" + 		 
	    "<if test='orderName != null'> and spm.order_name = #{orderName} </if> \r\n" + 		 
	    " ORDER by spc.stgrd_pay_id, spc.stgrd_sched_id, spc.sched_pay_date  "	+			 
	 	"</script>", "" })
	public List<Map<String, Object>> getPaymentScheduleList(HashMap<String, Object> paramMap);
	
	
	@Select({
		"<script>   select min(sps.stgrd_pay_id) as 'rowSchedId'   \r\n" + 
		"			 , sps.stgrd_sched_id as  'stgrdSchedId'   \r\n" + 
		"			 , sps.amount_to_pay  as 'amountToPay'   \r\n" + 
		"			 , min(sps.sched_pay_date) as 'nextPayDate'   \r\n" + 
		"			 , sps.pay_status as 'payStatus'    \r\n" + 
		"			 , spm.customer_no as 'customerNo'    \r\n" + 
		"		    , (select sum(amount_to_pay) from cms_db.staggered_payment_sched  \r\n" + 
		"		       where stgrd_sched_id =  sps.stgrd_sched_id) as 'totalAmountToPay'  \r\n" + 
		"		 from cms_db.staggered_payment_sched sps   \r\n" + 
		"		 join cms_db.staggered_payment_mst spm on sps.stgrd_sched_id = spm.stgrd_sched_id " + 
		" where spm.order_name = #{orderName} \r\n" +  
		"   and sps.pay_status = #{payStatus} \r\n" + 
	    " <if test='paymentDate != null'> and DATE_SUB(sps.sched_pay_date, INTERVAL 1 DAY) &lt;= STR_TO_DATE(#{paymentDate}, '%m/%d/%Y') </if> \r\n" + 		
		"  group by sps.stgrd_sched_id,  sps.pay_status, spm.customer_no , sps.amount_to_pay \r\n" + 
		"  limit 1 "	+			 
	 	"</script>", "" })
	public  Map<String, Object> getNextPaymentSched(HashMap<String, Object> paramMap);
	
	@Select({
		"<script> select min(sps.stgrd_pay_id) as 'rowSchedId' \r\n" + 
			" , sps.stgrd_sched_id as  'stgrdSchedId' \r\n" + 
			" , min(sps.amount_to_pay) as 'amountToPay' \r\n" + 
			" , min(sps.sched_pay_date) as 'nextPayDate' \r\n" + 
			" , sps.pay_status as 'payStatus' \r\n" + 
			" ,  spm.order_name as 'orderName' \r\n" + 
			" , sol.so_item_no as 'itemId'\r\n" + 
			" , sol.quantity as 'itemQty'    \r\n" + 
			" , pd.name as 'itemName'  " + 
			" , (select sum(amount_to_pay) from cms_db.staggered_payment_sched\r\n" + 
			"			where stgrd_sched_id =  sps.stgrd_sched_id) as 'totalAmountToPay'" + 
		" from cms_db.staggered_payment_sched sps \r\n" + 
		" join cms_db.staggered_payment_mst spm on sps.stgrd_sched_id = spm.stgrd_sched_id \r\n" + 
		" join cms_db.shop_order_line sol on sol.order_name = spm.order_name \r\n" + 
		" join cms_db.product_detail pd on pd.item_id = sol.so_item_no    " + 
		" where spm.order_name = #{orderName} \r\n" +  
		"   and sps.pay_status = #{payStatus} \r\n" + 
	    " <if test='paymentDate != null'> and DATE_SUB(sps.sched_pay_date, INTERVAL 1 DAY) &lt;= STR_TO_DATE(#{paymentDate}, '%m/%d/%Y') </if> \r\n" + 		
		" group by sps.stgrd_sched_id,   sps.pay_status, \r\n" + 
		" sol.so_item_no  , pd.name, sol.quantity,  spm.order_name  "	+			 
	 	"</script>", "" })
	public  List<Map<String, Object>>  getNextPaymentDetails(HashMap<String, Object> paramMap);
	
	
	@Select({
		"<script> select  spm.interest_rate  as 'interestRate' \r\n" + 
		"     from cms_db.staggered_payment_mst spm \r\n" + 
		"     join cms_db.shop_order so on so.order_name = spm.order_name \r\n" + 
		"     where spm.order_name = #{orderName} limit 1 "	+			 
	 	"</script>", "" })
	public Map<String, Object>  getStaggeredInterestRate(String orderName);
	
	
	@Select({
		"<script> SELECT stgrd_pay_id as 'stgrdPayId',\r\n" + 
				"stgrd_sched_id as 'stgrdSchedId',\r\n" + 
				"sched_pay_date as 'schedPayDate',\r\n" + 
				"actual_pay_date as 'actualPayDate',\r\n" + 
				"amount_to_pay as 'amountToPay',\r\n" + 
				"amount_paid as 'amountPaid',\r\n" + 
				"update_date as 'updateDate  \r\n" + 
			" FROM cms_db.staggered_payment_sched\r\n" + 
			" WHERE stgrd_pay_id is not null " ,
			"<if test='stgrdSchedId != null'> and stgrd_sched_id = #{stgrdSchedId} </if> \r\n" + 		 
			 
	 	"</script>", "" })
	public List<Map<String, Object>>  getStaggeredSchedByOrder(HashMap<String, Object> paramMap);
	
	@Select({
		"<script> SELECT stgrd_pay_id as 'stgrdPayId',\r\n" + 
				"stgrd_sched_id as 'stgrdSchedId',\r\n" + 
				"sched_pay_date as 'schedPayDate',\r\n" + 
				"actual_pay_date as 'actualPayDate',\r\n" + 
				"amount_to_pay as 'amountToPay',\r\n" + 
				"amount_paid as 'amountPaid',\r\n" + 
				"update_date as 'updateDate  \r\n" + 
			" FROM cms_db.staggered_payment_sched\r\n" + 
			" WHERE stgrd_pay_id is not null " ,
			"<if test='stgrdSchedId != null'> and stgrd_sched_id = #{stgrdSchedId} </if> \r\n" + 		 
			 
	 	"</script>", "" })
	public  List<Map<String, Object>> getProductsForNPDIssuance(HashMap<String, Object> paramMap);
	
	
	@Select({
		"<script> SELECT DISTINCT spm.item_no as 'itemNo',\r\n" + 
		"	    pd.name as 'itemName',	 \r\n" + 
		"	    spm.item_qty as 'itemQty'\r\n" +  
		" FROM cms_db.staggered_payment_mst spm \r\n" + 
		" JOIN cms_db.product_detail pd on spm.item_no = pd.item_id\r\n" +  
		" WHERE spm.item_no is not null" ,
	    "<if test='promoCode != null'> and spm.promo_code = #{promoCode} </if> \r\n" + 		 
	    "<if test='customerNo != null'> and spm.customer_no = #{customerNo} </if> \r\n" + 		 
	    				 
	 	"</script>", "" })
	public  List<Map<String, Object>> getProductsForStaggeredIssuance(HashMap<String, Object> paramMap);
	
	@Select({
		"<script> SELECT  spm.promo_code as 'promoCode'\r\n" + 
			"	 , spm.customer_no as 'customerNo'\r\n" + 
			"	 , spm.stgrd_sched_id as 'stgrdSchedId'\r\n" + 
			"   , max(so.db_create_date) as 'creationDate'\r\n " +
		  "     , max(spm.interest_rate) as 'interestRate' " + 
		  "     , count(spm.item_no) as 'itemCount'  " + 
	 	" FROM cms_db.shop_order_line sol\r\n" + 
		" JOIN  cms_db.staggered_payment_mst spm on sol.so_item_no = spm.item_no\r\n" + 
		"	and sol.quantity = spm.item_qty\r\n" + 
		" JOIN cms_db.shop_order so on sol.order_name = so.order_name\r\n" + 
		"  and so.so_customer_no = spm.customer_no 	\r\n" + 
		" WHERE sol.order_name = #{orderName}	\r\n" + 
		"  and spm.oos_enabled = 'Y' \r\n" + 
		"  and spm.issued_flag = 'N'\r\n" + 
		" GROUP BY  spm.promo_code , spm.customer_no, spm.stgrd_sched_id " + 
	 	  		 
	 	"</script>", "" })
	public  List<Map<String, Object>> getStaggeredIssuanceListByOrder(HashMap<String, Object> paramMap);
	 
	
	@Select({
		"<script> SELECT  sai.promo_code as 'promoCode'\r\n" + 
		"	 , sai.customer_no as 'customerNo'\r\n" + 
		"	 , sai.warehouse_code as  'warehouse' \r\n" + 
		"	 , max(so.db_create_date) as 'creationDate'\r\n" + 
		"    , count(sai.item_id ) as 'itemCount'  \r\n" + 
		" FROM cms_db.shop_order_line sol\r\n" + 
		" JOIN cms_db.store_auto_issuance sai on sol.so_item_no = sai.item_id\r\n" + 
	 	"  and sol.quantity = sai.qty_issuance\r\n" + 
		" JOIN cms_db.shop_order so on sol.order_name = so.order_name\r\n" + 
		"    and so.so_customer_no = sai.customer_no " + 
		" WHERE sol.order_name = #{orderName}	\r\n" + 
		"  and sai.oos_enabled = 'Y' \r\n" + 
		"  and sai.issued_flag = 'N' \r\n" + 
		" GROUP BY  sai.promo_code , sai.customer_no , sai.warehouse_code  \r\n" + 
 	  
	 	"</script>", "" })
	public  List<Map<String, Object>> getAutoIssuanceListByOrder(HashMap<String, Object> paramMap);
	
	@Insert({ "<script>",
		" INSERT INTO cms_db.staggered_payment_mst\r\n" + 
		"( promo_code,\r\n" + 
		"customer_no,\r\n" + 
		"item_no,\r\n" + 
		"item_variant_id,\r\n" + 
		"item_prod_id,\r\n" + 
		"item_qty,\r\n" + 
		"date_issued,\r\n" + 
		"oos_enabled,\r\n" + 
		"issued_flag,\r\n" + 
		"order_name,\r\n" + 
		"stgrd_sched_id,\r\n" + 
		"total_amt_to_pay,\r\n" + 
		"total_amt_paid,\r\n" + 
		"pay_terms_count,\r\n" + 
		"interest_rate,\r\n" + 
	    "update_date)\r\n" + 
		"VALUES\r\n" + 
		"(#{promoCode},\r\n" + 
		"#{customerNo},\r\n" + 
		"#{itemNo},\r\n" + 
		"#{itemVariantId},\r\n" + 
		"#{itemProdId},\r\n" + 
		"#{itemQty},\r\n" + 
		"#{dateIssued},\r\n" + 
		"#{oosEnabled},\r\n" + 
		"#{issuedFlag},\r\n" + 
		"#{orderName},\r\n" + 
		"#{stgrdSchedId},\r\n" + 
		"#{totalAmtToPay},\r\n" + 
		"#{totalAmtPaid},\r\n" + 
		"#{payTermsCount},\r\n" + 
		"#{interestRate},\r\n" + 
	    "now()) ",
	"</script>" })
    public int insertStaggeredIssuanceMaster(HashMap<String, Object> paramMap);
	
	
	@Insert({ "<script>",
		" INSERT INTO cms_db.staggered_payment_sched\r\n" + 
		"( stgrd_sched_id,\r\n" + 
		"sched_pay_date,\r\n" + 
		"actual_pay_date,\r\n" + 
		"amount_to_pay,\r\n" + 
		"amount_paid,\r\n" + 
		"pay_status,\r\n" + 
		"update_date)\r\n" + 
		"VALUES\r\n" + 
		"(#{stgrdSchedId},\r\n" + 
		"#{schedPayDate},\r\n" + 
		"#{actualPayDate},\r\n" + 
		"#{amountToPay},\r\n" + 
		"#{amountPaid},\r\n" + 
		"#{payStatus},\r\n" + 
		  "now()) ",
	"</script>" })
    public int insertStaggeredIssuanceSched(HashMap<String, Object> paramMap);
	
	
	@Update({ "<script> UPDATE cms_db.staggered_payment_mst \r\n" + 
			"SET update_date = now() \r\n" +   
			"<if test='itemNo != null'>, item_no = #{itemNo} </if> \r\n" + 
			"<if test='itemVariantId != null'>, item_variant_id = #{itemVariantId} </if> \r\n" + 
			"<if test='itemProdId != null'>, item_prod_id = #{itemProdId} </if> \r\n" + 
			"<if test='itemQty != null'>, item_qty = #{itemQty} </if> \r\n" + 
			"<if test='dateIssued != null'>, date_issued = #{dateIssued} </if> \r\n" + 
			"<if test='oosEnabled != null'>, oos_enabled = #{oosEnabled} </if> \r\n" + 
			"<if test='issuedFlag != null'>, issued_flag = #{issuedFlag} </if> \r\n" + 
			"<if test='orderName != null'>, order_name = #{orderName} </if> \r\n" + 
			"<if test='stgrdSchedId != null'>, stgrd_sched_id = #{stgrdSchedId} </if> \r\n" + 
			"<if test='totalAmtToPay != null'>, total_amt_to_pay = #{totalAmtToPay} </if> \r\n" + 
			"<if test='totalAmtPaid != null'>, total_amt_paid = #{totalAmtPaid} </if> " + 
			"<if test='payTermsCount != null'>, pay_terms_count = #{payTermsCount} </if> " + 
			"<if test='finalPayStatus != null'>, final_pay_status = #{finalPayStatus} </if> " + 
			"<if test='interestRate != null'>, interest_rate = #{interestRate} </if> " + 
					 " WHERE promo_code is not null " + 
			 "<if test='promoCode != null'> and promo_code = #{promoCode} </if> \r\n" + 
			 "<if test='customerNo != null'> and customer_no = #{customerNo} </if> \r\n" + 
			 "<if test='stgrdSchedId != null'> and stgrd_sched_id = #{stgrdSchedId} </if> \r\n" 
			  			 		
	 	  , "</script>" })
    public int updateStaggeredIssuanceMaster(HashMap<String, Object> paramMap);
	
	
	@Update({ "<script> UPDATE cms_db.staggered_payment_sched\r\n" + 
			" SET update_date = now() \r\n" + 
	 		"<if test='schedPayDate != null'>, sched_pay_date = #{schedPayDate} </if> \r\n" + 
			"<if test='actualPayDate != null'>, actual_pay_date = #{actualPayDate} </if> \r\n" + 
			"<if test='amountToPay != null'>, amount_to_pay = #{amountToPay} </if> \r\n" + 
			"<if test='amountPaid != null'>, amount_paid = #{amountPaid} </if> \r\n" + 
			"<if test='payStatus != null'>, pay_status = #{payStatus} </if>  " + 
			  " WHERE stgrd_pay_id is not null " + 
			  "<if test='stgrdPayId != null'> and stgrd_pay_id = #{stgrdPayId} </if>  " + 
			  "<if test='stgrdSchedId != null'> and stgrd_sched_id = #{stgrdSchedId} </if> \r\n" 
	 	  , "</script>" })
    public int updateStaggeredIssuanceSched(HashMap<String, Object> paramMap);
	
	 
	@Update({ "<script> UPDATE cms_db.promo_issuance_lkp \r\n" + 
			" SET update_date = now() \r\n" + 
	 		"<if test='promoName != null'>, promo_name = #{promoName} </if> \r\n" + 
			"<if test='effectStartDate != null'>, effect_start_date = #{effectStartDate} </if> \r\n" + 
			"<if test='effectEndDate != null'>, effect_end_date = #{effectEndDate} </if> \r\n" +  
			  " WHERE promo_code is not null " + 
			  "<if test='promoCode != null'> and promo_code = #{promoCode} </if>  " 
	 	  , "</script>" })
    public int updatePromoIssuance(HashMap<String, Object> paramMap);
	
	@Select({
		"<script> select promo_code \r\n" + 
		" FROM cms_db.${tableName} " +  
		" WHERE promo_code = #{promoCode} and customer_no = #{customerNo} " +
		" LIMIT 1" ,	 
	   	"</script>", "" })
	public Map<String, Object> getOnePromoDataByCustomer(String tableName, 
			String promoCode, String customerNo);
		
	
	@Delete({ "<script>", " DELETE FROM cms_db.staggered_payment_mst ",
		" WHERE  promo_code=#{promoCode} and customer_no=#{customerNo} ",
		 "</script>" })
	public int deleteStaggeredIssuanceMaster(HashMap<String, Object> paramMap);
	
	@Delete({ "<script>", " DELETE FROM cms_db.staggered_payment_sched ",
		" WHERE stgrd_pay_id is not null  ",
		"   <if test='stgrdSchedId != null'> and stgrd_sched_id = #{stgrdSchedId} </if>",
	  	 "</script>" })
	public int deleteStaggeredIssuanceSched(HashMap<String, Object> paramMap);
	
	
	@Delete({ "<script>", " DELETE FROM cms_db.store_auto_issuance ",
		" WHERE promo_code=#{promoCode} and customer_no=#{customerNo}   ",
  	  	 "</script>" })
	public int deleteNPDIssuance(HashMap<String, Object> paramMap);
	
}