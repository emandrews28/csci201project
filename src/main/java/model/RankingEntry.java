package model;

import java.sql.Timestamp;

public class RankingEntry {
    private long rankingId;
    private long userId;
    private long restaurantId;
    private int rankPosition;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public RankingEntry() {
    }

    public RankingEntry(long userId, long restaurantId, int rankPosition) {
        this.userId = userId;
        this.restaurantId = restaurantId;
        this.rankPosition = rankPosition;
    }

    public long getRankingId() {
        return rankingId;
    }

    public void setRankingId(long rankingId) {
        this.rankingId = rankingId;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public long getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(long restaurantId) {
        this.restaurantId = restaurantId;
    }

    public int getRankPosition() {
        return rankPosition;
    }

    public void setRankPosition(int rankPosition) {
        this.rankPosition = rankPosition;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }
}