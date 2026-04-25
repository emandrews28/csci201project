package servlet;

import com.google.gson.Gson;
import dao.RankingDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.RankingEntry;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@WebServlet("/api/rankings")
public class RankingServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private final RankingDAO rankingDAO = new RankingDAO();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        String userIdStr = request.getParameter("userId");
        long userId;

        if (userIdStr != null && !userIdStr.isBlank()) {
            userId = Long.parseLong(userIdStr);
        } else {
            HttpSession session = request.getSession(false);
            if (session == null || session.getAttribute("userId") == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                out.print("{\"error\":\"Not logged in\"}");
                return;
            }
            userId = (long) session.getAttribute("userId");
        }

        List<RankingEntry> rankings = rankingDAO.findByUser(userId);
        out.print(gson.toJson(rankings));
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

        RankingEntry entry = gson.fromJson(request.getReader(), RankingEntry.class);
        long userId = (long) session.getAttribute("userId");
        entry.setUserId(userId);

        if (entry.getRestaurantId() <= 0) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\":\"restaurantId must be provided\"}");
            return;
        }

        if (entry.getRankPosition() <= 0) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\":\"rankPosition must be greater than 0\"}");
            return;
        }

        if (!rankingDAO.restaurantExists(entry.getRestaurantId())) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            out.print("{\"error\":\"Restaurant not found\"}");
            return;
        }

        if (rankingDAO.findByUserAndRestaurant(userId, entry.getRestaurantId()) != null) {
            response.setStatus(HttpServletResponse.SC_CONFLICT);
            out.print("{\"error\":\"Restaurant already ranked\"}");
            return;
        }

        RankingEntry created = rankingDAO.createRanking(entry);

        if (created != null) {
            response.setStatus(HttpServletResponse.SC_CREATED);
            out.print(gson.toJson(created));
        } else {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\":\"Failed to create ranking\"}");
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

        RankingEntry entry = gson.fromJson(request.getReader(), RankingEntry.class);
        long userId = (long) session.getAttribute("userId");

        if (entry.getRestaurantId() <= 0) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\":\"restaurantId must be provided\"}");
            return;
        }

        if (entry.getRankPosition() <= 0) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\":\"rankPosition must be greater than 0\"}");
            return;
        }

        if (rankingDAO.findByUserAndRestaurant(userId, entry.getRestaurantId()) == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            out.print("{\"error\":\"Ranking not found\"}");
            return;
        }

        boolean updated = rankingDAO.updateRankingPosition(userId, entry.getRestaurantId(), entry.getRankPosition());

        if (updated) {
            out.print("{\"message\":\"Ranking updated successfully\"}");
        } else {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\":\"Failed to update ranking\"}");
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
        long restaurantId = Long.parseLong(restaurantIdStr);

        boolean deleted = rankingDAO.deleteRanking(userId, restaurantId);

        if (deleted) {
            out.print("{\"message\":\"Ranking deleted successfully\"}");
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            out.print("{\"error\":\"Ranking not found\"}");
        }
    }
}