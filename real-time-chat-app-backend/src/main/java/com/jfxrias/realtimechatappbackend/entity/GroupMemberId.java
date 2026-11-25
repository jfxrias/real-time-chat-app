package com.jfxrias.realtimechatappbackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class GroupMemberId implements Serializable {
    @Column(name = "group_uuid", length = 36)
    private String groupUuid;

    @Column(name = "user_uuid", length = 36)
    private String userUuid;

    public GroupMemberId() {
    }

    public GroupMemberId(String groupUuid, String userUuid) {
        this.groupUuid = groupUuid;
        this.userUuid = userUuid;
    }

    public String getGroupUuid() {
        return groupUuid;
    }

    public void setGroupUuid(String groupUuid) {
        this.groupUuid = groupUuid;
    }

    public String getUserUuid() {
        return userUuid;
    }

    public void setUserUuid(String userUuid) {
        this.userUuid = userUuid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GroupMemberId that)) return false;
        return Objects.equals(groupUuid, that.groupUuid) && Objects.equals(userUuid, that.userUuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupUuid, userUuid);
    }
}
