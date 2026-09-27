package com.avms.accounts;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Idempotent port of Django accounts/management/commands/seed_roles.py.
 * Same 6 roles; permission codes follow {module}.view / {module}.manage.
 */
@Service
public class SeedRolesService {

  static final List<String> MODULES =
      List.of("dashboard", "ventures", "customers", "suppliers", "products", "categories", "units",
          "warehouses", "inventory", "stock_movements", "sales", "sales_returns", "purchases",
          "purchase_returns", "payments", "expenses", "employees", "attendance", "reports",
          "notifications", "audit", "users", "roles", "settings");

  static final Map<String, List<String>> ROLE_MATRIX = new LinkedHashMap<>();

  static {
    ROLE_MATRIX.put("ADMIN", List.of("*"));
    ROLE_MATRIX.put("MANAGER", List.of(
        "customers.view", "customers.manage", "suppliers.view", "suppliers.manage",
        "products.view", "products.manage", "categories.view", "categories.manage",
        "units.view", "units.manage", "warehouses.view", "warehouses.manage",
        "inventory.view", "inventory.manage", "stock_movements.view", "stock_movements.manage",
        "sales.view", "sales.manage", "sales_returns.view", "sales_returns.manage",
        "purchases.view", "purchases.manage", "purchase_returns.view", "purchase_returns.manage",
        "payments.view", "expenses.view", "employees.view", "attendance.view", "attendance.manage",
        "reports.view", "notifications.view", "dashboard.view"));
    ROLE_MATRIX.put("ACCOUNTANT", List.of(
        "sales.view", "purchases.view", "payments.view", "payments.manage",
        "expenses.view", "expenses.manage", "reports.view", "notifications.view", "dashboard.view"));
    ROLE_MATRIX.put("SALES_STAFF", List.of(
        "customers.view", "customers.manage", "sales.view", "sales.manage",
        "sales_returns.view", "sales_returns.manage", "payments.view",
        "notifications.view", "dashboard.view"));
    ROLE_MATRIX.put("INVENTORY_STAFF", List.of(
        "products.view", "products.manage", "categories.view", "units.view",
        "warehouses.view", "warehouses.manage", "inventory.view", "inventory.manage",
        "stock_movements.view", "stock_movements.manage", "notifications.view", "dashboard.view"));
    ROLE_MATRIX.put("EMPLOYEE", List.of("dashboard.view"));
  }

  private final PermissionRepository permissions;
  private final RoleRepository roles;

  public SeedRolesService(PermissionRepository permissions, RoleRepository roles) {
    this.permissions = permissions;
    this.roles = roles;
  }

  @Transactional
  public void seed() {
    for (String module : MODULES) {
      for (String action : List.of("view", "manage")) {
        String code = module + "." + action;
        permissions.findByCode(code).orElseGet(() ->
            permissions.save(new Permission(code, humanize(module) + " " + action, module)));
      }
    }
    List<Permission> all = permissions.findAll();
    for (var entry : ROLE_MATRIX.entrySet()) {
      String roleCode = entry.getKey();
      Role role = roles.findByCode(roleCode)
          .orElseGet(() -> new Role(roleCode, humanize(roleCode), roleCode + " role"));
      List<String> wanted = entry.getValue();
      if (wanted.equals(List.of("*"))) {
        role.setPermissions(new java.util.HashSet<>(all));
      } else {
        java.util.Set<Permission> set = new java.util.HashSet<>();
        for (String code : wanted) {
          permissions.findByCode(code).ifPresent(set::add);
        }
        role.setPermissions(set);
      }
      role.setActive(true);
      roles.save(role);
    }
  }

  private static String humanize(String code) {
    String s = code.replace('_', ' ').toLowerCase();
    return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
  }
}
