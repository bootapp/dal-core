package com.bootapp.core.repository;

import com.bootapp.core.domain.RoleUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleUserRepository extends JpaRepository<RoleUser, Long> {
    void deleteAllByIdIn(Iterable<Long> ids);
}
