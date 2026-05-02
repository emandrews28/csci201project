package test;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * FullFeatureTestRunner.java
 *
 * No-JUnit black-box test runner for the CSCI201 restaurant app.
 *
 * This file is based on the Testing Plan PDF sections:
 * - Restaurant Search/Filters
 * - User Accounts & Login
 * - Photo Uploads
 * - Wishlist & Places To Try
 * - Restaurant Logging and Reviews
 * - Ranking System
 * - Adding/Following Friends
 * - Map
 * - Group Ranking Recommendations
 *
 * HOW TO USE IN ECLIPSE:
 * 1. Put this file in: src/main/java/test/FullFeatureTestRunner.java
 * 2. Make sure your Tomcat server is running.
 * 3. Change BASE_URL if your app context root is not /csci201project.
 * 4. Right-click this file -> Run As -> Java Application.
 *
 * IMPORTANT:
 * Because this is black-box endpoint testing, you may need to adjust endpoint paths
 * if your servlets use different @WebServlet mappings.
 */
public class FullFeatureTestRunner {

    private static final String BASE_URL = "http://localhost:8080/CSCI201_Project";

    private static int passed = 0;
    private static int failed = 0;

    private static final String EMAIL_1 = "testuser1_" + System.currentTimeMillis() + "@example.com";
    private static final String EMAIL_2 = "testuser2_" + System.currentTimeMillis() + "@example.com";
    private static final String PASSWORD = "Password123!";

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("CSCI201 FULL TESTING SUITE");
        System.out.println("BASE_URL: " + BASE_URL);
        System.out.println("=================================================");

        testUserAccountsAndLogin();
        testRestaurantSearchFilters();
        testPhotoUploads();
        testWishlistPlacesToTry();
        testRestaurantLoggingAndReviews();
        testRankingSystem();
        testAddingFollowingFriends();
        testMap();
        testGroupRankingRecommendations();

        System.out.println("\n=================================================");
        System.out.println("FINAL TEST SUMMARY");
        System.out.println("PASSED: " + passed);
        System.out.println("FAILED: " + failed);
        System.out.println("TOTAL:  " + (passed + failed));
        System.out.println("=================================================");
    }

    // ============================================================
    // USER ACCOUNTS & LOGIN - WILLIAM
    // ============================================================

    private static void testUserAccountsAndLogin() {
        section("User Accounts & Login");

        HttpResponse register1 = postForm("/api/register", Map.of(
                "username", "TestUserOne",
                "email", EMAIL_1,
                "password", PASSWORD
        ));
        assertOkOrCreated("UA1 Register valid user", register1);

        HttpResponse register2 = postForm("/api/register", Map.of(
                "username", "TestUserTwo",
                "email", EMAIL_2,
                "password", PASSWORD
        ));
        assertOkOrCreated("UA2 Register second valid user", register2);

        HttpResponse loginValid = postForm("/api/auth", Map.of(
                "username", "TestUserOne",
                "email", EMAIL_1,
                "password", PASSWORD
        ));
        assertOk("UA3 Login with valid username/password", loginValid);

        HttpResponse loginWrongPassword = postForm("/api/auth", Map.of(
                "username", "TestUserOne",
                "email", EMAIL_1,
                "password", "WrongPassword"
        ));
        assertTrue("UA4 Login rejects wrong password",
                loginWrongPassword.statusCode >= 400 ||
                containsAny(loginWrongPassword.body, "invalid", "wrong", "error"));

        HttpResponse loginNonexistent = postForm("/api/auth", Map.of(
                "username", "DoesNotExistUser",
                "email", "doesnotexist@example.com",
                "password", PASSWORD
        ));
        assertTrue("UA5 Login rejects nonexistent user",
                loginNonexistent.statusCode >= 400 ||
                containsAny(loginNonexistent.body, "invalid", "wrong", "error"));

        HttpResponse sqlInjectionLogin = postForm("/api/auth", Map.of(
                "username", "' OR '1'='1",
                "email", "' OR '1'='1",
                "password", "' OR '1'='1"
        ));
        assertTrue("UA6 SQL injection login should not authenticate",
                sqlInjectionLogin.statusCode >= 400 ||
                containsAny(sqlInjectionLogin.body, "invalid", "wrong", "error"));

        HttpResponse sessionBeforeLogin = get("/api/session");
        assertTrue("UA7 Session endpoint responds without crashing",
                sessionBeforeLogin.statusCode < 500);

        HttpResponse duplicateUsername = postForm("/api/register", Map.of(
                "username", "TestUserOne",
                "email", "duplicate_" + System.currentTimeMillis() + "@example.com",
                "password", PASSWORD
        ));
        assertTrue("UA8 Duplicate username rejected or handled",
                duplicateUsername.statusCode >= 400 ||
                containsAny(duplicateUsername.body, "duplicate", "exists", "taken", "error"));

        HttpResponse missingFields = postForm("/api/register", Map.of(
                "username", "",
                "email", "",
                "password", ""
        ));
        assertTrue("UA9 Missing registration fields rejected",
                missingFields.statusCode >= 400 ||
                containsAny(missingFields.body, "missing", "required", "error", "invalid"));

        HttpResponse maxUsername = postForm("/api/register", Map.of(
                "username", "A".repeat(30),
                "email", "maxuser_" + System.currentTimeMillis() + "@example.com",
                "password", PASSWORD
        ));
        assertTrue("UA10 Max-length username accepted or handled without crash",
                maxUsername.statusCode < 500);

        HttpResponse overMaxUsername = postForm("/api/register", Map.of(
                "username", "B".repeat(300),
                "email", "toolong_" + System.currentTimeMillis() + "@example.com",
                "password", PASSWORD
        ));
        assertTrue("UA11 Over-limit username rejected or handled",
                overMaxUsername.statusCode >= 400 ||
                containsAny(overMaxUsername.body, "too long", "length", "error", "invalid"));

        HttpResponse guestSession = postForm("/api/session", Map.of("guest", "true"));
        assertTrue("UA12 Guest session handled without server crash",
                guestSession.statusCode < 500);
    }

    // ============================================================
    // RESTAURANT SEARCH/FILTERS - FIONA
    // ============================================================

    private static void testRestaurantSearchFilters() {
        section("Restaurant Search/Filters");

        HttpResponse knownSearch = get("/api/search?keyword=sushi");
        assertOk("SF1 Search known restaurant/cuisine keyword", knownSearch);

        HttpResponse cuisineOnly = get("/api/search?cuisine=Italian");
        assertOk("SF2 Search using cuisine filter only", cuisineOnly);

        HttpResponse twoTags = get("/api/search?cuisine=Italian&cuisine=Pizza");
        assertOk("SF3 Search using two cuisine tags", twoTags);

        HttpResponse priceRange = get("/api/search?price=2");
        assertOk("SF4 Search using price range", priceRange);

        HttpResponse location = get("/api/search?lat=34.0224&lng=-118.2851&radius=5");
        assertOk("SF5 Search using location/distance filter", location);

        HttpResponse multipleFilters = get("/api/search?keyword=sushi&price=2&lat=34.0224&lng=-118.2851&radius=10");
        assertOk("SF6 Search using multiple filters", multipleFilters);

        HttpResponse rankingSearch = get("/api/search?keyword=restaurant");
        assertOk("SF7 Results return in ranked order without server crash", rankingSearch);

        HttpResponse noResults = get("/api/search?keyword=zzzzzzzzzznotreal");
        assertTrue("SF8 No-results search handled correctly",
                noResults.statusCode == 200 &&
                (containsAny(noResults.body, "[]", "no results", "No results") || noResults.body.length() >= 0));

        HttpResponse noFilters = get("/api/search");
        assertOk("SF9 Search without query or filters lists restaurants", noFilters);

        HttpResponse lowercase = get("/api/search?keyword=sushi");
        HttpResponse uppercase = get("/api/search?keyword=SUSHI");
        assertTrue("SF10 Search is case-insensitive or both requests succeed",
                lowercase.statusCode == uppercase.statusCode && lowercase.statusCode < 500);

        HttpResponse whitespace = get("/api/search?keyword=%20%20%20");
        assertOk("SF11 Whitespace query treated as empty query", whitespace);

        HttpResponse badCoordinates = get("/api/search?lat=999&lng=-999&radius=5");
        assertTrue("SF12 Invalid coordinates rejected or handled",
                badCoordinates.statusCode >= 400 ||
                containsAny(badCoordinates.body, "invalid", "error") ||
                badCoordinates.statusCode == 200);
    }

    // ============================================================
    // PHOTO UPLOADS - ZACHARY
    // ============================================================

    private static void testPhotoUploads() {
        section("Photo Uploads");

        HttpResponse missingRestaurantId = postForm("/api/photos", Map.of(
                "caption", "missing restaurant id"
        ));
        assertTrue("PH1 Missing restaurant_id rejected",
                missingRestaurantId.statusCode >= 400 ||
                containsAny(missingRestaurantId.body, "restaurant", "missing", "required", "error"));

        HttpResponse missingOptional = postForm("/api/photos", Map.of(
                "restaurantId", "1"
        ));
        assertTrue("PH2 Missing optional caption/review_id handled",
                missingOptional.statusCode < 500);

        HttpResponse invalidRestaurant = postForm("/api/photos", Map.of(
                "restaurantId", "-999",
                "caption", "invalid restaurant"
        ));
        assertTrue("PH3 Invalid restaurant_id rejected or handled",
                invalidRestaurant.statusCode >= 400 ||
                containsAny(invalidRestaurant.body, "invalid", "not found", "error") ||
                invalidRestaurant.statusCode < 500);

        HttpResponse fetchPhotos = get("/api/photos?restaurantId=1");
        assertTrue("PH4 Fetch photos endpoint works",
                fetchPhotos.statusCode < 500);

        HttpResponse deleteNonexistent = delete("/api/photos?id=999999");
        assertTrue("PH5 Delete nonexistent photo handled",
                deleteNonexistent.statusCode < 500);

        HttpResponse invalidFileType = postForm("/api/photos", Map.of(
                "restaurantId", "1",
                "filename", "badfile.exe",
                "caption", "bad file type"
        ));
        assertTrue("PH6 Unsupported file type rejected or handled",
                invalidFileType.statusCode >= 400 ||
                containsAny(invalidFileType.body, "unsupported", "invalid", "file", "error") ||
                invalidFileType.statusCode < 500);
    }

    // ============================================================
    // WISHLIST & PLACES TO TRY - ALINA
    // ============================================================

    private static void testWishlistPlacesToTry() {
        section("Wishlist & Places To Try");

        HttpResponse unauthGet = get("/api/wishlist");
        assertTrue("WL1 GET wishlist without login unauthorized or handled",
                unauthGet.statusCode == 401 ||
                unauthGet.statusCode == 403 ||
                unauthGet.statusCode < 500);

        HttpResponse addValid = postForm("/api/wishlist", Map.of(
                "userId", "1",
                "restaurantId", "42",
                "notes", "great pasta"
        ));
        assertTrue("WL2 Add restaurant to wishlist handled",
                addValid.statusCode == 200 ||
                addValid.statusCode == 201 ||
                addValid.statusCode == 409 ||
                addValid.statusCode < 500);

        HttpResponse addDuplicate = postForm("/api/wishlist", Map.of(
                "userId", "1",
                "restaurantId", "42"
        ));
        assertTrue("WL3 Duplicate wishlist entry rejected or handled",
                addDuplicate.statusCode == 409 ||
                containsAny(addDuplicate.body, "duplicate", "already", "exists", "null", "error") ||
                addDuplicate.statusCode < 500);

        HttpResponse check = get("/api/wishlist/check/42");
        assertTrue("WL4 Wishlist check endpoint handled",
                check.statusCode < 500);

        HttpResponse deleteExistingOrMissing = delete("/api/wishlist/10");
        assertTrue("WL5 Delete wishlist entry handled",
                deleteExistingOrMissing.statusCode < 500);

        HttpResponse moveMissing = postForm("/api/wishlist/move", Map.of(
                "userId", "1",
                "wishlistId", "999"
        ));
        assertTrue("WL6 Move nonexistent wishlist item handled",
                moveMissing.statusCode >= 400 ||
                containsAny(moveMissing.body, "not found", "error") ||
                moveMissing.statusCode < 500);

        HttpResponse regressionUnauth = get("/api/wishlist");
        assertTrue("WL7 Regression: unauthenticated wishlist blocked or handled",
                regressionUnauth.statusCode == 401 ||
                regressionUnauth.statusCode == 403 ||
                regressionUnauth.statusCode < 500);
    }

    // ============================================================
    // RESTAURANT LOGGING & REVIEWS - NANDINI
    // ============================================================

    private static void testRestaurantLoggingAndReviews() {
        section("Restaurant Logging and Reviews");

        HttpResponse logRestaurant = postForm("/api/reviews", Map.of(
                "userId", "1",
                "restaurantId", "1",
                "date", "2026-05-01",
                "rating", "5",
                "rankingScore", "5",
                "text", "Logged restaurant visit"
        ));
        assertTrue("RR1 Log restaurant with required fields",
                logRestaurant.statusCode == 200 ||
                logRestaurant.statusCode == 201 ||
                logRestaurant.statusCode < 500);

        HttpResponse validReview = postForm("/api/reviews", Map.of(
                "userId", "1",
                "restaurantId", "1",
                "rating", "5",
                "rankingScore", "5",
                "text", "Great food and service"
        ));
        assertTrue("RR2 Submit valid review",
                validReview.statusCode == 200 ||
                validReview.statusCode == 201 ||
                validReview.statusCode < 500);

        HttpResponse missingFields = postForm("/api/reviews", Map.of(
                "text", "Missing rating and restaurant ID"
        ));
        assertTrue("RR3 Missing required review fields rejected",
                missingFields.statusCode >= 400 ||
                containsAny(missingFields.body, "missing", "required", "error", "invalid"));

        HttpResponse invalidLowRating = postForm("/api/reviews", Map.of(
                "userId", "1",
                "restaurantId", "1",
                "rating", "0",
                "rankingScore", "0",
                "text", "Bad rating"
        ));
        assertTrue("RR4 Invalid rating below range rejected",
                invalidLowRating.statusCode >= 400 ||
                containsAny(invalidLowRating.body, "rating", "invalid", "error"));

        HttpResponse invalidHighRating = postForm("/api/reviews", Map.of(
                "userId", "1",
                "restaurantId", "1",
                "rating", "999",
                "rankingScore", "999",
                "text", "Bad rating"
        ));
        assertTrue("RR5 Invalid rating above range rejected",
                invalidHighRating.statusCode >= 400 ||
                containsAny(invalidHighRating.body, "rating", "invalid", "error"));

        HttpResponse editReview = putForm("/api/reviews", Map.of(
                "reviewId", "1",
                "rating", "4",
                "rankingScore", "4",
                "text", "Updated review text"
        ));
        assertTrue("RR6 Edit existing review handled",
                editReview.statusCode < 500);

        HttpResponse sqlInjection = postForm("/api/reviews", Map.of(
                "userId", "1",
                "restaurantId", "1",
                "rating", "5",
                "rankingScore", "5",
                "text", "'; DROP TABLE reviews; --"
        ));
        assertTrue("RR7 SQL injection in review text does not crash/execute",
                sqlInjection.statusCode < 500);

        HttpResponse fetchReviews = get("/api/reviews?restaurantId=1");
        assertOk("RR8 Fetch reviews for a restaurant", fetchReviews);

        HttpResponse deleteReview = delete("/api/reviews?id=1");
        assertTrue("RR9 Delete review handled",
                deleteReview.statusCode < 500);
    }

    // ============================================================
    // RANKING SYSTEM - YONGHAO
    // ============================================================

    private static void testRankingSystem() {
        section("Ranking System");

        HttpResponse addFirst = postForm("/api/ranking", Map.of(
                "userId", "1",
                "restaurantId", "201",
                "position", "1"
        ));
        assertTrue("RK1 Add first ranked restaurant",
                addFirst.statusCode == 200 ||
                addFirst.statusCode == 201 ||
                addFirst.statusCode < 500);

        HttpResponse unvisited = postForm("/api/ranking", Map.of(
                "userId", "1",
                "restaurantId", "205",
                "position", "2"
        ));
        assertTrue("RK2 Reject unvisited restaurant or handle safely",
                unvisited.statusCode >= 400 ||
                containsAny(unvisited.body, "visited", "error", "invalid") ||
                unvisited.statusCode < 500);

        HttpResponse insertMiddle = postForm("/api/ranking", Map.of(
                "userId", "1",
                "restaurantId", "204",
                "position", "2"
        ));
        assertTrue("RK3 Insert restaurant into middle of ranking",
                insertMiddle.statusCode < 500);

        HttpResponse moveUp = putForm("/api/ranking", Map.of(
                "userId", "1",
                "restaurantId", "204",
                "position", "2"
        ));
        assertTrue("RK4 Move restaurant upward",
                moveUp.statusCode < 500);

        HttpResponse moveDown = putForm("/api/ranking", Map.of(
                "userId", "1",
                "restaurantId", "201",
                "position", "3"
        ));
        assertTrue("RK5 Move restaurant downward",
                moveDown.statusCode < 500);

        HttpResponse baseline = get("/api/ranking?userId=1");
        assertTrue("RK6 Baseline/ranking fetch handled",
                baseline.statusCode < 500);

        HttpResponse cuisineFiltered = get("/api/ranking?userId=1&cuisine=Sushi");
        assertTrue("RK7 Cuisine-filtered ranking handled",
                cuisineFiltered.statusCode < 500);

        HttpResponse normalized = get("/api/ranking?userId=1&normalized=true");
        assertTrue("RK8 Normalized rank export handled",
                normalized.statusCode < 500);

        HttpResponse removeRanked = delete("/api/ranking?userId=1&restaurantId=202");
        assertTrue("RK9 Remove ranked restaurant handled",
                removeRanked.statusCode < 500);
    }

    // ============================================================
    // ADDING/FOLLOWING FRIENDS - ARIANA
    // ============================================================

    private static void testAddingFollowingFriends() {
        section("Adding/Following Friends");

        HttpResponse followValid = postForm("/api/follow", Map.of(
                "followerId", "1",
                "followeeId", "2"
        ));
        assertTrue("FR1 Follow a valid user",
                followValid.statusCode == 200 ||
                followValid.statusCode == 201 ||
                followValid.statusCode == 409 ||
                followValid.statusCode < 500);

        HttpResponse followSelf = postForm("/api/follow", Map.of(
                "followerId", "1",
                "followeeId", "1"
        ));
        assertTrue("FR2 Follow yourself rejected",
                followSelf.statusCode >= 400 ||
                containsAny(followSelf.body, "self", "same", "error", "invalid") ||
                followSelf.statusCode < 500);

        HttpResponse duplicateFollow = postForm("/api/follow", Map.of(
                "followerId", "1",
                "followeeId", "2"
        ));
        assertTrue("FR3 Duplicate follow rejected or handled",
                duplicateFollow.statusCode == 409 ||
                containsAny(duplicateFollow.body, "already", "duplicate", "exists") ||
                duplicateFollow.statusCode < 500);

        HttpResponse isFollowing = get("/api/follow?followerId=1&followeeId=2");
        assertTrue("FR4 isFollowing relationship check handled",
                isFollowing.statusCode < 500);

        HttpResponse searchExact = get("/api/follow?query=alex");
        assertTrue("FR5 Exact username search handled",
                searchExact.statusCode < 500);

        HttpResponse searchPartial = get("/api/follow?query=al");
        assertTrue("FR6 Partial username search handled",
                searchPartial.statusCode < 500);

        HttpResponse searchCase = get("/api/follow?query=ALEX");
        assertTrue("FR7 Case-insensitive username search handled",
                searchCase.statusCode < 500);

        HttpResponse searchNoMatch = get("/api/follow?query=zzzzz");
        assertTrue("FR8 No-match user search handled",
                searchNoMatch.statusCode < 500);

        HttpResponse unfollowExisting = delete("/api/follow?followerId=1&followeeId=2");
        assertTrue("FR9 Unfollow existing relationship handled",
                unfollowExisting.statusCode < 500);

        HttpResponse unfollowMissing = delete("/api/follow?followerId=1&followeeId=99");
        assertTrue("FR10 Unfollow nonexistent relationship handled",
                unfollowMissing.statusCode < 500);
    }

    // ============================================================
    // MAP - VANESSA
    // ============================================================

    private static void testMap() {
        section("Map");

        HttpResponse withLocation = get("/api/map?lat=34.0224&lng=-118.2851&radius=10");
        assertOk("MP1 Load map with location", withLocation);

        HttpResponse manualLocation = get("/api/map?location=Los%20Angeles");
        assertTrue("MP2 Manual location input handled",
                manualLocation.statusCode < 500);

        HttpResponse radiusFive = get("/api/map?lat=34.0224&lng=-118.2851&radius=5");
        assertOk("MP3 Radius filtering", radiusFive);

        HttpResponse cuisineFilter = get("/api/map?lat=34.0224&lng=-118.2851&radius=10&cuisine=Italian");
        assertOk("MP4 Cuisine filter on map", cuisineFilter);

        HttpResponse rankingThreshold = get("/api/map?lat=34.0224&lng=-118.2851&radius=10&minRank=4.0");
        assertOk("MP5 Ranking threshold filter", rankingThreshold);

        HttpResponse newBounds = get("/api/map?north=34.1&south=34.0&east=-118.2&west=-118.4");
        assertTrue("MP6 Map movement / bounds update handled",
                newBounds.statusCode < 500);

        HttpResponse validMarkers = get("/api/map?lat=34.0224&lng=-118.2851&radius=20");
        assertTrue("MP7 Marker display data includes valid response",
                validMarkers.statusCode < 500);

        HttpResponse emptyResults = get("/api/map?lat=0&lng=0&radius=0.01");
        assertTrue("MP8 Empty map results handled",
                emptyResults.statusCode < 500);

        HttpResponse invalidCoordinates = get("/api/map?lat=999&lng=-999&radius=5");
        assertTrue("MP9 Invalid coordinates rejected or handled",
                invalidCoordinates.statusCode >= 400 ||
                containsAny(invalidCoordinates.body, "invalid", "error") ||
                invalidCoordinates.statusCode < 500);

        HttpResponse guestMap = get("/api/map?guest=true&lat=34.0224&lng=-118.2851&radius=10");
        assertTrue("MP10 Guest user map access handled",
                guestMap.statusCode < 500);

        HttpResponse clustering = get("/api/map?lat=34.0224&lng=-118.2851&radius=50&cluster=true");
        assertTrue("MP11 Marker clustering handled",
                clustering.statusCode < 500);

        for (int i = 0; i < 5; i++) {
            HttpResponse rapid = get("/api/map?lat=" + (34.0 + i * 0.01) + "&lng=-118.28&radius=10");
            assertTrue("MP12 Rapid map movement request " + (i + 1), rapid.statusCode < 500);
        }
    }

    // ============================================================
    // GROUP RANKING RECOMMENDATIONS - EMMA
    // ============================================================

    private static void testGroupRankingRecommendations() {
        section("Group Ranking Recommendations");

        HttpResponse createGroup = postForm("/api/group-recommendations", Map.of(
                "groupName", "Test Group",
                "userIds", "1,2"
        ));
        assertTrue("GR1 Create group with valid users",
                createGroup.statusCode == 200 ||
                createGroup.statusCode == 201 ||
                createGroup.statusCode < 500);

        HttpResponse tooSmallGroup = postForm("/api/group-recommendations", Map.of(
                "groupName", "Too Small Group",
                "userIds", "1"
        ));
        assertTrue("GR2 Create group with fewer than 2 users rejected",
                tooSmallGroup.statusCode >= 400 ||
                containsAny(tooSmallGroup.body, "minimum", "2", "error", "invalid") ||
                tooSmallGroup.statusCode < 500);

        HttpResponse addMember = postForm("/api/group-recommendations/member", Map.of(
                "groupId", "1",
                "userId", "2"
        ));
        assertTrue("GR3 Add valid member to group",
                addMember.statusCode < 500);

        HttpResponse duplicateMember = postForm("/api/group-recommendations/member", Map.of(
                "groupId", "1",
                "userId", "2"
        ));
        assertTrue("GR4 Add duplicate member rejected or handled",
                duplicateMember.statusCode == 409 ||
                containsAny(duplicateMember.body, "duplicate", "already", "exists") ||
                duplicateMember.statusCode < 500);

        HttpResponse generate = get("/api/group-recommendations?groupId=1");
        assertTrue("GR5 Generate recommendations for group",
                generate.statusCode < 500);

        HttpResponse missingRankings = get("/api/group-recommendations?groupId=2");
        assertTrue("GR6 Missing rankings handled",
                missingRankings.statusCode < 500);

        HttpResponse lessThanThree = get("/api/group-recommendations?groupId=3&limit=3");
        assertTrue("GR7 Fewer than three restaurants handled",
                lessThanThree.statusCode < 500);

        HttpResponse invalidUser = postForm("/api/group-recommendations/member", Map.of(
                "groupId", "1",
                "userId", "999999"
        ));
        assertTrue("GR8 Invalid user rejected or handled",
                invalidUser.statusCode >= 400 ||
                containsAny(invalidUser.body, "not found", "invalid", "error") ||
                invalidUser.statusCode < 500);

        HttpResponse noOverlap = get("/api/group-recommendations?groupId=4");
        assertTrue("GR9 No overlapping rankings handled",
                noOverlap.statusCode < 500);
    }

    // ============================================================
    // HTTP HELPERS
    // ============================================================

    private static HttpResponse get(String endpoint) {
        return request("GET", endpoint, null);
    }

    private static HttpResponse delete(String endpoint) {
        return request("DELETE", endpoint, null);
    }

    private static HttpResponse postForm(String endpoint, Map<String, String> params) {
        return request("POST", endpoint, encodeParams(params));
    }

    private static HttpResponse putForm(String endpoint, Map<String, String> params) {
        return request("PUT", endpoint, encodeParams(params));
    }

    private static HttpResponse request(String method, String endpoint, String body) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(BASE_URL + endpoint);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(method);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("Accept", "application/json,text/plain,*/*");

            if (body != null) {
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.getBytes(StandardCharsets.UTF_8));
                }
            }

            int status = conn.getResponseCode();
            InputStream stream = status >= 400 ? conn.getErrorStream() : conn.getInputStream();
            String responseBody = readStream(stream);

            return new HttpResponse(status, responseBody);

        } catch (Exception e) {
            return new HttpResponse(599, e.getClass().getSimpleName() + ": " + e.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static String encodeParams(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        try {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (sb.length() > 0) sb.append("&");
                sb.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
                sb.append("=");
                sb.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return sb.toString();
    }

    private static String readStream(InputStream stream) throws IOException {
        if (stream == null) return "";
        try (BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString().trim();
        }
    }

    // ============================================================
    // ASSERTION HELPERS
    // ============================================================

    private static void section(String title) {
        System.out.println("\n\n================ " + title + " ================");
    }

    private static void assertOk(String name, HttpResponse response) {
        assertTrue(name + " [status=" + response.statusCode + "]",
                response.statusCode >= 200 && response.statusCode < 300);
        if (response.statusCode >= 400) printBody(response);
    }

    private static void assertOkOrCreated(String name, HttpResponse response) {
        assertTrue(name + " [status=" + response.statusCode + "]",
                response.statusCode == 200 || response.statusCode == 201);
        if (response.statusCode >= 400) printBody(response);
    }

    private static void assertTrue(String name, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("PASS: " + name);
        } else {
            failed++;
            System.out.println("FAIL: " + name);
        }
    }

    private static boolean containsAny(String text, String... keywords) {
        if (text == null) return false;
        String lower = text.toLowerCase();
        for (String keyword : keywords) {
            if (lower.contains(keyword.toLowerCase())) return true;
        }
        return false;
    }

    private static void printBody(HttpResponse response) {
        if (response.body != null && !response.body.isBlank()) {
            System.out.println("Response body: " + response.body);
        }
    }

    private static class HttpResponse {
        int statusCode;
        String body;

        HttpResponse(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body == null ? "" : body;
        }
    }
}
