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
    long id;
    @Column(name = "pid")
    long pid;
    @Column(name = "org_id")
    long orgId;
    @Column(name = "name")
    String name;
    @Column(name = "description")
    String description;
    @Column(name = "remark")
    String remark;
    public CoreCommon.Department toProto() {
        CoreCommon.Department.Builder builder = CoreCommon.Department.newBuilder();
        builder.setId(id);
        builder.setPid(pid);
        builder.setOrgId(orgId);
        if (name != null) builder.setName(name);
        if (description != null) builder.setDescription(description);
        if (remark != null) builder.setRemark(remark);
        builder.setStatusValue(this.getStatus());
        builder.setCreateAt(this.getCreateAt());
        builder.setUpdateAt(this.getUpdateAt());
        return builder.build();
    }

    public void fromProto(CoreCommon.DepartmentEdit proto) {
        if (proto.hasName()) name = proto.getName().getValue();
        if (proto.getPid() != 0L) pid = proto.getPid();
        if (proto.getOrgId() != 0L) orgId = proto.getOrgId();
        if (proto.hasDescription()) description = proto.getDescription().getValue();
        if (proto.hasRemark()) remark = proto.getRemark().getValue();
        if (proto.getStatus() != CoreCommon.EntityStatus.ENTITY_STATUS_NULL) this.setStatus(proto.getStatusValue());
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
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
