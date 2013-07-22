package com.arthur.mta.core.context;

 public class SimpleClassLoader extends ClassLoader {

    public Class<?> defineClass(String className, byte [] classBytes){
        return super.defineClass(className, classBytes, 0, classBytes.length);
    }
}