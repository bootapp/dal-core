package com.bootapp.core.domain;

import com.bootapp.grpc.core.CoreCommon;

import javax.persistence.*;

/**
 * A Inbox.
 */
@Entity
@Table(name = "feedback")
public class Feedback extends AbstractEntity {
    @Id
    private long id;

    @Column(name = "user_id")
    private long userId;

    @Column(name = "type_id")
    private long typeId;

    @Column(name = "title")
    private String title;

    @Lob
    @Column(name = "content")
    private String content;

    @Column(name = "img_urls")
    private String imgUrls;

    @Column(name = "reply")
    private String reply;

    public void setId(long id) {
        this.id = id;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public void fromProto(CoreCommon.FeedbackEdit item) {
        if (item.getUserId() != 0L) userId = item.getUserId();
        if (item.getTypeId() != 0L) typeId = item.getTypeId();
        if (item.hasTitle()) title = item.getTitle().getValue();
        if (item.hasContent()) content = item.getContent().getValue();
        if (item.hasImgUrls()) imgUrls = item.getImgUrls().getValue();
        if (item.getStatus() != CoreCommon.EntityStatus.ENTITY_STATUS_NULL) status = item.getStatusValue();
        if (item.hasReply()) reply = item.getReply().getValue();
    }

    public CoreCommon.Feedback toProto() {
        CoreCommon.Feedback.Builder builder = CoreCommon.Feedback.newBuilder();
        builder.setId(id);
        builder.setUserId(userId);
        builder.setTypeId(typeId);
        if (title != null) builder.setTitle(title);
        if (content != null) builder.setContent(content);
        if (imgUrls != null) builder.setImgUrls(imgUrls);
        if (reply != null) builder.setReply(reply);
        builder.setStatusValue(status);
        builder.setCreatedAt(createdAt);
        builder.setUpdatedAt(updatedAt);
        return builder.build();
    }

}
