package com.avms.employees;

import com.avms.accounts.AccountsUser;
import com.avms.accounts.AccountsUserRepository;
import com.avms.audit.AuditService;
import com.avms.common.BusinessRuleException;
import com.avms.common.Money;
import com.avms.common.ResourceNotFoundException;
import com.avms.common.SequenceService;
import com.avms.common.VentureContextHolder;
import com.avms.security.CustomUserDetails;
import com.avms.ventures.Venture;
import com.avms.ventures.VentureRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Port of Django employees views: EMP-#### records, self-service attendance, bulk roster. */
@Service
public class EmployeeService {

  private static final Set<String> EMP_STATUSES = Set.of("ACTIVE", "INACTIVE", "TERMINATED");
  private static final Set<String> ATT_STATUSES = Set.of("PRESENT", "ABSENT", "HALF_DAY", "LEAVE");

  private final EmployeeRepository employees;
  private final AttendanceRepository attendance;
  private final VentureRepository ventures;
  private final AccountsUserRepository users;
  private final SequenceService sequences;
  private final AuditService audit;

  public EmployeeService(EmployeeRepository employees, AttendanceRepository attendance,
      VentureRepository ventures, AccountsUserRepository users, SequenceService sequences,
      AuditService audit) {
    this.employees = employees;
    this.attendance = attendance;
    this.ventures = ventures;
    this.users = users;
    this.sequences = sequences;
    this.audit = audit;
  }

  @Transactional(readOnly = true)
  public Page<EmployeeDtos.EmployeeResponse> list(String search, String status, Long ventureParam,
      String department, String designation, Pageable pageable) {
    Long ventureId = ventureParam != null ? ventureParam : VentureContextHolder.get();
    String pattern = search != null && !search.isBlank() ? "%" + search.trim().toLowerCase() + "%" : null;
    String st = status != null && !status.isBlank() ? status.trim().toUpperCase() : null;
    return employees.search(ventureId, pattern, st, department, designation, pageable).map(this::toResponse);
  }

  @Transactional
  public EmployeeDtos.EmployeeResponse create(EmployeeDtos.EmployeeRequest req) {
    req.validate(true);
    Venture venture = resolveVenture(req.venture());
    Employee employee = new Employee();
    employee.setVenture(venture);
    apply(req, employee);
    employee.setEmployeeCode(sequences.generateCode("employees", venture.getId(), "EMP"));
    if (employee.getStatus() == null) {
      employee.setStatus("ACTIVE");
    }
    if (employee.getSalary() == null) {
      employee.setSalary(BigDecimal.ZERO);
    }
    if (req.user() != null) {
      employee.setUser(users.findById(req.user())
          .orElseThrow(() -> new ResourceNotFoundException("User not found.")));
    }
    employees.save(employee);
    audit.log("CREATE", "employees", "Employee", String.valueOf(employee.getId()),
        "Employee " + employee.getEmployeeCode() + " created.");
    return toResponse(employee);
  }

  @Transactional(readOnly = true)
  public EmployeeDtos.EmployeeResponse get(Long id) {
    return toResponse(findScoped(id));
  }

  @Transactional
  public EmployeeDtos.EmployeeResponse update(Long id, EmployeeDtos.EmployeeRequest req) {
    Employee employee = findScoped(id);
    apply(req, employee);
    if (req.user() != null) {
      employee.setUser(users.findById(req.user())
          .orElseThrow(() -> new ResourceNotFoundException("User not found.")));
    }
    employees.save(employee);
    audit.log("UPDATE", "employees", "Employee", String.valueOf(id),
        "Employee " + employee.getEmployeeCode() + " updated.");
    return toResponse(employee);
  }

  @Transactional
  public void delete(Long id) {
    Employee employee = findScoped(id);
    employees.delete(employee);
    audit.log("DELETE", "employees", "Employee", String.valueOf(id),
        "Employee " + employee.getEmployeeCode() + " deleted.");
  }

  @Transactional(readOnly = true)
  public EmployeeDtos.MyProfileResponse myProfile() {
    Employee employee = ownEmployee();
    if (employee == null) {
      return new EmployeeDtos.MyProfileResponse(false, null, null, monthSummary(null));
    }
    LocalDate today = LocalDate.now();
    EmployeeDtos.AttendanceResponse todayRow = attendance
        .findByEmployeeIdAndAttendanceDate(employee.getId(), today)
        .map(this::toAttendanceResponse).orElse(null);
    return new EmployeeDtos.MyProfileResponse(true, toResponse(employee), todayRow,
        monthSummary(employee.getId()));
  }

  @Transactional(readOnly = true)
  public Page<EmployeeDtos.AttendanceResponse> listAttendance(Long ventureParam, Long employeeParam,
      String department, String status, LocalDate from, LocalDate to, Pageable pageable) {
    if (!canManageAttendance() && !hasPermission("attendance.view")) {
      Employee own = ownEmployee();
      Long ownId = own == null ? -1L : own.getId();
      return attendance.search(ownId, null, null, null, null, null, pageable).map(this::toAttendanceResponse);
    }
    Long ventureId = ventureParam != null ? ventureParam : VentureContextHolder.get();
    String st = status != null && !status.isBlank() ? status.trim().toUpperCase() : null;
    return attendance.search(employeeParam, ventureId, department, st, from, to, pageable)
        .map(this::toAttendanceResponse);
  }

  @Transactional
  public EmployeeDtos.AttendanceResponse mark(EmployeeDtos.AttendanceRequest req) {
    if (req.employee() == null) {
      throw new IllegalArgumentException("employee is required.");
    }
    if (req.date() == null) {
      throw new IllegalArgumentException("date is required.");
    }
    Employee employee = employees.findById(req.employee())
        .orElseThrow(() -> new ResourceNotFoundException("Employee not found."));
    boolean manager = canManageAttendance();
    if (!manager) {
      Employee own = ownEmployee();
      if (own == null || !own.getId().equals(employee.getId())) {
        throw new BusinessRuleException("You can only mark your own attendance.");
      }
    }
    String status = req.status() == null ? "PRESENT" : req.status().trim().toUpperCase();
    if (!ATT_STATUSES.contains(status)) {
      throw new IllegalArgumentException("Unknown status: " + req.status());
    }
    Attendance row = attendance
        .findByEmployeeIdAndAttendanceDate(employee.getId(), req.date()).orElseGet(() -> {
          Attendance created = new Attendance();
          created.setEmployee(employee);
          created.setAttendanceDate(req.date());
          return created;
        });
    row.setStatus(status);
    if (req.checkIn() != null) {
      row.setCheckIn(parseTime(req.checkIn()));
    } else if (row.getId() == null && "PRESENT".equals(status)) {
      row.setCheckIn(LocalTime.now());
    }
    if (req.checkOut() != null) {
      row.setCheckOut(parseTime(req.checkOut()));
    }
    if (req.notes() != null) {
      row.setNotes(req.notes());
    }
    attendance.save(row);
    audit.log(row.getId() == null ? "CREATE" : "UPDATE", "attendance", "Attendance",
        String.valueOf(row.getId()), "Attendance marked for " + employee.fullName() + ".");
    return toAttendanceResponse(row);
  }

  @Transactional
  public EmployeeDtos.AttendanceResponse updateAttendance(Long id, EmployeeDtos.AttendanceRequest req) {
    Attendance row = attendance.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Attendance not found."));
    boolean manager = canManageAttendance();
    if (!manager) {
      Employee own = ownEmployee();
      if (own == null || !own.getId().equals(row.getEmployee().getId())) {
        throw new BusinessRuleException("You can only update your own attendance.");
      }
    }
    if (req.status() != null) {
      String status = req.status().trim().toUpperCase();
      if (!ATT_STATUSES.contains(status)) {
        throw new IllegalArgumentException("Unknown status: " + req.status());
      }
      row.setStatus(status);
    }
    if (req.checkIn() != null) {
      row.setCheckIn(parseTime(req.checkIn()));
    }
    if (req.checkOut() != null) {
      row.setCheckOut(parseTime(req.checkOut()));
    }
    if (req.notes() != null) {
      row.setNotes(req.notes());
    }
    attendance.save(row);
    audit.log("UPDATE", "attendance", "Attendance", String.valueOf(id), "Attendance updated.");
    return toAttendanceResponse(row);
  }

  @Transactional
  public void deleteAttendance(Long id) {
    if (!canManageAttendance()) {
      throw new BusinessRuleException("You do not have permission to perform this action.");
    }
    Attendance row = attendance.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Attendance not found."));
    attendance.delete(row);
    audit.log("DELETE", "attendance", "Attendance", String.valueOf(id), "Attendance deleted.");
  }

  @Transactional
  public EmployeeDtos.BulkResult bulk(EmployeeDtos.BulkRequest req) {
    req.validate();
    if (!canManageAttendance()) {
      throw new BusinessRuleException("You do not have permission to perform this action.");
    }
    int created = 0;
    int updated = 0;
    List<EmployeeDtos.AttendanceResponse> results = new ArrayList<>();
    for (EmployeeDtos.BulkItem item : req.items()) {
      Employee employee = employees.findById(item.employee())
          .orElseThrow(() -> new ResourceNotFoundException("Employee not found."));
      String status = item.status() == null ? "PRESENT" : item.status().trim().toUpperCase();
      if (!ATT_STATUSES.contains(status)) {
        throw new IllegalArgumentException("Unknown status: " + item.status());
      }
      Attendance row = attendance
          .findByEmployeeIdAndAttendanceDate(employee.getId(), req.date()).orElse(null);
      boolean isNew = row == null;
      if (isNew) {
        row = new Attendance();
        row.setEmployee(employee);
        row.setAttendanceDate(req.date());
        created++;
      } else {
        updated++;
      }
      row.setStatus(status);
      if (item.remarks() != null) {
        row.setNotes(item.remarks());
      }
      attendance.save(row);
      results.add(toAttendanceResponse(row));
    }
    audit.log("CREATE", "attendance", "Attendance", null,
        "Bulk attendance marked for " + req.date() + ": " + created + " created, " + updated + " updated.");
    return new EmployeeDtos.BulkResult(created, updated, results);
  }

  private Employee findScoped(Long id) {
    Long ventureId = VentureContextHolder.get();
    Employee employee = ventureId == null
        ? employees.findById(id).orElse(null)
        : employees.findByIdAndVentureId(id, ventureId).orElse(null);
    if (employee == null) {
      throw new ResourceNotFoundException("Employee not found.");
    }
    return employee;
  }

  private Venture resolveVenture(Long requested) {
    Long ventureId = requested != null ? requested : VentureContextHolder.get();
    if (ventureId == null) {
      throw new IllegalArgumentException("venture is required.");
    }
    return ventures.findById(ventureId)
        .orElseThrow(() -> new ResourceNotFoundException("Venture not found."));
  }

  private void apply(EmployeeDtos.EmployeeRequest req, Employee employee) {
    if (req.firstName() != null) {
      employee.setFirstName(req.firstName());
    }
    if (req.lastName() != null) {
      employee.setLastName(req.lastName());
    }
    if (req.phone() != null) {
      employee.setPhone(req.phone());
    }
    if (req.email() != null) {
      employee.setEmail(req.email());
    }
    if (req.department() != null) {
      employee.setDepartment(req.department());
    }
    if (req.designation() != null) {
      employee.setDesignation(req.designation());
    }
    if (req.joiningDate() != null) {
      employee.setJoiningDate(req.joiningDate());
    }
    if (req.salary() != null) {
      employee.setSalary(Money.amount(req.salary()));
    }
    if (req.status() != null) {
      String st = req.status().trim().toUpperCase();
      if (!EMP_STATUSES.contains(st)) {
        throw new IllegalArgumentException("Unknown status: " + req.status());
      }
      employee.setStatus(st);
    }
  }

  private Employee ownEmployee() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null) {
      return null;
    }
    Object principal = auth.getPrincipal();
    String email = com.avms.security.SecurityEmails.currentUserEmail();
    if (email != null) {
      AccountsUser user = users.findByEmailIgnoreCase(email).orElse(null);
      if (user != null) {
        Employee linked = employees.findByUserId(user.getId()).orElse(null);
        if (linked != null) {
          return linked;
        }
      }
      List<Employee> matches = employees.findByEmailIgnoreCase(email);
      return matches.isEmpty() ? null : matches.get(0);
    }
    return null;
  }

  private boolean canManageAttendance() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null) {
      return false;
    }
    Object principal = auth.getPrincipal();
    if (principal instanceof CustomUserDetails cud) {
      return cud.hasPermission("attendance.manage");
    }
    return false;
  }

  private boolean hasPermission(String code) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null) {
      return false;
    }
    Object principal = auth.getPrincipal();
    if (principal instanceof CustomUserDetails cud) {
      return cud.hasPermission(code);
    }
    return false;
  }

  private LocalTime parseTime(String value) {
    if (value == null) {
      return null;
    }
    if ("now".equalsIgnoreCase(value.trim())) {
      return LocalTime.now();
    }
    return LocalTime.parse(value.trim());
  }

  private Map<String, Long> monthSummary(Long employeeId) {
    Map<String, Long> summary = new LinkedHashMap<>();
    summary.put("present", 0L);
    summary.put("absent", 0L);
    summary.put("half_day", 0L);
    summary.put("leave", 0L);
    if (employeeId == null) {
      return summary;
    }
    LocalDate today = LocalDate.now();
    LocalDate first = today.withDayOfMonth(1);
    for (Object[] row : attendance.countByStatus(employeeId, first, today)) {
      String status = String.valueOf(row[0]).toLowerCase();
      summary.put(status, (Long) row[1]);
    }
    return summary;
  }

  EmployeeDtos.EmployeeResponse toResponse(Employee employee) {
    return new EmployeeDtos.EmployeeResponse(employee.getId(), employee.getVenture().getId(),
        employee.getVenture().getVentureCode(), employee.getEmployeeCode(), employee.getFirstName(),
        employee.getLastName(), employee.fullName(), employee.getPhone(), employee.getEmail(),
        employee.getDepartment(), employee.getDesignation(), employee.getJoiningDate(),
        employee.getSalary(), employee.getStatus(),
        employee.getUser() == null ? null : employee.getUser().getId(),
        employee.getCreatedAt(), employee.getUpdatedAt());
  }

  EmployeeDtos.AttendanceResponse toAttendanceResponse(Attendance row) {
    return new EmployeeDtos.AttendanceResponse(row.getId(), row.getEmployee().getId(),
        row.getEmployee().fullName(), row.getAttendanceDate(), row.getStatus(), row.getCheckIn(),
        row.getCheckOut(), row.getNotes());
  }
}
