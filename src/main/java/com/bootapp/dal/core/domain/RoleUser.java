package com.bootapp.dal.core.domain;

import javax.persistence.*;

@Entity
@Table(name = "roles_user")
public class RoleUser extends AbstractEntity {
    @Id
    @Column(nullable = false, updatable = false)
    long id;

    @Column(name = "org_id", nullable = false, updatable = false)
    long orgId;

    @Column(length = 50)
    String name;

    @Column
    String remark;

    @Lob
    @Column(columnDefinition = "text")
    String authorities;
}
