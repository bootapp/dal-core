package com.bootapp.core.domain;

import javax.persistence.*;

@Entity
@Table(name = "roles_user")
public class RoleUser extends AbstractEntity {
    @Id
    @Column(nullable = false, updatable = false)
    long id;

    @Column(name = "org_id", nullable = false, updatable = false)
    long orgId;

    @Column(length = 50)
    String name;

    @Column
    String remark;

    @Lob
    @Column(name = "authorities")
    String authorities;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getOrgId() {
        return orgId;
    }

    public void setOrgId(long orgId) {
        this.orgId = orgId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getAuthorities() {
        return authorities;
    }

    public void setAuthorities(String authorities) {
        this.authorities = authorities;
    }
}
