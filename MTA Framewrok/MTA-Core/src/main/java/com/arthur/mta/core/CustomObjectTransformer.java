package com.arthur.mta.core;



import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;

import com.arthur.mta.core.annotations.MultiTenantable;
import com.arthur.mta.core.context.MyClassLoader;
import com.arthur.mta.core.enhance.BytecodeEnhancer;


public class CustomObjectTransformer {
	
	public Class<?> transform(Class<?> sourceClass ){
		
		String className = getAnnotatedClassName(sourceClass);
		if(!className.equals("")){
			BytecodeEnhancer be = new BytecodeEnhancer();
			//return be.enhance(className);
		}
		
		return sourceClass;
	}
	
	public Object transform2(Class<?> sourceClass , Object obj ){
		
		String className = getAnnotatedClassName(sourceClass);
		boolean isTrue = false;
		if(!className.equals("")){
			BytecodeEnhancer be = new BytecodeEnhancer();
			//isTrue =be.buildCustomObject(className);
		}
		
		if(isTrue){
			//ClassLoader parentClassLoader = MyClassLoader.class.getClassLoader();
		    //MyClassLoader classLoader = new MyClassLoader(parentClassLoader);
			CustomClassLoader classLoader = new CustomClassLoader();
		    try {
				obj = classLoader.loadClass(className).newInstance();
			} catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}

		return obj;
	}
	
 
	
	private String getAnnotatedClassName(Class<?> cls){
		
		Annotation[] annotatedClasses = cls.getAnnotations();
	
		String csName = "";
		for (Annotation an : annotatedClasses) {
			if(an.annotationType().equals(MultiTenantable.class)){
				csName = cls.getName();
				break;
			}
		}
		
		return csName;
	}
	
	/*
	private Class createClass( String className , String classPath ){
		
		ClassLoader parentClassLoader = MyClassLoader.class.getClassLoader();
	    MyClassLoader classLoader = new MyClassLoader(parentClassLoader,classPath);
	    Class myObjectClass =null;
	    
		try {
			myObjectClass = classLoader.loadClass(className);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return myObjectClass;
		
	}*/
	
	
}
