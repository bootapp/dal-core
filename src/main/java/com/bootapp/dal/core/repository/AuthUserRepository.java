package com.bootapp.dal.core.repository;

import com.bootapp.dal.core.domain.AuthorityUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthUserRepository extends JpaRepository<AuthorityUser, Long> {
}
