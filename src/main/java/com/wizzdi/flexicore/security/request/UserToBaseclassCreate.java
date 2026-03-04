package com.wizzdi.flexicore.security.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.flexicore.model.Baseclass;
import com.flexicore.model.Clazz;
import com.flexicore.model.SecurityOperation;
import com.flexicore.model.SecurityUser;
import com.wizzdi.flexicore.security.validation.Create;
import com.wizzdi.flexicore.security.validation.IdValid;
import com.wizzdi.flexicore.security.validation.Update;

import javax.validation.Valid;

public class UserToBaseclassCreate extends SecurityLinkCreate {
    String baseclassId;
    String securityOperationId;
    String clazzId;
    String securityUserId;
    Boolean allow;
    @JsonIgnore
    private Clazz clazz;
    @JsonIgnore
    private SecurityOperation securityOperation;
    @JsonIgnore
    private SecurityUser securityUser;
    @JsonIgnore
    private Baseclass baseclass;
    public String getBaseclassId() {
        return baseclassId;
    }
    public UserToBaseclassCreate setBaseclassId(String baseclassId) {
        baseclassId = baseclassId;
        return this;
    }
    public String getSecurityOperationId() {
        return securityOperationId;
    }
    public UserToBaseclassCreate setSecurityOperationId(String securityOperationId) {
        this.securityOperationId = securityOperationId;
        return this;
    }
    public String getClazzId() {
        return clazzId;
    }
    public UserToBaseclassCreate setClazzId(String clazzId) {
        this.clazzId = clazzId;
        return this;
    }
    public String getSecurityUserId() {
        return securityUserId;
    }
    public UserToBaseclassCreate setSecurityUserId(String securityUserId) {
        this.securityUserId = securityUserId;
        return this;
    }
    public Boolean getAllow() {
        return allow;
    }
    public UserToBaseclassCreate setAllow(Boolean allow) {
        this.allow = allow;
        return this;
    }
    public Clazz getClazz() {
        return clazz;
    }
    public UserToBaseclassCreate setClazz(Clazz clazz) {
        this.clazz = clazz;
        return this;
    }
    public SecurityOperation getSecurityOperation() {
        return securityOperation;
    }
    public UserToBaseclassCreate setSecurityOperation(SecurityOperation securityOperation) {
        this.securityOperation = securityOperation;
        return this;
    }
    public SecurityUser getSecurityUser() {
        return securityUser;
    }
    public UserToBaseclassCreate setSecurityUser(SecurityUser securityUser) {
        this.securityUser = securityUser;
        return this;
    }
    public Baseclass getBaseclass() {
        return baseclass;
    }
    public UserToBaseclassCreate setBaseclass(Baseclass baseclass) {
        this.baseclass = baseclass;
        return this;
    }

}
