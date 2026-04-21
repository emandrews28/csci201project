package model;

import java.sql.Timestamp;

public class Review {
    private long reviewId;
    private long userId;
    private long restaurantId;
    private int rankingScore;
    private String reviewText;
    private Timestamp timestamp;

    public Review() {}

    public Review(long userId, long restaurantId, int rankingScore, String reviewText) {
        this.userId = userId;
        this.restaurantId = restaurantId;
        this.rankingScore = rankingScore;
        this.reviewText = reviewText;
    }

    public long getReviewId() { return reviewId; }
    public void setReviewId(long reviewId) { this.reviewId = reviewId; }
    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }
    public long getRestaurantId() { return restaurantId; }
    public void setRestaurantId(long restaurantId) { this.restaurantId = restaurantId; }
    public int getRankingScore() { return rankingScore; }
    public void setRankingScore(int rankingScore) { this.rankingScore = rankingScore; }
    public String getReviewText() { return reviewText; }
    public void setReviewText(String reviewText) { this.reviewText = reviewText; }
    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
}