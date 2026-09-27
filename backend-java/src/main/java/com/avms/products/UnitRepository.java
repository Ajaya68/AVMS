package com.avms.products;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UnitRepository extends JpaRepository<Unit, Long> {

  @Query("select u from Unit u where (:pattern is null"
      + " or lower(u.unitCode) like :pattern or lower(u.unitName) like :pattern)")
  Page<Unit> search(String pattern, Pageable pageable);

  Optional<Unit> findByUnitCode(String unitCode);
}
