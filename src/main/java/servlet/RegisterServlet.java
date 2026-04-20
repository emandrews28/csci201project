package servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.User;
import service.AuthService;

import java.io.IOException;

@WebServlet("/register")
public class RegisterServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private final AuthService authService = new AuthService();

    private static final String REGISTER_PAGE = "/register.html";
    private static final String HOME_PAGE = "/index.html";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendRedirect(request.getContextPath() + REGISTER_PAGE);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        String username = request.getParameter("username");
        String email = request.getParameter("email");
        String password = request.getParameter("password");

        try {
            User createdUser = authService.register(username, email, password);

            // Optional: log the user in immediately after registration
            HttpSession session = request.getSession(true);
            session.setAttribute("userId", createdUser.getUserId());
            session.setAttribute("username", createdUser.getUsername());
            session.setAttribute("email", createdUser.getEmail());

            response.sendRedirect(request.getContextPath() + HOME_PAGE);

        } catch (IllegalArgumentException e) {
            // Handles username taken, email taken, invalid input, weak password, etc.
            response.sendRedirect(
                    request.getContextPath() + REGISTER_PAGE + "?error=" + encode(e.getMessage())
            );

        } catch (RuntimeException e) {
            response.sendRedirect(request.getContextPath() + REGISTER_PAGE + "?error=server");
        }
    }

    private String encode(String message) {
        if (message == null) return "unknown";
        return message.replace(" ", "_");
    }
}