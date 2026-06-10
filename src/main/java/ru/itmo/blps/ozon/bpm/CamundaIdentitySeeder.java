package ru.itmo.blps.ozon.bpm;

import java.util.List;
import org.camunda.bpm.engine.AuthorizationService;
import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.authorization.Authorization;
import org.camunda.bpm.engine.authorization.Permissions;
import org.camunda.bpm.engine.authorization.Resources;
import org.camunda.bpm.engine.identity.Group;
import org.camunda.bpm.engine.identity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.camunda.identity-seed.enabled", havingValue = "true", matchIfMissing = true)
public class CamundaIdentitySeeder {

    private static final String PROCESS_DEFINITION_KEY = "orderFulfillment";
    private static final String TASKLIST = "tasklist";
    private static final String COCKPIT = "cockpit";
    private static final String ADMIN_APP = "admin";
    private static final String CAMUNDA_ADMIN_GROUP = "camunda-admin";

    private final IdentityService identityService;
    private final AuthorizationService authorizationService;
    private final String managerPassword;
    private final String warehousePassword;
    private final String deliveryPassword;
    private final String adminPassword;

    public CamundaIdentitySeeder(IdentityService identityService,
                                 AuthorizationService authorizationService,
                                 @Value("${app.security.seed.manager-password}") String managerPassword,
                                 @Value("${app.security.seed.warehouse-password}") String warehousePassword,
                                 @Value("${app.security.seed.delivery-password}") String deliveryPassword,
                                 @Value("${app.security.seed.admin-password}") String adminPassword) {
        this.identityService = identityService;
        this.authorizationService = authorizationService;
        this.managerPassword = managerPassword;
        this.warehousePassword = warehousePassword;
        this.deliveryPassword = deliveryPassword;
        this.adminPassword = adminPassword;
    }

    @EventListener
    public void seed(ApplicationReadyEvent event) {
        ensureGroup("MANAGER", "Managers");
        ensureGroup("WAREHOUSE", "Warehouse operators");
        ensureGroup("DELIVERY", "Delivery operators");
        ensureGroup("ADMIN", "Administrators");
        ensureGroup(CAMUNDA_ADMIN_GROUP, "camunda BPM Administrators", "SYSTEM");

        ensureUser("manager", "Manager", managerPassword, "MANAGER");
        ensureUser("warehouse", "Warehouse", warehousePassword, "WAREHOUSE");
        ensureUser("delivery", "Delivery", deliveryPassword, "DELIVERY");
        ensureUser("admin", "Admin", adminPassword, "ADMIN");
        ensureMembership("admin", CAMUNDA_ADMIN_GROUP);

        grantWorkflowPermissions("MANAGER");
        grantWorkflowPermissions("WAREHOUSE");
        grantWorkflowPermissions("DELIVERY");
        grantAdminPermissions();
        grantCamundaAdminPermissions();
    }

    private void ensureGroup(String id, String name) {
        ensureGroup(id, name, "WORKFLOW");
    }

    private void ensureGroup(String id, String name, String type) {
        Group group = identityService.createGroupQuery().groupId(id).singleResult();
        if (group != null) {
            return;
        }
        group = identityService.newGroup(id);
        group.setName(name);
        group.setType(type);
        identityService.saveGroup(group);
    }

    private void ensureUser(String id, String firstName, String password, String groupId) {
        User user = identityService.createUserQuery().userId(id).singleResult();
        if (user == null) {
            user = identityService.newUser(id);
            user.setFirstName(firstName);
            user.setPassword(password);
            identityService.saveUser(user);
        }
        ensureMembership(id, groupId);
    }

    private void ensureMembership(String userId, String groupId) {
        if (identityService.createGroupQuery().groupMember(userId).groupId(groupId).singleResult() == null) {
            identityService.createMembership(userId, groupId);
        }
    }

    private void grantWorkflowPermissions(String groupId) {
        grant(groupId, Resources.APPLICATION, TASKLIST, Permissions.ACCESS);
        grant(groupId, Resources.PROCESS_DEFINITION, PROCESS_DEFINITION_KEY, Permissions.READ, Permissions.CREATE_INSTANCE);
        grant(groupId, Resources.PROCESS_INSTANCE, Authorization.ANY, Permissions.READ);
        grant(groupId, Resources.TASK, Authorization.ANY, Permissions.READ, Permissions.TASK_WORK);
    }

    private void grantAdminPermissions() {
        for (String application : List.of(TASKLIST, COCKPIT, ADMIN_APP)) {
            grant("ADMIN", Resources.APPLICATION, application, Permissions.ACCESS);
        }
        grant("ADMIN", Resources.PROCESS_DEFINITION, Authorization.ANY, Permissions.ALL);
        grant("ADMIN", Resources.PROCESS_INSTANCE, Authorization.ANY, Permissions.ALL);
        grant("ADMIN", Resources.TASK, Authorization.ANY, Permissions.ALL);
        grant("ADMIN", Resources.AUTHORIZATION, Authorization.ANY, Permissions.ALL);
        grant("ADMIN", Resources.USER, Authorization.ANY, Permissions.ALL);
        grant("ADMIN", Resources.GROUP, Authorization.ANY, Permissions.ALL);
    }

    private void grantCamundaAdminPermissions() {
        for (Resources resource : Resources.values()) {
            grant(CAMUNDA_ADMIN_GROUP, resource, Authorization.ANY, Permissions.ALL);
        }
    }

    private void grant(String groupId, Resources resource, String resourceId, Permissions... permissions) {
        long existing = authorizationService.createAuthorizationQuery()
                .groupIdIn(groupId)
                .resourceType(resource)
                .resourceId(resourceId)
                .count();
        if (existing > 0) {
            return;
        }
        Authorization authorization = authorizationService.createNewAuthorization(Authorization.AUTH_TYPE_GRANT);
        authorization.setGroupId(groupId);
        authorization.setResource(resource);
        authorization.setResourceId(resourceId);
        for (Permissions permission : permissions) {
            authorization.addPermission(permission);
        }
        authorizationService.saveAuthorization(authorization);
    }
}
