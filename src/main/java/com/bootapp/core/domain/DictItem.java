package com.bootapp.core.domain;

import com.bootapp.core.grpc.CoreCommon;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "dict_items")
public class DictItem extends AbstractEntity {
    @Id
    long id;
    @Column
    long pid;
    @Column
    long seq;
    @Column
    String name;
    @Column
    String code;
    @Column
    String description;
    @Column
    String content;
    @Column
    String remark;

    public void setId(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public void fromProto(CoreCommon.DictItemEdit proto) {
        if (proto.getPid() != 0L) pid = proto.getPid();
        if (proto.getSeq() != 0L) seq = proto.getSeq();
        if (proto.hasName()) name = proto.getName().getValue();
        if (proto.hasCode()) code = proto.getCode().getValue();
        if (proto.hasDescription()) description = proto.getDescription().getValue();
        if (proto.hasContent()) content = proto.getContent().getValue();
        if (proto.hasRemark()) remark = proto.getRemark().getValue();
    }
    public CoreCommon.DictItem toProto() {
        return toProtoBuilder().build();
    }
    public CoreCommon.DictItem.Builder toProtoBuilder() {
        CoreCommon.DictItem.Builder builder = CoreCommon.DictItem.newBuilder();
        builder.setId(id);
        builder.setPid(pid);
        builder.setSeq(seq);
        builder.setName(name);
        builder.setCode(code);
        builder.setDescription(description);
        builder.setContent(content);
        builder.setRemark(remark);
        builder.setUpdatedAt(updatedAt);
        builder.setCreatedAt(createdAt);
        return builder;
    }
}