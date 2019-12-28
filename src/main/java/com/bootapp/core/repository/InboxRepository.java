package com.bootapp.core.repository;

import com.bootapp.core.domain.Inbox;
import com.bootapp.core.domain.RelationVisit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InboxRepository extends JpaRepository<Inbox, Long> {
    void deleteByIdIn(List<Long> ids);
}
