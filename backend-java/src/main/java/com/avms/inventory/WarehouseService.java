package com.avms.inventory;

import com.avms.audit.AuditService;
import com.avms.common.ResourceNotFoundException;
import com.avms.common.SequenceService;
import com.avms.common.VentureContextHolder;
import com.avms.ventures.Venture;
import com.avms.ventures.VentureRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Port of Django inventory warehouse views: W-#### codes, venture scoping, audited CRUD. */
@Service
public class WarehouseService {

  private final WarehouseRepository warehouses;
  private final VentureRepository ventures;
  private final SequenceService sequences;
  private final AuditService audit;

  public WarehouseService(WarehouseRepository warehouses, VentureRepository ventures,
      SequenceService sequences, AuditService audit) {
    this.warehouses = warehouses;
    this.ventures = ventures;
    this.sequences = sequences;
    this.audit = audit;
  }

  @Transactional(readOnly = true)
  public Page<InventoryDtos.WarehouseResponse> list(String search, String status, Pageable pageable) {
    Long ventureId = VentureContextHolder.get();
    String pattern = search != null && !search.isBlank() ? "%" + search.trim().toLowerCase() + "%" : null;
    String st = status != null && !status.isBlank() ? status.trim().toUpperCase() : null;
    return warehouses.search(ventureId, pattern, st, pageable).map(this::toResponse);
  }

  @Transactional
  public InventoryDtos.WarehouseResponse create(InventoryDtos.WarehouseRequest req) {
    req.validate(true);
    Venture venture = resolveVenture(req.venture());
    Warehouse warehouse = new Warehouse();
    warehouse.setVenture(venture);
    apply(req, warehouse);
    warehouse.setWarehouseCode(sequences.generateCode("warehouses", venture.getId(), "W"));
    if (warehouse.getStatus() == null) {
      warehouse.setStatus("ACTIVE");
    }
    warehouses.save(warehouse);
    audit.log("CREATE", "warehouses", "Warehouse", String.valueOf(warehouse.getId()),
        "Warehouse " + warehouse.getWarehouseCode() + " created.");
    return toResponse(warehouse);
  }

  @Transactional(readOnly = true)
  public InventoryDtos.WarehouseResponse get(Long id) {
    return toResponse(findScoped(id));
  }

  @Transactional
  public InventoryDtos.WarehouseResponse update(Long id, InventoryDtos.WarehouseRequest req) {
    Warehouse warehouse = findScoped(id);
    apply(req, warehouse);
    warehouses.save(warehouse);
    audit.log("UPDATE", "warehouses", "Warehouse", String.valueOf(id),
        "Warehouse " + warehouse.getWarehouseCode() + " updated.");
    return toResponse(warehouse);
  }

  @Transactional
  public void delete(Long id) {
    Warehouse warehouse = findScoped(id);
    warehouses.delete(warehouse);
    audit.log("DELETE", "warehouses", "Warehouse", String.valueOf(id),
        "Warehouse " + warehouse.getWarehouseCode() + " deleted.");
  }

  private Warehouse findScoped(Long id) {
    Long ventureId = VentureContextHolder.get();
    Warehouse warehouse = ventureId == null
        ? warehouses.findById(id).orElse(null)
        : warehouses.findByIdAndVentureId(id, ventureId).orElse(null);
    if (warehouse == null) {
      throw new ResourceNotFoundException("Warehouse not found.");
    }
    return warehouse;
  }

  private Venture resolveVenture(Long requested) {
    Long ventureId = requested != null ? requested : VentureContextHolder.get();
    if (ventureId == null) {
      throw new IllegalArgumentException("venture is required.");
    }
    return ventures.findById(ventureId)
        .orElseThrow(() -> new ResourceNotFoundException("Venture not found."));
  }

  private void apply(InventoryDtos.WarehouseRequest req, Warehouse warehouse) {
    if (req.warehouseName() != null) {
      warehouse.setWarehouseName(req.warehouseName());
    }
    if (req.address() != null) {
      warehouse.setAddress(req.address());
    }
    if (req.city() != null) {
      warehouse.setCity(req.city());
    }
    if (req.state() != null) {
      warehouse.setState(req.state());
    }
    if (req.pincode() != null) {
      warehouse.setPincode(req.pincode());
    }
    if (req.manager() != null) {
      warehouse.setManager(req.manager());
    }
    if (req.status() != null) {
      warehouse.setStatus(req.status().toUpperCase());
    }
  }

  InventoryDtos.WarehouseResponse toResponse(Warehouse warehouse) {
    return new InventoryDtos.WarehouseResponse(warehouse.getId(), warehouse.getVenture().getId(),
        warehouse.getVenture().getVentureCode(), warehouse.getWarehouseCode(), warehouse.getWarehouseName(),
        warehouse.getAddress(), warehouse.getCity(), warehouse.getState(), warehouse.getPincode(),
        warehouse.getManager(), warehouse.getStatus(), warehouse.getCreatedAt(),
        warehouse.getUpdatedAt());
  }
}
