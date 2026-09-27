package com.avms.common;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Port of Django core.services.generate_code: SELECT FOR UPDATE counter,
 * format PREFIX-0001, never reused after delete.
 */
@Service
public class SequenceService {

  private final SequenceCounterRepository repository;

  public SequenceService(SequenceCounterRepository repository) {
    this.repository = repository;
  }

  @Transactional
  public String generateCode(String module, Long ventureId, String prefix) {
    SequenceCounter counter =
        repository
            .findForUpdate(module, ventureId)
            .orElseGet(() -> new SequenceCounter(module, ventureId));
    long value = counter.getNextValue();
    counter.setNextValue(value + 1);
    repository.save(counter);
    return String.format("%s-%04d", prefix, value);
  }
}
