package com.arthur.mta.core.context;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.objectweb.asm.ClassVisitor;

public class InterfaceAdder  extends ClassVisitor{

	private Set<String> newInterfaces;
	
	public InterfaceAdder(int api , ClassVisitor cv , Set<String> newInterfaces) {
		super(api ,cv);
		this.newInterfaces = newInterfaces;
	}
	
	public void visit(int version , int access , String name , String signature ,
			String superName , String [] interfaces){
		//super.visit(version, access, name, signature, superName, interfaces);
		Set<String> ints = new HashSet(newInterfaces);
		ints.addAll(Arrays.asList(interfaces));
		String[] array = ints.toArray(new String[0]);
		cv.visit(version, access, name, signature, superName,array);
	}
	

}
