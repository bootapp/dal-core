package com.bootapp.core.domain;

import javax.persistence.*;

@MappedSuperclass
public abstract class AbstractEntity {
    @Column
    int status = 1;

    @Column(name = "created_at")
    long createdAt;

    @Column(name = "updated_at")
    long updatedAt;

    @Column(name = "created_by")
    long createdBy;

    @Column(name = "last_updated_by")
    long lastUpdatedBy;

    @PrePersist
    void preInsert() {
        if (this.createdAt == 0L) {
            this.createdAt = this.updatedAt = System.currentTimeMillis();
        }
    }

    // hook for pre-update:
    @PreUpdate
    void preUpdate() {
        this.updatedAt = System.currentTimeMillis();
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(long createdBy) {
        this.createdBy = createdBy;
    }

    public long getLastUpdatedBy() {
        return lastUpdatedBy;
    }

    public void setLastUpdatedBy(long lastUpdatedBy) {
        this.lastUpdatedBy = lastUpdatedBy;
    }
}
