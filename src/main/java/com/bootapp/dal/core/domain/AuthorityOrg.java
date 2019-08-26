package com.bootapp.dal.core.domain;

import com.bootapp.dal.core.grpc.Auth;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "authorities_org")
public class AuthorityOrg extends AbstractEntity {
    @Id
    @Column(name = "auth_key", nullable = false, updatable = false, length = 50)
    String authKey;

    @Column(name = "auth_value")
    long authValue;

    @Column(length = 50)
    String name;

    public Auth.AuthorityOrg toProto() {
        Auth.AuthorityOrg.Builder proto = Auth.AuthorityOrg.newBuilder();
        proto.setOrgAuthKey(authKey);
        proto.setOrgAuthValue(authValue);
        proto.setName(name);
        return proto.buildPartial();
    }
}
