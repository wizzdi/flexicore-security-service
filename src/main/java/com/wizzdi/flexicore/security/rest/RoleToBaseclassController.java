package com.wizzdi.flexicore.security.rest;

import com.flexicore.annotations.IOperation;
import com.flexicore.annotations.OperationsInside;
import com.flexicore.model.RoleToBaseclass;
import com.wizzdi.flexicore.boot.base.interfaces.Plugin;
import com.flexicore.security.SecurityContextBase;
import com.wizzdi.flexicore.security.request.RoleToBaseclassCreate;
import com.wizzdi.flexicore.security.request.RoleToBaseclassFilter;
import com.wizzdi.flexicore.security.request.RoleToBaseclassUpdate;
import com.wizzdi.flexicore.security.response.PaginationResponse;
import com.wizzdi.flexicore.security.service.RoleToBaseclassService;
import org.pf4j.Extension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@OperationsInside
@RequestMapping("/roleToBaseclass")
@Extension
public class RoleToBaseclassController implements Plugin {

	@Autowired
	private RoleToBaseclassService roleToBaseclassService;

	@IOperation(Name = "creates RoleToBaseclass",Description = "creates RoleToBaseclass")
	@PostMapping("/create")
	public RoleToBaseclass create(@RequestHeader("authenticationKey") String authenticationKey,@RequestBody RoleToBaseclassCreate roleToBaseclassCreate, @RequestAttribute SecurityContextBase securityContext){
		roleToBaseclassService.validate(roleToBaseclassCreate,securityContext);
		return roleToBaseclassService.createRoleToBaseclass(roleToBaseclassCreate,securityContext);
	}

	@IOperation(Name = "returns RoleToBaseclass",Description = "returns RoleToBaseclass")
	@PostMapping("/getAll")
	public PaginationResponse<RoleToBaseclass> getAll(@RequestHeader("authenticationKey") String authenticationKey,@RequestBody RoleToBaseclassFilter roleToBaseclassFilter, @RequestAttribute SecurityContextBase securityContext){
		roleToBaseclassService.validate(roleToBaseclassFilter,securityContext);
		return roleToBaseclassService.getAllRoleToBaseclass(roleToBaseclassFilter,securityContext);
	}

	@IOperation(Name = "updates RoleToBaseclass",Description = "updates RoleToBaseclass")
	@PutMapping("/update")
	public RoleToBaseclass update(@RequestHeader("authenticationKey") String authenticationKey,@RequestBody RoleToBaseclassUpdate roleToBaseclassUpdate, @RequestAttribute SecurityContextBase securityContext){
		String id=roleToBaseclassUpdate.getId();
		RoleToBaseclass roleToBaseclass=id!=null?roleToBaseclassService.getByIdOrNull(id,RoleToBaseclass.class,securityContext):null;
		if(roleToBaseclass==null){
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"no security user with id "+id);
		}
		roleToBaseclassUpdate.setRoleToBaseclass(roleToBaseclass);
		roleToBaseclassService.validate(roleToBaseclassUpdate,securityContext);
		return roleToBaseclassService.updateRoleToBaseclass(roleToBaseclassUpdate,securityContext);
	}
}
