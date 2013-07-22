package com.arthur.mta.core.enhance;


import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.Iterator;
import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.RemappingMethodAdapter;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.CheckClassAdapter;

import com.arthur.mta.core.CustomObject;
import com.arthur.mta.core.ProxyCustomObject;

public class MTAClassFileTransformer implements ClassFileTransformer {
	
	 //static final Logger logger = LoggerFactory.getLogger(MTAClassFileTransformer.class);

	    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
	            ProtectionDomain protectionDomain, byte[] classfileBuffer)
	            throws IllegalClassFormatException {
	        //logger.info("class file transformer invoked for className: {}", className);

	    	//System.out.println("class file transformer invoked for className: {} " + className);
	    	InputStream in = loader.getResourceAsStream(className.replace('.', '/') + ".class");
	        if (in != null) {
	        	
	        	ClassReader cr = null ;
	        	ClassReader cr2 = null ;
	        	try {
	        		
	        		cr = new ClassReader(in); 
					ClassNode classNode=new ClassNode();
					cr.accept(classNode, (int)ClassReader.SKIP_FRAMES);
				
					if(checkAnnoations(classNode.visibleAnnotations)){
						
						System.out.println("This is  my" + className + "!!!!!!!!!!!!!");
						
						InputStream is = ProxyCustomObject.class.getClassLoader().getResourceAsStream(
			        			ProxyCustomObject.class.getName().replace('.', '/') + ".class");
		        		
		        		 cr2 = new ClassReader(is);
		        		 ClassNode msNode = new ClassNode();
						 cr2.accept(msNode, (int)ClassReader.SKIP_FRAMES);
						 //msNode.visibleAnnotations
					
						 //ClassNode classNode2=new ClassNode();
						 //cr.accept(classNode, (int)ClassReader.SKIP_FRAMES);
						 classNode.interfaces.add(Type.getInternalName(CustomObject.class));
						 
						 for(Iterator<?> it = msNode.fields.iterator();it.hasNext();){
								FieldNode fn = ((FieldNode)it.next());
								fn.accept(classNode);
						 }
						 
						 for(Iterator<?> it = msNode.methods.iterator();it.hasNext();){
								MethodNode mn = (MethodNode)it.next();
								if(!mn.name.equals("<init>")){
									//System.out.println(mn.name);
									String[] exceptions = 
											new String[mn.exceptions.size()];
									mn.exceptions.toArray(exceptions);
									MethodVisitor mv = classNode.visitMethod(
											mn.access, mn.name, mn.desc, mn.signature, exceptions);
									mn.instructions.resetLabels();
									
									mn.accept(new RemappingMethodAdapter(
											mn.access,mn.desc,mv, new SimpleRemapper(msNode.name , className )));
								}
								
							}
						 
					     classNode.visitEnd();
					   
					    // MergeAdapter ma = new MergeAdapter(Opcodes.ASM4 ,classNode ,msNode );
					        //cr.accept(ma,(int)ClassReader.SKIP_FRAMES);
					     
					     ClassWriter cw = new ClassWriter(0);
						 ClassVisitor checkAdapter = new MyClassAdapter(new CheckClassAdapter(cw));
						   // TraceClassVisitor checkAdapter = new TraceClassVisitor(cw,
						    //new PrintWriter(System.out));
						 classNode.accept(checkAdapter);
					     
						 //Dump the class in a file
						   // File outDir=new File("/home/arthur/");
						    //outDir.mkdirs();
						    //DataOutputStream dout=new DataOutputStream(new FileOutputStream(
						    	//	new File(outDir, "aa.class")));
						   //dout.write(cw.toByteArray());
						    //dout.flush();
						    //dout.close();
						    System.out.println("111111111111111111");
						 
						 
						  return cw.toByteArray();
						
					}
	        	 
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	         
	        }

	        return classfileBuffer;
	    }
	    
	    private boolean checkAnnoations(List<?> visibleAnnotations){

			for (Object o : visibleAnnotations) {
				AnnotationNode aNode = (AnnotationNode)o;
				if(aNode.desc.equals("Lcom/arthur/mta/core/annotations/MultiTenantable;")){
					return true;
				}
			}
			
			return false;
	    	
	    }

}
