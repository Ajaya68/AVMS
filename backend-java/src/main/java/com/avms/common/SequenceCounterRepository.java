package com.avms.common;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface SequenceCounterRepository extends JpaRepository<SequenceCounter, Long> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select s from SequenceCounter s where s.module = :module and ((s.ventureId is null and :ventureId is null) or s.ventureId = :ventureId)")
  Optional<SequenceCounter> findForUpdate(String module, Long ventureId);
}
