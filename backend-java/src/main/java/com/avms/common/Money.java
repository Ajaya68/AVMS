package com.avms.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Bill totals math shared by sales/purchases: mirrors Django recompute logic. */
public final class Money {

  private Money() {}

  public static BigDecimal amount(BigDecimal value) {
    if (value == null) {
      return BigDecimal.ZERO.setScale(2);
    }
    return value.setScale(2, RoundingMode.HALF_UP);
  }

  /** Line total = (qty * price - discount) * (1 + tax/100). */
  public static BigDecimal lineTotal(BigDecimal quantity, BigDecimal unitPrice, BigDecimal discount,
      BigDecimal taxRate) {
    BigDecimal qty = quantity == null ? BigDecimal.ZERO : quantity;
    BigDecimal price = unitPrice == null ? BigDecimal.ZERO : unitPrice;
    BigDecimal disc = discount == null ? BigDecimal.ZERO : discount;
    BigDecimal tax = taxRate == null ? BigDecimal.ZERO : taxRate;
    BigDecimal net = qty.multiply(price).subtract(disc);
    BigDecimal factor = BigDecimal.ONE.add(tax.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP));
    return amount(net.multiply(factor));
  }

  /** Due = total - paid - returned, floored at zero. */
  public static BigDecimal due(BigDecimal total, BigDecimal paid, BigDecimal returned) {
    BigDecimal t = total == null ? BigDecimal.ZERO : total;
    BigDecimal p = paid == null ? BigDecimal.ZERO : paid;
    BigDecimal r = returned == null ? BigDecimal.ZERO : returned;
    BigDecimal due = t.subtract(p).subtract(r);
    if (due.compareTo(BigDecimal.ZERO) < 0) {
      return BigDecimal.ZERO.setScale(2);
    }
    return amount(due);
  }
}
