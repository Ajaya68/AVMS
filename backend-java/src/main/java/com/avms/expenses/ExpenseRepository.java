package com.avms.expenses;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

  @Query("select e from Expense e where (:ventureId is null or e.venture.id = :ventureId)"
      + " and (:categoryId is null or e.category.id = :categoryId)"
      + " and (:pattern is null or lower(e.description) like :pattern"
      + " or lower(e.category.categoryName) like :pattern)"
      + " order by e.expenseDate desc, e.id desc")
  Page<Expense> search(Long ventureId, Long categoryId, String pattern, Pageable pageable);

  Optional<Expense> findByIdAndVentureId(Long id, Long ventureId);

  @Query("select coalesce(sum(e.amount), 0) from Expense e"
      + " where (:ventureId is null or e.venture.id = :ventureId)"
      + " and e.expenseDate between :from and :to")
  java.math.BigDecimal totalInRange(Long ventureId, java.time.LocalDate from, java.time.LocalDate to);
}
