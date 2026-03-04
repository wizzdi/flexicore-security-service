# RoleCopyFilter Documentation

`RoleCopyFilter` is used to specify the parameters for copying roles from one or more source tenants to a target tenant. It extends `RoleFilter`, inheriting its filtering capabilities.

## Fields

### Target Tenant Settings

*   **`targetTenantId`** (String):
    *   The unique identifier (ID) of the tenant where the roles will be copied to.
    *   **Required**: Yes.
    *   **Validation**: Must be a valid `SecurityTenant` ID.
*   **`targetTenant`** (SecurityTenant):
    *   The actual `SecurityTenant` object.
    *   **Note**: This field is marked with `@JsonIgnore` and is typically populated by the system based on `targetTenantId`.

### Copy Logic Settings

*   **`copyType`** (RoleCopyType):
    *   Defines which types of permissions (`RoleToBaseclass` links) associated with the roles should be copied.
    *   **Default**: `ClazzAndOperation`.
    *   **Possible Values**:
        *   `All`: Copies all associated permissions.
        *   `Clazz`: Copies only permissions related to classes (`Clazz`).
        *   `Operation`: Copies only permissions related to security operations (`SecurityOperation`).
        *   `ClazzAndOperation`: Copies permissions related to both classes and operations.
        *   `ById`: Copies only permissions for specific baseclasses provided in `baseclassIds`.
*   **`wildcardIncludes`** (Set<String>):
    *   A set of canonical class name patterns or explicit canonical names (e.g., `com.flexicore.ui.model.*` or `com.flexicore.ui.model.Widget`) to filter target baseclasses.
    *   **Note**: Only effective when `copyType` is set to `All`. If not empty, only baseclasses whose canonical class name matches one of the patterns or is equal to an explicit name will be copied.
    *   `Clazz` and `SecurityOperation` instances are exempt from this filter and are always copied when `copyType` is `All`.
*   **`baseclassIds`** (Set<String>):
    *   A set of baseclass IDs to copy permissions for.
    *   **Note**: Only used when `copyType` is set to `ById`.
    *   **Validation**: Each ID must correspond to a valid `Baseclass`.
*   **`baseclasses`** (List<Baseclass>):
    *   A list of `Baseclass` objects corresponding to `baseclassIds`.
    *   **Note**: This field is marked with `@JsonIgnore` and is typically populated by the system.

### Naming Settings

*   **`constructNames`** (boolean):
    *   If set to `true`, or if the original role name includes `:`, the system will generate new names for the copied roles using the provided `prefix`.
    *   If `false` and the original name does not contain `:`, roles will retain their original names (if possible).
*   **`prefix`** (String):
    *   The prefix to use when `constructNames` is `true` or the original name contains `:`.
    *   **Required**: If `constructNames` is `true`, or if any source role name includes `:`, the prefix must be provided and cannot be empty.
    *   The resulting role name will be formatted as `{prefix}:{original_name_suffix}`.

## Role Synchronization and Permission Logic

When the `copyRoles` API is invoked, the system performs a synchronization between the source roles (filtered by the `RoleCopyFilter`) and the target roles in the `targetTenant`.

### Role Identification
*   Roles are matched by their name.
*   If a role with the target name (either original or constructed using `prefix`) does not exist in the target tenant, it is created.
*   If the role already exists, its basic properties are updated, and its permissions are synchronized.

### Permission Synchronization (Add/Update/Delete)
The system synchronizes `RoleToBaseclass` links between the source and target roles. A permission is defined by the combination of its `Operation` and its target `Baseclass` (right-side entity).

1.  **Add**: Any permission present in the source role that is missing in the target role (within the filter's scope) is created in the target tenant.
2.  **Update**: If a matching permission exists but its `simpleValue` differs, the target permission's `simpleValue` is updated to match the source.
3.  **Delete (Cleanup)**: Any permission in the target role that does **not** exist in the source role is soft-deleted, **provided it falls within the scope** of the current `RoleCopyFilter`. This ensures that permissions managed by other filters or created manually outside the scope of the current synchronization are preserved.

### Filter Scope and `shouldCopy`
The `RoleCopyType` and `wildcardIncludes` settings define the **management scope** of the synchronization. 
*   If `copyType` is `Clazz`, only permissions related to `Clazz` objects are synchronized. Permissions related to `SecurityOperation` or specific instances will not be added, updated, or removed from the target role.
*   The `shouldCopy` logic is applied to both source and target permissions to determine if they should be included in the synchronization process.

### Tenant Admin Exclusion
Roles that grant full administrative permissions over a tenant are explicitly excluded from the copy process for security reasons. A role is considered a "tenant admin" and will be skipped if it has a `RoleToBaseclass` link where:
*   The target baseclass is `SecurityWildcard`.
*   The operation is the `All` operation (identified by `All.class.getCanonicalName()`).

## API and Response Structure

### Copy Roles API
The `copyRoles` endpoint returns a list of `RoleWithPermissions` objects, detailing the result of the synchronization for each role.

### Get Roles with Permissions API
A separate API is available to retrieve roles and their detailed permissions without performing a copy. This is useful for auditing and verifying current role configurations.

### Response DTOs
*   **`RoleWithPermissions`**:
    *   `role`: The `Role` object.
    *   `permissions`: A list of `PermissionDetail` objects.
*   **`PermissionDetail`**:
    *   `rightsideType`: The type of the target entity (e.g., `Clazz`, `Operation`, or the class name of the instance).
    *   `rightsideName`: The name or identifier of the target entity.
    *   `operationName`: The name of the `SecurityOperation`.
    *   `simpleValue`: The value associated with the permission.

## Inherited Fields

Since `RoleCopyFilter` extends `RoleFilter`, it also includes:

*   **`securityTenantsIds`** (Set<String>):
    *   Used to filter the **source** roles by their tenant IDs.
*   **`basicPropertiesFilter`**:
    *   Used to filter roles by name, description, soft delete status, etc.
*   **Pagination Fields**:
    *   `pagesize`, `currentPage`, etc., inherited via `BaseclassFilter` -> `PaginationFilter`.
