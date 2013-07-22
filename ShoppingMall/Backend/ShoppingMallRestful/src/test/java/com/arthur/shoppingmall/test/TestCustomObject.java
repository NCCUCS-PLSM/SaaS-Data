package com.arthur.shoppingmall.test;

import static org.junit.Assert.*;

import java.util.List;

import javax.annotation.Resource;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.arthur.mta.core.CustomField;
import com.arthur.mta.core.CustomObject;
import com.arthur.mta.core.CustomRelationship;
import com.arthur.mta.core.FieldType;
import com.arthur.mta.core.IndexType;
import com.arthur.mta.core.MultiTenantUser;

import com.arthur.mta.core.context.TenantContextKeeper;
import com.arthur.mta.core.customization.CustomizationHandler;
import com.arthur.shoppingmall.CustomObjectSrv;
import com.arthur.shoppingmall.ICustomObjectSrv;
import com.arthur.shoppingmall.domain.User;


//@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration(locations = {"securityContext.xml"})
public class TestCustomObject {

	//@Resource(name = "authenticationManager")
	//private AuthenticationManager authenticationManager; // specific for Spring Security
	
	@Test
	public void testDel(){
		this.login(1);
		ICustomObjectSrv srv = new CustomObjectSrv();
		srv.deleteCustomRelationships(3, "16");
		this.logout();
	}
	
	@Test
	public void testFindByIndex(){
		
		this.login(1);
		List<CustomObject> cos  = CustomizationHandler.findCustomObjectsByIndexes(1, "2013/06/13");
				for (CustomObject co : cos) {
					System.out.println(co.getName());
					for (CustomField cf : co.getCustomFields()) {
						System.out.println(cf.getName());
						System.out.println(cf.getType().getKey());
						System.out.println(cf.getIndexType().getKey());
						System.out.println(cf.getValue());
						System.out.println("====================");
					}
				}
		this.logout();
		
	}
	
	
	@Test
	public void testFindRelData() {
		
		this.login(1);
		CustomObject co  = CustomizationHandler.findCustomObject(10, "1");
		
			System.out.println(co.getName());
			for (CustomField cf : co.getCustomFields()) {
				System.out.println(cf.getName());
				//System.out.println(cf.getType().getKey());
				//System.out.println(cf.getIndexType().getKey());
				System.out.println(cf.getValue());
				if(cf.getValues()!=null){
					for (CustomObject c : cf.getValues()) {
						for (CustomField f : c.getCustomFields()) {
							System.out.println(f.getName());
							System.out.println(f.getValue());
						}
					}
				}
				
			}
		
		
		this.logout();
	}
	
	@Test
	public void testSaveRel(){
		
		this.login(1);
		CustomObject co = CustomizationHandler.findCustomObjectRelationships(3);
		CustomRelationship cusRel = CustomizationHandler.newCustomRelationship();
		cusRel.setDetailObjectId(3);
		cusRel.setDetailObjectName("Product");
		cusRel.setMasterObjectId(10);
		cusRel.setMasterObjectName("TestCustomObject2");
		co.addCustomRelationship(cusRel);
		CustomizationHandler.saveCustomObjectRelatioinship(co);
		
	}
	
	@Test
	public void testRelationship() {
		
		this.login(1);
		CustomObject co = CustomizationHandler.findCustomObjectRelationships(3);
		for (com.arthur.mta.core.CustomRelationship cusRel : co.getCustomRelationships()) {
			System.out.println(cusRel.getMasterObjectName());
			System.out.println(cusRel.getDetailObjectId());
			System.out.println(cusRel.getDetailObjectName());
		}
	}
	
	@Test
	public void testMetaData() {
		
		this.login(1);
		
		CustomObject co = CustomizationHandler.newCustomObject();
		co.setName("Inventory");
		
		CustomField cf = CustomizationHandler.newCustomField();
		cf.setName("InventoryId");
		cf.setFieldNum("0");
		cf.setType(FieldType.Number);
		cf.setIndexType(IndexType.Primarykey);
		
		co.addCustomField(cf);
		
		CustomField cf2 = CustomizationHandler.newCustomField();
		cf2.setName("InventoryNmae");
		cf2.setFieldNum("1");
		cf2.setType(FieldType.String);
		cf2.setIndexType(IndexType.NotIndexed);
		
		co.addCustomField(cf2);
		
		CustomizationHandler.saveCustomObjectMetaData(co);
		
		this.logout();
	}
	
	@Test
	public void testValue() {
		
		this.login(1);
		
		CustomObject co = CustomizationHandler.findCustomObject(10, "3");
		
		/*
		System.out.println(co.getName());
		for (CustomField cf : co.getCustomFields()) {
			System.out.println(cf.getName());
			System.out.println(cf.getType().getKey());
			System.out.println(cf.getIndexType().getKey());
		}*/
		
		co.getCustomFields().get(1).setValue("test12345");
		co.getCustomFields().get(2).setValue("2000");
		co.getCustomFields().get(3).setValue("2013/06/17");
		//co.getCustomFields().get(2).setValue("test");
		CustomizationHandler.saveCustomObject(co);
		
		this.logout();
	}
	
	@Test
	public void testUpdateMetaData() {
		
		this.login(1);
		
		CustomObject co = CustomizationHandler.findCustomObjectMetaData(14);
		
		CustomField cf2 = CustomizationHandler.newCustomField();
		cf2.setName("InventoryDate");
		cf2.setFieldNum("1");
		cf2.setType(FieldType.String);
		cf2.setIndexType(IndexType.NotIndexed);
		
		co.addCustomField(cf2);
		
		co.getCustomFields().get(1).setType(FieldType.Imagestring);
		co.getCustomFields().get(1).setName("image");
		
		CustomizationHandler.saveCustomObjectMetaData(co);
		
		this.logout();
	}
	
	@Test
	public void testUpdateValue() {
		
		this.login(1);
		
		CustomObject co = CustomizationHandler.findCustomObject(14, "1");
		
		
		System.out.println(co.getName());
		for (CustomField cf : co.getCustomFields()) {
			System.out.println(cf.getName());
			System.out.println(cf.getType().getKey());
			System.out.println(cf.getIndexType().getKey());
			System.out.println(cf.getValue());
		}
		
	
		
		this.logout();
	}
	
	@Test
	public void testFindAll() {
		
		this.login(1);
		
		List<CustomObject> cos = CustomizationHandler.findCustomObjectsMetaData();
		for (CustomObject co : cos) {
			System.out.println(co.getName());
			for (CustomField cf : co.getCustomFields()) {
				System.out.println(cf.getName());
				System.out.println(cf.getType().getKey());
				System.out.println(cf.getIndexType().getKey());
				System.out.println(cf.getValue());
			}
		}
		
		this.logout();
	}
	
	@Test
	public void testFindAllV() {
		
		this.login(1);
		List<CustomObject> cos = CustomizationHandler.findCustomObjects(14);
		for (CustomObject co : cos) {
			System.out.println(co.getName());
			for (CustomField cf : co.getCustomFields()) {
				System.out.println(cf.getName());
				//System.out.println(cf.getType().getKey());
				//System.out.println(cf.getIndexType().getKey());
				System.out.println(cf.getValue());
			}
		}
		
		this.logout();
	}
	
	@Test
	public void testAddData() {
		
		this.login(1);
		CustomObject co = CustomizationHandler.findCustomObjectMetaData(14);
			for (CustomField cf : co.getCustomFields()) {
				if(cf.getIndexType().getValue() != 1){
					cf.setValue("222");
				}
			}
			
			CustomizationHandler.saveCustomObject(co);
		
		
		this.logout();
	}
	
	@Test
	public void testFindMDByName() {
		
		this.login(1);
		CustomObject co  = CustomizationHandler.findCustomObjectMetaData("Product");
		
			System.out.println(co.getName());
			for (CustomField cf : co.getCustomFields()) {
				System.out.println(cf.getName());
				//System.out.println(cf.getType().getKey());
				//System.out.println(cf.getIndexType().getKey());
				System.out.println(cf.getValue());
			}
		
		
		this.logout();
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

}
