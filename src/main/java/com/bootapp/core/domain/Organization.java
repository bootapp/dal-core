package com.bootapp.core.domain;

import javax.persistence.*;

@Entity
@Table(name = "organizations")
@Cacheable
public class Organization extends AbstractEntity {
    @Id
    @Column(nullable = false, updatable = false)
    long id;

    @Column(name = "org_role_id", nullable = false, updatable = false)
    long orgRoleId;

    @Column(length = 20)
    String name;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getOrgRoleId() {
        return orgRoleId;
    }

    public void setOrgRoleId(long orgRoleId) {
        this.orgRoleId = orgRoleId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
