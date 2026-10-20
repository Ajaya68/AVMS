package com.avms.common;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.avms.customers.CustomerDtos;
import com.avms.expenses.ExpenseDtos;
import com.avms.payments.PaymentDtos;
import com.avms.products.ProductsDtos;
import com.avms.sales.SaleDtos;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/** PH7 regression: DTO validators enforce type/length/format/range/enum rules
 *  identically for create and partial-update (validate(false)) flows. */
class InputValidationTest {

  @Test
  void customerRejectsBadEmailOversizeAndRange() {
    var bad = new CustomerDtos.CustomerRequest(1L, "Ok", null, "not-an-email", null, null, null,
        null, null, BigDecimal.ZERO, null);
    assertThatThrownBy(() -> bad.validate(true)).isInstanceOf(IllegalArgumentException.class)
        .hasMessage("email must be a valid email address.");

    var oversize = new CustomerDtos.CustomerRequest(1L, "x".repeat(151), null, null, null, null, null,
        null, null, null, null);
    assertThatThrownBy(() -> oversize.validate(true))
        .hasMessage("name must not exceed 150 characters.");

    var negative = new CustomerDtos.CustomerRequest(1L, "Ok", null, null, null, null, null,
        null, null, BigDecimal.valueOf(-0.01d), null);
    assertThatThrownBy(() -> negative.validate(true))
        .hasMessage("credit_limit must not be negative.");

    var wrongEnum = new CustomerDtos.CustomerRequest(1L, "Ok", null, null, null, null, null,
        null, null, null, "BROKEN");
    assertThatThrownBy(() -> wrongEnum.validate(false))
        .hasMessageContaining("status must be one of");
  }

  @Test
  void partialUpdateValidatesProvidedFieldsOnly() {
    // PATCH with only name present: other fields null -> no spurious failures
    var patch = new CustomerDtos.CustomerRequest(null, "Renamed", null, "a@b.co", null, null, null,
        null, null, null, null);
    assertThatCode(() -> patch.validate(false)).doesNotThrowAnyException();
    // But an invalid provided value still trips on update
    var badPatch = new CustomerDtos.CustomerRequest(null, "Renamed", null, "a@b",
        null, null, null, null, null, null, null);
    assertThatThrownBy(() -> badPatch.validate(false)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void productRejectsNegativePricesAndBadStatus() {
    var bad = new ProductsDtos.ProductRequest(1L, null, 1L, "Soap", null,
        BigDecimal.valueOf(-1), BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO, null);
    assertThatThrownBy(() -> bad.validate(true))
        .hasMessage("purchase_price must not be negative.");

    var tax = new ProductsDtos.ProductRequest(1L, null, 1L, "Soap", null,
        BigDecimal.TEN, BigDecimal.TEN, BigDecimal.valueOf(-5), BigDecimal.ZERO, null);
    assertThatThrownBy(() -> tax.validate(false)).hasMessage("tax_rate must not be negative.");
  }

  @Test
  void saleRejectsNegativeDiscountTaxOnHeaderAndLines() {
    var validLine = new SaleDtos.SaleItemRequest(1L, BigDecimal.ONE, BigDecimal.TEN, null, null);
    var header = new SaleDtos.SaleRequest(1L, 1L, 1L, java.time.LocalDate.parse("2026-01-01"),
        BigDecimal.valueOf(-1), null, null, null, List.of(validLine));
    assertThatThrownBy(() -> header.validate(true)).hasMessage("discount must not be negative.");

    var line = new SaleDtos.SaleRequest(1L, 1L, 1L, java.time.LocalDate.parse("2026-01-01"),
        null, null, null, null, List.of(
            new SaleDtos.SaleItemRequest(1L, BigDecimal.ONE, BigDecimal.TEN, BigDecimal.valueOf(-1), null)));
    assertThatThrownBy(() -> line.validate(true)).hasMessage("discount must not be negative.");
  }

  @Test
  void expenseUpdateAlsoValidatesAmount() {
    var update = new ExpenseDtos.ExpenseRequest(1L, 1L, BigDecimal.valueOf(-5),
        java.time.LocalDate.parse("2026-01-01"), null, null);
    assertThatThrownBy(() -> update.validate(false)).hasMessage("amount must not be negative.");
  }

  @Test
  void paymentCapsReferenceLength() {
    var bad = new PaymentDtos.PaymentRequest(1L, "RECEIVED", "SALE", 1L, BigDecimal.ONE,
        java.time.LocalDate.parse("2026-01-01"), "CASH", "r".repeat(101), null);
    assertThatThrownBy(() -> bad.validate())
        .hasMessage("transaction_reference must not exceed 100 characters.");
  }
}
