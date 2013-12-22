package com.arthur.shoppingmall.domain;

import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;



import com.arthur.mta.core.MultiTenantUser;



@PersistenceCapable(identityType =IdentityType.DATASTORE , table="Users")
public class User implements MultiTenantUser  {

    private static final long serialVersionUID = 1L;

    @PrimaryKey
    private Integer id;
    @Persistent
    private String name;
    @Persistent
    private String username;
    @Persistent
    private Integer tenantid;
    
    //private String password;


    public User(String name, String password , Integer userId , int tenantId) {
        this.name = name;
      //  this.password = password;
        this.id = userId;
        this.tenantid = tenantId;
        
    }

  
    public String getPassword() {
        // TODO Auto-generated method stub
       // return password;
    	return "";
    }

    public String getUsername() {
        // TODO Auto-generated method stub
        return name;
    }

    public Integer getId() {
		return id;
	}

	public boolean isAccountNonExpired() {
        // TODO Auto-generated method stub
        return true;
    }


    public boolean isAccountNonLocked() {
        // TODO Auto-generated method stub
        return true;
    }

    
    public boolean isCredentialsNonExpired() {
        // TODO Auto-generated method stub
        return true;
    }


    public boolean isEnabled() {
        // TODO Auto-generated method stub
        return true;
    }

    
	@Override
	public int getTenantId() {
		return tenantid;
	}

	
}
