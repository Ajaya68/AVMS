package com.avms.expenses;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategory, Long> {

  List<ExpenseCategory> findAllByOrderByCategoryNameAsc();

  Optional<ExpenseCategory> findByCategoryCode(String categoryCode);
}
