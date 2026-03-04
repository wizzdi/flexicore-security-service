package com.wizzdi.flexicore.security.service;

import com.flexicore.annotations.IOperation;
import com.flexicore.annotations.IOperation.Access;
import com.flexicore.model.Baselink;
import com.flexicore.model.Role;
import com.flexicore.model.SecurityOperation;
import com.flexicore.model.SecurityTenant;
import com.flexicore.model.SecurityUser;
import com.flexicore.security.SecurityContextBase;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.wizzdi.flexicore.boot.base.interfaces.Plugin;
import com.wizzdi.flexicore.security.data.SecurityRepository;
import com.wizzdi.flexicore.security.request.BaselinkFilter;
import org.pf4j.Extension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Lazy
@Service
@Extension
public class OperationValidatorService implements Plugin, InitializingBean {


    private static final String SUPER_ADMIN_ROLE_ID = "HzFnw-nVR0Olq6WBvwKcQg";
    private static final Logger operationLogger = LoggerFactory.getLogger("OperationValidatorService");
    private static final Cache<String,Boolean> accessControlCache= CacheBuilder.newBuilder().maximumSize(1000).expireAfterWrite(24, TimeUnit.HOURS).build();

    String USER_TYPE = "USER";
    String ROLE_TYPE = "ROLE";
    String TENANT_TYPE = "TENANT";

    @Autowired
    private BaselinkService baselinkService;

    @Autowired
    private SecurityRepository securityRepository;




    static String getAccessControlKey(String type, String opId, String securityEntityId, IOperation.Access access){
        return type+"."+opId+"."+securityEntityId+"."+access.name();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        operationLogger.info("OperationValidatorService started");
    }

    public boolean checkIfAllowed(SecurityContextBase securityContextBase) {
        if (isSuperAdmin(securityContextBase)) {
            return true;
        }
        Access defaultaccess = securityContextBase.getOperation() != null && securityContextBase.getOperation().getDefaultaccess() != null ? securityContextBase.getOperation().getDefaultaccess() : Access.allow;
        SecurityUser user = securityContextBase.getUser();
        operationLogger.debug("[checkIfAllowed] User id is {} , default access is {} ", user.getId(),defaultaccess);
        return checkIfAllowed(user, securityContextBase.getTenants(), securityContextBase.getOperation(), defaultaccess);
    }

    private boolean isSuperAdmin(SecurityUser securityUser) {
        return securityRepository.isSuperAdmin(securityUser);
    }

    private boolean isSuperAdmin(SecurityContextBase<?, ?, ?, ?> securityContextBase) {
        if (securityContextBase.getRoleMap() != null) {
            return securityContextBase.getRoleMap().values().stream().flatMap(List::stream).anyMatch(f -> SUPER_ADMIN_ROLE_ID.equals(f.getId()));
        }
        return false;
    }

    public boolean userAllowed(SecurityOperation operation, SecurityUser user) {
        return checkUser(operation, user, IOperation.Access.allow);

    }

    public boolean userDenied(SecurityOperation operation, SecurityUser user) {
        return checkUser(operation, user, IOperation.Access.deny);
    }

    public boolean roleAllowed(SecurityOperation operation, SecurityUser user) {
        IOperation.Access access = IOperation.Access.allow;
        String cacheKey= getAccessControlKey(ROLE_TYPE,operation.getId(),user.getId(),access);
        Boolean val=accessControlCache.getIfPresent(cacheKey);
        if(val!=null){
            operationLogger.debug("Cache hit for role allowed check: op={}, user={}, result={}", operation.getId(), user.getId(), val);
            return val;
        }
        val = securityRepository.checkRole(operation, user, access);
        operationLogger.debug("Cache miss for role allowed check: op={}, user={}, result={}", operation.getId(), user.getId(), val);
        accessControlCache.put(cacheKey,val);

        return val;

    }

    public boolean roleDenied(SecurityOperation operation, SecurityUser user) {
        IOperation.Access access = IOperation.Access.deny;

        String cacheKey=getAccessControlKey(ROLE_TYPE,operation.getId(),user.getId(),access);
        Boolean val=accessControlCache.getIfPresent(cacheKey);
        if(val!=null){
            operationLogger.debug("Cache hit for role denied check: op={}, user={}, result={}", operation.getId(), user.getId(), val);
            return val;
        }
        val = securityRepository.checkRole(operation, user, access);
        operationLogger.debug("Cache miss for role denied check: op={}, user={}, result={}", operation.getId(), user.getId(), val);
        accessControlCache.put(cacheKey,val);
        return val;
    }

    public boolean checkUser(SecurityOperation operation, SecurityUser user, IOperation.Access access) {
        String cacheKey= getAccessControlKey(USER_TYPE,operation.getId(),user.getId(),access);
        Boolean val=accessControlCache.getIfPresent(cacheKey);
        if(val!=null){
            operationLogger.debug("Cache hit for user check ({}): op={}, user={}, result={}", access, operation.getId(), user.getId(), val);
            return val;
        }
        val = securityRepository.checkUser(operation, user, access);
        operationLogger.debug("Cache miss for user check ({}): op={}, user={}, result={}", access, operation.getId(), user.getId(), val);
        accessControlCache.put(cacheKey,val);
        return val;
    }
    public boolean tenantAllowed(SecurityOperation operation, SecurityTenant tenant) {
        IOperation.Access access=IOperation.Access.allow;
        String cacheKey= getAccessControlKey(TENANT_TYPE,operation.getId(),tenant.getId(),access);
        Boolean val=accessControlCache.getIfPresent(cacheKey);
        if(val!=null){
            operationLogger.debug("Cache hit for tenant allowed check: op={}, tenant={}, result={}", operation.getId(), tenant.getId(), val);
            return val;
        }
        Baselink link = baselinkService.listAllBaselinks(new BaselinkFilter().setLeftside(Collections.singletonList(tenant)).setRightside(Collections.singletonList(operation)).setSimpleValues(Collections.singleton(access.name())),null).stream().findFirst().orElse(null);
        val= link != null;
        operationLogger.debug("Cache miss for tenant allowed check: op={}, tenant={}, result={}", operation.getId(), tenant.getId(), val);
        accessControlCache.put(cacheKey,val);
        return val;
    }

    public boolean tenantDenied(SecurityOperation operation, SecurityTenant tenant) {
        IOperation.Access access=IOperation.Access.deny;
        String cacheKey= getAccessControlKey(TENANT_TYPE,operation.getId(),tenant.getId(),access);
        Boolean val=accessControlCache.getIfPresent(cacheKey);
        if(val!=null){
            operationLogger.debug("Cache hit for tenant denied check: op={}, tenant={}, result={}", operation.getId(), tenant.getId(), val);
            return val;
        }
        Baselink link = baselinkService.listAllBaselinks(new BaselinkFilter().setLeftside(Collections.singletonList(tenant)).setRightside(Collections.singletonList(operation)).setSimpleValues(Collections.singleton(access.name())),null).stream().findFirst().orElse(null);
        val= link != null;
        operationLogger.debug("Cache miss for tenant denied check: op={}, tenant={}, result={}", operation.getId(), tenant.getId(), val);
        accessControlCache.put(cacheKey,val);
        return val;
    }


    
    public void clearCacheByUser(SecurityUser user) {
        String userId = user.getId();
        operationLogger.info("Clearing cache for user {}", userId);
        accessControlCache.asMap().keySet().removeIf(key -> key.contains("." + userId + "."));
    }

    public void clearCacheByTenant(SecurityTenant tenant) {
        String tenantId = tenant.getId();
        operationLogger.info("Clearing cache for tenant {}", tenantId);
        accessControlCache.asMap().keySet().removeIf(key -> key.contains("." + tenantId + "."));
    }

    public void clearCacheByRole(Role role) {
        String roleId = role.getId();
        operationLogger.info("Clearing cache for role {}", roleId);
        accessControlCache.asMap().keySet().removeIf(key -> key.contains("." + roleId + "."));
    }


    public boolean checkIfAllowed(SecurityUser securityUser, List<SecurityTenant> tenants, SecurityOperation securityOperation, Access access) {
        operationLogger.debug("Checking if operation {} is allowed for user {}", securityOperation.getId(), securityUser.getId());
        if (isSuperAdmin(securityUser)) {
            operationLogger.info("Access granted to user {} for operation {}: super admin", securityUser.getId(), securityOperation.getId());
            return true;
        }
        if (userAllowed(securityOperation, securityUser)) {
            operationLogger.info("Access granted to user {} for operation {}: user allowed directly", securityUser.getId(), securityOperation.getId());
            return true;
        } else {
            if (userDenied(securityOperation, securityUser)) {
                operationLogger.info("Access denied to user {} for operation {}: user denied directly", securityUser.getId(), securityOperation.getId());
                return false;
            }
            if (roleAllowed(securityOperation, securityUser)) {
                operationLogger.info("Access granted to user {} for operation {}: role allowed", securityUser.getId(), securityOperation.getId());
                return true;
            } else {
                if (roleDenied(securityOperation, securityUser)) {
                    operationLogger.info("Access denied to user {} for operation {}: role denied", securityUser.getId(), securityOperation.getId());
                    return false;
                } else {
                    for (SecurityTenant securityTenant : tenants) {
                        if (tenantAllowed(securityOperation, securityTenant)) {
                            operationLogger.info("Access granted to user {} for operation {}: tenant {} allowed", securityUser.getId(), securityOperation.getId(), securityTenant.getId());
                            return true;
                        }
                    }
                    boolean allDenied = true;
                    for (SecurityTenant securityTenant : tenants) {
                        boolean denied = tenantDenied(securityOperation, securityTenant);
                        allDenied = denied && allDenied;
                        if(denied){
                            operationLogger.debug("Tenant {} denied for operation {}", securityTenant.getId(), securityOperation.getId());
                        }
                    }
                    if (allDenied) {
                        operationLogger.info("Access denied to user {} for operation {}: all tenants denied", securityUser.getId(), securityOperation.getId());
                        return false;
                    } else {
                        boolean result = access == Access.allow;
                        operationLogger.info("Access {} to user {} for operation {}: following default access", result ? "granted" : "denied", securityUser.getId(), securityOperation.getId());
                        return result;
                    }


                }
            }

        }
    }





}
