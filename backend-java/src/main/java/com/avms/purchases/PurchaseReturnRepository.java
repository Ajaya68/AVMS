package com.avms.purchases;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PurchaseReturnRepository extends JpaRepository<PurchaseReturn, Long> {

  @Query("select r from PurchaseReturn r where (:ventureId is null or r.venture.id = :ventureId)"
      + " and (:pattern is null or lower(r.returnNumber) like :pattern"
      + " or lower(r.purchase.invoiceNumber) like :pattern)"
      + " order by r.returnDate desc, r.id desc")
  Page<PurchaseReturn> search(Long ventureId, String pattern, Pageable pageable);

  Optional<PurchaseReturn> findByIdAndVentureId(Long id, Long ventureId);
}
