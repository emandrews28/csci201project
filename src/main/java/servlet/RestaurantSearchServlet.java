package servlet;

import com.google.gson.Gson;
import dao.CuisineDAO;
import dao.RestaurantDAO;
import model.FilterParams;
import model.Restaurant;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * GET /api/restaurants/search
 *
 * Query params:
 *   q             - text search on name (optional)
 *   cuisines      - comma-separated cuisine IDs, e.g. cuisines=1,3,5 (optional)
 *   price         - comma-separated price tiers 1-4, e.g. price=1,2 (optional)
 *   lat           - user latitude (optional)
 *   lng           - user longitude (optional)
 *   radius        - radius in miles (optional, requires lat+lng)
 *   friendsOnly   - true/false (optional, requires login)
 *   sort          - top_rated | distance | newest (default: top_rated)
 *   page          - 0-indexed page number (default: 0)
 *   limit         - results per page (default: 20, max: 50)
 *
 * GET /api/restaurants/{id}  (handled via pathInfo)
 */
@WebServlet("/api/restaurants/*")
public class RestaurantSearchServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private final RestaurantDAO restaurantDAO = new RestaurantDAO();
    private final CuisineDAO cuisineDAO = new CuisineDAO();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        String pathInfo = request.getPathInfo(); // e.g. "/search" or "/42"

        // Route: GET /api/restaurants/search
        if (pathInfo != null && pathInfo.equals("/search")) {
            handleSearch(request, response, out);
            return;
        }

        // Route: GET /api/restaurants/{id}
        if (pathInfo != null && pathInfo.length() > 1) {
            try {
                long restaurantId = Long.parseLong(pathInfo.substring(1));
                Restaurant r = restaurantDAO.findById(restaurantId);
                if (r == null) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print("{\"error\":\"Restaurant not found\"}");
                } else {
                    out.print(gson.toJson(r));
                }
            } catch (NumberFormatException e) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"error\":\"Invalid restaurant ID\"}");
            }
            return;
        }

        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        out.print("{\"error\":\"Use /api/restaurants/search or /api/restaurants/{id}\"}");
    }

    private void handleSearch(HttpServletRequest request, HttpServletResponse response, PrintWriter out)
            throws IOException {
        HttpSession session = request.getSession(false);
        long userId = (session != null && session.getAttribute("userId") != null)
                ? (long) session.getAttribute("userId") : -1L;

        FilterParams params = new FilterParams();

        // Text query
        String q = request.getParameter("q");
        if (q != null && !q.isBlank()) params.setQuery(q.trim());

        // Cuisine IDs — comma-separated, each expanded to include children
        String cuisinesParam = request.getParameter("cuisines");
        if (cuisinesParam != null && !cuisinesParam.isBlank()) {
            List<Long> expandedIds = new ArrayList<>();
            for (String part : cuisinesParam.split(",")) {
                try {
                    long cid = Long.parseLong(part.trim());
                    List<Long> subtree = cuisineDAO.expandCuisineTree(cid);
                    for (Long id : subtree) {
                        if (!expandedIds.contains(id)) expandedIds.add(id);
                    }
                } catch (NumberFormatException ignored) {}
            }
            if (!expandedIds.isEmpty()) params.setCuisineIds(expandedIds);
        }

        // Price tiers
        String priceParam = request.getParameter("price");
        if (priceParam != null && !priceParam.isBlank()) {
            List<Integer> tiers = new ArrayList<>();
            for (String part : priceParam.split(",")) {
                try {
                    int tier = Integer.parseInt(part.trim());
                    if (tier >= 1 && tier <= 4) tiers.add(tier);
                } catch (NumberFormatException ignored) {}
            }
            if (!tiers.isEmpty()) params.setPriceTiers(tiers);
        }

        // Location
        String latStr = request.getParameter("lat");
        String lngStr = request.getParameter("lng");
        if (latStr != null && lngStr != null) {
            try {
                params.setUserLat(Double.parseDouble(latStr));
                params.setUserLng(Double.parseDouble(lngStr));
            } catch (NumberFormatException ignored) {}
        }

        // Radius
        String radiusStr = request.getParameter("radius");
        if (radiusStr != null) {
            try {
                double radius = Double.parseDouble(radiusStr);
                if (radius > 0) params.setRadiusMiles(radius);
            } catch (NumberFormatException ignored) {}
        }

        // Friends only — requires login
        String friendsOnly = request.getParameter("friendsOnly");
        if ("true".equalsIgnoreCase(friendsOnly) && userId != -1L) {
            params.setFriendsOnly(true);
        }

        // Sort
        String sort = request.getParameter("sort");
        if (sort != null && (sort.equals("distance") || sort.equals("newest") || sort.equals("top_rated"))) {
            params.setSortBy(sort);
        }

        // Pagination
        try {
            String pageStr = request.getParameter("page");
            if (pageStr != null) params.setPage(Math.max(0, Integer.parseInt(pageStr)));
            String limitStr = request.getParameter("limit");
            if (limitStr != null) params.setLimit(Math.min(50, Math.max(1, Integer.parseInt(limitStr))));
        } catch (NumberFormatException ignored) {}

        List<Restaurant> results = restaurantDAO.search(params, userId);
        out.print(gson.toJson(results));
    }
}
