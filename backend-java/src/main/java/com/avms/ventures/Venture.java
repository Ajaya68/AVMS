package com.avms.ventures;

import com.avms.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Table;

@Entity
@Table(name = "VENTURES_VENTURE")
public class Venture extends BaseEntity {

  @jakarta.persistence.Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "venture_code", nullable = false, unique = true, length = 20)
  private String ventureCode;

  @Column(name = "venture_name", nullable = false, length = 150)
  private String ventureName;

  @Column(name = "description", length = 1000)
  private String description;

  @Column(name = "business_type", length = 30)
  private String businessType;

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

  @Column(name = "status", nullable = false, length = 20)
  private String status = "ACTIVE";

  public Venture() {}

  public Long getId() { return id; }
  public String getVentureCode() { return ventureCode; }
  public void setVentureCode(String ventureCode) { this.ventureCode = ventureCode; }
  public String getVentureName() { return ventureName; }
  public void setVentureName(String ventureName) { this.ventureName = ventureName; }
  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }
  public String getBusinessType() { return businessType; }
  public void setBusinessType(String businessType) { this.businessType = businessType; }
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
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
}
