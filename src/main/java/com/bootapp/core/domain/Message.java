package com.bootapp.core.domain;

import com.bootapp.grpc.core.CoreCommon;
import com.google.protobuf.StringValue;

import javax.persistence.*;
import java.io.Serializable;

/**
 * A Message.
 */
@Entity
@Table(name = "messages")
public class Message extends AbstractEntity {
    @Id
    private long id;

    @Column(name = "type")
    private int type;

    @Column(name = "receive_type")
    private int receiveType;

    @Column(name = "org_id")
    private long orgId;

    @Column(name = "user_id")
    private long userId;

    @Column(name = "send_to")
    private long sendTo;

    @Column(name = "title")
    private String title;

    @Lob
    @Column(name = "content")
    private String content;

    @Column(name = "file_urls")
    private String fileUrls;

    @Column(name = "img_urls")
    private String imgUrls;

    public void fromProto(CoreCommon.MessageEdit proto) {
        if (proto.getType() != CoreCommon.MessageType.MESSAGE_TYPE_NULL) type = proto.getTypeValue();
        if (proto.getReceiveType() != CoreCommon.ReceiveType.RECEIVE_TYPE_NULL) receiveType = proto.getReceiveTypeValue();

        if (proto.getUserId() != 0L) userId = proto.getUserId();
        if (proto.getOrgId() != 0L) orgId = proto.getOrgId();
        if (proto.getTo() != 0L) sendTo = proto.getTo();
        if (proto.hasTitle()) title = proto.getTitle().getValue();
        if (proto.hasContent()) content = proto.getContent().getValue();
        if (proto.hasFileUrls()) fileUrls = proto.getFileUrls().getValue();
        if (proto.hasImgUrls()) imgUrls = proto.getImgUrls().getValue();
        if (proto.getStatus() != CoreCommon.EntityStatus.ENTITY_STATUS_NULL) status = proto.getStatusValue();
    }

    public CoreCommon.Message toProto() {
        return toProtoBuilder().build();
    }
    public CoreCommon.Message.Builder toProtoBuilder() {
        CoreCommon.Message.Builder msgResp = CoreCommon.Message.newBuilder();
        msgResp.setId(id);
        msgResp.setUserId(userId);
        msgResp.setOrgId(orgId);
        msgResp.setTypeValue(type);
        msgResp.setReceiveTypeValue(receiveType);
        msgResp.setTo(sendTo);
        if (title != null) msgResp.setTitle(title);
        if (content != null) msgResp.setContent(content);
        if (fileUrls != null) msgResp.setFileUrls(fileUrls);
        if (imgUrls != null) msgResp.setImgUrls(imgUrls);
        msgResp.setCreatedAt(createdAt);
        msgResp.setUpdatedAt(updatedAt);
        msgResp.setStatusValue(status);
        return msgResp;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public long getUserId() {
        return userId;
    }

    public void setOrgId(long orgId) {
        this.orgId = orgId;
    }

    public long getOrgId() {
        return orgId;
    }
}
