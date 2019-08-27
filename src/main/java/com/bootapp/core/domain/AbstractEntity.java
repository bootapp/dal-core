package com.bootapp.core.domain;

import javax.persistence.*;

@MappedSuperclass
public abstract class AbstractEntity {
    @Column
    int status;

    @Column(name = "create_at")
    long createAt;

    @Column(name = "update_at")
    long updateAt;

    @PrePersist
    void preInsert() {
        if (this.createAt == 0L) {
            this.createAt = this.updateAt = System.currentTimeMillis();
        }
    }

    // hook for pre-update:
    @PreUpdate
    void preUpdate() {
        this.updateAt = System.currentTimeMillis();
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public long getCreateAt() {
        return createAt;
    }

    public void setCreateAt(long createAt) {
        this.createAt = createAt;
    }

    public long getUpdateAt() {
        return updateAt;
    }

    public void setUpdateAt(long updateAt) {
        this.updateAt = updateAt;
    }
}
