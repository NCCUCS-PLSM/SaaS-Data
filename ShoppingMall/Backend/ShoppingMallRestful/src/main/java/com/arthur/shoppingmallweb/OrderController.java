package com.arthur.shoppingmallweb;

import java.util.List;

import javax.annotation.Resource;


import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.web.bind.annotation.ResponseBody;

import com.arthur.shoppingmall.IOrderSrv;
import com.arthur.shoppingmall.OrderSrv;
import com.arthur.shoppingmall.domain.User;
import com.arthur.shoppingmall.view.OrderDto;
import com.arthur.shoppingmall.view.OrderLineitemDto;

@Controller
@RequestMapping(value = "/backend/order")
public class OrderController {

	
	@RequestMapping(value = "", method = RequestMethod.GET)
	public String order( Model model) {
		model.addAttribute("userName", "" );
		return "order";
	}
	
	
	@RequestMapping(value="/olist", method=RequestMethod.GET)
	public @ResponseBody List<OrderDto> listOrders() {

		IOrderSrv srv = new OrderSrv();
		return srv.listOrders();

	}
	
	@RequestMapping(value="/oilist", method=RequestMethod.GET)
	public @ResponseBody List<OrderLineitemDto> listOrderLineitems(@RequestParam("oid") String oid) {
			
		IOrderSrv srv = new OrderSrv();
		return srv.listOrderLineitems(oid);

	}
	
	

}
