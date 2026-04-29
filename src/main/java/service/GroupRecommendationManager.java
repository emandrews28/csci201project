package service;

import dao.GroupRecommendationDAO;
import dao.RestaurantDAO;
import model.RankingEntry;
import model.Restaurant;

import java.util.*;
import java.util.stream.Collectors;

public class GroupRecommendationManager {

    private final GroupRecommendationDAO dao;
    private final RestaurantDAO restaurantDAO = new RestaurantDAO();

    // default constructor preserves existing behavior
    public GroupRecommendationManager() {
        this.dao = new GroupRecommendationDAO();
    }

    // testable constructor
    public GroupRecommendationManager(GroupRecommendationDAO dao) {
        this.dao = dao;
    }

    // returns candidate restaurants (union of all ranked restaurants by members)
    public List<Restaurant> getCandidateRestaurants(long groupId) {
        List<Long> members = dao.getGroupMembers(groupId);
        Set<Long> candidateIds = new HashSet<>();
        for (Long member : members) {
            List<RankingEntry> ranks = dao.getRankingsForUser(member);
            for (RankingEntry r : ranks) candidateIds.add(r.getRestaurantId());
        }
        List<Long> ids = new ArrayList<>(candidateIds);
        Collections.sort(ids);
        return restaurantDAO.findByIds(ids);
    }

    // compute average scores using normalized ranking vectors (1.0 top, decreasing)
    public Map<Long, AggregateResult> calculateAverageScores(long groupId) {
        List<Long> members = dao.getGroupMembers(groupId);
        Map<Long, List<Double>> candidateScores = new HashMap<>();

        for (Long member : members) {
            List<RankingEntry> ranks = dao.getRankingsForUser(member);
            if (ranks == null || ranks.isEmpty()) continue;
            // normalize: top rank (position 1) -> score 1.0, position N -> score = (N - pos + 1)/N
            int n = ranks.size();
            for (RankingEntry entry : ranks) {
                double score = ((double)(n - entry.getRankPosition() + 1)) / (double)n;
                candidateScores.computeIfAbsent(entry.getRestaurantId(), k -> new ArrayList<>()).add(score);
            }
        }

        Map<Long, AggregateResult> results = new HashMap<>();
        for (Map.Entry<Long, List<Double>> e : candidateScores.entrySet()) {
            List<Double> scores = e.getValue();
            double avg = scores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            results.put(e.getKey(), new AggregateResult(e.getKey(), avg, scores.size()));
        }
        return results;
    }

    public List<Restaurant> getTopThree(long groupId) {
        Map<Long, AggregateResult> agg = calculateAverageScores(groupId);
        if (agg.isEmpty()) return List.of();

        List<AggregateResult> list = new ArrayList<>(agg.values());
        list.sort((a, b) -> {
            int cmp = Double.compare(b.avgScore, a.avgScore);
            if (cmp != 0) return cmp;
            cmp = Integer.compare(b.contributorCount, a.contributorCount);
            if (cmp != 0) return cmp;
            return Long.compare(a.restaurantId, b.restaurantId);
        });

        List<Long> topIds = list.stream().limit(3).map(ar -> ar.restaurantId).collect(Collectors.toList());
        return restaurantDAO.findByIds(topIds);
    }

    public static class AggregateResult {
        public long restaurantId;
        public double avgScore;
        public int contributorCount;

        public AggregateResult(long restaurantId, double avgScore, int contributorCount) {
            this.restaurantId = restaurantId;
            this.avgScore = avgScore;
            this.contributorCount = contributorCount;
        }
    }
}