package com.avms.security;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
  Optional<RefreshToken> findByJti(String jti);

  java.util.List<RefreshToken> findByUserEmailIgnoreCase(String userEmail);
}
