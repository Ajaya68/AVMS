package com.avms.employees;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

  Optional<Attendance> findByEmployeeIdAndAttendanceDate(Long employeeId, LocalDate date);

  @Query("select a from Attendance a where (:employeeId is null or a.employee.id = :employeeId)"
      + " and (:ventureId is null or a.employee.venture.id = :ventureId)"
      + " and (:department is null or a.employee.department = :department)"
      + " and (:status is null or a.status = :status)"
      + " and (cast(:from as date) is null or a.attendanceDate >= :from)"
      + " and (cast(:to as date) is null or a.attendanceDate <= :to)"
      + " order by a.attendanceDate desc, a.employee.firstName asc")
  Page<Attendance> search(Long employeeId, Long ventureId, String department, String status,
      LocalDate from, LocalDate to, Pageable pageable);

  @Query("select a.status, count(a) from Attendance a where a.employee.id = :employeeId"
      + " and a.attendanceDate >= :from and a.attendanceDate <= :to group by a.status")
  List<Object[]> countByStatus(Long employeeId, LocalDate from, LocalDate to);
}
