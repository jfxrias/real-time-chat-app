package com.jfxrias.realtimechatappbackend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "message")
public class Message {
    @Id
    @Column(name = "message_uuid", length = 36, nullable = false, updatable = false)
    private String messageUuid = UUID.randomUUID().toString();

    @Column(name = "sender_uuid", length = 36, nullable = false, updatable = false)
    private String senderUuid;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "sent_at", nullable = false, updatable = false)
    private LocalDateTime sentAt = LocalDateTime.now();

    @Column(name = "edited_at")
    private LocalDateTime editedAt;

    @Column(name = "is_deleted_by_sender", nullable = false)
    private Boolean isDeletedBySender = false;

    public Message() {
    }

    public Message(String senderUuid, String content) {
        this.senderUuid = senderUuid;
        this.content = content;
    }

    public String getMessageUuid() { return messageUuid; }
    public void setMessageUuid(String messageUuid) { this.messageUuid = messageUuid; }
    public String getSenderUuid() { return senderUuid; }
    public void setSenderUuid(String senderUuid) { this.senderUuid = senderUuid; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
    public LocalDateTime getEditedAt() { return editedAt; }
    public void setEditedAt(LocalDateTime editedAt) { this.editedAt = editedAt; }
    public Boolean getIsDeletedBySender() { return isDeletedBySender; }
    public void setIsDeletedBySender(Boolean isDeletedBySender) { this.isDeletedBySender = isDeletedBySender; }
}
