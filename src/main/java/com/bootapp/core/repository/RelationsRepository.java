package com.bootapp.core.repository;

import com.bootapp.core.domain.Relation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RelationsRepository extends JpaRepository<Relation, Long> {
    void deleteByIdIn(List<Long> ids);
}
