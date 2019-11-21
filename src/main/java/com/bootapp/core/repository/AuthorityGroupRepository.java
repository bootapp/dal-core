package com.bootapp.core.repository;

import com.bootapp.core.domain.AuthorityGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuthorityGroupRepository extends JpaRepository<AuthorityGroup, Long> {
    List<AuthorityGroup> findAllByPid(long pId);
    void deleteAllByIdIn(Iterable<Long> ids);
}
