package com.avms.sales;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SaleItemRepository extends JpaRepository<SaleItem, Long> {

  List<SaleItem> findBySaleId(Long saleId);

  @Query("select coalesce(sum(i.quantity), 0) from SaleItem i where i.sale.id = :saleId"
      + " and i.product.id = :productId")
  BigDecimal soldQuantity(Long saleId, Long productId);

  @Query("select coalesce(sum(i.quantity), 0) from SaleItem i"
      + " where (:ventureId is null or i.sale.venture.id = :ventureId)"
      + " and i.sale.saleDate between :from and :to and i.sale.status <> 'CANCELLED'")
  BigDecimal itemsSold(Long ventureId, java.time.LocalDate from, java.time.LocalDate to);

  @Query("select i.product.id, i.product.sku, i.product.productName,"
      + " coalesce(sum(i.quantity), 0), coalesce(sum(i.total), 0) from SaleItem i"
      + " where (:ventureId is null or i.sale.venture.id = :ventureId)"
      + " and i.sale.saleDate between :from and :to and i.sale.status <> 'CANCELLED'"
      + " group by i.product.id, i.product.sku, i.product.productName"
      + " order by sum(i.total) desc")
  List<Object[]> topProducts(Long ventureId, java.time.LocalDate from, java.time.LocalDate to,
      org.springframework.data.domain.Pageable pageable);

  @Query("select coalesce(sum(i.quantity * i.product.purchasePrice), 0) from SaleItem i"
      + " where (:ventureId is null or i.sale.venture.id = :ventureId)"
      + " and i.sale.saleDate between :from and :to and i.sale.status <> 'CANCELLED'")
  BigDecimal cogs(Long ventureId, java.time.LocalDate from, java.time.LocalDate to);
}
