package com.bootapp.core.repository;

import com.bootapp.core.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, QuerydslPredicateExecutor<User> {
    Optional<User> findOneByPhone(String phone);
    Optional<User> findOneByUsername(String username);
    Optional<User> findOneByEmail(String email);
}
