package com.bootapp.core.repository;

import com.bootapp.core.domain.Organization;
import com.bootapp.core.domain.RelationVisit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RelationVisitRepository extends JpaRepository<RelationVisit, Long> {
    void deleteByUserIdAndToUserId(long userId, long toId);
}
