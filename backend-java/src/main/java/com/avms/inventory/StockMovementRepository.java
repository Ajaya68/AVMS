package com.avms.inventory;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

  @Query("select m from StockMovement m where (:ventureId is null or m.venture.id = :ventureId)"
      + " and (:warehouseId is null or m.warehouse.id = :warehouseId)"
      + " and (:productId is null or m.product.id = :productId)"
      + " and (:movementType is null or m.movementType = :movementType)"
      + " order by m.movementDate desc, m.id desc")
  Page<StockMovement> search(Long ventureId, Long warehouseId, Long productId, String movementType,
      Pageable pageable);

  Optional<StockMovement> findByIdAndVentureId(Long id, Long ventureId);
}
