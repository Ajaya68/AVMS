package com.avms.products;

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

/** Port of Django products.Category: venture-scoped, name unique per venture. */
@Entity
@Table(name = "PRODUCTS_CATEGORY",
    uniqueConstraints = @UniqueConstraint(name = "uniq_category_venture_name",
        columnNames = {"venture_id", "category_name"}))
public class Category extends BaseEntity {

  @jakarta.persistence.Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "venture_id", nullable = false)
  private Venture venture;

  @Column(name = "category_name", nullable = false, length = 100)
  private String categoryName;

  @Column(name = "description", length = 500)
  private String description;

  @Column(name = "status", nullable = false, length = 20)
  private String status = "ACTIVE";

  public Category() {}

  public Long getId() { return id; }
  public Venture getVenture() { return venture; }
  public void setVenture(Venture venture) { this.venture = venture; }
  public String getCategoryName() { return categoryName; }
  public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
}
