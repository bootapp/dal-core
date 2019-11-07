package com.bootapp.core.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "department_users")
public class DepartmentUser extends AbstractEntity {
    @Id
    long id;
    @Column(name = "dept_id")
    long deptId;
    @Column(name = "user_id")
    long userId;
    @Column(name = "position")
    String position;
}
