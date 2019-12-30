package com.bootapp.core.domain;

import com.bootapp.grpc.core.CoreCommon;
import com.bootapp.grpc.core.DalSysAuth;

import javax.persistence.*;

@Entity
@Table(name = "authorities")
public class Authority {
    @Id
    @Column(name = "key", nullable = false, updatable = false, length = 50)
    String key;
    @Column(name = "group_id")
    long groupId;
    @Column(name = "value")
    long value;
    @Column(length = 50)
    String name;
    public void fromProto(CoreCommon.AuthorityEdit proto) {
        if (!proto.getKey().equals("")) key = proto.getKey();
        if (proto.getGroupId() != 0L) groupId = proto.getGroupId();
        if (proto.hasValue()) value = proto.getValue().getValue();
        if (!proto.getName().equals("")) name = proto.getName();
    }
    public CoreCommon.Authority toProto() {
        CoreCommon.Authority.Builder proto = CoreCommon.Authority.newBuilder();
        if (key != null) proto.setKey(key);
        proto.setGroupId(groupId);
        proto.setValue(value);
        if (name != null) proto.setName(name);
        return proto.build();
    }
    public DalSysAuth.SysAuthority toSysProto() {
        DalSysAuth.SysAuthority.Builder proto = DalSysAuth.SysAuthority.newBuilder();
        if (key != null) proto.setKey(key);
        proto.setGroupId(groupId);
        proto.setValue(value);
        if (name != null) proto.setName(name);
        return proto.buildPartial();
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public long getGroupId() {
        return groupId;
    }

    public void setGroupId(long groupId) {
        this.groupId = groupId;
    }

    public long getValue() {
        return value;
    }

    public void setValue(long value) {
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}