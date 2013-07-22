package com.arthur.mta.utbdbservice;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.arthur.mta.core.CustomField;
import com.arthur.mta.core.CustomFieldImpl;
import com.arthur.mta.core.CustomObject;
import com.arthur.mta.core.CustomObjectImpl;
import com.arthur.mta.core.CustomRelationship;
import com.arthur.mta.core.CustomRelationshipImpl;
import com.arthur.mta.core.FieldType;
import com.arthur.mta.core.IndexType;
import com.arthur.mta.core.MultiTenantUser;

import com.arthur.mta.core.context.TenantContextKeeper;
import com.arthur.mta.utbdbservice.boundary.IAssociationDao;
import com.arthur.mta.utbdbservice.boundary.IDataDao;
import com.arthur.mta.utbdbservice.boundary.IFieldDao;
import com.arthur.mta.utbdbservice.boundary.IIndexDao;
import com.arthur.mta.utbdbservice.boundary.IObjectDao;
import com.arthur.mta.utbdbservice.boundary.IUniqueFieldDao;
import com.arthur.mta.utbdbservice.command.Command;
import com.arthur.mta.utbdbservice.command.Delete;
import com.arthur.mta.utbdbservice.command.Insert;
import com.arthur.mta.utbdbservice.command.ObjectProvider;
import com.arthur.mta.utbdbservice.command.Update;
import com.arthur.mta.utbdbservice.domain.Association;
import com.arthur.mta.utbdbservice.domain.Data;
import com.arthur.mta.utbdbservice.domain.Field;
import com.arthur.mta.utbdbservice.domain.Index;
import com.arthur.mta.utbdbservice.domain.UniqueField;
import com.arthur.mta.utbdbservice.util.CTX;
import com.arthur.mta.utbdbservice.util.DateChecker;

public class CustomObjectController {
	
	public void saveCustomObjectMetaData(CustomObject customObject){
		
		IObjectDao dao = (IObjectDao) CTX.get().getBean("objectDao");
		IFieldDao fDao = (IFieldDao) CTX.get().getBean("fieldDao");
		com.arthur.mta.utbdbservice.domain.Object obj =  dao.findByObjectName(customObject.getName());
		
		if(obj == null){
			dao.save(this.transferToObject(customObject));
			obj = dao.findByObjectName(customObject.getName());
			int count = 0;
			for (CustomField cf : customObject.getCustomFields()) {
				
				Field f =this.createField(obj.getObjectId(), count , cf);
				fDao.save(f);
				f = fDao.findByNum(obj.getObjectId(), TenantContextKeeper.getContext().getTenantUser().getTenantId()
						, count);
				if(cf.getType().getValue() == 5){
					Association assoc = doAssociation(cf ,obj, f, dao);
					IAssociationDao aDao = (IAssociationDao) CTX.get().getBean("associationDao");
					aDao.save(assoc);
				}
			
				count+=1;	
			}	
		}else{
			//dao.update(this.transferToObject(customObject));
			Map<String, Field> fields = obj.getFields();
			int baseFieldNum = fields.size() -1;
			for (CustomField cf : customObject.getCustomFields()) {
				boolean isExist = false;
				for(Map.Entry<String, Field> entry : fields.entrySet() ) {
					Field f = entry.getValue();
					if(f.getFieldId() == cf.getId()){
						if(!isFieldDataEqual(f,cf)){
							f = this.updateField(f, cf);
							fDao.update(f);
						}
						isExist = true;
						break;
					}
				}
				
				if(!isExist){
					baseFieldNum = baseFieldNum+1;
					Field f = this.createField(obj.getObjectId(), baseFieldNum , cf);
					fDao.save(f);
					f = fDao.findByNum(obj.getObjectId(), TenantContextKeeper.getContext().getTenantUser().getTenantId()
							, baseFieldNum);
					
					if(cf.getType().getValue() == 5){
						Association assoc = doAssociation(cf ,obj, f, dao);
						IAssociationDao aDao = (IAssociationDao) CTX.get().getBean("associationDao");
						aDao.save(assoc);
					}
			
				}
			}
		}
		
	}
	
	public void saveCustomObjectRelatioinship(CustomObject customObject){
		
		IObjectDao oDao = (IObjectDao) CTX.get().getBean("objectDao");
		IFieldDao fDao = (IFieldDao) CTX.get().getBean("fieldDao");
		IAssociationDao aDao = (IAssociationDao) CTX.get().getBean("associationDao");
		
		for (CustomRelationship cusRel : customObject.getCustomRelationships()) {
			if(cusRel.getId() == 0){
				com.arthur.mta.utbdbservice.domain.Object obj =
						oDao.findByObjectId(cusRel.getDetailObjectId());		
				
				com.arthur.mta.utbdbservice.domain.Object tarObj =
						oDao.findByObjectId(cusRel.getMasterObjectId());	
				int tenantId = TenantContextKeeper.getContext().getTenantUser().getTenantId();
				
				Field msF = new Field();
				msF.setTenantId(tenantId);
				msF.setObjectId(tarObj.getObjectId());
				msF.setFieldName(cusRel.getMasterObjectName()+"_"+cusRel.getDetailObjectName());
				int fNum = tarObj.getFields().size();
				msF.setFieldNum(Integer.toString(fNum));
				msF.setFieldType(FieldType.CustomRelationship);
				msF.setIndexType(IndexType.NotIndexed);
				fDao.save(msF);
				
				Field f = new Field();
				f.setTenantId(tenantId);
				f.setObjectId(obj.getObjectId());
				f.setFieldName(cusRel.getMasterObjectName()+"_"+cusRel.getDetailObjectName());
				fNum = obj.getFields().size();
				f.setFieldNum(Integer.toString(fNum));
				f.setFieldType(FieldType.CustomRelationship);
				f.setIndexType(IndexType.NotIndexed);
				fDao.save(f);
				f = fDao.findByNum(cusRel.getDetailObjectId(), tenantId, fNum);
				
				
				Association aoc = new Association();
				aoc.setTenantId(tenantId);
				aoc.setObjectId(cusRel.getDetailObjectId());
				aoc.setFieldId(f.getFieldId());
				aoc.setFieldNum(Integer.parseInt(f.getFieldNum()));
				aoc.setTargetObjectId(cusRel.getMasterObjectId());
				f = tarObj.findPkField();
				aoc.setTargetFieldId(f.getFieldId());
				aoc.setTargetFieldNum(Integer.parseInt(f.getFieldNum()));
				aDao.save(aoc);
				
			}
		}
		
	}
	
	public void saveCustomObject(CustomObject customObject){
		
		ObjectProvider op = new ObjectProvider(customObject);
		Command cmd = null;
		
		CustomField pkField = customObject.getPkCustomField();
		if(pkField.getValue() != null && pkField.getValue().length() != 0){
			cmd = new Update(op);
		}else{
			cmd = new Insert(op);
		}
		
		cmd.excute();

	}
	
	public void deleteCustomObject(CustomObject customObject){
		
		ObjectProvider op = new ObjectProvider(customObject);
		Command cmd = new Delete(op);
		cmd.excute();
	}
	
	public CustomObject findCustomObjectMetaData(int customObjectId){
		
		IObjectDao dao = (IObjectDao) CTX.get().getBean("objectDao");
		return this.transferToCustomObject(dao.findByObjectId(customObjectId));
		
	}
	
	public CustomObject findCustomObjectMetaData(String customObjectName){
		
		IObjectDao dao = (IObjectDao) CTX.get().getBean("objectDao");
		return this.transferToCustomObject(dao.findByObjectName(customObjectName));
		
	}
	
	public List<CustomObject> findCustomObjectsMetaData(){
		
		IObjectDao dao = (IObjectDao) CTX.get().getBean("objectDao");
		List<CustomObject> cos = new ArrayList<CustomObject>();
		List<com.arthur.mta.utbdbservice.domain.Object> objs = dao.findCustomObjects();
		for (com.arthur.mta.utbdbservice.domain.Object object : objs) {
			cos.add( this.transferToCustomObject(object));
		}
		
		return cos;
		
	}
	
	public CustomObject findCustomObject(int customObjectId , String pkValue){
		
		MultiTenantUser user = TenantContextKeeper.getContext().getTenantUser();
		IObjectDao oDao = (IObjectDao) CTX.get().getBean("objectDao");
		CustomObject co = this.transferToCustomObject(oDao.findByObjectId(customObjectId));
		
		CustomField pkField = co.getPkCustomField();
		IUniqueFieldDao uDao = (IUniqueFieldDao) CTX.get().getBean("uniqueFieldDao");
		UniqueField uf = 
				uDao.findByStringValue(user.getTenantId(), customObjectId, pkField.getFieldNum(),pkValue);
		
		IDataDao dao = (IDataDao) CTX.get().getBean("dataDao");
		Data data = dao.findByDataGuid(uf.getDataGuid());

		return this.transferDataToCustomObject(data, co);
		
	}
	
	public CustomObject findCustomObject(String dataGuid){
		
		MultiTenantUser user = TenantContextKeeper.getContext().getTenantUser();
		IDataDao dao = (IDataDao) CTX.get().getBean("dataDao");
		Data data = dao.findByDataGuid(dataGuid);
		
		IObjectDao oDao = (IObjectDao) CTX.get().getBean("objectDao");
		CustomObject co = this.transferToCustomObject(oDao.findByObjectId(data.getObjectId()));
		
		return this.transferDataToCustomObject(data, co);
		
	}	
	public List<CustomObject> findCustomObjects(int customObjectId){
		
		IObjectDao oDao = (IObjectDao) CTX.get().getBean("objectDao");
		com.arthur.mta.utbdbservice.domain.Object sourceObj = oDao.findByObjectId(customObjectId);
		
		IDataDao dao = (IDataDao) CTX.get().getBean("dataDao");
		List<Data> dataList = dao.findByObjectId(customObjectId);
		List<CustomObject> cos = new ArrayList<CustomObject>();
		for (Data data2 : dataList) {
			CustomObject co = this.transferToCustomObject(sourceObj);
			cos.add(this.transferDataToCustomObject(data2, co));
		}
		 
		return cos;
	}
	
	public CustomObject findCustomObjectRelationships(int customObjectId){
		
		IObjectDao oDao = (IObjectDao) CTX.get().getBean("objectDao");
		com.arthur.mta.utbdbservice.domain.Object sourceObj =
				oDao.findByObjectId(customObjectId);
		CustomObject co = this.transferToCustomObject(sourceObj);
		
	
		IAssociationDao aDao = 
				(IAssociationDao) CTX.get().getBean("associationDao");
		List<Association> aList = aDao.find(customObjectId);
		for (Association association : aList) {
			int [] objectIds = new int []{association.getObjectId() , association.getTargetObjectId()};
			List<com.arthur.mta.utbdbservice.domain.Object> objs = 
					oDao.findByObjectIds(objectIds);
			co.addCustomRelationship(this.createCustomRelationship(association, objs));
		}
		
		return co;
	}
	
	public  List<CustomObject> findCustomObjectsByIndexes(int customObjectId , String value){
		
		
		IIndexDao iDao = (IIndexDao) CTX.get().getBean("indexDao");
		List<Index> iFields = null;
		IObjectDao oDao = (IObjectDao) CTX.get().getBean("objectDao");
		com.arthur.mta.utbdbservice.domain.Object object =
				oDao.findByObjectId(customObjectId);
		
		StringBuilder result = new StringBuilder();
		for(Map.Entry<String, Field> entry : object.getFields().entrySet()) {
			
		    Field field = entry.getValue();
		    if(field.getIndexType().getValue() == 2){
		    	if(field.getFieldType().getValue() == 0 || field.getFieldType().getValue() == 3 || field.getFieldType().getValue() ==4)
					iFields = iDao.findByStringValue( object.getObjectId(), field.getFieldNum(),value,"=");
				
				if(field.getFieldType().getValue() == 1)
					iFields = iDao.findByNumValue( object.getObjectId(), field.getFieldNum(),value,"=");
				
				if(field.getFieldType().getValue() == 2){
					iFields = iDao.findByDateValue(object.getObjectId(), field.getFieldNum(),value,"=");
				}
				
				if(iFields != null ){
					if(iFields.size() > 0){
						StringBuilder result2 = new StringBuilder();
						result2.append(" DataGuid in (");
						for (Index index : iFields) {
							result2.append("'"+index.getDataGuid() + "',");
						}
						
						String indexClause = result2.toString();
						result.append(indexClause.substring(0,indexClause.length()-1) + ") OR ");
					}
				}
		    }
		}
		
		List<CustomObject> cos = new ArrayList<CustomObject>();
		String indexexClause = result.toString();
		if(indexexClause !=null && !indexexClause.isEmpty()){
			indexexClause = "("+indexexClause.substring(0 , indexexClause.length()-3) + ")";
			
			IDataDao dao = (IDataDao) CTX.get().getBean("dataDao");
			List<Data> dataList = dao.findByIndexesDataGuid(object.getObjectId(), indexexClause);
			
			for (Data data2 : dataList) {
				CustomObject co = this.transferToCustomObject(object);
				cos.add(this.transferDataToCustomObject(data2, co));
			}
		}
		
		return cos;
	}
	
	protected CustomObject findCustomObject2(String dataGuid){
		
		MultiTenantUser user = TenantContextKeeper.getContext().getTenantUser();
		IDataDao dao = (IDataDao) CTX.get().getBean("dataDao");
		Data data = dao.findByDataGuid(dataGuid);
		
		IObjectDao oDao = (IObjectDao) CTX.get().getBean("objectDao");
		CustomObject co = this.transferToCustomObject(oDao.findByObjectId(data.getObjectId()));
		
		return this.transferDataToCustomObject2(data, co);
		
	}
	
	protected CustomObject findCustomObject2(int customObjectId , String pkValue){
		
		MultiTenantUser user = TenantContextKeeper.getContext().getTenantUser();
		IObjectDao oDao = (IObjectDao) CTX.get().getBean("objectDao");
		CustomObject co = this.transferToCustomObject(oDao.findByObjectId(customObjectId));
		
		CustomField pkField = co.getPkCustomField();
		IUniqueFieldDao uDao = (IUniqueFieldDao) CTX.get().getBean("uniqueFieldDao");
		UniqueField uf = 
				uDao.findByStringValue(user.getTenantId(), customObjectId, pkField.getFieldNum(),pkValue);
		if(uf !=null){
			IDataDao dao = (IDataDao) CTX.get().getBean("dataDao");
			Data data = dao.findByDataGuid(uf.getDataGuid());

			return this.transferDataToCustomObject2(data, co);
		}
	
		return null;
	}
	
	
	private com.arthur.mta.core.CustomRelationship createCustomRelationship(Association association , 
			List<com.arthur.mta.utbdbservice.domain.Object> objs ){
		
		CustomRelationship cusRel = new CustomRelationshipImpl(association.getAssocationId() , association.getTargetObjectId()
				,  association.getObjectId());
		
		for (com.arthur.mta.utbdbservice.domain.Object obj : objs) {
			if(obj.getObjectId() == association.getObjectId())
				cusRel.setDetailObjectName(obj.getObjectName());
			
			if(obj.getObjectId() == association.getTargetObjectId())
				cusRel.setMasterObjectName(obj.getObjectName());
		}
		
		return cusRel;
	}
		
	private com.arthur.mta.utbdbservice.domain.Object transferToObject(CustomObject customObject){
		
		com.arthur.mta.utbdbservice.domain.Object obj = 
				new com.arthur.mta.utbdbservice.domain.Object();
		if(customObject.getId() != 0) obj.setObjectId(customObject.getId()); 
	    obj.setTenantId(customObject.getTenantId());
	    obj.setObjectName(customObject.getName());
	    obj.setIsDefault('0');
	    
	    return obj;
		
	}
	
	private CustomObject transferToCustomObject(com.arthur.mta.utbdbservice.domain.Object object){
		
		CustomObject co = new CustomObjectImpl();
		co.setId(object.getObjectId()); 
		co.setTenantId(object.getTenantId());
		co.setName(object.getObjectName());
		
		for(Map.Entry<String, Field> entry : object.getFields().entrySet() ) {
			CustomField cf = new CustomFieldImpl();
			Field f = entry.getValue();
			cf.setId(f.getFieldId());
			cf.setName(f.getFieldName());
			cf.setFieldNum(f.getFieldNum());
			cf.setType(f.getFieldType());
			cf.setIndexType(f.getIndexType());
			co.addCustomField(cf);
		}

	    return co;
		
	}
	
	private CustomObject transferDataToCustomObject(Data data , CustomObject co){
		
		String pkVal = "";
		int pkFieldId =0;
		ObjectController ctrl = new ObjectController();
		Class<?> c = data.getClass();
		for(CustomField cf : co.getCustomFields()) {
			
			try {
				String fieldName = "value"+cf.getFieldNum();
				java.lang.reflect.Field privateField = c.getDeclaredField(fieldName);
				privateField.setAccessible(true);
				cf.setValue(privateField.get(data).toString());
				if(cf.getIndexType().getValue() == 1){
					pkVal = cf.getValue();
					pkFieldId = cf.getId();
				}
				if(cf.getType().getValue() ==2){
					Date date = DateChecker.isValidDate(cf.getValue(),"yyyy-MM-dd HH:mm:ss");
					if(date!=null){
						 DateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
						 cf.setValue(sdf.format(date));
					}
				}
				if(cf.getType().getValue() == 5){
					String [] ary = cf.getName().split("_");
					if(ary[0].equals(co.getName())){
						cf.setValues(ctrl.retrieveDetailCustomObjectsByTargetObjIdFieldId(co.getId(), pkFieldId, pkVal , 
								cf.getValue(),ary[1]));
					}else{
						cf.setValues(ctrl.retrieveMasterCustomObjectByObjIdFieldId(co.getId(), cf.getId(), cf.getValue()));
					}

				}
				
			} catch (NoSuchFieldException e) {
				e.printStackTrace();
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}

	    return co;

	}
	
	private CustomObject transferDataToCustomObject2(Data data , CustomObject co){
		

		ObjectController ctrl = new ObjectController();
		Class<?> c = data.getClass();
		for(CustomField cf : co.getCustomFields()) {
			
			try {
				String fieldName = "value"+cf.getFieldNum();
				java.lang.reflect.Field privateField = c.getDeclaredField(fieldName);
				privateField.setAccessible(true);
				if(cf.getType().getValue() == 2){
					String val = privateField.get(data).toString();
					Date date = DateChecker.isValidDate(val,"yyyy-MM-dd HH:mm:ss");
					if(date!=null){
						 DateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
						 cf.setValue(sdf.format(date));
					}
				}else{
					cf.setValue(privateField.get(data).toString());
				}

			} catch (NoSuchFieldException e) {
				e.printStackTrace();
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}

	    return co;

	}
	
	
	private com.arthur.mta.utbdbservice.domain.Field updateField(Field field , CustomField customField){
		
		field.setFieldName(customField.getName()); 
		field.setFieldType(customField.getType() ); 
		field.setIndexType(customField.getIndexType()); 
	    
	    return field;
		
	}
	
	private com.arthur.mta.utbdbservice.domain.Field createField(int objectId , int fieldNum , CustomField customField){
		MultiTenantUser user = TenantContextKeeper.getContext().getTenantUser();
		Field f = new Field();
		f.setObjectId(objectId); 
		f.setTenantId(user.getTenantId()); 
		f.setFieldName(customField.getName()); 
		f.setFieldType(customField.getType());
		f.setIndexType(customField.getIndexType());
		f.setFieldNum(Integer.toString(fieldNum)); 

	    return f;
		
	}
	
	private boolean isFieldDataEqual(Field field , CustomField  customField){
		
		String cfData = customField.getName() +"_"+customField.getType().getValue()+"_"+customField.getIndexType().getValue();
		String fData = field.getFieldName() +"_"+field.getFieldType().getValue() +"_"+ field.getIndexType().getValue();
		if(fData.equals(cfData)){
			return true;
		}else{
			return false;
		}

	}
	
	private Association doAssociation(CustomField cf ,com.arthur.mta.utbdbservice.domain.Object targetObj ,
			Field targetField ,IObjectDao oDao){
		
		com.arthur.mta.utbdbservice.domain.Object sourceObj = 
				oDao.findByObjectId(Integer.parseInt(cf.getValue()));
		
		Field pkF = sourceObj.findPkField();
		
		Association association = new Association();
		
		association.setTenantId(TenantContextKeeper.getContext().getTenantUser().getTenantId());
		association.setObjectId(sourceObj.getObjectId());
		association.setTargetObjectId(targetObj.getObjectId()); 
		association.setFieldId(pkF.getFieldId());
		association.setTargetFieldId(targetField.getFieldId());
		association.setFieldNum(Integer.parseInt(pkF.getFieldNum())); 
		association.setTargetFieldNum(Integer.parseInt(targetField.getFieldNum()));
		
		
		return association;
	}
	
	
	
	
}
