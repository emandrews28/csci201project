package model;

import java.sql.Timestamp;

public class Photo {
    private long photoId;
    private long userId;
    private long restaurantId;
    private Long reviewId;
    private String imageUrl;
    private String caption;
    private Timestamp createdAt;
    
    private String restaurantName;
    private String username;

    public Photo() {}

    public Photo(long userId, long restaurantId, Long reviewId, String imageUrl, String caption) {
        this.userId = userId;
        this.restaurantId = restaurantId;
        this.reviewId = reviewId;
        this.imageUrl = imageUrl;
        this.caption = caption;
    }

    public long getPhotoId() { return photoId; }
    public void setPhotoId(long photoId) { this.photoId = photoId; }
    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }
    public long getRestaurantId() { return restaurantId; }
    public void setRestaurantId(long restaurantId) { this.restaurantId = restaurantId; }
    public Long getReviewId() { return reviewId; }
    public void setReviewId(Long reviewId) { this.reviewId = reviewId; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    
    public String getRestaurantName() { return restaurantName; }
    public void setRestaurantName(String restaurantName) { this.restaurantName = restaurantName; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
}
