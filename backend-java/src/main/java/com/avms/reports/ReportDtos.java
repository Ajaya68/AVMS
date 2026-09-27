package com.avms.reports;

import java.math.BigDecimal;
import java.util.List;

public class ReportDtos {

  public record SalesSummary(long count, BigDecimal itemsSold, BigDecimal totalAmount,
      BigDecimal returnedAmount, BigDecimal netAmount) {}

  public record TopProduct(Long product, String productCode, String productName, BigDecimal quantity,
      BigDecimal revenue) {}

  public record DayPoint(String label, BigDecimal amount, long count) {}

  public record SalesReport(SalesSummary summary, List<TopProduct> topProducts, List<DayPoint> series) {}

  public record PurchasesSummary(long count, BigDecimal itemsPurchased, BigDecimal totalAmount,
      BigDecimal returnedAmount, BigDecimal netAmount) {}

  public record TopSupplier(Long supplier, String supplierName, BigDecimal amount, long count) {}

  public record PurchasesReport(PurchasesSummary summary, List<TopSupplier> topSuppliers,
      List<DayPoint> series) {}

  public record StockLine(Long product, String productCode, String productName, String warehouse,
      String warehouseCode, BigDecimal quantity, BigDecimal unitCost, BigDecimal valuation) {}

  public record LowStockLine(String productCode, String productName, String warehouse, BigDecimal quantity,
      BigDecimal reorderLevel) {}

  public record InventorySummary(long stockProducts, long activeProducts, BigDecimal totalQuantity,
      BigDecimal stockValue) {}

  public record InventoryReport(InventorySummary summary, List<StockLine> items,
      List<LowStockLine> lowStock) {}

  public record FinancialSummary(BigDecimal revenue, BigDecimal purchases, BigDecimal cogs,
      BigDecimal expenses, BigDecimal grossMargin, BigDecimal netProfit) {}

  public record Outstanding(BigDecimal receivables, BigDecimal payables) {}

  public record CashFlow(BigDecimal received, BigDecimal paid, BigDecimal net) {}

  public record FinancialReport(FinancialSummary summary, Outstanding outstanding, CashFlow cashFlow) {}
}
