package ru.itmo.blps.ozon.security;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ru.itmo.blps.ozon.entity.Delivery;
import ru.itmo.blps.ozon.entity.Order;
import ru.itmo.blps.ozon.entity.OrderItem;
import ru.itmo.blps.ozon.entity.OrderStatus;
import ru.itmo.blps.ozon.repository.OrderRepository;
import ru.itmo.blps.ozon.security.entity.Privilege;
import ru.itmo.blps.ozon.security.entity.Role;
import ru.itmo.blps.ozon.security.entity.UserAccount;
import ru.itmo.blps.ozon.security.repository.PrivilegeRepository;
import ru.itmo.blps.ozon.security.repository.RoleRepository;
import ru.itmo.blps.ozon.security.repository.UserAccountRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PrivilegeRepository privilegeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        userAccountRepository.deleteAll();
        roleRepository.deleteAll();
        privilegeRepository.deleteAll();
        seedSecurityModel();
    }

    @Test
    void anonymousGetOrdersShouldReturnUnauthorizedJson() throws Exception {
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("WWW-Authenticate"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Authentication is required"))
                .andExpect(jsonPath("$.validationErrors").value(Matchers.nullValue()));
    }

    @Test
    void anonymousCreateOrderShouldReturnUnauthorizedJson() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(APPLICATION_JSON)
                        .content(createOrderRequest()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Authentication is required"))
                .andExpect(jsonPath("$.validationErrors").value(Matchers.nullValue()));
    }

    @Test
    void managerShouldBeAbleToCreateReadAcceptAndCancelOrders() throws Exception {
        Order createdOrder = saveOrder(OrderStatus.CREATED);
        Order cancellableOrder = saveOrder(OrderStatus.CREATED);

        mockMvc.perform(post("/api/orders")
                        .with(httpBasic("manager", "manager123"))
                        .contentType(APPLICATION_JSON)
                        .content(createOrderRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CREATED"));

        mockMvc.perform(get("/api/orders")
                        .with(httpBasic("manager", "manager123")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/orders/" + createdOrder.getId())
                        .with(httpBasic("manager", "manager123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdOrder.getId()));

        mockMvc.perform(post("/api/orders/" + createdOrder.getId() + "/accept")
                        .with(httpBasic("manager", "manager123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        mockMvc.perform(post("/api/orders/" + cancellableOrder.getId() + "/cancel")
                        .with(httpBasic("manager", "manager123"))
                        .contentType(APPLICATION_JSON)
                        .content(cancelOrderRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void managerShouldBeForbiddenToPackHandoffAndDeliver() throws Exception {
        mockMvc.perform(post("/api/orders/100/pack")
                        .with(httpBasic("manager", "manager123")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Access is denied"))
                .andExpect(jsonPath("$.validationErrors").value(Matchers.nullValue()));

        mockMvc.perform(post("/api/orders/100/handoff")
                        .with(httpBasic("manager", "manager123"))
                        .contentType(APPLICATION_JSON)
                        .content(handoffRequest()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        mockMvc.perform(post("/api/orders/100/deliver")
                        .with(httpBasic("manager", "manager123")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void warehouseShouldBeAbleToReadAndPackOnly() throws Exception {
        Order acceptedOrder = saveOrder(OrderStatus.ACCEPTED);

        mockMvc.perform(get("/api/orders")
                        .with(httpBasic("warehouse", "warehouse123")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/orders/" + acceptedOrder.getId() + "/pack")
                        .with(httpBasic("warehouse", "warehouse123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PACKED"));

        mockMvc.perform(post("/api/orders")
                        .with(httpBasic("warehouse", "warehouse123"))
                        .contentType(APPLICATION_JSON)
                        .content(createOrderRequest()))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/orders/" + saveOrder(OrderStatus.CREATED).getId() + "/accept")
                        .with(httpBasic("warehouse", "warehouse123")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/orders/" + saveOrder(OrderStatus.CREATED).getId() + "/cancel")
                        .with(httpBasic("warehouse", "warehouse123"))
                        .contentType(APPLICATION_JSON)
                        .content(cancelOrderRequest()))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/orders/" + acceptedOrder.getId() + "/handoff")
                        .with(httpBasic("warehouse", "warehouse123"))
                        .contentType(APPLICATION_JSON)
                        .content(handoffRequest()))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/orders/" + acceptedOrder.getId() + "/deliver")
                        .with(httpBasic("warehouse", "warehouse123")))
                .andExpect(status().isForbidden());
    }

    @Test
    void deliveryShouldBeAbleToReadHandoffAndDeliverOnly() throws Exception {
        Order packedOrder = saveOrder(OrderStatus.PACKED);
        Order inDeliveryOrder = saveOrder(OrderStatus.IN_DELIVERY);

        mockMvc.perform(get("/api/orders")
                        .with(httpBasic("delivery", "delivery123")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/orders/" + packedOrder.getId() + "/handoff")
                        .with(httpBasic("delivery", "delivery123"))
                        .contentType(APPLICATION_JSON)
                        .content(handoffRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_DELIVERY"));

        mockMvc.perform(post("/api/orders/" + inDeliveryOrder.getId() + "/deliver")
                        .with(httpBasic("delivery", "delivery123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED"));

        mockMvc.perform(post("/api/orders")
                        .with(httpBasic("delivery", "delivery123"))
                        .contentType(APPLICATION_JSON)
                        .content(createOrderRequest()))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/orders/" + saveOrder(OrderStatus.CREATED).getId() + "/accept")
                        .with(httpBasic("delivery", "delivery123")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/orders/" + saveOrder(OrderStatus.ACCEPTED).getId() + "/pack")
                        .with(httpBasic("delivery", "delivery123")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/orders/" + saveOrder(OrderStatus.CREATED).getId() + "/cancel")
                        .with(httpBasic("delivery", "delivery123"))
                        .contentType(APPLICATION_JSON)
                        .content(cancelOrderRequest()))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminShouldBeAbleToAccessEveryOperation() throws Exception {
        Order createdOrder = saveOrder(OrderStatus.CREATED);
        Order acceptedOrder = saveOrder(OrderStatus.ACCEPTED);
        Order packedOrder = saveOrder(OrderStatus.PACKED);
        Order inDeliveryOrder = saveOrder(OrderStatus.IN_DELIVERY);
        Order cancellableOrder = saveOrder(OrderStatus.CREATED);

        mockMvc.perform(get("/api/orders")
                        .with(httpBasic("admin", "admin123")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/orders/" + createdOrder.getId())
                        .with(httpBasic("admin", "admin123")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/orders")
                        .with(httpBasic("admin", "admin123"))
                        .contentType(APPLICATION_JSON)
                        .content(createOrderRequest()))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/orders/" + createdOrder.getId() + "/accept")
                        .with(httpBasic("admin", "admin123")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/orders/" + acceptedOrder.getId() + "/pack")
                        .with(httpBasic("admin", "admin123")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/orders/" + packedOrder.getId() + "/handoff")
                        .with(httpBasic("admin", "admin123"))
                        .contentType(APPLICATION_JSON)
                        .content(handoffRequest()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/orders/" + inDeliveryOrder.getId() + "/deliver")
                        .with(httpBasic("admin", "admin123")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/orders/" + cancellableOrder.getId() + "/cancel")
                        .with(httpBasic("admin", "admin123"))
                        .contentType(APPLICATION_JSON)
                        .content(cancelOrderRequest()))
                .andExpect(status().isOk());
    }

    private void seedSecurityModel() {
        Privilege create = privilegeRepository.save(new Privilege(PrivilegeName.ORDER_CREATE));
        Privilege read = privilegeRepository.save(new Privilege(PrivilegeName.ORDER_READ));
        Privilege accept = privilegeRepository.save(new Privilege(PrivilegeName.ORDER_ACCEPT));
        Privilege pack = privilegeRepository.save(new Privilege(PrivilegeName.ORDER_PACK));
        Privilege handoff = privilegeRepository.save(new Privilege(PrivilegeName.ORDER_HANDOFF));
        Privilege deliver = privilegeRepository.save(new Privilege(PrivilegeName.ORDER_DELIVER));
        Privilege cancel = privilegeRepository.save(new Privilege(PrivilegeName.ORDER_CANCEL));

        Role manager = saveRole(RoleName.ROLE_MANAGER, create, read, accept, cancel);
        Role warehouse = saveRole(RoleName.ROLE_WAREHOUSE, read, pack);
        Role deliveryRole = saveRole(RoleName.ROLE_DELIVERY, read, handoff, deliver);
        Role admin = saveRole(RoleName.ROLE_ADMIN, create, read, accept, pack, handoff, deliver, cancel);

        saveUser("manager", "manager123", manager);
        saveUser("warehouse", "warehouse123", warehouse);
        saveUser("delivery", "delivery123", deliveryRole);
        saveUser("admin", "admin123", admin);
    }

    private Role saveRole(RoleName roleName, Privilege... privileges) {
        Role role = new Role(roleName);
        role.setPrivileges(new LinkedHashSet<>(Set.of(privileges)));
        return roleRepository.save(role);
    }

    private void saveUser(String username, String password, Role role) {
        UserAccount userAccount = new UserAccount();
        userAccount.setUsername(username);
        userAccount.setPasswordHash(passwordEncoder.encode(password));
        userAccount.setEnabled(true);
        userAccount.setRoles(new LinkedHashSet<>(Set.of(role)));
        userAccountRepository.save(userAccount);
    }

    private Order saveOrder(OrderStatus status) {
        LocalDateTime now = LocalDateTime.of(2026, 3, 16, 13, 15, 30);
        Order order = Order.create("Ivan Petrov", "Saint Petersburg, Nevsky 1", now);
        order.markStockAvailable(true);
        order.changeStatus(status);

        OrderItem item = OrderItem.create("SKU-" + UUID.randomUUID(), "Phone", 1, BigDecimal.valueOf(499.99));
        order.addItem(item);

        if (status == OrderStatus.IN_DELIVERY || status == OrderStatus.DELIVERED) {
            Delivery delivery = Delivery.create("OZON Delivery", "TRACK-" + UUID.randomUUID(), now);
            if (status == OrderStatus.DELIVERED) {
                delivery.markDelivered(now.plusHours(2));
            }
            order.setDelivery(delivery);
        }

        return orderRepository.save(order);
    }

    private String createOrderRequest() {
        return """
                {
                  "customerName": "Ivan Petrov",
                  "deliveryAddress": "Saint Petersburg, Nevsky 1",
                  "items": [
                    {
                      "sku": "SKU-1",
                      "productName": "Phone",
                      "quantity": 2,
                      "unitPrice": 499.99
                    }
                  ]
                }
                """;
    }

    private String handoffRequest() {
        return """
                {
                  "carrierName": "OZON Delivery",
                  "trackingNumber": "TRACK-001"
                }
                """;
    }

    private String cancelOrderRequest() {
        return """
                {
                  "reason": "Client cancelled the order"
                }
                """;
    }
}
