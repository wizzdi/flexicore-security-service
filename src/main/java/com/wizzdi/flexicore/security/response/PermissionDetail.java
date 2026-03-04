package com.wizzdi.flexicore.security.response;

public class PermissionDetail {
    private String id;
    private String name;
    private String type; // Operation, Clazz, or Other
    private String operationName;

    public PermissionDetail() {
    }

    public PermissionDetail(String id, String name, String type, String operationName) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.operationName = operationName;
    }

    public String getId() {
        return id;
    }

    public PermissionDetail setId(String id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public PermissionDetail setName(String name) {
        this.name = name;
        return this;
    }

    public String getType() {
        return type;
    }

    public PermissionDetail setType(String type) {
        this.type = type;
        return this;
    }

    public String getOperationName() {
        return operationName;
    }

    public PermissionDetail setOperationName(String operationName) {
        this.operationName = operationName;
        return this;
    }
}
