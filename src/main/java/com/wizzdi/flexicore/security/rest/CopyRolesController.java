package com.wizzdi.flexicore.security.rest;

import com.flexicore.annotations.IOperation;
import com.flexicore.annotations.IOperation.Access;
import com.flexicore.annotations.OperationsInside;
import com.flexicore.model.Role;
import com.flexicore.security.SecurityContextBase;
import com.wizzdi.flexicore.boot.base.interfaces.Plugin;
import com.wizzdi.flexicore.security.request.RoleCopyFilter;
import com.wizzdi.flexicore.security.request.RoleFilter;
import com.wizzdi.flexicore.security.response.RoleWithPermissions;
import com.wizzdi.flexicore.security.service.RoleService;
import org.pf4j.Extension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@OperationsInside
@RequestMapping("/copy-roles")
@Extension
public class CopyRolesController implements Plugin {

    @Autowired
    private RoleService roleService;

    @IOperation(Name = "copy Roles", Description = "copy Roles")
    @PostMapping("/copyRoles")
    public List<RoleWithPermissions> copyRoles(@RequestBody @Valid RoleCopyFilter roleCopyFilter, @RequestAttribute SecurityContextBase securityContext) {
        return roleService.copyRoles(roleCopyFilter, securityContext);
    }

    @IOperation(Name = "get roles with permissions", Description = "get roles with permissions")
    @PostMapping("/getRolesWithPermissions")
    public List<RoleWithPermissions> getRolesWithPermissions(@RequestBody @Valid RoleFilter roleFilter, @RequestAttribute SecurityContextBase securityContext) {
        return roleService.getRolesWithPermissions(roleFilter, securityContext);
    }

    @IOperation(Name = "get role copy filter documentation", Description = "get role copy filter documentation", access = Access.allow)
    @GetMapping("/getRoleCopyFilterDoc")
    public String getRoleCopyFilterDoc() {
        return roleService.getRoleCopyFilterDoc();
    }


}
