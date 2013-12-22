package com.arthur.shoppingmall;

import com.arthur.shoppingmall.domain.Tenant;
import com.arthur.shoppingmall.view.UserDto;

public interface IUserSrv {
	
	UserDto getUser(int userId);
	
	Tenant getTenant(int tenantId);

}
