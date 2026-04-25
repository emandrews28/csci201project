package model;

import java.sql.Timestamp;

public class RankingEntry {
    private long rankingId;
    private long userId;
    private long restaurantId;
    private int rankPosition;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    private String restaurantName;
    private String restaurantAddress;
    private String cuisine;
    private Integer priceTier;
    
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
    
    
    public String getRestaurantName() {
        return restaurantName;
    }

    public void setRestaurantName(String restaurantName) {
        this.restaurantName = restaurantName;
    }

    public String getRestaurantAddress() {
        return restaurantAddress;
    }

    public void setRestaurantAddress(String restaurantAddress) {
        this.restaurantAddress = restaurantAddress;
    }

    public String getCuisine() {
        return cuisine;
    }

    public void setCuisine(String cuisine) {
        this.cuisine = cuisine;
    }

    public Integer getPriceTier() {
        return priceTier;
    }

    public void setPriceTier(Integer priceTier) {
        this.priceTier = priceTier;
    }
}