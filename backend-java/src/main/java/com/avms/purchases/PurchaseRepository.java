package com.avms.purchases;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

  @Query("select p from Purchase p where (:ventureId is null or p.venture.id = :ventureId)"
      + " and (:status is null or p.status = :status)"
      + " and (:pattern is null or lower(p.invoiceNumber) like :pattern"
      + " or lower(p.supplier.name) like :pattern)"
      + " order by p.purchaseDate desc, p.id desc")
  Page<Purchase> search(Long ventureId, String pattern, String status, Pageable pageable);

  Optional<Purchase> findByIdAndVentureId(Long id, Long ventureId);

  List<Purchase> findByVentureId(Long ventureId);

  @Query("select count(p) from Purchase p"
      + " where (:ventureId is null or p.venture.id = :ventureId)"
      + " and p.purchaseDate between :from and :to and p.status <> 'CANCELLED'")
  long countPurchases(Long ventureId, java.time.LocalDate from, java.time.LocalDate to);

  @Query("select coalesce(sum(p.totalAmount), 0) from Purchase p"
      + " where (:ventureId is null or p.venture.id = :ventureId)"
      + " and p.purchaseDate between :from and :to and p.status <> 'CANCELLED'")
  java.math.BigDecimal totalPurchases(Long ventureId, java.time.LocalDate from, java.time.LocalDate to);

  @Query("select coalesce(sum(p.returnedAmount), 0) from Purchase p"
      + " where (:ventureId is null or p.venture.id = :ventureId)"
      + " and p.purchaseDate between :from and :to and p.status <> 'CANCELLED'")
  java.math.BigDecimal returnedPurchases(Long ventureId, java.time.LocalDate from, java.time.LocalDate to);

  @Query("select p.purchaseDate, coalesce(sum(p.totalAmount), 0), count(p) from Purchase p"
      + " where (:ventureId is null or p.venture.id = :ventureId)"
      + " and p.purchaseDate between :from and :to and p.status <> 'CANCELLED'"
      + " group by p.purchaseDate order by p.purchaseDate")
  List<Object[]> purchasesSeries(Long ventureId, java.time.LocalDate from, java.time.LocalDate to);

  @Query("select coalesce(sum(p.dueAmount), 0) from Purchase p"
      + " where (:ventureId is null or p.venture.id = :ventureId) and p.status <> 'CANCELLED'")
  java.math.BigDecimal payables(Long ventureId);

  @Query("select p.supplier.id, p.supplier.name,"
      + " coalesce(sum(p.totalAmount), 0), count(p) from Purchase p"
      + " where (:ventureId is null or p.venture.id = :ventureId)"
      + " and p.purchaseDate between :from and :to and p.status <> 'CANCELLED'"
      + " group by p.supplier.id, p.supplier.name order by sum(p.totalAmount) desc")
  List<Object[]> topSuppliers(Long ventureId, java.time.LocalDate from, java.time.LocalDate to,
      org.springframework.data.domain.Pageable pageable);
}
