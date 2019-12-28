package com.bootapp.core.domain;

import com.bootapp.core.grpc.CoreCommon;

import javax.persistence.*;

/**
 * A RelationBlacklist.
 */
@Entity
@Table(name = "relation_blacklist")
public class RelationBlacklist {

    @Id
    private long id;

    @Column(name = "user_id")
    private long userId;

    @Column(name = "blocked_user_id")
    private long blockedUserId;

    @Column(name = "updated_at")
    private long updatedAt;

    public CoreCommon.SimpleRelation toProto() {
        CoreCommon.SimpleRelation.Builder builder = CoreCommon.SimpleRelation.newBuilder();
        builder.setId(id);
        builder.setFromId(userId);
        builder.setToId(blockedUserId);
        builder.setUpdatedAt(updatedAt);
        return builder.build();
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public void setBlockedUserId(long blockedUserId) {
        this.blockedUserId = blockedUserId;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = System.currentTimeMillis();
    }
}
