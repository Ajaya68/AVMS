package com.avms.suppliers;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {

  @Query("select s from Supplier s where (:ventureId is null or s.venture.id = :ventureId)"
      + " and (:status is null or s.status = :status)"
      + " and (:pattern is null or lower(s.name) like :pattern"
      + " or lower(s.supplierCode) like :pattern or lower(s.contactPerson) like :pattern"
      + " or lower(s.city) like :pattern or lower(s.phone) like :pattern"
      + " or lower(s.email) like :pattern)")
  Page<Supplier> search(Long ventureId, String pattern, String status, Pageable pageable);

  Optional<Supplier> findByIdAndVentureId(Long id, Long ventureId);
}
