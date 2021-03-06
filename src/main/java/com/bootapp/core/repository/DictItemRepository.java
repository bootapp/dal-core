package com.bootapp.core.repository;

import com.bootapp.core.domain.DictItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DictItemRepository extends JpaRepository<DictItem, Long> {
    void deleteByIdIn(List<Long> ids);
}
