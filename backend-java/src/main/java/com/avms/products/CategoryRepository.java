package com.avms.products;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CategoryRepository extends JpaRepository<Category, Long> {

  @Query("select c from Category c where (:ventureId is null or c.venture.id = :ventureId)"
      + " and (:status is null or c.status = :status)"
      + " and (:pattern is null or lower(c.categoryName) like :pattern)")
  Page<Category> search(Long ventureId, String pattern, String status, Pageable pageable);

  Optional<Category> findByIdAndVentureId(Long id, Long ventureId);

  boolean existsByVentureIdAndCategoryName(Long ventureId, String categoryName);

  List<Category> findByVentureId(Long ventureId);
}
