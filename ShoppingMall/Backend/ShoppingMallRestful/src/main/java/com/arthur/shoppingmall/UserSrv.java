package com.arthur.shoppingmall;

import org.springframework.stereotype.Component;

import com.arthur.shoppingmall.boundary.TenantRepo;
import com.arthur.shoppingmall.boundary.UserRepo;
import com.arthur.shoppingmall.domain.Tenant;
import com.arthur.shoppingmall.domain.User;
import com.arthur.shoppingmall.view.UserDto;

@Component
public class UserSrv implements IUserSrv{

	@Override
	public UserDto getUser(int userId) {
		
		UserRepo uReop = new UserRepo(); 
		User u = uReop.getUser(userId);
		
		TenantRepo tRepo = new TenantRepo();
		Tenant t = tRepo.getTenant(u.getTenantId());

		return new UserDto(u.getId(), u.getTenantId() , t.getTenantName());
	}

	@Override
	public Tenant getTenant(int tenantId) {
		TenantRepo tRepo = new TenantRepo();
		return tRepo.getTenant(tenantId);
	}
	
}
