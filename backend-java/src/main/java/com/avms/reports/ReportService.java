package com.avms.reports;

import com.avms.common.Money;
import com.avms.common.VentureContextHolder;
import com.avms.expenses.ExpenseRepository;
import com.avms.inventory.Stock;
import com.avms.inventory.StockRepository;
import com.avms.payments.PaymentRepository;
import com.avms.purchases.PurchaseRepository;
import com.avms.sales.SaleItemRepository;
import com.avms.sales.SaleRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Port of Django reports views: period sales/purchases, stock/valuation, financials. */
@Service
public class ReportService {

  private final SaleRepository sales;
  private final SaleItemRepository saleItems;
  private final PurchaseRepository purchases;
  private final StockRepository stocks;
  private final ExpenseRepository expenses;
  private final PaymentRepository payments;

  public ReportService(SaleRepository sales, SaleItemRepository saleItems, PurchaseRepository purchases,
      StockRepository stocks, ExpenseRepository expenses,
      PaymentRepository payments) {
    this.sales = sales;
    this.saleItems = saleItems;
    this.purchases = purchases;
    this.stocks = stocks;
    this.expenses = expenses;
    this.payments = payments;
  }

  @Transactional(readOnly = true)
  public ReportDtos.SalesReport salesReport(LocalDate from, LocalDate to) {
    Long ventureId = VentureContextHolder.get();
    LocalDate end = to == null ? LocalDate.now() : to;
    LocalDate start = from == null ? end.minusDays(29) : from;

    long count = sales.countSales(ventureId, start, end);
    BigDecimal total = Money.amount(sales.totalSales(ventureId, start, end));
    BigDecimal returned = Money.amount(sales.returnedSales(ventureId, start, end));
    BigDecimal itemsSold = Money.amount(saleItems.itemsSold(ventureId, start, end));
    ReportDtos.SalesSummary salesSummary = new ReportDtos.SalesSummary(count, itemsSold, total,
        returned, Money.amount(total.subtract(returned)));

    List<ReportDtos.TopProduct> top = new ArrayList<>();
    for (Object[] row : saleItems.topProducts(ventureId, start, end, PageRequest.of(0, 10))) {
      top.add(new ReportDtos.TopProduct(((Number) row[0]).longValue(), String.valueOf(row[1]),
          String.valueOf(row[2]), Money.amount((BigDecimal) row[3]), Money.amount((BigDecimal) row[4])));
    }
    return new ReportDtos.SalesReport(salesSummary, top, series(sales.salesSeries(ventureId, start, end),
        start, end));
  }

  @Transactional(readOnly = true)
  public ReportDtos.PurchasesReport purchasesReport(LocalDate from, LocalDate to) {
    Long ventureId = VentureContextHolder.get();
    LocalDate end = to == null ? LocalDate.now() : to;
    LocalDate start = from == null ? end.minusDays(29) : from;

    long count = purchases.countPurchases(ventureId, start, end);
    BigDecimal total = Money.amount(purchases.totalPurchases(ventureId, start, end));
    BigDecimal returned = Money.amount(purchases.returnedPurchases(ventureId, start, end));
    ReportDtos.PurchasesSummary purchasesSummary = new ReportDtos.PurchasesSummary(count,
        Money.amount(BigDecimal.ZERO), total, returned, Money.amount(total.subtract(returned)));

    List<ReportDtos.TopSupplier> top = new ArrayList<>();
    for (Object[] row : purchases.topSuppliers(ventureId, start, end, PageRequest.of(0, 10))) {
      top.add(new ReportDtos.TopSupplier(((Number) row[0]).longValue(), String.valueOf(row[1]),
          Money.amount((BigDecimal) row[2]), ((Number) row[3]).longValue()));
    }
    return new ReportDtos.PurchasesReport(purchasesSummary, top,
        series(purchases.purchasesSeries(ventureId, start, end), start, end));
  }

  @Transactional(readOnly = true)
  public ReportDtos.InventoryReport inventoryReport() {
    Long ventureId = VentureContextHolder.get();
    List<Stock> rows = ventureId == null ? stocks.findAll() : stocks.findByVentureId(ventureId);

    List<ReportDtos.StockLine> items = new ArrayList<>();
    List<ReportDtos.LowStockLine> low = new ArrayList<>();
    java.util.Set<Long> distinct = new java.util.HashSet<>();
    java.util.Set<Long> active = new java.util.HashSet<>();
    BigDecimal totalQty = BigDecimal.ZERO;
    BigDecimal stockValue = BigDecimal.ZERO;
    for (Stock stock : rows) {
      BigDecimal qty = stock.getQuantity() == null ? BigDecimal.ZERO : stock.getQuantity();
      BigDecimal cost = stock.getProduct().getPurchasePrice() == null
          ? BigDecimal.ZERO : stock.getProduct().getPurchasePrice();
      BigDecimal valuation = Money.amount(qty.multiply(cost));
      items.add(new ReportDtos.StockLine(stock.getProduct().getId(), stock.getProduct().getSku(),
          stock.getProduct().getProductName(), stock.getWarehouse().getWarehouseName(),
          stock.getWarehouse().getWarehouseCode(), Money.amount(qty), Money.amount(cost), valuation));
      distinct.add(stock.getProduct().getId());
      if (qty.compareTo(BigDecimal.ZERO) > 0) {
        active.add(stock.getProduct().getId());
      }
      totalQty = totalQty.add(qty);
      stockValue = stockValue.add(valuation);
      if (stock.low()) {
        low.add(new ReportDtos.LowStockLine(stock.getProduct().getSku(),
            stock.getProduct().getProductName(), stock.getWarehouse().getWarehouseName(),
            Money.amount(qty), Money.amount(stock.getReorderLevel())));
      }
    }
    ReportDtos.InventorySummary summary = new ReportDtos.InventorySummary(distinct.size(), active.size(),
        Money.amount(totalQty), Money.amount(stockValue));
    return new ReportDtos.InventoryReport(summary, items, low);
  }

  @Transactional(readOnly = true)
  public ReportDtos.FinancialReport financialReport(LocalDate from, LocalDate to) {
    Long ventureId = VentureContextHolder.get();
    LocalDate end = to == null ? LocalDate.now() : to;
    LocalDate start = from == null ? end.minusDays(29) : from;

    BigDecimal revenue = Money.amount(
        sales.totalSales(ventureId, start, end).subtract(sales.returnedSales(ventureId, start, end)));
    BigDecimal purchaseNet = Money.amount(purchases.totalPurchases(ventureId, start, end)
        .subtract(purchases.returnedPurchases(ventureId, start, end)));
    BigDecimal cogs = Money.amount(saleItems.cogs(ventureId, start, end));
    BigDecimal expenseTotal = Money.amount(expenses.totalInRange(ventureId, start, end));
    BigDecimal grossMargin = Money.amount(revenue.subtract(cogs));
    BigDecimal netProfit = Money.amount(grossMargin.subtract(expenseTotal));

    BigDecimal receivables = Money.amount(sales.receivables(ventureId));
    BigDecimal payables = Money.amount(purchases.payables(ventureId));
    BigDecimal received = Money.amount(payments.totalByTypeInRange(ventureId, "RECEIVED", start, end));
    BigDecimal paid = Money.amount(payments.totalByTypeInRange(ventureId, "PAID", start, end));

    return new ReportDtos.FinancialReport(
        new ReportDtos.FinancialSummary(revenue, purchaseNet, cogs, expenseTotal, grossMargin, netProfit),
        new ReportDtos.Outstanding(receivables, payables),
        new ReportDtos.CashFlow(received, paid, Money.amount(received.subtract(paid))));
  }

  private List<ReportDtos.DayPoint> series(List<Object[]> rows, LocalDate start, LocalDate end) {
    Map<LocalDate, Object[]> byDay = new LinkedHashMap<>();
    for (Object[] row : rows) {
      Object day = row[0];
      LocalDate date = day instanceof LocalDate local ? local
          : LocalDate.parse(String.valueOf(day).substring(0, 10));
      byDay.put(date, row);
    }
    List<ReportDtos.DayPoint> points = new ArrayList<>();
    for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
      Object[] row = byDay.get(day);
      BigDecimal amount = row == null ? Money.amount(BigDecimal.ZERO) : Money.amount((BigDecimal) row[1]);
      long count = row == null ? 0L : ((Number) row[2]).longValue();
      points.add(new ReportDtos.DayPoint(day.toString(), amount, count));
    }
    return points;
  }
}
