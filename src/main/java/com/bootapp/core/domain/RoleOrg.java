package com.bootapp.core.domain;

import com.bootapp.grpc.core.CoreCommon;

import javax.persistence.*;

@Entity
@Table(name = "roles_org")
public class RoleOrg extends AbstractEntity {
    @Id
    @Column(nullable = false, updatable = false)
    long id;

    @Column(length = 50)
    String name;

    @Column
    String remark;

    @Lob
    @Column(name = "authorities")
    String authorities;

    public void fromProto(CoreCommon.RoleOrgEdit proto) {
        if (proto.hasName()) name = proto.getName().getValue();

    }
    public CoreCommon.RoleOrg toProto() {
        CoreCommon.RoleOrg.Builder builder = CoreCommon.RoleOrg.newBuilder();
        builder.setId(id);
        if (name != null) builder.setName(name);
        if (remark != null) builder.setRemark(remark);
        if (authorities != null) builder.setAuthorities(authorities);
        return builder.build();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
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
