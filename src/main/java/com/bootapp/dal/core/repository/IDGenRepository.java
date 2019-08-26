package com.bootapp.dal.core.repository;

import com.bootapp.dal.core.domain.IDGen;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IDGenRepository extends JpaRepository<IDGen, Long> {
}
