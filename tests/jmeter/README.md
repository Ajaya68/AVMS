# JMeter plans (one per wave; run pre-cutover per environment)

- `smoke.jmx` — login → refresh → list ventures → health (baseline p95 gate).
- `masters_crud.jmx` — customer/supplier/product CRUD burst.
- `sale_post_oversell.jmx` — concurrent SALE on same product: must serialize, zero oversell.
- `purchase_settle_overpay.jmx` — settle then over-pay: 400 expected, payment count unchanged.
- `reports_30d.jmx` — 30-day sales/purchases/inventory/financial reports.
- `stock_movement_burst.jmx` — concurrent movements incl. transfers.

Gates: 0 oversell/overpay under concurrency; p95 within 1.5x Django baseline or documented cause.
(JMX bodies to be recorded from the Spring API once each wave lands; this file is the index, not a placeholder for results.)
