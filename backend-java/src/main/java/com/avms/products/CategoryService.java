package com.avms.products;

import com.avms.audit.AuditService;
import com.avms.common.ResourceNotFoundException;
import com.avms.common.VentureContextHolder;
import com.avms.ventures.Venture;
import com.avms.ventures.VentureRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Port of Django products category views: venture scoping, audited CRUD. */
@Service
public class CategoryService {

  private final CategoryRepository categories;
  private final ProductRepository products;
  private final VentureRepository ventures;
  private final AuditService audit;

  public CategoryService(CategoryRepository categories, ProductRepository products,
      VentureRepository ventures, AuditService audit) {
    this.categories = categories;
    this.products = products;
    this.ventures = ventures;
    this.audit = audit;
  }

  @Transactional(readOnly = true)
  public Page<ProductsDtos.CategoryResponse> list(String search, String status, Pageable pageable) {
    Long ventureId = VentureContextHolder.get();
    String pattern = search != null && !search.isBlank() ? "%" + search.trim().toLowerCase() + "%" : null;
    String st = status != null && !status.isBlank() ? status.trim().toUpperCase() : null;
    return categories.search(ventureId, pattern, st, pageable).map(this::toResponse);
  }

  @Transactional
  public ProductsDtos.CategoryResponse create(ProductsDtos.CategoryRequest req) {
    req.validate(true);
    Venture venture = resolveVenture(req.venture());
    if (categories.existsByVentureIdAndCategoryName(venture.getId(), req.categoryName().trim())) {
      throw new IllegalArgumentException("Category with this name already exists in this venture.");
    }
    Category category = new Category();
    category.setVenture(venture);
    apply(req, category);
    if (category.getStatus() == null) {
      category.setStatus("ACTIVE");
    }
    categories.save(category);
    audit.log("CREATE", "categories", "Category", String.valueOf(category.getId()),
        "Category " + category.getCategoryName() + " created.");
    return toResponse(category);
  }

  @Transactional(readOnly = true)
  public ProductsDtos.CategoryResponse get(Long id) {
    return toResponse(findScoped(id));
  }

  @Transactional
  public ProductsDtos.CategoryResponse update(Long id, ProductsDtos.CategoryRequest req) {
    Category category = findScoped(id);
    if (req.categoryName() != null && !req.categoryName().trim().equalsIgnoreCase(category.getCategoryName())
        && categories.existsByVentureIdAndCategoryName(category.getVenture().getId(), req.categoryName().trim())) {
      throw new IllegalArgumentException("Category with this name already exists in this venture.");
    }
    apply(req, category);
    categories.save(category);
    audit.log("UPDATE", "categories", "Category", String.valueOf(id),
        "Category " + category.getCategoryName() + " updated.");
    return toResponse(category);
  }

  @Transactional
  public void delete(Long id) {
    Category category = findScoped(id);
    for (Product product : products.findByCategoryId(id)) {
      product.setCategory(null);
    }
    categories.delete(category);
    audit.log("DELETE", "categories", "Category", String.valueOf(id),
        "Category " + category.getCategoryName() + " deleted.");
  }

  private Category findScoped(Long id) {
    Long ventureId = VentureContextHolder.get();
    Category category = ventureId == null
        ? categories.findById(id).orElse(null)
        : categories.findByIdAndVentureId(id, ventureId).orElse(null);
    if (category == null) {
      throw new ResourceNotFoundException("Category not found.");
    }
    return category;
  }

  private Venture resolveVenture(Long requested) {
    Long ventureId = requested != null ? requested : VentureContextHolder.get();
    if (ventureId == null) {
      throw new IllegalArgumentException("venture is required.");
    }
    return ventures.findById(ventureId)
        .orElseThrow(() -> new ResourceNotFoundException("Venture not found."));
  }

  private void apply(ProductsDtos.CategoryRequest req, Category category) {
    if (req.categoryName() != null) {
      category.setCategoryName(req.categoryName().trim());
    }
    if (req.description() != null) {
      category.setDescription(req.description());
    }
    if (req.status() != null) {
      category.setStatus(req.status().toUpperCase());
    }
  }

  ProductsDtos.CategoryResponse toResponse(Category category) {
    return new ProductsDtos.CategoryResponse(category.getId(), category.getVenture().getId(),
        category.getVenture().getVentureCode(), category.getCategoryName(), category.getDescription(),
        category.getStatus(), category.getCreatedAt(), category.getUpdatedAt());
  }
}
