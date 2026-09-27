package com.avms.expenses;

import com.avms.accounts.AccountsUser;
import com.avms.accounts.AccountsUserRepository;
import com.avms.audit.AuditService;
import com.avms.common.Money;
import com.avms.common.ResourceNotFoundException;
import com.avms.common.VentureContextHolder;
import com.avms.ventures.Venture;
import com.avms.ventures.VentureRepository;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Port of Django expenses views: category reference data plus venture-scoped CRUD. */
@Service
public class ExpenseService {

  private static final Set<String> METHODS = Set.of("CASH", "UPI", "BANK_TRANSFER", "CARD", "OTHER");

  private final ExpenseRepository expenses;
  private final ExpenseCategoryRepository categories;
  private final VentureRepository ventures;
  private final AccountsUserRepository users;
  private final AuditService audit;

  public ExpenseService(ExpenseRepository expenses, ExpenseCategoryRepository categories,
      VentureRepository ventures, AccountsUserRepository users, AuditService audit) {
    this.expenses = expenses;
    this.categories = categories;
    this.ventures = ventures;
    this.users = users;
    this.audit = audit;
  }

  @Transactional(readOnly = true)
  public List<ExpenseDtos.CategoryResponse> listCategories() {
    return categories.findAllByOrderByCategoryNameAsc().stream()
        .map(c -> new ExpenseDtos.CategoryResponse(c.getId(), c.getCategoryCode(), c.getCategoryName()))
        .toList();
  }

  @Transactional(readOnly = true)
  public Page<ExpenseDtos.ExpenseResponse> list(String search, Long categoryId, Pageable pageable) {
    Long ventureId = VentureContextHolder.get();
    String pattern = search != null && !search.isBlank() ? "%" + search.trim().toLowerCase() + "%" : null;
    return expenses.search(ventureId, categoryId, pattern, pageable).map(this::toResponse);
  }

  @Transactional
  public ExpenseDtos.ExpenseResponse create(ExpenseDtos.ExpenseRequest req) {
    req.validate(true);
    Venture venture = resolveVenture(req.venture());
    ExpenseCategory category = categories.findById(req.category())
        .orElseThrow(() -> new ResourceNotFoundException("Expense category not found."));
    Expense expense = new Expense();
    expense.setVenture(venture);
    expense.setCategory(category);
    expense.setAmount(Money.amount(req.amount()));
    expense.setExpenseDate(req.expenseDate());
    expense.setPaymentMethod(validatedMethod(req.paymentMethod()));
    expense.setDescription(req.description());
    expense.setCreatedBy(currentUser());
    expenses.save(expense);
    audit.log("CREATE", "expenses", "Expense", String.valueOf(expense.getId()),
        "Expense of " + expense.getAmount() + " (" + category.getCategoryName() + ") recorded.");
    return toResponse(expense);
  }

  @Transactional(readOnly = true)
  public ExpenseDtos.ExpenseResponse get(Long id) {
    return toResponse(findScoped(id));
  }

  @Transactional
  public ExpenseDtos.ExpenseResponse update(Long id, ExpenseDtos.ExpenseRequest req) {
    Expense expense = findScoped(id);
    if (req.category() != null) {
      expense.setCategory(categories.findById(req.category())
          .orElseThrow(() -> new ResourceNotFoundException("Expense category not found.")));
    }
    if (req.amount() != null) {
      expense.setAmount(Money.amount(req.amount()));
    }
    if (req.expenseDate() != null) {
      expense.setExpenseDate(req.expenseDate());
    }
    if (req.paymentMethod() != null) {
      expense.setPaymentMethod(validatedMethod(req.paymentMethod()));
    }
    if (req.description() != null) {
      expense.setDescription(req.description());
    }
    expenses.save(expense);
    audit.log("UPDATE", "expenses", "Expense", String.valueOf(id), "Expense updated.");
    return toResponse(expense);
  }

  @Transactional
  public void delete(Long id) {
    Expense expense = findScoped(id);
    expenses.delete(expense);
    audit.log("DELETE", "expenses", "Expense", String.valueOf(id), "Expense deleted.");
  }

  private Expense findScoped(Long id) {
    Long ventureId = VentureContextHolder.get();
    Expense expense = ventureId == null
        ? expenses.findById(id).orElse(null)
        : expenses.findByIdAndVentureId(id, ventureId).orElse(null);
    if (expense == null) {
      throw new ResourceNotFoundException("Expense not found.");
    }
    return expense;
  }

  private Venture resolveVenture(Long requested) {
    Long ventureId = requested != null ? requested : VentureContextHolder.get();
    if (ventureId == null) {
      throw new IllegalArgumentException("venture is required.");
    }
    return ventures.findById(ventureId)
        .orElseThrow(() -> new ResourceNotFoundException("Venture not found."));
  }

  private String validatedMethod(String method) {
    String m = method == null ? "CASH" : method.trim().toUpperCase();
    if (!METHODS.contains(m)) {
      throw new IllegalArgumentException("Unknown payment_method: " + method);
    }
    return m;
  }

  private AccountsUser currentUser() {
    String email = com.avms.security.SecurityEmails.currentUserEmail();
    if (email == null) {
      return null;
    }
    return users.findByEmailIgnoreCase(email).orElse(null);
  }

  ExpenseDtos.ExpenseResponse toResponse(Expense expense) {
    return new ExpenseDtos.ExpenseResponse(expense.getId(), expense.getVenture().getId(),
        expense.getCategory().getId(), expense.getCategory().getCategoryName(), expense.getAmount(),
        expense.getExpenseDate(), expense.getPaymentMethod(), expense.getDescription(),
        expense.getCreatedAt());
  }
}
