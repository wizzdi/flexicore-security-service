package com.wizzdi.flexicore.security.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.flexicore.model.Baseclass;
import com.flexicore.model.SecurityTenant;
import com.wizzdi.flexicore.security.validation.IdValid;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@IdValid.List({
        @IdValid(targetField = "targetTenant", fieldType = SecurityTenant.class, field = "targetTenantId"),
        @IdValid(targetField = "baseclasses", fieldType = Baseclass.class, field = "baseclassIds")
})
public class RoleCopyFilter extends RoleFilter {

    private String targetTenantId;
    @JsonIgnore
    private SecurityTenant targetTenant;

    private RoleCopyType copyType = RoleCopyType.ClazzAndOperation;
    private Set<String> baseclassIds = new HashSet<>();
    @JsonIgnore
    private List<Baseclass> baseclasses;

    private boolean constructNames;
    private String prefix;
    private Set<String> wildcardIncludes = new HashSet<>();

    public String getTargetTenantId() {
        return targetTenantId;
    }

    public <T extends RoleCopyFilter> T setTargetTenantId(String targetTenantId) {
        this.targetTenantId = targetTenantId;
        return (T) this;
    }

    @JsonIgnore
    public SecurityTenant getTargetTenant() {
        return targetTenant;
    }

    public <T extends RoleCopyFilter> T setTargetTenant(SecurityTenant targetTenant) {
        this.targetTenant = targetTenant;
        return (T) this;
    }

    public RoleCopyType getCopyType() {
        return copyType;
    }

    public <T extends RoleCopyFilter> T setCopyType(RoleCopyType copyType) {
        this.copyType = copyType;
        return (T) this;
    }

    public Set<String> getBaseclassIds() {
        return baseclassIds;
    }

    public <T extends RoleCopyFilter> T setBaseclassIds(Set<String> baseclassIds) {
        this.baseclassIds = baseclassIds;
        return (T) this;
    }

    @JsonIgnore
    public List<Baseclass> getBaseclasses() {
        return baseclasses;
    }

    public <T extends RoleCopyFilter> T setBaseclasses(List<Baseclass> baseclasses) {
        this.baseclasses = baseclasses;
        return (T) this;
    }

    public boolean isConstructNames() {
        return constructNames;
    }

    public <T extends RoleCopyFilter> T setConstructNames(boolean constructNames) {
        this.constructNames = constructNames;
        return (T) this;
    }

    public String getPrefix() {
        return prefix;
    }

    public <T extends RoleCopyFilter> T setPrefix(String prefix) {
        this.prefix = prefix;
        return (T) this;
    }

    public Set<String> getWildcardIncludes() {
        return wildcardIncludes;
    }

    public <T extends RoleCopyFilter> T setWildcardIncludes(Set<String> wildcardIncludes) {
        this.wildcardIncludes = wildcardIncludes;
        return (T) this;
    }
}
