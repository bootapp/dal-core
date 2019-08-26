package com.bootapp.dal.core.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "user_orgs")
public class UserOrgs extends AbstractEntity {
    @Id
    @Column(nullable = false)
    long id;

    @Column(name = "user_id", nullable = false)
    long userId;

    @Column(name = "org_id", nullable = false)
    long orgId;
}
