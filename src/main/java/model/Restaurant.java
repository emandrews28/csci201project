package model;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

// for search & filtering
import model.Cuisine;

public class Restaurant {
    private long restaurantId;
    private String name;
    private String address;
    private Double latitude;
    private Double longitude;
    private Integer priceTier;
    private String cuisine;
    private Boolean openNow;
    private BigDecimal avgRating;
    private Integer reviewCount;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // for search & filtering
    private String googlePlaceId;
    private Double rankScore;
    private Double distanceMiles;
    private List<Cuisine> cuisines;

    public long getRestaurantId() { return restaurantId; }
    public void setRestaurantId(long restaurantId) { this.restaurantId = restaurantId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public Integer getPriceTier() { return priceTier; }
    public void setPriceTier(Integer priceTier) { this.priceTier = priceTier; }
    public String getCuisine() { return cuisine; }
    public void setCuisine(String cuisine) { this.cuisine = cuisine; }
    public Boolean getOpenNow() { return openNow; }
    public void setOpenNow(Boolean openNow) { this.openNow = openNow; }
    public BigDecimal getAvgRating() { return avgRating; }
    public void setAvgRating(BigDecimal avgRating) { this.avgRating = avgRating; }
    public Integer getReviewCount() { return reviewCount; }
    public void setReviewCount(Integer reviewCount) { this.reviewCount = reviewCount; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    // for search & filtering
    public String getGooglePlaceId() { return googlePlaceId; }
    public void setGooglePlaceId(String googlePlaceId) { this.googlePlaceId = googlePlaceId; }
    public Double getRankScore() { return rankScore; }
    public void setRankScore(Double rankScore) { this.rankScore = rankScore; }
    public Double getDistanceMiles() { return distanceMiles; }
    public void setDistanceMiles(Double distanceMiles) { this.distanceMiles = distanceMiles; }
    public List<Cuisine> getCuisines() { return cuisines; }
    public void setCuisines(List<Cuisine> cuisines) { this.cuisines = cuisines; }
}
