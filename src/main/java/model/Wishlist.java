package model;

import java.sql.Timestamp;

public class Wishlist {
    private long wishlistId;
    private long userId;
    private long restaurantId;
    private String notes;
    private Timestamp addedAt;

    private String restaurantName;
    private String restaurantAddress;
    private String cuisine;
    private Integer priceTier;

    public Wishlist() {}

    public Wishlist(long userId, long restaurantId, String notes) {
        this.userId = userId;
        this.restaurantId = restaurantId;
        this.notes = notes;
    }

    public long getWishlistId() { return wishlistId; }
    public void setWishlistId(long wishlistId) { this.wishlistId = wishlistId; }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public long getRestaurantId() { return restaurantId; }
    public void setRestaurantId(long restaurantId) { this.restaurantId = restaurantId; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Timestamp getAddedAt() { return addedAt; }
    public void setAddedAt(Timestamp addedAt) { this.addedAt = addedAt; }

    public String getRestaurantName() { return restaurantName; }
    public void setRestaurantName(String restaurantName) { this.restaurantName = restaurantName; }

    public String getRestaurantAddress() { return restaurantAddress; }
    public void setRestaurantAddress(String restaurantAddress) { this.restaurantAddress = restaurantAddress; }

    public String getCuisine() { return cuisine; }
    public void setCuisine(String cuisine) { this.cuisine = cuisine; }

    public Integer getPriceTier() { return priceTier; }
    public void setPriceTier(Integer priceTier) { this.priceTier = priceTier; }
}
