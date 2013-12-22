package com.arthur.shoppingmallweb;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.arthur.mta.core.context.TenantContextKeeper;
import com.arthur.shoppingmall.IMaintainProductSrv;
import com.arthur.shoppingmall.IOrderSrv;
import com.arthur.shoppingmall.MaintainProductSrv;
import com.arthur.shoppingmall.OrderSrv;
import com.arthur.shoppingmall.domain.SFField;
import com.arthur.shoppingmall.domain.User;
import com.arthur.shoppingmall.view.ProductDto;
import com.arthur.shoppingmall.view.ProductView;
import com.arthur.shoppingmall.view.ViewField;

@Controller
@RequestMapping(value = "/backend/product")
public class ProductController {
	
	@RequestMapping(value = "", method = RequestMethod.GET)
	public String product(Model model) {
		model.addAttribute("userName", "" );
		return "product";
	}
	
	@RequestMapping(value="/pslist", method=RequestMethod.GET)
	public @ResponseBody List<ProductDto> listProducts() {
		
		IMaintainProductSrv srv = new MaintainProductSrv();
		return srv.listProducts();
		
	}
	
	/*
	@RequestMapping(value="/rpv", method=RequestMethod.GET)
	public @ResponseBody ProductView retrieveProduct(@RequestParam("productId") String productId ) {
	
		IMaintainProductSrv srv = new MaintainProductSrv();
		return srv.retrieveProduct(productId);
	}*/
	
	@RequestMapping(value="/sch", method=RequestMethod.GET)
	public @ResponseBody ProductView retrieveSchema() {
	
		IMaintainProductSrv srv = new MaintainProductSrv();
		return srv.fetchProductSchema();
	}
	
	 
	@RequestMapping(value="/schema",method=RequestMethod.POST)
	public String saveSchema(@RequestParam(value="hidSchema", required=true) String hidSchema){	
		
		ObjectMapper mapper = new ObjectMapper();
		try {
		
			Map<String,Object> schemaInfo = mapper.readValue(hidSchema, new TypeReference<Map<String,Object>>() { });
			IMaintainProductSrv srv = new MaintainProductSrv();
			//srv.saveSchema(schemaInfo);
			
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
		
		return "redirect:/backend/product";
	}
	
	@RequestMapping(value="/save",method=RequestMethod.POST)
	public String saveProduct(@RequestParam(value="hidProduct", required=true) String hidProduct ,
			@RequestParam("file") List<MultipartFile> files){	
		
		ObjectMapper mapper = new ObjectMapper();
		try {
			//TODO tenantID need check image seq
			List<String> filePath = doFile(files,TenantContextKeeper.getContext().getTenantUser().getTenantId());
			Map<String,Object> pInfo = mapper.readValue(hidProduct, new TypeReference<Map<String,Object>>() { });
			List<Map<String,Object>> fields = ((List<Map<String,Object>>)pInfo.get("fields"));
			
			ProductView pv = new ProductView();
			int valueCount =0;
			for (Map<String, Object> map : fields) {
				
				String value = map.get("fieldValue").toString();
				if(map.get("controlType").toString().equals("image")){
					value = filePath.get(valueCount);
					valueCount +=1;
				}
				
				ViewField vf = new ViewField(map.get("fieldName").toString() , "" , value ,
						map.get("controlType").toString());
				
				pv.addViewField(vf);	
			}
			
			IMaintainProductSrv srv = new MaintainProductSrv();
			srv.saveProduct(pv);
			
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch blockhandleProduct
			e.printStackTrace();
		}
		

		  return "redirect:/backend/product";
	}
	
	private List<String> doFile(List<MultipartFile> files , Integer tenantId) {
		 List<String> values = new ArrayList<String>();
		 
		 if (!files.isEmpty()) {
			 int count =1;
	        for (MultipartFile multipartFile : files) {
	        	  	//String fileName = multipartFile.getOriginalFilename().su; 
	        		String fileName =genNewFileName(tenantId ,count);
	              System.out.println(fileName);
	              File srvfile = new File("/home/arthur/Projects/ShoppingForce/ShoppingForceWeb" +
	              		"/src/main/webapp/resources/images/"+fileName); 
	              try {
					multipartFile.transferTo(srvfile);
					values.add("/resources/images/"+fileName);
					count +=1;
				} catch (IllegalStateException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
           
		  }
		 
		 return values;
	}
	
	private String genNewFileName(Integer tenantId , Integer index){
		//TODO file type need check
		java.util.Date now = new java.util.Date();
		return tenantId.toString() + (new java.text.SimpleDateFormat("yyyyMMddHHmmss").format(now)) + index.toString()+".jpg";
		
	}
	


}
