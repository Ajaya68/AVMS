package com.avms.sales;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SaleRepository extends JpaRepository<Sale, Long> {

  @Query("select s from Sale s where (:ventureId is null or s.venture.id = :ventureId)"
      + " and (:status is null or s.status = :status)"
      + " and (:pattern is null or lower(s.invoiceNumber) like :pattern"
      + " or lower(s.customer.name) like :pattern)"
      + " order by s.saleDate desc, s.id desc")
  Page<Sale> search(Long ventureId, String pattern, String status, Pageable pageable);

  Optional<Sale> findByIdAndVentureId(Long id, Long ventureId);

  List<Sale> findByVentureId(Long ventureId);

  @Query("select count(s) from Sale s where (:ventureId is null or s.venture.id = :ventureId)"
      + " and s.saleDate between :from and :to and s.status <> 'CANCELLED'")
  long countSales(Long ventureId, java.time.LocalDate from, java.time.LocalDate to);

  @Query("select coalesce(sum(s.totalAmount), 0) from Sale s"
      + " where (:ventureId is null or s.venture.id = :ventureId)"
      + " and s.saleDate between :from and :to and s.status <> 'CANCELLED'")
  java.math.BigDecimal totalSales(Long ventureId, java.time.LocalDate from, java.time.LocalDate to);

  @Query("select coalesce(sum(s.returnedAmount), 0) from Sale s"
      + " where (:ventureId is null or s.venture.id = :ventureId)"
      + " and s.saleDate between :from and :to and s.status <> 'CANCELLED'")
  java.math.BigDecimal returnedSales(Long ventureId, java.time.LocalDate from, java.time.LocalDate to);

  @Query("select s.saleDate, coalesce(sum(s.totalAmount), 0), count(s) from Sale s"
      + " where (:ventureId is null or s.venture.id = :ventureId)"
      + " and s.saleDate between :from and :to and s.status <> 'CANCELLED'"
      + " group by s.saleDate order by s.saleDate")
  List<Object[]> salesSeries(Long ventureId, java.time.LocalDate from, java.time.LocalDate to);

  @Query("select coalesce(sum(s.dueAmount), 0) from Sale s"
      + " where (:ventureId is null or s.venture.id = :ventureId) and s.status <> 'CANCELLED'")
  java.math.BigDecimal receivables(Long ventureId);
}
