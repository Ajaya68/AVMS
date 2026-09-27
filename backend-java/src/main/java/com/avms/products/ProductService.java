package com.avms.products;

import com.avms.audit.AuditService;
import com.avms.common.ResourceNotFoundException;
import com.avms.common.SequenceService;
import com.avms.common.VentureContextHolder;
import com.avms.ventures.Venture;
import com.avms.ventures.VentureRepository;
import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Port of Django products views: P-#### SKUs, venture scoping, audited CRUD. */
@Service
public class ProductService {

  private final ProductRepository products;
  private final CategoryRepository categories;
  private final UnitRepository units;
  private final VentureRepository ventures;
  private final SequenceService sequences;
  private final AuditService audit;

  public ProductService(ProductRepository products, CategoryRepository categories, UnitRepository units,
      VentureRepository ventures, SequenceService sequences, AuditService audit) {
    this.products = products;
    this.categories = categories;
    this.units = units;
    this.ventures = ventures;
    this.sequences = sequences;
    this.audit = audit;
  }

  @Transactional(readOnly = true)
  public Page<ProductsDtos.ProductResponse> list(String search, String status, Pageable pageable) {
    Long ventureId = VentureContextHolder.get();
    String pattern = search != null && !search.isBlank() ? "%" + search.trim().toLowerCase() + "%" : null;
    String st = status != null && !status.isBlank() ? status.trim().toUpperCase() : null;
    return products.search(ventureId, pattern, st, pageable).map(this::toResponse);
  }

  @Transactional
  public ProductsDtos.ProductResponse create(ProductsDtos.ProductRequest req) {
    req.validate(true);
    Venture venture = resolveVenture(req.venture());
    Unit unit = units.findById(req.unit())
        .orElseThrow(() -> new ResourceNotFoundException("Unit not found."));
    Product product = new Product();
    product.setVenture(venture);
    product.setUnit(unit);
    if (req.category() != null) {
      product.setCategory(resolveCategory(req.category(), venture.getId()));
    }
    apply(req, product);
    product.setSku(sequences.generateCode("products", venture.getId(), "P"));
    if (product.getStatus() == null) {
      product.setStatus("ACTIVE");
    }
    defaults(product);
    products.save(product);
    audit.log("CREATE", "products", "Product", String.valueOf(product.getId()),
        "Product " + product.getSku() + " created.");
    return toResponse(product);
  }

  @Transactional(readOnly = true)
  public ProductsDtos.ProductResponse get(Long id) {
    return toResponse(findScoped(id));
  }

  @Transactional
  public ProductsDtos.ProductResponse update(Long id, ProductsDtos.ProductRequest req) {
    Product product = findScoped(id);
    if (req.unit() != null) {
      product.setUnit(units.findById(req.unit())
          .orElseThrow(() -> new ResourceNotFoundException("Unit not found.")));
    }
    if (req.category() != null) {
      product.setCategory(resolveCategory(req.category(), product.getVenture().getId()));
    }
    apply(req, product);
    products.save(product);
    audit.log("UPDATE", "products", "Product", String.valueOf(id),
        "Product " + product.getSku() + " updated.");
    return toResponse(product);
  }

  @Transactional
  public void delete(Long id) {
    Product product = findScoped(id);
    products.delete(product);
    audit.log("DELETE", "products", "Product", String.valueOf(id),
        "Product " + product.getSku() + " deleted.");
  }

  private Product findScoped(Long id) {
    Long ventureId = VentureContextHolder.get();
    Product product = ventureId == null
        ? products.findById(id).orElse(null)
        : products.findByIdAndVentureId(id, ventureId).orElse(null);
    if (product == null) {
      throw new ResourceNotFoundException("Product not found.");
    }
    return product;
  }

  private Venture resolveVenture(Long requested) {
    Long ventureId = requested != null ? requested : VentureContextHolder.get();
    if (ventureId == null) {
      throw new IllegalArgumentException("venture is required.");
    }
    return ventures.findById(ventureId)
        .orElseThrow(() -> new ResourceNotFoundException("Venture not found."));
  }

  private Category resolveCategory(Long categoryId, Long ventureId) {
    Category category = categories.findById(categoryId)
        .orElseThrow(() -> new ResourceNotFoundException("Category not found."));
    if (!category.getVenture().getId().equals(ventureId)) {
      throw new IllegalArgumentException("Category belongs to a different venture.");
    }
    return category;
  }

  private void apply(ProductsDtos.ProductRequest req, Product product) {
    if (req.productName() != null) {
      product.setProductName(req.productName());
    }
    if (req.description() != null) {
      product.setDescription(req.description());
    }
    if (req.purchasePrice() != null) {
      product.setPurchasePrice(req.purchasePrice());
    }
    if (req.sellingPrice() != null) {
      product.setSellingPrice(req.sellingPrice());
    }
    if (req.taxRate() != null) {
      product.setTaxRate(req.taxRate());
    }
    if (req.reorderLevel() != null) {
      product.setReorderLevel(req.reorderLevel());
    }
    if (req.status() != null) {
      product.setStatus(req.status().toUpperCase());
    }
  }

  private void defaults(Product product) {
    if (product.getPurchasePrice() == null) {
      product.setPurchasePrice(BigDecimal.ZERO);
    }
    if (product.getSellingPrice() == null) {
      product.setSellingPrice(BigDecimal.ZERO);
    }
    if (product.getTaxRate() == null) {
      product.setTaxRate(BigDecimal.ZERO);
    }
    if (product.getReorderLevel() == null) {
      product.setReorderLevel(BigDecimal.ZERO);
    }
  }

  ProductsDtos.ProductResponse toResponse(Product product) {
    Category category = product.getCategory();
    return new ProductsDtos.ProductResponse(product.getId(), product.getVenture().getId(),
        product.getVenture().getVentureCode(),
        category == null ? null : category.getId(),
        category == null ? null : category.getCategoryName(),
        product.getUnit().getId(), product.getUnit().getUnitCode(), product.getSku(),
        product.getProductName(), product.getDescription(), product.getPurchasePrice(),
        product.getSellingPrice(), product.getTaxRate(), product.getReorderLevel(), product.getStatus(),
        product.getCreatedAt(), product.getUpdatedAt());
  }
}
