package servlet;

import com.google.gson.Gson;
import dao.WishlistDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.Wishlist;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@WebServlet("/api/wishlist")
public class WishlistServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private final WishlistDAO wishlistDAO = new WishlistDAO();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"error\":\"Not logged in\"}");
            return;
        }

        long userId = (long) session.getAttribute("userId");
        List<Wishlist> entries = wishlistDAO.findByUser(userId);
        out.print(gson.toJson(entries));
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"error\":\"Not logged in\"}");
            return;
        }

        Wishlist entry = gson.fromJson(request.getReader(), Wishlist.class);
        if (entry == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\":\"Request body required\"}");
            return;
        }

        long userId = (long) session.getAttribute("userId");
        entry.setUserId(userId);

        if (entry.getRestaurantId() <= 0) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\":\"restaurantId must be provided\"}");
            return;
        }

        if (!wishlistDAO.restaurantExists(entry.getRestaurantId())) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            out.print("{\"error\":\"Restaurant not found\"}");
            return;
        }

        if (wishlistDAO.findByUserAndRestaurant(userId, entry.getRestaurantId()) != null) {
            response.setStatus(HttpServletResponse.SC_CONFLICT);
            out.print("{\"error\":\"Restaurant already on wishlist\"}");
            return;
        }

        Wishlist created = wishlistDAO.createWishlist(entry);

        if (created != null) {
            response.setStatus(HttpServletResponse.SC_CREATED);
            out.print(gson.toJson(created));
        } else {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\":\"Failed to add to wishlist\"}");
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"error\":\"Not logged in\"}");
            return;
        }

        Wishlist entry = gson.fromJson(request.getReader(), Wishlist.class);
        if (entry == null || entry.getRestaurantId() <= 0) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\":\"restaurantId must be provided\"}");
            return;
        }

        long userId = (long) session.getAttribute("userId");
        boolean updated = wishlistDAO.updateNotes(userId, entry.getRestaurantId(), entry.getNotes());

        if (updated) {
            out.print("{\"message\":\"Notes updated\"}");
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            out.print("{\"error\":\"Wishlist entry not found\"}");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"error\":\"Not logged in\"}");
            return;
        }

        String restaurantIdStr = request.getParameter("restaurantId");
        if (restaurantIdStr == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\":\"restaurantId required\"}");
            return;
        }

        long userId = (long) session.getAttribute("userId");
        long restaurantId;
        try {
            restaurantId = Long.parseLong(restaurantIdStr);
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\":\"restaurantId must be a number\"}");
            return;
        }

        boolean deleted = wishlistDAO.deleteWishlist(userId, restaurantId);

        if (deleted) {
            out.print("{\"message\":\"Removed from wishlist\"}");
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            out.print("{\"error\":\"Wishlist entry not found\"}");
        }
    }
}
