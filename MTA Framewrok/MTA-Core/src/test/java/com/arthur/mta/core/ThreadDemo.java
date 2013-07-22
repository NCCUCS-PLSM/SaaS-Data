package com.arthur.mta.core;

import java.lang.reflect.InvocationTargetException;

public class ThreadDemo implements Runnable {

	   Thread t;

	   ThreadDemo() {

	      t = new Thread(this);
	      // this will call run() function
	      t.start();
	   }

	   public void run() {

	      ClassLoader c = t.getContextClassLoader();
	      // sets the context ClassLoader for this Thread
	      CustomClassLoader test = new CustomClassLoader();
	      t.setContextClassLoader(test);
	      //t.setContextClassLoader(c);
	      System.out.println("Class = " + c.getClass());
	      System.out.println("Parent = " + c.getParent());
	      
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
	      
	   }

	   public static void main(String args[]) {
	      new ThreadDemo();
	   }
} 
