package model;

import java.util.ArrayList;
import java.util.List;

public class MapCluster {
    private double latitude;
    private double longitude;
    private int count;
    private double avgRankScore;
    private final List<Long> restaurantIds = new ArrayList<>();

    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public int getCount() { return count; }
    public double getAvgRankScore() { return avgRankScore; }
    public List<Long> getRestaurantIds() { return restaurantIds; }

    public void addRestaurant(MapResult restaurant) {
        if (count == 0) {
            latitude = restaurant.getLatitude();
            longitude = restaurant.getLongitude();
        } else {
            latitude = (latitude * count + restaurant.getLatitude()) / (count + 1);
            longitude = (longitude * count + restaurant.getLongitude()) / (count + 1);
        }
        avgRankScore = (avgRankScore * count + restaurant.getRankScore()) / (count + 1);
        count++;
        restaurantIds.add(restaurant.getRestaurantId());
    }
}
