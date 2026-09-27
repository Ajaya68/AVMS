package com.avms.inventory;

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

/** Port of Django inventory.Warehouse: venture-scoped, auto W-#### codes. */
@Entity
@Table(name = "INVENTORY_WAREHOUSE",
    uniqueConstraints = @UniqueConstraint(name = "uniq_warehouse_venture_code",
        columnNames = {"venture_id", "warehouse_code"}))
public class Warehouse extends BaseEntity {

  @jakarta.persistence.Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "venture_id", nullable = false)
  private Venture venture;

  @Column(name = "warehouse_code", nullable = false, length = 20)
  private String warehouseCode;

  @Column(name = "warehouse_name", nullable = false, length = 150)
  private String warehouseName;

  @Column(name = "address", length = 255)
  private String address;

  @Column(name = "city", length = 100)
  private String city;

  @Column(name = "state", length = 100)
  private String state;

  @Column(name = "pincode", length = 20)
  private String pincode;

  @Column(name = "manager", length = 150)
  private String manager;

  @Column(name = "status", nullable = false, length = 20)
  private String status = "ACTIVE";

  public Warehouse() {}

  public Long getId() { return id; }
  public Venture getVenture() { return venture; }
  public void setVenture(Venture venture) { this.venture = venture; }
  public String getWarehouseCode() { return warehouseCode; }
  public void setWarehouseCode(String warehouseCode) { this.warehouseCode = warehouseCode; }
  public String getWarehouseName() { return warehouseName; }
  public void setWarehouseName(String warehouseName) { this.warehouseName = warehouseName; }
  public String getAddress() { return address; }
  public void setAddress(String address) { this.address = address; }
  public String getCity() { return city; }
  public void setCity(String city) { this.city = city; }
  public String getState() { return state; }
  public void setState(String state) { this.state = state; }
  public String getPincode() { return pincode; }
  public void setPincode(String pincode) { this.pincode = pincode; }
  public String getManager() { return manager; }
  public void setManager(String manager) { this.manager = manager; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
}
