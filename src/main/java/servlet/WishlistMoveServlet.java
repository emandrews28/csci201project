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

@WebServlet("/api/wishlist/move-to-visited")
public class WishlistMoveServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private final WishlistDAO wishlistDAO = new WishlistDAO();
    private final Gson gson = new Gson();

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

        long userId = (long) session.getAttribute("userId");
        long restaurantId = 0;

        String restaurantIdStr = request.getParameter("restaurantId");
        if (restaurantIdStr != null) {
            try {
                restaurantId = Long.parseLong(restaurantIdStr);
            } catch (NumberFormatException ignore) {
                // fall through to body parse below
            }
        }

        if (restaurantId <= 0) {
            try {
                Wishlist entry = gson.fromJson(request.getReader(), Wishlist.class);
                if (entry != null) restaurantId = entry.getRestaurantId();
            } catch (Exception ignore) {
                // leave restaurantId at 0, handled below
            }
        }

        if (restaurantId <= 0) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\":\"restaurantId must be provided\"}");
            return;
        }

        boolean moved = wishlistDAO.moveToVisited(userId, restaurantId);

        if (moved) {
            out.print("{\"message\":\"Moved to visited list\"}");
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            out.print("{\"error\":\"Wishlist entry not found\"}");
        }
    }
}
