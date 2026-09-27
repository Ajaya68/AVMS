package com.avms.employees;

import com.avms.accounts.AccountsUser;
import com.avms.common.BaseEntity;
import com.avms.ventures.Venture;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Port of Django employees.Employee: venture-scoped HR record, auto EMP-#### codes. */
@Entity
@Table(name = "EMPLOYEES_EMPLOYEE",
    uniqueConstraints = @UniqueConstraint(name = "uniq_employee_venture_code",
        columnNames = {"venture_id", "employee_code"}))
public class Employee extends BaseEntity {

  @jakarta.persistence.Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "venture_id", nullable = false)
  private Venture venture;

  @Column(name = "employee_code", nullable = false, length = 20)
  private String employeeCode;

  @Column(name = "first_name", nullable = false, length = 100)
  private String firstName;

  @Column(name = "last_name", length = 100)
  private String lastName;

  @Column(name = "phone", length = 20)
  private String phone;

  @Column(name = "email", length = 254)
  private String email;

  @Column(name = "department", length = 100)
  private String department;

  @Column(name = "designation", length = 100)
  private String designation;

  @Column(name = "joining_date")
  private LocalDate joiningDate;

  @Column(name = "salary", nullable = false, precision = 12, scale = 2)
  private BigDecimal salary = BigDecimal.ZERO;

  @Column(name = "status", nullable = false, length = 20)
  private String status = "ACTIVE";

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", unique = true)
  private AccountsUser user;

  public Employee() {}

  public Long getId() { return id; }
  public Venture getVenture() { return venture; }
  public void setVenture(Venture venture) { this.venture = venture; }
  public String getEmployeeCode() { return employeeCode; }
  public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }
  public String getFirstName() { return firstName; }
  public void setFirstName(String firstName) { this.firstName = firstName; }
  public String getLastName() { return lastName; }
  public void setLastName(String lastName) { this.lastName = lastName; }
  public String getPhone() { return phone; }
  public void setPhone(String phone) { this.phone = phone; }
  public String getEmail() { return email; }
  public void setEmail(String email) { this.email = email; }
  public String getDepartment() { return department; }
  public void setDepartment(String department) { this.department = department; }
  public String getDesignation() { return designation; }
  public void setDesignation(String designation) { this.designation = designation; }
  public LocalDate getJoiningDate() { return joiningDate; }
  public void setJoiningDate(LocalDate joiningDate) { this.joiningDate = joiningDate; }
  public BigDecimal getSalary() { return salary; }
  public void setSalary(BigDecimal salary) { this.salary = salary; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public AccountsUser getUser() { return user; }
  public void setUser(AccountsUser user) { this.user = user; }

  public String fullName() {
    String last = lastName == null ? "" : " " + lastName;
    return (firstName == null ? "" : firstName) + last;
  }
}
