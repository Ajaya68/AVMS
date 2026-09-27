package com.avms.products;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Table;

/** Port of Django products.Unit: global (no venture), user-provided codes. */
@Entity
@Table(name = "PRODUCTS_UNIT")
public class Unit {

  @jakarta.persistence.Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "unit_code", nullable = false, unique = true, length = 20)
  private String unitCode;

  @Column(name = "unit_name", nullable = false, length = 50)
  private String unitName;

  @Column(name = "is_base", nullable = false)
  private Boolean isBase = true;

  public Unit() {}

  public Long getId() { return id; }
  public String getUnitCode() { return unitCode; }
  public void setUnitCode(String unitCode) { this.unitCode = unitCode; }
  public String getUnitName() { return unitName; }
  public void setUnitName(String unitName) { this.unitName = unitName; }
  public Boolean getIsBase() { return isBase; }
  public void setIsBase(Boolean isBase) { this.isBase = isBase; }
}
