package servlet;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import model.BoundingBox;
import model.MapFilterParams;
import model.MapResult;
import service.MapService;

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

@WebServlet("/api/map/restaurants")
public class MapServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private final MapService mapService = new MapService();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        try {
            MapFilterParams filters = parseFilters(request);
            Long userId = currentUserId(request);
            List<MapResult> results = mapService.getNearbyRestaurants(filters, userId);
            out.print(gson.toJson(results));
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print(gson.toJson(error(e.getMessage())));
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print(gson.toJson(error("Failed to load map data")));
        }
    }

    private MapFilterParams parseFilters(HttpServletRequest request) {
        MapFilterParams filters = new MapFilterParams();
        filters.setUserLat(parseRequiredDouble(request, "lat"));
        filters.setUserLng(parseRequiredDouble(request, "lng"));
        filters.setRadiusMiles(parseOptionalDouble(request, "radius_miles", 25.0));
        filters.setMinRank(parseOptionalDouble(request, "min_rank", null));
        filters.setOpenNow(parseOptionalBoolean(request, "open_now"));
        filters.setBounds(parseBounds(request));
        filters.setCuisines(parseStringValues(request, "cuisines"));
        filters.setPriceTiers(parseIntegerValues(request, "priceTiers", "price"));
        return filters;
    }

    private BoundingBox parseBounds(HttpServletRequest request) {
        Double north = parseOptionalDouble(request, "boundsNorth", null);
        Double south = parseOptionalDouble(request, "boundsSouth", null);
        Double east = parseOptionalDouble(request, "boundsEast", null);
        Double west = parseOptionalDouble(request, "boundsWest", null);
        if (north == null || south == null || east == null || west == null) {
            north = parseOptionalDouble(request, "north", null);
            south = parseOptionalDouble(request, "south", null);
            east = parseOptionalDouble(request, "east", null);
            west = parseOptionalDouble(request, "west", null);
        }
        if (north == null || south == null || east == null || west == null) return null;
        return new BoundingBox(north, south, east, west);
    }

    private List<String> parseStringValues(HttpServletRequest request, String parameterName) {
        String[] values = request.getParameterValues(parameterName);
        List<String> parsed = new ArrayList<>();
        if (values == null) return parsed;
        for (String value : values) {
            if (value == null || value.isBlank()) continue;
            for (String token : value.split(",")) {
                String trimmed = token.trim();
                if (!trimmed.isEmpty()) parsed.add(trimmed);
            }
        }
        return parsed;
    }

    private List<Integer> parseIntegerValues(HttpServletRequest request, String... names) {
        List<Integer> values = new ArrayList<>();
        for (String name : names) {
            String[] rawValues = request.getParameterValues(name);
            if (rawValues == null) continue;
            for (String raw : rawValues) {
                if (raw == null || raw.isBlank()) continue;
                for (String token : raw.split(",")) {
                    String trimmed = token.trim();
                    if (trimmed.isEmpty()) continue;
                    try {
                        values.add(Integer.parseInt(trimmed));
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Invalid integer parameter: " + trimmed);
                    }
                }
            }
        }
        return values;
    }

    private Double parseRequiredDouble(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required");
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number for " + name);
        }
    }

    private Double parseOptionalDouble(HttpServletRequest request, String name, Double defaultValue) {
        String value = request.getParameter(name);
        if (value == null || value.isBlank()) return defaultValue;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number for " + name);
        }
    }

    private Boolean parseOptionalBoolean(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        if (value == null || value.isBlank()) return null;
        return Boolean.parseBoolean(value);
    }

    private Long currentUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) return null;
        Object value = session.getAttribute("userId");
        if (value instanceof Long longValue) return longValue;
        if (value instanceof Integer integerValue) return integerValue.longValue();
        return Long.parseLong(String.valueOf(value));
    }

    private JsonObject error(String message) {
        JsonObject object = new JsonObject();
        object.addProperty("error", message);
        return object;
    }
}
