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

    @Column(name = "update_at")
    long updateAt;

    public IDGen() {
        this.id = 0L;
        this.updateAt = 0L;
    }
    public IDGen(long id, long updateAt) {
        this.id = id;
        this.updateAt = updateAt;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getUpdateAt() {
        return updateAt;
    }

    public void setUpdateAt(long updateAt) {
        this.updateAt = updateAt;
    }
}
