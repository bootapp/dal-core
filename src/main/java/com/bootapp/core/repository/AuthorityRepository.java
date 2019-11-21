package com.bootapp.core.repository;

import com.bootapp.core.domain.Authority;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AuthorityRepository extends JpaRepository<Authority, String> {
    Optional<Authority> findOneByGroupIdAndValue(long groupId, long value);
    void deleteByKeyIn(Iterable<String> keys);
}
