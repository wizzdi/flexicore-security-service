package com.wizzdi.flexicore.security.service;

import com.flexicore.model.*;
import com.flexicore.security.SecurityContextBase;
import com.wizzdi.flexicore.boot.base.interfaces.Plugin;
import com.wizzdi.flexicore.security.data.TenantToUserRepository;
import com.wizzdi.flexicore.security.request.TenantToUserCreate;
import com.wizzdi.flexicore.security.request.TenantToUserFilter;
import com.wizzdi.flexicore.security.request.TenantToUserUpdate;
import com.wizzdi.flexicore.security.response.PaginationResponse;
import org.pf4j.Extension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import java.util.*;
import java.util.stream.Collectors;

@Extension
@Component
public class TenantToUserService implements Plugin {

	@Autowired
	private BaselinkService baselinkService;
	@Autowired
	private TenantToUserRepository tenantToUserRepository;


	public TenantToUser createTenantToUser(TenantToUserCreate tenantToUserCreate, SecurityContextBase securityContext){
		TenantToUser tenantToUser= createTenantToUserNoMerge(tenantToUserCreate,securityContext);
		tenantToUserRepository.merge(tenantToUser);
		return tenantToUser;
	}

	public <T> T merge(T o){
		return tenantToUserRepository.merge(o);
	}
	public void massMerge(List<Object> list){
		tenantToUserRepository.massMerge(list);
	}
	public <T extends Baseclass> List<T> listByIds(Class<T> c,Set<String> ids,  SecurityContextBase securityContext) {
		return tenantToUserRepository.listByIds(c, ids, securityContext);
	}

	public TenantToUser createTenantToUserNoMerge(TenantToUserCreate tenantToUserCreate, SecurityContextBase securityContext){
		TenantToUser tenantToUser=new TenantToUser(tenantToUserCreate.getName(),securityContext);
		updateTenantToUserNoMerge(tenantToUserCreate,tenantToUser);
		tenantToUserRepository.merge(tenantToUser);
		return tenantToUser;
	}

	public boolean updateTenantToUserNoMerge(TenantToUserCreate tenantToUserCreate, TenantToUser tenantToUser) {
		boolean update = baselinkService.updateBaselinkNoMerge(tenantToUserCreate, tenantToUser);
		if(tenantToUserCreate.getSecurityUser()!=null&&(tenantToUser.getRightside()==null||!tenantToUserCreate.getSecurityUser().getId().equals(tenantToUser.getRightside().getId()))){
			tenantToUser.setRightside(tenantToUserCreate.getSecurityUser());
			update=true;
		}
		if(tenantToUserCreate.getTenant()!=null&&(tenantToUser.getLeftside()==null||!tenantToUserCreate.getTenant().getId().equals(tenantToUser.getLeftside().getId()))){
			tenantToUser.setLeftside(tenantToUserCreate.getTenant());
			update=true;
		}

		if(tenantToUserCreate.getDefaultTenant()!=null&&!tenantToUserCreate.getDefaultTenant().equals(tenantToUser.isDefualtTennant())){
			tenantToUser.setDefualtTennant(tenantToUserCreate.getDefaultTenant());
			update=true;
		}
		return update;
	}

	public TenantToUser updateTenantToUser(TenantToUserUpdate tenantToUserUpdate, SecurityContextBase securityContext){
		TenantToUser tenantToUser=tenantToUserUpdate.getTenantToUser();
		if(updateTenantToUserNoMerge(tenantToUserUpdate,tenantToUser)){
			tenantToUserRepository.merge(tenantToUser);
		}
		return tenantToUser;
	}

	@Deprecated
	public void validate(TenantToUserCreate tenantToUserCreate, SecurityContextBase securityContext) {
		baselinkService.validate(tenantToUserCreate,securityContext);
	}

	@Deprecated
	public void validate(TenantToUserFilter tenantToUserFilter, SecurityContextBase securityContext) {
		baselinkService.validate(tenantToUserFilter,securityContext);
		Set<String> tenantsIds=tenantToUserFilter.getTenantsIds();
		Map<String, SecurityTenant> securityTenantMap=tenantsIds.isEmpty()?new HashMap<>():listByIds(SecurityTenant.class,tenantsIds,securityContext).stream().collect(Collectors.toMap(f->f.getId(), f->f));
		tenantsIds.removeAll(securityTenantMap.keySet());
		if(!tenantsIds.isEmpty()){
			throw new HttpClientErrorException(HttpStatus.BAD_REQUEST,"no SecurityTenant with ids "+tenantsIds);
		}
		tenantToUserFilter.setSecurityTenants(new ArrayList<>(securityTenantMap.values()));

		Set<String> usersIds=tenantToUserFilter.getUsersIds();
		Map<String, SecurityUser> userMap=usersIds.isEmpty()?new HashMap<>():listByIds(SecurityUser.class,usersIds,securityContext).stream().collect(Collectors.toMap(f->f.getId(), f->f));
		usersIds.removeAll(userMap.keySet());
		if(!usersIds.isEmpty()){
			throw new HttpClientErrorException(HttpStatus.BAD_REQUEST,"no security users with ids "+usersIds);
		}
		tenantToUserFilter.setSecurityUsers(new ArrayList<>(userMap.values()));
	}

	public <T extends Baseclass> T getByIdOrNull(String id,Class<T> c, SecurityContextBase securityContext) {
		return tenantToUserRepository.getByIdOrNull(id,c,securityContext);
	}

	public PaginationResponse<TenantToUser> getAllTenantToUsers(TenantToUserFilter tenantToUserFilter, SecurityContextBase securityContext) {
		List<TenantToUser> list= listAllTenantToUsers(tenantToUserFilter, securityContext);
		long count=tenantToUserRepository.countAllTenantToUsers(tenantToUserFilter,securityContext);
		return new PaginationResponse<>(list,tenantToUserFilter,count);
	}

	public List<TenantToUser> listAllTenantToUsers(TenantToUserFilter tenantToUserFilter, SecurityContextBase securityContext) {
		return tenantToUserRepository.listAllTenantToUsers(tenantToUserFilter, securityContext);
	}

	public <T extends Baseclass> List<T> findByIds(Class<T> c, Set<String> requested) {
		return tenantToUserRepository.findByIds(c, requested);
	}

	public <T> T findByIdOrNull(Class<T> type, String id) {
		return tenantToUserRepository.findByIdOrNull(type, id);
	}
}
