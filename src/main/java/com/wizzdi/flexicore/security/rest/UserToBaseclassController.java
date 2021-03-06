package com.wizzdi.flexicore.security.rest;

import com.flexicore.annotations.IOperation;
import com.flexicore.annotations.OperationsInside;
import com.flexicore.model.UserToBaseClass;
import com.wizzdi.flexicore.boot.base.interfaces.Plugin;
import com.wizzdi.flexicore.security.request.UserToBaseclassCreate;
import com.wizzdi.flexicore.security.request.UserToBaseclassFilter;
import com.wizzdi.flexicore.security.request.UserToBaseclassUpdate;
import com.flexicore.security.SecurityContextBase;
import com.wizzdi.flexicore.security.response.PaginationResponse;
import com.wizzdi.flexicore.security.service.UserToBaseclassService;
import org.pf4j.Extension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@OperationsInside
@RequestMapping("/userToBaseclass")
@Extension
public class UserToBaseclassController implements Plugin {

	@Autowired
	private UserToBaseclassService userToBaseclassService;

	@IOperation(Name = "create user to baseclass",Description = "creates user to baseclass")
	@PostMapping("/create")
	public UserToBaseClass create(@RequestHeader("authenticationKey") String authenticationKey,@RequestBody UserToBaseclassCreate userToBaseclassCreate, @RequestAttribute SecurityContextBase securityContext){
		userToBaseclassService.validate(userToBaseclassCreate,securityContext);
		return userToBaseclassService.createUserToBaseclass(userToBaseclassCreate,securityContext);
	}

	@IOperation(Name = "returns user to baseclass",Description = "returns user to baseclass")

	@PostMapping("/getAll")
	public PaginationResponse<UserToBaseClass> getAll(@RequestHeader("authenticationKey") String authenticationKey,@RequestBody UserToBaseclassFilter userToBaseclassFilter, @RequestAttribute SecurityContextBase securityContext){
		userToBaseclassService.validate(userToBaseclassFilter,securityContext);
		return userToBaseclassService.getAllUserToBaseclass(userToBaseclassFilter,securityContext);
	}

	@IOperation(Name = "updates user to baseclass",Description = "updates user to baseclass")

	@PutMapping("/update")
	public UserToBaseClass update(@RequestHeader("authenticationKey") String authenticationKey,@RequestBody UserToBaseclassUpdate userToBaseclassUpdate, @RequestAttribute SecurityContextBase securityContext){
		String id=userToBaseclassUpdate.getId();
		UserToBaseClass userToBaseclass=id!=null?userToBaseclassService.getByIdOrNull(id,UserToBaseClass.class,securityContext):null;
		if(userToBaseclass==null){
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"no security user with id "+id);
		}
		userToBaseclassUpdate.setUserToBaseclass(userToBaseclass);
		userToBaseclassService.validate(userToBaseclassUpdate,securityContext);
		return userToBaseclassService.updateUserToBaseclass(userToBaseclassUpdate,securityContext);
	}
}
