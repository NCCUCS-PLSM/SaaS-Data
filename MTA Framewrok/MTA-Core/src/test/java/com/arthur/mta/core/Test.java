package com.arthur.mta.core;

import static org.junit.Assert.*;

import java.io.File;
import java.lang.annotation.Annotation;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import org.junit.BeforeClass;
import org.reflections.Reflections;
import org.reflections.Store;
import org.reflections.scanners.ConvertersScanner;
import org.reflections.scanners.FieldAnnotationsScanner;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import com.arthur.mta.core.annotations.MultiTenantable;

import com.arthur.mta.core.context.MyClassLoader;
import com.google.common.base.Predicate;

public class Test {
	
	 static Reflections reflections;
	
	 @BeforeClass
	    public static void init() {
		 Predicate<String> filter = new FilterBuilder().include(FilterBuilder.prefix("com.arthur.mta.core"));
	        reflections =   new Reflections(new ConfigurationBuilder()
	          .filterInputsBy(filter)
	          .setUrls(ClasspathHelper.forPackage("com.arthur.mta.core"))
	          .setScanners(new SubTypesScanner(),
	                       new TypeAnnotationsScanner().filterResultsBy(filter),
	                       new ResourcesScanner()));
	        
  
	    }
	
	@org.junit.Test
	public void test() {
		//Demo d = new Demo();
		
		 //Reflections reflections2 = new Reflections("com.arthur.mta.core");
		
		Set<String> as = reflections.getStore().getTypesAnnotatedWith(MultiTenantable.class.getName());
		for (String string : as) {
			System.out.println(as);
		}
		
		
		Set<Class<?>> annotated2 =  reflections.getTypesAnnotatedWith(MultiTenantable.class.getAnnotations()[0]);
		for (Class<?> class2 : annotated2) {
			System.out.println(class2.getName());
		}
		
		Set<Class<?>> annotated1 =
	               reflections.getTypesAnnotatedWith(new MultiTenantable() {
	                    public String value() { return "1"; }
	                    public Class<? extends Annotation> annotationType() { return MultiTenantable.class; }
	                });
	
		for (Class<?> class1 : annotated1) {
			System.out.println(class1.getName());
		}
	
	}
	
	@org.junit.Test
	public void testProxyMerge(){
		
		//MTApplicationContext mta = new MTApplicationContext("com.arthur.mta.core");
		//mta.testProxyClassNode();
		
		String purls = "com.arthur.mta.core";
		Reflections reflections = new Reflections(
			    new ConfigurationBuilder()
			        .setUrls(ClasspathHelper.forJavaClassPath())
			        .setUrls(ClasspathHelper.forPackage(purls))
			);
		
		
		Set<Class<?>> annotatedClasses =
	               reflections.getTypesAnnotatedWith(MultiTenantable.class);
		
		
		for (Class<?> class1 : annotatedClasses) {
			
			System.out.println(class1.getName());
		}
	}
	
	@org.junit.Test
	public void testCast() throws InterruptedException, ClassNotFoundException, InstantiationException, IllegalAccessException{
		
		/*
		ReloadingClassLoader classloader = new ReloadingClassLoader(this.getClass().getClassLoader());
		ReloadingListener listener = new ReloadingListener();

		listener.addReloadNotificationListener(classloader);

		File directory = new File("file://home/arthur/Projects/ShoppingMall/MTA-Core/target/classes/com/arthur/mta/core/Foo.class");
		
		FilesystemAlterationMonitor fam = new FilesystemAlterationMonitor();
		fam.addListener(directory, listener);
		fam.start();
		*/
		

		//System.out.println(Foo.class.getClassLoader());
		//System.out.println(Foo.class.getClassLoader().getParent());
		//System.out.println(Foo.class.getClassLoader().getParent().getParent());
		
				
		//System.out.println(someClass.getClassLoader());
		//Foo.class.getClassLoader().getParent().getParent()
		//Foo f = new Foo();
		//System.out.println(f.getClass().getClassLoader());
		//System.out.println(f.getClass().getInterfaces());
		//CustomObject co = (CustomObject)f;
		//System.out.println(Foo.class.getClassLoader());
      
		/*

		*/
		
		//System.out.println(Foo.class.getClassLoader());
		
	
		//Bar b = new Bar();
		//b.test();
		
		//Object f;
	
			//f = Class.forName(Foo.class.getName()).newInstance();
			
		
			//System.out.println(co.getTenantId());
			
		
		//System.out.println(f.getClass().getClassLoader());
		

		
	}
	

}
