package com.avms.inventory;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {

  @Query("select w from Warehouse w where (:ventureId is null or w.venture.id = :ventureId)"
      + " and (:status is null or w.status = :status)"
      + " and (:pattern is null or lower(w.warehouseName) like :pattern"
      + " or lower(w.warehouseCode) like :pattern or lower(w.city) like :pattern"
      + " or lower(w.manager) like :pattern)")
  Page<Warehouse> search(Long ventureId, String pattern, String status, Pageable pageable);

  Optional<Warehouse> findByIdAndVentureId(Long id, Long ventureId);
}
