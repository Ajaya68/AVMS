package com.avms.purchases;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PurchaseReturnItemRepository extends JpaRepository<PurchaseReturnItem, Long> {

  List<PurchaseReturnItem> findByPurchaseReturnId(Long purchaseReturnId);

  @Query("select coalesce(sum(i.quantity), 0) from PurchaseReturnItem i"
      + " where i.purchaseReturn.purchase.id = :purchaseId and i.product.id = :productId")
  BigDecimal returnedQuantity(Long purchaseId, Long productId);
}
