package com.bootapp.core.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.bootapp.core.grpc.CoreCommon;

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

    public void fromProto(CoreCommon.User item) {
        if (item.getId() != 0L) id = item.getId();
        if (item.getStatusValue() != 0) status = item.getStatusValue();
        if (item.getPhone() != null && !item.getPhone().equals("")) phone = item.getPhone();
        if (item.getEmail() != null && !item.getEmail().equals("")) email = item.getEmail();
        if (item.getUsername() != null && !item.getUsername().equals("")) username = item.getUsername();
        if (item.getCreateAt() != 0L) createAt = item.getCreateAt();
        if (item.getUpdateAt() != 0L) updateAt = item.getUpdateAt();
    }
    public CoreCommon.User toProto() {
        CoreCommon.User.Builder builder = CoreCommon.User.newBuilder();
        if (this.getId() != 0L) builder.setId(this.getId());
        if (this.status != 0) builder.setStatusValue(this.status);
        if (this.phone != null && !this.phone.equals("")) builder.setPhone(this.phone);
        if (this.email != null && !this.email.equals("")) builder.setEmail(this.email);
        if (this.username != null && !this.username.equals("")) builder.setUsername(this.username);
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
}
