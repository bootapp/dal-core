package com.bootapp.dal.core.repository;

import com.bootapp.dal.core.domain.AuthorityOrg;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthOrgRepository extends JpaRepository<AuthorityOrg, Long> {
}
