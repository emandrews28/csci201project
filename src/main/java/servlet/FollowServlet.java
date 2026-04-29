package servlet;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dao.FollowDAO;
import model.User;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@WebServlet("/api/follow/*")
public class FollowServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private final FollowDAO followDAO = new FollowDAO();
    private final Gson gson = new Gson();

    // GET /api/follow/following?userId=X  → list of users X follows
    // GET /api/follow/followers?userId=X  → list of users following X
    // GET /api/follow/status?followerId=X&followingId=Y → is X following Y?
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        PrintWriter out = resp.getWriter();

        // Reject request if the user is not logged in
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"error\":\"Not logged in\"}");
            return;
        }

        // getPathInfo() returns the part of the URL after /api/follow — "/following", "/followers", "/status"
        String path = req.getPathInfo();

        try {
            if ("/following".equals(path)) {
                // Return the list of users that userId is following
                long userId = Long.parseLong(req.getParameter("userId"));
                List<User> following = followDAO.getFollowing(userId);
                out.print(gson.toJson(following));

            } else if ("/followers".equals(path)) {
                // Return the list of users that follow userId
                long userId = Long.parseLong(req.getParameter("userId"));
                List<User> followers = followDAO.getFollowers(userId);
                out.print(gson.toJson(followers));

            } else if ("/status".equals(path)) {
                // Return whether followerId is currently following followingId — used to show follow/unfollow button state
                long followerId = Long.parseLong(req.getParameter("followerId"));
                long followingId = Long.parseLong(req.getParameter("followingId"));
                boolean isFollowing = followDAO.isFollowing(followerId, followingId);
                JsonObject result = new JsonObject();
                result.addProperty("isFollowing", isFollowing);
                out.print(gson.toJson(result));

            } else {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (NumberFormatException e) {
            // Triggered if userId/followerId/followingId can't be parsed as a number
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\":\"Invalid user ID\"}");
        }
    }

    // POST /api/follow/follow   params: followerId, followingId → creates a follow relationship
    // POST /api/follow/unfollow params: followerId, followingId → removes a follow relationship
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        PrintWriter out = resp.getWriter();

        // Reject request if the user is not logged in
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"error\":\"Not logged in\"}");
            return;
        }

        String path = req.getPathInfo();

        try {
            long followerId = Long.parseLong(req.getParameter("followerId"));
            long followingId = Long.parseLong(req.getParameter("followingId"));

            // Prevent a user from following themselves
            if (followerId == followingId) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"error\":\"Cannot follow yourself\"}");
                return;
            }

            boolean success;
            if ("/follow".equals(path)) {
                success = followDAO.followUser(followerId, followingId);
            } else if ("/unfollow".equals(path)) {
                success = followDAO.unfollowUser(followerId, followingId);
            } else {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            // Return whether the operation succeeded
            JsonObject result = new JsonObject();
            result.addProperty("success", success);
            out.print(gson.toJson(result));

        } catch (NumberFormatException e) {
            // Triggered if followerId or followingId can't be parsed as a number
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\":\"Invalid user ID\"}");
        }
    }
}
