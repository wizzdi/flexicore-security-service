package com.wizzdi.flexicore.security.service;

import com.flexicore.model.*;
import com.wizzdi.flexicore.boot.base.interfaces.Plugin;
import com.wizzdi.flexicore.security.data.RoleRepository;
import com.flexicore.security.SecurityContextBase;
import com.wizzdi.flexicore.security.request.*;
import com.wizzdi.flexicore.security.response.PaginationResponse;
import org.pf4j.Extension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Extension
@Component
public class RoleService implements Plugin {

	@Autowired
	private SecurityEntityService securityEntityService;
	@Autowired
	private RoleRepository roleRepository;
	@Autowired
	@Lazy
	private RoleToBaseclassService roleToBaseclassService;


	public Role createRole(RoleCreate roleCreate, SecurityContextBase securityContext){
		Role role= createRoleNoMerge(roleCreate,securityContext);
		roleRepository.merge(role);
		return role;
	}

	public <T> T merge(T o){
		return roleRepository.merge(o);
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

	@Deprecated
	public void validate(RoleCreate roleCreate, SecurityContextBase securityContext) {
		securityEntityService.validate(roleCreate,securityContext);
	}

	@Deprecated
	public void validate(RoleFilter roleFilter, SecurityContextBase securityContext) {
		securityEntityService.validate(roleFilter,securityContext);
		Set<String> securityTenantIds=roleFilter.getSecurityTenantsIds();
		Map<String, SecurityTenant> securityTenantMap=securityTenantIds.isEmpty()?new HashMap<>():roleRepository.listByIds(SecurityTenant.class,securityTenantIds,securityContext).stream().collect(Collectors.toMap(f->f.getId(),f->f));
		securityTenantIds.removeAll(securityTenantMap.keySet());
		if(!securityTenantIds.isEmpty()){
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"no security tenants with ids "+securityTenantIds);
		}
		roleFilter.setSecurityTenants(new ArrayList<>(securityTenantMap.values()));
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

	public <T extends Baseclass> List<T> findByIds(Class<T> c, Set<String> requested) {
		return roleRepository.findByIds(c, requested);
	}

	public <T> T findByIdOrNull(Class<T> type, String id) {
		return roleRepository.findByIdOrNull(type, id);
	}

	public List<Role> copyRoles(RoleCopyFilter roleCopyFilter, SecurityContextBase securityContext) {
		validate(roleCopyFilter, securityContext);
		if (roleCopyFilter.getTargetTenant() == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "target tenant is required");
		}
		if (roleCopyFilter.isConstructNames() && (roleCopyFilter.getPrefix() == null || roleCopyFilter.getPrefix().isEmpty())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "prefix is required when constructNames is true");
		}
		SecurityTenant targetTenant = roleCopyFilter.getTargetTenant();
		List<Role> sourceRoles = listAllRoles(roleCopyFilter, securityContext);
		List<Role> result = new ArrayList<>();
		for (Role sourceRole : sourceRoles) {
			String targetName = sourceRole.getName();
			String sourceSuffix = sourceRole.getName();
			if (sourceSuffix.contains(":")) {
				sourceSuffix = sourceSuffix.substring(sourceSuffix.indexOf(":") + 1);
			}

			if (roleCopyFilter.isConstructNames()) {
				targetName = roleCopyFilter.getPrefix() + ":" + sourceSuffix;
			}

			RoleFilter targetRoleFilter = new RoleFilter()
					.setSecurityTenants(Collections.singletonList(targetTenant))
					.setBasicPropertiesFilter(new BasicPropertiesFilter()
							.setSoftDelete(SoftDeleteOption.DEFAULT));
			List<Role> existing = listAllRoles(targetRoleFilter, securityContext);

			final String finalSourceSuffix = sourceSuffix;
			final String finalTargetName = targetName;
			existing = existing.stream().filter(f -> {
				if (f.getName().contains(":")) {
					String suffix = f.getName().substring(f.getName().indexOf(":") + 1);
					return suffix.equals(finalSourceSuffix);
				}
				return f.getName().equals(finalTargetName);
			}).collect(Collectors.toList());

			Role targetRole;
			if (existing.isEmpty()) {
				RoleCreate roleCreate = new RoleCreate()
						.setName(targetName);
				roleCreate.setTenant(targetTenant);
				targetRole = createRole(roleCreate, securityContext);
			} else {
				targetRole = existing.get(0);
			}
			result.add(targetRole);

			// Copy RoleToBaseclass
			RoleToBaseclassFilter rtbFilter = new RoleToBaseclassFilter()
					.setLeftside(Collections.singletonList(sourceRole));
			List<RoleToBaseclass> sourceLinks = roleToBaseclassService.listAllRoleToBaseclasss(rtbFilter, securityContext);
			for (RoleToBaseclass sourceLink : sourceLinks) {
				if (!shouldCopy(sourceLink, roleCopyFilter)) {
					continue;
				}
				RoleToBaseclassFilter targetLinkFilter = new RoleToBaseclassFilter()
						.setLeftside(Collections.singletonList(targetRole))
						.setRightside(Collections.singletonList(sourceLink.getRightside()))
						.setValues(sourceLink.getValue() != null ? Collections.singletonList(sourceLink.getValue()) : null);
				List<RoleToBaseclass> existingLinks = roleToBaseclassService.listAllRoleToBaseclasss(targetLinkFilter, securityContext);
				if (existingLinks.isEmpty()) {
					RoleToBaseclassCreate rtbCreate = new RoleToBaseclassCreate()
							.setRole(targetRole)
							.setBaseclass(sourceLink.getRightside())
							.setValue(sourceLink.getValue());
					rtbCreate.setSimpleValue(sourceLink.getSimplevalue())
							.setName(sourceLink.getName());
					rtbCreate.setTenant(targetTenant);
					roleToBaseclassService.createRoleToBaseclass(rtbCreate, securityContext);
				}
			}
		}
		return result;
	}

	private boolean shouldCopy(RoleToBaseclass sourceLink, RoleCopyFilter roleCopyFilter) {
		RoleCopyType type = roleCopyFilter.getCopyType();
		if (type == null) {
			type = RoleCopyType.ClazzAndOperation;
		}
		switch (type) {
			case All:
				return true;
			case Clazz:
				return sourceLink.getRightside() instanceof Clazz;
			case Operation:
				return sourceLink.getRightside() instanceof SecurityOperation;
			case ClazzAndOperation:
				return sourceLink.getRightside() instanceof Clazz || sourceLink.getRightside() instanceof SecurityOperation;
			case ById:
				return roleCopyFilter.getBaseclassIds().contains(sourceLink.getRightside().getId());
			default:
				return false;
		}
	}
}
