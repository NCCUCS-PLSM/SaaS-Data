package com.arthur.shoppingmall;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import com.arthur.mta.core.CustomObject;
import com.arthur.shoppingmall.boundary.OrderLineitemRepo;
import com.arthur.shoppingmall.boundary.OrderRepo;
import com.arthur.shoppingmall.boundary.ProductRepo;
import com.arthur.shoppingmall.boundary.VendorRepo;
import com.arthur.shoppingmall.domain.Order;
import com.arthur.shoppingmall.domain.OrderLineitem;
import com.arthur.shoppingmall.domain.Product;
import com.arthur.shoppingmall.view.OrderDto;
import com.arthur.shoppingmall.view.OrderLineitemDto;
import com.arthur.shoppingmall.view.ProductDto;
import com.arthur.shoppingmall.view.VendorDto;

public class OrderSrv implements IOrderSrv{

	public List<VendorDto> listVendors() {
	
		VendorRepo repo = new VendorRepo();
		return repo.getVenodrs();

	}

	public List<ProductDto> listProducts(Integer vendorId) {
		
		ProductRepo repo = new ProductRepo();
		List<Product> ps = repo.getProducts();
		List<ProductDto> pDtos = new ArrayList<ProductDto>();
		for (Product p : ps) {
			CustomObject co = (CustomObject)p;
			ProductDto pDto = new ProductDto(co.getId() , p.getProductId() , p.getProductName() ,
					Double.toString(p.getUnitPrice()),p.getThumbnailImage() ,co.getCustomFields());
			pDtos.add(pDto);
		}
		
		return pDtos;
	}

	public void order(Map<String, Object> orderInfo , int tenantId) {
		
		OrderRepo oRepo = new OrderRepo();
	
		
		List<Map<String,Object>> ols = ((List<Map<String,Object>>)orderInfo.get("OrderLineitem"));
		Order o = new Order(tenantId,orderInfo.get("Customer").toString());
		o.setOrderId("o"+System.currentTimeMillis());
		int count = 0;
		for (Map<String, Object> map : ols) {
			Integer olTenantId = Integer.parseInt(map.get("tenantId").toString() );
			if(tenantId == olTenantId ){
				OrderLineitem item = new OrderLineitem(tenantId ,
						map.get("productId").toString() , map.get("qty").toString());
				item.setOrderId(o.getOrderId());
				item.setOrderLineitemId("ol" + System.currentTimeMillis()+ count);
				o.addLineitem(item);	
				count+=1;
				}
		}
		o.calculateTotalAmount();	
		oRepo.addOrder(o);

	}

	public List<OrderDto> listOrders() {
		
		OrderRepo oRepo = new OrderRepo();
		List<Order> orders = oRepo.getOrders();
		List<OrderDto> oDtos = new ArrayList<OrderDto>();
		for (Order order : orders) {
			//todo cus
		
			String oD = (new java.text.SimpleDateFormat().format(order.getOrderDate() ));
			
			oDtos.add(new OrderDto(order.getOrderId() ,order.getCustomer() , oD , order.getOrderAmount()));
		}
		
		return oDtos;
	}

	public List<OrderLineitemDto> listOrderLineitems( String orderId) {
		
		OrderLineitemRepo oRepo = new OrderLineitemRepo();
		List<OrderLineitem> orderitems = oRepo.getOrderLineitems(orderId);
		List<OrderLineitemDto> oDtos = new ArrayList<OrderLineitemDto>();
		try {
			for (OrderLineitem oi : orderitems) {
				oDtos.add(new OrderLineitemDto(oi.getOrderLineitemId() , 
						oi.getProduct().getProductName() ,Double.toString( oi.getProduct().getUnitPrice() )
						,oi.getQty() , oi.getSubTotal()) );
				
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
	
		
		return oDtos;
	}

	@Override
	public List<Integer> doOrderTenantIds(List<Map<String, Object>> ols) {
		
		List<Integer> rawTenantIds = new ArrayList<Integer>();
		for (Map<String, Object> map : ols) {
			rawTenantIds.add(Integer.parseInt(map.get("tenantId").toString()));
		}
		
		Collections.sort(rawTenantIds);
		List<Integer> tenantIds = new ArrayList<Integer>();
		for (Integer integer : rawTenantIds) {
			if(tenantIds.size() == 0){
				tenantIds.add(integer);
			}else{
				for (Integer t : tenantIds) {
					if(t != integer){
						tenantIds.add(integer);
						break;
					}
				}
			}
		}
		
		return tenantIds;
	}
	


}
