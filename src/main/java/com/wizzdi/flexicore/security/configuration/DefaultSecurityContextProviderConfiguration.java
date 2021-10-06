package com.wizzdi.flexicore.security.configuration;

import com.flexicore.model.*;
import com.flexicore.security.SecurityContextBase;
import com.wizzdi.flexicore.security.interfaces.SecurityContextProvider;
import com.wizzdi.flexicore.security.request.RoleToUserFilter;
import com.wizzdi.flexicore.security.request.TenantToUserFilter;
import com.wizzdi.flexicore.security.service.RoleToUserService;
import com.wizzdi.flexicore.security.service.TenantToUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@ConditionalOnMissingBean(SecurityContextProvider.class)
public class DefaultSecurityContextProviderConfiguration {

    @Autowired
    private TenantToUserService tenantToUserService;
    @Autowired
    private RoleToUserService roleToUserService;

    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
    public SecurityContextProvider securityContextProvider() {
        return this::getSecurityContext;
    }

    private SecurityContextBase getSecurityContext(SecurityUser securityUser) {
        List<TenantToUser> links=tenantToUserService.listAllTenantToUsers(new TenantToUserFilter().setSecurityUsers(Collections.singletonList(securityUser)),null);
        List<RoleToUser> roleToUsers=roleToUserService.listAllRoleToUsers(new RoleToUserFilter().setSecurityUsers(Collections.singletonList(securityUser)),null);

        List<SecurityTenant> tenants = new ArrayList<>(links.stream().map(f -> f.getLeftside()).collect(Collectors.toMap(f -> f.getId(), f -> f, (a, b) -> a)).values());
        SecurityTenant tenantToCreateIn = links.stream().filter(f -> f.isDefualtTennant()).map(f -> f.getLeftside()).findFirst().orElse(null);
        Map<String, List<Role>> roleMap = roleToUsers.stream().map(f -> f.getLeftside()).collect(Collectors.groupingBy(f -> f.getTenant().getId()));
        return new SecurityContextBase<>()
                .setTenants(tenants)
                .setTenantToCreateIn(tenantToCreateIn)
                .setRoleMap(roleMap)
                .setUser(securityUser);
    }
}
