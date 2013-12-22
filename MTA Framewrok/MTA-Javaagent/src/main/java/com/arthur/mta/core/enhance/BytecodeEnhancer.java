package com.arthur.mta.core.enhance;


public class BytecodeEnhancer {
	/*
	public Class<?> enhance(String className){
		
		Class class1;
			
		try {
				class1 = Class.forName(className);
				InputStream is = class1.getClassLoader().getResourceAsStream(
						class1.getName().replace('.', '/') + ".class");
		
			    ClassReader cr;
			    ClassWriter cw = new ClassWriter(0);
			    ClassVisitor checkAdapter = new MyClassAdapter(new CheckClassAdapter(cw));
			    SimpleClassLoader loader = new SimpleClassLoader();
			    
					try {
						cr = new ClassReader(is);
					    ClassNode classNode=new ClassNode();
					    cr.accept(classNode, (int)ClassReader.SKIP_FRAMES);
				       
				        if(!isHandled(classNode)){
				        	
				        	ClassNode msNode = getMergeSourceClassNode();
				        	ClassNode classNode2=new ClassNode();
				        	//classNode2.interfaces.clear();
				            classNode2.interfaces.add(Type.getInternalName(CustomObject.class));
					        MergeAdapter ma = new MergeAdapter(Opcodes.ASM4 ,classNode2 ,msNode );
					        cr.accept(ma,(int)ClassReader.SKIP_FRAMES);
					        //ma.visitEnd();
						   // TraceClassVisitor checkAdapter = new TraceClassVisitor(cw,new PrintWriter(System.out));
						    classNode2.accept(checkAdapter);
						    //Dump the class in a file
						    File outDir=new File("/"+ this.getClassPathDir(class1));
						    DataOutputStream dout=new DataOutputStream(new FileOutputStream(
						    		new File(outDir,class1.getSimpleName() + ".class")));
						    dout.write(cw.toByteArray());
						    dout.flush();
						    dout.close();
						     
						  
						    try {
					        	 //return  loader.defineClass(classNode2.name.replace('/', '.'),
					  	            //   cw.toByteArray());
						    	
						    	//ClassLoader parentClassLoader = StubClassLoader.class.getClassLoader();
						    	//StubClassLoader sLoader = new StubClassLoader(parentClassLoader, cw);
						    	//sLoader.loadClass(classNode2.name.replace('/', '.'));
						    	
						    	 //new CustomClassLoader();
						    	 //ClassLoader test = Thread.currentThread().getContextClassLoader();
						    	 //CustomClassLoader t =(CustomClassLoader) test;
						    	CustomClassLoader t =new CustomClassLoader();
						         Class<?> newCls= t.loadClass(classNode2.name.replace('/', '.'));
					        	 return newCls;
						      } catch (Exception ex) {
						         throw new RuntimeException(ex);
						      }
						    
				        }else{
				        	 classNode.accept(checkAdapter);
				        	 return  loader.defineClass(classNode.name.replace('/', '.'),
					  	               cw.toByteArray());
				        }
				      
					} catch (IOException e) {
						e.printStackTrace();
					}
			} catch (ClassNotFoundException e1) {
				e1.printStackTrace();	
			}
		
		return null;
		
	}
	
	public boolean buildCustomObject(String className){
		
		boolean isSuccess = true;
		ClassNode proxyNode = getMergeSourceClassNode();
		Class class1;
			
		try {
				class1 = Class.forName(className);
				InputStream is = class1.getClassLoader().getResourceAsStream(
						class1.getName().replace('.', '/') + ".class");
		
			    ClassReader cr;

					try {
						cr = new ClassReader(is);
					    ClassNode classNode=new ClassNode();
					    cr.accept(classNode, (int)ClassReader.SKIP_FRAMES);
				        //ClassNode is a ClassVisitor
				        // if interface exist?
				        //classNode.interfaces.add(Type.getInternalName(CustomObject.class));
				       // classNode.visitEnd();
				        //Set<String> ints = new HashSet<String>();
				        //ints.add(Type.getInternalName(CustomObject.class).toString());
				        //InterfaceAdder ia = new InterfaceAdder(Opcodes.ASM4, classNode, ints);
				        //cr.accept(ia,(int)ClassReader.SKIP_FRAMES);
				        //ia.visitEnd();
				        if(!isHandled(classNode)){
				        	
				        	ClassNode classNode2=new ClassNode();
				        	//classNode2.interfaces.clear();
				            classNode2.interfaces.add(Type.getInternalName(CustomObject.class));
					        MergeAdapter ma = new MergeAdapter(Opcodes.ASM4 ,classNode2 ,proxyNode );
					        cr.accept(ma,(int)ClassReader.SKIP_FRAMES);
					        //ma.visitEnd();
					        
					        ClassWriter cw = new ClassWriter(0);
						    ClassVisitor checkAdapter = new MyClassAdapter(new CheckClassAdapter(cw));
						   // TraceClassVisitor checkAdapter = new TraceClassVisitor(cw,
						    //new PrintWriter(System.out));
						    classNode2.accept(checkAdapter);
						     
						        
						    //Dump the class in a file
						    File outDir=new File("/"+ this.getClassPathDir(class1));
						    //outDir.mkdirs();
						    DataOutputStream dout=new DataOutputStream(new FileOutputStream(
						    		new File(outDir,class1.getSimpleName() + ".class")));
						    dout.write(cw.toByteArray());
						    dout.flush();
						    dout.close();
				        }
				      
					} catch (IOException e) {
						e.printStackTrace();
						isSuccess = false;
					}
			} catch (ClassNotFoundException e1) {
				e1.printStackTrace();
				isSuccess = false;
			}
		
		return isSuccess;
		
	}
	
	private ClassNode getMergeSourceClassNode(){
		
		Class<?> proxyClass = ProxyCustomObject.class;
		//Class<?> proxyClass = ProxyCustomObject.class;
		InputStream proxyClassIs = proxyClass.getClassLoader().getResourceAsStream(
				proxyClass.getName().replace('.', '/') + ".class");
        ClassReader proxyCr = null;
        ClassNode classNode=new ClassNode(); 
        try {
			proxyCr = new ClassReader(proxyClassIs);
			proxyCr.accept(classNode, (int)ClassReader.SKIP_FRAMES);
	        classNode.visitEnd();
			
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

        return classNode;
		
	}
	
	private String getClassPathDir(Class<?> type){
		
		String [] ary = type.getClassLoader().getResource(
				type.getName().replace('.', '/') + ".class").toString().split("/");
		String path = "";
		for (int i = 0; i < ary.length; i++) {
			if(i == ary.length -1)
				break;
			if(i!=0)path += ary[i]+ "/";
		}
		
		return path;
		
		
	}
	
	private boolean isHandled(ClassNode classNode){
		
		for (Object obj :  classNode.interfaces) {
			if(obj.toString().equals(Type.getInternalName(CustomObject.class))){
				return true;
			}
		}

		return false;
		
	}
	
	private void createSetter(String propertyName, String type, Class c ,String className , ClassWriter cw) {
		  String methodName = "set" + propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
		  MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, methodName, "(" + type + ")V", null, null);
		  mv.visitVarInsn(Opcodes.ALOAD, 0);
		  mv.visitVarInsn(Type.getType(c).getOpcode(Opcodes.ILOAD), 1);
		  mv.visitFieldInsn(Opcodes.PUTFIELD, className, propertyName, type);
		  mv.visitInsn(Opcodes.RETURN);
		  mv.visitMaxs(0, 0);
		}

	private void createGetter(String propertyName, String returnType, Class c , String internalClassName , ClassWriter cw) {
		  String methodName = "get" + propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
		  MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, methodName, "()" + returnType, null, null);
		  mv.visitVarInsn(Opcodes.ALOAD, 0);
		  mv.visitFieldInsn(Opcodes.GETFIELD, internalClassName, propertyName, returnType);
		  mv.visitInsn(Type.getType(c).getOpcode(Opcodes.IRETURN));
		  mv.visitMaxs(0, 0);
	}
	*/

}
