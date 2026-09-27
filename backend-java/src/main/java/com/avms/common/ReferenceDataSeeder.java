package com.avms.common;

import com.avms.expenses.ExpenseCategory;
import com.avms.expenses.ExpenseCategoryRepository;
import com.avms.products.Unit;
import com.avms.products.UnitRepository;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds static reference data (units, expense categories) idempotently on every
 * startup. Mirrors the Django data migrations; Flyway DDL never inserts rows so
 * this is the single source of truth on Oracle and on H2 test databases alike.
 */
@Component
public class ReferenceDataSeeder implements ApplicationRunner {

  private static final List<String[]> UNITS = List.of(
      new String[]{"kg", "Kilogram", "true"},
      new String[]{"g", "Gram", "false"},
      new String[]{"pcs", "Pieces", "true"},
      new String[]{"packet", "Packet", "false"},
      new String[]{"litre", "Litre", "true"},
      new String[]{"box", "Box", "false"});

  private static final List<String[]> CATEGORIES = List.of(
      new String[]{"ELECTRICITY", "Electricity"},
      new String[]{"TRANSPORT", "Transport"},
      new String[]{"RENT", "Rent"},
      new String[]{"SALARY", "Salary"},
      new String[]{"RAW_MATERIALS", "Raw Materials"},
      new String[]{"MARKETING", "Marketing"},
      new String[]{"MAINTENANCE", "Maintenance"},
      new String[]{"OTHER", "Other"});

  private final UnitRepository units;
  private final ExpenseCategoryRepository categories;

  public ReferenceDataSeeder(UnitRepository units, ExpenseCategoryRepository categories) {
    this.units = units;
    this.categories = categories;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    for (String[] row : UNITS) {
      if (units.findByUnitCode(row[0]).isEmpty()) {
        Unit unit = new Unit();
        unit.setUnitCode(row[0]);
        unit.setUnitName(row[1]);
        unit.setIsBase(Boolean.parseBoolean(row[2]));
        units.save(unit);
      }
    }
    for (String[] row : CATEGORIES) {
      if (categories.findByCategoryCode(row[0]).isEmpty()) {
        ExpenseCategory category = new ExpenseCategory();
        category.setCategoryCode(row[0]);
        category.setCategoryName(row[1]);
        categories.save(category);
      }
    }
  }
}
