package com.arthur.shoppingmall.test;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;


@Aspect
public class MultiTenantAspect {
	
	@Pointcut("call(com.arhur.domain.Product.new(..))")
	    public  void init(ProceedingJoinPoint pjp) {
	}
	    
	@Around("init(pjp)")
	public Object initAdvice(ProceedingJoinPoint pjp) throws Throwable{
	        Object ret = pjp.proceed();
	        if(ret == null){
	        	System.out.println("111");
	        }
	        //System.out.println(ret.toString());
	    	System.out.println("AAA");
	        return pjp;
	}
	

}
