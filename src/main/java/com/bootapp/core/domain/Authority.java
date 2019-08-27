package com.bootapp.core.domain;

import com.bootapp.core.grpc.CoreCommon;
import com.bootapp.core.grpc.DalAuth;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "authorities")
public class Authority extends AbstractEntity {
    @Id
    @Column(name = "auth_key", nullable = false, updatable = false, length = 50)
    String authKey;

    @Column(name = "auth_value")
    long authValue;

    @Column(name = "group_auth_key", nullable = false, length = 50)
    String groupAuthKey;

    @Column(length = 50)
    String name;

    public DalAuth.Authority toProto() {
        DalAuth.Authority.Builder proto = DalAuth.Authority.newBuilder();
        proto.setGroupAuthKey(groupAuthKey);
        proto.setAuthKey(authKey);
        proto.setAuthValue(authValue);
        proto.setName(name);
        return proto.buildPartial();
    }
}
