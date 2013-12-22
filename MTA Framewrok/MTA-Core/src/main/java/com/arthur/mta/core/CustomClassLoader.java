package com.arthur.mta.core;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Hashtable;

public class CustomClassLoader extends ClassLoader {
	
	private Hashtable classes = new Hashtable();
	
    public CustomClassLoader(){
        super(CustomClassLoader.class.getClassLoader());
    }
  
    public Class loadClass(String className) throws ClassNotFoundException {
         return findClass(className);
    }
 
    public Class findClass(String className){
        byte classByte[];
        Class result=null;
        result = (Class)classes.get(className);
        if(result != null){
            return result;
        }

        try{
           String classPath =    ((String)ClassLoader.getSystemResource(className.replace('.',File.separatorChar)+".class").getFile()).substring(1);
           classByte = loadClassData(classPath);
            result = defineClass(className,classByte,0,classByte.length,null);
            classes.put(className,result);
            return result;
        }catch(Exception e){
        	try{
                Class<?> sysCls=  findSystemClass(className);
                if(sysCls != null){
              	  return sysCls;
                }
              }catch(Exception e1){
            	  e1.printStackTrace();
              }
            
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