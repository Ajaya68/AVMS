package com.avms.customers;

import com.avms.common.BaseEntity;
import com.avms.ventures.Venture;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;

/** Port of Django customers.Customer: venture-scoped, auto C-#### codes. */
@Entity
@Table(name = "CUSTOMERS_CUSTOMER",
    uniqueConstraints = @UniqueConstraint(name = "uniq_customer_venture_code",
        columnNames = {"venture_id", "customer_code"}))
public class Customer extends BaseEntity {

  @jakarta.persistence.Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "venture_id", nullable = false)
  private Venture venture;

  @Column(name = "customer_code", nullable = false, length = 20)
  private String customerCode;

  @Column(name = "name", nullable = false, length = 150)
  private String name;

  @Column(name = "phone", length = 30)
  private String phone;

  @Column(name = "email", length = 254)
  private String email;

  @Column(name = "address", length = 255)
  private String address;

  @Column(name = "city", length = 100)
  private String city;

  @Column(name = "state", length = 100)
  private String state;

  @Column(name = "pincode", length = 20)
  private String pincode;

  @Column(name = "gst_number", length = 30)
  private String gstNumber;

  @Column(name = "credit_limit", nullable = false, precision = 12, scale = 2)
  private BigDecimal creditLimit = BigDecimal.ZERO;

  @Column(name = "status", nullable = false, length = 20)
  private String status = "ACTIVE";

  public Customer() {}

  public Long getId() { return id; }
  public Venture getVenture() { return venture; }
  public void setVenture(Venture venture) { this.venture = venture; }
  public String getCustomerCode() { return customerCode; }
  public void setCustomerCode(String customerCode) { this.customerCode = customerCode; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public String getPhone() { return phone; }
  public void setPhone(String phone) { this.phone = phone; }
  public String getEmail() { return email; }
  public void setEmail(String email) { this.email = email; }
  public String getAddress() { return address; }
  public void setAddress(String address) { this.address = address; }
  public String getCity() { return city; }
  public void setCity(String city) { this.city = city; }
  public String getState() { return state; }
  public void setState(String state) { this.state = state; }
  public String getPincode() { return pincode; }
  public void setPincode(String pincode) { this.pincode = pincode; }
  public String getGstNumber() { return gstNumber; }
  public void setGstNumber(String gstNumber) { this.gstNumber = gstNumber; }
  public BigDecimal getCreditLimit() { return creditLimit; }
  public void setCreditLimit(BigDecimal creditLimit) { this.creditLimit = creditLimit; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
}
