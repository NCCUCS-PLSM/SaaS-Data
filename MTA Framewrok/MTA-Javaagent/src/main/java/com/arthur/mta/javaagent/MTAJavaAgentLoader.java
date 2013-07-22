package com.arthur.mta.javaagent;

import java.lang.management.ManagementFactory;

import com.sun.tools.attach.VirtualMachine;

public class MTAJavaAgentLoader {
	
	  	private static final String jarFilePath = "/home/arthur/mtajavaagent.jar";
		//private static final String jarFilePath = "/home/arthur/Projects/ShoppingMall/.metadata/.plugins/org.eclipse.wst.server.core/tmp0/wtpwebapps/ShoppingMallWeb/WEB-INF/lib/mtajavaagent.jar";

	    public static void loadAgent() {
	        //logger.info("dynamically loading javaagent");
	        String nameOfRunningVM = ManagementFactory.getRuntimeMXBean().getName();
	        int p = nameOfRunningVM.indexOf('@');
	        String pid = nameOfRunningVM.substring(0, p);

	        try {
	        	System.out.println(jarFilePath);
	            VirtualMachine vm = VirtualMachine.attach(pid);
	            System.out.println("JJJJJJJJJJJJJJJJJJJJJJJJ");
	            vm.loadAgent(jarFilePath, "");
	            System.out.println("AAAAAAAAAAAAAAAAAAAAA");
	            vm.detach();
	        } catch (Exception e) {
	            throw new RuntimeException(e);
	        }
	    }
	

}
