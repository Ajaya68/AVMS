package com.avms.products;

import com.avms.audit.AuditService;
import com.avms.common.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Port of Django products unit views: global codes, audited CRUD. */
@Service
public class UnitService {

  private final UnitRepository units;
  private final ProductRepository products;
  private final AuditService audit;

  public UnitService(UnitRepository units, ProductRepository products, AuditService audit) {
    this.units = units;
    this.products = products;
    this.audit = audit;
  }

  @Transactional(readOnly = true)
  public Page<ProductsDtos.UnitResponse> list(String search, Pageable pageable) {
    String pattern = search != null && !search.isBlank() ? "%" + search.trim().toLowerCase() + "%" : null;
    return units.search(pattern, pageable).map(this::toResponse);
  }

  @Transactional
  public ProductsDtos.UnitResponse create(ProductsDtos.UnitRequest req) {
    req.validate(true);
    if (units.findByUnitCode(req.unitCode().trim()).isPresent()) {
      throw new IllegalArgumentException("Unit with this code already exists.");
    }
    Unit unit = new Unit();
    apply(req, unit);
    units.save(unit);
    audit.log("CREATE", "units", "Unit", String.valueOf(unit.getId()),
        "Unit " + unit.getUnitCode() + " created.");
    return toResponse(unit);
  }

  @Transactional(readOnly = true)
  public ProductsDtos.UnitResponse get(Long id) {
    return toResponse(units.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Unit not found.")));
  }

  @Transactional
  public ProductsDtos.UnitResponse update(Long id, ProductsDtos.UnitRequest req) {
    Unit unit = units.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Unit not found."));
    apply(req, unit);
    units.save(unit);
    audit.log("UPDATE", "units", "Unit", String.valueOf(id),
        "Unit " + unit.getUnitCode() + " updated.");
    return toResponse(unit);
  }

  @Transactional
  public void delete(Long id) {
    Unit unit = units.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Unit not found."));
    if (products.existsByUnitId(id)) {
      throw new com.avms.common.BusinessRuleException("Unit is in use by products and cannot be deleted.");
    }
    units.delete(unit);
    audit.log("DELETE", "units", "Unit", String.valueOf(id),
        "Unit " + unit.getUnitCode() + " deleted.");
  }

  private void apply(ProductsDtos.UnitRequest req, Unit unit) {
    if (req.unitCode() != null) {
      unit.setUnitCode(req.unitCode().trim());
    }
    if (req.unitName() != null) {
      unit.setUnitName(req.unitName());
    }
    if (req.isBase() != null) {
      unit.setIsBase(req.isBase());
    }
  }

  ProductsDtos.UnitResponse toResponse(Unit unit) {
    return new ProductsDtos.UnitResponse(unit.getId(), unit.getUnitCode(), unit.getUnitName(),
        unit.getIsBase());
  }
}
