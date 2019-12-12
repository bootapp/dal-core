package com.bootapp.core.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "id_generators")
public class IDGen {
    @Id
    long id;

    @Column(name = "updated_at")
    long updatedAt;

    public IDGen() {
        this.id = 0L;
        this.updatedAt = 0L;
    }
    public IDGen(long id, long updateAt) {
        this.id = id;
        this.updatedAt = updateAt;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}
