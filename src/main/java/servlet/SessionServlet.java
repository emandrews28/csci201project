package servlet;

import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/api/session")
public class SessionServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out = response.getWriter();
        HttpSession session = request.getSession(false);

        Map<String, Object> result = new HashMap<>();

        if (session == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            result.put("loggedIn", false);
            result.put("isGuest", false);
            out.print(gson.toJson(result));
            return;
        }

        // Check guest flag
        Boolean isGuest = (Boolean) session.getAttribute("isGuest");

        // If guest
        if (Boolean.TRUE.equals(isGuest)) {
            result.put("loggedIn", true);
            result.put("isGuest", true);
            result.put("username", "Guest");
            out.print(gson.toJson(result));
            return;
        }

        // Normal logged-in user
        if (session.getAttribute("userId") != null) {
            result.put("loggedIn", true);
            result.put("isGuest", false);
            result.put("userId", session.getAttribute("userId"));
            result.put("username", session.getAttribute("username"));
            result.put("email", session.getAttribute("email"));
            out.print(gson.toJson(result));
            return;
        }

        // Fallback: not logged in
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        result.put("loggedIn", false);
        result.put("isGuest", false);
        out.print(gson.toJson(result));
    }
}