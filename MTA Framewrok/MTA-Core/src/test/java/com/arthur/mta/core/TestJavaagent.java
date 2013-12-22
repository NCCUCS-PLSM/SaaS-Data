package com.arthur.mta.core;

import static org.junit.Assert.*;

import org.junit.Test;



public class TestJavaagent {
	
	 static {
	      //  MTAJavaAgent.initialize();
	    }

	@Test
	public void test() {
		Foo f = new Foo();
		CustomObject co  = (CustomObject)f;
		System.out.println(co.getTenantId());
		//co.addField(new CustomFieldImp("1","2","3"));
		/*
		for (CustomField e : co.getFields()) {
			System.out.println(e.getName());
			System.out.println(e.getType());
			System.out.println(e.getValue());
		}*/
		Foo f2 = (Foo)co;
	}

}
