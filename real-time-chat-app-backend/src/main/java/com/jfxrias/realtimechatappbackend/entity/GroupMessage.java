package com.jfxrias.realtimechatappbackend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "group_message")
public class GroupMessage {
    @Id
    @Column(name = "message_uuid", length = 36)
    private String messageUuid;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_uuid", insertable = false, updatable = false)
    private Message message;

    @Column(name = "target_group_uuid", length = 36, nullable = false)
    private String targetGroupUuid;

    public GroupMessage() {
    }

    public GroupMessage(String messageUuid, String targetGroupUuid) {
        this.messageUuid = messageUuid;
        this.targetGroupUuid = targetGroupUuid;
    }

    public String getMessageUuid() { return messageUuid; }
    public void setMessageUuid(String messageUuid) { this.messageUuid = messageUuid; }
    public Message getMessage() { return message; }
    public void setMessage(Message message) { this.message = message; }
    public String getTargetGroupUuid() { return targetGroupUuid; }
    public void setTargetGroupUuid(String targetGroupUuid) { this.targetGroupUuid = targetGroupUuid; }
}
