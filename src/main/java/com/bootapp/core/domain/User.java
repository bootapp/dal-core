package com.bootapp.core.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.bootapp.core.grpc.CoreCommon;
import com.google.protobuf.StringValue;

@Entity
@Table(name = "users")
public class User extends AbstractEntity {
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    long id;

    @Column(name = "username", length = 32, unique = true)
    String username;

    @Column(name = "phone", length = 20, unique = true)
    String phone;

    @Column(name = "email", unique = true)
    String email;

    @Column(name = "password_hash", length = 60)
    String passwordHash;

    @Column(name = "name", length = 32)
    String name;

    public void fromProto(CoreCommon.User item) {
        if (item.getId() != 0L) id = item.getId();
        if (item.getStatusValue() != 0) status = item.getStatusValue();
        if (item.hasPhone()) phone = item.getPhone().getValue();
        if (item.hasEmail()) email = item.getEmail().getValue();
        if (item.hasUsername()) username = item.getUsername().getValue();
        if (item.hasName()) name = item.getName().getValue();
    }
    public CoreCommon.User toProto() {
        CoreCommon.User.Builder builder = CoreCommon.User.newBuilder();
        if (this.getId() != 0L) builder.setId(this.getId());
        if (this.status != 0) builder.setStatusValue(this.status);
        if (this.phone != null && !this.phone.equals("")) builder.setPhone(StringValue.of(this.phone));
        if (this.email != null && !this.email.equals("")) builder.setEmail(StringValue.of(this.email));
        if (this.username != null && !this.username.equals("")) builder.setUsername(StringValue.of(this.username));
        if (this.name != null && !this.name.equals("")) builder.setName(StringValue.of(this.name));
        builder.setCreateAt(this.createAt);
        builder.setUpdateAt(this.updateAt);
        return builder.buildPartial();
    }
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
