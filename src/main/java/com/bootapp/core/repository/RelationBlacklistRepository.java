package com.bootapp.core.repository;

import com.bootapp.core.domain.RelationBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RelationBlacklistRepository extends JpaRepository<RelationBlacklist, Long> {
    void deleteByUserIdAndBlockedUserId(long userId, long blockedId);
}
