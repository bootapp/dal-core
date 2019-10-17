package com.bootapp.core.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "user_orgs")
public class UserOrgs extends AbstractEntity {
    public UserOrgs() {
        super();
    }
    public UserOrgs(long id, long userId, long orgId, long userRoleId) {
        this.id = id;
        this.orgId = orgId;
        this.userId = userId;
        this.userRoleId = userRoleId;
        this.status = 1;
    }
    @Id
    long id;
    @Column(name = "user_id", nullable = false)
    private long userId;

    @Column(name = "org_id", nullable = false)
    private long orgId;

    @Column(name = "user_role_id")
    private long userRoleId;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public long getOrgId() {
        return orgId;
    }

    public void setOrgId(long orgId) {
        this.orgId = orgId;
    }

    public long getUserRoleId() {
        return userRoleId;
    }

    public void setUserRoleId(long userRoleId) {
        this.userRoleId = userRoleId;
    }
}
