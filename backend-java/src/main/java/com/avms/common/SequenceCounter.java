package com.avms.common;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Port of Django core.SequenceCounter: one row per (module, venture). */
@Entity
@Table(
    name = "CORE_SEQUENCE_COUNTER",
    uniqueConstraints = @UniqueConstraint(name = "uniq_sequence_module_venture", columnNames = {"module", "venture_id"}))
public class SequenceCounter {

  @jakarta.persistence.Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "module", nullable = false, length = 40)
  private String module;

  @Column(name = "venture_id")
  @JdbcTypeCode(SqlTypes.BIGINT)
  private Long ventureId;

  @Column(name = "next_value", nullable = false)
  private long nextValue = 1L;

  public SequenceCounter() {}

  public SequenceCounter(String module, Long ventureId) {
    this.module = module;
    this.ventureId = ventureId;
  }

  public Long getId() {
    return id;
  }

  public String getModule() {
    return module;
  }

  public Long getVentureId() {
    return ventureId;
  }

  public long getNextValue() {
    return nextValue;
  }

  public void setNextValue(long nextValue) {
    this.nextValue = nextValue;
  }
}
