package com.avms.payments;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

  @Query("select p from Payment p where (:ventureId is null or p.venture.id = :ventureId)"
      + " and (:paymentType is null or p.paymentType = :paymentType)"
      + " and (:referenceType is null or p.referenceType = :referenceType)"
      + " and (:pattern is null or lower(p.notes) like :pattern"
      + " or lower(p.transactionReference) like :pattern)"
      + " order by p.paymentDate desc, p.id desc")
  Page<Payment> search(Long ventureId, String paymentType, String referenceType, String pattern,
      Pageable pageable);

  Optional<Payment> findByIdAndVentureId(Long id, Long ventureId);

  @Query("select coalesce(sum(p.amount), 0) from Payment p"
      + " where (:ventureId is null or p.venture.id = :ventureId)"
      + " and p.paymentType = :paymentType and p.paymentDate between :from and :to")
  java.math.BigDecimal totalByTypeInRange(Long ventureId, String paymentType,
      java.time.LocalDate from, java.time.LocalDate to);
}
