package com.avms.inventory;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface StockRepository extends JpaRepository<Stock, Long> {

  /** Locked (SELECT ... FOR UPDATE) read-modify-write lookup: serializes
   *  concurrent stock deltas on one row so overselling cannot race. The only
   *  caller is InventoryService.applyDelta. H2 supports FOR UPDATE; locks are
   *  released at transaction end (short-lived). */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<Stock> findByWarehouseIdAndProductId(Long warehouseId, Long productId);

  @Query("select s from Stock s where (:ventureId is null or s.venture.id = :ventureId)"
      + " and (:warehouseId is null or s.warehouse.id = :warehouseId)"
      + " and (:productId is null or s.product.id = :productId)"
      + " and (:lowOnly = false or s.quantity <= s.reservedQuantity + s.reorderLevel)")
  Page<Stock> search(Long ventureId, Long warehouseId, Long productId, boolean lowOnly, Pageable pageable);

  List<Stock> findByVentureId(Long ventureId);
}
