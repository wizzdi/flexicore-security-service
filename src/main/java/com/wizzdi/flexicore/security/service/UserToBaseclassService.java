package com.wizzdi.flexicore.security.service;

import com.flexicore.model.*;
import com.wizzdi.flexicore.boot.base.interfaces.Plugin;
import com.wizzdi.flexicore.security.data.BaseclassRepository;
import com.wizzdi.flexicore.security.data.UserToBaseclassRepository;
import com.wizzdi.flexicore.security.request.UserToBaseclassCreate;
import com.wizzdi.flexicore.security.request.UserToBaseclassFilter;
import com.wizzdi.flexicore.security.request.UserToBaseclassUpdate;
import com.flexicore.security.SecurityContextBase;
import com.wizzdi.flexicore.security.response.PaginationResponse;
import org.pf4j.Extension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

@Extension
@Component
public class UserToBaseclassService implements Plugin {

	@Autowired
	private SecurityLinkService securityLinkService;
	@Autowired
	private UserToBaseclassRepository userToBaseclassRepository;
	@Autowired
	private OperationValidatorService operationValidatorService;
	@Autowired
	private BaseclassRepository baseclassRepository;


	public UserToBaseClass createUserToBaseclass(UserToBaseclassCreate userToBaseclassCreate, SecurityContextBase securityContext){
		operationValidatorService.clearCacheByUser(userToBaseclassCreate.getSecurityUser());
        return createUserToBaseclassNoMerge(userToBaseclassCreate,securityContext);
	}


	public UserToBaseClass createUserToBaseclassNoMerge(UserToBaseclassCreate userToBaseclassCreate, SecurityContextBase securityContext){
		UserToBaseClass userToBaseclass=new UserToBaseClass(userToBaseclassCreate.getName(),securityContext);
		boolean updated = updateUserToBaseclassNoMerge(userToBaseclassCreate, userToBaseclass);
		if (updated) {
			merge(userToBaseclass);
		}
		return userToBaseclass;
	}

	public boolean updateUserToBaseclassNoMerge(UserToBaseclassCreate req, UserToBaseClass userToBaseclass) {
		boolean updated=false;
		if (req.getSecurityOperation()!=null){
			if (userToBaseclass.getRightside()==null|| !userToBaseclass.getRightside().getId().equals(req.getSecurityOperation().getId())){
				userToBaseclass.setRightside(req.getSecurityOperation());
				updated=true;
			}

		}
		if (req.getSecurityUser()!=null){
			if (userToBaseclass.getLeftside()==null|| !userToBaseclass.getLeftside().getId().equals(req.getSecurityUser().getId())){
				userToBaseclass.setLeftside(req.getSecurityUser());
				updated=true;
			}

		}
		if (req.getAllow()!=null){
			String simple = req.getAllow() ? "allow" : "deny";
			if (userToBaseclass.getSimplevalue()==null|| !userToBaseclass.getSimplevalue().equals(simple)) {
				userToBaseclass.setSimplevalue(simple);
				updated=true;
			}
		}
		boolean rightSideUpdated=false;
		if (req.getBaseclass()!=null){
			if (userToBaseclass.getRightside()==null|| !userToBaseclass.getRightside().getId().equals(req.getBaseclass().getId())){
				userToBaseclass.setRightside(req.getBaseclass());
				updated=true;
				rightSideUpdated=true;
			}

		}
		if (req.getClazz()!=null){
			if (userToBaseclass.getRightside()==null|| !userToBaseclass.getRightside().getId().equals(req.getClazz().getId())){
				userToBaseclass.setRightside(req.getClazz());
				updated=true;
				rightSideUpdated=true;
			}

		}
		if(req.getSecurityOperation()!=null && rightSideUpdated){
			userToBaseclass.setValue(req.getSecurityOperation());
			updated=true;
		}
		return updated;

	}

	public UserToBaseClass updateUserToBaseclass(UserToBaseclassUpdate userToBaseclassUpdate, SecurityContextBase securityContext){
		UserToBaseClass userToBaseclass=userToBaseclassUpdate.getUserToBaseclass();
		if(updateUserToBaseclassNoMerge(userToBaseclassUpdate,userToBaseclass)){
			userToBaseclassRepository.merge(userToBaseclass);
		}
		return userToBaseclass;
	}
	public void merge(Object o){
		userToBaseclassRepository.merge(o);
	}
	public void massMerge(List<Object> list){
		userToBaseclassRepository.massMerge(list);
	}


	public void validate(UserToBaseclassCreate req, SecurityContextBase securityContext) {
		if (req.getSecurityUserId()!=null) {
			SecurityUser securityUser = baseclassRepository.getByIdOrNull(req.getSecurityUserId(), SecurityUser.class, SecurityUser_.clazz, securityContext);
			if (securityUser==null) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"no SecurityUser with id: "+req.getSecurityUserId()
				);


			}else {
				req.setSecurityUser(securityUser);
			}
		}
		if (req.getSecurityOperationId()!=null) {
			SecurityOperation securityOperation = baseclassRepository.getByIdOrNull(req.getSecurityOperationId(), SecurityOperation.class, SecurityOperation_.clazz, securityContext);
			if (securityOperation==null) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"no SecurityOperation with id: "+req.getSecurityOperation()
				);
			}else {
				req.setSecurityOperation(securityOperation);
			}
		}
		if (req.getClazzId()!=null) {
			Clazz clazz = baseclassRepository.getByIdOrNull(req.getClazzId(), Clazz.class, SecurityOperation_.clazz, securityContext);
			if (clazz==null) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"no Clazz with id: "+req.getClazzId()
				);
			}else {
				req.setClazz(clazz);
			}
		}
		if (req.getBaseclassId()!=null) {
			Baseclass baseclass = baseclassRepository.getByIdOrNull(req.getClazzId(), Baseclass.class, SecurityOperation_.clazz, securityContext);
			if (baseclass==null) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"no baseclass with id: "+req.getBaseclassId()
				);
			}else {
				req.setBaseclass(baseclass);
			}
		}



	}

	@Deprecated
	public void validate(UserToBaseclassFilter userToBaseclassFilter, SecurityContextBase securityContext) {
		securityLinkService.validate(userToBaseclassFilter,securityContext);
	}

	public <T extends Baseclass> T getByIdOrNull(String id,Class<T> c, SecurityContextBase securityContext) {
		return userToBaseclassRepository.getByIdOrNull(id,c,securityContext);
	}

	public <T extends Baseclass> List<T> listByIds(Class<T> c,Set<String> ids,  SecurityContextBase securityContext) {
		return userToBaseclassRepository.listByIds(c, ids, securityContext);
	}

	public PaginationResponse<UserToBaseClass> getAllUserToBaseclass(UserToBaseclassFilter userToBaseclassFilter, SecurityContextBase securityContext) {
		List<UserToBaseClass> list= listAllUserToBaseclasss(userToBaseclassFilter, securityContext);
		long count=userToBaseclassRepository.countAllUserToBaseclasss(userToBaseclassFilter,securityContext);
		return new PaginationResponse<>(list,userToBaseclassFilter,count);
	}

	public List<UserToBaseClass> listAllUserToBaseclasss(UserToBaseclassFilter userToBaseclassFilter, SecurityContextBase securityContext) {
		return userToBaseclassRepository.listAllUserToBaseclasss(userToBaseclassFilter, securityContext);
	}
}
