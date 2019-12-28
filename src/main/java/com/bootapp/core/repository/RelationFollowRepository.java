package com.bootapp.core.repository;

import com.bootapp.core.domain.RelationFollow;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RelationFollowRepository extends JpaRepository<RelationFollow, Long> {
    void deleteByUserIdAndToUserId(long userId, long toId);
}
