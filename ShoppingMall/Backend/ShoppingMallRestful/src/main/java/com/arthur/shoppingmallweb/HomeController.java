package com.arthur.shoppingmallweb;

import java.io.IOException;
import java.net.URLDecoder;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Resource;
import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Query;
import javax.servlet.http.HttpServletRequest;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.datanucleus.api.jdo.JDOPersistenceManager;
import org.datanucleus.query.typesafe.TypesafeQuery;
import org.datanucleus.util.NucleusLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.support.RequestContext;

import com.arthur.mta.core.CustomField;
import com.arthur.mta.core.CustomObject;
import com.arthur.mta.core.FieldType;
import com.arthur.mta.core.IndexType;

import com.arthur.mta.core.context.TenantContextKeeper;
import com.arthur.mta.core.customization.CustomizationHandler;
import com.arthur.mta.javaagent.MTAJavaAgent;
import com.arthur.shoppingmall.CustomObjectSrv;
import com.arthur.shoppingmall.ICustomObjectSrv;
import com.arthur.shoppingmall.IMaintainProductSrv;
import com.arthur.shoppingmall.IOrderSrv;
import com.arthur.shoppingmall.IUserSrv;
import com.arthur.shoppingmall.MaintainProductSrv;
import com.arthur.shoppingmall.OrderSrv;
import com.arthur.shoppingmall.UserSrv;
import com.arthur.shoppingmall.boundary.UserRepo;
import com.arthur.shoppingmall.domain.Order;
import com.arthur.shoppingmall.domain.OrderLineitem;
import com.arthur.shoppingmall.domain.Product;
import com.arthur.shoppingmall.domain.User;
import com.arthur.shoppingmall.util.DataNucleusHelper;
import com.arthur.shoppingmall.util.JsonHelper;
import com.arthur.shoppingmall.view.OrderDto;
import com.arthur.shoppingmall.view.OrderLineitemDto;
import com.arthur.shoppingmall.view.ProductDto;
import com.arthur.shoppingmall.view.ProductView;
import com.arthur.shoppingmall.view.VendorDto;
import org.apache.commons.lang.StringEscapeUtils;

/**
 * Handles requests for the application home page.
 */
@Controller
public class HomeController {
	

	private static final Logger logger = LoggerFactory.getLogger(HomeController.class);
	

	
	@RequestMapping(value="/u", method=RequestMethod.GET)
	public @ResponseBody String getUser(@RequestParam("callback") String callbackName ,@RequestParam("uid") int userId) {
		
		IUserSrv srv = new UserSrv();
		return JsonHelper.toJson(callbackName, srv.getUser(userId));
		
	}
	
	@RequestMapping(value="/t", method=RequestMethod.GET)
	public @ResponseBody String getTenant(@RequestParam("callback") String callbackName ,@RequestParam("tid") int tenantId) {
		
		IUserSrv srv = new UserSrv();
		return JsonHelper.toJson(callbackName, srv.getTenant(tenantId));
		
	}
	
	@RequestMapping(value="/olist", method=RequestMethod.GET)
	public @ResponseBody String listOrders(@RequestParam("callback") String callbackName ,@RequestParam("tid") int tenantId,HttpServletRequest request ) {

		String page = request.getParameter("page");
		//System.out.println(page);
		login(tenantId);
		IOrderSrv srv = new OrderSrv();
		List<OrderDto> dtos = srv.listOrders();
		logout();
		return JsonHelper.toJson(callbackName, dtos);
		
	}
	
	@RequestMapping(value="/od", method=RequestMethod.GET)
	public @ResponseBody String listOrderLineitems(@RequestParam("callback") String callbackName ,@RequestParam("tid") int tenantId , @RequestParam("oid") String orderId) {

		login(tenantId);
		IOrderSrv srv = new OrderSrv();
		List<OrderLineitemDto> dtos = srv.listOrderLineitems(orderId);
		logout();
		return JsonHelper.toJson(callbackName, dtos);
		
	}
	
	@RequestMapping(value="/plist", method=RequestMethod.GET)
	public @ResponseBody String listProducts(@RequestParam("callback") String callbackName ,@RequestParam("tid") int tenantId) {
		login(tenantId);
		IMaintainProductSrv srv = new MaintainProductSrv();
		List<ProductDto> dtos = srv.listProducts();
		logout();
		return JsonHelper.toJson(callbackName, dtos);	
	}
	
	@RequestMapping(value="/rp", method=RequestMethod.GET)
	public @ResponseBody String retrieveProduct(@RequestParam("callback") String callbackName , @RequestParam("pid") String productId ,@RequestParam("tid") int tenantId) {
		login(tenantId);
		IMaintainProductSrv srv = new MaintainProductSrv();
		ProductDto pDto = srv.retrieveProduct(productId);
		logout();
		return JsonHelper.toJson(callbackName, pDto);	
	}
	
	@RequestMapping(value="/rps", method=RequestMethod.GET)
	public @ResponseBody String retrieveProduct(@RequestParam("callback") String callbackName , @RequestParam("tid") int tenantId) {
		login(tenantId);
		ICustomObjectSrv srv = new CustomObjectSrv();
		CustomObject co = srv.getCustomObjectMetaData("Product");
		logout();
		return JsonHelper.toJson(callbackName, co);	
	}
	
	@RequestMapping(value="/comdlist", method=RequestMethod.GET)
	public @ResponseBody String listCustomObjects(@RequestParam("callback") String callbackName ,@RequestParam("tid") int tenantId) {
		
		login(tenantId);
		ICustomObjectSrv srv = new CustomObjectSrv();
		List<CustomObject> cos = srv.listCustomObjects();
		logout();
		return JsonHelper.toJson(callbackName, cos);	
	}
	
	@RequestMapping(value="/colist", method=RequestMethod.GET)
	public @ResponseBody String listCustomObjects(@RequestParam("callback") String callbackName ,@RequestParam("tid") int tenantId , @RequestParam("oid") int customObjectId ) {
		
		login(tenantId);
		ICustomObjectSrv srv = new CustomObjectSrv();
		List<CustomObject> cos = srv.listCustomObjects(customObjectId);
		logout();
		return JsonHelper.toJson(callbackName, cos);	
	}
	
	@RequestMapping(value="/colistbk", method=RequestMethod.GET)
	public @ResponseBody String listCustomObjectsBykeyword(@RequestParam("callback") String callbackName ,
			@RequestParam("tid") int tenantId , @RequestParam("oid") int customObjectId ,@RequestParam("kw") String keyword  ) {
		
		login(tenantId);
		ICustomObjectSrv srv = new CustomObjectSrv();
		List<CustomObject> cos = srv.listCustomObjects(customObjectId,keyword);
		logout();
		return JsonHelper.toJson(callbackName, cos);	
	}
	
	@RequestMapping(value="/co", method=RequestMethod.GET)
	public @ResponseBody String getCustomObject(@RequestParam("callback") String callbackName ,@RequestParam("tid") int tenantId , @RequestParam("oid") int customObjectId , @RequestParam("opk") String customObjectPkVal ) {
		
		login(tenantId);
		ICustomObjectSrv srv = new CustomObjectSrv();
		CustomObject co = srv.getCustomObject(customObjectId, customObjectPkVal);
		logout();
		return JsonHelper.toJson(callbackName, co);	
	}
	
	@RequestMapping(value="/pcr", method=RequestMethod.GET)
	public @ResponseBody String getProductCustomRelationship(@RequestParam("callback") String callbackName ,@RequestParam("tid") int tenantId  ) {
		
		login(tenantId);
		ICustomObjectSrv srv = new CustomObjectSrv();
		CustomObject co = srv.getCustomObjectRelationship("Product");
		logout();
		return JsonHelper.toJson(callbackName, co);	
	}
	
	@RequestMapping(value="/cr", method=RequestMethod.GET)
	public @ResponseBody String getCustomRelationship(@RequestParam("callback") String callbackName ,@RequestParam("tid") int tenantId ,
			@RequestParam("cid") int customObjectId) {
		
		login(tenantId);
		ICustomObjectSrv srv = new CustomObjectSrv();
		CustomObject co = srv.getCustomObjectRelationship(customObjectId);
		logout();
		return JsonHelper.toJson(callbackName, co);	
	}
	
	@RequestMapping(value="/spcr", method=RequestMethod.GET)
	public void saveProductCustomRelationship(@RequestParam("callback") String callbackName ,@RequestParam("tid") int tenantId ,@RequestParam("coids") String customObjectIds ) {
		
		login(tenantId);
		ICustomObjectSrv srv = new CustomObjectSrv();
		CustomObject co = srv.getCustomObjectRelationship("Product");
		logout();
		
	}
	
	@RequestMapping(value="/scr",method=RequestMethod.POST)
	@ResponseBody
	public String  saveCustomRelationship(@RequestParam(value="text", required=true) String text ) {
		
		try {
			ObjectMapper mapper = new ObjectMapper();
			Map<String,Object> info = mapper.readValue(text, new TypeReference<Map<String,Object>>() { });
		
			login(Integer.parseInt(info.get("tid").toString()));
			ICustomObjectSrv srv = new CustomObjectSrv();
			srv.saveCustomRelationships(Integer.parseInt(info.get("cid").toString())
					, info.get("coids").toString());
			logout();
			return "success";
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		return "fail";
			
	}
	
	@RequestMapping(value="/addco",method=RequestMethod.POST)
	public void addCustomObject(@RequestParam(value="text", required=true) String text ){	
		
		try {
			
			ObjectMapper mapper = new ObjectMapper();
			Map<String,Object> info = mapper.readValue(text, new TypeReference<Map<String,Object>>() { });
			String name = info.get("name").toString();
			login(Integer.parseInt(info.get("tid").toString()));

			ICustomObjectSrv srv = new CustomObjectSrv();
			srv.addCustomObject(name);
			logout();
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}
	
	@RequestMapping(value="/scf",method=RequestMethod.POST)
	@ResponseBody
	public void saveCustomObjectMetaData(@RequestParam(value="text", required=true) String text ){	
		
		try {
			
			ObjectMapper mapper = new ObjectMapper();
			Map<String,Object> info = mapper.readValue(text, new TypeReference<Map<String,Object>>() { });
			String coId = info.get("customObjectId").toString();
			login(Integer.parseInt(info.get("tid").toString()));

			ICustomObjectSrv srv = new CustomObjectSrv();
			CustomObject co = srv.getCustomObjectMetaData(Integer.parseInt(coId));
			
			List<Map<String,Object>> cfs = ((List<Map<String,Object>>)info.get("customFields"));
			
			int preFieldNum = 0;
			for (Map<String, Object> map : cfs) {
				int id = Integer.parseInt(map.get("id").toString());
				if(id ==0){
					CustomField cf = CustomizationHandler.newCustomField();
					cf.setName(map.get("name").toString());
					cf.setFieldNum(Integer.toString(preFieldNum+1));
					cf.setType(FieldType.getType(Integer.parseInt(map.get("type").toString())));
					cf.setIndexType(IndexType.getType(Integer.parseInt(map.get("indexType").toString())));
					co.addCustomField(cf);
				}else{
					for (CustomField cf : co.getCustomFields()) {
						if(cf.getId() == id ){
							cf.setName(map.get("name").toString());
							cf.setType(FieldType.getType(Integer.parseInt(map.get("type").toString())));
							cf.setIndexType(IndexType.getType(Integer.parseInt(map.get("indexType").toString())));
							preFieldNum = Integer.parseInt(cf.getFieldNum());
							break;
						}
					}
				}
				
			}
			
			srv.saveCustomObjectMetaData(co);
			
			logout();
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}
	
	@RequestMapping(value="/scodata",method=RequestMethod.POST)
	@ResponseBody
	public void saveCustomObject(@RequestParam(value="text", required=true) String text ){	
		
		try {
			
			ObjectMapper mapper = new ObjectMapper();
			Map<String,Object> info = mapper.readValue(text, new TypeReference<Map<String,Object>>() { });
			int coId = Integer.parseInt(info.get("customObjectId").toString());
			String pkVal = info.get("coPkVal").toString();
			login(Integer.parseInt(info.get("tid").toString()));

			ICustomObjectSrv srv = new CustomObjectSrv();
			CustomObject co  = null;
			if(pkVal.equals("0")){
				co = srv.getCustomObjectMetaData(coId);
			}else{
				co = srv.getCustomObject(coId, pkVal);
			}
		
			List<Map<String,Object>> cfs = ((List<Map<String,Object>>)info.get("customFields"));
			for (CustomField cf : co.getCustomFields()) {
				boolean isExisted = false;
				for (Map<String, Object> map : cfs) {
					int id = Integer.parseInt(map.get("id").toString());
					if(cf.getId() == id && cf.getIndexType().getValue() != 1){
						if(cf.getType().getValue() == 6){
							cf.setValue(StringEscapeUtils.unescapeHtml(map.get("value").toString()));
						}else{
							cf.setValue(map.get("value").toString());
						}
						isExisted = true;
						break;
					}
				}
				
				if(!isExisted){
					if(cf.getType().getValue() == 5){
						String [] ary = cf.getName().split("_");
						if(ary[0].equals(co.getName())){
							CustomObject dtlCo = srv.getCustomObjectMetaData(ary[1]);
							cf.setValue(Integer.toString(dtlCo.getId()));
						}
					}
				}
			}
			
			srv.saveCustomObject(co);
			
			logout();
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}
	
	@RequestMapping(value="/deldata",method=RequestMethod.POST)
	@ResponseBody
	public void deleteCustomObject(@RequestParam(value="text", required=true) String text ){
		
		try {
			
			ObjectMapper mapper = new ObjectMapper();
			Map<String,Object> info = mapper.readValue(text, new TypeReference<Map<String,Object>>() { });
			int coId = Integer.parseInt(info.get("customObjectId").toString());
			String pkVal = info.get("coPkVal").toString();
			login(Integer.parseInt(info.get("tid").toString()));

			ICustomObjectSrv srv = new CustomObjectSrv();
			srv.deleteCustomRelationships(coId, pkVal);
			
			logout();
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		
		
		
	}
	
	@RequestMapping(value="/delcos",method=RequestMethod.POST)
	@ResponseBody
	public void deleteCustomObjects(@RequestParam(value="text", required=true) String text ){
		
		try {
			
			ObjectMapper mapper = new ObjectMapper();
			Map<String,Object> info = mapper.readValue(text, new TypeReference<Map<String,Object>>() { });
			int coId = Integer.parseInt(info.get("customObjectId").toString());
			String copkVals = info.get("coPkVals").toString();
			login(Integer.parseInt(info.get("tid").toString()));
			if(!copkVals.isEmpty()){
				String [] pkVals = copkVals.split(",");
				ICustomObjectSrv srv = new CustomObjectSrv();
				for (int i = 0; i < pkVals.length; i++) {
					srv.deleteCustomRelationships(coId, pkVals[i]);
				}
			}

			logout();
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		
		
		
	}
	

	@RequestMapping(value="/order",method=RequestMethod.POST)
	@ResponseBody
	public String saveOrder(@RequestParam(value="text", required=true) String text){	
		
		ObjectMapper mapper = new ObjectMapper();
		try {
		
			Map<String,Object> orderInfo = mapper.readValue(text, new TypeReference<Map<String,Object>>() { });
			IOrderSrv srv = new OrderSrv();
			
			List<Integer> tenantids = srv.doOrderTenantIds(((List<Map<String,Object>>)orderInfo.get("OrderLineitem")));
			for (Integer tid : tenantids) {
				this.login(tid);
				srv.order(orderInfo,tid);
				this.logout();
			}
			
			
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return "redirect:/";
	}
	
	
	

	@RequestMapping(value = "/", method = RequestMethod.GET)
	public String home() {
		
		return "home";
	}
	
	private void login(int tid) {
		
		User u1 = null;
		String userName = "";
		switch (tid) {
		case 1:
			u1 = new User("Apple", "12345",1,1);
			break;
		case 2:
			u1 = new User("Orange", "12345",1,2);
			break;
		case 3:
			u1 = new User("Banana", "12345",3,3);
			break;
		}
		
		TenantContextKeeper.getContext().setTenantUser(u1);
		  
			
	}
	
	private void logout(){

		  TenantContextKeeper.clearContext();

	}
	
	
/*
	@Resource(name = "authenticationManager")
	private AuthenticationManager authenticationManager; // specific for Spring Security
	
	private String login(int tid) {
		
		String userName = "";
		switch (tid) {
		case 1:
			userName = "Apple";
			break;
		case 2:
			userName = "Orange";
			break;
		}
		
		try {
			Authentication authenticate = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(userName, "12345"));
	        if (authenticate.isAuthenticated()) {
	           SecurityContextHolder.getContext().setAuthentication(authenticate); 
	        //	User user =
	   				// (User)(UserDetails)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
	        	 	
	            return "__";
	        }else{
	        	return "testF";
	        }
	    }
	    catch (AuthenticationException e) {   
	    	return "testS";
	    }
	   // return false;
			
	}
	
	private void logout(){
		
	 	//authenticate.setAuthenticated(false);
    	SecurityContextHolder.getContext().setAuthentication(null); 
    	//SecurityContextHolder.clearContext();
	}
	*/
	
}
