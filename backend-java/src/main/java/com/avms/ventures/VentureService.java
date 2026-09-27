package com.avms.ventures;

import com.avms.audit.AuditService;
import com.avms.common.ResourceNotFoundException;
import com.avms.common.SequenceService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Port of Django ventures views/services: V-#### codes, search/status, audited CRUD. */
@Service
public class VentureService {

  private final VentureRepository ventures;
  private final SequenceService sequences;
  private final AuditService audit;

  public VentureService(VentureRepository ventures, SequenceService sequences, AuditService audit) {
    this.ventures = ventures;
    this.sequences = sequences;
    this.audit = audit;
  }

  @Transactional(readOnly = true)
  public Page<VentureDtos.VentureResponse> list(String search, String status, Pageable pageable) {
    String pattern = search != null && !search.isBlank() ? "%" + search.trim().toLowerCase() + "%" : null;
    String st = status != null && !status.isBlank() ? status.trim().toUpperCase() : null;
    return ventures.search(pattern, st, pageable).map(this::toResponse);
  }

  @Transactional
  public VentureDtos.VentureResponse create(VentureDtos.VentureRequest req) {
    req.validate(true);
    Venture venture = new Venture();
    apply(req, venture);
    venture.setVentureCode(sequences.generateCode("ventures", null, "V"));
    if (venture.getStatus() == null) {
      venture.setStatus("ACTIVE");
    }
    ventures.save(venture);
    audit.log("CREATE", "ventures", "Venture", String.valueOf(venture.getId()),
        "Venture " + venture.getVentureCode() + " created.");
    return toResponse(venture);
  }

  @Transactional(readOnly = true)
  public VentureDtos.VentureResponse get(Long id) {
    return toResponse(ventures.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Venture not found.")));
  }

  @Transactional
  public VentureDtos.VentureResponse update(Long id, VentureDtos.VentureRequest req) {
    Venture venture = ventures.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Venture not found."));
    apply(req, venture);
    ventures.save(venture);
    audit.log("UPDATE", "ventures", "Venture", String.valueOf(id),
        "Venture " + venture.getVentureCode() + " updated.");
    return toResponse(venture);
  }

  @Transactional
  public void delete(Long id) {
    Venture venture = ventures.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Venture not found."));
    ventures.delete(venture);
    audit.log("DELETE", "ventures", "Venture", String.valueOf(id),
        "Venture " + venture.getVentureCode() + " deleted.");
  }

  private void apply(VentureDtos.VentureRequest req, Venture venture) {
    if (req.ventureName() != null) {
      venture.setVentureName(req.ventureName());
    }
    if (req.description() != null) {
      venture.setDescription(req.description());
    }
    if (req.businessType() != null) {
      venture.setBusinessType(req.businessType());
    }
    if (req.phone() != null) {
      venture.setPhone(req.phone());
    }
    if (req.email() != null) {
      venture.setEmail(req.email());
    }
    if (req.address() != null) {
      venture.setAddress(req.address());
    }
    if (req.city() != null) {
      venture.setCity(req.city());
    }
    if (req.state() != null) {
      venture.setState(req.state());
    }
    if (req.pincode() != null) {
      venture.setPincode(req.pincode());
    }
    if (req.status() != null) {
      venture.setStatus(req.status().toUpperCase());
    }
  }

  VentureDtos.VentureResponse toResponse(Venture venture) {
    return new VentureDtos.VentureResponse(venture.getId(), venture.getVentureCode(), venture.getVentureName(),
        venture.getDescription(), venture.getBusinessType(), venture.getPhone(), venture.getEmail(),
        venture.getAddress(), venture.getCity(), venture.getState(), venture.getPincode(), venture.getStatus());
  }
}
