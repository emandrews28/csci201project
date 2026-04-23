package model;

public class MapResult {
    private long restaurantId;
    private String name;
    private double latitude;
    private double longitude;
    private double rankScore;
    private double distanceMiles;
    private String cuisine;
    private Integer priceTier;
    private String address;
    private Boolean openNow;

    public long getRestaurantId() { return restaurantId; }
    public void setRestaurantId(long restaurantId) { this.restaurantId = restaurantId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public double getRankScore() { return rankScore; }
    public void setRankScore(double rankScore) { this.rankScore = rankScore; }
    public double getDistanceMiles() { return distanceMiles; }
    public void setDistanceMiles(double distanceMiles) { this.distanceMiles = distanceMiles; }
    public String getCuisine() { return cuisine; }
    public void setCuisine(String cuisine) { this.cuisine = cuisine; }
    public Integer getPriceTier() { return priceTier; }
    public void setPriceTier(Integer priceTier) { this.priceTier = priceTier; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public Boolean getOpenNow() { return openNow; }
    public void setOpenNow(Boolean openNow) { this.openNow = openNow; }

    public MapMarker toMarker() {
        MapMarker marker = new MapMarker();
        marker.setRestaurantId(restaurantId);
        marker.setName(name);
        marker.setLatitude(latitude);
        marker.setLongitude(longitude);
        marker.setRankScore(rankScore);
        marker.setDistanceMiles(distanceMiles);
        marker.setCuisine(cuisine);
        marker.setPriceTier(priceTier);
        marker.setAddress(address);
        marker.setOpenNow(openNow);
        return marker;
    }
}
