# AVMS Database Design

## Overview

- Engine: MySQL 8.0 (`utf8mb4`).
- Managed exclusively through Django ORM + migrations (`makemigrations`/`migrate`).
- Every business record carries `created_at` / `updated_at`.
- Soft deletion (`is_deleted` + `deleted_at`) is available via the shared base
  models in `core/models.py` for records that must be preserved (products,
  customers, etc.).
- Every venture-scoped model carries a `venture_id` foreign key.

## Current Schema (Phases 1-2)

The platform models below are implemented; Auth (Phase 2) added roles,
permissions and the audit log. Feature models are added in the phases listed
further down.

### accounts_user

| Column          | Type          | Notes                          |
| --------------- | ------------- | ------------------------------ |
| id              | bigint PK     |                                |
| email           | varchar(254)  | unique, USERNAME_FIELD         |
| password        | varchar(128)  | hashed                         |
| full_name       | varchar(255)  |                                |
| phone           | varchar(20)   |                                |
| is_active       | bool          |                                |
| is_staff        | bool          |                                |
| is_superuser    | bool          |                                |
| last_login      | datetime      |                                |
| created_at      | datetime      |                                |
| updated_at      | datetime      |                                |

Custom `AUTH_USER_MODEL = accounts.User` since Phase 1 (see ARCHITECTURE).

### accounts_role / accounts_permission (Phase 2)

- `accounts_role`: code (unique: ADMIN, MANAGER, ACCOUNTANT, SALES_STAFF,
  INVENTORY_STAFF, EMPLOYEE), name, description, is_active, timestamps.
- `accounts_permission`: code (unique, e.g. `customers.manage`), name, module.
- `accounts_user_roles`: user FK + role FK (many-to-many).
- `accounts_role_permissions`: role FK + permission FK (many-to-many).

### audit_logs (Phase 2)

- `audit_logs`: user FK (nullable), action (LOGIN/LOGOUT/CREATE/UPDATE/...),
  module, object_type, object_id, ip_address, description, created_at.

Plus Django's standard `auth_group`, `auth_permission`, `django_admin_log`,
`django_content_type`, `django_migrations`, `django_session` and the
`token_blacklist` tables (SimpleJWT).

## Planned Tables by Phase

### Phase 3 - ventures (complete)
- `ventures`: venture_code (unique), venture_name, description, business_type,
  phone, email, address, city, state, pincode, status, timestamps.
- `ventures_venturecodecounter`: single-row counter (code sequence) — codes use
  `SELECT ... FOR UPDATE` + `F()` increment; counter row persists across deletes so
  codes never repeat (independent of MySQL auto-increment gaps in tests).

### Phase 4 - Masters (complete)
- `customers`: `customers_*`, customer_code (unique per venture), name, phone,
  email, address, city, state, pincode, gst_number, credit_limit, status.
- `suppliers`: `suppliers_*`, supplier_code (unique per venture), name,
  contact_person, phone, email, address, city, state, pincode, gst_number,
  payment_terms, status.
- `categories`: `products_category`, venture-scoped, category_name + description + status.
- `units`: `products_unit` static codes (kg, g, pcs, packet, litre, box).
- `products`: `products_product`, sku (unique per venture), category FK, unit FK,
  product_name, description, purchase_price, selling_price, tax_rate,
  reorder_level, status.
- Codes for all masters come from `core_sequencecounter` (one row per
  module+venture, `SELECT ... FOR UPDATE` + `F()` increment).

### Phase 5 - Inventory (complete)
- `warehouses`: warehouse_code (unique per venture), warehouse_name, address,
  city, state, pincode, manager, status.
- `inventory`: venture FK, warehouse FK, product FK, quantity, reserved_quantity,
  reorder_level; unique (warehouse, product); available = quantity - reserved.
- `stock_movements`: venture FK, warehouse FK (source), destination_warehouse FK
  (transfers), product FK, movement_type (PURCHASE | SALE | PURCHASE_RETURN |
  SALES_RETURN | ADJUSTMENT_IN | ADJUSTMENT_OUT | TRANSFER_IN | TRANSFER_OUT),
  quantity, movement_date, notes, reference_type/id, created_by, paired_movement
  (transfer pairs).

### Phase 6 - Purchases (complete)
- `purchases`: venture FK, supplier FK, warehouse FK (nullable - receipt point),
  invoice_number (auto `PINV-####`, unique per venture), purchase_date, status,
  subtotal, discount, tax, total_amount, paid_amount, returned_amount,
  due_amount, notes, created_by.
- `purchase_items`: purchase FK, product FK, quantity, unit_price, discount
  (per-line amount), tax (per-line %), total.
- `purchase_returns`: venture FK, purchase FK, return_number (auto `RET-####`,
  unique per venture), return_date, status, total_amount, notes, created_by.
- `purchase_return_items`: purchase_return FK, product FK, quantity, unit_price,
  total.
- Stock integration: creating a purchase records `PURCHASE` movements at its
  warehouse; creating a return records `PURCHASE_RETURN` movements against the
  purchase warehouse and adjusts the purchase balances.

### Phase 7 - Sales
- `sales`: venture FK, customer FK, invoice_number (unique per venture),
  sale_date, status, subtotal, discount, tax, total_amount, paid_amount,
  due_amount, notes, created_by.
- `sale_items`: sale FK, product FK, quantity, unit_price, discount, tax, total.
- `sales_returns`, `sales_return_items`.

### Phase 8 - Finance
- `payments`: venture FK, payment_type, reference_type, reference_id, amount,
  payment_method (CASH | UPI | BANK_TRANSFER | CARD | OTHER), payment_date,
  transaction_reference, notes, created_by.
- `expense_categories`: static (Electricity, Transport, Rent, Salary,
  Raw Materials, Marketing, Maintenance, Other).
- `expenses`: venture FK, category FK, amount, expense_date, payment_method,
  description, created_by.

### Phase 9 - Employees
- `employees`: venture FK, employee_code, first_name, last_name, phone, email,
  department, designation, joining_date, salary, status.

### Phase 11 - Platform
- `notifications`: user FK, message, type, read_at, created_at.
- Audit log (`audit_logs`) already exists from Phase 2; Phase 11 adds the full
  event surface for business modules and the notifications audience.
- `notifications`: user FK, type, message, is_read, created_at.

## Key Constraints & Business Rules

- Product SKU is unique per venture.
- Invoice numbers are unique per venture for sales and purchases.
- Sale quantity cannot exceed available stock.
- Purchase quantity must be non-negative.
- Payment amount cannot exceed the outstanding amount (unless configured).
- Stock mutations flow through `stock_movements`; the `inventory.quantity` is
  derived from and consistent with those movements.
- Inactive customers/suppliers are not selectable on new transactions.

## ERD

Entity relationship documentation is progressively added to this file as each
module's models land. A rendered ERD / migration overview can also be generated
with:

```bash
cd backend
python manage.py graph_models -a -o docs/diagram.png   # requires pydot + graphviz
```