package com.bootapp.dal.core.domain;

import com.bootapp.dal.core.grpc.Auth;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "authorities_user")
public class AuthorityUser extends AbstractEntity {
    @Id
    @Column(name = "auth_key", nullable = false, updatable = false, length = 50)
    String authKey;

    @Column(name = "auth_value")
    long authValue;

    @Column(name = "org_auth_key", nullable = false, length = 50)
    String orgAuthKey;

    @Column(length = 50)
    String name;

    public Auth.AuthorityUser toProto() {
        Auth.AuthorityUser.Builder proto = Auth.AuthorityUser.newBuilder();
        proto.setOrgAuthKey(orgAuthKey);
        proto.setUserAuthKey(authKey);
        proto.setUserAuthValue(authValue);
        proto.setName(name);
        return proto.buildPartial();
    }
}
