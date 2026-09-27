package com.avms.purchases;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PurchaseItemRepository extends JpaRepository<PurchaseItem, Long> {

  List<PurchaseItem> findByPurchaseId(Long purchaseId);

  @Query("select coalesce(sum(i.quantity), 0) from PurchaseItem i where i.purchase.id = :purchaseId"
      + " and i.product.id = :productId")
  BigDecimal purchasedQuantity(Long purchaseId, Long productId);
}
