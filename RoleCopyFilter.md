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

## Inherited Fields

Since `RoleCopyFilter` extends `RoleFilter`, it also includes:

*   **`securityTenantsIds`** (Set<String>):
    *   Used to filter the **source** roles by their tenant IDs.
*   **`basicPropertiesFilter`**:
    *   Used to filter roles by name, description, soft delete status, etc.
*   **Pagination Fields**:
    *   `pagesize`, `currentPage`, etc., inherited via `BaseclassFilter` -> `PaginationFilter`.
