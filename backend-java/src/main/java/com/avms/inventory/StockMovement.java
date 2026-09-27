package com.avms.inventory;

import com.avms.accounts.AccountsUser;
import com.avms.common.BaseEntity;
import com.avms.products.Product;
import com.avms.ventures.Venture;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Port of Django inventory.StockMovement: immutable ledger rows that apply quantity deltas. */
@Entity
@Table(name = "INVENTORY_MOVEMENT")
public class StockMovement extends BaseEntity {

  @jakarta.persistence.Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "venture_id", nullable = false)
  private Venture venture;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "warehouse_id", nullable = false)
  private Warehouse warehouse;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "destination_warehouse_id")
  private Warehouse destinationWarehouse;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id", nullable = false)
  private Product product;

  @Column(name = "movement_type", nullable = false, length = 20)
  private String movementType;

  @Column(name = "quantity", nullable = false, precision = 14, scale = 2)
  private BigDecimal quantity;

  @Column(name = "movement_date", nullable = false)
  private LocalDate movementDate;

  @Column(name = "notes", length = 255)
  private String notes;

  @Column(name = "reference_type", length = 80)
  private String referenceType;

  @Column(name = "reference_id")
  private Long referenceId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by")
  private AccountsUser createdBy;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "paired_movement_id")
  private StockMovement pairedMovement;

  public StockMovement() {}

  public Long getId() { return id; }
  public Venture getVenture() { return venture; }
  public void setVenture(Venture venture) { this.venture = venture; }
  public Warehouse getWarehouse() { return warehouse; }
  public void setWarehouse(Warehouse warehouse) { this.warehouse = warehouse; }
  public Warehouse getDestinationWarehouse() { return destinationWarehouse; }
  public void setDestinationWarehouse(Warehouse destinationWarehouse) {
    this.destinationWarehouse = destinationWarehouse;
  }
  public Product getProduct() { return product; }
  public void setProduct(Product product) { this.product = product; }
  public String getMovementType() { return movementType; }
  public void setMovementType(String movementType) { this.movementType = movementType; }
  public BigDecimal getQuantity() { return quantity; }
  public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
  public LocalDate getMovementDate() { return movementDate; }
  public void setMovementDate(LocalDate movementDate) { this.movementDate = movementDate; }
  public String getNotes() { return notes; }
  public void setNotes(String notes) { this.notes = notes; }
  public String getReferenceType() { return referenceType; }
  public void setReferenceType(String referenceType) { this.referenceType = referenceType; }
  public Long getReferenceId() { return referenceId; }
  public void setReferenceId(Long referenceId) { this.referenceId = referenceId; }
  public AccountsUser getCreatedBy() { return createdBy; }
  public void setCreatedBy(AccountsUser createdBy) { this.createdBy = createdBy; }
  public StockMovement getPairedMovement() { return pairedMovement; }
  public void setPairedMovement(StockMovement pairedMovement) { this.pairedMovement = pairedMovement; }
}
