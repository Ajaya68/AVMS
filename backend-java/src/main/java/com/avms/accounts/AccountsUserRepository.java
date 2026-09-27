package com.avms.accounts;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountsUserRepository extends JpaRepository<AccountsUser, Long> {
  Optional<AccountsUser> findByEmailIgnoreCase(String email);
  List<AccountsUser> findByIsActiveTrue();
  List<AccountsUser> findByEmailContainingIgnoreCaseOrFullNameContainingIgnoreCase(String email, String name);
}
