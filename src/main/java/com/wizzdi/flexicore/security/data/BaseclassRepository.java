package com.wizzdi.flexicore.security.data;

import com.flexicore.annotations.IOperation;
import com.flexicore.annotations.rest.All;
import com.flexicore.model.*;
import com.flexicore.security.SecurityContext;
import com.wizzdi.flexicore.security.request.BaseclassFilter;
import org.springframework.data.util.Pair;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class BaseclassRepository {

	@PersistenceContext
	private EntityManager em;
	private Operation allOp;


	public static <T> boolean addPagination(BaseclassFilter securityEntityFilter, TypedQuery<T> q){
		if(securityEntityFilter.getPageSize()!=null&&securityEntityFilter.getCurrentPage()!=null&&securityEntityFilter.getCurrentPage()>=0&&securityEntityFilter.getPageSize()>0){
			q.setFirstResult(securityEntityFilter.getCurrentPage()*securityEntityFilter.getPageSize());
			q.setMaxResults(securityEntityFilter.getPageSize());
		}
		return false;
	}

	public <T extends Baseclass> void addBaseclassPredicates(CriteriaBuilder cb, CommonAbstractCriteria q, Path<T> r, List<Predicate> predicates, SecurityContext securityContext) {
		List<Tenant> tenants = securityContext.getTenants();
		SecurityUser securityUser=securityContext.getUser();
		Operation op=securityContext.getOperation();
		boolean impersonated= securityContext.isImpersonated();
		Set<String> tenantIds = tenants.parallelStream().map(f -> f.getId()).collect(Collectors.toSet());

		Map<String, List<Role>> rolesInTenants = securityContext.getRoleMap();

		List<Role> roles = securityContext.getRoleMap().values().stream().flatMap(List::stream).collect(Collectors.toList());
		if (isSuperAdmin(roles)) {
			return;
		}
		Pair<List<Baseclass>, List<Baseclass>> denied = getDenied(securityUser, op);
		List<Baseclass> userDenied = denied.getFirst();
		List<Baseclass> roleDenied = denied.getSecond();
		roleDenied.addAll(userDenied);


		Clazz clazz = Baseclass.getClazzByName(r.getJavaType().getCanonicalName());
		Subquery<String> sub = getBaseclassSpecificSubQeury(q, cb, roles, securityUser, tenants, op, clazz, userDenied, roleDenied);
		Subquery<String> subPermissionGroup = getPermissionGroupSubQuery(q, cb, roles, securityUser, tenants, op, clazz, userDenied, roleDenied);

		Predicate premissive = cb.or();
		Predicate all = cb.or();
		//check for allow all links for tenants or securityUser - if link is for securityUser grant permission for all objects in all tenants , if allow link
		List<SecurityLink> hasAllLink = getAllowAllLinks(cb, roles, securityUser, tenants, op);
		for (SecurityLink securityLink : hasAllLink) {
			Predicate mid = cb.or();
			if (!tenants.isEmpty() && securityLink.getLeftside() instanceof SecurityUser) {
				mid = r.get(Baseclass_.tenant).in(tenants);
			} else {
				if (securityLink.getLeftside() instanceof Tenant) {
					mid = cb.equal(r.get(Baseclass_.tenant), securityLink.getLeftside());
				} else {
					if (securityLink.getLeftside() instanceof Role) {
						Role role = (Role) securityLink.getLeftside();
						if (role.getTenant() != null && rolesInTenants.get(role.getTenant().getId()) != null) {
							mid = cb.equal(r.get(Baseclass_.tenant), role.getTenant());

						}
					}
				}
			}
			if (!roleDenied.isEmpty()) {
				mid = cb.and(mid, cb.not(r.in(roleDenied)));
			}
			premissive = cb.or(premissive, mid);
		}

		Subquery<String> subPremissive = getPremissiveSubQueryForTenantAndUser(q, cb, securityUser, tenants, op);
		premissive = cb.or(
				premissive,
				cb.and(r.get(Baseclass_.clazz).get(Clazz_.id).in(subPremissive),
						r.get(Baseclass_.tenant).in(tenants)));


		Map<String, Tenant> tenantMap = tenants.parallelStream().collect(Collectors.toMap(f -> f.getId(), f -> f, (a, b) -> a));
		for (Map.Entry<String, List<Role>> entry : rolesInTenants.entrySet()) {
			Tenant tenant = tenantMap.get(entry.getKey());
			if (tenant != null) {
				Subquery<String> subPremissiveRole = getPremissiveSubQueryForRole(q, cb, entry.getValue(), op);
				premissive = cb.or(premissive, cb.and(r.get(Baseclass_.clazz).get(Clazz_.id).in(subPremissiveRole), cb.equal(r.get(Baseclass_.tenant), tenant)));


			}

		}


		Predicate creatorPred = cb.equal(r.get(Baseclass_.creator), securityUser);
		if (impersonated) {
			creatorPred = cb.and(creatorPred, r.get(Baseclass_.tenant).in(tenants));
		}
		Predicate predicate = cb.or(

				r.get(Baseclass_.id).in(sub),
				r.get(Baseclass_.id).in(subPermissionGroup),
				premissive,
				all,


				//creator
				creatorPred,
				//enforce tenancy on Premissive Creator value
				cb.and(cb.isNull(r.get(Baseclass_.creator)), r.get(Baseclass_.tenant).in(tenants))


		);


		List<Predicate> ors = new ArrayList<>();
		ors.add(predicate);


		//ors.add(predicate);

		Predicate[] preds = new Predicate[ors.size()];
		preds = ors.toArray(preds);
		Predicate securityPredicates = cb.or(preds);
		predicates.add(securityPredicates);

	}

	/**
	 * @param securityUser securityUser
	 * @param op operation
	 * @return a list of denied baseclasses  for securityUser using Operation
	 */
	public Pair<List<Baseclass>, List<Baseclass>> getDenied(SecurityUser securityUser, Operation op) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<UserToBaseClass> q = cb.createQuery(UserToBaseClass.class);
		Root<UserToBaseClass> r = q.from(UserToBaseClass.class);

		Predicate p1 = cb.and(
				cb.or(cb.isFalse(r.get(SecurityLink_.softDelete)), r.get(SecurityLink_.softDelete).isNull()),
				cb.equal(r.get(UserToBaseClass_.leftside), securityUser),
				cb.equal(r.get(UserToBaseClass_.value), op),
				cb.equal(r.get(UserToBaseClass_.simplevalue), IOperation.Access.deny.name())
		);

		//p1=cb.or(p1,p2);
		List<Predicate> preds = new ArrayList<>();
		preds.add(p1);
		q.select(r).where(preds.toArray(Predicate[]::new));
		TypedQuery<UserToBaseClass> query = em.createQuery(q);

		List<UserToBaseClass> deniedUsers = query.getResultList();
		CriteriaQuery<RoleToBaseclass> q1 = cb.createQuery(RoleToBaseclass.class);
		Root<RoleToBaseclass> r1 = q1.from(RoleToBaseclass.class);
		Join<RoleToBaseclass, Role> j1 = cb.treat(r1.join(RoleToBaseclass_.leftside, JoinType.LEFT), Role.class);
		Join<Role, RoleToUser> j2 = j1.join(Role_.roleToUser, JoinType.INNER);

		Predicate p2 = cb.and(
				cb.or(cb.isFalse(r1.get(SecurityLink_.softDelete)), r1.get(SecurityLink_.softDelete).isNull()),

				cb.equal(j2.get(RoleToUser_.rightside), securityUser),
				cb.equal(r1.get(RoleToBaseclass_.value), op),
				cb.equal(r1.get(RoleToBaseclass_.simplevalue), IOperation.Access.deny.name())
		);
		List<Predicate> preds1 = new ArrayList<>();
		preds.add(p2);
		q1.select(r1).where(preds.toArray(Predicate[]::new));
		TypedQuery<RoleToBaseclass> query1 = em.createQuery(q1);
		List<RoleToBaseclass> deniedRoles = query1.getResultList();
		List<Baseclass> deniedUsersBase = new ArrayList<>();
		List<Baseclass> deniedRolesBase = new ArrayList<>();
		for (RoleToBaseclass roleToBaseclass : deniedRoles) {
			deniedRolesBase.add(roleToBaseclass.getRightside());
		}
		for (UserToBaseClass userToBaseClass : deniedUsers) {
			deniedUsersBase.add(userToBaseClass.getRightside());
		}

		return Pair.of(deniedUsersBase, deniedRolesBase);
	}

	private Subquery<String> getPermissionGroupSubQuery(CommonAbstractCriteria query, CriteriaBuilder cb, List<Role> roles, SecurityUser securityUser, List<Tenant> tenants, Operation op, Clazz clazz, List<Baseclass> userDenied, List<Baseclass> roleDenied) {
		Subquery<String> sub = query.subquery(String.class);
		Root<SecurityLink> securityLinkRoot = sub.from(SecurityLink.class);
		Join<SecurityLink, PermissionGroup> rightsideJoin = cb.treat(securityLinkRoot.join(SecurityLink_.rightside), PermissionGroup.class);
		Root<UserToBaseClass> userToBaseClassRoot = cb.treat(securityLinkRoot, UserToBaseClass.class);
		Root<RoleToBaseclass> roleToBaseclassRoot = cb.treat(securityLinkRoot, RoleToBaseclass.class);
		Root<TenantToBaseClassPremission> tenantToBaseClassPremissionRoot = cb.treat(securityLinkRoot, TenantToBaseClassPremission.class);
		Join<PermissionGroup, PermissionGroupToBaseclass> permissionGroupLinkJoin = rightsideJoin.join(PermissionGroup_.links);
		Join<PermissionGroupToBaseclass, Baseclass> permissionGroupTargetJoin = permissionGroupLinkJoin.join(PermissionGroupToBaseclass_.rightside);


		Predicate linkPredicate = cb.and(
				createBaseclassSpecificPredicate(cb, roles, securityUser, tenants, op, userDenied, roleDenied, securityLinkRoot, userToBaseClassRoot, roleToBaseclassRoot, tenantToBaseClassPremissionRoot),
				cb.isFalse(permissionGroupLinkJoin.get(PermissionGroupToBaseclass_.softDelete)),
				cb.isFalse(rightsideJoin.get(PermissionGroup_.softDelete))
		);

		sub.select(permissionGroupTargetJoin.get(Baseclass_.id)).where(linkPredicate);
		return sub;

	}

	private Subquery<String> getBaseclassSpecificSubQeury(CommonAbstractCriteria query, CriteriaBuilder cb, List<Role> roles, SecurityUser securityUser
			, List<Tenant> tenants, Operation op, Clazz clazz, List<Baseclass> userDenied, List<Baseclass> roleDenied) {
		Subquery<String> sub = query.subquery(String.class);
		Root<SecurityLink> securityLinkRoot = sub.from(SecurityLink.class);
		Join<SecurityLink, Baseclass> rightsideJoin = securityLinkRoot.join(SecurityLink_.rightside);
		Root<UserToBaseClass> userToBaseClassRoot = cb.treat(securityLinkRoot, UserToBaseClass.class);
		Root<RoleToBaseclass> roleToBaseclassRoot = cb.treat(securityLinkRoot, RoleToBaseclass.class);
		Root<TenantToBaseClassPremission> tenantToBaseClassPremissionRoot = cb.treat(securityLinkRoot, TenantToBaseClassPremission.class);


		Predicate linkPredicate = createBaseclassSpecificPredicate(cb, roles, securityUser, tenants, op, userDenied, roleDenied, securityLinkRoot, userToBaseClassRoot, roleToBaseclassRoot, tenantToBaseClassPremissionRoot);
		sub.select(rightsideJoin.get(Baseclass_.id)).where(linkPredicate);
		return sub;

	}

	private Predicate createBaseclassSpecificPredicate(CriteriaBuilder cb, List<Role> roles, SecurityUser securityUser, List<Tenant> tenants, Operation op, List<Baseclass> userDenied, List<Baseclass> roleDenied, Root<SecurityLink> securityLinkRoot, Root<UserToBaseClass> userToBaseClassRoot, Root<RoleToBaseclass> roleToBaseclassRoot, Root<TenantToBaseClassPremission> tenantToBaseClassPremissionRoot) {
		Operation allOpId = getAllOperation();

		Predicate rolesPredicate = cb.or();
		if (!roles.isEmpty()) {
			rolesPredicate = cb.and(
					roleToBaseclassRoot.get(RoleToBaseclass_.leftside).in(roles));

		}


		Predicate userPredicate = cb.and(
				cb.equal(userToBaseClassRoot.get(UserToBaseClass_.leftside), securityUser));

		Predicate tenantPredicate = cb.and(
				tenantToBaseClassPremissionRoot.get(TenantToBaseClassPremission_.leftside).in(tenants));


		if (!userDenied.isEmpty()) {
			userPredicate = cb.and(userPredicate, cb.not(securityLinkRoot.get(SecurityLink_.rightside).in(userDenied)));
		}


		if (!roleDenied.isEmpty()) {
			rolesPredicate = cb.and(rolesPredicate, cb.not(securityLinkRoot.get(SecurityLink_.rightside).in(roleDenied)));
			tenantPredicate = cb.and(tenantPredicate, cb.not(securityLinkRoot.get(SecurityLink_.rightside).in(roleDenied)));
		}

		return cb.and(
				cb.or(cb.isFalse(securityLinkRoot.get(SecurityLink_.softDelete)), securityLinkRoot.get(SecurityLink_.softDelete).isNull()),
				cb.or(cb.equal(securityLinkRoot.get(SecurityLink_.value), op), cb.equal(securityLinkRoot.get(SecurityLink_.value), allOpId)),
				//cb.or(cb.equal(securityLinkRoot.get(SecurityLink_.rightside).get(Baseclass_.clazz),clazz)),
				cb.equal(securityLinkRoot.get(SecurityLink_.simplevalue), IOperation.Access.allow.name()),
				cb.or(
						userPredicate,
						rolesPredicate,
						tenantPredicate


				)


		);
	}

	private Operation getAllOperation() {
		if (allOp == null) {
			allOp = em.find(Operation.class,Baseclass.generateUUIDFromString(All.class.getCanonicalName()));
		}
		return allOp;
	}

	private Subquery<String> getPremissiveSubQueryForRole(CommonAbstractCriteria query, CriteriaBuilder cb, List<Role> roles, Operation op) {

		Subquery<String> subPremissive = query.subquery(String.class);
		Root<SecurityLink> securityLinkRootPremissive = subPremissive.from(SecurityLink.class);
		Join<SecurityLink, Clazz> rightside = cb.treat(securityLinkRootPremissive.join(SecurityLink_.rightside), Clazz.class);
		Root<RoleToBaseclass> roleToBaseclassRoot = cb.treat(securityLinkRootPremissive, RoleToBaseclass.class);
		Operation allOpId = getAllOperation();
		Predicate rolesPredicatePremissive = cb.or();
		if (!roles.isEmpty()) {
			rolesPredicatePremissive = cb.and(
					roleToBaseclassRoot.get(RoleToBaseclass_.leftside).in(roles));

		}


		Predicate premissive = cb.and(
				cb.isFalse(securityLinkRootPremissive.get(SecurityLink_.softDelete)),
				cb.or(cb.equal(securityLinkRootPremissive.get(SecurityLink_.value), op), cb.equal(securityLinkRootPremissive.get(SecurityLink_.value), allOpId)),
				cb.or(cb.equal(securityLinkRootPremissive.get(SecurityLink_.rightside).type(), Clazz.class), cb.equal(securityLinkRootPremissive.get(SecurityLink_.rightside).type(), ClazzLink.class)),
				cb.equal(securityLinkRootPremissive.get(SecurityLink_.simplevalue), IOperation.Access.allow.name()),
				rolesPredicatePremissive


		);

		subPremissive.select(rightside.get(Clazz_.id)).where(premissive);
		return subPremissive;

	}

	private Subquery<String> getPremissiveSubQueryForTenantAndUser(CommonAbstractCriteria query, CriteriaBuilder cb, SecurityUser securityUser, List<Tenant> tenants, Operation op) {

		Subquery<String> subPremissive = query.subquery(String.class);
		Root<SecurityLink> securityLinkRootPremissive = subPremissive.from(SecurityLink.class);
		Join<SecurityLink, Clazz> rightside = cb.treat(securityLinkRootPremissive.join(SecurityLink_.rightside), Clazz.class);
		Root<UserToBaseClass> userToBaseClassRoot = cb.treat(securityLinkRootPremissive, UserToBaseClass.class);
		Root<TenantToBaseClassPremission> tenantToBaseClassPremissionRoot = cb.treat(securityLinkRootPremissive, TenantToBaseClassPremission.class);
		Operation allOpId = getAllOperation();


		Predicate userPredicatePremissive = cb.and(
				cb.equal(userToBaseClassRoot.get(UserToBaseClass_.leftside), securityUser));
		Predicate tenantPredicatePremissive = cb.and(
				tenantToBaseClassPremissionRoot.get(TenantToBaseClassPremission_.leftside).in(tenants));
		Predicate premissive = cb.and(
				cb.isFalse(securityLinkRootPremissive.get(SecurityLink_.softDelete)),
				cb.or(cb.equal(securityLinkRootPremissive.get(SecurityLink_.value), op), cb.equal(securityLinkRootPremissive.get(SecurityLink_.value), allOpId)),
				cb.or(cb.equal(securityLinkRootPremissive.get(SecurityLink_.rightside).type(), Clazz.class), cb.equal(securityLinkRootPremissive.get(SecurityLink_.rightside).type(), ClazzLink.class)),
				cb.equal(securityLinkRootPremissive.get(SecurityLink_.simplevalue), IOperation.Access.allow.name()),
				cb.or(
						userPredicatePremissive,
						tenantPredicatePremissive

				)


		);

		subPremissive.select(rightside.get(Clazz_.id)).where(premissive);
		return subPremissive;

	}

	private List<SecurityLink> getAllowAllLinks(CriteriaBuilder cb, List<Role> roles, SecurityUser securityUser, List<Tenant> tenants, Operation op) {
     /*   Clazz tenantToBaseClassClazz = Baseclass.getClazzbyname(TenantToBaseClassPremission.class.getCanonicalName());
        Clazz roleToBaseClassClazz = Baseclass.getClazzbyname(RoleToBaseclass.class.getCanonicalName());
        Clazz userToBaseClassClazz = Baseclass.getClazzbyname(UserToBaseClass.class.getCanonicalName());*/
		CriteriaQuery<SecurityLink> subPremissive = cb.createQuery(SecurityLink.class);
		Root<SecurityLink> securityLinkRootPremissive = subPremissive.from(SecurityLink.class);
		Root<UserToBaseClass> userToBaseClassRoot = cb.treat(securityLinkRootPremissive, UserToBaseClass.class);
		Root<RoleToBaseclass> roleToBaseclassRoot = cb.treat(securityLinkRootPremissive, RoleToBaseclass.class);
		Root<TenantToBaseClassPremission> tenantToBaseClassPremissionRoot = cb.treat(securityLinkRootPremissive, TenantToBaseClassPremission.class);
		Operation allOpId = getAllOperation();

		Predicate rolesPredicatePremissive = cb.or();
		if (!roles.isEmpty()) {
			rolesPredicatePremissive = cb.and(
					roleToBaseclassRoot.get(RoleToBaseclass_.leftside).in(roles));

		}


		Predicate userPredicatePremissive = cb.and(
				cb.equal(userToBaseClassRoot.get(UserToBaseClass_.leftside), securityUser));
		Predicate tenantPredicatePremissive = cb.or();
		if (!tenants.isEmpty()) {
			tenantPredicatePremissive = cb.and(
					tenantToBaseClassPremissionRoot.get(TenantToBaseClassPremission_.leftside).in(tenants));
		}

		Clazz securityWildcard = Baseclass.getClazzByName(SecurityWildcard.class.getCanonicalName());
		Predicate premissive = cb.and(
				cb.or(cb.equal(securityLinkRootPremissive.get(SecurityLink_.value), op), cb.equal(securityLinkRootPremissive.get(SecurityLink_.value), allOpId)),
				cb.equal(securityLinkRootPremissive.get(SecurityLink_.rightside), securityWildcard),
				cb.or(
						userPredicatePremissive,
						tenantPredicatePremissive,
						rolesPredicatePremissive

				),
				cb.isFalse(securityLinkRootPremissive.get(SecurityLink_.softDelete))


		);

		subPremissive.select(securityLinkRootPremissive).where(premissive);
		TypedQuery<SecurityLink> query = em.createQuery(subPremissive);
		List<SecurityLink> all = query.getResultList();

		return all;

	}

	private boolean isSuperAdmin(List<Role> roles) {
		for (Role role : roles) {
			if (role.getId().equals("HzFnw-nVR0Olq6WBvwKcQg")) {
				return true;
			}

		}
		return false;

	}

}
