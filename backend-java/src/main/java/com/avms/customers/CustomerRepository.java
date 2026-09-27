package com.avms.customers;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

  @Query("select c from Customer c where (:ventureId is null or c.venture.id = :ventureId)"
      + " and (:status is null or c.status = :status)"
      + " and (:pattern is null or lower(c.name) like :pattern"
      + " or lower(c.customerCode) like :pattern or lower(c.city) like :pattern"
      + " or lower(c.phone) like :pattern or lower(c.email) like :pattern)")
  Page<Customer> search(Long ventureId, String pattern, String status, Pageable pageable);

  Optional<Customer> findByIdAndVentureId(Long id, Long ventureId);
}
