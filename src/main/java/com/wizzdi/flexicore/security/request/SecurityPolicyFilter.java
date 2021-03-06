package com.wizzdi.flexicore.security.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.flexicore.model.Role;
import com.flexicore.model.SecurityTenant;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SecurityPolicyFilter extends BaseclassFilter {

	private Set<String> securityTenantsIds=new HashSet<>();
	@JsonIgnore
	private List<SecurityTenant> securityTenants;
	private Set<String> rolesIds=new HashSet<>();
	@JsonIgnore
	private List<Role> roles;
	private OffsetDateTime startTime;
	private Boolean enabled;
	private boolean includeNoRole;

	public Set<String> getSecurityTenantsIds() {
		return securityTenantsIds;
	}

	public <T extends SecurityPolicyFilter> T setSecurityTenantsIds(Set<String> securityTenantsIds) {
		this.securityTenantsIds = securityTenantsIds;
		return (T) this;
	}

	@JsonIgnore
	public List<SecurityTenant> getSecurityTenants() {
		return securityTenants;
	}

	public <T extends SecurityPolicyFilter> T setSecurityTenants(List<SecurityTenant> securityTenants) {
		this.securityTenants = securityTenants;
		return (T) this;
	}

	public Set<String> getRolesIds() {
		return rolesIds;
	}

	public <T extends SecurityPolicyFilter> T setRolesIds(Set<String> rolesIds) {
		this.rolesIds = rolesIds;
		return (T) this;
	}

	@JsonIgnore
	public List<Role> getRoles() {
		return roles;
	}

	public <T extends SecurityPolicyFilter> T setRoles(List<Role> roles) {
		this.roles = roles;
		return (T) this;
	}

	public OffsetDateTime getStartTime() {
		return startTime;
	}

	public <T extends SecurityPolicyFilter> T setStartTime(OffsetDateTime startTime) {
		this.startTime = startTime;
		return (T) this;
	}

	public Boolean getEnabled() {
		return enabled;
	}

	public <T extends SecurityPolicyFilter> T setEnabled(Boolean enabled) {
		this.enabled = enabled;
		return (T) this;
	}

	public boolean isIncludeNoRole() {
		return includeNoRole;
	}

	public <T extends SecurityPolicyFilter> T setIncludeNoRole(boolean includeNoRole) {
		this.includeNoRole = includeNoRole;
		return (T) this;
	}
}
