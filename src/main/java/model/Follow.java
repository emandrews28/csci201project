package model;

import java.sql.Timestamp;

// Represents a row in the follows table — who is following whom and when
public class Follow {
    private long followerId;   // the user doing the following
    private long followingId;  // the user being followed
    private Timestamp createdAt;

    public long getFollowerId() { return followerId; }
    public void setFollowerId(long followerId) { this.followerId = followerId; }

    public long getFollowingId() { return followingId; }
    public void setFollowingId(long followingId) { this.followingId = followingId; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}