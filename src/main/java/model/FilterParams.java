package model;

import java.util.List;

/**
 * Value object that holds all parameters for a restaurant search/filter request.
 * Populated by RestaurantSearchServlet from HTTP query params.
 */
public class FilterParams {
    private String query;           // free-text search on restaurant name
    private List<Long> cuisineIds;  // multi-select cuisine filter
    private List<Integer> priceTiers; // 1–4, multi-select
    private Double userLat;         // null if location not provided
    private Double userLng;
    private Double radiusMiles;     // null means no distance filter
    private boolean friendsOnly;    // restrict to restaurants friends have reviewed
    private String sortBy;          // "top_rated" | "distance" | "newest"
    private int page;               // 0-indexed
    private int limit;              // results per page, default 20

    public FilterParams() {
        this.sortBy = "top_rated";
        this.page = 0;
        this.limit = 20;
    }

    public String getQuery()                    { return query; }
    public void setQuery(String v)              { this.query = v; }

    public List<Long> getCuisineIds()           { return cuisineIds; }
    public void setCuisineIds(List<Long> v)     { this.cuisineIds = v; }

    public List<Integer> getPriceTiers()        { return priceTiers; }
    public void setPriceTiers(List<Integer> v)  { this.priceTiers = v; }

    public Double getUserLat()                  { return userLat; }
    public void setUserLat(Double v)            { this.userLat = v; }

    public Double getUserLng()                  { return userLng; }
    public void setUserLng(Double v)            { this.userLng = v; }

    public Double getRadiusMiles()              { return radiusMiles; }
    public void setRadiusMiles(Double v)        { this.radiusMiles = v; }

    public boolean isFriendsOnly()              { return friendsOnly; }
    public void setFriendsOnly(boolean v)       { this.friendsOnly = v; }

    public String getSortBy()                   { return sortBy; }
    public void setSortBy(String v)             { this.sortBy = v; }

    public int getPage()                        { return page; }
    public void setPage(int v)                  { this.page = v; }

    public int getLimit()                       { return limit; }
    public void setLimit(int v)                 { this.limit = v; }

    public boolean hasLocation() {
        return userLat != null && userLng != null;
    }

    public boolean hasDistanceFilter() {
        return hasLocation() && radiusMiles != null && radiusMiles > 0;
    }
}
