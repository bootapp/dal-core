package com.bootapp.core.repository;

import com.bootapp.core.domain.UserOrgs;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserOrgsRepository extends JpaRepository<UserOrgs, Long> {
    List<UserOrgs> findAllByUserId(long userId);
    List<UserOrgs> findAllByUserIdIn(List<Long> userId);
    Optional<UserOrgs> findOneByUserIdAndOrgId(long userId, long orgId);
}
