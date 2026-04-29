package test;

import model.RankingEntry;
import dao.GroupRecommendationDAO;
import service.GroupRecommendationManager;

import java.util.*;

public class TestGroupRecommendationRunner {
    public static void main(String[] args) {
        // Create fake DAO via anonymous subclass
        GroupRecommendationDAO fakeDao = new GroupRecommendationDAO() {
            private Map<Long, List<Long>> groups = new HashMap<>() {{ put(1L, Arrays.asList(10L, 11L, 12L)); }};
            private Map<Long, List<RankingEntry>> ranks = new HashMap<>() {{
                put(10L, Arrays.asList(new RankingEntry(10L, 100L, 1), new RankingEntry(10L, 101L, 2)));
                put(11L, Arrays.asList(new RankingEntry(11L, 101L, 1), new RankingEntry(11L, 102L, 2)));
                put(12L, Arrays.asList(new RankingEntry(12L, 102L, 1), new RankingEntry(12L, 100L, 2)));
            }};

            @Override
            public List<Long> getGroupMembers(long groupId) { return groups.getOrDefault(groupId, List.of()); }

            @Override
            public List<RankingEntry> getRankingsForUser(long userId) { return ranks.getOrDefault(userId, List.of()); }
        };

        GroupRecommendationManager mgr = new GroupRecommendationManager(fakeDao);
        var agg = mgr.calculateAverageScores(1L);
        System.out.println("Aggregate results:");
        List<service.GroupRecommendationManager.AggregateResult> sorted = new ArrayList<>(agg.values());
        sorted.sort((a,b) -> {
            int cmp = Double.compare(b.avgScore, a.avgScore);
            if (cmp!=0) return cmp;
            cmp = Integer.compare(b.contributorCount, a.contributorCount);
            if (cmp!=0) return cmp;
            return Long.compare(a.restaurantId, b.restaurantId);
        });
        sorted.forEach(a -> System.out.println("restaurant=" + a.restaurantId + " avg=" + a.avgScore + " contributors=" + a.contributorCount));

        System.out.println("Top 3 restaurant ids:");
        sorted.stream().limit(3).forEach(r -> System.out.println(r.restaurantId));
    }
}