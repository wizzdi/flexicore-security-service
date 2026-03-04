package com.wizzdi.flexicore.security.service;

import com.flexicore.annotations.rest.All;
import com.flexicore.model.*;
import com.wizzdi.flexicore.boot.base.interfaces.Plugin;
import com.wizzdi.flexicore.security.data.RoleRepository;
import com.flexicore.security.SecurityContextBase;
import com.wizzdi.flexicore.security.request.*;
import com.wizzdi.flexicore.security.response.PaginationResponse;
import com.wizzdi.flexicore.security.response.PermissionDetail;
import com.wizzdi.flexicore.security.response.RoleWithPermissions;
import org.pf4j.Extension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Extension
@Component
public class RoleService implements Plugin, InitializingBean {

	@Autowired
	private SecurityEntityService securityEntityService;
	@Autowired
	private RoleRepository roleRepository;
	@Autowired
	@Lazy
	private RoleToBaseclassService roleToBaseclassService;

	private static final Logger logger = LoggerFactory.getLogger("CopyRoles");
	private static final String ALL_OP_ID = Baseclass.generateUUIDFromString(All.class.getCanonicalName());


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

	@Override
	public void afterPropertiesSet() throws Exception {
		logger.info("RoleService initialized, CopyRoles logger is ready");
	}

	public List<RoleWithPermissions> copyRoles(RoleCopyFilter roleCopyFilter, SecurityContextBase securityContext) {
		logger.info("Starting copyRoles with filter: {}", roleCopyFilter);
		validate(roleCopyFilter, securityContext);
		if (roleCopyFilter.getTargetTenant() == null) {
			logger.error("target tenant is required for copyRoles");
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "target tenant is required");
		}
		SecurityTenant targetTenant = roleCopyFilter.getTargetTenant();
		logger.info("Target tenant identified: {}", targetTenant.getId());
		List<Role> sourceRoles = listAllRoles(roleCopyFilter, securityContext);
		logger.info("Found {} source roles to copy", sourceRoles.size());
		if ((roleCopyFilter.isConstructNames() || sourceRoles.stream().anyMatch(f -> f.getName().contains(":"))) && (roleCopyFilter.getPrefix() == null || roleCopyFilter.getPrefix().isEmpty())) {
			logger.error("prefix is required when constructNames is true or any source role name contains ':'");
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "prefix is required when constructNames is true or any source role name contains ':'");
		}
		List<RoleWithPermissions> result = new ArrayList<>();
		for (Role sourceRole : sourceRoles) {
			logger.info("Processing source role: {} ({})", sourceRole.getName(), sourceRole.getId());
			// Copy RoleToBaseclass
			RoleToBaseclassFilter rtbFilter = new RoleToBaseclassFilter()
					.setLeftside(Collections.singletonList(sourceRole));
			List<RoleToBaseclass> sourceLinks = roleToBaseclassService.listAllRoleToBaseclasss(rtbFilter, securityContext);

			if (isTenantAdminRole(sourceLinks)) {
				logger.info("Skipping source role {} as it is a tenant admin role", sourceRole.getName());
				continue;
			}

			String targetName = sourceRole.getName();
			String sourceSuffix = sourceRole.getName();
			if (sourceSuffix.contains(":")) {
				sourceSuffix = sourceSuffix.substring(sourceSuffix.indexOf(":") + 1);
			}

			if (roleCopyFilter.isConstructNames() || sourceRole.getName().contains(":")) {
				targetName = roleCopyFilter.getPrefix() + ":" + sourceSuffix;
				logger.info("Constructed target name: {}", targetName);
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
				logger.info("Target role {} does not exist in target tenant, creating it", targetName);
				RoleCreate roleCreate = new RoleCreate()
						.setName(targetName);
				roleCreate.setTenant(targetTenant);
				targetRole = createRole(roleCreate, securityContext);
			} else {
				targetRole = existing.get(0);
				logger.info("Target role {} already exists in target tenant", targetRole.getName());
			}

			// Copy RoleToBaseclass
			logger.info("Found {} RoleToBaseclass links for source role {}", sourceLinks.size(), sourceRole.getName());
			List<PermissionDetail> copiedPermissions = new ArrayList<>();
			
			// 1. Determine which source links should be copied/synchronized
			List<RoleToBaseclass> sourceLinksToSync = sourceLinks.stream()
					.filter(f -> shouldCopy(f, roleCopyFilter))
					.collect(Collectors.toList());

			// 2. Identify all existing permissions on the target role
			RoleToBaseclassFilter targetLinksFilter = new RoleToBaseclassFilter()
					.setLeftside(Collections.singletonList(targetRole));
			List<RoleToBaseclass> existingTargetLinks = roleToBaseclassService.listAllRoleToBaseclasss(targetLinksFilter, securityContext);

			// 3. Compare and Update/Add
			for (RoleToBaseclass sourceLink : sourceLinksToSync) {
				Baseclass rightside = sourceLink.getRightside();
				String rightsideType = rightside instanceof Clazz ? "Clazz" : (rightside instanceof SecurityOperation ? "Operation" : "Instance");
				String operationName = sourceLink.getValue() != null ? sourceLink.getValue().getName() : null;

				Optional<RoleToBaseclass> existingLinkOpt = existingTargetLinks.stream().filter(f -> 
						f.getRightside().getId().equals(rightside.getId()) &&
						((f.getValue() == null && sourceLink.getValue() == null) || (f.getValue() != null && sourceLink.getValue() != null && f.getValue().getId().equals(sourceLink.getValue().getId())))
				).findFirst();

				RoleToBaseclass targetLink;
				if (existingLinkOpt.isEmpty()) {
					logger.info("Copying RoleToBaseclass link to {} ({}) for target role {}, operation: {}", rightside.getName(), rightsideType, targetRole.getName(), operationName);
					RoleToBaseclassCreate rtbCreate = new RoleToBaseclassCreate()
							.setRole(targetRole)
							.setBaseclass(sourceLink.getRightside())
							.setValue(sourceLink.getValue());
					rtbCreate.setSimpleValue(sourceLink.getSimplevalue())
							.setName(sourceLink.getName());
					rtbCreate.setTenant(targetTenant);
					targetLink = roleToBaseclassService.createRoleToBaseclass(rtbCreate, securityContext);
				} else {
					targetLink = existingLinkOpt.get();
					logger.debug("RoleToBaseclass link to {} ({}) already exists for target role {}, operation: {}", rightside.getName(), rightsideType, targetRole.getName(), operationName);
					// Check for simpleValue update
					if (!Objects.equals(targetLink.getSimplevalue(), sourceLink.getSimplevalue())) {
						logger.info("Updating simpleValue for existing link to {} for target role {}", rightside.getName(), targetRole.getName());
						targetLink.setSimplevalue(sourceLink.getSimplevalue());
						roleToBaseclassService.merge(targetLink);
					}
				}
				copiedPermissions.add(new PermissionDetail(rightside.getId(), rightside.getName(), rightsideType, operationName));
			}

			// 4. Delete target permissions that are no longer in source but within filter scope
			for (RoleToBaseclass targetLink : existingTargetLinks) {
				// We only consider deleting it if it matches the current filter (so it's within our management scope)
				if (shouldCopy(targetLink, roleCopyFilter)) {
					boolean foundInSource = sourceLinksToSync.stream().anyMatch(s -> 
							s.getRightside().getId().equals(targetLink.getRightside().getId()) &&
							((s.getValue() == null && targetLink.getValue() == null) || (s.getValue() != null && targetLink.getValue() != null && s.getValue().getId().equals(targetLink.getValue().getId())))
					);

					if (!foundInSource) {
						logger.info("Removing RoleToBaseclass link to {} for target role {} as it is no longer in source", targetLink.getRightside().getName(), targetRole.getName());
						targetLink.setSoftDelete(true);
						roleToBaseclassService.merge(targetLink);
					}
				}
			}
			result.add(new RoleWithPermissions(targetRole, copiedPermissions));
		}
		logger.info("Finished copyRoles, copied {} roles", result.size());
		return result;
	}

	private boolean isTenantAdminRole(List<RoleToBaseclass> sourceLinks) {
		for (RoleToBaseclass sourceLink : sourceLinks) {
			Baseclass rightside = sourceLink.getRightside();
			if (rightside instanceof Clazz && SecurityWildcard.class.getCanonicalName().equals(rightside.getName())) {
				Baseclass operation = sourceLink.getValue();
				if (operation != null && ALL_OP_ID.equals(operation.getId())) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean shouldCopy(RoleToBaseclass sourceLink, RoleCopyFilter roleCopyFilter) {
		RoleCopyType type = roleCopyFilter.getCopyType();
		if (type == null) {
			type = RoleCopyType.ClazzAndOperation;
		}

		switch (type) {
			case All:
				if (roleCopyFilter.getWildcardIncludes() != null && !roleCopyFilter.getWildcardIncludes().isEmpty()) {
					Baseclass rightside = sourceLink.getRightside();
					if (rightside instanceof Clazz || rightside instanceof SecurityOperation) {
						return true;
					}
					String canonicalName = rightside.getClass().getCanonicalName();
					return roleCopyFilter.getWildcardIncludes().stream().anyMatch(pattern -> matchWildcard(canonicalName, pattern));
				}
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

	private boolean matchWildcard(String text, String pattern) {
		if (pattern.endsWith("*")) {
			String prefix = pattern.substring(0, pattern.length() - 1);
			return text.startsWith(prefix);
		}
		return text.equals(pattern);
	}

	public List<RoleWithPermissions> getRolesWithPermissions(RoleFilter roleFilter, SecurityContextBase securityContext) {
		List<Role> roles = listAllRoles(roleFilter, securityContext);
		List<RoleWithPermissions> result = new ArrayList<>();
		for (Role role : roles) {
			RoleToBaseclassFilter rtbFilter = new RoleToBaseclassFilter()
					.setLeftside(Collections.singletonList(role));
			List<RoleToBaseclass> links = roleToBaseclassService.listAllRoleToBaseclasss(rtbFilter, securityContext);
			List<PermissionDetail> permissions = links.stream().map(f -> {
				Baseclass rightside = f.getRightside();
				String rightsideType = rightside instanceof Clazz ? "Clazz" : (rightside instanceof SecurityOperation ? "Operation" : "Instance");
				String operationName = f.getValue() != null ? f.getValue().getName() : null;
				return new PermissionDetail(rightside.getId(), rightside.getName(), rightsideType, operationName);
			}).collect(Collectors.toList());
			result.add(new RoleWithPermissions(role, permissions));
		}
		return result;
	}

	public String getRoleCopyFilterDoc() {
		try {
			return new String(Files.readAllBytes(Paths.get("/home/flexicore/docs/RoleCopyFilter.md")));
		} catch (IOException e) {
			throw new ResponseStatusException(
					HttpStatus.BAD_REQUEST, "Could not read documentation");
		}
	}
}
