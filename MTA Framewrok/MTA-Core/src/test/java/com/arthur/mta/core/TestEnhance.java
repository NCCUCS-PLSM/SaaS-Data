package com.arthur.mta.core;

import static org.junit.Assert.*;

import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;

import org.junit.Test;

import com.arthur.mta.core.context.MyClassLoader;

public class TestEnhance {

	@Test
	public void test() throws ClassNotFoundException {
		
		
		ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
		ClassLoader newClassLoader = new CustomClassLoader();


		try {    
			Thread.currentThread().setContextClassLoader(newClassLoader);
		
			// write code to load new classes
			CustomObjectTransformer tf = new CustomObjectTransformer();
			Class<?> cls = tf.transform(Foo.class);
			
			//ClassLoader parentClassLoader = MyClassLoader.class.getClassLoader();
		    //MyClassLoader classLoader = new MyClassLoader(parentClassLoader );
		   
			try {
				
				Object o= cls.getConstructor().newInstance();
				//Foo f = (Foo)o;
				
				CustomObject co = (CustomObject)o;
				//co.addField(new CustomFieldImp("1","2","3"));
				/*
				for (CustomField e : co.getFields()) {
					System.out.println(e.getName());
					System.out.println(e.getType());
					System.out.println(e.getValue());
				}*/
			
			}catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} finally {
			Thread.currentThread().setContextClassLoader(originalClassLoader);
		}

		 //CustomClassLoader test = new CustomClassLoader();
         //Class<?> newCls= test.loadClass("com.arthur.mta.core.Foo");
         //Thread.currentThread().setContextClassLoader(test);
		
		
		
		
	}

}
