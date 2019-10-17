package com.bootapp.core.repository;

import com.bootapp.core.domain.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    List<Organization> findAllByIdIn(List<Long> ids);
}
