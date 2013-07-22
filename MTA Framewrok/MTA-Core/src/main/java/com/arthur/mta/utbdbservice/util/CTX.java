package com.arthur.mta.utbdbservice.util;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public  final class CTX {
	
 
    private static final ApplicationContext ctx = 
    		new ClassPathXmlApplicationContext("app-context.xml");
    
    private CTX() {}

    public static ApplicationContext get() {
        return ctx;
    }
    
} 