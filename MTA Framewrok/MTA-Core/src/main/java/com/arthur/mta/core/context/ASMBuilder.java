package com.arthur.mta.core.context;

import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicLong;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.TraceClassVisitor;



public class ASMBuilder {
	
	
private AtomicLong UNIQUE_ID_GENERATOR = new AtomicLong(System.nanoTime());
	

	public Object build(Class type , Class type1){
		
		//ClassNode classNode = this.createClass2(type);
		 ClassNode classNode = createClass(makeUnique(type.getSimpleName()));
		
		SimpleClassLoader loader = new SimpleClassLoader();
		/*
		  InputStream in;
		  ClassNode classNode = null;
		try {
			in = new FileInputStream("bin/com/arthur/test2/P2.class");
			 ClassReader cr;
		
				cr = new ClassReader(in);
		
			 classNode =new ClassNode();
		     //ClassNode is a ClassVisitor
		        cr.accept(classNode, 0);
		
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}catch (IOException e) {
			// TODO Auto-generated catch block
		}
		*/
	        classNode.superName = Type.getInternalName(type1);
	        classNode.interfaces.add(Type.getInternalName(type));
	        
	     //   createInstancesField(classNode);
	        createConstructor(classNode);
	        
	        classNode.visitEnd();
	        
		      ClassWriter cw = new ClassWriter(0);
		      TraceClassVisitor checkAdapter = new TraceClassVisitor(cw,
		            new PrintWriter(System.out));
		      classNode.accept(checkAdapter);

		   
	        
	        try {
	        	 return  loader.defineClass(classNode.name.replace('/', '.'),
	  	               cw.toByteArray()).getConstructor().newInstance();
	        	
		         
		      } catch (Exception ex) {
		         throw new RuntimeException(ex);
		      }
		
		
	}
	

	   private void createInstancesField(ClassNode classNode) {
	      classNode.fields.add(new FieldNode(Opcodes.ACC_PRIVATE, "_instances",
	            Type.getDescriptor(Object[].class), null, null));
	   }
	
	private String makeUnique(String id) {
	      return id + "_" + Long.toHexString(UNIQUE_ID_GENERATOR.getAndIncrement());
	}
	
	  private ClassNode createClass(String newClassName) {
	      ClassNode cn = new ClassNode(4);
	      cn.visit(Opcodes.V1_5, Opcodes.ACC_PUBLIC, newClassName, null, Type
	            .getInternalName(Object.class), null);
	      return cn;
	   }	        
	
	   private void createConstructor(ClassNode classNode) {
		   
//		   public <init>()V
//		   L0
//		    LINENUMBER 3 L0
//		    ALOAD 0
//		    INVOKESPECIAL com/arthur/test2/Product.<init> ()V
//		    RETURN
//		   L1
//		    LOCALVARIABLE this Lcom/arthur/test2/P2; L0 L1 0
//		    MAXSTACK = 1
//		    MAXLOCALS = 1
		   
		      MethodNode constructor = new MethodNode(Opcodes.ACC_PUBLIC, "<init>",
		            "()V", null, null);
		      constructor.visitVarInsn(Opcodes.ALOAD, 0);
		      constructor.visitMethodInsn(Opcodes.INVOKESPECIAL, classNode.superName,
		            "<init>", "()V");	      
		      constructor.visitInsn(Opcodes.RETURN);
		      constructor.visitMaxs(1, 1);
		      constructor.visitEnd();
		      classNode.methods.add(constructor);
	   }
	   
	   
	   private ClassNode createClass2(Class type){
		   
		   ClassNode classNode=new ClassNode(4);//4 is just the API version number
	        
	        //These properties of the classNode must be set
	        classNode.version=Opcodes.V1_6;//The generated class will only run on JRE 1.6 or above
	        classNode.access=Opcodes.ACC_PUBLIC;
	        classNode.signature="Lcom/geekyarticles/asm/Generated;";
	        classNode.name="com/geekyarticles/asm/Generated";
	        classNode.superName="java/lang/Object";
	        classNode.interfaces.add(Type.getInternalName(type));
	        
//	        //Create a method
//	        MethodNode mainMethod=new MethodNode(4,Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,"main", "([Ljava/lang/String;)V",null, null);
//	        
//	        mainMethod.instructions.add(new FieldInsnNode(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"));
//	        mainMethod.instructions.add(new LdcInsnNode("Hello World!"));
//	        mainMethod.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"));
//	        mainMethod.instructions.add(new InsnNode(Opcodes.RETURN));
//
//	        //Add the method to the classNode
//	        classNode.methods.add(mainMethod);
	        
	        //Write the class
	        ClassWriter cw=new ClassWriter(ClassWriter.COMPUTE_MAXS|ClassWriter.COMPUTE_FRAMES);
	        classNode.accept(cw);
		   
	        return classNode;
		   
	   }
	
	

}
