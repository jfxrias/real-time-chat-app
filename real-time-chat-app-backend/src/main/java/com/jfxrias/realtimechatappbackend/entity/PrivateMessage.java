package com.jfxrias.realtimechatappbackend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "private_message")
public class PrivateMessage {
    @Id
    @Column(name = "message_uuid", length = 36)
    private String messageUuid;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_uuid", insertable = false, updatable = false)
    private Message message;

    @Column(name = "receiver_uuid", length = 36, nullable = false)
    private String receiverUuid;

    @Column(name = "is_deleted_by_receiver", nullable = false)
    private Boolean isDeletedByReceiver = false;

    public PrivateMessage() {
    }

    public PrivateMessage(String messageUuid, String receiverUuid) {
        this.messageUuid = messageUuid;
        this.receiverUuid = receiverUuid;
    }

    public String getMessageUuid() { return messageUuid; }
    public void setMessageUuid(String messageUuid) { this.messageUuid = messageUuid; }
    public Message getMessage() { return message; }
    public void setMessage(Message message) { this.message = message; }
    public String getReceiverUuid() { return receiverUuid; }
    public void setReceiverUuid(String receiverUuid) { this.receiverUuid = receiverUuid; }
    public Boolean getIsDeletedByReceiver() { return isDeletedByReceiver; }
    public void setIsDeletedByReceiver(Boolean isDeletedByReceiver) { this.isDeletedByReceiver = isDeletedByReceiver; }
}
