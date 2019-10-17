package com.bootapp.core.domain;

import com.bootapp.core.grpc.CoreCommon;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "departments")
public class Department extends AbstractEntity {
    @Id
    private long id;

    @Column(name = "pid")
    private long pid;

    @Column(name = "org_id")
    private long orgId;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "remark")
    private String remark;

    public CoreCommon.Department toProto() {
        CoreCommon.Department.Builder builder = CoreCommon.Department.newBuilder();
        builder.setId(id);
        builder.setPid(pid);
        builder.setOrgId(orgId);
        builder.setName(name);
        builder.setDescription(description);
        builder.setRemark(remark);
        builder.setStatusValue(status);
        builder.setCreateAt(createAt);
        builder.setUpdateAt(updateAt);
        return builder.build();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getPid() {
        return pid;
    }

    public void setPid(long pid) {
        this.pid = pid;
    }

    public long getOrgId() {
        return orgId;
    }

    public void setOrgId(long orgId) {
        this.orgId = orgId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
