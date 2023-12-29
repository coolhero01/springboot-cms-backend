package com.oneclicktech.spring.mapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ShopOrderMapper {
	@Select({ "<script> SELECT DISTINCT so.shop_order_id as 'shopOrderId',\r\n" + 
			"		 so.order_name as 'orderName',\r\n" + 
			"		 so.sales_order_no as 'salesOrderNo',\r\n" + 
			"		 so.so_customer_no as 'soCustomerNo',\r\n" + 
			"		 so.app_id as 'appId',\r\n" + 
			"		 so.cancel_reason as 'cancelReason',\r\n" + 
			"		 so.cancelled_at as 'cancelledAt',\r\n" + 
			"		 so.contact_email as 'contactEmail',\r\n" + 
			"		 so.created_at as 'createdAt',\r\n" + 
			"		 so.current_subtotal_price as 'currentSubtotalPrice',\r\n" + 
			"		 so.current_total_price as 'currentTotalPrice',\r\n" + 
			"		 so.current_total_tax as 'currentTotalTax',\r\n" + 
			"		 so.order_number as 'orderNumber',\r\n" + 
			"		 so.processed_at as 'processedAt',\r\n" + 
			"		 so.order_status_url as 'orderStatusUrl',\r\n" + 
			"		 so.subtotal_price as 'subtotalPrice',\r\n" + 
			"		 so.tags as 'tags',\r\n" + 
			"		 so.total_discounts as 'totalDiscounts',\r\n" + 
			"		 so.total_line_items_price as 'totalLineItemsPrice',\r\n" + 
			"		 so.total_tax as 'totalTax',\r\n" + 
			"		 so.currency as 'currency',\r\n" + 
			"		 so.financial_status as 'financialStatus',\r\n" + 
			"		 so.fulfillment_status as 'fulfillmentStatus',\r\n" + 
			"		 so.customer_id as 'customerId',\r\n" + 
			"		 so.default_address_id as 'defaultAddressId',\r\n" + 
			"		 so.updated_at as 'updatedAt',\r\n" + 
			"		 so.request_delivery_date as 'requestDeliveryDate',\r\n" + 
			"		 so.so_fulfillment_status as 'soFulfillmentStatus',\r\n" + 
			"		 so.so_fulfillment_process_date as 'soFulfillmentProcessDate',\r\n" + 
			"		 so.so_sync_to_online as 'soSyncToOnline',\r\n" + 
			"		 so.so_create_date as 'soCreateDate',\r\n" + 
			"		 so.payment_date as 'paymentDate',\r\n" + 
			"		 so.po_number as 'poNumber',\r\n" + 
			"		 so.payee_bank as 'payeeBank',\r\n" + 
			"		 so.pay_journal_no as 'payJournalNo',\r\n" + 
			"		 so.db_create_date as 'dbCreateDate',\r\n" + 
			"		 so.db_update_date as 'dbUpdateDate',\r\n" +  
			" sol.shop_line_id as 'shopLineId',\r\n" +   
			" sol.so_item_no as 'soItemNo',\r\n" + 
			" sol.so_warehouse_code as 'soWarehouseCode',\r\n" + 
			" sol.so_warehouse_site as 'soWarehouseSite',\r\n" + 
			" sol.so_uom as 'soUom',\r\n" + 
			" sol.so_item_group as 'soItemGroup',\r\n" + 
			" sol.variant_id as 'variantId',\r\n" + 
			" sol.sku as 'sku',\r\n" + 
			" sol.fulfillable_quantity as 'fulfillableQuantity',\r\n" + 
			" sol.fulfillment_service as 'fulfillmentService',\r\n" + 
			" sol.fulfillment_status as 'fulfillmentStatus',\r\n" + 
			" sol.name as 'name',\r\n" + 
			" sol.price as 'price',\r\n" + 
			" sol.product_id as 'productId',\r\n" + 
			" sol.quantity as 'quantity',\r\n" + 
			" sol.title as 'title',\r\n" + 
			" sol.vendor as 'vendor',\r\n" + 
			" sol.requires_shipping as 'requiresShipping'\r\n" + 
			" FROM cms_db.shop_order so\r\n" + 
			" JOIN cms_db.shop_order_line sol on sol.order_name = so.order_name\r\n" + 
			" WHERE so.shop_order_id is not null  ",
			"  <if test='orderName != null'>    and so.order_name = #{orderName}  </if>",
			"  <if test='orderIn != null'>    and so.order_name in (${orderIn})  </if>",
			"  <if test='shopOrderId != null'>  and so.shop_order_id = #{shopOrderId}  </if>", 
			"  <if test='emptySO != null'> 	    and (so.sales_order_no is null or trim(so.sales_order_no) = '')</if>",
			"  <if test='withSO != null'> 	    and (so.sales_order_no is not null and trim(so.sales_order_no) != '')</if>",
			"  <if test='salesOrderNo != null'> and so.sales_order_no = #{salesOrderNo}  </if>",
			"  <if test='soFulfillmentStatus != null'> and so.so_fulfillment_status = #{soFulfillmentStatus}  </if>",
		    "  <if test='soSyncToOnline != null'> and so.so_sync_to_online = #{soSyncToOnline}  </if>",
			"  <if test='financialStatus != null'> and so.financial_status = #{financialStatus}  </if>",
			"  <if test='noneAutoPay != null'> and not exists (\r\n" + 
					"	select cap.email \r\n" + 
					"	from cms_db.customer_auto_pay cap \r\n" + 
					"   where cap.email = so.contact_email \r\n" + 
					") "
			+ " </if>", 
			"  <if test='poNumber != null'> and so.po_number = #{poNumber}  </if>",
		    "  <if test='dbUpdateFrom != null and dbUpdateTo != null'> and so.db_update_date BETWEEN  "
			+ " DATE_SUB(now(), INTERVAL 1 DAY) and DATE_ADD(now(), INTERVAL 1 DAY)  </if>",
	    " ORDER BY so.order_name  ",

			"</script>" }) 
	public List<Map<String, Object>> getShopOrderList(HashMap<String, Object> paramMap);
	
	
	@Select({ "<script> SELECT so.order_name as 'orderName', so.sales_order_no as 'salesOrderNo'"
			+ " , so.financial_status as 'financialStatus' "
			+ " FROM cms_db.shop_order so "
			+ " WHERE so.order_name = #{orderName} limit 1 ",
 			"</script>" }) 
	public Map<String, Object> getOneOrderData(HashMap<String, Object> paramMap);
	
	
	@Select({ "<script> SELECT DISTINCT so.shop_order_id as 'shopOrderId',\r\n" + 
			"		 so.order_name as 'orderName',\r\n" + 
			"		 so.sales_order_no as 'salesOrderNo',\r\n" + 
			"		 so.so_customer_no as 'soCustomerNo',\r\n" + 
			"		 so.app_id as 'appId',\r\n" + 
			"		 so.cancel_reason as 'cancelReason',\r\n" + 
			"		 so.cancelled_at as 'cancelledAt',\r\n" + 
			"		 so.contact_email as 'contactEmail',\r\n" + 
			"		 so.created_at as 'createdAt',\r\n" + 
			"		 so.current_subtotal_price as 'currentSubtotalPrice',\r\n" + 
			"		 so.current_total_price as 'currentTotalPrice',\r\n" + 
			"		 so.current_total_tax as 'currentTotalTax',\r\n" + 
			"		 so.order_number as 'orderNumber',\r\n" + 
			"		 so.processed_at as 'processedAt',\r\n" + 
			"		 so.order_status_url as 'orderStatusUrl',\r\n" + 
			"		 so.subtotal_price as 'subtotalPrice',\r\n" + 
			"		 so.tags as 'tags',\r\n" + 
			"		 so.total_discounts as 'totalDiscounts',\r\n" + 
			"		 so.total_line_items_price as 'totalLineItemsPrice',\r\n" + 
			"		 so.total_tax as 'totalTax',\r\n" + 
			"		 so.currency as 'currency',\r\n" + 
			"		 so.financial_status as 'financialStatus',\r\n" + 
			"		 so.fulfillment_status as 'fulfillmentStatus',\r\n" + 
			"		 so.customer_id as 'customerId',\r\n" + 
			"		 so.default_address_id as 'defaultAddressId',\r\n" + 
			"		 so.updated_at as 'updatedAt',\r\n" + 
			"		 so.request_delivery_date as 'requestDeliveryDate',\r\n" + 
			"		 so.so_fulfillment_status as 'soFulfillmentStatus',\r\n" + 
			"		 so.so_fulfillment_process_date as 'soFulfillmentProcessDate',\r\n" + 
			"		 so.so_sync_to_online as 'soSyncToOnline',\r\n" + 
			"		 so.so_create_date as 'soCreateDate',\r\n" + 
			"		 so.payment_date as 'paymentDate',\r\n" + 
			"		 so.po_number as 'poNumber',\r\n" + 
			"		 so.payee_bank as 'payeeBank',\r\n" + 
			"		 so.pay_journal_no as 'payJournalNo',\r\n" + 
			"		 so.db_create_date as 'dbCreateDate',\r\n" + 
			"		 so.db_update_date as 'dbUpdateDate' \r\n" +   
			" FROM cms_db.shop_order so \r\n" + 
		  	" WHERE so.shop_order_id is not null  ",
			"  <if test='orderName != null'>    and so.order_name = #{orderName}  </if>",
			"  <if test='orderIn != null'>    and so.order_name in (${orderIn})  </if>",
		    "  <if test='shopOrderId != null'>  and so.shop_order_id = #{shopOrderId}  </if>", 
			"  <if test='emptySO != null'> 	    and (so.sales_order_no is null or trim(so.sales_order_no) = '')</if>",
			"  <if test='withSO != null'> 	    and (so.sales_order_no is not null and trim(so.sales_order_no) != '')</if>",
			"  <if test='salesOrderNo != null'> and so.sales_order_no = #{salesOrderNo}  </if>",
			"  <if test='soFulfillmentStatus != null'> and so.so_fulfillment_status = #{soFulfillmentStatus}  </if>",
			"  <if test='notInvoiced != null'> and (so.so_fulfillment_status is null or trim(so.so_fulfillment_status) = '' or so.so_fulfillment_status not in ('Invoiced')) </if>",
		    "  <if test='soSyncToOnline != null'> and so.so_sync_to_online = #{soSyncToOnline}  </if>",
			"  <if test='fulfillStatus != null'> and so.fulfillment_status = #{fulfillStatus}  </if>",
			"  <if test='notFulfilled != null'> and (so.fulfillment_status is null or trim(so.fulfillment_status) = '' or so.fulfillment_status not in ('fulfilled'))   </if>",
			"  <if test='financialStatus != null'> and so.financial_status = #{financialStatus}  </if>",
			"  <if test='poNumber != null'> and so.po_number = #{poNumber}  </if>",
			"  <if test='payeeBank != null'> and so.payee_bank = #{payeeBank}  </if>",
			"  <if test='payJournalNo != null'> and so.pay_journal_no = #{payJournalNo}  </if>",
			"  <if test='emptyPO != null'>  and (so.po_number is null or trim(so.po_number) = '')</if>",
			"  <if test='soCustomerNoLike != null'> and so.so_customer_no like #{soCustomerNoLike}  </if>",
		    "  <if test='dbUpdateFrom != null and dbUpdateTo != null'> and so.db_update_date BETWEEN  "
			+ " DATE_SUB(now(), INTERVAL 1 DAY) and DATE_ADD(now(), INTERVAL 1 DAY)  </if>",
	    " ORDER BY so.order_name ",

			"</script>" })
	public List<Map<String, Object>> getShopOrderWithNoLines(HashMap<String, Object> paramMap);

	
	@Insert({ "<script>",
			"INSERT INTO cms_db.shop_order ( shop_order_id,\r\n" + 
			"order_name,\r\n" + 
			"sales_order_no,\r\n" + 
			"so_customer_no,\r\n" + 
			"app_id,\r\n" + 
			"cancel_reason,\r\n" + 
			"cancelled_at,\r\n" + 
			"contact_email,\r\n" + 
			"created_at,\r\n" + 
			"current_subtotal_price,\r\n" + 
			"current_total_price,\r\n" + 
			"current_total_tax,\r\n" + 
			"order_number,\r\n" + 
			"processed_at,\r\n" + 
			"order_status_url,\r\n" + 
			"subtotal_price,\r\n" + 
			"tags,\r\n" + 
			"total_discounts,\r\n" + 
			"total_line_items_price,\r\n" + 
			"total_tax,\r\n" + 
			"currency,\r\n" + 
			"financial_status,\r\n" + 
			"fulfillment_status,\r\n" + 
			"customer_id,\r\n" + 
			"default_address_id,\r\n" + 
			"updated_at,\r\n" + 
			"request_delivery_date,\r\n" + 
			"so_fulfillment_status,\r\n" + 
			"so_fulfillment_process_date,\r\n" + 
			"so_sync_to_online,\r\n" + 
			"so_create_date,\r\n" + 
				"db_create_date,\r\n" + 
			"db_update_date ) VALUES ( #{shopOrderId},\r\n" + 
			"#{orderName},\r\n" + 
			"#{salesOrderNo},\r\n" + 
			"#{soCustomerNo},\r\n" + 
			"#{appId},\r\n" + 
			"#{cancelReason},\r\n" + 
			"#{cancelledAt},\r\n" + 
			"#{contactEmail},\r\n" + 
			"#{createdAt},\r\n" + 
			"#{currentSubtotalPrice},\r\n" + 
			"#{currentTotalPrice},\r\n" + 
			"#{currentTotalTax},\r\n" + 
			"#{orderNumber},\r\n" + 
			"#{processedAt},\r\n" + 
			"#{orderStatusUrl},\r\n" + 
			"#{subtotalPrice},\r\n" + 
			"#{tags},\r\n" + 
			"#{totalDiscounts},\r\n" + 
			"#{totalLineItemsPrice},\r\n" + 
			"#{totalTax},\r\n" + 
			"#{currency},\r\n" + 
			"#{financialStatus},\r\n" + 
			"#{fulfillmentStatus},\r\n" + 
			"#{customerId},\r\n" + 
			"#{defaultAddressId},\r\n" + 
			"#{updatedAt},\r\n" + 
			"#{requestDeliveryDate},\r\n" + 
			"#{soFulfillmentProcess},\r\n" + 
			"#{soFulfillmentProcessDate},\r\n" + 
			"#{soSyncToOnline},\r\n" + 
			"#{soCreateDate},\r\n" + 
			 "now() ,\r\n" + 
			"now() ) ",
			"</script>" })
	public int insertShopOrder(HashMap<String, Object> paramMap);

	@Insert({ "<script> ",
			" INSERT INTO cms_db.shop_order_line (shop_line_id,\r\n" + 
			"order_name,\r\n" + 
			"so_item_no,\r\n" + 
		 
			"so_warehouse_code,\r\n" + 
			"so_warehouse_site,\r\n" + 
			"so_uom,\r\n" + 
			"so_item_group,\r\n" + 
			"variant_id,\r\n" + 
			"sku,\r\n" + 
			"fulfillable_quantity,\r\n" + 
			"fulfillment_service,\r\n" + 
			"fulfillment_status,\r\n" + 
			"name,\r\n" + 
			"price,\r\n" + 
			"product_id,\r\n" + 
			"quantity,\r\n" + 
			"title,\r\n" + 
			"vendor,\r\n" + 
			"requires_shipping ",
			" ) VALUES ( #{shopLineId},\r\n" + 
			"#{orderName},\r\n" + 
			"#{soItemNo},\r\n" + 
		 
			 "#{soWarehouseCode},\r\n" + 
			"#{soWarehouseSite},\r\n" + 
			"#{soUom},\r\n" + 
			"#{soItemGroup},\r\n" + 
			"#{variantId},\r\n" + 
			"#{sku},\r\n" + 
			"#{fulfillableQuantity},\r\n" + 
			"#{fulfillmentService},\r\n" + 
			"#{fulfillmentStatus},\r\n" + 
			"#{name},\r\n" + 
			"#{price},\r\n" + 
			"#{productId},\r\n" + 
			"#{quantity},\r\n" + 
			"#{title},\r\n" + 
			"#{vendor},\r\n" + 
			"#{requiresShipping} ) ",
			"</script>" })
	public int insertShopOrderLine(HashMap<String, Object> paramMap);

	@Update({ "<script> UPDATE  cms_db.shop_order  set db_update_date = now() " +
			  
			"<if test='salesOrderNo != null'>, sales_order_no = #{salesOrderNo} </if> \r\n" + 
			"<if test='soCustomerNo != null'>, so_customer_no = #{soCustomerNo} </if> \r\n" + 
			"<if test='appId != null'>, app_id = #{appId} </if> \r\n" + 
			"<if test='cancelReason != null'>, cancel_reason = #{cancelReason} </if> \r\n" + 
			"<if test='cancelledAt != null'>, cancelled_at = #{cancelledAt} </if> \r\n" + 
			"<if test='contactEmail != null'>, contact_email = #{contactEmail} </if> \r\n" + 
			"<if test='createdAt != null'>, created_at = #{createdAt} </if> \r\n" + 
			"<if test='currentSubtotalPrice != null'>, current_subtotal_price = #{currentSubtotalPrice} </if> \r\n" + 
			"<if test='currentTotalPrice != null'>, current_total_price = #{currentTotalPrice} </if> \r\n" + 
			"<if test='currentTotalTax != null'>, current_total_tax = #{currentTotalTax} </if> \r\n" + 
			"<if test='orderNumber != null'>, order_number = #{orderNumber} </if> \r\n" + 
			"<if test='processedAt != null'>, processed_at = #{processedAt} </if> \r\n" + 
			"<if test='orderStatusUrl != null'>, order_status_url = #{orderStatusUrl} </if> \r\n" + 
			"<if test='subtotalPrice != null'>, subtotal_price = #{subtotalPrice} </if> \r\n" + 
			"<if test='tags != null'>, tags = #{tags} </if> \r\n" + 
			" <if test='totalDiscounts != null'>, total_discounts = #{totalDiscounts} </if> \r\n" + 
			" <if test='totalLineItemsPrice != null'>, total_line_items_price = #{totalLineItemsPrice} </if> \r\n" + 
			" <if test='totalTax != null'>, total_tax = #{totalTax} </if> \r\n" + 
			" <if test='currency != null'>, currency = #{currency} </if> \r\n" + 
			" <if test='financialStatus != null'>, financial_status = #{financialStatus} </if> \r\n" + 
			" <if test='fulfillmentStatus != null'>, fulfillment_status = #{fulfillmentStatus} </if> \r\n" + 
			" <if test='customerId != null'>, customer_id = #{customerId} </if> \r\n" + 
			" <if test='defaultAddressId != null'>, default_address_id = #{defaultAddressId} </if> \r\n" + 
			" <if test='updatedAt != null'>, updated_at = #{updatedAt} </if> \r\n" + 
			" <if test='requestDeliveryDate != null'>, request_delivery_date = #{requestDeliveryDate} </if> \r\n" + 
			" <if test='soFulfillmentStatus != null'>, so_fulfillment_status = #{soFulfillmentStatus} </if> \r\n" + 
			" <if test='soFulfillmentProcessDate != null'>, so_fulfillment_process_date = #{soFulfillmentProcessDate} </if> \r\n" + 
			" <if test='soSyncToOnline != null'>, so_sync_to_online = #{soSyncToOnline} </if> " +  
			" <if test='soCreateDate != null'>, so_create_date = #{soCreateDate}  </if> " +  
		    " <if test='paymentDate != null'>, payment_date = #{paymentDate} </if> " +  
		    " <if test='poNumber != null'>, po_number = #{poNumber} </if> " +  
		    " <if test='payeeBank != null'>, payee_bank = #{payeeBank} </if> " +  
		    " <if test='payJournalNo != null'>, pay_journal_no = #{payJournalNo}  </if>",			
			" WHERE order_name = #{orderName}  ",
				
			"</script>" })
	public int updateShopOrder(HashMap<String, Object> paramMap);

	@Update({ "<script> UPDATE  cms_db.shop_order_line  set product_id = #{productId}  " +
			    
		 	"<if test='soWarehouseCode != null'>, so_warehouse_code = #{soWarehouseCode} </if> \r\n" + 
			"<if test='soWarehouseSite != null'>, so_warehouse_site = #{soWarehouseSite} </if> \r\n" + 
			"<if test='soUom != null'>, so_uom = #{soUom} </if> \r\n" + 
			"<if test='soItemGroup != null'>, so_item_group = #{soItemGroup} </if> \r\n" + 
			"<if test='variantId != null'>, variant_id = #{variantId} </if> \r\n" + 
			"<if test='sku != null'>, sku = #{sku} </if> \r\n" + 
			"<if test='fulfillableQuantity != null'>, fulfillable_quantity = #{fulfillableQuantity} </if> \r\n" + 
			"<if test='fulfillmentService != null'>, fulfillment_service = #{fulfillmentService} </if> \r\n" + 
			"<if test='fulfillmentStatus != null'>, fulfillment_status = #{fulfillmentStatus} </if> \r\n" + 
			"<if test='name != null'>, name = #{name} </if> \r\n" + 
			"<if test='price != null'>, price = #{price} </if> \r\n" + 
			"<if test='productId != null'>, product_id = #{productId} </if> \r\n" + 
			"<if test='quantity != null'>, quantity = #{quantity} </if> \r\n" + 
			"<if test='title != null'>, title = #{title} </if> \r\n" + 
			"<if test='vendor != null'>, vendor = #{vendor} </if> \r\n" + 
			"<if test='requiresShipping != null'>, requires_shipping = #{requiresShipping} </if> " +  
			 	" WHERE order_name = #{orderName}  " + 
				"  and  so_item_no = #{soItemNo} " , 
			
			"</script>" })
	public int updateShopOrderLine(HashMap<String, Object> paramMap);

}