package com.bootapp.core.domain;

import com.bootapp.core.grpc.CoreCommon;

import javax.persistence.*;

@Entity
@Table(name = "organizations")
@Cacheable
public class Organization extends AbstractEntity {
    @Id
    @Column(nullable = false, updatable = false)
    long id;

    String code;

    @Column(name = "org_role_id", nullable = false, updatable = false)
    long orgRoleId;

    @Column(length = 20)
    String name;

    public CoreCommon.Organization toProto() {
        CoreCommon.Organization.Builder builder = CoreCommon.Organization.newBuilder();
        builder.setId(id);
        builder.setCode(code);
        builder.setOrgRoleId(orgRoleId);
        builder.setName(name);
        builder.setStatusValue(status);
        builder.setCreateAt(createAt);
        builder.setUpdateAt(updateAt);
        return builder.build();
    }

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
