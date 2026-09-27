package com.avms.suppliers;

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

/** Port of Django suppliers views: S-#### codes, venture scoping, audited CRUD. */
@Service
public class SupplierService {

  private final SupplierRepository suppliers;
  private final VentureRepository ventures;
  private final SequenceService sequences;
  private final AuditService audit;

  public SupplierService(SupplierRepository suppliers, VentureRepository ventures,
      SequenceService sequences, AuditService audit) {
    this.suppliers = suppliers;
    this.ventures = ventures;
    this.sequences = sequences;
    this.audit = audit;
  }

  @Transactional(readOnly = true)
  public Page<SupplierDtos.SupplierResponse> list(String search, String status, Pageable pageable) {
    Long ventureId = VentureContextHolder.get();
    String pattern = search != null && !search.isBlank() ? "%" + search.trim().toLowerCase() + "%" : null;
    String st = status != null && !status.isBlank() ? status.trim().toUpperCase() : null;
    return suppliers.search(ventureId, pattern, st, pageable).map(this::toResponse);
  }

  @Transactional
  public SupplierDtos.SupplierResponse create(SupplierDtos.SupplierRequest req) {
    req.validate(true);
    Venture venture = resolveVenture(req.venture());
    Supplier supplier = new Supplier();
    supplier.setVenture(venture);
    apply(req, supplier);
    supplier.setSupplierCode(sequences.generateCode("suppliers", venture.getId(), "S"));
    if (supplier.getStatus() == null) {
      supplier.setStatus("ACTIVE");
    }
    suppliers.save(supplier);
    audit.log("CREATE", "suppliers", "Supplier", String.valueOf(supplier.getId()),
        "Supplier " + supplier.getSupplierCode() + " created.");
    return toResponse(supplier);
  }

  @Transactional(readOnly = true)
  public SupplierDtos.SupplierResponse get(Long id) {
    return toResponse(findScoped(id));
  }

  @Transactional
  public SupplierDtos.SupplierResponse update(Long id, SupplierDtos.SupplierRequest req) {
    Supplier supplier = findScoped(id);
    apply(req, supplier);
    suppliers.save(supplier);
    audit.log("UPDATE", "suppliers", "Supplier", String.valueOf(id),
        "Supplier " + supplier.getSupplierCode() + " updated.");
    return toResponse(supplier);
  }

  @Transactional
  public void delete(Long id) {
    Supplier supplier = findScoped(id);
    suppliers.delete(supplier);
    audit.log("DELETE", "suppliers", "Supplier", String.valueOf(id),
        "Supplier " + supplier.getSupplierCode() + " deleted.");
  }

  private Supplier findScoped(Long id) {
    Long ventureId = VentureContextHolder.get();
    Supplier supplier = ventureId == null
        ? suppliers.findById(id).orElse(null)
        : suppliers.findByIdAndVentureId(id, ventureId).orElse(null);
    if (supplier == null) {
      throw new ResourceNotFoundException("Supplier not found.");
    }
    return supplier;
  }

  private Venture resolveVenture(Long requested) {
    Long ventureId = requested != null ? requested : VentureContextHolder.get();
    if (ventureId == null) {
      throw new IllegalArgumentException("venture is required.");
    }
    return ventures.findById(ventureId)
        .orElseThrow(() -> new ResourceNotFoundException("Venture not found."));
  }

  private void apply(SupplierDtos.SupplierRequest req, Supplier supplier) {
    if (req.name() != null) {
      supplier.setName(req.name());
    }
    if (req.contactPerson() != null) {
      supplier.setContactPerson(req.contactPerson());
    }
    if (req.phone() != null) {
      supplier.setPhone(req.phone());
    }
    if (req.email() != null) {
      supplier.setEmail(req.email());
    }
    if (req.address() != null) {
      supplier.setAddress(req.address());
    }
    if (req.city() != null) {
      supplier.setCity(req.city());
    }
    if (req.state() != null) {
      supplier.setState(req.state());
    }
    if (req.pincode() != null) {
      supplier.setPincode(req.pincode());
    }
    if (req.gstNumber() != null) {
      supplier.setGstNumber(req.gstNumber());
    }
    if (req.paymentTerms() != null) {
      supplier.setPaymentTerms(req.paymentTerms());
    }
    if (req.status() != null) {
      supplier.setStatus(req.status().toUpperCase());
    }
  }

  SupplierDtos.SupplierResponse toResponse(Supplier supplier) {
    return new SupplierDtos.SupplierResponse(supplier.getId(), supplier.getVenture().getId(),
        supplier.getVenture().getVentureCode(), supplier.getSupplierCode(), supplier.getName(),
        supplier.getContactPerson(), supplier.getPhone(), supplier.getEmail(), supplier.getAddress(),
        supplier.getCity(), supplier.getState(), supplier.getPincode(), supplier.getGstNumber(),
        supplier.getPaymentTerms(), supplier.getStatus(), supplier.getCreatedAt(),
        supplier.getUpdatedAt());
  }
}
