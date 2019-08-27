package com.bootapp.core.repository;

import com.bootapp.core.domain.IDGen;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IDGenRepository extends JpaRepository<IDGen, Long> {
}
