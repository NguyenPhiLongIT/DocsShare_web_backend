package com.docsshare_web_backend.follow.models;

import java.io.Serializable;
import java.util.Objects;

public class FollowId implements Serializable {
    private Long follower;
    private Long following;

    public FollowId() {}

    public FollowId(Long follower, Long following) {
        this.follower = follower;
        this.following = following;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FollowId followId = (FollowId) o;
        return Objects.equals(follower, followId.follower) &&
                Objects.equals(following, followId.following);
    }

    @Override
    public int hashCode() {
        return Objects.hash(follower, following);
    }
}