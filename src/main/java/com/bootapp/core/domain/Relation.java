package com.bootapp.core.domain;

import com.bootapp.core.grpc.CoreCommon;
import com.bootapp.core.grpc.DalUser;
import com.google.protobuf.StringValue;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "relations")
public class Relation extends AbstractEntity {
    @Id
    long id;
    @Column(name = "source_id")
    long sourceId;
    @Column(name = "target_id")
    long targetId;
    @Column(name = "relation_type")
    int relationType;
    @Column(name = "attached_id1")
    long attachedId1;
    @Column(name = "attached_id2")
    long attachedId2;
    @Column(name = "value1")
    String value1;
    @Column(name = "value2")
    String value2;

    public void fromProto(CoreCommon.Relation req) {
        if (req.getId() != 0L) id = req.getId();
        if (req.getSourceId() != 0L) sourceId = req.getSourceId();
        if (req.getTargetId() != 0L) targetId = req.getTargetId();
        if (req.getType() != CoreCommon.RelationType.RELATION_NULL)
            relationType = req.getTypeValue();
        if (req.getAttachedId1() != 0L) attachedId1 = req.getAttachedId1();
        if (req.getAttachedId2() != 0L) attachedId2 = req.getAttachedId2();
        if (req.hasValue1()) value1 = req.getValue1().getValue();
        if (req.hasValue2()) value2 = req.getValue2().getValue();
        if (req.getStatus() != CoreCommon.EntityStatus.ENTITY_STATUS_NULL)
            status = req.getStatusValue();
    }

    public CoreCommon.Relation toProto() {
        CoreCommon.Relation.Builder builder = CoreCommon.Relation.newBuilder();
        builder.setId(id);
        builder.setSourceId(sourceId);
        builder.setTargetId(targetId);
        builder.setTypeValue(relationType);
        builder.setAttachedId1(attachedId1);
        builder.setAttachedId2(attachedId2);
        if (value1 != null) builder.setValue1(StringValue.of(value1));
        if (value2 != null) builder.setValue2(StringValue.of(value2));
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

    public long getSourceId() {
        return sourceId;
    }

    public void setSourceId(long sourceId) {
        this.sourceId = sourceId;
    }

    public long getTargetId() {
        return targetId;
    }

    public void setTargetId(long targetId) {
        this.targetId = targetId;
    }

    public int getRelationType() {
        return relationType;
    }

    public void setRelationType(int relationType) {
        this.relationType = relationType;
    }

    public long getAttachedId1() {
        return attachedId1;
    }

    public void setAttachedId1(long attachedId1) {
        this.attachedId1 = attachedId1;
    }

    public long getAttachedId2() {
        return attachedId2;
    }

    public void setAttachedId2(long attachedId2) {
        this.attachedId2 = attachedId2;
    }

    public String getValue1() {
        return value1;
    }

    public void setValue1(String value1) {
        this.value1 = value1;
    }

    public String getValue2() {
        return value2;
    }

    public void setValue2(String value2) {
        this.value2 = value2;
    }
}
