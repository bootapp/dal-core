package com.bootapp.core.repository;

import com.bootapp.core.domain.AuthorityGroups;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthorityGroupRepository extends JpaRepository<AuthorityGroups, Long> {
}
