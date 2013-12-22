package com.arthur.mta.core.context;


import java.util.List;



import com.arthur.mta.core.CustomField;
import com.arthur.mta.core.CustomFieldImpl;
import com.arthur.mta.core.CustomObject;
import com.arthur.mta.core.CustomObjectImpl;
import com.arthur.mta.core.MultiTenantUser;
import com.arthur.mta.utbdbservice.CustomObjectController;


 class MTApplicationContext {
	
	private static CustomObjectController ctrl = new CustomObjectController();
	public static int getTenantId() {
		
		MultiTenantUser user = TenantContextKeeper.getContext().getTenantUser();
		return user.getTenantId();
	}
	
	public static CustomObject newCustomObject(){
		
		CustomObject co = new CustomObjectImpl();
		co.setTenantId(MTApplicationContext.getTenantId());
		
		return co;
	}
	
	public static CustomField newCustomField(){
		CustomField cf = new CustomFieldImpl();
		return cf;
	}
	
	public static void saveCustomObjectMetaData(CustomObject customObject){
		ctrl.saveCustomObjectMetaData(customObject);
	}
	
	public static void saveCustomObject(CustomObject customObject){
		ctrl.saveCustomObject(customObject);
	}
	
	public static void deleteCustomObject(CustomObject customObject){
		ctrl.deleteCustomObject(customObject);
	}
	
	public static CustomObject findCustomObjectMetaData(int customObjectId){
		return ctrl.findCustomObjectMetaData(customObjectId);
	}
	
	public static CustomObject findCustomObjectMetaData(String customObjectName){
		return ctrl.findCustomObjectMetaData(customObjectName);
	}
	
	public static List<CustomObject> findCustomObjectsMetaData(){
		return ctrl.findCustomObjectsMetaData();
	}
	
	public static CustomObject findCustomObject(int customObjectId , String pkValue){
		return ctrl.findCustomObject(customObjectId , pkValue);
	}
	
	public static List<CustomObject> findCustomObjects(int customObjectId){
		return ctrl.findCustomObjects(customObjectId );
	}
	
	
	

	
	//private Reflections reflections;
	
	/*
	public void init(){

		Reflections reflections = new Reflections(
			    new ConfigurationBuilder()
			        .setUrls(ClasspathHelper.forJavaClassPath())
			        .setUrls(ClasspathHelper.forPackage(urls))
			);
			
		
	     // Set<Class<?>> annotatedClasses =
	       //        reflections.getTypesAnnotatedWith(MultiTenantable.class);	
		
		Set<String> annotatedClasses =
				reflections.getStore().getTypesAnnotatedWith(MultiTenantable.class.getName());
	      
		buildCustomObject(annotatedClasses);
	      
	
		
	}
	
	
	private void buildCustomObject(Set<String> annotatedClasses){
		
		ClassNode proxyNode = getProxyClassNode();
		
		for (String className : annotatedClasses) {
			
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
						      
						    //Write the class
						    // ClassWriter cw=new ClassWriter(ClassWriter.COMPUTE_MAXS|ClassWriter.COMPUTE_FRAMES);
						    //classNode.accept(cw);
						        
						    //Dump the class in a file
						    File outDir=new File("/"+ this.getClassPathDir(class1));
						    //outDir.mkdirs();
						    DataOutputStream dout=new DataOutputStream(new FileOutputStream(
						    		new File(outDir,class1.getSimpleName() + ".class")));
						    dout.write(cw.toByteArray());
						    dout.flush();
						    dout.close(); 
						    System.out.println("111111112222");
						  
				        }
				      
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			} catch (ClassNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
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
	
	private ClassNode getProxyClassNode(){
		
		Class<?> proxyClass = ProxyCustomObject.class;
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
	
	private boolean isHandled(ClassNode classNode){
		
		for (Object obj :  classNode.interfaces) {
			if(obj.toString().equals(Type.getInternalName(CustomObject.class))){
				return true;
			}
		}

		return false;
		
	}
	
	
	*/
	
}
