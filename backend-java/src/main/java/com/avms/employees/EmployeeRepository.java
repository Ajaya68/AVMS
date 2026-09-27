package com.avms.employees;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

  @Query("select e from Employee e where (:ventureId is null or e.venture.id = :ventureId)"
      + " and (:status is null or e.status = :status)"
      + " and (:department is null or e.department = :department)"
      + " and (:designation is null or e.designation = :designation)"
      + " and (:pattern is null or lower(e.employeeCode) like :pattern"
      + " or lower(e.firstName) like :pattern or lower(e.lastName) like :pattern"
      + " or lower(e.email) like :pattern or lower(e.department) like :pattern"
      + " or lower(e.designation) like :pattern)")
  Page<Employee> search(Long ventureId, String pattern, String status, String department,
      String designation, Pageable pageable);

  Optional<Employee> findByIdAndVentureId(Long id, Long ventureId);

  Optional<Employee> findByUserId(Long userId);

  List<Employee> findByEmailIgnoreCase(String email);
}
