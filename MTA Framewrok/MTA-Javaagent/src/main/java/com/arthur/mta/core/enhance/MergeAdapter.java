package com.arthur.mta.core.enhance;

import java.util.Iterator;


import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.RemappingMethodAdapter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.commons.SimpleRemapper;

import com.arthur.mta.core.CustomObject;

public class MergeAdapter extends ClassVisitor{
	
	private ClassNode cn;
	private String cname;
	

	public MergeAdapter(int api , ClassVisitor cv , ClassNode cn) {
		super(api ,cv);
		this.cn = cn;
	}
	
	public void visit(int version , int access , String name , String signature ,
			String superName , String [] interfaces){
		
		super.visit(version, access, name, signature, superName, interfaces);
		this.cname = name;

	}
	
	public void visitEnd(){
		
		for(Iterator<?> it = cn.fields.iterator();it.hasNext();){
			FieldNode fn = ((FieldNode)it.next());
			fn.accept(this);
		}
		
		for(Iterator<?> it = cn.methods.iterator();it.hasNext();){
			MethodNode mn = (MethodNode)it.next();
			if(!mn.name.equals("<init>")){
				//System.out.println(mn.name);
				String[] exceptions = 
						new String[mn.exceptions.size()];
				mn.exceptions.toArray(exceptions);
				MethodVisitor mv = cv.visitMethod(
						mn.access, mn.name, mn.desc, mn.signature, exceptions);
				mn.instructions.resetLabels();
				System.out.println("This is a test_" + cn.name);
				System.out.println("This is a test_" + cname);
				mn.accept(new RemappingMethodAdapter(
						mn.access,mn.desc,mv, new SimpleRemapper(cn.name , cname)));
			}
			
		}
		
		super.visitEnd();
		
	}
	

	

}
