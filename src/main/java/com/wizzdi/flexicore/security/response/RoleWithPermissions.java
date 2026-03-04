package com.wizzdi.flexicore.security.response;

import com.flexicore.model.Role;
import java.util.List;

public class RoleWithPermissions {
    private Role role;
    private List<PermissionDetail> permissions;

    public RoleWithPermissions() {
    }

    public RoleWithPermissions(Role role, List<PermissionDetail> permissions) {
        this.role = role;
        this.permissions = permissions;
    }

    public Role getRole() {
        return role;
    }

    public RoleWithPermissions setRole(Role role) {
        this.role = role;
        return this;
    }

    public List<PermissionDetail> getPermissions() {
        return permissions;
    }

    public RoleWithPermissions setPermissions(List<PermissionDetail> permissions) {
        this.permissions = permissions;
        return this;
    }
}
