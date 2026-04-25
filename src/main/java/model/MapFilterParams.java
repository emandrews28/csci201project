package model;

import java.util.ArrayList;
import java.util.List;

public class MapFilterParams {
    private Double userLat;
    private Double userLng;
    private Double radiusMiles;
    private List<Integer> cuisineIds = new ArrayList<>();
    private List<Integer> priceTiers = new ArrayList<>();
    private Double minRank;
    private Boolean openNow;
    private BoundingBox bounds;

    public Double getUserLat() { return userLat; }
    public void setUserLat(Double userLat) { this.userLat = userLat; }
    public Double getUserLng() { return userLng; }
    public void setUserLng(Double userLng) { this.userLng = userLng; }
    public Double getRadiusMiles() { return radiusMiles; }
    public void setRadiusMiles(Double radiusMiles) { this.radiusMiles = radiusMiles; }
    public List<Integer> getCuisineIds() { return cuisineIds; }
    public void setCuisineIds(List<Integer> cuisineIds) { this.cuisineIds = cuisineIds == null ? new ArrayList<>() : cuisineIds; }
    public List<Integer> getPriceTiers() { return priceTiers; }
    public void setPriceTiers(List<Integer> priceTiers) { this.priceTiers = priceTiers == null ? new ArrayList<>() : priceTiers; }
    public Double getMinRank() { return minRank; }
    public void setMinRank(Double minRank) { this.minRank = minRank; }
    public Boolean getOpenNow() { return openNow; }
    public void setOpenNow(Boolean openNow) { this.openNow = openNow; }
    public BoundingBox getBounds() { return bounds; }
    public void setBounds(BoundingBox bounds) { this.bounds = bounds; }

    public boolean validate() {
        if (userLat == null || userLng == null) return false;
        if (userLat < -90 || userLat > 90) return false;
        if (userLng < -180 || userLng > 180) return false;
        if (radiusMiles != null && (radiusMiles < 1 || radiusMiles > 25)) return false;
        if (minRank != null && minRank < 0) return false;
        if (bounds != null && bounds.getNorth() < bounds.getSouth()) return false;
        return true;
    }
}
