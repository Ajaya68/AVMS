package com.avms.sales;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SaleReturnRepository extends JpaRepository<SaleReturn, Long> {

  @Query("select r from SaleReturn r where (:ventureId is null or r.venture.id = :ventureId)"
      + " and (:pattern is null or lower(r.returnNumber) like :pattern"
      + " or lower(r.sale.invoiceNumber) like :pattern)"
      + " order by r.returnDate desc, r.id desc")
  Page<SaleReturn> search(Long ventureId, String pattern, Pageable pageable);

  Optional<SaleReturn> findByIdAndVentureId(Long id, Long ventureId);
}
