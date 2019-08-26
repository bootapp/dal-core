package com.bootapp.dal.core.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "organizations")
public class Organization extends AbstractEntity {
    @Id
    @Column(nullable = false, updatable = false)
    long id;

    @Column(name = "org_role_id", nullable = false, updatable = false)
    long orgRoleId;

    @Column(length = 20)
    String name;
}
