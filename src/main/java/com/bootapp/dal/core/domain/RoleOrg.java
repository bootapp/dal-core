package com.bootapp.dal.core.domain;

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
    @Column(columnDefinition = "text")
    String authorities;
}
