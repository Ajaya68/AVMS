package com.avms.ventures;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface VentureRepository extends JpaRepository<Venture, Long> {
  @Query("select v from Venture v where (:status is null or v.status = :status) and (:pattern is null or lower(v.ventureCode) like :pattern or lower(v.ventureName) like :pattern or lower(v.city) like :pattern)")
  Page<Venture> search(String pattern, String status, Pageable pageable);

  List<Venture> findByStatus(String status);
}
