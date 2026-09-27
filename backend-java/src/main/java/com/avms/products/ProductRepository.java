package com.avms.products;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProductRepository extends JpaRepository<Product, Long> {

  @Query("select p from Product p left join p.category c"
      + " where (:ventureId is null or p.venture.id = :ventureId)"
      + " and (:status is null or p.status = :status)"
      + " and (:pattern is null or lower(p.productName) like :pattern"
      + " or lower(p.sku) like :pattern or lower(c.categoryName) like :pattern)")
  Page<Product> search(Long ventureId, String pattern, String status, Pageable pageable);

  Optional<Product> findByIdAndVentureId(Long id, Long ventureId);

  boolean existsByUnitId(Long unitId);

  List<Product> findByCategoryId(Long categoryId);

  List<Product> findByVentureId(Long ventureId);
}
