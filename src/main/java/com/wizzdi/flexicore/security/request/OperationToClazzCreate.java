package com.wizzdi.flexicore.security.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.flexicore.model.Clazz;
import com.flexicore.model.SecurityOperation;
import com.wizzdi.flexicore.security.validation.Create;
import com.wizzdi.flexicore.security.validation.IdValid;
import com.wizzdi.flexicore.security.validation.Update;

@IdValid.List({
        @IdValid(targetField = "securityOperation", fieldType = SecurityOperation.class, field = "securityOperationId", groups = {Create.class, Update.class}),
        @IdValid(targetField = "clazz", fieldType = Clazz.class, field = "clazzId", groups = {Create.class, Update.class})

})
public class OperationToClazzCreate extends BaselinkCreate {

    @JsonIgnore
    private SecurityOperation securityOperation;
    private String securityOperationId;
    @JsonIgnore
    private Clazz clazz;
    private String clazzId;

    @JsonIgnore
    public SecurityOperation getSecurityOperation() {
        return securityOperation;
    }

    public <T extends OperationToClazzCreate> T setSecurityOperation(SecurityOperation securityOperation) {
        this.securityOperation = securityOperation;
        return (T) this;
    }

    public String getSecurityOperationId() {
        return securityOperationId;
    }

    public <T extends OperationToClazzCreate> T setSecurityOperationId(String securityOperationId) {
        this.securityOperationId = securityOperationId;
        return (T) this;
    }

    @JsonIgnore
    public Clazz getClazz() {
        return clazz;
    }

    public <T extends OperationToClazzCreate> T setClazz(Clazz clazz) {
        this.clazz = clazz;
        return (T) this;
    }

    public String getClazzId() {
        return clazzId;
    }

    public <T extends OperationToClazzCreate> T setClazzId(String clazzId) {
        this.clazzId = clazzId;
        return (T) this;
    }
}
