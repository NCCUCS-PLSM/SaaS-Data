package com.arthur.mta.core.context;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Hashtable;



public class MyClassLoader extends ClassLoader{

	private Hashtable classes = new Hashtable();
	 public MyClassLoader(ClassLoader parent) {
	        super(parent);
	    }

	    public Class loadClass(String name) throws ClassNotFoundException {
	        //if(!"reflection.MyObject".equals(name))
	                //return super.loadClass(name);

	        try {
	            String url = "file://localhost/home/arthur/Projects/ShoppingMall/ShoppingMallWeb/target/classes/com/arthur/shoppingmall/domain/Product.class";
	            URL myUrl = new URL(url);
	            URLConnection connection = myUrl.openConnection();
	            InputStream input = connection.getInputStream();
	            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
	            int data = input.read();

	            while(data != -1){
	                buffer.write(data);
	                data = input.read();
	            }

	            input.close();

	            byte[] classData = buffer.toByteArray();

	            return defineClass(name,
	                    classData, 0, classData.length);

	        } catch (MalformedURLException e) {
	            e.printStackTrace();
	        } catch (IOException e) {
	            e.printStackTrace(); 
	        }

	        return null;
	    }
	    
	    public Class findClass(String className){
	        byte classByte[];
	        Class result=null;
	        result = (Class)classes.get(className);
	        if(result != null){
	            return result;
	        }
	        
	        try{
	            Class<?> sysCls=  findSystemClass(className);
	            if(sysCls != null){
	          	  return sysCls;
	            }
	          }catch(Exception e1){
	        	  e1.printStackTrace();
	          }
	        
	        try{
	           String classPath =    ((String)ClassLoader.getSystemResource(className.replace('.',File.separatorChar)+".class").getFile()).substring(1);
	           classByte = loadClassData(classPath);
	            result = defineClass(className,classByte,0,classByte.length,null);
	            classes.put(className,result);
	            return result;
	        }catch(Exception e){
	            
	        }
	        return null;
	    }
	    
	    private byte[] loadClassData(String className) throws IOException{
	    	 /*
	        File f ;
	        f = new File(className);
	        int size = (int)f.length();
	        byte buff[] = new byte[size];
	        FileInputStream fis = new FileInputStream(f);
	        DataInputStream dis = new DataInputStream(fis);
	        dis.readFully(buff);
	        dis.close();
	        */
	    	
	        URL myUrl = new URL("file://localhost/"+className);
	        URLConnection connection = myUrl.openConnection();
	        InputStream input = connection.getInputStream();
	        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
	        int data = input.read();

	        while(data != -1){
	            buffer.write(data);
	            data = input.read();
	        }

	        input.close();
	        byte[] classData = buffer.toByteArray();
	        return classData;
	    }
	    

}
