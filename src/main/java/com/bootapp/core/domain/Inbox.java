package com.bootapp.core.domain;

import javax.persistence.*;
import java.io.Serializable;

/**
 * A Inbox.
 */
@Entity
@Table(name = "inbox")
public class Inbox extends AbstractEntity {
    @Id
    private long id;

    @Column(name = "user_id")
    private long userId;

    @Column(name = "msg_id")
    private long msgId;
}
