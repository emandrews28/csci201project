package servlet;

import com.google.gson.Gson;
import dao.ReviewDAO;
import model.Review;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@WebServlet("/api/reviews")
public class ReviewServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final ReviewDAO reviewDAO = new ReviewDAO();
    private final Gson gson = new Gson();

    // GET - fetch all reviews for a user
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        String userIdStr = request.getParameter("userId");
        if (userIdStr == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\":\"userId required\"}");
            return;
        }

        long userId = Long.parseLong(userIdStr);
        List<Review> reviews = reviewDAO.findByUser(userId);
        out.print(gson.toJson(reviews));
    }

    // POST - create a new review
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

        Review review = gson.fromJson(request.getReader(), Review.class);
        review.setUserId((long) session.getAttribute("userId"));

        if (review.getRankingScore() < 1 || review.getRankingScore() > 10) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\":\"rankingScore must be between 1 and 10\"}");
            return;
        }

        if (reviewDAO.findByUserAndRestaurant(review.getUserId(), review.getRestaurantId()) != null) {
            response.setStatus(HttpServletResponse.SC_CONFLICT);
            out.print("{\"error\":\"Review already exists. Use PUT to update.\"}");
            return;
        }

        Review created = reviewDAO.createReview(review);
        if (created != null) {
            response.setStatus(HttpServletResponse.SC_CREATED);
            out.print(gson.toJson(created));
        } else {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\":\"Failed to create review\"}");
        }
    }

    // PUT - update an existing review
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

        Review review = gson.fromJson(request.getReader(), Review.class);
        review.setUserId((long) session.getAttribute("userId"));

        if (review.getRankingScore() < 1 || review.getRankingScore() > 10) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\":\"rankingScore must be between 1 and 10\"}");
            return;
        }

        boolean updated = reviewDAO.updateReview(review);
        if (updated) {
            out.print("{\"message\":\"Review updated successfully\"}");
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            out.print("{\"error\":\"Review not found\"}");
        }
    }
    
    // DELETE - delete a review
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

        boolean deleted = reviewDAO.deleteReview(userId, restaurantId);

        if (deleted) {
            out.print("{\"message\":\"Review deleted\"}");
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            out.print("{\"error\":\"Review not found\"}");
        }
    }
}