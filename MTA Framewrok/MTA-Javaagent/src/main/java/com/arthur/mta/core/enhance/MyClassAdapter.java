package com.arthur.mta.core.enhance;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

public class MyClassAdapter extends ClassVisitor{
	
	public MyClassAdapter(ClassVisitor cv) {
		super(Opcodes.ASM4, cv);
		}

}
