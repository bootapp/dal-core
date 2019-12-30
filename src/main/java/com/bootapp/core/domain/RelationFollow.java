package com.bootapp.core.domain;

import com.bootapp.grpc.core.CoreCommon;

import javax.persistence.*;

/**
 * A RelationFollow.
 */
@Entity
@Table(name = "relation_follows")
public class RelationFollow {

    @Id
    private long id;

    @Column(name = "user_id")
    private long userId;

    @Column(name = "to_user_id")
    private long toUserId;

    @Column(name = "updated_at")
    private long updatedAt;

    public CoreCommon.SimpleRelation toProto() {
        CoreCommon.SimpleRelation.Builder builder = CoreCommon.SimpleRelation.newBuilder();
        builder.setId(id);
        builder.setFromId(userId);
        builder.setToId(toUserId);
        builder.setUpdatedAt(updatedAt);
        return builder.build();
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public void setToUserId(long toUserId) {
        this.toUserId = toUserId;
    }

    @PrePersist
    @PreUpdate
    void preUpdate() {
        this.updatedAt = System.currentTimeMillis();
    }
}
