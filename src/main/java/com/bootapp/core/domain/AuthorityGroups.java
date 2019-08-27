package com.bootapp.core.domain;

import com.bootapp.core.grpc.CoreCommon;
import com.bootapp.core.grpc.DalAuth;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "authority_groups")
public class AuthorityGroups extends AbstractEntity {
    @Id
    @Column(name = "auth_key", nullable = false, updatable = false, length = 50)
    String authKey;

    @Column(name = "auth_value")
    long authValue;

    @Column(length = 50)
    String name;

    @Column(name = "module_id")
    long moduleId; // reserved

    public DalAuth.AuthorityGroup toProto() {
        DalAuth.AuthorityGroup.Builder proto = DalAuth.AuthorityGroup.newBuilder();
        proto.setGroupAuthKey(authKey);
        proto.setGroupAuthValue(authValue);
        proto.setName(name);
        return proto.buildPartial();
    }
}
