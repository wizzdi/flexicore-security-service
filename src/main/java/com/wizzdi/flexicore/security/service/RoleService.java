package com.wizzdi.flexicore.security.service;

import com.flexicore.model.Baseclass;
import com.flexicore.model.Role;
import com.wizzdi.flexicore.boot.base.interfaces.Plugin;
import com.wizzdi.flexicore.security.data.RoleRepository;
import com.flexicore.security.SecurityContextBase;
import com.wizzdi.flexicore.security.request.RoleCreate;
import com.wizzdi.flexicore.security.request.RoleFilter;
import com.wizzdi.flexicore.security.request.RoleUpdate;
import com.wizzdi.flexicore.security.response.PaginationResponse;
import org.pf4j.Extension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Extension
@Component
public class RoleService implements Plugin {

	@Autowired
	private SecurityEntityService securityEntityService;
	@Autowired
	private RoleRepository roleRepository;


	public Role createRole(RoleCreate roleCreate, SecurityContextBase securityContext){
		Role role= createRoleNoMerge(roleCreate,securityContext);
		roleRepository.merge(role);
		return role;
	}

	public void merge(Object o){
		roleRepository.merge(o);
	}
	public void massMerge(List<Object> list){
		roleRepository.massMerge(list);
	}

	public <T extends Baseclass> List<T> listByIds(Class<T> c,Set<String> ids,  SecurityContextBase securityContext) {
		return roleRepository.listByIds(c, ids, securityContext);
	}


	public Role createRoleNoMerge(RoleCreate roleCreate, SecurityContextBase securityContext){
		Role role=new Role(roleCreate.getName(),securityContext);
		updateRoleNoMerge(roleCreate,role);
		roleRepository.merge(role);
		return role;
	}

	public boolean updateRoleNoMerge(RoleCreate roleCreate, Role role) {
		return securityEntityService.updateNoMerge(roleCreate,role);
	}

	public Role updateRole(RoleUpdate roleUpdate, SecurityContextBase securityContext){
		Role role=roleUpdate.getRole();
		if(updateRoleNoMerge(roleUpdate,role)){
			roleRepository.merge(role);
		}
		return role;
	}

	public void validate(RoleCreate roleCreate, SecurityContextBase securityContext) {
		securityEntityService.validate(roleCreate,securityContext);
	}

	public void validate(RoleFilter roleFilter, SecurityContextBase securityContext) {
		securityEntityService.validate(roleFilter,securityContext);
	}

	public <T extends Baseclass> T getByIdOrNull(String id,Class<T> c, SecurityContextBase securityContext) {
		return roleRepository.getByIdOrNull(id,c,securityContext);
	}

	public PaginationResponse<Role> getAllRoles(RoleFilter roleFilter, SecurityContextBase securityContext) {
		List<Role> list= listAllRoles(roleFilter, securityContext);
		long count=roleRepository.countAllRoles(roleFilter,securityContext);
		return new PaginationResponse<>(list,roleFilter,count);
	}

	public List<Role> listAllRoles(RoleFilter roleFilter, SecurityContextBase securityContext) {
		return roleRepository.listAllRoles(roleFilter, securityContext);
	}
}
