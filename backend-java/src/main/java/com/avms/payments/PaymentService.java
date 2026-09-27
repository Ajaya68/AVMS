package com.avms.payments;

import com.avms.accounts.AccountsUser;
import com.avms.accounts.AccountsUserRepository;
import com.avms.audit.AuditService;
import com.avms.common.Money;
import com.avms.common.ResourceNotFoundException;
import com.avms.common.VentureContextHolder;
import com.avms.notifications.NotificationService;
import com.avms.purchases.Purchase;
import com.avms.purchases.PurchaseRepository;
import com.avms.sales.Sale;
import com.avms.sales.SaleRepository;
import com.avms.ventures.Venture;
import com.avms.ventures.VentureRepository;
import java.math.BigDecimal;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Port of Django payments services/views: capped bill settlement with reversal on delete. */
@Service
public class PaymentService {

  private static final Set<String> METHODS = Set.of("CASH", "UPI", "BANK_TRANSFER", "CARD", "OTHER");

  private final PaymentRepository payments;
  private final SaleRepository sales;
  private final PurchaseRepository purchases;
  private final VentureRepository ventures;
  private final AccountsUserRepository users;
  private final AuditService audit;
  private final NotificationService notifications;

  public PaymentService(PaymentRepository payments, SaleRepository sales, PurchaseRepository purchases,
      VentureRepository ventures, AccountsUserRepository users, AuditService audit,
      NotificationService notifications) {
    this.payments = payments;
    this.sales = sales;
    this.purchases = purchases;
    this.ventures = ventures;
    this.users = users;
    this.audit = audit;
    this.notifications = notifications;
  }

  @Transactional(readOnly = true)
  public Page<PaymentDtos.PaymentResponse> list(String paymentType, String referenceType, String search,
      Pageable pageable) {
    Long ventureId = VentureContextHolder.get();
    String pt = paymentType != null && !paymentType.isBlank() ? paymentType.trim().toUpperCase() : null;
    String rt = referenceType != null && !referenceType.isBlank() ? referenceType.trim().toUpperCase() : null;
    String pattern = search != null && !search.isBlank() ? "%" + search.trim().toLowerCase() + "%" : null;
    return payments.search(ventureId, pt, rt, pattern, pageable).map(this::toResponse);
  }

  @Transactional
  public PaymentDtos.PaymentResponse create(PaymentDtos.PaymentRequest req) {
    req.validate();
    String paymentType = req.paymentType().trim().toUpperCase();
    String referenceType = req.referenceType().trim().toUpperCase();
    if (!Set.of("RECEIVED", "PAID").contains(paymentType)) {
      throw new IllegalArgumentException("Unknown payment_type: " + req.paymentType());
    }
    if (!Set.of("SALE", "PURCHASE").contains(referenceType)) {
      throw new IllegalArgumentException("Unknown reference_type: " + req.referenceType());
    }
    if ("RECEIVED".equals(paymentType) && !"SALE".equals(referenceType)) {
      throw new IllegalArgumentException("RECEIVED payments must reference a SALE.");
    }
    if ("PAID".equals(paymentType) && !"PURCHASE".equals(referenceType)) {
      throw new IllegalArgumentException("PAID payments must reference a PURCHASE.");
    }
    String method = req.paymentMethod() == null ? "CASH" : req.paymentMethod().trim().toUpperCase();
    if (!METHODS.contains(method)) {
      throw new IllegalArgumentException("Unknown payment_method: " + req.paymentMethod());
    }

    Venture venture = resolveVenture(req.venture());
    BigDecimal outstanding;
    String billNumber;
    if ("SALE".equals(referenceType)) {
      Sale sale = saleInVenture(req.referenceId(), venture.getId());
      outstanding = sale.getTotalAmount().subtract(sale.getPaidAmount()).subtract(sale.getReturnedAmount());
      billNumber = sale.getInvoiceNumber();
      if (Money.amount(req.amount()).compareTo(Money.amount(outstanding)) > 0) {
        throw new IllegalArgumentException(
            "Payment exceeds outstanding balance (" + Money.amount(outstanding) + ").");
      }
      sale.setPaidAmount(Money.amount(sale.getPaidAmount().add(req.amount())));
      sale.setDueAmount(Money.due(sale.getTotalAmount(), sale.getPaidAmount(), sale.getReturnedAmount()));
      sales.save(sale);
    } else {
      Purchase purchase = purchaseInVenture(req.referenceId(), venture.getId());
      outstanding = purchase.getTotalAmount().subtract(purchase.getPaidAmount())
          .subtract(purchase.getReturnedAmount());
      billNumber = purchase.getInvoiceNumber();
      if (Money.amount(req.amount()).compareTo(Money.amount(outstanding)) > 0) {
        throw new IllegalArgumentException(
            "Payment exceeds outstanding balance (" + Money.amount(outstanding) + ").");
      }
      purchase.setPaidAmount(Money.amount(purchase.getPaidAmount().add(req.amount())));
      purchase.setDueAmount(
          Money.due(purchase.getTotalAmount(), purchase.getPaidAmount(), purchase.getReturnedAmount()));
      purchases.save(purchase);
    }

    Payment payment = new Payment();
    payment.setVenture(venture);
    payment.setPaymentType(paymentType);
    payment.setReferenceType(referenceType);
    payment.setReferenceId(req.referenceId());
    payment.setAmount(Money.amount(req.amount()));
    payment.setPaymentDate(req.paymentDate());
    payment.setPaymentMethod(method);
    payment.setTransactionReference(req.transactionReference());
    payment.setNotes(req.notes());
    payment.setCreatedBy(currentUser());
    payments.save(payment);

    audit.log("CREATE", "payments", "Payment", String.valueOf(payment.getId()),
        paymentType + " payment of " + payment.getAmount() + " against " + billNumber + ".");
    String event = "RECEIVED".equals(paymentType) ? "PAYMENT_RECEIVED" : "PAYMENT_PAID";
    notifications.notifyByPermission(event,
        paymentType + " payment of " + payment.getAmount() + " recorded against " + billNumber + ".",
        "payments.manage", "/payments/" + payment.getId());
    return toResponse(payment);
  }

  @Transactional(readOnly = true)
  public PaymentDtos.PaymentResponse get(Long id) {
    return toResponse(findScoped(id));
  }

  @Transactional
  public void delete(Long id) {
    Payment payment = findScoped(id);
    if ("SALE".equals(payment.getReferenceType())) {
      Sale sale = sales.findById(payment.getReferenceId()).orElse(null);
      if (sale != null) {
        BigDecimal paid = sale.getPaidAmount().subtract(payment.getAmount());
        if (paid.compareTo(BigDecimal.ZERO) < 0) {
          paid = BigDecimal.ZERO;
        }
        sale.setPaidAmount(Money.amount(paid));
        sale.setDueAmount(Money.due(sale.getTotalAmount(), sale.getPaidAmount(), sale.getReturnedAmount()));
        sales.save(sale);
      }
    } else {
      Purchase purchase = purchases.findById(payment.getReferenceId()).orElse(null);
      if (purchase != null) {
        BigDecimal paid = purchase.getPaidAmount().subtract(payment.getAmount());
        if (paid.compareTo(BigDecimal.ZERO) < 0) {
          paid = BigDecimal.ZERO;
        }
        purchase.setPaidAmount(Money.amount(paid));
        purchase.setDueAmount(
            Money.due(purchase.getTotalAmount(), purchase.getPaidAmount(), purchase.getReturnedAmount()));
        purchases.save(purchase);
      }
    }
    payments.delete(payment);
    audit.log("DELETE", "payments", "Payment", String.valueOf(id),
        "Payment of " + payment.getAmount() + " reversed.");
  }

  private Payment findScoped(Long id) {
    Long ventureId = VentureContextHolder.get();
    Payment payment = ventureId == null
        ? payments.findById(id).orElse(null)
        : payments.findByIdAndVentureId(id, ventureId).orElse(null);
    if (payment == null) {
      throw new ResourceNotFoundException("Payment not found.");
    }
    return payment;
  }

  private Sale saleInVenture(Long id, Long ventureId) {
    Sale sale = sales.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Sale not found."));
    if (!sale.getVenture().getId().equals(ventureId)) {
      throw new IllegalArgumentException("Sale belongs to a different venture.");
    }
    return sale;
  }

  private Purchase purchaseInVenture(Long id, Long ventureId) {
    Purchase purchase = purchases.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Purchase not found."));
    if (!purchase.getVenture().getId().equals(ventureId)) {
      throw new IllegalArgumentException("Purchase belongs to a different venture.");
    }
    return purchase;
  }

  private Venture resolveVenture(Long requested) {
    Long ventureId = requested != null ? requested : VentureContextHolder.get();
    if (ventureId == null) {
      throw new IllegalArgumentException("venture is required.");
    }
    return ventures.findById(ventureId)
        .orElseThrow(() -> new ResourceNotFoundException("Venture not found."));
  }

  private AccountsUser currentUser() {
    String email = com.avms.security.SecurityEmails.currentUserEmail();
    if (email == null) {
      return null;
    }
    return users.findByEmailIgnoreCase(email).orElse(null);
  }

  PaymentDtos.PaymentResponse toResponse(Payment payment) {
    String number = null;
    if ("SALE".equals(payment.getReferenceType())) {
      Sale sale = sales.findById(payment.getReferenceId()).orElse(null);
      number = sale == null ? null : sale.getInvoiceNumber();
    } else {
      Purchase purchase = purchases.findById(payment.getReferenceId()).orElse(null);
      number = purchase == null ? null : purchase.getInvoiceNumber();
    }
    return new PaymentDtos.PaymentResponse(payment.getId(), payment.getVenture().getId(),
        payment.getPaymentType(), payment.getReferenceType(), payment.getReferenceId(), number,
        payment.getAmount(), payment.getPaymentDate(), payment.getPaymentMethod(),
        payment.getTransactionReference(), payment.getNotes(), payment.getCreatedAt());
  }
}
