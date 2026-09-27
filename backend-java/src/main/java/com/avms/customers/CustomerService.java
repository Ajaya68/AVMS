package com.avms.customers;

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

/** Port of Django customers views: C-#### codes, venture scoping, audited CRUD. */
@Service
public class CustomerService {

  private final CustomerRepository customers;
  private final VentureRepository ventures;
  private final SequenceService sequences;
  private final AuditService audit;

  public CustomerService(CustomerRepository customers, VentureRepository ventures,
      SequenceService sequences, AuditService audit) {
    this.customers = customers;
    this.ventures = ventures;
    this.sequences = sequences;
    this.audit = audit;
  }

  @Transactional(readOnly = true)
  public Page<CustomerDtos.CustomerResponse> list(String search, String status, Pageable pageable) {
    Long ventureId = VentureContextHolder.get();
    String pattern = search != null && !search.isBlank() ? "%" + search.trim().toLowerCase() + "%" : null;
    String st = status != null && !status.isBlank() ? status.trim().toUpperCase() : null;
    return customers.search(ventureId, pattern, st, pageable).map(this::toResponse);
  }

  @Transactional
  public CustomerDtos.CustomerResponse create(CustomerDtos.CustomerRequest req) {
    req.validate(true);
    Venture venture = resolveVenture(req.venture());
    Customer customer = new Customer();
    customer.setVenture(venture);
    apply(req, customer);
    customer.setCustomerCode(sequences.generateCode("customers", venture.getId(), "C"));
    if (customer.getStatus() == null) {
      customer.setStatus("ACTIVE");
    }
    if (customer.getCreditLimit() == null) {
      customer.setCreditLimit(BigDecimal.ZERO);
    }
    customers.save(customer);
    audit.log("CREATE", "customers", "Customer", String.valueOf(customer.getId()),
        "Customer " + customer.getCustomerCode() + " created.");
    return toResponse(customer);
  }

  @Transactional(readOnly = true)
  public CustomerDtos.CustomerResponse get(Long id) {
    return toResponse(findScoped(id));
  }

  @Transactional
  public CustomerDtos.CustomerResponse update(Long id, CustomerDtos.CustomerRequest req) {
    Customer customer = findScoped(id);
    apply(req, customer);
    customers.save(customer);
    audit.log("UPDATE", "customers", "Customer", String.valueOf(id),
        "Customer " + customer.getCustomerCode() + " updated.");
    return toResponse(customer);
  }

  @Transactional
  public void delete(Long id) {
    Customer customer = findScoped(id);
    customers.delete(customer);
    audit.log("DELETE", "customers", "Customer", String.valueOf(id),
        "Customer " + customer.getCustomerCode() + " deleted.");
  }

  private Customer findScoped(Long id) {
    Long ventureId = VentureContextHolder.get();
    Customer customer = ventureId == null
        ? customers.findById(id).orElse(null)
        : customers.findByIdAndVentureId(id, ventureId).orElse(null);
    if (customer == null) {
      throw new ResourceNotFoundException("Customer not found.");
    }
    return customer;
  }

  private Venture resolveVenture(Long requested) {
    Long ventureId = requested != null ? requested : VentureContextHolder.get();
    if (ventureId == null) {
      throw new IllegalArgumentException("venture is required.");
    }
    return ventures.findById(ventureId)
        .orElseThrow(() -> new ResourceNotFoundException("Venture not found."));
  }

  private void apply(CustomerDtos.CustomerRequest req, Customer customer) {
    if (req.name() != null) {
      customer.setName(req.name());
    }
    if (req.phone() != null) {
      customer.setPhone(req.phone());
    }
    if (req.email() != null) {
      customer.setEmail(req.email());
    }
    if (req.address() != null) {
      customer.setAddress(req.address());
    }
    if (req.city() != null) {
      customer.setCity(req.city());
    }
    if (req.state() != null) {
      customer.setState(req.state());
    }
    if (req.pincode() != null) {
      customer.setPincode(req.pincode());
    }
    if (req.gstNumber() != null) {
      customer.setGstNumber(req.gstNumber());
    }
    if (req.creditLimit() != null) {
      customer.setCreditLimit(req.creditLimit());
    }
    if (req.status() != null) {
      customer.setStatus(req.status().toUpperCase());
    }
  }

  CustomerDtos.CustomerResponse toResponse(Customer customer) {
    return new CustomerDtos.CustomerResponse(customer.getId(), customer.getVenture().getId(),
        customer.getVenture().getVentureCode(), customer.getCustomerCode(), customer.getName(),
        customer.getPhone(), customer.getEmail(), customer.getAddress(), customer.getCity(),
        customer.getState(), customer.getPincode(), customer.getGstNumber(), customer.getCreditLimit(),
        customer.getStatus(), customer.getCreatedAt(), customer.getUpdatedAt());
  }
}
