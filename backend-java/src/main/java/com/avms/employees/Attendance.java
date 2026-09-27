package com.avms.employees;

import com.avms.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.LocalTime;

/** Port of Django employees.Attendance: one row per employee per day. */
@Entity
@Table(name = "EMPLOYEES_ATTENDANCE",
    uniqueConstraints = @UniqueConstraint(name = "uniq_attendance_employee_date",
        columnNames = {"employee_id", "attendance_date"}))
public class Attendance extends BaseEntity {

  @jakarta.persistence.Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "employee_id", nullable = false)
  private Employee employee;

  @Column(name = "attendance_date", nullable = false)
  private LocalDate attendanceDate;

  @Column(name = "status", nullable = false, length = 20)
  private String status;

  @Column(name = "check_in")
  private LocalTime checkIn;

  @Column(name = "check_out")
  private LocalTime checkOut;

  @Column(name = "notes", length = 255)
  private String notes;

  public Attendance() {}

  public Long getId() { return id; }
  public Employee getEmployee() { return employee; }
  public void setEmployee(Employee employee) { this.employee = employee; }
  public LocalDate getAttendanceDate() { return attendanceDate; }
  public void setAttendanceDate(LocalDate attendanceDate) { this.attendanceDate = attendanceDate; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public LocalTime getCheckIn() { return checkIn; }
  public void setCheckIn(LocalTime checkIn) { this.checkIn = checkIn; }
  public LocalTime getCheckOut() { return checkOut; }
  public void setCheckOut(LocalTime checkOut) { this.checkOut = checkOut; }
  public String getNotes() { return notes; }
  public void setNotes(String notes) { this.notes = notes; }
}
