package com.bootapp.core.domain;

import com.bootapp.grpc.core.CoreCommon;
import com.bootapp.grpc.core.DalSysAuth;

import javax.persistence.*;

@Entity
@Table(name = "authority_groups")
public class AuthorityGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id;
    @Column(name = "pid")
    long pid;
    @Column(length = 50)
    String name;

    public void fromProto(CoreCommon.AuthGroupEdit proto) {
        if (proto.hasPid()) pid = proto.getPid().getValue();
        if (proto.getName() != null) name = proto.getName();
    }
    public CoreCommon.AuthGroup toProto() {
        CoreCommon.AuthGroup.Builder proto = CoreCommon.AuthGroup.newBuilder();
        proto.setId(id);
        proto.setPid(pid);
        if (name != null) proto.setName(name);
        return proto.build();
    }
    public DalSysAuth.SysAuthorityGroup toSysProto() {
        DalSysAuth.SysAuthorityGroup.Builder proto = DalSysAuth.SysAuthorityGroup.newBuilder();
        proto.setId(id);
        proto.setPid(pid);
        if (name != null) proto.setName(name);
        return proto.buildPartial();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getPid() {
        return pid;
    }

    public void setPid(long pid) {
        this.pid = pid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
