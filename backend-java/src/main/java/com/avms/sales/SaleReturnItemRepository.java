package com.avms.sales;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SaleReturnItemRepository extends JpaRepository<SaleReturnItem, Long> {

  List<SaleReturnItem> findBySaleReturnId(Long saleReturnId);

  @Query("select coalesce(sum(i.quantity), 0) from SaleReturnItem i"
      + " where i.saleReturn.sale.id = :saleId and i.product.id = :productId")
  BigDecimal returnedQuantity(Long saleId, Long productId);
}
