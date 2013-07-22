package com.arthur.shoppingmall;

import java.util.List;
import java.util.Map;

import com.arthur.shoppingmall.view.OrderDto;
import com.arthur.shoppingmall.view.OrderLineitemDto;
import com.arthur.shoppingmall.view.ProductDto;
import com.arthur.shoppingmall.view.VendorDto;

public interface IOrderSrv {
	
	public List<VendorDto> listVendors();
	public List<ProductDto> listProducts(Integer vendorId);
	public void order(Map<String,Object> orderInfo , int tenantId);
	public List<OrderDto> listOrders();
	public List<OrderLineitemDto> listOrderLineitems(String orderId);
	public List<Integer> doOrderTenantIds(List<Map<String,Object>> ols);
}
