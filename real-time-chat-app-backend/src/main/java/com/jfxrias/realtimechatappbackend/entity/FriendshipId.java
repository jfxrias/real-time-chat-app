package com.jfxrias.realtimechatappbackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class FriendshipId implements Serializable {
    @Column(name = "first_user_uuid", length = 36)
    private String firstUserUuid;

    @Column(name = "second_user_uuid", length = 36)
    private String secondUserUuid;

    public FriendshipId() {
    }

    public FriendshipId(String firstUserUuid, String secondUserUuid) {
        this.firstUserUuid = firstUserUuid;
        this.secondUserUuid = secondUserUuid;
    }

    public String getFirstUserUuid() {
        return firstUserUuid;
    }

    public void setFirstUserUuid(String firstUserUuid) {
        this.firstUserUuid = firstUserUuid;
    }

    public String getSecondUserUuid() {
        return secondUserUuid;
    }

    public void setSecondUserUuid(String secondUserUuid) {
        this.secondUserUuid = secondUserUuid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FriendshipId that)) return false;
        return Objects.equals(firstUserUuid, that.firstUserUuid) && Objects.equals(secondUserUuid, that.secondUserUuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(firstUserUuid, secondUserUuid);
    }
}
